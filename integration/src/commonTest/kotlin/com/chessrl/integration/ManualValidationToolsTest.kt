package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*
import kotlin.test.*

/**
 * Comprehensive tests for manual validation tools
 */
class ManualValidationToolsTest {
    
    @Test
    fun testValidationConfigCreation() {
        // Test that we can create validation config
        val config = ManualValidationConfig(
            topMovesToShow = 3,
            topInvalidMovesToShow = 2,
            includeFeatureBreakdown = false,
            showConfidenceThreshold = 0.2
        )
        
        assertEquals(3, config.topMovesToShow)
        assertEquals(2, config.topInvalidMovesToShow)
        assertFalse(config.includeFeatureBreakdown)
        assertEquals(0.2, config.showConfidenceThreshold, 0.01)
    }
    
    @Test
    fun testMoveAnalysisDataClass() {
        // Test MoveAnalysis data class
        val move = Move(Position(1, 4), Position(3, 4))
        val analysis = MoveAnalysis(
            move = move,
            probability = 0.6,
            qValue = 1.2,
            rank = 1
        )
        
        assertEquals(move, analysis.move)
        assertEquals(0.6, analysis.probability, 0.01)
        assertEquals(1.2, analysis.qValue, 0.01)
        assertEquals(1, analysis.rank)
    }
    
    @Test
    fun testMoveProbabilityEntry() {
        // Test MoveProbabilityEntry data class
        val move = Move(Position(1, 4), Position(3, 4))
        val entry = MoveProbabilityEntry(
            move = move,
            algebraicNotation = "e4",
            probability = 0.4,
            isValid = true
        )
        
        assertEquals(move, entry.move)
        assertEquals("e4", entry.algebraicNotation)
        assertEquals(0.4, entry.probability, 0.01)
        assertTrue(entry.isValid)
    }
    
    @Test
    fun testGameQualityAssessmentDataClass() {
        // Test GameQualityAssessment data class
        val moveQuality = MoveQualityAnalysis(
            developmentMoves = 3,
            centerControlMoves = 5,
            tacticalMoves = 2,
            totalMoves = 20,
            qualityScore = 0.7
        )
        
        val progression = GameProgressionAnalysis(
            openingLength = 10,
            middlegameLength = 8,
            endgameLength = 2,
            gamePhaseBalance = 0.8
        )
        
        val assessment = GameQualityAssessment(
            gameLength = 20,
            gameResult = GameStatus.WHITE_WINS,
            qualityScore = 0.75,
            moveQuality = moveQuality,
            gameProgression = progression,
            insights = listOf("Good opening", "Strong tactics"),
            averageMoveTime = 2.5
        )
        
        assertEquals(20, assessment.gameLength)
        assertEquals(GameStatus.WHITE_WINS, assessment.gameResult)
        assertEquals(0.75, assessment.qualityScore, 0.01)
        assertEquals(moveQuality, assessment.moveQuality)
        assertEquals(progression, assessment.gameProgression)
        assertEquals(2, assessment.insights.size)
        assertEquals(2.5, assessment.averageMoveTime, 0.01)
    }
    
    @Test
    fun testPositionEvaluationDisplayDataClass() {
        // Test PositionEvaluationDisplay data class
        val qValueRange = QValueRange(2.0, -0.5, 0.75)
        val featureBreakdown = PositionFeatureBreakdown(
            materialBalance = 1,
            activityBalance = 2,
            kingSafetyBalance = 0.5,
            developmentBalance = 1,
            whiteMaterial = 40,
            blackMaterial = 39,
            whiteActivity = 3,
            blackActivity = 1
        )
        val tacticalAssessment = TacticalAssessment(
            checksPresent = false,
            attackedPieces = 2,
            defendedPieces = 4,
            tacticalComplexity = 0.6
        )
        
        val evaluation = PositionEvaluationDisplay(
            position = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            activeColor = PieceColor.WHITE,
            whiteEvaluation = 0.3,
            blackEvaluation = -0.1,
            netEvaluation = 0.4,
            agentQValueRange = qValueRange,
            featureBreakdown = featureBreakdown,
            tacticalAssessment = tacticalAssessment,
            evaluationSummary = "Position is balanced"
        )
        
        assertEquals(PieceColor.WHITE, evaluation.activeColor)
        assertEquals(0.3, evaluation.whiteEvaluation, 0.01)
        assertEquals(-0.1, evaluation.blackEvaluation, 0.01)
        assertEquals(0.4, evaluation.netEvaluation, 0.01)
        assertEquals(qValueRange, evaluation.agentQValueRange)
        assertEquals(featureBreakdown, evaluation.featureBreakdown)
        assertEquals(tacticalAssessment, evaluation.tacticalAssessment)
        assertEquals("Position is balanced", evaluation.evaluationSummary)
    }
    
    @Test
    fun testTrainingScenarioDataClass() {
        // Test TrainingScenario data class
        val expectedMoves = listOf(
            Move(Position(1, 4), Position(3, 4)), // e4
            Move(Position(1, 3), Position(3, 3))  // d4
        )
        
        val scenario = TrainingScenario(
            name = "Test Scenario",
            description = "Test scenario for validation",
            startingFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            expectedMoves = expectedMoves,
            category = "opening"
        )
        
        assertEquals("Test Scenario", scenario.name)
        assertEquals("Test scenario for validation", scenario.description)
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", scenario.startingFEN)
        assertEquals(expectedMoves, scenario.expectedMoves)
        assertEquals("opening", scenario.category)
    }
    
    @Test
    fun testConsoleConfigDataClass() {
        // Test ConsoleConfig data class
        val config = ConsoleConfig(
            maxMovesToDisplay = 15,
            maxMovesToAnalyze = 25,
            showInvalidMoves = true,
            includeFeatureBreakdown = false
        )
        
        assertEquals(15, config.maxMovesToDisplay)
        assertEquals(25, config.maxMovesToAnalyze)
        assertTrue(config.showInvalidMoves)
        assertFalse(config.includeFeatureBreakdown)
    }
    

    
    @Test
    fun testTrainingScenarioFactory() {
        // Act
        val tacticalScenarios = TrainingScenarioFactory.createTacticalScenarios()
        val endgameScenarios = TrainingScenarioFactory.createEndgameScenarios()
        val openingScenarios = TrainingScenarioFactory.createOpeningScenarios()
        val allScenarios = TrainingScenarioFactory.getAllScenarios()
        
        // Assert
        assertTrue(tacticalScenarios.isNotEmpty())
        assertTrue(endgameScenarios.isNotEmpty())
        assertTrue(openingScenarios.isNotEmpty())
        
        assertEquals(
            tacticalScenarios.size + endgameScenarios.size + openingScenarios.size,
            allScenarios.size
        )
        
        // Check that scenarios have required fields
        allScenarios.forEach { scenario ->
            assertTrue(scenario.name.isNotEmpty())
            assertTrue(scenario.description.isNotEmpty())
            assertTrue(scenario.expectedMoves.isNotEmpty())
            assertTrue(scenario.category.isNotEmpty())
        }
        
        // Check specific scenarios
        val checkmateScenario = tacticalScenarios.find { it.name == "Checkmate in One" }
        assertNotNull(checkmateScenario)
        assertEquals("tactics", checkmateScenario.category)
        
        val queenEndgame = endgameScenarios.find { it.name == "Queen Endgame" }
        assertNotNull(queenEndgame)
        assertEquals("endgame", queenEndgame.category)
    }
    
    @Test
    fun testBoardVisualization() {
        // Arrange
        val board = ChessBoard()
        val console = ValidationConsole(
            ManualValidationTools(
                // We can't create real instances, so we'll test the console separately
                ChessAgentFactory.createDQNAgent(), 
                ChessEnvironment()
            )
        )
        
        // Act
        val visualization = console.visualizeBoard(board)
        
        // Assert
        assertTrue(visualization.contains("+---+---+---+---+---+---+---+---+"))
        assertTrue(visualization.contains("a   b   c   d   e   f   g   h"))
        assertTrue(visualization.contains("8 |"))
        assertTrue(visualization.contains("1 |"))
        
        // Check that it contains piece symbols (initial position)
        assertTrue(visualization.contains("r")) // Black rook
        assertTrue(visualization.contains("R")) // White rook
        assertTrue(visualization.contains("p")) // Black pawn
        assertTrue(visualization.contains("P")) // White pawn
    }
}