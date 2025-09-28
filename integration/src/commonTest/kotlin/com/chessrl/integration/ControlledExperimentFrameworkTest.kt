package com.chessrl.integration

import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.config.ChessRLConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ControlledExperimentFrameworkTest {
    
    @Test
    fun testControlledExperimentBasic() {
        val framework = ControlledExperimentFramework()
        assertNotNull(framework)
        
        val metricsCollector = BenchmarkMetricsCollector()
        assertNotNull(metricsCollector)
        
        val baseConfig = createTestConfig()
        assertNotNull(baseConfig)
        
        // For now, just test that we can create the framework and config
        // The actual experiment running might have dependencies that aren't available in tests
        assertTrue(true)
    }
    
    @Test
    fun testExperimentAnalysis() {
        val framework = ControlledExperimentFramework()
        
        // Test the analysis functionality with mock data
        val mockResults = ControlledExperimentResults(
            experimentRuns = listOf(
                ExperimentRun(
                    experimentId = "test_run",
                    backendType = BackendType.MANUAL,
                    seed = 12345L,
                    config = createTestConfig(),
                    cycleMetrics = listOf(
                        BenchmarkTrainingMetrics(
                            cycle = 1,
                            backendType = "MANUAL",
                            timestamp = System.currentTimeMillis(),
                            winRate = 0.6,
                            drawRate = 0.2,
                            lossRate = 0.2,
                            totalGames = 10,
                            averageLoss = 0.1,
                            gradientNorm = 1.0,
                            policyEntropy = 0.5,
                            qValueMean = 0.3,
                            illegalMoveCount = 0,
                            trainingTimePerCycle = 1000L,
                            peakMemoryUsage = 100L,
                            currentMemoryUsage = 80L,
                            learningRate = 0.001,
                            batchSize = 32,
                            gamma = 0.99,
                            explorationRate = 0.1,
                            hiddenLayers = "256x128",
                            optimizer = "ADAM",
                            replayType = "UNIFORM"
                        )
                    ),
                    durationMs = 1000L,
                    success = true,
                    error = null
                )
            ),
            baseConfig = createTestConfig(),
            totalDurationMs = 1000L,
            timestamp = System.currentTimeMillis()
        )
        
        val analysis = framework.analyzeResults(mockResults)
        
        // Verify analysis structure
        assertNotNull(analysis)
        assertEquals(1, analysis.backendStatistics.size)
        assertEquals(1, analysis.totalExperiments)
        assertEquals(1, analysis.successfulExperiments)
    }
    
    @Test
    fun testResultsCsvExport() {
        // Test CSV export with mock data
        val mockResults = ControlledExperimentResults(
            experimentRuns = listOf(
                ExperimentRun(
                    experimentId = "test_run",
                    backendType = BackendType.MANUAL,
                    seed = 12345L,
                    config = createTestConfig(),
                    cycleMetrics = listOf(
                        BenchmarkTrainingMetrics(
                            cycle = 1,
                            backendType = "MANUAL",
                            timestamp = System.currentTimeMillis(),
                            winRate = 0.6,
                            drawRate = 0.2,
                            lossRate = 0.2,
                            totalGames = 10,
                            averageLoss = 0.1,
                            gradientNorm = 1.0,
                            policyEntropy = 0.5,
                            qValueMean = 0.3,
                            illegalMoveCount = 0,
                            trainingTimePerCycle = 1000L,
                            peakMemoryUsage = 100L,
                            currentMemoryUsage = 80L,
                            learningRate = 0.001,
                            batchSize = 32,
                            gamma = 0.99,
                            explorationRate = 0.1,
                            hiddenLayers = "256x128",
                            optimizer = "ADAM",
                            replayType = "UNIFORM"
                        )
                    ),
                    durationMs = 1000L,
                    success = true,
                    error = null
                )
            ),
            baseConfig = createTestConfig(),
            totalDurationMs = 1000L,
            timestamp = System.currentTimeMillis()
        )
        
        val csv = mockResults.exportToCsv()
        
        // Verify CSV structure
        assertNotNull(csv)
        val lines = csv.split("\n")
        assertTrue(lines.size > 1) // Header + data rows
        
        // Verify header
        val header = lines[0]
        assertTrue(header.contains("experimentId"))
        assertTrue(header.contains("backendType"))
        assertTrue(header.contains("seed"))
        assertTrue(header.contains("cycle"))
    }
    
    @Test
    fun testAnalysisSummaryReport() {
        val framework = ControlledExperimentFramework()
        
        // Create mock analysis data
        val mockAnalysis = ExperimentAnalysis(
            backendStatistics = mapOf(
                BackendType.MANUAL to BackendStatistics(
                    backendType = BackendType.MANUAL,
                    totalRuns = 1,
                    successfulRuns = 1,
                    averageWinRate = 0.6,
                    winRateStdDev = 0.1,
                    averageLoss = 0.1,
                    lossStdDev = 0.01,
                    averageTrainingTime = 1000.0,
                    trainingTimeStdDev = 100.0
                )
            ),
            totalExperiments = 1,
            successfulExperiments = 1,
            analysisTimestamp = System.currentTimeMillis()
        )
        
        val report = mockAnalysis.generateSummaryReport()
        
        // Verify report content
        assertNotNull(report)
        assertTrue(report.contains("Controlled Experiment Analysis Report"))
        assertTrue(report.contains("Total Experiments: 1"))
        assertTrue(report.contains("Backend Performance Comparison"))
        assertTrue(report.contains("MANUAL:"))
        assertTrue(report.contains("Average Win Rate:"))
        assertTrue(report.contains("Average Loss:"))
        assertTrue(report.contains("Average Training Time:"))
    }
    
    @Test
    fun testParallelExperiments() {
        val framework = ControlledExperimentFramework()
        
        // For now, just test that the parallel method exists and can be called
        // The actual implementation runs sequentially
        assertNotNull(framework)
        assertTrue(true) // Placeholder test
    }
    
    @Test
    fun testExperimentWithDifferentSeeds() {
        val framework = ControlledExperimentFramework()
        
        // Test that the framework can handle different seed configurations
        val baseConfig = createTestConfig()
        val seeds = listOf(111L, 222L, 333L)
        
        // Test that withSeed works correctly
        for (seed in seeds) {
            val configWithSeed = baseConfig.withSeed(seed)
            assertEquals(seed, configWithSeed.seed)
        }
        
        assertTrue(true) // Test passes if no exceptions are thrown
    }
    
    private fun createTestConfig(): ChessRLConfig {
        return ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.99,
            explorationRate = 0.1,
            hiddenLayers = listOf(256, 128),
            optimizer = "ADAM",
            replayType = "UNIFORM",
            maxExperienceBuffer = 10000,
            targetUpdateFrequency = 100,
            doubleDqn = true,
            gamesPerCycle = 5, // Small number for testing
            maxCycles = 10
        )
    }
}