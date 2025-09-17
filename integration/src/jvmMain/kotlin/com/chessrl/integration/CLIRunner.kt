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
            argList.contains("--eval-h2h") -> evalHeadToHead(argList)
            argList.contains("--eval-non-nn") -> evalNonNN(argList)
            argList.contains("--train-advanced") -> trainAdvanced(argList)
            argList.contains("--play-human") -> playHuman(argList)
            else -> printHelp()
        }
    }

    private fun evalNonNN(args: List<String>) {
        // Configure sides
        val white = args.getAfter("--white")?.lowercase()?.trim() ?: "minimax"
        val black = args.getAfter("--black")?.lowercase()?.trim() ?: "heuristic"
        val games = args.getAfter("--games")?.toIntOrNull() ?: 10
        val maxSteps = args.getAfter("--max-steps")?.toIntOrNull() ?: 200
        val depth = args.getAfter("--depth")?.toIntOrNull() ?: 2
        val topk = args.getAfter("--topk")?.toIntOrNull() ?: 5
        val tau = args.getAfter("--tau")?.toDoubleOrNull() ?: 1.0

        // Instantiate Minimax teacher with seeded randomness when available
        val rng = runCatching { SeedManager.getNeuralNetworkRandom() }.getOrElse { kotlin.random.Random.Default }
        val teacher = com.chessrl.teacher.MinimaxTeacher(depth = depth, random = rng)
        val actionEncoder = ChessActionEncoder()
        val env = ChessEnvironment()

        var wins = 0
        var draws = 0
        var losses = 0
        var totalLen = 0

        println("Evaluating non-NN matchup: WHITE=$white vs BLACK=$black, games=$games, depth=$depth, topk=$topk, tau=$tau")

        repeat(games) { gi ->
            var state = env.reset()
            var steps = 0
            while (!env.isTerminal(state) && steps < maxSteps) {
                val actions = env.getValidActions(state)
                if (actions.isEmpty()) break
                val isWhiteToMove = env.getCurrentBoard().getActiveColor().name.contains("WHITE")

                val action = when {
                    isWhiteToMove && white == "heuristic" -> {
                        val sel = BaselineHeuristicOpponent.selectAction(env, actions)
                        if (sel >= 0) sel else actions.first()
                    }
                    isWhiteToMove && white == "minimax" -> {
                        val move = teacher.act(env.getCurrentBoard(), topK = topk, tau = tau).bestMove
                        val idx = actionEncoder.encodeMove(move)
                        if (idx in actions) idx else actions.first()
                    }
                    !isWhiteToMove && black == "heuristic" -> {
                        val sel = BaselineHeuristicOpponent.selectAction(env, actions)
                        if (sel >= 0) sel else actions.first()
                    }
                    !isWhiteToMove && black == "minimax" -> {
                        val move = teacher.act(env.getCurrentBoard(), topK = topk, tau = tau).bestMove
                        val idx = actionEncoder.encodeMove(move)
                        if (idx in actions) idx else actions.first()
                    }
                    else -> actions.first()
                }

                val step = env.step(action)
                state = step.nextState
                steps++
            }

            totalLen += steps
            val status = env.getGameStatus().name
            when {
                status.contains("WHITE_WINS") -> wins++
                status.contains("BLACK_WINS") -> losses++
                else -> draws++
            }
        }

        val counted = (wins + draws + losses).coerceAtLeast(1)
        val winRate = wins.toDouble() / counted
        val drawRate = draws.toDouble() / counted
        val lossRate = losses.toDouble() / counted
        val avgLen = totalLen.toDouble() / games.coerceAtLeast(1)

        println("Non-NN evaluation complete:")
        println("  White=$white vs Black=$black")
        println("  Wins=$wins, Draws=$draws, Losses=$losses")
        println("  WinRate=${"%.3f".format(winRate)}, DrawRate=${"%.3f".format(drawRate)}, LossRate=${"%.3f".format(lossRate)}")
        println("  AvgGameLength=${"%.1f".format(avgLen)} (max=$maxSteps)")
    }

    private fun evalBaseline(args: List<String>) {
        val games = args.getAfter("--games")?.toIntOrNull() ?: 5
        val maxSteps = args.getAfter("--max-steps")?.toIntOrNull() ?: 200
        val evalEpsilon = args.getAfter("--eval-epsilon")?.toDoubleOrNull()
        val colors = when (args.getAfter("--colors")?.lowercase()) {
            "white" -> ColorMode.WHITE
            "black" -> ColorMode.BLACK
            "alternate" -> ColorMode.ALTERNATE
            else -> ColorMode.WHITE
        }

        // Optional hidden layers from CLI or profile for shape-matching loaded models
        val profiles = ProfilesLoader.loadProfiles(listOf("profiles.yaml", "integration/profiles.yaml"))
        val profileName = args.getAfter("--profile")
        val profile = profileName?.let { profiles[it] }
        val hiddenFromCli = args.getAfter("--hidden")?.let { raw ->
            raw.split(",", " ", ";").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }
        }
        val hiddenFromProfile = profile?.get("hiddenLayers")?.let { raw ->
            raw.trim().removePrefix("[").removeSuffix("]")
                .split(",", " ", ";")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it > 0 }
                .takeIf { it.isNotEmpty() }
        }
        val hiddenLayers = hiddenFromCli ?: hiddenFromProfile ?: listOf(512, 256, 128)

        val agent = ChessAgentFactory.createDQNAgent(hiddenLayers = hiddenLayers)
        // Force evaluation epsilon if provided (greedy when 0.0)
        evalEpsilon?.let { agent.setExplorationRate(it) }
        // Optional checkpoint loading
        args.getAfter("--load")?.let { raw ->
            val path = resolvePath(raw)
            runCatching { agent.load(path) }.onFailure { println("Warning: failed to load checkpoint: ${it.message}") }
        } ?: run {
            if (args.contains("--load-best")) {
                val dir = args.getAfter("--checkpoint-dir") ?: AdvancedSelfPlayConfig().checkpointDirectory
                findBestCheckpointPath(dir)?.let { bestPrimary ->
                    // Prefer dedicated model artifact if present
                    val modelPath = bestPrimary
                        .replace(".json.gz", "_qnet.json")
                        .replace(".json", "_qnet.json")
                    val resolved = resolvePath(modelPath)
                    runCatching { agent.load(resolved) }
                        .recoverCatching {
                            // Fallback to primary checkpoint path
                            agent.load(resolvePath(bestPrimary))
                        }
                        .onFailure { println("Warning: failed to load best checkpoint: ${it.message}") }
                }
            }
        }
        // Align evaluation rewards with profile when available (no per-step shaping)
        val evalWin = profile?.get("winReward")?.toDoubleOrNull() ?: AdvancedSelfPlayConfig().winReward
        val evalLoss = profile?.get("lossReward")?.toDoubleOrNull() ?: AdvancedSelfPlayConfig().lossReward
        val evalDraw = profile?.get("drawReward")?.toDoubleOrNull() ?: 0.0
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = evalWin,
                lossReward = evalLoss,
                drawReward = evalDraw,
                stepPenalty = 0.0,
                enablePositionRewards = profile?.get("enablePositionRewards")?.toBooleanStrictOrNull() ?: false,
                maxGameLength = maxSteps
            )
        )

        var totalOutcomeScore = 0.0
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
            val agentIsWhite = when (colors) {
                ColorMode.WHITE -> true
                ColorMode.BLACK -> false
                ColorMode.ALTERNATE -> agentStartsWhite
            }
            while (!env.isTerminal(state) && steps < maxSteps) {
                val actions = env.getValidActions(state)
                if (actions.isEmpty()) break
                val active = env.getCurrentBoard().getActiveColor()
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
            // Use terminal outcome score for evaluation rather than per-step shaped reward
            totalOutcomeScore += when (outcome) {
                GameOutcome.WHITE_WINS -> if (agentIsWhite) 1.0 else -1.0
                GameOutcome.BLACK_WINS -> if (!agentIsWhite) 1.0 else -1.0
                else -> 0.0
            }
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
        val avgReward = totalOutcomeScore / games
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

    /**
     * Head-to-head evaluation: Agent A vs Agent B, alternating colors.
     */
    private fun evalHeadToHead(args: List<String>) {
        val games = args.getAfter("--games")?.toIntOrNull() ?: 20
        val maxSteps = args.getAfter("--max-steps")?.toIntOrNull() ?: 120
        val evalEpsilon = args.getAfter("--eval-epsilon")?.toDoubleOrNull() ?: 0.0
        val dumpDraws = args.contains("--dump-draws")
        val dumpLimit = args.getAfter("--dump-limit")?.toIntOrNull() ?: 3

        val pathAraw = args.getAfter("--loadA")
        val pathBraw = args.getAfter("--loadB")
        if (pathAraw == null || pathBraw == null) {
            println("Error: --loadA and --loadB are required for --eval-h2h")
            return
        }
        val pathA = resolvePath(pathAraw)
        val pathB = resolvePath(pathBraw)
        println("Resolved A=$pathA")
        println("Resolved B=$pathB")

        // Hidden layers (separate for A and B; fallback to shared or profile or default)
        val profiles = ProfilesLoader.loadProfiles(listOf("profiles.yaml", "integration/profiles.yaml"))
        val profileName = args.getAfter("--profile")
        val profile = profileName?.let { profiles[it] }
        fun parseHidden(raw: String?): List<Int>? = raw?.split(",", " ", ";")
            ?.mapNotNull { it.trim().toIntOrNull() }?.filter { it > 0 }?.takeIf { it.isNotEmpty() }
        val hiddenA = parseHidden(args.getAfter("--hiddenA"))
            ?: parseHidden(args.getAfter("--hidden"))
            ?: parseHidden(profile?.get("hiddenLayers"))
            ?: listOf(512, 256, 128)
        val hiddenB = parseHidden(args.getAfter("--hiddenB"))
            ?: parseHidden(args.getAfter("--hidden"))
            ?: parseHidden(profile?.get("hiddenLayers"))
            ?: listOf(512, 256, 128)

        val agentA = ChessAgentFactory.createDQNAgent(hiddenLayers = hiddenA)
        val agentB = ChessAgentFactory.createDQNAgent(hiddenLayers = hiddenB)
        runCatching { agentA.setExplorationRate(evalEpsilon) }
        runCatching { agentB.setExplorationRate(evalEpsilon) }

        val loadedA = runCatching { agentA.load(pathA) }.isSuccess
        val loadedB = runCatching { agentB.load(pathB) }.isSuccess
        if (!loadedA || !loadedB) {
            println("Warning: failed to load one or both agents. A=$loadedA, B=$loadedB")
        }

        // Outcome-based evaluation (no step penalty, no shaping)
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0, lossReward = -1.0, drawReward = 0.0,
                stepPenalty = 0.0, enablePositionRewards = false, maxGameLength = maxSteps
            )
        )

        var winsA = 0
        var draws = 0
        var lossesA = 0
        var totalLen = 0
        var drawStepLimit = 0
        var drawStalemate = 0
        var drawRepetition = 0
        var drawFifty = 0
        var drawInsufficient = 0
        var drawOther = 0
        var totalInvalid = 0

        var aStartsWhite = true
        var dumped = 0
        repeat(games) {
            var state = env.reset()
            var steps = 0
            val moveList = mutableListOf<String>()
            val startFen = env.getCurrentBoard().toFEN()
            var midFen: String? = null
            while (!env.isTerminal(state) && steps < maxSteps) {
                val valid = env.getValidActions(state)
                if (valid.isEmpty()) break
                val active = env.getCurrentBoard().getActiveColor()
                val aTurn = (active.name.contains("WHITE") && aStartsWhite) || (active.name.contains("BLACK") && !aStartsWhite)
                val action = if (aTurn) agentA.selectAction(state, valid) else agentB.selectAction(state, valid)
                val step = env.step(action)
                if (step.info["error"] != null) totalInvalid++
                step.info["move"]?.let { moveList.add(it.toString()) }
                state = step.nextState
                steps++
                if (midFen == null && steps >= maxSteps / 2) {
                    midFen = env.getCurrentBoard().toFEN()
                }
            }
            totalLen += steps
            val status = env.getGameStatus().name
            val stepLimited = steps >= maxSteps && !env.isTerminal(state)
            val outcomeA = when {
                status.contains("WHITE_WINS") -> if (aStartsWhite) 1 else -1
                status.contains("BLACK_WINS") -> if (!aStartsWhite) 1 else -1
                else -> 0
            }
            when (outcomeA) {
                1 -> winsA++
                -1 -> lossesA++
                else -> {
                    draws++
                    if (stepLimited) drawStepLimit++ else when {
                        status.contains("STALEMATE") -> drawStalemate++
                        status.contains("REPETITION") -> drawRepetition++
                        status.contains("FIFTY_MOVE_RULE") -> drawFifty++
                        status.contains("INSUFFICIENT_MATERIAL") -> drawInsufficient++
                        else -> drawOther++
                    }
                    if (dumpDraws && dumped < dumpLimit) {
                        dumped++
                        println("--- Draw #$dumped ---")
                        println("Reason: ${if (stepLimited) "STEP_LIMIT" else status}")
                        println("Steps: $steps / max=$maxSteps")
                        println("Start FEN: $startFen")
                        midFen?.let { println("Mid FEN: $it") }
                        println("Final FEN: ${env.getCurrentBoard().toFEN()}")
                        println("Final board:\n${env.getCurrentBoard().toASCII()}")
                        val tail = moveList.takeLast(10)
                        if (tail.isNotEmpty()) println("Last moves: ${tail.joinToString(",")}")
                    }
                }
            }
            aStartsWhite = !aStartsWhite
        }
        val counted = (winsA + draws + lossesA).coerceAtLeast(1)
        val winRate = winsA.toDouble() / counted
        val drawRate = draws.toDouble() / counted
        val lossRate = lossesA.toDouble() / counted
        val avgLen = totalLen.toDouble() / counted
        val avgOutcome = (winsA - lossesA).toDouble() / counted
        println("{" +
                "\"games\":$counted," +
                "\"avg_outcome\":$avgOutcome," +
                "\"avg_length\":$avgLen," +
                "\"win_rate\":$winRate," +
                "\"draw_rate\":$drawRate," +
                "\"loss_rate\":$lossRate," +
                "\"invalid_moves\":$totalInvalid," +
                "\"draw_details\":{" +
                    "\"step_limit\":$drawStepLimit," +
                    "\"stalemate\":$drawStalemate," +
                    "\"repetition\":$drawRepetition," +
                    "\"fifty_move\":$drawFifty," +
                    "\"insufficient\":$drawInsufficient," +
                    "\"other\":$drawOther" +
                "}" +
                "}")
    }

    private fun resolvePath(raw: String): String {
        // Try raw and common model/checkpoint variants across likely base dirs
        val bases = listOf("", "..", "../..")
        fun variants(p: String): List<String> = listOf(
            p,
            p.replace(".json.gz", "_qnet.json"),
            p.replace(".json", "_qnet.json")
        ).distinct()
        val candidates = bases.flatMap { base -> variants(if (base.isEmpty()) raw else "$base/$raw") }
        return candidates.firstOrNull { java.nio.file.Files.exists(java.nio.file.Path.of(it)) } ?: raw
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
        // Profiles support: load profiles.yaml and apply overrides if --profile is provided
        val profileName = args.getAfter("--profile")
        val profilesPathArg = args.getAfter("--profiles")
        val profiles = ProfilesLoader.loadProfiles(
            listOfNotNull(
                profilesPathArg,
                "profiles.yaml",
                "integration/profiles.yaml"
            )
        )
        val profile = profileName?.let { profiles[it] }

        // Determine algorithm from profile (default DQN)
        val algo = AgentType.DQN // Policy-gradient path removed; default to DQN

        // Use a more learnable default and merge profile/CLI overrides
        // Optional hidden layer override from CLI or profile
        val hiddenFromCli = args.getAfter("--hidden")?.let { raw ->
            raw.split(",", " ", ";").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }
        }
        val hiddenFromProfile = profile?.get("hiddenLayers")?.let { raw ->
            // Accept formats like "512,256" or "[512, 256]"
            raw.trim().removePrefix("[").removeSuffix("]")
                .split(",", " ", ";")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it > 0 }
                .takeIf { it.isNotEmpty() }
        }

        val epsStart = args.getAfter("--eps-start")?.toDoubleOrNull()
        val epsEnd = args.getAfter("--eps-end")?.toDoubleOrNull()
        val epsCycles = args.getAfter("--eps-cycles")?.toIntOrNull()

        val tunedConfig = AdvancedSelfPlayConfig(
            hiddenLayers = hiddenFromCli ?: hiddenFromProfile ?: AdvancedSelfPlayConfig().hiddenLayers,
            maxStepsPerGame = args.getAfter("--max-steps")?.toIntOrNull()
                ?: profile?.get("maxStepsPerGame")?.toIntOrNull() ?: 100,
            enablePositionRewards = profile?.get("enablePositionRewards")?.toBooleanStrictOrNull() ?: true,
            maxConcurrentGames = args.getAfter("--concurrency")?.toIntOrNull()
                ?: profile?.get("maxConcurrentGames")?.toIntOrNull() ?: 4,
            explorationRate = profile?.get("explorationRate")?.toDoubleOrNull() ?: AdvancedSelfPlayConfig().explorationRate,
            targetUpdateFrequency = args.getAfter("--target-update")?.toIntOrNull()
                ?: profile?.get("targetUpdateFrequency")?.toIntOrNull() ?: AdvancedSelfPlayConfig().targetUpdateFrequency,
            enableDoubleDQN = args.contains("--double-dqn") || (profile?.get("enableDoubleDQN")?.toBooleanStrictOrNull() ?: false),
            explorationWarmupCycles = profile?.get("explorationWarmupCycles")?.toIntOrNull() ?: AdvancedSelfPlayConfig().explorationWarmupCycles,
            explorationWarmupRate = profile?.get("explorationWarmupRate")?.toDoubleOrNull() ?: AdvancedSelfPlayConfig().explorationWarmupRate,
            epsDecayStart = epsStart,
            epsDecayEnd = epsEnd,
            epsDecayCycles = epsCycles,
            rollbackWarmupCycles = profile?.get("rollbackWarmupCycles")?.toIntOrNull() ?: AdvancedSelfPlayConfig().rollbackWarmupCycles,
            enableModelRollback = profile?.get("enableModelRollback")?.toBooleanStrictOrNull() ?: AdvancedSelfPlayConfig().enableModelRollback,
            checkpointDirectory = args.getAfter("--checkpoint-dir")
                ?: profile?.get("checkpointDirectory") ?: AdvancedSelfPlayConfig().checkpointDirectory,
            checkpointInterval = args.getAfter("--checkpoint-interval")?.toIntOrNull()
                ?: profile?.get("checkpointInterval")?.toIntOrNull() ?: AdvancedSelfPlayConfig().checkpointInterval,
            stepLimitPenalty = profile?.get("stepLimitPenalty")?.toDoubleOrNull() ?: AdvancedSelfPlayConfig().stepLimitPenalty,
            drawReward = args.getAfter("--draw-reward")?.toDoubleOrNull()
                ?: profile?.get("drawReward")?.toDoubleOrNull() ?: AdvancedSelfPlayConfig().drawReward,
            batchSize = profile?.get("batchSize")?.toIntOrNull() ?: AdvancedSelfPlayConfig().batchSize,
            treatStepLimitAsDrawForReporting = profile?.get("treatStepLimitAsDraw")?.toBooleanStrictOrNull() ?: AdvancedSelfPlayConfig().treatStepLimitAsDrawForReporting,
            autoCleanupOnFinish = if (args.contains("--no-cleanup")) false else (profile?.get("autoCleanupOnFinish")?.toBooleanStrictOrNull() ?: AdvancedSelfPlayConfig().autoCleanupOnFinish),
            opponentWarmupCycles = profile?.get("opponentWarmupCycles")?.toIntOrNull() ?: AdvancedSelfPlayConfig().opponentWarmupCycles,
            opponentUpdateStrategy = profile?.get("opponentUpdateStrategy")?.let { raw ->
                runCatching { OpponentUpdateStrategy.valueOf(raw.trim().uppercase()) }.getOrElse { AdvancedSelfPlayConfig().opponentUpdateStrategy }
            } ?: AdvancedSelfPlayConfig().opponentUpdateStrategy,
            opponentUpdateFrequency = profile?.get("opponentUpdateFrequency")?.toIntOrNull() ?: AdvancedSelfPlayConfig().opponentUpdateFrequency,
            opponentHistoryLag = profile?.get("opponentHistoryLag")?.toIntOrNull() ?: AdvancedSelfPlayConfig().opponentHistoryLag,
            retention = CheckpointRetention(
                keepBest = if (args.contains("--no-keep-best")) false else (profile?.get("keepBest")?.toBooleanStrictOrNull() ?: true),
                keepLastN = args.getAfter("--keep-last")?.toIntOrNull() ?: (profile?.get("keepLastN")?.toIntOrNull() ?: 2),
                keepEveryN = args.getAfter("--keep-every")?.toIntOrNull() ?: profile?.get("keepEveryN")?.toIntOrNull()
            )
        )
        // Helpful context for locating outputs
        runCatching {
            val cwd = java.nio.file.Paths.get("").toAbsolutePath().normalize().toString()
            println("Working directory: $cwd")
            println("Checkpoints directory (relative): ${tunedConfig.checkpointDirectory}")
        }
        val pipeline = AdvancedSelfPlayTrainingPipeline(tunedConfig, agentType = algo)
        check(pipeline.initialize())
        // Load model from CLI or profile (profile used only if CLI flag not provided and not resuming best)
        val cliLoad = args.getAfter("--load")
        val profileLoad = profile?.get("loadModelPath")
        fun resolveExistingPath(p: String?): String? {
            if (p.isNullOrBlank()) return null
            val candidates = listOf(
                java.nio.file.Path.of(p),
                java.nio.file.Path.of("..", p),
                java.nio.file.Path.of("../..", p)
            )
            return candidates.firstOrNull { java.nio.file.Files.exists(it) }?.toString()
        }
        when {
            cliLoad != null -> pipeline.loadCheckpointPath(resolveExistingPath(cliLoad) ?: cliLoad)
            resumeBest -> pipeline.loadBestCheckpoint()
            !profileLoad.isNullOrBlank() -> {
                val resolved = resolveExistingPath(profileLoad) ?: profileLoad
                pipeline.loadCheckpointPath(resolved)
            }
        }
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
              --eval-h2h --loadA PATH --loadB PATH [--games N] [--max-steps M] [--eval-epsilon E]
                Head-to-head: Agent A vs Agent B, alternating colors. Optional: --hiddenA/--hiddenB [--profile NAME]
              --train-advanced --cycles N [--resume-best] [--seed S] [--load PATH]
                Run advanced training for N cycles; optionally resume from best checkpoint.
                Profiles: [--profile NAME] [--profiles PATH]
                  - Overrides defaults via YAML (keys: hiddenLayers, algo, maxStepsPerGame, maxConcurrentGames, enablePositionRewards,
                    explorationRate, explorationWarmupCycles, explorationWarmupRate, rollbackWarmupCycles, enableModelRollback,
                    batchSize, checkpointDirectory, checkpointInterval, autoCleanupOnFinish, keepBest, keepLastN, keepEveryN,
                    stepLimitPenalty, treatStepLimitAsDraw, targetUpdateFrequency, enableDoubleDQN, loadModelPath)
                Overrides (flags take precedence over profiles):
                  --max-steps M              Per-game step cap
                  --eps-start E --eps-end E --eps-cycles N
                                         Linear epsilon decay after warmup (optional)
                  --checkpoint-dir DIR       Checkpoint base directory
                  --checkpoint-interval N    Save a regular checkpoint every N cycles
                  --no-cleanup               Disable end-of-run retention cleanup
                  --no-keep-best             Do not force keeping the best checkpoint
                  --keep-last K              Keep last K checkpoints (default 2)
                  --keep-every N             Also keep every N-th checkpoint (sparse history)
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
