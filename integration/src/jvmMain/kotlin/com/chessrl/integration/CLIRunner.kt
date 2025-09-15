package com.chessrl.integration

import com.chessrl.chess.PieceColor
import kotlin.math.abs

object CLIRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        val argList = args.toList()

        // Optional seed initialization for deterministic runs
        runCatching {
            val seedIdx = argList.indexOf("--seed")
            when {
                seedIdx >= 0 && seedIdx + 1 < argList.size -> {
                    val seed = argList[seedIdx + 1].toLong()
                    SeedManager.initializeWithSeed(seed)
                }
                argList.contains("--random-seed") -> {
                    SeedManager.initializeWithRandomSeed()
                }
                else -> Unit
            }
        }.onFailure { /* ignore parse errors and proceed non-deterministic */ }
        when {
            argList.contains("--eval-baseline") -> evalBaseline(argList)
            argList.contains("--train-advanced") -> trainAdvanced(argList)
            argList.contains("--play-human") -> playHuman(argList)
            else -> printHelp()
        }
    }

    private fun evalBaseline(args: List<String>) {
        val games = args.getAfter("--games")?.toIntOrNull() ?: 5
        val maxSteps = args.getAfter("--max-steps")?.toIntOrNull() ?: 200
        val colors = when (args.getAfter("--colors")?.lowercase()) {
            "white" -> ColorMode.WHITE
            "black" -> ColorMode.BLACK
            "alternate" -> ColorMode.ALTERNATE
            else -> ColorMode.WHITE
        }

        val agent = ChessAgentFactory.createDQNAgent()
        // Optional checkpoint loading
        args.getAfter("--load")?.let { path ->
            runCatching { agent.load(path) }.onFailure { println("Warning: failed to load checkpoint: ${it.message}") }
        } ?: run {
            if (args.contains("--load-best")) {
                val dir = args.getAfter("--checkpoint-dir") ?: AdvancedSelfPlayConfig().checkpointDirectory
                findBestCheckpointPath(dir)?.let { best ->
                    runCatching { agent.load(best) }.onFailure { println("Warning: failed to load best checkpoint: ${it.message}") }
                }
            }
        }
        val env = ChessEnvironment()

        var totalReward = 0.0
        var totalLen = 0
        var wins = 0
        var draws = 0
        var losses = 0
        val results = mutableListOf<EvaluationGameResult>()

        var agentStartsWhite = true
        repeat(games) { idx ->
            var state = env.reset()
            var rewardSum = 0.0
            var steps = 0
            while (!env.isTerminal(state) && steps < maxSteps) {
                val actions = env.getValidActions(state)
                if (actions.isEmpty()) break
                val active = env.getCurrentBoard().getActiveColor()
                val agentIsWhite = when (colors) {
                    ColorMode.WHITE -> true
                    ColorMode.BLACK -> false
                    ColorMode.ALTERNATE -> agentStartsWhite
                }
                val agentTurn = (active.name.contains("WHITE") && agentIsWhite) || (active.name.contains("BLACK") && !agentIsWhite)
                val action = if (agentTurn) agent.selectAction(state, actions) else {
                    val sel = BaselineHeuristicOpponent.selectAction(env, actions)
                    if (sel >= 0) sel else agent.selectAction(state, actions)
                }
                val step = env.step(action)
                rewardSum += step.reward
                state = step.nextState
                steps++
            }
            val status = env.getGameStatus().name
            val outcome = when {
                status.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
                status.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
                status.contains("DRAW") || status.contains("ONGOING") -> GameOutcome.DRAW
                else -> GameOutcome.DRAW
            }
            when (outcome) {
                GameOutcome.WHITE_WINS -> wins++
                GameOutcome.BLACK_WINS -> losses++
                GameOutcome.DRAW, GameOutcome.ONGOING -> draws++
            }
            totalReward += rewardSum
            totalLen += steps
            results.add(
                EvaluationGameResult(
                    gameIndex = idx,
                    gameLength = steps,
                    totalReward = rewardSum,
                    gameOutcome = outcome,
                    finalPosition = env.getCurrentBoard().toFEN()
                )
            )
            if (colors == ColorMode.ALTERNATE) agentStartsWhite = !agentStartsWhite
        }

        val counted = (wins + draws + losses).coerceAtLeast(1)
        val avgReward = totalReward / games
        val avgLen = totalLen.toDouble() / games
        val winRate = wins.toDouble() / counted
        val drawRate = draws.toDouble() / counted
        val lossRate = losses.toDouble() / counted
        val perfScore = calculatePerformanceScore(avgReward, winRate, drawRate, avgLen, maxSteps)

        // Print concise JSON-like summary
        println("{" +
            "\"games\":$games," +
            "\"avg_reward\":$avgReward," +
            "\"avg_length\":$avgLen," +
            "\"win_rate\":$winRate," +
            "\"draw_rate\":$drawRate," +
            "\"loss_rate\":$lossRate," +
            "\"performance_score\":$perfScore" +
        "}")
    }

    private fun calculatePerformanceScore(
        avgReward: Double,
        winRate: Double,
        drawRate: Double,
        avgGameLength: Double,
        maxStepsPerGame: Int
    ): Double {
        val rewardWeight = 0.4
        val winRateWeight = 0.3
        val drawRateWeight = 0.1
        val gameLengthWeight = 0.2
        val normalizedReward = ((avgReward + 1.0) / 2.0).coerceIn(0.0, 1.0)
        val normalizedGameLength = 1.0 - (avgGameLength / maxStepsPerGame).coerceIn(0.0, 1.0)
        return rewardWeight * normalizedReward +
                winRateWeight * winRate +
                drawRateWeight * drawRate +
                gameLengthWeight * normalizedGameLength
    }

    private fun trainAdvanced(args: List<String>) {
        val cycles = args.getAfter("--cycles")?.toIntOrNull() ?: 3
        val resumeBest = args.contains("--resume-best")
        // Use a more learnable default: shorter episodes + positional rewards
        val tunedConfig = AdvancedSelfPlayConfig(
            maxStepsPerGame = args.getAfter("--max-steps")?.toIntOrNull() ?: 100,
            enablePositionRewards = true
        )
        // Helpful context for locating outputs
        runCatching {
            val cwd = java.nio.file.Paths.get("").toAbsolutePath().normalize().toString()
            println("Working directory: $cwd")
            println("Checkpoints directory (relative): ${tunedConfig.checkpointDirectory}")
        }
        val pipeline = AdvancedSelfPlayTrainingPipeline(tunedConfig)
        check(pipeline.initialize())
        args.getAfter("--load")?.let { path ->
            pipeline.loadCheckpointPath(path)
        }
        if (resumeBest) pipeline.loadBestCheckpoint()
        val results = pipeline.runAdvancedTraining(cycles)
        println("Training completed: cycles=${results.totalCycles}, bestModel=${results.bestModelVersion}, bestPerf=${results.bestPerformance}")
    }

    private fun playHuman(args: List<String>) {
        val humanColor = when (args.getAfter("--as")?.lowercase()) {
            "white" -> PieceColor.WHITE
            "black" -> PieceColor.BLACK
            else -> PieceColor.WHITE
        }
        val maxSteps = args.getAfter("--max-steps")?.toIntOrNull() ?: 200

        val agent = ChessAgentFactory.createDQNAgent()
        // Optional checkpoint loading
        args.getAfter("--load")?.let { path ->
            runCatching { agent.load(path) }.onFailure { println("Warning: failed to load checkpoint: ${it.message}") }
        } ?: run {
            if (args.contains("--load-best")) {
                val dir = args.getAfter("--checkpoint-dir") ?: AdvancedSelfPlayConfig().checkpointDirectory
                findBestCheckpointPath(dir)?.let { best ->
                    runCatching { agent.load(best) }.onFailure { println("Warning: failed to load best checkpoint: ${it.message}") }
                }
            }
        }

        val env = ChessEnvironment()
        var state = env.reset()
        val encoder = ChessActionEncoder()

        println("Starting human vs agent. You play ${if (humanColor == PieceColor.WHITE) "White" else "Black"}.")
        println("Enter moves in coordinate form like e2e4. 'q' to quit.")
        println(env.getCurrentBoard().toASCII())

        var steps = 0
        while (steps < maxSteps) {
            val active = env.getCurrentBoard().getActiveColor()
            val humanTurn = active == humanColor
            val validActions = env.getValidActions(state)
            if (validActions.isEmpty()) break

            if (humanTurn) {
                print("Your move> ")
                val input = readLine()?.trim()?.lowercase()
                if (input == null) continue
                if (input == "q" || input == "quit" || input == "exit") {
                    println("Exiting.")
                    return
                }
                val move = com.chessrl.chess.Move.fromAlgebraic(input)
                if (move == null) {
                    println("Invalid format. Use coordinate like e2e4.")
                    continue
                }
                // Find matching legal action
                val actionFromInput = encoder.encodeMove(move)
                val chosen = validActions.firstOrNull { a ->
                    val decoded = encoder.decodeAction(a)
                    decoded.from == move.from && decoded.to == move.to
                } ?: actionFromInput
                val step = env.step(chosen)
                if (step.info["error"] != null) {
                    println("Illegal move: ${input}. Try again.")
                    continue
                }
                state = step.nextState
                steps++
                println(env.getCurrentBoard().toASCII())
                if (step.done) break
            } else {
                val action = agent.selectAction(state, validActions)
                val step = env.step(action)
                state = step.nextState
                steps++
                val moveStr = step.info["move"] ?: "?"
                println("Agent plays: ${moveStr}")
                println(env.getCurrentBoard().toASCII())
                if (step.done) break
            }
        }

        val status = env.getGameStatus()
        println("Game over: ${status}")
    }

    private fun List<String>.getAfter(flag: String): String? {
        val i = indexOf(flag)
        return if (i >= 0 && i + 1 < size) this[i + 1] else null
    }

    private fun printHelp() {
        println(
            """
            Chess RL CLI
            Usage:
              --eval-baseline --games N [--max-steps M] [--colors white|black|alternate] [--seed S] [--load PATH|--load-best [--checkpoint-dir DIR]]
                Evaluate agent vs baseline for N games. Agent plays White by default.
              --train-advanced --cycles N [--resume-best] [--seed S] [--load PATH]
                Run advanced training for N cycles; optionally resume from best checkpoint.
              --play-human [--as white|black] [--max-steps M] [--seed S] [--load PATH|--load-best [--checkpoint-dir DIR]]
                Play against the agent in the console. Enter moves like e2e4; 'q' to quit.
            """.trimIndent()
        )
    }

    private enum class ColorMode { WHITE, BLACK, ALTERNATE }

    private fun findBestCheckpointPath(dir: String): String? {
        return try {
            val d = java.nio.file.Path.of(dir)
            if (!java.nio.file.Files.exists(d)) return null
            java.nio.file.Files.list(d)
                .filter { p ->
                    val n = p.fileName.toString()
                    n.startsWith("checkpoint_v") && (n.endsWith(".json") || n.endsWith(".json.gz"))
                }
                .max(java.util.Comparator.comparingLong { p -> p.toFile().lastModified() })
                .map { it.toString() }
                .orElse(null)
        } catch (e: Exception) {
            println("Warning: failed to scan checkpoints in $dir: ${e.message}")
            null
        }
    }
}
