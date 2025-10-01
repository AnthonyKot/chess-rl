package com.chessrl.chess

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Comprehensive tests for chess engine data structures
 */
class ChessBoardTest {
    
    // Position Tests
    @Test
    fun testPositionCreation() {
        val position = Position(4, 4)
        assertEquals(4, position.rank)
        assertEquals(4, position.file)
    }
    
    @Test
    fun testPositionAlgebraicConversion() {
        val e4 = Position(3, 4)
        assertEquals("e4", e4.toAlgebraic())
        
        val a1 = Position(0, 0)
        assertEquals("a1", a1.toAlgebraic())
        
        val h8 = Position(7, 7)
        assertEquals("h8", h8.toAlgebraic())
    }
    
    @Test
    fun testPositionFromAlgebraic() {
        val e4 = Position.fromAlgebraic("e4")
        assertNotNull(e4)
        assertEquals(3, e4.rank)
        assertEquals(4, e4.file)
        
        val a1 = Position.fromAlgebraic("a1")
        assertNotNull(a1)
        assertEquals(0, a1.rank)
        assertEquals(0, a1.file)
        
        val h8 = Position.fromAlgebraic("h8")
        assertNotNull(h8)
        assertEquals(7, h8.rank)
        assertEquals(7, h8.file)
        
        // Invalid positions
        assertNull(Position.fromAlgebraic("i9"))
        assertNull(Position.fromAlgebraic("a0"))
        assertNull(Position.fromAlgebraic("z1"))
        assertNull(Position.fromAlgebraic("e"))
    }
    
    @Test
    fun testPositionValidation() {
        assertTrue(Position(0, 0).isValid())
        assertTrue(Position(7, 7).isValid())
        assertTrue(Position(3, 4).isValid())
        
        assertFalse(Position(-1, 0).isValid())
        assertFalse(Position(0, -1).isValid())
        assertFalse(Position(8, 0).isValid())
        assertFalse(Position(0, 8).isValid())
    }
    
    // Piece Tests
    @Test
    fun testPieceCreation() {
        val whiteKing = Piece(PieceType.KING, PieceColor.WHITE)
        assertEquals(PieceType.KING, whiteKing.type)
        assertEquals(PieceColor.WHITE, whiteKing.color)
        
        val blackPawn = Piece(PieceType.PAWN, PieceColor.BLACK)
        assertEquals(PieceType.PAWN, blackPawn.type)
        assertEquals(PieceColor.BLACK, blackPawn.color)
    }
    
    @Test
    fun testAllPieceTypes() {
        val types = listOf(
            PieceType.PAWN, PieceType.ROOK, PieceType.KNIGHT,
            PieceType.BISHOP, PieceType.QUEEN, PieceType.KING
        )
        
        for (type in types) {
            val whitePiece = Piece(type, PieceColor.WHITE)
            val blackPiece = Piece(type, PieceColor.BLACK)
            
            assertEquals(type, whitePiece.type)
            assertEquals(type, blackPiece.type)
            assertEquals(PieceColor.WHITE, whitePiece.color)
            assertEquals(PieceColor.BLACK, blackPiece.color)
        }
    }
    
    // Move Tests
    @Test
    fun testMoveCreation() {
        val from = Position(1, 4)
        val to = Position(3, 4)
        val move = Move(from, to)
        
        assertEquals(from, move.from)
        assertEquals(to, move.to)
        assertNull(move.promotion)
        assertFalse(move.isPromotion())
    }
    
    @Test
    fun testMoveWithPromotion() {
        val from = Position(6, 4)
        val to = Position(7, 4)
        val move = Move(from, to, PieceType.QUEEN)
        
        assertEquals(from, move.from)
        assertEquals(to, move.to)
        assertEquals(PieceType.QUEEN, move.promotion)
        assertTrue(move.isPromotion())
    }
    
    @Test
    fun testMoveAlgebraicConversion() {
        val move = Move(Position(1, 4), Position(3, 4))
        assertEquals("e2e4", move.toAlgebraic())
        
        val promotionMove = Move(Position(6, 0), Position(7, 0), PieceType.QUEEN)
        assertEquals("a7a8q", promotionMove.toAlgebraic())
        
        val knightPromotion = Move(Position(6, 7), Position(7, 7), PieceType.KNIGHT)
        assertEquals("h7h8n", knightPromotion.toAlgebraic())
    }
    
    @Test
    fun testMoveFromAlgebraic() {
        val move = Move.fromAlgebraic("e2e4")
        assertNotNull(move)
        assertEquals(Position(1, 4), move.from)
        assertEquals(Position(3, 4), move.to)
        assertNull(move.promotion)
        
        val promotionMove = Move.fromAlgebraic("a7a8q")
        assertNotNull(promotionMove)
        assertEquals(Position(6, 0), promotionMove.from)
        assertEquals(Position(7, 0), promotionMove.to)
        assertEquals(PieceType.QUEEN, promotionMove.promotion)
        
        // Invalid moves
        assertNull(Move.fromAlgebraic("e2"))
        assertNull(Move.fromAlgebraic("e2e9"))
        assertNull(Move.fromAlgebraic("z1a1"))
    }
    
    // ChessBoard Tests
    @Test
    fun testChessBoardCreation() {
        val board = ChessBoard()
        assertNotNull(board)
    }
    
    @Test
    fun testEmptyBoard() {
        val board = ChessBoard()
        board.clearBoard()
        assertTrue(board.isEmpty())
        
        // Check all squares are empty
        for (rank in 0..7) {
            for (file in 0..7) {
                assertNull(board.getPieceAt(Position(rank, file)))
            }
        }
    }
    
    @Test
    fun testStandardBoardInitialization() {
        val board = ChessBoard()
        
        // Test white pieces on first rank
        assertEquals(Piece(PieceType.ROOK, PieceColor.WHITE), board.getPieceAt(Position(0, 0)))
        assertEquals(Piece(PieceType.KNIGHT, PieceColor.WHITE), board.getPieceAt(Position(0, 1)))
        assertEquals(Piece(PieceType.BISHOP, PieceColor.WHITE), board.getPieceAt(Position(0, 2)))
        assertEquals(Piece(PieceType.QUEEN, PieceColor.WHITE), board.getPieceAt(Position(0, 3)))
        assertEquals(Piece(PieceType.KING, PieceColor.WHITE), board.getPieceAt(Position(0, 4)))
        assertEquals(Piece(PieceType.BISHOP, PieceColor.WHITE), board.getPieceAt(Position(0, 5)))
        assertEquals(Piece(PieceType.KNIGHT, PieceColor.WHITE), board.getPieceAt(Position(0, 6)))
        assertEquals(Piece(PieceType.ROOK, PieceColor.WHITE), board.getPieceAt(Position(0, 7)))
        
        // Test white pawns on second rank
        for (file in 0..7) {
            assertEquals(Piece(PieceType.PAWN, PieceColor.WHITE), board.getPieceAt(Position(1, file)))
        }
        
        // Test empty squares in middle
        for (rank in 2..5) {
            for (file in 0..7) {
                assertNull(board.getPieceAt(Position(rank, file)))
            }
        }
        
        // Test black pawns on seventh rank
        for (file in 0..7) {
            assertEquals(Piece(PieceType.PAWN, PieceColor.BLACK), board.getPieceAt(Position(6, file)))
        }
        
        // Test black pieces on eighth rank
        assertEquals(Piece(PieceType.ROOK, PieceColor.BLACK), board.getPieceAt(Position(7, 0)))
        assertEquals(Piece(PieceType.KNIGHT, PieceColor.BLACK), board.getPieceAt(Position(7, 1)))
        assertEquals(Piece(PieceType.BISHOP, PieceColor.BLACK), board.getPieceAt(Position(7, 2)))
        assertEquals(Piece(PieceType.QUEEN, PieceColor.BLACK), board.getPieceAt(Position(7, 3)))
        assertEquals(Piece(PieceType.KING, PieceColor.BLACK), board.getPieceAt(Position(7, 4)))
        assertEquals(Piece(PieceType.BISHOP, PieceColor.BLACK), board.getPieceAt(Position(7, 5)))
        assertEquals(Piece(PieceType.KNIGHT, PieceColor.BLACK), board.getPieceAt(Position(7, 6)))
        assertEquals(Piece(PieceType.ROOK, PieceColor.BLACK), board.getPieceAt(Position(7, 7)))
    }
    
    @Test
    fun testSetAndGetPiece() {
        val board = ChessBoard()
        board.clearBoard()
        
        val piece = Piece(PieceType.QUEEN, PieceColor.WHITE)
        val position = Position(4, 4)
        
        board.setPieceAt(position, piece)
        assertEquals(piece, board.getPieceAt(position))
        
        board.setPieceAt(position, null)
        assertNull(board.getPieceAt(position))
    }
    
    @Test
    fun testInvalidPositions() {
        val board = ChessBoard()
        
        assertFalse(board.isValidPosition(Position(-1, 0)))
        assertFalse(board.isValidPosition(Position(0, -1)))
        assertFalse(board.isValidPosition(Position(8, 0)))
        assertFalse(board.isValidPosition(Position(0, 8)))
        
        assertTrue(board.isValidPosition(Position(0, 0)))
        assertTrue(board.isValidPosition(Position(7, 7)))
        assertTrue(board.isValidPosition(Position(3, 4)))
        
        // Getting piece at invalid position should return null
        assertNull(board.getPieceAt(Position(-1, 0)))
        assertNull(board.getPieceAt(Position(8, 0)))
    }
    
    @Test
    fun testGetPiecesOfColor() {
        val board = ChessBoard()
        
        val whitePieces = board.getPiecesOfColor(PieceColor.WHITE)
        val blackPieces = board.getPiecesOfColor(PieceColor.BLACK)
        
        // Standard position has 16 pieces per color
        assertEquals(16, whitePieces.size)
        assertEquals(16, blackPieces.size)
        
        // All white pieces should be on ranks 0 and 1
        for ((position, piece) in whitePieces) {
            assertEquals(PieceColor.WHITE, piece.color)
            assertTrue(position.rank in 0..1)
        }
        
        // All black pieces should be on ranks 6 and 7
        for ((position, piece) in blackPieces) {
            assertEquals(PieceColor.BLACK, piece.color)
            assertTrue(position.rank in 6..7)
        }
    }
    
    @Test
    fun testFindKing() {
        val board = ChessBoard()
        
        val whiteKing = board.findKing(PieceColor.WHITE)
        val blackKing = board.findKing(PieceColor.BLACK)
        
        assertNotNull(whiteKing)
        assertNotNull(blackKing)
        
        assertEquals(Position(0, 4), whiteKing)
        assertEquals(Position(7, 4), blackKing)
        
        // Test with empty board
        board.clearBoard()
        assertNull(board.findKing(PieceColor.WHITE))
        assertNull(board.findKing(PieceColor.BLACK))
    }
    
    @Test
    fun testBoardCopy() {
        val original = ChessBoard()
        val copy = original.deepCopy()
        
        // Should be equal but not the same instance
        assertEquals(original, copy)
        assertTrue(original !== copy)
        
        // Modifying copy shouldn't affect original
        copy.setPieceAt(Position(4, 4), Piece(PieceType.QUEEN, PieceColor.WHITE))
        assertFalse(original == copy)
        assertNull(original.getPieceAt(Position(4, 4)))
        assertNotNull(copy.getPieceAt(Position(4, 4)))
    }
    
    @Test
    fun testBoardEquality() {
        val board1 = ChessBoard()
        val board2 = ChessBoard()
        
        assertEquals(board1, board2)
        assertEquals(board1.hashCode(), board2.hashCode())
        
        // Modify one board
        board1.setPieceAt(Position(4, 4), Piece(PieceType.QUEEN, PieceColor.WHITE))
        assertFalse(board1 == board2)
        
        // Make the same modification to the other
        board2.setPieceAt(Position(4, 4), Piece(PieceType.QUEEN, PieceColor.WHITE))
        assertEquals(board1, board2)
    }
    
    // FEN Tests
    @Test
    fun testPieceFENConversion() {
        assertEquals('K', Piece(PieceType.KING, PieceColor.WHITE).toFENChar())
        assertEquals('k', Piece(PieceType.KING, PieceColor.BLACK).toFENChar())
        assertEquals('Q', Piece(PieceType.QUEEN, PieceColor.WHITE).toFENChar())
        assertEquals('q', Piece(PieceType.QUEEN, PieceColor.BLACK).toFENChar())
        assertEquals('R', Piece(PieceType.ROOK, PieceColor.WHITE).toFENChar())
        assertEquals('r', Piece(PieceType.ROOK, PieceColor.BLACK).toFENChar())
        assertEquals('B', Piece(PieceType.BISHOP, PieceColor.WHITE).toFENChar())
        assertEquals('b', Piece(PieceType.BISHOP, PieceColor.BLACK).toFENChar())
        assertEquals('N', Piece(PieceType.KNIGHT, PieceColor.WHITE).toFENChar())
        assertEquals('n', Piece(PieceType.KNIGHT, PieceColor.BLACK).toFENChar())
        assertEquals('P', Piece(PieceType.PAWN, PieceColor.WHITE).toFENChar())
        assertEquals('p', Piece(PieceType.PAWN, PieceColor.BLACK).toFENChar())
    }
    
    @Test
    fun testPieceFromFEN() {
        assertEquals(Piece(PieceType.KING, PieceColor.WHITE), Piece.fromFENChar('K'))
        assertEquals(Piece(PieceType.KING, PieceColor.BLACK), Piece.fromFENChar('k'))
        assertEquals(Piece(PieceType.QUEEN, PieceColor.WHITE), Piece.fromFENChar('Q'))
        assertEquals(Piece(PieceType.QUEEN, PieceColor.BLACK), Piece.fromFENChar('q'))
        assertEquals(Piece(PieceType.ROOK, PieceColor.WHITE), Piece.fromFENChar('R'))
        assertEquals(Piece(PieceType.ROOK, PieceColor.BLACK), Piece.fromFENChar('r'))
        assertEquals(Piece(PieceType.BISHOP, PieceColor.WHITE), Piece.fromFENChar('B'))
        assertEquals(Piece(PieceType.BISHOP, PieceColor.BLACK), Piece.fromFENChar('b'))
        assertEquals(Piece(PieceType.KNIGHT, PieceColor.WHITE), Piece.fromFENChar('N'))
        assertEquals(Piece(PieceType.KNIGHT, PieceColor.BLACK), Piece.fromFENChar('n'))
        assertEquals(Piece(PieceType.PAWN, PieceColor.WHITE), Piece.fromFENChar('P'))
        assertEquals(Piece(PieceType.PAWN, PieceColor.BLACK), Piece.fromFENChar('p'))
        
        assertNull(Piece.fromFENChar('x'))
        assertNull(Piece.fromFENChar('1'))
    }
    
    @Test
    fun testStartingPositionFEN() {
        val board = ChessBoard()
        val expectedFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        assertEquals(expectedFEN, board.toFEN())
    }
    
    @Test
    fun testFENRoundTrip() {
        val originalBoard = ChessBoard()
        val fen = originalBoard.toFEN()
        
        val newBoard = ChessBoard()
        newBoard.clearBoard()
        assertTrue(newBoard.fromFEN(fen))
        
        assertEquals(originalBoard, newBoard)
        assertEquals(fen, newBoard.toFEN())
    }
    
    @Test
    fun testCustomPositionFEN() {
        val board = ChessBoard()
        board.clearBoard()
        
        // Set up a simple position: white king on e1, black king on e8
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE))
        board.setPieceAt(Position(7, 4), Piece(PieceType.KING, PieceColor.BLACK))
        
        val fen = board.toFEN()
        assertTrue(fen.startsWith("4k3/8/8/8/8/8/8/4K3"))
        
        // Test loading this position
        val newBoard = ChessBoard()
        newBoard.clearBoard()
        assertTrue(newBoard.fromFEN(fen))
        
        assertEquals(board, newBoard)
    }
    
    @Test
    fun testFENWithNumbers() {
        val board = ChessBoard()
        board.clearBoard()
        
        // Empty board should be all 8s
        val emptyFEN = board.toFEN()
        assertTrue(emptyFEN.startsWith("8/8/8/8/8/8/8/8"))
        
        // Test loading empty board
        assertTrue(board.fromFEN("8/8/8/8/8/8/8/8 w - - 0 1"))
        assertTrue(board.isEmpty())
    }
    
    @Test
    fun testInvalidFEN() {
        val board = ChessBoard()
        
        // Invalid FEN strings should return false
        assertFalse(board.fromFEN(""))
        assertFalse(board.fromFEN("invalid"))
        assertFalse(board.fromFEN("8/8/8/8/8/8/8")) // Too few ranks
        assertFalse(board.fromFEN("9/8/8/8/8/8/8/8 w - - 0 1")) // Invalid number
        assertFalse(board.fromFEN("8/8/8/8/8/8/8/8x w - - 0 1")) // Invalid character
    }
    
    // Game State Tests
    @Test
    fun testGameStateInitialization() {
        val board = ChessBoard()
        val gameState = board.getGameState()
        
        assertEquals(PieceColor.WHITE, gameState.activeColor)
        assertTrue(gameState.whiteCanCastleKingside)
        assertTrue(gameState.whiteCanCastleQueenside)
        assertTrue(gameState.blackCanCastleKingside)
        assertTrue(gameState.blackCanCastleQueenside)
        assertNull(gameState.enPassantTarget)
        assertEquals(0, gameState.halfmoveClock)
        assertEquals(1, gameState.fullmoveNumber)
    }
    
    @Test
    fun testActiveColorSwitching() {
        val board = ChessBoard()
        
        assertEquals(PieceColor.WHITE, board.getActiveColor())
        
        board.switchActiveColor()
        assertEquals(PieceColor.BLACK, board.getActiveColor())
        assertEquals(1, board.getGameState().fullmoveNumber)
        
        board.switchActiveColor()
        assertEquals(PieceColor.WHITE, board.getActiveColor())
        assertEquals(2, board.getGameState().fullmoveNumber)
    }
    
    @Test
    fun testCastlingRights() {
        val gameState = GameState()
        
        assertEquals("KQkq", gameState.getCastlingRights())
        
        gameState.whiteCanCastleKingside = false
        assertEquals("Qkq", gameState.getCastlingRights())
        
        gameState.whiteCanCastleQueenside = false
        gameState.blackCanCastleKingside = false
        gameState.blackCanCastleQueenside = false
        assertEquals("-", gameState.getCastlingRights())
        
        // Test setting from string
        gameState.setCastlingRights("Kq")
        assertTrue(gameState.whiteCanCastleKingside)
        assertFalse(gameState.whiteCanCastleQueenside)
        assertFalse(gameState.blackCanCastleKingside)
        assertTrue(gameState.blackCanCastleQueenside)
    }
    
    @Test
    fun testComplexFENParsing() {
        val board = ChessBoard()
        val complexFEN = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        
        assertTrue(board.fromFEN(complexFEN))
        
        assertEquals(PieceColor.BLACK, board.getActiveColor())
        val gameState = board.getGameState()
        assertTrue(gameState.whiteCanCastleKingside)
        assertTrue(gameState.whiteCanCastleQueenside)
        assertTrue(gameState.blackCanCastleKingside)
        assertTrue(gameState.blackCanCastleQueenside)
        assertEquals(Position(2, 4), gameState.enPassantTarget)
        assertEquals(0, gameState.halfmoveClock)
        assertEquals(1, gameState.fullmoveNumber)
        
        // Check that the pawn moved
        assertEquals(Piece(PieceType.PAWN, PieceColor.WHITE), board.getPieceAt(Position(3, 4)))
        assertNull(board.getPieceAt(Position(1, 4)))
    }
    
    // Move Execution Tests
    @Test
    fun testBasicMoveExecution() {
        val board = ChessBoard()
        
        // Move white pawn from e2 to e4
        val move = Move(Position(1, 4), Position(3, 4))
        val result = board.makeMove(move)
        
        assertEquals(MoveResult.SUCCESS, result)
        assertNull(board.getPieceAt(Position(1, 4)))
        assertEquals(Piece(PieceType.PAWN, PieceColor.WHITE), board.getPieceAt(Position(3, 4)))
    }
    
    @Test
    fun testPromotionMove() {
        val board = ChessBoard()
        board.clearBoard()
        
        // Place white pawn on 7th rank
        board.setPieceAt(Position(6, 0), Piece(PieceType.PAWN, PieceColor.WHITE))
        
        // Promote to queen
        val move = Move(Position(6, 0), Position(7, 0), PieceType.QUEEN)
        val result = board.makeMove(move)
        
        assertEquals(MoveResult.SUCCESS, result)
        assertNull(board.getPieceAt(Position(6, 0)))
        assertEquals(Piece(PieceType.QUEEN, PieceColor.WHITE), board.getPieceAt(Position(7, 0)))
    }
    
    @Test
    fun testInvalidMoveExecution() {
        val board = ChessBoard()
        
        // Try to move from empty square
        val invalidMove = Move(Position(3, 3), Position(4, 4))
        val result = board.makeMove(invalidMove)
        
        assertEquals(MoveResult.INVALID_MOVE, result)
        
        // Try to move to invalid position
        val invalidMove2 = Move(Position(1, 4), Position(8, 4))
        val result2 = board.makeMove(invalidMove2)
        
        assertEquals(MoveResult.INVALID_MOVE, result2)
    }
    
    // Visualization Tests
    @Test
    fun testASCIIRendering() {
        val board = ChessBoard()
        val ascii = board.toASCII()
        
        // Should contain board structure
        assertTrue(ascii.contains("+---+"))
        assertTrue(ascii.contains("| a | b | c |"))
        assertTrue(ascii.contains("8 |"))
        assertTrue(ascii.contains("1 |"))
        
        // Should contain pieces in starting position
        assertTrue(ascii.contains("r")) // Black rook
        assertTrue(ascii.contains("R")) // White rook
        assertTrue(ascii.contains("k")) // Black king
        assertTrue(ascii.contains("K")) // White king
        
        // Should contain game state info
        assertTrue(ascii.contains("Active Color: White"))
        assertTrue(ascii.contains("Castling Rights: KQkq"))
        assertTrue(ascii.contains("FEN:"))
    }
    
    @Test
    fun testASCIIWithHighlights() {
        val board = ChessBoard()
        val highlights = setOf(Position(0, 4), Position(7, 4)) // Both kings
        val ascii = board.toASCIIWithHighlights(highlights)
        
        // Should contain highlighted squares
        assertTrue(ascii.contains("[K]") || ascii.contains("[k]"))
    }
    
    @Test
    fun testPositionDescription() {
        val board = ChessBoard()
        val description = board.getPositionDescription()
        
        assertTrue(description.contains("Chess Position Analysis"))
        assertTrue(description.contains("White: 16 pieces"))
        assertTrue(description.contains("Black: 16 pieces"))
        assertTrue(description.contains("White King: e1"))
        assertTrue(description.contains("Black King: e8"))
        assertTrue(description.contains("Active Color: White"))
    }
    
    // ChessGame Tests
    @Test
    fun testChessGameCreation() {
        val game = ChessGame()
        val board = game.getCurrentBoard()
        
        // Should start with standard position
        assertEquals(ChessBoard(), board)
        assertTrue(game.getMoveHistory().isEmpty())
    }
    
    @Test
    fun testChessGameMoveHistory() {
        val game = ChessGame()
        
        // Make a few moves
        val move1 = Move(Position(1, 4), Position(3, 4)) // e2-e4
        val move2 = Move(Position(6, 4), Position(4, 4)) // e7-e5
        
        assertEquals(MoveResult.SUCCESS, game.makeMove(move1))
        assertEquals(MoveResult.SUCCESS, game.makeMove(move2))
        
        val history = game.getMoveHistory()
        assertEquals(2, history.size)
        
        val firstMove = history[0]
        assertEquals(move1, firstMove.move)
        assertEquals(PieceType.PAWN, firstMove.piece.type)
        assertEquals(PieceColor.WHITE, firstMove.piece.color)
        assertTrue(firstMove.isWhiteMove)
        assertEquals(1, firstMove.moveNumber)
        
        val secondMove = history[1]
        assertEquals(move2, secondMove.move)
        assertEquals(PieceType.PAWN, secondMove.piece.type)
        assertEquals(PieceColor.BLACK, secondMove.piece.color)
        assertFalse(secondMove.isWhiteMove)
        assertEquals(1, secondMove.moveNumber)
    }
    
    @Test
    fun testMoveHistoryPGN() {
        val game = ChessGame()
        
        val move1 = Move(Position(1, 4), Position(3, 4)) // e2-e4
        val move2 = Move(Position(6, 4), Position(4, 4)) // e7-e5
        
        game.makeMove(move1)
        game.makeMove(move2)
        
        val pgn = game.getMoveHistoryPGN()
        assertTrue(pgn.contains("1."))
        assertTrue(pgn.contains("e2e4"))
        assertTrue(pgn.contains("e7e5"))
    }
    
    @Test
    fun testMoveHistoryDetailed() {
        val game = ChessGame()
        
        val move = Move(Position(1, 4), Position(3, 4)) // e2-e4
        game.makeMove(move)
        
        val detailed = game.getMoveHistoryDetailed()
        assertTrue(detailed.contains("Move History"))
        assertTrue(detailed.contains("White Pawn from e2 to e4"))
        assertTrue(detailed.contains("PGN:"))
    }
    
    @Test
    fun testGameReset() {
        val game = ChessGame()
        
        // Make a move
        game.makeMove(Move(Position(1, 4), Position(3, 4)))
        assertFalse(game.getMoveHistory().isEmpty())
        
        // Reset
        game.reset()
        assertTrue(game.getMoveHistory().isEmpty())
        assertEquals(ChessBoard(), game.getCurrentBoard())
    }
    
    @Test
    fun testGameLoadFromFEN() {
        val game = ChessGame()
        val customFEN = "8/8/8/8/8/8/8/4K2k w - - 0 1"
        
        assertTrue(game.loadFromFEN(customFEN))
        
        val board = game.getCurrentBoard()
        assertEquals(Piece(PieceType.KING, PieceColor.WHITE), board.getPieceAt(Position(0, 4)))
        assertEquals(Piece(PieceType.KING, PieceColor.BLACK), board.getPieceAt(Position(0, 7)))
        
        // Invalid FEN should fail
        assertFalse(game.loadFromFEN("invalid"))
    }
    
    // ChessValidator Tests
    @Test
    fun testFENValidation() {
        // Valid FEN
        val validResult = ChessValidator.validateFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertTrue(validResult.isValid)
        assertTrue(validResult.issues.isEmpty())
        
        // Invalid FEN - missing king
        val invalidResult = ChessValidator.validateFEN("rnbq1bnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertFalse(invalidResult.isValid)
        assertTrue(invalidResult.issues.any { it.contains("king") })
        
        // Completely invalid FEN
        val badResult = ChessValidator.validateFEN("invalid")
        assertFalse(badResult.isValid)
        assertTrue(badResult.issues.isNotEmpty())
    }
    
    @Test
    fun testValidationReport() {
        val result = ChessValidator.validateFEN("invalid")
        val report = result.getReport()
        
        assertTrue(report.contains("FEN Validation Report"))
        assertTrue(report.contains("FEN: invalid"))
        assertTrue(report.contains("Valid: false"))
        assertTrue(report.contains("Issues:"))
    }
    
    @Test
    fun testPositionComparison() {
        val fen1 = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val fen2 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        
        val comparison = ChessValidator.comparePositions(fen1, fen2)
        
        assertTrue(comparison.contains("Position Comparison"))
        assertTrue(comparison.contains("Positions differ"))
        assertTrue(comparison.contains("e2:")) // Should show the pawn move
        assertTrue(comparison.contains("e4:"))
    }
    
    @Test
    fun testMoveHistoryEntry() {
        val move = Move(Position(1, 4), Position(3, 4))
        val piece = Piece(PieceType.PAWN, PieceColor.WHITE)
        
        val entry = MoveHistoryEntry(
            move = move,
            piece = piece,
            capturedPiece = null,
            boardStateBefore = "before",
            boardStateAfter = "after",
            moveNumber = 1,
            isWhiteMove = true
        )
        
        assertEquals("e2e4", entry.toAlgebraicNotation())
        
        val description = entry.getDescription()
        assertTrue(description.contains("1. White Pawn from e2 to e4"))
    }
    
    @Test
    fun testMoveHistoryWithCapture() {
        val move = Move(Position(3, 4), Position(4, 3))
        val piece = Piece(PieceType.PAWN, PieceColor.WHITE)
        val capturedPiece = Piece(PieceType.PAWN, PieceColor.BLACK)
        
        val entry = MoveHistoryEntry(
            move = move,
            piece = piece,
            capturedPiece = capturedPiece,
            boardStateBefore = "before",
            boardStateAfter = "after",
            moveNumber = 2,
            isWhiteMove = true
        )
        
        val description = entry.getDescription()
        assertTrue(description.contains("captures pawn"))
    }
    
    @Test
    fun testMoveHistoryWithPromotion() {
        val move = Move(Position(6, 0), Position(7, 0), PieceType.QUEEN)
        val piece = Piece(PieceType.PAWN, PieceColor.WHITE)
        
        val entry = MoveHistoryEntry(
            move = move,
            piece = piece,
            capturedPiece = null,
            boardStateBefore = "before",
            boardStateAfter = "after",
            moveNumber = 10,
            isWhiteMove = true
        )
        
        val description = entry.getDescription()
        assertTrue(description.contains("promotes to queen"))
        assertTrue(entry.toAlgebraicNotation().contains("=Q"))
    }
}
