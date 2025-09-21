package com.chessrl.integration

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Test that step count tracking works correctly across different game scenarios.
 */
class StepCountTrackingTest {
    
    @Test
    fun testSelfPlayGameResultIncludesStepCount() {
        // Create a mock SelfPlayGameResult with step count
        val gameResult = SelfPlayGameResult(
            gameId = 1,
            gameLength = 42, // This should be the actual step count
            gameOutcome = GameOutcome.WHITE_WINS,
            terminationReason = EpisodeTerminationReason.GAME_ENDED,
            gameDuration = 5000L,
            experiences = emptyList(),
            chessMetrics = ChessMetrics(
                gameLength = 42,
                captureCount = 5,
                checkCount = 2,
                castlingCount = 2,
                promotionCount = 0,
                enPassantCount = 0
            ),
            finalPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        )
        
        // Verify that gameLength contains the step count
        assertEquals(42, gameResult.gameLength)
        assertEquals(42, gameResult.chessMetrics.gameLength)
    }
    
    @Test
    fun testAverageGameLengthCalculation() {
        // Create multiple game results with different step counts
        val gameResults = listOf(
            createMockGameResult(gameLength = 30),
            createMockGameResult(gameLength = 40),
            createMockGameResult(gameLength = 50)
        )
        
        // Calculate average game length
        val averageLength = gameResults.map { it.gameLength }.average()
        
        // Verify the calculation is correct
        assertEquals(40.0, averageLength)
    }
    
    @Test
    fun testStepCountTrackingWithDifferentTerminationReasons() {
        // Test game that ended naturally
        val naturalEnd = createMockGameResult(
            gameLength = 35,
            terminationReason = EpisodeTerminationReason.GAME_ENDED
        )
        
        // Test game that hit step limit
        val stepLimit = createMockGameResult(
            gameLength = 200, // Max steps
            terminationReason = EpisodeTerminationReason.STEP_LIMIT
        )
        
        // Both should have valid step counts
        assertTrue(naturalEnd.gameLength > 0)
        assertTrue(stepLimit.gameLength > 0)
        assertNotEquals(naturalEnd.gameLength, stepLimit.gameLength)
    }
    
    @Test
    fun testEvaluationGameResultIncludesStepCount() {
        // Test the new EvaluationGameResult structure
        val evaluationResult = EvaluationGameResult(
            outcome = GameOutcome.DRAW,
            gameLength = 75
        )
        
        // Verify step count is tracked
        assertEquals(75, evaluationResult.gameLength)
        assertTrue(evaluationResult.gameLength > 0)
    }
    
    @Test
    fun testMetricsValidationDetectsInvalidStepCounts() {
        // Create metrics with invalid step count (0.0 despite games played)
        val invalidMetrics = CycleMetrics(
            cycle = 1,
            timestamp = System.currentTimeMillis(),
            gamesPlayed = 5, // Games were played
            avgEpisodeLength = 0.0, // But average length is 0.0 - this is invalid
            minEpisodeLength = 0,
            maxEpisodeLength = 0,
            experienceBufferSize = 100,
            bufferUtilization = 0.5,
            winRate = 0.4,
            drawRate = 0.3,
            lossRate = 0.3,
            averageReward = 0.1,
            averageLoss = 0.05,
            averageGradientNorm = 0.02,
            averagePolicyEntropy = 1.5,
            batchCount = 10,
            qValueStats = QValueStats(0.1, -0.5, 0.7, 0.1),
            gamesPerSecond = 2.0,
            totalGamesPlayed = 5,
            totalExperiencesCollected = 200,
            terminationAnalysis = TerminationAnalysis(1.0, 0.0, 0.0)
        )
        
        // Create a MetricsCollector to test validation
        val config = createMockConfig()
        val metricsCollector = MetricsCollector(config)
        
        // Validate metrics - should detect the issue
        val issues = metricsCollector.validateMetrics(invalidMetrics)
        
        // Should detect the invalid average episode length
        assertTrue(issues.isNotEmpty())
        assertTrue(issues.any { it.contains("Average episode length is 0.0") })
    }
    
    private fun createMockGameResult(
        gameLength: Int,
        terminationReason: EpisodeTerminationReason = EpisodeTerminationReason.GAME_ENDED
    ): SelfPlayGameResult {
        return SelfPlayGameResult(
            gameId = 1,
            gameLength = gameLength,
            gameOutcome = GameOutcome.WHITE_WINS,
            terminationReason = terminationReason,
            gameDuration = 3000L,
            experiences = emptyList(),
            chessMetrics = ChessMetrics(
                gameLength = gameLength,
                captureCount = 3,
                checkCount = 1,
                castlingCount = 1,
                promotionCount = 0,
                enPassantCount = 0
            ),
            finalPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        )
    }
    
    private fun createMockConfig(): ChessRLConfig {
        return ChessRLConfig(
            maxExperienceBuffer = 1000,
            maxCycles = 10,
            gamesPerCycle = 5,
            batchSize = 32,
            evaluationGames = 10,
            maxStepsPerGame = 200,
            checkpointInterval = 5,
            logInterval = 1,
            winReward = 1.0,
            lossReward = -1.0,
            drawReward = 0.0,
            stepLimitPenalty = -0.1
        )
    }
}