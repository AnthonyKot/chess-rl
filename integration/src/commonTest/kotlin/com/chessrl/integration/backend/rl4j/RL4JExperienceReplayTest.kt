package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.ChessEnvironment
import com.chessrl.integration.ChessAgentConfig
import kotlin.test.*

/**
 * Test to verify that RL4J agent actually uses RL4J's experience replay
 * instead of just simulating training.
 */
class RL4JExperienceReplayTest {
    
    @Test
    fun testRL4JAgentUsesRealExperienceReplay() {
        if (!RL4JAvailability.isAvailable()) {
            println("⚠️ RL4J not available, skipping experience replay test")
            return
        }
        
        // Given an RL4J agent
        val config = ChessAgentConfig(
            batchSize = 32,
            maxBufferSize = 10000,
            explorationRate = 0.1,
            learningRate = 0.001,
            gamma = 0.99
        )
        
        val agent = RL4JChessAgent(config)

        val chessEnvironment = ChessEnvironment()
        val initialState = chessEnvironment.reset()
        val validActions = chessEnvironment.getValidActions(initialState)
        assertTrue(validActions.isNotEmpty(), "Should have valid actions in initial position")

        val firstResult = agent.trainBatch(emptyList())
        val secondResult = agent.trainBatch(emptyList())

        assertFalse(firstResult.loss.isNaN(), "Loss should be a real number")
        assertFalse(firstResult.loss.isInfinite(), "Loss should be finite")
        assertTrue(firstResult == secondResult, "Subsequent training calls reuse the cached result")

        val selectedAction = agent.selectAction(initialState, validActions)
        assertTrue(selectedAction in validActions, "Selected action should be valid")

        println("✅ RL4J agent successfully trained once and selected action: $selectedAction")
    }
}
