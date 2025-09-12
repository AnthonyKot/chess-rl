package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*
import kotlin.math.*

/**
 * Training controller that provides a high-level interface for managing
 * chess RL training with comprehensive monitoring and control features.
 */
class TrainingController(
    private val config: TrainingControllerConfig = TrainingControllerConfig()
) {
    
    private var pipeline: ChessTrainingPipeline? = null
    private var isTraining = false
    private var isPaused = false
    
    // Training components
    private var agent: ChessAgent? = null
    private var environment: ChessEnvironment? = null
    
    // Monitoring
    private val trainingMonitor = TrainingMonitor()
    private var trainingStartTime = 0L
    
    /**
     * Initialize training components
     */
    fun initialize(): Boolean {
        try {
            println("🔧 Initializing Chess RL Training Controller")
            
            // Create chess environment
            environment = ChessEnvironment(
                rewardConfig = ChessRewardConfig(
                    winReward = config.winReward,
                    lossReward = config.lossReward,
                    drawReward = config.drawReward,
                    enablePositionRewards = config.enablePositionRewards
                )
            )
            
            // Create chess agent
            agent = createAgent()
            
            // Create training pipeline
            pipeline = ChessTrainingPipeline(
                agent = agent!!,
                environment = environment!!,
                config = TrainingPipelineConfig(
                    maxStepsPerEpisode = config.maxStepsPerEpisode,
                    batchSize = config.batchSize,
                    batchTrainingFrequency = config.batchTrainingFrequency,
                    maxBufferSize = config.maxBufferSize,
                    progressReportInterval = config.progressReportInterval,
                    checkpointInterval = config.checkpointInterval,
                    enableEarlyStopping = config.enableEarlyStopping,
                    earlyStoppingThreshold = config.earlyStoppingThreshold
                )
            )
            
            println("✅ Training controller initialized successfully")
            return true
            
        } catch (e: Exception) {
            println("❌ Failed to initialize training controller: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Start training with specified parameters
     */
    fun startTraining(episodes: Int): TrainingResults? {
        if (isTraining) {
            println("⚠️ Training is already in progress")
            return null
        }
        
        val pipeline = this.pipeline ?: run {
            println("❌ Training controller not initialized")
            return null
        }
        
        try {
            isTraining = true
            isPaused = false
            trainingStartTime = getCurrentTimeMillis()
            
            println("🚀 Starting training for $episodes episodes")
            trainingMonitor.startMonitoring()
            
            // Run training pipeline
            val results = pipeline.train(episodes)
            
            // Stop monitoring
            trainingMonitor.stopMonitoring()
            
            isTraining = false
            
            // Display final results
            displayFinalResults(results)
            
            return results
            
        } catch (e: Exception) {
            println("❌ Training failed: ${e.message}")
            e.printStackTrace()
            isTraining = false
            trainingMonitor.stopMonitoring()
            return null
        }
    }
    
    /**
     * Pause training
     */
    fun pauseTraining() {
        if (!isTraining || isPaused) {
            println("⚠️ Training is not running or already paused")
            return
        }
        
        pipeline?.pause()
        isPaused = true
        trainingMonitor.pauseMonitoring()
        println("⏸️ Training paused")
    }
    
    /**
     * Resume training
     */
    fun resumeTraining() {
        if (!isTraining || !isPaused) {
            println("⚠️ Training is not paused")
            return
        }
        
        pipeline?.resume()
        isPaused = false
        trainingMonitor.resumeMonitoring()
        println("▶️ Training resumed")
    }
    
    /**
     * Stop training gracefully
     */
    fun stopTraining() {
        if (!isTraining) {
            println("⚠️ Training is not running")
            return
        }
        
        pipeline?.stop()
        trainingMonitor.stopMonitoring()
        isTraining = false
        isPaused = false
        println("🛑 Training stopped")
    }
    
    /**
     * Get current training status
     */
    fun getTrainingStatus(): TrainingStatus {
        val pipeline = this.pipeline
        val statistics = pipeline?.getCurrentStatistics()
        
        return TrainingStatus(
            isTraining = isTraining,
            isPaused = isPaused,
            currentEpisode = statistics?.currentEpisode ?: 0,
            totalSteps = statistics?.totalSteps ?: 0,
            averageReward = statistics?.averageReward ?: 0.0,
            recentAverageReward = statistics?.recentAverageReward ?: 0.0,
            bestPerformance = statistics?.bestPerformance ?: Double.NEGATIVE_INFINITY,
            bufferSize = statistics?.bufferSize ?: 0,
            batchUpdates = statistics?.batchUpdates ?: 0,
            elapsedTime = if (isTraining) getCurrentTimeMillis() - trainingStartTime else 0L
        )
    }
    
    /**
     * Run a quick training test
     */
    fun runTrainingTest(episodes: Int = 5): Boolean {
        println("🧪 Running training test with $episodes episodes")
        
        if (!initialize()) {
            return false
        }
        
        val results = startTraining(episodes)
        return results != null && results.totalEpisodes > 0
    }
    
    /**
     * Demonstrate agent playing a single game
     */
    fun demonstrateGame(): GameDemonstration? {
        val agent = this.agent ?: run {
            println("❌ Agent not initialized")
            return null
        }
        
        val environment = this.environment ?: run {
            println("❌ Environment not initialized")
            return null
        }
        
        println("🎮 Demonstrating agent gameplay")
        
        try {
            var state = environment.reset()
            val moves = mutableListOf<GameMove>()
            var stepCount = 0
            val maxSteps = 50
            
            while (!environment.isTerminal(state) && stepCount < maxSteps) {
                val validActions = environment.getValidActions(state)
                if (validActions.isEmpty()) break
                
                val action = agent.selectAction(state, validActions)
                val stepResult = environment.step(action)
                
                val move = GameMove(
                    step = stepCount + 1,
                    action = action,
                    reward = stepResult.reward,
                    gameStatus = stepResult.info["game_status"]?.toString() ?: "ongoing",
                    move = stepResult.info["move"]?.toString() ?: "unknown"
                )
                
                moves.add(move)
                state = stepResult.nextState
                stepCount++
                
                if (stepResult.done) break
            }
            
            val finalStatus = environment.getGameStatus()
            val chessMetrics = environment.getChessMetrics()
            
            return GameDemonstration(
                totalMoves = moves.size,
                finalStatus = finalStatus.name,
                moves = moves,
                chessMetrics = chessMetrics
            )
            
        } catch (e: Exception) {
            println("❌ Game demonstration failed: ${e.message}")
            return null
        }
    }
    
    /**
     * Analyze agent performance
     */
    fun analyzePerformance(): PerformanceAnalysis? {
        val agent = this.agent ?: run {
            println("❌ Agent not initialized")
            return null
        }
        
        val environment = this.environment ?: run {
            println("❌ Environment not initialized")
            return null
        }
        
        println("📊 Analyzing agent performance")
        
        try {
            // Test on multiple positions
            val testResults = mutableListOf<PositionTest>()
            
            repeat(10) { testIndex ->
                val state = environment.reset()
                val validActions = environment.getValidActions(state).take(5)
                
                if (validActions.isNotEmpty()) {
                    val qValues = agent.getQValues(state, validActions)
                    val probabilities = agent.getActionProbabilities(state, validActions)
                    val selectedAction = agent.selectAction(state, validActions)
                    
                    testResults.add(
                        PositionTest(
                            testIndex = testIndex,
                            validActions = validActions.size,
                            selectedAction = selectedAction,
                            maxQValue = qValues.values.maxOrNull() ?: 0.0,
                            minQValue = qValues.values.minOrNull() ?: 0.0,
                            avgQValue = qValues.values.average(),
                            maxProbability = probabilities.values.maxOrNull() ?: 0.0,
                            entropyScore = calculateEntropy(probabilities.values.toList())
                        )
                    )
                }
            }
            
            // Calculate overall statistics
            val avgMaxQ = testResults.map { it.maxQValue }.average()
            val avgMinQ = testResults.map { it.minQValue }.average()
            val avgEntropy = testResults.map { it.entropyScore }.average()
            val avgValidActions = testResults.map { it.validActions }.average()
            
            return PerformanceAnalysis(
                testsPerformed = testResults.size,
                averageMaxQValue = avgMaxQ,
                averageMinQValue = avgMinQ,
                averageEntropy = avgEntropy,
                averageValidActions = avgValidActions,
                testResults = testResults
            )
            
        } catch (e: Exception) {
            println("❌ Performance analysis failed: ${e.message}")
            return null
        }
    }
    
    /**
     * Create agent based on configuration
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
                        maxBufferSize = config.maxBufferSize
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
                        maxBufferSize = config.maxBufferSize
                    )
                )
            }
        }
    }
    
    /**
     * Display final training results
     */
    private fun displayFinalResults(results: TrainingResults) {
        println("\n🏁 Training Completed!")
        println("=" * 50)
        println("Episodes: ${results.totalEpisodes}")
        println("Total Steps: ${results.totalSteps}")
        println("Training Duration: ${results.trainingDuration}ms")
        println("Best Performance: ${results.bestPerformance}")
        println()
        
        val finalMetrics = results.finalMetrics
        println("📈 Final Statistics:")
        println("   Average Reward: ${finalMetrics.averageReward}")
        println("   Average Game Length: ${finalMetrics.averageGameLength} steps")
        println("   Win Rate: ${(finalMetrics.winRate * 100)}%")
        println("   Draw Rate: ${(finalMetrics.drawRate * 100)}%")
        println("   Loss Rate: ${(finalMetrics.lossRate * 100)}%")
        println("   Total Batch Updates: ${finalMetrics.totalBatchUpdates}")
        println("   Average Batch Loss: ${finalMetrics.averageBatchLoss}")
        println("   Final Buffer Size: ${finalMetrics.finalBufferSize}")
    }
    
    /**
     * Calculate entropy of probability distribution
     */
    private fun calculateEntropy(probabilities: List<Double>): Double {
        var entropy = 0.0
        for (prob in probabilities) {
            if (prob > 0.0) {
                entropy -= prob * ln(prob)
            }
        }
        return entropy
    }
}

/**
 * Configuration for training controller
 */
data class TrainingControllerConfig(
    // Agent configuration
    val agentType: AgentType = AgentType.DQN,
    val hiddenLayers: List<Int> = listOf(512, 256, 128),
    val learningRate: Double = 0.001,
    val explorationRate: Double = 0.1,
    val temperature: Double = 1.0, // For policy gradient
    
    // Training configuration
    val maxStepsPerEpisode: Int = 200,
    val batchSize: Int = 64,
    val batchTrainingFrequency: Int = 1,
    val maxBufferSize: Int = 50000,
    
    // Reward configuration
    val winReward: Double = 1.0,
    val lossReward: Double = -1.0,
    val drawReward: Double = 0.0,
    val enablePositionRewards: Boolean = false,
    
    // Monitoring configuration
    val progressReportInterval: Int = 100,
    val checkpointInterval: Int = 1000,
    
    // Early stopping
    val enableEarlyStopping: Boolean = false,
    val earlyStoppingThreshold: Double = 0.8
)

/**
 * Agent types supported by the controller
 */
enum class AgentType {
    DQN,
    POLICY_GRADIENT
}

/**
 * Current training status
 */
data class TrainingStatus(
    val isTraining: Boolean,
    val isPaused: Boolean,
    val currentEpisode: Int,
    val totalSteps: Int,
    val averageReward: Double,
    val recentAverageReward: Double,
    val bestPerformance: Double,
    val bufferSize: Int,
    val batchUpdates: Int,
    val elapsedTime: Long
)

/**
 * Game demonstration results
 */
data class GameDemonstration(
    val totalMoves: Int,
    val finalStatus: String,
    val moves: List<GameMove>,
    val chessMetrics: ChessMetrics
)

/**
 * Single move in game demonstration
 */
data class GameMove(
    val step: Int,
    val action: Int,
    val reward: Double,
    val gameStatus: String,
    val move: String
)

/**
 * Performance analysis results
 */
data class PerformanceAnalysis(
    val testsPerformed: Int,
    val averageMaxQValue: Double,
    val averageMinQValue: Double,
    val averageEntropy: Double,
    val averageValidActions: Double,
    val testResults: List<PositionTest>
)

/**
 * Single position test result
 */
data class PositionTest(
    val testIndex: Int,
    val validActions: Int,
    val selectedAction: Int,
    val maxQValue: Double,
    val minQValue: Double,
    val avgQValue: Double,
    val maxProbability: Double,
    val entropyScore: Double
)

/**
 * Training monitor for real-time monitoring
 */
class TrainingMonitor {
    private var isMonitoring = false
    private var isPaused = false
    
    fun startMonitoring() {
        isMonitoring = true
        isPaused = false
        println("📡 Training monitor started")
    }
    
    fun pauseMonitoring() {
        isPaused = true
        println("📡 Training monitor paused")
    }
    
    fun resumeMonitoring() {
        isPaused = false
        println("📡 Training monitor resumed")
    }
    
    fun stopMonitoring() {
        isMonitoring = false
        isPaused = false
        println("📡 Training monitor stopped")
    }
    
    fun isActive(): Boolean = isMonitoring && !isPaused
}

