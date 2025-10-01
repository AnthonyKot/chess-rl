package com.chessrl.rl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * RL framework package tests
 */
class EnvironmentTest {
    
    @Test
    fun testStepResultCreation() {
        val stepResult = StepResult(
            nextState = "test_state",
            reward = 1.0,
            done = false
        )
        assertEquals("test_state", stepResult.nextState)
        assertEquals(1.0, stepResult.reward)
        assertEquals(false, stepResult.done)
    }
    
    @Test
    fun testExperienceCreation() {
        val experience = Experience(
            state = "state1",
            action = "action1",
            reward = 0.5,
            nextState = "state2",
            done = false
        )
        assertEquals("state1", experience.state)
        assertEquals("action1", experience.action)
        assertEquals(0.5, experience.reward)
        assertEquals("state2", experience.nextState)
        assertEquals(false, experience.done)
    }
}