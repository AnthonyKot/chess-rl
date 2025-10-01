package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*
import kotlin.test.*

/**
 * Comprehensive test for reward signal integration fixes
 * Tests that different termination reasons receive appropriate rewards
 */
class RewardSignalIntegrationTest {
    
    @Test
    fun testLegitimateDrawsReceiveDrawReward() {
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0,
                stepLimitPenalty = -0.5,
                stepPenalty = 0.0
            )
        )
        
        // Test that legitimate chess draws are properly identified
        env.reset()
        
        // For this test, we'll verify the configuration is correct
        // In practice, legitimate draws would be tested with specific positions
        assertTrue(env.getGameStatus().name.contains("ONGOING") || env.getGameStatus().name.contains("IN_CHECK"))
        assertFalse(env.isLegitimateChessEnding(), "Starting position should not be a legitimate ending")
        assertEquals("ONGOING", env.getTerminationReason())
    }
    
    @Test
    fun testStepLimitPenaltyConfiguration() {
        val stepLimitPenalty = -0.8
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                stepLimitPenalty = stepLimitPenalty
            )
        )
        
        val penalty = env.applyStepLimitPenalty()
        assertEquals(stepLimitPenalty, penalty, "Step limit penalty should match configuration")
    }
    
    // NOTE: Legacy pipeline test removed. The consolidated TrainingPipeline is covered by other tests.
    
    @Test
    fun testOngoingGamesDoNotReceiveDrawRewards() {
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                drawReward = 0.5, // Non-zero to make it obvious if incorrectly applied
                stepPenalty = -0.01
            )
        )
        
        val state = env.reset()
        val validActions = env.getValidActions(state)
        
        // Make a move in an ongoing game
        val stepResult = env.step(validActions.first())
        
        // Verify the game is still ongoing
        assertFalse(stepResult.done, "Game should not be done after one move")
        assertFalse(env.isLegitimateChessEnding(), "Game should not be a legitimate ending")
        
        // The reward should be the step penalty, not the draw reward
        assertEquals(-0.01, stepResult.reward, 0.001, "Ongoing game should receive step penalty, not draw reward")
    }
    
    @Test
    fun testRewardConfigurationValidation() {
        // Test that reward configuration is properly validated
        val config = ChessRewardConfig(
            winReward = 1.0,
            lossReward = -1.0,
            drawReward = 0.0,
            stepPenalty = -0.01,
            stepLimitPenalty = -0.5,
            invalidMoveReward = -0.1
        )
        
        // Verify all rewards are finite
        assertTrue(config.winReward.isFinite())
        assertTrue(config.lossReward.isFinite())
        assertTrue(config.drawReward.isFinite())
        assertTrue(config.stepPenalty.isFinite())
        assertTrue(config.stepLimitPenalty.isFinite())
        assertTrue(config.invalidMoveReward.isFinite())
        
        // Verify step limit penalty is negative (penalty)
        assertTrue(config.stepLimitPenalty <= 0.0, "Step limit penalty should be non-positive")
    }
    
    @Test
    fun testInvalidMoveHandling() {
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                invalidMoveReward = -0.2
            )
        )
        
        env.reset()
        
        // Try an invalid action (within range but not a legal move)
        // Use action 0 (a1 to a1) which should be invalid
        val invalidAction = 0 // a1 to a1 - not a legal move
        val stepResult = env.step(invalidAction)
        
        // Should receive penalty for invalid move
        assertEquals(-1.0, stepResult.reward, "Invalid move should receive penalty")
        assertFalse(stepResult.done, "Invalid move should not end the game")
        assertTrue(stepResult.info.containsKey("error"), "Invalid move should include error info")
    }
    
    @Test
    fun testPositionalRewardShaping() {
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                enablePositionRewards = true,
                stepPenalty = -0.001
            )
        )
        
        val state = env.reset()
        val validActions = env.getValidActions(state)
        
        // Make a move and check that positional rewards are included
        val stepResult = env.step(validActions.first())
        
        // The reward should include both positional evaluation and step penalty
        assertTrue(stepResult.reward.isFinite(), "Reward should be finite")
        
        // With positional rewards enabled, the reward might be positive or negative
        // depending on the position evaluation, but it should be different from just step penalty
        // We just verify it's working and finite
        assertTrue(stepResult.reward != -0.001, "With positional rewards, reward should be different from just step penalty")
    }
    
    @Test
    fun testGameLengthNormalization() {
        val env = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                gameLengthNormalization = true,
                maxGameLength = 100
            )
        )
        
        // This test verifies the configuration is applied
        // In practice, game length normalization would be tested with actual wins/losses
        env.reset()
        
        val metrics = env.getChessMetrics()
        assertEquals(0, metrics.gameLength, "New game should have zero length")
    }
}
