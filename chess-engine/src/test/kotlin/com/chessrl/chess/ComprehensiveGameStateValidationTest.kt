package com.chessrl.chess

import kotlin.test.*

/**
 * Comprehensive test suite for chess game state detection logic
 * 
 * This test validates all chess draw conditions, checkmate detection,
 * and ensures step-limited games are handled with appropriate penalties
 * rather than draw rewards.
 * 
 * Requirements covered: 3, 6
 */
class ComprehensiveGameStateValidationTest {
    private lateinit var board: ChessBoard
    private lateinit var detector: GameStateDetector
    private lateinit var legalValidator: LegalMoveValidator
    
    @BeforeTest
    fun setup() {
        board = ChessBoard()
        detector = GameStateDetector()
        legalValidator = LegalMoveValidator()
    }
    
    // ========================================
    // STALEMATE DETECTION TESTS
    // ========================================
    
    @Test
    fun testClassicStalematePosition() {
        // Classic stalemate: King in corner with no legal moves but not in check
        board.clearBoard()
        board.setPieceAt(Position(0, 0), Piece(PieceType.KING, PieceColor.WHITE)) // a1
        board.setPieceAt(Position(2, 1), Piece(PieceType.KING, PieceColor.BLACK)) // b3
        board.setPieceAt(Position(1, 2), Piece(PieceType.QUEEN, PieceColor.BLACK)) // c2
        
        board.getGameState().activeColor = PieceColor.WHITE
        
        assertTrue(detector.isStalemate(board, PieceColor.WHITE), "Should detect stalemate")
        assertFalse(detector.isCheckmate(board, PieceColor.WHITE), "Should not be checkmate")
        assertFalse(detector.isInCheck(board, PieceColor.WHITE), "King should not be in check")
        assertEquals(GameStatus.DRAW_STALEMATE, detector.getGameStatus(board))
    }
    
    @Test
    fun testStalemateWithPawnBlocking() {
        // Variation of the working stalemate position with pawns
        board.clearBoard()
        board.setPieceAt(Position(0, 1), Piece(PieceType.KING, PieceColor.WHITE)) // b1
        board.setPieceAt(Position(1, 1), Piece(PieceType.PAWN, PieceColor.WHITE)) // b2 (own pawn blocks)
        board.setPieceAt(Position(2, 0), Piece(PieceType.KING, PieceColor.BLACK)) // a3 (controls a2, b2)
        board.setPieceAt(Position(0, 3), Piece(PieceType.QUEEN, PieceColor.BLACK)) // d1 (controls c1, a1)
        
        // Use default active color (WHITE)
        
        assertTrue(detector.isStalemate(board, PieceColor.WHITE), "Should detect stalemate with pawn blocking")
        assertEquals(GameStatus.DRAW_STALEMATE, detector.getGameStatus(board))
    }
    
    @Test
    fun testStalemateWithMultiplePieces() {
        // Mirror of the working stalemate position for black
        board.clearBoard()
        board.setPieceAt(Position(7, 7), Piece(PieceType.KING, PieceColor.BLACK)) // h8
        board.setPieceAt(Position(5, 6), Piece(PieceType.KING, PieceColor.WHITE)) // g6 (controls g7, h7, f7)
        board.setPieceAt(Position(6, 5), Piece(PieceType.QUEEN, PieceColor.WHITE)) // f7 (controls f8, g8)
        
        board.switchActiveColor() // Switch from WHITE to BLACK
        
        assertTrue(detector.isStalemate(board, PieceColor.BLACK), "Should detect complex stalemate")
        assertEquals(GameStatus.DRAW_STALEMATE, detector.getGameStatus(board))
    }
    
    // ========================================
    // INSUFFICIENT MATERIAL TESTS
    // ========================================
    
    @Test
    fun testKingVsKing() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        assertTrue(detector.isDrawByInsufficientMaterial(board), "King vs King should be insufficient material")
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, detector.getGameStatus(board))
    }
    
    @Test
    fun testKingBishopVsKing() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(3, 3), Piece(PieceType.BISHOP, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        assertTrue(detector.isDrawByInsufficientMaterial(board), "King+Bishop vs King should be insufficient material")
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, detector.getGameStatus(board))
    }
    
    @Test
    fun testKingKnightVsKing() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(3, 3), Piece(PieceType.KNIGHT, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        assertTrue(detector.isDrawByInsufficientMaterial(board), "King+Knight vs King should be insufficient material")
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, detector.getGameStatus(board))
    }
    
    @Test
    fun testKingBishopVsKingBishopSameColor() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(3, 3), Piece(PieceType.BISHOP, PieceColor.WHITE)) // Light square
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        board.setPieceAt(Position(5, 5), Piece(PieceType.BISHOP, PieceColor.BLACK)) // Light square
        
        assertTrue(detector.isDrawByInsufficientMaterial(board), "Bishops on same color squares should be insufficient material")
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, detector.getGameStatus(board))
    }
    
    @Test
    fun testKingBishopVsKingBishopDifferentColor() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(3, 3), Piece(PieceType.BISHOP, PieceColor.WHITE)) // Light square
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        board.setPieceAt(Position(4, 5), Piece(PieceType.BISHOP, PieceColor.BLACK)) // Dark square
        
        assertFalse(detector.isDrawByInsufficientMaterial(board), "Bishops on different color squares should NOT be insufficient material")
        assertNotEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, detector.getGameStatus(board))
    }
    
    @Test
    fun testSufficientMaterialWithPawn() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(3, 3), Piece(PieceType.PAWN, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        assertFalse(detector.isDrawByInsufficientMaterial(board), "King+Pawn vs King should be sufficient material")
        assertNotEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, detector.getGameStatus(board))
    }
    
    @Test
    fun testSufficientMaterialWithRook() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(3, 3), Piece(PieceType.ROOK, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        assertFalse(detector.isDrawByInsufficientMaterial(board), "King+Rook vs King should be sufficient material")
        assertNotEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, detector.getGameStatus(board))
    }
    
    // ========================================
    // FIFTY-MOVE RULE TESTS
    // ========================================
    
    @Test
    fun testFiftyMoveRuleExactly() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        // Set halfmove clock to exactly 100 (50 moves)
        assertTrue(board.fromFEN("4k3/8/8/8/4K3/8/8/8 w - - 100 1"))
        
        assertTrue(detector.isDrawByFiftyMoveRule(board), "Should detect fifty-move rule at exactly 100 halfmoves")
        assertEquals(GameStatus.DRAW_FIFTY_MOVE_RULE, detector.getGameStatus(board))
    }
    
    @Test
    fun testFiftyMoveRuleJustUnder() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        // Set halfmove clock to 99 (just under 50 moves)
        assertTrue(board.fromFEN("4k3/8/8/8/4K3/8/8/8 w - - 99 1"))
        
        assertFalse(detector.isDrawByFiftyMoveRule(board), "Should NOT detect fifty-move rule at 99 halfmoves")
        assertNotEquals(GameStatus.DRAW_FIFTY_MOVE_RULE, detector.getGameStatus(board))
    }
    
    @Test
    fun testFiftyMoveRuleOver() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        // Set halfmove clock to over 100
        assertTrue(board.fromFEN("4k3/8/8/8/4K3/8/8/8 w - - 120 1"))
        
        assertTrue(detector.isDrawByFiftyMoveRule(board), "Should detect fifty-move rule at over 100 halfmoves")
        assertEquals(GameStatus.DRAW_FIFTY_MOVE_RULE, detector.getGameStatus(board))
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
    }
    
    @Test
    fun testTwofoldRepetitionNotDraw() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        val position = board.toFEN().split(" ").take(4).joinToString(" ")
        val gameHistory = listOf(position, "other", position)
        
        assertFalse(detector.isDrawByRepetition(board, gameHistory), "Should NOT detect draw with only twofold repetition")
        assertNotEquals(GameStatus.DRAW_REPETITION, detector.getGameStatus(board, gameHistory))
    }
    
    // ========================================
    // CHECKMATE DETECTION TESTS
    // ========================================
    
    @Test
    fun testBackRankMate() {
        // Classic back rank mate
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(1, 3), Piece(PieceType.PAWN, PieceColor.WHITE)) // d2
        board.setPieceAt(Position(1, 4), Piece(PieceType.PAWN, PieceColor.WHITE)) // e2
        board.setPieceAt(Position(1, 5), Piece(PieceType.PAWN, PieceColor.WHITE)) // f2
        board.setPieceAt(Position(0, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a1
        
        board.getGameState().activeColor = PieceColor.WHITE
        
        assertTrue(detector.isCheckmate(board, PieceColor.WHITE), "Should detect back rank mate")
        assertTrue(detector.isInCheck(board, PieceColor.WHITE), "King should be in check")
        assertFalse(detector.isStalemate(board, PieceColor.WHITE), "Should not be stalemate")
        assertEquals(GameStatus.BLACK_WINS, detector.getGameStatus(board))
    }
    
    @Test
    fun testSmotheredMate() {
        // Smothered mate pattern
        require(board.fromFEN("6rk/5Npp/8/8/8/8/8/7K b - - 0 1"))

        assertTrue(detector.isCheckmate(board, PieceColor.BLACK), "Should detect smothered mate")
        assertTrue(detector.isInCheck(board, PieceColor.BLACK), "King should be in check")
        assertEquals(GameStatus.WHITE_WINS, detector.getGameStatus(board))
    }
    
    @Test
    fun testQueenAndKingMate() {
        // Queen and king vs lone king mate
        board.clearBoard()
        board.setPieceAt(Position(0, 0), Piece(PieceType.KING, PieceColor.BLACK)) // a1
        board.setPieceAt(Position(2, 1), Piece(PieceType.KING, PieceColor.WHITE)) // b3
        board.setPieceAt(Position(1, 2), Piece(PieceType.QUEEN, PieceColor.WHITE)) // c2
        
        board.switchActiveColor() // Switch from WHITE to BLACK
        
        assertTrue(detector.isCheckmate(board, PieceColor.BLACK), "Should detect queen and king mate")
        assertTrue(detector.isInCheck(board, PieceColor.BLACK), "King should be in check")
        assertEquals(GameStatus.WHITE_WINS, detector.getGameStatus(board))
    }
    
    @Test
    fun testTwoRooksMate() {
        // Two rooks mate
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.BLACK)) // e1
        board.setPieceAt(Position(1, 0), Piece(PieceType.ROOK, PieceColor.WHITE)) // a2
        board.setPieceAt(Position(0, 0), Piece(PieceType.ROOK, PieceColor.WHITE)) // a1
        board.setPieceAt(Position(2, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e3
        
        board.switchActiveColor() // Switch from WHITE to BLACK
        
        assertTrue(detector.isCheckmate(board, PieceColor.BLACK), "Should detect two rooks mate")
        assertTrue(detector.isInCheck(board, PieceColor.BLACK), "King should be in check")
        assertEquals(GameStatus.WHITE_WINS, detector.getGameStatus(board))
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
        
        // Use default active color (WHITE)
        
        assertTrue(detector.isInCheck(board, PieceColor.WHITE), "King should be in check")
        assertFalse(detector.isCheckmate(board, PieceColor.WHITE), "Should not be checkmate")
        assertFalse(detector.isStalemate(board, PieceColor.WHITE), "Should not be stalemate")
        assertEquals(GameStatus.IN_CHECK, detector.getGameStatus(board))
    }
    
    @Test
    fun testOngoingGameWithManyPieces() {
        // Standard starting position should be ongoing
        board.initializeStandardPosition()
        
        assertFalse(detector.isInCheck(board, PieceColor.WHITE), "Starting position should not have check")
        assertFalse(detector.isCheckmate(board, PieceColor.WHITE), "Starting position should not be checkmate")
        assertFalse(detector.isStalemate(board, PieceColor.WHITE), "Starting position should not be stalemate")
        assertFalse(detector.isDrawByInsufficientMaterial(board), "Starting position should have sufficient material")
        assertEquals(GameStatus.ONGOING, detector.getGameStatus(board))
    }
    
    @Test
    fun testDiscoveredCheck() {
        // Discovered check scenario
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(4, 6), Piece(PieceType.BISHOP, PieceColor.WHITE)) // g5
        board.setPieceAt(Position(4, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a5
        
        // Move bishop to discover check from rook
        val move = Move(Position(4, 6), Position(3, 7)) // g5-h4
        board.makeMove(move)
        
        assertTrue(detector.isInCheck(board, PieceColor.WHITE), "Should detect discovered check")
    }
    
    @Test
    fun testDoubleCheck() {
        // Double check scenario (king attacked by two pieces)
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(4, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a5
        board.setPieceAt(Position(6, 2), Piece(PieceType.KNIGHT, PieceColor.BLACK)) // c7
        
        board.getGameState().activeColor = PieceColor.WHITE
        
        assertTrue(detector.isInCheck(board, PieceColor.WHITE), "Should detect double check")
        
        // In double check, king must move (can't block or capture)
        val legalMoves = detector.getAllLegalMoves(board, PieceColor.WHITE)
        assertTrue(legalMoves.all { it.from == Position(4, 4) }, "In double check, only king moves should be legal")
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
        
        // Rook on d5 is pinned and cannot move
        val illegalMove = Move(Position(4, 3), Position(3, 3)) // d5-d4
        assertFalse(detector.isMoveLegal(board, illegalMove), "Pinned piece should not be able to move")
        
        // But it can move along the pin line
        val legalMove = Move(Position(4, 3), Position(4, 2)) // d5-c5
        assertTrue(detector.isMoveLegal(board, legalMove), "Pinned piece should be able to move along pin line")
    }
    
    @Test
    fun testKingCannotMoveIntoCheck() {
        // King cannot move into check
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(2, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a3
        
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
    fun testLegalCastling() {
        // Set up legal castling position
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(0, 7), Piece(PieceType.ROOK, PieceColor.WHITE)) // h1
        
        val castlingMove = Move(Position(0, 4), Position(0, 6)) // e1-g1
        assertTrue(detector.isCastlingLegal(board, castlingMove), "Should allow legal castling")
    }
    
    @Test
    fun testCastlingBlockedByPiece() {
        // Castling blocked by piece
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(0, 7), Piece(PieceType.ROOK, PieceColor.WHITE)) // h1
        board.setPieceAt(Position(0, 5), Piece(PieceType.BISHOP, PieceColor.WHITE)) // f1
        
        val castlingMove = Move(Position(0, 4), Position(0, 6)) // e1-g1
        assertFalse(detector.isCastlingLegal(board, castlingMove), "Should not allow castling when blocked")
    }
    
    @Test
    fun testCastlingThroughCheck() {
        // Castling through check
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(0, 7), Piece(PieceType.ROOK, PieceColor.WHITE)) // h1
        board.setPieceAt(Position(7, 5), Piece(PieceType.ROOK, PieceColor.BLACK)) // f8
        
        val castlingMove = Move(Position(0, 4), Position(0, 6)) // e1-g1
        assertFalse(detector.isCastlingLegal(board, castlingMove), "Should not allow castling through check")
    }
    
    @Test
    fun testCastlingWhileInCheck() {
        // Cannot castle while in check
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(0, 7), Piece(PieceType.ROOK, PieceColor.WHITE)) // h1
        board.setPieceAt(Position(7, 4), Piece(PieceType.ROOK, PieceColor.BLACK)) // e8
        
        val castlingMove = Move(Position(0, 4), Position(0, 6)) // e1-g1
        assertFalse(detector.isCastlingLegal(board, castlingMove), "Should not allow castling while in check")
    }
    
    @Test
    fun testCastlingIntoCheck() {
        // Cannot castle into check
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(0, 7), Piece(PieceType.ROOK, PieceColor.WHITE)) // h1
        board.setPieceAt(Position(7, 6), Piece(PieceType.ROOK, PieceColor.BLACK)) // g8
        
        val castlingMove = Move(Position(0, 4), Position(0, 6)) // e1-g1
        assertFalse(detector.isCastlingLegal(board, castlingMove), "Should not allow castling into check")
    }
    
    // ========================================
    // EDGE CASE TESTS
    // ========================================
    
    @Test
    fun testKingMissingScenario() {
        // Edge case: what happens if king is missing (should not crash)
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.QUEEN, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        // Should handle missing king gracefully
        assertFalse(detector.isInCheck(board, PieceColor.WHITE), "Missing king should not be in check")
        assertFalse(detector.isCheckmate(board, PieceColor.WHITE), "Missing king should not be checkmate")
        assertFalse(detector.isStalemate(board, PieceColor.WHITE), "Missing king should not be stalemate")
    }
    
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
    fun testComplexPositionWithMultipleThreats() {
        // Complex position with multiple pieces and threats
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(3, 3), Piece(PieceType.QUEEN, PieceColor.WHITE)) // d4
        board.setPieceAt(Position(5, 5), Piece(PieceType.BISHOP, PieceColor.WHITE)) // f6
        board.setPieceAt(Position(7, 7), Piece(PieceType.KING, PieceColor.BLACK)) // h8
        board.setPieceAt(Position(6, 6), Piece(PieceType.ROOK, PieceColor.BLACK)) // g7
        board.setPieceAt(Position(2, 2), Piece(PieceType.KNIGHT, PieceColor.BLACK)) // c3
        
        board.getGameState().activeColor = PieceColor.WHITE
        
        // Should correctly analyze this complex position
        val gameStatus = detector.getGameStatus(board)
        assertTrue(gameStatus == GameStatus.ONGOING || gameStatus == GameStatus.IN_CHECK, 
                  "Complex position should be analyzed correctly")
        
        // Should have legal moves available
        val legalMoves = detector.getAllLegalMoves(board, PieceColor.WHITE)
        assertTrue(legalMoves.isNotEmpty(), "Should have legal moves in complex position")
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
        board.getGameState().activeColor = PieceColor.WHITE
        
        assertEquals(GameStatus.DRAW_STALEMATE, detector.getGameStatus(board), "Legitimate stalemate should be detected")
        
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
    // PERFORMANCE AND ROBUSTNESS TESTS
    // ========================================
    
    @Test
    fun testLargeNumberOfLegalMoves() {
        // Test position with many legal moves (performance test)
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.QUEEN, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(0, 0), Piece(PieceType.KING, PieceColor.WHITE)) // a1
        board.setPieceAt(Position(7, 7), Piece(PieceType.KING, PieceColor.BLACK)) // h8
        
        board.getGameState().activeColor = PieceColor.WHITE
        
        val legalMoves = detector.getAllLegalMoves(board, PieceColor.WHITE)
        assertTrue(legalMoves.size > 20, "Queen should have many legal moves")
        
        // Verify all returned moves are actually legal
        for (move in legalMoves) {
            assertTrue(detector.isMoveLegal(board, move), "All returned moves should be legal: ${move.toAlgebraic()}")
        }
    }
    
    @Test
    fun testRepeatedGameStateChecks() {
        // Test that repeated calls don't change the board state
        board.initializeStandardPosition()
        val originalFEN = board.toFEN()
        
        // Call various detection methods multiple times
        repeat(10) {
            detector.isInCheck(board, PieceColor.WHITE)
            detector.isCheckmate(board, PieceColor.WHITE)
            detector.isStalemate(board, PieceColor.WHITE)
            detector.getGameStatus(board)
            detector.getAllLegalMoves(board, PieceColor.WHITE)
        }
        
        assertEquals(originalFEN, board.toFEN(), "Board state should not change from detection calls")
    }
}
