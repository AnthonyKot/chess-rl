package com.chessrl.integration.backend

import kotlin.test.*
import java.io.File

/**
 * Test checkpoint compatibility between different backends.
 * This test demonstrates the conversion rules and error handling for cross-backend compatibility.
 */
class CheckpointCompatibilityTest {
    
    private val config = BackendConfig(
        inputSize = 10,
        outputSize = 5,
        hiddenLayers = listOf(8),
        learningRate = 0.001
    )
    
    @Test
    fun testManualBackendCannotLoadDl4jCheckpoints() {
        // Create and save a DL4J model
        val dl4jAdapter = Dl4jNetworkAdapter(config)
        val dl4jPath = "test_dl4j_model"
        dl4jAdapter.save(dl4jPath)
        
        // Try to load with manual backend - should fail with helpful error
        val manualAdapter = ManualNetworkAdapter(config)
        
        val exception = assertFailsWith<RuntimeException> {
            manualAdapter.load("$dl4jPath.zip")
        }
        
        // Error message should explain the incompatibility
        val message = exception.message ?: ""
        assertTrue(message.contains("ZIP") || message.contains("DL4J") || message.contains("dl4j"),
                  "Error should mention ZIP/DL4J format incompatibility: $message")
        
        // Clean up
        File("$dl4jPath.zip").delete()
        File("$dl4jPath.zip.meta.json").delete()
    }
    
    @Test
    fun testDl4jBackendCannotLoadManualCheckpoints() {
        // Create a fake JSON checkpoint (simulating manual backend format)
        val jsonPath = "test_manual_model.json"
        File(jsonPath).writeText("""
            {
                "version": "1.0",
                "backend": "manual",
                "layers": [
                    {"type": "dense", "inputSize": 10, "outputSize": 8},
                    {"type": "dense", "inputSize": 8, "outputSize": 5}
                ],
                "weights": [[0.1, 0.2], [0.3, 0.4]]
            }
        """.trimIndent())
        
        // Try to load with DL4J backend - should fail with helpful error
        val dl4jAdapter = Dl4jNetworkAdapter(config)
        
        val exception = assertFailsWith<RuntimeException> {
            dl4jAdapter.load(jsonPath)
        }
        
        // Error message should explain the incompatibility and suggest solution
        val message = exception.message ?: ""
        assertTrue(message.contains("JSON") || message.contains("manual"),
                  "Error should mention JSON/manual format: $message")
        assertTrue(message.contains("--nn manual") || message.contains("manual backend"),
                  "Error should suggest using manual backend: $message")
        
        // Clean up
        File(jsonPath).delete()
    }
    
    @Test
    fun testCheckpointFormatDetection() {
        // Test that DL4J adapter correctly handles different path formats
        val dl4jAdapter = Dl4jNetworkAdapter(config)
        
        // Save with explicit .zip extension
        val zipPath = "test_model.zip"
        dl4jAdapter.save(zipPath)
        assertTrue(File(zipPath).exists(), "ZIP file should be created")
        
        // Save without extension (should add .zip automatically)
        val noExtPath = "test_model_no_ext"
        dl4jAdapter.save(noExtPath)
        assertTrue(File("$noExtPath.zip").exists(), "ZIP file should be created with .zip extension")
        
        // Load should work with both formats
        val adapter1 = Dl4jNetworkAdapter(config)
        val adapter2 = Dl4jNetworkAdapter(config)
        
        adapter1.load(zipPath)
        adapter2.load(noExtPath) // Should automatically find .zip file
        
        // Both should produce same outputs
        val testInput = DoubleArray(config.inputSize) { 0.1 }
        val output1 = adapter1.forward(testInput)
        val output2 = adapter2.forward(testInput)
        
        assertContentEquals(output1, output2, "Outputs should be identical regardless of path format")
        
        // Clean up
        File(zipPath).delete()
        File("$zipPath.meta.json").delete()
        File("$noExtPath.zip").delete()
        File("$noExtPath.zip.meta.json").delete()
    }
    
    @Test
    fun testMetadataGeneration() {
        val dl4jAdapter = Dl4jNetworkAdapter(config)
        val modelPath = "test_model_with_metadata"
        
        dl4jAdapter.save(modelPath)
        
        // Check that metadata file was created
        val metadataFile = File("$modelPath.zip.meta.json")
        assertTrue(metadataFile.exists(), "Metadata file should be created")
        
        // Check metadata content
        val metadata = metadataFile.readText()
        assertTrue(metadata.contains("\"backend\": \"dl4j\""), "Metadata should specify DL4J backend")
        assertTrue(metadata.contains("\"format\": \"zip\""), "Metadata should specify ZIP format")
        assertTrue(metadata.contains("\"inputSize\": ${config.inputSize}"), "Metadata should include input size")
        assertTrue(metadata.contains("\"outputSize\": ${config.outputSize}"), "Metadata should include output size")
        assertTrue(metadata.contains("\"parameterCount\""), "Metadata should include parameter count")
        
        // Clean up
        File("$modelPath.zip").delete()
        metadataFile.delete()
    }
    
    @Test
    fun testConversionRulesDocumentation() {
        // This test documents the conversion rules between backends
        
        // Rule 1: Manual backend uses JSON format
        val manualAdapter = ManualNetworkAdapter(config)
        val manualPath = "manual_model.json"
        manualAdapter.save(manualPath)
        assertTrue(File(manualPath).exists(), "Manual backend should create JSON file")
        
        // Rule 2: DL4J backend uses ZIP format
        val dl4jAdapter = Dl4jNetworkAdapter(config)
        val dl4jPath = "dl4j_model"
        dl4jAdapter.save(dl4jPath)
        assertTrue(File("$dl4jPath.zip").exists(), "DL4J backend should create ZIP file")
        
        // Rule 3: Cross-backend loading is not supported
        assertFailsWith<RuntimeException> {
            manualAdapter.load("$dl4jPath.zip")
        }
        assertFailsWith<RuntimeException> {
            dl4jAdapter.load(manualPath)
        }
        
        // Rule 4: Same-backend loading works
        val manualAdapter2 = ManualNetworkAdapter(config)
        val dl4jAdapter2 = Dl4jNetworkAdapter(config)
        
        // These should work without exceptions
        manualAdapter2.load(manualPath)
        dl4jAdapter2.load(dl4jPath)
        
        // Clean up
        File(manualPath).delete()
        File("$manualPath.optimizer").delete()
        File("$dl4jPath.zip").delete()
        File("$dl4jPath.zip.meta.json").delete()
    }
    
    @Test
    fun testHelpfulErrorMessagesForMissingFiles() {
        val dl4jAdapter = Dl4jNetworkAdapter(config)
        
        // Test loading non-existent ZIP file (should get file not found error)
        val exception1 = assertFailsWith<RuntimeException> {
            dl4jAdapter.load("nonexistent_model.zip")
        }
        val message1 = exception1.message ?: ""
        assertTrue(message1.contains("not found") || message1.contains("does not exist") || message1.contains("No such file"),
                  "Should indicate file not found, got: $message1")
        
        // Test loading file with wrong extension (should get format error or file not found)
        // The checkpoint resolution system will detect that the file doesn't exist
        val exception2 = assertFailsWith<RuntimeException> {
            dl4jAdapter.load("nonexistent_model.json")
        }
        val message2 = exception2.message ?: ""
        assertTrue(message2.contains("JSON") || message2.contains("manual") || 
                  message2.contains("not found") || message2.contains("No checkpoint found"),
                  "Should indicate format incompatibility or file not found, got: $message2")
    }
    
    @Test
    fun testNetworkValidationAfterLoad() {
        val originalAdapter = Dl4jNetworkAdapter(config)
        val modelPath = "validation_test_model"
        
        // Train and save
        val inputs = Array(2) { DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble() } }
        val targets = Array(2) { DoubleArray(config.outputSize) { kotlin.random.Random.nextDouble() } }
        originalAdapter.trainBatch(inputs, targets)
        originalAdapter.save(modelPath)
        
        // Load into new adapter
        val loadedAdapter = Dl4jNetworkAdapter(config)
        loadedAdapter.load(modelPath)
        
        // Validation should pass
        assertTrue(loadedAdapter.validateGradients(), "Loaded network should pass gradient validation")
        
        // Forward pass should work with correct dimensions
        val testInput = DoubleArray(config.inputSize) { 0.1 }
        val output = loadedAdapter.forward(testInput)
        assertEquals(config.outputSize, output.size, "Output should have correct dimensions")
        assertTrue(output.all { it.isFinite() }, "All outputs should be finite")
        
        // Clean up
        File("$modelPath.zip").delete()
        File("$modelPath.zip.meta.json").delete()
    }
}