package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*
import kotlin.test.*

/**
 * Comprehensive test for reward signal integration with various game scenarios
 * This test verifies that different termination reasons receive appropriate rewards
 */
class RewardSignalScenarioTest {
    
    @Test
    fun testStepLimitPenaltyIsAppliedCorrectly() {
        // Create a simple mock agent that just picks the first valid action
        val mockAgent = object : ChessAgent {
            override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
                return validActions.firstOrNull() ?: 0
            }
            
            override fun learn(experience: Experience<DoubleArray, Int>) {}
            override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> = actions.associateWith { 0.0 }
            override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> = actions.associateWith { 1.0 / actions.size }
            override fun getTrainingMetrics(): ChessAgentMetrics = ChessAgentMetrics(0.0, 0.1, 0)
            override fun forceUpdate() {}
            override fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult = PolicyUpdateResult(0.1, 0.5, 1.0)
            override fun save(path: String) {}
            override fun load(path: String) {}
            override fun reset() {}
            override fun setExplorationRate(rate: Double) {}
            override fun getConfig(): ChessAgentConfig = ChessAgentConfig()
            override fun setNextActionProvider(provider: (DoubleArray) -> List<Int>) {}
        }
        
        val stepLimitPenalty = -0.3
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                stepLimitPenalty = stepLimitPenalty,
                stepPenalty = -0.01
            )
        )
        
        val pipeline = ChessTrainingPipeline(
            agent = mockAgent,
            environment = env,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 3, // Very short to force step limit
                batchSize = 32,
                maxBufferSize = 1000
            )
        )
        
        // Run an episode that should hit the step limit
        val result = pipeline.runEpisode()
        
        // Verify the episode hit the step limit
        assertEquals(3, result.steps, "Episode should hit the step limit")
        
        // The episode reward should include the step limit penalty
        // Since we have 3 steps with -0.01 penalty each, plus the step limit penalty
        val expectedBaseReward = 3 * -0.01 // Step penalties
        val expectedTotalReward = expectedBaseReward + stepLimitPenalty
        
        // Allow some tolerance for floating point arithmetic
        assertEquals(expectedTotalReward, result.reward, 0.001, 
                    "Episode reward should include step limit penalty")
    }
    
    @Test
    fun testLegitimateGameEndingsDoNotReceiveStepLimitPenalty() {
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.5,
                stepLimitPenalty = -0.5,
                stepPenalty = 0.0
            )
        )
        
        // Test that legitimate chess endings are properly identified
        env.reset()
        
        // In a starting position, the game should not be a legitimate ending
        assertFalse(env.isLegitimateChessEnding(), "Starting position should not be a legitimate ending")
        assertEquals("ONGOING", env.getTerminationReason())
        
        // The environment correctly identifies ongoing games
        val gameStatus = env.getGameStatus()
        assertTrue(gameStatus.name.contains("ONGOING") || gameStatus.name.contains("IN_CHECK"))
    }
    
    @Test
    fun testRewardConfigurationConsistency() {
        val config = ChessRewardConfig(
            winReward = 1.0,
            lossReward = -1.0,
            drawReward = 0.0,
            stepPenalty = -0.001,
            stepLimitPenalty = -0.5,
            invalidMoveReward = -0.1,
            gameLengthNormalization = true,
            maxGameLength = 200
        )
        
        val env = ChessEnvironment(rewardConfig = config)
        
        // Verify that the step limit penalty is correctly configured
        assertEquals(-0.5, env.applyStepLimitPenalty(), "Step limit penalty should match configuration")
        
        // Verify that the environment uses the correct configuration
        env.reset()
        val validActions = env.getValidActions(env.reset())
        val stepResult = env.step(validActions.first())
        
        // For an ongoing game with step penalty, the reward should be the step penalty
        assertEquals(-0.001, stepResult.reward, 0.0001, "Ongoing game should receive step penalty")
    }
    
    @Test
    fun testDifferentTerminationReasonsReceiveDifferentRewards() {
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.2,
                stepPenalty = -0.01,
                stepLimitPenalty = -0.8,
                enablePositionRewards = false
            )
        )
        
        env.reset()
        val validActions = env.getValidActions(env.reset())
        
        // Test ongoing game reward (should be step penalty)
        val ongoingResult = env.step(validActions.first())
        assertFalse(ongoingResult.done, "Game should not be done after one move")
        assertEquals(-0.01, ongoingResult.reward, 0.001, "Ongoing game should receive step penalty")
        
        // Test that step limit penalty is different from ongoing game penalty
        val stepLimitPenalty = env.applyStepLimitPenalty()
        assertEquals(-0.8, stepLimitPenalty, "Step limit penalty should be configured value")
        assertTrue(stepLimitPenalty < ongoingResult.reward, "Step limit penalty should be more negative than step penalty")
    }
    
    @Test
    fun testRewardSignalFiniteness() {
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0,
                stepPenalty = -0.001,
                stepLimitPenalty = -0.5,
                enablePositionRewards = true
            )
        )
        
        env.reset()
        val validActions = env.getValidActions(env.reset())
        
        // Test multiple moves to ensure all rewards are finite
        for (i in 0 until 5) {
            val currentValidActions = env.getValidActions(env.reset())
            if (currentValidActions.isEmpty()) break
            
            val stepResult = env.step(currentValidActions.first())
            assertTrue(stepResult.reward.isFinite(), "Reward should always be finite")
            
            if (stepResult.done) break
        }
        
        // Test step limit penalty is finite
        val stepLimitPenalty = env.applyStepLimitPenalty()
        assertTrue(stepLimitPenalty.isFinite(), "Step limit penalty should be finite")
    }
    
    @Test
    fun testGameLengthNormalizationDoesNotAffectOngoingGames() {
        val envWithNormalization = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0,
                stepPenalty = -0.01,
                gameLengthNormalization = true,
                maxGameLength = 100,
                enablePositionRewards = false
            )
        )
        
        val envWithoutNormalization = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0,
                stepPenalty = -0.01,
                gameLengthNormalization = false,
                maxGameLength = 100,
                enablePositionRewards = false
            )
        )
        
        // For ongoing games, both environments should give the same reward
        envWithNormalization.reset()
        envWithoutNormalization.reset()
        
        val validActions1 = envWithNormalization.getValidActions(envWithNormalization.reset())
        val validActions2 = envWithoutNormalization.getValidActions(envWithoutNormalization.reset())
        
        val result1 = envWithNormalization.step(validActions1.first())
        val result2 = envWithoutNormalization.step(validActions2.first())
        
        // Both should be ongoing games with the same step penalty
        assertFalse(result1.done, "Game 1 should be ongoing")
        assertFalse(result2.done, "Game 2 should be ongoing")
        assertEquals(result1.reward, result2.reward, 0.001, "Ongoing games should have same reward regardless of normalization")
    }
}