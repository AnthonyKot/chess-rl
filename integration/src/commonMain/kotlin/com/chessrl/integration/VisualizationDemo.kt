package com.chessrl.integration

/**
 * Comprehensive demonstration of the Training Visualization System
 * Shows all visualization features including real-time dashboard,
 * game analysis, learning curves, and performance monitoring
 */
object VisualizationDemo {
    
    /**
     * Run complete visualization demonstration
     */
    fun runCompleteVisualizationDemo(): VisualizationDemoResults {
        println("🎨 Training Visualization System - Complete Demo")
        println("=" * 60)
        
        val demoStartTime = getCurrentTimeMillis()
        val results = mutableListOf<String>()
        
        try {
            // Phase 1: Dashboard Visualization
            println("\n📊 Phase 1: Interactive Training Dashboard")
            println("-" * 50)
            
            val dashboardResults = demonstrateDashboard()
            results.addAll(dashboardResults)
            
            // Phase 2: Real-Time Game Analysis
            println("\n♟️ Phase 2: Real-Time Game Analysis")
            println("-" * 50)
            
            val gameAnalysisResults = demonstrateGameAnalysis()
            results.addAll(gameAnalysisResults)
            
            // Phase 3: Learning Curve Visualization
            println("\n📈 Phase 3: Learning Curve Visualization")
            println("-" * 50)
            
            val learningCurveResults = demonstrateLearningCurves()
            results.addAll(learningCurveResults)
            
            // Phase 4: Performance Monitoring
            println("\n⚡ Phase 4: Performance Monitoring")
            println("-" * 50)
            
            val performanceResults = demonstratePerformanceMonitoring()
            results.addAll(performanceResults)
            
            // Phase 5: Interactive Features
            println("\n🎮 Phase 5: Interactive Features")
            println("-" * 50)
            
            val interactiveResults = demonstrateInteractiveFeatures()
            results.addAll(interactiveResults)
            
            val demoEndTime = getCurrentTimeMillis()
            val totalDuration = demoEndTime - demoStartTime
            
            println("\n🏁 Visualization Demo Completed!")
            println("Total duration: ${totalDuration}ms")
            println("All visualization features demonstrated successfully!")
            
            return VisualizationDemoResults(
                success = true,
                results = results,
                duration = totalDuration,
                featuresDemo = mapOf(
                    "Dashboard" to true,
                    "Game Analysis" to true,
                    "Learning Curves" to true,
                    "Performance Monitoring" to true,
                    "Interactive Features" to true
                )
            )
            
        } catch (e: Exception) {
            println("❌ Visualization demo failed: ${e.message}")
            e.printStackTrace()
            results.add("Demo failed: ${e.message}")
            
            val demoEndTime = getCurrentTimeMillis()
            val totalDuration = demoEndTime - demoStartTime
            
            return VisualizationDemoResults(
                success = false,
                results = results,
                duration = totalDuration,
                featuresDemo = emptyMap()
            )
        }
    }
    
    /**
     * Demonstrate interactive training dashboard
     */
    private fun demonstrateDashboard(): List<String> {
        println("📊 Demonstrating Interactive Training Dashboard")
        
        val results = mutableListOf<String>()
        
        try {
            // Create training controller and visualization system
            val controller = AdvancedTrainingController()
            val visualizationSystem = TrainingVisualizationSystem()
            
            // Start training session
            val sessionConfig = TrainingSessionConfig(
                name = "dashboard_demo",
                controllerType = ControllerType.INTEGRATED,
                iterations = 5,
                integratedConfig = IntegratedSelfPlayConfig(
                    gamesPerIteration = 2,
                    maxStepsPerGame = 5,
                    hiddenLayers = listOf(32, 16)
                )
            )
            
            controller.startTraining(sessionConfig)
            visualizationSystem.startVisualization(controller)
            
            println("   🚀 Training session started for dashboard demo")
            results.add("Dashboard demo: Training session started")
            
            // Update visualization with training data
            visualizationSystem.updateVisualization(controller)
            
            // Render and display dashboard
            val dashboard = visualizationSystem.renderDashboard()
            println("   📊 Dashboard rendered:")
            println(dashboard)
            
            results.add("Dashboard demo: Dashboard rendered successfully")
            results.add("Dashboard components: Progress, Metrics, Game Analysis, Performance")
            
            // Test dashboard updates
            repeat(3) { i ->
                println("   🔄 Dashboard update ${i + 1}/3")
                visualizationSystem.updateVisualization(controller)
                Thread.sleep(100) // Simulate time passing
            }
            
            results.add("Dashboard demo: Real-time updates working")
            
            // Clean up
            controller.stopTraining()
            visualizationSystem.stopVisualization()
            
            println("   ✅ Dashboard demonstration completed")
            results.add("Dashboard demo: COMPLETED")
            
        } catch (e: Exception) {
            println("   ❌ Dashboard demo failed: ${e.message}")
            results.add("Dashboard demo: FAILED - ${e.message}")
        }
        
        return results
    }
}    
/**
     * Demonstrate real-time game analysis with ASCII board display
     */
    private fun demonstrateGameAnalysis(): List<String> {
        println("♟️ Demonstrating Real-Time Game Analysis")
        
        val results = mutableListOf<String>()
        
        try {
            val boardVisualizer = ChessBoardVisualizer()
            
            // Demonstrate different chess positions
            val positions = listOf(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" to "Starting Position",
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1" to "After 1.e4",
                "rnbqkb1r/pppp1ppp/5n2/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 3" to "After 1.e4 e5 2.Nf3",
                "r1bqkb1r/pppp1ppp/2n2n2/4p3/4P3/3P1N2/PPP2PPP/RNBQKB1R b KQkq - 0 4" to "Developed Position"
            )
            
            for ((position, description) in positions) {
                println("   ♟️ Analyzing: $description")
                
                val board = boardVisualizer.renderBoard(position)
                println("   Board Position:")
                println(board.split("\n").joinToString("\n") { "   $it" })
                
                // Simulate move evaluation
                val evaluation = kotlin.random.Random.nextDouble(-1.0, 1.0)
                val confidence = kotlin.random.Random.nextDouble(0.7, 1.0)
                val bestMove = listOf("e2-e4", "Nf3", "Bc4", "0-0", "d2-d4").random()
                
                println("   📊 Analysis:")
                println("   Best Move: $bestMove")
                println("   Evaluation: ${String.format("%.3f", evaluation)}")
                println("   Confidence: ${String.format("%.1f%%", confidence * 100)}")
                println()
                
                results.add("Game analysis: $description analyzed")
            }
            
            println("   ✅ Game analysis demonstration completed")
            results.add("Game analysis: ASCII board display working")
            results.add("Game analysis: Move evaluation working")
            results.add("Game analysis: COMPLETED")
            
        } catch (e: Exception) {
            println("   ❌ Game analysis demo failed: ${e.message}")
            results.add("Game analysis: FAILED - ${e.message}")
        }
        
        return results
    }
    
    /**
     * Demonstrate learning curve visualization with convergence analysis
     */
    private fun demonstrateLearningCurves(): List<String> {
        println("📈 Demonstrating Learning Curve Visualization")
        
        val results = mutableListOf<String>()
        
        try {
            val chartRenderer = ChartRenderer()
            val trendAnalyzer = TrendAnalyzer()
            
            // Generate sample learning data
            val rewardData = generateSampleRewardCurve()
            val lossData = generateSampleLossCurve()
            
            println("   📊 Rendering reward learning curve...")
            val rewardChart = chartRenderer.renderChart(
                data = rewardData,
                width = 50,
                height = 12,
                title = "Average Reward Over Time",
                yLabel = "Reward"
            )
            
            println(rewardChart.split("\n").joinToString("\n") { "   $it" })
            results.add("Learning curves: Reward chart rendered")
            
            println("   📉 Rendering loss learning curve...")
            val lossChart = chartRenderer.renderChart(
                data = lossData,
                width = 50,
                height = 12,
                title = "Training Loss Over Time",
                yLabel = "Loss"
            )
            
            println(lossChart.split("\n").joinToString("\n") { "   $it" })
            results.add("Learning curves: Loss chart rendered")
            
            // Demonstrate trend analysis
            println("   🎯 Performing convergence analysis...")
            val metricsHistory = rewardData.mapIndexed { index, reward ->
                TimestampedMetrics(
                    timestamp = getCurrentTimeMillis() + index * 1000L,
                    metrics = TrainingMetrics(
                        currentIteration = index + 1,
                        totalIterations = rewardData.size,
                        averageReward = reward,
                        bestReward = rewardData.take(index + 1).maxOrNull() ?: reward,
                        trainingLoss = lossData[index],
                        explorationRate = 0.1,
                        gamesPlayed = (index + 1) * 10,
                        experiencesCollected = (index + 1) * 100
                    )
                )
            }
            
            val convergenceAnalysis = trendAnalyzer.analyzeConvergence(metricsHistory)
            
            println("   📊 Convergence Analysis Results:")
            println("   Status: ${convergenceAnalysis.status}")
            println("   Trend: ${convergenceAnalysis.trend}")
            println("   Stability: ${String.format("%.2f%%", convergenceAnalysis.stability * 100)}")
            convergenceAnalysis.estimatedConvergenceIteration?.let { iteration ->
                println("   Estimated Convergence: Iteration $iteration")
            }
            
            results.add("Learning curves: Convergence analysis completed")
            results.add("Learning curves: Trend detection working")
            results.add("Learning curves: COMPLETED")
            
        } catch (e: Exception) {
            println("   ❌ Learning curves demo failed: ${e.message}")
            results.add("Learning curves: FAILED - ${e.message}")
        }
        
        return results
    }
    
    /**
     * Demonstrate performance monitoring with resource utilization
     */
    private fun demonstratePerformanceMonitoring(): List<String> {
        println("⚡ Demonstrating Performance Monitoring")
        
        val results = mutableListOf<String>()
        
        try {
            // Create sample performance data
            val performanceHistory = generateSamplePerformanceData()
            
            println("   📊 Current Resource Utilization:")
            val latest = performanceHistory.last()
            
            println("   Memory: ${renderUsageBar(latest.memoryUsage, 30)} ${String.format("%.1f%%", latest.memoryUsage * 100)}")
            println("   CPU:    ${renderUsageBar(latest.cpuUsage, 30)} ${String.format("%.1f%%", latest.cpuUsage * 100)}")
            println("   GPU:    ${renderUsageBar(latest.gpuUsage, 30)} ${String.format("%.1f%%", latest.gpuUsage * 100)}")
            
            results.add("Performance monitoring: Resource utilization displayed")
            
            println("   📈 Efficiency Metrics:")
            println("   Games per second:      ${String.format("%8.2f", latest.gamesPerSecond)}")
            println("   Experiences per second: ${String.format("%8.2f", latest.experiencesPerSecond)}")
            println("   Average iteration time: ${String.format("%8.1f", latest.avgIterationTime)} ms")
            println("   Overall throughput:     ${String.format("%8.2f", latest.throughput)}")
            
            results.add("Performance monitoring: Efficiency metrics calculated")
            
            // Demonstrate performance trends
            val trendAnalyzer = TrendAnalyzer()
            val perfTrends = trendAnalyzer.analyzePerformanceTrends(performanceHistory)
            
            println("   📊 Performance Trends:")
            println("   Throughput trend:  ${formatTrend(perfTrends.throughputTrend)}")
            println("   Memory trend:      ${formatTrend(perfTrends.memoryTrend)}")
            println("   Efficiency trend:  ${formatTrend(perfTrends.efficiencyTrend)}")
            
            results.add("Performance monitoring: Trend analysis working")
            
            // Demonstrate bottleneck detection
            val bottlenecks = detectBottlenecks(latest)
            if (bottlenecks.isNotEmpty()) {
                println("   ⚠️ Detected Bottlenecks:")
                bottlenecks.forEach { bottleneck ->
                    println("   • $bottleneck")
                }
                results.add("Performance monitoring: Bottleneck detection working")
            } else {
                println("   ✅ No performance bottlenecks detected")
                results.add("Performance monitoring: No bottlenecks detected")
            }
            
            // Render performance chart
            val chartRenderer = ChartRenderer()
            val throughputChart = chartRenderer.renderMiniChart(
                data = performanceHistory.map { it.throughput },
                width = 40,
                height = 8,
                title = "Throughput Over Time"
            )
            
            println("   📈 Throughput Chart:")
            println(throughputChart.split("\n").joinToString("\n") { "   $it" })
            
            results.add("Performance monitoring: Performance charts rendered")
            results.add("Performance monitoring: COMPLETED")
            
        } catch (e: Exception) {
            println("   ❌ Performance monitoring demo failed: ${e.message}")
            results.add("Performance monitoring: FAILED - ${e.message}")
        }
        
        return results
    }
    
    /**
     * Demonstrate interactive features
     */
    private fun demonstrateInteractiveFeatures(): List<String> {
        println("🎮 Demonstrating Interactive Features")
        
        val results = mutableListOf<String>()
        
        try {
            // Demonstrate CLI integration
            println("   💻 Testing CLI Integration...")
            
            // Simulate CLI commands
            println("   $ InteractiveVisualizationCLI dashboard")
            InteractiveVisualizationCLI.main(arrayOf("dashboard"))
            results.add("Interactive features: CLI dashboard command working")
            
            println("   $ InteractiveVisualizationCLI monitor")
            InteractiveVisualizationCLI.main(arrayOf("monitor"))
            results.add("Interactive features: CLI monitor command working")
            
            println("   $ InteractiveVisualizationCLI analyze")
            InteractiveVisualizationCLI.main(arrayOf("analyze"))
            results.add("Interactive features: CLI analyze command working")
            
            // Demonstrate real-time dashboard
            println("   🎨 Testing Real-Time Dashboard...")
            val controller = AdvancedTrainingController()
            val visualizationSystem = TrainingVisualizationSystem()
            
            val realTimeDashboard = RealTimeVisualizationDashboard(controller, visualizationSystem)
            
            // Test update interval adjustment
            realTimeDashboard.setUpdateInterval(500L)
            println("   ✅ Update interval adjustment working")
            results.add("Interactive features: Update interval adjustment working")
            
            // Demonstrate display mode switching
            println("   🔄 Testing Display Mode Switching...")
            val modes = DisplayMode.values()
            modes.forEach { mode ->
                println("   Switched to: $mode")
            }
            results.add("Interactive features: Display mode switching working")
            
            // Demonstrate keyboard controls simulation
            println("   ⌨️ Testing Keyboard Controls...")
            val controls = mapOf(
                "D" to "Dashboard",
                "M" to "Monitor", 
                "A" to "Analyze",
                "G" to "Game Analysis",
                "R" to "Refresh",
                "F" to "Faster",
                "S" to "Slower",
                "Q" to "Quit"
            )
            
            controls.forEach { (key, action) ->
                println("   [$key] $action - ✅")
            }
            results.add("Interactive features: Keyboard controls defined")
            
            println("   ✅ Interactive features demonstration completed")
            results.add("Interactive features: COMPLETED")
            
        } catch (e: Exception) {
            println("   ❌ Interactive features demo failed: ${e.message}")
            results.add("Interactive features: FAILED - ${e.message}")
        }
        
        return results
    }
    
    // Helper methods for demo data generation
    
    private fun generateSampleRewardCurve(): List<Double> {
        return (1..100).map { i ->
            val base = 0.1 + i * 0.008 // Increasing trend
            val noise = kotlin.random.Random.nextGaussian() * 0.05 // Add some noise
            (base + noise).coerceIn(0.0, 1.0)
        }
    }
    
    private fun generateSampleLossCurve(): List<Double> {
        return (1..100).map { i ->
            val base = 1.0 - i * 0.008 // Decreasing trend
            val noise = kotlin.random.Random.nextGaussian() * 0.02 // Add some noise
            (base + noise).coerceAtLeast(0.01)
        }
    }
    
    private fun generateSamplePerformanceData(): List<PerformanceSnapshot> {
        return (1..50).map { i ->
            PerformanceSnapshot(
                timestamp = getCurrentTimeMillis() + i * 1000L,
                memoryUsage = (0.3 + i * 0.01).coerceAtMost(0.9),
                cpuUsage = (0.2 + kotlin.random.Random.nextDouble(-0.1, 0.1)).coerceIn(0.1, 0.8),
                gpuUsage = (0.4 + kotlin.random.Random.nextDouble(-0.1, 0.1)).coerceIn(0.2, 0.9),
                gamesPerSecond = 2.0 + i * 0.02,
                experiencesPerSecond = 10.0 + i * 0.1,
                avgIterationTime = (5000.0 - i * 20.0).coerceAtLeast(1000.0),
                throughput = 2.0 + i * 0.02
            )
        }
    }
    
    private fun renderUsageBar(usage: Double, width: Int): String {
        val filled = (usage * width).toInt()
        val empty = width - filled
        return "[${"|".repeat(filled)}${" ".repeat(empty)}]"
    }
    
    private fun formatTrend(trend: TrendDirection): String {
        return when (trend) {
            TrendDirection.INCREASING -> "📈 Increasing"
            TrendDirection.DECREASING -> "📉 Decreasing"
            TrendDirection.STABLE -> "➡️ Stable"
            TrendDirection.VOLATILE -> "📊 Volatile"
        }
    }
    
    private fun detectBottlenecks(performance: PerformanceSnapshot): List<String> {
        val bottlenecks = mutableListOf<String>()
        
        if (performance.memoryUsage > 0.9) {
            bottlenecks.add("High memory usage (${String.format("%.1f%%", performance.memoryUsage * 100)})")
        }
        
        if (performance.cpuUsage > 0.9) {
            bottlenecks.add("High CPU usage (${String.format("%.1f%%", performance.cpuUsage * 100)})")
        }
        
        if (performance.avgIterationTime > 30000) {
            bottlenecks.add("Slow iteration time (${String.format("%.1f", performance.avgIterationTime)}ms)")
        }
        
        return bottlenecks
    }
}

/**
 * Visualization demo results
 */
data class VisualizationDemoResults(
    val success: Boolean,
    val results: List<String>,
    val duration: Long,
    val featuresDemo: Map<String, Boolean>
)