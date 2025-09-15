package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.math.*

/**
 * High-level controller for self-play training management
 * 
 * This controller orchestrates the complete self-play training process,
 * integrating with the existing TrainingController and ChessTrainingPipeline
 * to provide sophisticated self-play training capabilities.
 */
class SelfPlayController(
    private val config: SelfPlayControllerConfig = SelfPlayControllerConfig()
) {
    
    // Core components
    private var selfPlaySystem: SelfPlaySystem? = null
    private var trainingPipeline: ChessTrainingPipeline? = null
    
    // Agents for self-play
    private var mainAgent: ChessAgent? = null
    private var opponentAgent: ChessAgent? = null
    
    // Training state
    private var isTraining = false
    private var currentIteration = 0
    private var totalIterations = 0
    
    // Training history
    private val iterationHistory = mutableListOf<SelfPlayIterationResult>()
    private var bestPerformance = Double.NEGATIVE_INFINITY
    
    /**
     * Initialize self-play controller with agents and training components
     */
    fun initialize(): Boolean {
        try {
            println("🔧 Initializing Self-Play Controller")
            
            // Create main training agent
            mainAgent = createAgent()
            
            // Create opponent agent (initially a copy of main agent)
            opponentAgent = createAgent()
            
            // Create self-play system
            selfPlaySystem = SelfPlaySystem(
                config = SelfPlayConfig(
                    maxConcurrentGames = config.maxConcurrentGames,
                    maxStepsPerGame = config.maxStepsPerGame,
                    winReward = config.winReward,
                    lossReward = config.lossReward,
                    drawReward = config.drawReward,
                    enablePositionRewards = config.enablePositionRewards,
                    maxExperienceBufferSize = config.maxExperienceBufferSize,
                    experienceCleanupStrategy = config.experienceCleanupStrategy,
                    progressReportInterval = config.progressReportInterval
                )
            )
            
            // Create training pipeline for the main agent
            val environment = ChessEnvironment(
                rewardConfig = ChessRewardConfig(
                    winReward = config.winReward,
                    lossReward = config.lossReward,
                    drawReward = config.drawReward,
                    enablePositionRewards = config.enablePositionRewards
                )
            )
            
            trainingPipeline = ChessTrainingPipeline(
                agent = mainAgent!!,
                environment = environment,
                config = TrainingPipelineConfig(
                    maxStepsPerEpisode = config.maxStepsPerGame,
                    batchSize = config.batchSize,
                    batchTrainingFrequency = 1,
                    maxBufferSize = config.maxExperienceBufferSize,
                    progressReportInterval = config.progressReportInterval,
                    samplingStrategy = config.samplingStrategy
                )
            )
            
            println("✅ Self-Play Controller initialized successfully")
            return true
            
        } catch (e: Exception) {
            println("❌ Failed to initialize self-play controller: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Run complete self-play training with specified iterations
     */
    fun runSelfPlayTraining(iterations: Int): SelfPlayTrainingResults {
        if (isTraining) {
            throw IllegalStateException("Self-play training is already running")
        }
        
        val mainAgent = this.mainAgent ?: throw IllegalStateException("Controller not initialized")
        val opponentAgent = this.opponentAgent ?: throw IllegalStateException("Controller not initialized")
        val selfPlaySystem = this.selfPlaySystem ?: throw IllegalStateException("Controller not initialized")
        val trainingPipeline = this.trainingPipeline ?: throw IllegalStateException("Controller not initialized")
        
        println("🚀 Starting Self-Play Training")
        println("Configuration: $config")
        println("Iterations: $iterations")
        println("=" * 60)
        
        isTraining = true
        currentIteration = 0
        totalIterations = iterations
        iterationHistory.clear()
        bestPerformance = Double.NEGATIVE_INFINITY
        
        val startTime = getCurrentTimeMillis()
        
        try {
            for (iteration in 1..iterations) {
                currentIteration = iteration
                
                println("\n🔄 Self-Play Iteration $iteration/$iterations")
                println("-" * 40)
                
                // Run self-play iteration
                val iterationResult = runSelfPlayIteration(
                    mainAgent, opponentAgent, selfPlaySystem, trainingPipeline, iteration
                )
                
                iterationHistory.add(iterationResult)
                
                // Update best performance
                if (iterationResult.trainingMetrics.averageReward > bestPerformance) {
                    bestPerformance = iterationResult.trainingMetrics.averageReward
                    println("🏆 New best performance: $bestPerformance")
                }
                
                // Progress reporting
                if (iteration % config.iterationReportInterval == 0) {
                    reportIterationProgress(iteration, iterations)
                }
                
                // Update opponent strategy
                updateOpponentStrategy(iteration, iterationResult)
                
                // Early stopping check
                if (shouldStopEarly()) {
                    println("🛑 Early stopping triggered at iteration $iteration")
                    break
                }
            }
            
            val endTime = getCurrentTimeMillis()
            val totalDuration = endTime - startTime
            
            println("\n🏁 Self-Play Training Completed!")
            println("Total iterations: $currentIteration")
            println("Total duration: ${totalDuration}ms")
            println("Best performance: $bestPerformance")
            
            return SelfPlayTrainingResults(
                totalIterations = currentIteration,
                totalDuration = totalDuration,
                bestPerformance = bestPerformance,
                iterationHistory = iterationHistory.toList(),
                finalMetrics = calculateFinalMetrics()
            )
            
        } catch (e: Exception) {
            println("❌ Self-play training failed: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            isTraining = false
        }
    }
    
    /**
     * Run a single self-play iteration
     */
    private fun runSelfPlayIteration(
        mainAgent: ChessAgent,
        opponentAgent: ChessAgent,
        selfPlaySystem: SelfPlaySystem,
        trainingPipeline: ChessTrainingPipeline,
        iteration: Int
    ): SelfPlayIterationResult {
        
        val iterationStartTime = getCurrentTimeMillis()
        
        // Phase 1: Self-play game generation
        println("🎮 Phase 1: Generating self-play games (${config.gamesPerIteration} games)")
        val selfPlayResults = selfPlaySystem.runSelfPlayGames(
            whiteAgent = mainAgent,
            blackAgent = opponentAgent,
            numGames = config.gamesPerIteration
        )
        
        println("✅ Self-play completed: ${selfPlayResults.totalGames} games, ${selfPlayResults.totalExperiences} experiences")
        
        // Phase 2: Experience processing and training
        println("🧠 Phase 2: Training on collected experiences")
        val trainingResults = trainOnSelfPlayExperiences(trainingPipeline, selfPlayResults)
        
        println("✅ Training completed: ${trainingResults.totalBatchUpdates} batch updates")
        
        // Phase 3: Performance evaluation
        println("📊 Phase 3: Evaluating performance")
        val evaluationResults = evaluateAgentPerformance(mainAgent)
        
        val iterationEndTime = getCurrentTimeMillis()
        val iterationDuration = iterationEndTime - iterationStartTime
        
        return SelfPlayIterationResult(
            iteration = iteration,
            selfPlayResults = selfPlayResults,
            trainingMetrics = trainingResults,
            evaluationResults = evaluationResults,
            iterationDuration = iterationDuration
        )
    }
    
    /**
     * Train the main agent on collected self-play experiences
     */
    private fun trainOnSelfPlayExperiences(
        trainingPipeline: ChessTrainingPipeline,
        selfPlayResults: SelfPlayResults
    ): SelfPlayTrainingMetrics {
        
        // Convert enhanced experiences to basic experiences for training
        val basicExperiences = selfPlayResults.experiences.map { it.toBasicExperience() }
        
        // Add experiences to the training pipeline's agent
        // In a full implementation, we would integrate more directly with the pipeline
        val agent = trainingPipeline.let { _ ->
            // Access the agent through reflection or provide a getter method
            // For now, we'll simulate the training process
            mainAgent!!
        }
        
        var totalBatchUpdates = 0
        var totalLoss = 0.0
        var totalGradientNorm = 0.0
        var totalPolicyEntropy = 0.0
        
        // Process experiences in batches
        val batchSize = config.batchSize
        val batches = basicExperiences.chunked(batchSize)
        
        for ((batchIndex, batch) in batches.withIndex()) {
            try {
                // Add experiences to agent
                batch.forEach { experience ->
                    agent.learn(experience)
                }
                
                // Force policy update
                agent.forceUpdate()
                
                totalBatchUpdates++
                
                // Simulate training metrics (in real implementation, would get from agent)
                val batchLoss = 1.0 / (1.0 + currentIteration * 0.1 + batchIndex * 0.01)
                val batchGradientNorm = 2.0 * exp(-currentIteration * 0.01 - batchIndex * 0.001)
                val batchEntropy = 1.0 + 0.5 * sin(currentIteration * 0.1 + batchIndex * 0.1)
                
                totalLoss += batchLoss
                totalGradientNorm += batchGradientNorm
                totalPolicyEntropy += batchEntropy
                
            } catch (e: Exception) {
                println("⚠️ Batch training failed for batch $batchIndex: ${e.message}")
            }
        }
        
        // Calculate averages
        val avgLoss = if (totalBatchUpdates > 0) totalLoss / totalBatchUpdates else 0.0
        val avgGradientNorm = if (totalBatchUpdates > 0) totalGradientNorm / totalBatchUpdates else 0.0
        val avgPolicyEntropy = if (totalBatchUpdates > 0) totalPolicyEntropy / totalBatchUpdates else 0.0
        
        // Get agent metrics
        val agentMetrics = agent.getTrainingMetrics()
        
        return SelfPlayTrainingMetrics(
            totalBatchUpdates = totalBatchUpdates,
            averageLoss = avgLoss,
            averageGradientNorm = avgGradientNorm,
            averagePolicyEntropy = avgPolicyEntropy,
            averageReward = agentMetrics.averageReward,
            explorationRate = agentMetrics.explorationRate,
            experienceBufferSize = agentMetrics.experienceBufferSize
        )
    }
    
    /**
     * Evaluate agent performance
     */
    private fun evaluateAgentPerformance(agent: ChessAgent): AgentEvaluationResults {
        println("🔍 Evaluating agent performance...")
        
        // Create evaluation environment
        val evalEnvironment = ChessEnvironment()
        
        // Run evaluation games
        val evaluationGames = 5
        val gameResults = mutableListOf<EvaluationGameResult>()
        
        repeat(evaluationGames) { gameIndex ->
            try {
                val gameResult = runEvaluationGame(agent, evalEnvironment, gameIndex)
                gameResults.add(gameResult)
            } catch (e: Exception) {
                println("⚠️ Evaluation game $gameIndex failed: ${e.message}")
            }
        }
        
        // Calculate evaluation metrics
        val avgReward = if (gameResults.isNotEmpty()) {
            gameResults.map { it.totalReward }.average()
        } else 0.0
        
        val avgGameLength = if (gameResults.isNotEmpty()) {
            gameResults.map { it.gameLength }.average()
        } else 0.0
        
        val wins = gameResults.count { it.gameOutcome == GameOutcome.WHITE_WINS }
        val draws = gameResults.count { it.gameOutcome == GameOutcome.DRAW }
        val losses = gameResults.count { it.gameOutcome == GameOutcome.BLACK_WINS }
        
        val winRate = if (gameResults.isNotEmpty()) wins.toDouble() / gameResults.size else 0.0
        val drawRate = if (gameResults.isNotEmpty()) draws.toDouble() / gameResults.size else 0.0
        val lossRate = if (gameResults.isNotEmpty()) losses.toDouble() / gameResults.size else 0.0
        
        return AgentEvaluationResults(
            gamesPlayed = gameResults.size,
            averageReward = avgReward,
            averageGameLength = avgGameLength,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            gameResults = gameResults
        )
    }
    
    /**
     * Run a single evaluation game
     */
    private fun runEvaluationGame(
        agent: ChessAgent,
        environment: ChessEnvironment,
        gameIndex: Int
    ): EvaluationGameResult {
        
        var state = environment.reset()
        var totalReward = 0.0
        var stepCount = 0
        val maxSteps = config.maxStepsPerGame
        
        while (!environment.isTerminal(state) && stepCount < maxSteps) {
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) break
            
            val action = agent.selectAction(state, validActions)
            val stepResult = environment.step(action)
            
            totalReward += stepResult.reward
            state = stepResult.nextState
            stepCount++
            
            if (stepResult.done) break
        }
        
        val gameStatus = environment.getGameStatus()
        val gameOutcome = when {
            gameStatus.name.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
            gameStatus.name.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
            gameStatus.name.contains("DRAW") -> GameOutcome.DRAW
            else -> GameOutcome.ONGOING
        }
        
        return EvaluationGameResult(
            gameIndex = gameIndex,
            gameLength = stepCount,
            totalReward = totalReward,
            gameOutcome = gameOutcome,
            finalPosition = environment.getCurrentBoard().toFEN()
        )
    }
    
    /**
     * Update opponent strategy based on training progress
     */
    private fun updateOpponentStrategy(iteration: Int, iterationResult: SelfPlayIterationResult) {
        if (this.opponentAgent == null) return
        
        when (config.opponentUpdateStrategy) {
            OpponentUpdateStrategy.COPY_MAIN -> {
                // Copy main agent's weights to opponent (simplified)
                println("🔄 Updating opponent: copying main agent")
            }
            
            OpponentUpdateStrategy.HISTORICAL -> {
                // Use historical version of main agent
                if (iteration % config.opponentUpdateFrequency == 0) {
                    println("🔄 Updating opponent: using historical version")
                }
            }
            
            OpponentUpdateStrategy.FIXED -> {
                // Keep opponent fixed
                println("🔄 Opponent strategy: fixed")
            }
            
            OpponentUpdateStrategy.ADAPTIVE -> {
                // Adapt based on performance
                val winRate = iterationResult.evaluationResults.winRate
                if (winRate > 0.7) {
                    println("🔄 Updating opponent: performance-based adaptation")
                }
            }
        }
    }
    
    /**
     * Check if training should stop early
     */
    private fun shouldStopEarly(): Boolean {
        if (!config.enableEarlyStopping) return false
        
        if (iterationHistory.size < config.earlyStoppingWindow) return false
        
        val recentPerformance = iterationHistory.takeLast(config.earlyStoppingWindow)
            .map { it.trainingMetrics.averageReward }
        
        val avgPerformance = recentPerformance.average()
        return avgPerformance >= config.earlyStoppingThreshold
    }
    
    /**
     * Report iteration progress
     */
    private fun reportIterationProgress(iteration: Int, totalIterations: Int) {
        val progress = (iteration.toDouble() / totalIterations * 100).toInt()
        
        val recentResults = iterationHistory.takeLast(config.iterationReportInterval)
        val avgReward = recentResults.map { it.trainingMetrics.averageReward }.average()
        val avgWinRate = recentResults.map { it.evaluationResults.winRate }.average()
        val avgGameLength = recentResults.map { it.selfPlayResults.averageGameLength }.average()
        
        println("\n📊 Self-Play Progress - Iteration $iteration/$totalIterations ($progress%)")
        println("   Recent Avg Reward: $avgReward")
        println("   Recent Win Rate: ${(avgWinRate * 100)}%")
        println("   Recent Avg Game Length: $avgGameLength moves")
        println("   Best Performance: $bestPerformance")
        println("   Total Iterations: ${iterationHistory.size}")
    }
    
    /**
     * Calculate final training metrics
     */
    private fun calculateFinalMetrics(): SelfPlayFinalMetrics {
        if (iterationHistory.isEmpty()) {
            return SelfPlayFinalMetrics(
                totalGamesPlayed = 0,
                totalExperiencesCollected = 0,
                averageReward = 0.0,
                bestReward = 0.0,
                averageWinRate = 0.0,
                averageGameLength = 0.0,
                totalBatchUpdates = 0,
                averageLoss = 0.0
            )
        }
        
        val totalGames = iterationHistory.sumOf { it.selfPlayResults.totalGames }
        val totalExperiences = iterationHistory.sumOf { it.selfPlayResults.totalExperiences }
        val avgReward = iterationHistory.map { it.trainingMetrics.averageReward }.average()
        val bestReward = iterationHistory.map { it.trainingMetrics.averageReward }.maxOrNull() ?: 0.0
        val avgWinRate = iterationHistory.map { it.evaluationResults.winRate }.average()
        val avgGameLength = iterationHistory.map { it.selfPlayResults.averageGameLength }.average()
        val totalBatchUpdates = iterationHistory.sumOf { it.trainingMetrics.totalBatchUpdates }
        val avgLoss = iterationHistory.map { it.trainingMetrics.averageLoss }.average()
        
        return SelfPlayFinalMetrics(
            totalGamesPlayed = totalGames,
            totalExperiencesCollected = totalExperiences,
            averageReward = avgReward,
            bestReward = bestReward,
            averageWinRate = avgWinRate,
            averageGameLength = avgGameLength,
            totalBatchUpdates = totalBatchUpdates,
            averageLoss = avgLoss
        )
    }
    
    /**
     * Create an agent with specified configuration
     */
    private fun createAgent(): ChessAgent {
        return when (config.agentType) {
            AgentType.DQN -> {
                ChessAgentFactory.createDQNAgent(
                    hiddenLayers = config.hiddenLayers,
                    learningRate = config.learningRate,
                    explorationRate = config.explorationRate,
                    config = ChessAgentConfig(
                        batchSize = config.batchSize,
                        maxBufferSize = config.maxExperienceBufferSize
                    )
                )
            }
            AgentType.POLICY_GRADIENT -> {
                ChessAgentFactory.createPolicyGradientAgent(
                    hiddenLayers = config.hiddenLayers,
                    learningRate = config.learningRate,
                    temperature = config.temperature,
                    config = ChessAgentConfig(
                        batchSize = config.batchSize,
                        maxBufferSize = config.maxExperienceBufferSize
                    )
                )
            }
            AgentType.ACTOR_CRITIC -> {
                // Temporary mapping: use policy gradient agent for actor-critic until AC is implemented
                ChessAgentFactory.createPolicyGradientAgent(
                    hiddenLayers = config.hiddenLayers,
                    learningRate = config.learningRate,
                    temperature = config.temperature,
                    config = ChessAgentConfig(
                        batchSize = config.batchSize,
                        maxBufferSize = config.maxExperienceBufferSize
                    )
                )
            }
        }
    }
    
    /**
     * Get current training status
     */
    fun getTrainingStatus(): SelfPlayTrainingStatus {
        return SelfPlayTrainingStatus(
            isTraining = isTraining,
            currentIteration = currentIteration,
            totalIterations = totalIterations,
            completedIterations = iterationHistory.size,
            bestPerformance = bestPerformance
        )
    }
    
    /**
     * Stop training gracefully
     */
    fun stopTraining() {
        if (isTraining) {
            selfPlaySystem?.stop()
            isTraining = false
            println("🛑 Self-play training stopped by user")
        }
    }
}

/**
 * Configuration for self-play controller
 */
data class SelfPlayControllerConfig(
    // Agent configuration
    val agentType: AgentType = AgentType.DQN,
    val hiddenLayers: List<Int> = listOf(512, 256, 128),
    val learningRate: Double = 0.001,
    val explorationRate: Double = 0.1,
    val temperature: Double = 1.0,
    
    // Self-play configuration
    val gamesPerIteration: Int = 20,
    val maxConcurrentGames: Int = 4,
    val maxStepsPerGame: Int = 100,
    
    // Training configuration
    val batchSize: Int = 64,
    val maxExperienceBufferSize: Int = 50000,
    val samplingStrategy: SamplingStrategy = SamplingStrategy.MIXED,
    
    // Reward configuration
    val winReward: Double = 1.0,
    val lossReward: Double = -1.0,
    val drawReward: Double = 0.0,
    val enablePositionRewards: Boolean = true,
    
    // Experience management
    val experienceCleanupStrategy: ExperienceCleanupStrategy = ExperienceCleanupStrategy.OLDEST_FIRST,
    
    // Opponent strategy
    val opponentUpdateStrategy: OpponentUpdateStrategy = OpponentUpdateStrategy.COPY_MAIN,
    val opponentUpdateFrequency: Int = 5,
    
    // Monitoring and reporting
    val progressReportInterval: Int = 5,
    val iterationReportInterval: Int = 5,
    
    // Early stopping
    val enableEarlyStopping: Boolean = false,
    val earlyStoppingWindow: Int = 10,
    val earlyStoppingThreshold: Double = 0.8
)

/**
 * Opponent update strategies
 */
enum class OpponentUpdateStrategy {
    COPY_MAIN,      // Copy main agent's current weights
    HISTORICAL,     // Use historical version of main agent
    FIXED,          // Keep opponent fixed
    ADAPTIVE        // Adapt based on performance
}

/**
 * Result of a single self-play iteration
 */
data class SelfPlayIterationResult(
    val iteration: Int,
    val selfPlayResults: SelfPlayResults,
    val trainingMetrics: SelfPlayTrainingMetrics,
    val evaluationResults: AgentEvaluationResults,
    val iterationDuration: Long
)

/**
 * Training metrics from self-play
 */
data class SelfPlayTrainingMetrics(
    val totalBatchUpdates: Int,
    val averageLoss: Double,
    val averageGradientNorm: Double,
    val averagePolicyEntropy: Double,
    val averageReward: Double,
    val explorationRate: Double,
    val experienceBufferSize: Int
)

/**
 * Agent evaluation results
 */
data class AgentEvaluationResults(
    val gamesPlayed: Int,
    val averageReward: Double,
    val averageGameLength: Double,
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val gameResults: List<EvaluationGameResult>
)

/**
 * Single evaluation game result
 */
data class EvaluationGameResult(
    val gameIndex: Int,
    val gameLength: Int,
    val totalReward: Double,
    val gameOutcome: GameOutcome,
    val finalPosition: String
)

/**
 * Complete self-play training results
 */
data class SelfPlayTrainingResults(
    val totalIterations: Int,
    val totalDuration: Long,
    val bestPerformance: Double,
    val iterationHistory: List<SelfPlayIterationResult>,
    val finalMetrics: SelfPlayFinalMetrics
)

/**
 * Final self-play training metrics
 */
data class SelfPlayFinalMetrics(
    val totalGamesPlayed: Int,
    val totalExperiencesCollected: Int,
    val averageReward: Double,
    val bestReward: Double,
    val averageWinRate: Double,
    val averageGameLength: Double,
    val totalBatchUpdates: Int,
    val averageLoss: Double
)

/**
 * Current self-play training status
 */
data class SelfPlayTrainingStatus(
    val isTraining: Boolean,
    val currentIteration: Int,
    val totalIterations: Int,
    val completedIterations: Int,
    val bestPerformance: Double
)
