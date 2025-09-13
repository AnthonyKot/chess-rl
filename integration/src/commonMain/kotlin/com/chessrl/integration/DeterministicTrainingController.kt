package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*
import kotlin.math.*

/**
 * Enhanced training controller with comprehensive seed management and deterministic training support.
 * Provides centralized seeding for all stochastic components and deterministic test modes.
 */
class DeterministicTrainingController(
    private val config: TrainingConfiguration = TrainingConfiguration()
) {
    
    private var pipeline: ChessTrainingPipeline? = null
    private var isTraining = false
    private var isPaused = false
    
    // Training components with seeded random generators
    private var agent: ChessAgent? = null
    private var environment: ChessEnvironment? = null
    
    // Seed management
    private val seedManager = SeedManager.getInstance()
    
    // Enhanced monitoring and checkpointing
    private val trainingMonitor = EnhancedTrainingMonitor()
    private val checkpointManager = CheckpointManager()
    private var trainingStartTime = 0L
    
    // Training state
    private var currentEpisode = 0
    private var trainingResults: TrainingResults? = null
    
    /**
     * Initialize training components with seed management
     */
    fun initialize(): Boolean {
        try {
            println("🔧 Initializing Deterministic Chess RL Training Controller")
            
            // Initialize seed management
            initializeSeedManagement()
            
            // Create chess environment with seeded random
            environment = createSeededChessEnvironment()
            
            // Create chess agent with seeded random
            agent = createSeededChessAgent()
            
            // Create training pipeline with seeded components
            pipeline = createSeededTrainingPipeline()
            
            // Validate seed consistency
            validateSeedConsistency()
            
            println("✅ Deterministic training controller initialized successfully")
            logSeedConfiguration()
            
            return true
            
        } catch (e: Exception) {
            println("❌ Failed to initialize deterministic training controller: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Start training with comprehensive seed logging and checkpointing
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
            currentEpisode = 0
            
            println("🚀 Starting deterministic training for $episodes episodes")
            if (seedManager.isDeterministic()) {
                println("🎲 Deterministic mode enabled - seed: ${seedManager.getMasterSeed()}")
            }
            
            trainingMonitor.startMonitoring()
            
            // Create initial checkpoint with seed configuration
            createInitialCheckpoint()
            
            // Run training pipeline with enhanced monitoring
            val results = runTrainingWithSeedTracking(pipeline, episodes)
            
            // Create final checkpoint
            createFinalCheckpoint(results)
            
            // Stop monitoring
            trainingMonitor.stopMonitoring()
            
            isTraining = false
            trainingResults = results
            
            // Display final results with seed information
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
     * Enable deterministic test mode for CI
     */
    fun enableDeterministicTestMode(testSeed: Long = 12345L): Boolean {
        try {
            seedManager.enableDeterministicTestMode(testSeed)
            
            // Reinitialize components with test seed
            if (initialize()) {
                println("🧪 Deterministic test mode enabled successfully")
                return true
            } else {
                println("❌ Failed to enable deterministic test mode")
                return false
            }
            
        } catch (e: Exception) {
            println("❌ Failed to enable deterministic test mode: ${e.message}")
            return false
        }
    }
    
    /**
     * Run a deterministic test with fixed seed
     */
    fun runDeterministicTest(episodes: Int = 5, testSeed: Long = 12345L): DeterministicTestResult {
        println("🧪 Running deterministic test with seed $testSeed")
        
        val testStartTime = getCurrentTimeMillis()
        
        try {
            // Enable test mode
            if (!enableDeterministicTestMode(testSeed)) {
                return DeterministicTestResult(
                    success = false,
                    seed = testSeed,
                    episodes = 0,
                    duration = 0L,
                    error = "Failed to enable deterministic test mode"
                )
            }
            
            // Run training
            val results = startTraining(episodes)
            
            val testEndTime = getCurrentTimeMillis()
            val testDuration = testEndTime - testStartTime
            
            if (results != null) {
                println("✅ Deterministic test completed successfully")
                return DeterministicTestResult(
                    success = true,
                    seed = testSeed,
                    episodes = results.totalEpisodes,
                    duration = testDuration,
                    finalPerformance = results.bestPerformance,
                    seedConfiguration = seedManager.getSeedConfiguration()
                )
            } else {
                return DeterministicTestResult(
                    success = false,
                    seed = testSeed,
                    episodes = 0,
                    duration = testDuration,
                    error = "Training returned null results"
                )
            }
            
        } catch (e: Exception) {
            val testEndTime = getCurrentTimeMillis()
            val testDuration = testEndTime - testStartTime
            
            return DeterministicTestResult(
                success = false,
                seed = testSeed,
                episodes = 0,
                duration = testDuration,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Load checkpoint and restore seed configuration
     */
    fun loadCheckpointWithSeedRestore(checkpointInfo: CheckpointInfo): Boolean {
        val agent = this.agent ?: run {
            println("❌ Agent not initialized")
            return false
        }
        
        try {
            // Load checkpoint
            val loadResult = checkpointManager.loadCheckpoint(checkpointInfo, agent)
            
            if (loadResult.success) {
                // Restore seed configuration if available
                val seedConfig = checkpointInfo.metadata.seedConfiguration
                if (seedConfig != null) {
                    seedManager.restoreSeedConfiguration(seedConfig)
                    println("🎲 Seed configuration restored from checkpoint")
                } else {
                    println("⚠️ No seed configuration found in checkpoint")
                }
                
                return true
            } else {
                println("❌ Failed to load checkpoint: ${loadResult.error}")
                return false
            }
            
        } catch (e: Exception) {
            println("❌ Failed to load checkpoint with seed restore: ${e.message}")
            return false
        }
    }
    
    /**
     * Get comprehensive training status including seed information
     */
    fun getEnhancedTrainingStatus(): EnhancedTrainingStatus {
        val basicStatus = getBasicTrainingStatus()
        val seedSummary = seedManager.getSeedSummary()
        val seedValidation = seedManager.validateSeedConsistency()
        
        return EnhancedTrainingStatus(
            basicStatus = basicStatus,
            seedSummary = seedSummary,
            seedValidation = seedValidation,
            configurationSummary = config.getSummary(),
            checkpointSummary = checkpointManager.getSummary()
        )
    }
    
    // Private helper methods
    
    private fun initializeSeedManagement() {
        val seedConfig = config.getSeedConfig()
        
        when {
            seedConfig.seed != null -> {
                // Use specified seed
                seedManager.setSeed(seedConfig.seed)
            }
            seedConfig.deterministicMode -> {
                // Generate deterministic seed
                seedManager.enableDeterministicTestMode()
            }
            else -> {
                // Use random seed
                seedManager.setRandomSeed()
            }
        }
    }
    
    private fun createSeededChessEnvironment(): ChessEnvironment {
        return ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = config.winReward,
                lossReward = config.lossReward,
                drawReward = config.drawReward,
                enablePositionRewards = config.enablePositionRewards
            ),
            random = seedManager.getDataGenerationRandom()
        )
    }
    
    private fun createSeededChessAgent(): ChessAgent {
        val nnRandom = seedManager.getNeuralNetworkRandom()
        val explorationRandom = seedManager.getExplorationRandom()
        
        return when (config.optimizer) {
            "dqn" -> {
                ChessAgentFactory.createSeededDQNAgent(
                    hiddenLayers = config.hiddenLayers,
                    learningRate = config.learningRate,
                    explorationRate = config.explorationRate,
                    neuralNetworkRandom = nnRandom,
                    explorationRandom = explorationRandom,
                    weightInitType = config.weightInitialization,
                    config = ChessAgentConfig(
                        batchSize = config.batchSize,
                        maxBufferSize = config.maxBufferSize
                    )
                )
            }
            else -> {
                ChessAgentFactory.createSeededPolicyGradientAgent(
                    hiddenLayers = config.hiddenLayers,
                    learningRate = config.learningRate,
                    temperature = 1.0,
                    neuralNetworkRandom = nnRandom,
                    explorationRandom = explorationRandom,
                    weightInitType = config.weightInitialization,
                    config = ChessAgentConfig(
                        batchSize = config.batchSize,
                        maxBufferSize = config.maxBufferSize
                    )
                )
            }
        }
    }
    
    private fun createSeededTrainingPipeline(): ChessTrainingPipeline {
        return ChessTrainingPipeline(
            agent = agent!!,
            environment = environment!!,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = config.maxStepsPerEpisode,
                batchSize = config.batchSize,
                batchTrainingFrequency = 1,
                maxBufferSize = config.maxBufferSize,
                progressReportInterval = config.progressReportInterval,
                checkpointInterval = config.checkpointInterval,
                enableEarlyStopping = false,
                earlyStoppingThreshold = 0.8
            ),
            replayBufferRandom = seedManager.getReplayBufferRandom()
        )
    }
    
    private fun validateSeedConsistency() {
        val validation = seedManager.validateSeedConsistency()
        if (!validation.isValid) {
            println("⚠️ Seed validation issues detected:")
            validation.issues.forEach { println("   - $it") }
        }
    }
    
    private fun logSeedConfiguration() {
        val summary = seedManager.getSeedSummary()
        println("🎲 Seed Configuration:")
        println("   Master Seed: ${summary.masterSeed}")
        println("   Deterministic Mode: ${summary.isDeterministicMode}")
        println("   Component Seeds: ${summary.componentCount}")
        
        if (config.enableSeedLogging) {
            val componentSeeds = seedManager.getAllComponentSeeds()
            componentSeeds.forEach { (component, seed) ->
                println("   $component: $seed")
            }
        }
    }
    
    private fun runTrainingWithSeedTracking(
        pipeline: ChessTrainingPipeline,
        episodes: Int
    ): TrainingResults {
        // Enhanced training loop with seed tracking
        return pipeline.train(episodes)
    }
    
    private fun createInitialCheckpoint() {
        try {
            val metadata = CheckpointMetadata(
                cycle = 0,
                performance = 0.0,
                description = "Initial checkpoint with seed configuration",
                seedConfiguration = seedManager.getSeedConfiguration(),
                trainingConfiguration = config
            )
            
            checkpointManager.createCheckpoint(agent!!, 0, metadata)
            
        } catch (e: Exception) {
            println("⚠️ Failed to create initial checkpoint: ${e.message}")
        }
    }
    
    private fun createFinalCheckpoint(results: TrainingResults) {
        try {
            val metadata = CheckpointMetadata(
                cycle = results.totalEpisodes,
                performance = results.bestPerformance,
                description = "Final checkpoint after ${results.totalEpisodes} episodes",
                isBest = true,
                seedConfiguration = seedManager.getSeedConfiguration(),
                trainingConfiguration = config
            )
            
            checkpointManager.createCheckpoint(agent!!, results.totalEpisodes, metadata)
            
        } catch (e: Exception) {
            println("⚠️ Failed to create final checkpoint: ${e.message}")
        }
    }
    
    private fun getBasicTrainingStatus(): TrainingStatus {
        val pipeline = this.pipeline
        val statistics = pipeline?.getCurrentStatistics()
        
        return TrainingStatus(
            isTraining = isTraining,
            isPaused = isPaused,
            currentEpisode = statistics?.currentEpisode ?: currentEpisode,
            totalSteps = statistics?.totalSteps ?: 0,
            averageReward = statistics?.averageReward ?: 0.0,
            recentAverageReward = statistics?.recentAverageReward ?: 0.0,
            bestPerformance = statistics?.bestPerformance ?: Double.NEGATIVE_INFINITY,
            bufferSize = statistics?.bufferSize ?: 0,
            batchUpdates = statistics?.batchUpdates ?: 0,
            elapsedTime = if (isTraining) getCurrentTimeMillis() - trainingStartTime else 0L
        )
    }
    
    private fun displayFinalResults(results: TrainingResults) {
        println("\n🏁 Deterministic Training Completed!")
        println("=" * 60)
        println("Episodes: ${results.totalEpisodes}")
        println("Total Steps: ${results.totalSteps}")
        println("Training Duration: ${results.trainingDuration}ms")
        println("Best Performance: ${results.bestPerformance}")
        
        // Display seed information
        val seedSummary = seedManager.getSeedSummary()
        println("\n🎲 Seed Information:")
        println("   Master Seed: ${seedSummary.masterSeed}")
        println("   Deterministic Mode: ${seedSummary.isDeterministicMode}")
        println("   Component Seeds: ${seedSummary.componentCount}")
        
        // Display final metrics
        val finalMetrics = results.finalMetrics
        println("\n📈 Final Statistics:")
        println("   Average Reward: ${finalMetrics.averageReward}")
        println("   Win Rate: ${(finalMetrics.winRate * 100)}%")
        println("   Total Batch Updates: ${finalMetrics.totalBatchUpdates}")
        println("   Final Buffer Size: ${finalMetrics.finalBufferSize}")
        
        // Display reproducibility information
        if (seedSummary.isDeterministicMode) {
            println("\n🔄 Reproducibility:")
            println("   This run can be reproduced using seed: ${seedSummary.masterSeed}")
            println("   Use --seed ${seedSummary.masterSeed} --deterministic to reproduce")
        }
    }
}

/**
 * Enhanced training status with seed information
 */
data class EnhancedTrainingStatus(
    val basicStatus: TrainingStatus,
    val seedSummary: SeedSummary,
    val seedValidation: SeedValidationResult,
    val configurationSummary: ConfigurationSummary,
    val checkpointSummary: CheckpointSummary
)

/**
 * Deterministic test result
 */
data class DeterministicTestResult(
    val success: Boolean,
    val seed: Long,
    val episodes: Int,
    val duration: Long,
    val finalPerformance: Double? = null,
    val seedConfiguration: SeedConfiguration? = null,
    val error: String? = null
)

/**
 * Enhanced training monitor with seed tracking
 */
class EnhancedTrainingMonitor {
    private var isMonitoring = false
    private var isPaused = false
    private var seedEvents = mutableListOf<SeedEvent>()
    
    fun startMonitoring() {
        isMonitoring = true
        isPaused = false
        println("📡 Enhanced training monitor started with seed tracking")
    }
    
    fun pauseMonitoring() {
        isPaused = true
        println("📡 Enhanced training monitor paused")
    }
    
    fun resumeMonitoring() {
        isPaused = false
        println("📡 Enhanced training monitor resumed")
    }
    
    fun stopMonitoring() {
        isMonitoring = false
        isPaused = false
        println("📡 Enhanced training monitor stopped")
    }
    
    fun recordSeedEvent(event: SeedEvent) {
        if (isMonitoring && !isPaused) {
            seedEvents.add(event)
        }
    }
    
    fun getSeedEvents(): List<SeedEvent> = seedEvents.toList()
    
    fun isActive(): Boolean = isMonitoring && !isPaused
}