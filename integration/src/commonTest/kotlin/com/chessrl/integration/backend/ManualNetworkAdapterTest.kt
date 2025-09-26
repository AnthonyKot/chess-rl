package com.chessrl.integration.backend

import kotlin.test.*
import kotlin.random.Random

class ManualNetworkAdapterTest {
    
    @Test
    fun testBasicForwardPass() {
        val config = BackendConfig(
            inputSize = 10,
            outputSize = 5,
            hiddenLayers = listOf(8),
            learningRate = 0.01
        )
        
        val adapter = ManualNetworkAdapter(config)
        
        // Test forward pass
        val input = DoubleArray(10) { Random.nextDouble() }
        val output = adapter.forward(input)
        
        assertEquals(5, output.size, "Output size should match config")
        assertTrue(output.all { it.isFinite() }, "All outputs should be finite")
    }
    
    @Test
    fun testInputSizeValidation() {
        val config = BackendConfig(inputSize = 10, outputSize = 5)
        val adapter = ManualNetworkAdapter(config)
        
        // Test with wrong input size
        val wrongInput = DoubleArray(8) { Random.nextDouble() }
        
        assertFailsWith<IllegalArgumentException> {
            adapter.forward(wrongInput)
        }
    }
    
    @Test
    fun testTrainBatch() {
        val config = BackendConfig(
            inputSize = 4,
            outputSize = 2,
            hiddenLayers = listOf(3),
            learningRate = 0.1
        )
        
        val adapter = ManualNetworkAdapter(config)
        
        // Create simple training batch
        val batchSize = 3
        val inputs = Array(batchSize) { DoubleArray(4) { Random.nextDouble() } }
        val targets = Array(batchSize) { DoubleArray(2) { Random.nextDouble() } }
        
        val loss = adapter.trainBatch(inputs, targets)
        
        assertTrue(loss.isFinite(), "Loss should be finite")
        assertTrue(loss >= 0.0, "Loss should be non-negative")
    }
    
    @Test
    fun testBackendIdentification() {
        val config = BackendConfig()
        val adapter = ManualNetworkAdapter(config)
        
        assertEquals("manual", adapter.getBackendName())
        assertEquals(config, adapter.getConfig())
    }
    
    @Test
    fun testParameterCount() {
        val config = BackendConfig(
            inputSize = 10,
            outputSize = 5,
            hiddenLayers = listOf(8)
        )
        
        val adapter = ManualNetworkAdapter(config)
        val paramCount = adapter.getParameterCount()
        
        // Expected: (10*8 + 8) + (8*5 + 5) = 80 + 8 + 40 + 5 = 133
        val expectedParams = (10 * 8 + 8) + (8 * 5 + 5)
        assertEquals(expectedParams.toLong(), paramCount)
    }
    
    @Test
    fun testWeightSynchronization() {
        val config = BackendConfig(
            inputSize = 4,
            outputSize = 2,
            hiddenLayers = listOf(3)
        )
        
        val source = ManualNetworkAdapter(config)
        val target = ManualNetworkAdapter(config)
        
        // Get initial outputs (should be different due to random initialization)
        val testInput = DoubleArray(4) { Random.nextDouble() }
        val sourceOutput1 = source.forward(testInput)
        val targetOutput1 = target.forward(testInput)
        
        // Copy weights
        source.copyWeightsTo(target)
        
        // Get outputs after weight copying (should be identical)
        val sourceOutput2 = source.forward(testInput)
        val targetOutput2 = target.forward(testInput)
        
        // Outputs should now be identical
        for (i in sourceOutput2.indices) {
            assertEquals(sourceOutput2[i], targetOutput2[i], 1e-10, 
                "Outputs should be identical after weight copying")
        }
    }
    
    @Test
    fun testGradientValidation() {
        val config = BackendConfig(
            inputSize = 5,
            outputSize = 3,
            hiddenLayers = listOf(4)
        )
        
        val adapter = ManualNetworkAdapter(config)
        
        assertTrue(adapter.validateGradients(), "Gradient validation should pass")
    }
    
    @Test
    fun testDifferentLossFunctions() {
        val configHuber = BackendConfig(
            inputSize = 3,
            outputSize = 2,
            lossFunction = "huber"
        )
        
        val configMSE = BackendConfig(
            inputSize = 3,
            outputSize = 2,
            lossFunction = "mse"
        )
        
        val adapterHuber = ManualNetworkAdapter(configHuber)
        val adapterMSE = ManualNetworkAdapter(configMSE)
        
        val inputs = arrayOf(DoubleArray(3) { Random.nextDouble() })
        val targets = arrayOf(DoubleArray(2) { Random.nextDouble() })
        
        val lossHuber = adapterHuber.trainBatch(inputs, targets)
        val lossMSE = adapterMSE.trainBatch(inputs, targets)
        
        assertTrue(lossHuber.isFinite(), "Huber loss should be finite")
        assertTrue(lossMSE.isFinite(), "MSE loss should be finite")
    }
    
    @Test
    fun testDifferentOptimizers() {
        val configAdam = BackendConfig(
            inputSize = 3,
            outputSize = 2,
            optimizer = "adam"
        )
        
        val configSGD = BackendConfig(
            inputSize = 3,
            outputSize = 2,
            optimizer = "sgd"
        )
        
        val adapterAdam = ManualNetworkAdapter(configAdam)
        val adapterSGD = ManualNetworkAdapter(configSGD)
        
        // Both should work without errors
        val testInput = DoubleArray(3) { Random.nextDouble() }
        val outputAdam = adapterAdam.forward(testInput)
        val outputSGD = adapterSGD.forward(testInput)
        
        assertEquals(2, outputAdam.size)
        assertEquals(2, outputSGD.size)
    }
    
    @Test
    fun testBatchTrainingActuallyUpdatesWeights() {
        val config = BackendConfig(
            inputSize = 10,
            outputSize = 5,
            hiddenLayers = listOf(8),
            learningRate = 0.1 // Higher learning rate for more visible changes
        )
        val adapter = ManualNetworkAdapter(config)
        
        // Get initial output
        val input = DoubleArray(config.inputSize) { 0.1 }
        val initialOutput = adapter.forward(input)
        
        // Train on a batch that should change the weights
        val inputs = Array(4) { DoubleArray(config.inputSize) { 0.1 } }
        val targets = Array(4) { DoubleArray(config.outputSize) { 1.0 } } // Different from initial output
        
        // Train multiple times to ensure weight updates
        repeat(10) {
            adapter.trainBatch(inputs, targets)
        }
        
        // Get output after training
        val finalOutput = adapter.forward(input)
        
        // Verify that the output has changed (weights were updated)
        var hasChanged = false
        for (i in initialOutput.indices) {
            if (kotlin.math.abs(initialOutput[i] - finalOutput[i]) > 1e-6) {
                hasChanged = true
                break
            }
        }
        
        assertTrue(hasChanged, "Network output should change after training, indicating weights were updated")
    }
    
    @Test
    fun testEnhancedSaveLoadWithOptimizerState() {
        val config = BackendConfig(
            inputSize = 10,
            outputSize = 5,
            hiddenLayers = listOf(8),
            learningRate = 0.01
        )
        val adapter = ManualNetworkAdapter(config)
        val tempPath = "test_model_with_optimizer"
        
        // Train the adapter a bit to establish some state
        val inputs = Array(2) { DoubleArray(config.inputSize) { Random.nextDouble() } }
        val targets = Array(2) { DoubleArray(config.outputSize) { Random.nextDouble() } }
        adapter.trainBatch(inputs, targets)
        
        // Get output before saving
        val testInput = DoubleArray(config.inputSize) { 0.5 }
        val outputBeforeSave = adapter.forward(testInput)
        
        // Save the model
        adapter.save(tempPath)
        
        // Create a new adapter and load the model
        val newAdapter = ManualNetworkAdapter(config)
        newAdapter.load(tempPath)
        
        // Verify that the loaded model produces the same output
        val outputAfterLoad = newAdapter.forward(testInput)
        
        // The outputs should be identical (or very close due to floating point precision)
        for (i in outputBeforeSave.indices) {
            assertEquals(outputBeforeSave[i], outputAfterLoad[i], 1e-10, 
                "Output at index $i should be identical after save/load")
        }
        
        // Clean up test files (this would be platform-specific in a real implementation)
        // For now, we just verify the functionality works
    }
}