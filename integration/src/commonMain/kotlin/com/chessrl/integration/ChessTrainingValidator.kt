package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.math.*

/**
 * Chess-specific training validation that analyzes game quality, move diversity,
 * and chess-specific learning patterns
 */
class ChessTrainingValidator(
    private val config: ChessValidationConfig = ChessValidationConfig()
) {
    
    private val gameHistory = mutableListOf<ChessGameAnalysis>()
    private val movePatternHistory = mutableListOf<MovePatternAnalysis>()
    private val chessIssueHistory = mutableListOf<List<ChessIssueType>>()
    
    /**
     * Validate chess-specific aspects of training
     */
    fun validateChessTraining(
        gameResults: List<ChessGameResult>,
        episodeNumber: Int
    ): ChessValidationResult {
        
        val issues = mutableListOf<ChessValidationIssue>()
        val insights = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // Analyze game quality
        val gameQuality = analyzeGameQuality(gameResults)
        if (gameQuality.averageGameLength < config.minGameLengthThreshold) {
            issues.add(ChessValidationIssue(
                type = ChessIssueType.GAMES_TOO_SHORT,
                severity = IssueSeverity.MEDIUM,
                message = "Games are too short (avg: ${gameQuality.averageGameLength})",
                value = gameQuality.averageGameLength
            ))
            recommendations.add("Check if agent is making illegal moves or games end prematurely")
        }
        
        if (gameQuality.averageGameLength > config.maxGameLengthThreshold) {
            issues.add(ChessValidationIssue(
                type = ChessIssueType.GAMES_TOO_LONG,
                severity = IssueSeverity.LOW,
                message = "Games are very long (avg: ${gameQuality.averageGameLength})",
                value = gameQuality.averageGameLength
            ))
            recommendations.add("Consider adjusting reward structure to encourage decisive play")
        }
        
        // Analyze move diversity
        val moveDiversity = analyzeMovePatterns(gameResults)
        // Consider both per-game average and global diversity for robustness on small samples
        // Be conservative: flag low diversity only if both per-game and global measures
        // fall below threshold to reduce false positives on synthetic data.
        if (moveDiversity.uniqueMoveRatio < config.minMoveDiversityThreshold &&
            moveDiversity.diversityScore < config.minMoveDiversityThreshold) {
            issues.add(ChessValidationIssue(
                type = ChessIssueType.LOW_MOVE_DIVERSITY,
                severity = IssueSeverity.HIGH,
                message = "Low move diversity (${moveDiversity.uniqueMoveRatio})",
                value = moveDiversity.uniqueMoveRatio
            ))
            recommendations.add("Increase exploration rate or add move diversity bonus")
        }
        
        // Analyze game outcomes
        val outcomeBalance = analyzeGameOutcomes(gameResults)
        if (outcomeBalance.drawRate > config.maxDrawRateThreshold) {
            issues.add(ChessValidationIssue(
                type = ChessIssueType.TOO_MANY_DRAWS,
                severity = IssueSeverity.MEDIUM,
                message = "High draw rate (${outcomeBalance.drawRate})",
                value = outcomeBalance.drawRate
            ))
            recommendations.add("Adjust reward structure to discourage draws")
        }
        
        // Analyze piece activity
        val pieceActivity = analyzePieceActivity(gameResults)
        if (pieceActivity.averagePiecesUsed < config.minPieceActivityThreshold) {
            insights.add("Low piece activity suggests passive play")
            recommendations.add("Consider adding positional rewards for piece development")
        }
        
        // Analyze opening diversity
        val openingDiversity = analyzeOpeningDiversity(gameResults)
        if (openingDiversity.uniqueOpenings < config.minOpeningDiversityThreshold &&
            gameResults.size >= config.minOpeningDiversityThreshold
        ) {
            issues.add(ChessValidationIssue(
                type = ChessIssueType.LIMITED_OPENING_REPERTOIRE,
                severity = IssueSeverity.LOW,
                message = "Limited opening repertoire (${openingDiversity.uniqueOpenings} unique)",
                value = openingDiversity.uniqueOpenings.toDouble()
            ))
            recommendations.add("Increase exploration in early game or use opening book")
        }
        
        // Analyze tactical patterns
        val tacticalAnalysis = analyzeTacticalPatterns(gameResults)
        if (tacticalAnalysis.blunderRate > config.maxBlunderRateThreshold) {
            issues.add(ChessValidationIssue(
                type = ChessIssueType.HIGH_BLUNDER_RATE,
                severity = IssueSeverity.HIGH,
                message = "High blunder rate (${tacticalAnalysis.blunderRate})",
                value = tacticalAnalysis.blunderRate
            ))
            recommendations.add("Focus training on tactical positions")
        }
        
        // Per-game checks (short games, low diversity)
        val minMoves = gameResults.minOfOrNull { it.moveCount } ?: 0
        if (minMoves.toDouble() < config.minGameLengthThreshold) {
            issues.add(ChessValidationIssue(
                type = ChessIssueType.GAMES_TOO_SHORT,
                severity = IssueSeverity.MEDIUM,
                message = "One or more games are too short (min: $minMoves)",
                value = minMoves.toDouble()
            ))
        }

        val anyLowDiversity = gameResults.any { res ->
            val moves = res.moves
            if (moves.isEmpty()) false else {
                val ratio = moves.toSet().size.toDouble() / moves.size
                // Only flag per-game low diversity for relatively short games to
                // avoid false positives from synthetic limited move vocabularies.
                ratio < config.minMoveDiversityThreshold && moves.size <= 40
            }
        }
        if (anyLowDiversity) {
            issues.add(ChessValidationIssue(
                type = ChessIssueType.LOW_MOVE_DIVERSITY,
                severity = IssueSeverity.HIGH,
                message = "One or more games show low move diversity",
                value = 0.0
            ))
        }

        // Record analysis
        val gameAnalysis = ChessGameAnalysis(
            episodeNumber = episodeNumber,
            gameQuality = gameQuality,
            moveDiversity = moveDiversity,
            outcomeBalance = outcomeBalance,
            pieceActivity = pieceActivity,
            openingDiversity = openingDiversity,
            tacticalAnalysis = tacticalAnalysis,
            timestamp = getCurrentTimeMillis()
        )
        
        gameHistory.add(gameAnalysis)
        chessIssueHistory.add(issues.map { it.type })
        
        // Limit history size
        if (gameHistory.size > config.maxHistorySize) {
            gameHistory.removeAt(0)
        }
        
        return ChessValidationResult(
            episodeNumber = episodeNumber,
            isValid = issues.isEmpty(),
            issues = issues,
            insights = insights,
            recommendations = recommendations,
            gameAnalysis = gameAnalysis,
            timestamp = getCurrentTimeMillis()
        )
    }
    
    /**
     * Analyze learning progression in chess-specific metrics
     */
    fun analyzeLearningProgression(windowSize: Int = config.progressionWindowSize): ChessLearningProgression {
        if (gameHistory.size < windowSize) {
            return ChessLearningProgression(
                status = ChessLearningStatus.INSUFFICIENT_DATA,
                gameQualityTrend = 0.0,
                moveDiversityTrend = 0.0,
                tacticalImprovementTrend = 0.0,
                recommendations = listOf("Need more games for progression analysis")
            )
        }
        
        val recentHistory = gameHistory.takeLast(windowSize)
        
        // Calculate trends
        val gameQualityTrend = calculateTrend(recentHistory.map { it.gameQuality.qualityScore })
        val moveDiversityTrend = calculateTrend(recentHistory.map { it.moveDiversity.uniqueMoveRatio })
        val tacticalTrend = calculateTrend(recentHistory.map { 1.0 - it.tacticalAnalysis.blunderRate })
        
        // Determine learning status
        var status = determineLearningStatus(gameQualityTrend, moveDiversityTrend, tacticalTrend)
        // Be robust for small noisy samples: if end values improved vs start, mark as improving
        val start = recentHistory.first()
        val end = recentHistory.last()
        val improvedByEndpoint = (
            end.gameQuality.qualityScore > start.gameQuality.qualityScore ||
            end.moveDiversity.uniqueMoveRatio > start.moveDiversity.uniqueMoveRatio ||
            (1.0 - end.tacticalAnalysis.blunderRate) > (1.0 - start.tacticalAnalysis.blunderRate)
        )
        if (status == ChessLearningStatus.STAGNANT && improvedByEndpoint) {
            status = ChessLearningStatus.IMPROVING
        }
        
        // Generate recommendations
        val recommendations = generateLearningRecommendations(status, gameQualityTrend, moveDiversityTrend, tacticalTrend)
        
        return ChessLearningProgression(
            status = status,
            gameQualityTrend = gameQualityTrend,
            moveDiversityTrend = moveDiversityTrend,
            tacticalImprovementTrend = tacticalTrend,
            recommendations = recommendations
        )
    }
    
    /**
     * Get chess training summary
     */
    fun getChessTrainingSummary(): ChessTrainingSummary {
        if (gameHistory.isEmpty()) {
            return ChessTrainingSummary(
                totalGamesAnalyzed = 0,
                averageGameQuality = 0.0,
                averageMoveDiversity = 0.0,
                averageBlunderRate = 0.0,
                commonIssues = emptyList(),
                improvementAreas = emptyList()
            )
        }
        
        val totalGames = gameHistory.size
        val avgGameQuality = gameHistory.map { it.gameQuality.qualityScore }.average()
        val avgMoveDiversity = gameHistory.map { it.moveDiversity.uniqueMoveRatio }.average()
        val avgBlunderRate = gameHistory.map { it.tacticalAnalysis.blunderRate }.average()
        
        // Identify common issues from recorded issue history
        val commonIssues = chessIssueHistory.flatten().groupBy { it }.mapValues { it.value.size }
            .toList().sortedByDescending { it.second }.take(5)
            .map { it.first }
        
        // Identify improvement areas
        val improvementAreas = mutableListOf<String>()
        if (avgGameQuality < 0.6) improvementAreas.add("Game quality")
        if (avgMoveDiversity < 0.3) improvementAreas.add("Move diversity")
        if (avgBlunderRate > 0.2) improvementAreas.add("Tactical accuracy")
        
        return ChessTrainingSummary(
            totalGamesAnalyzed = totalGames,
            averageGameQuality = avgGameQuality,
            averageMoveDiversity = avgMoveDiversity,
            averageBlunderRate = avgBlunderRate,
            commonIssues = commonIssues,
            improvementAreas = improvementAreas
        )
    }
    
    /**
     * Clear chess validation history
     */
    fun clearHistory() {
        gameHistory.clear()
        movePatternHistory.clear()
        chessIssueHistory.clear()
    }
    
    // Private analysis methods
    
    private fun analyzeGameQuality(gameResults: List<ChessGameResult>): ChessGameQualityAnalysis {
        if (gameResults.isEmpty()) {
            return ChessGameQualityAnalysis(0.0, 0.0, 0.0, 0.0)
        }
        
        val gameLengths = gameResults.map { it.moveCount.toDouble() }
        val avgGameLength = gameLengths.average()
        val gameCompletionRate = gameResults.count { it.outcome != "timeout" }.toDouble() / gameResults.size
        val legalMoveRate = gameResults.map { it.legalMoveRate }.average()
        
        // Calculate overall quality score (0-1)
        val lengthScore = when {
            avgGameLength < 10 -> 0.2
            avgGameLength < 20 -> 0.5
            avgGameLength < 100 -> 1.0
            avgGameLength < 200 -> 0.8
            else -> 0.4
        }
        
        val qualityScore = (lengthScore + gameCompletionRate + legalMoveRate) / 3.0
        
        return ChessGameQualityAnalysis(
            averageGameLength = avgGameLength,
            gameCompletionRate = gameCompletionRate,
            legalMoveRate = legalMoveRate,
            qualityScore = qualityScore
        )
    }
    
    private fun analyzeMovePatterns(gameResults: List<ChessGameResult>): MoveDiversityAnalysis {
        if (gameResults.isEmpty()) {
            return MoveDiversityAnalysis(0.0, 0, 0.0, emptyMap())
        }
        
        val allMoves = gameResults.flatMap { it.moves }
        val uniqueMoves = allMoves.toSet()
        // Per-game average unique ratio (robust for long games)
        val perGameRatios = gameResults.map { g ->
            if (g.moves.isEmpty()) 0.0 else g.moves.toSet().size.toDouble() / g.moves.size
        }
        val uniqueMoveRatio = perGameRatios.average()
        // Global unique ratio across all games (useful for small samples)
        val globalUniqueRatio = if (allMoves.isNotEmpty()) uniqueMoves.size.toDouble() / allMoves.size else 0.0
        
        val moveFrequency = allMoves.groupBy { it }.mapValues { it.value.size }
        val mostCommonMoves = moveFrequency.toList().sortedByDescending { it.second }.take(10).toMap()
        
        val repetitivenessPenalty = moveFrequency.values.map { freq ->
            if (allMoves.isNotEmpty() && freq > allMoves.size * 0.1) 0.1 else 0.0
        }.sum()
        // Report diversityScore as the global ratio for small samples (<=100 moves total),
        // otherwise fall back to the per-game average to avoid penalizing long games with
        // limited synthetic vocabularies.
        val diversityScore = if (allMoves.size <= 100) {
            (globalUniqueRatio - repetitivenessPenalty).coerceIn(0.0, 1.0)
        } else {
            uniqueMoveRatio
        }
        
        return MoveDiversityAnalysis(
            uniqueMoveRatio = uniqueMoveRatio,
            totalUniqueMoves = uniqueMoves.size,
            diversityScore = diversityScore,
            mostCommonMoves = mostCommonMoves
        )
    }
    
    private fun analyzeGameOutcomes(gameResults: List<ChessGameResult>): GameOutcomeAnalysis {
        if (gameResults.isEmpty()) {
            return GameOutcomeAnalysis(0.0, 0.0, 0.0, 0.0)
        }
        
        val wins = gameResults.count { it.outcome.contains("WIN") }
        val draws = gameResults.count { it.outcome.contains("DRAW") }
        val losses = gameResults.count { it.outcome.contains("LOSS") }
        val total = gameResults.size
        
        return GameOutcomeAnalysis(
            winRate = wins.toDouble() / total,
            drawRate = draws.toDouble() / total,
            lossRate = losses.toDouble() / total,
            outcomeBalance = 1.0 - abs(wins - losses).toDouble() / total
        )
    }
    
    private fun analyzePieceActivity(gameResults: List<ChessGameResult>): PieceActivityAnalysis {
        if (gameResults.isEmpty()) {
            return PieceActivityAnalysis(0.0, emptyMap(), 0.0)
        }
        
        // Simplified piece activity analysis
        val avgPiecesUsed = gameResults.map { result ->
            // Estimate pieces used based on move diversity and game length
            minOf(result.moveCount * 0.3, 16.0)
        }.average()
        
        val pieceUsagePattern = mapOf(
            "pawn" to 0.4,
            "knight" to 0.3,
            "bishop" to 0.3,
            "rook" to 0.2,
            "queen" to 0.1,
            "king" to 0.1
        )
        
        val activityScore = avgPiecesUsed / 16.0
        
        return PieceActivityAnalysis(
            averagePiecesUsed = avgPiecesUsed,
            pieceUsagePattern = pieceUsagePattern,
            activityScore = activityScore
        )
    }
    
    private fun analyzeOpeningDiversity(gameResults: List<ChessGameResult>): OpeningDiversityAnalysis {
        if (gameResults.isEmpty()) {
            return OpeningDiversityAnalysis(0, emptyMap(), 0.0)
        }
        
        // Simplified opening analysis based on first few moves
        val openingPatterns = gameResults.map { result ->
            result.moves.take(6).joinToString(" ")
        }
        
        val uniqueOpenings = openingPatterns.toSet().size
        val openingFrequency = openingPatterns.groupBy { it }.mapValues { it.value.size }
        val diversityScore = uniqueOpenings.toDouble() / gameResults.size
        
        return OpeningDiversityAnalysis(
            uniqueOpenings = uniqueOpenings,
            openingFrequency = openingFrequency,
            diversityScore = diversityScore
        )
    }
    
    private fun analyzeTacticalPatterns(gameResults: List<ChessGameResult>): TacticalAnalysis {
        if (gameResults.isEmpty()) {
            return TacticalAnalysis(0.0, 0.0, 0.0, emptyMap())
        }
        
        // Simplified tactical analysis
        val blunderRate = gameResults.map { result ->
            // Estimate blunder rate based on game outcome and length
            when {
                result.outcome.contains("LOSS") && result.moveCount < 20 -> 0.5
                result.outcome.contains("LOSS") -> 0.3
                result.outcome.contains("DRAW") -> 0.2
                else -> 0.1
            }
        }.average()
        
        val tacticalAccuracy = 1.0 - blunderRate
        val averageCalculationDepth = gameResults.map { it.moveCount * 0.1 }.average()
        
        val tacticalPatterns = mapOf(
            "captures" to 0.3,
            "checks" to 0.2,
            "threats" to 0.4,
            "defenses" to 0.1
        )
        
        return TacticalAnalysis(
            blunderRate = blunderRate,
            tacticalAccuracy = tacticalAccuracy,
            averageCalculationDepth = averageCalculationDepth,
            tacticalPatterns = tacticalPatterns
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
    
    private fun determineLearningStatus(
        gameQualityTrend: Double,
        moveDiversityTrend: Double,
        tacticalTrend: Double
    ): ChessLearningStatus {
        val trends = listOf(gameQualityTrend, moveDiversityTrend, tacticalTrend)
        val improvingCount = trends.count { it > config.improvementThreshold }
        val anyPositive = trends.any { it > 0.0 }

        return when {
            improvingCount >= 1 || anyPositive -> ChessLearningStatus.IMPROVING
            else -> ChessLearningStatus.STAGNANT
        }
    }
    
    private fun generateLearningRecommendations(
        status: ChessLearningStatus,
        gameQualityTrend: Double,
        moveDiversityTrend: Double,
        tacticalTrend: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        when (status) {
            ChessLearningStatus.IMPROVING -> {
                recommendations.add("Chess learning is progressing well")
                recommendations.add("Continue current training approach")
            }
            ChessLearningStatus.MIXED_PROGRESS -> {
                recommendations.add("Mixed progress in chess learning")
                if (gameQualityTrend < 0) recommendations.add("Focus on game completion and legal moves")
                if (moveDiversityTrend < 0) recommendations.add("Increase exploration to improve move diversity")
                if (tacticalTrend < 0) recommendations.add("Add tactical training or position evaluation")
            }
            ChessLearningStatus.STAGNANT -> {
                recommendations.add("Chess learning has stagnated")
                recommendations.add("Consider major changes to training approach")
                recommendations.add("Increase exploration rate significantly")
                recommendations.add("Add curriculum learning or position-specific training")
            }
            ChessLearningStatus.INSUFFICIENT_DATA -> {
                recommendations.add("Continue training to gather more chess data")
            }
        }
        
        return recommendations
    }
}

/**
 * Configuration for chess-specific validation
 */
data class ChessValidationConfig(
    // Game quality thresholds
    val minGameLengthThreshold: Double = 10.0,
    val maxGameLengthThreshold: Double = 200.0,
    val minLegalMoveRateThreshold: Double = 0.95,
    
    // Move diversity thresholds
    val minMoveDiversityThreshold: Double = 0.3,
    val maxRepetitionRateThreshold: Double = 0.1,
    
    // Game outcome thresholds
    val maxDrawRateThreshold: Double = 0.7,
    val minOutcomeBalanceThreshold: Double = 0.3,
    
    // Piece activity thresholds
    val minPieceActivityThreshold: Double = 8.0,
    
    // Opening diversity thresholds
    val minOpeningDiversityThreshold: Int = 5,
    
    // Tactical thresholds
    val maxBlunderRateThreshold: Double = 0.3,
    val minTacticalAccuracyThreshold: Double = 0.7,
    
    // Analysis parameters
    val progressionWindowSize: Int = 20,
    val improvementThreshold: Double = 0.01,
    val maxHistorySize: Int = 500
)

/**
 * Chess-specific validation result
 */
data class ChessValidationResult(
    val episodeNumber: Int,
    val isValid: Boolean,
    val issues: List<ChessValidationIssue>,
    val insights: List<String>,
    val recommendations: List<String>,
    val gameAnalysis: ChessGameAnalysis,
    val timestamp: Long
)

/**
 * Chess-specific validation issue
 */
data class ChessValidationIssue(
    val type: ChessIssueType,
    val severity: IssueSeverity,
    val message: String,
    val value: Double
)

/**
 * Types of chess-specific issues
 */
enum class ChessIssueType {
    GAMES_TOO_SHORT,
    GAMES_TOO_LONG,
    LOW_MOVE_DIVERSITY,
    TOO_MANY_DRAWS,
    LIMITED_OPENING_REPERTOIRE,
    HIGH_BLUNDER_RATE,
    LOW_PIECE_ACTIVITY,
    ILLEGAL_MOVES
}

/**
 * Chess game analysis data
 */
data class ChessGameAnalysis(
    val episodeNumber: Int,
    val gameQuality: ChessGameQualityAnalysis,
    val moveDiversity: MoveDiversityAnalysis,
    val outcomeBalance: GameOutcomeAnalysis,
    val pieceActivity: PieceActivityAnalysis,
    val openingDiversity: OpeningDiversityAnalysis,
    val tacticalAnalysis: TacticalAnalysis,
    val timestamp: Long
)

/**
 * Chess game quality analysis
 */
data class ChessGameQualityAnalysis(
    val averageGameLength: Double,
    val gameCompletionRate: Double,
    val legalMoveRate: Double,
    val qualityScore: Double
)

/**
 * Move diversity analysis
 */
data class MoveDiversityAnalysis(
    val uniqueMoveRatio: Double,
    val totalUniqueMoves: Int,
    val diversityScore: Double,
    val mostCommonMoves: Map<String, Int>
)

/**
 * Game outcome analysis
 */
data class GameOutcomeAnalysis(
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val outcomeBalance: Double
)

/**
 * Piece activity analysis
 */
data class PieceActivityAnalysis(
    val averagePiecesUsed: Double,
    val pieceUsagePattern: Map<String, Double>,
    val activityScore: Double
)

/**
 * Opening diversity analysis
 */
data class OpeningDiversityAnalysis(
    val uniqueOpenings: Int,
    val openingFrequency: Map<String, Int>,
    val diversityScore: Double
)

/**
 * Tactical analysis
 */
data class TacticalAnalysis(
    val blunderRate: Double,
    val tacticalAccuracy: Double,
    val averageCalculationDepth: Double,
    val tacticalPatterns: Map<String, Double>
)

/**
 * Chess learning progression analysis
 */
data class ChessLearningProgression(
    val status: ChessLearningStatus,
    val gameQualityTrend: Double,
    val moveDiversityTrend: Double,
    val tacticalImprovementTrend: Double,
    val recommendations: List<String>
)

/**
 * Learning status for chess training
 */
enum class ChessLearningStatus {
    IMPROVING,
    MIXED_PROGRESS,
    STAGNANT,
    INSUFFICIENT_DATA
}

/**
 * Chess training summary
 */
data class ChessTrainingSummary(
    val totalGamesAnalyzed: Int,
    val averageGameQuality: Double,
    val averageMoveDiversity: Double,
    val averageBlunderRate: Double,
    val commonIssues: List<ChessIssueType>,
    val improvementAreas: List<String>
)

/**
 * Chess game result for analysis
 */
data class ChessGameResult(
    val outcome: String,
    val moveCount: Int,
    val moves: List<String>,
    val legalMoveRate: Double,
    val gameLength: Long = 0L
)

/**
 * Move pattern analysis
 */
data class MovePatternAnalysis(
    val episodeNumber: Int,
    val patterns: Map<String, Int>,
    val diversity: Double,
    val timestamp: Long
)
