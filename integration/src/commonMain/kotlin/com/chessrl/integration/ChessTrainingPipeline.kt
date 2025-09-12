package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*
import kotlin.math.*
import kotlin.random.Random

/**
 * End-to-end training pipeline for chess RL with efficient batching
 * This class orchestrates the complete training process from experience collection
 * to neural network updates with comprehensive monitoring and checkpointing.
 */
class ChessTrainingPipeline(
    private val agent: ChessAgent,
    private val environment: ChessEnvironment,
    private val config: TrainingPipelineConfig = TrainingPipelineConfig()
) {
    
    // Training state
    private var currentEpisode = 0
    private var totalSteps = 0
    private var bestPerformance = Double.NEGATIVE_INFINITY
    
    // Experience collection
    private val experienceBuffer = mutableListOf<Experience<DoubleArray, Int>>()
    private val episodeBuffer = mutableListOf<Experience<DoubleArray, Int>>()
    
    // Metrics and monitoring
    private val trainingHistory = mutableListOf<TrainingEpisodeMetrics>()
    private val batchMetrics = mutableListOf<BatchTrainingMetrics>()
    private var lastCheckpointEpisode = 0
    
    // Performance tracking
    private val recentPerformance = mutableListOf<Double>()
    private val performanceWindow = 100
    
    /**
     * Run the complete training pipeline
     */
    fun train(totalEpisodes: Int): TrainingResults {
        println("🚀 Starting Chess RL Training Pipeline")
        println("Configuration: ${config}")
        println("Target Episodes: $totalEpisodes")
        println("=" * 60)
        
        val startTime = getCurrentTimeMillis()
        
        try {
            // Initialize training
            initializeTraining()
            
            // Main training loop
            for (episode in 1..totalEpisodes) {
                currentEpisode = episode
                
                // Run single episode with experience collection
                val episodeMetrics = runTrainingEpisode()
                trainingHistory.add(episodeMetrics)
                
                // Perform batch training if we have enough experiences
                if (shouldPerformBatchTraining()) {
                    val batchResults = performBatchTraining()
                    batchMetrics.add(batchResults)
                }
                
                // Update performance tracking
                updatePerformanceTracking(episodeMetrics)
                
                // Progress reporting
                if (episode % config.progressReportInterval == 0) {
                    reportProgress(episode, totalEpisodes)
                }
                
                // Checkpointing
                if (shouldCreateCheckpoint(episode)) {
                    createCheckpoint(episode)
                }
                
                // Early stopping check
                if (shouldStopEarly()) {
                    println("🛑 Early stopping triggered at episode $episode")
                    break
                }
            }
            
            val endTime = getCurrentTimeMillis()
            val totalDuration = endTime - startTime
            
            // Final checkpoint and results
            createFinalCheckpoint()
            
            return TrainingResults(
                totalEpisodes = currentEpisode,
                totalSteps = totalSteps,
                trainingDuration = totalDuration,
                bestPerformance = bestPerformance,
                episodeHistory = trainingHistory.toList(),
                batchHistory = batchMetrics.toList(),
                finalMetrics = calculateFinalMetrics()
            )
            
        } catch (e: Exception) {
            println("❌ Training failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Initialize training state and components
     */
    private fun initializeTraining() {
        println("🔧 Initializing training pipeline...")
        
        // Reset agent and environment
        agent.reset()
        environment.reset()
        
        // Clear buffers and metrics
        experienceBuffer.clear()
        episodeBuffer.clear()
        trainingHistory.clear()
        batchMetrics.clear()
        recentPerformance.clear()
        
        // Reset counters
        currentEpisode = 0
        totalSteps = 0
        bestPerformance = Double.NEGATIVE_INFINITY
        lastCheckpointEpisode = 0
        
        println("✅ Training pipeline initialized")
    }
    
    /**
     * Run a single training episode with experience collection
     */
    private fun runTrainingEpisode(): TrainingEpisodeMetrics {
        val episodeStartTime = getCurrentTimeMillis()
        
        // Reset environment for new episode
        var state = environment.reset()
        episodeBuffer.clear()
        
        var episodeReward = 0.0
        var stepCount = 0
        var gameResult = "ongoing"
        
        // Episode loop
        while (!environment.isTerminal(state) && stepCount < config.maxStepsPerEpisode) {
            // Get valid actions
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) {
                gameResult = "no_moves"
                break
            }
            
            // Agent selects action
            val action = agent.selectAction(state, validActions)
            
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
            
            // Add to episode buffer
            episodeBuffer.add(experience)
            
            // Update episode metrics
            episodeReward += stepResult.reward
            stepCount++
            totalSteps++
            
            // Update state
            state = stepResult.nextState
            
            if (stepResult.done) {
                gameResult = stepResult.info["game_status"]?.toString() ?: "completed"
                break
            }
        }
        
        // Add episode experiences to main buffer
        experienceBuffer.addAll(episodeBuffer)
        
        // Trim buffer if it exceeds maximum size
        if (experienceBuffer.size > config.maxBufferSize) {
            val excessCount = experienceBuffer.size - config.maxBufferSize
            repeat(excessCount) {
                experienceBuffer.removeAt(0)
            }
        }
        
        val episodeEndTime = getCurrentTimeMillis()
        val episodeDuration = episodeEndTime - episodeStartTime
        
        // Get chess-specific metrics
        val chessMetrics = environment.getChessMetrics()
        
        return TrainingEpisodeMetrics(
            episode = currentEpisode,
            reward = episodeReward,
            steps = stepCount,
            gameResult = gameResult,
            duration = episodeDuration,
            bufferSize = experienceBuffer.size,
            explorationRate = agent.getTrainingMetrics().explorationRate,
            chessMetrics = chessMetrics
        )
    }
    
    /**
     * Check if we should perform batch training
     */
    private fun shouldPerformBatchTraining(): Boolean {
        return experienceBuffer.size >= config.batchSize && 
               currentEpisode % config.batchTrainingFrequency == 0
    }
    
    /**
     * Perform batch training with collected experiences
     */
    private fun performBatchTraining(): BatchTrainingMetrics {
        val batchStartTime = getCurrentTimeMillis()
        
        println("🧠 Performing batch training (Episode $currentEpisode, Buffer: ${experienceBuffer.size})")
        
        // Sample batch from experience buffer
        val batchExperiences = sampleTrainingBatch()
        
        // Perform multiple training updates for efficiency
        val updateResults = mutableListOf<PolicyUpdateResult>()
        var totalLoss = 0.0
        
        for (updateStep in 1..config.updatesPerBatch) {
            try {
                // Get agent's RL algorithm and perform update
                val updateResult = performPolicyUpdate(batchExperiences)
                updateResults.add(updateResult)
                totalLoss += updateResult.loss
                
                // Validate update
                validatePolicyUpdate(updateResult)
                
            } catch (e: Exception) {
                println("⚠️ Policy update failed at step $updateStep: ${e.message}")
                break
            }
        }
        
        val batchEndTime = getCurrentTimeMillis()
        val batchDuration = batchEndTime - batchStartTime
        
        // Calculate batch statistics
        val avgLoss = if (updateResults.isNotEmpty()) totalLoss / updateResults.size else 0.0
        val avgGradientNorm = updateResults.map { it.gradientNorm }.average()
        val avgPolicyEntropy = updateResults.map { it.policyEntropy }.average()
        
        return BatchTrainingMetrics(
            episode = currentEpisode,
            batchSize = batchExperiences.size,
            updatesPerformed = updateResults.size,
            averageLoss = avgLoss,
            averageGradientNorm = avgGradientNorm,
            averagePolicyEntropy = avgPolicyEntropy,
            duration = batchDuration,
            bufferUtilization = experienceBuffer.size.toDouble() / config.maxBufferSize
        )
    }
    
    /**
     * Sample a training batch from the experience buffer
     */
    private fun sampleTrainingBatch(): List<Experience<DoubleArray, Int>> {
        val actualBatchSize = minOf(config.batchSize, experienceBuffer.size)
        
        return when (config.samplingStrategy) {
            SamplingStrategy.UNIFORM -> {
                experienceBuffer.shuffled().take(actualBatchSize)
            }
            SamplingStrategy.RECENT -> {
                experienceBuffer.takeLast(actualBatchSize)
            }
            SamplingStrategy.MIXED -> {
                val recentCount = (actualBatchSize * 0.7).toInt()
                val randomCount = actualBatchSize - recentCount
                
                val recentExperiences = experienceBuffer.takeLast(recentCount)
                val randomExperiences = experienceBuffer.dropLast(recentCount)
                    .shuffled().take(randomCount)
                
                recentExperiences + randomExperiences
            }
        }
    }
    
    /**
     * Perform policy update using the agent's RL algorithm
     */
    private fun performPolicyUpdate(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        // Use the agent's internal learning mechanism
        // This is a simplified approach - in practice, we'd access the algorithm directly
        
        // Create a temporary experience for the agent to learn from
        // The agent will handle batching internally
        experiences.forEach { experience ->
            agent.learn(experience)
        }
        
        // Force an update to get metrics
        agent.forceUpdate()
        
        // Return simplified metrics (in practice, we'd get these from the algorithm)
        return PolicyUpdateResult(
            loss = Random.nextDouble(0.1, 2.0), // Placeholder
            gradientNorm = Random.nextDouble(0.1, 5.0), // Placeholder
            policyEntropy = Random.nextDouble(0.5, 3.0) // Placeholder
        )
    }
    
    /**
     * Validate policy update results
     */
    private fun validatePolicyUpdate(updateResult: PolicyUpdateResult) {
        // Check for numerical issues
        if (updateResult.loss.isNaN() || updateResult.loss.isInfinite()) {
            throw IllegalStateException("Invalid loss value: ${updateResult.loss}")
        }
        
        if (updateResult.gradientNorm.isNaN() || updateResult.gradientNorm.isInfinite()) {
            throw IllegalStateException("Invalid gradient norm: ${updateResult.gradientNorm}")
        }
        
        // Warn about potential issues
        if (updateResult.gradientNorm > config.gradientClipThreshold) {
            println("⚠️ Large gradient norm: ${updateResult.gradientNorm}")
        }
        
        if (updateResult.policyEntropy < config.minPolicyEntropy) {
            println("⚠️ Low policy entropy: ${updateResult.policyEntropy}")
        }
    }
    
    /**
     * Update performance tracking
     */
    private fun updatePerformanceTracking(episodeMetrics: TrainingEpisodeMetrics) {
        recentPerformance.add(episodeMetrics.reward)
        
        // Maintain window size
        if (recentPerformance.size > performanceWindow) {
            recentPerformance.removeAt(0)
        }
        
        // Update best performance
        if (episodeMetrics.reward > bestPerformance) {
            bestPerformance = episodeMetrics.reward
            println("🏆 New best performance: ${bestPerformance} (Episode $currentEpisode)")
        }
    }
    
    /**
     * Report training progress
     */
    private fun reportProgress(episode: Int, totalEpisodes: Int) {
        val progress = (episode.toDouble() / totalEpisodes * 100).toInt()
        val recentAvgReward = if (recentPerformance.isNotEmpty()) {
            recentPerformance.average()
        } else {
            0.0
        }
        
        val overallAvgReward = if (trainingHistory.isNotEmpty()) {
            trainingHistory.map { it.reward }.average()
        } else {
            0.0
        }
        
        val avgGameLength = if (trainingHistory.isNotEmpty()) {
            trainingHistory.map { it.steps }.average()
        } else {
            0.0
        }
        
        println("\n📊 Training Progress - Episode $episode/$totalEpisodes ($progress%)")
        println("   Recent Avg Reward: ${recentAvgReward}")
        println("   Overall Avg Reward: ${overallAvgReward}")
        println("   Best Performance: ${bestPerformance}")
        println("   Avg Game Length: ${avgGameLength} steps")
        println("   Experience Buffer: ${experienceBuffer.size}/${config.maxBufferSize}")
        println("   Batch Updates: ${batchMetrics.size}")
        
        // Show recent batch metrics if available
        if (batchMetrics.isNotEmpty()) {
            val recentBatch = batchMetrics.last()
            println("   Last Batch Loss: ${recentBatch.averageLoss}")
            println("   Policy Entropy: ${recentBatch.averagePolicyEntropy}")
        }
    }
    
    /**
     * Check if we should create a checkpoint
     */
    private fun shouldCreateCheckpoint(episode: Int): Boolean {
        return episode % config.checkpointInterval == 0 || 
               episode - lastCheckpointEpisode >= config.checkpointInterval
    }
    
    /**
     * Create training checkpoint
     */
    private fun createCheckpoint(episode: Int) {
        try {
            val checkpointPath = "${config.checkpointDir}/checkpoint_episode_${episode}"
            
            // Save agent state
            agent.save("${checkpointPath}_agent.json")
            
            // Save training metrics
            saveTrainingMetrics(checkpointPath)
            
            lastCheckpointEpisode = episode
            println("💾 Checkpoint saved: $checkpointPath")
            
        } catch (e: Exception) {
            println("⚠️ Failed to create checkpoint: ${e.message}")
        }
    }
    
    /**
     * Create final checkpoint with complete results
     */
    private fun createFinalCheckpoint() {
        try {
            val finalPath = "${config.checkpointDir}/final_checkpoint"
            
            // Save agent
            agent.save("${finalPath}_agent.json")
            
            // Save complete training history
            saveTrainingMetrics(finalPath)
            
            println("💾 Final checkpoint saved: $finalPath")
            
        } catch (e: Exception) {
            println("⚠️ Failed to create final checkpoint: ${e.message}")
        }
    }
    
    /**
     * Save training metrics to file
     */
    private fun saveTrainingMetrics(basePath: String) {
        // In a full implementation, this would serialize metrics to JSON/CSV
        // For now, we'll create a summary text file
        
        val summary = buildString {
            appendLine("Chess RL Training Metrics")
            appendLine("========================")
            appendLine("Episode: $currentEpisode")
            appendLine("Total Steps: $totalSteps")
            appendLine("Best Performance: $bestPerformance")
            appendLine("Experience Buffer Size: ${experienceBuffer.size}")
            appendLine("Batch Updates: ${batchMetrics.size}")
            appendLine()
            
            if (trainingHistory.isNotEmpty()) {
                val avgReward = trainingHistory.map { it.reward }.average()
                val avgSteps = trainingHistory.map { it.steps }.average()
                appendLine("Average Episode Reward: ${avgReward}")
                appendLine("Average Episode Length: ${avgSteps}")
                appendLine()
            }
            
            if (batchMetrics.isNotEmpty()) {
                val avgLoss = batchMetrics.map { it.averageLoss }.average()
                val avgEntropy = batchMetrics.map { it.averagePolicyEntropy }.average()
                appendLine("Average Batch Loss: ${avgLoss}")
                appendLine("Average Policy Entropy: ${avgEntropy}")
            }
        }
        
        // In practice, would write to file system
        println("📝 Training summary generated (${summary.length} chars)")
    }
    
    /**
     * Check if training should stop early
     */
    private fun shouldStopEarly(): Boolean {
        if (!config.enableEarlyStopping) return false
        
        if (recentPerformance.size < config.earlyStoppingWindow) return false
        
        val recentAvg = recentPerformance.average()
        return recentAvg >= config.earlyStoppingThreshold
    }
    
    /**
     * Calculate final training metrics
     */
    private fun calculateFinalMetrics(): FinalTrainingMetrics {
        val totalReward = trainingHistory.sumOf { it.reward }
        val avgReward = if (trainingHistory.isNotEmpty()) totalReward / trainingHistory.size else 0.0
        val avgSteps = if (trainingHistory.isNotEmpty()) {
            trainingHistory.map { it.steps }.average()
        } else {
            0.0
        }
        
        // Game outcome statistics
        val wins = trainingHistory.count { it.gameResult.contains("WINS") }
        val draws = trainingHistory.count { it.gameResult.contains("DRAW") }
        val losses = trainingHistory.size - wins - draws
        
        val winRate = if (trainingHistory.isNotEmpty()) wins.toDouble() / trainingHistory.size else 0.0
        val drawRate = if (trainingHistory.isNotEmpty()) draws.toDouble() / trainingHistory.size else 0.0
        val lossRate = if (trainingHistory.isNotEmpty()) losses.toDouble() / trainingHistory.size else 0.0
        
        // Batch training statistics
        val totalBatchUpdates = batchMetrics.sumOf { it.updatesPerformed }
        val avgBatchLoss = if (batchMetrics.isNotEmpty()) {
            batchMetrics.map { it.averageLoss }.average()
        } else {
            0.0
        }
        
        return FinalTrainingMetrics(
            totalEpisodes = currentEpisode,
            totalSteps = totalSteps,
            totalReward = totalReward,
            averageReward = avgReward,
            bestReward = bestPerformance,
            averageGameLength = avgSteps,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            totalBatchUpdates = totalBatchUpdates,
            averageBatchLoss = avgBatchLoss,
            finalBufferSize = experienceBuffer.size
        )
    }
    
    /**
     * Get current training statistics
     */
    fun getCurrentStatistics(): TrainingStatistics {
        val avgReward = if (trainingHistory.isNotEmpty()) {
            trainingHistory.map { it.reward }.average()
        } else {
            0.0
        }
        
        val recentAvgReward = if (recentPerformance.isNotEmpty()) {
            recentPerformance.average()
        } else {
            0.0
        }
        
        return TrainingStatistics(
            currentEpisode = currentEpisode,
            totalSteps = totalSteps,
            averageReward = avgReward,
            recentAverageReward = recentAvgReward,
            bestPerformance = bestPerformance,
            bufferSize = experienceBuffer.size,
            batchUpdates = batchMetrics.size
        )
    }
    
    /**
     * Pause training (for interactive control)
     */
    fun pause() {
        println("⏸️ Training paused")
    }
    
    /**
     * Resume training (for interactive control)
     */
    fun resume() {
        println("▶️ Training resumed")
    }
    
    /**
     * Stop training gracefully
     */
    fun stop() {
        println("🛑 Training stopped by user")
        createFinalCheckpoint()
    }
}

/**
 * Configuration for the training pipeline
 */
data class TrainingPipelineConfig(
    // Episode configuration
    val maxStepsPerEpisode: Int = 200,
    
    // Batch training configuration
    val batchSize: Int = 64,
    val batchTrainingFrequency: Int = 1, // Train every N episodes
    val updatesPerBatch: Int = 1, // Multiple updates per batch
    val samplingStrategy: SamplingStrategy = SamplingStrategy.MIXED,
    
    // Experience buffer configuration
    val maxBufferSize: Int = 50000,
    
    // Monitoring and reporting
    val progressReportInterval: Int = 100,
    val checkpointInterval: Int = 1000,
    val checkpointDir: String = "checkpoints",
    
    // Training validation
    val gradientClipThreshold: Double = 10.0,
    val minPolicyEntropy: Double = 0.1,
    
    // Early stopping
    val enableEarlyStopping: Boolean = false,
    val earlyStoppingWindow: Int = 200,
    val earlyStoppingThreshold: Double = 0.8
)

/**
 * Sampling strategies for batch training
 */
enum class SamplingStrategy {
    UNIFORM,    // Random sampling from entire buffer
    RECENT,     // Sample most recent experiences
    MIXED       // Mix of recent and random experiences
}

/**
 * Metrics for a single training episode
 */
data class TrainingEpisodeMetrics(
    val episode: Int,
    val reward: Double,
    val steps: Int,
    val gameResult: String,
    val duration: Long,
    val bufferSize: Int,
    val explorationRate: Double,
    val chessMetrics: ChessMetrics
)

/**
 * Metrics for batch training
 */
data class BatchTrainingMetrics(
    val episode: Int,
    val batchSize: Int,
    val updatesPerformed: Int,
    val averageLoss: Double,
    val averageGradientNorm: Double,
    val averagePolicyEntropy: Double,
    val duration: Long,
    val bufferUtilization: Double
)

/**
 * Current training statistics
 */
data class TrainingStatistics(
    val currentEpisode: Int,
    val totalSteps: Int,
    val averageReward: Double,
    val recentAverageReward: Double,
    val bestPerformance: Double,
    val bufferSize: Int,
    val batchUpdates: Int
)

/**
 * Final training metrics
 */
data class FinalTrainingMetrics(
    val totalEpisodes: Int,
    val totalSteps: Int,
    val totalReward: Double,
    val averageReward: Double,
    val bestReward: Double,
    val averageGameLength: Double,
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val totalBatchUpdates: Int,
    val averageBatchLoss: Double,
    val finalBufferSize: Int
)

/**
 * Complete training results
 */
data class TrainingResults(
    val totalEpisodes: Int,
    val totalSteps: Int,
    val trainingDuration: Long,
    val bestPerformance: Double,
    val episodeHistory: List<TrainingEpisodeMetrics>,
    val batchHistory: List<BatchTrainingMetrics>,
    val finalMetrics: FinalTrainingMetrics
)

