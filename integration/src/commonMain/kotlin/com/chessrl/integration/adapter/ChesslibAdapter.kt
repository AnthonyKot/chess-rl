package com.chessrl.integration.adapter

import com.chessrl.chess.PieceColor
import com.chessrl.chess.PieceType
import com.chessrl.chess.Position
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Rank
import com.github.bhlangonijr.chesslib.File
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.github.bhlangonijr.chesslib.move.Move as ChesslibMove

/**
 * Chesslib Chess Engine Adapter
 * 
 * This adapter implementation uses the bhlangonijr/chesslib library to provide
 * an alternative chess rule implementation for validation and testing purposes.
 * 
 * The adapter uses FEN-based immutable state management with Board rehydration
 * pattern to ensure thread safety and consistency with the adapter interface.
 * 
 * All chess logic is delegated to the chesslib library, providing an independent
 * validation source for chess rules and game state detection.
 */
class ChesslibAdapter : ChessEngineAdapter {
    
    override fun initialState(): ChessState {
        val board = Board()
        return ChessState(
            fen = board.fen,
            activeColor = convertSideToColor(board.sideToMove)
        )
    }
    
    override fun getLegalMoves(state: ChessState): List<ChessMove> {
        // Return cached moves if available for performance
        state.legalMoves?.let { return it }
        
        val board = Board()
        board.loadFromFen(state.fen)
        
        val legalMoves = MoveGenerator.generateLegalMoves(board)
        return legalMoves.map { convertToChessMove(it) }
    }
    
    override fun applyMove(state: ChessState, move: ChessMove): ChessState {
        val board = Board()
        board.loadFromFen(state.fen)
        
        val chesslibMove = convertToChesslibMove(move, board)
        if (!board.legalMoves().contains(chesslibMove)) {
            throw IllegalArgumentException("Invalid move: ${move.algebraic}")
        }
        
        board.doMove(chesslibMove)
        
        return ChessState(
            fen = board.fen,
            activeColor = convertSideToColor(board.sideToMove)
        )
    }
    
    override fun isTerminal(state: ChessState): Boolean {
        val board = Board()
        board.loadFromFen(state.fen)
        
        return board.isMated || board.isStaleMate || 
               board.isDraw || board.isRepetition || 
               board.halfMoveCounter >= 100
    }
    
    override fun getOutcome(state: ChessState): TerminalInfo {
        val board = Board()
        board.loadFromFen(state.fen)
        
        return when {
            board.isMated -> {
                val winner = if (board.sideToMove == Side.WHITE) GameOutcome.BLACK_WINS else GameOutcome.WHITE_WINS
                TerminalInfo(true, winner, "checkmate")
            }
            board.isStaleMate -> TerminalInfo(true, GameOutcome.DRAW, "stalemate")
            board.isInsufficientMaterial -> TerminalInfo(true, GameOutcome.DRAW, "insufficient_material")
            board.halfMoveCounter >= 100 -> TerminalInfo(true, GameOutcome.DRAW, "fifty_move_rule")
            board.isRepetition -> TerminalInfo(true, GameOutcome.DRAW, "repetition")
            board.isDraw -> TerminalInfo(true, GameOutcome.DRAW, "draw")
            else -> TerminalInfo(false, GameOutcome.ONGOING, "ongoing")
        }
    }
    
    override fun toFen(state: ChessState): String = state.fen
    
    override fun fromFen(fen: String): ChessState? {
        return try {
            val board = Board()
            board.loadFromFen(fen)
            ChessState(
                fen = fen,
                activeColor = convertSideToColor(board.sideToMove)
            )
        } catch (e: Exception) {
            null
        }
    }
    
    override fun perft(state: ChessState, depth: Int): Long {
        val board = Board()
        board.loadFromFen(state.fen)
        return calculatePerft(board, depth)
    }
    
    override fun getEngineName(): String = "chesslib"
    
    /**
     * Convert chesslib Move to adapter ChessMove
     * 
     * @param move The chesslib Move to convert
     * @return ChessMove representation
     */
    private fun convertToChessMove(move: ChesslibMove): ChessMove {
        val from = Position(
            rank = move.from.rank.ordinal,
            file = move.from.file.ordinal
        )
        val to = Position(
            rank = move.to.rank.ordinal,
            file = move.to.file.ordinal
        )
        val promotion = convertPieceToType(move.promotion)
        
        return ChessMove(from, to, promotion)
    }
    
    /**
     * Convert adapter ChessMove to chesslib Move
     * 
     * This method finds the matching legal move from chesslib's move generation
     * to handle promotion disambiguation and ensure the move is valid.
     * 
     * @param move The ChessMove to convert
     * @param board The current board position
     * @return Chesslib Move representation
     * @throws IllegalArgumentException if no matching legal move is found
     */
    private fun convertToChesslibMove(move: ChessMove, board: Board): ChesslibMove {
        val fromSquare = Square.encode(
            Rank.values()[move.from.rank],
            File.values()[move.from.file]
        )
        val toSquare = Square.encode(
            Rank.values()[move.to.rank],
            File.values()[move.to.file]
        )
        
        // Find matching legal move (handles promotion disambiguation)
        val legalMoves = board.legalMoves()
        val matchingMove = legalMoves.find { chesslibMove ->
            chesslibMove.from == fromSquare && chesslibMove.to == toSquare &&
            (move.promotion == null || convertPieceToType(chesslibMove.promotion) == move.promotion)
        }
        
        return matchingMove ?: throw IllegalArgumentException("No legal move found for ${move.algebraic}")
    }
    
    /**
     * Convert chesslib Side to adapter PieceColor
     * 
     * @param side The chesslib Side to convert
     * @return PieceColor representation
     */
    private fun convertSideToColor(side: Side): PieceColor {
        return when (side) {
            Side.WHITE -> PieceColor.WHITE
            Side.BLACK -> PieceColor.BLACK
        }
    }
    
    /**
     * Convert chesslib Piece to adapter PieceType
     * 
     * @param piece The chesslib Piece to convert (may be null)
     * @return PieceType representation, null if piece is null or not a promotion piece
     */
    private fun convertPieceToType(piece: Piece?): PieceType? {
        return when (piece) {
            Piece.WHITE_QUEEN, Piece.BLACK_QUEEN -> PieceType.QUEEN
            Piece.WHITE_ROOK, Piece.BLACK_ROOK -> PieceType.ROOK
            Piece.WHITE_BISHOP, Piece.BLACK_BISHOP -> PieceType.BISHOP
            Piece.WHITE_KNIGHT, Piece.BLACK_KNIGHT -> PieceType.KNIGHT
            else -> null
        }
    }
    
    /**
     * Calculate perft (performance test) for position validation
     * 
     * This recursive function counts all possible positions at a given depth,
     * which is useful for engine validation and performance testing.
     * 
     * @param board The current board position
     * @param depth The remaining search depth
     * @return Number of leaf nodes at the specified depth
     */
    private fun calculatePerft(board: Board, depth: Int): Long {
        if (depth == 0) return 1L
        
        var nodes = 0L
        val moves = board.legalMoves()
        
        for (move in moves) {
            board.doMove(move)
            nodes += calculatePerft(board, depth - 1)
            board.undoMove()
        }
        
        return nodes
    }
}