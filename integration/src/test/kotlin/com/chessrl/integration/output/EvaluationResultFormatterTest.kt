package com.chessrl.integration.output

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertContains

class EvaluationResultFormatterTest {
    
    private val formatter = EvaluationResultFormatter()
    
    @Test
    fun `formatEvaluationResult includes all key information`() {
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
        
        val result = EnhancedEvaluationResults(
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
            opponentType = "Test Opponent"
        )
        
        val formatted = formatter.formatEvaluationResult(result)
        
        // Check that all key information is present
        assertContains(formatted, "Test Opponent")
        assertContains(formatted, "10") // total games
        assertContains(formatted, "50.0%") // win rate
        assertContains(formatted, "30.0%") // draw rate
        assertContains(formatted, "20.0%") // loss rate
        assertContains(formatted, "45.5") // average game length
        assertContains(formatted, "confidence interval")
        assertContains(formatted, "statistically significant")
        assertContains(formatted, "As White")
        assertContains(formatted, "As Black")
        assertContains(formatted, "Competitive performance")
    }
    
    @Test
    fun `formatComparisonResult includes statistical analysis`() {
        val colorStats = ColorAlternationStats(
            whiteGames = 5,
            blackGames = 5,
            whiteWins = 4,
            blackWins = 1,
            whiteDraws = 1,
            blackDraws = 2,
            whiteLosses = 0,
            blackLosses = 2
        )
        
        val result = EnhancedComparisonResults(
            totalGames = 10,
            modelAWins = 5,
            draws = 3,
            modelBWins = 2,
            modelAWinRate = 0.5,
            drawRate = 0.3,
            modelBWinRate = 0.2,
            averageGameLength = 42.0,
            confidenceInterval = 0.25..0.75,
            statisticalSignificance = false,
            pValue = 0.42,
            effectSize = 0.3,
            colorAlternation = colorStats
        )
        
        val formatted = formatter.formatComparisonResult(result, "ModelA", "ModelB")
        
        assertContains(formatted, "MODEL COMPARISON")
        assertContains(formatted, "ModelA")
        assertContains(formatted, "ModelB")
        assertContains(formatted, "Effect size")
        assertContains(formatted, "small") // effect size interpretation
        assertContains(formatted, "not statistically significant")
        assertContains(formatted, "Model A Color Performance")
    }
    
    @Test
    fun `formatMultipleEvaluations creates summary table`() {
        val colorStats1 = ColorAlternationStats(5, 5, 3, 2, 1, 2, 1, 1)
        val colorStats2 = ColorAlternationStats(5, 5, 2, 3, 2, 1, 1, 1)
        
        val result1 = EnhancedEvaluationResults(
            totalGames = 10, wins = 5, draws = 3, losses = 2,
            winRate = 0.5, drawRate = 0.3, lossRate = 0.2,
            averageGameLength = 45.0, colorAlternation = colorStats1,
            opponentType = "Heuristic"
        )
        
        val result2 = EnhancedEvaluationResults(
            totalGames = 10, wins = 5, draws = 2, losses = 3,
            winRate = 0.5, drawRate = 0.2, lossRate = 0.3,
            averageGameLength = 50.0, colorAlternation = colorStats2,
            opponentType = "Minimax-2"
        )
        
        val results = listOf("Heuristic" to result1, "Minimax-2" to result2)
        val formatted = formatter.formatMultipleEvaluations(results)
        
        assertContains(formatted, "MULTI-OPPONENT EVALUATION SUMMARY")
        assertContains(formatted, "Heuristic")
        assertContains(formatted, "Minimax-2")
        assertContains(formatted, "Overall Performance")
        assertContains(formatted, "Best vs")
        assertContains(formatted, "Worst vs")
    }
    
    @Test
    fun `formatConciseSummary creates one-line summary`() {
        val colorStats = ColorAlternationStats(5, 5, 3, 2, 1, 2, 1, 1)
        
        val result = EnhancedEvaluationResults(
            totalGames = 10, wins = 6, draws = 2, losses = 2,
            winRate = 0.6, drawRate = 0.2, lossRate = 0.2,
            averageGameLength = 40.0, 
            confidenceInterval = 0.4..0.8,
            statisticalSignificance = true,
            colorAlternation = colorStats,
            opponentType = "Test"
        )
        
        val summary = formatter.formatConciseSummary(result)
        
        assertContains(summary, "Test: 6/10")
        assertContains(summary, "60.0%")
        assertContains(summary, "[0.40, 0.80]")
        assertContains(summary, "(sig)")
    }
    
    @Test
    fun `handles empty results gracefully`() {
        val emptyResults = emptyList<Pair<String, EnhancedEvaluationResults>>()
        val formatted = formatter.formatMultipleEvaluations(emptyResults)
        
        assertContains(formatted, "No evaluation results")
    }
}
