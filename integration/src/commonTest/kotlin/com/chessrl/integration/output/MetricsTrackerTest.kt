package com.chessrl.integration.output

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class MetricsTrackerTest {
    
    @Test
    fun `creates MetricsTracker with default window size`() {
        val tracker = MetricsTracker()
        assertEquals(0, tracker.getHistorySize())
        assertNull(tracker.getCurrentBestPerformance())
    }
    
    @Test
    fun `creates MetricsTracker with custom window size`() {
        val tracker = MetricsTracker(trendWindowSize = 5)
        assertEquals(0, tracker.getHistorySize())
    }
    
    @Test
    fun `updateMetrics adds data and tracks best performance`() {
        val tracker = MetricsTracker(trendWindowSize = 3)
        
        val cycleData1 = createCycleData(1, averageReward = 0.5)
        val cycleData2 = createCycleData(2, averageReward = 0.7)
        val cycleData3 = createCycleData(3, averageReward = 0.6)
        
        tracker.updateMetrics(cycleData1)
        assertEquals(1, tracker.getHistorySize())
        assertEquals(0.5, tracker.getCurrentBestPerformance())
        
        tracker.updateMetrics(cycleData2)
        assertEquals(2, tracker.getHistorySize())
        assertEquals(0.7, tracker.getCurrentBestPerformance())
        
        tracker.updateMetrics(cycleData3)
        assertEquals(3, tracker.getHistorySize())
        assertEquals(0.7, tracker.getCurrentBestPerformance()) // Still best
    }
    
    @Test
    fun `getRewardTrend returns stable for insufficient data`() {
        val tracker = MetricsTracker(trendWindowSize = 5)
        
        // Add less than window size
        repeat(3) { i ->
            tracker.updateMetrics(createCycleData(i + 1, averageReward = 0.5))
        }
        
        val trend = tracker.getRewardTrend()
        assertEquals(TrendDirection.STABLE, trend.direction)
        assertEquals(0.0, trend.magnitude)
        assertEquals(0.0, trend.confidence)
    }
    
    @Test
    fun `getRewardTrend detects upward trend`() {
        val tracker = MetricsTracker(trendWindowSize = 3)
        
        // Add data with clear upward trend
        tracker.updateMetrics(createCycleData(1, averageReward = 0.3))
        tracker.updateMetrics(createCycleData(2, averageReward = 0.4))
        tracker.updateMetrics(createCycleData(3, averageReward = 0.5))
        tracker.updateMetrics(createCycleData(4, averageReward = 0.6))
        tracker.updateMetrics(createCycleData(5, averageReward = 0.7))
        tracker.updateMetrics(createCycleData(6, averageReward = 0.8))
        
        val trend = tracker.getRewardTrend()
        assertEquals(TrendDirection.UP, trend.direction)
        assertTrue(trend.magnitude > 0)
        assertTrue(trend.confidence > 0)
    }
    
    @Test
    fun `getRewardTrend detects downward trend`() {
        val tracker = MetricsTracker(trendWindowSize = 3)
        
        // Add data with clear downward trend
        tracker.updateMetrics(createCycleData(1, averageReward = 0.8))
        tracker.updateMetrics(createCycleData(2, averageReward = 0.7))
        tracker.updateMetrics(createCycleData(3, averageReward = 0.6))
        tracker.updateMetrics(createCycleData(4, averageReward = 0.5))
        tracker.updateMetrics(createCycleData(5, averageReward = 0.4))
        tracker.updateMetrics(createCycleData(6, averageReward = 0.3))
        
        val trend = tracker.getRewardTrend()
        assertEquals(TrendDirection.DOWN, trend.direction)
        assertTrue(trend.magnitude > 0)
        assertTrue(trend.confidence > 0)
    }
    
    @Test
    fun `getWinRateTrend calculates correctly`() {
        val tracker = MetricsTracker(trendWindowSize = 2)
        
        // Add data with improving win rate
        tracker.updateMetrics(createCycleData(1, winDrawLoss = Triple(3, 2, 5))) // 30% win rate
        tracker.updateMetrics(createCycleData(2, winDrawLoss = Triple(4, 2, 4))) // 40% win rate
        tracker.updateMetrics(createCycleData(3, winDrawLoss = Triple(6, 2, 2))) // 60% win rate
        tracker.updateMetrics(createCycleData(4, winDrawLoss = Triple(7, 2, 1))) // 70% win rate
        
        val trend = tracker.getWinRateTrend()
        assertEquals(TrendDirection.UP, trend.direction)
        assertTrue(trend.magnitude > 0)
    }
    
    @Test
    fun `getLossTrend inverts direction correctly`() {
        val tracker = MetricsTracker(trendWindowSize = 2)
        
        // Add data with decreasing loss (which is good, so should be UP trend)
        tracker.updateMetrics(createCycleData(1, averageLoss = 1.0))
        tracker.updateMetrics(createCycleData(2, averageLoss = 0.9))
        tracker.updateMetrics(createCycleData(3, averageLoss = 0.7))
        tracker.updateMetrics(createCycleData(4, averageLoss = 0.6))
        
        val trend = tracker.getLossTrend()
        assertEquals(TrendDirection.UP, trend.direction) // UP because loss is decreasing
        assertTrue(trend.magnitude > 0)
    }
    
    @Test
    fun `getETA returns null for insufficient data`() {
        val tracker = MetricsTracker()
        
        tracker.updateMetrics(createCycleData(1))
        tracker.updateMetrics(createCycleData(2))
        
        val eta = tracker.getETA(10)
        assertNull(eta)
    }
    
    @Test
    fun `getETA calculates correctly with sufficient data`() {
        val tracker = MetricsTracker()
        
        // Add cycles with 2-second duration each
        repeat(5) { i ->
            tracker.updateMetrics(createCycleData(i + 1, cycleDuration = 2.seconds))
        }
        
        val eta = tracker.getETA(10)
        assertNotNull(eta)
        assertEquals(20.seconds, eta) // 10 cycles * 2 seconds each
    }
    
    @Test
    fun `getBestPerformanceDelta returns null initially`() {
        val tracker = MetricsTracker()
        
        tracker.updateMetrics(createCycleData(1, averageReward = 0.5))
        
        val delta = tracker.getBestPerformanceDelta()
        assertNull(delta) // No previous best to compare to
    }
    
    @Test
    fun `getBestPerformanceDelta calculates correctly`() {
        val tracker = MetricsTracker()
        
        tracker.updateMetrics(createCycleData(1, averageReward = 0.5))
        tracker.updateMetrics(createCycleData(2, averageReward = 0.4)) // Not better
        tracker.updateMetrics(createCycleData(3, averageReward = 0.7)) // New best
        
        val delta = tracker.getBestPerformanceDelta()
        assertNotNull(delta)
        assertEquals(0.2, delta, 0.001) // 0.7 - 0.5 = 0.2
    }
    
    @Test
    fun `getMovingAverages calculates correctly`() {
        val tracker = MetricsTracker(trendWindowSize = 3)
        
        tracker.updateMetrics(createCycleData(1, averageReward = 0.4, winDrawLoss = Triple(4, 2, 4)))
        tracker.updateMetrics(createCycleData(2, averageReward = 0.6, winDrawLoss = Triple(6, 2, 2)))
        tracker.updateMetrics(createCycleData(3, averageReward = 0.8, winDrawLoss = Triple(8, 1, 1)))
        
        val averages = tracker.getMovingAverages()
        assertEquals(0.6, averages.reward, 0.001) // (0.4 + 0.6 + 0.8) / 3
        assertEquals(0.6, averages.winRate, 0.001) // Average of 40%, 60%, 80%
    }
    
    @Test
    fun `getTrendData returns comprehensive trend information`() {
        val tracker = MetricsTracker(trendWindowSize = 2)
        
        // Add enough data for trends
        repeat(4) { i ->
            tracker.updateMetrics(createCycleData(i + 1, averageReward = 0.5 + i * 0.1))
        }
        
        val trendData = tracker.getTrendData()
        assertNotNull(trendData.rewardTrend)
        assertNotNull(trendData.winRateTrend)
        assertNotNull(trendData.lossTrend)
        assertNotNull(trendData.movingAverages)
        // bestPerformanceDelta and eta may be null depending on data
    }
    
    @Test
    fun `clear resets all data`() {
        val tracker = MetricsTracker()
        
        tracker.updateMetrics(createCycleData(1, averageReward = 0.5))
        tracker.updateMetrics(createCycleData(2, averageReward = 0.7))
        
        assertEquals(2, tracker.getHistorySize())
        assertEquals(0.7, tracker.getCurrentBestPerformance())
        
        tracker.clear()
        
        assertEquals(0, tracker.getHistorySize())
        assertNull(tracker.getCurrentBestPerformance())
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
        cycleDuration: kotlin.time.Duration = 1.seconds
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
            cycleDuration = cycleDuration
        )
    }
}