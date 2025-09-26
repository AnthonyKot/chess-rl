package com.chessrl.integration.backend

import kotlin.test.*
import kotlin.random.Random

/**
 * Test suite for checkpoint compatibility and save/load round-trip validation
 */
class CheckpointSaveLoadTest {
    
    @Test
    fun testCheckpointFormatDetection() {
        // Test format detection from file paths
        assertEquals(BackendType.MANUAL, CheckpointCompatibility.detectBackendFromFile("model.json"))
        assertEquals(BackendType.MANUAL, CheckpointCompatibility.detectBackendFromFile("checkpoint.json.gz"))
        assertEquals(BackendType.DL4J, CheckpointCompatibility.detectBackendFromFile("model.zip"))
        assertEquals(BackendType.KOTLINDL, CheckpointCompatibility.detectBackendFromFile("model.h5"))
        assertEquals(BackendType.KOTLINDL, CheckpointCompatibility.detectBackendFromFile("model.savedmodel"))
        assertEquals(null, CheckpointCompatibility.detectBackendFromFile("model.unknown"))
    }
    
    @Test
    fun testFormatCompatibility() {
        // Test format compatibility checking
        assertTrue(CheckpointCompatibility.isFormatCompatible("model.json", BackendType.MANUAL))
        assertTrue(CheckpointCompatibility.isFormatCompatible("model.zip", BackendType.DL4J))
        assertFalse(CheckpointCompatibility.isFormatCompatible("model.json", BackendType.DL4J))
        assertFalse(CheckpointCompatibility.isFormatCompatible("model.zip", BackendType.MANUAL))
    }
    
    @Test
    fun testSavePathGeneration() {
        // Test save path generation for different backends
        assertEquals("model.json", CheckpointCompatibility.generateSavePath("model", BackendType.MANUAL))
        assertEquals("model.zip", CheckpointCompatibility.generateSavePath("model", BackendType.DL4J))
        assertEquals("model.h5", CheckpointCompatibility.generateSavePath("model", BackendType.KOTLINDL))
        
        // Test with existing extensions (should be replaced)
        assertEquals("model.json", CheckpointCompatibility.generateSavePath("model.zip", BackendType.MANUAL))
        assertEquals("model.zip", CheckpointCompatibility.generateSavePath("model.json", BackendType.DL4J))
    }
    
    @Test
    fun testFormatMismatchMessages() {
        // Test helpful error messages for format mismatches
        val jsonToDl4jMessage = CheckpointCompatibility.getFormatMismatchMessage("model.json", BackendType.DL4J)
        assertTrue(jsonToDl4jMessage.contains("Cannot load JSON checkpoint with DL4J backend"))
        assertTrue(jsonToDl4jMessage.contains("--nn manual"))
        
        val zipToManualMessage = CheckpointCompatibility.getFormatMismatchMessage("model.zip", BackendType.MANUAL)
        assertTrue(zipToManualMessage.contains("Cannot load ZIP checkpoint with manual backend"))
        assertTrue(zipToManualMessage.contains("--nn dl4j"))
        
        val unknownFormatMessage = CheckpointCompatibility.getFormatMismatchMessage("model.unknown", BackendType.MANUAL)
        assertTrue(unknownFormatMessage.contains("Unknown checkpoint format"))
    }
    
    @Test
    fun testManualAdapterSaveLoadRoundTrip() {
        val config = BackendConfig(
            inputSize = 10,
            outputSize = 5,
            hiddenLayers = listOf(8, 6),
            learningRate = 0.01,
            seed = 42
        )
        
        val adapter = ManualNetworkAdapter(config)
        val testPath = "test_manual_model"
        
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
        val newAdapter = ManualNetworkAdapter(config)
        newAdapter.load(testPath)
        
        // Verify that the loaded model produces the same output
        val outputAfterLoad = newAdapter.forward(testInput)
        
        // The outputs should be identical (within floating point precision)
        assertEquals(outputBeforeSave.size, outputAfterLoad.size)
        for (i in outputBeforeSave.indices) {
            assertEquals(outputBeforeSave[i], outputAfterLoad[i], 1e-10, 
                "Output at index $i should be identical after save/load")
        }
        
        // Verify backend identification is preserved
        assertEquals("manual", newAdapter.getBackendName())
        assertEquals(config.inputSize, newAdapter.getConfig().inputSize)
        assertEquals(config.outputSize, newAdapter.getConfig().outputSize)
    }
    
    @Test
    fun testManualAdapterSaveLoadWithDifferentConfigurations() {
        // Test that loading fails gracefully when configurations don't match
        val config1 = BackendConfig(inputSize = 10, outputSize = 5, hiddenLayers = listOf(8))
        val config2 = BackendConfig(inputSize = 12, outputSize = 5, hiddenLayers = listOf(8)) // Different input size
        
        val adapter1 = ManualNetworkAdapter(config1)
        val testPath = "test_config_mismatch"
        
        // Save with first configuration
        adapter1.save(testPath)
        
        // Try to load with different configuration
        val adapter2 = ManualNetworkAdapter(config2)
        
        // This should fail with a helpful error message
        assertFailsWith<RuntimeException> {
            adapter2.load(testPath)
        }
    }
    
    @Test
    fun testManualAdapterLoadNonExistentFile() {
        val config = BackendConfig()
        val adapter = ManualNetworkAdapter(config)
        
        // Try to load a file that doesn't exist
        assertFailsWith<RuntimeException> {
            adapter.load("nonexistent_model")
        }
    }
    
    @Test
    fun testManualAdapterFormatMismatchError() {
        val config = BackendConfig()
        val adapter = ManualNetworkAdapter(config)
        
        // Create a mock DL4J file (we can't actually create a real one in common tests)
        // This test verifies the error message logic
        val mockDl4jPath = "model.zip"
        
        // The load should fail with a helpful error message about format mismatch
        // Note: In a real scenario, this would be caught by the checkpoint resolution
        // For now, we test the error message generation
        val errorMessage = CheckpointCompatibility.getFormatMismatchMessage(mockDl4jPath, BackendType.MANUAL)
        assertTrue(errorMessage.contains("Cannot load ZIP checkpoint with manual backend"))
    }
    
    @Test
    fun testSaveLoadPreservesTrainingState() {
        val config = BackendConfig(
            inputSize = 6,
            outputSize = 3,
            hiddenLayers = listOf(4),
            learningRate = 0.1
        )
        
        val adapter = ManualNetworkAdapter(config)
        val testPath = "test_training_state"
        
        // Train for several iterations to establish optimizer state
        val inputs = Array(2) { DoubleArray(config.inputSize) { Random.nextDouble() } }
        val targets = Array(2) { DoubleArray(config.outputSize) { Random.nextDouble() } }
        
        repeat(5) {
            adapter.trainBatch(inputs, targets)
        }
        
        // Get outputs before and after training to verify learning occurred
        val testInput = DoubleArray(config.inputSize) { 0.5 }
        val outputAfterTraining = adapter.forward(testInput)
        
        // Save the model
        adapter.save(testPath)
        
        // Load into a new adapter
        val newAdapter = ManualNetworkAdapter(config)
        newAdapter.load(testPath)
        
        // Verify the loaded model produces the same output
        val outputAfterLoad = newAdapter.forward(testInput)
        
        for (i in outputAfterTraining.indices) {
            assertEquals(outputAfterTraining[i], outputAfterLoad[i], 1e-10,
                "Training state should be preserved after save/load")
        }
        
        // Verify that further training works on the loaded model
        val lossAfterLoad = newAdapter.trainBatch(inputs, targets)
        assertTrue(lossAfterLoad.isFinite(), "Loaded model should be trainable")
    }
    
    @Test
    fun testMultipleBackendSaveLoadCompatibility() {
        // Test that each backend saves in its own format and can load it back
        val config = BackendConfig(
            inputSize = 8,
            outputSize = 4,
            hiddenLayers = listOf(6),
            learningRate = 0.01
        )
        
        // Test manual backend
        val manualAdapter = ManualNetworkAdapter(config)
        val manualPath = "test_manual_format"
        
        val testInput = DoubleArray(config.inputSize) { Random.nextDouble() }
        val manualOutput = manualAdapter.forward(testInput)
        
        manualAdapter.save(manualPath)
        
        val loadedManualAdapter = ManualNetworkAdapter(config)
        loadedManualAdapter.load(manualPath)
        
        val loadedManualOutput = loadedManualAdapter.forward(testInput)
        
        for (i in manualOutput.indices) {
            assertEquals(manualOutput[i], loadedManualOutput[i], 1e-10,
                "Manual backend save/load should preserve outputs")
        }
        
        // Note: DL4J tests would go here but require JVM-specific testing
        // They are covered in the JVM-specific test files
    }
    
    @Test
    fun testParameterCountConsistency() {
        val config = BackendConfig(
            inputSize = 10,
            outputSize = 5,
            hiddenLayers = listOf(8, 6)
        )
        
        val adapter = ManualNetworkAdapter(config)
        val paramCountBefore = adapter.getParameterCount()
        
        // Save and load
        val testPath = "test_param_count"
        adapter.save(testPath)
        
        val newAdapter = ManualNetworkAdapter(config)
        newAdapter.load(testPath)
        val paramCountAfter = newAdapter.getParameterCount()
        
        assertEquals(paramCountBefore, paramCountAfter, 
            "Parameter count should be consistent after save/load")
    }
    
    @Test
    fun testGradientValidationAfterLoad() {
        val config = BackendConfig(
            inputSize = 5,
            outputSize = 3,
            hiddenLayers = listOf(4)
        )
        
        val adapter = ManualNetworkAdapter(config)
        val testPath = "test_gradient_validation"
        
        // Verify gradients are valid before save
        assertTrue(adapter.validateGradients(), "Gradients should be valid before save")
        
        // Save and load
        adapter.save(testPath)
        
        val newAdapter = ManualNetworkAdapter(config)
        newAdapter.load(testPath)
        
        // Verify gradients are still valid after load
        assertTrue(newAdapter.validateGradients(), "Gradients should be valid after load")
    }
}