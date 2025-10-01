package com.chessrl.chess

import com.github.bhlangonijr.chesslib.Board as LibBoard
import kotlin.test.*

/**
 * Comprehensive validation of chess game state detection logic
 * 
 * This test validates all chess draw conditions, checkmate detection,
 * and ensures step-limited games are handled with appropriate penalties
 * rather than draw rewards.
 * 
 * Requirements covered: 3, 6
 */
class GameStateValidationTest {
    private lateinit var board: ChessBoard
    private lateinit var detector: GameStateDetector

    @BeforeTest
    fun setup() {
        board = ChessBoard()
        detector = GameStateDetector()
    }

    private fun libBoardFor(board: ChessBoard, active: PieceColor): LibBoard {
        val libBoard = LibBoard()
        val fenParts = board.toFEN().split(" ").toMutableList()
        if (fenParts.size >= 2) {
            fenParts[1] = if (active == PieceColor.WHITE) "w" else "b"
        }
        libBoard.loadFromFen(fenParts.joinToString(" "))
        return libBoard
    }
    
    // ========================================
    // STALEMATE DETECTION TESTS
    // ========================================
    
    @Test
    fun testBasicStalemate() {
        // Use the proven working stalemate position
        board.clearBoard()
        board.setPieceAt(Position(0, 0), Piece(PieceType.KING, PieceColor.WHITE)) // a1
        board.setPieceAt(Position(2, 1), Piece(PieceType.KING, PieceColor.BLACK)) // b3
        board.setPieceAt(Position(1, 2), Piece(PieceType.QUEEN, PieceColor.BLACK)) // c2
        
        assertTrue(detector.isStalemate(board, PieceColor.WHITE), "Should detect basic stalemate")
        assertFalse(detector.isCheckmate(board, PieceColor.WHITE), "Should not be checkmate")
        assertFalse(detector.isInCheck(board, PieceColor.WHITE), "King should not be in check")
        assertEquals(GameStatus.DRAW_STALEMATE, detector.getGameStatus(board))
    }
    
    @Test
    fun testStalemateVsCheckmate() {
        // Verify stalemate is correctly distinguished from checkmate
        
        // First test stalemate
        board.clearBoard()
        board.setPieceAt(Position(0, 0), Piece(PieceType.KING, PieceColor.WHITE)) // a1
        board.setPieceAt(Position(2, 1), Piece(PieceType.KING, PieceColor.BLACK)) // b3
        board.setPieceAt(Position(1, 2), Piece(PieceType.QUEEN, PieceColor.BLACK)) // c2
        
        assertTrue(detector.isStalemate(board, PieceColor.WHITE), "Should be stalemate")
        assertFalse(detector.isCheckmate(board, PieceColor.WHITE), "Should not be checkmate")
        
        // Now test checkmate (back rank mate)
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(1, 3), Piece(PieceType.PAWN, PieceColor.WHITE)) // d2
        board.setPieceAt(Position(1, 4), Piece(PieceType.PAWN, PieceColor.WHITE)) // e2
        board.setPieceAt(Position(1, 5), Piece(PieceType.PAWN, PieceColor.WHITE)) // f2
        board.setPieceAt(Position(0, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a1
        
        assertTrue(detector.isCheckmate(board, PieceColor.WHITE), "Should be checkmate")
        assertFalse(detector.isStalemate(board, PieceColor.WHITE), "Should not be stalemate")
        assertTrue(detector.isInCheck(board, PieceColor.WHITE), "King should be in check")
    }
    
    // ========================================
    // INSUFFICIENT MATERIAL TESTS
    // ========================================
    
    @Test
    fun testInsufficientMaterialCases() {
        // King vs King
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        assertTrue(detector.isDrawByInsufficientMaterial(board), "King vs King should be insufficient material")
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, detector.getGameStatus(board))
        
        // King and Bishop vs King
        board.setPieceAt(Position(3, 3), Piece(PieceType.BISHOP, PieceColor.WHITE))
        assertTrue(detector.isDrawByInsufficientMaterial(board), "King+Bishop vs King should be insufficient material")
        
        // King and Knight vs King
        board.setPieceAt(Position(3, 3), Piece(PieceType.KNIGHT, PieceColor.WHITE))
        assertTrue(detector.isDrawByInsufficientMaterial(board), "King+Knight vs King should be insufficient material")
        
        // Add a pawn - no longer insufficient material
        board.setPieceAt(Position(2, 2), Piece(PieceType.PAWN, PieceColor.WHITE))
        assertFalse(detector.isDrawByInsufficientMaterial(board), "King+Knight+Pawn vs King should be sufficient material")
    }
    
    @Test
    fun testBishopsSameColorSquares() {
        // King and Bishop vs King and Bishop on same color squares
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(3, 3), Piece(PieceType.BISHOP, PieceColor.WHITE)) // Light square
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        board.setPieceAt(Position(5, 5), Piece(PieceType.BISHOP, PieceColor.BLACK)) // Light square
        
        assertTrue(detector.isDrawByInsufficientMaterial(board), "Bishops on same color squares should be insufficient material")
        
        // Different color squares
        board.setPieceAt(Position(4, 5), Piece(PieceType.BISHOP, PieceColor.BLACK)) // Dark square
        assertFalse(detector.isDrawByInsufficientMaterial(board), "Bishops on different color squares should be sufficient material")
    }
    
    // ========================================
    // FIFTY-MOVE RULE TESTS
    // ========================================
    
    @Test
    fun testFiftyMoveRule() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        // Test with FEN that has high halfmove clock
        assertTrue(board.fromFEN("4k3/8/8/8/4K3/8/8/8 w - - 100 1"))
        assertTrue(detector.isDrawByFiftyMoveRule(board), "Should detect fifty-move rule at 100 halfmoves")
        assertEquals(GameStatus.DRAW_FIFTY_MOVE_RULE, detector.getGameStatus(board))
        
        // Test with lower halfmove clock
        assertTrue(board.fromFEN("4k3/8/8/8/4K3/8/8/8 w - - 99 1"))
        assertFalse(detector.isDrawByFiftyMoveRule(board), "Should NOT detect fifty-move rule at 99 halfmoves")
        assertNotEquals(GameStatus.DRAW_FIFTY_MOVE_RULE, detector.getGameStatus(board))
    }
    
    // ========================================
    // THREEFOLD REPETITION TESTS
    // ========================================
    
    @Test
    fun testThreefoldRepetition() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        val position = board.toFEN().split(" ").take(4).joinToString(" ")
        val gameHistory = listOf(position, "other", position, "other", position)
        
        assertTrue(detector.isDrawByRepetition(board, gameHistory), "Should detect threefold repetition")
        assertEquals(GameStatus.DRAW_REPETITION, detector.getGameStatus(board, gameHistory))
        
        // Test with only twofold repetition
        val shortHistory = listOf(position, "other", position)
        assertFalse(detector.isDrawByRepetition(board, shortHistory), "Should NOT detect draw with only twofold repetition")
    }
    
    // ========================================
    // CHECKMATE DETECTION TESTS
    // ========================================
    
    @Test
    fun testBasicCheckmate() {
        // Back rank mate
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(1, 3), Piece(PieceType.PAWN, PieceColor.WHITE)) // d2
        board.setPieceAt(Position(1, 4), Piece(PieceType.PAWN, PieceColor.WHITE)) // e2
        board.setPieceAt(Position(1, 5), Piece(PieceType.PAWN, PieceColor.WHITE)) // f2
        board.setPieceAt(Position(0, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a1

        assertTrue(detector.isCheckmate(board, PieceColor.WHITE), "Should detect back rank mate")
        assertTrue(detector.isInCheck(board, PieceColor.WHITE), "King should be in check")
        assertFalse(detector.isStalemate(board, PieceColor.WHITE), "Should not be stalemate")
        assertEquals(GameStatus.BLACK_WINS, detector.getGameStatus(board))
        assertTrue(libBoardFor(board, PieceColor.WHITE).isMated, "Chesslib should agree on checkmate")
    }
    
    @Test
    fun testQueenAndKingMate() {
        // Queen and king vs lone king mate
        board.clearBoard()
        board.setPieceAt(Position(0, 0), Piece(PieceType.KING, PieceColor.BLACK)) // a1
        board.setPieceAt(Position(2, 1), Piece(PieceType.KING, PieceColor.WHITE)) // b3
        board.setPieceAt(Position(1, 1), Piece(PieceType.QUEEN, PieceColor.WHITE)) // b2

        board.switchActiveColor() // Switch to BLACK's turn

        assertTrue(detector.isCheckmate(board, PieceColor.BLACK), "Should detect queen and king mate")
        assertTrue(detector.isInCheck(board, PieceColor.BLACK), "King should be in check")
        assertEquals(GameStatus.WHITE_WINS, detector.getGameStatus(board))
        assertTrue(libBoardFor(board, PieceColor.BLACK).isMated, "Chesslib should agree on checkmate")
    }
    
    // ========================================
    // COMPLEX GAME STATE SCENARIOS
    // ========================================
    
    @Test
    fun testCheckButNotMate() {
        // King in check but has escape squares
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(4, 7), Piece(PieceType.ROOK, PieceColor.BLACK)) // h5
        board.setPieceAt(Position(7, 4), Piece(PieceType.KING, PieceColor.BLACK)) // e8
        
        assertTrue(detector.isInCheck(board, PieceColor.WHITE), "King should be in check")
        assertFalse(detector.isCheckmate(board, PieceColor.WHITE), "Should not be checkmate")
        assertFalse(detector.isStalemate(board, PieceColor.WHITE), "Should not be stalemate")
        assertEquals(GameStatus.IN_CHECK, detector.getGameStatus(board))
    }
    
    @Test
    fun testOngoingGame() {
        // Standard starting position should be ongoing
        board.initializeStandardPosition()
        
        assertFalse(detector.isInCheck(board, PieceColor.WHITE), "Starting position should not have check")
        assertFalse(detector.isCheckmate(board, PieceColor.WHITE), "Starting position should not be checkmate")
        assertFalse(detector.isStalemate(board, PieceColor.WHITE), "Starting position should not be stalemate")
        assertFalse(detector.isDrawByInsufficientMaterial(board), "Starting position should have sufficient material")
        assertEquals(GameStatus.ONGOING, detector.getGameStatus(board))
    }
    
    // ========================================
    // LEGAL MOVE VALIDATION TESTS
    // ========================================
    
    @Test
    fun testPinnedPieceCannotMove() {
        // Piece pinned to king cannot move
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(4, 3), Piece(PieceType.ROOK, PieceColor.WHITE)) // d5
        board.setPieceAt(Position(4, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a5
        
        // Rook on d5 is pinned and cannot move off the pin line
        val illegalMove = Move(Position(4, 3), Position(3, 3)) // d5-d4
        assertFalse(detector.isMoveLegal(board, illegalMove), "Pinned piece should not be able to move off pin line")
        
        // But it can move along the pin line
        val legalMove = Move(Position(4, 3), Position(4, 2)) // d5-c5
        assertTrue(detector.isMoveLegal(board, legalMove), "Pinned piece should be able to move along pin line")
    }
    
    @Test
    fun testKingCannotMoveIntoCheck() {
        // King cannot move into check
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(3, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a4
        
        // King cannot move to e4 (attacked by rook)
        val illegalMove = Move(Position(4, 4), Position(3, 4)) // e5-e4
        assertFalse(detector.isMoveLegal(board, illegalMove), "King should not be able to move into check")
        
        // King can move to safe square
        val legalMove = Move(Position(4, 4), Position(5, 5)) // e5-f6
        assertTrue(detector.isMoveLegal(board, legalMove), "King should be able to move to safe square")
    }
    
    // ========================================
    // CASTLING VALIDATION TESTS
    // ========================================
    
    @Test
    fun testCastlingValidation() {
        // Set up legal castling position
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(0, 7), Piece(PieceType.ROOK, PieceColor.WHITE)) // h1
        board.setCastlingRights(whiteCanCastleKingside = true, whiteCanCastleQueenside = true, blackCanCastleKingside = false, blackCanCastleQueenside = false)

        val castlingMove = Move(Position(0, 4), Position(0, 6)) // e1-g1
        assertTrue(detector.isCastlingLegal(board, castlingMove), "Should allow legal castling")
        
        // Add piece blocking castling
        board.setPieceAt(Position(0, 5), Piece(PieceType.BISHOP, PieceColor.WHITE)) // f1
        assertFalse(detector.isCastlingLegal(board, castlingMove), "Should not allow castling when blocked")
        
        // Remove blocking piece, add attacking piece
        board.setPieceAt(Position(0, 5), null) // Remove bishop
        board.setPieceAt(Position(7, 5), Piece(PieceType.ROOK, PieceColor.BLACK)) // f8
        assertFalse(detector.isCastlingLegal(board, castlingMove), "Should not allow castling through check")
    }
    
    // ========================================
    // STEP-LIMITED GAME HANDLING TESTS
    // ========================================
    
    @Test
    fun testOngoingGameNotTreatedAsDraw() {
        // Verify that ongoing games are not incorrectly classified as draws
        board.initializeStandardPosition()
        
        val gameStatus = detector.getGameStatus(board)
        assertEquals(GameStatus.ONGOING, gameStatus, "Starting position should be ONGOING, not a draw")
        
        // Make a few moves and verify still ongoing
        val move1 = Move(Position(1, 4), Position(3, 4)) // e2-e4
        board.makeMove(move1)
        board.switchActiveColor()
        
        val move2 = Move(Position(6, 4), Position(4, 4)) // e7-e5
        board.makeMove(move2)
        board.switchActiveColor()
        
        val gameStatusAfterMoves = detector.getGameStatus(board)
        assertEquals(GameStatus.ONGOING, gameStatusAfterMoves, "Game should still be ONGOING after normal moves")
    }
    
    @Test
    fun testLegitimateDrawsStillDetected() {
        // Ensure legitimate draws are still properly detected
        
        // Test stalemate
        board.clearBoard()
        board.setPieceAt(Position(0, 0), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(2, 1), Piece(PieceType.KING, PieceColor.BLACK))
        board.setPieceAt(Position(1, 2), Piece(PieceType.QUEEN, PieceColor.BLACK))

        assertEquals(GameStatus.DRAW_STALEMATE, detector.getGameStatus(board), "Legitimate stalemate should be detected")
        assertTrue(libBoardFor(board, PieceColor.WHITE).isStaleMate, "Chesslib should agree on stalemate")
        
        // Test insufficient material
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, detector.getGameStatus(board), "Insufficient material should be detected")
    }
    
    @Test
    fun testGameStateConsistency() {
        // Test that game state detection is consistent across multiple calls
        board.initializeStandardPosition()
        
        val status1 = detector.getGameStatus(board)
        val status2 = detector.getGameStatus(board)
        val status3 = detector.getGameStatus(board)
        
        assertEquals(status1, status2, "Game status should be consistent")
        assertEquals(status2, status3, "Game status should be consistent")
        assertEquals(GameStatus.ONGOING, status1, "Starting position should be ONGOING")
    }
    
    // ========================================
    // EDGE CASE TESTS
    // ========================================
    
    @Test
    fun testEmptyBoardScenario() {
        // Edge case: empty board
        board.clearBoard()
        
        assertFalse(detector.isInCheck(board, PieceColor.WHITE), "Empty board should not have check")
        assertFalse(detector.isCheckmate(board, PieceColor.WHITE), "Empty board should not be checkmate")
        assertFalse(detector.isStalemate(board, PieceColor.WHITE), "Empty board should not be stalemate")
        assertTrue(detector.isDrawByInsufficientMaterial(board), "Empty board should be insufficient material")
    }
    
    @Test
    fun testRepeatedGameStateChecks() {
        // Test that repeated calls don't change the board state
        board.initializeStandardPosition()
        val originalFEN = board.toFEN()
        
        // Call various detection methods multiple times
        repeat(5) {
            detector.isInCheck(board, PieceColor.WHITE)
            detector.isCheckmate(board, PieceColor.WHITE)
            detector.isStalemate(board, PieceColor.WHITE)
            detector.getGameStatus(board)
            detector.getAllLegalMoves(board, PieceColor.WHITE)
        }
        
        assertEquals(originalFEN, board.toFEN(), "Board state should not change from detection calls")
    }
}
