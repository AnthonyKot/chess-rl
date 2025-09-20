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
 * Trend analysis for metrics
 */
data class TrendAnalysis(
    val rewardTrend: Double,
    val winRateTrend: Double,
    val gameQualityTrend: Double,
    val efficiencyTrend: Double
)

// Optimization recommendations



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
