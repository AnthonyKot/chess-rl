package com.chessrl.integration

import kotlin.test.*

/**
 * Comprehensive tests for the Training Visualization System
 */
class TrainingVisualizationTest {
    
    private lateinit var visualizationSystem: TrainingVisualizationSystem
    private lateinit var trainingController: AdvancedTrainingController
    
    @BeforeTest
    fun setup() {
        visualizationSystem = TrainingVisualizationSystem()
        trainingController = AdvancedTrainingController()
    }
    
    @Test
    fun testVisualizationSystemInitialization() {
        println("Testing Visualization System Initialization")
        
        val result = visualizationSystem.startVisualization(trainingController)
        
        assertTrue(result is VisualizationResult.Success, "Visualization should start successfully")
        
        val stopResult = visualizationSystem.stopVisualization()
        assertTrue(stopResult is VisualizationResult.Success, "Visualization should stop successfully")
    }
    
    @Test
    fun testDashboardRendering() {
        println("Testing Dashboard Rendering")
        
        visualizationSystem.startVisualization(trainingController)
        
        val dashboard = visualizationSystem.renderDashboard()
        
        assertNotNull(dashboard, "Dashboard should be rendered")
        assertTrue(dashboard.isNotEmpty(), "Dashboard should not be empty")
        assertTrue(dashboard.contains("TRAINING DASHBOARD"), "Dashboard should contain title")
        assertTrue(dashboard.contains("TRAINING PROGRESS"), "Dashboard should contain progress section")
        assertTrue(dashboard.contains("GAME ANALYSIS"), "Dashboard should contain game analysis section")
        assertTrue(dashboard.contains("PERFORMANCE MONITOR"), "Dashboard should contain performance section")
        
        visualizationSystem.stopVisualization()
    }
    
    @Test
    fun testLearningCurveVisualization() {
        println("Testing Learning Curve Visualization")
        
        visualizationSystem.startVisualization(trainingController)
        
        // Update with some sample data
        visualizationSystem.updateVisualization(trainingController)
        
        val learningCurve = visualizationSystem.renderLearningCurve()
        
        assertNotNull(learningCurve, "Learning curve should be rendered")
        assertTrue(learningCurve.contains("LEARNING CURVE ANALYSIS"), "Should contain learning curve title")
        assertTrue(learningCurve.contains("CONVERGENCE ANALYSIS"), "Should contain convergence analysis")
        
        visualizationSystem.stopVisualization()
    }
    
    @Test
    fun testPerformanceMonitoring() {
        println("Testing Performance Monitoring")
        
        visualizationSystem.startVisualization(trainingController)
        
        val performanceMonitor = visualizationSystem.renderPerformanceMonitor()
        
        assertNotNull(performanceMonitor, "Performance monitor should be rendered")
        assertTrue(performanceMonitor.contains("PERFORMANCE MONITORING"), "Should contain performance title")
        assertTrue(performanceMonitor.contains("RESOURCE UTILIZATION"), "Should contain resource utilization")
        assertTrue(performanceMonitor.contains("EFFICIENCY METRICS"), "Should contain efficiency metrics")
        
        visualizationSystem.stopVisualization()
    }
    
    @Test
    fun testChessBoardVisualization() {
        println("Testing Chess Board Visualization")
        
        val boardVisualizer = ChessBoardVisualizer()
        
        // Test standard starting position
        val startingPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val board = boardVisualizer.renderBoard(startingPosition)
        
        assertNotNull(board, "Board should be rendered")
        assertTrue(board.contains("a b c d e f g h"), "Board should contain file labels")
        assertTrue(board.contains("r"), "Board should contain black rook")
        assertTrue(board.contains("R"), "Board should contain white rook")
        assertTrue(board.contains("8"), "Board should contain rank numbers")
        
        // Test empty board
        val emptyBoard = boardVisualizer.renderEmptyBoard()
        assertNotNull(emptyBoard, "Empty board should be rendered")
    }
    
    @Test
    fun testChartRendering() {
        println("Testing Chart Rendering")
        
        val chartRenderer = ChartRenderer()
        
        // Test full chart
        val data = listOf(0.1, 0.3, 0.5, 0.7, 0.6, 0.8, 0.9, 0.85, 0.95, 1.0)
        val chart = chartRenderer.renderChart(
            data = data,
            width = 30,
            height = 10,
            title = "Test Chart",
            yLabel = "Value"
        )
        
        assertNotNull(chart, "Chart should be rendered")
        assertTrue(chart.contains("Test Chart"), "Chart should contain title")
        assertTrue(chart.contains("█"), "Chart should contain data points")
        
        // Test mini chart
        val miniChart = chartRenderer.renderMiniChart(
            data = data,
            width = 20,
            height = 5,
            title = "Mini Chart"
        )
        
        assertNotNull(miniChart, "Mini chart should be rendered")
        assertTrue(miniChart.contains("Mini Chart"), "Mini chart should contain title")
    }
    
    @Test
    fun testTrendAnalysis() {
        println("Testing Trend Analysis")
        
        val trendAnalyzer = TrendAnalyzer()
        
        // Create sample metrics history
        val metricsHistory = (1..50).map { i ->
            TimestampedMetrics(
                timestamp = getCurrentTimeMillis() + i * 1000L,
                metrics = TrainingMetrics(
                    currentIteration = i,
                    totalIterations = 100,
                    averageReward = 0.1 + i * 0.01, // Increasing trend
                    bestReward = 0.1 + i * 0.015,
                    trainingLoss = 1.0 - i * 0.01, // Decreasing trend
                    explorationRate = 0.1,
                    gamesPlayed = i * 10,
                    experiencesCollected = i * 100
                )
            )
        }
        
        val trendAnalysis = trendAnalyzer.analyzeTrends(metricsHistory)
        
        assertNotNull(trendAnalysis, "Trend analysis should be performed")
        assertEquals(TrendDirection.INCREASING, trendAnalysis.rewardTrend, "Reward should show increasing trend")
        assertEquals(TrendDirection.DECREASING, trendAnalysis.lossTrend, "Loss should show decreasing trend")
        assertTrue(trendAnalysis.stability > 0.0, "Stability should be calculated")
        
        // Test convergence analysis
        val convergenceAnalysis = trendAnalyzer.analyzeConvergence(metricsHistory)
        assertNotNull(convergenceAnalysis, "Convergence analysis should be performed")
        assertTrue(convergenceAnalysis.stability >= 0.0, "Stability should be non-negative")
    }
    
    @Test
    fun testProgressDisplay() {
        println("Testing Progress Display")
        
        val progressDisplay = ProgressDisplay()
        
        // Test progress bar with different values
        val progress25 = progressDisplay.renderProgressBar(25, 100, 20)
        assertTrue(progress25.contains("25%"), "Should show 25% progress")
        assertTrue(progress25.contains("["), "Should contain progress bar brackets")
        
        val progress75 = progressDisplay.renderProgressBar(75, 100, 20)
        assertTrue(progress75.contains("75%"), "Should show 75% progress")
        
        val progress100 = progressDisplay.renderProgressBar(100, 100, 20)
        assertTrue(progress100.contains("100%"), "Should show 100% progress")
        
        // Test default progress bar
        val defaultProgress = progressDisplay.renderProgressBar()
        assertNotNull(defaultProgress, "Default progress bar should be rendered")
    }
    
    @Test
    fun testVisualizationDataCollection() {
        println("Testing Visualization Data Collection")
        
        // Start training session for data collection
        val sessionConfig = TrainingSessionConfig(
            name = "visualization_test",
            controllerType = ControllerType.INTEGRATED,
            iterations = 3,
            integratedConfig = IntegratedSelfPlayConfig(
                gamesPerIteration = 1,
                maxStepsPerGame = 3,
                hiddenLayers = listOf(8, 4)
            )
        )
        
        trainingController.startTraining(sessionConfig)
        visualizationSystem.startVisualization(trainingController)
        
        // Update visualization to collect data
        visualizationSystem.updateVisualization(trainingController)
        
        // Verify dashboard renders with collected data
        val dashboard = visualizationSystem.renderDashboard()
        assertTrue(dashboard.isNotEmpty(), "Dashboard should render with collected data")
        
        trainingController.stopTraining()
        visualizationSystem.stopVisualization()
    }
    
    @Test
    fun testInteractiveVisualizationComponents() {
        println("Testing Interactive Visualization Components")
        
        // Test display modes
        val modes = DisplayMode.values()
        assertTrue(modes.contains(DisplayMode.DASHBOARD), "Should have dashboard mode")
        assertTrue(modes.contains(DisplayMode.PERFORMANCE_MONITOR), "Should have performance monitor mode")
        assertTrue(modes.contains(DisplayMode.LEARNING_ANALYSIS), "Should have learning analysis mode")
        assertTrue(modes.contains(DisplayMode.GAME_ANALYSIS), "Should have game analysis mode")
        
        // Test real-time dashboard creation
        val realTimeDashboard = RealTimeVisualizationDashboard(trainingController, visualizationSystem)
        assertNotNull(realTimeDashboard, "Real-time dashboard should be created")
        
        // Test update interval setting
        realTimeDashboard.setUpdateInterval(2000L)
        // No exception should be thrown
    }
    
    @Test
    fun testVisualizationConfiguration() {
        println("Testing Visualization Configuration")
        
        val config = VisualizationConfig(
            dashboardWidth = 120,
            dashboardHeight = 40,
            panelWidth = 38,
            updateIntervalMs = 500L,
            maxHistorySize = 500,
            enableRealTimeUpdates = true,
            enableGameAnalysis = true,
            enablePerformanceMonitoring = true
        )
        
        val customVisualizationSystem = TrainingVisualizationSystem(config)
        
        val result = customVisualizationSystem.startVisualization(trainingController)
        assertTrue(result is VisualizationResult.Success, "Custom configuration should work")
        
        customVisualizationSystem.stopVisualization()
    }
    
    @Test
    fun testVisualizationErrorHandling() {
        println("Testing Visualization Error Handling")
        
        // Test stopping without starting
        val stopResult = visualizationSystem.stopVisualization()
        assertTrue(stopResult is VisualizationResult.Success, "Should handle stop without start gracefully")
        
        // Test rendering without initialization
        val dashboard = visualizationSystem.renderDashboard()
        assertTrue(dashboard.contains("not active"), "Should handle rendering without initialization")
        
        // Test chart rendering with empty data
        val chartRenderer = ChartRenderer()
        val emptyChart = chartRenderer.renderChart(emptyList())
        assertTrue(emptyChart.contains("No data"), "Should handle empty data gracefully")
        
        val emptyMiniChart = chartRenderer.renderMiniChart(emptyList())
        assertTrue(emptyMiniChart.contains("No data"), "Should handle empty mini chart data")
    }
    
    @Test
    fun testPerformanceMetricsCalculation() {
        println("Testing Performance Metrics Calculation")
        
        // Create sample performance history
        val performanceHistory = (1..20).map { i ->
            PerformanceSnapshot(
                timestamp = getCurrentTimeMillis() + i * 1000L,
                memoryUsage = 0.3 + i * 0.01,
                cpuUsage = 0.2 + i * 0.005,
                gpuUsage = 0.4 + i * 0.008,
                gamesPerSecond = 2.0 + i * 0.1,
                experiencesPerSecond = 10.0 + i * 0.5,
                avgIterationTime = 5000.0 - i * 50.0,
                throughput = 2.0 + i * 0.1
            )
        }
        
        val trendAnalyzer = TrendAnalyzer()
        val perfTrends = trendAnalyzer.analyzePerformanceTrends(performanceHistory)
        
        assertNotNull(perfTrends, "Performance trends should be calculated")
        assertEquals(TrendDirection.INCREASING, perfTrends.throughputTrend, "Throughput should show increasing trend")
        assertEquals(TrendDirection.INCREASING, perfTrends.memoryTrend, "Memory should show increasing trend")
        assertEquals(TrendDirection.INCREASING, perfTrends.efficiencyTrend, "Efficiency should show increasing trend")
    }
    
    @Test
    fun testVisualizationIntegrationWithTrainingController() {
        println("Testing Visualization Integration with Training Controller")
        
        // Start training session
        val sessionConfig = TrainingSessionConfig(
            name = "integration_test",
            controllerType = ControllerType.INTEGRATED,
            iterations = 2,
            integratedConfig = IntegratedSelfPlayConfig(
                gamesPerIteration = 1,
                maxStepsPerGame = 2,
                hiddenLayers = listOf(8, 4)
            )
        )
        
        val startResult = trainingController.startTraining(sessionConfig)
        assertTrue(startResult is TrainingControlResult.Success, "Training should start successfully")
        
        // Start visualization
        val vizResult = visualizationSystem.startVisualization(trainingController)
        assertTrue(vizResult is VisualizationResult.Success, "Visualization should start successfully")
        
        // Update visualization with training data
        visualizationSystem.updateVisualization(trainingController)
        
        // Verify integration works
        val dashboard = visualizationSystem.renderDashboard()
        assertTrue(dashboard.contains("TRAINING DASHBOARD"), "Dashboard should show training data")
        
        val status = trainingController.getTrainingStatus()
        assertNotNull(status.session, "Training session should be active")
        
        // Clean up
        trainingController.stopTraining()
        visualizationSystem.stopVisualization()
    }
}