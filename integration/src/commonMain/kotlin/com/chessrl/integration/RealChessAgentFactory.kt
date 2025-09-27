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
        inputSize: Int = ChessStateEncoder.TOTAL_FEATURES, // Chess state features
        outputSize: Int = 4096, // Chess action space
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        explorationRate: Double = 0.1,
        batchSize: Int = 32,
        maxBufferSize: Int = 10000,
        targetUpdateFrequency: Int = 100,
        doubleDqn: Boolean = false,
        replayType: String = "UNIFORM",
        gamma: Double = 0.99,
        lossFunction: String = "huber",
        optimizer: String = "adam",
        l2Regularization: Double = 0.001,
        beta1: Double = 0.9,
        beta2: Double = 0.999,
        epsilon: Double = 1e-8,
        momentum: Double = 0.0,
        weightInitType: String = "he"
    ): RealChessAgent {
        // Seeded randoms if available
        val nnRandom = try { SeedManager.getNeuralNetworkRandom() } catch (_: Throwable) { kotlin.random.Random.Default }
        
        // Create main Q-network using actual neural network package
        val qNetworkLayers = mutableListOf<Layer>()
        
        var currentInputSize = inputSize
        for (hiddenSize in hiddenLayers) {
            qNetworkLayers.add(
                DenseLayer(
                    inputSize = currentInputSize,
                    outputSize = hiddenSize,
                    activation = ReLUActivation(),
                    random = nnRandom,
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
                random = nnRandom,
                weightInitType = weightInitType
            )
        )
        
        val lossFn = createLossFunction(lossFunction)
        val optimizerInstance = createOptimizer(
            optimizer = optimizer,
            learningRate = learningRate,
            beta1 = beta1,
            beta2 = beta2,
            epsilon = epsilon,
            momentum = momentum
        )
        val regularization = if (l2Regularization > 0.0) L2Regularization(l2Regularization) else null
        
        val qNetwork = FeedforwardNetwork(
            _layers = qNetworkLayers,
            lossFunction = lossFn,
            optimizer = optimizerInstance,
            regularization = regularization
        )
        
        // Create target network (copy of main network)
        val targetNetworkLayers = mutableListOf<Layer>()
        
        currentInputSize = inputSize
        for (hiddenSize in hiddenLayers) {
            targetNetworkLayers.add(
                DenseLayer(
                    inputSize = currentInputSize,
                    outputSize = hiddenSize,
                    activation = ReLUActivation(),
                    random = nnRandom,
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
                random = nnRandom,
                weightInitType = weightInitType
            )
        )
        
        val targetNetwork = FeedforwardNetwork(
            _layers = targetNetworkLayers,
            lossFunction = lossFn,
            optimizer = createOptimizer(
                optimizer = optimizer,
                learningRate = learningRate,
                beta1 = beta1,
                beta2 = beta2,
                epsilon = epsilon,
                momentum = momentum
            ),
            regularization = regularization
        )
        
        // Create experience replay buffer (uniform or prioritized)
        val experienceReplay: ExperienceReplay<DoubleArray, Int> = when (replayType.uppercase()) {
            "PRIORITIZED" -> PrioritizedExperienceBuffer(maxSize = maxBufferSize)
            else -> CircularExperienceBuffer(maxSize = maxBufferSize)
        }
        
        // Create DQN algorithm
        val dqnAlgorithm = DQNAlgorithm(
            qNetwork = RealNeuralNetworkWrapper(qNetwork),
            targetNetwork = RealNeuralNetworkWrapper(targetNetwork),
            experienceReplay = experienceReplay,
            gamma = gamma,
            targetUpdateFrequency = targetUpdateFrequency,
            batchSize = batchSize,
            doubleDQN = doubleDqn
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
        inputSize: Int = ChessStateEncoder.TOTAL_FEATURES,
        outputSize: Int = 4096,
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        temperature: Double = 1.0
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
            // For policy gradients, train logits with softmax cross-entropy for stable learning
            lossFunction = SoftmaxCrossEntropyLoss(),
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
        inputSize: Int = ChessStateEncoder.TOTAL_FEATURES,
        outputSize: Int = 4096,
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        explorationRate: Double = 0.1,
        batchSize: Int = 32,
        maxBufferSize: Int = 10000,
        neuralNetworkRandom: kotlin.random.Random,
        explorationRandom: kotlin.random.Random,
        replayBufferRandom: kotlin.random.Random,
        weightInitType: String = "he",
        targetUpdateFrequency: Int = 100,
        replayType: String = "UNIFORM",
        gamma: Double = 0.99,
        doubleDqn: Boolean = false,
        lossFunction: String = "huber",
        optimizer: String = "adam",
        l2Regularization: Double = 0.001,
        beta1: Double = 0.9,
        beta2: Double = 0.999,
        epsilon: Double = 1e-8,
        momentum: Double = 0.0
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
        val seededLossFn = createLossFunction(lossFunction)
        val seededOptimizer = createOptimizer(
            optimizer = optimizer,
            learningRate = learningRate,
            beta1 = beta1,
            beta2 = beta2,
            epsilon = epsilon,
            momentum = momentum
        )
        val seededRegularization = if (l2Regularization > 0.0) L2Regularization(l2Regularization) else null
        val qNetwork = FeedforwardNetwork(
            _layers = qNetworkLayers,
            lossFunction = seededLossFn,
            optimizer = seededOptimizer,
            regularization = seededRegularization
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
            lossFunction = seededLossFn,
            optimizer = createOptimizer(
                optimizer = optimizer,
                learningRate = learningRate,
                beta1 = beta1,
                beta2 = beta2,
                epsilon = epsilon,
                momentum = momentum
            ),
            regularization = seededRegularization
        )
        
        // Create seeded experience replay buffer (uniform) or prioritized (unseeded)
        val experienceReplay: ExperienceReplay<DoubleArray, Int> = when (replayType.uppercase()) {
            "PRIORITIZED" -> PrioritizedExperienceBuffer(maxSize = maxBufferSize)
            else -> SeededCircularExperienceBuffer(
                maxSize = maxBufferSize,
                random = replayBufferRandom
            )
        }
        
        // Create DQN algorithm
        val dqnAlgorithm = DQNAlgorithm(
            qNetwork = RealNeuralNetworkWrapper(qNetwork),
            targetNetwork = RealNeuralNetworkWrapper(targetNetwork),
            experienceReplay = experienceReplay,
            gamma = gamma,
            targetUpdateFrequency = targetUpdateFrequency,
            batchSize = batchSize,
            doubleDQN = doubleDqn
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
        inputSize: Int = ChessStateEncoder.TOTAL_FEATURES,
        outputSize: Int = 4096,
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        temperature: Double = 1.0,
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
            lossFunction = MSELoss(),
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

    private fun createLossFunction(type: String): LossFunction {
        return when (type.lowercase()) {
            "huber" -> HuberLoss(delta = 1.0)
            "mse" -> MSELoss()
            else -> HuberLoss(delta = 1.0)
        }
    }

    private fun createOptimizer(
        optimizer: String,
        learningRate: Double,
        beta1: Double,
        beta2: Double,
        epsilon: Double,
        momentum: Double
    ): Optimizer {
        return when (optimizer.lowercase()) {
            "adam" -> AdamOptimizer(
                learningRate = learningRate,
                beta1 = beta1,
                beta2 = beta2,
                epsilon = epsilon
            )
            "sgd" -> SGDOptimizer(
                learningRate = learningRate,
                momentum = momentum
            )
            "rmsprop" -> RMSpropOptimizer(
                learningRate = learningRate,
                decay = beta2,
                epsilon = epsilon,
                momentum = momentum
            )
            else -> AdamOptimizer(
                learningRate = learningRate,
                beta1 = beta1,
                beta2 = beta2,
                epsilon = epsilon
            )
        }
    }
}

/**
 * Wrapper to adapt FeedforwardNetwork to RL NeuralNetwork interface
 */
class RealNeuralNetworkWrapper(
    private val network: FeedforwardNetwork
) : com.chessrl.rl.TrainableNeuralNetwork, com.chessrl.rl.SynchronizableNetwork {
    
    override fun forward(input: DoubleArray): DoubleArray {
        return network.forward(input)
    }
    
    override fun backward(target: DoubleArray): DoubleArray {
        return network.backward(target)
    }
    
    override fun predict(input: DoubleArray): DoubleArray {
        return network.predict(input)
    }

    override fun trainBatch(inputs: Array<DoubleArray>, targets: Array<DoubleArray>): Double {
        // Train for a single epoch on provided batch; return average loss
        val history = network.train(
            inputs = inputs,
            targets = targets,
            epochs = 1,
            batchSize = maxOf(1, minOf(64, inputs.size))
        )
        // Return average loss of the last epoch
        return if (history.isNotEmpty()) history.last().averageLoss else 0.0
    }

    override fun copyWeightsTo(target: com.chessrl.rl.NeuralNetwork) {
        // Copy weights layer-by-layer when both sides are RealNeuralNetworkWrapper
        val t = target as? RealNeuralNetworkWrapper ?: return
        val srcLayers = network.layers
        val dstLayers = t.network.layers
        require(srcLayers.size == dstLayers.size) { "Layer count mismatch for sync: ${srcLayers.size} vs ${dstLayers.size}" }
        for (i in srcLayers.indices) {
            val src = srcLayers[i]
            val dst = dstLayers[i]
            if (src is DenseLayer && dst is DenseLayer) {
                dst.setWeights(src.getWeights())
                dst.setBiases(src.getBiases())
            }
        }
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
    
    private val explorationRandom: kotlin.random.Random = try {
        SeedManager.getExplorationRandom()
    } catch (_: Throwable) {
        kotlin.random.Random.Default
    }

    private var lastUpdate: PolicyUpdateResult? = null

    fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        val actionValues = algorithm.getActionValues(state, validActions)
        return explorationStrategy.selectAction(validActions, actionValues, explorationRandom)
    }
    
    fun learn(experience: Experience<DoubleArray, Int>) {
        // Track episode reward regardless of algorithm type
        episodeReward += experience.reward

        // Treat as off-policy (DQN-style)
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
        qNetwork.save(path)
    }
    
    fun load(path: String) {
        qNetwork.load(path)
        // After loading main network, sync to target if present
        try {
            if (targetNetwork != null) {
                val src = RealNeuralNetworkWrapper(qNetwork)
                val dst = RealNeuralNetworkWrapper(targetNetwork)
                src.copyWeightsTo(dst)
            }
        } catch (_: Throwable) { /* best-effort sync */ }
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
        // Wire provider only when algorithm supports it (DQN)
        if (algorithm is DQNAlgorithm) {
            algorithm.setNextActionProvider(provider)
        }
    }

    fun getLastUpdate(): PolicyUpdateResult? = lastUpdate
}

/**
 * Simplified RL metrics for the real agent
 */
data class SimpleRLMetrics(
    val averageReward: Double,
    val explorationRate: Double,
    val experienceBufferSize: Int
)
