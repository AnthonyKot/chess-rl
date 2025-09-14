package com.chessrl.nn

import kotlin.math.*
import kotlin.random.Random

/**
 * Neural Network Package - Core interfaces and data structures
 * This package provides the foundation for neural network operations
 * including training, inference, and model management.
 */

/**
 * Platform-specific time function
 */
expect fun getCurrentTimeMillis(): Long

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
    private val random: Random = Random.Default,
    private val weightInitType: String = "he"
) : Layer {
    
    // Weights matrix: [outputSize x inputSize]
    private val weights: Array<DoubleArray> = Array(outputSize) { 
        DoubleArray(inputSize) { 
            when (weightInitType.lowercase()) {
                "he" -> random.nextGaussian() * sqrt(2.0 / inputSize)
                "xavier" -> random.nextGaussian() * sqrt(1.0 / inputSize)
                "uniform" -> {
                    val bound = sqrt(6.0 / inputSize)
                    random.nextDouble(-bound, bound)
                }
                else -> random.nextGaussian() * sqrt(2.0 / inputSize) // Default to He
            }
        }
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
    
    // Setter methods for model loading
    fun setWeights(newWeights: Array<DoubleArray>) {
        require(newWeights.size == outputSize) { "Weight matrix must have $outputSize rows" }
        require(newWeights.all { it.size == inputSize }) { "Weight matrix must have $inputSize columns" }
        
        for (i in weights.indices) {
            for (j in weights[i].indices) {
                weights[i][j] = newWeights[i][j]
            }
        }
    }
    
    fun setBiases(newBiases: DoubleArray) {
        require(newBiases.size == outputSize) { "Bias vector must have $outputSize elements" }
        
        for (i in biases.indices) {
            biases[i] = newBiases[i]
        }
    }
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
 * Dataset interface for training data management
 */
interface Dataset {
    fun getBatch(batchSize: Int): TrainingBatch
    fun shuffle()
    fun size(): Int
    fun hasNext(): Boolean
    fun reset()
    fun getAllData(): Pair<Array<DoubleArray>, Array<DoubleArray>>
}

/**
 * In-memory dataset implementation
 */
class InMemoryDataset(
    private val inputs: Array<DoubleArray>,
    private val targets: Array<DoubleArray>
) : Dataset {
    
    private var indices: MutableList<Int> = (0 until inputs.size).toMutableList()
    private var currentIndex = 0
    
    init {
        require(inputs.size == targets.size) { "Inputs and targets must have same size" }
        require(inputs.isNotEmpty()) { "Dataset cannot be empty" }
    }
    
    override fun getBatch(batchSize: Int): TrainingBatch {
        require(batchSize > 0) { "Batch size must be positive" }
        
        val actualBatchSize = minOf(batchSize, indices.size - currentIndex)
        if (actualBatchSize == 0) {
            throw IllegalStateException("No more data available. Call reset() or shuffle() first.")
        }
        
        val batchInputs = Array(actualBatchSize) { i ->
            inputs[indices[currentIndex + i]]
        }
        val batchTargets = Array(actualBatchSize) { i ->
            targets[indices[currentIndex + i]]
        }
        
        currentIndex += actualBatchSize
        
        return TrainingBatch(batchInputs, batchTargets)
    }
    
    override fun shuffle() {
        indices.shuffle()
        currentIndex = 0
    }
    
    override fun size(): Int = inputs.size
    
    override fun hasNext(): Boolean = currentIndex < indices.size
    
    override fun reset() {
        currentIndex = 0
    }
    
    override fun getAllData(): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        return Pair(inputs.copyOf(), targets.copyOf())
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
    val gradientNorm: Double,
    val learningRate: Double = 0.0,
    val regularizationLoss: Double = 0.0,
    val validationLoss: Double? = null,
    val validationAccuracy: Double? = null
)

/**
 * Comprehensive training history tracking
 */
data class TrainingHistory(
    val metrics: List<TrainingMetrics>,
    val startTime: Long,
    val endTime: Long,
    val totalEpochs: Int,
    val bestEpoch: Int,
    val bestValidationLoss: Double?,
    val finalTrainingLoss: Double,
    val convergenceEpoch: Int? = null
) {
    val trainingDuration: Long get() = endTime - startTime
    val averageEpochTime: Double get() = trainingDuration.toDouble() / totalEpochs
    
    fun getTrainingLosses(): List<Double> = metrics.map { it.averageLoss }
    fun getValidationLosses(): List<Double?> = metrics.map { it.validationLoss }
    fun getLearningRates(): List<Double> = metrics.map { it.learningRate }
    fun getGradientNorms(): List<Double> = metrics.map { it.gradientNorm }
}

/**
 * Evaluation metrics for model assessment
 */
data class EvaluationMetrics(
    val loss: Double,
    val accuracy: Double? = null,
    val precision: Double? = null,
    val recall: Double? = null,
    val f1Score: Double? = null,
    val meanAbsoluteError: Double? = null,
    val rootMeanSquareError: Double? = null,
    val r2Score: Double? = null,
    val sampleCount: Int
) {
    companion object {
        /**
         * Compute classification metrics
         */
        fun forClassification(
            predicted: Array<DoubleArray>,
            actual: Array<DoubleArray>,
            loss: Double
        ): EvaluationMetrics {
            require(predicted.size == actual.size) { "Predicted and actual must have same size" }
            
            var correctPredictions = 0
            var truePositives = 0
            var falsePositives = 0
            var falseNegatives = 0
            
            for (i in predicted.indices) {
                val predictedClass = predicted[i].indexOfMax()
                val actualClass = actual[i].indexOfMax()
                
                if (predictedClass == actualClass) {
                    correctPredictions++
                }
                
                // Binary classification metrics (assuming class 1 is positive)
                if (actualClass == 1) {
                    if (predictedClass == 1) truePositives++ else falseNegatives++
                } else {
                    if (predictedClass == 1) falsePositives++
                }
            }
            
            val accuracy = correctPredictions.toDouble() / predicted.size
            val precision = if (truePositives + falsePositives > 0) {
                truePositives.toDouble() / (truePositives + falsePositives)
            } else null
            val recall = if (truePositives + falseNegatives > 0) {
                truePositives.toDouble() / (truePositives + falseNegatives)
            } else null
            val f1 = if (precision != null && recall != null && precision + recall > 0) {
                2 * precision * recall / (precision + recall)
            } else null
            
            return EvaluationMetrics(
                loss = loss,
                accuracy = accuracy,
                precision = precision,
                recall = recall,
                f1Score = f1,
                sampleCount = predicted.size
            )
        }
        
        /**
         * Compute regression metrics
         */
        fun forRegression(
            predicted: Array<DoubleArray>,
            actual: Array<DoubleArray>,
            loss: Double
        ): EvaluationMetrics {
            require(predicted.size == actual.size) { "Predicted and actual must have same size" }
            
            var sumAbsoluteError = 0.0
            var sumSquaredError = 0.0
            var sumActual = 0.0
            var sumSquaredActual = 0.0
            var totalSamples = 0
            
            for (i in predicted.indices) {
                for (j in predicted[i].indices) {
                    val error = predicted[i][j] - actual[i][j]
                    sumAbsoluteError += abs(error)
                    sumSquaredError += error * error
                    sumActual += actual[i][j]
                    sumSquaredActual += actual[i][j] * actual[i][j]
                    totalSamples++
                }
            }
            
            val mae = sumAbsoluteError / totalSamples
            val rmse = sqrt(sumSquaredError / totalSamples)
            
            // R² score calculation
            val meanActual = sumActual / totalSamples
            var totalSumSquares = 0.0
            for (i in actual.indices) {
                for (j in actual[i].indices) {
                    val diff = actual[i][j] - meanActual
                    totalSumSquares += diff * diff
                }
            }
            val r2 = if (totalSumSquares > 0) {
                1.0 - (sumSquaredError / totalSumSquares)
            } else null
            
            return EvaluationMetrics(
                loss = loss,
                meanAbsoluteError = mae,
                rootMeanSquareError = rmse,
                r2Score = r2,
                sampleCount = predicted.size
            )
        }
    }
}

// Extension function for finding max index
private fun DoubleArray.indexOfMax(): Int {
    var maxIndex = 0
    var maxValue = this[0]
    for (i in 1 until size) {
        if (this[i] > maxValue) {
            maxValue = this[i]
            maxIndex = i
        }
    }
    return maxIndex
}

/**
 * Model serialization utilities
 */
data class NetworkWeights(
    val layerWeights: List<LayerWeights>
) {
    data class LayerWeights(
        val weights: Array<DoubleArray>,
        val biases: DoubleArray,
        val layerType: String,
        val inputSize: Int,
        val outputSize: Int,
        val activationFunction: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            
            other as LayerWeights
            
            if (!weights.contentDeepEquals(other.weights)) return false
            if (!biases.contentEquals(other.biases)) return false
            if (layerType != other.layerType) return false
            if (inputSize != other.inputSize) return false
            if (outputSize != other.outputSize) return false
            if (activationFunction != other.activationFunction) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = weights.contentDeepHashCode()
            result = 31 * result + biases.contentHashCode()
            result = 31 * result + layerType.hashCode()
            result = 31 * result + inputSize
            result = 31 * result + outputSize
            result = 31 * result + activationFunction.hashCode()
            return result
        }
    }
}

/**
 * Model configuration for serialization
 */
data class NetworkConfig(
    val inputSize: Int,
    val outputSize: Int,
    val hiddenLayers: List<Int>,
    val activationFunctions: List<String>,
    val lossFunction: String,
    val optimizer: String,
    val learningRate: Double,
    val regularization: String? = null,
    val regularizationLambda: Double? = null
)

/**
 * Model serialization interface
 */
interface ModelSerializer {
    fun serialize(network: FeedforwardNetwork): String
    fun deserialize(data: String): NetworkData
    
    data class NetworkData(
        val config: NetworkConfig,
        val weights: NetworkWeights,
        val trainingHistory: TrainingHistory?
    )
}

/**
 * JSON-like serialization implementation (simplified for Kotlin/Native)
 */
class SimpleModelSerializer : ModelSerializer {
    
    override fun serialize(network: FeedforwardNetwork): String {
        val config = extractConfig(network)
        val weights = extractWeights(network)
        val history = network.getTrainingHistory()
        
        return buildString {
            appendLine("# Neural Network Model")
            appendLine("## Configuration")
            appendLine("input_size=${config.inputSize}")
            appendLine("output_size=${config.outputSize}")
            appendLine("hidden_layers=${config.hiddenLayers.joinToString(",")}")
            appendLine("activation_functions=${config.activationFunctions.joinToString(",")}")
            appendLine("loss_function=${config.lossFunction}")
            appendLine("optimizer=${config.optimizer}")
            appendLine("learning_rate=${config.learningRate}")
            config.regularization?.let { appendLine("regularization=$it") }
            config.regularizationLambda?.let { appendLine("regularization_lambda=$it") }
            
            appendLine("## Weights")
            weights.layerWeights.forEachIndexed { layerIndex, layerWeights ->
                appendLine("layer_$layerIndex=${layerWeights.layerType}")
                appendLine("layer_${layerIndex}_input_size=${layerWeights.inputSize}")
                appendLine("layer_${layerIndex}_output_size=${layerWeights.outputSize}")
                appendLine("layer_${layerIndex}_activation=${layerWeights.activationFunction}")
                
                // Serialize weights
                appendLine("layer_${layerIndex}_weights_start")
                layerWeights.weights.forEach { row ->
                    appendLine(row.joinToString(","))
                }
                appendLine("layer_${layerIndex}_weights_end")
                
                // Serialize biases
                appendLine("layer_${layerIndex}_biases=${layerWeights.biases.joinToString(",")}")
            }
            
            // Serialize training history if available
            if (history.isNotEmpty()) {
                appendLine("## Training History")
                appendLine("epochs=${history.size}")
                history.forEach { metrics ->
                    appendLine("epoch_${metrics.epoch}=${metrics.averageLoss},${metrics.gradientNorm},${metrics.learningRate}")
                }
            }
        }
    }
    
    override fun deserialize(data: String): ModelSerializer.NetworkData {
        val lines = data.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        val config = parseConfig(lines)
        val weights = parseWeights(lines, config)
        val history = parseTrainingHistory(lines)
        
        return ModelSerializer.NetworkData(config, weights, history)
    }
    
    private fun extractConfig(network: FeedforwardNetwork): NetworkConfig {
        val layers = network.layers
        val inputSize = layers.first().inputSize
        val outputSize = layers.last().outputSize
        val hiddenLayers = layers.dropLast(1).map { it.outputSize }
        
        // Extract activation function names (simplified)
        val activationFunctions = layers.map { layer ->
            when (layer) {
                is DenseLayer -> "relu" // Default assumption
                else -> "linear"
            }
        }
        
        return NetworkConfig(
            inputSize = inputSize,
            outputSize = outputSize,
            hiddenLayers = hiddenLayers,
            activationFunctions = activationFunctions,
            lossFunction = network.getLossFunction()::class.simpleName ?: "MSE",
            optimizer = "SGD", // Default assumption
            learningRate = 0.01 // Default assumption
        )
    }
    
    private fun extractWeights(network: FeedforwardNetwork): NetworkWeights {
        val layerWeights = network.layers.map { layer ->
            when (layer) {
                is DenseLayer -> NetworkWeights.LayerWeights(
                    weights = layer.getWeights(),
                    biases = layer.getBiases(),
                    layerType = "Dense",
                    inputSize = layer.inputSize,
                    outputSize = layer.outputSize,
                    activationFunction = "relu" // Default assumption
                )
                else -> throw IllegalArgumentException("Unsupported layer type: ${layer::class.simpleName}")
            }
        }
        
        return NetworkWeights(layerWeights)
    }
    
    private fun parseConfig(lines: List<String>): NetworkConfig {
        val configMap = lines.filter { "=" in it }.associate { line ->
            val (key, value) = line.split("=", limit = 2)
            key.trim() to value.trim()
        }
        
        return NetworkConfig(
            inputSize = configMap["input_size"]?.toInt() ?: throw IllegalArgumentException("Missing input_size"),
            outputSize = configMap["output_size"]?.toInt() ?: throw IllegalArgumentException("Missing output_size"),
            hiddenLayers = configMap["hidden_layers"]?.split(",")?.map { it.toInt() } ?: emptyList(),
            activationFunctions = configMap["activation_functions"]?.split(",") ?: emptyList(),
            lossFunction = configMap["loss_function"] ?: "MSE",
            optimizer = configMap["optimizer"] ?: "SGD",
            learningRate = configMap["learning_rate"]?.toDouble() ?: 0.01,
            regularization = configMap["regularization"],
            regularizationLambda = configMap["regularization_lambda"]?.toDouble()
        )
    }
    
    private fun parseWeights(lines: List<String>, config: NetworkConfig): NetworkWeights {
        val layerWeights = mutableListOf<NetworkWeights.LayerWeights>()
        var currentLayer = 0
        
        while (true) {
            val layerPrefix = "layer_$currentLayer"
            val layerType = lines.find { it.startsWith("${layerPrefix}=") }?.substringAfter("=") ?: break
            
            val inputSize = lines.find { it.startsWith("${layerPrefix}_input_size=") }?.substringAfter("=")?.toInt()
                ?: throw IllegalArgumentException("Missing input size for layer $currentLayer")
            val outputSize = lines.find { it.startsWith("${layerPrefix}_output_size=") }?.substringAfter("=")?.toInt()
                ?: throw IllegalArgumentException("Missing output size for layer $currentLayer")
            val activation = lines.find { it.startsWith("${layerPrefix}_activation=") }?.substringAfter("=")
                ?: "linear"
            
            // Parse weights
            val weightsStartIndex = lines.indexOfFirst { it == "${layerPrefix}_weights_start" }
            val weightsEndIndex = lines.indexOfFirst { it == "${layerPrefix}_weights_end" }
            
            if (weightsStartIndex == -1 || weightsEndIndex == -1) {
                throw IllegalArgumentException("Missing weights for layer $currentLayer")
            }
            
            val weightLines = lines.subList(weightsStartIndex + 1, weightsEndIndex)
            val weights = Array(outputSize) { i ->
                weightLines[i].split(",").map { it.toDouble() }.toDoubleArray()
            }
            
            // Parse biases
            val biasesLine = lines.find { it.startsWith("${layerPrefix}_biases=") }?.substringAfter("=")
                ?: throw IllegalArgumentException("Missing biases for layer $currentLayer")
            val biases = biasesLine.split(",").map { it.toDouble() }.toDoubleArray()
            
            layerWeights.add(
                NetworkWeights.LayerWeights(
                    weights = weights,
                    biases = biases,
                    layerType = layerType,
                    inputSize = inputSize,
                    outputSize = outputSize,
                    activationFunction = activation
                )
            )
            
            currentLayer++
        }
        
        return NetworkWeights(layerWeights)
    }
    
    private fun parseTrainingHistory(lines: List<String>): TrainingHistory? {
        val epochCount = lines.find { it.startsWith("epochs=") }?.substringAfter("=")?.toInt() ?: return null
        
        val metrics = mutableListOf<TrainingMetrics>()
        for (epoch in 1..epochCount) {
            val epochLine = lines.find { it.startsWith("epoch_$epoch=") }?.substringAfter("=")
            if (epochLine != null) {
                val parts = epochLine.split(",")
                if (parts.size >= 3) {
                    metrics.add(
                        TrainingMetrics(
                            epoch = epoch,
                            batchLoss = parts[0].toDouble(),
                            averageLoss = parts[0].toDouble(),
                            gradientNorm = parts[1].toDouble(),
                            learningRate = parts[2].toDouble()
                        )
                    )
                }
            }
        }
        
        return if (metrics.isNotEmpty()) {
            TrainingHistory(
                metrics = metrics,
                startTime = 0L,
                endTime = 0L,
                totalEpochs = epochCount,
                bestEpoch = metrics.minByOrNull { it.averageLoss }?.epoch ?: 1,
                bestValidationLoss = null,
                finalTrainingLoss = metrics.lastOrNull()?.averageLoss ?: 0.0
            )
        } else null
    }
}

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
    private val modelSerializer: ModelSerializer = SimpleModelSerializer()
    
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
        val currentLearningRate = learningRateScheduler?.let { scheduler ->
            val scheduledLearningRate = scheduler.getScheduledLearningRate(epoch, initialLearningRate)
            optimizer.setLearningRate(scheduledLearningRate)
            scheduledLearningRate
        } ?: initialLearningRate
        
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
            gradientNorm = avgGradientNorm,
            learningRate = currentLearningRate,
            regularizationLoss = regularizationLoss
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
        val dataset = InMemoryDataset(inputs, targets)
        val history = train(
            dataset = dataset,
            epochs = epochs,
            batchSize = batchSize,
            validationDataset = null,
            validationFrequency = 1
        )
        return history.metrics
    }
    
    /**
     * Train with dataset interface for better data management
     */
    fun train(
        dataset: Dataset,
        epochs: Int,
        batchSize: Int = 32,
        validationDataset: Dataset? = null,
        validationFrequency: Int = 1
    ): TrainingHistory {
        require(epochs > 0) { "Epochs must be positive" }
        require(batchSize > 0) { "Batch size must be positive" }
        require(validationFrequency > 0) { "Validation frequency must be positive" }
        
        val startTime = getCurrentTimeMillis()
        val metrics = mutableListOf<TrainingMetrics>()
        var bestValidationLoss = Double.MAX_VALUE
        var bestEpoch = 1
        var convergenceEpoch: Int? = null
        val convergenceThreshold = 1e-6
        var lastLoss = Double.MAX_VALUE
        
        for (epoch in 1..epochs) {
            dataset.shuffle()
            dataset.reset()
            
            var epochLoss = 0.0
            var epochGradientNorm = 0.0
            var epochRegularizationLoss = 0.0
            var batchCount = 0
            
            // Process all batches in the dataset
            while (dataset.hasNext()) {
                val batch = try {
                    dataset.getBatch(batchSize)
                } catch (e: IllegalStateException) {
                    break // No more data
                }
                
                val batchMetrics = trainBatch(batch, epoch)
                
                epochLoss += batchMetrics.averageLoss
                epochGradientNorm += batchMetrics.gradientNorm
                epochRegularizationLoss += batchMetrics.regularizationLoss
                batchCount++
            }
            
            val avgLoss = if (batchCount > 0) epochLoss / batchCount else 0.0
            val avgGradientNorm = if (batchCount > 0) epochGradientNorm / batchCount else 0.0
            val avgRegularizationLoss = if (batchCount > 0) epochRegularizationLoss / batchCount else 0.0
            
            // Validation
            var validationLoss: Double? = null
            var validationAccuracy: Double? = null
            if (validationDataset != null && epoch % validationFrequency == 0) {
                val validationMetrics = evaluate(validationDataset)
                validationLoss = validationMetrics.loss
                validationAccuracy = validationMetrics.accuracy
                
                if (validationLoss < bestValidationLoss) {
                    bestValidationLoss = validationLoss
                    bestEpoch = epoch
                }
            }
            
            val epochMetrics = TrainingMetrics(
                epoch = epoch,
                batchLoss = epochLoss,
                averageLoss = avgLoss,
                gradientNorm = avgGradientNorm,
                learningRate = optimizer.getLearningRate(),
                regularizationLoss = avgRegularizationLoss,
                validationLoss = validationLoss,
                validationAccuracy = validationAccuracy
            )
            
            metrics.add(epochMetrics)
            trainingHistory.add(epochMetrics)
            
            // Check for convergence
            if (convergenceEpoch == null && abs(lastLoss - avgLoss) < convergenceThreshold) {
                convergenceEpoch = epoch
            }
            lastLoss = avgLoss
        }
        
        val endTime = getCurrentTimeMillis()
        
        return TrainingHistory(
            metrics = metrics,
            startTime = startTime,
            endTime = endTime,
            totalEpochs = epochs,
            bestEpoch = bestEpoch,
            bestValidationLoss = if (bestValidationLoss != Double.MAX_VALUE) bestValidationLoss else null,
            finalTrainingLoss = metrics.lastOrNull()?.averageLoss ?: 0.0,
            convergenceEpoch = convergenceEpoch
        )
    }
    
    /**
     * Evaluate the model on a dataset
     */
    fun evaluate(dataset: Dataset): EvaluationMetrics {
        dataset.reset()
        val (inputs, targets) = dataset.getAllData()
        
        val predictions = Array(inputs.size) { i ->
            predict(inputs[i])
        }
        
        // Compute average loss
        var totalLoss = 0.0
        for (i in predictions.indices) {
            totalLoss += lossFunction.computeLoss(predictions[i], targets[i])
        }
        val avgLoss = totalLoss / predictions.size
        
        // Determine if this is classification or regression based on output
        val isClassification = targets.all { target ->
            target.all { value -> value == 0.0 || value == 1.0 }
        } && targets.all { it.sum() == 1.0 } // One-hot encoded
        
        return if (isClassification) {
            EvaluationMetrics.forClassification(predictions, targets, avgLoss)
        } else {
            EvaluationMetrics.forRegression(predictions, targets, avgLoss)
        }
    }
    
    /**
     * Train with hyperparameter testing
     */
    fun trainWithHyperparameters(
        dataset: Dataset,
        validationDataset: Dataset,
        hyperparameterConfigs: List<HyperparameterConfig>
    ): List<HyperparameterResult> {
        val results = mutableListOf<HyperparameterResult>()
        
        for ((index, config) in hyperparameterConfigs.withIndex()) {
            println("Testing hyperparameter configuration ${index + 1}/${hyperparameterConfigs.size}")
            
            // Create new network with this configuration
            val testNetwork = createNetworkWithConfig(config)
            
            // Train with this configuration
            val history = testNetwork.train(
                dataset = dataset,
                epochs = config.epochs,
                batchSize = config.batchSize,
                validationDataset = validationDataset,
                validationFrequency = config.validationFrequency
            )
            
            // Evaluate final performance
            val finalMetrics = testNetwork.evaluate(validationDataset)
            
            results.add(
                HyperparameterResult(
                    config = config,
                    trainingHistory = history,
                    finalValidationMetrics = finalMetrics,
                    bestValidationLoss = history.bestValidationLoss ?: Double.MAX_VALUE,
                    convergenceEpoch = history.convergenceEpoch
                )
            )
        }
        
        return results.sortedBy { it.bestValidationLoss }
    }
    
    private fun createNetworkWithConfig(config: HyperparameterConfig): FeedforwardNetwork {
        // Create layers based on configuration
        val layers = mutableListOf<Layer>()
        
        // Input to first hidden layer
        if (config.hiddenLayers.isNotEmpty()) {
            layers.add(DenseLayer(
                inputSize = _layers.first().inputSize,
                outputSize = config.hiddenLayers.first(),
                activation = getActivationFunction(config.activationFunction)
            ))
            
            // Hidden to hidden layers
            for (i in 1 until config.hiddenLayers.size) {
                layers.add(DenseLayer(
                    inputSize = config.hiddenLayers[i-1],
                    outputSize = config.hiddenLayers[i],
                    activation = getActivationFunction(config.activationFunction)
                ))
            }
            
            // Last hidden to output
            layers.add(DenseLayer(
                inputSize = config.hiddenLayers.last(),
                outputSize = _layers.last().outputSize,
                activation = getActivationFunction(config.outputActivation)
            ))
        } else {
            // Direct input to output
            layers.add(DenseLayer(
                inputSize = _layers.first().inputSize,
                outputSize = _layers.last().outputSize,
                activation = getActivationFunction(config.outputActivation)
            ))
        }
        
        // Create optimizer
        val optimizer = when (config.optimizerType) {
            "SGD" -> SGDOptimizer(config.learningRate, config.momentum)
            "Adam" -> AdamOptimizer(config.learningRate, config.beta1, config.beta2)
            "RMSprop" -> RMSpropOptimizer(config.learningRate, config.rmsDecay, momentum = config.momentum)
            else -> SGDOptimizer(config.learningRate)
        }
        
        // Create regularization
        val regularization = when (config.regularizationType) {
            "L1" -> L1Regularization(config.regularizationLambda)
            "L2" -> L2Regularization(config.regularizationLambda)
            "Dropout" -> DropoutRegularization(config.dropoutRate)
            else -> null
        }
        
        // Create learning rate scheduler
        val scheduler = when (config.schedulerType) {
            "Exponential" -> ExponentialDecayScheduler(config.decayRate, config.decaySteps)
            "Step" -> StepDecayScheduler(config.stepSize, config.gamma)
            "Linear" -> LinearDecayScheduler(config.epochs, config.finalLearningRate)
            else -> null
        }
        
        return FeedforwardNetwork(
            _layers = layers,
            lossFunction = lossFunction,
            optimizer = optimizer,
            regularization = regularization,
            learningRateScheduler = scheduler
        )
    }
    
    private fun getActivationFunction(name: String): ActivationFunction {
        return when (name.lowercase()) {
            "relu" -> ReLUActivation()
            "sigmoid" -> SigmoidActivation()
            "tanh" -> TanhActivation()
            "linear" -> LinearActivation()
            else -> ReLUActivation()
        }
    }
    
    // Use the platform-specific time function
    
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
        try {
            val serializedData = modelSerializer.serialize(this)
            // Write to file (platform-specific implementation on JVM)
            saveToFile(path, serializedData)
        } catch (e: Exception) {
            throw RuntimeException("Failed to save model to $path: ${e.message}", e)
        }
    }
    
    override fun load(path: String) {
        try {
            val serializedData = loadFromFile(path)
            val networkData = modelSerializer.deserialize(serializedData)
            
            // Load weights into current network
            loadWeights(networkData.weights)
            
            // Load training history if available
            networkData.trainingHistory?.let { history ->
                trainingHistory.clear()
                trainingHistory.addAll(history.metrics)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to load model from $path: ${e.message}", e)
        }
    }
    
    private fun loadWeights(weights: NetworkWeights) {
        require(weights.layerWeights.size == _layers.size) {
            "Saved model has ${weights.layerWeights.size} layers, but current network has ${_layers.size} layers"
        }
        
        for ((index, layer) in _layers.withIndex()) {
            val savedLayerWeights = weights.layerWeights[index]
            
            require(layer.inputSize == savedLayerWeights.inputSize) {
                "Layer $index input size mismatch: expected ${layer.inputSize}, got ${savedLayerWeights.inputSize}"
            }
            require(layer.outputSize == savedLayerWeights.outputSize) {
                "Layer $index output size mismatch: expected ${layer.outputSize}, got ${savedLayerWeights.outputSize}"
            }
            
            if (layer is DenseLayer) {
                // Load weights and biases into the layer
                // This would require additional methods in DenseLayer to set weights
                loadDenseLayerWeights(layer, savedLayerWeights)
            }
        }
    }
    
    private fun loadDenseLayerWeights(layer: DenseLayer, savedWeights: NetworkWeights.LayerWeights) {
        layer.setWeights(savedWeights.weights)
        layer.setBiases(savedWeights.biases)
    }
    
    // File I/O using platform-specific implementations
    private fun saveToFile(path: String, data: String) {
        writeTextFile(path, data)
    }
    
    private fun loadFromFile(path: String): String {
        return readTextFile(path)
    }
    
    // Helper methods for testing and monitoring
    fun getTrainingHistory(): List<TrainingMetrics> = trainingHistory.toList()
    fun getLossFunction(): LossFunction = lossFunction
}

// Platform-specific file I/O declarations
expect fun writeTextFile(path: String, content: String)
expect fun readTextFile(path: String): String

/**
 * Hyperparameter configuration for automated testing
 */
data class HyperparameterConfig(
    val learningRate: Double = 0.01,
    val epochs: Int = 100,
    val batchSize: Int = 32,
    val hiddenLayers: List<Int> = listOf(64, 32),
    val activationFunction: String = "relu",
    val outputActivation: String = "linear",
    val optimizerType: String = "SGD",
    val momentum: Double = 0.0,
    val beta1: Double = 0.9,
    val beta2: Double = 0.999,
    val rmsDecay: Double = 0.9,
    val regularizationType: String? = null,
    val regularizationLambda: Double = 0.01,
    val dropoutRate: Double = 0.5,
    val schedulerType: String? = null,
    val decayRate: Double = 0.95,
    val decaySteps: Int = 100,
    val stepSize: Int = 100,
    val gamma: Double = 0.1,
    val finalLearningRate: Double = 0.0,
    val validationFrequency: Int = 1
)

/**
 * Result of hyperparameter testing
 */
data class HyperparameterResult(
    val config: HyperparameterConfig,
    val trainingHistory: TrainingHistory,
    val finalValidationMetrics: EvaluationMetrics,
    val bestValidationLoss: Double,
    val convergenceEpoch: Int?
) {
    val converged: Boolean get() = convergenceEpoch != null
    val trainingTime: Long get() = trainingHistory.trainingDuration
    val finalAccuracy: Double? get() = finalValidationMetrics.accuracy
}

/**
 * Utility functions for creating common hyperparameter configurations
 */
object HyperparameterUtils {
    
    /**
     * Generate grid search configurations
     */
    fun gridSearch(
        learningRates: List<Double> = listOf(0.001, 0.01, 0.1),
        batchSizes: List<Int> = listOf(16, 32, 64),
        hiddenLayerConfigs: List<List<Int>> = listOf(listOf(64), listOf(64, 32), listOf(128, 64, 32)),
        optimizers: List<String> = listOf("SGD", "Adam"),
        regularizationTypes: List<String?> = listOf(null, "L2"),
        epochs: Int = 100
    ): List<HyperparameterConfig> {
        val configs = mutableListOf<HyperparameterConfig>()
        
        for (lr in learningRates) {
            for (batchSize in batchSizes) {
                for (hiddenLayers in hiddenLayerConfigs) {
                    for (optimizer in optimizers) {
                        for (regularization in regularizationTypes) {
                            configs.add(
                                HyperparameterConfig(
                                    learningRate = lr,
                                    epochs = epochs,
                                    batchSize = batchSize,
                                    hiddenLayers = hiddenLayers,
                                    optimizerType = optimizer,
                                    regularizationType = regularization
                                )
                            )
                        }
                    }
                }
            }
        }
        
        return configs
    }
    
    /**
     * Generate random search configurations
     */
    fun randomSearch(
        count: Int = 20,
        epochs: Int = 100,
        random: Random = Random.Default
    ): List<HyperparameterConfig> {
        val configs = mutableListOf<HyperparameterConfig>()
        
        repeat(count) {
            val learningRate = 10.0.pow(random.nextDouble(-4.0, -1.0)) // 0.0001 to 0.1
            val batchSize = listOf(16, 32, 64, 128).random(random)
            val hiddenLayerCount = random.nextInt(1, 4) // 1 to 3 hidden layers
            val hiddenLayers = (1..hiddenLayerCount).map {
                listOf(32, 64, 128, 256).random(random)
            }
            val optimizer = listOf("SGD", "Adam", "RMSprop").random(random)
            val regularization = listOf(null, "L1", "L2", "Dropout").random(random)
            
            configs.add(
                HyperparameterConfig(
                    learningRate = learningRate,
                    epochs = epochs,
                    batchSize = batchSize,
                    hiddenLayers = hiddenLayers,
                    optimizerType = optimizer,
                    regularizationType = regularization,
                    regularizationLambda = random.nextDouble(0.001, 0.1),
                    dropoutRate = random.nextDouble(0.2, 0.8)
                )
            )
        }
        
        return configs
    }
    
    /**
     * Create configurations for learning rate scheduling tests
     */
    fun learningRateSchedulingConfigs(
        baseLearningRate: Double = 0.01,
        epochs: Int = 200
    ): List<HyperparameterConfig> {
        return listOf(
            // No scheduling
            HyperparameterConfig(
                learningRate = baseLearningRate,
                epochs = epochs,
                schedulerType = null
            ),
            // Exponential decay
            HyperparameterConfig(
                learningRate = baseLearningRate,
                epochs = epochs,
                schedulerType = "Exponential",
                decayRate = 0.95,
                decaySteps = 50
            ),
            // Step decay
            HyperparameterConfig(
                learningRate = baseLearningRate,
                epochs = epochs,
                schedulerType = "Step",
                stepSize = 50,
                gamma = 0.5
            ),
            // Linear decay
            HyperparameterConfig(
                learningRate = baseLearningRate,
                epochs = epochs,
                schedulerType = "Linear",
                finalLearningRate = baseLearningRate * 0.1
            )
        )
    }
}
