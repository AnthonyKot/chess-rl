package com.chessrl.chess

import kotlin.test.*

/**
 * Test that exactly matches the existing working test
 */
class ExactMatchTest {
    private lateinit var board: ChessBoard
    private lateinit var detector: GameStateDetector
    
    @BeforeTest
    fun setup() {
        board = ChessBoard()
        detector = GameStateDetector()
    }
    
    @Test
    fun testExactMatchStalemate() {
        // Exact copy of the working test from GameStateDetectionTest
        board.clearBoard()
        board.setPieceAt(Position(0, 0), Piece(PieceType.KING, PieceColor.WHITE)) // a1
        board.setPieceAt(Position(2, 1), Piece(PieceType.KING, PieceColor.BLACK)) // b3 (controls b1, b2, a2)
        board.setPieceAt(Position(1, 2), Piece(PieceType.QUEEN, PieceColor.BLACK)) // c2 (controls a2, b1)
        
        // Don't set active color - use default (WHITE)
        
        assertTrue(detector.isStalemate(board, PieceColor.WHITE))
        assertFalse(detector.isCheckmate(board, PieceColor.WHITE))
    }
    
    @Test
    fun testExactMatchCheckmate() {
        // Exact copy of the working test from GameStateDetectionTest
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(1, 3), Piece(PieceType.PAWN, PieceColor.WHITE)) // d2
        board.setPieceAt(Position(1, 4), Piece(PieceType.PAWN, PieceColor.WHITE)) // e2
        board.setPieceAt(Position(1, 5), Piece(PieceType.PAWN, PieceColor.WHITE)) // f2
        board.setPieceAt(Position(0, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a1
        
        // Don't set active color - use default (WHITE)
        
        assertTrue(detector.isCheckmate(board, PieceColor.WHITE))
        assertFalse(detector.isStalemate(board, PieceColor.WHITE))
    }
}