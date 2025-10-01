package com.chessrl.integration.backend

import kotlin.test.*

class SafeNetworkAdapterTest {
    
    private val testConfig = BackendConfig(
        inputSize = 10,
        outputSize = 5,
        hiddenLayers = listOf(8, 6),
        learningRate = 0.01,
        batchSize = 2
    )
    
    @Test
    fun testSafeAdapterBasicOperation() {
        val primary = ManualNetworkAdapter(testConfig)
        val safeAdapter = SafeNetworkAdapter(primary)
        
        val input = DoubleArray(testConfig.inputSize) { 0.5 }
        val output = safeAdapter.forward(input)
        
        assertEquals(testConfig.outputSize, output.size)
        assertTrue(output.all { it.isFinite() })
        assertEquals("safe(manual)", safeAdapter.getBackendName())
    }
    
    @Test
    fun testSafeAdapterWithFallback() {
        val primary = ManualNetworkAdapter(testConfig)
        val fallback = ManualNetworkAdapter(testConfig)
        val safeAdapter = SafeNetworkAdapter(primary, fallback)
        
        val input = DoubleArray(testConfig.inputSize) { 0.5 }
        val output = safeAdapter.forward(input)
        
        assertEquals(testConfig.outputSize, output.size)
        assertTrue(output.all { it.isFinite() })
        
        val stats = safeAdapter.getSafetyStats()
        assertEquals("manual", stats.primaryBackend)
        assertEquals("manual", stats.fallbackBackend)
        assertFalse(stats.fallbackActive)
    }
    
    @Test
    fun testSafeAdapterFallbackActivation() {
        val primary = ManualNetworkAdapter(testConfig)
        val fallback = ManualNetworkAdapter(testConfig)
        val safeAdapter = SafeNetworkAdapter(primary, fallback)
        
        // Force fallback activation
        safeAdapter.forceFallback()
        
        val stats = safeAdapter.getSafetyStats()
        assertTrue(stats.fallbackActive)
        assertEquals("manual", stats.activeBackend)
        assertTrue(safeAdapter.getBackendName().contains("fallback"))
    }
    
    @Test
    fun testSafeAdapterHealthCheck() {
        val primary = ManualNetworkAdapter(testConfig)
        val safeAdapter = SafeNetworkAdapter(primary)
        
        assertTrue(safeAdapter.isHealthy())
    }
    
    @Test
    fun testSafeAdapterTraining() {
        val primary = ManualNetworkAdapter(testConfig)
        val safeAdapter = SafeNetworkAdapter(primary)
        
        val inputs = Array(2) { DoubleArray(testConfig.inputSize) { 0.5 } }
        val targets = Array(2) { DoubleArray(testConfig.outputSize) { 0.3 } }
        
        val loss = safeAdapter.trainBatch(inputs, targets)
        
        assertTrue(loss.isFinite())
        assertTrue(loss >= 0.0)
    }
    
    @Test
    fun testSafeAdapterWeightSynchronization() {
        val primary = ManualNetworkAdapter(testConfig)
        val target = ManualNetworkAdapter(testConfig)
        val safeAdapter = SafeNetworkAdapter(primary)
        
        // This should work without throwing
        safeAdapter.copyWeightsTo(target)
    }
    
    @Test
    fun testSafeAdapterSaveLoad() {
        val primary = ManualNetworkAdapter(testConfig)
        val safeAdapter = SafeNetworkAdapter(primary)
        val testPath = "test_safe_adapter"
        
        val testInput = DoubleArray(testConfig.inputSize) { 0.5 }
        val originalOutput = safeAdapter.forward(testInput)
        
        // Save and load
        safeAdapter.save(testPath)
        
        // Modify
        val trainInputs = Array(1) { DoubleArray(testConfig.inputSize) { 0.3 } }
        val trainTargets = Array(1) { DoubleArray(testConfig.outputSize) { 0.7 } }
        safeAdapter.trainBatch(trainInputs, trainTargets)
        
        // Load
        safeAdapter.load(testPath)
        
        // Verify restoration
        val restoredOutput = safeAdapter.forward(testInput)
        for (i in originalOutput.indices) {
            assertEquals(originalOutput[i], restoredOutput[i], 1e-10)
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
    fun testSafeAdapterFailureTracking() {
        val primary = ManualNetworkAdapter(testConfig)
        val safeAdapter = SafeNetworkAdapter(primary)
        
        val initialStats = safeAdapter.getSafetyStats()
        assertEquals(0, initialStats.primaryFailureCount)
        
        // Reset should work
        safeAdapter.resetFailureTracking()
        
        val resetStats = safeAdapter.getSafetyStats()
        assertEquals(0, resetStats.primaryFailureCount)
        assertFalse(resetStats.fallbackActive)
    }
    
    @Test
    fun testSafeAdapterConfiguration() {
        val primary = ManualNetworkAdapter(testConfig)
        val safetyConfig = SafetyConfig(
            maxPrimaryFailures = 5,
            maxOutputValue = 500.0,
            strictValidation = true
        )
        val safeAdapter = SafeNetworkAdapter(primary, config = safetyConfig)
        
        assertEquals(testConfig, safeAdapter.getConfig())
    }
    
    @Test
    fun testSafetyStatsSummary() {
        val stats = SafetyStats(
            primaryBackend = "manual",
            fallbackBackend = "manual",
            primaryFailureCount = 2,
            fallbackActive = true,
            activeBackend = "manual"
        )
        
        val summary = stats.getSummary()
        
        assertTrue(summary.contains("Primary Backend: manual"))
        assertTrue(summary.contains("Fallback Backend: manual"))
        assertTrue(summary.contains("Primary Failures: 2"))
        assertTrue(summary.contains("Fallback Active: true"))
    }
    
    @Test
    fun testSafeAdapterGradientValidation() {
        val primary = ManualNetworkAdapter(testConfig)
        val safeAdapter = SafeNetworkAdapter(primary)
        
        val isValid = safeAdapter.validateGradients()
        assertTrue(isValid)
    }
    
    @Test
    fun testSafeAdapterPerformanceMetrics() {
        val primary = ManualNetworkAdapter(testConfig)
        val safeAdapter = SafeNetworkAdapter(primary)
        
        val paramCount = safeAdapter.getParameterCount()
        val memoryUsage = safeAdapter.getMemoryUsage()
        
        assertTrue(paramCount > 0)
        assertTrue(memoryUsage > 0)
    }
}