package com.chessrl.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChessEnvironmentInvalidActionTest {
    @Test
    fun invalidActionYieldsNegativeReward() {
        val env = ChessEnvironment()
        val state = env.reset()
        assertEquals(ChessStateEncoder.TOTAL_FEATURES, state.size)

        // Choose an always-illegal action in the starting position: a1 -> a1 (index 0)
        val invalidAction = 0
        val step = env.step(invalidAction)

        assertTrue(step.reward < 0.0, "Invalid move should produce negative reward")
        assertFalse(step.done, "Invalid move should not terminate the episode")
        assertEquals(ChessStateEncoder.TOTAL_FEATURES, step.nextState.size)
        assertTrue(step.info.containsKey("error"), "Should include error info for invalid move")
    }
}

