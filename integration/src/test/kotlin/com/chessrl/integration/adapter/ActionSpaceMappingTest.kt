package com.chessrl.integration.adapter

import com.chessrl.chess.Position
import com.chessrl.chess.PieceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for ActionSpaceMapping
 * 
 * Tests the encoding/decoding system for the 4096 from-to action space
 * and validates promotion handling and action masking functionality.
 */
class ActionSpaceMappingTest {
    
    @Test
    fun testActionSpaceConstants() {
        assertEquals(64, ActionSpaceMapping.BOARD_SIZE)
        assertEquals(4096, ActionSpaceMapping.ACTION_SPACE_SIZE)
    }
    
    @Test
    fun testBasicMoveEncoding() {
        // Test e2e4 move (classic opening move)
        val e2 = Position(1, 4) // rank 1 (2nd rank), file 4 (e-file)
        val e4 = Position(3, 4) // rank 3 (4th rank), file 4 (e-file)
        val move = ChessMove(e2, e4)
        
        val actionIndex = ActionSpaceMapping.encodeMove(move)
        
        // Expected: (1 * 8 + 4) * 64 + (3 * 8 + 4) = 12 * 64 + 28 = 768 + 28 = 796
        assertEquals(796, actionIndex)
    }
    
    @Test
    fun testBasicMoveDecoding() {
        // Test decoding action index 796 back to e2e4
        val decodedMove = ActionSpaceMapping.decodeAction(796)
        
        assertEquals(Position(1, 4), decodedMove.from) // e2
        assertEquals(Position(3, 4), decodedMove.to)   // e4
        assertEquals("e2e4", decodedMove.algebraic)
    }
    
    @Test
    fun testCornerSquareEncoding() {
        // Test a1a8 (corner to corner)
        val a1 = Position(0, 0)
        val a8 = Position(7, 0)
        val move = ChessMove(a1, a8)
        
        val actionIndex = ActionSpaceMapping.encodeMove(move)
        
        // Expected: (0 * 8 + 0) * 64 + (7 * 8 + 0) = 0 * 64 + 56 = 56
        assertEquals(56, actionIndex)
        
        // Test h1h8 (other corner to corner)
        val h1 = Position(0, 7)
        val h8 = Position(7, 7)
        val move2 = ChessMove(h1, h8)
        
        val actionIndex2 = ActionSpaceMapping.encodeMove(move2)
        
        // Expected: (0 * 8 + 7) * 64 + (7 * 8 + 7) = 7 * 64 + 63 = 448 + 63 = 511
        assertEquals(511, actionIndex2)
    }
    
    @Test
    fun testMaxActionIndex() {
        // Test h8h8 (maximum possible action index)
        val h8 = Position(7, 7)
        val move = ChessMove(h8, h8)
        
        val actionIndex = ActionSpaceMapping.encodeMove(move)
        
        // Expected: (7 * 8 + 7) * 64 + (7 * 8 + 7) = 63 * 64 + 63 = 4032 + 63 = 4095
        assertEquals(4095, actionIndex)
    }
    
    @Test
    fun testPromotionMoveEncoding() {
        // Test pawn promotion e7e8q
        val e7 = Position(6, 4)
        val e8 = Position(7, 4)
        val promotionMove = ChessMove(e7, e8, PieceType.QUEEN)
        
        val actionIndex = ActionSpaceMapping.encodeMove(promotionMove)
        
        // Expected: (6 * 8 + 4) * 64 + (7 * 8 + 4) = 52 * 64 + 60 = 3328 + 60 = 3388
        assertEquals(3388, actionIndex)
        
        // Verify that promotion info is preserved in the move but not in action index
        assertEquals(PieceType.QUEEN, promotionMove.promotion)
        assertEquals("e7e8q", promotionMove.algebraic)
    }
    
    @Test
    fun testRoundTripEncoding() {
        // Test that encoding then decoding preserves from-to information
        val originalMove = ChessMove(Position(2, 3), Position(4, 5)) // d3f5
        
        val actionIndex = ActionSpaceMapping.encodeMove(originalMove)
        val decodedMove = ActionSpaceMapping.decodeAction(actionIndex)
        
        assertEquals(originalMove.from, decodedMove.from)
        assertEquals(originalMove.to, decodedMove.to)
        // Note: promotion info is not preserved in basic encoding/decoding
    }
    
    @Test
    fun testInvalidActionIndexDecoding() {
        // Test negative action index
        assertFailsWith<IllegalArgumentException> {
            ActionSpaceMapping.decodeAction(-1)
        }
        
        // Test action index too large
        assertFailsWith<IllegalArgumentException> {
            ActionSpaceMapping.decodeAction(4096)
        }
        
        // Test way too large action index
        assertFailsWith<IllegalArgumentException> {
            ActionSpaceMapping.decodeAction(10000)
        }
    }
    
    @Test
    fun testActionMaskCreation() {
        // Create some sample legal moves
        val legalMoves = listOf(
            ChessMove(Position(1, 4), Position(3, 4)), // e2e4
            ChessMove(Position(1, 3), Position(3, 3)), // d2d4
            ChessMove(Position(0, 1), Position(2, 2))  // b1c3
        )
        
        val mask = ActionSpaceMapping.createActionMask(legalMoves)
        
        // Verify mask size
        assertEquals(4096, mask.size)
        
        // Verify that legal moves are marked as true
        for (move in legalMoves) {
            val actionIndex = ActionSpaceMapping.encodeMove(move)
            assertTrue(mask[actionIndex], "Legal move ${move.algebraic} should be marked as true in mask")
        }
        
        // Verify that most other actions are false (spot check a few)
        val illegalActionIndex = ActionSpaceMapping.encodeMove(ChessMove(Position(0, 0), Position(7, 7))) // a1h8
        if (illegalActionIndex !in legalMoves.map { ActionSpaceMapping.encodeMove(it) }) {
            assertFalse(mask[illegalActionIndex], "Illegal move should be marked as false in mask")
        }
    }
    
    @Test
    fun testGetLegalActionIndices() {
        val legalMoves = listOf(
            ChessMove(Position(1, 4), Position(3, 4)), // e2e4 -> 796
            ChessMove(Position(1, 3), Position(3, 3)), // d2d4 -> 731
            ChessMove(Position(0, 1), Position(2, 2))  // b1c3 -> 82
        )
        
        val actionIndices = ActionSpaceMapping.getLegalActionIndices(legalMoves)
        
        assertEquals(3, actionIndices.size)
        assertTrue(796 in actionIndices) // e2e4
        assertTrue(731 in actionIndices) // d2d4  
        assertTrue(82 in actionIndices) // b1c3
    }
    
    @Test
    fun testFindMatchingMoveExactMatch() {
        val decodedMove = ChessMove(Position(1, 4), Position(3, 4)) // e2e4
        val legalMoves = listOf(
            ChessMove(Position(1, 4), Position(3, 4)), // e2e4 - exact match
            ChessMove(Position(1, 3), Position(3, 3))  // d2d4
        )
        
        val matchingMove = ActionSpaceMapping.findMatchingMove(decodedMove, legalMoves)
        
        assertNotNull(matchingMove)
        assertEquals(decodedMove.from, matchingMove.from)
        assertEquals(decodedMove.to, matchingMove.to)
    }
    
    @Test
    fun testFindMatchingMoveNoMatch() {
        val decodedMove = ChessMove(Position(1, 4), Position(3, 4)) // e2e4
        val legalMoves = listOf(
            ChessMove(Position(1, 3), Position(3, 3)), // d2d4
            ChessMove(Position(0, 1), Position(2, 2))  // b1c3
        )
        
        val matchingMove = ActionSpaceMapping.findMatchingMove(decodedMove, legalMoves)
        
        assertNull(matchingMove)
    }
    
    @Test
    fun testFindMatchingMovePromotionDefault() {
        // Test that queen promotion is preferred when multiple promotions are available
        val decodedMove = ChessMove(Position(6, 4), Position(7, 4)) // e7e8 (no promotion specified)
        val legalMoves = listOf(
            ChessMove(Position(6, 4), Position(7, 4), PieceType.ROOK),   // e7e8r
            ChessMove(Position(6, 4), Position(7, 4), PieceType.QUEEN),  // e7e8q
            ChessMove(Position(6, 4), Position(7, 4), PieceType.BISHOP), // e7e8b
            ChessMove(Position(6, 4), Position(7, 4), PieceType.KNIGHT)  // e7e8n
        )
        
        val matchingMove = ActionSpaceMapping.findMatchingMove(decodedMove, legalMoves)
        
        assertNotNull(matchingMove)
        assertEquals(PieceType.QUEEN, matchingMove.promotion)
        assertEquals("e7e8q", matchingMove.algebraic)
    }
    
    @Test
    fun testFindMatchingMoveSingleCandidate() {
        val decodedMove = ChessMove(Position(1, 4), Position(3, 4)) // e2e4
        val legalMoves = listOf(
            ChessMove(Position(1, 4), Position(3, 4)) // e2e4 - single candidate
        )
        
        val matchingMove = ActionSpaceMapping.findMatchingMove(decodedMove, legalMoves)
        
        assertNotNull(matchingMove)
        assertEquals(decodedMove.from, matchingMove.from)
        assertEquals(decodedMove.to, matchingMove.to)
    }
    
    @Test
    fun testIsValidActionIndex() {
        assertTrue(ActionSpaceMapping.isValidActionIndex(0))
        assertTrue(ActionSpaceMapping.isValidActionIndex(796)) // e2e4
        assertTrue(ActionSpaceMapping.isValidActionIndex(4095)) // max index
        
        assertFalse(ActionSpaceMapping.isValidActionIndex(-1))
        assertFalse(ActionSpaceMapping.isValidActionIndex(4096))
        assertFalse(ActionSpaceMapping.isValidActionIndex(10000))
    }
    
    @Test
    fun testDescribeAction() {
        assertEquals("e2 to e4", ActionSpaceMapping.describeAction(796))
        assertEquals("a1 to a8", ActionSpaceMapping.describeAction(56))
        assertEquals("h8 to h8", ActionSpaceMapping.describeAction(4095))
        
        assertTrue(ActionSpaceMapping.describeAction(-1).contains("Invalid"))
        assertTrue(ActionSpaceMapping.describeAction(4096).contains("Invalid"))
    }
    
    @Test
    fun testAllSquareCombinations() {
        // Test that we can encode/decode all possible square combinations
        var actionIndex = 0
        
        for (fromRank in 0..7) {
            for (fromFile in 0..7) {
                for (toRank in 0..7) {
                    for (toFile in 0..7) {
                        val move = ChessMove(Position(fromRank, fromFile), Position(toRank, toFile))
                        val encoded = ActionSpaceMapping.encodeMove(move)
                        val decoded = ActionSpaceMapping.decodeAction(encoded)
                        
                        assertEquals(actionIndex, encoded)
                        assertEquals(move.from, decoded.from)
                        assertEquals(move.to, decoded.to)
                        
                        actionIndex++
                    }
                }
            }
        }
        
        assertEquals(4096, actionIndex)
    }
}