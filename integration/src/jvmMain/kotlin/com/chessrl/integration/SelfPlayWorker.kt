package com.chessrl.integration

import com.chessrl.integration.adapter.ChessEngineFactory
import com.chessrl.integration.adapter.EngineBackend
import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.config.ConfigParser
import com.chessrl.rl.Experience
// Remove kotlinx.serialization for now - use simple JSON manually
import java.io.File
import kotlin.system.exitProcess
import com.chessrl.teacher.MinimaxTeacher
import com.chessrl.integration.opponent.OpponentKind
import com.chessrl.integration.opponent.OpponentSelector
import kotlin.random.Random

/**
 * Self-play worker that runs as a separate process.
 * Loads a model, plays a single game, and writes the result to a file.
 */
object SelfPlayWorker {
    
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val params = parseArgs(args)
            val result = runSelfPlayGame(params)
            saveResult(result, params.outputFile)
            exitProcess(0)
        } catch (e: Exception) {
            // Use simple error output since structured logging may not be initialized
            System.err.println("Self-play worker failed: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
    }
    
    private fun parseArgs(args: Array<String>): WorkerParams {
        val argsMap = args.toList().windowed(2, 2)
            .filter { it.size == 2 && it[0].startsWith("--") }
            .associate { it[0] to it[1] }
        
        return WorkerParams(
            modelPath = argsMap["--model"] ?: throw IllegalArgumentException("--model required"),
            gameId = argsMap["--game-id"]?.toInt() ?: throw IllegalArgumentException("--game-id required"),
            outputFile = argsMap["--output"] ?: throw IllegalArgumentException("--output required"),
            configPath = argsMap["--config"],
            seed = argsMap["--seed"]?.toLong(),
            maxSteps = argsMap["--max-steps"]?.toInt() ?: 80,
            winReward = argsMap["--win-reward"]?.toDouble() ?: 1.0,
            lossReward = argsMap["--loss-reward"]?.toDouble() ?: -1.0,
            drawReward = argsMap["--draw-reward"]?.toDouble() ?: -0.2,
            stepLimitPenalty = argsMap["--step-limit-penalty"]?.toDouble() ?: -1.0,
            hiddenLayers = argsMap["--hidden-layers"]?.let { parseHiddenLayers(it) },
            opponent = argsMap["--opponent"],
            opponentDepth = argsMap["--opponent-depth"]?.toInt() ?: 2,
            trainEarlyAdjudication = argsMap["--train-early-adjudication"]?.equals("true", true)
                ?: EnvironmentDefaults.ENABLE_EARLY_ADJUDICATION,
            trainResignMaterialThreshold = argsMap["--train-resign-threshold"]?.toInt()
                ?: EnvironmentDefaults.RESIGN_MATERIAL_THRESHOLD,
            trainNoProgressPlies = argsMap["--train-no-progress-plies"]?.toInt()
                ?: EnvironmentDefaults.NO_PROGRESS_PLIES,
            engine = argsMap["--engine"]?.let { EngineBackend.fromString(it) } ?: EngineBackend.BUILTIN,
            nnBackend = argsMap["--nn-backend"]?.let { BackendType.fromString(it) } ?: BackendType.getDefault()
        )
    }
    
    private fun runSelfPlayGame(params: WorkerParams): WorkerGameResult {
        val startTime = System.currentTimeMillis()
        
        // Initialize SeedManager in the worker process
        // Use provided seed (offset by gameId) for deterministic runs, otherwise use a random seed
        if (params.seed != null) {
            SeedManager.initializeWithSeed(params.seed + params.gameId) // Unique seed per game
        } else {
            SeedManager.initializeWithRandomSeed()
        }
        
        // Create simple configuration
        val config = ChessRLConfig(
            maxStepsPerGame = params.maxSteps,
            winReward = params.winReward,
            lossReward = params.lossReward,
            drawReward = params.drawReward,
            stepLimitPenalty = params.stepLimitPenalty,
            hiddenLayers = params.hiddenLayers ?: ChessRLConfig().hiddenLayers,
            engine = params.engine
        )
        
        // Create agents by loading the model
        val mainAgent = loadAgent(params.modelPath, config, params.nnBackend)
        val opponentAgent = loadAgent(params.modelPath, config, params.nnBackend) // Same model for self-play
        
        // Create environment
        val environment = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = config.winReward,
                lossReward = config.lossReward,
                drawReward = config.drawReward,
                stepLimitPenalty = config.stepLimitPenalty,
                enablePositionRewards = false,
                gameLengthNormalization = false,
                enableEarlyAdjudication = params.trainEarlyAdjudication,
                resignMaterialThreshold = params.trainResignMaterialThreshold,
                noProgressPlies = params.trainNoProgressPlies
            ),
            adapter = ChessEngineFactory.create(params.engine)
        )
        
        // Run the game
        val experiences = mutableListOf<Experience<DoubleArray, Int>>()
        var state = environment.reset()
        var stepCount = 0
        var stalledNoEncodableMoves = false
        val selectionRandom = runCatching { SeedManager.getInstance().createSeededRandom("workerOpponentSelection-${params.gameId}") }
            .getOrNull() ?: Random.Default
        val opponentSelection = OpponentSelector.select(params.opponent, params.opponentDepth, selectionRandom)
        val minimaxTeacher = if (opponentSelection.kind == OpponentKind.MINIMAX) {
            val depth = opponentSelection.minimaxDepth ?: params.opponentDepth
            val teacherDepth = if (depth <= 0) 2 else depth
            val rnd = runCatching { SeedManager.getInstance().createSeededRandom("minimaxWorker-${params.gameId}") }.getOrNull()
            MinimaxTeacher(depth = teacherDepth, random = rnd ?: Random.Default)
        } else null
        val actionEncoder = ChessActionEncoder()
        
        while (!environment.isTerminal(state) && stepCount < config.maxStepsPerGame) {
            // Determine current player (white on even steps, black on odd)
            val currentAgent = if (stepCount % 2 == 0) mainAgent else opponentAgent
            
            // Get valid actions
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) { stalledNoEncodableMoves = true; break }
            
            // Agent selects action (no synchronization needed - single process)
            val action = if (currentAgent === opponentAgent && opponentSelection.kind != OpponentKind.SELF) {
                when (opponentSelection.kind) {
                    OpponentKind.MINIMAX -> {
                        val move = minimaxTeacher!!.act(environment.getCurrentBoard()).bestMove
                        val idx = actionEncoder.encodeMove(move)
                        if (idx in validActions) idx else {
                            val fallback = validActions.firstOrNull { ai ->
                                val m2 = actionEncoder.decodeAction(ai)
                                m2.from == move.from && m2.to == move.to
                            }
                            fallback ?: validActions.first()
                        }
                    }
                    OpponentKind.HEURISTIC -> {
                        val idx = BaselineHeuristicOpponent.selectAction(environment, validActions)
                        if (idx >= 0) idx else validActions.first()
                    }
                    else -> validActions.first()
                }
            } else {
                currentAgent.selectAction(state, validActions)
            }
            
            // Take step in environment
            val stepResult = environment.step(action)
            
            // Create experience
            val experience = Experience(
                state = state,
                action = action,
                reward = stepResult.reward,
                nextState = stepResult.nextState,
                done = stepResult.done
            )
            
            experiences.add(experience)
            
            // Update state
            state = stepResult.nextState
            stepCount++
            
            if (stepResult.done) break
        }
        
        // Apply step limit penalty if game hit the limit
        if (stepCount >= config.maxStepsPerGame && !environment.isTerminal(state) && experiences.isNotEmpty()) {
            val lastExperience = experiences.last()
            val penalizedExperience = lastExperience.copy(
                reward = lastExperience.reward + config.stepLimitPenalty,
                done = true
            )
            experiences[experiences.lastIndex] = penalizedExperience
        }
        
        val endTime = System.currentTimeMillis()
        val gameDuration = endTime - startTime
        
        // Determine game outcome
        var gameOutcome = determineGameOutcome(environment, stepCount >= config.maxStepsPerGame)
        var terminationReason = if (stepCount >= config.maxStepsPerGame) {
            EpisodeTerminationReason.STEP_LIMIT
        } else {
            EpisodeTerminationReason.GAME_ENDED
        }
        if (stalledNoEncodableMoves && gameOutcome == GameOutcome.ONGOING) {
            gameOutcome = GameOutcome.DRAW
            terminationReason = EpisodeTerminationReason.MANUAL
            if (experiences.isNotEmpty()) {
                val last = experiences.last()
                experiences[experiences.lastIndex] = last.copy(reward = last.reward + config.stepLimitPenalty, done = true)
            }
        }
        
        return WorkerGameResult(
            gameId = params.gameId,
            gameLength = stepCount,
            gameOutcome = gameOutcome,
            terminationReason = terminationReason,
            gameDuration = gameDuration,
            experiences = experiences,
            finalPosition = environment.getCurrentBoard().toFEN(),
            success = true,
            errorMessage = null
        )
    }
    
    private fun loadAgent(modelPath: String, config: ChessRLConfig, backendType: BackendType): ChessAgent {
        // Create agent configuration
        val agentConfig = ChessAgentConfig(
            batchSize = config.batchSize,
            maxBufferSize = config.maxExperienceBuffer,
            targetUpdateFrequency = config.targetUpdateFrequency
        )
        
        // Create agent and load model
        val agent = ChessAgentFactory.createSeededDQNAgent(
            backendType = backendType,
            hiddenLayers = config.hiddenLayers,
            learningRate = config.learningRate,
            optimizer = config.optimizer,
            explorationRate = config.explorationRate,
            config = agentConfig,
            replayType = config.replayType,
            gamma = config.gamma
        )
        
        // Load model weights if file exists
        if (File(modelPath).exists()) {
            agent.load(modelPath)
        }
        
        return agent
    }
    
    private fun determineGameOutcome(environment: ChessEnvironment, hitStepLimit: Boolean): GameOutcome {
        return if (hitStepLimit) {
            GameOutcome.DRAW // Treat step limit as draw
        } else {
            // Use effective status to include early adjudication outcomes
            val gameStatus = environment.getEffectiveGameStatus()
            when {
                gameStatus.name.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
                gameStatus.name.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
                gameStatus.name.contains("DRAW") -> GameOutcome.DRAW
                else -> GameOutcome.ONGOING
            }
        }
    }
    
    private fun saveResult(result: WorkerGameResult, outputFile: String) {
        // Write summary JSON
        val summary = buildString {
            appendLine("{")
            appendLine("  \"gameId\": ${result.gameId},")
            appendLine("  \"gameLength\": ${result.gameLength},")
            appendLine("  \"gameOutcome\": \"${result.gameOutcome}\",")
            appendLine("  \"terminationReason\": \"${result.terminationReason}\",")
            appendLine("  \"gameDuration\": ${result.gameDuration},")
            appendLine("  \"experienceCount\": ${result.experiences.size},")
            appendLine("  \"finalPosition\": \"${result.finalPosition}\",")
            appendLine("  \"success\": ${result.success}")
            if (result.errorMessage != null) {
                appendLine(",\n  \"errorMessage\": \"${result.errorMessage}\"")
            }
            appendLine("}")
        }
        File(outputFile).writeText(summary)

        // Write experiences as NDJSON (one JSON object per line)
        val ndjsonPath = "$outputFile.ndjson"
        File(ndjsonPath).bufferedWriter().use { out ->
            for (exp in result.experiences) {
                val s = exp.state.joinToString(prefix = "[", postfix = "]") { d -> d.toString() }
                val ns = exp.nextState.joinToString(prefix = "[", postfix = "]") { d -> d.toString() }
                val line = "{" +
                        "\"s\":$s," +
                        "\"a\":${exp.action}," +
                        "\"r\":${exp.reward}," +
                        "\"ns\":$ns," +
                        "\"d\":${exp.done}" +
                        "}"
                out.appendLine(line)
            }
        }
    }
}

/**
 * Parameters for the self-play worker process
 */
private data class WorkerParams(
    val modelPath: String,
    val gameId: Int,
    val outputFile: String,
    val configPath: String? = null,
    val seed: Long? = null,
    val maxSteps: Int = 80,
    val winReward: Double = 1.0,
    val lossReward: Double = -1.0,
    val drawReward: Double = -0.2,
    val stepLimitPenalty: Double = -1.0,
    val hiddenLayers: List<Int>? = null,
    val opponent: String? = null,
    val opponentDepth: Int = 2,
    val trainEarlyAdjudication: Boolean = EnvironmentDefaults.ENABLE_EARLY_ADJUDICATION,
    val trainResignMaterialThreshold: Int = EnvironmentDefaults.RESIGN_MATERIAL_THRESHOLD,
    val trainNoProgressPlies: Int = EnvironmentDefaults.NO_PROGRESS_PLIES,
    val engine: EngineBackend = EngineBackend.BUILTIN,
    val nnBackend: BackendType = BackendType.getDefault()
)

/**
 * Simple game result for inter-process communication
 */
data class WorkerGameResult(
    val gameId: Int,
    val gameLength: Int,
    val gameOutcome: GameOutcome,
    val terminationReason: EpisodeTerminationReason,
    val gameDuration: Long,
    val experiences: List<Experience<DoubleArray, Int>>, // Keep original format for now
    val finalPosition: String,
    val success: Boolean,
    val errorMessage: String?
)

// Removed serialization extensions for now

// Local helper to parse hidden layers from CLI string (e.g., "768,512,256" or "[768,512,256]")
private fun parseHiddenLayers(value: String): List<Int> {
    val clean = value.trim()
    return when {
        clean.startsWith("[") && clean.endsWith("]") -> clean.removeSurrounding("[", "]").split(',').map { it.trim().toInt() }
        "," in clean -> clean.split(',').map { it.trim().toInt() }
        clean.contains(" ") -> clean.split("\\s+".toRegex()).map { it.trim().toInt() }
        clean.isNotEmpty() -> listOf(clean.toInt())
        else -> emptyList()
    }
}
