package com.chessrl.integration.backend

import kotlin.test.*

class Dl4jNetworkAdapterTest {
    
    private val config = BackendConfig(
        inputSize = 839,
        outputSize = 4096,
        hiddenLayers = listOf(512, 256),
        learningRate = 0.001,
        batchSize = 32
    )
    
    @Test
    fun testAdapterCreation() {
        val adapter = Dl4jNetworkAdapter(config)
        assertEquals("dl4j", adapter.getBackendName())
        assertEquals(config, adapter.getConfig())
    }
    
    @Test
    fun testForwardPass() {
        val adapter = Dl4jNetworkAdapter(config)
        val input = DoubleArray(config.inputSize) { 0.1 }
        
        val output = adapter.forward(input)
        
        assertEquals(config.outputSize, output.size)
        assertTrue(output.all { it.isFinite() }, "All outputs should be finite")
    }
    
    @Test
    fun testForwardPassWithRandomInput() {
        val adapter = Dl4jNetworkAdapter(config)
        val input = DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble(-1.0, 1.0) }
        
        val output = adapter.forward(input)
        
        assertEquals(config.outputSize, output.size)
        assertTrue(output.all { it.isFinite() }, "All outputs should be finite")
    }
    
    @Test
    fun testInvalidInputSize() {
        val adapter = Dl4jNetworkAdapter(config)
        val invalidInput = DoubleArray(config.inputSize - 1) { 0.1 }
        
        assertFailsWith<IllegalArgumentException> {
            adapter.forward(invalidInput)
        }
    }
    
    @Test
    fun testBatchTraining() {
        val adapter = Dl4jNetworkAdapter(config)
        val batchSize = 4
        val inputs = Array(batchSize) { 
            DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble(-1.0, 1.0) } 
        }
        val targets = Array(batchSize) { 
            DoubleArray(config.outputSize) { kotlin.random.Random.nextDouble(-1.0, 1.0) } 
        }
        
        val loss = adapter.trainBatch(inputs, targets)
        
        assertTrue(loss.isFinite(), "Loss should be finite")
        assertTrue(loss >= 0.0, "Loss should be non-negative")
    }
    
    @Test
    fun testEmptyBatchTraining() {
        val adapter = Dl4jNetworkAdapter(config)
        val emptyInputs = emptyArray<DoubleArray>()
        val emptyTargets = emptyArray<DoubleArray>()
        
        assertFailsWith<IllegalArgumentException> {
            adapter.trainBatch(emptyInputs, emptyTargets)
        }
    }
    
    @Test
    fun testMismatchedBatchSizes() {
        val adapter = Dl4jNetworkAdapter(config)
        val inputs = Array(2) { DoubleArray(config.inputSize) { 0.1 } }
        val targets = Array(3) { DoubleArray(config.outputSize) { 0.1 } }
        
        assertFailsWith<IllegalArgumentException> {
            adapter.trainBatch(inputs, targets)
        }
    }
    
    @Test
    fun testWeightSynchronization() {
        val adapter1 = Dl4jNetworkAdapter(config)
        val adapter2 = Dl4jNetworkAdapter(config)
        
        // Get initial outputs (should be different due to random initialization)
        val input = DoubleArray(config.inputSize) { 0.1 }
        val output1Before = adapter1.forward(input)
        val output2Before = adapter2.forward(input)
        
        // Copy weights from adapter1 to adapter2
        adapter1.copyWeightsTo(adapter2)
        
        // Now outputs should be identical
        val output1After = adapter1.forward(input)
        val output2After = adapter2.forward(input)
        
        assertContentEquals(output1After, output2After, "Outputs should be identical after weight sync")
    }
    
    @Test
    fun testCrossBackendWeightSyncFails() {
        val dl4jAdapter = Dl4jNetworkAdapter(config)
        val manualAdapter = ManualNetworkAdapter(config)
        
        assertFailsWith<UnsupportedOperationException> {
            dl4jAdapter.copyWeightsTo(manualAdapter)
        }
    }
    
    @Test
    fun testParameterCount() {
        val adapter = Dl4jNetworkAdapter(config)
        val paramCount = adapter.getParameterCount()
        
        assertTrue(paramCount > 0, "Parameter count should be positive")
        
        // Calculate expected parameter count:
        // Layer 1: (839 + 1) * 512 = 430,080
        // Layer 2: (512 + 1) * 256 = 131,328  
        // Layer 3: (256 + 1) * 4096 = 1,052,672
        // Total: 1,614,080
        val expectedParams = (839 + 1) * 512 + (512 + 1) * 256 + (256 + 1) * 4096
        assertEquals(expectedParams.toLong(), paramCount, "Parameter count should match expected value")
    }
    
    @Test
    fun testMemoryUsage() {
        val adapter = Dl4jNetworkAdapter(config)
        val memoryUsage = adapter.getMemoryUsage()
        
        assertTrue(memoryUsage > 0, "Memory usage should be positive")
    }
    
    @Test
    fun testGradientValidation() {
        val adapter = Dl4jNetworkAdapter(config)
        
        // Initially gradients should be valid (finite)
        assertTrue(adapter.validateGradients(), "Initial gradients should be valid")
        
        // After training, gradients should still be valid
        val inputs = Array(2) { DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble() } }
        val targets = Array(2) { DoubleArray(config.outputSize) { kotlin.random.Random.nextDouble() } }
        adapter.trainBatch(inputs, targets)
        
        assertTrue(adapter.validateGradients(), "Gradients should remain valid after training")
    }
    
    @Test
    fun testDifferentArchitectures() {
        val smallConfig = config.copy(
            inputSize = 10,
            outputSize = 5,
            hiddenLayers = listOf(8)
        )
        
        val adapter = Dl4jNetworkAdapter(smallConfig)
        val input = DoubleArray(10) { 0.1 }
        val output = adapter.forward(input)
        
        assertEquals(5, output.size)
        assertTrue(output.all { it.isFinite() })
    }
    
    @Test
    fun testDifferentOptimizers() {
        val adamConfig = config.copy(optimizer = "adam")
        val sgdConfig = config.copy(optimizer = "sgd")
        val rmspropConfig = config.copy(optimizer = "rmsprop")
        
        // All should create successfully
        val adamAdapter = Dl4jNetworkAdapter(adamConfig)
        val sgdAdapter = Dl4jNetworkAdapter(sgdConfig)
        val rmspropAdapter = Dl4jNetworkAdapter(rmspropConfig)
        
        // All should produce valid outputs
        val input = DoubleArray(config.inputSize) { 0.1 }
        assertTrue(adamAdapter.forward(input).all { it.isFinite() })
        assertTrue(sgdAdapter.forward(input).all { it.isFinite() })
        assertTrue(rmspropAdapter.forward(input).all { it.isFinite() })
    }
    
    @Test
    fun testDifferentLossFunctions() {
        val huberConfig = config.copy(lossFunction = "huber")
        val mseConfig = config.copy(lossFunction = "mse")
        
        val huberAdapter = Dl4jNetworkAdapter(huberConfig)
        val mseAdapter = Dl4jNetworkAdapter(mseConfig)
        
        val inputs = Array(2) { DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble() } }
        val targets = Array(2) { DoubleArray(config.outputSize) { kotlin.random.Random.nextDouble() } }
        
        val huberLoss = huberAdapter.trainBatch(inputs, targets)
        val mseLoss = mseAdapter.trainBatch(inputs, targets)
        
        assertTrue(huberLoss.isFinite())
        assertTrue(mseLoss.isFinite())
    }
    
    @Test
    fun testSaveLoadRoundTrip() {
        val adapter = Dl4jNetworkAdapter(config)
        val testInput = DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble() }
        
        // Train the network a bit to create non-random weights
        val inputs = Array(4) { DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble() } }
        val targets = Array(4) { DoubleArray(config.outputSize) { kotlin.random.Random.nextDouble() } }
        adapter.trainBatch(inputs, targets)
        
        // Get output before saving
        val outputBefore = adapter.forward(testInput)
        
        // Save and load
        val tempPath = "test_model_dl4j"
        adapter.save(tempPath)
        
        val newAdapter = Dl4jNetworkAdapter(config)
        newAdapter.load(tempPath)
        
        // Get output after loading
        val outputAfter = newAdapter.forward(testInput)
        
        // Outputs should be identical (within floating point precision)
        assertEquals(outputBefore.size, outputAfter.size)
        for (i in outputBefore.indices) {
            assertEquals(outputBefore[i], outputAfter[i], 1e-6, "Output $i should match after save/load")
        }
        
        // Clean up
        java.io.File("$tempPath.zip").delete()
        java.io.File("$tempPath.zip.meta.json").delete()
    }
    
    @Test
    fun testSaveLoadWithUpdaterState() {
        val adapter = Dl4jNetworkAdapter(config)
        
        // Train for several batches to build up optimizer state
        repeat(5) {
            val inputs = Array(4) { DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble() } }
            val targets = Array(4) { DoubleArray(config.outputSize) { kotlin.random.Random.nextDouble() } }
            adapter.trainBatch(inputs, targets)
        }
        
        // Save the model with optimizer state
        val tempPath = "test_model_with_optimizer"
        adapter.save(tempPath)
        
        // Load into new adapter
        val newAdapter = Dl4jNetworkAdapter(config)
        newAdapter.load(tempPath)
        
        // Both should produce identical outputs
        val testInput = DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble() }
        val output1 = adapter.forward(testInput)
        val output2 = newAdapter.forward(testInput)
        
        assertContentEquals(output1, output2, "Outputs should be identical after loading with optimizer state")
        
        // Clean up
        java.io.File("$tempPath.zip").delete()
        java.io.File("$tempPath.zip.meta.json").delete()
    }
    
    @Test
    fun testWeightSynchronizationWithUpdaterState() {
        val adapter1 = Dl4jNetworkAdapter(config)
        val adapter2 = Dl4jNetworkAdapter(config)
        
        // Train adapter1 to create optimizer state
        repeat(3) {
            val inputs = Array(2) { DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble() } }
            val targets = Array(2) { DoubleArray(config.outputSize) { kotlin.random.Random.nextDouble() } }
            adapter1.trainBatch(inputs, targets)
        }
        
        // Synchronize weights and optimizer state
        adapter1.copyWeightsTo(adapter2)
        
        // Both should produce identical outputs
        val testInput = DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble() }
        val output1 = adapter1.forward(testInput)
        val output2 = adapter2.forward(testInput)
        
        assertContentEquals(output1, output2, "Outputs should be identical after weight synchronization")
    }
    
    @Test
    fun testLoadIncompatibleFormat() {
        val adapter = Dl4jNetworkAdapter(config)
        
        // Try to load a non-existent JSON file (manual format)
        assertFailsWith<RuntimeException> {
            adapter.load("nonexistent.json")
        }
        
        // The error message should mention format incompatibility
        val exception = assertFailsWith<RuntimeException> {
            adapter.load("nonexistent.json")
        }
        assertTrue(exception.message?.contains("JSON") == true || 
                  exception.message?.contains("manual") == true,
                  "Error message should mention JSON/manual format incompatibility")
    }
    
    @Test
    fun testParameterCountAccuracy() {
        val adapter = Dl4jNetworkAdapter(config)
        val actualCount = adapter.getParameterCount()
        
        // Calculate expected count manually
        // Layer 1: (839 + 1) * 512 = 430,080 (input + bias to first hidden)
        // Layer 2: (512 + 1) * 256 = 131,328 (first hidden + bias to second hidden)  
        // Layer 3: (256 + 1) * 4096 = 1,052,672 (second hidden + bias to output)
        val expectedCount = (839 + 1) * 512 + (512 + 1) * 256 + (256 + 1) * 4096
        
        assertEquals(expectedCount.toLong(), actualCount, 
                    "Parameter count should exactly match calculated value")
    }
    
    @Test
    fun testMemoryUsageEstimation() {
        val adapter = Dl4jNetworkAdapter(config)
        val memoryUsage = adapter.getMemoryUsage()
        
        // Memory usage should be reasonable (not zero, not excessive)
        assertTrue(memoryUsage > 0, "Memory usage should be positive")
        assertTrue(memoryUsage < 1_000_000_000, "Memory usage should be reasonable (< 1GB)")
        
        // Should include both parameter memory and workspace memory
        val paramCount = adapter.getParameterCount()
        val minExpectedMemory = paramCount * 4 // 4 bytes per float parameter
        assertTrue(memoryUsage >= minExpectedMemory, 
                  "Memory usage should at least account for parameter storage")
    }
    
    @Test
    fun testCheckpointCompatibilityErrorMessages() {
        val adapter = Dl4jNetworkAdapter(config)
        
        // Create a fake JSON file to simulate manual backend checkpoint
        val jsonPath = "fake_manual_checkpoint.json"
        java.io.File(jsonPath).writeText("{\"fake\": \"manual checkpoint\"}")
        
        try {
            val exception = assertFailsWith<RuntimeException> {
                adapter.load(jsonPath)
            }
            
            // Error message should clearly explain the format incompatibility
            val message = exception.message ?: ""
            assertTrue(message.contains("JSON") || message.contains("manual"), 
                      "Error should mention JSON/manual format: $message")
            assertTrue(message.contains("DL4J") || message.contains("dl4j"), 
                      "Error should mention DL4J backend: $message")
            
        } finally {
            // Clean up
            java.io.File(jsonPath).delete()
        }
    }
}