package com.chessrl.integration

import com.chessrl.chess.PieceColor
import com.chessrl.integration.backend.DqnLearningBackend
import com.chessrl.integration.backend.LearningBackend
import com.chessrl.integration.config.ChessRLConfig
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
        val enableAdjudication = args.contains("--adjudicate")
        val resignMaterial = args.getAfter("--resign-material")?.toIntOrNull() ?: 9
        val noProgressPlies = args.getAfter("--no-progress-plies")?.toIntOrNull() ?: 40

        // Instantiate Minimax teacher with seeded randomness when available
        val rng = runCatching { SeedManager.getNeuralNetworkRandom() }.getOrElse { kotlin.random.Random.Default }
        val teacher = com.chessrl.teacher.MinimaxTeacher(depth = depth, random = rng)
        val actionEncoder = ChessActionEncoder()
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0,
                stepPenalty = 0.0,
                stepLimitPenalty = -0.5,
                enablePositionRewards = false,
                gameLengthNormalization = false,
                maxGameLength = maxSteps,
                enableEarlyAdjudication = enableAdjudication,
                resignMaterialThreshold = resignMaterial,
                noProgressPlies = noProgressPlies
            )
        )

        var wins = 0
        var draws = 0
        var losses = 0
        var totalLen = 0

        println("Evaluating non-NN matchup: WHITE=$white vs BLACK=$black, games=$games, depth=$depth, topk=$topk, tau=$tau")
        if (enableAdjudication) println("  Early adjudication: resignMaterial=$resignMaterial, noProgressPlies=$noProgressPlies")

        repeat(games) { _ ->
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
                val dir = args.getAfter("--checkpoint-dir") ?: "checkpoints"
                resolveBestModelPath(dir)?.let { best ->
                    val resolved = resolvePath(best)
                    // Print performance score if sidecar meta exists
                    runCatching {
                        fun metaFor(p: String): String {
                            return if (p.endsWith("_qnet.json")) p.replace("_qnet.json", "_qnet_meta.json") else "$p.meta.json"
                        }
                        val meta = java.nio.file.Path.of(metaFor(resolved))
                        if (java.nio.file.Files.exists(meta)) {
                            val txt = java.nio.file.Files.readString(meta)
                            val perfIdx = txt.indexOf("\"performance\":")
                            val perf = if (perfIdx >= 0) txt.substring(perfIdx + 14).takeWhile { it !in listOf(',', '}', ' ', '\n', '\r') } else null
                            if (perf != null) println("Model performance (meta): $perf")
                        }
                    }
                    runCatching { agent.load(resolved) }
                        .onFailure { println("Warning: failed to load best model: ${it.message}") }
                } ?: run {
                    // Final fallback: latest checkpoint
                    findBestCheckpointPath(dir)?.let { latest ->
                        val modelPath = latest
                            .replace(".json.gz", "_qnet.json")
                            .replace(".json", "_qnet.json")
                        val resolved = resolvePath(modelPath)
                        runCatching { agent.load(resolved) }
                            .recoverCatching { agent.load(resolvePath(latest)) }
                            .onFailure { println("Warning: failed to load latest checkpoint: ${it.message}") }
                    }
                }
            }
        }
        // Align evaluation rewards with profile when available (no per-step shaping)
        val evalWin = profile?.get("winReward")?.toDoubleOrNull() ?: 1.0
        val evalLoss = profile?.get("lossReward")?.toDoubleOrNull() ?: -1.0
        val evalDraw = profile?.get("drawReward")?.toDoubleOrNull() ?: 0.0
        val enableAdjudication = args.contains("--adjudicate")
        val resignMaterial = args.getAfter("--resign-material")?.toIntOrNull() ?: 9
        val noProgressPlies = args.getAfter("--no-progress-plies")?.toIntOrNull() ?: 40
        val enableLocalThreefoldEval = args.contains("--local-threefold")
        val localThreefoldEvalThreshold = args.getAfter("--threefold-threshold")?.toIntOrNull() ?: 3
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = evalWin,
                lossReward = evalLoss,
                drawReward = evalDraw,
                stepPenalty = 0.0,
                stepLimitPenalty = profile?.get("stepLimitPenalty")?.toDoubleOrNull() ?: -0.5,
                enablePositionRewards = profile?.get("enablePositionRewards")?.toBooleanStrictOrNull() ?: false,
                maxGameLength = maxSteps,
                enableEarlyAdjudication = enableAdjudication,
                resignMaterialThreshold = resignMaterial,
                noProgressPlies = noProgressPlies,
                gameLengthNormalization = false
            )
        )

        var totalOutcomeScore = 0.0
        var totalLen = 0
        var wins = 0
        var draws = 0
        var losses = 0
        val results = mutableListOf<EvaluationGameSummary>()
        // Draw breakdown
        var drawStepLimit = 0
        var drawStalemate = 0
        var drawRepetition = 0
        var drawFifty = 0
        var drawInsufficient = 0
        var drawOther = 0
        var drawLocalThreefold = 0

        var agentStartsWhite = true
        val dumpDraws = args.contains("--dump-draws")
        val dumpLimit = args.getAfter("--dump-limit")?.toIntOrNull() ?: 3
        val drawSamples = mutableListOf<String>()
        repeat(games) { idx ->
            var state = env.reset()
            var rewardSum = 0.0
            var steps = 0
            val agentIsWhite = when (colors) {
                ColorMode.WHITE -> true
                ColorMode.BLACK -> false
                ColorMode.ALTERNATE -> agentStartsWhite
            }
            val startFen = env.getCurrentBoard().toFEN()
            var midFen: String? = null
            val moveList = mutableListOf<String>()
            val fenCounts = mutableMapOf(env.getCurrentBoard().toFEN() to 1)
            var trippedLocalThreefold = false
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
                step.info["move"]?.let { moveList.add(it.toString()) }
                if (midFen == null && steps >= maxSteps / 2) {
                    midFen = env.getCurrentBoard().toFEN()
                }
                if (enableLocalThreefoldEval) {
                    val fen = env.getCurrentBoard().toFEN()
                    val cnt = (fenCounts[fen] ?: 0) + 1
                    fenCounts[fen] = cnt
                    if (cnt >= localThreefoldEvalThreshold) {
                        trippedLocalThreefold = true
                        break
                    }
                }
            }
            val status = env.getGameStatus().name
            val raw = when {
                trippedLocalThreefold -> GameOutcome.DRAW
                status.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
                status.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
                status.contains("DRAW") -> GameOutcome.DRAW
                status.contains("ONGOING") -> GameOutcome.ONGOING
                else -> GameOutcome.ONGOING  // Default to ONGOING for unrecognized states
            }
            val agentOutcome = when (raw) {
                GameOutcome.WHITE_WINS -> if (agentIsWhite) 1 else -1
                GameOutcome.BLACK_WINS -> if (!agentIsWhite) 1 else -1
                else -> 0
            }
            when (agentOutcome) {
                1 -> wins++
                -1 -> losses++
                else -> draws++
            }
            if (agentOutcome == 0) {
                val stepLimited = (steps >= maxSteps && !env.isTerminal(state))
                when {
                    trippedLocalThreefold -> drawLocalThreefold++
                    status.contains("STALEMATE") -> drawStalemate++
                    status.contains("REPETITION") -> drawRepetition++
                    status.contains("FIFTY_MOVE_RULE") -> drawFifty++
                    status.contains("INSUFFICIENT_MATERIAL") -> drawInsufficient++
                    stepLimited -> drawStepLimit++
                    else -> drawOther++
                }
                if (dumpDraws && drawSamples.size < dumpLimit) {
                    val reason = when {
                        trippedLocalThreefold -> "LOCAL_THREEFOLD"
                        status.contains("STALEMATE") -> "STALEMATE"
                        status.contains("REPETITION") -> "REPETITION"
                        status.contains("FIFTY_MOVE_RULE") -> "FIFTY_MOVE_RULE"
                        status.contains("INSUFFICIENT_MATERIAL") -> "INSUFFICIENT_MATERIAL"
                        stepLimited -> "STEP_LIMIT"
                        else -> status
                    }
                    val finalFen = env.getCurrentBoard().toFEN()
                    val lastMoves = moveList.takeLast(10).joinToString(",")
                    drawSamples.add(
                        "--- Draw #${drawSamples.size + 1} ---\n" +
                        "Reason: ${reason}\n" +
                        "Steps: ${steps} / max=${maxSteps}\n" +
                        "Start FEN: ${startFen}\n" +
                        (midFen?.let { "Mid FEN: ${it}\n" } ?: "") +
                        "Final FEN: ${finalFen}\n" +
                        (if (lastMoves.isNotEmpty()) "Last moves: ${lastMoves}\n" else "")
                    )
                }
            }
            // Use terminal outcome score from the agent's perspective
            totalOutcomeScore += when (raw) {
                GameOutcome.WHITE_WINS -> if (agentIsWhite) 1.0 else -1.0
                GameOutcome.BLACK_WINS -> if (!agentIsWhite) 1.0 else -1.0
                else -> 0.0
            }
            totalLen += steps
            results.add(
                EvaluationGameSummary(
                    index = idx,
                    moveCount = steps,
                    score = rewardSum,
                    outcome = raw,
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
        // Optionally dump a few draw samples for debugging
        if (drawSamples.isNotEmpty()) {
            drawSamples.forEach { println(it) }
        }
        // Print concise JSON-like summary with draw breakdown (no performance_score)
        println("{" +
            "\"games\":$games," +
            "\"avg_reward\":$avgReward," +
            "\"avg_length\":$avgLen," +
            "\"win_rate\":$winRate," +
            "\"draw_rate\":$drawRate," +
            "\"loss_rate\":$lossRate," +
            "\"draw_details\":{" +
                "\"step_limit\":$drawStepLimit," +
                "\"stalemate\":$drawStalemate," +
                "\"repetition\":$drawRepetition," +
                "\"fifty_move\":$drawFifty," +
                "\"insufficient\":$drawInsufficient," +
                "\"other\":$drawOther," +
                "\"local_threefold\":$drawLocalThreefold" +
            "}" +
        "}")
    }

    /**
     * Head-to-head evaluation: Agent A vs Agent B, alternating colors.
     */
    private fun evalHeadToHead(args: List<String>) {
        val games = args.getAfter("--games")?.toIntOrNull() ?: 20
        val maxSteps = args.getAfter("--max-steps")?.toIntOrNull() ?: 120
        val evalEpsilon = args.getAfter("--eval-epsilon")?.toDoubleOrNull() ?: 0.0
        val enableLocalThreefold = args.contains("--local-threefold")
        val localThreefoldThreshold = args.getAfter("--threefold-threshold")?.toIntOrNull() ?: 3
        val invalidLoss = args.contains("--invalid-loss")
        // Early adjudication flags (optional)
        val enableAdjudication = args.contains("--adjudicate")
        val resignMaterial = args.getAfter("--resign-material")?.toIntOrNull() ?: 9
        val noProgressPlies = args.getAfter("--no-progress-plies")?.toIntOrNull() ?: 40
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
        val actionEncoder = ChessActionEncoder()

        val loadedA = runCatching { agentA.load(pathA) }.isSuccess
        val loadedB = runCatching { agentB.load(pathB) }.isSuccess
        if (!loadedA || !loadedB) {
            println("Warning: failed to load one or both agents. A=$loadedA, B=$loadedB")
        }

        // Outcome-based evaluation (no step penalty, no shaping)
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0, lossReward = -1.0, drawReward = 0.0,
                stepPenalty = 0.0, stepLimitPenalty = -0.5, enablePositionRewards = false, maxGameLength = maxSteps
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
        var drawLocalThreefold = 0
        var totalInvalid = 0
        var invalidLossA = 0

        var aStartsWhite = true
        var dumped = 0
        repeat(games) {
            var state = env.reset()
            var steps = 0
            val moveList = mutableListOf<String>()
            val startFen = env.getCurrentBoard().toFEN()
            var midFen: String? = null
            val fenCounts = mutableMapOf(startFen to 1)
            var trippedLocalThreefold = false
            var forcedOutcomeFromInvalid: Int? = null // 1 = A wins, -1 = A loses
            var adjudicatedOutcomeA: Int? = null // 1 = A wins; -1 = A loses; 0 = draw
            // Track no-progress plies (no capture and no check) via FEN material totals and IN_CHECK
            fun matTotals(fen: String): Pair<Int, Int> {
                var w = 0; var b = 0
                for (ch in fen.takeWhile { it != ' ' }) {
                    when (ch) {
                        'P' -> w += 1; 'p' -> b += 1
                        'N', 'B' -> w += 3; 'n', 'b' -> b += 3
                        'R' -> w += 5; 'r' -> b += 5
                        'Q' -> w += 9; 'q' -> b += 9
                    }
                }
                return w to b
            }
            var lastTotals = matTotals(startFen)
            var noProg = 0
            while (!env.isTerminal(state) && steps < maxSteps) {
                val valid = env.getValidActions(state)
                if (valid.isEmpty()) break
                val active = env.getCurrentBoard().getActiveColor()
                val aTurn = (active.name.contains("WHITE") && aStartsWhite) || (active.name.contains("BLACK") && !aStartsWhite)
                val preSelected = if (aTurn) agentA.selectAction(state, valid) else agentB.selectAction(state, valid)
                var chosen = preSelected
                val presentInValid = chosen in valid
                if (!presentInValid) {
                    // Guard against any buggy selection; fall back to a valid action
                    chosen = valid.first()
                }
                var step = env.step(chosen)
                if (step.info["error"] != null) {
                    totalInvalid++
                    // Diagnostic log for the first few invalids
                    if (totalInvalid <= 12) {
                        val decodedChosen = runCatching { actionEncoder.decodeAction(chosen).toAlgebraic() }.getOrNull()
                        val decodedPre = runCatching { actionEncoder.decodeAction(preSelected).toAlgebraic() }.getOrNull()
                        val fenNow = env.getCurrentBoard().toFEN()
                        val sampleValid = valid.take(5).map { v -> actionEncoder.decodeAction(v).toAlgebraic() }
                        val failure = step.info["error"] ?: "?"
                        println("INVALID sel: aTurn=$aTurn presentInValid=$presentInValid pre=$preSelected($decodedPre) chosen=$chosen($decodedChosen) validCount=${valid.size} sampleValid=${sampleValid}")
                        println("INVALID fail: $failure")
                        println("INVALID fen: ${fenNow}")
                    }
                    // Retry once with a re-fetched valid list to avoid stale set
                    val valid2 = env.getValidActions(state)
                    val fallback = if (valid2.isNotEmpty()) valid2.random() else valid.random()
                    step = env.step(fallback)
                    if (step.info["error"] != null) {
                        totalInvalid++
                        if (invalidLoss) {
                            // Assign loss to the side that failed to produce a valid move twice
                            forcedOutcomeFromInvalid = if (aTurn) -1 else 1
                            if (aTurn) invalidLossA++
                        }
                        // Terminate the game on repeated invalids
                        break
                    }
                }
                step.info["move"]?.let { moveList.add(it.toString()) }
                state = step.nextState
                steps++
                if (midFen == null && steps >= maxSteps / 2) {
                    midFen = env.getCurrentBoard().toFEN()
                }
                if (enableLocalThreefold) {
                    val fen = env.getCurrentBoard().toFEN()
                    val cnt = (fenCounts[fen] ?: 0) + 1
                    fenCounts[fen] = cnt
                    if (cnt >= localThreefoldThreshold) {
                        trippedLocalThreefold = true
                        break
                    }
                }
                if (enableAdjudication && adjudicatedOutcomeA == null) {
                    val fenNow = env.getCurrentBoard().toFEN()
                    val (wNow, bNow) = matTotals(fenNow)
                    val (wPrev, bPrev) = lastTotals
                    val capture = (wNow + bNow) < (wPrev + bPrev)
                    val statusNow = env.getGameStatus()
                    val inCheck = statusNow.name.contains("IN_CHECK")
                    noProg = if (capture || inCheck) 0 else noProg + 1
                    lastTotals = (wNow to bNow)
                    val matDiff = wNow - bNow
                    if (abs(matDiff) >= resignMaterial) {
                        adjudicatedOutcomeA = if (matDiff > 0) {
                            if (aStartsWhite) 1 else -1
                        } else {
                            if (!aStartsWhite) 1 else -1
                        }
                        break
                    }
                    if (noProg >= noProgressPlies) {
                        adjudicatedOutcomeA = 0
                        break
                    }
                }
            }
            totalLen += steps
            val status = env.getGameStatus().name
            val stepLimited = steps >= maxSteps && !env.isTerminal(state)
            val outcomeA = when {
                forcedOutcomeFromInvalid != null -> forcedOutcomeFromInvalid!!
                adjudicatedOutcomeA != null -> adjudicatedOutcomeA!!
                trippedLocalThreefold -> 0
                status.contains("WHITE_WINS") -> if (aStartsWhite) 1 else -1
                status.contains("BLACK_WINS") -> if (!aStartsWhite) 1 else -1
                else -> 0
            }
            when (outcomeA) {
                1 -> winsA++
                -1 -> lossesA++
                else -> {
                    draws++
                    if (trippedLocalThreefold) drawLocalThreefold++
                    else if (stepLimited) drawStepLimit++ else when {
                        status.contains("STALEMATE") -> drawStalemate++
                        status.contains("REPETITION") -> drawRepetition++
                        status.contains("FIFTY_MOVE_RULE") -> drawFifty++
                        status.contains("INSUFFICIENT_MATERIAL") -> drawInsufficient++
                        else -> drawOther++
                    }
                    if (dumpDraws && dumped < dumpLimit) {
                        dumped++
                        println("--- Draw #$dumped ---")
                        println("Reason: ${when { trippedLocalThreefold -> "LOCAL_THREEFOLD"; stepLimited -> "STEP_LIMIT"; else -> status }}")
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
                "\"invalid_loss_A\":$invalidLossA," +
                "\"draw_details\":{" +
                    "\"step_limit\":$drawStepLimit," +
                    "\"stalemate\":$drawStalemate," +
                    "\"repetition\":$drawRepetition," +
                    "\"fifty_move\":$drawFifty," +
                    "\"insufficient\":$drawInsufficient," +
                    "\"other\":$drawOther," +
                    "\"local_threefold\":$drawLocalThreefold" +
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

    // performance_score removed from outputs; report raw rates and lengths only.

    private fun trainAdvanced(args: List<String>) {
        val cycles = args.getAfter("--cycles")?.toIntOrNull() ?: 3
        
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

        // Parse hidden layers from CLI or profile
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

        // Create ChessRLConfig with CLI and profile overrides
        val config = ChessRLConfig(
            // Neural Network Configuration
            hiddenLayers = hiddenFromCli ?: hiddenFromProfile ?: listOf(512, 256, 128),
            learningRate = profile?.get("learningRate")?.toDoubleOrNull() ?: 0.001,
            batchSize = profile?.get("batchSize")?.toIntOrNull() ?: 64,
            
            // RL Training Configuration
            explorationRate = profile?.get("explorationRate")?.toDoubleOrNull() ?: 0.1,
            targetUpdateFrequency = args.getAfter("--target-update")?.toIntOrNull()
                ?: profile?.get("targetUpdateFrequency")?.toIntOrNull() ?: 100,
            maxExperienceBuffer = profile?.get("maxExperienceBuffer")?.toIntOrNull() ?: 50000,
            
            // Self-Play Configuration
            gamesPerCycle = profile?.get("gamesPerCycle")?.toIntOrNull() ?: 20,
            maxConcurrentGames = args.getAfter("--concurrency")?.toIntOrNull()
                ?: profile?.get("maxConcurrentGames")?.toIntOrNull() ?: 4,
            maxStepsPerGame = args.getAfter("--max-steps")?.toIntOrNull()
                ?: profile?.get("maxStepsPerGame")?.toIntOrNull() ?: 80,
            maxCycles = cycles,
            
            // Reward Structure
            winReward = profile?.get("winReward")?.toDoubleOrNull() ?: 1.0,
            lossReward = profile?.get("lossReward")?.toDoubleOrNull() ?: -1.0,
            drawReward = args.getAfter("--draw-reward")?.toDoubleOrNull()
                ?: profile?.get("drawReward")?.toDoubleOrNull() ?: -0.2,
            stepLimitPenalty = profile?.get("stepLimitPenalty")?.toDoubleOrNull() ?: -1.0,
            
            // System Configuration
            seed = args.getAfter("--seed")?.toLongOrNull(),
            checkpointInterval = args.getAfter("--checkpoint-interval")?.toIntOrNull()
                ?: profile?.get("checkpointInterval")?.toIntOrNull() ?: 5,
            checkpointDirectory = args.getAfter("--checkpoint-dir")
                ?: profile?.get("checkpointDirectory") ?: "checkpoints",
            evaluationGames = profile?.get("evaluationGames")?.toIntOrNull() ?: 100
        )

        // Helpful context for locating outputs
        runCatching {
            val cwd = java.nio.file.Paths.get("").toAbsolutePath().normalize().toString()
            println("Working directory: $cwd")
            println("Checkpoints directory (relative): ${config.checkpointDirectory}")
        }

        // Select learning backend
        val backendId = args.getAfter("--backend")?.lowercase() ?: "dqn"
        val backend = createBackend(backendId)
        if (backend.id != backendId) {
            println("Unknown backend '$backendId', falling back to '${backend.id}'.")
        }

        // Create and initialize training pipeline
        val pipeline = TrainingPipeline(config, backend)
        check(pipeline.initialize()) { "Failed to initialize training pipeline" }

        // TODO: Add checkpoint loading support for the new pipeline
        // This would require implementing load/save methods in the new TrainingPipeline
        
        // Run training
        runCatching {
            val results = pipeline.runTraining()
            println("Training completed: cycles=${results.totalCycles}, bestPerf=${results.bestPerformance}")
        }.onFailure { e ->
            println("Training failed: ${e.message}")
            e.printStackTrace()
        }
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
                val dir = args.getAfter("--checkpoint-dir") ?: "checkpoints"
                resolveBestModelPath(dir)?.let { best ->
                    val resolved = resolvePath(best)
                    runCatching { agent.load(resolved) }.onFailure { println("Warning: failed to load best model: ${it.message}") }
                } ?: run {
                    findBestCheckpointPath(dir)?.let { latest ->
                        runCatching { agent.load(resolvePath(latest)) }.onFailure { println("Warning: failed to load latest checkpoint: ${it.message}") }
                    }
                }
            }
        }

        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                stepLimitPenalty = -0.5
            )
        )
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

    private fun createBackend(id: String): LearningBackend {
        return when (id.lowercase()) {
            "dqn", "default" -> DqnLearningBackend()
            else -> DqnLearningBackend()
        }
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
                Options: --local-threefold [--threefold-threshold N] --invalid-loss
                  • --local-threefold: stop local position cycles early during eval
                  • --invalid-loss: repeated invalid moves (twice in a row) lose the game for that side
              --train-advanced --cycles N [--resume-best] [--seed S] [--load PATH]
                Run advanced training for N cycles; optionally resume from best checkpoint.
                Profiles: [--profile NAME] [--profiles PATH]
                  - Overrides defaults via YAML (keys: hiddenLayers, algo, maxStepsPerGame, maxConcurrentGames, enablePositionRewards,
                    explorationRate, explorationWarmupCycles, explorationWarmupRate, rollbackWarmupCycles, enableModelRollback,
                    batchSize, checkpointDirectory, checkpointInterval, autoCleanupOnFinish, keepBest, keepLastN, keepEveryN,
                    stepLimitPenalty, treatStepLimitAsDraw, targetUpdateFrequency, enableDoubleDQN, loadModelPath,
                    enableLocalThreefoldDraw, localThreefoldThreshold, repetitionPenalty, repetitionPenaltyAfter)
                Overrides (flags take precedence over profiles):
                  --max-steps M              Per-game step cap
                  --eps-start E --eps-end E --eps-cycles N
                                         Linear epsilon decay after warmup (optional)
                  --checkpoint-dir DIR       Checkpoint base directory
                  --checkpoint-interval N    Save a regular checkpoint every N cycles
                  --local-threefold          Enable local threefold detection (training)
                  --threefold-threshold N    Repetition threshold for local threefold (default 3)
                  --repetition-penalty X     End-episode penalty when repetition detected (training-only shaping)
                  --repetition-penalty-after N  Start penalizing once a FEN repeats ≥ N times (default 2)
                  --no-cleanup               Disable end-of-run retention cleanup
                  --no-keep-best             Do not force keeping the best checkpoint
                  --keep-last K              Keep last K checkpoints (default 2)
                  --keep-every N             Also keep every N-th checkpoint (sparse history)
                  --backend NAME            Select learning backend (dqn)
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

    /**
     * Resolve canonical best model path.
     * Preference order:
     * 1) <dir>/best_qnet.json
     * 2) path from <dir>/best_checkpoint.txt (supports either _qnet.json or primary path)
     * 3) null (caller should fallback to latest checkpoint)
     */
    private fun resolveBestModelPath(dir: String): String? {
        return try {
            val base = java.nio.file.Path.of(dir)
            // 1) Dedicated best model
            val bestQnet = base.resolve("best_qnet.json")
            if (java.nio.file.Files.exists(bestQnet)) return bestQnet.toString()
            // 2) Pointer file
            val pointer = base.resolve("best_checkpoint.txt")
            if (java.nio.file.Files.exists(pointer)) {
                val lines = java.nio.file.Files.readAllLines(pointer)
                // Try explicit path= first
                val fromKey = lines.firstOrNull { it.startsWith("path=") }?.substringAfter("path=")?.trim()
                val parsed = fromKey ?: lines.firstOrNull { it.isNotBlank() }?.trim()
                if (!parsed.isNullOrBlank()) {
                    val p = java.nio.file.Path.of(parsed)
                    if (java.nio.file.Files.exists(p)) {
                        // Prefer dedicated model artifact sibling if possible
                        val qnet = parsed.replace(".json.gz", "_qnet.json").replace(".json", "_qnet.json")
                        val qnetPath = java.nio.file.Path.of(qnet)
                        return if (java.nio.file.Files.exists(qnetPath)) qnetPath.toString() else p.toString()
                    }
                }
            }
            null
        } catch (e: Exception) {
            println("Warning: failed to resolve canonical best model in $dir: ${e.message}")
            null
        }
    }
}
