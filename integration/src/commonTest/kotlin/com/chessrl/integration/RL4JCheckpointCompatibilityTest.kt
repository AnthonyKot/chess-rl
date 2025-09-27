package com.chessrl.integration

import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.backend.CheckpointCompatibility
import com.chessrl.integration.backend.CheckpointResolution
import com.chessrl.integration.config.ChessRLConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test to verify the specific RL4J checkpoint compatibility issues identified
 */
class RL4JCheckpointCompatibilityTest {

    private fun createTestConfig(): ChessRLConfig {
        return ChessRLConfig(
            learningRate = 0.001,
            batchSize = 8,
            gamma = 0.99,
            targetUpdateFrequency = 10,
            maxExperienceBuffer = 100,
            hiddenLayers = listOf(64, 32),
            explorationRate = 0.1,
            doubleDqn = false,
            optimizer = "adam",
            replayType = "UNIFORM",
            seed = 12345L,
            maxStepsPerGame = 100,
            nnBackend = BackendType.RL4J
        )
    }

    @Test
    fun testZipFormatMappedToRL4J() {
        // Verify that ZIP format is now mapped to RL4J, not DL4J
        val format = CheckpointCompatibility.CheckpointFormat.fromPath("test.zip")
        assertEquals(CheckpointCompatibility.CheckpointFormat.ZIP, format)
        assertEquals(BackendType.RL4J, format?.backend)
    }

    @Test
    fun testRL4JCheckpointResolution() {
        // Test that resolving a .zip path for RL4J backend should work
        val resolution = CheckpointCompatibility.resolveCheckpointPath("nonexistent.zip", BackendType.RL4J)
        
        when (resolution) {
            is CheckpointResolution.NotFound -> {
                // This is expected since the file doesn't exist
                assertTrue(resolution.message.contains("nonexistent.zip"))
            }
            is CheckpointResolution.FormatMismatch -> {
                // This should NOT happen anymore - ZIP should be compatible with RL4J
                throw AssertionError("ZIP format should be compatible with RL4J backend, but got format mismatch: ${resolution.suggestion}")
            }
            is CheckpointResolution.Success -> {
                // This won't happen since file doesn't exist, but would be correct
                assertEquals(BackendType.RL4J, resolution.backend)
            }
        }
    }

    @Test
    fun testBaselineEvaluatorFormatMapping() {
        // Test the BaselineEvaluator's format detection
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        val formatInfo = evaluator.getCheckpointFormatInfo("test.zip")
        
        assertEquals(CheckpointFormat.ZIP, formatInfo.format)
        assertEquals(BackendType.RL4J, formatInfo.suggestedBackend)
    }

    @Test
    fun testCompatibilityCheck() {
        // Test that ZIP files are considered compatible with RL4J
        val isCompatible = CheckpointCompatibility.isFormatCompatible("test.zip", BackendType.RL4J)
        assertTrue(isCompatible, "ZIP format should be compatible with RL4J backend")
        
        // Test that ZIP files are NOT compatible with DL4J anymore
        val isIncompatible = CheckpointCompatibility.isFormatCompatible("test.zip", BackendType.DL4J)
        assertTrue(!isIncompatible, "ZIP format should NOT be compatible with DL4J backend")
    }

    @Test
    fun testErrorMessageForZipWithDL4J() {
        // Test that requesting DL4J for a ZIP file gives appropriate error message
        val message = CheckpointCompatibility.getFormatMismatchMessage("test.zip", BackendType.DL4J)
        assertTrue(message.contains("rl4j"), "Error message should suggest using RL4J for ZIP files")
    }
}