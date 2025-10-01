package com.chessrl.integration.backend

import com.chessrl.integration.*
import com.chessrl.rl.*
import com.chessrl.integration.logging.ChessRLLogger

/**
 * DL4J-specific chess agent implementation
 * Uses DL4J NetworkAdapter with identical behavior to manual backend
 */
class Dl4jChessAgent(
    private val algorithm: RLAlgorithm<DoubleArray, Int>,
    private val explorationStrategy: ExplorationStrategy<Int>,
    private val qNetworkAdapter: NetworkAdapter,
    private val targetNetworkAdapter: NetworkAdapter,
    private val config: ChessAgentConfig
) {
    
    private val logger = ChessRLLogger.forComponent("Dl4jChessAgent")
    private val experienceBuffer = mutableListOf<Experience<DoubleArray, Int>>()
    private var episodeCount = 0
    private var totalReward = 0.0
    private var episodeReward = 0.0
    private var lastUpdate: PolicyUpdateResult? = null
    
    private val explorationRandom: kotlin.random.Random = try {
        SeedManager.getExplorationRandom()
    } catch (_: Throwable) {
        kotlin.random.Random.Default
    }
    
    fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        val actionValues = algorithm.getActionValues(state, validActions)
        return explorationStrategy.selectAction(validActions, actionValues, explorationRandom)
    }
    
    fun learn(experience: Experience<DoubleArray, Int>) {
        episodeReward += experience.reward
        lastUpdate = algorithm.updatePolicy(listOf(experience))
        if (experience.done) {
            episodeCount++
            totalReward += episodeReward
            episodeReward = 0.0
        }
    }
    
    fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return algorithm.getActionValues(state, actions)
    }
    
    fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return algorithm.getActionProbabilities(state, actions)
    }
    
    fun getTrainingMetrics(): SimpleRLMetrics {
        val averageReward = if (episodeCount > 0) totalReward / episodeCount else 0.0
        val bufferSize = if (algorithm is DQNAlgorithm) algorithm.getReplaySize() else 0
        return SimpleRLMetrics(
            averageReward = averageReward,
            explorationRate = explorationStrategy.getExplorationRate(),
            experienceBufferSize = bufferSize
        )
    }
    
    fun forceUpdate() {
        if (experienceBuffer.isNotEmpty()) {
            algorithm.updatePolicy(experienceBuffer.toList())
        }
    }

    fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        val result = algorithm.updatePolicy(experiences)
        lastUpdate = result
        return result
    }
    
    fun save(path: String) {
        try {
            val dl4jPath = if (path.endsWith(".zip")) path else "$path.zip"
            qNetworkAdapter.save(dl4jPath)
            logger.info("Saved DL4J model to $dl4jPath")
        } catch (e: Exception) {
            logger.error("Failed to save DL4J model: ${e.message}")
            throw e
        }
    }
    
    fun load(path: String) {
        try {
            val actualPath = if (path.endsWith(".zip")) {
                path
            } else {
                val zipPath = "$path.zip"
                if (java.io.File(zipPath).exists()) zipPath else path
            }
            
            qNetworkAdapter.load(actualPath)
            logger.info("Loaded DL4J model from $actualPath")
            
            try {
                qNetworkAdapter.copyWeightsTo(targetNetworkAdapter)
                logger.debug("Synchronized weights to target network")
            } catch (e: Exception) {
                logger.warn("Failed to sync weights to target network: ${e.message}")
            }
        } catch (e: Exception) {
            logger.error("Failed to load DL4J model: ${e.message}")
            throw e
        }
    }
    
    fun reset() {
        experienceBuffer.clear()
        episodeCount = 0
        totalReward = 0.0
        episodeReward = 0.0
    }
    
    fun setExplorationRate(rate: Double) {
        if (explorationStrategy is EpsilonGreedyStrategy) {
            explorationStrategy.setEpsilon(rate)
        }
    }

    fun setNextActionProvider(provider: (DoubleArray) -> List<Int>) {
        if (algorithm is DQNAlgorithm) {
            algorithm.setNextActionProvider(provider)
        }
    }

    fun getLastUpdate(): PolicyUpdateResult? = lastUpdate
}

/**
 * Seeded DL4J-specific chess agent implementation
 */
class SeededDl4jChessAgent(
    private val algorithm: RLAlgorithm<DoubleArray, Int>,
    private val explorationStrategy: ExplorationStrategy<Int>,
    private val qNetworkAdapter: NetworkAdapter,
    private val targetNetworkAdapter: NetworkAdapter,
    private val config: ChessAgentConfig,
    private val seedManager: SeedManager
) {
    
    private val logger = ChessRLLogger.forComponent("SeededDl4jChessAgent")
    private val experienceBuffer = mutableListOf<Experience<DoubleArray, Int>>()
    private var episodeCount = 0
    private var totalReward = 0.0
    private var episodeReward = 0.0
    private var lastUpdate: PolicyUpdateResult? = null
    
    private val explorationRandom = seedManager.getExplorationRandom()
    
    fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        val actionValues = algorithm.getActionValues(state, validActions)
        return explorationStrategy.selectAction(validActions, actionValues, explorationRandom)
    }
    
    fun learn(experience: Experience<DoubleArray, Int>) {
        episodeReward += experience.reward
        lastUpdate = algorithm.updatePolicy(listOf(experience))
        if (experience.done) {
            episodeCount++
            totalReward += episodeReward
            episodeReward = 0.0
        }
    }
    
    fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return algorithm.getActionValues(state, actions)
    }
    
    fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return algorithm.getActionProbabilities(state, actions)
    }
    
    fun getTrainingMetrics(): SimpleRLMetrics {
        val averageReward = if (episodeCount > 0) totalReward / episodeCount else 0.0
        val bufferSize = if (algorithm is DQNAlgorithm) algorithm.getReplaySize() else 0
        return SimpleRLMetrics(
            averageReward = averageReward,
            explorationRate = explorationStrategy.getExplorationRate(),
            experienceBufferSize = bufferSize
        )
    }
    
    fun forceUpdate() {
        if (experienceBuffer.isNotEmpty()) {
            algorithm.updatePolicy(experienceBuffer.toList())
        }
    }

    fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        val result = algorithm.updatePolicy(experiences)
        lastUpdate = result
        return result
    }
    
    fun save(path: String) {
        try {
            val dl4jPath = if (path.endsWith(".zip")) path else "$path.zip"
            qNetworkAdapter.save(dl4jPath)
            logger.info("Saved seeded DL4J model to $dl4jPath")
        } catch (e: Exception) {
            logger.error("Failed to save seeded DL4J model: ${e.message}")
            throw e
        }
    }
    
    fun load(path: String) {
        try {
            val actualPath = if (path.endsWith(".zip")) {
                path
            } else {
                val zipPath = "$path.zip"
                if (java.io.File(zipPath).exists()) zipPath else path
            }
            
            qNetworkAdapter.load(actualPath)
            logger.info("Loaded seeded DL4J model from $actualPath")
            
            try {
                qNetworkAdapter.copyWeightsTo(targetNetworkAdapter)
                logger.debug("Synchronized weights to target network")
            } catch (e: Exception) {
                logger.warn("Failed to sync weights to target network: ${e.message}")
            }
        } catch (e: Exception) {
            logger.error("Failed to load seeded DL4J model: ${e.message}")
            throw e
        }
    }
    
    fun reset() {
        experienceBuffer.clear()
        episodeCount = 0
        totalReward = 0.0
        episodeReward = 0.0
    }
    
    fun setExplorationRate(rate: Double) {
        if (explorationStrategy is EpsilonGreedyStrategy) {
            explorationStrategy.setEpsilon(rate)
        }
    }

    fun setNextActionProvider(provider: (DoubleArray) -> List<Int>) {
        if (algorithm is DQNAlgorithm) {
            algorithm.setNextActionProvider(provider)
        }
    }

    fun getLastUpdate(): PolicyUpdateResult? = lastUpdate
}