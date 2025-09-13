package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*

/**
 * Factory for creating real chess agents that use actual neural networks
 * This replaces the mock implementations with real NN integration
 */
object RealChessAgentFactory {
    
    /**
     * Create a DQN agent with real neural networks
     */
    fun createRealDQNAgent(
        inputSize: Int = 776, // Chess state features
        outputSize: Int = 4096, // Chess action space
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        explorationRate: Double = 0.1,
        batchSize: Int = 32,
        maxBufferSize: Int = 10000
    ): RealChessAgent {
        
        // Create main Q-network using actual neural network package
        val qNetworkLayers = mutableListOf<Layer>()
        
        var currentInputSize = inputSize
        for (hiddenSize in hiddenLayers) {
            qNetworkLayers.add(
                DenseLayer(
                    inputSize = currentInputSize,
                    outputSize = hiddenSize,
                    activation = ReLUActivation()
                )
            )
            currentInputSize = hiddenSize
        }
        
        // Output layer
        qNetworkLayers.add(
            DenseLayer(
                inputSize = currentInputSize,
                outputSize = outputSize,
                activation = LinearActivation()
            )
        )
        
        val qNetwork = FeedforwardNetwork(
            _layers = qNetworkLayers,
            lossFunction = HuberLoss(delta = 1.0),
            optimizer = AdamOptimizer(learningRate = learningRate),
            regularization = L2Regularization(lambda = 0.001)
        )
        
        // Create target network (copy of main network)
        val targetNetworkLayers = mutableListOf<Layer>()
        
        currentInputSize = inputSize
        for (hiddenSize in hiddenLayers) {
            targetNetworkLayers.add(
                DenseLayer(
                    inputSize = currentInputSize,
                    outputSize = hiddenSize,
                    activation = ReLUActivation()
                )
            )
            currentInputSize = hiddenSize
        }
        
        targetNetworkLayers.add(
            DenseLayer(
                inputSize = currentInputSize,
                outputSize = outputSize,
                activation = LinearActivation()
            )
        )
        
        val targetNetwork = FeedforwardNetwork(
            _layers = targetNetworkLayers,
            lossFunction = HuberLoss(delta = 1.0),
            optimizer = AdamOptimizer(learningRate = learningRate),
            regularization = L2Regularization(lambda = 0.001)
        )
        
        // Create experience replay buffer
        val experienceReplay = com.chessrl.rl.CircularExperienceBuffer<DoubleArray, Int>(maxSize = maxBufferSize)
        
        // Create DQN algorithm
        val dqnAlgorithm = DQNAlgorithm(
            qNetwork = RealNeuralNetworkWrapper(qNetwork),
            targetNetwork = RealNeuralNetworkWrapper(targetNetwork),
            experienceReplay = experienceReplay,
            gamma = 0.99,
            targetUpdateFrequency = 100,
            batchSize = batchSize
        )
        
        // Create exploration strategy
        val explorationStrategy = EpsilonGreedyStrategy<Int>(explorationRate)
        
        return RealChessAgent(
            algorithm = dqnAlgorithm,
            explorationStrategy = explorationStrategy,
            qNetwork = qNetwork,
            targetNetwork = targetNetwork
        )
    }
    
    /**
     * Create a Policy Gradient agent with real neural networks
     */
    fun createRealPolicyGradientAgent(
        inputSize: Int = 776,
        outputSize: Int = 4096,
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        temperature: Double = 1.0,
        batchSize: Int = 32
    ): RealChessAgent {
        
        // Create policy network
        val policyNetworkLayers = mutableListOf<Layer>()
        
        var currentInputSize = inputSize
        for (hiddenSize in hiddenLayers) {
            policyNetworkLayers.add(
                DenseLayer(
                    inputSize = currentInputSize,
                    outputSize = hiddenSize,
                    activation = ReLUActivation()
                )
            )
            currentInputSize = hiddenSize
        }
        
        // Output layer with softmax-friendly linear activation
        policyNetworkLayers.add(
            DenseLayer(
                inputSize = currentInputSize,
                outputSize = outputSize,
                activation = LinearActivation()
            )
        )
        
        val policyNetwork = FeedforwardNetwork(
            _layers = policyNetworkLayers,
            lossFunction = CrossEntropyLoss(),
            optimizer = AdamOptimizer(learningRate = learningRate),
            regularization = L2Regularization(lambda = 0.001)
        )
        
        // Create policy gradient algorithm
        val pgAlgorithm = PolicyGradientAlgorithm(
            policyNetwork = RealNeuralNetworkWrapper(policyNetwork),
            valueNetwork = null, // No baseline for simplicity
            gamma = 0.99
        )
        
        // Create exploration strategy
        val explorationStrategy = BoltzmannStrategy<Int>(temperature)
        
        return RealChessAgent(
            algorithm = pgAlgorithm,
            explorationStrategy = explorationStrategy,
            qNetwork = policyNetwork,
            targetNetwork = null
        )
    }
    
    /**
     * Create a seeded DQN agent with deterministic neural network initialization
     */
    fun createSeededDQNAgent(
        inputSize: Int = 776,
        outputSize: Int = 4096,
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        explorationRate: Double = 0.1,
        batchSize: Int = 32,
        maxBufferSize: Int = 10000,
        neuralNetworkRandom: kotlin.random.Random,
        explorationRandom: kotlin.random.Random,
        replayBufferRandom: kotlin.random.Random,
        weightInitType: String = "he"
    ): RealChessAgent {
        
        // Create main Q-network with seeded initialization
        val qNetworkLayers = mutableListOf<Layer>()
        
        var currentInputSize = inputSize
        for (hiddenSize in hiddenLayers) {
            qNetworkLayers.add(
                DenseLayer(
                    inputSize = currentInputSize,
                    outputSize = hiddenSize,
                    activation = ReLUActivation(),
                    random = neuralNetworkRandom,
                    weightInitType = weightInitType
                )
            )
            currentInputSize = hiddenSize
        }
        
        // Output layer
        qNetworkLayers.add(
            DenseLayer(
                inputSize = currentInputSize,
                outputSize = outputSize,
                activation = LinearActivation(),
                random = neuralNetworkRandom,
                weightInitType = weightInitType
            )
        )
        
        val qNetwork = FeedforwardNetwork(
            _layers = qNetworkLayers,
            lossFunction = HuberLoss(delta = 1.0),
            optimizer = AdamOptimizer(learningRate = learningRate),
            regularization = L2Regularization(lambda = 0.001)
        )
        
        // Create target network with same seeded initialization
        val targetNetworkLayers = mutableListOf<Layer>()
        
        currentInputSize = inputSize
        for (hiddenSize in hiddenLayers) {
            targetNetworkLayers.add(
                DenseLayer(
                    inputSize = currentInputSize,
                    outputSize = hiddenSize,
                    activation = ReLUActivation(),
                    random = neuralNetworkRandom,
                    weightInitType = weightInitType
                )
            )
            currentInputSize = hiddenSize
        }
        
        targetNetworkLayers.add(
            DenseLayer(
                inputSize = currentInputSize,
                outputSize = outputSize,
                activation = LinearActivation(),
                random = neuralNetworkRandom,
                weightInitType = weightInitType
            )
        )
        
        val targetNetwork = FeedforwardNetwork(
            _layers = targetNetworkLayers,
            lossFunction = HuberLoss(delta = 1.0),
            optimizer = AdamOptimizer(learningRate = learningRate),
            regularization = L2Regularization(lambda = 0.001)
        )
        
        // Create seeded experience replay buffer
        val experienceReplay = SeededCircularExperienceBuffer<DoubleArray, Int>(
            maxSize = maxBufferSize,
            random = replayBufferRandom
        )
        
        // Create DQN algorithm
        val dqnAlgorithm = DQNAlgorithm(
            qNetwork = RealNeuralNetworkWrapper(qNetwork),
            targetNetwork = RealNeuralNetworkWrapper(targetNetwork),
            experienceReplay = experienceReplay,
            gamma = 0.99,
            targetUpdateFrequency = 100,
            batchSize = batchSize
        )
        
        // Create seeded exploration strategy
        val explorationStrategy = SeededEpsilonGreedyStrategy<Int>(
            epsilon = explorationRate,
            random = explorationRandom
        )
        
        return RealChessAgent(
            algorithm = dqnAlgorithm,
            explorationStrategy = explorationStrategy,
            qNetwork = qNetwork,
            targetNetwork = targetNetwork
        )
    }
    
    /**
     * Create a seeded Policy Gradient agent with deterministic neural network initialization
     */
    fun createSeededPolicyGradientAgent(
        inputSize: Int = 776,
        outputSize: Int = 4096,
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        temperature: Double = 1.0,
        batchSize: Int = 32,
        neuralNetworkRandom: kotlin.random.Random,
        explorationRandom: kotlin.random.Random,
        weightInitType: String = "he"
    ): RealChessAgent {
        
        // Create seeded policy network
        val policyNetworkLayers = mutableListOf<Layer>()
        
        var currentInputSize = inputSize
        for (hiddenSize in hiddenLayers) {
            policyNetworkLayers.add(
                DenseLayer(
                    inputSize = currentInputSize,
                    outputSize = hiddenSize,
                    activation = ReLUActivation(),
                    random = neuralNetworkRandom,
                    weightInitType = weightInitType
                )
            )
            currentInputSize = hiddenSize
        }
        
        // Output layer with softmax-friendly linear activation
        policyNetworkLayers.add(
            DenseLayer(
                inputSize = currentInputSize,
                outputSize = outputSize,
                activation = LinearActivation(),
                random = neuralNetworkRandom,
                weightInitType = weightInitType
            )
        )
        
        val policyNetwork = FeedforwardNetwork(
            _layers = policyNetworkLayers,
            lossFunction = CrossEntropyLoss(),
            optimizer = AdamOptimizer(learningRate = learningRate),
            regularization = L2Regularization(lambda = 0.001)
        )
        
        // Create policy gradient algorithm
        val pgAlgorithm = PolicyGradientAlgorithm(
            policyNetwork = RealNeuralNetworkWrapper(policyNetwork),
            valueNetwork = null, // No baseline for simplicity
            gamma = 0.99
        )
        
        // Create seeded exploration strategy
        val explorationStrategy = SeededBoltzmannStrategy<Int>(
            temperature = temperature,
            random = explorationRandom
        )
        
        return RealChessAgent(
            algorithm = pgAlgorithm,
            explorationStrategy = explorationStrategy,
            qNetwork = policyNetwork,
            targetNetwork = null
        )
    }
}

/**
 * Wrapper to adapt FeedforwardNetwork to RL NeuralNetwork interface
 */
class RealNeuralNetworkWrapper(
    private val network: FeedforwardNetwork
) : com.chessrl.rl.NeuralNetwork {
    
    override fun forward(input: DoubleArray): DoubleArray {
        return network.forward(input)
    }
    
    override fun backward(target: DoubleArray): DoubleArray {
        return network.backward(target)
    }
    
    override fun predict(input: DoubleArray): DoubleArray {
        return network.predict(input)
    }
}

/**
 * Real chess agent implementation using actual neural networks
 */
class RealChessAgent(
    private val algorithm: RLAlgorithm<DoubleArray, Int>,
    private val explorationStrategy: ExplorationStrategy<Int>,
    private val qNetwork: FeedforwardNetwork,
    private val targetNetwork: FeedforwardNetwork?
) {
    
    private val experienceBuffer = mutableListOf<Experience<DoubleArray, Int>>()
    private var episodeCount = 0
    private var totalReward = 0.0
    private var episodeReward = 0.0
    
    fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        val actionValues = algorithm.getActionValues(state, validActions)
        return explorationStrategy.selectAction(validActions, actionValues)
    }
    
    fun learn(experience: Experience<DoubleArray, Int>) {
        experienceBuffer.add(experience)
        episodeReward += experience.reward
        
        // Update policy when we have enough experiences or episode ends
        if (experience.done || experienceBuffer.size >= 32) {
            algorithm.updatePolicy(experienceBuffer.toList())
            
            if (experience.done) {
                episodeCount++
                totalReward += episodeReward
                episodeReward = 0.0
            }
            
            // Clear buffer for on-policy methods
            if (algorithm is PolicyGradientAlgorithm) {
                experienceBuffer.clear()
            }
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
        return SimpleRLMetrics(
            averageReward = averageReward,
            explorationRate = explorationStrategy.getExplorationRate(),
            experienceBufferSize = experienceBuffer.size
        )
    }
    
    fun forceUpdate() {
        if (experienceBuffer.isNotEmpty()) {
            algorithm.updatePolicy(experienceBuffer.toList())
        }
    }
    
    fun save(path: String) {
        qNetwork.save(path)
    }
    
    fun load(path: String) {
        qNetwork.load(path)
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
}

/**
 * Simplified RL metrics for the real agent
 */
data class SimpleRLMetrics(
    val averageReward: Double,
    val explorationRate: Double,
    val experienceBufferSize: Int
)