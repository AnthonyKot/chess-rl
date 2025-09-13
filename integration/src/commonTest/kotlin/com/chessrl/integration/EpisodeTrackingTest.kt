package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.test.*

/**
 * Test to verify enhanced episode tracking functionality
 */
class EpisodeTrackingTest {
    
    @Test
    fun testEpisodeTerminationTracking() {
        println("🧪 Testing enhanced episode termination tracking...")
        
        // Create agent
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val environment = ChessEnvironment()
        
        // Test 1: Manual episode completion (need to have some episode activity first)
        // Simulate some episode activity
        var state = environment.reset()
        val validActions = environment.getValidActions(state)
        val action = agent.selectAction(state, validActions)
        val stepResult = environment.step(action)
        
        val experience1 = Experience(
            state = state,
            action = action,
            reward = stepResult.reward,
            nextState = stepResult.nextState,
            done = false // Don't end naturally
        )
        agent.learn(experience1)
        
        // Now complete manually
        agent.completeEpisodeManually(EpisodeTerminationReason.STEP_LIMIT)
        
        // Repeat for other termination types
        state = environment.reset()
        val experience2 = Experience(
            state = state,
            action = action,
            reward = 0.0,
            nextState = state,
            done = false
        )
        agent.learn(experience2)
        agent.completeEpisodeManually(EpisodeTerminationReason.GAME_ENDED)
        
        val experience3 = Experience(
            state = state,
            action = action,
            reward = 0.0,
            nextState = state,
            done = false
        )
        agent.learn(experience3)
        agent.completeEpisodeManually(EpisodeTerminationReason.MANUAL)
        
        val metrics1 = agent.getTrainingMetrics()
        assertEquals(3, metrics1.episodeCount, "Should have 3 completed episodes")
        assertEquals(1, metrics1.stepLimitEpisodes, "Should have 1 step limit episode")
        assertEquals(1, metrics1.gameEndedEpisodes, "Should have 1 game ended episode")
        assertEquals(1, metrics1.manualEpisodes, "Should have 1 manual episode")
        
        // Test 2: Natural episode completion through learning
        state = environment.reset()
        val experience4 = Experience(
            state = state,
            action = action,
            reward = 1.0,
            nextState = state,
            done = true // Simulate natural game end
        )
        
        agent.learn(experience4)
        
        val metrics2 = agent.getTrainingMetrics()
        assertEquals(4, metrics2.episodeCount, "Should have 4 completed episodes")
        assertEquals(2, metrics2.gameEndedEpisodes, "Should have 2 game ended episodes")
        
        // Test 3: Reset functionality
        agent.reset()
        val metrics3 = agent.getTrainingMetrics()
        assertEquals(0, metrics3.episodeCount, "Episode count should be reset")
        assertEquals(0, metrics3.stepLimitEpisodes, "Step limit episodes should be reset")
        assertEquals(0, metrics3.gameEndedEpisodes, "Game ended episodes should be reset")
        assertEquals(0, metrics3.manualEpisodes, "Manual episodes should be reset")
        
        println("✅ Episode termination tracking test passed")
    }
    
    @Test
    fun testTrainingPipelineEpisodeTracking() {
        println("🧪 Testing training pipeline episode tracking...")
        
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val environment = ChessEnvironment()
        
        val pipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 10, // Short episodes
                batchSize = 32,
                progressReportInterval = 1
            )
        )
        
        // Run a few episodes
        pipeline.train(totalEpisodes = 2)
        
        // Verify episode tracking
        val metrics = agent.getTrainingMetrics()
        assertTrue(metrics.episodeCount >= 2, "Should have completed at least 2 episodes, but got ${metrics.episodeCount}")
        assertTrue(metrics.stepLimitEpisodes + metrics.gameEndedEpisodes >= 2, 
                  "Episodes should be properly categorized")
        
        println("📊 Pipeline Training Metrics:")
        println("  Episodes completed: ${metrics.episodeCount}")
        println("  - Game ended: ${metrics.gameEndedEpisodes}")
        println("  - Step limit: ${metrics.stepLimitEpisodes}")
        println("  - Manual: ${metrics.manualEpisodes}")
        
        println("✅ Training pipeline episode tracking test passed")
    }
}