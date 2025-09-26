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
     * Convenience wrapper: compute two-tailed binomial test p-value and compare to threshold.
     */
    fun testStatisticalSignificance(
        wins: Int,
        totalGames: Int,
        expectedWinRate: Double = 0.5,
        significanceLevel: Double = 0.05
    ): Boolean {
        if (totalGames <= 0) return false
        val pValue = binomialTwoTailedPValue(wins, totalGames, expectedWinRate)
        return pValue < significanceLevel
    }

    /**
     * Exact two-tailed binomial p-value for observing exactly k successes out of n trials
     * with success probability p0 under the null hypothesis.
     * Uses cumulative probability of values with probability less than or equal to the observed outcome.
     */
    fun binomialTwoTailedPValue(k: Int, n: Int, p0: Double = 0.5): Double {
        if (n <= 0) return 1.0
        if (k < 0 || k > n) return 1.0

        val pmf = DoubleArray(n + 1)
        for (i in 0..n) {
            pmf[i] = binomialProbability(i, n, p0)
        }

        val observedProb = pmf[k]
        var cumulative = 0.0
        for (i in 0..n) {
            if (pmf[i] <= observedProb + 1e-15) {
                cumulative += pmf[i]
            }
        }
        return min(1.0, cumulative)
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

    private fun binomialProbability(k: Int, n: Int, p: Double): Double {
        if (p <= 0.0) return if (k == 0) 1.0 else 0.0
        if (p >= 1.0) return if (k == n) 1.0 else 0.0

        val logCoeff = logCombination(n, k)
        val logProb = k * ln(p) + (n - k) * ln(1 - p)
        return exp(logCoeff + logProb)
    }

    private fun logCombination(n: Int, k: Int): Double {
        if (k < 0 || k > n) return Double.NEGATIVE_INFINITY
        if (k == 0 || k == n) return 0.0

        val kEff = min(k, n - k)
        var result = 0.0
        for (i in 1..kEff) {
            result += ln((n - kEff + i).toDouble()) - ln(i.toDouble())
        }
        return result
    }
}
