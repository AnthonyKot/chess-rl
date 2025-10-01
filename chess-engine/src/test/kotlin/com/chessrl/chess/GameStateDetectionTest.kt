package com.chessrl.chess

import kotlin.test.*

class GameStateDetectionTest {
    private lateinit var board: ChessBoard
    private lateinit var detector: GameStateDetector
    
    @BeforeTest
    fun setup() {
        board = ChessBoard()
        detector = GameStateDetector()
    }
    
    @Test
    fun testCheckDetection() {
        // Set up a position where white king is in check
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(4, 7), Piece(PieceType.ROOK, PieceColor.BLACK)) // h5
        
        assertTrue(detector.isInCheck(board, PieceColor.WHITE))
        assertFalse(detector.isInCheck(board, PieceColor.BLACK))
    }
    
    @Test
    fun testCheckDetectionWithBlockedAttack() {
        // Set up a position where attack is blocked
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(4, 7), Piece(PieceType.ROOK, PieceColor.BLACK)) // h5
        board.setPieceAt(Position(4, 6), Piece(PieceType.PAWN, PieceColor.WHITE)) // g5 (blocking)
        
        assertFalse(detector.isInCheck(board, PieceColor.WHITE))
    }
    
    @Test
    fun testKnightCheck() {
        // Set up knight check
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(6, 5), Piece(PieceType.KNIGHT, PieceColor.BLACK)) // f7
        
        assertTrue(detector.isInCheck(board, PieceColor.WHITE))
    }
    
    @Test
    fun testPawnCheck() {
        // Set up pawn check
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(5, 3), Piece(PieceType.PAWN, PieceColor.BLACK)) // d6
        
        assertTrue(detector.isInCheck(board, PieceColor.WHITE))
    }
    
    @Test
    fun testBishopCheck() {
        // Set up bishop check
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(7, 7), Piece(PieceType.BISHOP, PieceColor.BLACK)) // h8
        
        assertTrue(detector.isInCheck(board, PieceColor.WHITE))
    }
    
    @Test
    fun testQueenCheck() {
        // Set up queen check (diagonal)
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(1, 1), Piece(PieceType.QUEEN, PieceColor.BLACK)) // b2
        
        assertTrue(detector.isInCheck(board, PieceColor.WHITE))
        
        // Set up queen check (horizontal)
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(4, 0), Piece(PieceType.QUEEN, PieceColor.BLACK)) // a5
        
        assertTrue(detector.isInCheck(board, PieceColor.WHITE))
    }
    
    @Test
    fun testSimpleCheckmate() {
        // Back rank mate
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(1, 3), Piece(PieceType.PAWN, PieceColor.WHITE)) // d2
        board.setPieceAt(Position(1, 4), Piece(PieceType.PAWN, PieceColor.WHITE)) // e2
        board.setPieceAt(Position(1, 5), Piece(PieceType.PAWN, PieceColor.WHITE)) // f2
        board.setPieceAt(Position(0, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a1
        
        assertTrue(detector.isCheckmate(board, PieceColor.WHITE))
        assertFalse(detector.isStalemate(board, PieceColor.WHITE))
    }
    
    @Test
    fun testSimpleStalemate() {
        // King in corner with no legal moves but not in check
        board.clearBoard()
        board.setPieceAt(Position(0, 0), Piece(PieceType.KING, PieceColor.WHITE)) // a1
        board.setPieceAt(Position(2, 1), Piece(PieceType.KING, PieceColor.BLACK)) // b3 (controls b1, b2, a2)
        board.setPieceAt(Position(1, 2), Piece(PieceType.QUEEN, PieceColor.BLACK)) // c2 (controls a2, b1)
        
        assertTrue(detector.isStalemate(board, PieceColor.WHITE))
        assertFalse(detector.isCheckmate(board, PieceColor.WHITE))
    }
    
    @Test
    fun testLegalMoveValidation() {
        // Set up position where king would be in check after move
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e5
        board.setPieceAt(Position(4, 3), Piece(PieceType.ROOK, PieceColor.WHITE)) // d5
        board.setPieceAt(Position(4, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a5
        
        // Moving the rook would expose the king to check
        val illegalMove = Move(Position(4, 3), Position(3, 3)) // d5-d4
        assertFalse(detector.isMoveLegal(board, illegalMove))
        
        // Moving the king is legal
        val legalMove = Move(Position(4, 4), Position(5, 4)) // e5-e6
        assertTrue(detector.isMoveLegal(board, legalMove))
    }
    
    @Test
    fun testCastlingValidation() {
        // Set up position for castling
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(0, 7), Piece(PieceType.ROOK, PieceColor.WHITE)) // h1
        board.setCastlingRights(whiteCanCastleKingside = true, whiteCanCastleQueenside = true, blackCanCastleKingside = false, blackCanCastleQueenside = false)

        val castlingMove = Move(Position(0, 4), Position(0, 6)) // e1-g1
        assertTrue(detector.isCastlingLegal(board, castlingMove))

        // Add piece blocking castling
        board.setPieceAt(Position(0, 5), Piece(PieceType.BISHOP, PieceColor.WHITE)) // f1
        assertFalse(detector.isCastlingLegal(board, castlingMove))
    }
    
    @Test
    fun testCastlingThroughCheck() {
        // Set up position where king would pass through check
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(0, 7), Piece(PieceType.ROOK, PieceColor.WHITE)) // h1
        board.setCastlingRights(whiteCanCastleKingside = true, whiteCanCastleQueenside = true, blackCanCastleKingside = false, blackCanCastleQueenside = false)
        board.setPieceAt(Position(7, 5), Piece(PieceType.ROOK, PieceColor.BLACK)) // f8

        val castlingMove = Move(Position(0, 4), Position(0, 6)) // e1-g1
        assertFalse(detector.isCastlingLegal(board, castlingMove)) // King passes through f1 which is attacked
    }
    
    @Test
    fun testCastlingWhileInCheck() {
        // Set up position where king is in check
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(0, 7), Piece(PieceType.ROOK, PieceColor.WHITE)) // h1
        board.setCastlingRights(whiteCanCastleKingside = true, whiteCanCastleQueenside = true, blackCanCastleKingside = false, blackCanCastleQueenside = false)
        board.setPieceAt(Position(7, 4), Piece(PieceType.ROOK, PieceColor.BLACK)) // e8

        val castlingMove = Move(Position(0, 4), Position(0, 6)) // e1-g1
        assertFalse(detector.isCastlingLegal(board, castlingMove)) // Can't castle while in check
    }
    
    @Test
    fun testInsufficientMaterialDraw() {
        // King vs King
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        assertTrue(detector.isDrawByInsufficientMaterial(board))
        
        // King and Bishop vs King
        board.setPieceAt(Position(3, 3), Piece(PieceType.BISHOP, PieceColor.WHITE))
        assertTrue(detector.isDrawByInsufficientMaterial(board))
        
        // King and Knight vs King
        board.setPieceAt(Position(3, 3), Piece(PieceType.KNIGHT, PieceColor.WHITE))
        assertTrue(detector.isDrawByInsufficientMaterial(board))
        
        // Add a pawn - no longer insufficient material
        board.setPieceAt(Position(2, 2), Piece(PieceType.PAWN, PieceColor.WHITE))
        assertFalse(detector.isDrawByInsufficientMaterial(board))
    }
    
    @Test
    fun testFiftyMoveRule() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        // Create a board with high halfmove clock
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        // Test with FEN that has high halfmove clock
        assertTrue(board.fromFEN("4k3/8/8/8/4K3/8/8/8 w - - 100 1"))
        assertTrue(detector.isDrawByFiftyMoveRule(board))
        
        // Test with lower halfmove clock
        assertTrue(board.fromFEN("4k3/8/8/8/4K3/8/8/8 w - - 99 1"))
        assertFalse(detector.isDrawByFiftyMoveRule(board))
    }
    
    @Test
    fun testGameStatusDetection() {
        // Test ongoing game
        assertEquals(GameStatus.ONGOING, detector.getGameStatus(board))
        
        // Test checkmate
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(1, 3), Piece(PieceType.PAWN, PieceColor.WHITE)) // d2
        board.setPieceAt(Position(1, 4), Piece(PieceType.PAWN, PieceColor.WHITE)) // e2
        board.setPieceAt(Position(1, 5), Piece(PieceType.PAWN, PieceColor.WHITE)) // f2
        board.setPieceAt(Position(0, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a1
        
        assertEquals(GameStatus.BLACK_WINS, detector.getGameStatus(board))
        
        // Test stalemate
        board.clearBoard()
        board.setPieceAt(Position(0, 0), Piece(PieceType.KING, PieceColor.WHITE)) // a1
        board.setPieceAt(Position(2, 1), Piece(PieceType.KING, PieceColor.BLACK)) // b3
        board.setPieceAt(Position(1, 2), Piece(PieceType.QUEEN, PieceColor.BLACK)) // c2
        
        assertEquals(GameStatus.DRAW_STALEMATE, detector.getGameStatus(board))
        
        // Test insufficient material
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(6, 6), Piece(PieceType.KING, PieceColor.BLACK))
        
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, detector.getGameStatus(board))
    }
    
    @Test
    fun testLegalMoveValidator() {
        val legalValidator = LegalMoveValidator()
        
        // Test normal legal move
        val legalMove = Move(Position(1, 4), Position(2, 4)) // e2-e3
        assertTrue(legalValidator.isLegalMove(board, legalMove))
        
        // Test illegal move (piece doesn't exist)
        val illegalMove = Move(Position(3, 3), Position(4, 4)) // d4-e5 (no piece on d4)
        assertFalse(legalValidator.isLegalMove(board, illegalMove))
        
        // Test getting legal moves for a piece
        val pawnMoves = legalValidator.getLegalMoves(board, Position(1, 4)) // e2
        assertTrue(pawnMoves.contains(Move(Position(1, 4), Position(2, 4)))) // e2-e3
        assertTrue(pawnMoves.contains(Move(Position(1, 4), Position(3, 4)))) // e2-e4
        assertEquals(2, pawnMoves.size)
    }
    
    @Test
    fun testSquareAttackDetection() {
        board.clearBoard()
        board.setPieceAt(Position(4, 4), Piece(PieceType.QUEEN, PieceColor.BLACK)) // e5
        
        // Queen attacks multiple squares
        assertTrue(detector.isSquareAttacked(board, Position(4, 0), PieceColor.BLACK)) // a5
        assertTrue(detector.isSquareAttacked(board, Position(0, 4), PieceColor.BLACK)) // e1
        assertTrue(detector.isSquareAttacked(board, Position(7, 7), PieceColor.BLACK)) // h8
        assertTrue(detector.isSquareAttacked(board, Position(1, 1), PieceColor.BLACK)) // b2
        
        // Queen doesn't attack squares blocked by pieces
        board.setPieceAt(Position(4, 2), Piece(PieceType.PAWN, PieceColor.WHITE)) // c5
        assertFalse(detector.isSquareAttacked(board, Position(4, 0), PieceColor.BLACK)) // a5 (blocked)
        assertTrue(detector.isSquareAttacked(board, Position(4, 2), PieceColor.BLACK)) // c5 (can capture)
    }
    
    @Test
    fun testComplexCheckmate() {
        // Smothered mate pattern identical to debug test
        require(board.fromFEN("6rk/5Npp/8/8/8/8/8/7K b - - 0 1"))

        assertTrue(detector.isInCheck(board, PieceColor.BLACK))
        assertTrue(detector.getAllLegalMoves(board, PieceColor.BLACK).isEmpty())
        assertTrue(detector.isCheckmate(board, PieceColor.BLACK))
    }
}
