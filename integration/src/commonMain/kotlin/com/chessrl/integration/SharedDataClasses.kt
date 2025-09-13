package com.chessrl.integration

import com.chessrl.chess.*

/**
 * Shared data classes used across the integration module
 * This file consolidates common data structures to avoid redeclarations
 */

// Common enums

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
        val message: String
    ) : CommandResult()
}

// Optimization recommendations

/**
 * Optimization recommendations
 */
data class OptimizationRecommendation(
    val category: String,
    val expectedImprovement: Double,
    val implementationEffort: String
)

// Training dashboard

/**
 * Training dashboard data structure
 */
data class TrainingDashboard(
    val sessionInfo: SessionInfo,
    val currentStats: CurrentStatistics,
    val recentTrends: TrendAnalysis,
    val systemHealth: SystemHealth,
    val activeIssues: List<TrainingIssue>,
    val performanceMetrics: PerformanceMetrics,
    val gameQualityMetrics: GameQualityMetrics,
    val trainingEfficiency: TrainingEfficiency,
    val lastUpdate: Long,
    val interfaceInfo: InterfaceInfo? = null
)

// Supporting data classes for dashboard

data class SessionInfo(
    val sessionId: Long,
    val totalEpisodes: Int,
    val totalSteps: Int,
    val sessionDuration: Long
)

data class CurrentStatistics(
    val currentEpisode: Double,
    val averageReward: Double,
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val gameLength: Double
)

data class TrendAnalysis(
    val rewardTrend: Double,
    val winRateTrend: Double,
    val gameQualityTrend: Double,
    val efficiencyTrend: Double
)

data class SystemHealth(
    val status: HealthStatus,
    val score: Double,
    val warnings: Int,
    val errors: Int
)

enum class HealthStatus {
    HEALTHY,
    WARNING,
    CRITICAL
}

data class TrainingEfficiency(
    val episodesPerSecond: Double,
    val batchesPerSecond: Double,
    val memoryUsage: Double,
    val cpuUsage: Double
)

data class InterfaceInfo(
    val sessionId: String,
    val sessionDuration: Long,
    val dashboardUpdateInterval: Long,
    val lastUpdate: Long,
    val activeFeatures: List<String>
)

// Training issues

data class TrainingIssue(
    val type: String,
    val severity: String,
    val description: String,
    val recommendation: String
)