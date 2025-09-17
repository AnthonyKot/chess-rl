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
        config: ChessAgentConfig = ChessAgentConfig(),
        enableDoubleDQN: Boolean = false
    ): ChessAgent {
        return RealChessAgentFactory.createRealDQNAgent(
            inputSize = ChessStateEncoder.TOTAL_FEATURES, // Chess state features
            outputSize = 4096, // Chess action space
            hiddenLayers = hiddenLayers,
            learningRate = learningRate,
            explorationRate = explorationRate,
            batchSize = config.batchSize,
            maxBufferSize = config.maxBufferSize,
            targetUpdateFrequency = config.targetUpdateFrequency,
            doubleDqn = enableDoubleDQN
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
        seedManager: SeedManager = SeedManager.getInstance()
    ): ChessAgent {
        return RealChessAgentFactory.createSeededDQNAgent(
            inputSize = ChessStateEncoder.TOTAL_FEATURES,
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
