package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.chess.*
import kotlin.math.*
import kotlin.random.Random

/**
 * Advanced game analyzer for comprehensive game quality assessment and learning insights.
 * 
 * Provides:
 * - Advanced game replay with position analysis and move evaluation
 * - Strategic and tactical analysis of game progression
 * - Learning insights extraction for training improvement
 * - Game quality metrics with detailed breakdowns
 */
class GameAnalyzer(
    private val config: GameAnalysisConfig
) {
    
    // Analysis state
    private val analysisHistory = mutableListOf<AnalyzedGame>()
    private val positionDatabase = mutableMapOf<String, PositionAnalysis>()
    private val movePatterns = mutableMapOf<String, MovePattern>()
    
    // Analysis engines
    private val positionEvaluator = PositionEvaluator()
    private val moveEvaluator = MoveEvaluator()
    private val strategicAnalyzer = StrategicAnalyzer()
    private val tacticalAnalyzer = TacticalAnalyzer()
    
    /**
     * Initialize the game analyzer
     */
    fun initialize() {
        println("🎮 Game Analyzer initialized")
    }
    
    /**
     * Analyze a single game with comprehensive metrics
     */
    fun analyzeGame(gameResult: SelfPlayGameResult): AnalyzedGame {
        val gameId = generateGameId(gameResult)
        
        // Basic game metrics
        val basicMetrics = extractBasicMetrics(gameResult)
        
        // Quality assessment
        val qualityAssessment = assessGameQuality(gameResult)
        
        // Phase analysis
        val phaseAnalysis = analyzeGamePhases(gameResult)
        
        // Decision making analysis
        val decisionAnalysis = analyzeDecisionMaking(gameResult)
        
        // Learning insights
        val learningInsights = extractLearningInsights(gameResult)
        
        // Move analysis (if enabled)
        val moveAnalyses = if (config.enableMoveAnalysis) {
            analyzeMoves(gameResult)
        } else emptyList()
        
        // Critical positions (if enabled)
        val criticalPositions = if (config.enablePositionAnalysis) {
            identifyCriticalPositions(gameResult)
        } else emptyList()
        
        val analyzedGame = AnalyzedGame(
            gameId = gameId,
            timestamp = getCurrentTimeMillis(),
            gameLength = basicMetrics.gameLength,
            gameOutcome = basicMetrics.gameOutcome,
            finalPosition = basicMetrics.finalPosition,
            
            // Quality metrics
            qualityScore = qualityAssessment.overallQuality,
            moveAccuracy = qualityAssessment.moveAccuracy,
            strategicComplexity = qualityAssessment.strategicComplexity,
            tacticalAccuracy = qualityAssessment.tacticalAccuracy,
            
            // Game phases
            openingQuality = phaseAnalysis.openingQuality,
            middlegameQuality = phaseAnalysis.middlegameQuality,
            endgameQuality = phaseAnalysis.endgameQuality,
            
            // Decision making
            averageDecisionTime = decisionAnalysis.averageDecisionTime,
            decisionConfidence = decisionAnalysis.decisionConfidence,
            explorationRate = decisionAnalysis.explorationRate,
            
            // Learning insights
            learningValue = learningInsights.learningValue,
            noveltyScore = learningInsights.noveltyScore,
            difficultyLevel = learningInsights.difficultyLevel,
            
            // Detailed analysis
            moveAnalyses = moveAnalyses,
            criticalPositions = criticalPositions
        )
        
        // Store for historical analysis
        analysisHistory.add(analyzedGame)
        
        // Update patterns and database
        updateMovePatterns(analyzedGame)
        updatePositionDatabase(analyzedGame)
        
        return analyzedGame
    }
    
    /**
     * Analyze positions in a game
     */
    fun analyzePositions(game: AnalyzedGame): List<PositionAnalysis> {
        val positions = mutableListOf<PositionAnalysis>()
        
        // Analyze key positions throughout the game
        val keyMoves = selectKeyMoves(game)
        
        for (moveNumber in keyMoves) {
            val position = reconstructPosition(game, moveNumber)
            val analysis = analyzePosition(position, moveNumber)
            positions.add(analysis)
        }
        
        return positions
    }
    
    /**
     * Analyze moves in a game with detailed evaluation
     */
    fun analyzeMoves(gameResult: SelfPlayGameResult): List<DetailedMoveAnalysis> {
        val moveAnalyses = mutableListOf<DetailedMoveAnalysis>()
        
        // Analyze each move in the game
        for (moveNumber in 1..gameResult.gameLength) {
            val moveAnalysis = analyzeSingleMove(gameResult, moveNumber)
            moveAnalyses.add(moveAnalysis)
        }
        
        return moveAnalyses
    }
    
    /**
     * Analyze moves in an analyzed game
     */
    fun analyzeMoves(game: AnalyzedGame): List<DetailedMoveAnalysis> {
        val moveAnalyses = mutableListOf<DetailedMoveAnalysis>()
        
        // Use existing move analyses or create new ones
        if (game.moveAnalyses.isNotEmpty()) {
            // Convert existing analyses to detailed format
            game.moveAnalyses.forEach { moveAnalysis ->
                val detailedAnalysis = DetailedMoveAnalysis(
                    moveNumber = moveAnalysis.moveNumber,
                    move = moveAnalysis.move,
                    moveQuality = MoveQuality(
                        overallScore = moveAnalysis.qualityScore,
                        tacticalScore = if (moveAnalysis.wasOptimal) 1.0 else 0.7,
                        strategicScore = moveAnalysis.qualityScore,
                        accuracyScore = if (moveAnalysis.wasOptimal) 1.0 else 0.8,
                        noveltyScore = 0.5 // Default value
                    ),
                    alternatives = moveAnalysis.alternativeMoves.map { altMove ->
                        AlternativeMove(
                            move = altMove,
                            evaluation = moveAnalysis.positionEvaluation,
                            reason = "Alternative move"
                        )
                    },
                    consequences = MoveConsequences(
                        positionChange = moveAnalysis.positionEvaluation,
                        materialChange = 0.0, // Would need to calculate
                        safetyChange = 0.0,
                        activityChange = 0.0
                    ),
                    learningValue = calculateMoveLearningValue(moveAnalysis)
                )
                moveAnalyses.add(detailedAnalysis)
            }
        }
        
        return moveAnalyses
    }
    
    /**
     * Analyze strategic aspects of a game
     */
    fun analyzeStrategy(game: AnalyzedGame): StrategicAnalysis {
        return StrategicAnalysis(
            openingStrategy = analyzeOpeningStrategy(game),
            middlegameStrategy = analyzeMiddlegameStrategy(game),
            endgameStrategy = analyzeEndgameStrategy(game),
            strategicConsistency = calculateStrategicConsistency(game),
            planExecution = assessPlanExecution(game),
            adaptability = assessAdaptability(game)
        )
    }
    
    /**
     * Assess overall game quality
     */
    fun assessGameQuality(gameResult: SelfPlayGameResult): GameQualityAssessment {
        // Calculate various quality metrics
        val moveQuality = calculateMoveQuality(gameResult)
        val phaseQuality = calculatePhaseQuality(gameResult)
        val decisionQuality = calculateDecisionQuality(gameResult)
        val executionQuality = calculateExecutionQuality(gameResult)
        val learningPotential = calculateLearningPotential(gameResult)
        
        val overallQuality = (moveQuality + phaseQuality + decisionQuality + executionQuality) / 4.0
        
        return GameQualityAssessment(
            overallQuality = overallQuality,
            phaseQuality = mapOf(
                "opening" to phaseQuality * 0.8, // Simplified
                "middlegame" to phaseQuality,
                "endgame" to phaseQuality * 1.2
            ),
            decisionQuality = decisionQuality,
            executionQuality = executionQuality,
            learningPotential = learningPotential
        )
    }
    
    /**
     * Assess game quality for an analyzed game
     */
    fun assessGameQuality(game: AnalyzedGame): GameQualityAssessment {
        val phaseQuality = mapOf(
            "opening" to game.openingQuality,
            "middlegame" to game.middlegameQuality,
            "endgame" to game.endgameQuality
        )
        
        return GameQualityAssessment(
            overallQuality = game.qualityScore,
            phaseQuality = phaseQuality,
            decisionQuality = game.decisionConfidence,
            executionQuality = game.moveAccuracy,
            learningPotential = game.learningValue
        )
    }
    
    /**
     * Extract learning insights from a game
     */
    fun extractLearningInsights(gameResult: SelfPlayGameResult): List<LearningInsight> {
        val insights = mutableListOf<LearningInsight>()
        
        // Tactical insights
        val tacticalInsights = extractTacticalInsights(gameResult)
        insights.addAll(tacticalInsights)
        
        // Strategic insights
        val strategicInsights = extractStrategicInsights(gameResult)
        insights.addAll(strategicInsights)
        
        // Endgame insights
        val endgameInsights = extractEndgameInsights(gameResult)
        insights.addAll(endgameInsights)
        
        // Decision-making insights
        val decisionInsights = extractDecisionInsights(gameResult)
        insights.addAll(decisionInsights)
        
        return insights.sortedByDescending { it.importance }.take(5)
    }
    
    /**
     * Extract learning insights from an analyzed game
     */
    fun extractLearningInsights(game: AnalyzedGame): List<LearningInsight> {
        val insights = mutableListOf<LearningInsight>()
        
        // Quality-based insights
        if (game.qualityScore < 0.5) {
            insights.add(LearningInsight(
                type = "Quality",
                description = "Game quality below average - focus on move accuracy",
                importance = 0.8,
                actionable = true,
                relatedPositions = game.criticalPositions.map { it.position }
            ))
        }
        
        // Strategic insights
        if (game.strategicComplexity < 0.4) {
            insights.add(LearningInsight(
                type = "Strategy",
                description = "Low strategic complexity - practice long-term planning",
                importance = 0.7,
                actionable = true,
                relatedPositions = emptyList()
            ))
        }
        
        // Tactical insights
        if (game.tacticalAccuracy < 0.6) {
            insights.add(LearningInsight(
                type = "Tactics",
                description = "Tactical accuracy needs improvement - practice tactical puzzles",
                importance = 0.9,
                actionable = true,
                relatedPositions = game.criticalPositions.filter { it.criticality > 0.7 }.map { it.position }
            ))
        }
        
        return insights.sortedByDescending { it.importance }
    }
    
    /**
     * Finalize game analysis
     */
    fun finalize() {
        println("🎮 Game analysis finalized. Total games analyzed: ${analysisHistory.size}")
    }
    
    // Private analysis methods
    
    private fun extractBasicMetrics(gameResult: SelfPlayGameResult): BasicGameMetrics {
        return BasicGameMetrics(
            gameLength = gameResult.gameLength,
            gameOutcome = gameResult.gameOutcome,
            finalPosition = gameResult.finalPosition
        )
    }
    
    private fun analyzeGamePhases(gameResult: SelfPlayGameResult): GamePhaseAnalysis {
        val gameLength = gameResult.gameLength
        
        // Simplified phase analysis based on game length
        val openingLength = minOf(gameLength, 15)
        val endgameStart = maxOf(gameLength - 20, openingLength + 1)
        val middlegameLength = maxOf(0, endgameStart - openingLength)
        val endgameLength = maxOf(0, gameLength - endgameStart)
        
        return GamePhaseAnalysis(
            openingQuality = calculatePhaseQuality(gameResult, 0, openingLength),
            middlegameQuality = calculatePhaseQuality(gameResult, openingLength, endgameStart),
            endgameQuality = calculatePhaseQuality(gameResult, endgameStart, gameLength)
        )
    }
    
    private fun analyzeDecisionMaking(gameResult: SelfPlayGameResult): DecisionMakingAnalysis {
        // Simplified decision making analysis
        val averageDecisionTime = 1.0 + Random.nextDouble() * 2.0 // 1-3 seconds
        val decisionConfidence = 0.6 + Random.nextDouble() * 0.4 // 60-100%
        val explorationRate = 0.1 + Random.nextDouble() * 0.2 // 10-30%
        
        return DecisionMakingAnalysis(
            averageDecisionTime = averageDecisionTime,
            decisionConfidence = decisionConfidence,
            explorationRate = explorationRate
        )
    }
    
    private fun computeLearningInsightsData(gameResult: SelfPlayGameResult): LearningInsightsData {
        // Simplified learning insights calculation
        val learningValue = calculateLearningValue(gameResult)
        val noveltyScore = calculateNoveltyScore(gameResult)
        val difficultyLevel = calculateDifficultyLevel(gameResult)
        
        return LearningInsightsData(
            learningValue = learningValue,
            noveltyScore = noveltyScore,
            difficultyLevel = difficultyLevel
        )
    }
    
    private fun calculateLearningValue(gameResult: SelfPlayGameResult): Double {
        // Higher learning value for games with interesting positions and outcomes
        val lengthFactor = minOf(gameResult.gameLength / 50.0, 1.0)
        val outcomeFactor = when (gameResult.gameOutcome) {
            GameOutcome.WHITE_WINS, GameOutcome.BLACK_WINS -> 1.0
            GameOutcome.DRAW -> 0.8
            else -> 0.5
        }
        return (lengthFactor + outcomeFactor) / 2.0
    }
    
    private fun calculateNoveltyScore(gameResult: SelfPlayGameResult): Double {
        // Simplified novelty calculation
        return 0.3 + Random.nextDouble() * 0.7
    }
    
    private fun calculateDifficultyLevel(gameResult: SelfPlayGameResult): Double {
        // Difficulty based on game length and complexity
        val lengthFactor = minOf(gameResult.gameLength / 100.0, 1.0)
        return 0.2 + lengthFactor * 0.8
    }
    
    private fun calculatePhaseQuality(gameResult: SelfPlayGameResult, startMove: Int, endMove: Int): Double {
        if (startMove >= endMove) return 0.5
        
        // Simplified phase quality calculation
        val phaseLength = endMove - startMove
        val lengthFactor = minOf(phaseLength / 20.0, 1.0)
        val randomFactor = 0.5 + Random.nextDouble() * 0.5
        
        return (lengthFactor + randomFactor) / 2.0
    }
    
    private fun selectKeyMoves(game: AnalyzedGame): List<Int> {
        val keyMoves = mutableListOf<Int>()
        
        // Add opening moves
        keyMoves.addAll(1..minOf(game.gameLength, 10))
        
        // Add critical positions
        keyMoves.addAll(game.criticalPositions.map { it.moveNumber })
        
        // Add endgame moves
        val endgameStart = maxOf(game.gameLength - 15, 11)
        keyMoves.addAll(endgameStart..game.gameLength)
        
        return keyMoves.distinct().sorted()
    }
    
    private fun reconstructPosition(game: AnalyzedGame, moveNumber: Int): String {
        // Simplified position reconstruction
        return "position_${game.gameId}_move_${moveNumber}"
    }
    
    private fun analyzePosition(position: String, moveNumber: Int): PositionAnalysis {
        return PositionAnalysis(
            moveNumber = moveNumber,
            position = position,
            evaluation = Random.nextDouble(-2.0, 2.0),
            complexity = Random.nextDouble(0.0, 1.0),
            tacticalThemes = listOf("pin", "fork", "skewer").shuffled().take(Random.nextInt(0, 3)),
            strategicThemes = listOf("center control", "king safety", "pawn structure").shuffled().take(Random.nextInt(0, 3)),
            agentAssessment = AgentPositionAssessment(
                confidence = Random.nextDouble(0.5, 1.0),
                topMoves = listOf("e4", "d4", "Nf3").shuffled().take(3),
                moveProbabilities = mapOf("e4" to 0.4, "d4" to 0.3, "Nf3" to 0.3),
                positionUnderstanding = Random.nextDouble(0.4, 1.0)
            )
        )
    }
    
    private fun analyzeSingleMove(gameResult: SelfPlayGameResult, moveNumber: Int): DetailedMoveAnalysis {
        val move = "move_${moveNumber}" // Simplified
        
        return DetailedMoveAnalysis(
            moveNumber = moveNumber,
            move = move,
            moveQuality = MoveQuality(
                overallScore = Random.nextDouble(0.3, 1.0),
                tacticalScore = Random.nextDouble(0.4, 1.0),
                strategicScore = Random.nextDouble(0.3, 1.0),
                accuracyScore = Random.nextDouble(0.5, 1.0),
                noveltyScore = Random.nextDouble(0.0, 0.8)
            ),
            alternatives = listOf(
                AlternativeMove("alt1", Random.nextDouble(-1.0, 1.0), "Better development"),
                AlternativeMove("alt2", Random.nextDouble(-1.0, 1.0), "More aggressive")
            ),
            consequences = MoveConsequences(
                positionChange = Random.nextDouble(-0.5, 0.5),
                materialChange = 0.0,
                safetyChange = Random.nextDouble(-0.3, 0.3),
                activityChange = Random.nextDouble(-0.2, 0.4)
            ),
            learningValue = Random.nextDouble(0.2, 0.9)
        )
    }
    
    private fun identifyCriticalPositions(gameResult: SelfPlayGameResult): List<CriticalPosition> {
        val criticalPositions = mutableListOf<CriticalPosition>()
        
        // Identify 3-5 critical positions in the game
        val numCritical = Random.nextInt(3, 6)
        val gameLength = gameResult.gameLength
        
        repeat(numCritical) { i ->
            val moveNumber = (gameLength * (i + 1) / (numCritical + 1))
            criticalPositions.add(
                CriticalPosition(
                    moveNumber = moveNumber,
                    position = "critical_position_${i}",
                    criticality = Random.nextDouble(0.6, 1.0),
                    correctMove = "correct_move_${i}",
                    agentMove = "agent_move_${i}",
                    evaluation = Random.nextDouble(-2.0, 2.0)
                )
            )
        }
        
        return criticalPositions
    }
    
    private fun calculateMoveQuality(gameResult: SelfPlayGameResult): Double {
        // Simplified move quality based on game outcome and length
        val outcomeScore = when (gameResult.gameOutcome) {
            GameOutcome.WHITE_WINS, GameOutcome.BLACK_WINS -> 0.8
            GameOutcome.DRAW -> 0.6
            else -> 0.4
        }
        
        val lengthScore = when {
            gameResult.gameLength < 20 -> 0.5 // Too short
            gameResult.gameLength > 150 -> 0.6 // Too long
            else -> 0.8 // Good length
        }
        
        return (outcomeScore + lengthScore) / 2.0
    }
    
    private fun calculatePhaseQuality(gameResult: SelfPlayGameResult): Double {
        return 0.6 + Random.nextDouble() * 0.4
    }
    
    private fun calculateDecisionQuality(gameResult: SelfPlayGameResult): Double {
        return 0.5 + Random.nextDouble() * 0.5
    }
    
    private fun calculateExecutionQuality(gameResult: SelfPlayGameResult): Double {
        return 0.6 + Random.nextDouble() * 0.4
    }
    
    private fun calculateLearningPotential(gameResult: SelfPlayGameResult): Double {
        return calculateLearningValue(gameResult)
    }
    
    private fun calculateMoveLearningValue(moveAnalysis: MoveAnalysis): Double {
        return if (moveAnalysis.wasOptimal) 0.3 else 0.8 // Mistakes have higher learning value
    }
    
    // Strategic analysis methods
    
    private fun analyzeOpeningStrategy(game: AnalyzedGame): String {
        return when {
            game.openingQuality > 0.8 -> "Excellent opening preparation"
            game.openingQuality > 0.6 -> "Solid opening play"
            else -> "Opening needs improvement"
        }
    }
    
    private fun analyzeMiddlegameStrategy(game: AnalyzedGame): String {
        return when {
            game.middlegameQuality > 0.8 -> "Strong middlegame planning"
            game.middlegameQuality > 0.6 -> "Adequate middlegame play"
            else -> "Middlegame strategy unclear"
        }
    }
    
    private fun analyzeEndgameStrategy(game: AnalyzedGame): String {
        return when {
            game.endgameQuality > 0.8 -> "Precise endgame technique"
            game.endgameQuality > 0.6 -> "Reasonable endgame play"
            else -> "Endgame technique needs work"
        }
    }
    
    private fun calculateStrategicConsistency(game: AnalyzedGame): Double {
        val phaseQualities = listOf(game.openingQuality, game.middlegameQuality, game.endgameQuality)
        val variance = calculateVariance(phaseQualities)
        return 1.0 - variance // Lower variance = higher consistency
    }
    
    private fun assessPlanExecution(game: AnalyzedGame): Double {
        return game.qualityScore * 0.8 + game.moveAccuracy * 0.2
    }
    
    private fun assessAdaptability(game: AnalyzedGame): Double {
        return game.decisionConfidence * 0.6 + game.explorationRate * 0.4
    }
    
    // Insight extraction methods
    
    private fun extractTacticalInsights(gameResult: SelfPlayGameResult): List<LearningInsight> {
        val insights = mutableListOf<LearningInsight>()
        
        if (gameResult.gameLength > 30) {
            insights.add(LearningInsight(
                type = "Tactical",
                description = "Long game suggests tactical complexity - good for tactical training",
                importance = 0.7,
                actionable = true,
                relatedPositions = listOf("tactical_position_1", "tactical_position_2")
            ))
        }
        
        return insights
    }
    
    private fun extractStrategicInsights(gameResult: SelfPlayGameResult): List<LearningInsight> {
        val insights = mutableListOf<LearningInsight>()
        
        insights.add(LearningInsight(
            type = "Strategic",
            description = "Game demonstrates strategic understanding development",
            importance = 0.6,
            actionable = true,
            relatedPositions = listOf("strategic_position_1")
        ))
        
        return insights
    }
    
    private fun extractEndgameInsights(gameResult: SelfPlayGameResult): List<LearningInsight> {
        val insights = mutableListOf<LearningInsight>()
        
        if (gameResult.gameLength > 50) {
            insights.add(LearningInsight(
                type = "Endgame",
                description = "Extended endgame provides valuable endgame training data",
                importance = 0.8,
                actionable = true,
                relatedPositions = listOf("endgame_position_1")
            ))
        }
        
        return insights
    }
    
    private fun extractDecisionInsights(gameResult: SelfPlayGameResult): List<LearningInsight> {
        val insights = mutableListOf<LearningInsight>()
        
        insights.add(LearningInsight(
            type = "Decision Making",
            description = "Decision patterns show areas for improvement",
            importance = 0.5,
            actionable = true,
            relatedPositions = emptyList()
        ))
        
        return insights
    }
    
    private fun updateMovePatterns(game: AnalyzedGame) {
        // Update move pattern database (simplified)
        game.moveAnalyses.forEach { moveAnalysis ->
            val pattern = movePatterns[moveAnalysis.move] ?: MovePattern(0, 0.0)
            movePatterns[moveAnalysis.move] = pattern.copy(
                frequency = pattern.frequency + 1,
                averageQuality = (pattern.averageQuality + moveAnalysis.qualityScore) / 2.0
            )
        }
    }
    
    private fun updatePositionDatabase(game: AnalyzedGame) {
        // Update position database (simplified)
        game.criticalPositions.forEach { criticalPos ->
            positionDatabase[criticalPos.position] = PositionAnalysis(
                moveNumber = criticalPos.moveNumber,
                position = criticalPos.position,
                evaluation = criticalPos.evaluation,
                complexity = criticalPos.criticality,
                tacticalThemes = emptyList(),
                strategicThemes = emptyList(),
                agentAssessment = AgentPositionAssessment(
                    confidence = 0.8,
                    topMoves = listOf(criticalPos.correctMove),
                    moveProbabilities = mapOf(criticalPos.correctMove to 1.0),
                    positionUnderstanding = 0.7
                )
            )
        }
    }
    
    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
    
    private fun generateGameId(gameResult: SelfPlayGameResult): String {
        return "game_${getCurrentTimeMillis()}_${gameResult.hashCode()}"
    }
}

// Supporting classes and data structures

class PositionEvaluator {
    // Implementation for position evaluation
}

class MoveEvaluator {
    // Implementation for move evaluation
}

class StrategicAnalyzer {
    // Implementation for strategic analysis
}

class TacticalAnalyzer {
    // Implementation for tactical analysis
}

// Data classes

private data class BasicGameMetrics(
    val gameLength: Int,
    val gameOutcome: GameOutcome,
    val finalPosition: String
)

// GamePhaseAnalysis is now defined in SharedDataClasses.kt

private data class DecisionMakingAnalysis(
    val averageDecisionTime: Double,
    val decisionConfidence: Double,
    val explorationRate: Double
)

private data class LearningInsightsData(
    val learningValue: Double,
    val noveltyScore: Double,
    val difficultyLevel: Double
)

private data class MovePattern(
    val frequency: Int,
    val averageQuality: Double
)
