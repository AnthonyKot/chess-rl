package com.chessrl.teacher

import java.io.File
import kotlin.math.ln

data class DiversityArgs(
    val dataPath: String = "data/teacher.ndjson",
    val topN: Int = 20
)

private fun parseArgs(args: Array<String>): DiversityArgs {
    var cfg = DiversityArgs()
    var i = 0
    while (i < args.size) {
        when (val a = args[i]) {
            "--data" -> { cfg = cfg.copy(dataPath = args.getOrNull(++i) ?: cfg.dataPath); i++ }
            "--top" -> { cfg = cfg.copy(topN = args.getOrNull(++i)?.toIntOrNull() ?: cfg.topN); i++ }
            else -> { println("Unknown arg: $a"); i++ }
        }
    }
    return cfg
}

private fun entropyBits(probs: Collection<Double>): Double {
    var s = 0.0
    for (p in probs) {
        if (p > 0.0) s += -p * (ln(p) / ln(2.0))
    }
    return s
}

private fun parseStringField(line: String, key: String): String? {
    val regex = Regex("\"$key\":\"(.*?)\"")
    val m = regex.find(line) ?: return null
    return m.groupValues[1]
}

private fun parseIntField(line: String, key: String): Int? {
    val regex = Regex("\"$key\":(-?\\d+)")
    val m = regex.find(line) ?: return null
    return m.groupValues[1].toIntOrNull()
}

private fun parseIntArray(line: String, key: String): List<Int> {
    val regex = Regex("\"$key\":\\[(.*?)\\]")
    val m = regex.find(line) ?: return emptyList()
    val body = m.groupValues[1].trim()
    if (body.isEmpty()) return emptyList()
    return body.split(',').mapNotNull { it.trim().toIntOrNull() }
}

private fun parsePolicy(line: String): Map<Int, Double> {
    val regex = Regex("\"teacher_policy\":\\{(.*?)\\}")
    val m = regex.find(line) ?: return emptyMap()
    val body = m.groupValues[1].trim()
    if (body.isEmpty()) return emptyMap()
    val out = HashMap<Int, Double>()
    val parts = body.split(',')
    for (p in parts) {
        val kv = p.split(":", limit = 2)
        if (kv.size == 2) {
            val k = kv[0].trim().trim('"').toIntOrNull()
            val v = kv[1].trim().toDoubleOrNull()
            if (k != null && v != null) out[k] = v
        }
    }
    return out
}

fun main(args: Array<String>) {
    val cfg = parseArgs(args)
    val file = File(cfg.dataPath)
    if (!file.exists()) {
        println("Dataset not found: ${file.absolutePath}")
        return
    }

    var total = 0L
    var whiteCount = 0L
    var blackCount = 0L

    val fenCounts = HashMap<String, Int>()
    val firstMoveCounts = HashMap<String, Int>() // ply 0 (white)
    val firstReplyCounts = HashMap<String, Int>() // ply 1 (black)

    val uniqueBestActions = HashSet<Int>()
    val uniqueTopKActions = HashSet<Int>()

    var topKTotal = 0L
    var topKSamples = 0L

    var entropySum = 0.0
    var entropySamples = 0L

    val gameMaxPly = HashMap<Int, Int>()

    file.bufferedReader().useLines { lines ->
        lines.forEach { line ->
            if (line.isBlank()) return@forEach
            total++

            val fen = parseStringField(line, "fen")
            if (fen != null) fenCounts[fen] = (fenCounts[fen] ?: 0) + 1

            when (parseStringField(line, "side")) {
                "w" -> whiteCount++
                "b" -> blackCount++
            }

            val move = parseStringField(line, "move")
            val ply = parseIntField(line, "ply")
            if (ply == 0 && move != null) firstMoveCounts[move] = (firstMoveCounts[move] ?: 0) + 1
            if (ply == 1 && move != null) firstReplyCounts[move] = (firstReplyCounts[move] ?: 0) + 1

            val best = parseIntField(line, "best_action")
            if (best != null) uniqueBestActions.add(best)

            val tk = parseIntArray(line, "top_k")
            if (tk.isNotEmpty()) {
                uniqueTopKActions.addAll(tk)
                topKTotal += tk.size
                topKSamples++
            }

            val pol = parsePolicy(line)
            if (pol.isNotEmpty()) {
                entropySum += entropyBits(pol.values)
                entropySamples++
            }

            val gid = parseIntField(line, "game_id")
            if (gid != null && ply != null) {
                val m = gameMaxPly[gid]
                if (m == null || ply > m) gameMaxPly[gid] = ply
            }
        }
    }

    val uniqueFens = fenCounts.size
    val uniqueFenRatio = if (total > 0) uniqueFens.toDouble() / total.toDouble() else 0.0
    val avgTopK = if (topKSamples > 0) topKTotal.toDouble() / topKSamples.toDouble() else 0.0
    val avgEntropy = if (entropySamples > 0) entropySum / entropySamples.toDouble() else 0.0
    val games = gameMaxPly.size
    val avgSamplesPerGame = if (games > 0) total.toDouble() / games.toDouble() else 0.0
    val minPly = gameMaxPly.values.minOrNull() ?: 0
    val maxPly = gameMaxPly.values.maxOrNull() ?: 0
    val avgMaxPly = if (games > 0) gameMaxPly.values.sum().toDouble() / games.toDouble() else 0.0

    fun <K> topN(map: Map<K, Int>, n: Int): List<Pair<K, Int>> = map.entries
        .sortedByDescending { it.value }
        .take(n)
        .map { it.key to it.value }

    println("=== Diversity Report ===")
    println("File: ${file.absolutePath}")
    println("Samples: $total | Games: $games | Avg samples/game: ${"%.2f".format(avgSamplesPerGame)}")
    println("Sides: white=$whiteCount black=$blackCount")
    println("Unique FENs: $uniqueFens (${"%.2f".format(uniqueFenRatio * 100)}%)")
    println("Unique best_action: ${uniqueBestActions.size} | Unique top_k actions: ${uniqueTopKActions.size}")
    println("Avg top_k size: ${"%.2f".format(avgTopK)} | Avg policy entropy (bits): ${"%.3f".format(avgEntropy)}")
    println("Game max ply: min=$minPly max=$maxPly avg=${"%.1f".format(avgMaxPly)}")

    println()
    println("Top ${cfg.topN} First Moves (ply=0):")
    for ((mv, c) in topN(firstMoveCounts, cfg.topN)) println("  $c\t$mv")

    println()
    println("Top ${cfg.topN} First Replies (ply=1):")
    for ((mv, c) in topN(firstReplyCounts, cfg.topN)) println("  $c\t$mv")

    println()
    println("Most Repeated FENs (top ${cfg.topN}):")
    for ((fen, c) in topN(fenCounts, cfg.topN)) println("  $c\t$fen")
}

