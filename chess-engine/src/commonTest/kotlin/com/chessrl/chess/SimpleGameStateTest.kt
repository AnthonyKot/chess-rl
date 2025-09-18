package com.chessrl.chess

import kotlin.test.*

/**
 * Simple test to debug game state detection issues
 */
class SimpleGameStateTest {
    private lateinit var board: ChessBoard
    private lateinit var detector: GameStateDetector
    
    @BeforeTest
    fun setup() {
        board = ChessBoard()
        detector = GameStateDetector()
    }
    
    @Test
    fun testBasicStalemate() {
        // Simple stalemate test
        board.clearBoard()
        board.setPieceAt(Position(0, 0), Piece(PieceType.KING, PieceColor.WHITE)) // a1
        board.setPieceAt(Position(2, 1), Piece(PieceType.KING, PieceColor.BLACK)) // b3
        board.setPieceAt(Position(1, 2), Piece(PieceType.QUEEN, PieceColor.BLACK)) // c2
        
        board.getGameState().activeColor = PieceColor.WHITE
        
        println("Board FEN: ${board.toFEN()}")
        println("Is in check: ${detector.isInCheck(board, PieceColor.WHITE)}")
        println("Legal moves: ${detector.getAllLegalMoves(board, PieceColor.WHITE)}")
        println("Is stalemate: ${detector.isStalemate(board, PieceColor.WHITE)}")
        println("Game status: ${detector.getGameStatus(board)}")
        
        assertFalse(detector.isInCheck(board, PieceColor.WHITE), "King should not be in check")
        assertTrue(detector.getAllLegalMoves(board, PieceColor.WHITE).isEmpty(), "Should have no legal moves")
        assertTrue(detector.isStalemate(board, PieceColor.WHITE), "Should detect stalemate")
    }
    
    @Test
    fun testBasicCheckmate() {
        // Simple checkmate test
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(1, 3), Piece(PieceType.PAWN, PieceColor.WHITE)) // d2
        board.setPieceAt(Position(1, 4), Piece(PieceType.PAWN, PieceColor.WHITE)) // e2
        board.setPieceAt(Position(1, 5), Piece(PieceType.PAWN, PieceColor.WHITE)) // f2
        board.setPieceAt(Position(0, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a1
        
        board.getGameState().activeColor = PieceColor.WHITE
        
        println("Board FEN: ${board.toFEN()}")
        println("Is in check: ${detector.isInCheck(board, PieceColor.WHITE)}")
        println("Legal moves: ${detector.getAllLegalMoves(board, PieceColor.WHITE)}")
        println("Is checkmate: ${detector.isCheckmate(board, PieceColor.WHITE)}")
        println("Game status: ${detector.getGameStatus(board)}")
        
        assertTrue(detector.isInCheck(board, PieceColor.WHITE), "King should be in check")
        assertTrue(detector.getAllLegalMoves(board, PieceColor.WHITE).isEmpty(), "Should have no legal moves")
        assertTrue(detector.isCheckmate(board, PieceColor.WHITE), "Should detect checkmate")
    }
}