package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*
import com.chessrl.integration.backend.*

/**
 * Factory for creating chess agents with proper integration between
 * the RL framework and neural network implementations.
 * 
 * Now supports pluggable neural network backends via BackendAwareChessAgentFactory.
 * The manual backend (existing FeedforwardNetwork) remains the default.
 */
object ChessAgentFactory {
    
    /**
     * Create a DQN agent with default configuration
     * Uses manual backend by default for backward compatibility
     */
    fun createDQNAgent(
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        optimizer: String = "adam",
        explorationRate: Double = 0.1,
        config: ChessAgentConfig = ChessAgentConfig(),
        enableDoubleDQN: Boolean = false,
        replayType: String = "UNIFORM",
        gamma: Double = 0.99
    ): ChessAgent {
        val agentConfig = config.copy(explorationRate = explorationRate)
        // Use manual backend by default to preserve existing behavior
        val backendConfig = BackendAwareChessAgentFactory.createBackendConfig(
            hiddenLayers = hiddenLayers,
            learningRate = learningRate,
            batchSize = agentConfig.batchSize,
            optimizer = optimizer
        )
        
        return BackendAwareChessAgentFactory.createDQNAgent(
            backendType = BackendType.MANUAL,
            backendConfig = backendConfig,
            agentConfig = agentConfig,
            enableDoubleDQN = enableDoubleDQN,
            replayType = replayType,
            gamma = gamma
        )
    }
    
    /**
     * Create a DQN agent with specified neural network backend
     */
    fun createDQNAgent(
        backendType: BackendType,
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        optimizer: String = "adam",
        explorationRate: Double = 0.1,
        config: ChessAgentConfig = ChessAgentConfig(),
        enableDoubleDQN: Boolean = false,
        replayType: String = "UNIFORM",
        gamma: Double = 0.99
    ): ChessAgent {
        val agentConfig = config.copy(explorationRate = explorationRate)
        val backendConfig = BackendAwareChessAgentFactory.createBackendConfig(
            hiddenLayers = hiddenLayers,
            learningRate = learningRate,
            batchSize = agentConfig.batchSize,
            optimizer = optimizer
        )
        
        return BackendAwareChessAgentFactory.createDQNAgent(
            backendType = backendType,
            backendConfig = backendConfig,
            agentConfig = agentConfig,
            enableDoubleDQN = enableDoubleDQN,
            replayType = replayType,
            gamma = gamma
        )
    }
    
    
    /**
     * Create a seeded DQN agent for deterministic training
     * Uses manual backend by default for backward compatibility
     */
    fun createSeededDQNAgent(
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        optimizer: String = "adam",
        explorationRate: Double = 0.1,
        config: ChessAgentConfig = ChessAgentConfig(),
        seedManager: SeedManager = SeedManager.getInstance(),
        replayType: String = "UNIFORM",
        gamma: Double = 0.99,
        enableDoubleDQN: Boolean = false
    ): ChessAgent {
        val agentConfig = config.copy(explorationRate = explorationRate)
        // Use manual backend by default to preserve existing behavior
        val backendConfig = BackendAwareChessAgentFactory.createBackendConfig(
            hiddenLayers = hiddenLayers,
            learningRate = learningRate,
            batchSize = agentConfig.batchSize,
            optimizer = optimizer
        )
        
        return BackendAwareChessAgentFactory.createSeededDQNAgent(
            backendType = BackendType.MANUAL,
            backendConfig = backendConfig,
            agentConfig = agentConfig,
            seedManager = seedManager,
            enableDoubleDQN = enableDoubleDQN,
            replayType = replayType,
            gamma = gamma
        )
    }
    
    /**
     * Create a seeded DQN agent with specified neural network backend
     */
    fun createSeededDQNAgent(
        backendType: BackendType,
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        optimizer: String = "adam",
        explorationRate: Double = 0.1,
        config: ChessAgentConfig = ChessAgentConfig(),
        seedManager: SeedManager = SeedManager.getInstance(),
        replayType: String = "UNIFORM",
        gamma: Double = 0.99,
        enableDoubleDQN: Boolean = false
    ): ChessAgent {
        val agentConfig = config.copy(explorationRate = explorationRate)
        val backendConfig = BackendAwareChessAgentFactory.createBackendConfig(
            hiddenLayers = hiddenLayers,
            learningRate = learningRate,
            batchSize = agentConfig.batchSize,
            optimizer = optimizer
        )
        
        return BackendAwareChessAgentFactory.createSeededDQNAgent(
            backendType = backendType,
            backendConfig = backendConfig,
            agentConfig = agentConfig,
            seedManager = seedManager,
            enableDoubleDQN = enableDoubleDQN,
            replayType = replayType,
            gamma = gamma
        )
    }
    
}

/**
 * Adapter that wraps RealChessAgent to implement the ChessAgent interface
 */
class ChessAgentAdapter(
    private val realAgent: RealChessAgent,
    private val config: ChessAgentConfig
) : ChessAgent {
    
    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        val proposed = realAgent.selectAction(state, validActions)
        if (proposed in validActions) return proposed
        // Strict masking: fall back to a legal action only
        return try {
            val probs = realAgent.getActionProbabilities(state, validActions)
            if (probs.isNotEmpty()) {
                // Pick highest-prob legal action
                probs.maxByOrNull { it.value }?.key ?: validActions.first()
            } else {
                val q = realAgent.getQValues(state, validActions)
                if (q.isNotEmpty()) q.maxByOrNull { it.value }?.key ?: validActions.first() else validActions.first()
            }
        } catch (_: Throwable) {
            validActions.first()
        }
    }
    
    override fun learn(experience: Experience<DoubleArray, Int>) {
        realAgent.learn(experience)
    }
    
    override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return realAgent.getQValues(state, actions)
    }
    
    override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return realAgent.getActionProbabilities(state, actions)
    }
    
    override fun getTrainingMetrics(): ChessAgentMetrics {
        val simpleMetrics = realAgent.getTrainingMetrics()
        val last = realAgent.getLastUpdate()
        return ChessAgentMetrics(
            averageReward = simpleMetrics.averageReward,
            explorationRate = simpleMetrics.explorationRate,
            experienceBufferSize = simpleMetrics.experienceBufferSize,
            averageLoss = last?.loss ?: 0.0,
            policyEntropy = last?.policyEntropy ?: 0.0
        )
    }
    
    override fun forceUpdate() {
        realAgent.forceUpdate()
    }

    override fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        return realAgent.trainBatch(experiences)
    }
    
    override fun save(path: String) {
        realAgent.save(path)
    }
    
    override fun load(path: String) {
        realAgent.load(path)
    }
    
    override fun reset() {
        realAgent.reset()
    }
    
    override fun setExplorationRate(rate: Double) {
        realAgent.setExplorationRate(rate)
    }
    
    override fun getConfig(): ChessAgentConfig {
        return config
    }

    override fun setNextActionProvider(provider: (DoubleArray) -> List<Int>) {
        realAgent.setNextActionProvider(provider)
    }
}
