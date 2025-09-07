package com.chessrl.chess

import kotlin.test.*

class MoveValidationTest {
    private lateinit var board: ChessBoard
    private lateinit var validator: MoveValidationSystem
    
    @BeforeTest
    fun setup() {
        board = ChessBoard()
        validator = MoveValidationSystem()
    }
    
    @Test
    fun testPawnMoves() {
        // Test white pawn moves from starting position
        val whitePawnPos = Position(1, 4) // e2
        val whitePawnMoves = validator.getValidMoves(board, whitePawnPos)
        
        assertTrue(whitePawnMoves.contains(Move(whitePawnPos, Position(2, 4)))) // e2-e3
        assertTrue(whitePawnMoves.contains(Move(whitePawnPos, Position(3, 4)))) // e2-e4
        assertEquals(2, whitePawnMoves.size)
        
        // Test black pawn moves from starting position
        board.switchActiveColor() // Switch to black
        val blackPawnPos = Position(6, 4) // e7
        val blackPawnMoves = validator.getValidMoves(board, blackPawnPos)
        
        assertTrue(blackPawnMoves.contains(Move(blackPawnPos, Position(5, 4)))) // e7-e6
        assertTrue(blackPawnMoves.contains(Move(blackPawnPos, Position(4, 4)))) // e7-e5
        assertEquals(2, blackPawnMoves.size)
    }
    
    @Test
    fun testPawnCaptures() {
        // Set up a position with capture opportunities
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.PAWN, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(5, 3), Piece(PieceType.PAWN, PieceColor.BLACK)) // d6
        board.setPieceAt(Position(5, 5), Piece(PieceType.PAWN, PieceColor.BLACK)) // f6
        
        val whitePawnMoves = validator.getValidMoves(board, Position(4, 4))
        
        assertTrue(whitePawnMoves.contains(Move(Position(4, 4), Position(5, 4)))) // e5-e6 (forward)
        assertTrue(whitePawnMoves.contains(Move(Position(4, 4), Position(5, 3)))) // e5xd6 (capture)
        assertTrue(whitePawnMoves.contains(Move(Position(4, 4), Position(5, 5)))) // e5xf6 (capture)
        assertEquals(3, whitePawnMoves.size)
    }
    
    @Test
    fun testPawnPromotion() {
        // Set up white pawn ready for promotion
        board.clearBoard()
        board.setPieceAt(Position(6, 0), Piece(PieceType.PAWN, PieceColor.WHITE)) // a7
        
        val promotionMoves = validator.getValidMoves(board, Position(6, 0))
        
        // Should have 4 promotion moves (Queen, Rook, Bishop, Knight)
        assertEquals(4, promotionMoves.size)
        assertTrue(promotionMoves.contains(Move(Position(6, 0), Position(7, 0), PieceType.QUEEN)))
        assertTrue(promotionMoves.contains(Move(Position(6, 0), Position(7, 0), PieceType.ROOK)))
        assertTrue(promotionMoves.contains(Move(Position(6, 0), Position(7, 0), PieceType.BISHOP)))
        assertTrue(promotionMoves.contains(Move(Position(6, 0), Position(7, 0), PieceType.KNIGHT)))
    }
    
    @Test
    fun testRookMoves() {
        // Test rook on empty board
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.ROOK, PieceColor.WHITE)) // e5
        
        val rookMoves = validator.getValidMoves(board, Position(4, 4))
        
        // Rook should have 14 moves (7 horizontal + 7 vertical)
        assertEquals(14, rookMoves.size)
        
        // Test some specific moves
        assertTrue(rookMoves.contains(Move(Position(4, 4), Position(4, 0)))) // e5-a5
        assertTrue(rookMoves.contains(Move(Position(4, 4), Position(4, 7)))) // e5-h5
        assertTrue(rookMoves.contains(Move(Position(4, 4), Position(0, 4)))) // e5-e1
        assertTrue(rookMoves.contains(Move(Position(4, 4), Position(7, 4)))) // e5-e8
    }
    
    @Test
    fun testRookBlockedPath() {
        // Test rook with blocked paths
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.ROOK, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(4, 6), Piece(PieceType.PAWN, PieceColor.WHITE)) // g5 (own piece)
        board.setPieceAt(Position(6, 4), Piece(PieceType.PAWN, PieceColor.BLACK)) // e7 (enemy piece)
        
        val rookMoves = validator.getValidMoves(board, Position(4, 4))
        
        // Should not be able to move to g5 or beyond (blocked by own piece)
        assertFalse(rookMoves.contains(Move(Position(4, 4), Position(4, 6))))
        assertFalse(rookMoves.contains(Move(Position(4, 4), Position(4, 7))))
        
        // Should be able to capture on e7 but not move beyond
        assertTrue(rookMoves.contains(Move(Position(4, 4), Position(6, 4))))
        assertFalse(rookMoves.contains(Move(Position(4, 4), Position(7, 4))))
    }
    
    @Test
    fun testBishopMoves() {
        // Test bishop on empty board
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.BISHOP, PieceColor.WHITE)) // e5
        
        val bishopMoves = validator.getValidMoves(board, Position(4, 4))
        
        // Bishop should have 13 moves (4 diagonal directions)
        assertEquals(13, bishopMoves.size)
        
        // Test some specific moves
        assertTrue(bishopMoves.contains(Move(Position(4, 4), Position(7, 7)))) // e5-h8
        assertTrue(bishopMoves.contains(Move(Position(4, 4), Position(1, 1)))) // e5-b2
        assertTrue(bishopMoves.contains(Move(Position(4, 4), Position(0, 0)))) // e5-a1
        assertTrue(bishopMoves.contains(Move(Position(4, 4), Position(7, 1)))) // e5-b8
    }
    
    @Test
    fun testKnightMoves() {
        // Test knight on empty board
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KNIGHT, PieceColor.WHITE)) // e5
        
        val knightMoves = validator.getValidMoves(board, Position(4, 4))
        
        // Knight should have 8 moves from center
        assertEquals(8, knightMoves.size)
        
        // Test all L-shaped moves
        val expectedMoves = listOf(
            Move(Position(4, 4), Position(6, 5)), // e5-f7
            Move(Position(4, 4), Position(6, 3)), // e5-d7
            Move(Position(4, 4), Position(2, 5)), // e5-f3
            Move(Position(4, 4), Position(2, 3)), // e5-d3
            Move(Position(4, 4), Position(5, 6)), // e5-g6
            Move(Position(4, 4), Position(5, 2)), // e5-c6
            Move(Position(4, 4), Position(3, 6)), // e5-g4
            Move(Position(4, 4), Position(3, 2))  // e5-c4
        )
        
        for (move in expectedMoves) {
            assertTrue(knightMoves.contains(move), "Knight should be able to move to ${move.to.toAlgebraic()}")
        }
    }
    
    @Test
    fun testKnightCornerMoves() {
        // Test knight in corner (limited moves)
        board.clearBoard()
        board.setPieceAt(Position(0, 0), Piece(PieceType.KNIGHT, PieceColor.WHITE)) // a1
        
        val knightMoves = validator.getValidMoves(board, Position(0, 0))
        
        // Knight should have only 2 moves from corner
        assertEquals(2, knightMoves.size)
        assertTrue(knightMoves.contains(Move(Position(0, 0), Position(2, 1)))) // a1-b3
        assertTrue(knightMoves.contains(Move(Position(0, 0), Position(1, 2)))) // a1-c2
    }
    
    @Test
    fun testQueenMoves() {
        // Test queen on empty board
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.QUEEN, PieceColor.WHITE)) // e5
        
        val queenMoves = validator.getValidMoves(board, Position(4, 4))
        
        // Queen should have 27 moves (14 rook-like + 13 bishop-like)
        assertEquals(27, queenMoves.size)
        
        // Test some specific moves (combination of rook and bishop)
        assertTrue(queenMoves.contains(Move(Position(4, 4), Position(4, 0)))) // e5-a5 (rook-like)
        assertTrue(queenMoves.contains(Move(Position(4, 4), Position(7, 7)))) // e5-h8 (bishop-like)
        assertTrue(queenMoves.contains(Move(Position(4, 4), Position(0, 4)))) // e5-e1 (rook-like)
        assertTrue(queenMoves.contains(Move(Position(4, 4), Position(1, 1)))) // e5-b2 (bishop-like)
    }
    
    @Test
    fun testKingMoves() {
        // Test king on empty board
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        
        val kingMoves = validator.getValidMoves(board, Position(4, 4))
        
        // King should have 8 moves (one square in each direction)
        assertEquals(8, kingMoves.size)
        
        // Test all adjacent squares
        val expectedMoves = listOf(
            Move(Position(4, 4), Position(5, 4)), // e5-e6
            Move(Position(4, 4), Position(5, 5)), // e5-f6
            Move(Position(4, 4), Position(4, 5)), // e5-f5
            Move(Position(4, 4), Position(3, 5)), // e5-f4
            Move(Position(4, 4), Position(3, 4)), // e5-e4
            Move(Position(4, 4), Position(3, 3)), // e5-d4
            Move(Position(4, 4), Position(4, 3)), // e5-d5
            Move(Position(4, 4), Position(5, 3))  // e5-d6
        )
        
        for (move in expectedMoves) {
            assertTrue(kingMoves.contains(move), "King should be able to move to ${move.to.toAlgebraic()}")
        }
    }
    
    @Test
    fun testInvalidMoves() {
        // Test various invalid moves
        
        // Pawn moving backwards
        assertFalse(validator.isValidMove(board, Move(Position(1, 4), Position(0, 4))))
        
        // Rook moving diagonally
        assertFalse(validator.isValidMove(board, Move(Position(0, 0), Position(1, 1))))
        
        // Bishop moving horizontally
        assertFalse(validator.isValidMove(board, Move(Position(0, 2), Position(0, 5))))
        
        // Knight moving like a rook
        assertFalse(validator.isValidMove(board, Move(Position(0, 1), Position(0, 3))))
        
        // King moving two squares
        assertFalse(validator.isValidMove(board, Move(Position(0, 4), Position(2, 4))))
    }
    
    @Test
    fun testMoveValidationWithWrongTurn() {
        // Test that pieces can't move when it's not their turn
        
        // It's white's turn initially
        val blackPawnMove = Move(Position(6, 4), Position(5, 4)) // e7-e6
        assertFalse(validator.isValidMove(board, blackPawnMove))
        
        // Switch to black's turn
        board.switchActiveColor()
        
        // Now white can't move
        val whitePawnMove = Move(Position(1, 4), Position(2, 4)) // e2-e3
        assertFalse(validator.isValidMove(board, whitePawnMove))
        
        // But black can move
        assertTrue(validator.isValidMove(board, blackPawnMove))
    }
    
    @Test
    fun testGetAllValidMovesForColor() {
        // Test getting all valid moves for white in starting position
        val whiteMoves = validator.getAllValidMoves(board, PieceColor.WHITE)
        
        // White should have 20 moves in starting position (16 pawn moves + 4 knight moves)
        assertEquals(20, whiteMoves.size)
        
        // Test getting all valid moves for black (should be empty since it's white's turn)
        val blackMoves = validator.getAllValidMoves(board, PieceColor.BLACK)
        assertEquals(0, blackMoves.size)
        
        // Switch turns and test again
        board.switchActiveColor()
        val blackMovesAfterSwitch = validator.getAllValidMoves(board, PieceColor.BLACK)
        assertEquals(20, blackMovesAfterSwitch.size)
    }
    
    @Test
    fun testCaptureValidation() {
        // Set up a position with capture opportunities
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.QUEEN, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(4, 6), Piece(PieceType.PAWN, PieceColor.BLACK)) // g5
        board.setPieceAt(Position(6, 6), Piece(PieceType.BISHOP, PieceColor.BLACK)) // g7
        
        val queenMoves = validator.getValidMoves(board, Position(4, 4))
        
        // Queen should be able to capture both black pieces
        assertTrue(queenMoves.contains(Move(Position(4, 4), Position(4, 6)))) // Capture pawn
        assertTrue(queenMoves.contains(Move(Position(4, 4), Position(6, 6)))) // Capture bishop
        
        // But should not be able to move beyond captured pieces
        assertFalse(queenMoves.contains(Move(Position(4, 4), Position(4, 7)))) // Beyond pawn
        assertFalse(queenMoves.contains(Move(Position(4, 4), Position(7, 7)))) // Beyond bishop
    }
    
    @Test
    fun testCannotCaptureOwnPieces() {
        // Set up position with own pieces blocking
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.ROOK, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(4, 6), Piece(PieceType.PAWN, PieceColor.WHITE)) // g5 (own piece)
        
        val rookMoves = validator.getValidMoves(board, Position(4, 4))
        
        // Rook should not be able to move to or beyond own pawn
        assertFalse(rookMoves.contains(Move(Position(4, 4), Position(4, 6))))
        assertFalse(rookMoves.contains(Move(Position(4, 4), Position(4, 7))))
        
        // But should be able to move to squares before the pawn
        assertTrue(rookMoves.contains(Move(Position(4, 4), Position(4, 5))))
    }
    
    @Test
    fun testEdgeCases() {
        // Test moves to invalid positions
        assertFalse(validator.isValidMove(board, Move(Position(0, 0), Position(-1, 0))))
        assertFalse(validator.isValidMove(board, Move(Position(0, 0), Position(0, 8))))
        
        // Test moves from empty squares
        board.clearBoard()
        assertEquals(0, validator.getValidMoves(board, Position(4, 4)).size)
        
        // Test moves with invalid piece types (this shouldn't happen in practice)
        // The validator should handle missing validators gracefully
    }
}