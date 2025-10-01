package com.chessrl.integration.output

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class TrendFormattingTest {
    
    private val formatManager = FormatManager()
    
    @Test
    fun `formatTrendIndicator shows symbol and magnitude for significant trends`() {
        val upTrend = TrendIndicator(TrendDirection.UP, 0.05, 0.8)
        val downTrend = TrendIndicator(TrendDirection.DOWN, 0.03, 0.7)
        val stableTrend = TrendIndicator(TrendDirection.STABLE, 0.001, 0.5)
        
        val upFormatted = formatManager.formatTrendIndicator(upTrend)
        val downFormatted = formatManager.formatTrendIndicator(downTrend)
        val stableFormatted = formatManager.formatTrendIndicator(stableTrend)
        
        assertTrue(upFormatted.contains("↑"))
        assertTrue(upFormatted.contains("0.05"))
        
        assertTrue(downFormatted.contains("↓"))
        assertTrue(downFormatted.contains("0.03"))
        
        assertEquals("→", stableFormatted) // Small magnitude, just symbol
    }
    
    @Test
    fun `formatTrendSymbol shows only symbol for inline display`() {
        val significantUpTrend = TrendIndicator(TrendDirection.UP, 0.05, 0.8)
        val insignificantTrend = TrendIndicator(TrendDirection.UP, 0.0005, 0.2)
        val stableTrend = TrendIndicator(TrendDirection.STABLE, 0.01, 0.9)
        
        assertEquals("↑", formatManager.formatTrendSymbol(significantUpTrend))
        assertEquals("", formatManager.formatTrendSymbol(insignificantTrend)) // Too small/low confidence
        assertEquals("", formatManager.formatTrendSymbol(stableTrend)) // Stable trends don't show symbol
    }
    
    @Test
    fun `formatCycleSummaryWithTrends includes trend indicators`() {
        val cycleData = createCycleData(5, averageReward = 0.75)
        val trendData = TrendData(
            rewardTrend = TrendIndicator(TrendDirection.UP, 0.05, 0.8),
            winRateTrend = TrendIndicator(TrendDirection.UP, 0.1, 0.7),
            lossTrend = TrendIndicator(TrendDirection.UP, 0.02, 0.6), // UP for loss trend means loss is decreasing
            bestPerformanceDelta = 0.03,
            eta = null,
            movingAverages = MovingAverages(0.7, 0.6, 0.5, 1.2, 2.seconds)
        )
        
        val summary = formatManager.formatCycleSummaryWithTrends(cycleData, trendData, 30.seconds)
        
        // Check that trend symbols are included
        assertTrue(summary.contains("reward=0.75↑"))
        assertTrue(summary.contains("loss=0.80↑"))
        assertTrue(summary.contains("bestΔ=+0.03"))
        assertTrue(summary.contains("ETA=30.0s"))
    }
    
    @Test
    fun `formatCycleSummaryWithTrends handles null trend data gracefully`() {
        val cycleData = createCycleData(5, averageReward = 0.75)
        
        val summary = formatManager.formatCycleSummaryWithTrends(cycleData, null, null)
        
        // Should work without trends
        assertTrue(summary.contains("Cycle 5/100"))
        assertTrue(summary.contains("reward=0.75"))
        assertTrue(summary.contains("loss=0.80"))
        // Should not contain trend symbols or ETA
        assertTrue(!summary.contains("↑"))
        assertTrue(!summary.contains("ETA="))
        assertTrue(!summary.contains("bestΔ="))
    }
    
    @Test
    fun `formatCycleSummaryWithTrends shows checkpoint saved indicator`() {
        val checkpointEvent = CheckpointEvent(CheckpointType.BEST, "/path/to/checkpoint", 0.03, true)
        val cycleData = createCycleData(5, checkpointEvent = checkpointEvent)
        val trendData = TrendData(
            rewardTrend = TrendIndicator(TrendDirection.UP, 0.05, 0.8),
            winRateTrend = TrendIndicator(TrendDirection.UP, 0.1, 0.7),
            lossTrend = TrendIndicator(TrendDirection.UP, 0.02, 0.6),
            bestPerformanceDelta = 0.03,
            eta = null,
            movingAverages = MovingAverages(0.7, 0.6, 0.5, 1.2, 2.seconds)
        )
        
        val summary = formatManager.formatCycleSummaryWithTrends(cycleData, trendData, null)
        
        assertTrue(summary.contains("bestΔ=+0.03"))
        assertTrue(summary.contains("(saved)"))
    }
    
    private fun createCycleData(
        cycleNumber: Int,
        totalCycles: Int = 100,
        gamesPlayed: Int = 10,
        winDrawLoss: Triple<Int, Int, Int> = Triple(5, 2, 3),
        averageGameLength: Double = 25.0,
        averageReward: Double = 0.5,
        batchesProcessed: Int = 5,
        averageLoss: Double = 0.8,
        gradientNorm: Double = 1.2,
        cycleDuration: kotlin.time.Duration = 1.seconds,
        checkpointEvent: CheckpointEvent? = null
    ): CycleProgressData {
        return CycleProgressData(
            cycleNumber = cycleNumber,
            totalCycles = totalCycles,
            gamesPlayed = gamesPlayed,
            winDrawLoss = winDrawLoss,
            averageGameLength = averageGameLength,
            averageReward = averageReward,
            batchesProcessed = batchesProcessed,
            averageLoss = averageLoss,
            gradientNorm = gradientNorm,
            bufferUtilization = BufferStats.create(500, 1000),
            cycleDuration = cycleDuration,
            checkpointEvent = checkpointEvent
        )
    }
}