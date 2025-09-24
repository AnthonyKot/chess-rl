package com.chessrl.integration.output

import kotlin.math.*

private fun formatWithPlaces(value: Double, decimalPlaces: Int): String {
    val formatter = FormatManager(decimalPlaces)
    return formatter.formatNumber(value, decimalPlaces)
}

/**
 * Statistical utilities for evaluation result analysis
 */
object StatisticalUtils {
    
    /**
     * Calculate confidence interval for win rate using Wilson score interval
     * This is more accurate than normal approximation for small sample sizes
     */
    fun calculateWinRateConfidenceInterval(
        wins: Int,
        totalGames: Int,
        confidenceLevel: Double = 0.95
    ): ClosedFloatingPointRange<Double>? {
        if (totalGames <= 0) return null
        
        val p = wins.toDouble() / totalGames
        val n = totalGames.toDouble()
        
        // Z-score for confidence level (1.96 for 95%, 2.576 for 99%)
        val z = when {
            confidenceLevel >= 0.99 -> 2.576
            confidenceLevel >= 0.95 -> 1.96
            confidenceLevel >= 0.90 -> 1.645
            else -> 1.96 // Default to 95%
        }
        
        // Wilson score interval
        val denominator = 1 + z * z / n
        val center = (p + z * z / (2 * n)) / denominator
        val margin = z * sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / denominator
        
        val lower = maxOf(0.0, center - margin)
        val upper = minOf(1.0, center + margin)
        
        return lower..upper
    }
    
    /**
     * Test statistical significance of win rate difference from expected value
     * Uses binomial test to determine if observed win rate is significantly different from 0.5
     */
    fun testStatisticalSignificance(
        wins: Int,
        totalGames: Int,
        expectedWinRate: Double = 0.5,
        significanceLevel: Double = 0.05
    ): Boolean {
        if (totalGames <= 0) return false
        
        val observedWinRate = wins.toDouble() / totalGames
        val n = totalGames.toDouble()
        val p = expectedWinRate
        
        // Use normal approximation to binomial for large samples
        if (n * p >= 5 && n * (1 - p) >= 5) {
            val standardError = sqrt(p * (1 - p) / n)
            val zScore = abs(observedWinRate - p) / standardError
            
            // Two-tailed test
            val criticalValue = when {
                significanceLevel <= 0.01 -> 2.576
                significanceLevel <= 0.05 -> 1.96
                else -> 1.645
            }
            
            return zScore > criticalValue
        }
        
        // For small samples, use exact binomial test (simplified)
        // This is a conservative approximation
        return abs(observedWinRate - p) > 2 * sqrt(p * (1 - p) / n)
    }
    
    /**
     * Calculate effect size (Cohen's h) for comparing two win rates
     */
    fun calculateEffectSize(winRate1: Double, winRate2: Double): Double {
        val phi1 = 2 * asin(sqrt(winRate1))
        val phi2 = 2 * asin(sqrt(winRate2))
        return abs(phi1 - phi2)
    }
    
    /**
     * Interpret effect size magnitude
     */
    fun interpretEffectSize(effectSize: Double): String {
        return when {
            effectSize < 0.2 -> "negligible"
            effectSize < 0.5 -> "small"
            effectSize < 0.8 -> "medium"
            else -> "large"
        }
    }
    
    /**
     * Calculate standard error for win rate
     */
    fun calculateStandardError(winRate: Double, totalGames: Int): Double {
        if (totalGames <= 0) return 0.0
        return sqrt(winRate * (1 - winRate) / totalGames)
    }
    
    /**
     * Format confidence interval as string
     */
    fun formatConfidenceInterval(
        interval: ClosedFloatingPointRange<Double>,
        decimalPlaces: Int = 3
    ): String {
        val lower = formatWithPlaces(interval.start, decimalPlaces)
        val upper = formatWithPlaces(interval.endInclusive, decimalPlaces)
        return "[$lower, $upper]"
    }
}
