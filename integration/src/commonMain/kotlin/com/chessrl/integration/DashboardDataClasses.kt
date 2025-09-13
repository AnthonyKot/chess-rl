package com.chessrl.integration

import com.chessrl.chess.*

/**
 * Data classes for the interactive training dashboard
 */

// Configuration classes

/**
 * Configuration for the dashboard
 */
data class DashboardConfig(
    val minUpdateInterval: Long = 1000L, // Minimum time between updates (ms)
    val maxHistorySize: Int = 100, // Maximum number of metrics to keep in history
    val defaultView: DashboardView = DashboardView.OVERVIEW,
    val enableAutoUpdate: Boolean = true,
    val autoUpdateInterval: Long = 5000L, // Auto-update interval (ms)
    val maxCommandHistory: Int = 50, // Maximum number of commands to keep in history
    val enableColorOutput: Boolean = true,
    val chartWidth: Int = 60, // Width of ASCII charts
    val progressBarWidth: Int = 50 // Width of progress bars
)

// Enums

/**
 * Dashboard views
 */
enum class DashboardView {
    OVERVIEW,       // General overview of training
    TRAINING,       // Training-focused metrics and progress
    GAMES,          // Game analysis and quality metrics
    ANALYSIS,       // Position and move analysis
    PERFORMANCE,    // Performance monitoring and resource usage
    HELP            // Help and command reference
}

// Session classes

/**
 * Dashboard session information
 */
data class DashboardSession(
    val sessionId: String,
    val startTime: Long,
    var isActive: Boolean
)

/**
 * Dashboard metrics snapshot
 */
data class DashboardMetrics(
    val timestamp: Long,
    val sessionInfo: SessionInfo,
    val currentStats: CurrentStatistics,
    val recentTrends: TrendAnalysis,
    val systemHealth: SystemHealth,
    val performanceMetrics: PerformanceMetrics,
    val gameQualityMetrics: GameQualityMetrics,
    val trainingEfficiency: TrainingEfficiency
)

// Command classes

/**
 * Dashboard commands
 */
sealed class DashboardCommand {
    abstract val type: String
    
    object Help : DashboardCommand() {
        override val type = "help"
    }
    
    data class View(
        val view: DashboardView,
        override val type: String = "view"
    ) : DashboardCommand()
    
    data class Analyze(
        val target: String,
        override val type: String = "analyze"
    ) : DashboardCommand()
    
    data class Play(
        val color: PieceColor,
        override val type: String = "play"
    ) : DashboardCommand()
    
    data class Export(
        val format: String,
        val path: String,
        override val type: String = "export"
    ) : DashboardCommand()
    
    data class Configure(
        val setting: String,
        val value: String,
        override val type: String = "configure"
    ) : DashboardCommand()
    
    data class Training(
        val action: String,
        override val type: String = "training"
    ) : DashboardCommand()
    
    object Clear : DashboardCommand() {
        override val type = "clear"
    }
    
    object Quit : DashboardCommand() {
        override val type = "quit"
    }
    
    data class Unknown(
        val input: String,
        override val type: String = "unknown"
    ) : DashboardCommand()
}

/**
 * Command execution record
 */
data class DashboardCommand(
    val type: String,
    val timestamp: Long,
    val input: String
)

// Result classes

/**
 * Dashboard update result
 */
sealed class DashboardUpdateResult {
    data class Success(
        val updateCount: Int,
        val timeSinceLastUpdate: Long
    ) : DashboardUpdateResult()
    
    data class TooSoon(
        val timeSinceLastUpdate: Long
    ) : DashboardUpdateResult()
    
    data class Error(
        val message: String
    ) : DashboardUpdateResult()
    
    object NotActive : DashboardUpdateResult()
}

/**
 * Command execution result
 */
sealed class CommandResult {
    data class Success(
        val message: String,
        val data: Any? = null
    ) : CommandResult()
    
    data class Error(
        val message: String
    ) : CommandResult()
}

/**
 * Dashboard stop result
 */
sealed class DashboardStopResult {
    data class Success(
        val sessionDuration: Long,
        val totalUpdates: Int,
        val commandsExecuted: Int
    ) : DashboardStopResult()
    
    object NotActive : DashboardStopResult()
}

// Display classes

/**
 * ASCII chart configuration
 */
data class ChartConfig(
    val width: Int = 60,
    val height: Int = 10,
    val showAxes: Boolean = true,
    val showGrid: Boolean = false,
    val title: String? = null
)

/**
 * Progress bar configuration
 */
data class ProgressBarConfig(
    val width: Int = 50,
    val showPercentage: Boolean = true,
    val filledChar: Char = '█',
    val emptyChar: Char = '░'
)

/**
 * Color scheme for dashboard output
 */
data class ColorScheme(
    val primary: String = "\u001B[36m",    // Cyan
    val secondary: String = "\u001B[35m",  // Magenta
    val success: String = "\u001B[32m",    // Green
    val warning: String = "\u001B[33m",    // Yellow
    val error: String = "\u001B[31m",      // Red
    val reset: String = "\u001B[0m"        // Reset
)

// Analysis classes

/**
 * Game analysis summary for dashboard
 */
data class GameAnalysisSummary(
    val gameId: String,
    val gameLength: Int,
    val gameResult: String,
    val qualityScore: Double,
    val analysisAvailable: Boolean,
    val keyInsights: List<String>
)

/**
 * Position analysis summary for dashboard
 */
data class PositionAnalysisSummary(
    val position: String,
    val evaluation: Double,
    val bestMove: String,
    val confidence: Double,
    val complexity: Double
)

/**
 * Agent decision summary for dashboard
 */
data class AgentDecisionSummary(
    val topMoves: List<MoveOption>,
    val positionEvaluation: Double,
    val decisionConfidence: Double,
    val policyEntropy: Double
)

/**
 * Move option for agent decisions
 */
data class MoveOption(
    val move: String,
    val probability: Double,
    val qValue: Double,
    val rank: Int
)

// Visualization classes

/**
 * Learning curve data
 */
data class LearningCurveData(
    val metric: String,
    val values: List<Double>,
    val timestamps: List<Long>,
    val trend: Double,
    val stability: Double
)

/**
 * Performance trend data
 */
data class PerformanceTrendData(
    val scores: List<Double>,
    val winRates: List<Double>,
    val gameQualities: List<Double>,
    val efficiencies: List<Double>,
    val timestamps: List<Long>
)

/**
 * Resource utilization data
 */
data class ResourceUtilizationData(
    val cpuUsage: Double,
    val memoryUsage: Double,
    val gpuUsage: Double,
    val diskUsage: Double,
    val networkUsage: Double,
    val timestamp: Long
)

// Interactive features classes

/**
 * Human vs agent game state for dashboard
 */
data class HumanVsAgentGameState(
    val sessionId: String,
    val humanColor: PieceColor,
    val currentPosition: String,
    val moveHistory: List<String>,
    val gameStatus: String,
    val isHumanTurn: Boolean,
    val lastAgentMove: String?,
    val agentThinking: Boolean = false
)

/**
 * Training experiment comparison
 */
data class ExperimentComparison(
    val experiments: List<ExperimentSummary>,
    val comparisonMetrics: List<String>,
    val bestPerforming: String,
    val recommendations: List<String>
)

/**
 * Experiment summary for comparison
 */
data class ExperimentSummary(
    val experimentId: String,
    val name: String,
    val config: Map<String, Any>,
    val finalScore: Double,
    val trainingTime: Long,
    val convergenceTime: Long?,
    val status: String
)

// Utility classes

/**
 * Dashboard statistics
 */
data class DashboardStatistics(
    val totalUpdates: Int,
    val averageUpdateTime: Long,
    val commandsExecuted: Int,
    val viewSwitches: Int,
    val analysisRequests: Int,
    val exportRequests: Int
)

/**
 * Dashboard preferences
 */
data class DashboardPreferences(
    val defaultView: DashboardView,
    val autoUpdateEnabled: Boolean,
    val updateInterval: Long,
    val colorEnabled: Boolean,
    val soundEnabled: Boolean,
    val compactMode: Boolean
)

/**
 * Dashboard alert
 */
data class DashboardAlert(
    val type: AlertType,
    val message: String,
    val timestamp: Long,
    val acknowledged: Boolean = false
)

/**
 * Alert types
 */
enum class AlertType {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}

// Extension functions for dashboard formatting

/**
 * Format number with appropriate precision
 */
fun Double.formatForDashboard(precision: Int = 3): String {
    return when {
        this >= 1000 -> "%.1fk".format(this / 1000)
        this >= 1 -> "%.${precision}f".format(this)
        this >= 0.001 -> "%.${precision}f".format(this)
        else -> "%.2e".format(this)
    }
}

/**
 * Format percentage for dashboard
 */
fun Double.formatPercentage(): String {
    return "${(this * 100).toInt()}%"
}

/**
 * Format duration for dashboard
 */
fun Long.formatDuration(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

/**
 * Create trend indicator
 */
fun Double.toTrendIndicator(): String {
    return when {
        this > 0.01 -> "↗"
        this < -0.01 -> "↘"
        else -> "→"
    }
}

/**
 * Create health status indicator
 */
fun HealthStatus.toIndicator(): String {
    return when (this) {
        HealthStatus.HEALTHY -> "🟢"
        HealthStatus.WARNING -> "🟡"
        HealthStatus.CRITICAL -> "🔴"
    }
}

/**
 * Create ASCII bar chart
 */
fun List<Double>.toAsciiChart(width: Int = 60): String {
    if (isEmpty()) return "No data".padEnd(width)
    
    val min = minOrNull() ?: 0.0
    val max = maxOrNull() ?: 1.0
    val range = max - min
    
    if (range == 0.0) return "─".repeat(width)
    
    val normalized = map { ((it - min) / range * 8).toInt().coerceIn(0, 8) }
    val chars = "▁▂▃▄▅▆▇█"
    
    return normalized.map { chars[it] }.joinToString("").take(width).padEnd(width)
}

/**
 * Create ASCII progress bar
 */
fun Double.toProgressBar(width: Int = 50, filledChar: Char = '█', emptyChar: Char = '░'): String {
    val progress = this.coerceIn(0.0, 1.0)
    val filled = (progress * width).toInt()
    val empty = width - filled
    return filledChar.toString().repeat(filled) + emptyChar.toString().repeat(empty)
}

/**
 * Create usage bar with color coding
 */
fun Double.toUsageBar(width: Int = 20): String {
    val usage = this.coerceIn(0.0, 100.0)
    val filled = (usage / 100.0 * width).toInt()
    val empty = width - filled
    
    val char = when {
        usage >= 90 -> "█" // Critical
        usage >= 70 -> "▓" // High
        usage >= 50 -> "▒" // Medium
        else -> "░" // Low
    }
    
    return char.repeat(filled) + "░".repeat(empty)
}

/**
 * Pad string to center alignment
 */
fun String.padCenter(width: Int, padChar: Char = ' '): String {
    if (length >= width) return this
    val padding = width - length
    val leftPad = padding / 2
    val rightPad = padding - leftPad
    return padChar.toString().repeat(leftPad) + this + padChar.toString().repeat(rightPad)
}

/**
 * Truncate string with ellipsis
 */
fun String.truncateWithEllipsis(maxLength: Int): String {
    return if (length <= maxLength) this else take(maxLength - 3) + "..."
}

/**
 * Format large numbers with units
 */
fun Int.formatWithUnits(): String {
    return when {
        this >= 1_000_000 -> "%.1fM".format(this / 1_000_000.0)
        this >= 1_000 -> "%.1fK".format(this / 1_000.0)
        else -> toString()
    }
}

/**
 * Create box drawing characters for tables
 */
object BoxDrawing {
    const val HORIZONTAL = "─"
    const val VERTICAL = "│"
    const val TOP_LEFT = "┌"
    const val TOP_RIGHT = "┐"
    const val BOTTOM_LEFT = "└"
    const val BOTTOM_RIGHT = "┘"
    const val CROSS = "┼"
    const val T_DOWN = "┬"
    const val T_UP = "┴"
    const val T_RIGHT = "├"
    const val T_LEFT = "┤"
    
    fun horizontalLine(width: Int) = HORIZONTAL.repeat(width)
    fun verticalLine(height: Int) = (1..height).map { VERTICAL }.joinToString("\n")
}

/**
 * ANSI color codes for terminal output
 */
object AnsiColors {
    const val RESET = "\u001B[0m"
    const val BLACK = "\u001B[30m"
    const val RED = "\u001B[31m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val BLUE = "\u001B[34m"
    const val PURPLE = "\u001B[35m"
    const val CYAN = "\u001B[36m"
    const val WHITE = "\u001B[37m"
    
    const val BRIGHT_BLACK = "\u001B[90m"
    const val BRIGHT_RED = "\u001B[91m"
    const val BRIGHT_GREEN = "\u001B[92m"
    const val BRIGHT_YELLOW = "\u001B[93m"
    const val BRIGHT_BLUE = "\u001B[94m"
    const val BRIGHT_PURPLE = "\u001B[95m"
    const val BRIGHT_CYAN = "\u001B[96m"
    const val BRIGHT_WHITE = "\u001B[97m"
    
    const val BG_BLACK = "\u001B[40m"
    const val BG_RED = "\u001B[41m"
    const val BG_GREEN = "\u001B[42m"
    const val BG_YELLOW = "\u001B[43m"
    const val BG_BLUE = "\u001B[44m"
    const val BG_PURPLE = "\u001B[45m"
    const val BG_CYAN = "\u001B[46m"
    const val BG_WHITE = "\u001B[47m"
    
    const val BOLD = "\u001B[1m"
    const val UNDERLINE = "\u001B[4m"
    const val REVERSED = "\u001B[7m"
}