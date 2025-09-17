package com.chessrl.teacher

import com.chessrl.chess.*
import kotlin.random.Random
import java.io.BufferedWriter
import java.io.File
import java.time.Instant
import java.util.Locale

data class CollectorConfig(
    val games: Int = 50,
    val maxPlies: Int = 160,
    val topK: Int = 5,
    val tau: Double = 1.0,
    val depth: Int = 2,
    val seed: Long = 42L,
    val outPath: String = "teacher_dataset.ndjson",
    val dropDraws: Boolean = false,
    val maxRepeatsPerPosition: Int = 2,
    val movesLimit: Int = 0
)

data class NDJSONSample(
    val fen: String,
    val side: String,
    val best_action: Int,
    val top_k: List<Int>,
    val teacher_policy: Map<Int, Double>,
    val value: Double,
    val valid_actions: List<Int>,
    val move: String,
    val game_id: Int,
    val ply: Int,
    val result_hint: String? = null,
    val ts: Long = Instant.now().toEpochMilli()
)

fun runCollection(config: CollectorConfig) {
    val rng = Random(config.seed)
    val teacher = MinimaxTeacher(
        depth = config.depth,
        random = rng,
        maxCandidates = if (config.movesLimit > 0) config.movesLimit else null
    )
    val outFile = File(config.outPath)
    outFile.parentFile?.mkdirs()
    outFile.bufferedWriter().use { writer ->
        val seen = mutableMapOf<String, Int>()
        var written = 0
        for (g in 0 until config.games) {
            val game = ChessGame()
            var ply = 0
            var done = false
            while (!done && ply < config.maxPlies) {
                val board = game.getCurrentBoard()
                val fen = board.toFEN()
                // Deduplicate by FEN+side (FEN already encodes side-to-move)
                val count = seen.getOrDefault(fen, 0)
                val active = board.getActiveColor()
                val validMoves = board.getAllValidMoves(active)
                if (validMoves.isEmpty()) {
                    // Terminal; break
                    done = true
                    break
                }
                val teach = teacher.act(board, config.topK, config.tau)
                if (count < config.maxRepeatsPerPosition) {
                    val validActions = validMoves.map { ActionIndexMapper.moveToIndex(it) }
                    val bestIdx = ActionIndexMapper.moveToIndex(teach.bestMove)
                    val tkIdx = teach.topK.map { ActionIndexMapper.moveToIndex(it) }
                    val pol = teach.policy.entries.associate { (m, p) -> ActionIndexMapper.moveToIndex(m) to p }
                    val sample = NDJSONSample(
                        fen = fen,
                        side = if (active == PieceColor.WHITE) "w" else "b",
                        best_action = bestIdx,
                        top_k = tkIdx,
                        teacher_policy = pol,
                        value = teach.value,
                        valid_actions = validActions,
                        move = teach.bestMove.toAlgebraic(),
                        game_id = g,
                        ply = ply,
                        result_hint = null
                    )
                    writer.write(toJson(sample))
                    writer.newLine()
                    written++
                    seen[fen] = count + 1
                }

                // Play teacher move sampled from top-k distribution
                val pick = sampleFromPolicy(teach.policy, rng) ?: teach.bestMove
                game.makeMove(pick)
                ply++
                if (ply % 20 == 0) println("game=${g+1}/${config.games} ply=$ply")

                // Optional: drop draws by skipping later plies while game is undecided
                val status = game.getCurrentBoard().getGameStatus()
                when (status) {
                    GameStatus.WHITE_WINS, GameStatus.BLACK_WINS -> done = true
                    GameStatus.DRAW_STALEMATE, GameStatus.DRAW_INSUFFICIENT_MATERIAL,
                    GameStatus.DRAW_FIFTY_MOVE_RULE, GameStatus.DRAW_REPETITION -> {
                        if (config.dropDraws) done = true
                    }
                    else -> {}
                }
            }
        }
        writer.flush()
        println("Wrote $written samples to ${outFile.absolutePath}")
    }
}

private fun sampleFromPolicy(prob: Map<Move, Double>, rng: Random): Move? {
    if (prob.isEmpty()) return null
    val entries = prob.entries.toList()
    var r = rng.nextDouble()
    for ((m, p) in entries) {
        r -= p
        if (r <= 0.0) return m
    }
    return entries.last().key
}

// Minimal JSON writer to avoid extra deps
private fun toJson(s: NDJSONSample): String {
    fun esc(x: String) = x.replace("\\", "\\\\").replace("\"", "\\\"")
    val policyJson = s.teacher_policy.entries.joinToString(prefix = "{", postfix = "}") { (k, v) -> "\"$k\":${String.format(Locale.US, "%.6f", v)}" }
    val topkJson = s.top_k.joinToString(prefix = "[", postfix = "]")
    val validJson = s.valid_actions.joinToString(prefix = "[", postfix = "]")
    return "{" +
        "\"fen\":\"${esc(s.fen)}\"," +
        "\"side\":\"${esc(s.side)}\"," +
        "\"best_action\":${s.best_action}," +
        "\"top_k\":$topkJson," +
        "\"teacher_policy\":$policyJson," +
        "\"value\":${String.format(Locale.US, "%.6f", s.value)}," +
        "\"valid_actions\":$validJson," +
        "\"move\":\"${esc(s.move)}\"," +
        "\"game_id\":${s.game_id}," +
        "\"ply\":${s.ply}," +
        (s.result_hint?.let { "\"result_hint\":\"${esc(it)}\"," } ?: "") +
        "\"ts\":${s.ts}" +
    "}"
}

/**
 * CLI entrypoint for teacher dataset collection.
 * Example:
 *   ./gradlew :chess-engine:runTeacherCollector -Dargs="--collect --games 20 --depth 2 --topk 5 --tau 1.0 --out data/teacher.ndjson"
 */
fun main(args: Array<String>) {
    var cfg = CollectorConfig()
    var collect = false

    var i = 0
    while (i < args.size) {
        when (val a = args[i]) {
            "--collect" -> { collect = true; i++ }
            "--games" -> { cfg = cfg.copy(games = args.getOrNull(++i)?.toInt() ?: cfg.games); i++ }
            "--max-plies" -> { cfg = cfg.copy(maxPlies = args.getOrNull(++i)?.toInt() ?: cfg.maxPlies); i++ }
            "--topk" -> { cfg = cfg.copy(topK = args.getOrNull(++i)?.toInt() ?: cfg.topK); i++ }
            "--tau" -> { cfg = cfg.copy(tau = args.getOrNull(++i)?.toDouble() ?: cfg.tau); i++ }
            "--depth" -> { cfg = cfg.copy(depth = args.getOrNull(++i)?.toInt() ?: cfg.depth); i++ }
            "--seed" -> { cfg = cfg.copy(seed = args.getOrNull(++i)?.toLong() ?: cfg.seed); i++ }
            "--out" -> { cfg = cfg.copy(outPath = args.getOrNull(++i) ?: cfg.outPath); i++ }
            "--drop-draws" -> { cfg = cfg.copy(dropDraws = true); i++ }
            "--max-repeats" -> { cfg = cfg.copy(maxRepeatsPerPosition = args.getOrNull(++i)?.toInt() ?: cfg.maxRepeatsPerPosition); i++ }
            "--moves-limit" -> { cfg = cfg.copy(movesLimit = args.getOrNull(++i)?.toInt() ?: cfg.movesLimit); i++ }
            else -> { println("Unknown arg: $a"); i++ }
        }
    }
    if (!collect) {
        println("No action. Use --collect to generate teacher dataset.")
        return
    }
    println("Teacher data collection starting: $cfg")
    runCollection(cfg)
}
