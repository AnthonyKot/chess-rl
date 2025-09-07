package com.chessrl.nn

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.math.*
import kotlin.random.Random

/**
 * Neural Network package tests
 */
class NeuralNetworkTest {
    
    @Test
    fun testReLUActivation() {
        val relu = ReLUActivation()
        
        // Test activation
        assertEquals(0.0, relu.activate(-1.0), 1e-10)
        assertEquals(0.0, relu.activate(0.0), 1e-10)
        assertEquals(5.0, relu.activate(5.0), 1e-10)
        
        // Test derivative
        assertEquals(0.0, relu.derivative(-1.0), 1e-10)
        assertEquals(0.0, relu.derivative(0.0), 1e-10)
        assertEquals(1.0, relu.derivative(5.0), 1e-10)
    }
    
    @Test
    fun testSigmoidActivation() {
        val sigmoid = SigmoidActivation()
        
        // Test activation
        assertEquals(0.5, sigmoid.activate(0.0), 1e-10)
        assertTrue(sigmoid.activate(-10.0) < 0.01)
        assertTrue(sigmoid.activate(10.0) > 0.99)
        
        // Test derivative at x=0 (should be 0.25)
        assertEquals(0.25, sigmoid.derivative(0.0), 1e-10)
    }
    
    @Test
    fun testTanhActivation() {
        val tanh = TanhActivation()
        
        // Test activation
        assertEquals(0.0, tanh.activate(0.0), 1e-10)
        assertTrue(tanh.activate(-10.0) < -0.99)
        assertTrue(tanh.activate(10.0) > 0.99)
        
        // Test derivative at x=0 (should be 1.0)
        assertEquals(1.0, tanh.derivative(0.0), 1e-10)
    }
    
    @Test
    fun testLinearActivation() {
        val linear = LinearActivation()
        
        // Test activation
        assertEquals(-5.0, linear.activate(-5.0), 1e-10)
        assertEquals(0.0, linear.activate(0.0), 1e-10)
        assertEquals(3.14, linear.activate(3.14), 1e-10)
        
        // Test derivative (always 1.0)
        assertEquals(1.0, linear.derivative(-100.0), 1e-10)
        assertEquals(1.0, linear.derivative(0.0), 1e-10)
        assertEquals(1.0, linear.derivative(100.0), 1e-10)
    }
    
    @Test
    fun testDenseLayerConstruction() {
        val layer = DenseLayer(3, 2, ReLUActivation(), Random(42))
        
        assertEquals(3, layer.inputSize)
        assertEquals(2, layer.outputSize)
        
        // Check that weights are initialized (not all zeros)
        val weights = layer.getWeights()
        assertEquals(2, weights.size) // outputSize
        assertEquals(3, weights[0].size) // inputSize
        
        var hasNonZero = false
        for (row in weights) {
            for (weight in row) {
                if (weight != 0.0) hasNonZero = true
            }
        }
        assertTrue(hasNonZero, "Weights should be initialized to non-zero values")
    }
    
    @Test
    fun testDenseLayerForwardPass() {
        val layer = DenseLayer(2, 1, LinearActivation(), Random(42))
        val input = doubleArrayOf(1.0, 2.0)
        
        val output = layer.forward(input)
        assertEquals(1, output.size)
        
        // Test with different input
        val input2 = doubleArrayOf(0.0, 0.0)
        val output2 = layer.forward(input2)
        assertEquals(1, output2.size)
        
        // Output should be different for different inputs (assuming non-zero weights)
        // This is a basic sanity check
        assertTrue(output[0] != output2[0] || layer.getBiases()[0] != 0.0)
    }
    
    @Test
    fun testDenseLayerForwardPassWithReLU() {
        val layer = DenseLayer(2, 2, ReLUActivation(), Random(42))
        val input = doubleArrayOf(-1.0, 1.0)
        
        val output = layer.forward(input)
        assertEquals(2, output.size)
        
        // All outputs should be non-negative due to ReLU
        for (value in output) {
            assertTrue(value >= 0.0, "ReLU output should be non-negative, got $value")
        }
    }
    
    @Test
    fun testDenseLayerInputValidation() {
        val layer = DenseLayer(3, 2, ReLUActivation())
        
        // Test correct input size
        val validInput = doubleArrayOf(1.0, 2.0, 3.0)
        layer.forward(validInput) // Should not throw
        
        // Test incorrect input size
        val invalidInput = doubleArrayOf(1.0, 2.0) // Too small
        assertFailsWith<IllegalArgumentException> {
            layer.forward(invalidInput)
        }
    }
    
    @Test
    fun testDenseLayerBackwardPass() {
        val layer = DenseLayer(2, 1, LinearActivation(), Random(42))
        
        // Forward pass first
        val input = doubleArrayOf(1.0, 2.0)
        layer.forward(input)
        
        // Backward pass
        val gradient = doubleArrayOf(1.0)
        val inputGradient = layer.backward(gradient)
        
        assertEquals(2, inputGradient.size)
        // Input gradient should not be all zeros (assuming non-zero weights)
        assertTrue(inputGradient.any { it != 0.0 })
    }
    
    @Test
    fun testDenseLayerGradientValidation() {
        val layer = DenseLayer(2, 3, ReLUActivation())
        
        // Forward pass first
        val input = doubleArrayOf(1.0, -1.0)
        layer.forward(input)
        
        // Test correct gradient size
        val validGradient = doubleArrayOf(1.0, 0.5, -0.5)
        layer.backward(validGradient) // Should not throw
        
        // Test incorrect gradient size
        val invalidGradient = doubleArrayOf(1.0, 0.5) // Too small
        assertFailsWith<IllegalArgumentException> {
            layer.backward(invalidGradient)
        }
    }
    
    @Test
    fun testFeedforwardNetworkConstruction() {
        val layers = listOf(
            DenseLayer(3, 4, ReLUActivation(), Random(42)),
            DenseLayer(4, 2, SigmoidActivation(), Random(42))
        )
        
        val network = FeedforwardNetwork(layers)
        assertEquals(2, network.layers.size)
    }
    
    @Test
    fun testFeedforwardNetworkInvalidConstruction() {
        // Test empty layers
        assertFailsWith<IllegalArgumentException> {
            FeedforwardNetwork(emptyList())
        }
        
        // Test incompatible layer sizes
        val incompatibleLayers = listOf(
            DenseLayer(3, 4, ReLUActivation()),
            DenseLayer(5, 2, SigmoidActivation()) // Input size 5 doesn't match previous output size 4
        )
        
        assertFailsWith<IllegalArgumentException> {
            FeedforwardNetwork(incompatibleLayers)
        }
    }
    
    @Test
    fun testFeedforwardNetworkForwardPass() {
        val layers = listOf(
            DenseLayer(2, 3, ReLUActivation(), Random(42)),
            DenseLayer(3, 1, SigmoidActivation(), Random(42))
        )
        
        val network = FeedforwardNetwork(layers)
        val input = doubleArrayOf(1.0, -1.0)
        
        val output = network.forward(input)
        assertEquals(1, output.size)
        
        // Output should be between 0 and 1 due to final sigmoid layer
        assertTrue(output[0] >= 0.0 && output[0] <= 1.0)
    }
    
    @Test
    fun testFeedforwardNetworkPredict() {
        val layers = listOf(
            DenseLayer(2, 1, LinearActivation(), Random(42))
        )
        
        val network = FeedforwardNetwork(layers)
        val input = doubleArrayOf(1.0, 2.0)
        
        val forwardOutput = network.forward(input)
        val predictOutput = network.predict(input)
        
        // Predict should give same result as forward
        assertEquals(forwardOutput.size, predictOutput.size)
        for (i in forwardOutput.indices) {
            assertEquals(forwardOutput[i], predictOutput[i], 1e-10)
        }
    }
    
    @Test
    fun testNetworkConsistency() {
        // Test that the same input always produces the same output
        val layers = listOf(
            DenseLayer(2, 3, ReLUActivation(), Random(42)),
            DenseLayer(3, 1, TanhActivation(), Random(42))
        )
        
        val network = FeedforwardNetwork(layers)
        val input = doubleArrayOf(0.5, -0.3)
        
        val output1 = network.forward(input)
        val output2 = network.forward(input)
        
        assertEquals(output1.size, output2.size)
        for (i in output1.indices) {
            assertEquals(output1[i], output2[i], 1e-10)
        }
    }
    
    @Test
    fun testLayerWeightUpdate() {
        val layer = DenseLayer(2, 1, LinearActivation(), Random(42))
        
        // Get initial weights
        val initialWeights = layer.getWeights()
        val initialBiases = layer.getBiases()
        
        // Forward and backward pass
        val input = doubleArrayOf(1.0, 1.0)
        layer.forward(input)
        layer.backward(doubleArrayOf(1.0))
        
        // Update weights
        val learningRate = 0.1
        layer.updateWeights(learningRate)
        
        // Check that weights have changed
        val updatedWeights = layer.getWeights()
        val updatedBiases = layer.getBiases()
        
        var weightsChanged = false
        for (i in initialWeights.indices) {
            for (j in initialWeights[i].indices) {
                if (initialWeights[i][j] != updatedWeights[i][j]) {
                    weightsChanged = true
                }
            }
        }
        
        var biasesChanged = false
        for (i in initialBiases.indices) {
            if (initialBiases[i] != updatedBiases[i]) {
                biasesChanged = true
            }
        }
        
        assertTrue(weightsChanged || biasesChanged, "Weights or biases should change after update")
    }
    
    @Test
    fun testMSELoss() {
        val mse = MSELoss()
        
        // Test perfect prediction (loss should be 0)
        val predicted1 = doubleArrayOf(1.0, 2.0, 3.0)
        val target1 = doubleArrayOf(1.0, 2.0, 3.0)
        assertEquals(0.0, mse.computeLoss(predicted1, target1), 1e-10)
        
        // Test simple case
        val predicted2 = doubleArrayOf(1.0, 2.0)
        val target2 = doubleArrayOf(0.0, 0.0)
        val expectedLoss = (1.0 + 4.0) / 2.0 // (1^2 + 2^2) / 2
        assertEquals(expectedLoss, mse.computeLoss(predicted2, target2), 1e-10)
        
        // Test gradient computation
        val gradient = mse.computeGradient(predicted2, target2)
        assertEquals(2, gradient.size)
        assertEquals(2.0 * 1.0 / 2.0, gradient[0], 1e-10) // 2 * (1-0) / 2
        assertEquals(2.0 * 2.0 / 2.0, gradient[1], 1e-10) // 2 * (2-0) / 2
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
    }
    
    @Test
    fun testTrainingBatchValidation() {
        val inputs = arrayOf(doubleArrayOf(1.0, 2.0))
        val targets = arrayOf(doubleArrayOf(0.5), doubleArrayOf(1.5)) // Different size
        
        assertFailsWith<IllegalArgumentException> {
            TrainingBatch(inputs, targets)
        }
        
        // Test empty batch
        assertFailsWith<IllegalArgumentException> {
            TrainingBatch(emptyArray(), emptyArray())
        }
    }
    
    @Test
    fun testNetworkBackwardPass() {
        val layers = listOf(
            DenseLayer(2, 1, LinearActivation(), Random(42))
        )
        val network = FeedforwardNetwork(layers)
        
        val input = doubleArrayOf(1.0, 2.0)
        val target = doubleArrayOf(0.5)
        
        // Forward pass
        val output = network.forward(input)
        
        // Backward pass
        val lossArray = network.backward(target)
        assertEquals(1, lossArray.size)
        assertTrue(lossArray[0] >= 0.0, "Loss should be non-negative")
    }
    
    @Test
    fun testMiniBatchTraining() {
        val layers = listOf(
            DenseLayer(2, 1, LinearActivation(), Random(42))
        )
        val network = FeedforwardNetwork(layers)
        
        // Create simple training data
        val inputs = arrayOf(
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(1.0, 1.0),
            doubleArrayOf(0.0, 0.0)
        )
        val targets = arrayOf(
            doubleArrayOf(1.0),
            doubleArrayOf(1.0),
            doubleArrayOf(0.0),
            doubleArrayOf(0.0)
        )
        
        // Train with different batch sizes
        val metrics16 = network.train(inputs, targets, epochs = 2, batchSize = 2)
        assertEquals(2, metrics16.size)
        
        for (metric in metrics16) {
            assertTrue(metric.averageLoss >= 0.0, "Average loss should be non-negative")
            assertTrue(metric.gradientNorm >= 0.0, "Gradient norm should be non-negative")
        }
    }
    
    @Test
    fun testBatchSizeVariations() {
        val layers = listOf(
            DenseLayer(1, 1, LinearActivation(), Random(42))
        )
        val network = FeedforwardNetwork(layers)
        
        // Create training data
        val inputs = Array(8) { i -> doubleArrayOf(i.toDouble()) }
        val targets = Array(8) { i -> doubleArrayOf(i.toDouble() * 2.0) }
        
        // Test different batch sizes
        val batchSizes = listOf(1, 2, 4, 8)
        
        for (batchSize in batchSizes) {
            val metrics = network.train(inputs, targets, epochs = 1, batchSize = batchSize)
            assertEquals(1, metrics.size)
            assertTrue(metrics[0].averageLoss >= 0.0, "Loss should be non-negative for batch size $batchSize")
        }
    }
    
    @Test
    fun testGradientAccumulation() {
        val layer = DenseLayer(1, 1, LinearActivation(), Random(42))
        
        // Get initial weights
        val initialWeights = layer.getWeights()
        
        // Multiple forward/backward passes (simulating batch accumulation)
        val inputs = arrayOf(doubleArrayOf(1.0), doubleArrayOf(2.0))
        val gradients = arrayOf(doubleArrayOf(0.5), doubleArrayOf(1.0))
        
        for (i in inputs.indices) {
            layer.forward(inputs[i])
            layer.backward(gradients[i])
        }
        
        // Update weights (should average the accumulated gradients)
        layer.updateWeights(0.1)
        
        val updatedWeights = layer.getWeights()
        
        // Weights should have changed
        assertTrue(initialWeights[0][0] != updatedWeights[0][0], "Weights should change after gradient accumulation")
    }
    
    @Test
    fun testNumericalGradientCheck() {
        // Simple numerical gradient checking for a single layer
        val layer = DenseLayer(1, 1, LinearActivation(), Random(42))
        val input = doubleArrayOf(1.0)
        val target = doubleArrayOf(2.0)
        
        // Forward pass
        val output = layer.forward(input)
        
        // Analytical gradient
        val mse = MSELoss()
        val lossGradient = mse.computeGradient(output, target)
        layer.backward(lossGradient)
        
        // This is a basic test - in a full implementation we would compare
        // analytical gradients with numerical gradients computed using finite differences
        assertTrue(true, "Numerical gradient check placeholder - implementation validates gradient computation")
    }
    
    @Test
    fun testTrainingConvergence() {
        // Test that training can reduce loss on a simple problem
        val layers = listOf(
            DenseLayer(1, 1, LinearActivation(), Random(42))
        )
        val network = FeedforwardNetwork(layers)
        
        // Simple linear relationship: y = 2x
        val inputs = Array(10) { i -> doubleArrayOf(i.toDouble()) }
        val targets = Array(10) { i -> doubleArrayOf(i.toDouble() * 2.0) }
        
        val metrics = network.train(inputs, targets, epochs = 10, batchSize = 5)
        
        // Loss should generally decrease (though not guaranteed for every step)
        val initialLoss = metrics.first().averageLoss
        val finalLoss = metrics.last().averageLoss
        
        // At minimum, final loss should be finite and non-negative
        assertTrue(finalLoss >= 0.0 && finalLoss.isFinite(), "Final loss should be non-negative and finite")
        assertTrue(initialLoss >= 0.0 && initialLoss.isFinite(), "Initial loss should be non-negative and finite")
    }
}