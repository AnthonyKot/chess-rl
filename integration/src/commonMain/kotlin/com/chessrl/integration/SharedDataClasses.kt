package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*

/**
 * Shared data classes used across the integration module
 * This file consolidates common data structures to avoid redeclarations
 */

// Common enums

/**
 * Unified game outcome enumeration used across all self-play components
 */
enum class GameOutcome {
    WHITE_WINS,
    BLACK_WINS,
    DRAW,
    ONGOING
}

/**
 * Episode termination reasons for self-play/training episodes
 */
enum class EpisodeTerminationReason {
    GAME_ENDED,   // Natural end: checkmate/stalemate/draw
    STEP_LIMIT,   // Reached maximum configured steps
    MANUAL        // Manually stopped/interrupted
}

/**
 * Trend direction for metrics analysis
 */
enum class TrendDirection {
    IMPROVING, 
    DECLINING, 
    STABLE, 
    UNKNOWN
}

/**
 * Recommendation priority levels
 */
enum class RecommendationPriority {
    LOW,
    MEDIUM, 
    HIGH,
    CRITICAL
}

/**
 * Report types for training reports
 */
enum class ReportType {
    COMPREHENSIVE,
    PERFORMANCE,
    GAME_QUALITY,
    VALIDATION,
    ISSUES
}

/**
 * Visualization types for analysis
 */
enum class VisualizationType {
    BOARD,
    LEARNING_CURVE,
    PERFORMANCE_METRICS,
    HEATMAP,
    ARROW_DIAGRAM,
    PROBABILITY_BARS,
    DECISION_TREE
}

// Performance metrics

/**
 * Performance metrics for training monitoring
 */
data class PerformanceMetrics(
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val averageReward: Double,
    val rewardVariance: Double
)

/**
 * Game quality metrics for analysis
 */
data class GameQualityMetrics(
    val averageQuality: Double,
    val moveAccuracy: Double,
    val strategicDepth: Double,
    val tacticalAccuracy: Double
)

/**
 * Performance snapshot for monitoring
 */
data class PerformanceSnapshot(
    val cycle: Int,
    val timestamp: Long,
    val overallScore: Double,
    val winRate: Double,
    val averageReward: Double,
    val gameQuality: Double,
    val trainingEfficiency: Double,
    val convergenceIndicator: Double
)

// Analysis results

/**
 * Game phase analysis results
 */
data class GamePhaseAnalysis(
    val openingQuality: Double,
    val middlegameQuality: Double,
    val endgameQuality: Double
)

/**
 * Strategic analysis results
 */
data class StrategicAnalysis(
    val openingStrategy: Double,
    val middlegameStrategy: Double,
    val endgameStrategy: Double,
    val strategicConsistency: Double,
    val planExecution: Double,
    val adaptability: Double
)

/**
 * Position analysis results
 */
data class PositionAnalysis(
    val position: String,
    val evaluation: Double,
    val complexity: Double,
    val tacticalThemes: List<String>,
    val strategicThemes: List<String>,
    val agentAssessment: Double
)

// Command results

/**
 * Command execution results
 */
sealed class CommandResult {
    data class Success(
        val message: String,
        val data: Any? = null
    ) : CommandResult()
    
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : CommandResult()
}

// Dashboard commands

/**
 * Simple dashboard command record data class
 */
data class DashboardCommandRecord(
    val type: String,
    val timestamp: Long,
    val input: String
)

// Dashboard supporting classes

/**
 * Session information for training dashboard
 */
data class SessionInfo(
    val sessionId: Long,
    val totalEpisodes: Int,
    val totalSteps: Int,
    val sessionDuration: Long
)

/**
 * Current training statistics
 */
data class CurrentStatistics(
    val currentEpisode: Double,
    val averageReward: Double,
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val gameLength: Double
)

/**
 * Trend analysis for metrics
 */
data class TrendAnalysis(
    val rewardTrend: Double,
    val winRateTrend: Double,
    val gameQualityTrend: Double,
    val efficiencyTrend: Double
)

/**
 * System health status
 */
data class SystemHealth(
    val status: HealthStatus,
    val score: Double,
    val warnings: Int,
    val errors: Int
)

/**
 * Health status enumeration
 */
enum class HealthStatus {
    HEALTHY,
    WARNING,
    CRITICAL
}

/**
 * Training efficiency metrics
 */
data class TrainingEfficiency(
    val episodesPerSecond: Double,
    val batchesPerSecond: Double,
    val memoryUsage: Double,
    val cpuUsage: Double
)

/**
 * Interface information for dashboard
 */
data class InterfaceInfo(
    val sessionId: String,
    val sessionDuration: Long,
    val dashboardUpdateInterval: Long,
    val lastUpdate: Long,
    val activeFeatures: List<String>
)

/**
 * Training dashboard data structure
 */
data class TrainingDashboard(
    val sessionInfo: SessionInfo,
    val currentStats: CurrentStatistics,
    val recentTrends: TrendAnalysis,
    val systemHealth: SystemHealth,
    val activeIssues: List<ActiveIssue>,
    val performanceMetrics: PerformanceMetrics,
    val gameQualityMetrics: GameQualityMetrics,
    val trainingEfficiency: TrainingEfficiency,
    val lastUpdate: Long,
    val interfaceInfo: InterfaceInfo? = null
)

/**
 * Training issue data class
 */
// ActiveIssue is defined in MonitoringDataClasses.kt and used in dashboards

// Optimization recommendations

/**
 * Optimization recommendations
 */
data class OptimizationRecommendation(
    val category: String,
    val expectedImprovement: Double,
    val implementationEffort: String
)

// Duplicate definitions removed - these are already defined above

// Unified self-play result models

/**
 * Unified result of a single self-play game
 * This is the canonical model used across all self-play components
 */
data class SelfPlayGameResult(
    val gameId: Int,
    val gameLength: Int,
    val gameOutcome: GameOutcome,
    val terminationReason: EpisodeTerminationReason,
    val gameDuration: Long,
    val experiences: List<Experience<DoubleArray, Int>>,
    val chessMetrics: ChessMetrics,
    val finalPosition: String,
    // Local repetition stats (lightweight, optional)
    val localRepeatMax: Int = 0,
    val localPositionsRepeated: Int = 0
)
