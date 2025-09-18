package com.chessrl.chess

import kotlin.test.*

/**
 * Debug specific game state detection issues
 */
class DebugGameStateTest {
    private lateinit var board: ChessBoard
    private lateinit var detector: GameStateDetector
    
    @BeforeTest
    fun setup() {
        board = ChessBoard()
        detector = GameStateDetector()
    }
    
    @Test
    fun debugStalemateWithPawnBlocking() {
        // Stalemate where pawn blocks king's only escape
        board.clearBoard()
        board.setPieceAt(Position(0, 7), Piece(PieceType.KING, PieceColor.WHITE)) // h1
        board.setPieceAt(Position(1, 7), Piece(PieceType.PAWN, PieceColor.WHITE)) // h2 (own pawn blocks)
        board.setPieceAt(Position(1, 6), Piece(PieceType.PAWN, PieceColor.WHITE)) // g2 (own pawn blocks)
        board.setPieceAt(Position(2, 5), Piece(PieceType.KING, PieceColor.BLACK)) // f3
        board.setPieceAt(Position(0, 5), Piece(PieceType.QUEEN, PieceColor.BLACK)) // f1
        
        board.getGameState().activeColor = PieceColor.WHITE
        
        println("=== Debug Stalemate with Pawn Blocking ===")
        println("Board FEN: ${board.toFEN()}")
        println("Board ASCII:")
        println(board.toASCII())
        
        println("Is in check: ${detector.isInCheck(board, PieceColor.WHITE)}")
        val legalMoves = detector.getAllLegalMoves(board, PieceColor.WHITE)
        println("Legal moves: ${legalMoves.map { it.toAlgebraic() }}")
        println("Is stalemate: ${detector.isStalemate(board, PieceColor.WHITE)}")
        println("Game status: ${detector.getGameStatus(board)}")
        
        // Let's check what squares the queen attacks
        println("Queen attacks f1: ${detector.isSquareAttacked(board, Position(0, 5), PieceColor.BLACK)}")
        println("Queen attacks g1: ${detector.isSquareAttacked(board, Position(0, 6), PieceColor.BLACK)}")
        println("Queen attacks h1: ${detector.isSquareAttacked(board, Position(0, 7), PieceColor.BLACK)}")
        println("Queen attacks g2: ${detector.isSquareAttacked(board, Position(1, 6), PieceColor.BLACK)}")
        println("Queen attacks h2: ${detector.isSquareAttacked(board, Position(1, 7), PieceColor.BLACK)}")
        
        assertFalse(detector.isInCheck(board, PieceColor.WHITE), "King should not be in check")
        assertTrue(legalMoves.isEmpty(), "Should have no legal moves")
        assertTrue(detector.isStalemate(board, PieceColor.WHITE), "Should detect stalemate with pawn blocking")
    }
    
    @Test
    fun debugSmotheredMate() {
        // Smothered mate pattern
        board.clearBoard()
        board.setPieceAt(Position(7, 6), Piece(PieceType.KING, PieceColor.BLACK)) // g8
        board.setPieceAt(Position(6, 5), Piece(PieceType.PAWN, PieceColor.BLACK)) // f7
        board.setPieceAt(Position(6, 6), Piece(PieceType.PAWN, PieceColor.BLACK)) // g7
        board.setPieceAt(Position(6, 7), Piece(PieceType.PAWN, PieceColor.BLACK)) // h7
        board.setPieceAt(Position(7, 7), Piece(PieceType.ROOK, PieceColor.BLACK)) // h8
        board.setPieceAt(Position(5, 5), Piece(PieceType.KNIGHT, PieceColor.WHITE)) // f6
        
        board.getGameState().activeColor = PieceColor.BLACK
        
        println("=== Debug Smothered Mate ===")
        println("Board FEN: ${board.toFEN()}")
        println("Board ASCII:")
        println(board.toASCII())
        
        println("Is in check: ${detector.isInCheck(board, PieceColor.BLACK)}")
        val legalMoves = detector.getAllLegalMoves(board, PieceColor.BLACK)
        println("Legal moves: ${legalMoves.map { it.toAlgebraic() }}")
        println("Is checkmate: ${detector.isCheckmate(board, PieceColor.BLACK)}")
        println("Game status: ${detector.getGameStatus(board)}")
        
        // Check knight attack
        println("Knight attacks g8: ${detector.isSquareAttacked(board, Position(7, 6), PieceColor.WHITE)}")
        
        assertTrue(detector.isInCheck(board, PieceColor.BLACK), "King should be in check")
        assertTrue(legalMoves.isEmpty(), "Should have no legal moves")
        assertTrue(detector.isCheckmate(board, PieceColor.BLACK), "Should detect smothered mate")
    }
}