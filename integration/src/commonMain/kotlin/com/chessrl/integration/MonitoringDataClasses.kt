package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.chess.*

/**
 * Data classes and enums for the comprehensive training monitoring system
 */

// Configuration classes

/**
 * Main configuration for the monitoring system
 */
data class MonitoringConfig(
    val metricsConfig: MetricsCollectorConfig = MetricsCollectorConfig(),
    val monitorConfig: RealTimeMonitorConfig = RealTimeMonitorConfig(),
    val issueDetectionConfig: IssueDetectionConfig = IssueDetectionConfig(),
    val gameAnalysisConfig: GameAnalysisConfig = GameAnalysisConfig(),
    val reportConfig: ReportGeneratorConfig = ReportGeneratorConfig(),
    val displayUpdateInterval: Int = 5,
    val convergenceAnalysisWindow: Int = 20,
    val convergenceStabilityThreshold: Double = 0.8
)

data class MetricsCollectorConfig(
    val enableStatisticalSignificance: Boolean = true,
    val significanceLevel: Double = 0.05,
    val enableResourceMonitoring: Boolean = true,
    val enableThroughputTracking: Boolean = true,
    val metricsRetentionPeriod: Long = 24 * 60 * 60 * 1000 // 24 hours
)

data class RealTimeMonitorConfig(
    val updateInterval: Long = 1000, // 1 second
    val enableInteractiveCommands: Boolean = true,
    val enableLiveVisualization: Boolean = true,
    val maxDisplayLines: Int = 50
)

data class IssueDetectionConfig(
    val enableAutomatedDetection: Boolean = true,
    val enablePredictiveAnalysis: Boolean = true,
    val issueThresholds: Map<String, Double> = mapOf(
        "gradient_norm_high" to 10.0,
        "gradient_norm_low" to 1e-6,
        "policy_entropy_low" to 0.1,
        "win_rate_low" to 0.2,
        "loss_high" to 5.0
    )
)

data class GameAnalysisConfig(
    val enablePositionAnalysis: Boolean = true,
    val enableMoveAnalysis: Boolean = true,
    val enableStrategicAnalysis: Boolean = true,
    val maxGamesPerAnalysis: Int = 100,
    val analysisDepth: Int = 3
)

data class ReportGeneratorConfig(
    val enableDetailedReports: Boolean = true,
    val includeVisualizations: Boolean = true,
    val reportFormat: String = "comprehensive",
    val maxReportSize: Int = 10000 // lines
)

// Core data structures

/**
 * Comprehensive metrics for a training cycle
 */
data class TrainingCycleMetrics(
    val cycle: Int,
    val timestamp: Long,
    
    // Game statistics
    val gamesPlayed: Int,
    val averageGameLength: Double,
    val experiencesCollected: Int,
    
    // Performance metrics
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val averageReward: Double,
    val rewardVariance: Double,
    
    // Training metrics
    val batchUpdates: Int,
    val averageLoss: Double,
    val lossVariance: Double,
    val policyEntropy: Double,
    val gradientNorm: Double,
    
    // Quality metrics
    val averageGameQuality: Double,
    val moveAccuracy: Double,
    val strategicDepth: Double,
    val tacticalAccuracy: Double,
    
    // Efficiency metrics
    val trainingEfficiency: Double,
    val throughput: Double,
    val resourceUtilization: Double,
    
    // Statistical significance
    val statisticalSignificance: StatisticalSignificance? = null,
    
    // Additional metrics
    val customMetrics: Map<String, Double> = emptyMap()
)

/**
 * Statistical significance analysis
 */
data class StatisticalSignificance(
    val sampleSize: Int,
    val confidenceInterval: ConfidenceInterval,
    val pValue: Double,
    val isSignificant: Boolean,
    val effectSize: Double
)

data class ConfidenceInterval(
    val lowerBound: Double,
    val upperBound: Double,
    val confidenceLevel: Double
)

/**
 * Analyzed game with comprehensive metrics
 */
data class AnalyzedGame(
    val gameId: String,
    val timestamp: Long,
    val gameLength: Int,
    val gameOutcome: GameOutcome,
    val finalPosition: String,
    
    // Quality metrics
    val qualityScore: Double,
    val moveAccuracy: Double,
    val strategicComplexity: Double,
    val tacticalAccuracy: Double,
    
    // Game phases
    val openingQuality: Double,
    val middlegameQuality: Double,
    val endgameQuality: Double,
    
    // Decision making
    val averageDecisionTime: Double,
    val decisionConfidence: Double,
    val explorationRate: Double,
    
    // Learning insights
    val learningValue: Double,
    val noveltyScore: Double,
    val difficultyLevel: Double,
    
    // Move analysis
    val moveAnalyses: List<MoveAnalysis> = emptyList(),
    val criticalPositions: List<CriticalPosition> = emptyList()
)

data class MoveAnalysis(
    val moveNumber: Int,
    val move: String,
    val wasOptimal: Boolean,
    val qualityScore: Double,
    val alternativeMoves: List<String>,
    val positionEvaluation: Double,
    val timeSpent: Double
)

data class CriticalPosition(
    val moveNumber: Int,
    val position: String,
    val criticality: Double,
    val correctMove: String,
    val agentMove: String,
    val evaluation: Double
)

/**
 * Detected training issue
 */
data class DetectedIssue(
    val type: IssueType,
    val severity: Double, // 0.0 to 1.0
    val description: String,
    val timestamp: Long,
    val cycle: Int,
    val affectedMetrics: List<String>,
    val suggestedActions: List<String>,
    val confidence: Double
)

/**
 * Performance snapshot for trend analysis
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

/**
 * Training cycle analysis result
 */
data class TrainingCycleAnalysis(
    val cycle: Int,
    val metrics: TrainingCycleMetrics,
    val gameAnalyses: List<AnalyzedGame>,
    val detectedIssues: List<DetectedIssue>,
    val performanceSnapshot: PerformanceSnapshot,
    val convergenceStatus: ConvergenceAnalysis,
    val recommendations: List<TrainingRecommendation>,
    val analysisTime: Long
)

/**
 * Convergence analysis
 */
data class ConvergenceAnalysis(
    val status: ConvergenceStatus,
    val confidence: Double,
    val estimatedCyclesToConvergence: Int?,
    val stabilityScore: Double,
    val trendDirection: TrendDirection = TrendDirection.STABLE,
    val convergenceRate: Double = 0.0
)

// Using shared TrendDirection from SharedDataClasses.kt

/**
 * Training recommendation
 */
data class TrainingRecommendation(
    val type: RecommendationType,
    val priority: RecommendationPriority,
    val title: String,
    val description: String,
    val actions: List<String>,
    val expectedImpact: String = "",
    val confidence: Double = 1.0
)

enum class RecommendationType {
    PERFORMANCE, STABILITY, EXPLORATION, EFFICIENCY, QUALITY
}

// Using shared RecommendationPriority from SharedDataClasses.kt

// Dashboard and monitoring interfaces

/**
 * Real-time training dashboard
 */
// Using shared TrainingDashboard from SharedDataClasses.kt

data class SessionInfo(
    val elapsedTime: Long,
    val totalCycles: Int,
    val totalGames: Int,
    val totalExperiences: Int
)

data class CurrentStatistics(
    val averageReward: Double,
    val winRate: Double,
    val gameQuality: Double,
    val trainingEfficiency: Double,
    val policyEntropy: Double,
    val convergenceScore: Double
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
    val criticalIssues: Int,
    val totalIssues: Int
)

enum class HealthStatus {
    HEALTHY, WARNING, CRITICAL
}

data class ActiveIssue(
    val type: IssueType,
    val description: String,
    val severity: Double,
    val firstDetected: Long,
    val occurrences: Int
)

// Using shared PerformanceMetrics from SharedDataClasses.kt

// Using shared GameQualityMetrics from SharedDataClasses.kt

data class TrainingEfficiency(
    val gamesPerSecond: Double,
    val experiencesPerSecond: Double,
    val batchUpdatesPerSecond: Double,
    val overallEfficiency: Double
)

// Command system

/**
 * Interactive monitoring commands
 */
sealed class MonitoringCommand {
    object Pause : MonitoringCommand()
    object Resume : MonitoringCommand()
    object Stop : MonitoringCommand()
    data class GenerateReport(val reportType: ReportType) : MonitoringCommand()
    data class AnalyzeGame(val gameId: String) : MonitoringCommand()
    data class ShowTrends(val metric: String, val window: Int) : MonitoringCommand()
    object DiagnoseIssues : MonitoringCommand()
    data class ExportData(val format: String, val path: String) : MonitoringCommand()
    data class Configure(val newConfig: MonitoringConfig) : MonitoringCommand()
}

/**
 * Command execution result
 */
sealed class CommandResult {
    data class Success(val message: String, val data: Any? = null) : CommandResult()
    data class Error(val message: String, val exception: Throwable? = null) : CommandResult()
}

enum class ReportType {
    COMPREHENSIVE, PERFORMANCE, GAME_QUALITY, ISSUES
}

// Report structures

/**
 * Comprehensive training report
 */
data class ComprehensiveTrainingReport(
    val sessionId: String,
    val generatedAt: Long,
    val trainingPeriod: TrainingPeriod,
    val executiveSummary: ExecutiveSummary,
    val performanceAnalysis: PerformanceAnalysis,
    val gameQualityAnalysis: GameQualityAnalysis,
    val trainingEfficiencyAnalysis: TrainingEfficiencyAnalysis,
    val issueAnalysis: IssueAnalysis,
    val convergenceAnalysis: ConvergenceAnalysis,
    val recommendations: List<TrainingRecommendation>,
    val technicalDetails: TechnicalDetails,
    val generationTime: Long
)

data class TrainingPeriod(
    val startTime: Long,
    val endTime: Long,
    val totalCycles: Int,
    val totalDuration: Long
)

data class ExecutiveSummary(
    val overallPerformance: String,
    val keyAchievements: List<String>,
    val majorIssues: List<String>,
    val finalMetrics: Map<String, Double>,
    val recommendationSummary: String
)

data class PerformanceAnalysis(
    val performanceTrend: String,
    val bestPerformance: Double,
    val averagePerformance: Double,
    val performanceVariability: Double,
    val convergenceAssessment: String,
    val performanceBreakdown: Map<String, Double>
)

data class GameQualityAnalysis(
    val overallQuality: Double,
    val qualityTrend: String,
    val strategicImprovement: Double,
    val tacticalImprovement: Double,
    val gamePhaseAnalysis: Map<String, Double>,
    val qualityDistribution: Map<String, Int>
)

data class TrainingEfficiencyAnalysis(
    val overallEfficiency: Double,
    val throughputMetrics: Map<String, Double>,
    val resourceUtilization: Map<String, Double>,
    val bottleneckAnalysis: List<String>,
    val optimizationOpportunities: List<String>
)

data class IssueAnalysis(
    val totalIssues: Int,
    val issuesByType: Map<IssueType, Int>,
    val criticalIssues: List<DetectedIssue>,
    val issueResolution: Map<String, String>,
    val preventionRecommendations: List<String>
)

data class TechnicalDetails(
    val configurationUsed: Map<String, Any>,
    val systemMetrics: Map<String, Double>,
    val dataQuality: Map<String, Double>,
    val algorithmPerformance: Map<String, Double>,
    val resourceConsumption: Map<String, Double>
)

// Detailed game analysis

/**
 * Detailed game analysis with position evaluation
 */
data class DetailedGameAnalysis(
    val gameId: String,
    val game: AnalyzedGame,
    val positionAnalyses: List<PositionAnalysis>,
    val moveAnalyses: List<DetailedMoveAnalysis>,
    val strategicAnalysis: StrategicAnalysis?,
    val gameQuality: GameQualityAssessment,
    val learningInsights: List<LearningInsight>,
    val analysisTime: Long
)

data class PositionAnalysis(
    val moveNumber: Int,
    val position: String,
    val evaluation: Double,
    val complexity: Double,
    val tacticalThemes: List<String>,
    val strategicThemes: List<String>,
    val agentAssessment: AgentPositionAssessment
)

data class AgentPositionAssessment(
    val confidence: Double,
    val topMoves: List<String>,
    val moveProbabilities: Map<String, Double>,
    val positionUnderstanding: Double
)

data class DetailedMoveAnalysis(
    val moveNumber: Int,
    val move: String,
    val moveQuality: MoveQuality,
    val alternatives: List<AlternativeMove>,
    val consequences: MoveConsequences,
    val learningValue: Double
)

data class MoveQuality(
    val overallScore: Double,
    val tacticalScore: Double,
    val strategicScore: Double,
    val accuracyScore: Double,
    val noveltyScore: Double
)

data class AlternativeMove(
    val move: String,
    val evaluation: Double,
    val reason: String
)

data class MoveConsequences(
    val positionChange: Double,
    val materialChange: Double,
    val safetyChange: Double,
    val activityChange: Double
)

data class StrategicAnalysis(
    val openingStrategy: String,
    val middlegameStrategy: String,
    val endgameStrategy: String,
    val strategicConsistency: Double,
    val planExecution: Double,
    val adaptability: Double
)

data class GameQualityAssessment(
    val overallQuality: Double,
    val phaseQuality: Map<String, Double>,
    val decisionQuality: Double,
    val executionQuality: Double,
    val learningPotential: Double
)

data class LearningInsight(
    val type: String,
    val description: String,
    val importance: Double,
    val actionable: Boolean,
    val relatedPositions: List<String>
)

// Session and results

/**
 * Monitoring session information
 */
data class MonitoringSession(
    val sessionId: String,
    val startTime: Long,
    val trainingConfig: Any,
    val monitoringConfig: MonitoringConfig
)

/**
 * Final monitoring results
 */
data class MonitoringResults(
    val sessionDuration: Long,
    val totalCycles: Int,
    val totalGames: Int,
    val totalIssues: Int,
    val finalPerformance: Double,
    val finalReport: ComprehensiveTrainingReport,
    val dataExports: List<String>
)