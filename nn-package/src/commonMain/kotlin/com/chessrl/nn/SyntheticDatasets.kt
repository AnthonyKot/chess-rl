package com.chessrl.nn

import kotlin.math.*
import kotlin.random.Random

/**
 * Synthetic dataset generators for neural network validation
 */
object SyntheticDatasets {
    
    /**
     * Generate polynomial regression dataset: y = a*x^2 + b*x + c + noise
     */
    fun generatePolynomialRegression(
        samples: Int = 1000,
        inputDim: Int = 1,
        coefficients: DoubleArray = doubleArrayOf(1.0, 2.0, 0.5), // [a, b, c]
        noiseLevel: Double = 0.1,
        random: Random = Random.Default
    ): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        
        val inputs = Array(samples) { 
            DoubleArray(inputDim) { random.nextDouble(-2.0, 2.0) }
        }
        
        val targets = Array(samples) { i ->
            val x = inputs[i][0]
            val y = coefficients[0] * x * x + coefficients[1] * x + coefficients[2]
            val noise = random.nextGaussian() * noiseLevel
            doubleArrayOf(y + noise)
        }
        
        return Pair(inputs, targets)
    }
    
    /**
     * Generate multidimensional polynomial: y = sum(ai * xi^2) + sum(bi * xi) + c + noise
     */
    fun generateMultiDimensionalPolynomial(
        samples: Int = 1000,
        inputDim: Int = 3,
        quadraticCoeffs: DoubleArray = doubleArrayOf(0.5, 1.0, 0.3),
        linearCoeffs: DoubleArray = doubleArrayOf(1.0, -0.5, 2.0),
        constant: Double = 0.1,
        noiseLevel: Double = 0.1,
        random: Random = Random.Default
    ): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        
        require(quadraticCoeffs.size == inputDim) { "Quadratic coefficients size must match input dimension" }
        require(linearCoeffs.size == inputDim) { "Linear coefficients size must match input dimension" }
        
        val inputs = Array(samples) { 
            DoubleArray(inputDim) { random.nextDouble(-1.5, 1.5) }
        }
        
        val targets = Array(samples) { i ->
            var y = constant
            for (j in 0 until inputDim) {
                val x = inputs[i][j]
                y += quadraticCoeffs[j] * x * x + linearCoeffs[j] * x
            }
            val noise = random.nextGaussian() * noiseLevel
            doubleArrayOf(y + noise)
        }
        
        return Pair(inputs, targets)
    }
    
    /**
     * Generate XOR problem dataset (classic non-linear classification)
     */
    fun generateXORProblem(
        samplesPerClass: Int = 250,
        noiseLevel: Double = 0.05,
        random: Random = Random.Default
    ): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        
        val totalSamples = samplesPerClass * 4
        val inputs = mutableListOf<DoubleArray>()
        val targets = mutableListOf<DoubleArray>()
        
        // Generate samples for each XOR class
        val xorCases = arrayOf(
            Pair(doubleArrayOf(0.0, 0.0), doubleArrayOf(0.0)),
            Pair(doubleArrayOf(0.0, 1.0), doubleArrayOf(1.0)),
            Pair(doubleArrayOf(1.0, 0.0), doubleArrayOf(1.0)),
            Pair(doubleArrayOf(1.0, 1.0), doubleArrayOf(0.0))
        )
        
        for (case in xorCases) {
            repeat(samplesPerClass) {
                val noisyInput = doubleArrayOf(
                    case.first[0] + random.nextGaussian() * noiseLevel,
                    case.first[1] + random.nextGaussian() * noiseLevel
                )
                inputs.add(noisyInput)
                targets.add(case.second.copyOf())
            }
        }
        
        // Shuffle the data
        val indices = (0 until totalSamples).shuffled(random)
        val shuffledInputs = Array(totalSamples) { inputs[indices[it]] }
        val shuffledTargets = Array(totalSamples) { targets[indices[it]] }
        
        return Pair(shuffledInputs, shuffledTargets)
    }
    
    /**
     * Generate sine wave regression dataset: y = A*sin(B*x + C) + noise
     */
    fun generateSineWave(
        samples: Int = 1000,
        amplitude: Double = 1.0,
        frequency: Double = 2.0,
        phase: Double = 0.0,
        noiseLevel: Double = 0.1,
        xRange: Pair<Double, Double> = Pair(-PI, PI),
        random: Random = Random.Default
    ): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        
        val inputs = Array(samples) { 
            doubleArrayOf(random.nextDouble(xRange.first, xRange.second))
        }
        
        val targets = Array(samples) { i ->
            val x = inputs[i][0]
            val y = amplitude * sin(frequency * x + phase)
            val noise = random.nextGaussian() * noiseLevel
            doubleArrayOf(y + noise)
        }
        
        return Pair(inputs, targets)
    }
    
    /**
     * Split dataset into training and testing sets
     */
    fun trainTestSplit(
        inputs: Array<DoubleArray>,
        targets: Array<DoubleArray>,
        trainRatio: Double = 0.8,
        random: Random = Random.Default
    ): Pair<Pair<Array<DoubleArray>, Array<DoubleArray>>, Pair<Array<DoubleArray>, Array<DoubleArray>>> {
        
        require(inputs.size == targets.size) { "Inputs and targets must have same size" }
        require(trainRatio > 0.0 && trainRatio < 1.0) { "Train ratio must be between 0 and 1" }
        
        val totalSamples = inputs.size
        val trainSize = (totalSamples * trainRatio).toInt()
        
        val indices = (0 until totalSamples).shuffled(random)
        val trainIndices = indices.take(trainSize)
        val testIndices = indices.drop(trainSize)
        
        val trainInputs = Array(trainSize) { inputs[trainIndices[it]] }
        val trainTargets = Array(trainSize) { targets[trainIndices[it]] }
        
        val testInputs = Array(testIndices.size) { inputs[testIndices[it]] }
        val testTargets = Array(testIndices.size) { targets[testIndices[it]] }
        
        return Pair(
            Pair(trainInputs, trainTargets),
            Pair(testInputs, testTargets)
        )
    }
}

/**
 * Training metrics and monitoring utilities
 */
data class LearningCurve(
    val epochs: List<Int>,
    val trainLosses: List<Double>,
    val testLosses: List<Double>,
    val gradientNorms: List<Double>
) {
    fun printSummary() {
        println("Learning Curve Summary:")
        println("  Initial train loss: ${trainLosses.first()}")
        println("  Final train loss: ${trainLosses.last()}")
        if (testLosses.isNotEmpty()) {
            println("  Initial test loss: ${testLosses.first()}")
            println("  Final test loss: ${testLosses.last()}")
        }
        println("  Average gradient norm: ${gradientNorms.average()}")
        
        val improvement = (trainLosses.first() - trainLosses.last()) / trainLosses.first() * 100
        println("  Training improvement: ${improvement.format(2)}%")
    }
}

/**
 * Convergence monitoring utilities
 */
object ConvergenceMonitor {
    
    /**
     * Check if training has converged based on loss improvement
     */
    fun hasConverged(
        losses: List<Double>,
        patience: Int = 5,
        minImprovement: Double = 1e-4
    ): Boolean {
        // Require at least patience+1 points to measure improvement over the window
        if (losses.size < patience + 1) return false
        
        val recent = losses.takeLast(patience + 1)
        // Improvement over the recent window (earliest -> latest)
        val improvement = recent.first() - recent.last()
        
        // Consider converged if improvement meets or exceeds the threshold
        return improvement >= minImprovement
    }
    
    /**
     * Detect training issues
     */
    fun detectIssues(metrics: List<TrainingMetrics>): List<String> {
        val issues = mutableListOf<String>()
        
        if (metrics.isEmpty()) return issues
        
        val losses = metrics.map { it.averageLoss }
        val gradientNorms = metrics.map { it.gradientNorm }
        
        // Check for exploding gradients
        if (gradientNorms.any { it > 10.0 }) {
            issues.add("Exploding gradients detected (norm > 10.0)")
        }
        
        // Check for vanishing gradients
        if (gradientNorms.takeLast(5).all { it < 1e-6 }) {
            issues.add("Vanishing gradients detected (norm < 1e-6)")
        }
        
        // Check for increasing loss
        if (losses.size >= 3) {
            val recentLosses = losses.takeLast(3)
            if (recentLosses[0] < recentLosses[1] && recentLosses[1] < recentLosses[2]) {
                issues.add("Loss is increasing - possible learning rate too high")
            }
        }
        
        // Check for NaN or infinite values
        if (losses.any { !it.isFinite() }) {
            issues.add("Non-finite loss values detected")
        }
        
        return issues
    }
}

/**
 * Extension function for formatting doubles (multiplatform compatible)
 */
fun Double.format(digits: Int): String {
    val multiplier = when (digits) {
        0 -> 1.0
        1 -> 10.0
        2 -> 100.0
        3 -> 1000.0
        4 -> 10000.0
        else -> {
            var result = 1.0
            repeat(digits) { result *= 10.0 }
            result
        }
    }
    val rounded = kotlin.math.round(this * multiplier) / multiplier
    return rounded.toString()
}
