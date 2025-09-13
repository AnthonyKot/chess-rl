package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*
import kotlin.math.*

/**
 * Manual validation tools for RL training that provide human-readable analysis
 * and visualization of agent decision-making processes
 */
class ManualValidationTools(
    private val agent: ChessAgent,
    private val environment: ChessEnvironment,
    private val config: ManualValidationConfig = ManualValidationConfig()
) {
    
    private val stateEncoder = ChessStateEncoder()
    private val actionEncoder = ChessActionEncoder()
    
    /**
     * Visualize agent decision-making process for a given position
     */
    fun visualizeAgentDecisionMaking(
        board: ChessBoard,
        showTopMoves: Int = config.topMovesToShow
    ): AgentDecisionAnalysis {
        
        // Encode current position
        val state = stateEncoder.encode(board)
        val validMoves = environment.getValidActions(state)
        val validChessMoves = validMoves.map { actionEncoder.decodeAction(it) }
        
        // Get agent's action probabilities and Q-values
        val actionProbabilities = agent.getActionProbabilities(state, validMoves)
        val qValues = agent.getQValues(state, validMoves)
        
        // Create move analysis for each valid move
        val moveAnalyses = validChessMoves.mapIndexed { index, move ->
            val actionIndex = validMoves[index]
            val probability = actionProbabilities[actionIndex] ?: 0.0
            val qValue = qValues[actionIndex] ?: 0.0
            
            MoveAnalysis(
                move = move,
                probability = probability,
                qValue = qValue,
                rank = 0 // Will be set after sorting
            )
        }.sortedByDescending { it.probability }
            .mapIndexed { index, analysis -> analysis.copy(rank = index + 1) }
            .take(showTopMoves)
        
        // Get position evaluation
        val positionEval = environment.getPositionEvaluation(board.getActiveColor())
        
        // Calculate decision confidence (entropy-based)
        val entropy = calculateEntropy(actionProbabilities.values.toList())
        val confidence = 1.0 - (entropy / ln(validMoves.size.toDouble()))
        
        return AgentDecisionAnalysis(
            position = board.toFEN(),
            activeColor = board.getActiveColor(),
            validMovesCount = validMoves.size,
            topMoves = moveAnalyses,
            positionEvaluation = positionEval,
            decisionConfidence = confidence,
            entropy = entropy
        )
    }
    
    /**
     * Display neural network outputs as move probabilities with chess notation
     */
    fun displayMoveProbabilities(
        board: ChessBoard,
        includeInvalidMoves: Boolean = false
    ): MoveProbabilityDisplay {
        
        val state = stateEncoder.encode(board)
        val validMoves = environment.getValidActions(state)
        val actionProbabilities = agent.getActionProbabilities(state, validMoves)
        
        // Create probability entries for valid moves
        val validProbabilities = validMoves.map { actionIndex ->
            val move = actionEncoder.decodeAction(actionIndex)
            val probability = actionProbabilities[actionIndex] ?: 0.0
            
            MoveProbabilityEntry(
                move = move,
                algebraicNotation = move.toAlgebraic(),
                probability = probability,
                isValid = true
            )
        }.sortedByDescending { it.probability }
        
        // Optionally include top invalid moves for analysis
        val invalidProbabilities = if (includeInvalidMoves) {
            val allActions = (0 until ChessActionEncoder.ACTION_SPACE_SIZE).toList()
            val invalidActions = allActions - validMoves.toSet()
            
            // Get probabilities for all actions (including invalid)
            val allActionProbs = agent.getActionProbabilities(state, allActions)
            
            invalidActions.map { actionIndex ->
                val move = actionEncoder.decodeAction(actionIndex)
                val probability = allActionProbs[actionIndex] ?: 0.0
                
                MoveProbabilityEntry(
                    move = move,
                    algebraicNotation = move.toAlgebraic(),
                    probability = probability,
                    isValid = false
                )
            }.sortedByDescending { it.probability }
                .take(config.topInvalidMovesToShow)
        } else {
            emptyList()
        }
        
        return MoveProbabilityDisplay(
            position = board.toFEN(),
            validMoves = validProbabilities,
            invalidMoves = invalidProbabilities,
            totalProbabilityMass = validProbabilities.sumOf { it.probability }
        )
    }
    
    /**
     * Assess game quality for human evaluation
     */
    fun assessGameQuality(
        gameHistory: List<Move>,
        gameResult: GameStatus,
        finalBoard: ChessBoard
    ): GameQualityAssessment {
        
        val gameLength = gameHistory.size
        val averageMoveTime = 1.0 // Placeholder - would need actual timing data
        
        // Analyze move quality
        val moveQuality = analyzeMoveQuality(gameHistory, finalBoard)
        
        // Analyze game progression
        val progression = analyzeGameProgression(gameHistory)
        
        // Calculate overall quality score
        val qualityScore = calculateGameQualityScore(
            gameLength, moveQuality, progression, gameResult
        )
        
        // Generate quality insights
        val insights = generateGameQualityInsights(
            gameLength, moveQuality, progression, gameResult
        )
        
        return GameQualityAssessment(
            gameLength = gameLength,
            gameResult = gameResult,
            qualityScore = qualityScore,
            moveQuality = moveQuality,
            gameProgression = progression,
            insights = insights,
            averageMoveTime = averageMoveTime
        )
    }
    
    /**
     * Create position evaluation display showing network's assessment
     */
    fun displayPositionEvaluation(
        board: ChessBoard,
        includeFeatureBreakdown: Boolean = true
    ): PositionEvaluationDisplay {
        
        val state = stateEncoder.encode(board)
        
        // Get evaluations for both colors
        val whiteEval = environment.getPositionEvaluation(PieceColor.WHITE)
        val blackEval = environment.getPositionEvaluation(PieceColor.BLACK)
        val netEvaluation = whiteEval - blackEval
        
        // Get agent's assessment through Q-values
        val validMoves = environment.getValidActions(state)
        val qValues = agent.getQValues(state, validMoves)
        val bestQValue = qValues.values.maxOrNull() ?: 0.0
        val worstQValue = qValues.values.minOrNull() ?: 0.0
        val averageQValue = qValues.values.average()
        
        // Feature breakdown (if requested)
        val featureBreakdown = if (includeFeatureBreakdown) {
            analyzePositionFeatures(board)
        } else {
            null
        }
        
        // Tactical assessment
        val tacticalAssessment = analyzeTacticalFeatures(board)
        
        return PositionEvaluationDisplay(
            position = board.toFEN(),
            activeColor = board.getActiveColor(),
            whiteEvaluation = whiteEval,
            blackEvaluation = blackEval,
            netEvaluation = netEvaluation,
            agentQValueRange = QValueRange(bestQValue, worstQValue, averageQValue),
            featureBreakdown = featureBreakdown,
            tacticalAssessment = tacticalAssessment,
            evaluationSummary = generateEvaluationSummary(netEvaluation, bestQValue)
        )
    }
    
    /**
     * Manually inspect and validate specific training scenarios
     */
    fun inspectTrainingScenario(
        scenario: TrainingScenario
    ): ScenarioInspectionResult {
        
        val results = mutableListOf<ScenarioStepResult>()
        
        // Reset environment to scenario starting position
        if (scenario.startingFEN != null) {
            environment.loadFromFEN(scenario.startingFEN)
        } else {
            environment.reset()
        }
        
        var currentBoard = environment.getCurrentBoard()
        var stepCount = 0
        
        // Execute scenario steps
        for (expectedMove in scenario.expectedMoves) {
            stepCount++
            
            // Get agent's decision
            val state = stateEncoder.encode(currentBoard)
            val validMoves = environment.getValidActions(state)
            val agentAction = agent.selectAction(state, validMoves)
            val agentMove = actionEncoder.decodeAction(agentAction)
            
            // Compare with expected move
            val moveMatches = movesMatch(agentMove, expectedMove)
            
            // Get decision analysis
            val decisionAnalysis = visualizeAgentDecisionMaking(currentBoard)
            
            // Execute the expected move (not agent's move) to continue scenario
            val stepResult = environment.step(actionEncoder.encodeMove(expectedMove))
            currentBoard = environment.getCurrentBoard()
            
            results.add(ScenarioStepResult(
                stepNumber = stepCount,
                expectedMove = expectedMove,
                agentMove = agentMove,
                moveMatches = moveMatches,
                decisionAnalysis = decisionAnalysis,
                positionAfterMove = currentBoard.toFEN(),
                reward = stepResult.reward
            ))
            
            if (stepResult.done) break
        }
        
        // Calculate scenario performance
        val correctMoves = results.count { it.moveMatches }
        val accuracy = correctMoves.toDouble() / results.size
        
        // Generate insights
        val insights = generateScenarioInsights(results, accuracy)
        
        return ScenarioInspectionResult(
            scenarioName = scenario.name,
            totalSteps = results.size,
            correctMoves = correctMoves,
            accuracy = accuracy,
            stepResults = results,
            insights = insights
        )
    }
    
    /**
     * Create interactive game analysis session
     */
    fun createInteractiveAnalysis(
        gameHistory: List<Move>,
        annotations: Map<Int, String> = emptyMap()
    ): InteractiveGameAnalysis {
        
        val moveAnalyses = mutableListOf<InteractiveMoveAnalysis>()
        val board = ChessBoard()
        
        gameHistory.forEachIndexed { index, move ->
            // Analyze position before move
            val positionBefore = board.copy()
            val decisionAnalysis = visualizeAgentDecisionMaking(positionBefore)
            
            // Execute move
            board.makeLegalMove(move)
            board.switchActiveColor()
            
            // Create move analysis
            val moveAnalysis = InteractiveMoveAnalysis(
                moveNumber = index + 1,
                move = move,
                positionBefore = positionBefore.toFEN(),
                positionAfter = board.toFEN(),
                decisionAnalysis = decisionAnalysis,
                annotation = annotations[index],
                wasTopChoice = decisionAnalysis.topMoves.firstOrNull()?.move?.let { 
                    movesMatch(it, move) 
                } ?: false
            )
            
            moveAnalyses.add(moveAnalysis)
        }
        
        return InteractiveGameAnalysis(
            gameHistory = gameHistory,
            moveAnalyses = moveAnalyses,
            finalPosition = board.toFEN()
        )
    }
    
    /**
     * Generate training validation report
     */
    fun generateValidationReport(
        testScenarios: List<TrainingScenario>,
        sampleGames: List<List<Move>>
    ): ValidationReport {
        
        // Test scenarios
        val scenarioResults = testScenarios.map { scenario ->
            inspectTrainingScenario(scenario)
        }
        
        // Analyze sample games
        val gameQualityResults = sampleGames.map { gameHistory ->
            val finalBoard = ChessBoard()
            gameHistory.forEach { move ->
                finalBoard.makeLegalMove(move)
                finalBoard.switchActiveColor()
            }
            val gameStatus = environment.getGameStatus()
            assessGameQuality(gameHistory, gameStatus, finalBoard)
        }
        
        // Calculate overall metrics
        val overallAccuracy = scenarioResults.map { it.accuracy }.average()
        val averageGameQuality = gameQualityResults.map { it.qualityScore }.average()
        
        // Generate recommendations
        val recommendations = generateValidationRecommendations(
            scenarioResults, gameQualityResults, overallAccuracy, averageGameQuality
        )
        
        return ValidationReport(
            scenarioResults = scenarioResults,
            gameQualityResults = gameQualityResults,
            overallAccuracy = overallAccuracy,
            averageGameQuality = averageGameQuality,
            recommendations = recommendations,
            generatedAt = getCurrentTimeMillis()
        )
    }
    
    // Private helper methods
    
    private fun calculateEntropy(probabilities: List<Double>): Double {
        return probabilities.filter { it > 0.0 }
            .sumOf { -it * ln(it) }
    }
    
    private fun analyzeMoveQuality(
        gameHistory: List<Move>,
        finalBoard: ChessBoard
    ): MoveQualityAnalysis {
        
        // Analyze different aspects of move quality
        val developmentMoves = gameHistory.take(10).count { move ->
            isKnightOrBishopDevelopment(move)
        }
        
        val centerControlMoves = gameHistory.count { move ->
            controlsCenterSquares(move)
        }
        
        val tacticalMoves = gameHistory.count { move ->
            isTacticalMove(move, finalBoard)
        }
        
        return MoveQualityAnalysis(
            developmentMoves = developmentMoves,
            centerControlMoves = centerControlMoves,
            tacticalMoves = tacticalMoves,
            totalMoves = gameHistory.size,
            qualityScore = calculateMoveQualityScore(
                developmentMoves, centerControlMoves, tacticalMoves, gameHistory.size
            )
        )
    }
    
    private fun analyzeGameProgression(gameHistory: List<Move>): GameProgressionAnalysis {
        val openingLength = minOf(gameHistory.size, 20)
        val middlegameStart = openingLength
        val middlegameLength = maxOf(0, gameHistory.size - openingLength - 10)
        val endgameLength = maxOf(0, gameHistory.size - openingLength - middlegameLength)
        
        return GameProgressionAnalysis(
            openingLength = openingLength,
            middlegameLength = middlegameLength,
            endgameLength = endgameLength,
            gamePhaseBalance = calculatePhaseBalance(openingLength, middlegameLength, endgameLength)
        )
    }
    
    private fun calculateGameQualityScore(
        gameLength: Int,
        moveQuality: MoveQualityAnalysis,
        progression: GameProgressionAnalysis,
        gameResult: GameStatus
    ): Double {
        var score = 0.5 // Base score
        
        // Length factor (prefer reasonable game lengths)
        val lengthFactor = when {
            gameLength < 20 -> 0.3 // Too short
            gameLength in 20..100 -> 1.0 // Good length
            gameLength in 101..150 -> 0.8 // Acceptable
            else -> 0.5 // Too long
        }
        
        // Move quality factor
        val qualityFactor = moveQuality.qualityScore
        
        // Game result factor
        val resultFactor = when (gameResult) {
            GameStatus.WHITE_WINS, GameStatus.BLACK_WINS -> 1.0 // Decisive
            GameStatus.DRAW_STALEMATE, GameStatus.DRAW_INSUFFICIENT_MATERIAL -> 0.8 // Draw
            else -> 0.6 // Other outcomes
        }
        
        return (score * lengthFactor * qualityFactor * resultFactor).coerceIn(0.0, 1.0)
    }
    
    private fun generateGameQualityInsights(
        gameLength: Int,
        moveQuality: MoveQualityAnalysis,
        progression: GameProgressionAnalysis,
        gameResult: GameStatus
    ): List<String> {
        val insights = mutableListOf<String>()
        
        when {
            gameLength < 20 -> insights.add("Game ended very quickly - may indicate tactical oversight")
            gameLength > 150 -> insights.add("Very long game - may indicate indecisive play")
            else -> insights.add("Game length is reasonable")
        }
        
        if (moveQuality.developmentMoves < 3) {
            insights.add("Limited piece development in opening")
        }
        
        if (moveQuality.centerControlMoves < gameLength * 0.1) {
            insights.add("Insufficient center control throughout game")
        }
        
        if (progression.gamePhaseBalance < 0.5) {
            insights.add("Unbalanced game phases - consider strategic planning")
        }
        
        return insights
    }
    
    private fun analyzePositionFeatures(board: ChessBoard): PositionFeatureBreakdown {
        val evaluator = ChessPositionEvaluator()
        
        val whiteMaterial = evaluator.evaluateMaterial(board, PieceColor.WHITE)
        val blackMaterial = evaluator.evaluateMaterial(board, PieceColor.BLACK)
        
        val whiteActivity = evaluator.evaluatePieceActivity(board, PieceColor.WHITE)
        val blackActivity = evaluator.evaluatePieceActivity(board, PieceColor.BLACK)
        
        val whiteKingSafety = evaluator.evaluateKingSafety(board, PieceColor.WHITE)
        val blackKingSafety = evaluator.evaluateKingSafety(board, PieceColor.BLACK)
        
        val whiteDevelopment = evaluator.evaluateDevelopment(board, PieceColor.WHITE)
        val blackDevelopment = evaluator.evaluateDevelopment(board, PieceColor.BLACK)
        
        return PositionFeatureBreakdown(
            materialBalance = whiteMaterial - blackMaterial,
            activityBalance = whiteActivity - blackActivity,
            kingSafetyBalance = whiteKingSafety - blackKingSafety,
            developmentBalance = whiteDevelopment - blackDevelopment,
            whiteMaterial = whiteMaterial,
            blackMaterial = blackMaterial,
            whiteActivity = whiteActivity,
            blackActivity = blackActivity
        )
    }
    
    private fun analyzeTacticalFeatures(board: ChessBoard): TacticalAssessment {
        val gameStateDetector = GameStateDetector()
        
        val whiteInCheck = gameStateDetector.isInCheck(board, PieceColor.WHITE)
        val blackInCheck = gameStateDetector.isInCheck(board, PieceColor.BLACK)
        
        // Count attacked pieces (simplified tactical analysis)
        val attackedPieces = countAttackedPieces(board)
        val defendedPieces = countDefendedPieces(board)
        
        return TacticalAssessment(
            checksPresent = whiteInCheck || blackInCheck,
            attackedPieces = attackedPieces,
            defendedPieces = defendedPieces,
            tacticalComplexity = (attackedPieces + defendedPieces) / 10.0
        )
    }
    
    private fun generateEvaluationSummary(netEvaluation: Double, bestQValue: Double): String {
        val positionAssessment = when {
            netEvaluation > 0.5 -> "White has a significant advantage"
            netEvaluation > 0.1 -> "White has a slight advantage"
            netEvaluation > -0.1 -> "Position is roughly equal"
            netEvaluation > -0.5 -> "Black has a slight advantage"
            else -> "Black has a significant advantage"
        }
        
        val confidenceAssessment = when {
            bestQValue > 1.0 -> "Agent is confident in this position"
            bestQValue > 0.0 -> "Agent is moderately optimistic"
            bestQValue > -1.0 -> "Agent is cautious about this position"
            else -> "Agent sees this position as problematic"
        }
        
        return "$positionAssessment. $confidenceAssessment."
    }
    
    private fun movesMatch(move1: Move, move2: Move): Boolean {
        return move1.from == move2.from && move1.to == move2.to &&
               (move1.promotion == move2.promotion || 
                (move1.promotion == null && move2.promotion == null))
    }
    
    private fun generateScenarioInsights(
        results: List<ScenarioStepResult>,
        accuracy: Double
    ): List<String> {
        val insights = mutableListOf<String>()
        
        when {
            accuracy >= 0.9 -> insights.add("Excellent performance on this scenario")
            accuracy >= 0.7 -> insights.add("Good performance with room for improvement")
            accuracy >= 0.5 -> insights.add("Moderate performance - needs more training")
            else -> insights.add("Poor performance - significant training needed")
        }
        
        val incorrectMoves = results.filter { !it.moveMatches }
        if (incorrectMoves.isNotEmpty()) {
            insights.add("${incorrectMoves.size} moves differed from expected")
            
            // Analyze common issues
            val lowConfidenceMoves = incorrectMoves.filter { 
                it.decisionAnalysis.decisionConfidence < 0.5 
            }
            if (lowConfidenceMoves.isNotEmpty()) {
                insights.add("${lowConfidenceMoves.size} incorrect moves had low confidence")
            }
        }
        
        return insights
    }
    
    private fun generateValidationRecommendations(
        scenarioResults: List<ScenarioInspectionResult>,
        gameQualityResults: List<GameQualityAssessment>,
        overallAccuracy: Double,
        averageGameQuality: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (overallAccuracy < 0.7) {
            recommendations.add("Overall accuracy is low - increase training time or adjust hyperparameters")
        }
        
        if (averageGameQuality < 0.6) {
            recommendations.add("Game quality is below average - consider reward shaping or curriculum learning")
        }
        
        val poorScenarios = scenarioResults.filter { it.accuracy < 0.5 }
        if (poorScenarios.isNotEmpty()) {
            recommendations.add("${poorScenarios.size} scenarios show poor performance - focus training on these areas")
        }
        
        val shortGames = gameQualityResults.filter { it.gameLength < 20 }
        if (shortGames.size > gameQualityResults.size * 0.3) {
            recommendations.add("Many games are ending too quickly - check for tactical oversights")
        }
        
        return recommendations
    }
    
    // Simplified helper methods for move analysis
    
    private fun isKnightOrBishopDevelopment(move: Move): Boolean {
        // Simplified check - would need access to piece information
        return move.from.rank in listOf(0, 7) && move.to.rank in listOf(2, 3, 4, 5)
    }
    
    private fun controlsCenterSquares(move: Move): Boolean {
        val centerSquares = setOf(
            Position(3, 3), Position(3, 4), Position(4, 3), Position(4, 4)
        )
        return move.to in centerSquares
    }
    
    private fun isTacticalMove(move: Move, board: ChessBoard): Boolean {
        // Simplified tactical detection - would need more sophisticated analysis
        return false // Placeholder
    }
    
    private fun calculateMoveQualityScore(
        developmentMoves: Int,
        centerControlMoves: Int,
        tacticalMoves: Int,
        totalMoves: Int
    ): Double {
        val developmentRatio = developmentMoves.toDouble() / minOf(totalMoves, 10)
        val centerRatio = centerControlMoves.toDouble() / totalMoves
        val tacticalRatio = tacticalMoves.toDouble() / totalMoves
        
        return (developmentRatio * 0.4 + centerRatio * 0.3 + tacticalRatio * 0.3).coerceIn(0.0, 1.0)
    }
    
    private fun calculatePhaseBalance(
        openingLength: Int,
        middlegameLength: Int,
        endgameLength: Int
    ): Double {
        val total = openingLength + middlegameLength + endgameLength
        if (total == 0) return 0.0
        
        val idealOpening = 0.3
        val idealMiddlegame = 0.5
        val idealEndgame = 0.2
        
        val actualOpening = openingLength.toDouble() / total
        val actualMiddlegame = middlegameLength.toDouble() / total
        val actualEndgame = endgameLength.toDouble() / total
        
        val deviation = abs(actualOpening - idealOpening) +
                       abs(actualMiddlegame - idealMiddlegame) +
                       abs(actualEndgame - idealEndgame)
        
        return (1.0 - deviation / 2.0).coerceIn(0.0, 1.0)
    }
    
    private fun countAttackedPieces(board: ChessBoard): Int {
        // Simplified implementation - would need full attack detection
        return 0
    }
    
    private fun countDefendedPieces(board: ChessBoard): Int {
        // Simplified implementation - would need full defense detection
        return 0
    }
}

/**
 * Configuration for manual validation tools
 */
data class ManualValidationConfig(
    val topMovesToShow: Int = 5,
    val topInvalidMovesToShow: Int = 3,
    val includeFeatureBreakdown: Boolean = true,
    val showConfidenceThreshold: Double = 0.1
)

// Data classes for validation results

data class AgentDecisionAnalysis(
    val position: String,
    val activeColor: PieceColor,
    val validMovesCount: Int,
    val topMoves: List<MoveAnalysis>,
    val positionEvaluation: Double,
    val decisionConfidence: Double,
    val entropy: Double
)

data class MoveAnalysis(
    val move: Move,
    val probability: Double,
    val qValue: Double,
    val rank: Int
)

data class MoveProbabilityDisplay(
    val position: String,
    val validMoves: List<MoveProbabilityEntry>,
    val invalidMoves: List<MoveProbabilityEntry>,
    val totalProbabilityMass: Double
)

data class MoveProbabilityEntry(
    val move: Move,
    val algebraicNotation: String,
    val probability: Double,
    val isValid: Boolean
)

data class GameQualityAssessment(
    val gameLength: Int,
    val gameResult: GameStatus,
    val qualityScore: Double,
    val moveQuality: MoveQualityAnalysis,
    val gameProgression: GameProgressionAnalysis,
    val insights: List<String>,
    val averageMoveTime: Double
)

data class MoveQualityAnalysis(
    val developmentMoves: Int,
    val centerControlMoves: Int,
    val tacticalMoves: Int,
    val totalMoves: Int,
    val qualityScore: Double
)

data class GameProgressionAnalysis(
    val openingLength: Int,
    val middlegameLength: Int,
    val endgameLength: Int,
    val gamePhaseBalance: Double
)

data class PositionEvaluationDisplay(
    val position: String,
    val activeColor: PieceColor,
    val whiteEvaluation: Double,
    val blackEvaluation: Double,
    val netEvaluation: Double,
    val agentQValueRange: QValueRange,
    val featureBreakdown: PositionFeatureBreakdown?,
    val tacticalAssessment: TacticalAssessment,
    val evaluationSummary: String
)

data class QValueRange(
    val best: Double,
    val worst: Double,
    val average: Double
)

data class PositionFeatureBreakdown(
    val materialBalance: Int,
    val activityBalance: Int,
    val kingSafetyBalance: Double,
    val developmentBalance: Int,
    val whiteMaterial: Int,
    val blackMaterial: Int,
    val whiteActivity: Int,
    val blackActivity: Int
)

data class TacticalAssessment(
    val checksPresent: Boolean,
    val attackedPieces: Int,
    val defendedPieces: Int,
    val tacticalComplexity: Double
)

data class TrainingScenario(
    val name: String,
    val description: String,
    val startingFEN: String?,
    val expectedMoves: List<Move>,
    val category: String = "general"
)

data class ScenarioInspectionResult(
    val scenarioName: String,
    val totalSteps: Int,
    val correctMoves: Int,
    val accuracy: Double,
    val stepResults: List<ScenarioStepResult>,
    val insights: List<String>
)

data class ScenarioStepResult(
    val stepNumber: Int,
    val expectedMove: Move,
    val agentMove: Move,
    val moveMatches: Boolean,
    val decisionAnalysis: AgentDecisionAnalysis,
    val positionAfterMove: String,
    val reward: Double
)

data class InteractiveGameAnalysis(
    val gameHistory: List<Move>,
    val moveAnalyses: List<InteractiveMoveAnalysis>,
    val finalPosition: String
)

data class InteractiveMoveAnalysis(
    val moveNumber: Int,
    val move: Move,
    val positionBefore: String,
    val positionAfter: String,
    val decisionAnalysis: AgentDecisionAnalysis,
    val annotation: String?,
    val wasTopChoice: Boolean
)

data class ValidationReport(
    val scenarioResults: List<ScenarioInspectionResult>,
    val gameQualityResults: List<GameQualityAssessment>,
    val overallAccuracy: Double,
    val averageGameQuality: Double,
    val recommendations: List<String>,
    val generatedAt: Long
)