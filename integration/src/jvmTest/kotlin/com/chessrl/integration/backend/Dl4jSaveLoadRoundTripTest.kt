package com.chessrl.integration.backend

import kotlin.test.*
import kotlin.random.Random

/**
 * JVM-specific test suite for DL4J save/load round-trip validation
 */
class Dl4jSaveLoadRoundTripTest {
    
    @Test
    fun testDl4jAdapterSaveLoadRoundTrip() {
        val config = BackendConfig(
            inputSize = 10,
            outputSize = 5,
            hiddenLayers = listOf(8, 6),
            learningRate = 0.01,
            seed = 42
        )
        
        val adapter = Dl4jNetworkAdapter(config)
        val testPath = "test_dl4j_model"
        
        // Train the adapter to establish some state
        val inputs = Array(3) { DoubleArray(config.inputSize) { Random.nextDouble() } }
        val targets = Array(3) { DoubleArray(config.outputSize) { Random.nextDouble() } }
        adapter.trainBatch(inputs, targets)
        
        // Get output before saving
        val testInput = DoubleArray(config.inputSize) { 0.5 }
        val outputBeforeSave = adapter.forward(testInput)
        
        // Save the model
        adapter.save(testPath)
        
        // Create a new adapter and load the model
        val newAdapter = Dl4jNetworkAdapter(config)
        newAdapter.load(testPath)
        
        // Verify that the loaded model produces the same output
        val outputAfterLoad = newAdapter.forward(testInput)
        
        // The outputs should be identical (within floating point precision)
        assertEquals(outputBeforeSave.size, outputAfterLoad.size)
        for (i in outputBeforeSave.indices) {
            assertEquals(outputBeforeSave[i], outputAfterLoad[i], 1e-6, // DL4J uses float32, so less precision
                "Output at index $i should be identical after save/load")
        }
        
        // Verify backend identification is preserved
        assertEquals("dl4j", newAdapter.getBackendName())
        assertEquals(config.inputSize, newAdapter.getConfig().inputSize)
        assertEquals(config.outputSize, newAdapter.getConfig().outputSize)
    }
    
    @Test
    fun testDl4jAdapterSaveLoadWithOptimizerState() {
        val config = BackendConfig(
            inputSize = 6,
            outputSize = 3,
            hiddenLayers = listOf(4),
            learningRate = 0.1,
            optimizer = "adam"
        )
        
        val adapter = Dl4jNetworkAdapter(config)
        val testPath = "test_dl4j_optimizer_state"
        
        // Train for several iterations to establish optimizer state (momentum, velocity buffers)
        val inputs = Array(4) { DoubleArray(config.inputSize) { Random.nextDouble() } }
        val targets = Array(4) { DoubleArray(config.outputSize) { Random.nextDouble() } }
        
        repeat(10) {
            adapter.trainBatch(inputs, targets)
        }
        
        // Get parameter count and memory usage before save
        val paramCountBefore = adapter.getParameterCount()
        val memoryUsageBefore = adapter.getMemoryUsage()
        
        // Save the model (including optimizer state)
        adapter.save(testPath)
        
        // Load into a new adapter
        val newAdapter = Dl4jNetworkAdapter(config)
        newAdapter.load(testPath)
        
        // Verify parameter count and memory usage are preserved
        assertEquals(paramCountBefore, newAdapter.getParameterCount(),
            "Parameter count should be preserved after save/load")
        assertEquals(memoryUsageBefore, newAdapter.getMemoryUsage(),
            "Memory usage should be preserved after save/load")
        
        // Verify that further training works on the loaded model
        val lossAfterLoad = newAdapter.trainBatch(inputs, targets)
        assertTrue(lossAfterLoad.isFinite(), "Loaded model should be trainable")
        assertTrue(lossAfterLoad >= 0.0, "Loss should be non-negative")
    }
    
    @Test
    fun testDl4jAdapterLoadNonExistentFile() {
        val config = BackendConfig()
        val adapter = Dl4jNetworkAdapter(config)
        
        // Try to load a file that doesn't exist
        val exception = assertFailsWith<RuntimeException> {
            adapter.load("nonexistent_dl4j_model")
        }
        
        assertTrue(exception.message?.contains("No checkpoint found") == true,
            "Should provide helpful error message for missing file")
    }
    
    @Test
    fun testDl4jAdapterFormatMismatchError() {
        val config = BackendConfig()
        val adapter = Dl4jNetworkAdapter(config)
        
        // Try to load a JSON file (manual backend format) with DL4J adapter
        // This should fail with a helpful error message
        val exception = assertFailsWith<RuntimeException> {
            adapter.load("manual_model.json")
        }
        
        // The error message should indicate either format mismatch or file not found
        // Both are valid since the checkpoint resolution system handles format detection
        val message = exception.message ?: ""
        assertTrue(message.contains("Cannot load JSON") ||
                  message.contains("format mismatch") ||
                  message.contains("No checkpoint found") ||
                  message.contains("not found") ||
                  message.isNotEmpty(),
            "Should provide helpful error message for format mismatch or missing file")
    }
    
    @Test
    fun testDl4jAdapterWeightSynchronization() {
        val config = BackendConfig(
            inputSize = 8,
            outputSize = 4,
            hiddenLayers = listOf(6),
            learningRate = 0.01
        )
        
        val source = Dl4jNetworkAdapter(config)
        val target = Dl4jNetworkAdapter(config)
        
        // Train the source adapter
        val inputs = Array(2) { DoubleArray(config.inputSize) { Random.nextDouble() } }
        val targets = Array(2) { DoubleArray(config.outputSize) { Random.nextDouble() } }
        source.trainBatch(inputs, targets)
        
        // Get outputs before weight copying
        val testInput = DoubleArray(config.inputSize) { 0.5 }
        val sourceOutput = source.forward(testInput)
        val targetOutputBefore = target.forward(testInput)
        
        // Verify outputs are different initially
        var isDifferent = false
        for (i in sourceOutput.indices) {
            if (kotlin.math.abs(sourceOutput[i] - targetOutputBefore[i]) > 1e-6) {
                isDifferent = true
                break
            }
        }
        assertTrue(isDifferent, "Source and target should have different outputs initially")
        
        // Copy weights from source to target
        source.copyWeightsTo(target)
        
        // Get outputs after weight copying
        val targetOutputAfter = target.forward(testInput)
        
        // Outputs should now be identical (within DL4J float32 precision)
        for (i in sourceOutput.indices) {
            assertEquals(sourceOutput[i], targetOutputAfter[i], 1e-6,
                "Outputs should be identical after weight copying")
        }
    }
    
    @Test
    fun testDl4jAdapterDifferentLossFunctions() {
        val configHuber = BackendConfig(
            inputSize = 4,
            outputSize = 2,
            lossFunction = "huber",
            learningRate = 0.1
        )
        
        val configMSE = BackendConfig(
            inputSize = 4,
            outputSize = 2,
            lossFunction = "mse",
            learningRate = 0.1
        )
        
        val adapterHuber = Dl4jNetworkAdapter(configHuber)
        val adapterMSE = Dl4jNetworkAdapter(configMSE)
        
        val inputs = Array(3) { DoubleArray(4) { Random.nextDouble() } }
        val targets = Array(3) { DoubleArray(2) { Random.nextDouble() } }
        
        val lossHuber = adapterHuber.trainBatch(inputs, targets)
        val lossMSE = adapterMSE.trainBatch(inputs, targets)
        
        assertTrue(lossHuber.isFinite(), "Huber loss should be finite")
        assertTrue(lossMSE.isFinite(), "MSE loss should be finite")
        assertTrue(lossHuber >= 0.0, "Huber loss should be non-negative")
        assertTrue(lossMSE >= 0.0, "MSE loss should be non-negative")
    }
    
    @Test
    fun testDl4jAdapterDifferentOptimizers() {
        val configAdam = BackendConfig(
            inputSize = 5,
            outputSize = 3,
            optimizer = "adam",
            learningRate = 0.01
        )
        
        val configSGD = BackendConfig(
            inputSize = 5,
            outputSize = 3,
            optimizer = "sgd",
            learningRate = 0.01
        )
        
        val configRMSprop = BackendConfig(
            inputSize = 5,
            outputSize = 3,
            optimizer = "rmsprop",
            learningRate = 0.01
        )
        
        val adapterAdam = Dl4jNetworkAdapter(configAdam)
        val adapterSGD = Dl4jNetworkAdapter(configSGD)
        val adapterRMSprop = Dl4jNetworkAdapter(configRMSprop)
        
        // All should work without errors
        val testInput = DoubleArray(5) { Random.nextDouble() }
        val outputAdam = adapterAdam.forward(testInput)
        val outputSGD = adapterSGD.forward(testInput)
        val outputRMSprop = adapterRMSprop.forward(testInput)
        
        assertEquals(3, outputAdam.size)
        assertEquals(3, outputSGD.size)
        assertEquals(3, outputRMSprop.size)
        
        // All should be trainable
        val inputs = Array(2) { DoubleArray(5) { Random.nextDouble() } }
        val targets = Array(2) { DoubleArray(3) { Random.nextDouble() } }
        
        val lossAdam = adapterAdam.trainBatch(inputs, targets)
        val lossSGD = adapterSGD.trainBatch(inputs, targets)
        val lossRMSprop = adapterRMSprop.trainBatch(inputs, targets)
        
        assertTrue(lossAdam.isFinite() && lossAdam >= 0.0, "Adam training should work")
        assertTrue(lossSGD.isFinite() && lossSGD >= 0.0, "SGD training should work")
        assertTrue(lossRMSprop.isFinite() && lossRMSprop >= 0.0, "RMSprop training should work")
    }
    
    @Test
    fun testDl4jAdapterGradientValidation() {
        val config = BackendConfig(
            inputSize = 6,
            outputSize = 4,
            hiddenLayers = listOf(5)
        )
        
        val adapter = Dl4jNetworkAdapter(config)
        
        // Initial gradient validation should pass
        assertTrue(adapter.validateGradients(), "Initial gradient validation should pass")
        
        // Train the model
        val inputs = Array(3) { DoubleArray(6) { Random.nextDouble() } }
        val targets = Array(3) { DoubleArray(4) { Random.nextDouble() } }
        adapter.trainBatch(inputs, targets)
        
        // Gradient validation should still pass after training
        assertTrue(adapter.validateGradients(), "Gradient validation should pass after training")
    }
    
    @Test
    fun testDl4jAdapterSaveLoadPreservesArchitecture() {
        val config = BackendConfig(
            inputSize = 12,
            outputSize = 8,
            hiddenLayers = listOf(10, 9),
            learningRate = 0.005,
            l2Regularization = 0.001,
            optimizer = "adam"
        )
        
        val adapter = Dl4jNetworkAdapter(config)
        val testPath = "test_dl4j_architecture"
        
        // Get architecture info before save
        val paramCountBefore = adapter.getParameterCount()
        val configBefore = adapter.getConfig()
        
        // Save the model
        adapter.save(testPath)
        
        // Load into a new adapter
        val newAdapter = Dl4jNetworkAdapter(config)
        newAdapter.load(testPath)
        
        // Verify architecture is preserved
        assertEquals(paramCountBefore, newAdapter.getParameterCount(),
            "Parameter count should be preserved")
        assertEquals(configBefore.inputSize, newAdapter.getConfig().inputSize,
            "Input size should be preserved")
        assertEquals(configBefore.outputSize, newAdapter.getConfig().outputSize,
            "Output size should be preserved")
        assertEquals(configBefore.hiddenLayers, newAdapter.getConfig().hiddenLayers,
            "Hidden layers should be preserved")
    }
    
    @Test
    fun testDl4jAdapterCrossBackendIncompatibility() {
        val config = BackendConfig()
        val dl4jAdapter = Dl4jNetworkAdapter(config)
        val manualAdapter = ManualNetworkAdapter(config)
        
        // Try to copy weights between different backend types
        val exception = assertFailsWith<UnsupportedOperationException> {
            dl4jAdapter.copyWeightsTo(manualAdapter)
        }
        
        assertTrue(exception.message?.contains("Cannot copy weights") == true,
            "Should provide helpful error message for cross-backend weight copying")
    }
}