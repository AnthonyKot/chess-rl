package com.chessrl.integration

import com.chessrl.chess.*

/**
 * Console interface for manual validation tools that provides human-readable
 * output and interactive analysis capabilities
 */
class ValidationConsole(
    private val validationTools: ManualValidationTools,
    private val config: ConsoleConfig = ConsoleConfig()
) {
    
    /**
     * Display agent decision-making analysis in console format
     */
    fun displayDecisionAnalysis(analysis: AgentDecisionAnalysis): String {
        val output = StringBuilder()
        
        output.appendLine("=== AGENT DECISION ANALYSIS ===")
        output.appendLine("Position: ${analysis.position}")
        output.appendLine("Active Color: ${analysis.activeColor}")
        output.appendLine("Valid Moves: ${analysis.validMovesCount}")
        output.appendLine("Decision Confidence: ${formatPercentage(analysis.decisionConfidence)}")
        output.appendLine("Position Evaluation: ${formatEvaluation(analysis.positionEvaluation)}")
        output.appendLine("Policy Entropy: ${formatDecimal(analysis.entropy, 3)}")
        output.appendLine()
        
        output.appendLine("TOP MOVE CANDIDATES:")
        output.appendLine("Rank | Move     | Probability | Q-Value")
        output.appendLine("-----|----------|-------------|--------")
        
        analysis.topMoves.forEach { moveAnalysis ->
            output.appendLine(
                "${moveAnalysis.rank.toString().padStart(4)} | " +
                "${moveAnalysis.move.toAlgebraic().padEnd(8)} | " +
                "${formatPercentage(moveAnalysis.probability).padStart(11)} | " +
                "${formatDecimal(moveAnalysis.qValue, 2).padStart(7)}"
            )
        }
        
        return output.toString()
    }
    
    /**
     * Display move probabilities in console format
     */
    fun displayMoveProbabilities(display: MoveProbabilityDisplay): String {
        val output = StringBuilder()
        
        output.appendLine("=== MOVE PROBABILITY ANALYSIS ===")
        output.appendLine("Position: ${display.position}")
        output.appendLine("Total Probability Mass: ${formatPercentage(display.totalProbabilityMass)}")
        output.appendLine()
        
        output.appendLine("VALID MOVES:")
        output.appendLine("Move     | Notation | Probability")
        output.appendLine("---------|----------|------------")
        
        display.validMoves.take(config.maxMovesToDisplay).forEach { entry ->
            output.appendLine(
                "${entry.move.toAlgebraic().padEnd(8)} | " +
                "${entry.algebraicNotation.padEnd(8)} | " +
                "${formatPercentage(entry.probability).padStart(11)}"
            )
        }
        
        if (display.invalidMoves.isNotEmpty()) {
            output.appendLine()
            output.appendLine("TOP INVALID MOVES (for debugging):")
            output.appendLine("Move     | Notation | Probability")
            output.appendLine("---------|----------|------------")
            
            display.invalidMoves.forEach { entry ->
                output.appendLine(
                    "${entry.move.toAlgebraic().padEnd(8)} | " +
                    "${entry.algebraicNotation.padEnd(8)} | " +
                    "${formatPercentage(entry.probability).padStart(11)}"
                )
            }
        }
        
        return output.toString()
    }
    
    /**
     * Display game quality assessment in console format
     */
    fun displayGameQuality(assessment: GameQualityAssessment): String {
        val output = StringBuilder()
        
        output.appendLine("=== GAME QUALITY ASSESSMENT ===")
        output.appendLine("Game Length: ${assessment.gameLength} moves")
        output.appendLine("Game Result: ${assessment.gameResult}")
        output.appendLine("Overall Quality Score: ${formatPercentage(assessment.qualityScore)}")
        output.appendLine("Average Move Time: ${formatDecimal(assessment.averageMoveTime, 2)}s")
        output.appendLine()
        
        output.appendLine("MOVE QUALITY ANALYSIS:")
        val moveQuality = assessment.moveQuality
        output.appendLine("  Development Moves: ${moveQuality.developmentMoves}/${minOf(moveQuality.totalMoves, 10)}")
        output.appendLine("  Center Control Moves: ${moveQuality.centerControlMoves}/${moveQuality.totalMoves}")
        output.appendLine("  Tactical Moves: ${moveQuality.tacticalMoves}/${moveQuality.totalMoves}")
        output.appendLine("  Move Quality Score: ${formatPercentage(moveQuality.qualityScore)}")
        output.appendLine()
        
        output.appendLine("GAME PROGRESSION:")
        val progression = assessment.gameProgression
        output.appendLine("  Opening: ${progression.openingLength} moves")
        output.appendLine("  Middlegame: ${progression.middlegameLength} moves")
        output.appendLine("  Endgame: ${progression.endgameLength} moves")
        output.appendLine("  Phase Balance: ${formatPercentage(progression.gamePhaseBalance)}")
        output.appendLine()
        
        if (assessment.insights.isNotEmpty()) {
            output.appendLine("INSIGHTS:")
            assessment.insights.forEach { insight ->
                output.appendLine("  • $insight")
            }
        }
        
        return output.toString()
    }
    
    /**
     * Display position evaluation in console format
     */
    fun displayPositionEvaluation(evaluation: PositionEvaluationDisplay): String {
        val output = StringBuilder()
        
        output.appendLine("=== POSITION EVALUATION ===")
        output.appendLine("Position: ${evaluation.position}")
        output.appendLine("Active Color: ${evaluation.activeColor}")
        output.appendLine()
        
        output.appendLine("EVALUATION SUMMARY:")
        output.appendLine("  ${evaluation.evaluationSummary}")
        output.appendLine()
        
        output.appendLine("DETAILED EVALUATION:")
        output.appendLine("  White Evaluation: ${formatEvaluation(evaluation.whiteEvaluation)}")
        output.appendLine("  Black Evaluation: ${formatEvaluation(evaluation.blackEvaluation)}")
        output.appendLine("  Net Evaluation: ${formatEvaluation(evaluation.netEvaluation)}")
        output.appendLine()
        
        output.appendLine("AGENT Q-VALUE ASSESSMENT:")
        val qRange = evaluation.agentQValueRange
        output.appendLine("  Best Q-Value: ${formatDecimal(qRange.best, 2)}")
        output.appendLine("  Worst Q-Value: ${formatDecimal(qRange.worst, 2)}")
        output.appendLine("  Average Q-Value: ${formatDecimal(qRange.average, 2)}")
        output.appendLine()
        
        evaluation.featureBreakdown?.let { features ->
            output.appendLine("FEATURE BREAKDOWN:")
            output.appendLine("  Material Balance: ${features.materialBalance} (W:${features.whiteMaterial}, B:${features.blackMaterial})")
            output.appendLine("  Activity Balance: ${features.activityBalance} (W:${features.whiteActivity}, B:${features.blackActivity})")
            output.appendLine("  King Safety Balance: ${formatDecimal(features.kingSafetyBalance, 2)}")
            output.appendLine("  Development Balance: ${features.developmentBalance}")
            output.appendLine()
        }
        
        output.appendLine("TACTICAL ASSESSMENT:")
        val tactical = evaluation.tacticalAssessment
        output.appendLine("  Checks Present: ${if (tactical.checksPresent) "Yes" else "No"}")
        output.appendLine("  Attacked Pieces: ${tactical.attackedPieces}")
        output.appendLine("  Defended Pieces: ${tactical.defendedPieces}")
        output.appendLine("  Tactical Complexity: ${formatDecimal(tactical.tacticalComplexity, 2)}")
        
        return output.toString()
    }
    
    /**
     * Display scenario inspection results in console format
     */
    fun displayScenarioInspection(result: ScenarioInspectionResult): String {
        val output = StringBuilder()
        
        output.appendLine("=== SCENARIO INSPECTION: ${result.scenarioName} ===")
        output.appendLine("Total Steps: ${result.totalSteps}")
        output.appendLine("Correct Moves: ${result.correctMoves}")
        output.appendLine("Accuracy: ${formatPercentage(result.accuracy)}")
        output.appendLine()
        
        output.appendLine("STEP-BY-STEP ANALYSIS:")
        output.appendLine("Step | Expected    | Agent       | Match | Confidence | Reward")
        output.appendLine("-----|-------------|-------------|-------|------------|-------")
        
        result.stepResults.forEach { step ->
            val match = if (step.moveMatches) "✓" else "✗"
            val confidence = formatPercentage(step.decisionAnalysis.decisionConfidence)
            val reward = formatDecimal(step.reward, 2)
            
            output.appendLine(
                "${step.stepNumber.toString().padStart(4)} | " +
                "${step.expectedMove.toAlgebraic().padEnd(11)} | " +
                "${step.agentMove.toAlgebraic().padEnd(11)} | " +
                "${match.padStart(5)} | " +
                "${confidence.padStart(10)} | " +
                "${reward.padStart(6)}"
            )
        }
        
        if (result.insights.isNotEmpty()) {
            output.appendLine()
            output.appendLine("INSIGHTS:")
            result.insights.forEach { insight ->
                output.appendLine("  • $insight")
            }
        }
        
        return output.toString()
    }
    
    /**
     * Display interactive game analysis in console format
     */
    fun displayInteractiveAnalysis(analysis: InteractiveGameAnalysis): String {
        val output = StringBuilder()
        
        output.appendLine("=== INTERACTIVE GAME ANALYSIS ===")
        output.appendLine("Total Moves: ${analysis.gameHistory.size}")
        output.appendLine("Final Position: ${analysis.finalPosition}")
        output.appendLine()
        
        output.appendLine("MOVE-BY-MOVE ANALYSIS:")
        output.appendLine("Move | Played      | Top Choice | Confidence | Annotation")
        output.appendLine("-----|-------------|------------|------------|------------")
        
        analysis.moveAnalyses.take(config.maxMovesToAnalyze).forEach { moveAnalysis ->
            val topChoice = if (moveAnalysis.wasTopChoice) "✓" else "✗"
            val confidence = formatPercentage(moveAnalysis.decisionAnalysis.decisionConfidence)
            val annotation = moveAnalysis.annotation ?: ""
            
            output.appendLine(
                "${moveAnalysis.moveNumber.toString().padStart(4)} | " +
                "${moveAnalysis.move.toAlgebraic().padEnd(11)} | " +
                "${topChoice.padStart(10)} | " +
                "${confidence.padStart(10)} | " +
                annotation
            )
        }
        
        if (analysis.moveAnalyses.size > config.maxMovesToAnalyze) {
            output.appendLine("... (${analysis.moveAnalyses.size - config.maxMovesToAnalyze} more moves)")
        }
        
        return output.toString()
    }
    
    /**
     * Display validation report in console format
     */
    fun displayValidationReport(report: ValidationReport): String {
        val output = StringBuilder()
        
        output.appendLine("=== TRAINING VALIDATION REPORT ===")
        output.appendLine("Generated: ${formatTimestamp(report.generatedAt)}")
        output.appendLine("Overall Accuracy: ${formatPercentage(report.overallAccuracy)}")
        output.appendLine("Average Game Quality: ${formatPercentage(report.averageGameQuality)}")
        output.appendLine()
        
        output.appendLine("SCENARIO RESULTS:")
        output.appendLine("Scenario                    | Steps | Correct | Accuracy")
        output.appendLine("----------------------------|-------|---------|----------")
        
        report.scenarioResults.forEach { scenario ->
            output.appendLine(
                "${scenario.scenarioName.take(27).padEnd(27)} | " +
                "${scenario.totalSteps.toString().padStart(5)} | " +
                "${scenario.correctMoves.toString().padStart(7)} | " +
                "${formatPercentage(scenario.accuracy).padStart(8)}"
            )
        }
        
        output.appendLine()
        output.appendLine("GAME QUALITY SUMMARY:")
        output.appendLine("Games Analyzed: ${report.gameQualityResults.size}")
        
        val qualityDistribution = report.gameQualityResults.groupBy { 
            when {
                it.qualityScore >= 0.8 -> "Excellent"
                it.qualityScore >= 0.6 -> "Good"
                it.qualityScore >= 0.4 -> "Fair"
                else -> "Poor"
            }
        }
        
        qualityDistribution.forEach { (quality, games) ->
            output.appendLine("  $quality: ${games.size} games (${formatPercentage(games.size.toDouble() / report.gameQualityResults.size)})")
        }
        
        if (report.recommendations.isNotEmpty()) {
            output.appendLine()
            output.appendLine("RECOMMENDATIONS:")
            report.recommendations.forEach { recommendation ->
                output.appendLine("  • $recommendation")
            }
        }
        
        return output.toString()
    }
    
    /**
     * Create a simple ASCII board visualization
     */
    fun visualizeBoard(board: ChessBoard): String {
        val output = StringBuilder()
        
        output.appendLine("  +---+---+---+---+---+---+---+---+")
        
        for (rank in 7 downTo 0) {
            output.append("${rank + 1} |")
            
            for (file in 0..7) {
                val piece = board.getPieceAt(Position(rank, file))
                val symbol = piece?.let { pieceToSymbol(it) } ?: " "
                output.append(" $symbol |")
            }
            
            output.appendLine()
            output.appendLine("  +---+---+---+---+---+---+---+---+")
        }
        
        output.appendLine("    a   b   c   d   e   f   g   h")
        
        return output.toString()
    }
    
    /**
     * Generate a comprehensive analysis summary
     */
    fun generateAnalysisSummary(
        decisionAnalysis: AgentDecisionAnalysis,
        positionEvaluation: PositionEvaluationDisplay,
        board: ChessBoard
    ): String {
        val output = StringBuilder()
        
        output.appendLine("=== COMPREHENSIVE POSITION ANALYSIS ===")
        output.appendLine()
        
        // Board visualization
        output.appendLine("CURRENT POSITION:")
        output.appendLine(visualizeBoard(board))
        output.appendLine()
        
        // Key metrics
        output.appendLine("KEY METRICS:")
        output.appendLine("  Decision Confidence: ${formatPercentage(decisionAnalysis.decisionConfidence)}")
        output.appendLine("  Position Evaluation: ${formatEvaluation(positionEvaluation.netEvaluation)}")
        output.appendLine("  Valid Moves: ${decisionAnalysis.validMovesCount}")
        output.appendLine("  Policy Entropy: ${formatDecimal(decisionAnalysis.entropy, 3)}")
        output.appendLine()
        
        // Top move
        val topMove = decisionAnalysis.topMoves.firstOrNull()
        if (topMove != null) {
            output.appendLine("RECOMMENDED MOVE:")
            output.appendLine("  ${topMove.move.toAlgebraic()} (${formatPercentage(topMove.probability)} probability, Q-value: ${formatDecimal(topMove.qValue, 2)})")
            output.appendLine()
        }
        
        // Position summary
        output.appendLine("POSITION SUMMARY:")
        output.appendLine("  ${positionEvaluation.evaluationSummary}")
        
        return output.toString()
    }
    
    // Formatting helper methods
    
    private fun formatPercentage(value: Double): String {
        return "${(value * 100).toInt()}%"
    }
    
    private fun formatDecimal(value: Double, places: Int): String {
        return "%.${places}f".format(value)
    }
    
    private fun formatEvaluation(value: Double): String {
        return when {
            value > 0.5 -> "+${formatDecimal(value, 2)} (White advantage)"
            value < -0.5 -> "${formatDecimal(value, 2)} (Black advantage)"
            else -> "${formatDecimal(value, 2)} (Balanced)"
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        // Simplified timestamp formatting
        return "Timestamp: $timestamp"
    }
    
    private fun pieceToSymbol(piece: Piece): String {
        val symbol = when (piece.type) {
            PieceType.PAWN -> "P"
            PieceType.ROOK -> "R"
            PieceType.KNIGHT -> "N"
            PieceType.BISHOP -> "B"
            PieceType.QUEEN -> "Q"
            PieceType.KING -> "K"
        }
        
        return if (piece.color == PieceColor.WHITE) symbol else symbol.lowercase()
    }
}

/**
 * Configuration for console display
 */
data class ConsoleConfig(
    val maxMovesToDisplay: Int = 10,
    val maxMovesToAnalyze: Int = 20,
    val showInvalidMoves: Boolean = false,
    val includeFeatureBreakdown: Boolean = true
)

/**
 * Factory for creating common training scenarios
 */
object TrainingScenarioFactory {
    
    /**
     * Create basic tactical scenarios for validation
     */
    fun createTacticalScenarios(): List<TrainingScenario> {
        return listOf(
            // Basic checkmate in one
            TrainingScenario(
                name = "Checkmate in One",
                description = "Simple back-rank mate",
                startingFEN = "6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1",
                expectedMoves = listOf(Move(Position(0, 4), Position(7, 4))), // Re8#
                category = "tactics"
            ),
            
            // Basic piece capture
            TrainingScenario(
                name = "Free Piece Capture",
                description = "Capture undefended piece",
                startingFEN = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
                expectedMoves = listOf(Move(Position(3, 4), Position(4, 4))), // exd5
                category = "tactics"
            ),
            
            // Basic development
            TrainingScenario(
                name = "Knight Development",
                description = "Develop knight to good square",
                startingFEN = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
                expectedMoves = listOf(Move(Position(7, 1), Position(5, 2))), // Nc6
                category = "opening"
            )
        )
    }
    
    /**
     * Create endgame scenarios for validation
     */
    fun createEndgameScenarios(): List<TrainingScenario> {
        return listOf(
            // King and Queen vs King
            TrainingScenario(
                name = "Queen Endgame",
                description = "Basic queen vs king endgame",
                startingFEN = "8/8/8/8/8/8/4Q3/4K1k1 w - - 0 1",
                expectedMoves = listOf(Move(Position(1, 4), Position(2, 4))), // Qe3+
                category = "endgame"
            ),
            
            // Pawn promotion
            TrainingScenario(
                name = "Pawn Promotion",
                description = "Promote pawn to queen",
                startingFEN = "8/4P3/8/8/8/8/8/4K1k1 w - - 0 1",
                expectedMoves = listOf(Move(Position(6, 4), Position(7, 4), PieceType.QUEEN)), // e8=Q
                category = "endgame"
            )
        )
    }
    
    /**
     * Create opening scenarios for validation
     */
    fun createOpeningScenarios(): List<TrainingScenario> {
        return listOf(
            // Center control
            TrainingScenario(
                name = "Center Control",
                description = "Control center with pawns",
                startingFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                expectedMoves = listOf(
                    Move(Position(1, 4), Position(3, 4)), // e4
                    Move(Position(6, 4), Position(4, 4)), // e5
                    Move(Position(1, 3), Position(3, 3))  // d4
                ),
                category = "opening"
            )
        )
    }
    
    /**
     * Get all predefined scenarios
     */
    fun getAllScenarios(): List<TrainingScenario> {
        return createTacticalScenarios() + createEndgameScenarios() + createOpeningScenarios()
    }
}