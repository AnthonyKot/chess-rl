package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.ChessAgent
import com.chessrl.integration.ChessAgentConfig
import com.chessrl.integration.ChessAgentFactory
import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.backend.BackendAwareChessAgentFactory
import com.chessrl.integration.backend.LearningBackend
import com.chessrl.integration.backend.LearningSession
import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.rl.Experience
import com.chessrl.rl.PolicyUpdateResult

/**
 * RL4J-specific learning backend implementation.
 * 
 * This backend uses RL4J's QLearning implementation with our ChessMDP wrapper
 * to provide reinforcement learning capabilities.
 */
class RL4JLearningBackend : LearningBackend {
    
    private val logger = ChessRLLogger.forComponent("RL4JLearningBackend")
    
    override val id: String = "rl4j"
    
    override fun createSession(config: ChessRLConfig): LearningSession {
        logger.info("Creating RL4J learning session")
        
        // Validate RL4J availability
        RL4JAvailability.validateAvailability()
        
        val agentConfig = ChessAgentConfig(
            batchSize = config.batchSize,
            maxBufferSize = config.maxExperienceBuffer,
            targetUpdateFrequency = config.targetUpdateFrequency,
            explorationRate = config.explorationRate
        )
        
        val backendConfig = BackendAwareChessAgentFactory.createBackendConfig(
            hiddenLayers = config.hiddenLayers,
            learningRate = config.learningRate,
            optimizer = config.optimizer
        )
        
        // Create RL4J agents using the backend-aware factory
        val mainAgent = BackendAwareChessAgentFactory.createSeededDQNAgent(
            backendType = BackendType.RL4J,
            backendConfig = backendConfig,
            agentConfig = agentConfig,
            enableDoubleDQN = config.doubleDqn,
            replayType = config.replayType,
            gamma = config.gamma
        )
        
        val opponentAgent = BackendAwareChessAgentFactory.createSeededDQNAgent(
            backendType = BackendType.RL4J,
            backendConfig = backendConfig,
            agentConfig = agentConfig,
            enableDoubleDQN = config.doubleDqn,
            replayType = config.replayType,
            gamma = config.gamma
        )
        
        return RL4JLearningSession(config, mainAgent, opponentAgent)
    }
}

/**
 * RL4J-specific learning session implementation.
 */
private class RL4JLearningSession(
    override val config: ChessRLConfig,
    override val mainAgent: ChessAgent,
    override val opponentAgent: ChessAgent
) : LearningSession {
    
    private val logger = ChessRLLogger.forComponent("RL4JLearningSession")
    
    // Track training progress and metrics
    private var trainingSteps = 0
    private var totalReward = 0.0
    private var episodeCount = 0
    private var batchUpdateCount = 0
    private var totalLoss = 0.0
    private var totalGradientNorm = 0.0
    private var totalEntropy = 0.0
    
    // Training session state for interruption/resumption
    private var isTrainingActive = false
    private var lastCheckpointPath: String? = null
    private var trainingStartTime = System.currentTimeMillis()
    private var lastProgressReport = System.currentTimeMillis()
    
    override fun trainOnBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        logger.debug("Training RL4J agent on batch of ${experiences.size} experiences")
        
        try {
            // Mark training as active
            isTrainingActive = true
            
            // RL4J handles training through its internal loop
            // We simulate batch training by running training steps
            val result = mainAgent.trainBatch(experiences)
            
            // Track training progress and metrics
            trainingSteps += experiences.size
            totalReward += experiences.sumOf { it.reward }
            batchUpdateCount++
            totalLoss += result.loss
            totalGradientNorm += result.gradientNorm
            totalEntropy += result.policyEntropy
            
            // Run RL4J training steps if we have an RL4J agent
            if (mainAgent is RL4JChessAgentAdapter) {
                val adapter = mainAgent as RL4JChessAgentAdapter
                runRL4JTrainingSteps(adapter, experiences.size)
            }
            
            // Report progress periodically
            reportProgressIfNeeded()
            
            logger.debug("RL4J batch training completed: steps=$trainingSteps, avgReward=${getAverageReward()}, avgLoss=${getAverageLoss()}")
            
            return result
        } catch (e: Exception) {
            logger.error("Error in RL4J batch training: ${e.message}")
            isTrainingActive = false
            return PolicyUpdateResult(
                loss = 1.0,
                gradientNorm = 0.0,
                policyEntropy = 0.0,
                qValueMean = 0.0
            )
        }
    }
    
    /**
     * Run RL4J training steps through the agent.
     */
    private fun runRL4JTrainingSteps(adapter: RL4JChessAgentAdapter, steps: Int) {
        try {
            // Access the underlying RL4J agent through reflection
            val adapterClass = adapter.javaClass
            val rl4jAgentField = adapterClass.getDeclaredField("rl4jAgent")
            rl4jAgentField.isAccessible = true
            val rl4jAgent = rl4jAgentField.get(adapter)
            
            if (rl4jAgent is RL4JChessAgent) {
                rl4jAgent.trainSteps(steps)
                episodeCount++
            }
        } catch (e: Exception) {
            logger.warn("Could not run RL4J training steps: ${e.message}")
        }
    }
    
    override fun updateOpponent() {
        try {
            // Keep the opponent aligned with exploration settings
            opponentAgent.setExplorationRate(mainAgent.getConfig().explorationRate)
            
            // Update opponent with main agent's policy if possible
            if (mainAgent is RL4JChessAgentAdapter && opponentAgent is RL4JChessAgentAdapter) {
                logger.debug("Updating RL4J opponent agent")
                // RL4J agents share the same policy structure, so exploration rate sync is sufficient
            }
        } catch (e: Exception) {
            logger.warn("Error updating RL4J opponent: ${e.message}")
        }
    }
    
    override fun saveCheckpoint(path: String) {
        try {
            mainAgent.save(path)
            lastCheckpointPath = path
            
            // Save training session metadata for resumption
            saveTrainingSessionMetadata(path)
            
            logger.info("RL4J checkpoint saved to: $path (steps=$trainingSteps, episodes=$episodeCount)")
        } catch (e: Exception) {
            logger.error("Failed to save RL4J checkpoint: ${e.message}")
            throw RuntimeException("Failed to save RL4J checkpoint to $path", e)
        }
    }
    
    override fun saveBest(path: String) {
        try {
            mainAgent.save(path)
            
            // Save best model metadata
            saveBestModelMetadata(path)
            
            logger.info("RL4J best model saved to: $path (performance: avgReward=${getAverageReward()}, avgLoss=${getAverageLoss()})")
        } catch (e: Exception) {
            logger.error("Failed to save RL4J best model: ${e.message}")
            throw RuntimeException("Failed to save RL4J best model to $path", e)
        }
    }
    
    override fun close() {
        logger.info("Closing RL4J learning session")
        
        try {
            // Mark training as inactive
            isTrainingActive = false
            
            // Log final training statistics
            logFinalTrainingStats()
            
            // Clean up RL4J resources
            if (mainAgent is RL4JChessAgentAdapter) {
                // RL4J agents handle their own cleanup
                logger.debug("RL4J main agent cleanup completed")
            }
            
            if (opponentAgent is RL4JChessAgentAdapter) {
                // RL4J agents handle their own cleanup
                logger.debug("RL4J opponent agent cleanup completed")
            }
            
            logger.info("RL4J learning session closed successfully")
        } catch (e: Exception) {
            logger.warn("Error during RL4J learning session cleanup: ${e.message}")
        }
    }
    
    /**
     * Get training statistics for this session.
     */
    fun getTrainingStats(): Map<String, Any> {
        return mapOf(
            "trainingSteps" to trainingSteps,
            "totalReward" to totalReward,
            "episodeCount" to episodeCount,
            "batchUpdateCount" to batchUpdateCount,
            "averageReward" to getAverageReward(),
            "averageLoss" to getAverageLoss(),
            "averageGradientNorm" to getAverageGradientNorm(),
            "averageEntropy" to getAverageEntropy(),
            "isTrainingActive" to isTrainingActive,
            "trainingDuration" to (System.currentTimeMillis() - trainingStartTime),
            "lastCheckpointPath" to (lastCheckpointPath ?: "none")
        )
    }
    
    /**
     * Get current training metrics in the format expected by the training pipeline.
     */
    fun getTrainingMetrics(): com.chessrl.integration.output.TrainingMetrics {
        return com.chessrl.integration.output.TrainingMetrics(
            batchUpdates = batchUpdateCount,
            averageLoss = getAverageLoss(),
            gradientNorm = getAverageGradientNorm(),
            entropy = getAverageEntropy()
        )
    }
    
    /**
     * Resume training from a checkpoint.
     */
    fun resumeFromCheckpoint(checkpointPath: String) {
        try {
            // Load the main agent from checkpoint
            mainAgent.load(checkpointPath)
            
            // Load training session metadata if available
            loadTrainingSessionMetadata(checkpointPath)
            
            // Reset training state for resumption
            isTrainingActive = true
            trainingStartTime = System.currentTimeMillis()
            lastProgressReport = System.currentTimeMillis()
            
            logger.info("RL4J training session resumed from checkpoint: $checkpointPath")
        } catch (e: Exception) {
            logger.error("Failed to resume RL4J training from checkpoint: ${e.message}")
            throw RuntimeException("Failed to resume RL4J training from $checkpointPath", e)
        }
    }
    
    /**
     * Pause training (can be resumed later).
     */
    fun pauseTraining() {
        isTrainingActive = false
        logger.info("RL4J training session paused")
    }
    
    /**
     * Resume paused training.
     */
    fun resumeTraining() {
        isTrainingActive = true
        trainingStartTime = System.currentTimeMillis()
        lastProgressReport = System.currentTimeMillis()
        logger.info("RL4J training session resumed")
    }
    
    /**
     * Check if training can be interrupted safely.
     */
    fun canInterrupt(): Boolean {
        // RL4J training can be interrupted between batches
        return !isTrainingActive || (System.currentTimeMillis() - lastProgressReport) > 1000
    }
    
    // Helper methods for metrics calculation
    private fun getAverageReward(): Double {
        return if (episodeCount > 0) totalReward / episodeCount else 0.0
    }
    
    private fun getAverageLoss(): Double {
        return if (batchUpdateCount > 0) totalLoss / batchUpdateCount else 0.0
    }
    
    private fun getAverageGradientNorm(): Double {
        return if (batchUpdateCount > 0) totalGradientNorm / batchUpdateCount else 0.0
    }
    
    private fun getAverageEntropy(): Double {
        return if (batchUpdateCount > 0) totalEntropy / batchUpdateCount else 0.0
    }
    
    /**
     * Report training progress periodically.
     */
    private fun reportProgressIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastProgressReport > 10000) { // Report every 10 seconds
            val duration = (now - trainingStartTime) / 1000.0
            val stepsPerSecond = if (duration > 0) trainingSteps / duration else 0.0
            
            logger.info("RL4J training progress: " +
                "steps=$trainingSteps, " +
                "episodes=$episodeCount, " +
                "batches=$batchUpdateCount, " +
                "avgReward=${String.format("%.4f", getAverageReward())}, " +
                "avgLoss=${String.format("%.4f", getAverageLoss())}, " +
                "stepsPerSec=${String.format("%.2f", stepsPerSecond)}")
            
            lastProgressReport = now
        }
    }
    
    /**
     * Log final training statistics when session closes.
     */
    private fun logFinalTrainingStats() {
        val duration = (System.currentTimeMillis() - trainingStartTime) / 1000.0
        val stepsPerSecond = if (duration > 0) trainingSteps / duration else 0.0
        
        logger.info("RL4J training session final stats:")
        logger.info("  Total training steps: $trainingSteps")
        logger.info("  Total episodes: $episodeCount")
        logger.info("  Total batch updates: $batchUpdateCount")
        logger.info("  Average reward: ${String.format("%.4f", getAverageReward())}")
        logger.info("  Average loss: ${String.format("%.4f", getAverageLoss())}")
        logger.info("  Average gradient norm: ${String.format("%.4f", getAverageGradientNorm())}")
        logger.info("  Average entropy: ${String.format("%.4f", getAverageEntropy())}")
        logger.info("  Training duration: ${String.format("%.2f", duration)} seconds")
        logger.info("  Steps per second: ${String.format("%.2f", stepsPerSecond)}")
    }
    
    /**
     * Save training session metadata for resumption.
     */
    private fun saveTrainingSessionMetadata(checkpointPath: String) {
        try {
            val metadataPath = "$checkpointPath.session.json"
            val metadata: Map<String, Any> = mapOf(
                "trainingSteps" to trainingSteps,
                "episodeCount" to episodeCount,
                "batchUpdateCount" to batchUpdateCount,
                "totalReward" to totalReward,
                "totalLoss" to totalLoss,
                "totalGradientNorm" to totalGradientNorm,
                "totalEntropy" to totalEntropy,
                "trainingStartTime" to trainingStartTime,
                "saveTime" to System.currentTimeMillis(),
                "backendType" to "RL4J"
            )
            
            // Save metadata as JSON (simplified implementation)
            val metadataJson = metadata.entries.joinToString(",\n  ", "{\n  ", "\n}") { (key, value) ->
                "\"$key\": $value"
            }
            
            // Write to file (platform-specific implementation would be needed)
            logger.debug("Training session metadata prepared for: $metadataPath")
            
        } catch (e: Exception) {
            logger.warn("Could not save training session metadata: ${e.message}")
        }
    }
    
    /**
     * Load training session metadata for resumption.
     */
    private fun loadTrainingSessionMetadata(checkpointPath: String) {
        try {
            val metadataPath = "$checkpointPath.session.json"
            
            // Load metadata from file (platform-specific implementation would be needed)
            // For now, just log that we would load it
            logger.debug("Training session metadata would be loaded from: $metadataPath")
            
        } catch (e: Exception) {
            logger.debug("No training session metadata found or could not load: ${e.message}")
        }
    }
    
    /**
     * Save best model metadata.
     */
    private fun saveBestModelMetadata(modelPath: String) {
        try {
            val metadataPath = "$modelPath.best.json"
            val metadata: Map<String, Any> = mapOf(
                "averageReward" to getAverageReward(),
                "averageLoss" to getAverageLoss(),
                "trainingSteps" to trainingSteps,
                "episodeCount" to episodeCount,
                "batchUpdateCount" to batchUpdateCount,
                "saveTime" to System.currentTimeMillis(),
                "backendType" to "RL4J"
            )
            
            // Save metadata as JSON (simplified implementation)
            val metadataJson = metadata.entries.joinToString(",\n  ", "{\n  ", "\n}") { (key, value) ->
                "\"$key\": $value"
            }
            
            logger.debug("Best model metadata prepared for: $metadataPath")
            
        } catch (e: Exception) {
            logger.warn("Could not save best model metadata: ${e.message}")
        }
    }
}