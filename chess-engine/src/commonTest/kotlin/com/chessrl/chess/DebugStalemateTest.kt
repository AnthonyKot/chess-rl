package com.chessrl.chess

import kotlin.test.*

/**
 * Debug the stalemate with pawn blocking test
 */
class DebugStalemateTest {
    
    @Test
    fun debugStalemateWithPawnBlocking() {
        val board = ChessBoard()
        val detector = GameStateDetector()
        
        // Use the EXACT same position as the working test
        board.clearBoard()
        board.setPieceAt(Position(0, 0), Piece(PieceType.KING, PieceColor.WHITE)) // a1
        board.setPieceAt(Position(2, 1), Piece(PieceType.KING, PieceColor.BLACK)) // b3 (controls b1, b2, a2)
        board.setPieceAt(Position(1, 2), Piece(PieceType.QUEEN, PieceColor.BLACK)) // c2 (controls a2, b1)
        
        // Check if king is in check
        val inCheck = detector.isInCheck(board, PieceColor.WHITE)
        println("King in check: $inCheck")
        
        // Get all legal moves
        val legalMoves = detector.getAllLegalMoves(board, PieceColor.WHITE)
        println("Legal moves: ${legalMoves.map { it.toAlgebraic() }}")
        
        // Check stalemate
        val isStalemate = detector.isStalemate(board, PieceColor.WHITE)
        println("Is stalemate: $isStalemate")
        
        // Check what squares are attacked by black pieces
        println("Queen attacks a2: ${detector.isSquareAttacked(board, Position(1, 0), PieceColor.BLACK)}")
        println("Queen attacks b1: ${detector.isSquareAttacked(board, Position(0, 1), PieceColor.BLACK)}")
        println("King attacks a2: ${detector.isSquareAttacked(board, Position(1, 0), PieceColor.BLACK)}")
        println("King attacks b2: ${detector.isSquareAttacked(board, Position(1, 1), PieceColor.BLACK)}")
        println("King attacks b1: ${detector.isSquareAttacked(board, Position(0, 1), PieceColor.BLACK)}")
        
        // The test should pass if:
        // 1. King is not in check
        // 2. No legal moves available
        // 3. Therefore stalemate
        
        assertFalse(inCheck, "King should not be in check")
        assertTrue(legalMoves.isEmpty(), "Should have no legal moves")
        assertTrue(isStalemate, "Should detect stalemate with pawn blocking")
    }
}