package com.chessrl.integration

import kotlin.math.*

/**
 * Real-time Training Visualization System
 * 
 * Features:
 * - Interactive training dashboard with comprehensive metrics
 * - Real-time game analysis with ASCII board display
 * - Learning curve visualization with convergence analysis
 * - Performance monitoring with resource utilization
 * - Trend detection and efficiency metrics
 */
class TrainingVisualizationSystem(
    private val config: VisualizationConfig = VisualizationConfig()
) {
    
    // Visualization state
    private var isActive = false
    private var currentDashboard: TrainingDashboard? = null
    
    // Data collectors
    private val metricsHistory = mutableListOf<TimestampedMetrics>()
    private val gameHistory = mutableListOf<GameVisualizationData>()
    private val performanceHistory = mutableListOf<PerformanceSnapshot>()
    
    // Visualization components
    private val progressDisplay = ProgressDisplay()
    private val boardVisualizer = ChessBoardVisualizer()
    private val chartRenderer = ChartRenderer()
    private val trendAnalyzer = TrendAnalyzer()
    
    /**
     * Start the visualization system
     */
    fun startVisualization(controller: AdvancedTrainingController): VisualizationResult {
        return try {
            isActive = true
            
            // Create main dashboard
            currentDashboard = TrainingDashboard(
                controller = controller,
                config = config
            )
            
            // Start data collection
            startDataCollection(controller)
            
            println("🎨 Training Visualization System Started")
            println("Dashboard: ${config.dashboardWidth}x${config.dashboardHeight}")
            println("Update interval: ${config.updateIntervalMs}ms")
            
            VisualizationResult.Success("Visualization system started successfully")
            
        } catch (e: Exception) {
            VisualizationResult.Error("Failed to start visualization: ${e.message}")
        }
    }
    
    /**
     * Stop the visualization system
     */
    fun stopVisualization(): VisualizationResult {
        return try {
            isActive = false
            currentDashboard = null
            
            println("🎨 Training Visualization System Stopped")
            VisualizationResult.Success("Visualization system stopped")
            
        } catch (e: Exception) {
            VisualizationResult.Error("Failed to stop visualization: ${e.message}")
        }
    }
    
    /**
     * Render the complete training dashboard
     */
    fun renderDashboard(): String {
        if (!isActive || currentDashboard == null) {
            return "Visualization system not active"
        }
        
        val dashboard = StringBuilder()
        
        // Header
        dashboard.append(renderHeader())
        dashboard.append("\n")
        
        // Main content area
        dashboard.append(renderMainContent())
        dashboard.append("\n")
        
        // Footer with controls
        dashboard.append(renderFooter())
        
        return dashboard.toString()
    }
    
    /**
     * Render interactive training dashboard
     */
    private fun renderMainContent(): String {
        val content = StringBuilder()
        
        // Split dashboard into sections
        val leftPanel = renderLeftPanel()
        val centerPanel = renderCenterPanel()
        val rightPanel = renderRightPanel()
        
        // Combine panels side by side
        val leftLines = leftPanel.split("\n")
        val centerLines = centerPanel.split("\n")
        val rightLines = rightPanel.split("\n")
        
        val maxLines = maxOf(leftLines.size, centerLines.size, rightLines.size)
        
        for (i in 0 until maxLines) {
            val leftLine = leftLines.getOrNull(i)?.padEnd(config.panelWidth) ?: " ".repeat(config.panelWidth)
            val centerLine = centerLines.getOrNull(i)?.padEnd(config.panelWidth) ?: " ".repeat(config.panelWidth)
            val rightLine = rightLines.getOrNull(i)?.padEnd(config.panelWidth) ?: " ".repeat(config.panelWidth)
            
            content.append("│$leftLine│$centerLine│$rightLine│\n")
        }
        
        return content.toString()
    }
    
    /**
     * Render left panel with training progress and metrics
     */
    private fun renderLeftPanel(): String {
        val panel = StringBuilder()
        
        panel.append("┌─ TRAINING PROGRESS ─────────────┐\n")
        
        // Progress bar
        val progress = progressDisplay.renderProgressBar()
        panel.append("│ $progress │\n")
        
        // Current metrics
        val metrics = getCurrentMetrics()
        panel.append("├─ CURRENT METRICS ──────────────┤\n")
        panel.append("│ Iteration: ${String.format("%6d", metrics.currentIteration)}/${metrics.totalIterations}        │\n")
        panel.append("│ Reward:    ${String.format("%8.4f", metrics.averageReward)}           │\n")
        panel.append("│ Best:      ${String.format("%8.4f", metrics.bestReward)}           │\n")
        panel.append("│ Loss:      ${String.format("%8.4f", metrics.trainingLoss)}           │\n")
        panel.append("│ Explore:   ${String.format("%8.4f", metrics.explorationRate)}           │\n")
        
        // Learning curve mini-chart
        panel.append("├─ LEARNING CURVE ───────────────┤\n")
        val learningCurve = chartRenderer.renderMiniChart(
            data = metricsHistory.takeLast(20).map { it.metrics.averageReward },
            width = config.panelWidth - 4,
            height = 8,
            title = "Reward"
        )
        learningCurve.split("\n").forEach { line ->
            panel.append("│ ${line.padEnd(config.panelWidth - 4)} │\n")
        }
        
        panel.append("└─────────────────────────────────┘")
        
        return panel.toString()
    }
    
    /**
     * Render center panel with game analysis and board display
     */
    private fun renderCenterPanel(): String {
        val panel = StringBuilder()
        
        panel.append("┌─ GAME ANALYSIS ─────────────────┐\n")
        
        // Current game board (if available)
        val currentGame = gameHistory.lastOrNull()
        if (currentGame != null) {
            panel.append("│ Current Game: ${currentGame.gameId.toString().padEnd(18)} │\n")
            panel.append("│ Move: ${currentGame.currentMove.toString().padEnd(26)} │\n")
            panel.append("├─ BOARD POSITION ───────────────┤\n")
            
            // ASCII chess board
            val boardDisplay = boardVisualizer.renderBoard(currentGame.boardState)
            boardDisplay.split("\n").forEach { line ->
                panel.append("│ ${line.padEnd(config.panelWidth - 4)} │\n")
            }
            
            panel.append("├─ MOVE EVALUATION ──────────────┤\n")
            panel.append("│ Best Move: ${currentGame.bestMove.padEnd(20)} │\n")
            panel.append("│ Evaluation: ${String.format("%6.3f", currentGame.evaluation).padEnd(18)} │\n")
            panel.append("│ Confidence: ${String.format("%6.1f%%", currentGame.confidence * 100).padEnd(18)} │\n")
            
        } else {
            panel.append("│ No active game                  │\n")
            panel.append("│                                 │\n")
            
            // Show placeholder board
            val emptyBoard = boardVisualizer.renderEmptyBoard()
            emptyBoard.split("\n").forEach { line ->
                panel.append("│ ${line.padEnd(config.panelWidth - 4)} │\n")
            }
        }
        
        panel.append("└─────────────────────────────────┘")
        
        return panel.toString()
    }
    
    /**
     * Render right panel with performance monitoring and trends
     */
    private fun renderRightPanel(): String {
        val panel = StringBuilder()
        
        panel.append("┌─ PERFORMANCE MONITOR ───────────┐\n")
        
        // Resource utilization
        val performance = getCurrentPerformance()
        panel.append("├─ RESOURCE USAGE ───────────────┤\n")
        panel.append("│ Memory:  ${renderUsageBar(performance.memoryUsage, 20)} │\n")
        panel.append("│ CPU:     ${renderUsageBar(performance.cpuUsage, 20)} │\n")
        panel.append("│ GPU:     ${renderUsageBar(performance.gpuUsage, 20)} │\n")
        
        // Efficiency metrics
        panel.append("├─ EFFICIENCY METRICS ───────────┤\n")
        panel.append("│ Games/sec:  ${String.format("%8.2f", performance.gamesPerSecond)}        │\n")
        panel.append("│ Exp/sec:    ${String.format("%8.2f", performance.experiencesPerSecond)}        │\n")
        panel.append("│ Iter Time:  ${String.format("%8.1f", performance.avgIterationTime)}ms      │\n")
        panel.append("│ Throughput: ${String.format("%8.2f", performance.throughput)}        │\n")
        
        // Trend analysis
        panel.append("├─ TREND ANALYSIS ───────────────┤\n")
        val trends = trendAnalyzer.analyzeTrends(metricsHistory.takeLast(50))
        panel.append("│ Reward Trend:   ${formatTrend(trends.rewardTrend).padEnd(14)} │\n")
        panel.append("│ Loss Trend:     ${formatTrend(trends.lossTrend).padEnd(14)} │\n")
        panel.append("│ Convergence:    ${formatConvergence(trends.convergenceStatus).padEnd(14)} │\n")
        
        // Performance chart
        panel.append("├─ PERFORMANCE CHART ────────────┤\n")
        val perfChart = chartRenderer.renderMiniChart(
            data = performanceHistory.takeLast(20).map { it.throughput },
            width = config.panelWidth - 4,
            height = 6,
            title = "Throughput"
        )
        perfChart.split("\n").forEach { line ->
            panel.append("│ ${line.padEnd(config.panelWidth - 4)} │\n")
        }
        
        panel.append("└─────────────────────────────────┘")
        
        return panel.toString()
    }
    
    /**
     * Render dashboard header
     */
    private fun renderHeader(): String {
        val header = StringBuilder()
        val totalWidth = config.dashboardWidth
        
        header.append("┌${"─".repeat(totalWidth - 2)}┐\n")
        
        val title = "🎮 CHESS RL TRAINING DASHBOARD 🎮"
        val padding = (totalWidth - title.length - 2) / 2
        header.append("│${" ".repeat(padding)}$title${" ".repeat(totalWidth - title.length - padding - 2)}│\n")
        
        val timestamp = "Last Update: ${formatCurrentTime()}"
        val statusText = if (isActive) "🟢 ACTIVE" else "🔴 INACTIVE"
        val headerInfo = "$timestamp | $statusText"
        val infoPadding = totalWidth - headerInfo.length - 2
        header.append("│$headerInfo${" ".repeat(infoPadding)}│\n")
        
        header.append("├${"─".repeat(config.panelWidth)}┬${"─".repeat(config.panelWidth)}┬${"─".repeat(config.panelWidth)}┤\n")
        
        return header.toString()
    }
    
    /**
     * Render dashboard footer with controls
     */
    private fun renderFooter(): String {
        val footer = StringBuilder()
        val totalWidth = config.dashboardWidth
        
        footer.append("├${"─".repeat(totalWidth - 2)}┤\n")
        
        val controls = "Controls: [R]efresh | [P]ause | [S]ave | [Q]uit | [H]elp"
        val controlsPadding = totalWidth - controls.length - 2
        footer.append("│$controls${" ".repeat(controlsPadding)}│\n")
        
        footer.append("└${"─".repeat(totalWidth - 2)}┘")
        
        return footer.toString()
    }
    
    /**
     * Start data collection from the training controller
     */
    private fun startDataCollection(controller: AdvancedTrainingController) {
        // In a real implementation, this would start a background thread
        // For now, we simulate data collection
        
        // Collect initial data
        collectMetrics(controller)
        collectPerformanceData(controller)
        collectGameData(controller)
    }
    
    /**
     * Collect training metrics
     */
    private fun collectMetrics(controller: AdvancedTrainingController) {
        val status = controller.getTrainingStatus()
        
        status.metrics?.let { metrics ->
            val timestampedMetrics = TimestampedMetrics(
                timestamp = getCurrentTimeMillis(),
                metrics = metrics
            )
            metricsHistory.add(timestampedMetrics)
            
            // Keep only recent history
            if (metricsHistory.size > config.maxHistorySize) {
                metricsHistory.removeAt(0)
            }
        }
    }
    
    /**
     * Collect performance data
     */
    private fun collectPerformanceData(controller: AdvancedTrainingController) {
        val status = controller.getTrainingStatus()
        
        status.performance?.let { perf ->
            val snapshot = PerformanceSnapshot(
                timestamp = getCurrentTimeMillis(),
                memoryUsage = perf.memoryUsage,
                cpuUsage = perf.cpuUsage,
                gpuUsage = kotlin.random.Random.nextDouble(0.3, 0.8), // Simulated
                gamesPerSecond = perf.throughput,
                experiencesPerSecond = perf.efficiency,
                avgIterationTime = perf.averageIterationTime.toDouble(),
                throughput = perf.throughput
            )
            performanceHistory.add(snapshot)
            
            // Keep only recent history
            if (performanceHistory.size > config.maxHistorySize) {
                performanceHistory.removeAt(0)
            }
        }
    }
    
    /**
     * Collect game visualization data
     */
    private fun collectGameData(controller: AdvancedTrainingController) {
        // Simulate current game data
        val gameData = GameVisualizationData(
            gameId = kotlin.random.Random.nextInt(1000, 9999),
            currentMove = kotlin.random.Random.nextInt(1, 50),
            boardState = generateSampleBoardState(),
            bestMove = generateSampleMove(),
            evaluation = kotlin.random.Random.nextDouble(-2.0, 2.0),
            confidence = kotlin.random.Random.nextDouble(0.5, 1.0)
        )
        
        gameHistory.add(gameData)
        
        // Keep only recent games
        if (gameHistory.size > 10) {
            gameHistory.removeAt(0)
        }
    }
    
    /**
     * Update visualization with new data
     */
    fun updateVisualization(controller: AdvancedTrainingController) {
        if (!isActive) return
        
        collectMetrics(controller)
        collectPerformanceData(controller)
        collectGameData(controller)
    }
    
    /**
     * Render learning curve visualization
     */
    fun renderLearningCurve(): String {
        val chart = StringBuilder()
        
        chart.append("📈 LEARNING CURVE ANALYSIS\n")
        chart.append("=" * 50 + "\n")
        
        if (metricsHistory.isEmpty()) {
            chart.append("No data available for learning curve\n")
            return chart.toString()
        }
        
        // Reward curve
        val rewardData = metricsHistory.map { it.metrics.averageReward }
        val rewardChart = chartRenderer.renderChart(
            data = rewardData,
            width = 60,
            height = 15,
            title = "Average Reward Over Time",
            yLabel = "Reward"
        )
        chart.append(rewardChart)
        chart.append("\n")
        
        // Loss curve
        val lossData = metricsHistory.map { it.metrics.trainingLoss }
        val lossChart = chartRenderer.renderChart(
            data = lossData,
            width = 60,
            height = 15,
            title = "Training Loss Over Time",
            yLabel = "Loss"
        )
        chart.append(lossChart)
        chart.append("\n")
        
        // Convergence analysis
        val convergenceAnalysis = trendAnalyzer.analyzeConvergence(metricsHistory)
        chart.append("🎯 CONVERGENCE ANALYSIS\n")
        chart.append("-" * 30 + "\n")
        chart.append("Status: ${convergenceAnalysis.status}\n")
        chart.append("Trend: ${convergenceAnalysis.trend}\n")
        chart.append("Stability: ${String.format("%.2f%%", convergenceAnalysis.stability * 100)}\n")
        chart.append("Estimated Convergence: ${convergenceAnalysis.estimatedConvergenceIteration ?: "Unknown"}\n")
        
        return chart.toString()
    }
    
    /**
     * Render performance monitoring dashboard
     */
    fun renderPerformanceMonitor(): String {
        val monitor = StringBuilder()
        
        monitor.append("⚡ PERFORMANCE MONITORING\n")
        monitor.append("=" * 40 + "\n")
        
        if (performanceHistory.isEmpty()) {
            monitor.append("No performance data available\n")
            return monitor.toString()
        }
        
        val latest = performanceHistory.last()
        
        // Current resource usage
        monitor.append("🖥️ RESOURCE UTILIZATION\n")
        monitor.append("-" * 25 + "\n")
        monitor.append("Memory: ${renderUsageBar(latest.memoryUsage, 30)} ${String.format("%.1f%%", latest.memoryUsage * 100)}\n")
        monitor.append("CPU:    ${renderUsageBar(latest.cpuUsage, 30)} ${String.format("%.1f%%", latest.cpuUsage * 100)}\n")
        monitor.append("GPU:    ${renderUsageBar(latest.gpuUsage, 30)} ${String.format("%.1f%%", latest.gpuUsage * 100)}\n")
        monitor.append("\n")
        
        // Efficiency metrics
        monitor.append("📊 EFFICIENCY METRICS\n")
        monitor.append("-" * 20 + "\n")
        monitor.append("Games per second:      ${String.format("%8.2f", latest.gamesPerSecond)}\n")
        monitor.append("Experiences per second: ${String.format("%8.2f", latest.experiencesPerSecond)}\n")
        monitor.append("Average iteration time: ${String.format("%8.1f", latest.avgIterationTime)} ms\n")
        monitor.append("Overall throughput:     ${String.format("%8.2f", latest.throughput)}\n")
        monitor.append("\n")
        
        // Performance trends
        val perfTrends = trendAnalyzer.analyzePerformanceTrends(performanceHistory)
        monitor.append("📈 PERFORMANCE TRENDS\n")
        monitor.append("-" * 20 + "\n")
        monitor.append("Throughput trend:  ${formatTrend(perfTrends.throughputTrend)}\n")
        monitor.append("Memory trend:      ${formatTrend(perfTrends.memoryTrend)}\n")
        monitor.append("Efficiency trend:  ${formatTrend(perfTrends.efficiencyTrend)}\n")
        
        // Bottleneck detection
        val bottlenecks = detectBottlenecks(latest)
        if (bottlenecks.isNotEmpty()) {
            monitor.append("\n⚠️ DETECTED BOTTLENECKS\n")
            monitor.append("-" * 20 + "\n")
            bottlenecks.forEach { bottleneck ->
                monitor.append("• $bottleneck\n")
            }
        }
        
        return monitor.toString()
    }
    
    // Helper methods
    
    private fun getCurrentMetrics(): TrainingMetrics {
        return metricsHistory.lastOrNull()?.metrics ?: TrainingMetrics(
            currentIteration = 0,
            totalIterations = 100,
            averageReward = 0.0,
            bestReward = 0.0,
            trainingLoss = 1.0,
            explorationRate = 0.1,
            gamesPlayed = 0,
            experiencesCollected = 0
        )
    }
    
    private fun getCurrentPerformance(): PerformanceSnapshot {
        return performanceHistory.lastOrNull() ?: PerformanceSnapshot(
            timestamp = getCurrentTimeMillis(),
            memoryUsage = 0.5,
            cpuUsage = 0.3,
            gpuUsage = 0.4,
            gamesPerSecond = 2.0,
            experiencesPerSecond = 10.0,
            avgIterationTime = 5000.0,
            throughput = 2.0
        )
    }
    
    private fun renderUsageBar(usage: Double, width: Int): String {
        val filled = (usage * width).toInt()
        val empty = width - filled
        return "[${"█".repeat(filled)}${" ".repeat(empty)}]"
    }
    
    private fun formatTrend(trend: TrendDirection): String {
        return when (trend) {
            TrendDirection.INCREASING -> "📈 Increasing"
            TrendDirection.DECREASING -> "📉 Decreasing"
            TrendDirection.STABLE -> "➡️ Stable"
            TrendDirection.VOLATILE -> "📊 Volatile"
        }
    }
    
    private fun formatConvergence(status: ConvergenceStatus): String {
        return when (status) {
            ConvergenceStatus.CONVERGING -> "🎯 Converging"
            ConvergenceStatus.CONVERGED -> "✅ Converged"
            ConvergenceStatus.DIVERGING -> "❌ Diverging"
            ConvergenceStatus.UNKNOWN -> "❓ Unknown"
        }
    }
    
    private fun formatCurrentTime(): String {
        return "Time: ${getCurrentTimeMillis()}"
    }
    
    private fun generateSampleBoardState(): String {
        // Generate a sample chess position
        return "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"
    }
    
    private fun generateSampleMove(): String {
        val moves = listOf("e2-e4", "d2-d4", "Nf3", "Nc3", "Bc4", "0-0")
        return moves.random()
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
        
        if (performance.gamesPerSecond < 1.0) {
            bottlenecks.add("Low game throughput (${String.format("%.2f", performance.gamesPerSecond)} games/sec)")
        }
        
        return bottlenecks
    }
}

// Data classes and enums follow.../*
*
 * Visualization configuration
 */
data class VisualizationConfig(
    val dashboardWidth: Int = 105, // 3 panels of 35 chars each
    val dashboardHeight: Int = 30,
    val panelWidth: Int = 33,
    val updateIntervalMs: Long = 1000L,
    val maxHistorySize: Int = 1000,
    val enableRealTimeUpdates: Boolean = true,
    val enableGameAnalysis: Boolean = true,
    val enablePerformanceMonitoring: Boolean = true
)

/**
 * Training dashboard
 */
data class TrainingDashboard(
    val controller: AdvancedTrainingController,
    val config: VisualizationConfig,
    val startTime: Long = getCurrentTimeMillis()
)

/**
 * Timestamped metrics for history tracking
 */
data class TimestampedMetrics(
    val timestamp: Long,
    val metrics: TrainingMetrics
)

/**
 * Performance snapshot for monitoring
 */
data class PerformanceSnapshot(
    val timestamp: Long,
    val memoryUsage: Double,
    val cpuUsage: Double,
    val gpuUsage: Double,
    val gamesPerSecond: Double,
    val experiencesPerSecond: Double,
    val avgIterationTime: Double,
    val throughput: Double
)

/**
 * Game visualization data
 */
data class GameVisualizationData(
    val gameId: Int,
    val currentMove: Int,
    val boardState: String, // FEN notation
    val bestMove: String,
    val evaluation: Double,
    val confidence: Double
)

/**
 * Trend analysis results
 */
data class TrendAnalysisResult(
    val rewardTrend: TrendDirection,
    val lossTrend: TrendDirection,
    val convergenceStatus: ConvergenceStatus,
    val stability: Double
)

/**
 * Performance trend analysis
 */
data class PerformanceTrendAnalysis(
    val throughputTrend: TrendDirection,
    val memoryTrend: TrendDirection,
    val efficiencyTrend: TrendDirection
)

/**
 * Convergence analysis result
 */
data class ConvergenceAnalysis(
    val status: ConvergenceStatus,
    val trend: TrendDirection,
    val stability: Double,
    val estimatedConvergenceIteration: Int?
)

/**
 * Trend direction enumeration
 */
enum class TrendDirection {
    INCREASING,
    DECREASING,
    STABLE,
    VOLATILE
}

/**
 * Convergence status enumeration
 */
enum class ConvergenceStatus {
    CONVERGING,
    CONVERGED,
    DIVERGING,
    UNKNOWN
}

/**
 * Visualization result
 */
sealed class VisualizationResult {
    data class Success(val message: String) : VisualizationResult()
    data class Error(val message: String) : VisualizationResult()
}

/**
 * Progress display component
 */
class ProgressDisplay {
    
    fun renderProgressBar(current: Int = 0, total: Int = 100, width: Int = 25): String {
        val progress = if (total > 0) current.toDouble() / total else 0.0
        val filled = (progress * width).toInt()
        val empty = width - filled
        
        val percentage = (progress * 100).toInt()
        return "[${"|".repeat(filled)}${" ".repeat(empty)}] $percentage%"
    }
    
    fun renderProgressBar(): String {
        // Get current progress from somewhere (simulated for now)
        val current = kotlin.random.Random.nextInt(0, 100)
        return renderProgressBar(current, 100, 20)
    }
}

/**
 * Chess board visualizer with ASCII display
 */
class ChessBoardVisualizer {
    
    fun renderBoard(fenPosition: String): String {
        val board = StringBuilder()
        
        // Parse FEN position (simplified)
        val position = fenPosition.split(" ")[0]
        val ranks = position.split("/")
        
        board.append("  a b c d e f g h\n")
        
        for ((rankIndex, rank) in ranks.withIndex()) {
            val rankNumber = 8 - rankIndex
            board.append("$rankNumber ")
            
            var fileIndex = 0
            for (char in rank) {
                when {
                    char.isDigit() -> {
                        // Empty squares
                        val emptyCount = char.toString().toInt()
                        repeat(emptyCount) {
                            val squareColor = if ((rankIndex + fileIndex) % 2 == 0) "." else " "
                            board.append("$squareColor ")
                            fileIndex++
                        }
                    }
                    else -> {
                        // Piece
                        board.append("$char ")
                        fileIndex++
                    }
                }
            }
            board.append("$rankNumber\n")
        }
        
        board.append("  a b c d e f g h")
        
        return board.toString()
    }
    
    fun renderEmptyBoard(): String {
        return renderBoard("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    }
}

/**
 * Chart renderer for metrics visualization
 */
class ChartRenderer {
    
    fun renderChart(
        data: List<Double>,
        width: Int = 50,
        height: Int = 10,
        title: String = "",
        yLabel: String = ""
    ): String {
        if (data.isEmpty()) return "No data to display"
        
        val chart = StringBuilder()
        
        // Title
        if (title.isNotEmpty()) {
            chart.append("$title\n")
            chart.append("-".repeat(title.length) + "\n")
        }
        
        // Normalize data to chart height
        val minValue = data.minOrNull() ?: 0.0
        val maxValue = data.maxOrNull() ?: 1.0
        val range = maxValue - minValue
        
        val normalizedData = if (range > 0) {
            data.map { ((it - minValue) / range * (height - 1)).toInt() }
        } else {
            data.map { height / 2 }
        }
        
        // Render chart from top to bottom
        for (row in (height - 1) downTo 0) {
            // Y-axis label
            if (yLabel.isNotEmpty() && row == height / 2) {
                chart.append("${yLabel.take(3).padEnd(3)} ")
            } else {
                chart.append("    ")
            }
            
            // Chart data
            for (i in normalizedData.indices) {
                val dataPoint = normalizedData[i]
                when {
                    dataPoint == row -> chart.append("█")
                    dataPoint > row -> chart.append("│")
                    else -> chart.append(" ")
                }
            }
            
            // Y-axis value
            val yValue = minValue + (row.toDouble() / (height - 1)) * range
            chart.append(" ${String.format("%.2f", yValue)}")
            chart.append("\n")
        }
        
        // X-axis
        chart.append("    " + "─".repeat(normalizedData.size) + "\n")
        
        return chart.toString()
    }
    
    fun renderMiniChart(
        data: List<Double>,
        width: Int = 20,
        height: Int = 5,
        title: String = ""
    ): String {
        if (data.isEmpty()) return "No data"
        
        val chart = StringBuilder()
        
        if (title.isNotEmpty()) {
            chart.append("$title\n")
        }
        
        // Simple sparkline-style chart
        val minValue = data.minOrNull() ?: 0.0
        val maxValue = data.maxOrNull() ?: 1.0
        val range = maxValue - minValue
        
        if (range == 0.0) {
            chart.append("─".repeat(width))
            return chart.toString()
        }
        
        // Take last 'width' data points
        val recentData = data.takeLast(width)
        
        for (row in (height - 1) downTo 0) {
            for (i in recentData.indices) {
                val normalizedValue = ((recentData[i] - minValue) / range * (height - 1)).toInt()
                when {
                    normalizedValue == row -> chart.append("█")
                    normalizedValue > row -> chart.append("│")
                    else -> chart.append(" ")
                }
            }
            chart.append("\n")
        }
        
        return chart.toString()
    }
}

/**
 * Trend analyzer for convergence detection
 */
class TrendAnalyzer {
    
    fun analyzeTrends(metricsHistory: List<TimestampedMetrics>): TrendAnalysisResult {
        if (metricsHistory.size < 10) {
            return TrendAnalysisResult(
                rewardTrend = TrendDirection.UNKNOWN,
                lossTrend = TrendDirection.UNKNOWN,
                convergenceStatus = ConvergenceStatus.UNKNOWN,
                stability = 0.0
            )
        }
        
        val rewardData = metricsHistory.map { it.metrics.averageReward }
        val lossData = metricsHistory.map { it.metrics.trainingLoss }
        
        return TrendAnalysisResult(
            rewardTrend = calculateTrend(rewardData),
            lossTrend = calculateTrend(lossData),
            convergenceStatus = analyzeConvergenceStatus(rewardData, lossData),
            stability = calculateStability(rewardData)
        )
    }
    
    fun analyzePerformanceTrends(performanceHistory: List<PerformanceSnapshot>): PerformanceTrendAnalysis {
        if (performanceHistory.size < 5) {
            return PerformanceTrendAnalysis(
                throughputTrend = TrendDirection.STABLE,
                memoryTrend = TrendDirection.STABLE,
                efficiencyTrend = TrendDirection.STABLE
            )
        }
        
        val throughputData = performanceHistory.map { it.throughput }
        val memoryData = performanceHistory.map { it.memoryUsage }
        val efficiencyData = performanceHistory.map { it.experiencesPerSecond }
        
        return PerformanceTrendAnalysis(
            throughputTrend = calculateTrend(throughputData),
            memoryTrend = calculateTrend(memoryData),
            efficiencyTrend = calculateTrend(efficiencyData)
        )
    }
    
    fun analyzeConvergence(metricsHistory: List<TimestampedMetrics>): ConvergenceAnalysis {
        if (metricsHistory.size < 20) {
            return ConvergenceAnalysis(
                status = ConvergenceStatus.UNKNOWN,
                trend = TrendDirection.UNKNOWN,
                stability = 0.0,
                estimatedConvergenceIteration = null
            )
        }
        
        val rewardData = metricsHistory.map { it.metrics.averageReward }
        val recentData = rewardData.takeLast(20)
        
        val stability = calculateStability(recentData)
        val trend = calculateTrend(recentData)
        
        val status = when {
            stability > 0.95 && trend == TrendDirection.STABLE -> ConvergenceStatus.CONVERGED
            stability > 0.8 && (trend == TrendDirection.INCREASING || trend == TrendDirection.STABLE) -> ConvergenceStatus.CONVERGING
            stability < 0.5 -> ConvergenceStatus.DIVERGING
            else -> ConvergenceStatus.UNKNOWN
        }
        
        val estimatedConvergence = if (status == ConvergenceStatus.CONVERGING) {
            estimateConvergenceIteration(rewardData)
        } else null
        
        return ConvergenceAnalysis(
            status = status,
            trend = trend,
            stability = stability,
            estimatedConvergenceIteration = estimatedConvergence
        )
    }
    
    private fun calculateTrend(data: List<Double>): TrendDirection {
        if (data.size < 5) return TrendDirection.UNKNOWN
        
        val recent = data.takeLast(10)
        val earlier = data.dropLast(10).takeLast(10)
        
        if (earlier.isEmpty()) return TrendDirection.STABLE
        
        val recentAvg = recent.average()
        val earlierAvg = earlier.average()
        val difference = recentAvg - earlierAvg
        val threshold = earlierAvg * 0.05 // 5% threshold
        
        // Calculate volatility
        val variance = recent.map { (it - recentAvg).pow(2) }.average()
        val volatilityThreshold = earlierAvg * 0.1
        
        return when {
            variance > volatilityThreshold -> TrendDirection.VOLATILE
            difference > threshold -> TrendDirection.INCREASING
            difference < -threshold -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }
    
    private fun calculateStability(data: List<Double>): Double {
        if (data.size < 5) return 0.0
        
        val mean = data.average()
        val variance = data.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        
        // Stability is inverse of coefficient of variation
        val coefficientOfVariation = if (mean != 0.0) stdDev / abs(mean) else 1.0
        return (1.0 / (1.0 + coefficientOfVariation)).coerceIn(0.0, 1.0)
    }
    
    private fun analyzeConvergenceStatus(rewardData: List<Double>, lossData: List<Double>): ConvergenceStatus {
        val rewardStability = calculateStability(rewardData.takeLast(20))
        val lossStability = calculateStability(lossData.takeLast(20))
        
        val overallStability = (rewardStability + lossStability) / 2.0
        
        return when {
            overallStability > 0.9 -> ConvergenceStatus.CONVERGED
            overallStability > 0.7 -> ConvergenceStatus.CONVERGING
            overallStability < 0.3 -> ConvergenceStatus.DIVERGING
            else -> ConvergenceStatus.UNKNOWN
        }
    }
    
    private fun estimateConvergenceIteration(rewardData: List<Double>): Int? {
        // Simple linear extrapolation based on recent trend
        if (rewardData.size < 10) return null
        
        val recent = rewardData.takeLast(10)
        val trend = calculateTrend(recent)
        
        return when (trend) {
            TrendDirection.INCREASING -> {
                // Estimate when improvement will plateau
                val currentIteration = rewardData.size
                val improvementRate = (recent.last() - recent.first()) / recent.size
                val estimatedIterations = if (improvementRate > 0) {
                    (0.1 / improvementRate).toInt() // Assume convergence when improvement < 0.1
                } else 50
                currentIteration + estimatedIterations
            }
            TrendDirection.STABLE -> rewardData.size // Already converged
            else -> null
        }
    }
}