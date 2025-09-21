package com.chessrl.integration

import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.config.ConfigParser
import com.chessrl.rl.Experience
// Remove kotlinx.serialization for now - use simple JSON manually
import java.io.File
import kotlin.system.exitProcess

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
            stepLimitPenalty = argsMap["--step-limit-penalty"]?.toDouble() ?: -1.0
        )
    }
    
    private fun runSelfPlayGame(params: WorkerParams): WorkerGameResult {
        val startTime = System.currentTimeMillis()
        
        // Initialize seed if provided
        params.seed?.let { seed ->
            SeedManager.initializeWithSeed(seed + params.gameId) // Unique seed per game
        }
        
        // Create simple configuration
        val config = ChessRLConfig(
            maxStepsPerGame = params.maxSteps,
            winReward = params.winReward,
            lossReward = params.lossReward,
            drawReward = params.drawReward,
            stepLimitPenalty = params.stepLimitPenalty
        )
        
        // Create agents by loading the model
        val mainAgent = loadAgent(params.modelPath, config)
        val opponentAgent = loadAgent(params.modelPath, config) // Same model for self-play
        
        // Create environment
        val environment = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = config.winReward,
                lossReward = config.lossReward,
                drawReward = config.drawReward,
                stepLimitPenalty = config.stepLimitPenalty,
                enablePositionRewards = false,
                gameLengthNormalization = false,
                enableEarlyAdjudication = EnvironmentDefaults.ENABLE_EARLY_ADJUDICATION,
                resignMaterialThreshold = EnvironmentDefaults.RESIGN_MATERIAL_THRESHOLD,
                noProgressPlies = EnvironmentDefaults.NO_PROGRESS_PLIES
            )
        )
        
        // Run the game
        val experiences = mutableListOf<Experience<DoubleArray, Int>>()
        var state = environment.reset()
        var stepCount = 0
        
        while (!environment.isTerminal(state) && stepCount < config.maxStepsPerGame) {
            // Determine current player (white on even steps, black on odd)
            val currentAgent = if (stepCount % 2 == 0) mainAgent else opponentAgent
            
            // Get valid actions
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) break
            
            // Agent selects action (no synchronization needed - single process)
            val action = currentAgent.selectAction(state, validActions)
            
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
        val gameOutcome = determineGameOutcome(environment, stepCount >= config.maxStepsPerGame)
        val terminationReason = if (stepCount >= config.maxStepsPerGame) {
            EpisodeTerminationReason.STEP_LIMIT
        } else {
            EpisodeTerminationReason.GAME_ENDED
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
    
    private fun loadAgent(modelPath: String, config: ChessRLConfig): ChessAgent {
        // Create agent configuration
        val agentConfig = ChessAgentConfig(
            batchSize = config.batchSize,
            maxBufferSize = config.maxExperienceBuffer,
            targetUpdateFrequency = config.targetUpdateFrequency
        )
        
        // Create agent and load model
        val agent = ChessAgentFactory.createSeededDQNAgent(
            hiddenLayers = config.hiddenLayers,
            learningRate = config.learningRate,
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
            val gameStatus = environment.getGameStatus()
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
    val stepLimitPenalty: Double = -1.0
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
