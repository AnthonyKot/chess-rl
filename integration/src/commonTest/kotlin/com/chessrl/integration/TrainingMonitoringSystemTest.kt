package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.test.*

/**
 * Comprehensive tests for the training monitoring and analysis system.
 * 
 * Tests cover:
 * - Monitoring system reliability and accuracy
 * - Advanced metrics collection with statistical significance
 * - Real-time monitoring interface functionality
 * - Training issue detection and automated recommendations
 * - Game analysis with position evaluation
 * - Report generation and data export
 */
class TrainingMonitoringSystemTest {
    
    private lateinit var monitoringSystem: TrainingMonitoringSystem
    private lateinit var mockSelfPlayResults: SelfPlayResults
    private lateinit var mockTrainingResults: ValidatedTrainingResults
    private lateinit var mockPerformanceResults: PerformanceEvaluationResult
    
    @BeforeTest
    fun setup() {
        val config = MonitoringConfig(
            displayUpdateInterval = 1, // Update every cycle for testing
            convergenceAnalysisWindow = 5, // Smaller window for testing
            convergenceStabilityThreshold = 0.7
        )
        
        monitoringSystem = TrainingMonitoringSystem(config)
        
        // Create mock data
        setupMockData()
    }
    
    @Test
    fun testMonitoringSystemInitialization() {
        // Test monitoring system starts correctly
        val session = monitoringSystem.startMonitoring("test_config")
        
        assertNotNull(session)
        assertTrue(session.sessionId.isNotEmpty())
        assertEquals("test_config", session.trainingConfig)
        assertTrue(session.startTime > 0)
    }
    
    @Test
    fun testTrainingCycleRecording() {
        // Start monitoring
        monitoringSystem.startMonitoring("test_config")
        
        // Record a training cycle
        val analysis = monitoringSystem.recordTrainingCycle(
            cycle = 1,
            selfPlayResults = mockSelfPlayResults,
            trainingResults = mockTrainingResults,
            performanceResults = mockPerformanceResults,
            additionalMetrics = mapOf("custom_metric" to 0.75)
        )
        
        // Verify analysis results
        assertNotNull(analysis)
        assertEquals(1, analysis.cycle)
        assertNotNull(analysis.metrics)
        assertNotNull(analysis.gameAnalyses)
        assertNotNull(analysis.performanceSnapshot)
        assertNotNull(analysis.convergenceStatus)
        assertTrue(analysis.analysisTime > 0)
        
        // Verify metrics are properly collected
        val metrics = analysis.metrics
        assertTrue(metrics.gamesPlayed > 0)
        assertTrue(metrics.experiencesCollected > 0)
        assertTrue(metrics.winRate >= 0.0 && metrics.winRate <= 1.0)
        assertTrue(metrics.averageReward >= -1.0 && metrics.averageReward <= 1.0)
        assertEquals(0.75, metrics.customMetrics["custom_metric"])
    }
    
    @Test
    fun testRealTimeDashboard() {
        // Start monitoring and record some cycles
        monitoringSystem.startMonitoring("test_config")
        
        repeat(3) { cycle ->
            monitoringSystem.recordTrainingCycle(
                cycle = cycle + 1,
                selfPlayResults = mockSelfPlayResults,
                trainingResults = mockTrainingResults,
                performanceResults = mockPerformanceResults
            )
        }
        
        // Get dashboard
        val dashboard = monitoringSystem.getTrainingDashboard()
        
        // Verify dashboard content
        assertNotNull(dashboard)
        assertEquals(3, dashboard.sessionInfo.totalCycles)
        assertTrue(dashboard.sessionInfo.totalGames > 0)
        assertTrue(dashboard.sessionInfo.elapsedTime > 0)
        
        assertNotNull(dashboard.currentStats)
        assertNotNull(dashboard.recentTrends)
        assertNotNull(dashboard.systemHealth)
        assertNotNull(dashboard.performanceMetrics)
        assertNotNull(dashboard.gameQualityMetrics)
        assertNotNull(dashboard.trainingEfficiency)
    }
    
    @Test
    fun testIssueDetection() {
        monitoringSystem.startMonitoring("test_config")
        
        // Create training results with issues
        val problematicResults = createProblematicTrainingResults()
        
        val analysis = monitoringSystem.recordTrainingCycle(
            cycle = 1,
            selfPlayResults = mockSelfPlayResults,
            trainingResults = problematicResults,
            performanceResults = mockPerformanceResults
        )
        
        // Verify issues were detected
        assertTrue(analysis.detectedIssues.isNotEmpty())
        
        val explodingGradientIssue = analysis.detectedIssues.find { 
            it.type == IssueType.EXPLODING_GRADIENTS 
        }
        assertNotNull(explodingGradientIssue)
        assertTrue(explodingGradientIssue.severity > 0.5)
        assertTrue(explodingGradientIssue.suggestedActions.isNotEmpty())
    }
    
    @Test
    fun testStatisticalSignificanceAnalysis() {
        monitoringSystem.startMonitoring("test_config")
        
        // Record multiple cycles to enable statistical analysis
        repeat(15) { cycle ->
            val performanceResults = createPerformanceResults(
                winRate = 0.3 + (cycle * 0.03), // Gradual improvement
                averageReward = -0.2 + (cycle * 0.05)
            )
            
            monitoringSystem.recordTrainingCycle(
                cycle = cycle + 1,
                selfPlayResults = mockSelfPlayResults,
                trainingResults = mockTrainingResults,
                performanceResults = performanceResults
            )
        }
        
        // Get the latest analysis
        val dashboard = monitoringSystem.getTrainingDashboard()
        
        // Verify statistical analysis is available
        assertNotNull(dashboard.currentStats)
        assertTrue(dashboard.recentTrends.rewardTrend > 0) // Should show improvement
        assertTrue(dashboard.recentTrends.winRateTrend > 0) // Should show improvement
    }
    
    @Test
    fun testConvergenceDetection() {
        monitoringSystem.startMonitoring("test_config")
        
        // Simulate converging training
        repeat(10) { cycle ->
            val winRate = 0.8 + (Random.nextDouble() - 0.5) * 0.02 // Stable around 0.8
            val performanceResults = createPerformanceResults(winRate, 0.5)
            
            monitoringSystem.recordTrainingCycle(
                cycle = cycle + 1,
                selfPlayResults = mockSelfPlayResults,
                trainingResults = mockTrainingResults,
                performanceResults = performanceResults
            )
        }
        
        val dashboard = monitoringSystem.getTrainingDashboard()
        
        // Should detect convergence
        assertTrue(dashboard.currentStats.convergenceScore > 0.5)
    }
    
    @Test
    fun testInteractiveCommands() {
        monitoringSystem.startMonitoring("test_config")
        
        // Test pause command
        val pauseResult = monitoringSystem.executeCommand(MonitoringCommand.Pause)
        assertTrue(pauseResult is CommandResult.Success)
        
        // Test resume command
        val resumeResult = monitoringSystem.executeCommand(MonitoringCommand.Resume)
        assertTrue(resumeResult is CommandResult.Success)
        
        // Test report generation command
        val reportResult = monitoringSystem.executeCommand(
            MonitoringCommand.GenerateReport(ReportType.PERFORMANCE)
        )
        assertTrue(reportResult is CommandResult.Success)
    }
    
    @Test
    fun testGameAnalysisWithPositions() {
        monitoringSystem.startMonitoring("test_config")
        
        // Record a cycle to generate game data
        val analysis = monitoringSystem.recordTrainingCycle(
            cycle = 1,
            selfPlayResults = mockSelfPlayResults,
            trainingResults = mockTrainingResults,
            performanceResults = mockPerformanceResults
        )
        
        // Analyze a specific game
        val gameId = analysis.gameAnalyses.first().gameId
        val detailedAnalysis = monitoringSystem.analyzeGameWithPositions(
            gameId = gameId,
            includePositionEvaluation = true,
            includeMoveAnalysis = true,
            includeStrategicAnalysis = true
        )
        
        // Verify detailed analysis
        assertNotNull(detailedAnalysis)
        assertEquals(gameId, detailedAnalysis.gameId)
        assertTrue(detailedAnalysis.positionAnalyses.isNotEmpty())
        assertTrue(detailedAnalysis.moveAnalyses.isNotEmpty())
        assertNotNull(detailedAnalysis.strategicAnalysis)
        assertNotNull(detailedAnalysis.gameQuality)
        assertTrue(detailedAnalysis.learningInsights.isNotEmpty())
        assertTrue(detailedAnalysis.analysisTime > 0)
    }
    
    @Test
    fun testComprehensiveReportGeneration() {
        monitoringSystem.startMonitoring("test_config")
        
        // Record multiple cycles
        repeat(5) { cycle ->
            monitoringSystem.recordTrainingCycle(
                cycle = cycle + 1,
                selfPlayResults = mockSelfPlayResults,
                trainingResults = mockTrainingResults,
                performanceResults = mockPerformanceResults
            )
        }
        
        // Generate comprehensive report
        val report = monitoringSystem.generateComprehensiveReport()
        
        // Verify report structure
        assertNotNull(report)
        assertTrue(report.sessionId.isNotEmpty())
        assertTrue(report.generatedAt > 0)
        assertTrue(report.generationTime > 0)
        
        // Verify report sections
        assertNotNull(report.executiveSummary)
        assertNotNull(report.performanceAnalysis)
        assertNotNull(report.gameQualityAnalysis)
        assertNotNull(report.trainingEfficiencyAnalysis)
        assertNotNull(report.issueAnalysis)
        assertNotNull(report.convergenceAnalysis)
        assertNotNull(report.recommendations)
        assertNotNull(report.technicalDetails)
        
        // Verify executive summary content
        val summary = report.executiveSummary
        assertTrue(summary.overallPerformance.isNotEmpty())
        assertTrue(summary.finalMetrics.isNotEmpty())
        assertTrue(summary.recommendationSummary.isNotEmpty())
        
        // Verify performance analysis
        val performance = report.performanceAnalysis
        assertTrue(performance.performanceTrend.isNotEmpty())
        assertTrue(performance.bestPerformance >= 0.0)
        assertTrue(performance.performanceBreakdown.isNotEmpty())
    }
    
    @Test
    fun testMonitoringSystemReliability() {
        monitoringSystem.startMonitoring("test_config")
        
        // Test with various edge cases
        
        // Empty results
        val emptyResults = SelfPlayResults(
            totalGames = 0,
            totalExperiences = 0,
            averageGameLength = 0.0,
            gameResults = emptyList(),
            experiences = emptyList()
        )
        
        val emptyTrainingResults = ValidatedTrainingResults(
            totalBatches = 0,
            validBatches = 0,
            averageLoss = 0.0,
            averageGradientNorm = 0.0,
            averagePolicyEntropy = 0.0,
            averageExperienceQuality = 0.0,
            batchResults = emptyList()
        )
        
        val emptyPerformanceResults = PerformanceEvaluationResult(
            cycle = 1,
            gamesPlayed = 0,
            averageReward = 0.0,
            averageGameLength = 0.0,
            winRate = 0.0,
            drawRate = 0.0,
            lossRate = 0.0,
            performanceScore = 0.0,
            gameResults = emptyList()
        )
        
        // Should handle empty results gracefully
        assertDoesNotThrow {
            monitoringSystem.recordTrainingCycle(
                cycle = 1,
                selfPlayResults = emptyResults,
                trainingResults = emptyTrainingResults,
                performanceResults = emptyPerformanceResults
            )
        }
        
        // Test with extreme values
        val extremePerformanceResults = createPerformanceResults(
            winRate = 1.0, // Perfect win rate
            averageReward = 1.0 // Maximum reward
        )
        
        assertDoesNotThrow {
            monitoringSystem.recordTrainingCycle(
                cycle = 2,
                selfPlayResults = mockSelfPlayResults,
                trainingResults = mockTrainingResults,
                performanceResults = extremePerformanceResults
            )
        }
    }
    
    @Test
    fun testMonitoringAccuracy() {
        monitoringSystem.startMonitoring("test_config")
        
        // Record cycle with known metrics
        val knownWinRate = 0.75
        val knownAverageReward = 0.5
        val knownGameLength = 45.0
        
        val performanceResults = createPerformanceResults(knownWinRate, knownAverageReward)
        val selfPlayResults = createSelfPlayResults(averageGameLength = knownGameLength)
        
        val analysis = monitoringSystem.recordTrainingCycle(
            cycle = 1,
            selfPlayResults = selfPlayResults,
            trainingResults = mockTrainingResults,
            performanceResults = performanceResults
        )
        
        // Verify accuracy of recorded metrics
        assertEquals(knownWinRate, analysis.metrics.winRate, 0.01)
        assertEquals(knownAverageReward, analysis.metrics.averageReward, 0.01)
        assertEquals(knownGameLength, analysis.metrics.averageGameLength, 0.1)
    }
    
    @Test
    fun testMonitoringStop() {
        monitoringSystem.startMonitoring("test_config")
        
        // Record some cycles
        repeat(3) { cycle ->
            monitoringSystem.recordTrainingCycle(
                cycle = cycle + 1,
                selfPlayResults = mockSelfPlayResults,
                trainingResults = mockTrainingResults,
                performanceResults = mockPerformanceResults
            )
        }
        
        // Stop monitoring
        val results = monitoringSystem.stopMonitoring()
        
        // Verify results
        assertNotNull(results)
        assertTrue(results.sessionDuration > 0)
        assertEquals(3, results.totalCycles)
        assertTrue(results.totalGames > 0)
        assertTrue(results.finalPerformance >= 0.0)
        assertNotNull(results.finalReport)
        assertTrue(results.dataExports.isNotEmpty())
    }
    
    // Helper methods for creating mock data
    
    private fun setupMockData() {
        mockSelfPlayResults = createSelfPlayResults()
        mockTrainingResults = createTrainingResults()
        mockPerformanceResults = createPerformanceResults()
    }
    
    private fun createSelfPlayResults(
        totalGames: Int = 10,
        totalExperiences: Int = 500,
        averageGameLength: Double = 50.0
    ): SelfPlayResults {
        val gameResults = (1..totalGames).map { gameIndex ->
            GameResult(
                gameIndex = gameIndex,
                gameLength = (averageGameLength + Random.nextDouble(-10.0, 10.0)).toInt(),
                gameOutcome = listOf(GameOutcome.WHITE_WINS, GameOutcome.BLACK_WINS, GameOutcome.DRAW).random(),
                finalPosition = "final_position_$gameIndex",
                terminationReason = listOf("CHECKMATE", "STALEMATE", "DRAW", "STEP_LIMIT").random()
            )
        }
        
        val experiences = (1..totalExperiences).map { expIndex ->
            EnhancedExperience(
                state = DoubleArray(776) { Random.nextDouble() },
                action = Random.nextInt(4096),
                reward = Random.nextDouble(-1.0, 1.0),
                nextState = DoubleArray(776) { Random.nextDouble() },
                done = Random.nextBoolean(),
                gameId = "game_${Random.nextInt(totalGames) + 1}",
                moveNumber = Random.nextInt(1, 100),
                gameOutcome = gameResults.random().gameOutcome,
                qualityScore = Random.nextDouble(0.3, 1.0)
            )
        }
        
        return SelfPlayResults(
            totalGames = totalGames,
            totalExperiences = totalExperiences,
            averageGameLength = averageGameLength,
            gameResults = gameResults,
            experiences = experiences
        )
    }
    
    private fun createTrainingResults(
        totalBatches: Int = 8,
        averageLoss: Double = 1.5,
        averageGradientNorm: Double = 2.0,
        averagePolicyEntropy: Double = 0.8
    ): ValidatedTrainingResults {
        val batchResults = (1..totalBatches).map { batchIndex ->
            ValidatedBatchResult(
                batchIndex = batchIndex,
                batchSize = 64,
                updateResult = PolicyUpdateResult(
                    loss = averageLoss + Random.nextDouble(-0.5, 0.5),
                    gradientNorm = averageGradientNorm + Random.nextDouble(-0.5, 0.5),
                    policyEntropy = averagePolicyEntropy + Random.nextDouble(-0.2, 0.2)
                ),
                validationResult = PolicyValidationResult(
                    episodeNumber = batchIndex,
                    isValid = true,
                    issues = emptyList(),
                    warnings = emptyList(),
                    recommendations = emptyList(),
                    updateMetrics = PolicyUpdateResult(averageLoss, averageGradientNorm, averagePolicyEntropy),
                    beforeMetrics = RLMetrics(0, 0.0, 0.0, 0.0, 0.0, policyEntropy = 0.0, gradientNorm = 0.0),
                    afterMetrics = RLMetrics(0, 0.0, 0.0, 0.0, 0.0, policyEntropy = 0.0, gradientNorm = 0.0),
                    timestamp = getCurrentTimeMillis()
                ),
                experienceQuality = Random.nextDouble(0.5, 1.0)
            )
        }
        
        return ValidatedTrainingResults(
            totalBatches = totalBatches,
            validBatches = totalBatches,
            averageLoss = averageLoss,
            averageGradientNorm = averageGradientNorm,
            averagePolicyEntropy = averagePolicyEntropy,
            averageExperienceQuality = 0.75,
            batchResults = batchResults
        )
    }
    
    private fun createPerformanceResults(
        winRate: Double = 0.6,
        averageReward: Double = 0.2
    ): PerformanceEvaluationResult {
        val gamesPlayed = 5
        val gameResults = (1..gamesPlayed).map { gameIndex ->
            EvaluationGameResult(
                gameIndex = gameIndex,
                gameLength = Random.nextInt(20, 80),
                totalReward = averageReward + Random.nextDouble(-0.3, 0.3),
                gameOutcome = if (Random.nextDouble() < winRate) GameOutcome.WHITE_WINS else GameOutcome.BLACK_WINS,
                finalPosition = "eval_position_$gameIndex"
            )
        }
        
        return PerformanceEvaluationResult(
            cycle = 1,
            gamesPlayed = gamesPlayed,
            averageReward = averageReward,
            averageGameLength = gameResults.map { it.gameLength }.average(),
            winRate = winRate,
            drawRate = 0.1,
            lossRate = 1.0 - winRate - 0.1,
            performanceScore = (winRate + (averageReward + 1.0) / 2.0) / 2.0,
            gameResults = gameResults
        )
    }
    
    private fun createProblematicTrainingResults(): ValidatedTrainingResults {
        // Create results with exploding gradients
        return createTrainingResults(
            averageGradientNorm = 15.0, // Above threshold
            averagePolicyEntropy = 0.05 // Below threshold
        )
    }
}