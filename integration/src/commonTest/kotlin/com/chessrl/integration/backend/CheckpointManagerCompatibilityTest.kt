package com.chessrl.integration.backend

import com.chessrl.integration.*
import com.chessrl.rl.*
import kotlin.test.*

/**
 * Test suite for CheckpointManager integration with backend compatibility system
 */
class CheckpointManagerCompatibilityTest {
    
    @Test
    fun testCheckpointManagerBackendAwareLoading() {
        val config = CheckpointConfig(
            baseDirectory = "test_checkpoints",
            validationEnabled = false // Disable validation for testing
        )
        val manager = CheckpointManager(config)
        
        // Test that the manager can handle backend-aware loading
        // Note: This is a unit test, so we test the logic without actual file I/O
        
        val backendConfig = BackendConfig(
            inputSize = 10,
            outputSize = 5,
            hiddenLayers = listOf(8)
        )
        
        // Create a mock agent for testing
        val mockAgent = MockChessAgent()
        
        // Test loading with different backend preferences
        val result = manager.loadCheckpointWithBackend(
            "test_model", 
            mockAgent, 
            BackendType.MANUAL
        )
        
        // The result should indicate failure since no actual file exists
        assertFalse(result.success, "Should fail when no checkpoint file exists")
        assertTrue(result.error?.contains("No checkpoint found") == true,
            "Should provide helpful error message")
    }
    
    @Test
    fun testCheckpointFormatDetectionInManager() {
        // Test that the manager correctly identifies different checkpoint formats
        
        // Manual backend formats
        assertEquals(BackendType.MANUAL, CheckpointCompatibility.detectBackendFromFile("model.json"))
        assertEquals(BackendType.MANUAL, CheckpointCompatibility.detectBackendFromFile("checkpoint.json.gz"))
        
        // DL4J backend formats
        assertEquals(BackendType.DL4J, CheckpointCompatibility.detectBackendFromFile("model.zip"))
        
        // KotlinDL backend formats
        assertEquals(BackendType.KOTLINDL, CheckpointCompatibility.detectBackendFromFile("model.h5"))
        assertEquals(BackendType.KOTLINDL, CheckpointCompatibility.detectBackendFromFile("model.savedmodel"))
        
        // Unknown format
        assertEquals(null, CheckpointCompatibility.detectBackendFromFile("model.unknown"))
    }
    
    @Test
    fun testCheckpointPathGeneration() {
        // Test that the manager generates correct paths for different backends
        
        assertEquals("model.json", CheckpointCompatibility.generateSavePath("model", BackendType.MANUAL))
        assertEquals("model.zip", CheckpointCompatibility.generateSavePath("model", BackendType.DL4J))
        assertEquals("model.h5", CheckpointCompatibility.generateSavePath("model", BackendType.KOTLINDL))
        
        // Test with existing extensions (should be replaced)
        assertEquals("model.json", CheckpointCompatibility.generateSavePath("model.zip", BackendType.MANUAL))
        assertEquals("model.zip", CheckpointCompatibility.generateSavePath("model.json", BackendType.DL4J))
    }
    
    @Test
    fun testCompatibilityValidation() {
        // Test compatibility validation logic
        
        assertTrue(CheckpointCompatibility.isFormatCompatible("model.json", BackendType.MANUAL))
        assertTrue(CheckpointCompatibility.isFormatCompatible("model.zip", BackendType.DL4J))
        assertTrue(CheckpointCompatibility.isFormatCompatible("model.h5", BackendType.KOTLINDL))
        
        assertFalse(CheckpointCompatibility.isFormatCompatible("model.json", BackendType.DL4J))
        assertFalse(CheckpointCompatibility.isFormatCompatible("model.zip", BackendType.MANUAL))
        assertFalse(CheckpointCompatibility.isFormatCompatible("model.h5", BackendType.MANUAL))
    }
    
    @Test
    fun testErrorMessageGeneration() {
        // Test that helpful error messages are generated for format mismatches
        
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
    fun testCheckpointResolutionLogic() {
        // Test the checkpoint resolution logic (without actual file I/O)
        
        // Test format detection from paths
        val formats = CheckpointCompatibility.CheckpointFormat.values()
        
        // Manual backend formats
        val manualFormats = CheckpointCompatibility.CheckpointFormat.forBackend(BackendType.MANUAL)
        assertTrue(manualFormats.any { it.extension == ".json" })
        assertTrue(manualFormats.any { it.extension == ".json.gz" })
        
        // DL4J backend formats
        val dl4jFormats = CheckpointCompatibility.CheckpointFormat.forBackend(BackendType.DL4J)
        assertTrue(dl4jFormats.any { it.extension == ".zip" })
        
        // KotlinDL backend formats
        val kotlindlFormats = CheckpointCompatibility.CheckpointFormat.forBackend(BackendType.KOTLINDL)
        assertTrue(kotlindlFormats.any { it.extension == ".h5" })
        assertTrue(kotlindlFormats.any { it.extension == ".savedmodel" })
    }
    
    /**
     * Mock ChessAgent for testing purposes
     */
    private class MockChessAgent : ChessAgent {
        override fun selectAction(state: DoubleArray, validActions: List<Int>): Int = validActions.firstOrNull() ?: 0
        override fun learn(experience: Experience<DoubleArray, Int>) {}
        override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> = actions.associateWith { 0.0 }
        override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> = actions.associateWith { 1.0 / actions.size }
        override fun getTrainingMetrics(): ChessAgentMetrics = ChessAgentMetrics(0.0, 0.1, 0)
        override fun forceUpdate() {}
        override fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult = PolicyUpdateResult(0.0, 0.0, 0.0)
        override fun save(path: String) {}
        override fun load(path: String) {
            // Simulate loading failure for non-existent files
            throw RuntimeException("Mock agent: file not found")
        }
        override fun reset() {}
        override fun setExplorationRate(rate: Double) {}
        override fun getConfig(): ChessAgentConfig = ChessAgentConfig()
    }
}