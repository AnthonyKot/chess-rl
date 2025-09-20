package com.chessrl.integration

import com.chessrl.chess.*

/**
 * Demonstration of manual validation tools for RL training
 * Shows how to use the validation tools to analyze agent performance
 */
class ManualValidationDemo(
    private val agent: ChessAgent,
    private val environment: ChessEnvironment
) {
    
    private val validationTools = ManualValidationTools(agent, environment)
    private val console = ValidationConsole(validationTools)
    
    /**
     * Run a comprehensive validation demonstration
     */
    fun runValidationDemo(): String {
        val output = StringBuilder()
        
        output.appendLine("=== CHESS RL MANUAL VALIDATION DEMO ===")
        output.appendLine()
        
        // 1. Analyze starting position
        output.appendLine("1. STARTING POSITION ANALYSIS")
        output.appendLine(repeatString("=", 50))
        val startingBoard = ChessBoard()
        val startingAnalysis = demonstratePositionAnalysis(startingBoard)
        output.appendLine(startingAnalysis)
        output.appendLine()
        
        // 2. Demonstrate move probability analysis
        output.appendLine("2. MOVE PROBABILITY ANALYSIS")
        output.appendLine(repeatString("=", 50))
        val probabilityAnalysis = demonstrateMoveProbabilityAnalysis(startingBoard)
        output.appendLine(probabilityAnalysis)
        output.appendLine()
        
        // 3. Test tactical scenarios
        output.appendLine("3. TACTICAL SCENARIO TESTING")
        output.appendLine(repeatString("=", 50))
        val tacticalResults = demonstrateTacticalScenarios()
        output.appendLine(tacticalResults)
        output.appendLine()
        
        // 4. Game quality assessment
        output.appendLine("4. GAME QUALITY ASSESSMENT")
        output.appendLine(repeatString("=", 50))
        val gameQualityDemo = demonstrateGameQualityAssessment()
        output.appendLine(gameQualityDemo)
        output.appendLine()
        
        // 5. Interactive game analysis
        output.appendLine("5. INTERACTIVE GAME ANALYSIS")
        output.appendLine(repeatString("=", 50))
        val interactiveDemo = demonstrateInteractiveAnalysis()
        output.appendLine(interactiveDemo)
        output.appendLine()
        
        // 6. Validation report
        output.appendLine("6. COMPREHENSIVE VALIDATION REPORT")
        output.appendLine(repeatString("=", 50))
        val validationReport = demonstrateValidationReport()
        output.appendLine(validationReport)
        
        return output.toString()
    }
    
    /**
     * Demonstrate position analysis capabilities
     */
    private fun demonstratePositionAnalysis(board: ChessBoard): String {
        val output = StringBuilder()
        
        // Get decision analysis
        val decisionAnalysis = validationTools.visualizeAgentDecisionMaking(board)
        output.appendLine(console.displayDecisionAnalysis(decisionAnalysis))
        output.appendLine()
        
        // Get position evaluation
        val positionEvaluation = validationTools.displayPositionEvaluation(board)
        output.appendLine(console.displayPositionEvaluation(positionEvaluation))
        output.appendLine()
        
        // Generate comprehensive summary
        val summary = console.generateAnalysisSummary(decisionAnalysis, positionEvaluation, board)
        output.appendLine("COMPREHENSIVE SUMMARY:")
        output.appendLine(summary)
        
        return output.toString()
    }
    
    /**
     * Demonstrate move probability analysis
     */
    private fun demonstrateMoveProbabilityAnalysis(board: ChessBoard): String {
        val output = StringBuilder()
        
        // Show valid move probabilities
        val validMovesDisplay = validationTools.displayMoveProbabilities(board, includeInvalidMoves = false)
        output.appendLine("VALID MOVES ONLY:")
        output.appendLine(console.displayMoveProbabilities(validMovesDisplay))
        output.appendLine()
        
        // Show with invalid moves for debugging
        val allMovesDisplay = validationTools.displayMoveProbabilities(board, includeInvalidMoves = true)
        output.appendLine("INCLUDING INVALID MOVES (for debugging):")
        output.appendLine(console.displayMoveProbabilities(allMovesDisplay))
        
        return output.toString()
    }
    
    /**
     * Demonstrate tactical scenario testing
     */
    private fun demonstrateTacticalScenarios(): String {
        val output = StringBuilder()
        
        // Test a few key scenarios
        val scenarios = listOf(
            ManualTrainingScenario(
                name = "Tactical Test",
                description = "Basic tactical scenario",
                startingFEN = null,
                expectedMoves = emptyList()
            ),
            ManualTrainingScenario(
                name = "Endgame Test", 
                description = "Basic endgame scenario",
                startingFEN = null,
                expectedMoves = emptyList()
            ),
            ManualTrainingScenario(
                name = "Opening Test",
                description = "Basic opening scenario", 
                startingFEN = null,
                expectedMoves = emptyList()
            )
        )
        
        scenarios.forEach { scenario ->
            output.appendLine("Testing Scenario: ${scenario.name}")
            output.appendLine(repeatString("-", 40))
            
            val result = validationTools.inspectTrainingScenario(scenario)
            output.appendLine(console.displayScenarioInspection(result))
            output.appendLine()
        }
        
        return output.toString()
    }
    
    /**
     * Demonstrate game quality assessment
     */
    private fun demonstrateGameQualityAssessment(): String {
        val output = StringBuilder()
        
        // Create sample games of different qualities
        val sampleGames = listOf(
            createSampleGame("Short Tactical Game", 15, GameStatus.WHITE_WINS),
            createSampleGame("Long Positional Game", 80, GameStatus.DRAW_STALEMATE),
            createSampleGame("Quick Blunder Game", 8, GameStatus.BLACK_WINS)
        )
        
        sampleGames.forEach { (description, gameHistory, result) ->
            output.appendLine("Analyzing: $description")
            output.appendLine(repeatString("-", 40))
            
            val finalBoard = replayGame(gameHistory)
            val assessment = validationTools.assessGameQuality(gameHistory, result, finalBoard)
            output.appendLine(console.displayGameQuality(assessment))
            output.appendLine()
        }
        
        return output.toString()
    }
    
    /**
     * Demonstrate interactive game analysis
     */
    private fun demonstrateInteractiveAnalysis(): String {
        val output = StringBuilder()
        
        // Create a sample game with annotations
        val gameHistory = listOf(
            Move(Position(1, 4), Position(3, 4)), // e4
            Move(Position(6, 4), Position(4, 4)), // e5
            Move(Position(0, 1), Position(2, 2)), // Nc3
            Move(Position(7, 1), Position(5, 2)), // Nc6
            Move(Position(0, 5), Position(3, 2)), // Bc4
            Move(Position(7, 5), Position(4, 2))  // Bc5
        )
        
        val annotations = mapOf(
            0 to "King's pawn opening - controls center",
            1 to "Symmetric response - good development",
            2 to "Knight development - attacks center",
            3 to "Mirror development",
            4 to "Italian Game setup - targets f7",
            5 to "Italian Defense - develops with tempo"
        )
        
        val analysis = validationTools.createInteractiveAnalysis(gameHistory, annotations)
        output.appendLine(console.displayInteractiveAnalysis(analysis))
        
        return output.toString()
    }
    
    /**
     * Demonstrate comprehensive validation report
     */
    private fun demonstrateValidationReport(): String {
        val output = StringBuilder()
        
        // Get all predefined scenarios
        val testScenarios = listOf(
            ManualTrainingScenario(
                name = "Comprehensive Test",
                description = "Full validation scenario",
                startingFEN = null,
                expectedMoves = emptyList()
            )
        )
        
        // Create sample games for analysis
        val sampleGames = listOf(
            createSampleGame("Game 1", 25, GameStatus.WHITE_WINS).second,
            createSampleGame("Game 2", 45, GameStatus.DRAW_STALEMATE).second,
            createSampleGame("Game 3", 35, GameStatus.BLACK_WINS).second
        )
        
        // Generate comprehensive report
        val report = validationTools.generateValidationReport(testScenarios, sampleGames)
        output.appendLine(console.displayValidationReport(report))
        
        return output.toString()
    }
    
    /**
     * Create a sample game for demonstration
     */
    private fun createSampleGame(description: String, length: Int, result: GameStatus): Triple<String, List<Move>, GameStatus> {
        val moves = mutableListOf<Move>()
        
        // Create a basic game sequence
        val basicMoves = listOf(
            Move(Position(1, 4), Position(3, 4)), // e4
            Move(Position(6, 4), Position(4, 4)), // e5
            Move(Position(0, 1), Position(2, 2)), // Nc3
            Move(Position(7, 1), Position(5, 2)), // Nc6
            Move(Position(0, 5), Position(3, 2)), // Bc4
            Move(Position(7, 5), Position(4, 2)), // Bc5
            Move(Position(1, 3), Position(3, 3)), // d3
            Move(Position(6, 3), Position(4, 3)), // d6
            Move(Position(0, 2), Position(1, 3)), // Bd2
            Move(Position(7, 2), Position(6, 3))  // Bd7
        )
        
        // Add basic moves
        moves.addAll(basicMoves.take(minOf(length, basicMoves.size)))
        
        // Fill remaining moves with simple pawn moves
        var currentLength = moves.size
        var fileIndex = 0
        while (currentLength < length && fileIndex < 8) {
            if (currentLength % 2 == 0) {
                // White move
                moves.add(Move(Position(1, fileIndex), Position(2, fileIndex)))
            } else {
                // Black move
                moves.add(Move(Position(6, fileIndex), Position(5, fileIndex)))
            }
            currentLength++
            fileIndex = (fileIndex + 1) % 8
        }
        
        return Triple(description, moves.take(length), result)
    }
    
    /**
     * Replay a game to get the final board position
     */
    private fun replayGame(gameHistory: List<Move>): ChessBoard {
        val board = ChessBoard()
        
        gameHistory.forEach { move ->
            try {
                board.makeLegalMove(move)
                board.switchActiveColor()
            } catch (e: Exception) {
                // Handle invalid moves gracefully
                println("Warning: Could not replay move ${move.toAlgebraic()}: ${e.message}")
            }
        }
        
        return board
    }
    
    /**
     * Run a quick validation check
     */
    fun runQuickValidation(): String {
        val output = StringBuilder()
        
        output.appendLine("=== QUICK VALIDATION CHECK ===")
        output.appendLine()
        
        // Test starting position decision making
        val board = ChessBoard()
        val analysis = validationTools.visualizeAgentDecisionMaking(board)
        
        output.appendLine("Agent Decision Confidence: ${(analysis.decisionConfidence * 100).toInt()}%")
        output.appendLine("Top Move: ${analysis.topMoves.firstOrNull()?.move?.toAlgebraic() ?: "None"}")
        output.appendLine("Valid Moves Available: ${analysis.validMovesCount}")
        output.appendLine()
        
        // Test one tactical scenario
        val tacticalScenario = ManualTrainingScenario(
            name = "Quick Tactical Test",
            description = "Quick tactical validation",
            startingFEN = null,
            expectedMoves = emptyList()
        )
        val scenarioResult = validationTools.inspectTrainingScenario(tacticalScenario)
        
        output.appendLine("Tactical Scenario Test: ${tacticalScenario.name}")
        output.appendLine("Accuracy: ${(scenarioResult.accuracy * 100).toInt()}%")
        output.appendLine("Correct Moves: ${scenarioResult.correctMoves}/${scenarioResult.totalSteps}")
        output.appendLine()
        
        // Overall assessment
        val overallScore = (analysis.decisionConfidence + scenarioResult.accuracy) / 2.0
        output.appendLine("Overall Validation Score: ${(overallScore * 100).toInt()}%")
        
        val assessment = when {
            overallScore >= 0.8 -> "Excellent - Agent shows strong performance"
            overallScore >= 0.6 -> "Good - Agent performance is satisfactory"
            overallScore >= 0.4 -> "Fair - Agent needs improvement"
            else -> "Poor - Agent requires significant training"
        }
        
        output.appendLine("Assessment: $assessment")
        
        return output.toString()
    }
    
    /**
     * Demonstrate specific position analysis
     */
    fun analyzeSpecificPosition(fen: String): String {
        val output = StringBuilder()
        
        output.appendLine("=== SPECIFIC POSITION ANALYSIS ===")
        output.appendLine("FEN: $fen")
        output.appendLine()
        
        // Load position
        if (!environment.loadFromFEN(fen)) {
            return "Error: Invalid FEN string"
        }
        
        val board = environment.getCurrentBoard()
        
        // Comprehensive analysis
        val decisionAnalysis = validationTools.visualizeAgentDecisionMaking(board)
        val positionEvaluation = validationTools.displayPositionEvaluation(board)
        
        output.appendLine(console.generateAnalysisSummary(decisionAnalysis, positionEvaluation, board))
        
        return output.toString()
    }
}

/**
 * Helper function to repeat strings (for formatting)
 */
private fun repeatString(str: String, count: Int): String {
    return str.repeat(count)
}