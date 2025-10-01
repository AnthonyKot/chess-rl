package com.chessrl.chess

import kotlin.test.*

/**
 * Debug the smothered mate test
 */
class DebugSmotheredMateTest {
    
    @Test
    fun debugSmotheredMate() {
        val board = ChessBoard()
        val detector = GameStateDetector()
        
        // Smothered mate pattern from the failing test
        require(board.fromFEN("6rk/5Npp/8/8/8/8/8/7K b - - 0 1"))

        println("=== Debug Smothered Mate ===")
        println("Board FEN: ${board.toFEN()}")
        println("Active color: ${board.getActiveColor()}")
        
        // Check if king is in check
        val inCheck = detector.isInCheck(board, PieceColor.BLACK)
        println("King in check: $inCheck")
        
        // Get all legal moves
        val legalMoves = detector.getAllLegalMoves(board, PieceColor.BLACK)
        println("Legal moves: ${legalMoves.map { it.toAlgebraic() }}")
        
        // Check checkmate
        val isCheckmate = detector.isCheckmate(board, PieceColor.BLACK)
        println("Is checkmate: $isCheckmate")
        
        // Check what squares the knight attacks
        println("Knight attacks g8: ${detector.isSquareAttacked(board, Position(7, 6), PieceColor.WHITE)}")
        println("Knight attacks h8: ${detector.isSquareAttacked(board, Position(7, 7), PieceColor.WHITE)}")
        println("Knight attacks f8: ${detector.isSquareAttacked(board, Position(7, 5), PieceColor.WHITE)}")
        
        // For checkmate:
        // 1. King must be in check
        // 2. No legal moves available
        
        assertTrue(inCheck, "King should be in check")
        assertTrue(legalMoves.isEmpty(), "Should have no legal moves")
        assertTrue(isCheckmate, "Should detect checkmate")
    }
}
