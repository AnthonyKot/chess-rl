package com.chessrl.integration.backend

import com.chessrl.nn.*
import com.chessrl.rl.NeuralNetwork
import kotlin.math.*
import kotlin.random.Random

/**
 * Adapter that wraps the existing FeedforwardNetwork without modification.
 * Maintains 100% compatibility with current manual implementation.
 */
class ManualNetworkAdapter(
    private val config: BackendConfig,
    private val random: kotlin.random.Random = kotlin.random.Random(config.seed)
) : NetworkAdapter {
    
    private val network: FeedforwardNetwork = createFeedforwardNetwork(config)
    
    override fun forward(input: DoubleArray): DoubleArray {
        require(input.size == config.inputSize) { 
            "Input size ${input.size} doesn't match expected ${config.inputSize}" 
        }
        val output = network.forward(input)
        require(output.size == config.outputSize) { 
            "Output size ${output.size} doesn't match expected ${config.outputSize}" 
        }
        require(output.all { it.isFinite() }) {
            "Forward pass produced non-finite values"
        }
        return output
    }
    
    override fun backward(target: DoubleArray): DoubleArray {
        require(target.size == config.outputSize) {
            "Target size ${target.size} doesn't match expected ${config.outputSize}"
        }
        return network.backward(target)
    }
    
    override fun trainBatch(inputs: Array<DoubleArray>, targets: Array<DoubleArray>): Double {
        require(inputs.size == targets.size) { "Batch inputs and targets must have same size" }
        require(inputs.isNotEmpty()) { "Batch cannot be empty" }
        
        // Create a training batch for the FeedforwardNetwork
        val batch = createTrainingBatch(inputs, targets)
        
        // Use the network's trainBatch method which properly handles gradient accumulation and weight updates
        val metrics = network.trainBatch(batch)
        
        return metrics.averageLoss
    }
    
    override fun copyWeightsTo(target: NeuralNetwork) {
        if (target is ManualNetworkAdapter) {
            // Copy weights between FeedforwardNetwork instances
            copyNetworkWeights(this.network, target.network)
        } else {
            throw UnsupportedOperationException(
                "Cannot copy weights from manual backend to ${target::class.simpleName} backend"
            )
        }
    }
    
    override fun save(path: String) {
        try {
            // Generate appropriate save path with correct extension for manual backend
            val savePath = CheckpointCompatibility.generateSavePath(path, BackendType.MANUAL)
            
            // Save the network using enhanced serializer that includes optimizer state
            saveNetworkWithOptimizerState(savePath)
        } catch (e: Exception) {
            throw RuntimeException("Failed to save manual network to $path: ${e.message}", e)
        }
    }
    
    override fun load(path: String) {
        try {
            // Resolve checkpoint path and validate compatibility
            val resolution = CheckpointCompatibility.resolveCheckpointPath(path, BackendType.MANUAL)
            
            when (resolution) {
                is CheckpointResolution.Success -> {
                    // Load the network using enhanced serializer that includes optimizer state
                    loadNetworkWithOptimizerState(resolution.path)
                }
                is CheckpointResolution.FormatMismatch -> {
                    throw RuntimeException(
                        CheckpointCompatibility.getFormatMismatchMessage(resolution.path, BackendType.MANUAL)
                    )
                }
                is CheckpointResolution.NotFound -> {
                    throw RuntimeException(resolution.message)
                }
            }
        } catch (e: Exception) {
            // Re-throw RuntimeExceptions as-is to preserve helpful error messages
            if (e is RuntimeException) {
                throw e
            }
            throw RuntimeException("Failed to load manual network from $path: ${e.message}", e)
        }
    }
    
    override fun getBackendName(): String = "manual"
    override fun getConfig(): BackendConfig = config
    
    override fun getParameterCount(): Long {
        var count = 0L
        for (layer in network.layers) {
            if (layer is DenseLayer) {
                count += layer.inputSize * layer.outputSize + layer.outputSize // weights + biases
            }
        }
        return count
    }
    
    override fun getMemoryUsage(): Long {
        // Estimate memory usage based on parameters and workspace
        val paramMemory = getParameterCount() * 8 // 8 bytes per double
        val workspaceMemory = config.batchSize * (config.inputSize + config.outputSize) * 8
        return paramMemory + workspaceMemory
    }
    
    override fun validateGradients(): Boolean {
        // Simple validation - check if network can perform forward and backward passes
        return try {
            val testInput = DoubleArray(config.inputSize) { Random.nextDouble() }
            val testTarget = DoubleArray(config.outputSize) { Random.nextDouble() }
            
            val output = forward(testInput)
            val loss = backward(testTarget)
            
            output.all { it.isFinite() } && loss.all { it.isFinite() }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun createFeedforwardNetwork(config: BackendConfig): FeedforwardNetwork {
        // Create layers based on configuration
        val layers = mutableListOf<Layer>()
        
        // Input to first hidden layer
        if (config.hiddenLayers.isNotEmpty()) {
            layers.add(DenseLayer(
                inputSize = config.inputSize,
                outputSize = config.hiddenLayers.first(),
                activation = ReLUActivation(),
                random = random,
                weightInitType = config.weightInitType
            ))
            
            // Hidden to hidden layers
            for (i in 1 until config.hiddenLayers.size) {
                layers.add(DenseLayer(
                    inputSize = config.hiddenLayers[i-1],
                    outputSize = config.hiddenLayers[i],
                    activation = ReLUActivation(),
                    random = random,
                    weightInitType = config.weightInitType
                ))
            }
            
            // Last hidden to output (identity activation for Q-values)
            layers.add(DenseLayer(
                inputSize = config.hiddenLayers.last(),
                outputSize = config.outputSize,
                activation = LinearActivation(),
                random = random,
                weightInitType = config.weightInitType
            ))
        } else {
            // Direct input to output
            layers.add(DenseLayer(
                inputSize = config.inputSize,
                outputSize = config.outputSize,
                activation = LinearActivation(),
                random = random,
                weightInitType = config.weightInitType
            ))
        }
        
        // Create optimizer
        val optimizer = when (config.optimizer.lowercase()) {
            "adam" -> AdamOptimizer(
                learningRate = config.learningRate,
                beta1 = config.beta1,
                beta2 = config.beta2,
                epsilon = config.epsilon
            )
            "sgd" -> SGDOptimizer(
                learningRate = config.learningRate,
                momentum = config.momentum
            )
            "rmsprop" -> RMSpropOptimizer(
                learningRate = config.learningRate,
                decay = config.beta2,
                epsilon = config.epsilon,
                momentum = config.momentum
            )
            else -> AdamOptimizer(config.learningRate)
        }
        
        // Create regularization if specified
        val regularization = if (config.l2Regularization > 0.0) {
            L2Regularization(config.l2Regularization)
        } else null
        
        // Create loss function
        val lossFunction = when (config.lossFunction.lowercase()) {
            "huber" -> HuberLoss(delta = 1.0)
            "mse" -> MSELoss()
            else -> HuberLoss(delta = 1.0)
        }
        
        return FeedforwardNetwork(
            _layers = layers,
            lossFunction = lossFunction,
            optimizer = optimizer,
            regularization = regularization
        )
    }
    
    private fun copyNetworkWeights(source: FeedforwardNetwork, target: FeedforwardNetwork) {
        require(source.layers.size == target.layers.size) {
            "Source and target networks must have the same number of layers"
        }
        
        for (i in source.layers.indices) {
            val sourceLayer = source.layers[i]
            val targetLayer = target.layers[i]
            
            if (sourceLayer is DenseLayer && targetLayer is DenseLayer) {
                require(sourceLayer.inputSize == targetLayer.inputSize) {
                    "Layer $i input sizes don't match: ${sourceLayer.inputSize} vs ${targetLayer.inputSize}"
                }
                require(sourceLayer.outputSize == targetLayer.outputSize) {
                    "Layer $i output sizes don't match: ${sourceLayer.outputSize} vs ${targetLayer.outputSize}"
                }
                
                // Copy weights and biases
                copyDenseLayerWeights(sourceLayer, targetLayer)
            }
        }
    }
    
    private fun copyDenseLayerWeights(source: DenseLayer, target: DenseLayer) {
        // Get weights and biases from source layer
        val sourceWeights = source.getWeights()
        val sourceBiases = source.getBiases()
        
        // Set weights and biases in target layer
        target.setWeights(sourceWeights)
        target.setBiases(sourceBiases)
    }
    
    private fun computeHuberLoss(predicted: DoubleArray, target: DoubleArray): Double {
        var sum = 0.0
        val delta = 1.0
        for (i in predicted.indices) {
            val diff = abs(predicted[i] - target[i])
            sum += if (diff <= delta) {
                0.5 * diff * diff
            } else {
                delta * diff - 0.5 * delta * delta
            }
        }
        return sum / predicted.size
    }
    
    private fun computeMSELoss(predicted: DoubleArray, target: DoubleArray): Double {
        var sum = 0.0
        for (i in predicted.indices) {
            val diff = predicted[i] - target[i]
            sum += diff * diff
        }
        return sum / predicted.size
    }
    
    /**
     * Create a TrainingBatch from input arrays for the FeedforwardNetwork
     */
    private fun createTrainingBatch(inputs: Array<DoubleArray>, targets: Array<DoubleArray>): TrainingBatch {
        return TrainingBatch(
            inputs = inputs,
            targets = targets
        )
    }
    
    /**
     * Enhanced save method that includes optimizer state
     */
    private fun saveNetworkWithOptimizerState(path: String) {
        // First save the basic network (weights, biases, architecture)
        network.save(path)
        
        // Then save optimizer state to a companion file
        val optimizerStatePath = "${path}.optimizer"
        saveOptimizerState(optimizerStatePath)
    }
    
    /**
     * Enhanced load method that includes optimizer state
     */
    private fun loadNetworkWithOptimizerState(path: String) {
        // First load the basic network (weights, biases, architecture)
        network.load(path)
        
        // Then try to load optimizer state from companion file
        val optimizerStatePath = "${path}.optimizer"
        try {
            loadOptimizerState(optimizerStatePath)
        } catch (e: Exception) {
            // If optimizer state file doesn't exist or is corrupted, continue without it
            // This maintains backward compatibility with models saved without optimizer state
            println("Warning: Could not load optimizer state from $optimizerStatePath: ${e.message}")
            println("Continuing with fresh optimizer state.")
        }
    }
    
    /**
     * Save optimizer state to file
     * This is a simplified implementation that saves the key optimizer parameters
     */
    private fun saveOptimizerState(path: String) {
        val optimizerData = buildString {
            appendLine("# Optimizer State")
            appendLine("optimizer_type=${config.optimizer}")
            appendLine("learning_rate=${config.learningRate}")
            appendLine("beta1=${config.beta1}")
            appendLine("beta2=${config.beta2}")
            appendLine("epsilon=${config.epsilon}")
            appendLine("momentum=${config.momentum}")
            
            // Note: In a full implementation, we would also save the internal state
            // of the optimizer (momentum buffers, velocity buffers, etc.)
            // This would require accessing the optimizer's internal state, which
            // the current FeedforwardNetwork API doesn't expose.
            // For now, we save the configuration parameters which allows
            // the optimizer to be recreated with the same settings.
            
            appendLine("# Note: Internal optimizer state (momentum/velocity buffers)")
            appendLine("# is not saved in this simplified implementation.")
            appendLine("# The optimizer will be recreated with fresh internal state.")
        }
        
        // Write to file using platform-specific implementation
        com.chessrl.integration.writeTextFile(path, optimizerData)
    }
    
    /**
     * Load optimizer state from file
     */
    private fun loadOptimizerState(path: String) {
        val optimizerData = com.chessrl.nn.readTextFile(path)
        val lines = optimizerData.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        
        // Parse optimizer configuration
        val configMap = lines.filter { "=" in it }.associate { line ->
            val (key, value) = line.split("=", limit = 2)
            key.trim() to value.trim()
        }
        
        // Validate that the loaded optimizer configuration matches current configuration
        val loadedOptimizerType = configMap["optimizer_type"]
        if (loadedOptimizerType != null && loadedOptimizerType != config.optimizer) {
            println("Warning: Loaded optimizer type ($loadedOptimizerType) differs from current config (${config.optimizer})")
        }
        
        val loadedLearningRate = configMap["learning_rate"]?.toDoubleOrNull()
        if (loadedLearningRate != null && abs(loadedLearningRate - config.learningRate) > 1e-10) {
            println("Warning: Loaded learning rate ($loadedLearningRate) differs from current config (${config.learningRate})")
        }
        
        // In a full implementation, we would restore the internal optimizer state here
        // For now, we just validate that the configuration matches
        println("Optimizer state loaded successfully (configuration parameters only)")
    }
}
