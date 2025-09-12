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
    
    /**
     * Update weights with momentum
     */
    fun updateWeightsWithMomentum(
        learningRate: Double,
        momentum: Double,
        weightMomentum: Array<DoubleArray>,
        biasMomentum: DoubleArray
    ) {
        if (gradientCount == 0) return
        
        // Update weights with momentum
        for (i in 0 until outputSize) {
            for (j in 0 until inputSize) {
                val gradient = weightGradients[i][j] / gradientCount
                weightMomentum[i][j] = momentum * weightMomentum[i][j] + learningRate * gradient
                weights[i][j] -= weightMomentum[i][j]
                weightGradients[i][j] = 0.0
            }
        }
        
        // Update biases with momentum
        for (i in 0 until outputSize) {
            val gradient = biasGradients[i] / gradientCount
            biasMomentum[i] = momentum * biasMomentum[i] + learningRate * gradient
            biases[i] -= biasMomentum[i]
            biasGradients[i] = 0.0
        }
        
        gradientCount = 0
    }
    
    /**
     * Update weights with Adam optimizer
     */
    fun updateWeightsWithAdam(
        learningRate: Double,
        beta1: Double,
        beta2: Double,
        epsilon: Double,
        weightMomentum: Array<DoubleArray>,
        biasMomentum: DoubleArray,
        weightVelocity: Array<DoubleArray>,
        biasVelocity: DoubleArray
    ) {
        if (gradientCount == 0) return
        
        // Update weights with Adam
        for (i in 0 until outputSize) {
            for (j in 0 until inputSize) {
                val gradient = weightGradients[i][j] / gradientCount
                
                // Update momentum and velocity
                weightMomentum[i][j] = beta1 * weightMomentum[i][j] + (1.0 - beta1) * gradient
                weightVelocity[i][j] = beta2 * weightVelocity[i][j] + (1.0 - beta2) * gradient * gradient
                
                // Update weights
                val update = learningRate * weightMomentum[i][j] / (sqrt(weightVelocity[i][j]) + epsilon)
                weights[i][j] -= update
                weightGradients[i][j] = 0.0
            }
        }
        
        // Update biases with Adam
        for (i in 0 until outputSize) {
            val gradient = biasGradients[i] / gradientCount
            
            // Update momentum and velocity
            biasMomentum[i] = beta1 * biasMomentum[i] + (1.0 - beta1) * gradient
            biasVelocity[i] = beta2 * biasVelocity[i] + (1.0 - beta2) * gradient * gradient
            
            // Update bias
            val update = learningRate * biasMomentum[i] / (sqrt(biasVelocity[i]) + epsilon)
            biases[i] -= update
            biasGradients[i] = 0.0
        }
        
        gradientCount = 0
    }
    
    /**
     * Update weights with RMSprop optimizer
     */
    fun updateWeightsWithRMSprop(
        learningRate: Double,
        decay: Double,
        epsilon: Double,
        momentum: Double,
        weightVelocity: Array<DoubleArray>,
        biasVelocity: DoubleArray,
        momentumPair: Pair<Array<DoubleArray>, DoubleArray>?
    ) {
        if (gradientCount == 0) return
        
        val (weightMomentum, biasMomentum) = momentumPair ?: Pair(null, null)
        
        // Update weights with RMSprop
        for (i in 0 until outputSize) {
            for (j in 0 until inputSize) {
                val gradient = weightGradients[i][j] / gradientCount
                
                // Update velocity
                weightVelocity[i][j] = decay * weightVelocity[i][j] + (1.0 - decay) * gradient * gradient
                
                val update = if (momentum > 0.0 && weightMomentum != null) {
                    // With momentum
                    weightMomentum[i][j] = momentum * weightMomentum[i][j] + 
                                          learningRate * gradient / (sqrt(weightVelocity[i][j]) + epsilon)
                    weightMomentum[i][j]
                } else {
                    // Without momentum
                    learningRate * gradient / (sqrt(weightVelocity[i][j]) + epsilon)
                }
                
                weights[i][j] -= update
                weightGradients[i][j] = 0.0
            }
        }
        
        // Update biases with RMSprop
        for (i in 0 until outputSize) {
            val gradient = biasGradients[i] / gradientCount
            
            // Update velocity
            biasVelocity[i] = decay * biasVelocity[i] + (1.0 - decay) * gradient * gradient
            
            val update = if (momentum > 0.0 && biasMomentum != null) {
                // With momentum
                biasMomentum[i] = momentum * biasMomentum[i] + 
                                 learningRate * gradient / (sqrt(biasVelocity[i]) + epsilon)
                biasMomentum[i]
            } else {
                // Without momentum
                learningRate * gradient / (sqrt(biasVelocity[i]) + epsilon)
            }
            
            biases[i] -= update
            biasGradients[i] = 0.0
        }
        
        gradientCount = 0
    }
    
    /**
     * Apply L1 regularization
     */
    fun applyL1Regularization(lambda: Double, learningRate: Double) {
        for (i in 0 until outputSize) {
            for (j in 0 until inputSize) {
                val sign = if (weights[i][j] > 0) 1.0 else -1.0
                weights[i][j] -= learningRate * lambda * sign
            }
        }
    }
    
    /**
     * Apply L2 regularization
     */
    fun applyL2Regularization(lambda: Double, learningRate: Double) {
        for (i in 0 until outputSize) {
            for (j in 0 until inputSize) {
                weights[i][j] -= learningRate * lambda * weights[i][j]
            }
        }
    }
    
    /**
     * Apply dropout regularization
     */
    fun applyDropout(dropoutRate: Double) {
        // For simplicity, we'll apply dropout to the cached output
        // In a full implementation, this would be integrated into the forward pass
        for (i in lastOutput.indices) {
            if (Random.nextDouble() < dropoutRate) {
                lastOutput[i] = 0.0
            } else {
                lastOutput[i] /= (1.0 - dropoutRate) // Scale remaining values
            }
        }
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
 * Cross Entropy loss function for classification
 */
class CrossEntropyLoss : LossFunction {
    override fun computeLoss(predicted: DoubleArray, target: DoubleArray): Double {
        require(predicted.size == target.size) { "Predicted and target arrays must have same size" }
        
        var sum = 0.0
        for (i in predicted.indices) {
            // Clip predictions to avoid log(0)
            val clippedPred = predicted[i].coerceIn(1e-15, 1.0 - 1e-15)
            sum -= target[i] * ln(clippedPred)
        }
        return sum / predicted.size
    }
    
    override fun computeGradient(predicted: DoubleArray, target: DoubleArray): DoubleArray {
        require(predicted.size == target.size) { "Predicted and target arrays must have same size" }
        
        return DoubleArray(predicted.size) { i ->
            // Clip predictions to avoid division by 0
            val clippedPred = predicted[i].coerceIn(1e-15, 1.0 - 1e-15)
            -target[i] / (clippedPred * predicted.size)
        }
    }
}

/**
 * Huber loss function (robust to outliers)
 */
class HuberLoss(private val delta: Double = 1.0) : LossFunction {
    override fun computeLoss(predicted: DoubleArray, target: DoubleArray): Double {
        require(predicted.size == target.size) { "Predicted and target arrays must have same size" }
        
        var sum = 0.0
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
    
    override fun computeGradient(predicted: DoubleArray, target: DoubleArray): DoubleArray {
        require(predicted.size == target.size) { "Predicted and target arrays must have same size" }
        
        return DoubleArray(predicted.size) { i ->
            val diff = predicted[i] - target[i]
            val gradient = if (abs(diff) <= delta) {
                diff
            } else {
                delta * if (diff > 0) 1.0 else -1.0
            }
            gradient / predicted.size
        }
    }
}

/**
 * Base optimizer interface for neural network training
 */
interface Optimizer {
    fun updateWeights(layers: List<Layer>)
    fun setLearningRate(learningRate: Double)
    fun getLearningRate(): Double
}

/**
 * Mini-batch SGD optimizer
 */
class SGDOptimizer(
    private var learningRate: Double = 0.01,
    private val momentum: Double = 0.0
) : Optimizer {
    
    // Momentum buffers for each layer
    private val momentumBuffers = mutableMapOf<Layer, Pair<Array<DoubleArray>, DoubleArray>>()
    
    override fun updateWeights(layers: List<Layer>) {
        for (layer in layers) {
            if (layer is DenseLayer) {
                updateDenseLayerWeights(layer)
            } else {
                layer.updateWeights(learningRate)
            }
        }
    }
    
    private fun updateDenseLayerWeights(layer: DenseLayer) {
        if (momentum == 0.0) {
            layer.updateWeights(learningRate)
            return
        }
        
        // Initialize momentum buffers if needed
        if (layer !in momentumBuffers) {
            val weightBuffer = Array(layer.outputSize) { DoubleArray(layer.inputSize) }
            val biasBuffer = DoubleArray(layer.outputSize)
            momentumBuffers[layer] = Pair(weightBuffer, biasBuffer)
        }
        
        val (weightMomentum, biasMomentum) = momentumBuffers[layer]!!
        
        // Update with momentum
        layer.updateWeightsWithMomentum(learningRate, momentum, weightMomentum, biasMomentum)
    }
    
    override fun setLearningRate(learningRate: Double) {
        this.learningRate = learningRate
    }
    
    override fun getLearningRate(): Double = learningRate
}

/**
 * Adam optimizer with adaptive learning rates
 */
class AdamOptimizer(
    private var learningRate: Double = 0.001,
    private val beta1: Double = 0.9,
    private val beta2: Double = 0.999,
    private val epsilon: Double = 1e-8
) : Optimizer {
    
    private var timeStep = 0
    private val momentumBuffers = mutableMapOf<Layer, Pair<Array<DoubleArray>, DoubleArray>>()
    private val velocityBuffers = mutableMapOf<Layer, Pair<Array<DoubleArray>, DoubleArray>>()
    
    override fun updateWeights(layers: List<Layer>) {
        timeStep++
        
        for (layer in layers) {
            if (layer is DenseLayer) {
                updateDenseLayerWeights(layer)
            } else {
                layer.updateWeights(learningRate)
            }
        }
    }
    
    private fun updateDenseLayerWeights(layer: DenseLayer) {
        // Initialize buffers if needed
        if (layer !in momentumBuffers) {
            val weightMomentum = Array(layer.outputSize) { DoubleArray(layer.inputSize) }
            val biasMomentum = DoubleArray(layer.outputSize)
            momentumBuffers[layer] = Pair(weightMomentum, biasMomentum)
            
            val weightVelocity = Array(layer.outputSize) { DoubleArray(layer.inputSize) }
            val biasVelocity = DoubleArray(layer.outputSize)
            velocityBuffers[layer] = Pair(weightVelocity, biasVelocity)
        }
        
        val (weightMomentum, biasMomentum) = momentumBuffers[layer]!!
        val (weightVelocity, biasVelocity) = velocityBuffers[layer]!!
        
        // Bias correction
        val biasCorrection1 = 1.0 - beta1.pow(timeStep)
        val biasCorrection2 = 1.0 - beta2.pow(timeStep)
        val correctedLearningRate = learningRate * sqrt(biasCorrection2) / biasCorrection1
        
        layer.updateWeightsWithAdam(
            correctedLearningRate, beta1, beta2, epsilon,
            weightMomentum, biasMomentum, weightVelocity, biasVelocity
        )
    }
    
    override fun setLearningRate(learningRate: Double) {
        this.learningRate = learningRate
    }
    
    override fun getLearningRate(): Double = learningRate
}

/**
 * RMSprop optimizer with adaptive learning rates
 */
class RMSpropOptimizer(
    private var learningRate: Double = 0.001,
    private val decay: Double = 0.9,
    private val epsilon: Double = 1e-8,
    private val momentum: Double = 0.0
) : Optimizer {
    
    private val velocityBuffers = mutableMapOf<Layer, Pair<Array<DoubleArray>, DoubleArray>>()
    private val momentumBuffers = mutableMapOf<Layer, Pair<Array<DoubleArray>, DoubleArray>>()
    
    override fun updateWeights(layers: List<Layer>) {
        for (layer in layers) {
            if (layer is DenseLayer) {
                updateDenseLayerWeights(layer)
            } else {
                layer.updateWeights(learningRate)
            }
        }
    }
    
    private fun updateDenseLayerWeights(layer: DenseLayer) {
        // Initialize buffers if needed
        if (layer !in velocityBuffers) {
            val weightVelocity = Array(layer.outputSize) { DoubleArray(layer.inputSize) }
            val biasVelocity = DoubleArray(layer.outputSize)
            velocityBuffers[layer] = Pair(weightVelocity, biasVelocity)
            
            if (momentum > 0.0) {
                val weightMomentum = Array(layer.outputSize) { DoubleArray(layer.inputSize) }
                val biasMomentum = DoubleArray(layer.outputSize)
                momentumBuffers[layer] = Pair(weightMomentum, biasMomentum)
            }
        }
        
        val (weightVelocity, biasVelocity) = velocityBuffers[layer]!!
        val momentumPair = if (momentum > 0.0) momentumBuffers[layer] else null
        
        layer.updateWeightsWithRMSprop(
            learningRate, decay, epsilon, momentum,
            weightVelocity, biasVelocity, momentumPair
        )
    }
    
    override fun setLearningRate(learningRate: Double) {
        this.learningRate = learningRate
    }
    
    override fun getLearningRate(): Double = learningRate
}

/**
 * Regularization interface for preventing overfitting
 */
interface Regularization {
    fun computeRegularizationLoss(layers: List<Layer>): Double
    fun applyRegularization(layers: List<Layer>, learningRate: Double)
}

/**
 * L1 regularization (Lasso)
 */
class L1Regularization(private val lambda: Double = 0.01) : Regularization {
    override fun computeRegularizationLoss(layers: List<Layer>): Double {
        var loss = 0.0
        for (layer in layers) {
            if (layer is DenseLayer) {
                val weights = layer.getWeights()
                for (row in weights) {
                    for (weight in row) {
                        loss += abs(weight)
                    }
                }
            }
        }
        return lambda * loss
    }
    
    override fun applyRegularization(layers: List<Layer>, learningRate: Double) {
        for (layer in layers) {
            if (layer is DenseLayer) {
                layer.applyL1Regularization(lambda, learningRate)
            }
        }
    }
}

/**
 * L2 regularization (Ridge)
 */
class L2Regularization(private val lambda: Double = 0.01) : Regularization {
    override fun computeRegularizationLoss(layers: List<Layer>): Double {
        var loss = 0.0
        for (layer in layers) {
            if (layer is DenseLayer) {
                val weights = layer.getWeights()
                for (row in weights) {
                    for (weight in row) {
                        loss += weight * weight
                    }
                }
            }
        }
        return 0.5 * lambda * loss
    }
    
    override fun applyRegularization(layers: List<Layer>, learningRate: Double) {
        for (layer in layers) {
            if (layer is DenseLayer) {
                layer.applyL2Regularization(lambda, learningRate)
            }
        }
    }
}

/**
 * Dropout regularization
 */
class DropoutRegularization(private val dropoutRate: Double = 0.5) : Regularization {
    private var isTraining = true
    
    fun setTraining(training: Boolean) {
        isTraining = training
    }
    
    override fun computeRegularizationLoss(layers: List<Layer>): Double = 0.0 // No additional loss
    
    override fun applyRegularization(layers: List<Layer>, learningRate: Double) {
        for (layer in layers) {
            if (layer is DenseLayer && isTraining) {
                layer.applyDropout(dropoutRate)
            }
        }
    }
}

/**
 * Learning rate scheduler interface
 */
interface LearningRateScheduler {
    fun getScheduledLearningRate(epoch: Int, initialLearningRate: Double): Double
}

/**
 * Exponential decay scheduler
 */
class ExponentialDecayScheduler(
    private val decayRate: Double = 0.95,
    private val decaySteps: Int = 100
) : LearningRateScheduler {
    override fun getScheduledLearningRate(epoch: Int, initialLearningRate: Double): Double {
        val decayFactor = decayRate.pow(epoch.toDouble() / decaySteps)
        return initialLearningRate * decayFactor
    }
}

/**
 * Step decay scheduler
 */
class StepDecayScheduler(
    private val stepSize: Int = 100,
    private val gamma: Double = 0.1
) : LearningRateScheduler {
    override fun getScheduledLearningRate(epoch: Int, initialLearningRate: Double): Double {
        val steps = epoch / stepSize
        return initialLearningRate * gamma.pow(steps)
    }
}

/**
 * Linear decay scheduler
 */
class LinearDecayScheduler(
    private val totalEpochs: Int,
    private val finalLearningRate: Double = 0.0
) : LearningRateScheduler {
    override fun getScheduledLearningRate(epoch: Int, initialLearningRate: Double): Double {
        val progress = epoch.toDouble() / totalEpochs
        return initialLearningRate * (1.0 - progress) + finalLearningRate * progress
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
    private val optimizer: Optimizer = SGDOptimizer(),
    private val regularization: Regularization? = null,
    private val learningRateScheduler: LearningRateScheduler? = null
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
    fun trainBatch(batch: TrainingBatch, epoch: Int = 0): TrainingMetrics {
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
        
        // Add regularization loss
        val regularizationLoss = regularization?.computeRegularizationLoss(_layers) ?: 0.0
        totalLoss += regularizationLoss
        
        // Update learning rate if scheduler is provided
        val initialLearningRate = optimizer.getLearningRate()
        learningRateScheduler?.let { scheduler ->
            val scheduledLearningRate = scheduler.getScheduledLearningRate(epoch, initialLearningRate)
            optimizer.setLearningRate(scheduledLearningRate)
        }
        
        // Update weights using accumulated gradients
        optimizer.updateWeights(_layers)
        
        // Apply regularization
        regularization?.applyRegularization(_layers, optimizer.getLearningRate())
        
        // Restore original learning rate if it was scheduled
        learningRateScheduler?.let {
            optimizer.setLearningRate(initialLearningRate)
        }
        
        val avgLoss = totalLoss / batch.batchSize
        val avgGradientNorm = totalGradientNorm / batch.batchSize
        
        return TrainingMetrics(
            epoch = epoch,
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
                val batchMetrics = trainBatch(batch, epoch)
                
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