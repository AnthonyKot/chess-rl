package com.chessrl.integration

import com.chessrl.chess.PieceColor
import com.chessrl.rl.*
import com.chessrl.nn.*
import kotlin.math.*

/**
 * High-level controller for self-play training that integrates with the existing
 * training infrastructure to provide a complete self-play training solution.
 */
class SelfPlayController(
    private val config: SelfPlayControllerConfig = SelfPlayControllerConfig()
) {
    
    private var selfPlaySystem: SelfPlaySystem? = null
    private var agent: ChessAgent? = null
    private var environment: ChessEnvironment? = null
    
    // Training state
    private var isTraining = false
    private var isPaused = false
    private var trainingStartTime = 0L
    
    // Monitoring
    private val trainingMonitor = SelfPlayMonitor()
    
    /**
     * Initialize self-play training components
     */
    fun initialize(): Boolean {
        try {
            println("🔧 Initializing Self-Play Training Controller")
            
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
            agent = createSelfPlayAgent()
            
            // Create self-play system
            selfPlaySystem = SelfPlaySystem(
                agent = agent!!,
                environment = environment!!,
                config = SelfPlayConfig(
                    maxMovesPerGame = config.maxMovesPerGame,
                    trainingFrequency = config.trainingFrequency,
                    minExperiencesForTraining = config.minExperiencesForTraining,
                    trainingBatchSize = config.trainingBatchSize,
                    updatesPerTraining = config.updatesPerTraining,
                    maxExperienceBufferSize = config.maxExperienceBufferSize,
                    progressReportInterval = config.progressReportInterval,
                    checkpointInterval = config.checkpointInterval
                )
            )
            
            println("✅ Self-play controller initialized successfully")
            return true
            
        } catch (e: Exception) {
            println("❌ Failed to initialize self-play controller: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Start self-play training
     */
    fun startSelfPlayTraining(totalGames: Int): SelfPlayResults? {
        if (isTraining) {
            println("⚠️ Self-play training is already in progress")
            return null
        }
        
        val selfPlaySystem = this.selfPlaySystem ?: run {
            println("❌ Self-play controller not initialized")
            return null
        }
        
        try {
            isTraining = true
            isPaused = false
            trainingStartTime = getCurrentTimeMillis()
            
            println("🚀 Starting self-play training for $totalGames games")
            trainingMonitor.startMonitoring()
            
            // Run self-play training
            val results = selfPlaySystem.runSelfPlayTraining(totalGames)
            
            // Stop monitoring
            trainingMonitor.stopMonitoring()
            
            isTraining = false
            
            // Display final results
            displaySelfPlayResults(results)
            
            return results
            
        } catch (e: Exception) {
            println("❌ Self-play training failed: ${e.message}")
            e.printStackTrace()
            isTraining = false
            trainingMonitor.stopMonitoring()
            return null
        }
    }
    
    /**
     * Pause self-play training
     */
    fun pauseTraining() {
        if (!isTraining || isPaused) {
            println("⚠️ Self-play training is not running or already paused")
            return
        }
        
        isPaused = true
        trainingMonitor.pauseMonitoring()
        println("⏸️ Self-play training paused")
    }
    
    /**
     * Resume self-play training
     */
    fun resumeTraining() {
        if (!isTraining || !isPaused) {
            println("⚠️ Self-play training is not paused")
            return
        }
        
        isPaused = false
        trainingMonitor.resumeMonitoring()
        println("▶️ Self-play training resumed")
    }
    
    /**
     * Stop self-play training gracefully
     */
    fun stopTraining() {
        if (!isTraining) {
            println("⚠️ Self-play training is not running")
            return
        }
        
        trainingMonitor.stopMonitoring()
        isTraining = false
        isPaused = false
        println("🛑 Self-play training stopped")
    }
    
    /**
     * Get current self-play training status
     */
    fun getTrainingStatus(): SelfPlayTrainingStatus {
        val selfPlaySystem = this.selfPlaySystem
        val progress = selfPlaySystem?.getCurrentProgress()
        
        return SelfPlayTrainingStatus(
            isTraining = isTraining,
            isPaused = isPaused,
            gamesCompleted = progress?.gamesCompleted ?: 0,
            movesPlayed = progress?.movesPlayed ?: 0,
            experiencesCollected = progress?.experiencesCollected ?: 0,
            currentWinRate = progress?.currentWinRate ?: 0.0,
            bestWinRate = progress?.bestWinRate ?: 0.0,
            averageGameLength = progress?.averageGameLength ?: 0.0,
            elapsedTime = if (isTraining) getCurrentTimeMillis() - trainingStartTime else 0L
        )
    }
    
    /**
     * Run a quick self-play test
     */
    fun runSelfPlayTest(games: Int = 5): Boolean {
        println("🧪 Running self-play test with $games games")
        
        if (!initialize()) {
            return false
        }
        
        val results = startSelfPlayTraining(games)
        return results != null && results.totalGames > 0
    }
    
    /**
     * Demonstrate self-play game
     */
    fun demonstrateSelfPlayGame(): SelfPlayGameDemonstration? {
        val agent = this.agent ?: run {
            println("❌ Agent not initialized")
            return null
        }
        
        val environment = this.environment ?: run {
            println("❌ Environment not initialized")
            return null
        }
        
        println("🎮 Demonstrating self-play game")
        
        try {
            var state = environment.reset()
            val moves = mutableListOf<SelfPlayMove>()
            var moveCount = 0
            val maxMoves = 50
            
            while (!environment.isTerminal(state) && moveCount < maxMoves) {
                val validActions = environment.getValidActions(state)
                if (validActions.isEmpty()) break
                
                val currentPlayer = if (moveCount % 2 == 0) PieceColor.WHITE else PieceColor.BLACK
                val action = agent.selectAction(state, validActions)
                val stepResult = environment.step(action)
                
                val move = SelfPlayMove(
                    moveNumber = moveCount + 1,
                    player = currentPlayer,
                    action = action,
                    reward = stepResult.reward,
                    gameStatus = stepResult.info["game_status"]?.toString() ?: "ongoing",
                    move = stepResult.info["move"]?.toString() ?: "unknown"
                )
                
                moves.add(move)
                state = stepResult.nextState
                moveCount++
                
                if (stepResult.done) break
            }
            
            val finalStatus = environment.getGameStatus()
            val chessMetrics = environment.getChessMetrics()
            
            return SelfPlayGameDemonstration(
                totalMoves = moves.size,
                finalStatus = finalStatus.name,
                moves = moves,
                chessMetrics = chessMetrics
            )
            
        } catch (e: Exception) {
            println("❌ Self-play game demonstration failed: ${e.message}")
            return null
        }
    }
    
    /**
     * Analyze self-play training quality
     */
    fun analyzeTrainingQuality(): SelfPlayQualityAnalysis? {
        val selfPlaySystem = this.selfPlaySystem ?: run {
            println("❌ Self-play system not initialized")
            return null
        }
        
        println("📊 Analyzing self-play training quality")
        
        try {
            val gameQuality = selfPlaySystem.analyzeGameQuality()
            val currentProgress = selfPlaySystem.getCurrentProgress()
            
            // Calculate additional quality metrics
            val trainingEfficiency = calculateTrainingEfficiency(currentProgress)
            val learningProgress = calculateLearningProgress(currentProgress)
            
            return SelfPlayQualityAnalysis(
                gameQuality = gameQuality,
                trainingEfficiency = trainingEfficiency,
                learningProgress = learningProgress,
                overallQualityScore = calculateOverallQuality(gameQuality, trainingEfficiency, learningProgress)
            )
            
        } catch (e: Exception) {
            println("❌ Self-play quality analysis failed: ${e.message}")
            return null
        }
    }
    
    /**
     * Create agent for self-play training
     */
    private fun createSelfPlayAgent(): ChessAgent {
        return when (config.agentType) {
            AgentType.DQN -> {
                ChessAgentFactory.createDQNAgent(
                    hiddenLayers = config.hiddenLayers,
                    learningRate = config.learningRate,
                    explorationRate = config.explorationRate,
                    config = ChessAgentConfig(
                        batchSize = config.trainingBatchSize,
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
                        batchSize = config.trainingBatchSize,
                        maxBufferSize = config.maxExperienceBufferSize
                    )
                )
            }
        }
    }
    
    /**
     * Display self-play training results
     */
    private fun displaySelfPlayResults(results: SelfPlayResults) {
        println("\n🏁 Self-Play Training Completed!")
        println("=" * 50)
        println("Games Played: ${results.totalGames}")
        println("Total Moves: ${results.totalMoves}")
        println("Training Duration: ${results.trainingDuration}ms")
        println("Experiences Collected: ${results.experienceCount}")
        println()
        
        val stats = results.finalStatistics
        println("📈 Final Statistics:")
        println("   Win Rate: ${(stats.winRate * 100)}%")
        println("   Draw Rate: ${(stats.drawRate * 100)}%")
        println("   Average Game Length: ${stats.averageGameLength} moves")
        println("   Average Game Duration: ${stats.averageGameDuration}ms")
        println("   Best Win Rate: ${(stats.bestWinRate * 100)}%")
        println("   Total Training Experiences: ${stats.experienceCount}")
    }
    
    /**
     * Calculate training efficiency score
     */
    private fun calculateTrainingEfficiency(progress: SelfPlayProgress): Double {
        if (progress.gamesCompleted == 0) return 0.0
        
        // Efficiency based on experience collection rate and game completion
        val experienceRate = progress.experiencesCollected.toDouble() / progress.gamesCompleted
        val gameEfficiency = progress.averageGameLength / 100.0 // Normalize to reasonable game length
        
        return minOf(1.0, (experienceRate / 50.0 + gameEfficiency) / 2.0)
    }
    
    /**
     * Calculate learning progress score
     */
    private fun calculateLearningProgress(progress: SelfPlayProgress): Double {
        // Learning progress based on win rate improvement
        val winRateProgress = progress.currentWinRate
        val bestRateProgress = progress.bestWinRate
        
        return (winRateProgress + bestRateProgress) / 2.0
    }
    
    /**
     * Calculate overall quality score
     */
    private fun calculateOverallQuality(
        gameQuality: SelfPlayGameQualityAnalysis,
        trainingEfficiency: Double,
        learningProgress: Double
    ): Double {
        val gameScore = (gameQuality.gameCompletionRate + gameQuality.legalMoveRate + gameQuality.qualityScore) / 3.0
        return (gameScore + trainingEfficiency + learningProgress) / 3.0
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
    val temperature: Double = 1.0, // For policy gradient
    
    // Game configuration
    val maxMovesPerGame: Int = 200,
    
    // Training configuration
    val trainingFrequency: Int = 10,
    val minExperiencesForTraining: Int = 100,
    val trainingBatchSize: Int = 64,
    val updatesPerTraining: Int = 5,
    val maxExperienceBufferSize: Int = 50000,
    
    // Reward configuration
    val winReward: Double = 1.0,
    val lossReward: Double = -1.0,
    val drawReward: Double = 0.0,
    val enablePositionRewards: Boolean = false,
    
    // Monitoring configuration
    val progressReportInterval: Int = 50,
    val checkpointInterval: Int = 500
)

/**
 * Current self-play training status
 */
data class SelfPlayTrainingStatus(
    val isTraining: Boolean,
    val isPaused: Boolean,
    val gamesCompleted: Int,
    val movesPlayed: Int,
    val experiencesCollected: Int,
    val currentWinRate: Double,
    val bestWinRate: Double,
    val averageGameLength: Double,
    val elapsedTime: Long
)

/**
 * Self-play game demonstration results
 */
data class SelfPlayGameDemonstration(
    val totalMoves: Int,
    val finalStatus: String,
    val moves: List<SelfPlayMove>,
    val chessMetrics: ChessMetrics
)

/**
 * Single move in self-play demonstration
 */
data class SelfPlayMove(
    val moveNumber: Int,
    val player: PieceColor,
    val action: Int,
    val reward: Double,
    val gameStatus: String,
    val move: String
)

/**
 * Self-play training quality analysis
 */
data class SelfPlayQualityAnalysis(
    val gameQuality: SelfPlayGameQualityAnalysis,
    val trainingEfficiency: Double,
    val learningProgress: Double,
    val overallQualityScore: Double
)

/**
 * Self-play training monitor
 */
class SelfPlayMonitor {
    private var isMonitoring = false
    private var isPaused = false
    
    fun startMonitoring() {
        isMonitoring = true
        isPaused = false
        println("📡 Self-play training monitor started")
    }
    
    fun pauseMonitoring() {
        isPaused = true
        println("📡 Self-play training monitor paused")
    }
    
    fun resumeMonitoring() {
        isPaused = false
        println("📡 Self-play training monitor resumed")
    }
    
    fun stopMonitoring() {
        isMonitoring = false
        isPaused = false
        println("📡 Self-play training monitor stopped")
    }
    
    fun isActive(): Boolean = isMonitoring && !isPaused
}