package com.chessrl.integration

import com.chessrl.chess.*

/**
 * Minimal data classes needed for GameAnalyzer to compile.
 * These are simplified versions to support the chess-engine interactive browser.
 */

data class GameAnalysisConfig(
    val enablePositionAnalysis: Boolean = true,
    val enableMoveAnalysis: Boolean = true,
    val enableStrategicAnalysis: Boolean = true,
    val enableTacticalAnalysis: Boolean = true,
    val maxAnalysisDepth: Int = 3
)

data class AnalyzedGame(
    val gameId: String,
    val moves: List<Move>,
    val positions: List<String>,
    val evaluations: List<Double>,
    val outcome: GameStatus,
    val analysisTimestamp: Long = System.currentTimeMillis()
)

data class DetailedMoveAnalysis(
    val move: Move,
    val piece: PieceType,
    val capturedPiece: PieceType?,
    val isWhiteMove: Boolean,
    val tacticalElements: List<String>,
    val positionalFactors: List<String>,
    val quality: MoveQuality,
    val materialChange: Int,
    val createsThreats: Boolean,
    val defendsThreats: Boolean
)

enum class MoveQuality(val symbol: String, val description: String) {
    EXCELLENT("!!", "Excellent move"),
    GOOD("!", "Good move"),
    NORMAL("", "Normal move"),
    INACCURATE("?!", "Inaccurate move"),
    MISTAKE("?", "Mistake"),
    BLUNDER("??", "Blunder")
}

data class AlternativeMove(
    val move: Move,
    val evaluation: Double,
    val description: String
)

data class MoveConsequences(
    val materialChange: Int,
    val positionalChange: Double,
    val tacticalThreats: List<String>
)

data class GameQualityAssessment(
    val overallScore: Double,
    val tacticalScore: Double,
    val strategicScore: Double,
    val accuracyScore: Double,
    val gameLength: Int,
    val complexity: Double
)

data class LearningInsight(
    val category: String,
    val description: String,
    val importance: Double,
    val gamePosition: Int
)

data class CriticalPosition(
    val position: String,
    val evaluation: Double,
    val bestMove: Move,
    val actualMove: Move,
    val evaluationDifference: Double
)



data class MovePattern(
    val pattern: String,
    val frequency: Int,
    val successRate: Double
)

// Simple analyzer classes
class PositionEvaluator {
    fun evaluate(@Suppress("UNUSED_PARAMETER") position: String): Double = 0.0
}

class MoveEvaluator {
    fun evaluate(@Suppress("UNUSED_PARAMETER") move: Move, @Suppress("UNUSED_PARAMETER") position: String): Double = 0.0
}

class StrategicAnalyzer {
    fun analyze(@Suppress("UNUSED_PARAMETER") game: AnalyzedGame): List<String> = emptyList()
}

class TacticalAnalyzer {
    fun analyze(@Suppress("UNUSED_PARAMETER") game: AnalyzedGame): List<String> = emptyList()
}

// Missing data class for SharedDataClasses.kt
data class ActiveIssue(
    val id: String,
    val severity: String,
    val description: String,
    val timestamp: Long
)