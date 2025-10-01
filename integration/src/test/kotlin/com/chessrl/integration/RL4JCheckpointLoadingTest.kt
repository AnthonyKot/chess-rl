package com.chessrl.integration

import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.rl.Experience
import com.chessrl.rl.PolicyUpdateResult
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Test to verify that RL4J checkpoints can actually be loaded through the UnifiedCheckpointManager
 */
class RL4JCheckpointLoadingTest {

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

    private fun createMockAgent(): ChessAgent {
        return object : ChessAgent {
            override fun selectAction(state: DoubleArray, validActions: List<Int>): Int = 0
            override fun learn(experience: Experience<DoubleArray, Int>) {}
            override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> = emptyMap()
            override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> = emptyMap()
            override fun getTrainingMetrics(): ChessAgentMetrics = ChessAgentMetrics(0.0, 0.0, 0)
            override fun forceUpdate() {}
            override fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult = PolicyUpdateResult(0.0, 0.0, 0.0)
            override fun save(path: String) {}
            override fun load(path: String) {}
            override fun reset() {}
            override fun setExplorationRate(rate: Double) {}
            override fun getConfig(): ChessAgentConfig = ChessAgentConfig()
        }
    }

    @Test
    fun testUnifiedCheckpointManagerRL4JLoading() {
        val config = createTestConfig()
        val manager = UnifiedCheckpointManager()
        val mockAgent = createMockAgent()
        
        // Test loading a non-existent RL4J checkpoint
        // This should fail with "file not found", not "format mismatch"
        val result = manager.loadCheckpointByPath("nonexistent_rl4j.zip", mockAgent, BackendType.RL4J)
        
        assertFalse(result.success, "Loading non-existent file should fail")
        assertTrue(
            result.error?.contains("not found") == true || result.error?.contains("does not exist") == true,
            "Error should be about file not found, not format mismatch. Got: ${result.error}"
        )
        assertFalse(
            result.error?.contains("format mismatch") == true,
            "Should not get format mismatch error for RL4J loading ZIP file. Got: ${result.error}"
        )
    }

    @Test
    fun testBaselineEvaluatorRL4JCheckpointLoading() {
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        val mockAgent = createMockAgent()
        
        // Test that BaselineEvaluator can attempt to load RL4J checkpoints
        // This should fail with file not found, not format issues
        try {
            val result = evaluator.loadAgentFromCheckpoint("nonexistent_rl4j.zip", mockAgent, BackendType.RL4J)
            assertFalse(result, "Loading non-existent file should return false")
        } catch (e: Exception) {
            // If it throws an exception, it should be about file not found, not format
            assertTrue(
                e.message?.contains("not found") == true || e.message?.contains("does not exist") == true,
                "Exception should be about file not found, not format. Got: ${e.message}"
            )
        }
    }

    @Test
    fun testRL4JCheckpointPathResolution() {
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        
        // Test that the path resolution works correctly for RL4J
        val formatInfo = evaluator.getCheckpointFormatInfo("test_model.zip")
        
        assertTrue(formatInfo.isSupported, "ZIP format should be supported")
        assertTrue(formatInfo.suggestedBackend == BackendType.RL4J, "ZIP should suggest RL4J backend")
    }
}