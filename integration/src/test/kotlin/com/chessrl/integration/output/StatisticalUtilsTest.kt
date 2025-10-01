package com.chessrl.integration.output

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class StatisticalUtilsTest {
    
    @Test
    fun `calculateWinRateConfidenceInterval returns null for zero games`() {
        val result = StatisticalUtils.calculateWinRateConfidenceInterval(0, 0)
        assertNull(result)
    }
    
    @Test
    fun `calculateWinRateConfidenceInterval returns valid interval for normal case`() {
        val result = StatisticalUtils.calculateWinRateConfidenceInterval(50, 100)
        assertNotNull(result)
        assertTrue(result.start >= 0.0)
        assertTrue(result.endInclusive <= 1.0)
        assertTrue(result.start <= result.endInclusive)
        
        // For 50/100 wins, confidence interval should be around 0.5
        assertTrue(result.start < 0.5)
        assertTrue(result.endInclusive > 0.5)
    }
    
    @Test
    fun `calculateWinRateConfidenceInterval handles edge cases`() {
        // All wins
        val allWins = StatisticalUtils.calculateWinRateConfidenceInterval(100, 100)
        assertNotNull(allWins)
        assertTrue(allWins.endInclusive <= 1.0)
        
        // No wins
        val noWins = StatisticalUtils.calculateWinRateConfidenceInterval(0, 100)
        assertNotNull(noWins)
        assertTrue(noWins.start >= 0.0)
    }
    
    @Test
    fun `testStatisticalSignificance returns false for zero games`() {
        val result = StatisticalUtils.testStatisticalSignificance(0, 0)
        assertFalse(result)
    }
    
    @Test
    fun `testStatisticalSignificance detects significant deviation`() {
        // 90% win rate over 100 games should be significant
        val result = StatisticalUtils.testStatisticalSignificance(90, 100)
        assertTrue(result)
    }
    
    @Test
    fun `testStatisticalSignificance does not detect insignificant deviation`() {
        // 52% win rate over 100 games should not be significant
        val result = StatisticalUtils.testStatisticalSignificance(52, 100)
        assertFalse(result)
    }
    
    @Test
    fun `calculateEffectSize returns correct values`() {
        // Same win rates should have zero effect size
        val sameRates = StatisticalUtils.calculateEffectSize(0.5, 0.5)
        assertEquals(0.0, sameRates, 0.001)
        
        // Different win rates should have positive effect size
        val differentRates = StatisticalUtils.calculateEffectSize(0.7, 0.3)
        assertTrue(differentRates > 0.0)
    }
    
    @Test
    fun `interpretEffectSize returns correct interpretations`() {
        assertEquals("negligible", StatisticalUtils.interpretEffectSize(0.1))
        assertEquals("small", StatisticalUtils.interpretEffectSize(0.3))
        assertEquals("medium", StatisticalUtils.interpretEffectSize(0.6))
        assertEquals("large", StatisticalUtils.interpretEffectSize(1.0))
    }
    
    @Test
    fun `calculateStandardError returns correct values`() {
        val se = StatisticalUtils.calculateStandardError(0.5, 100)
        assertTrue(se > 0.0)
        assertTrue(se < 1.0)
        
        // Standard error should be 0 for zero games
        val seZero = StatisticalUtils.calculateStandardError(0.5, 0)
        assertEquals(0.0, seZero)
    }
    
    @Test
    fun `formatConfidenceInterval formats correctly`() {
        val interval = 0.4..0.6
        val formatted = StatisticalUtils.formatConfidenceInterval(interval, 2)
        assertEquals("[0.40, 0.60]", formatted)
    }
}