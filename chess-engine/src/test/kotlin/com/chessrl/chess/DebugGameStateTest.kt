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
        board.setPieceAt(Position(0, 1), Piece(PieceType.KING, PieceColor.WHITE)) // b1
        board.setPieceAt(Position(1, 1), Piece(PieceType.PAWN, PieceColor.WHITE)) // b2 (own pawn blocks)
        board.setPieceAt(Position(2, 0), Piece(PieceType.KING, PieceColor.BLACK)) // a3
        board.setPieceAt(Position(0, 3), Piece(PieceType.QUEEN, PieceColor.BLACK)) // d1
        
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
        println("Queen attacks d1: ${detector.isSquareAttacked(board, Position(0, 3), PieceColor.BLACK)}")
        println("Queen attacks b1: ${detector.isSquareAttacked(board, Position(0, 1), PieceColor.BLACK)}")
        println("Queen attacks c1: ${detector.isSquareAttacked(board, Position(0, 2), PieceColor.BLACK)}")
        println("Queen attacks b2: ${detector.isSquareAttacked(board, Position(1, 1), PieceColor.BLACK)}")
        println("Queen attacks a2: ${detector.isSquareAttacked(board, Position(1, 0), PieceColor.BLACK)}")
        
        assertFalse(detector.isInCheck(board, PieceColor.WHITE), "King should not be in check")
        assertTrue(legalMoves.isEmpty(), "Should have no legal moves")
        assertTrue(detector.isStalemate(board, PieceColor.WHITE), "Should detect stalemate with pawn blocking")
    }
    
    @Test
    fun debugSmotheredMate() {
        // Smothered mate pattern
        require(board.fromFEN("6rk/5Npp/8/8/8/8/8/7K b - - 0 1"))

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
