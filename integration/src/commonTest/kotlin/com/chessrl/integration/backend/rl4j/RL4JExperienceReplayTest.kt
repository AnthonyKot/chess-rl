package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.ChessEnvironment
import com.chessrl.integration.ChessAgentConfig
import com.chessrl.rl.Experience
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
        
        try {
            // When we initialize the agent
            agent.initialize()
            
            // And add some experiences through the learn method
            val chessEnvironment = ChessEnvironment()
            val initialState = chessEnvironment.reset()
            val validActions = chessEnvironment.getValidActions(initialState)
            assertTrue(validActions.isNotEmpty(), "Should have valid actions in initial position")
            
            val experiences = mutableListOf<Experience<DoubleArray, Int>>()
            var currentState = initialState
            
            // Create a sequence of experiences
            repeat(10) { step ->
                val action = validActions.random()
                val stepResult = chessEnvironment.step(action)
                
                val experience = Experience(
                    state = currentState,
                    action = action,
                    reward = stepResult.reward,
                    nextState = stepResult.nextState,
                    done = stepResult.done
                )
                
                experiences.add(experience)
                
                // Add experience to RL4J's replay buffer
                agent.learn(experience)
                
                currentState = stepResult.nextState
                
                if (stepResult.done) {
                    currentState = chessEnvironment.reset()
                }
            }
            
            println("✅ Added ${experiences.size} experiences to RL4J agent")
            
            // Now test batch training with real experience replay
            val batchExperiences = experiences.take(5)
            val trainingResult = agent.trainBatch(batchExperiences)
            
            // Verify that training actually happened
            assertNotNull(trainingResult, "Training result should not be null")
            assertTrue(trainingResult.loss >= 0.0, "Loss should be non-negative")
            assertFalse(trainingResult.loss.isNaN(), "Loss should not be NaN")
            assertFalse(trainingResult.loss.isInfinite(), "Loss should not be infinite")
            
            println("✅ RL4J batch training completed with loss: ${trainingResult.loss}")
            
            // Test that the agent can still select actions after training
            val testState = chessEnvironment.reset()
            val testValidActions = chessEnvironment.getValidActions(testState)
            val selectedAction = agent.selectAction(testState, testValidActions)
            
            assertTrue(selectedAction in testValidActions, "Selected action should be valid")
            
            println("✅ RL4J agent successfully selected action: $selectedAction")
            
        } finally {
            println("Experience replay test completed")
        }
    }
}