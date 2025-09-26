package com.chessrl.integration.backend

import kotlin.test.*

class GradientValidatorTest {
    
    private val testConfig = BackendConfig(
        inputSize = 10,
        outputSize = 5,
        hiddenLayers = listOf(8, 6),
        learningRate = 0.01,
        batchSize = 2
    )
    
    @Test
    fun testValidateGradientsBasic() {
        val adapter = ManualNetworkAdapter(testConfig)
        val result = GradientValidator.validateGradients(adapter)
        
        assertTrue(result.isValid, "Gradient validation should pass for manual adapter")
        assertTrue(result.issues.isEmpty(), "Should have no issues: ${result.issues}")
    }
    
    @Test
    fun testQuickGradientCheck() {
        val adapter = ManualNetworkAdapter(testConfig)
        val isValid = GradientValidator.quickGradientCheck(adapter)
        
        assertTrue(isValid, "Quick gradient check should pass")
    }
    
    @Test
    fun testEstimateGradientNorm() {
        val adapter = ManualNetworkAdapter(testConfig)
        val gradientNorm = GradientValidator.estimateGradientNorm(adapter)
        
        assertTrue(gradientNorm.isFinite(), "Gradient norm should be finite")
        assertTrue(gradientNorm >= 0.0, "Gradient norm should be non-negative")
    }
    
    @Test
    fun testGradientValidationResultSummary() {
        val result = GradientValidationResult(
            isValid = false,
            issues = listOf("Gradient issue 1", "Gradient issue 2"),
            warnings = listOf("Gradient warning 1")
        )
        
        val summary = result.getSummary()
        
        assertTrue(summary.contains("FAILED"))
        assertTrue(summary.contains("Gradient issue 1"))
        assertTrue(summary.contains("Gradient issue 2"))
        assertTrue(summary.contains("Gradient warning 1"))
    }
    
    @Test
    fun testGradientValidationResultSummarySuccess() {
        val result = GradientValidationResult(
            isValid = true,
            issues = emptyList(),
            warnings = emptyList()
        )
        
        val summary = result.getSummary()
        
        assertTrue(summary.contains("PASSED"))
        assertTrue(summary.contains("All gradient validation checks passed"))
    }
    
    @Test
    fun testGradientValidationWithWarnings() {
        val result = GradientValidationResult(
            isValid = true,
            issues = emptyList(),
            warnings = listOf("Small gradient magnitude detected")
        )
        
        val summary = result.getSummary()
        
        assertTrue(summary.contains("PASSED"))
        assertTrue(summary.contains("Small gradient magnitude detected"))
    }
    
    @Test
    fun testGradientValidationBasicComputation() {
        val adapter = ManualNetworkAdapter(testConfig)
        
        // Test that we can compute loss without errors
        val input = DoubleArray(testConfig.inputSize) { 0.5 }
        val target = DoubleArray(testConfig.outputSize) { 0.3 }
        
        val output = adapter.forward(input)
        val lossArray = adapter.backward(target)
        
        assertEquals(1, lossArray.size)
        assertTrue(lossArray[0].isFinite())
        assertTrue(lossArray[0] >= 0.0)
    }
    
    @Test
    fun testGradientValidationMultipleSteps() {
        val adapter = ManualNetworkAdapter(testConfig)
        
        // Run multiple gradient computations to test stability
        repeat(5) {
            val input = DoubleArray(testConfig.inputSize) { 0.5 + it * 0.1 }
            val target = DoubleArray(testConfig.outputSize) { 0.3 + it * 0.1 }
            
            adapter.forward(input)
            val gradients = adapter.backward(target)
            
            assertTrue(gradients.all { it.isFinite() }, "Gradients should be finite at step $it")
        }
    }
    
    @Test
    fun testGradientValidationWithTraining() {
        val adapter = ManualNetworkAdapter(testConfig)
        
        // Perform some training steps
        val inputs = Array(2) { DoubleArray(testConfig.inputSize) { 0.5 } }
        val targets = Array(2) { DoubleArray(testConfig.outputSize) { 0.3 } }
        
        repeat(3) {
            val loss = adapter.trainBatch(inputs, targets)
            assertTrue(loss.isFinite(), "Loss should be finite at training step $it")
            
            // Check gradients are still valid after training
            val isValid = GradientValidator.quickGradientCheck(adapter)
            assertTrue(isValid, "Gradients should remain valid after training step $it")
        }
    }
    
    @Test
    fun testGradientNormEstimationConsistency() {
        val adapter = ManualNetworkAdapter(testConfig)
        
        // Estimate gradient norm multiple times with same input
        val norms = mutableListOf<Double>()
        repeat(3) {
            val norm = GradientValidator.estimateGradientNorm(adapter)
            norms.add(norm)
        }
        
        // All norms should be finite
        assertTrue(norms.all { it.isFinite() })
        
        // Norms should be reasonably consistent (within an order of magnitude)
        val minNorm = norms.minOrNull() ?: 0.0
        val maxNorm = norms.maxOrNull() ?: 0.0
        
        if (minNorm > 0.0) {
            assertTrue(maxNorm / minNorm < 100.0, "Gradient norms should be reasonably consistent")
        }
    }
}