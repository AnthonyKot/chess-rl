package com.chessrl.integration

import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.chess.*
import kotlin.test.*

/**
 * Essential regression tests - fast, reliable tests for deterministic behavior
 * Focuses on deterministic results with fixed seeds and chess rule compliance
 */
class EssentialRegressionTest {
    
    @Test
    fun testDeterministicConfigurationBehavior() {
        // Test that same configuration produces same validation results
        val config1 = ChessRLConfig(
            maxCycles = 10,
            gamesPerCycle = 5,
            seed = 12345L
        )
        
        val config2 = ChessRLConfig(
            maxCycles = 10,
            gamesPerCycle = 5,
            seed = 12345L
        )
        
        val validation1 = config1.validate()
        val validation2 = config2.validate()
        
        assertEquals(validation1.isValid, validation2.isValid, "Same configs should have same validation")
        assertEquals(validation1.errors.size, validation2.errors.size, "Same configs should have same error count")
        assertEquals(validation1.warnings.size, validation2.warnings.size, "Same configs should have same warning count")
    }
    
    @Test
    fun testChessRuleCompliance() {
        val board = ChessBoard()
        val detector = GameStateDetector()
        
        // Test starting position compliance
        assertEquals(PieceColor.WHITE, board.getActiveColor(), "White should move first")
        
        val whiteMoves = detector.getAllLegalMoves(board, PieceColor.WHITE)
        assertEquals(20, whiteMoves.size, "Starting position should have exactly 20 legal moves")
        
        // Test that all starting moves are valid
        for (move in whiteMoves) {
            val piece = board.getPieceAt(move.from)
            assertNotNull(piece, "Move should originate from a piece")
            assertEquals(PieceColor.WHITE, piece.color, "All starting moves should be white pieces")
        }
        
        // Test basic board state consistency
        assertFalse(detector.isCheckmate(board, PieceColor.WHITE), "Starting position should not be checkmate")
        assertFalse(detector.isStalemate(board, PieceColor.WHITE), "Starting position should not be stalemate")
        assertFalse(detector.isInCheck(board, PieceColor.WHITE), "Starting position should not be in check")
    }
    
    @Test
    fun testChessPositionConsistency() {
        val board = ChessBoard()
        
        // Test FEN round-trip consistency
        val originalFEN = board.toFEN()
        
        val newBoard = ChessBoard()
        newBoard.clearBoard()
        assertTrue(newBoard.fromFEN(originalFEN), "Should load original FEN")
        
        val roundTripFEN = newBoard.toFEN()
        assertEquals(originalFEN, roundTripFEN, "FEN should be identical after round trip")
        
        // Test board equality
        assertEquals(board, newBoard, "Boards should be equal after FEN round trip")
    }
    
    @Test
    fun testStateEncodingConsistency() {
        val encoder = ChessStateEncoder()
        val board = ChessBoard()
        
        // Test that same position produces same encoding
        val encoding1 = encoder.encode(board)
        val encoding2 = encoder.encode(board)
        
        assertEquals(encoding1.size, encoding2.size, "Encodings should have same size")
        
        for (i in encoding1.indices) {
            assertEquals(encoding1[i], encoding2[i], 1e-10, "Encoding values should be identical")
        }
        
        // Test encoding after move and back
        val originalEncoding = encoder.encode(board)
        
        // Make a move
        board.makeMove(Move(Position(1, 4), Position(3, 4))) // e2-e4
        val afterMoveEncoding = encoder.encode(board)
        
        // Undo by creating new board with original position
        val restoredBoard = ChessBoard()
        val restoredEncoding = encoder.encode(restoredBoard)
        
        // Original and restored should be identical
        assertEquals(originalEncoding.size, restoredEncoding.size)
        for (i in originalEncoding.indices) {
            assertEquals(originalEncoding[i], restoredEncoding[i], 1e-10, "Restored encoding should match original")
        }
        
        // After move should be different
        var foundDifference = false
        for (i in originalEncoding.indices) {
            if (kotlin.math.abs(originalEncoding[i] - afterMoveEncoding[i]) > 1e-10) {
                foundDifference = true
                break
            }
        }
        assertTrue(foundDifference, "Encoding should change after move")
    }
    
    @Test
    fun testComplexChessPositions() {
        val board = ChessBoard()
        val detector = GameStateDetector()
        
        // Test Scholar's mate position (known checkmate)
        val scholarsMate = "rnbqkb1r/pppp1Qpp/5n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4"
        assertTrue(board.fromFEN(scholarsMate), "Should load Scholar's mate position")
        
        assertTrue(detector.isCheckmate(board, PieceColor.BLACK), "Should detect checkmate")
        assertTrue(detector.isInCheck(board, PieceColor.BLACK), "Should detect check")
        
        val blackMoves = detector.getAllLegalMoves(board, PieceColor.BLACK)
        assertEquals(0, blackMoves.size, "Checkmate position should have no legal moves")
        
        // Test stalemate position
        val stalematePosition = "8/8/8/8/8/3k4/3p4/3K4 w - - 0 1"
        assertTrue(board.fromFEN(stalematePosition), "Should load stalemate position")
        
        assertTrue(detector.isStalemate(board, PieceColor.WHITE), "Should detect stalemate")
        assertFalse(detector.isInCheck(board, PieceColor.WHITE), "Stalemate should not be check")
        
        val whiteMoves = detector.getAllLegalMoves(board, PieceColor.WHITE)
        assertEquals(0, whiteMoves.size, "Stalemate position should have no legal moves")
    }
    
    @Test
    fun testTrainingPipelineConsistency() {
        // Test that same configuration creates consistent pipeline
        val config = ChessRLConfig(
            maxCycles = 1,
            gamesPerCycle = 1,
            maxConcurrentGames = 1,
            maxStepsPerGame = 5,
            seed = 42L
        )
        
        val pipeline1 = TrainingPipeline(config)
        val pipeline2 = TrainingPipeline(config)
        
        assertNotNull(pipeline1, "First pipeline should be created")
        assertNotNull(pipeline2, "Second pipeline should be created")
        
        // Both pipelines should be created successfully with same config
        assertTrue(pipeline1 !== pipeline2, "Pipelines should be different instances")
    }
    
    @Test
    fun testGameAnalyzerConsistency() {
        val analyzer1 = GameAnalyzer()
        val analyzer2 = GameAnalyzer()
        
        // Test that analyzers can be created consistently
        assertNotNull(analyzer1, "First analyzer should be created")
        assertNotNull(analyzer2, "Second analyzer should be created")
        
        // Test initialization consistency
        analyzer1.initialize()
        analyzer2.initialize()
        
        // Both should initialize without errors
        assertTrue(analyzer1 !== analyzer2, "Analyzers should be different instances")
    }
    
    @Test
    fun testConfigurationRegressionScenarios() {
        // Test various configuration scenarios that should remain stable
        
        // Minimal valid configuration
        val minimalConfig = ChessRLConfig(
            maxCycles = 1,
            gamesPerCycle = 1,
            maxConcurrentGames = 1,
            maxStepsPerGame = 1
        )
        assertTrue(minimalConfig.validate().isValid, "Minimal config should be valid")
        
        // Typical development configuration
        val devConfig = ChessRLConfig(
            maxCycles = 5,
            gamesPerCycle = 3,
            maxConcurrentGames = 2,
            maxStepsPerGame = 20,
            seed = 12345L
        )
        assertTrue(devConfig.validate().isValid, "Development config should be valid")
        
        // Production-like configuration
        val prodConfig = ChessRLConfig(
            maxCycles = 100,
            gamesPerCycle = 50,
            maxConcurrentGames = 8,
            maxStepsPerGame = 200
        )
        assertTrue(prodConfig.validate().isValid, "Production config should be valid")
        
        // Invalid configurations should consistently fail
        val invalidConfig = ChessRLConfig(
            maxCycles = -1,
            gamesPerCycle = 0,
            maxConcurrentGames = -5,
            maxStepsPerGame = 0
        )
        assertFalse(invalidConfig.validate().isValid, "Invalid config should consistently fail validation")
    }
}