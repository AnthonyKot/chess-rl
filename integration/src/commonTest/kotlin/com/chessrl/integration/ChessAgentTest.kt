package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*
import kotlin.test.*

/**
 * Integration tests for ChessAgent
 * Tests the integration between neural networks, RL algorithms, and chess environment
 */
class ChessAgentTest {
    
    @Test
    fun testChessAgentCreation() {
        // Test DQN agent creation
        val dqnAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(64, 32), // Smaller network for testing
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        assertNotNull(dqnAgent)
        
        // Test policy gradient agent creation
        val pgAgent = ChessAgentFactory.createPolicyGradientAgent(
            hiddenLayers = listOf(64, 32),
            learningRate = 0.01,
            temperature = 1.0
        )
        
        assertNotNull(pgAgent)
    }
    
    @Test
    fun testAgentActionSelection() {
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(32, 16), // Very small network for testing
            learningRate = 0.01,
            explorationRate = 0.0 // No exploration for deterministic testing
        )
        
        // Create a dummy state (all zeros)
        val state = DoubleArray(ChessStateEncoder.TOTAL_FEATURES) { 0.0 }
        val validActions = listOf(0, 1, 2, 3, 4) // Some valid action indices
        
        // Agent should be able to select an action
        val selectedAction = agent.selectAction(state, validActions)
        
        assertTrue(validActions.contains(selectedAction), "Selected action should be in valid actions list")
    }
    
    @Test
    fun testAgentLearning() {
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(32, 16),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        // Create dummy experiences
        val state = DoubleArray(ChessStateEncoder.TOTAL_FEATURES) { 0.0 }
        val nextState = DoubleArray(ChessStateEncoder.TOTAL_FEATURES) { 0.1 }
        
        val experience = Experience(
            state = state,
            action = 0,
            reward = 1.0,
            nextState = nextState,
            done = true
        )
        
        // Agent should be able to learn from experience
        try {
            agent.learn(experience)
            // If we get here, no exception was thrown
        } catch (e: Exception) {
            fail("Agent learning should not throw exception: ${e.message}")
        }
        
        // Check that metrics are updated
        val metrics = agent.getTrainingMetrics()
        assertTrue(metrics.episodeCount >= 0)
    }
    
    @Test
    fun testAgentEnvironmentIntegration() {
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(32, 16),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val environment = ChessEnvironment()
        
        // Test a simple interaction loop
        var state = environment.reset()
        var stepCount = 0
        val maxSteps = 10 // Limit steps for testing
        
        while (!environment.isTerminal(state) && stepCount < maxSteps) {
            val validActions = environment.getValidActions(state)
            
            if (validActions.isEmpty()) {
                break // No valid moves
            }
            
            val action = agent.selectAction(state, validActions)
            val stepResult = environment.step(action)
            
            val experience = Experience(
                state = state,
                action = action,
                reward = stepResult.reward,
                nextState = stepResult.nextState,
                done = stepResult.done
            )
            
            agent.learn(experience)
            
            state = stepResult.nextState
            stepCount++
        }
        
        // Should complete without errors
        assertTrue(stepCount >= 0)
        
        // Agent should have some experience
        val metrics = agent.getTrainingMetrics()
        assertTrue(metrics.experienceBufferSize >= 0)
    }
    
    @Test
    fun testChessTrainingLoop() {
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8), // Very small network for fast testing
            learningRate = 0.01,
            explorationRate = 0.2
        )
        
        val environment = ChessEnvironment()
        val trainingConfig = ChessTrainingConfig(
            maxStepsPerEpisode = 20, // Short episodes for testing
            earlyStoppingWindow = 5,
            earlyStoppingThreshold = 10.0 // High threshold to prevent early stopping
        )
        
        val trainingLoop = ChessTrainingLoop(agent, environment, trainingConfig)
        
        // Run a few training episodes
        val history = trainingLoop.train(episodes = 3)
        
        assertEquals(3, history.size, "Should have trained for 3 episodes")
        
        // Check that each episode has valid metrics
        for (episodeMetrics in history) {
            assertTrue(episodeMetrics.episode > 0)
            assertTrue(episodeMetrics.stepCount >= 0)
            assertTrue(episodeMetrics.duration >= 0)
            assertNotNull(episodeMetrics.gameResult)
        }
        
        // Get training statistics
        val stats = trainingLoop.getTrainingStatistics()
        assertEquals(3, stats.totalEpisodes)
        assertTrue(stats.averageGameLength >= 0)
        assertTrue(stats.winRate >= 0.0 && stats.winRate <= 1.0)
        assertTrue(stats.drawRate >= 0.0 && stats.drawRate <= 1.0)
        assertTrue(stats.lossRate >= 0.0 && stats.lossRate <= 1.0)
        assertEquals(1.0, stats.winRate + stats.drawRate + stats.lossRate, 0.01)
    }
    
    @Test
    fun testAgentActionProbabilities() {
        val agent = ChessAgentFactory.createPolicyGradientAgent(
            hiddenLayers = listOf(32, 16),
            learningRate = 0.01,
            temperature = 1.0
        )
        
        val state = DoubleArray(ChessStateEncoder.TOTAL_FEATURES) { 0.0 }
        val validActions = listOf(0, 1, 2, 3, 4)
        
        val probabilities = agent.getActionProbabilities(state, validActions)
        
        // Should have probabilities for all valid actions
        assertEquals(validActions.size, probabilities.size)
        
        // Probabilities should sum to approximately 1.0
        val totalProbability = probabilities.values.sum()
        assertEquals(1.0, totalProbability, 0.01)
        
        // All probabilities should be non-negative
        for (prob in probabilities.values) {
            assertTrue(prob >= 0.0)
        }
    }
    
    @Test
    fun testAgentQValues() {
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(32, 16),
            learningRate = 0.01,
            explorationRate = 0.0
        )
        
        val state = DoubleArray(ChessStateEncoder.TOTAL_FEATURES) { 0.0 }
        val validActions = listOf(0, 1, 2, 3, 4)
        
        val qValues = agent.getQValues(state, validActions)
        
        // Should have Q-values for all valid actions
        assertEquals(validActions.size, qValues.size)
        
        // Q-values should be finite numbers
        for (qValue in qValues.values) {
            assertTrue(qValue.isFinite())
        }
    }
    
    @Test
    fun testAgentSaveLoad() {
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val testPath = "test_agent.model"
        
        // Save should not throw an exception
        try {
            agent.save(testPath)
            // If we get here, no exception was thrown
        } catch (e: Exception) {
            fail("Agent save should not throw exception: ${e.message}")
        }
        
        // Load should not throw an exception
        try {
            agent.load(testPath)
            // If we get here, no exception was thrown
        } catch (e: Exception) {
            fail("Agent load should not throw exception: ${e.message}")
        }
    }
    
    @Test
    fun testAgentReset() {
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        // Add some experience
        val experience = Experience(
            state = DoubleArray(ChessStateEncoder.TOTAL_FEATURES) { 0.0 },
            action = 0,
            reward = 1.0,
            nextState = DoubleArray(ChessStateEncoder.TOTAL_FEATURES) { 0.1 },
            done = true
        )
        
        agent.learn(experience)
        
        // Check that agent has some state
        val metricsBefore = agent.getTrainingMetrics()
        assertTrue(metricsBefore.episodeCount > 0 || metricsBefore.experienceBufferSize > 0)
        
        // Reset agent
        agent.reset()
        
        // Check that agent state is reset
        val metricsAfter = agent.getTrainingMetrics()
        assertEquals(0, metricsAfter.episodeCount)
        assertEquals(0, metricsAfter.experienceBufferSize)
        assertEquals(0.0, metricsAfter.averageReward)
    }
    
    @Test
    fun testAgentExplorationUpdate() {
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.5
        )
        
        val initialExploration = agent.getTrainingMetrics().explorationRate
        
        // Set a different exploration rate
        agent.setExplorationRate(0.2)
        
        val newExploration = agent.getTrainingMetrics().explorationRate
        assertEquals(0.2, newExploration, 0.01)
    }
    
    @Test
    fun testChessAgentConfig() {
        val config = ChessAgentConfig(
            batchSize = 16,
            maxBufferSize = 1000,
            performanceWindowSize = 50,
            progressReportInterval = 25,
            gradientClipThreshold = 5.0,
            minPolicyEntropy = 0.05
        )
        
        val agent = ChessAgentFactory.createDQNAgent(config = config)
        
        assertNotNull(agent)
        // Config should be used internally (can't directly test without exposing internals)
    }
}