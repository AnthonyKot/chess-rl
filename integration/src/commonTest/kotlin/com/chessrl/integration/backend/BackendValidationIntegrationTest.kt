package com.chessrl.integration.backend

import kotlin.test.*

/**
 * Integration test demonstrating the complete backend validation and error handling system
 */
class BackendValidationIntegrationTest {
    
    private val testConfig = BackendConfig(
        inputSize = 20,
        outputSize = 10,
        hiddenLayers = listOf(16, 12),
        learningRate = 0.01,
        batchSize = 4
    )
    
    @Test
    fun testCompleteValidationWorkflow() {
        // 1. Create adapter
        val adapter = ManualNetworkAdapter(testConfig)
        
        // 2. Validate adapter comprehensively
        val validationResult = AdapterValidator.validateAdapter(adapter)
        
        assertTrue(validationResult.isValid, "Adapter should pass validation")
        assertEquals("manual", validationResult.backend)
        
        println("Validation Summary:")
        println(validationResult.getSummary())
        
        // 3. Test gradient validation
        val gradientResult = GradientValidator.validateGradients(adapter)
        assertTrue(gradientResult.isValid, "Gradient validation should pass")
        
        println("Gradient Validation Summary:")
        println(gradientResult.getSummary())
        
        // 4. Test quick checks
        assertTrue(GradientValidator.quickGradientCheck(adapter))
        
        val gradientNorm = GradientValidator.estimateGradientNorm(adapter)
        assertTrue(gradientNorm.isFinite())
        assertTrue(gradientNorm >= 0.0)
        
        println("Gradient norm estimate: $gradientNorm")
    }
    
    @Test
    fun testSafeAdapterWithValidation() {
        // 1. Create primary and fallback adapters
        val primary = ManualNetworkAdapter(testConfig)
        val fallback = ManualNetworkAdapter(testConfig)
        
        // 2. Create safe adapter
        val safeAdapter = SafeNetworkAdapter(primary, fallback)
        
        // 3. Validate safe adapter
        val validationResult = AdapterValidator.validateAdapter(safeAdapter)
        assertTrue(validationResult.isValid, "Safe adapter should pass validation")
        
        // 4. Test safety features
        assertTrue(safeAdapter.isHealthy())
        
        val stats = safeAdapter.getSafetyStats()
        assertEquals("manual", stats.primaryBackend)
        assertEquals("manual", stats.fallbackBackend)
        assertFalse(stats.fallbackActive)
        
        println("Safety Stats:")
        println(stats.getSummary())
        
        // 5. Test fallback mechanism
        safeAdapter.forceFallback()
        val statsAfterFallback = safeAdapter.getSafetyStats()
        assertTrue(statsAfterFallback.fallbackActive)
        
        // 6. Reset and verify
        safeAdapter.resetFailureTracking()
        val statsAfterReset = safeAdapter.getSafetyStats()
        assertFalse(statsAfterReset.fallbackActive)
    }
    
    @Test
    fun testValidationWithTraining() {
        val adapter = ManualNetworkAdapter(testConfig)
        
        // Initial validation
        val initialValidation = AdapterValidator.validateAdapter(adapter)
        assertTrue(initialValidation.isValid)
        
        // Perform some training
        val inputs = Array(4) { DoubleArray(testConfig.inputSize) { 0.5 } }
        val targets = Array(4) { DoubleArray(testConfig.outputSize) { 0.3 } }
        
        repeat(5) {
            val loss = adapter.trainBatch(inputs, targets)
            assertTrue(loss.isFinite())
            assertTrue(loss >= 0.0)
            
            // Validate after each training step
            assertTrue(GradientValidator.quickGradientCheck(adapter))
        }
        
        // Final validation
        val finalValidation = AdapterValidator.validateAdapter(adapter)
        assertTrue(finalValidation.isValid)
        
        println("Training completed successfully with validation")
    }
    
    @Test
    fun testValidationErrorReporting() {
        // Test with a configuration that might produce warnings
        val extremeConfig = BackendConfig(
            inputSize = 5,
            outputSize = 3,
            hiddenLayers = listOf(2),
            learningRate = 0.1, // High learning rate
            batchSize = 1
        )
        
        val adapter = ManualNetworkAdapter(extremeConfig)
        val validationResult = AdapterValidator.validateAdapter(adapter)
        
        // Should still be valid but might have warnings
        assertTrue(validationResult.isValid)
        
        println("Validation with extreme config:")
        println(validationResult.getSummary())
        
        if (validationResult.warnings.isNotEmpty()) {
            println("Warnings detected as expected for extreme configuration")
        }
    }
    
    @Test
    fun testBackendCompatibilityValidation() {
        // Test that validation works across different backend types
        val manualAdapter = ManualNetworkAdapter(testConfig)
        
        // Validate manual backend
        val manualValidation = AdapterValidator.validateAdapter(manualAdapter)
        assertTrue(manualValidation.isValid)
        assertEquals("manual", manualValidation.backend)
        
        // Test backend selector validation
        val availabilityResult = BackendSelector.validateBackendAvailability(BackendType.MANUAL)
        assertTrue(availabilityResult.isValid)
        assertEquals("MANUAL", availabilityResult.backend)
        
        // Test DL4J availability (should fail gracefully if not available)
        val dl4jResult = BackendSelector.validateBackendAvailability(BackendType.DL4J)
        // Don't assert on this as DL4J might not be available in test environment
        println("DL4J availability: ${dl4jResult.isValid}")
        if (!dl4jResult.isValid) {
            println("DL4J issues: ${dl4jResult.issues}")
        }
    }
}