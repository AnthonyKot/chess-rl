package com.chessrl.integration.output

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Tracks performance trends and calculates statistical indicators for training progress.
 * 
 * This class maintains a rolling window of performance metrics to calculate trends,
 * moving averages, and provide ETA estimates for training completion.
 */
class MetricsTracker(
    private val trendWindowSize: Int = 10
) {
    private val metricsHistory = mutableListOf<CycleProgressData>()
    private var bestPerformance: Double? = null
    private var previousBestPerformance: Double? = null
    
    init {
        require(trendWindowSize > 0) { "Trend window size must be positive" }
    }
    
    /**
     * Updates the metrics tracker with new cycle data
     */
    fun updateMetrics(cycleData: CycleProgressData) {
        metricsHistory.add(cycleData)
        
        // Maintain rolling window
        if (metricsHistory.size > trendWindowSize * 2) {
            metricsHistory.removeAt(0)
        }
        
        // Track best performance
        val currentPerformance = cycleData.averageReward
        if (bestPerformance == null || currentPerformance > bestPerformance!!) {
            previousBestPerformance = bestPerformance
            bestPerformance = currentPerformance
        }
    }
    
    /**
     * Calculates reward trend by comparing recent vs previous periods
     */
    fun getRewardTrend(): TrendIndicator {
        if (metricsHistory.size < trendWindowSize) {
            return TrendIndicator(TrendDirection.STABLE, 0.0, 0.0)
        }
        
        val recentRewards = metricsHistory.takeLast(trendWindowSize).map { it.averageReward }
        val previousRewards = if (metricsHistory.size >= trendWindowSize * 2) {
            metricsHistory.drop(metricsHistory.size - trendWindowSize * 2)
                .take(trendWindowSize)
                .map { it.averageReward }
        } else {
            // Use first half of available data
            val halfSize = metricsHistory.size / 2
            metricsHistory.take(halfSize).map { it.averageReward }
        }
        
        if (previousRewards.isEmpty()) {
            return TrendIndicator(TrendDirection.STABLE, 0.0, 0.0)
        }
        
        val recentAvg = recentRewards.average()
        val previousAvg = previousRewards.average()
        val delta = recentAvg - previousAvg
        
        return calculateTrendIndicator(delta, recentRewards, previousRewards)
    }
    
    /**
     * Calculates win rate trend by comparing recent vs previous periods
     */
    fun getWinRateTrend(): TrendIndicator {
        if (metricsHistory.size < trendWindowSize) {
            return TrendIndicator(TrendDirection.STABLE, 0.0, 0.0)
        }
        
        val recentWinRates = metricsHistory.takeLast(trendWindowSize).map { 
            val (wins, _, _) = it.winDrawLoss
            val totalGames = it.winDrawLoss.first + it.winDrawLoss.second + it.winDrawLoss.third
            if (totalGames > 0) wins.toDouble() / totalGames else 0.0
        }
        
        val previousWinRates = if (metricsHistory.size >= trendWindowSize * 2) {
            metricsHistory.drop(metricsHistory.size - trendWindowSize * 2)
                .take(trendWindowSize)
                .map { 
                    val (wins, _, _) = it.winDrawLoss
                    val totalGames = it.winDrawLoss.first + it.winDrawLoss.second + it.winDrawLoss.third
                    if (totalGames > 0) wins.toDouble() / totalGames else 0.0
                }
        } else {
            val halfSize = metricsHistory.size / 2
            metricsHistory.take(halfSize).map { 
                val (wins, _, _) = it.winDrawLoss
                val totalGames = it.winDrawLoss.first + it.winDrawLoss.second + it.winDrawLoss.third
                if (totalGames > 0) wins.toDouble() / totalGames else 0.0
            }
        }
        
        if (previousWinRates.isEmpty()) {
            return TrendIndicator(TrendDirection.STABLE, 0.0, 0.0)
        }
        
        val recentAvg = recentWinRates.average()
        val previousAvg = previousWinRates.average()
        val delta = recentAvg - previousAvg
        
        return calculateTrendIndicator(delta, recentWinRates, previousWinRates)
    }
    
    /**
     * Calculates loss trend by comparing recent vs previous periods
     */
    fun getLossTrend(): TrendIndicator {
        if (metricsHistory.size < trendWindowSize) {
            return TrendIndicator(TrendDirection.STABLE, 0.0, 0.0)
        }
        
        val recentLosses = metricsHistory.takeLast(trendWindowSize).map { it.averageLoss }
        val previousLosses = if (metricsHistory.size >= trendWindowSize * 2) {
            metricsHistory.drop(metricsHistory.size - trendWindowSize * 2)
                .take(trendWindowSize)
                .map { it.averageLoss }
        } else {
            val halfSize = metricsHistory.size / 2
            metricsHistory.take(halfSize).map { it.averageLoss }
        }
        
        if (previousLosses.isEmpty()) {
            return TrendIndicator(TrendDirection.STABLE, 0.0, 0.0)
        }
        
        val recentAvg = recentLosses.average()
        val previousAvg = previousLosses.average()
        val delta = recentAvg - previousAvg
        
        // For loss, lower is better, so invert the trend direction
        val invertedDelta = -delta
        return calculateTrendIndicator(invertedDelta, recentLosses, previousLosses)
    }
    
    /**
     * Estimates remaining training time based on moving average cycle duration
     */
    fun getETA(remainingCycles: Int): Duration? {
        if (metricsHistory.size < 3 || remainingCycles <= 0) {
            return null
        }
        
        // Use recent cycles for ETA calculation
        val recentCycles = metricsHistory.takeLast(minOf(trendWindowSize, metricsHistory.size))
        val avgCycleDuration = recentCycles.map { it.cycleDuration.inWholeMilliseconds }.average()
        
        return (avgCycleDuration * remainingCycles).toLong().toDuration()
    }
    
    /**
     * Gets the delta from the previous best performance, if available
     */
    fun getBestPerformanceDelta(): Double? {
        return if (bestPerformance != null && previousBestPerformance != null) {
            bestPerformance!! - previousBestPerformance!!
        } else null
    }
    
    /**
     * Calculates moving averages for smoothed metric display
     */
    fun getMovingAverages(): MovingAverages {
        if (metricsHistory.isEmpty()) {
            return MovingAverages(
                reward = 0.0,
                winRate = 0.0,
                loss = 0.0,
                gradientNorm = 0.0,
                cycleDuration = 0.seconds
            )
        }
        
        val windowSize = minOf(trendWindowSize, metricsHistory.size)
        val recentMetrics = metricsHistory.takeLast(windowSize)
        
        val avgReward = recentMetrics.map { it.averageReward }.average()
        val avgWinRate = recentMetrics.map { 
            val (wins, _, _) = it.winDrawLoss
            val totalGames = it.winDrawLoss.first + it.winDrawLoss.second + it.winDrawLoss.third
            if (totalGames > 0) wins.toDouble() / totalGames else 0.0
        }.average()
        val avgLoss = recentMetrics.map { it.averageLoss }.average()
        val avgGradientNorm = recentMetrics.map { it.gradientNorm }.average()
        val avgCycleDuration = recentMetrics.map { it.cycleDuration.inWholeMilliseconds }.average().toLong().toDuration()
        
        return MovingAverages(
            reward = avgReward,
            winRate = avgWinRate,
            loss = avgLoss,
            gradientNorm = avgGradientNorm,
            cycleDuration = avgCycleDuration
        )
    }
    
    /**
     * Gets comprehensive trend data for all metrics
     */
    fun getTrendData(): TrendData {
        return TrendData(
            rewardTrend = getRewardTrend(),
            winRateTrend = getWinRateTrend(),
            lossTrend = getLossTrend(),
            bestPerformanceDelta = getBestPerformanceDelta(),
            eta = null, // ETA requires remaining cycles count, set externally
            movingAverages = getMovingAverages()
        )
    }
    
    /**
     * Calculates trend indicator from delta and data sets
     */
    private fun calculateTrendIndicator(
        delta: Double, 
        recentData: List<Double>, 
        previousData: List<Double>
    ): TrendIndicator {
        val magnitude = abs(delta)
        
        // Calculate confidence based on variance and sample size
        val recentVariance = calculateVariance(recentData)
        val previousVariance = calculateVariance(previousData)
        val pooledVariance = (recentVariance + previousVariance) / 2.0
        
        // Simple confidence calculation based on magnitude vs variance
        val confidence = if (pooledVariance > 0) {
            minOf(1.0, magnitude / sqrt(pooledVariance))
        } else {
            if (magnitude > 0) 1.0 else 0.0
        }
        
        val direction = when {
            magnitude < 0.001 -> TrendDirection.STABLE // Very small changes are stable
            delta > 0 -> TrendDirection.UP
            else -> TrendDirection.DOWN
        }
        
        return TrendIndicator(direction, magnitude, confidence)
    }
    
    /**
     * Calculates variance of a data set
     */
    private fun calculateVariance(data: List<Double>): Double {
        if (data.size < 2) return 0.0
        
        val mean = data.average()
        val sumSquaredDiffs = data.sumOf { (it - mean) * (it - mean) }
        return sumSquaredDiffs / (data.size - 1)
    }
    
    /**
     * Clears all stored metrics history
     */
    fun clear() {
        metricsHistory.clear()
        bestPerformance = null
        previousBestPerformance = null
    }
    
    /**
     * Gets the current size of the metrics history
     */
    fun getHistorySize(): Int = metricsHistory.size
    
    /**
     * Gets the current best performance value
     */
    fun getCurrentBestPerformance(): Double? = bestPerformance
}