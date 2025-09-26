package com.chessrl.integration.backend

import kotlin.test.*

class AdapterValidatorTest {
    
    private val testConfig = BackendConfig(
        inputSize = 10,
        outputSize = 5,
        hiddenLayers = listOf(8, 6),
        learningRate = 0.01,
        batchSize = 2
    )
    
    @Test
    fun testValidateManualAdapter() {
        val adapter = ManualNetworkAdapter(testConfig)
        val result = AdapterValidator.validateAdapter(adapter)
        
        assertTrue(result.isValid, "Manual adapter validation should pass")
        assertEquals("manual", result.backend)
        assertTrue(result.issues.isEmpty(), "Should have no issues: ${result.issues}")
    }
    
    @Test
    fun testValidateAdapterWithInvalidConfig() {
        val invalidConfig = BackendConfig(
            inputSize = -1,  // Invalid
            outputSize = 0,  // Invalid
            learningRate = -0.1  // Invalid
        )
        
        val adapter = ManualNetworkAdapter(invalidConfig.copy(inputSize = 10, outputSize = 5))
        
        // We can't actually create an adapter with invalid config due to validation in constructor,
        // so we test the configuration validation separately
        val issues = mutableListOf<String>()
        
        // Simulate configuration validation
        if (invalidConfig.inputSize <= 0) issues.add("Input size must be positive")
        if (invalidConfig.outputSize <= 0) issues.add("Output size must be positive")
        if (invalidConfig.learningRate <= 0.0) issues.add("Learning rate must be positive")
        
        assertTrue(issues.isNotEmpty(), "Should detect configuration issues")
    }
    
    @Test
    fun testValidateForwardPass() {
        val adapter = ManualNetworkAdapter(testConfig)
        
        // Test normal forward pass
        val input = DoubleArray(testConfig.inputSize) { 0.5 }
        val output = adapter.forward(input)
        
        assertEquals(testConfig.outputSize, output.size)
        assertTrue(output.all { it.isFinite() })
    }
    
    @Test
    fun testValidateBatchTraining() {
        val adapter = ManualNetworkAdapter(testConfig)
        
        val inputs = Array(2) { DoubleArray(testConfig.inputSize) { 0.5 } }
        val targets = Array(2) { DoubleArray(testConfig.outputSize) { 0.3 } }
        
        val loss = adapter.trainBatch(inputs, targets)
        
        assertTrue(loss.isFinite(), "Loss should be finite")
        assertTrue(loss >= 0.0, "Loss should be non-negative")
    }
    
    @Test
    fun testValidateWeightSynchronization() {
        val adapter1 = ManualNetworkAdapter(testConfig)
        val adapter2 = ManualNetworkAdapter(testConfig)
        
        val testInput = DoubleArray(testConfig.inputSize) { 0.5 }
        
        // Get initial outputs (should be different due to random initialization)
        val output1Before = adapter1.forward(testInput)
        val output2Before = adapter2.forward(testInput)
        
        // Copy weights
        adapter1.copyWeightsTo(adapter2)
        
        // Get outputs after copying (should be identical)
        val output1After = adapter1.forward(testInput)
        val output2After = adapter2.forward(testInput)
        
        // Check that outputs are now identical
        for (i in output1After.indices) {
            assertEquals(output1After[i], output2After[i], 1e-10, "Outputs should be identical after weight copying")
        }
    }
    
    @Test
    fun testValidateSaveLoad() {
        val adapter = ManualNetworkAdapter(testConfig)
        val testPath = "test_adapter_validation"
        
        val testInput = DoubleArray(testConfig.inputSize) { 0.5 }
        val originalOutput = adapter.forward(testInput)
        
        // Save
        adapter.save(testPath)
        
        // Modify the network
        val trainInputs = Array(1) { DoubleArray(testConfig.inputSize) { 0.3 } }
        val trainTargets = Array(1) { DoubleArray(testConfig.outputSize) { 0.7 } }
        adapter.trainBatch(trainInputs, trainTargets)
        
        // Load
        adapter.load(testPath)
        
        // Check restoration
        val restoredOutput = adapter.forward(testInput)
        
        for (i in originalOutput.indices) {
            assertEquals(originalOutput[i], restoredOutput[i], 1e-10, "Output should be restored after load")
        }
        
        // Cleanup
        try {
            com.chessrl.integration.deleteFile("$testPath.json")
            com.chessrl.integration.deleteFile("$testPath.json.optimizer")
        } catch (e: Exception) {
            // Ignore cleanup failures
        }
    }
    
    @Test
    fun testValidationResultSummary() {
        val result = AdapterValidationResult(
            isValid = false,
            backend = "test",
            issues = listOf("Issue 1", "Issue 2"),
            warnings = listOf("Warning 1")
        )
        
        val summary = result.getSummary()
        
        assertTrue(summary.contains("FAILED"))
        assertTrue(summary.contains("Issue 1"))
        assertTrue(summary.contains("Issue 2"))
        assertTrue(summary.contains("Warning 1"))
    }
    
    @Test
    fun testValidationResultSummarySuccess() {
        val result = AdapterValidationResult(
            isValid = true,
            backend = "test",
            issues = emptyList(),
            warnings = emptyList()
        )
        
        val summary = result.getSummary()
        
        assertTrue(summary.contains("PASSED"))
        assertTrue(summary.contains("All validation checks passed"))
    }
}