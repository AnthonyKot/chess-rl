package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*

/**
 * Factory for creating chess agents with proper integration between
 * the RL framework and neural network implementations
 */
object ChessAgentFactory {
    
    /**
     * Create a DQN agent with default configuration
     */
    fun createDQNAgent(
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        explorationRate: Double = 0.1,
        config: ChessAgentConfig = ChessAgentConfig()
    ): ChessAgent {
        return RealChessAgentFactory.createRealDQNAgent(
            inputSize = 776, // Chess state features
            outputSize = 4096, // Chess action space
            hiddenLayers = hiddenLayers,
            learningRate = learningRate,
            explorationRate = explorationRate,
            batchSize = config.batchSize,
            maxBufferSize = config.maxBufferSize
        ).let { realAgent ->
            ChessAgentAdapter(realAgent, config)
        }
    }
    
    /**
     * Create a Policy Gradient agent with default configuration
     */
    fun createPolicyGradientAgent(
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        temperature: Double = 1.0,
        config: ChessAgentConfig = ChessAgentConfig()
    ): ChessAgent {
        return RealChessAgentFactory.createRealPolicyGradientAgent(
            inputSize = 776,
            outputSize = 4096,
            hiddenLayers = hiddenLayers,
            learningRate = learningRate,
            temperature = temperature,
            batchSize = config.batchSize
        ).let { realAgent ->
            ChessAgentAdapter(realAgent, config)
        }
    }
    
    /**
     * Create a seeded DQN agent for deterministic training
     */
    fun createSeededDQNAgent(
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        explorationRate: Double = 0.1,
        config: ChessAgentConfig = ChessAgentConfig(),
        seedManager: SeedManager = SeedManager
    ): ChessAgent {
        return RealChessAgentFactory.createSeededDQNAgent(
            inputSize = 776,
            outputSize = 4096,
            hiddenLayers = hiddenLayers,
            learningRate = learningRate,
            explorationRate = explorationRate,
            batchSize = config.batchSize,
            maxBufferSize = config.maxBufferSize,
            neuralNetworkRandom = seedManager.getNeuralNetworkRandom(),
            explorationRandom = seedManager.getExplorationRandom(),
            replayBufferRandom = seedManager.getReplayBufferRandom()
        ).let { realAgent ->
            ChessAgentAdapter(realAgent, config)
        }
    }
    
    /**
     * Create a seeded Policy Gradient agent for deterministic training
     */
    fun createSeededPolicyGradientAgent(
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        temperature: Double = 1.0,
        config: ChessAgentConfig = ChessAgentConfig(),
        seedManager: SeedManager = SeedManager
    ): ChessAgent {
        return RealChessAgentFactory.createSeededPolicyGradientAgent(
            inputSize = 776,
            outputSize = 4096,
            hiddenLayers = hiddenLayers,
            learningRate = learningRate,
            temperature = temperature,
            batchSize = config.batchSize,
            neuralNetworkRandom = seedManager.getNeuralNetworkRandom(),
            explorationRandom = seedManager.getExplorationRandom()
        ).let { realAgent ->
            ChessAgentAdapter(realAgent, config)
        }
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
        return realAgent.selectAction(state, validActions)
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
        return ChessAgentMetrics(
            averageReward = simpleMetrics.averageReward,
            explorationRate = simpleMetrics.explorationRate,
            experienceBufferSize = simpleMetrics.experienceBufferSize
        )
    }
    
    override fun forceUpdate() {
        realAgent.forceUpdate()
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
}