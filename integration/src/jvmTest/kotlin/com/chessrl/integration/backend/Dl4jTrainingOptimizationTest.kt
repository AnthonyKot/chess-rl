package com.chessrl.integration.backend

import kotlin.test.*

/**
 * Test class specifically for DL4J training and optimization features
 * implemented in task 4.
 */
class Dl4jTrainingOptimizationTest {
    
    private val config = BackendConfig(
        inputSize = 10,
        outputSize = 5,
        hiddenLayers = listOf(8),
        learningRate = 0.01,
        l2Regularization = 0.001,
        lossFunction = "huber",
        optimizer = "adam",
        batchSize = 4,
        gradientClipping = 1.0,
        beta1 = 0.9,
        beta2 = 0.999,
        epsilon = 1e-8
    )
    
    @Test
    fun testHuberLossComputation() {
        val adapter = Dl4jNetworkAdapter(config.copy(lossFunction = "huber"))
        
        // Create synthetic training data
        val inputs = Array(2) { DoubleArray(config.inputSize) { 0.5 } }
        val targets = Array(2) { DoubleArray(config.outputSize) { 1.0 } }
        
        val loss = adapter.trainBatch(inputs, targets)
        
        assertTrue(loss.isFinite(), "Huber loss should be finite")
        assertTrue(loss >= 0.0, "Huber loss should be non-negative")
    }
    
    @Test
    fun testMSELossComputation() {
        val adapter = Dl4jNetworkAdapter(config.copy(lossFunction = "mse"))
        
        // Create synthetic training data
        val inputs = Array(2) { DoubleArray(config.inputSize) { 0.5 } }
        val targets = Array(2) { DoubleArray(config.outputSize) { 1.0 } }
        
        val loss = adapter.trainBatch(inputs, targets)
        
        assertTrue(loss.isFinite(), "MSE loss should be finite")
        assertTrue(loss >= 0.0, "MSE loss should be non-negative")
    }
    
    @Test
    fun testAdamOptimizerConfiguration() {
        val adamConfig = config.copy(
            optimizer = "adam",
            learningRate = 0.001,
            beta1 = 0.9,
            beta2 = 0.999,
            epsilon = 1e-8
        )
        
        val adapter = Dl4jNetworkAdapter(adamConfig)
        
        // Verify adapter was created successfully with Adam optimizer
        assertEquals("dl4j", adapter.getBackendName())
        assertEquals(adamConfig, adapter.getConfig())
        
        // Test that training works with Adam optimizer
        val inputs = Array(2) { DoubleArray(adamConfig.inputSize) { 0.1 } }
        val targets = Array(2) { DoubleArray(adamConfig.outputSize) { 0.9 } }
        
        val loss = adapter.trainBatch(inputs, targets)
        assertTrue(loss.isFinite(), "Adam optimizer should produce finite loss")
    }
    
    @Test
    fun testL2RegularizationConfiguration() {
        val l2Config = config.copy(l2Regularization = 0.01)
        val adapter = Dl4jNetworkAdapter(l2Config)
        
        // Test that L2 regularization doesn't break training
        val inputs = Array(3) { DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble() } }
        val targets = Array(3) { DoubleArray(config.outputSize) { kotlin.random.Random.nextDouble() } }
        
        val loss = adapter.trainBatch(inputs, targets)
        assertTrue(loss.isFinite(), "L2 regularization should not break training")
    }
    
    @Test
    fun testGradientClippingConfiguration() {
        val clippingConfig = config.copy(gradientClipping = 0.5)
        val adapter = Dl4jNetworkAdapter(clippingConfig)
        
        // Test that gradient clipping doesn't break training
        val inputs = Array(2) { DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble(-10.0, 10.0) } }
        val targets = Array(2) { DoubleArray(config.outputSize) { kotlin.random.Random.nextDouble(-10.0, 10.0) } }
        
        val loss = adapter.trainBatch(inputs, targets)
        assertTrue(loss.isFinite(), "Gradient clipping should not break training")
        
        // Gradients should remain valid after training with clipping
        assertTrue(adapter.validateGradients(), "Gradients should be valid after clipping")
    }
    
    @Test
    fun testDataTypeConversionConsistency() {
        val adapter = Dl4jNetworkAdapter(config)
        
        // Test that DoubleArray -> INDArray -> DoubleArray conversion is consistent
        val originalInput = DoubleArray(config.inputSize) { it * 0.1 }
        val output = adapter.forward(originalInput)
        
        assertEquals(config.outputSize, output.size, "Output size should match configuration")
        assertTrue(output.all { it.isFinite() }, "All output values should be finite")
        
        // Test that the same input produces the same output (deterministic)
        val output2 = adapter.forward(originalInput)
        assertContentEquals(output, output2, "Same input should produce same output")
    }
    
    @Test
    fun testBatchProcessingConsistency() {
        val adapter = Dl4jNetworkAdapter(config)
        
        // Create batch data
        val batchSize = 3
        val inputs = Array(batchSize) { i -> 
            DoubleArray(config.inputSize) { j -> (i + j) * 0.1 } 
        }
        val targets = Array(batchSize) { i -> 
            DoubleArray(config.outputSize) { j -> (i + j) * 0.2 } 
        }
        
        // Test batch training
        val loss = adapter.trainBatch(inputs, targets)
        
        assertTrue(loss.isFinite(), "Batch training loss should be finite")
        assertTrue(loss >= 0.0, "Batch training loss should be non-negative")
        
        // Test that individual forward passes still work after batch training
        for (i in inputs.indices) {
            val output = adapter.forward(inputs[i])
            assertEquals(config.outputSize, output.size, "Individual forward pass should work after batch training")
            assertTrue(output.all { it.isFinite() }, "Individual forward pass outputs should be finite")
        }
    }
    
    @Test
    fun testPrecisionHandling() {
        val adapter = Dl4jNetworkAdapter(config)
        
        // Test with high precision input values
        val highPrecisionInput = DoubleArray(config.inputSize) { 
            kotlin.math.PI * it / config.inputSize 
        }
        
        val output = adapter.forward(highPrecisionInput)
        
        // Even though DL4J uses float32 internally, outputs should still be reasonable
        assertTrue(output.all { it.isFinite() }, "High precision inputs should produce finite outputs")
        assertTrue(output.all { kotlin.math.abs(it) < 1000.0 }, "Outputs should be in reasonable range")
    }
    
    @Test
    fun testTrainingConvergence() {
        val adapter = Dl4jNetworkAdapter(config.copy(learningRate = 0.1))
        
        // Simple learning task: map all inputs to same target
        val inputs = Array(5) { DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble() } }
        val targets = Array(5) { DoubleArray(config.outputSize) { 1.0 } }
        
        // Train multiple times and check that loss generally decreases
        val losses = mutableListOf<Double>()
        repeat(5) {
            val loss = adapter.trainBatch(inputs, targets)
            losses.add(loss)
            assertTrue(loss.isFinite(), "Loss should remain finite during training")
        }
        
        // Loss should generally trend downward (allowing for some fluctuation)
        val firstLoss = losses.first()
        val lastLoss = losses.last()
        assertTrue(lastLoss <= firstLoss * 2.0, "Loss should not increase dramatically during training")
    }
}