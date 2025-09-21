package com.chessrl.integration.output

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Configuration for output system behavior
 */
data class OutputConfig(
    val logLevel: LogLevel = LogLevel.INFO,
    val outputMode: OutputMode = OutputMode.STANDARD,
    val logInterval: Int = 1,
    val summaryOnly: Boolean = false,
    val metricsExport: MetricsExportConfig? = null,
    val trendWindowSize: Int = 10,
    val decimalPlaces: Int = 2
) {
    init {
        require(logInterval > 0) { "Log interval must be positive" }
        require(trendWindowSize > 0) { "Trend window size must be positive" }
        require(decimalPlaces in 0..4) { "Decimal places must be between 0 and 4" }
    }
}

/**
 * Log levels for filtering output
 */
enum class LogLevel(val priority: Int) {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3)
}

/**
 * Output modes for different use cases
 */
enum class OutputMode {
    STANDARD,      // Normal one-line summaries with periodic detailed blocks
    SUMMARY_ONLY,  // Only final summary and checkpoints
    VERBOSE        // Detailed blocks every cycle
}

/**
 * Configuration for metrics export functionality
 */
data class MetricsExportConfig(
    val jsonLogs: Boolean = false,
    val csvMetrics: Boolean = false,
    val outputDirectory: String = "metrics",
    val includeTimestamps: Boolean = true
) {
    init {
        require(outputDirectory.isNotBlank()) { "Output directory cannot be blank" }
    }
}

/**
 * Data for cycle progress reporting
 */
data class CycleProgressData(
    val cycleNumber: Int,
    val totalCycles: Int,
    val gamesPlayed: Int,
    val winDrawLoss: Triple<Int, Int, Int>,
    val averageGameLength: Double,
    val averageReward: Double,
    val batchesProcessed: Int,
    val averageLoss: Double,
    val gradientNorm: Double,
    val bufferUtilization: BufferStats,
    val cycleDuration: Duration,
    val checkpointEvent: CheckpointEvent? = null
) {
    init {
        require(cycleNumber > 0) { "Cycle number must be positive" }
        require(totalCycles > 0) { "Total cycles must be positive" }
        require(gamesPlayed >= 0) { "Games played cannot be negative" }
        require(averageGameLength >= 0) { "Average game length cannot be negative" }
        require(batchesProcessed >= 0) { "Batches processed cannot be negative" }
    }
}

/**
 * Buffer utilization statistics
 */
data class BufferStats(
    val currentSize: Int,
    val maxSize: Int,
    val utilizationPercent: Double
) {
    init {
        require(currentSize >= 0) { "Current size cannot be negative" }
        require(maxSize > 0) { "Max size must be positive" }
        require(currentSize <= maxSize) { "Current size cannot exceed max size" }
        require(utilizationPercent in 0.0..100.0) { "Utilization percent must be between 0 and 100" }
    }
    
    companion object {
        fun create(currentSize: Int, maxSize: Int): BufferStats {
            val utilization = if (maxSize > 0) (currentSize.toDouble() / maxSize * 100.0) else 0.0
            return BufferStats(currentSize, maxSize, utilization)
        }
    }
}

/**
 * Checkpoint event information
 */
data class CheckpointEvent(
    val type: CheckpointType,
    val path: String,
    val performanceDelta: Double? = null,
    val isBest: Boolean = false
) {
    init {
        require(path.isNotBlank()) { "Checkpoint path cannot be blank" }
    }
}

/**
 * Types of checkpoint events
 */
enum class CheckpointType {
    REGULAR,
    BEST,
    FINAL
}

/**
 * Final training summary data
 */
data class FinalSummaryData(
    val totalCycles: Int,
    val totalDuration: Duration,
    val bestPerformance: Double,
    val totalGamesPlayed: Int,
    val totalExperiencesCollected: Int,
    val finalAverageReward: Double,
    val finalWinRate: Double,
    val finalDrawRate: Double
) {
    init {
        require(totalCycles > 0) { "Total cycles must be positive" }
        require(totalGamesPlayed >= 0) { "Total games played cannot be negative" }
        require(totalExperiencesCollected >= 0) { "Total experiences collected cannot be negative" }
        require(finalWinRate in 0.0..1.0) { "Final win rate must be between 0 and 1" }
        require(finalDrawRate in 0.0..1.0) { "Final draw rate must be between 0 and 1" }
    }
}

/**
 * Detailed block data for verbose output
 */
data class DetailedBlockData(
    val episodeMetrics: EpisodeMetrics,
    val trainingMetrics: TrainingMetrics,
    val qValueStats: QValueStatistics,
    val efficiencyMetrics: EfficiencyMetrics,
    val validationResults: List<AggregatedValidationMessage> = emptyList()
)

/**
 * Episode-level metrics
 */
data class EpisodeMetrics(
    val gameRange: IntRange,
    val averageLength: Double,
    val terminationBreakdown: Map<TerminationType, Int>
) {
    init {
        require(averageLength >= 0) { "Average length cannot be negative" }
        require(terminationBreakdown.values.all { it >= 0 }) { "Termination counts cannot be negative" }
    }
}

/**
 * Training-level metrics
 */
data class TrainingMetrics(
    val batchUpdates: Int,
    val averageLoss: Double,
    val gradientNorm: Double,
    val entropy: Double
) {
    init {
        require(batchUpdates >= 0) { "Batch updates cannot be negative" }
        require(gradientNorm >= 0) { "Gradient norm cannot be negative" }
    }
}

/**
 * Q-value statistics
 */
data class QValueStatistics(
    val mean: Double,
    val range: ClosedFloatingPointRange<Double>,
    val variance: Double
) {
    init {
        require(variance >= 0) { "Variance cannot be negative" }
    }
}

/**
 * Efficiency metrics
 */
data class EfficiencyMetrics(
    val gamesPerSecond: Double,
    val totalGames: Int,
    val totalExperiences: Int
) {
    init {
        require(gamesPerSecond >= 0) { "Games per second cannot be negative" }
        require(totalGames >= 0) { "Total games cannot be negative" }
        require(totalExperiences >= 0) { "Total experiences cannot be negative" }
    }
}

/**
 * Types of episode termination
 */
enum class TerminationType {
    NATURAL_END,
    STEP_LIMIT,
    RESIGNATION,
    TIMEOUT
}

/**
 * Aggregated validation message
 */
data class AggregatedValidationMessage(
    val message: String,
    val severity: ValidationSeverity,
    val count: Int,
    val firstSeen: Long,
    val lastSeen: Long
) {
    init {
        require(message.isNotBlank()) { "Message cannot be blank" }
        require(count > 0) { "Count must be positive" }
        require(firstSeen <= lastSeen) { "First seen cannot be after last seen" }
    }
}

/**
 * Validation message severity levels
 */
enum class ValidationSeverity {
    INFO,
    WARNING,
    ERROR
}

/**
 * Extension functions for Duration formatting
 */
fun Duration.toMillisLong(): Long = this.inWholeMilliseconds

/**
 * Extension function to create Duration from milliseconds
 */
fun Long.toDuration(): Duration = this.milliseconds

/**
 * Trend analysis data classes
 */

/**
 * Indicates the direction and magnitude of a performance trend
 */
data class TrendIndicator(
    val direction: TrendDirection,
    val magnitude: Double,
    val confidence: Double
) {
    init {
        require(magnitude >= 0) { "Magnitude cannot be negative" }
        require(confidence in 0.0..1.0) { "Confidence must be between 0 and 1" }
    }
}

/**
 * Direction of a trend
 */
enum class TrendDirection {
    UP,
    DOWN,
    STABLE
}

/**
 * Moving averages for smoothed metric display
 */
data class MovingAverages(
    val reward: Double,
    val winRate: Double,
    val loss: Double,
    val gradientNorm: Double,
    val cycleDuration: Duration
) {
    init {
        require(winRate in 0.0..1.0) { "Win rate must be between 0 and 1" }
        require(gradientNorm >= 0) { "Gradient norm cannot be negative" }
    }
}

/**
 * Trend data for all tracked metrics
 */
data class TrendData(
    val rewardTrend: TrendIndicator,
    val winRateTrend: TrendIndicator,
    val lossTrend: TrendIndicator,
    val bestPerformanceDelta: Double? = null,
    val eta: Duration? = null,
    val movingAverages: MovingAverages
)

/**
 * Enhanced evaluation results with statistical information
 */
data class EnhancedEvaluationResults(
    val totalGames: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val averageGameLength: Double,
    val confidenceInterval: ClosedFloatingPointRange<Double>? = null,
    val statisticalSignificance: Boolean = false,
    val colorAlternation: ColorAlternationStats,
    val opponentType: String
) {
    init {
        require(totalGames > 0) { "Total games must be positive" }
        require(wins >= 0) { "Wins cannot be negative" }
        require(draws >= 0) { "Draws cannot be negative" }
        require(losses >= 0) { "Losses cannot be negative" }
        require(wins + draws + losses == totalGames) { "Win/draw/loss counts must sum to total games" }
        require(winRate in 0.0..1.0) { "Win rate must be between 0 and 1" }
        require(drawRate in 0.0..1.0) { "Draw rate must be between 0 and 1" }
        require(lossRate in 0.0..1.0) { "Loss rate must be between 0 and 1" }
        require(averageGameLength >= 0) { "Average game length cannot be negative" }
        require(opponentType.isNotBlank()) { "Opponent type cannot be blank" }
    }
}

/**
 * Color alternation statistics for evaluation games
 */
data class ColorAlternationStats(
    val whiteGames: Int,
    val blackGames: Int,
    val whiteWins: Int,
    val blackWins: Int,
    val whiteDraws: Int,
    val blackDraws: Int,
    val whiteLosses: Int,
    val blackLosses: Int
) {
    init {
        require(whiteGames >= 0) { "White games cannot be negative" }
        require(blackGames >= 0) { "Black games cannot be negative" }
        require(whiteWins >= 0) { "White wins cannot be negative" }
        require(blackWins >= 0) { "Black wins cannot be negative" }
        require(whiteDraws >= 0) { "White draws cannot be negative" }
        require(blackDraws >= 0) { "Black draws cannot be negative" }
        require(whiteLosses >= 0) { "White losses cannot be negative" }
        require(blackLosses >= 0) { "Black losses cannot be negative" }
        require(whiteWins + whiteDraws + whiteLosses == whiteGames) { "White results must sum to white games" }
        require(blackWins + blackDraws + blackLosses == blackGames) { "Black results must sum to black games" }
    }
    
    val whiteWinRate: Double get() = if (whiteGames > 0) whiteWins.toDouble() / whiteGames else 0.0
    val blackWinRate: Double get() = if (blackGames > 0) blackWins.toDouble() / blackGames else 0.0
    val whiteDrawRate: Double get() = if (whiteGames > 0) whiteDraws.toDouble() / whiteGames else 0.0
    val blackDrawRate: Double get() = if (blackGames > 0) blackDraws.toDouble() / blackGames else 0.0
}

/**
 * Configuration for play sessions
 */
data class PlaySessionConfig(
    val profileUsed: String,
    val maxSteps: Int,
    val verboseEngineOutput: Boolean = false
) {
    init {
        require(profileUsed.isNotBlank()) { "Profile used cannot be blank" }
        require(maxSteps > 0) { "Max steps must be positive" }
    }
}

/**
 * Enhanced results from comparing two models with statistical information
 */
data class EnhancedComparisonResults(
    val totalGames: Int,
    val modelAWins: Int,
    val draws: Int,
    val modelBWins: Int,
    val modelAWinRate: Double,
    val drawRate: Double,
    val modelBWinRate: Double,
    val averageGameLength: Double,
    val confidenceInterval: ClosedFloatingPointRange<Double>? = null,
    val statisticalSignificance: Boolean = false,
    val effectSize: Double,
    val colorAlternation: ColorAlternationStats
) {
    init {
        require(totalGames > 0) { "Total games must be positive" }
        require(modelAWins >= 0) { "Model A wins cannot be negative" }
        require(draws >= 0) { "Draws cannot be negative" }
        require(modelBWins >= 0) { "Model B wins cannot be negative" }
        require(modelAWins + draws + modelBWins == totalGames) { "Results must sum to total games" }
        require(modelAWinRate in 0.0..1.0) { "Model A win rate must be between 0 and 1" }
        require(drawRate in 0.0..1.0) { "Draw rate must be between 0 and 1" }
        require(modelBWinRate in 0.0..1.0) { "Model B win rate must be between 0 and 1" }
        require(averageGameLength >= 0) { "Average game length cannot be negative" }
        require(effectSize >= 0) { "Effect size cannot be negative" }
    }
}