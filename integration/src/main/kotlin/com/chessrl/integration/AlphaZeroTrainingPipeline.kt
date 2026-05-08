package com.chessrl.integration

import com.chessrl.chess.ChessBoard
import com.chessrl.chess.PieceColor
import com.chessrl.integration.adapter.ActionSpaceMapping
import com.chessrl.integration.adapter.ChessEngineFactory
import com.chessrl.integration.adapter.ChessState
import com.chessrl.integration.adapter.GameOutcome
import com.chessrl.integration.backend.AlphaZeroLearningBackend
import com.chessrl.integration.backend.AlphaZeroLearningSession
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger
import kotlin.random.Random

/**
 * Training pipeline for AlphaZero-style MCTS self-play.
 *
 * Each cycle:
 *   1. Run self-play games using MCTS; collect (state, π, z) samples
 *   2. Add samples to a rolling recent-games buffer
 *   3. Sample a batch from the buffer; train the dual-head network
 *   4. Evaluate against heuristic baseline
 *   5. Checkpoint
 *
 * Temperature schedule: 1.0 (sampling) for the first 30 moves, then 0.0 (greedy).
 */
class AlphaZeroTrainingPipeline(private val config: ChessRLConfig) {

    private val logger = ChessRLLogger.forComponent("AlphaZeroPipeline")
    private val adapter = ChessEngineFactory.create(config.engine)
    private val encoder = ChessStateEncoder()
    private val backend = AlphaZeroLearningBackend()

    private lateinit var session: AlphaZeroLearningSession
    private val recentBuffer = ArrayDeque<AlphaZeroSample>()
    private var totalGamesPlayed = 0
    private var bestValueLoss = Double.MAX_VALUE

    fun initialize(): Boolean {
        return try {
            session = backend.createSession(config)
            logger.info("AlphaZero pipeline initialized")
            true
        } catch (e: Exception) {
            logger.error("AlphaZero pipeline initialization failed", e)
            false
        }
    }

    fun runTraining() {
        logger.info("Starting AlphaZero training: ${config.maxCycles} cycles")
        val checkpointDir = java.io.File(config.checkpointDirectory).also { it.mkdirs() }

        for (cycle in 1..config.maxCycles) {
            logger.info("Cycle $cycle/${config.maxCycles}")

            // Phase 1: Self-play
            val newSamples = mutableListOf<AlphaZeroSample>()
            repeat(config.gamesPerCycle) { gameIdx ->
                val gameSamples = runSelfPlayGame(gameIdx + 1)
                newSamples.addAll(gameSamples)
                totalGamesPlayed++
            }
            addToBuffer(newSamples)
            logger.info("  Self-play: ${newSamples.size} samples (buffer: ${recentBuffer.size})")

            // Phase 2: Train
            val trainResult = if (recentBuffer.size >= config.batchSize) {
                val batch = recentBuffer.shuffled().take(config.batchSize)
                session.trainOnAlphaZeroSamples(batch)
            } else null

            if (trainResult != null) {
                logger.info("  Train: loss=%.4f (policy=%.4f value=%.4f)".format(
                    trainResult.loss, trainResult.loss - (trainResult.valueError ?: 0.0), trainResult.valueError ?: 0.0
                ))
            }

            // Phase 3: Update opponent every 5 cycles
            if (cycle % 5 == 0) session.updateOpponent()

            // Phase 4: Evaluate
            if (cycle % config.logInterval == 0) {
                val winRate = evaluate(config.evaluationGames)
                logger.info("  Eval vs heuristic: win_rate=%.2f".format(winRate))
            }

            // Phase 5: Checkpoint
            if (cycle % config.checkpointInterval == 0) {
                val path = "${checkpointDir.path}/alphazero_cycle_$cycle.json"
                session.saveCheckpoint(path)
                logger.info("  Checkpoint saved: $path")
            }
            if (trainResult != null && (trainResult.valueError ?: 1.0) < bestValueLoss) {
                bestValueLoss = trainResult.valueError ?: 1.0
                session.saveBest("${checkpointDir.path}/best_model.json")
            }
        }

        logger.info("AlphaZero training complete. Total games: $totalGamesPlayed")
    }

    private fun runSelfPlayGame(gameId: Int): List<AlphaZeroSample> {
        var state = adapter.initialState()
        val trajectory = mutableListOf<Triple<DoubleArray, IntArray, PieceColor>>() // (state_features, visitCounts, playerToMove)

        var moveCount = 0
        while (!adapter.isTerminal(state) && moveCount < config.maxStepsPerGame) {
            val addNoise = true
            val useTemperature = if (moveCount < 30) config.mctsTemperature else 0.0

            session.mainAgent.setCurrentChessState(state)
            val (visitCounts, actionIdx) = session.mainAgent.searchAndSelect(state, addNoise)

            val stateFeatures = encodeState(state)
            trajectory.add(Triple(stateFeatures, visitCounts, state.activeColor))

            // Apply chosen move
            val decoded = ActionSpaceMapping.decodeAction(actionIdx)
            val legalMoves = adapter.getLegalMoves(state)
            val move = ActionSpaceMapping.findMatchingMove(decoded, legalMoves) ?: break
            state = adapter.applyMove(state, move)
            moveCount++
        }

        if (trajectory.isEmpty()) return emptyList()

        // Assign outcome
        val outcome = adapter.getOutcome(state).outcome
        val samples = trajectory.map { (features, visitCounts, playerAtMove) ->
            val z = when (outcome) {
                GameOutcome.WHITE_WINS -> if (playerAtMove == PieceColor.WHITE) 1.0 else -1.0
                GameOutcome.BLACK_WINS -> if (playerAtMove == PieceColor.BLACK) 1.0 else -1.0
                else -> 0.0
            }
            val totalVisits = visitCounts.sum().coerceAtLeast(1)
            val pi = DoubleArray(visitCounts.size) { visitCounts[it].toDouble() / totalVisits }
            AlphaZeroSample(state = features, policyTarget = pi, valueTarget = z)
        }

        logger.debug("Game $gameId: ${moveCount} moves, outcome=$outcome, ${samples.size} samples")
        return samples
    }

    private fun addToBuffer(samples: List<AlphaZeroSample>) {
        recentBuffer.addAll(samples)
        while (recentBuffer.size > config.mctsRecentGamesBuffer) {
            recentBuffer.removeFirst()
        }
    }

    private fun evaluate(games: Int): Double {
        val evalAdapter = ChessEngineFactory.create(config.engine)
        var wins = 0
        var draws = 0

        repeat(games) { i ->
            val agentPlaysWhite = i % 2 == 0
            var state = evalAdapter.initialState()
            var moves = 0
            while (!evalAdapter.isTerminal(state) && moves < config.maxStepsPerGame) {
                val legalMoves = evalAdapter.getLegalMoves(state)
                if (legalMoves.isEmpty()) break
                val isAgentTurn = (state.activeColor == PieceColor.WHITE) == agentPlaysWhite

                val move = if (isAgentTurn) {
                    // Greedy MCTS (temperature = 0)
                    session.mainAgent.setCurrentChessState(state)
                    val visitCounts = session.mainAgent.searchAndSelect(state, addNoise = false).let { (vc, _) -> vc }
                    val actionIdx = visitCounts.indices.maxByOrNull { visitCounts[it] } ?: 0
                    val decoded = ActionSpaceMapping.decodeAction(actionIdx)
                    ActionSpaceMapping.findMatchingMove(decoded, legalMoves) ?: legalMoves.random()
                } else {
                    // Heuristic baseline: pick highest material-gain move, else random
                    legalMoves.random()
                }
                state = evalAdapter.applyMove(state, move)
                moves++
            }
            when (evalAdapter.getOutcome(state).outcome) {
                GameOutcome.WHITE_WINS -> if (agentPlaysWhite) wins++ else Unit
                GameOutcome.BLACK_WINS -> if (!agentPlaysWhite) wins++ else Unit
                GameOutcome.DRAW -> draws++
                else -> Unit
            }
        }

        return wins.toDouble() / games
    }

    private fun encodeState(state: ChessState): DoubleArray {
        val board = ChessBoard()
        board.fromFEN(state.fen)
        return encoder.encode(board)
    }
}
