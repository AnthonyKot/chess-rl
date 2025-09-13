package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.math.*

/**
 * Real-time monitoring interface for sophisticated progress observation.
 * 
 * Provides:
 * - Console-based dashboard with comprehensive metrics display
 * - Training status with detailed progress indicators and time estimates
 * - Interactive commands for training control (pause/resume/stop/restart/configure)
 * - Live performance visualization and trend analysis
 */
class RealTimeMonitor(
    private val config: RealTimeMonitorConfig
) {
    
    // Monitor state
    private var isRunning = false
    private var isPaused = false
    private var startTime = 0L
    
    // Current metrics and trends
    private var currentMetrics: TrainingCycleMetrics? = null
    private var currentIssues: List<DetectedIssue> = emptyList()
    private val metricsBuffer = mutableListOf<TrainingCycleMetrics>()
    
    // Interactive command system
    private val commandProcessor = InteractiveCommandProcessor()
    private var commandMode = false
    
    // Display state
    private var lastDisplayUpdate = 0L
    private val displayBuffer = mutableListOf<String>()
    
    /**
     * Start real-time monitoring
     */
    fun start() {
        if (isRunning) {
            throw IllegalStateException("Real-time monitor is already running")
        }
        
        isRunning = true
        isPaused = false
        startTime = getCurrentTimeMillis()
        
        println("🔴 Real-Time Training Monitor Started")
        println("=" * 80)
        
        if (config.enableInteractiveCommands) {
            println("💡 Interactive commands available:")
            println("   Type 'help' for command list")
            println("   Type 'dashboard' to show current dashboard")
            println("   Type 'pause', 'resume', 'stop' for training control")
            println("=" * 80)
        }
        
        // Initialize display
        initializeDisplay()
    }
    
    /**
     * Update metrics and refresh display
     */
    fun updateMetrics(metrics: TrainingCycleMetrics, issues: List<DetectedIssue>) {
        if (!isRunning) return
        
        currentMetrics = metrics
        currentIssues = issues
        metricsBuffer.add(metrics)
        
        // Limit buffer size
        if (metricsBuffer.size > 100) {
            metricsBuffer.removeAt(0)
        }
        
        // Update display if enough time has passed
        val currentTime = getCurrentTimeMillis()
        if (currentTime - lastDisplayUpdate >= config.updateInterval) {
            refreshDisplay()
            lastDisplayUpdate = currentTime
        }
    }
    
    /**
     * Pause monitoring
     */
    fun pause() {
        if (!isRunning || isPaused) return
        
        isPaused = true
        println("\n⏸️ Real-time monitoring paused")
        displayPausedStatus()
    }
    
    /**
     * Resume monitoring
     */
    fun resume() {
        if (!isRunning || !isPaused) return
        
        isPaused = false
        println("\n▶️ Real-time monitoring resumed")
        refreshDisplay()
    }
    
    /**
     * Stop monitoring
     */
    fun stop() {
        if (!isRunning) return
        
        isRunning = false
        isPaused = false
        
        val totalTime = getCurrentTimeMillis() - startTime
        println("\n🛑 Real-time monitoring stopped")
        println("Total monitoring time: ${formatDuration(totalTime)}")
        
        // Display final summary
        displayFinalSummary()
    }
    
    /**
     * Process interactive command
     */
    fun processCommand(command: String): String {
        if (!config.enableInteractiveCommands) {
            return "Interactive commands are disabled"
        }
        
        return commandProcessor.processCommand(command, this)
    }
    
    /**
     * Get current dashboard
     */
    fun getCurrentDashboard(): String {
        return generateDashboard()
    }
    
    /**
     * Get training status summary
     */
    fun getStatusSummary(): String {
        val metrics = currentMetrics ?: return "No metrics available"
        val elapsedTime = getCurrentTimeMillis() - startTime
        
        return buildString {
            appendLine("📊 Training Status Summary")
            appendLine("=" * 40)
            appendLine("Cycle: ${metrics.cycle}")
            appendLine("Elapsed Time: ${formatDuration(elapsedTime)}")
            appendLine("Win Rate: ${formatPercentage(metrics.winRate)}")
            appendLine("Average Reward: ${formatDecimal(metrics.averageReward, 3)}")
            appendLine("Game Quality: ${formatDecimal(metrics.averageGameQuality, 3)}")
            appendLine("Training Efficiency: ${formatDecimal(metrics.trainingEfficiency, 3)}")
            
            if (currentIssues.isNotEmpty()) {
                appendLine("\n⚠️ Active Issues: ${currentIssues.size}")
                currentIssues.take(3).forEach { issue ->
                    appendLine("  • ${issue.type}: ${issue.description}")
                }
            }
        }
    }
    
    // Private methods
    
    private fun initializeDisplay() {
        clearScreen()
        println(generateHeader())
        println("Initializing monitoring...")
    }
    
    private fun refreshDisplay() {
        if (isPaused) return
        
        clearScreen()
        println(generateDashboard())
        
        if (config.enableLiveVisualization) {
            println(generateVisualization())
        }
    }
    
    private fun generateDashboard(): String {
        val metrics = currentMetrics
        if (metrics == null) {
            return "No training data available yet..."
        }
        
        val elapsedTime = getCurrentTimeMillis() - startTime
        val trends = calculateTrends()
        
        return buildString {
            appendLine(generateHeader())
            appendLine()
            
            // Session info
            appendLine("📅 Session Information")
            appendLine("-" * 40)
            appendLine("Cycle: ${metrics.cycle}")
            appendLine("Elapsed Time: ${formatDuration(elapsedTime)}")
            appendLine("Status: ${if (isPaused) "PAUSED" else "RUNNING"}")
            appendLine("Games Played: ${metricsBuffer.sumOf { it.gamesPlayed }}")
            appendLine("Total Experiences: ${metricsBuffer.sumOf { it.experiencesCollected }}")
            appendLine()
            
            // Performance metrics
            appendLine("🎯 Performance Metrics")
            appendLine("-" * 40)
            appendLine("Win Rate: ${formatPercentage(metrics.winRate)} ${formatTrend(trends.winRateTrend)}")
            appendLine("Draw Rate: ${formatPercentage(metrics.drawRate)}")
            appendLine("Loss Rate: ${formatPercentage(metrics.lossRate)}")
            appendLine("Average Reward: ${formatDecimal(metrics.averageReward, 3)} ${formatTrend(trends.rewardTrend)}")
            appendLine("Reward Variance: ${formatDecimal(metrics.rewardVariance, 4)}")
            appendLine()
            
            // Game quality metrics
            appendLine("🎮 Game Quality Metrics")
            appendLine("-" * 40)
            appendLine("Average Quality: ${formatDecimal(metrics.averageGameQuality, 3)} ${formatTrend(trends.qualityTrend)}")
            appendLine("Move Accuracy: ${formatPercentage(metrics.moveAccuracy)}")
            appendLine("Strategic Depth: ${formatDecimal(metrics.strategicDepth, 3)}")
            appendLine("Tactical Accuracy: ${formatPercentage(metrics.tacticalAccuracy)}")
            appendLine("Average Game Length: ${formatDecimal(metrics.averageGameLength, 1)} moves")
            appendLine()
            
            // Training metrics
            appendLine("🧠 Training Metrics")
            appendLine("-" * 40)
            appendLine("Batch Updates: ${metrics.batchUpdates}")
            appendLine("Average Loss: ${formatDecimal(metrics.averageLoss, 4)} ${formatTrend(trends.lossTrend)}")
            appendLine("Loss Variance: ${formatDecimal(metrics.lossVariance, 6)}")
            appendLine("Policy Entropy: ${formatDecimal(metrics.policyEntropy, 3)}")
            appendLine("Gradient Norm: ${formatDecimal(metrics.gradientNorm, 3)}")
            appendLine()
            
            // Efficiency metrics
            appendLine("⚡ Efficiency Metrics")
            appendLine("-" * 40)
            appendLine("Training Efficiency: ${formatDecimal(metrics.trainingEfficiency, 3)}")
            appendLine("Throughput: ${formatDecimal(metrics.throughput, 2)} games/sec")
            appendLine("Resource Utilization: ${formatPercentage(metrics.resourceUtilization)}")
            appendLine()
            
            // Statistical significance
            metrics.statisticalSignificance?.let { stats ->
                appendLine("📈 Statistical Analysis")
                appendLine("-" * 40)
                appendLine("Sample Size: ${stats.sampleSize}")
                appendLine("Significance: ${if (stats.isSignificant) "YES" else "NO"} (p=${formatDecimal(stats.pValue, 4)})")
                appendLine("Effect Size: ${formatDecimal(stats.effectSize, 3)}")
                appendLine("Confidence Interval: [${formatDecimal(stats.confidenceInterval.lowerBound, 3)}, ${formatDecimal(stats.confidenceInterval.upperBound, 3)}]")
                appendLine()
            }
            
            // Active issues
            if (currentIssues.isNotEmpty()) {
                appendLine("⚠️ Active Issues (${currentIssues.size})")
                appendLine("-" * 40)
                currentIssues.take(5).forEach { issue ->
                    val severityIcon = when {
                        issue.severity > 0.8 -> "🔴"
                        issue.severity > 0.5 -> "🟡"
                        else -> "🟢"
                    }
                    appendLine("$severityIcon ${issue.type}: ${issue.description}")
                }
                if (currentIssues.size > 5) {
                    appendLine("... and ${currentIssues.size - 5} more issues")
                }
                appendLine()
            }
            
            // Progress indicators
            appendLine("📊 Progress Indicators")
            appendLine("-" * 40)
            appendLine("Convergence: ${generateProgressBar(calculateConvergenceProgress(), 20)}")
            appendLine("Performance: ${generateProgressBar(metrics.winRate, 20)}")
            appendLine("Quality: ${generateProgressBar(metrics.averageGameQuality, 20)}")
            appendLine()
            
            // Time estimates
            val estimates = calculateTimeEstimates()
            if (estimates.isNotEmpty()) {
                appendLine("⏱️ Time Estimates")
                appendLine("-" * 40)
                estimates.forEach { (label, estimate) ->
                    appendLine("$label: $estimate")
                }
                appendLine()
            }
            
            // Interactive commands
            if (config.enableInteractiveCommands) {
                appendLine("💻 Commands: help | dashboard | pause | resume | stop | trends | issues")
            }
            
            appendLine("=" * 80)
            appendLine("Last Update: ${formatTimestamp(getCurrentTimeMillis())}")
        }
    }
    
    private fun generateVisualization(): String {
        if (metricsBuffer.size < 2) return ""
        
        return buildString {
            appendLine("\n📈 Live Performance Visualization")
            appendLine("-" * 50)
            
            // Win rate trend
            appendLine("Win Rate Trend (last 20 cycles):")
            appendLine(generateSparkline(metricsBuffer.takeLast(20).map { it.winRate }))
            
            // Reward trend
            appendLine("Reward Trend (last 20 cycles):")
            appendLine(generateSparkline(metricsBuffer.takeLast(20).map { it.averageReward }))
            
            // Loss trend
            appendLine("Loss Trend (last 20 cycles):")
            appendLine(generateSparkline(metricsBuffer.takeLast(20).map { it.averageLoss }))
        }
    }
    
    private fun generateHeader(): String {
        return buildString {
            appendLine("🔍 CHESS RL TRAINING MONITOR")
            appendLine("=" * 80)
        }
    }
    
    private fun calculateTrends(): TrendData {
        if (metricsBuffer.size < 5) {
            return TrendData(0.0, 0.0, 0.0, 0.0)
        }
        
        val recent = metricsBuffer.takeLast(10)
        return TrendData(
            winRateTrend = calculateTrend(recent.map { it.winRate }),
            rewardTrend = calculateTrend(recent.map { it.averageReward }),
            qualityTrend = calculateTrend(recent.map { it.averageGameQuality }),
            lossTrend = calculateTrend(recent.map { it.averageLoss })
        )
    }
    
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
    
    private fun calculateConvergenceProgress(): Double {
        if (metricsBuffer.size < 10) return 0.0
        
        val recent = metricsBuffer.takeLast(10)
        val winRates = recent.map { it.winRate }
        val variance = calculateVariance(winRates)
        
        // Lower variance indicates better convergence
        return (1.0 - variance).coerceIn(0.0, 1.0)
    }
    
    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
    
    private fun calculateTimeEstimates(): Map<String, String> {
        val estimates = mutableMapOf<String, String>()
        
        if (metricsBuffer.size >= 5) {
            val recent = metricsBuffer.takeLast(5)
            val avgCycleTime = (recent.last().timestamp - recent.first().timestamp) / (recent.size - 1)
            
            // Estimate time to convergence
            val convergenceProgress = calculateConvergenceProgress()
            if (convergenceProgress < 0.9) {
                val remainingProgress = 1.0 - convergenceProgress
                val estimatedCycles = (remainingProgress * 100).toInt() // Rough estimate
                val estimatedTime = estimatedCycles * avgCycleTime
                estimates["Convergence"] = formatDuration(estimatedTime)
            }
            
            // Estimate time for next milestone
            val currentWinRate = metricsBuffer.last().winRate
            if (currentWinRate < 0.8) {
                val targetWinRate = 0.8
                val winRateTrend = calculateTrend(recent.map { it.winRate })
                if (winRateTrend > 0) {
                    val cyclesNeeded = ((targetWinRate - currentWinRate) / winRateTrend).toInt()
                    val timeNeeded = cyclesNeeded * avgCycleTime
                    estimates["80% Win Rate"] = formatDuration(timeNeeded)
                }
            }
        }
        
        return estimates
    }
    
    private fun displayPausedStatus() {
        println("\n" + "=" * 80)
        println("⏸️ TRAINING PAUSED")
        println("=" * 80)
        println("Current Status:")
        println(getStatusSummary())
        println("\nType 'resume' to continue monitoring")
        println("Type 'stop' to end monitoring")
        println("=" * 80)
    }
    
    private fun displayFinalSummary() {
        if (metricsBuffer.isEmpty()) return
        
        val firstMetrics = metricsBuffer.first()
        val lastMetrics = metricsBuffer.last()
        
        println("\n" + "=" * 80)
        println("📊 FINAL MONITORING SUMMARY")
        println("=" * 80)
        println("Total Cycles: ${lastMetrics.cycle}")
        println("Total Games: ${metricsBuffer.sumOf { it.gamesPlayed }}")
        println("Total Experiences: ${metricsBuffer.sumOf { it.experiencesCollected }}")
        println()
        println("Performance Improvement:")
        println("  Win Rate: ${formatPercentage(firstMetrics.winRate)} → ${formatPercentage(lastMetrics.winRate)}")
        println("  Average Reward: ${formatDecimal(firstMetrics.averageReward, 3)} → ${formatDecimal(lastMetrics.averageReward, 3)}")
        println("  Game Quality: ${formatDecimal(firstMetrics.averageGameQuality, 3)} → ${formatDecimal(lastMetrics.averageGameQuality, 3)}")
        println()
        println("Best Performance:")
        println("  Best Win Rate: ${formatPercentage(metricsBuffer.maxByOrNull { it.winRate }?.winRate ?: 0.0)}")
        println("  Best Reward: ${formatDecimal(metricsBuffer.maxByOrNull { it.averageReward }?.averageReward ?: 0.0, 3)}")
        println("  Best Quality: ${formatDecimal(metricsBuffer.maxByOrNull { it.averageGameQuality }?.averageGameQuality ?: 0.0, 3)}")
        println("=" * 80)
    }
    
    // Utility methods
    
    private fun clearScreen() {
        // Simple screen clearing for console
        repeat(50) { println() }
    }
    
    private fun formatPercentage(value: Double): String {
        return "${(value * 100).toInt()}%"
    }
    
    private fun formatDecimal(value: Double, places: Int): String {
        return "%.${places}f".format(value)
    }
    
    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        // Simplified timestamp formatting
        return "T+${formatDuration(timestamp - startTime)}"
    }
    
    private fun formatTrend(trend: Double): String {
        return when {
            trend > 0.01 -> "📈"
            trend < -0.01 -> "📉"
            else -> "➡️"
        }
    }
    
    private fun generateProgressBar(progress: Double, width: Int): String {
        val filled = (progress * width).toInt().coerceIn(0, width)
        val empty = width - filled
        return "[${"█".repeat(filled)}${" ".repeat(empty)}] ${formatPercentage(progress)}"
    }
    
    private fun generateSparkline(values: List<Double>): String {
        if (values.isEmpty()) return ""
        
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0
        val range = max - min
        
        if (range == 0.0) return "▄".repeat(values.size)
        
        val sparkChars = listOf(" ", "▁", "▂", "▃", "▄", "▅", "▆", "▇", "█")
        
        return values.map { value ->
            val normalized = (value - min) / range
            val index = (normalized * (sparkChars.size - 1)).toInt().coerceIn(0, sparkChars.size - 1)
            sparkChars[index]
        }.joinToString("")
    }
}

/**
 * Interactive command processor for real-time monitoring
 */
class InteractiveCommandProcessor {
    
    fun processCommand(command: String, monitor: RealTimeMonitor): String {
        val parts = command.trim().lowercase().split(" ")
        val cmd = parts[0]
        
        return when (cmd) {
            "help" -> getHelpText()
            "dashboard" -> monitor.getCurrentDashboard()
            "status" -> monitor.getStatusSummary()
            "pause" -> {
                monitor.pause()
                "Training monitoring paused"
            }
            "resume" -> {
                monitor.resume()
                "Training monitoring resumed"
            }
            "stop" -> {
                monitor.stop()
                "Training monitoring stopped"
            }
            "trends" -> {
                val metric = parts.getOrNull(1) ?: "all"
                "Showing trends for: $metric"
            }
            "issues" -> "Displaying current training issues"
            "clear" -> {
                repeat(50) { println() }
                "Screen cleared"
            }
            else -> "Unknown command: $command. Type 'help' for available commands."
        }
    }
    
    private fun getHelpText(): String {
        return buildString {
            appendLine("📖 Available Commands:")
            appendLine("-" * 30)
            appendLine("help        - Show this help text")
            appendLine("dashboard   - Show current dashboard")
            appendLine("status      - Show status summary")
            appendLine("pause       - Pause monitoring")
            appendLine("resume      - Resume monitoring")
            appendLine("stop        - Stop monitoring")
            appendLine("trends      - Show performance trends")
            appendLine("issues      - Show current issues")
            appendLine("clear       - Clear screen")
        }
    }
}

// Supporting data classes

private data class TrendData(
    val winRateTrend: Double,
    val rewardTrend: Double,
    val qualityTrend: Double,
    val lossTrend: Double
)