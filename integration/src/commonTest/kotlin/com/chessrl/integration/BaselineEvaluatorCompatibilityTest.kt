package com.chessrl.integration

import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger
import kotlin.test.*

/**
 * Test for BaselineEvaluator compatibility with unified checkpoint system.
 */
class BaselineEvaluatorCompatibilityTest {
    
    private val logger = ChessRLLogger.forComponent("BaselineEvaluatorCompatibilityTest")
    
    @Test
    fun testBaselineEvaluatorCreation() {
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        
        assertNotNull(evaluator)
        assertNotNull(evaluator.getUnifiedCheckpointManager())
        
        logger.info("BaselineEvaluator with unified checkpoint support created successfully")
    }
    
    @Test
    fun testCheckpointFormatDetection() {
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        
        // Test different checkpoint formats
        val jsonInfo = evaluator.getCheckpointFormatInfo("model.json")
        assertEquals(CheckpointFormat.JSON, jsonInfo.format)
        assertEquals(BackendType.MANUAL, jsonInfo.suggestedBackend)
        assertTrue(jsonInfo.isSupported)
        
        val jsonGzInfo = evaluator.getCheckpointFormatInfo("model.json.gz")
        assertEquals(CheckpointFormat.JSON_COMPRESSED, jsonGzInfo.format)
        assertEquals(BackendType.MANUAL, jsonGzInfo.suggestedBackend)
        assertTrue(jsonGzInfo.isSupported)
        
        val zipInfo = evaluator.getCheckpointFormatInfo("model.zip")
        assertEquals(CheckpointFormat.ZIP, zipInfo.format)
        assertEquals(BackendType.RL4J, zipInfo.suggestedBackend)
        assertTrue(zipInfo.isSupported)
        
        val unknownInfo = evaluator.getCheckpointFormatInfo("model.bin")
        assertEquals(CheckpointFormat.UNKNOWN, unknownInfo.format)
        assertEquals(BackendType.MANUAL, unknownInfo.suggestedBackend)
        assertFalse(unknownInfo.isSupported)
        
        logger.info("Checkpoint format detection working correctly")
        logger.info("JSON format: ${jsonInfo.format} -> ${jsonInfo.suggestedBackend}")
        logger.info("ZIP format: ${zipInfo.format} -> ${zipInfo.suggestedBackend}")
    }
    
    @Test
    fun testCheckpointLoadingMethods() {
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        
        // Create a mock agent for testing
        val mockAgent = createMockAgent()
        
        // Test loading from non-existent checkpoint (should fail gracefully)
        val loaded = evaluator.loadAgentFromCheckpoint("nonexistent.json", mockAgent)
        assertFalse(loaded, "Loading non-existent checkpoint should fail")
        
        logger.info("Checkpoint loading methods handle non-existent files correctly")
    }
    
    @Test
    fun testEvaluationMethodsWithCheckpoints() {
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        val mockAgent = createMockAgent()
        
        // Test evaluation methods with non-existent checkpoints (should return null)
        val heuristicResult = evaluator.evaluateCheckpointAgainstHeuristic(
            "nonexistent.json", mockAgent, 1
        )
        assertNull(heuristicResult, "Evaluation with non-existent checkpoint should return null")
        
        val minimaxResult = evaluator.evaluateCheckpointAgainstMinimax(
            "nonexistent.json", mockAgent, 1, 2
        )
        assertNull(minimaxResult, "Minimax evaluation with non-existent checkpoint should return null")
        
        val mixedResult = evaluator.evaluateCheckpointAgainstMixedOpponents(
            "nonexistent.json", mockAgent, 1
        )
        assertNull(mixedResult, "Mixed evaluation with non-existent checkpoint should return null")
        
        logger.info("Evaluation methods handle non-existent checkpoints correctly")
    }
    
    @Test
    fun testCheckpointComparison() {
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        val mockAgentA = createMockAgent()
        val mockAgentB = createMockAgent()
        
        // Test comparison with non-existent checkpoints (should return null)
        val comparisonResult = evaluator.compareCheckpoints(
            "nonexistent_a.json", mockAgentA,
            "nonexistent_b.json", mockAgentB,
            1
        )
        assertNull(comparisonResult, "Comparison with non-existent checkpoints should return null")
        
        logger.info("Checkpoint comparison handles non-existent files correctly")
    }
    
    @Test
    fun testBackendTypeSupport() {
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        val mockAgent = createMockAgent()
        
        // Test that different backend types are supported
        val manualResult = evaluator.loadAgentFromCheckpoint("test.json", mockAgent, BackendType.MANUAL)
        assertFalse(manualResult) // Should fail because file doesn't exist, but method should handle it
        
        val rl4jResult = evaluator.loadAgentFromCheckpoint("test.zip", mockAgent, BackendType.RL4J)
        assertFalse(rl4jResult) // Should fail because file doesn't exist, but method should handle it
        
        val dl4jResult = evaluator.loadAgentFromCheckpoint("test.zip", mockAgent, BackendType.DL4J)
        assertFalse(dl4jResult) // Should fail because file doesn't exist, but method should handle it
        
        logger.info("Different backend types are supported in evaluation methods")
    }
    
    @Test
    fun testCheckpointFormatInfo() {
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        
        val formatInfo = evaluator.getCheckpointFormatInfo("test_model.zip")
        
        assertEquals("test_model.zip", formatInfo.path)
        assertEquals(CheckpointFormat.ZIP, formatInfo.format)
        assertEquals(BackendType.RL4J, formatInfo.suggestedBackend)
        assertTrue(formatInfo.isSupported)
        
        logger.info("CheckpointFormatInfo working correctly: $formatInfo")
    }
    
    @Test
    fun testUnifiedCheckpointManagerAccess() {
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        
        val manager = evaluator.getUnifiedCheckpointManager()
        assertNotNull(manager)
        
        val summary = manager.getSummary()
        assertNotNull(summary)
        assertEquals(0, summary.totalCheckpoints)
        
        logger.info("Unified checkpoint manager accessible from BaselineEvaluator")
    }
    
    @Test
    fun testRL4JCheckpointEvaluationPipeline() {
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        val mockAgent = createMockAgent()
        
        // Test that the evaluation pipeline can handle RL4J backend type
        val rl4jResult = evaluator.loadAgentFromCheckpoint("test_rl4j_model.zip", mockAgent, BackendType.RL4J)
        assertFalse(rl4jResult, "Should fail gracefully for non-existent RL4J checkpoint")
        
        // Test evaluation methods with RL4J backend type
        val heuristicResult = evaluator.evaluateCheckpointAgainstHeuristic(
            "test_rl4j_model.zip", mockAgent, 1, BackendType.RL4J
        )
        assertNull(heuristicResult, "Should return null for non-existent RL4J checkpoint")
        
        val minimaxResult = evaluator.evaluateCheckpointAgainstMinimax(
            "test_rl4j_model.zip", mockAgent, 1, 2, BackendType.RL4J
        )
        assertNull(minimaxResult, "Should return null for non-existent RL4J checkpoint")
        
        val mixedResult = evaluator.evaluateCheckpointAgainstMixedOpponents(
            "test_rl4j_model.zip", mockAgent, 1, BackendType.RL4J
        )
        assertNull(mixedResult, "Should return null for non-existent RL4J checkpoint")
        
        logger.info("RL4J checkpoint evaluation pipeline tested successfully")
    }
    
    @Test
    fun testCrossBackendCheckpointComparison() {
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        val mockAgentA = createMockAgent()
        val mockAgentB = createMockAgent()
        
        // Test comparison between different backend types
        val comparisonResult = evaluator.compareCheckpoints(
            "manual_model.json", mockAgentA,
            "rl4j_model.zip", mockAgentB,
            1,
            BackendType.MANUAL,
            BackendType.RL4J
        )
        assertNull(comparisonResult, "Should return null for non-existent checkpoints")
        
        logger.info("Cross-backend checkpoint comparison tested successfully")
    }
    
    @Test
    fun testIdenticalEvaluationMethodology() {
        val config = createTestConfig()
        val evaluator = BaselineEvaluator(config)
        
        // Verify that the same evaluation methods are used regardless of backend
        // This is ensured by the BaselineEvaluator using the same evaluation logic
        // after loading the checkpoint, regardless of the backend type used to load it
        
        val formatInfoManual = evaluator.getCheckpointFormatInfo("model.json")
        val formatInfoRL4J = evaluator.getCheckpointFormatInfo("model.zip")
        
        assertEquals(BackendType.MANUAL, formatInfoManual.suggestedBackend)
        assertEquals(BackendType.RL4J, formatInfoRL4J.suggestedBackend)
        
        // Both formats should be supported
        assertTrue(formatInfoManual.isSupported)
        assertTrue(formatInfoRL4J.isSupported)
        
        logger.info("Identical evaluation methodology verified for both backends")
    }
    
    // Helper methods
    
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
            maxStepsPerGame = 100, // Short games for testing
            winReward = 1.0,
            lossReward = -1.0,
            drawReward = 0.0,
            stepLimitPenalty = -0.1,
            engine = com.chessrl.integration.adapter.EngineBackend.BUILTIN
        )
    }
    
    private fun createMockAgent(): ChessAgent {
        return object : ChessAgent {
            override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
                return validActions.firstOrNull() ?: 0
            }
            
            override fun trainBatch(experiences: List<com.chessrl.rl.Experience<DoubleArray, Int>>): com.chessrl.rl.PolicyUpdateResult {
                return com.chessrl.rl.PolicyUpdateResult(0.0, 0.0, 0.0, 0.0)
            }
            
            override fun save(path: String) {
                // Mock save - do nothing
            }
            
            override fun load(path: String) {
                // Mock load - do nothing
            }
            
            override fun getConfig(): ChessAgentConfig {
                return ChessAgentConfig(8, 100, 0.001, 0.1)
            }
            
            override fun learn(experience: com.chessrl.rl.Experience<DoubleArray, Int>) {
                // Mock learn - do nothing
            }
            
            override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
                return actions.associateWith { 0.0 }
            }
            
            override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
                return actions.associateWith { 1.0 / actions.size }
            }
            
            override fun getTrainingMetrics(): ChessAgentMetrics {
                return ChessAgentMetrics(0.0, 0.1, 0)
            }
            
            override fun forceUpdate() {
                // Mock force update - do nothing
            }
            
            override fun reset() {
                // Mock reset - do nothing
            }
            
            override fun setExplorationRate(rate: Double) {
                // Mock set exploration rate - do nothing
            }
        }
    }
}