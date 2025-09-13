package com.chessrl.integration

import kotlin.math.*

/**
 * Sophisticated convergence detection system for chess RL training
 * with multiple convergence criteria and confidence estimation.
 */
class ConvergenceDetector(
    private val config: ConvergenceConfig = ConvergenceConfig()
) {
    
    private val performanceHistory = mutableListOf<Double>()
    private val convergenceHistory = mutableListOf<ConvergenceStatus>()
    private var lastConvergenceCheck = 0L
    
    /**
     * Check convergence status based on performance history
     */
    fun checkConvergence(newPerformanceHistory: List<Double>): ConvergenceStatus {
        // Update internal history
        performanceHistory.clear()
        performanceHistory.addAll(newPerformanceHistory)
        
        if (performanceHistory.size < config.windowSize) {
            return ConvergenceStatus(
                hasConverged = false,
                confidence = 0.0,
                stability = 0.0,
                trend = 0.0,
                status = "Insufficient data",
                cyclesUntilConvergence = -1,
                recommendations = listOf("Continue training to gather more performance data")
            )
        }
        
        val recentPerformance = performanceHistory.takeLast(config.windowSize)
        
        // Calculate convergence metrics
        val stability = calculateStability(recentPerformance)
        val trend = calculateTrend(recentPerformance)
        val variance = calculateVariance(recentPerformance)
        val improvementRate = calculateImprovementRate(recentPerformance)
        
        // Determine convergence status
        val hasConverged = determineConvergence(stability, trend, variance, improvementRate)
        val confidence = calculateConfidence(stability, trend, variance)
        val status = generateStatusMessage(hasConverged, stability, trend, improvementRate)
        val cyclesUntilConvergence = estimateCyclesUntilConvergence(trend, improvementRate)
        val recommendations = generateRecommendations(hasConverged, stability, trend, improvementRate)
        
        val convergenceStatus = ConvergenceStatus(
            hasConverged = hasConverged,
            confidence = confidence,
            stability = stability,
            trend = trend,
            status = status,
            cyclesUntilConvergence = cyclesUntilConvergence,
            recommendations = recommendations,
            variance = variance,
            improvementRate = improvementRate
        )
        
        convergenceHistory.add(convergenceStatus)
        lastConvergenceCheck = getCurrentTimeMillis()
        
        return convergenceStatus
    }
    
    /**
     * Get final convergence status summary
     */
    fun getFinalStatus(): ConvergenceStatus {
        return convergenceHistory.lastOrNull() ?: ConvergenceStatus(
            hasConverged = false,
            confidence = 0.0,
            stability = 0.0,
            trend = 0.0,
            status = "No convergence analysis performed",
            cyclesUntilConvergence = -1,
            recommendations = emptyList()
        )
    }
    
    /**
     * Get convergence history
     */
    fun getConvergenceHistory(): List<ConvergenceStatus> = convergenceHistory.toList()
    
    /**
     * Reset convergence detector
     */
    fun reset() {
        performanceHistory.clear()
        convergenceHistory.clear()
        lastConvergenceCheck = 0L
    }
    
    // Private helper methods
    
    /**
     * Calculate stability score (higher = more stable)
     */
    private fun calculateStability(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        
        // Coefficient of variation (inverted for stability score)
        val cv = if (abs(mean) > 1e-8) stdDev / abs(mean) else Double.MAX_VALUE
        
        // Convert to stability score (0 to 1, higher is more stable)
        return 1.0 / (1.0 + cv)
    }
    
    /**
     * Calculate trend (positive = improving, negative = declining)
     */
    private fun calculateTrend(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val n = values.size
        val x = (0 until n).map { it.toDouble() }
        val y = values
        
        val xMean = x.average()
        val yMean = y.average()
        
        val numerator = x.zip(y) { xi, yi -> (xi - xMean) * (yi - yMean) }.sum()
        val denominator = x.map { (it - xMean).pow(2) }.sum()
        
        return if (denominator != 0.0) numerator / denominator else 0.0
    }
    
    /**
     * Calculate variance of recent performance
     */
    private fun calculateVariance(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
    
    /**
     * Calculate improvement rate (change per cycle)
     */
    private fun calculateImprovementRate(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val firstHalf = values.take(values.size / 2)
        val secondHalf = values.drop(values.size / 2)
        
        val firstHalfAvg = firstHalf.average()
        val secondHalfAvg = secondHalf.average()
        
        return (secondHalfAvg - firstHalfAvg) / (values.size / 2.0)
    }
    
    /**
     * Determine if training has converged
     */
    private fun determineConvergence(
        stability: Double,
        trend: Double,
        variance: Double,
        improvementRate: Double
    ): Boolean {
        
        // Multiple convergence criteria
        val stabilityConverged = stability >= config.stabilityThreshold
        val trendConverged = abs(trend) <= config.trendThreshold
        val varianceConverged = variance <= config.varianceThreshold
        val improvementConverged = abs(improvementRate) <= config.improvementThreshold
        
        // Require multiple criteria to be met
        val criteriaMetCount = listOf(
            stabilityConverged, trendConverged, varianceConverged, improvementConverged
        ).count { it }
        
        return criteriaMetCount >= config.minCriteriaMet
    }
    
    /**
     * Calculate confidence in convergence assessment
     */
    private fun calculateConfidence(stability: Double, trend: Double, variance: Double): Double {
        // Confidence based on how clearly the criteria are met
        val stabilityConfidence = stability.coerceIn(0.0, 1.0)
        val trendConfidence = 1.0 - (abs(trend) / config.trendThreshold).coerceIn(0.0, 1.0)
        val varianceConfidence = 1.0 - (variance / config.varianceThreshold).coerceIn(0.0, 1.0)
        
        return (stabilityConfidence + trendConfidence + varianceConfidence) / 3.0
    }
    
    /**
     * Generate status message
     */
    private fun generateStatusMessage(
        hasConverged: Boolean,
        stability: Double,
        trend: Double,
        improvementRate: Double
    ): String {
        return when {
            hasConverged -> {
                when {
                    stability > 0.9 -> "Strongly converged - very stable performance"
                    stability > 0.8 -> "Converged - stable performance"
                    else -> "Converged - moderately stable performance"
                }
            }
            trend > config.trendThreshold -> "Still improving - performance trending upward"
            trend < -config.trendThreshold -> "Performance declining - may need intervention"
            stability < 0.5 -> "Unstable performance - high variance detected"
            improvementRate > config.improvementThreshold -> "Slow but steady improvement"
            else -> "Training in progress - monitoring convergence"
        }
    }
    
    /**
     * Estimate cycles until convergence
     */
    private fun estimateCyclesUntilConvergence(trend: Double, improvementRate: Double): Int {
        if (trend <= 0 || improvementRate <= 0) {
            return -1 // Cannot estimate
        }
        
        // Simple linear extrapolation
        val currentPerformance = performanceHistory.lastOrNull() ?: 0.0
        val targetPerformance = currentPerformance + config.convergenceTarget
        val cyclesNeeded = (targetPerformance - currentPerformance) / improvementRate
        
        return cyclesNeeded.toInt().coerceIn(1, 1000)
    }
    
    /**
     * Generate recommendations based on convergence analysis
     */
    private fun generateRecommendations(
        hasConverged: Boolean,
        stability: Double,
        trend: Double,
        improvementRate: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        when {
            hasConverged -> {
                recommendations.add("Training has converged - consider stopping or fine-tuning")
                if (stability < 0.8) {
                    recommendations.add("Consider reducing learning rate for better stability")
                }
            }
            trend > config.trendThreshold * 2 -> {
                recommendations.add("Strong improvement trend - continue current settings")
                recommendations.add("Monitor for potential overfitting")
            }
            trend < -config.trendThreshold -> {
                recommendations.add("Performance declining - consider reducing learning rate")
                recommendations.add("Check for training instabilities or data issues")
                recommendations.add("Consider model rollback to previous best version")
            }
            stability < 0.5 -> {
                recommendations.add("High performance variance detected")
                recommendations.add("Consider reducing learning rate or batch size")
                recommendations.add("Increase experience buffer size for more stable training")
            }
            improvementRate < config.improvementThreshold / 2 -> {
                recommendations.add("Very slow improvement - consider increasing learning rate")
                recommendations.add("Try different exploration strategy or network architecture")
            }
            else -> {
                recommendations.add("Continue training with current settings")
                recommendations.add("Monitor performance for convergence signs")
            }
        }
        
        // Add general recommendations
        if (performanceHistory.size < config.windowSize * 2) {
            recommendations.add("Gather more training data for better convergence assessment")
        }
        
        return recommendations
    }
}

/**
 * Configuration for convergence detection
 */
data class ConvergenceConfig(
    val windowSize: Int = 20,
    val stabilityThreshold: Double = 0.8,
    val trendThreshold: Double = 0.001,
    val varianceThreshold: Double = 0.01,
    val improvementThreshold: Double = 0.001,
    val minCriteriaMet: Int = 3,
    val convergenceTarget: Double = 0.1
)

/**
 * Convergence status information
 */
data class ConvergenceStatus(
    val hasConverged: Boolean,
    val confidence: Double,
    val stability: Double,
    val trend: Double,
    val status: String,
    val cyclesUntilConvergence: Int,
    val recommendations: List<String>,
    val variance: Double = 0.0,
    val improvementRate: Double = 0.0
)