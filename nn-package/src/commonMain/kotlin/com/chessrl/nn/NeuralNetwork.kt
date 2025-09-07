package com.chessrl.nn

import kotlin.math.*
import kotlin.random.Random

/**
 * Neural Network Package - Core interfaces and data structures
 * This package provides the foundation for neural network operations
 * including training, inference, and model management.
 */

/**
 * Main neural network interface for the chess RL system
 */
interface NeuralNetwork {
    // Forward and backward propagation
    fun forward(input: DoubleArray): DoubleArray
    fun backward(target: DoubleArray): DoubleArray  // Returns loss
    
    // Training utilities  
    fun predict(input: DoubleArray): DoubleArray
    
    // Model management
    fun save(path: String)
    fun load(path: String)
}

/**
 * Layer interface for neural network components
 */
interface Layer {
    fun forward(input: DoubleArray): DoubleArray
    fun backward(gradient: DoubleArray): DoubleArray
    fun updateWeights(learningRate: Double)
    val inputSize: Int
    val outputSize: Int
}

/**
 * Activation function interface
 */
interface ActivationFunction {
    fun activate(x: Double): Double
    fun derivative(x: Double): Double
}

/**
 * ReLU activation function
 */
class ReLUActivation : ActivationFunction {
    override fun activate(x: Double): Double = max(0.0, x)
    override fun derivative(x: Double): Double = if (x > 0.0) 1.0 else 0.0
}

/**
 * Sigmoid activation function
 */
class SigmoidActivation : ActivationFunction {
    override fun activate(x: Double): Double = 1.0 / (1.0 + exp(-x))
    override fun derivative(x: Double): Double {
        val s = activate(x)
        return s * (1.0 - s)
    }
}

/**
 * Tanh activation function
 */
class TanhActivation : ActivationFunction {
    override fun activate(x: Double): Double = tanh(x)
    override fun derivative(x: Double): Double {
        val t = activate(x)
        return 1.0 - t * t
    }
}

/**
 * Linear activation function (identity)
 */
class LinearActivation : ActivationFunction {
    override fun activate(x: Double): Double = x
    override fun derivative(x: Double): Double = 1.0
}

/**
 * Dense (fully connected) layer implementation
 */
class DenseLayer(
    override val inputSize: Int,
    override val outputSize: Int,
    private val activation: ActivationFunction,
    private val random: Random = Random.Default
) : Layer {
    
    // Weights matrix: [outputSize x inputSize]
    private val weights: Array<DoubleArray> = Array(outputSize) { 
        DoubleArray(inputSize) { random.nextGaussian() * sqrt(2.0 / inputSize) } // He initialization
    }
    
    // Bias vector: [outputSize]
    private val biases: DoubleArray = DoubleArray(outputSize) { 0.0 }
    
    // Cache for backward pass
    private var lastInput: DoubleArray = doubleArrayOf()
    private var lastPreActivation: DoubleArray = doubleArrayOf()
    private var lastOutput: DoubleArray = doubleArrayOf()
    
    // Gradients for weight updates (accumulated across batch)
    private val weightGradients: Array<DoubleArray> = Array(outputSize) { DoubleArray(inputSize) }
    private val biasGradients: DoubleArray = DoubleArray(outputSize)
    private var gradientCount = 0
    
    override fun forward(input: DoubleArray): DoubleArray {
        require(input.size == inputSize) { "Input size ${input.size} doesn't match expected $inputSize" }
        
        // Cache input for backward pass
        lastInput = input.copyOf()
        
        // Compute pre-activation: z = W * x + b
        lastPreActivation = DoubleArray(outputSize) { i ->
            var sum = biases[i]
            for (j in input.indices) {
                sum += weights[i][j] * input[j]
            }
            sum
        }
        
        // Apply activation function
        lastOutput = DoubleArray(outputSize) { i ->
            activation.activate(lastPreActivation[i])
        }
        
        return lastOutput.copyOf()
    }
    
    override fun backward(gradient: DoubleArray): DoubleArray {
        require(gradient.size == outputSize) { "Gradient size ${gradient.size} doesn't match output size $outputSize" }
        
        // Compute gradient w.r.t. pre-activation: dL/dz = dL/da * da/dz
        val preActivationGradient = DoubleArray(outputSize) { i ->
            gradient[i] * activation.derivative(lastPreActivation[i])
        }
        
        // Accumulate weight gradients: dL/dW = dL/dz * x^T
        for (i in 0 until outputSize) {
            for (j in 0 until inputSize) {
                weightGradients[i][j] += preActivationGradient[i] * lastInput[j]
            }
        }
        
        // Accumulate bias gradients: dL/db = dL/dz
        for (i in 0 until outputSize) {
            biasGradients[i] += preActivationGradient[i]
        }
        
        gradientCount++
        
        // Compute input gradient: dL/dx = W^T * dL/dz
        val inputGradient = DoubleArray(inputSize) { j ->
            var sum = 0.0
            for (i in 0 until outputSize) {
                sum += weights[i][j] * preActivationGradient[i]
            }
            sum
        }
        
        return inputGradient
    }
    
    override fun updateWeights(learningRate: Double) {
        if (gradientCount == 0) return // No gradients accumulated
        
        // Average gradients across batch and update weights: W = W - lr * avg(dL/dW)
        for (i in 0 until outputSize) {
            for (j in 0 until inputSize) {
                weights[i][j] -= learningRate * (weightGradients[i][j] / gradientCount)
                weightGradients[i][j] = 0.0 // Reset for next batch
            }
        }
        
        // Average gradients across batch and update biases: b = b - lr * avg(dL/db)
        for (i in 0 until outputSize) {
            biases[i] -= learningRate * (biasGradients[i] / gradientCount)
            biasGradients[i] = 0.0 // Reset for next batch
        }
        
        gradientCount = 0 // Reset counter
    }
    
    // Getter methods for testing
    fun getWeights(): Array<DoubleArray> = weights.map { it.copyOf() }.toTypedArray()
    fun getBiases(): DoubleArray = biases.copyOf()
}

/**
 * Loss function interface for training
 */
interface LossFunction {
    fun computeLoss(predicted: DoubleArray, target: DoubleArray): Double
    fun computeGradient(predicted: DoubleArray, target: DoubleArray): DoubleArray
}

/**
 * Mean Squared Error loss function
 */
class MSELoss : LossFunction {
    override fun computeLoss(predicted: DoubleArray, target: DoubleArray): Double {
        require(predicted.size == target.size) { "Predicted and target arrays must have same size" }
        
        var sum = 0.0
        for (i in predicted.indices) {
            val diff = predicted[i] - target[i]
            sum += diff * diff
        }
        return sum / predicted.size
    }
    
    override fun computeGradient(predicted: DoubleArray, target: DoubleArray): DoubleArray {
        require(predicted.size == target.size) { "Predicted and target arrays must have same size" }
        
        return DoubleArray(predicted.size) { i ->
            2.0 * (predicted[i] - target[i]) / predicted.size
        }
    }
}

/**
 * Mini-batch SGD optimizer
 */
class SGDOptimizer(
    private val learningRate: Double = 0.01
) {
    fun updateWeights(layers: List<Layer>) {
        for (layer in layers) {
            layer.updateWeights(learningRate)
        }
    }
}

/**
 * Training batch data structure
 */
data class TrainingBatch(
    val inputs: Array<DoubleArray>,
    val targets: Array<DoubleArray>
) {
    init {
        require(inputs.size == targets.size) { "Inputs and targets must have same batch size" }
        require(inputs.isNotEmpty()) { "Batch cannot be empty" }
    }
    
    val batchSize: Int = inputs.size
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        
        other as TrainingBatch
        
        if (!inputs.contentDeepEquals(other.inputs)) return false
        if (!targets.contentDeepEquals(other.targets)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = inputs.contentDeepHashCode()
        result = 31 * result + targets.contentDeepHashCode()
        return result
    }
}

/**
 * Training metrics for monitoring progress
 */
data class TrainingMetrics(
    val epoch: Int,
    val batchLoss: Double,
    val averageLoss: Double,
    val gradientNorm: Double
)

/**
 * Simple feedforward neural network implementation with full training capabilities
 */
class FeedforwardNetwork(
    private val _layers: List<Layer>,
    private val lossFunction: LossFunction = MSELoss(),
    private val optimizer: SGDOptimizer = SGDOptimizer()
) : NeuralNetwork {
    
    val layers: List<Layer> get() = _layers
    
    private var lastOutput: DoubleArray = doubleArrayOf()
    private val trainingHistory = mutableListOf<TrainingMetrics>()
    
    init {
        require(_layers.isNotEmpty()) { "Network must have at least one layer" }
        
        // Validate layer connectivity
        for (i in 1 until _layers.size) {
            require(_layers[i-1].outputSize == _layers[i].inputSize) {
                "Layer ${i-1} output size ${_layers[i-1].outputSize} doesn't match layer $i input size ${_layers[i].inputSize}"
            }
        }
    }
    
    override fun forward(input: DoubleArray): DoubleArray {
        var output = input
        for (layer in _layers) {
            output = layer.forward(output)
        }
        lastOutput = output.copyOf()
        return output
    }
    
    override fun backward(target: DoubleArray): DoubleArray {
        require(lastOutput.isNotEmpty()) { "Must call forward() before backward()" }
        require(target.size == lastOutput.size) { "Target size ${target.size} doesn't match output size ${lastOutput.size}" }
        
        // Compute loss
        val loss = lossFunction.computeLoss(lastOutput, target)
        
        // Compute initial gradient from loss function
        var gradient = lossFunction.computeGradient(lastOutput, target)
        
        // Backpropagate through layers in reverse order
        for (i in _layers.size - 1 downTo 0) {
            gradient = _layers[i].backward(gradient)
        }
        
        return doubleArrayOf(loss)
    }
    
    /**
     * Train on a single batch with gradient accumulation
     */
    fun trainBatch(batch: TrainingBatch): TrainingMetrics {
        var totalLoss = 0.0
        var totalGradientNorm = 0.0
        
        // Process each sample in the batch
        for (i in 0 until batch.batchSize) {
            // Forward pass
            val output = forward(batch.inputs[i])
            
            // Compute loss
            val sampleLoss = lossFunction.computeLoss(output, batch.targets[i])
            totalLoss += sampleLoss
            
            // Backward pass (accumulates gradients in layers)
            val lossArray = backward(batch.targets[i])
            
            // Compute gradient norm for monitoring
            totalGradientNorm += computeGradientNorm()
        }
        
        // Update weights using accumulated gradients
        optimizer.updateWeights(_layers)
        
        val avgLoss = totalLoss / batch.batchSize
        val avgGradientNorm = totalGradientNorm / batch.batchSize
        
        return TrainingMetrics(
            epoch = 0, // Will be set by caller
            batchLoss = totalLoss,
            averageLoss = avgLoss,
            gradientNorm = avgGradientNorm
        )
    }
    
    /**
     * Train for multiple epochs with mini-batch processing
     */
    fun train(
        inputs: Array<DoubleArray>,
        targets: Array<DoubleArray>,
        epochs: Int,
        batchSize: Int = 32
    ): List<TrainingMetrics> {
        require(inputs.size == targets.size) { "Inputs and targets must have same size" }
        require(epochs > 0) { "Epochs must be positive" }
        require(batchSize > 0) { "Batch size must be positive" }
        
        val metrics = mutableListOf<TrainingMetrics>()
        
        for (epoch in 1..epochs) {
            // Shuffle data
            val indices = (0 until inputs.size).shuffled()
            var epochLoss = 0.0
            var epochGradientNorm = 0.0
            var batchCount = 0
            
            // Process mini-batches
            for (startIdx in indices.indices step batchSize) {
                val endIdx = minOf(startIdx + batchSize, indices.size)
                val batchIndices = indices.subList(startIdx, endIdx)
                
                val batchInputs = Array(batchIndices.size) { i -> inputs[batchIndices[i]] }
                val batchTargets = Array(batchIndices.size) { i -> targets[batchIndices[i]] }
                
                val batch = TrainingBatch(batchInputs, batchTargets)
                val batchMetrics = trainBatch(batch)
                
                epochLoss += batchMetrics.averageLoss
                epochGradientNorm += batchMetrics.gradientNorm
                batchCount++
            }
            
            val epochMetrics = TrainingMetrics(
                epoch = epoch,
                batchLoss = epochLoss,
                averageLoss = epochLoss / batchCount,
                gradientNorm = epochGradientNorm / batchCount
            )
            
            metrics.add(epochMetrics)
            trainingHistory.add(epochMetrics)
        }
        
        return metrics
    }
    
    /**
     * Compute gradient norm for monitoring training stability
     */
    private fun computeGradientNorm(): Double {
        // This is a simplified gradient norm computation
        // In a full implementation, we would access the actual gradients from layers
        return 1.0 // Placeholder
    }
    
    override fun predict(input: DoubleArray): DoubleArray = forward(input)
    
    override fun save(path: String) {
        // Placeholder - will be implemented later
        TODO("Save functionality not yet implemented")
    }
    
    override fun load(path: String) {
        // Placeholder - will be implemented later
        TODO("Load functionality not yet implemented")
    }
    
    // Helper methods for testing and monitoring
    fun getTrainingHistory(): List<TrainingMetrics> = trainingHistory.toList()
    fun getLossFunction(): LossFunction = lossFunction
}