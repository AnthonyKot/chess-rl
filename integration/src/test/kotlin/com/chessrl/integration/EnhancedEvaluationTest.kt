package com.chessrl.integration

import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.output.ColorAlternationStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EnhancedEvaluationTest {
    
    @Test
    fun `ColorAlternationStats calculates rates correctly`() {
        val stats = ColorAlternationStats(
            whiteGames = 10,
            blackGames = 10,
            whiteWins = 6,
            blackWins = 4,
            whiteDraws = 2,
            blackDraws = 3,
            whiteLosses = 2,
            blackLosses = 3
        )
        
        assertEquals(0.6, stats.whiteWinRate, 0.001)
        assertEquals(0.4, stats.blackWinRate, 0.001)
        assertEquals(0.2, stats.whiteDrawRate, 0.001)
        assertEquals(0.3, stats.blackDrawRate, 0.001)
    }
    
    @Test
    fun `ColorAlternationStats handles zero games`() {
        val stats = ColorAlternationStats(
            whiteGames = 0,
            blackGames = 10,
            whiteWins = 0,
            blackWins = 5,
            whiteDraws = 0,
            blackDraws = 2,
            whiteLosses = 0,
            blackLosses = 3
        )
        
        assertEquals(0.0, stats.whiteWinRate)
        assertEquals(0.5, stats.blackWinRate, 0.001)
    }
    
    @Test
    fun `EnhancedEvaluationResults validates input correctly`() {
        val colorStats = ColorAlternationStats(
            whiteGames = 5,
            blackGames = 5,
            whiteWins = 3,
            blackWins = 2,
            whiteDraws = 1,
            blackDraws = 2,
            whiteLosses = 1,
            blackLosses = 1
        )
        
        val results = com.chessrl.integration.output.EnhancedEvaluationResults(
            totalGames = 10,
            wins = 5,
            draws = 3,
            losses = 2,
            winRate = 0.5,
            drawRate = 0.3,
            lossRate = 0.2,
            averageGameLength = 45.5,
            confidenceInterval = 0.3..0.7,
            statisticalSignificance = true,
            colorAlternation = colorStats,
            opponentType = "Test"
        )
        
        assertEquals(10, results.totalGames)
        assertEquals(5, results.wins)
        assertEquals(0.5, results.winRate)
        assertEquals("Test", results.opponentType)
        assertNotNull(results.confidenceInterval)
        assertTrue(results.statisticalSignificance)
    }
    
    @Test
    fun `BaselineEvaluator can be created with config`() {
        val config = ChessRLConfig(
            maxStepsPerGame = 200,
            winReward = 1.0,
            lossReward = -1.0,
            drawReward = 0.0,
            stepLimitPenalty = -0.5
        )
        
        val evaluator = BaselineEvaluator(config)
        assertNotNull(evaluator)
    }
}