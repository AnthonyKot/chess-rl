package com.chessrl.chess

import kotlin.test.*

/**
 * Simple debug test to understand what's happening
 */
class SimpleDebugTest {
    
    @Test
    fun testBasicQueenAttack() {
        val board = ChessBoard()
        board.clearBoard()
        board.setPieceAt(Position(0, 5), Piece(PieceType.QUEEN, PieceColor.BLACK)) // f1
        board.setPieceAt(Position(0, 7), Piece(PieceType.KING, PieceColor.WHITE)) // h1
        
        val detector = GameStateDetector()
        
        // Queen on f1 should attack h1 (horizontally)
        val attacks = detector.isSquareAttacked(board, Position(0, 7), PieceColor.BLACK)
        assertTrue(attacks, "Queen on f1 should attack h1")
        
        // King should be in check
        val inCheck = detector.isInCheck(board, PieceColor.WHITE)
        assertTrue(inCheck, "King should be in check from queen")
    }
    
    @Test
    fun testKnightAttack() {
        val board = ChessBoard()
        board.clearBoard()
        board.setPieceAt(Position(5, 5), Piece(PieceType.KNIGHT, PieceColor.WHITE)) // f6
        board.setPieceAt(Position(7, 6), Piece(PieceType.KING, PieceColor.BLACK)) // g8
        
        val detector = GameStateDetector()
        
        // Knight on f6 should attack g8
        val attacks = detector.isSquareAttacked(board, Position(7, 6), PieceColor.WHITE)
        assertTrue(attacks, "Knight on f6 should attack g8")
        
        // King should be in check
        val inCheck = detector.isInCheck(board, PieceColor.BLACK)
        assertTrue(inCheck, "King should be in check from knight")
    }
}