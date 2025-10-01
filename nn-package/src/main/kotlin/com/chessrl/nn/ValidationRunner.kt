package com.chessrl.nn

import kotlin.math.*
import kotlin.random.Random

/**
 * Validation runner for demonstrating neural network capabilities
 * This class provides a simple interface to run validation tests
 * and demonstrate the neural network's performance on various tasks.
 */
object ValidationRunner {
    
    /**
     * Run a comprehensive validation suite
     */
    fun runValidationSuite() {
        println("=== Neural Network Validation Suite ===")
        println("Testing neural network implementation with diverse learning problems")
        
        // Test 1: XOR Classification
        println("\n1. XOR Classification Test")
        testXORClassification()
        
        // Test 2: Polynomial Regression
        println("\n2. Polynomial Regression Test")
        testPolynomialRegression()
        
        // Test 3: Multi-dimensional Regression
        println("\n3. Multi-dimensional Regression Test")
        testMultiDimensionalRegression()
        
        // Test 4: Architecture Comparison
        println("\n4. Architecture Comparison Test")
        testArchitectureComparison()
        
        println("\n=== Validation Suite Complete ===")
    }
    
    private fun testXORClassification() {
        val (inputs, targets) = SyntheticDatasets.generateXORProblem(
            samplesPerClass = 100,
            noiseLevel = 0.05,
            random = Random(42)
        )
        
        val network = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(2, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 4, ReLUActivation(), Random(42)),
                DenseLayer(4, 1, SigmoidActivation(), Random(42))
            ),
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(0.01)
        )
        
        val metrics = network.train(inputs, targets, epochs = 50, batchSize = 32)
        
        // Test on clean XOR cases
        val cleanCases = arrayOf(
            Pair(doubleArrayOf(0.0, 0.0), 0.0),
            Pair(doubleArrayOf(0.0, 1.0), 1.0),
            Pair(doubleArrayOf(1.0, 0.0), 1.0),
            Pair(doubleArrayOf(1.0, 1.0), 0.0)
        )
        
        var correct = 0
        for ((input, expected) in cleanCases) {
            val prediction = network.predict(input)[0]
            val predicted = if (prediction > 0.5) 1.0 else 0.0
            if (predicted == expected) correct++
        }
        
        println("  Final Loss: ${metrics.last().averageLoss.format(4)}")
        println("  XOR Accuracy: $correct/4 (${(correct * 25.0).format(1)}%)")
        println("  Status: ${if (correct >= 3) "PASS" else "NEEDS_IMPROVEMENT"}")
    }
    
    private fun testPolynomialRegression() {
        val (inputs, targets) = SyntheticDatasets.generatePolynomialRegression(
            samples = 300,
            coefficients = doubleArrayOf(0.5, -1.0, 0.2),
            noiseLevel = 0.1,
            random = Random(42)
        )
        
        val (train, test) = SyntheticDatasets.trainTestSplit(inputs, targets, 0.8, Random(42))
        
        val network = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(1, 16, ReLUActivation(), Random(42)),
                DenseLayer(16, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 1, LinearActivation(), Random(42))
            ),
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(0.01)
        )
        
        val metrics = network.train(train.first, train.second, epochs = 50, batchSize = 32)
        
        // Calculate R² on test set
        val r2 = calculateR2(network, test.first, test.second)
        
        println("  Final Training Loss: ${metrics.last().averageLoss.format(4)}")
        println("  Test R²: ${r2.format(3)}")
        println("  Status: ${if (r2 > 0.7) "PASS" else "NEEDS_IMPROVEMENT"}")
    }
    
    private fun testMultiDimensionalRegression() {
        val (inputs, targets) = SyntheticDatasets.generateMultiDimensionalPolynomial(
            samples = 400,
            inputDim = 3,
            quadraticCoeffs = doubleArrayOf(0.3, -0.5, 0.8),
            linearCoeffs = doubleArrayOf(1.2, -0.8, 0.5),
            constant = 0.5,
            noiseLevel = 0.1,
            random = Random(42)
        )
        
        val (train, test) = SyntheticDatasets.trainTestSplit(inputs, targets, 0.8, Random(42))
        
        val network = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(3, 16, ReLUActivation(), Random(42)),
                DenseLayer(16, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 1, LinearActivation(), Random(42))
            ),
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(0.005)
        )
        
        val metrics = network.train(train.first, train.second, epochs = 80, batchSize = 32)
        
        val r2 = calculateR2(network, test.first, test.second)
        
        println("  Final Training Loss: ${metrics.last().averageLoss.format(4)}")
        println("  Test R²: ${r2.format(3)}")
        println("  Status: ${if (r2 > 0.6) "PASS" else "NEEDS_IMPROVEMENT"}")
    }
    
    private fun testArchitectureComparison() {
        val (inputs, targets) = SyntheticDatasets.generatePolynomialRegression(
            samples = 200,
            coefficients = doubleArrayOf(1.0, -0.5, 0.2),
            noiseLevel = 0.1,
            random = Random(42)
        )
        
        val architectures = listOf(
            "Shallow" to listOf(
                DenseLayer(1, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 1, LinearActivation(), Random(42))
            ),
            "Deep" to listOf(
                DenseLayer(1, 16, ReLUActivation(), Random(42)),
                DenseLayer(16, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 4, ReLUActivation(), Random(42)),
                DenseLayer(4, 1, LinearActivation(), Random(42))
            )
        )
        
        for ((name, layers) in architectures) {
            val network = FeedforwardNetwork(
                _layers = layers,
                lossFunction = MSELoss(),
                optimizer = AdamOptimizer(0.01)
            )
            
            val metrics = network.train(inputs, targets, epochs = 30, batchSize = 16)
            val parameters = countParameters(layers)
            
            println("  $name Network:")
            println("    Parameters: $parameters")
            println("    Final Loss: ${metrics.last().averageLoss.format(4)}")
        }
    }
    
    private fun calculateR2(network: FeedforwardNetwork, inputs: Array<DoubleArray>, targets: Array<DoubleArray>): Double {
        var ssRes = 0.0
        var ssTot = 0.0
        
        val meanTarget = targets.map { it[0] }.average()
        
        for (i in inputs.indices) {
            val prediction = network.predict(inputs[i])[0]
            val actual = targets[i][0]
            
            ssRes += (actual - prediction) * (actual - prediction)
            ssTot += (actual - meanTarget) * (actual - meanTarget)
        }
        
        return 1.0 - (ssRes / ssTot)
    }
    
    private fun countParameters(layers: List<Layer>): Int {
        var count = 0
        for (layer in layers) {
            if (layer is DenseLayer) {
                count += layer.inputSize * layer.outputSize + layer.outputSize
            }
        }
        return count
    }
}

// Using the format function from SyntheticDatasets.kt