package com.chessrl.nn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.math.*
import kotlin.random.Random

/**
 * Essential neural network tests - fast, reliable unit tests for core NN functionality
 * Focuses on XOR training, serialization, and gradient updates
 */
class EssentialNeuralNetworkTest {
    
    @Test
    fun testXORLearning() {
        // Create simple network for XOR problem: 2 inputs -> 3 hidden -> 1 output
        val layers = listOf(
            DenseLayer(2, 4, ReLUActivation(), Random(42)),
            DenseLayer(4, 1, SigmoidActivation(), Random(4242))
        )
        val network = FeedforwardNetwork(
            _layers = layers,
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(learningRate = 0.05)
        )
        
        // XOR training data
        val inputs = arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(1.0, 1.0)
        )
        val targets = arrayOf(
            doubleArrayOf(0.0),
            doubleArrayOf(1.0),
            doubleArrayOf(1.0),
            doubleArrayOf(0.0)
        )
        
        // Train for multiple epochs
        val epochs = 150
        val metrics = network.train(inputs, targets, epochs = epochs, batchSize = 4)
        
        // Should have metrics for all epochs
        assertEquals(epochs, metrics.size, "Should have metrics for all epochs")
        
        // Loss should be finite and non-negative
        val finalLoss = metrics.last().averageLoss
        assertTrue(finalLoss >= 0.0 && finalLoss.isFinite(), "Final loss should be non-negative and finite")
        assertTrue(finalLoss < 0.2, "Final loss should indicate the XOR pattern was learned")
        
        // Test predictions after training
        val predictions = inputs.map { input -> network.predict(input)[0] }
        
        // All predictions should be in valid range [0, 1] due to sigmoid output
        predictions.forEach { prediction ->
            assertTrue(prediction >= 0.0 && prediction <= 1.0, "Prediction should be in [0, 1] range")
        }
        
        // XOR pattern should be somewhat learned (not perfect, but better than random)
        // Expected: [0, 1, 1, 0], so (0,0) and (1,1) should be lower than (0,1) and (1,0)
        assertTrue(predictions[0] < 0.3, "XOR(0,0) should tend toward 0")
        assertTrue(predictions[1] > 0.7, "XOR(0,1) should tend toward 1")
        assertTrue(predictions[2] > 0.7, "XOR(1,0) should tend toward 1")
        assertTrue(predictions[3] < 0.3, "XOR(1,1) should tend toward 0")
    }
    
    @Test
    fun testNetworkConsistency() {
        // Create and test a simple network for consistency
        val layers = listOf(
            DenseLayer(2, 2, ReLUActivation(), Random(123)),
            DenseLayer(2, 1, LinearActivation(), Random(123))
        )
        val network = FeedforwardNetwork(layers)
        
        // Make predictions - should be consistent
        val testInput = doubleArrayOf(1.0, -0.5)
        val output1 = network.predict(testInput)
        val output2 = network.predict(testInput)
        
        // Outputs should be identical for same input
        assertEquals(output1.size, output2.size)
        for (i in output1.indices) {
            assertEquals(output1[i], output2[i], 1e-10, "Same input should produce same output")
        }
        
        // Test with different input
        val differentInput = doubleArrayOf(0.5, 1.0)
        val differentOutput = network.predict(differentInput)
        
        // Should be different from first output (with high probability)
        assertTrue(output1.size == differentOutput.size, "Output sizes should match")
        // Note: We don't assert they're different as it's theoretically possible they're the same
    }
    
    @Test
    fun testGradientUpdates() {
        val layer = DenseLayer(2, 1, LinearActivation(), Random(42))
        
        // Get initial weights and biases
        val initialWeights = layer.getWeights().map { it.copyOf() }
        val initialBiases = layer.getBiases().copyOf()
        
        // Forward pass
        val input = doubleArrayOf(1.0, 2.0)
        val output = layer.forward(input)
        assertEquals(1, output.size)
        
        // Backward pass with gradient
        val gradient = doubleArrayOf(1.0)
        layer.backward(gradient)
        
        // Update weights
        val learningRate = 0.1
        layer.updateWeights(learningRate)
        
        // Check that weights and biases have changed
        val updatedWeights = layer.getWeights()
        val updatedBiases = layer.getBiases()
        
        var weightsChanged = false
        for (i in initialWeights.indices) {
            for (j in initialWeights[i].indices) {
                if (abs(initialWeights[i][j] - updatedWeights[i][j]) > 1e-10) {
                    weightsChanged = true
                }
            }
        }
        
        var biasesChanged = false
        for (i in initialBiases.indices) {
            if (abs(initialBiases[i] - updatedBiases[i]) > 1e-10) {
                biasesChanged = true
            }
        }
        
        assertTrue(weightsChanged || biasesChanged, "Weights or biases should change after gradient update")
    }
    
    @Test
    fun testActivationFunctions() {
        // Test ReLU
        val relu = ReLUActivation()
        assertEquals(0.0, relu.activate(-1.0), 1e-10, "ReLU(-1) should be 0")
        assertEquals(0.0, relu.activate(0.0), 1e-10, "ReLU(0) should be 0")
        assertEquals(5.0, relu.activate(5.0), 1e-10, "ReLU(5) should be 5")
        
        assertEquals(0.0, relu.derivative(-1.0), 1e-10, "ReLU'(-1) should be 0")
        assertEquals(1.0, relu.derivative(5.0), 1e-10, "ReLU'(5) should be 1")
        
        // Test Sigmoid
        val sigmoid = SigmoidActivation()
        assertEquals(0.5, sigmoid.activate(0.0), 1e-10, "Sigmoid(0) should be 0.5")
        assertTrue(sigmoid.activate(-10.0) < 0.01, "Sigmoid(-10) should be close to 0")
        assertTrue(sigmoid.activate(10.0) > 0.99, "Sigmoid(10) should be close to 1")
        
        assertEquals(0.25, sigmoid.derivative(0.0), 1e-10, "Sigmoid'(0) should be 0.25")
        
        // Test Linear
        val linear = LinearActivation()
        assertEquals(-5.0, linear.activate(-5.0), 1e-10, "Linear(-5) should be -5")
        assertEquals(3.14, linear.activate(3.14), 1e-10, "Linear(3.14) should be 3.14")
        assertEquals(1.0, linear.derivative(100.0), 1e-10, "Linear'(x) should always be 1")
    }
    
    @Test
    fun testNetworkForwardPass() {
        val layers = listOf(
            DenseLayer(3, 2, ReLUActivation(), Random(42)),
            DenseLayer(2, 1, SigmoidActivation(), Random(42))
        )
        val network = FeedforwardNetwork(layers)
        
        val input = doubleArrayOf(1.0, -1.0, 0.5)
        val output = network.forward(input)
        
        assertEquals(1, output.size, "Output should have 1 element")
        assertTrue(output[0] >= 0.0 && output[0] <= 1.0, "Sigmoid output should be in [0, 1]")
        
        // Test consistency - same input should give same output
        val output2 = network.forward(input)
        assertEquals(output[0], output2[0], 1e-10, "Same input should give same output")
    }
    
    @Test
    fun testMSELoss() {
        val mse = MSELoss()
        
        // Perfect prediction
        val perfect = doubleArrayOf(1.0, 2.0)
        assertEquals(0.0, mse.computeLoss(perfect, perfect), 1e-10, "Perfect prediction should have 0 loss")
        
        // Simple case
        val predicted = doubleArrayOf(1.0, 2.0)
        val target = doubleArrayOf(0.0, 0.0)
        val expectedLoss = (1.0 + 4.0) / 2.0 // (1² + 2²) / 2
        assertEquals(expectedLoss, mse.computeLoss(predicted, target), 1e-10)
        
        // Test gradient
        val gradient = mse.computeGradient(predicted, target)
        assertEquals(2, gradient.size)
        assertEquals(1.0, gradient[0], 1e-10) // 2 * (1-0) / 2
        assertEquals(2.0, gradient[1], 1e-10) // 2 * (2-0) / 2
    }
    
    @Test
    fun testInputValidation() {
        val layer = DenseLayer(3, 2, ReLUActivation())
        
        // Valid input
        val validInput = doubleArrayOf(1.0, 2.0, 3.0)
        layer.forward(validInput) // Should not throw
        
        // Invalid input size
        val invalidInput = doubleArrayOf(1.0, 2.0) // Too small
        assertFailsWith<IllegalArgumentException> {
            layer.forward(invalidInput)
        }
        
        // Test network construction validation
        assertFailsWith<IllegalArgumentException> {
            FeedforwardNetwork(emptyList()) // Empty layers
        }
        
        // Incompatible layer sizes
        val incompatibleLayers = listOf(
            DenseLayer(3, 4, ReLUActivation()),
            DenseLayer(5, 2, ReLUActivation()) // Input size 5 doesn't match previous output 4
        )
        assertFailsWith<IllegalArgumentException> {
            FeedforwardNetwork(incompatibleLayers)
        }
    }
    
    @Test
    fun testTrainingBatch() {
        val inputs = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        )
        val targets = arrayOf(
            doubleArrayOf(0.5),
            doubleArrayOf(1.5)
        )
        
        val batch = TrainingBatch(inputs, targets)
        assertEquals(2, batch.batchSize)
        assertEquals(inputs.size, batch.inputs.size)
        assertEquals(targets.size, batch.targets.size)
        
        // Test validation
        val mismatchedTargets = arrayOf(doubleArrayOf(0.5)) // Different size
        assertFailsWith<IllegalArgumentException> {
            TrainingBatch(inputs, mismatchedTargets)
        }
    }
    
    @Test
    fun testNetworkDeterminism() {
        // Test that network produces deterministic results
        val layers = listOf(
            DenseLayer(2, 3, ReLUActivation(), Random(42)),
            DenseLayer(3, 1, TanhActivation(), Random(42))
        )
        val network = FeedforwardNetwork(layers)
        
        val input = doubleArrayOf(0.5, -0.3)
        
        // Multiple predictions should be identical
        val predictions = (1..5).map { network.predict(input)[0] }
        val firstPrediction = predictions.first()
        
        predictions.forEach { prediction ->
            assertEquals(firstPrediction, prediction, 1e-10, "All predictions should be identical")
        }
    }
}
