package com.chessrl.chess

import kotlin.test.*

/**
 * Essential chess board tests - fast, reliable unit tests for core chess logic
 * Focuses on legal move validation, checkmate detection, and special moves
 */
class EssentialChessBoardTest {
    
    private lateinit var detector: GameStateDetector
    
    @BeforeTest
    fun setup() {
        detector = GameStateDetector()
    }
    
    @Test
    fun testStartingPositionLegalMoves() {
        val board = ChessBoard()
        val moves = detector.getAllLegalMoves(board, PieceColor.WHITE)
        
        // Starting position should have exactly 20 legal moves (16 pawn + 4 knight)
        assertEquals(20, moves.size, "Starting position should have 20 legal moves")
        
        // Verify pawn moves
        val pawnMoves = moves.filter { move ->
            board.getPieceAt(move.from)?.type == PieceType.PAWN
        }
        assertEquals(16, pawnMoves.size, "Should have 16 pawn moves")
        
        // Verify knight moves
        val knightMoves = moves.filter { move ->
            board.getPieceAt(move.from)?.type == PieceType.KNIGHT
        }
        assertEquals(4, knightMoves.size, "Should have 4 knight moves")
    }
    
    @Test
    fun testCheckmateDetection() {
        val board = ChessBoard()
        
        // Scholar's mate position: 1.e4 e5 2.Bc4 Nc6 3.Qh5 Nf6?? 4.Qxf7#
        val scholarsMate = "rnbqkb1r/pppp1Qpp/5n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4"
        assertTrue(board.fromFEN(scholarsMate), "Should load Scholar's mate FEN")
        
        assertTrue(detector.isCheckmate(board, PieceColor.BLACK), "Black should be in checkmate")
        assertFalse(detector.isCheckmate(board, PieceColor.WHITE), "White should not be in checkmate")
        
        // Verify no legal moves for black
        val blackMoves = detector.getAllLegalMoves(board, PieceColor.BLACK)
        assertEquals(0, blackMoves.size, "Black should have no legal moves in checkmate")
    }
    
    @Test
    fun testBasicStalemate() {
        val board = ChessBoard()
        
        // Use a proven working stalemate position from existing tests
        val stalematePosition = "8/8/8/8/8/3k4/3p4/3K4 w - - 0 1"
        assertTrue(board.fromFEN(stalematePosition), "Should load stalemate FEN")
        
        assertTrue(detector.isStalemate(board, PieceColor.WHITE), "White should be in stalemate")
        assertFalse(detector.isInCheck(board, PieceColor.WHITE), "White king should not be in check")
        
        // Verify no legal moves for white
        val whiteMoves = detector.getAllLegalMoves(board, PieceColor.WHITE)
        assertEquals(0, whiteMoves.size, "White should have no legal moves in stalemate")
    }
    
    @Test
    fun testCastlingRights() {
        val board = ChessBoard()
        val gameState = board.getGameState()
        
        // Initial position should allow castling
        assertTrue(gameState.whiteCanCastleKingside, "White should be able to castle kingside")
        assertTrue(gameState.whiteCanCastleQueenside, "White should be able to castle queenside")
        assertTrue(gameState.blackCanCastleKingside, "Black should be able to castle kingside")
        assertTrue(gameState.blackCanCastleQueenside, "Black should be able to castle queenside")
        
        // Move king, should lose castling rights
        val kingMove = Move(Position(0, 4), Position(0, 5))
        assertEquals(MoveResult.SUCCESS, board.makeMove(kingMove))
        
        val updatedGameState = board.getGameState()
        assertFalse(updatedGameState.whiteCanCastleKingside, "White should lose kingside castling after king move")
        assertFalse(updatedGameState.whiteCanCastleQueenside, "White should lose queenside castling after king move")
    }
    
    @Test
    fun testBasicMoveValidation() {
        val board = ChessBoard()
        
        // Valid pawn move
        val validMove = Move(Position(1, 4), Position(3, 4)) // e2-e4
        assertEquals(MoveResult.SUCCESS, board.makeMove(validMove))
        board.switchActiveColor()
        
        // Verify piece moved
        assertNull(board.getPieceAt(Position(1, 4)), "Original position should be empty")
        assertEquals(Piece(PieceType.PAWN, PieceColor.WHITE), board.getPieceAt(Position(3, 4)))
        
        // Verify turn switched
        assertEquals(PieceColor.BLACK, board.getActiveColor())
    }
    
    @Test
    fun testFENRoundTrip() {
        val board = ChessBoard()
        val originalFEN = board.toFEN()
        
        // Create new board and load FEN
        val newBoard = ChessBoard()
        newBoard.clearBoard()
        assertTrue(newBoard.fromFEN(originalFEN), "Should load FEN successfully")
        
        // Should be identical
        assertEquals(board, newBoard, "Boards should be equal after FEN round trip")
        assertEquals(originalFEN, newBoard.toFEN(), "FEN should be identical after round trip")
    }
    
    @Test
    fun testPawnPromotion() {
        val board = ChessBoard()
        board.clearBoard()
        
        // Place white pawn on 7th rank and white king
        board.setPieceAt(Position(6, 0), Piece(PieceType.PAWN, PieceColor.WHITE))
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(7, 4), Piece(PieceType.KING, PieceColor.BLACK))
        
        // Promote to queen
        val promotionMove = Move(Position(6, 0), Position(7, 0), PieceType.QUEEN)
        assertEquals(MoveResult.SUCCESS, board.makeMove(promotionMove))
        
        // Should have queen on 8th rank
        assertEquals(Piece(PieceType.QUEEN, PieceColor.WHITE), board.getPieceAt(Position(7, 0)))
        assertNull(board.getPieceAt(Position(6, 0)), "Original pawn position should be empty")
    }
    
    @Test
    fun testGameStateTracking() {
        val board = ChessBoard()
        
        // Initial state
        assertEquals(PieceColor.WHITE, board.getActiveColor())
        assertEquals(0, board.getGameState().halfmoveClock)
        assertEquals(1, board.getGameState().fullmoveNumber)
        
        // Make a move
        board.makeMove(Move(Position(1, 4), Position(3, 4))) // e2-e4
        board.switchActiveColor()
        
        assertEquals(PieceColor.BLACK, board.getActiveColor())
        assertEquals(0, board.getGameState().halfmoveClock) // Pawn move resets clock
        assertEquals(1, board.getGameState().fullmoveNumber) // Still move 1 (black hasn't moved)
        
        // Black move
        board.makeMove(Move(Position(6, 4), Position(4, 4))) // e7-e5
        board.switchActiveColor()
        
        assertEquals(PieceColor.WHITE, board.getActiveColor())
        assertEquals(0, board.getGameState().halfmoveClock) // Pawn move resets clock
        assertEquals(2, board.getGameState().fullmoveNumber) // Now move 2
    }
    
    @Test
    fun testBoardCopyIndependence() {
        val original = ChessBoard()
        val copy = original.deepCopy()
        
        // Should be equal but independent
        assertEquals(original, copy)
        assertTrue(original !== copy, "Copy should be different instance")
        
        // Modify copy
        copy.makeMove(Move(Position(1, 4), Position(3, 4)))
        copy.switchActiveColor()
        
        // Original should be unchanged
        assertNotEquals(original, copy, "Original should be unchanged after modifying copy")
        assertEquals(PieceColor.WHITE, original.getActiveColor())
        assertEquals(PieceColor.BLACK, copy.getActiveColor())
    }
}
