package com.chessrl.chess

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Chess engine package tests
 */
class ChessBoardTest {
    
    @Test
    fun testChessBoardCreation() {
        val board = ChessBoard()
        assertNotNull(board, "Chess board should be created successfully")
    }
    
    @Test
    fun testPieceCreation() {
        val piece = Piece(PieceType.KING, PieceColor.WHITE)
        assertEquals(PieceType.KING, piece.type)
        assertEquals(PieceColor.WHITE, piece.color)
    }
    
    @Test
    fun testMoveCreation() {
        val from = Position(0, 0)
        val to = Position(1, 1)
        val move = Move(from, to)
        assertEquals(from, move.from)
        assertEquals(to, move.to)
    }
    
    @Test
    fun testPositionCreation() {
        val position = Position(4, 4)
        assertEquals(4, position.rank)
        assertEquals(4, position.file)
    }
}