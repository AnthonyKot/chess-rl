package com.chessrl.integration

import com.chessrl.rl.*

/**
 * Training pipeline that integrates chess agents with the RL training framework
 * This bridges the gap between self-play experience collection and neural network training
 */
class ChessTrainingPipeline(
    private val agent: ChessAgent,
    private val environment: ChessEnvironment,
    private val config: TrainingPipelineConfig = TrainingPipelineConfig()
) {
    
    private val experienceBuffer = mutableListOf<Experience<DoubleArray, Int>>()
    private var episodeCount = 0
    private var totalReward = 0.0
    private val trainingMetrics = mutableListOf<TrainingEpisodeMetrics>()
    private var emaGradNorm: Double? = null
    private var emaEntropy: Double? = null
    
    /**
     * Run a single training episode
     */
    fun runEpisode(): TrainingEpisodeResult {
        val startTime = getCurrentTimeMillis()
        
        // Reset environment
        var state = environment.reset()
        var episodeReward = 0.0
        var stepCount = 0
        val episodeExperiences = mutableListOf<Experience<DoubleArray, Int>>()
        
        // Run episode
        while (!environment.isTerminal(state) && stepCount < config.maxStepsPerEpisode) {
            // Get valid actions
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) break
            
            // Agent selects action
            val action = agent.selectAction(state, validActions)
            
            // Take step
            val stepResult = environment.step(action)
            
            // Create experience
            val experience = Experience(
                state = state,
                action = action,
                reward = stepResult.reward,
                nextState = stepResult.nextState,
                done = stepResult.done
            )
            
            // Add to buffers
            episodeExperiences.add(experience)
            experienceBuffer.add(experience)
            
            // Learn from experience
            agent.learn(experience)
            
            // Update state and metrics
            state = stepResult.nextState
            episodeReward += stepResult.reward
            stepCount++
            
            if (stepResult.done) break
        }
        
        // Batch training if enough experiences
        if (experienceBuffer.size >= config.batchSize && 
            episodeCount % config.batchTrainingFrequency == 0) {
            performBatchTraining()
        }
        
        // Clean up experience buffer if too large
        if (experienceBuffer.size > config.maxBufferSize) {
            val excessCount = experienceBuffer.size - config.maxBufferSize
            repeat(excessCount) {
                experienceBuffer.removeAt(0) // Remove oldest
            }
        }
        
        episodeCount++
        totalReward += episodeReward
        
        val endTime = getCurrentTimeMillis()
        val episodeDuration = endTime - startTime
        
        val episodeMetrics = TrainingEpisodeMetrics(
            episode = episodeCount,
            reward = episodeReward,
            steps = stepCount,
            duration = episodeDuration,
            averageReward = totalReward / episodeCount,
            agentMetrics = agent.getTrainingMetrics()
        )
        
        trainingMetrics.add(episodeMetrics)
        
        // Progress reporting
        if (episodeCount % config.progressReportInterval == 0) {
            reportProgress()
        }
        
        return TrainingEpisodeResult(
            episode = episodeCount,
            reward = episodeReward,
            steps = stepCount,
            duration = episodeDuration,
            experiences = episodeExperiences,
            metrics = episodeMetrics
        )
    }

    init {
        // Provide next-state valid actions to agent for algorithms that can consume it (e.g., DQN masking)
        runCatching { agent.setNextActionProvider(environment::getValidActions) }
    }
    
    /**
     * Run multiple training episodes
     */
    fun runTraining(numEpisodes: Int): TrainingResults {
        val startTime = getCurrentTimeMillis()
        val episodeResults = mutableListOf<TrainingEpisodeResult>()
        
        println("🚀 Starting training pipeline: $numEpisodes episodes")
        
        repeat(numEpisodes) { episode ->
            try {
                val result = runEpisode()
                episodeResults.add(result)
            } catch (e: Exception) {
                println("⚠️ Episode ${episode + 1} failed: ${e.message}")
            }
        }
        
        val endTime = getCurrentTimeMillis()
        val totalDuration = endTime - startTime
        
        println("✅ Training completed: $numEpisodes episodes in ${totalDuration}ms")
        
        return TrainingResults(
            totalEpisodes = episodeResults.size,
            totalDuration = totalDuration,
            episodeResults = episodeResults,
            finalMetrics = calculateFinalMetrics(),
            agentConfig = agent.getConfig()
        )
    }
    
    /**
     * Perform batch training on collected experiences
     */
    private fun performBatchTraining() {
        // Sample experiences based on strategy
        val batch: List<Experience<DoubleArray, Int>> = when (config.samplingStrategy) {
            SamplingStrategy.RECENT -> experienceBuffer.takeLast(config.batchSize)
            SamplingStrategy.RANDOM -> experienceBuffer.shuffled().take(config.batchSize)
            SamplingStrategy.MIXED -> {
                val recentCount = config.batchSize / 2
                val randomCount = config.batchSize - recentCount
                val recent = experienceBuffer.takeLast(recentCount)
                val random = experienceBuffer.dropLast(recentCount).shuffled().take(randomCount)
                recent + random
            }
        }

        // Train agent on batch using real batch API
        val result = agent.trainBatch(batch)
        // Compute simple EMA for gradient norm to smooth noise
        emaGradNorm = if (emaGradNorm == null) result.gradientNorm else 0.2 * result.gradientNorm + 0.8 * emaGradNorm!!
        emaEntropy = if (emaEntropy == null) result.policyEntropy else 0.2 * result.policyEntropy + 0.8 * emaEntropy!!

        // Optional: lightweight progress print for batch metrics (raw and smoothed grad)
        println(
            "   Batch update: " +
            "loss=${"%.4f".format(result.loss)}, " +
            "entropy=${"%.4f".format(result.policyEntropy)} (ema=${"%.4f".format(emaEntropy)}), " +
            "grad=${"%.4f".format(result.gradientNorm)} (ema=${"%.4f".format(emaGradNorm)})"
        )
    }
    
    /**
     * Report training progress
     */
    private fun reportProgress() {
        val recentMetrics = trainingMetrics.takeLast(config.progressReportInterval)
        val avgReward = recentMetrics.map { it.reward }.average()
        val avgSteps = recentMetrics.map { it.steps }.average()
        val avgDuration = recentMetrics.map { it.duration }.average()
        
        println("📊 Training Progress - Episode $episodeCount:")
        println("   Recent Avg Reward: $avgReward")
        println("   Recent Avg Steps: $avgSteps")
        println("   Recent Avg Duration: ${avgDuration}ms")
        val agentMetrics = agent.getTrainingMetrics()
        println("   Experience Buffer: ${experienceBuffer.size}/${config.maxBufferSize}")
        println("   Agent Exploration: ${agentMetrics.explorationRate}")
        // If available, show latest training batch metrics
        if (agentMetrics.averageLoss != 0.0 || agentMetrics.policyEntropy != 0.0) {
            println("   Last Batch: loss=${agentMetrics.averageLoss}, entropy=${agentMetrics.policyEntropy}")
        }
    }
    
    /**
     * Calculate final training metrics
     */
    private fun calculateFinalMetrics(): TrainingFinalMetrics {
        if (trainingMetrics.isEmpty()) {
            return TrainingFinalMetrics(
                totalEpisodes = 0,
                averageReward = 0.0,
                bestReward = 0.0,
                averageSteps = 0.0,
                totalExperiences = 0,
                convergenceEpisode = null
            )
        }
        
        val rewards = trainingMetrics.map { it.reward }
        val steps = trainingMetrics.map { it.steps }
        
        return TrainingFinalMetrics(
            totalEpisodes = trainingMetrics.size,
            averageReward = rewards.average(),
            bestReward = rewards.maxOrNull() ?: 0.0,
            averageSteps = steps.average(),
            totalExperiences = experienceBuffer.size,
            convergenceEpisode = detectConvergence()
        )
    }
    
    /**
     * Detect if training has converged
     */
    private fun detectConvergence(): Int? {
        if (trainingMetrics.size < 100) return null
        
        val recentRewards = trainingMetrics.takeLast(50).map { it.reward }
        val earlierRewards = trainingMetrics.dropLast(50).takeLast(50).map { it.reward }
        
        val recentAvg = recentRewards.average()
        val earlierAvg = earlierRewards.average()
        
        // Consider converged if improvement is less than 1%
        return if (kotlin.math.abs(recentAvg - earlierAvg) / kotlin.math.abs(earlierAvg) < 0.01) {
            trainingMetrics.size - 50
        } else {
            null
        }
    }
    
    /**
     * Get current training state
     */
    fun getTrainingState(): TrainingState {
        return TrainingState(
            episodeCount = episodeCount,
            totalReward = totalReward,
            averageReward = if (episodeCount > 0) totalReward / episodeCount else 0.0,
            experienceBufferSize = experienceBuffer.size,
            agentMetrics = agent.getTrainingMetrics()
        )
    }
    
    /**
     * Reset training state
     */
    fun reset() {
        experienceBuffer.clear()
        episodeCount = 0
        totalReward = 0.0
        trainingMetrics.clear()
        agent.reset()
    }
    
    /**
     * Get the agent being trained
     */
    fun getAgent(): ChessAgent = agent
    
    /**
     * Get the training environment
     */
    fun getEnvironment(): ChessEnvironment = environment
}

/**
 * Configuration for training pipeline
 */
data class TrainingPipelineConfig(
    val maxStepsPerEpisode: Int = 200,
    val batchSize: Int = 32,
    val batchTrainingFrequency: Int = 1,
    val maxBufferSize: Int = 10000,
    val progressReportInterval: Int = 10,
    val samplingStrategy: SamplingStrategy = SamplingStrategy.MIXED
)

/**
 * Sampling strategies for experience replay
 */
enum class SamplingStrategy {
    RECENT,    // Use most recent experiences
    RANDOM,    // Random sampling
    MIXED      // Mix of recent and random
}

/**
 * Metrics for a single training episode
 */
data class TrainingEpisodeMetrics(
    val episode: Int,
    val reward: Double,
    val steps: Int,
    val duration: Long,
    val averageReward: Double,
    val agentMetrics: ChessAgentMetrics
)

/**
 * Result of a single training episode
 */
data class TrainingEpisodeResult(
    val episode: Int,
    val reward: Double,
    val steps: Int,
    val duration: Long,
    val experiences: List<Experience<DoubleArray, Int>>,
    val metrics: TrainingEpisodeMetrics
)

/**
 * Complete training results
 */
data class TrainingResults(
    val totalEpisodes: Int,
    val totalDuration: Long,
    val episodeResults: List<TrainingEpisodeResult>,
    val finalMetrics: TrainingFinalMetrics,
    val agentConfig: ChessAgentConfig
)

/**
 * Final training metrics summary
 */
data class TrainingFinalMetrics(
    val totalEpisodes: Int,
    val averageReward: Double,
    val bestReward: Double,
    val averageSteps: Double,
    val totalExperiences: Int,
    val convergenceEpisode: Int?
)

/**
 * Current training state
 */
data class TrainingState(
    val episodeCount: Int,
    val totalReward: Double,
    val averageReward: Double,
    val experienceBufferSize: Int,
    val agentMetrics: ChessAgentMetrics
)
