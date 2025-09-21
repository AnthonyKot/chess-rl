package com.chessrl.integration.output

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Basic tests for OutputManager functionality
 */
class OutputManagerTest {
    
    @Test
    fun testOutputManagerCreation() {
        val config = OutputConfig()
        val outputManager = OutputManager(config)
        
        assertNotNull(outputManager)
        assertEquals(config, outputManager.getConfig())
    }
    
    @Test
    fun testCycleProgressDataCreation() {
        val bufferStats = BufferStats.create(500, 1000)
        val cycleData = CycleProgressData(
            cycleNumber = 1,
            totalCycles = 10,
            gamesPlayed = 20,
            winDrawLoss = Triple(8, 4, 8),
            averageGameLength = 45.5,
            averageReward = 0.125,
            batchesProcessed = 5,
            averageLoss = 0.234,
            gradientNorm = 1.456,
            bufferUtilization = bufferStats,
            cycleDuration = 30.seconds
        )
        
        assertEquals(1, cycleData.cycleNumber)
        assertEquals(10, cycleData.totalCycles)
        assertEquals(20, cycleData.gamesPlayed)
        assertEquals(Triple(8, 4, 8), cycleData.winDrawLoss)
        assertEquals(45.5, cycleData.averageGameLength)
        assertEquals(0.125, cycleData.averageReward)
        assertEquals(50.0, bufferStats.utilizationPercent)
    }
    
    @Test
    fun testFormatManagerNumberFormatting() {
        val formatManager = FormatManager(decimalPlaces = 2)
        
        // Test basic number formatting
        val result1 = formatManager.formatNumber(0.25)
        println("formatNumber(0.25) = '$result1'")
        assertEquals("0.25", result1)
        
        val result2 = formatManager.formatNumber(1.0)
        println("formatNumber(1.0) = '$result2'")
        assertEquals("1.00", result2)
        
        val result3 = formatManager.formatNumber(123.456)
        println("formatNumber(123.456) = '$result3'")
        assertEquals("123.46", result3)
        
        val result4 = formatManager.formatNumber(Double.NaN)
        println("formatNumber(NaN) = '$result4'")
        assertEquals("NaN", result4)
        
        val result5 = formatManager.formatNumber(Double.POSITIVE_INFINITY)
        println("formatNumber(+Inf) = '$result5'")
        assertEquals("∞", result5)
    }
    
    @Test
    fun testFormatManagerDurationFormatting() {
        val formatManager = FormatManager()
        
        assertEquals("500ms", formatManager.formatDuration(500L.toDuration()))
        assertEquals("2.5s", formatManager.formatDuration(2500L.toDuration()))
        assertEquals("1m30s", formatManager.formatDuration(90000L.toDuration()))
        assertEquals("1h5m", formatManager.formatDuration(3900000L.toDuration()))
    }
    
    @Test
    fun testFormatManagerPercentageFormatting() {
        val formatManager = FormatManager()
        
        assertEquals("25.0%", formatManager.formatPercentage(0.25))
        assertEquals("100.0%", formatManager.formatPercentage(1.0))
        assertEquals("0.0%", formatManager.formatPercentage(0.0))
    }
    
    @Test
    fun testBufferStatsCreation() {
        val stats = BufferStats.create(750, 1000)
        
        assertEquals(750, stats.currentSize)
        assertEquals(1000, stats.maxSize)
        assertEquals(75.0, stats.utilizationPercent)
    }
    
    @Test
    fun testCycleSummaryFormatting() {
        val formatManager = FormatManager()
        val bufferStats = BufferStats.create(500, 1000)
        val cycleData = CycleProgressData(
            cycleNumber = 5,
            totalCycles = 20,
            gamesPlayed = 15,
            winDrawLoss = Triple(6, 3, 6),
            averageGameLength = 42.3,
            averageReward = 0.156,
            batchesProcessed = 3,
            averageLoss = 0.234,
            gradientNorm = 1.234,
            bufferUtilization = bufferStats,
            cycleDuration = 25.seconds
        )
        
        val summary = formatManager.formatCycleSummary(cycleData)
        println("Actual summary: '$summary'")
        
        assertTrue(summary.contains("Cycle 5/20"), "Should contain 'Cycle 5/20'")
        assertTrue(summary.contains("games=15"), "Should contain 'games=15'")
        assertTrue(summary.contains("win/draw/loss=6/3/6"), "Should contain 'win/draw/loss=6/3/6'")
        assertTrue(summary.contains("avgLen=42.30"), "Should contain 'avgLen=42.30'")
        assertTrue(summary.contains("reward=0.16"), "Should contain 'reward=0.16'")
        assertTrue(summary.contains("batches=3"), "Should contain 'batches=3'")
        assertTrue(summary.contains("loss=0.23"), "Should contain 'loss=0.23'")
        assertTrue(summary.contains("grad=1.23"), "Should contain 'grad=1.23'")
        assertTrue(summary.contains("buf=0.5k/1.0k"), "Should contain 'buf=0.5k/1.0k'")
        assertTrue(summary.contains("25.0s"), "Should contain '25.0s'")
    }
    
    @Test
    fun testConfigSummaryFormatting() {
        val formatManager = FormatManager()
        val profiles = listOf("default", "fast-training")
        val overrides = mapOf("maxCycles" to 50, "batchSize" to 64)
        val keyParams = mapOf("gamesPerCycle" to 20, "evaluationGames" to 10)
        
        val summary = formatManager.formatConfigSummary(profiles, overrides, keyParams)
        
        assertTrue(summary.contains("Training Configuration"))
        assertTrue(summary.contains("Profiles: default, fast-training"))
        assertTrue(summary.contains("CLI Overrides:"))
        assertTrue(summary.contains("maxCycles = 50"))
        assertTrue(summary.contains("Key Parameters:"))
        assertTrue(summary.contains("gamesPerCycle = 20"))
    }
    
    @Test
    fun testBestPerformanceTracking() {
        val outputManager = OutputManager()
        
        // Initially should be null (no data yet)
        assertEquals(null, outputManager.getBestPerformance())
        
        // Reset should work
        outputManager.resetMetricsTracking()
        assertEquals(null, outputManager.getBestPerformance())
    }
    
    @Test
    fun testMetricsTrackerIntegration() {
        val outputManager = OutputManager()
        val metricsTracker = outputManager.getMetricsTracker()
        
        assertNotNull(metricsTracker)
        assertEquals(0, metricsTracker.getHistorySize())
        
        // Add some cycle data
        val cycleData = CycleProgressData(
            cycleNumber = 1,
            totalCycles = 10,
            gamesPlayed = 20,
            winDrawLoss = Triple(8, 4, 8),
            averageGameLength = 45.5,
            averageReward = 0.125,
            batchesProcessed = 5,
            averageLoss = 0.234,
            gradientNorm = 1.456,
            bufferUtilization = BufferStats.create(500, 1000),
            cycleDuration = 30.seconds
        )
        
        // This should update the metrics tracker
        outputManager.reportCycleProgress(cycleData)
        
        assertEquals(1, metricsTracker.getHistorySize())
        assertEquals(0.125, metricsTracker.getCurrentBestPerformance())
    }
}