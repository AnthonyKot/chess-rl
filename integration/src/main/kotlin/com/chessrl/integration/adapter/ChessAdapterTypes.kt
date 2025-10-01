package com.chessrl.integration.adapter

import com.chessrl.chess.PieceColor
import com.chessrl.chess.PieceType
import com.chessrl.chess.Position

/**
 * Core data types for the chess engine adapter system
 * 
 * These immutable data classes provide a standardized representation for chess states,
 * moves, and game outcomes that can be used across different engine implementations.
 */

/**
 * Immutable chess state representation for adapter interface
 * 
 * This class represents a complete chess position using FEN-based state management
 * to ensure consistency and immutability across different engine implementations.
 * 
 * @property fen The FEN (Forsyth-Edwards Notation) string representing the position
 * @property activeColor The color whose turn it is to move
 * @property legalMoves Cached legal moves for performance (null if not computed)
 */
data class ChessState(
    val fen: String,
    val activeColor: PieceColor,
    val legalMoves: List<ChessMove>? = null
) {
    /**
     * Create a new ChessState with cached legal moves for performance optimization
     * 
     * @param moves The legal moves to cache
     * @return New ChessState with cached legal moves
     */
    fun withLegalMoves(moves: List<ChessMove>): ChessState = 
        copy(legalMoves = moves)
    
    /**
     * Validate that the FEN string is well-formed
     * 
     * @return true if FEN appears valid, false otherwise
     */
    fun isValidFen(): Boolean {
        val parts = fen.trim().split(" ")
        if (parts.size < 4) return false
        
        // Basic validation of board part
        val boardPart = parts[0]
        val ranks = boardPart.split("/")
        if (ranks.size != 8) return false
        
        // Validate each rank
        for (rank in ranks) {
            var fileCount = 0
            for (char in rank) {
                if (char.isDigit()) {
                    fileCount += char.digitToInt()
                } else if (char.lowercaseChar() in "prnbqk") {
                    fileCount++
                } else {
                    return false
                }
            }
            if (fileCount != 8) return false
        }
        
        // Validate active color
        if (parts[1] !in listOf("w", "b")) return false
        
        return true
    }
}

/**
 * Standardized chess move representation
 * 
 * This class represents a chess move with algebraic notation support and promotion handling.
 * All moves are represented in a consistent format regardless of the underlying engine.
 * 
 * @property from The starting position of the move
 * @property to The destination position of the move
 * @property promotion The piece type to promote to (null if not a promotion)
 * @property algebraic The algebraic notation string (e.g., "e2e4", "e7e8q")
 */
data class ChessMove(
    val from: Position,
    val to: Position,
    val promotion: PieceType? = null,
    val algebraic: String = generateAlgebraic(from, to, promotion)
) {
    
    /**
     * Check if this is a promotion move
     * 
     * @return true if this move includes a pawn promotion
     */
    fun isPromotion(): Boolean = promotion != null
    
    /**
     * Validate that the move positions are on the board
     * 
     * @return true if both from and to positions are valid
     */
    fun isValid(): Boolean = from.isValid() && to.isValid()
    
    companion object {
        /**
         * Create a ChessMove from algebraic notation
         * 
         * Supports standard algebraic notation like "e2e4" for regular moves
         * and "e7e8q" for promotion moves.
         * 
         * @param algebraic The algebraic notation string
         * @return ChessMove if parsing succeeds, null if invalid
         */
        fun fromAlgebraic(algebraic: String): ChessMove? {
            if (algebraic.length < 4) return null
            
            val from = Position.fromAlgebraic(algebraic.substring(0, 2)) ?: return null
            val to = Position.fromAlgebraic(algebraic.substring(2, 4)) ?: return null
            
            val promotion = if (algebraic.length == 5) {
                when (algebraic[4].lowercaseChar()) {
                    'q' -> PieceType.QUEEN
                    'r' -> PieceType.ROOK
                    'b' -> PieceType.BISHOP
                    'n' -> PieceType.KNIGHT
                    else -> null
                }
            } else null
            
            return ChessMove(from, to, promotion, algebraic)
        }
        
        /**
         * Generate algebraic notation string from move components
         * 
         * @param from Starting position
         * @param to Destination position
         * @param promotion Promotion piece type (null if not a promotion)
         * @return Algebraic notation string
         */
        private fun generateAlgebraic(from: Position, to: Position, promotion: PieceType?): String {
            val moveStr = "${from.toAlgebraic()}${to.toAlgebraic()}"
            return when (promotion) {
                PieceType.QUEEN -> "${moveStr}q"
                PieceType.ROOK -> "${moveStr}r"
                PieceType.BISHOP -> "${moveStr}b"
                PieceType.KNIGHT -> "${moveStr}n"
                else -> moveStr
            }
        }
    }
}

/**
 * Terminal game information with standardized outcomes
 * 
 * This class provides a consistent way to represent game endings across
 * different engine implementations.
 * 
 * @property isTerminal Whether the game has ended
 * @property outcome The game outcome (win/loss/draw/ongoing)
 * @property reason Human-readable reason for the game ending
 */
data class TerminalInfo(
    val isTerminal: Boolean,
    val outcome: GameOutcome,
    val reason: String
) {
    companion object {
        /**
         * Create TerminalInfo for an ongoing game
         * 
         * @return TerminalInfo representing a game in progress
         */
        fun ongoing(): TerminalInfo = TerminalInfo(
            isTerminal = false,
            outcome = GameOutcome.ONGOING,
            reason = "ongoing"
        )
        
        /**
         * Create TerminalInfo for a white win
         * 
         * @param reason The reason for white's victory
         * @return TerminalInfo representing white victory
         */
        fun whiteWins(reason: String = "checkmate"): TerminalInfo = TerminalInfo(
            isTerminal = true,
            outcome = GameOutcome.WHITE_WINS,
            reason = reason
        )
        
        /**
         * Create TerminalInfo for a black win
         * 
         * @param reason The reason for black's victory
         * @return TerminalInfo representing black victory
         */
        fun blackWins(reason: String = "checkmate"): TerminalInfo = TerminalInfo(
            isTerminal = true,
            outcome = GameOutcome.BLACK_WINS,
            reason = reason
        )
        
        /**
         * Create TerminalInfo for a draw
         * 
         * @param reason The reason for the draw
         * @return TerminalInfo representing a draw
         */
        fun draw(reason: String): TerminalInfo = TerminalInfo(
            isTerminal = true,
            outcome = GameOutcome.DRAW,
            reason = reason
        )
    }
}

/**
 * Standardized game outcome enumeration
 * 
 * This enum provides a consistent way to represent game results across
 * different engine implementations.
 */
enum class GameOutcome {
    /** White player wins */
    WHITE_WINS,
    
    /** Black player wins */
    BLACK_WINS,
    
    /** Game ends in a draw */
    DRAW,
    
    /** Game is still in progress */
    ONGOING;
    
    /**
     * Check if the game has ended
     * 
     * @return true if the game is over (not ongoing)
     */
    fun isGameOver(): Boolean = this != ONGOING
    
    /**
     * Get the winning color if there is one
     * 
     * @return PieceColor of the winner, null if draw or ongoing
     */
    fun getWinner(): PieceColor? = when (this) {
        WHITE_WINS -> PieceColor.WHITE
        BLACK_WINS -> PieceColor.BLACK
        else -> null
    }
}

/**
 * Utility functions for chess adapter types
 */
object ChessAdapterUtils {
    
    /**
     * Convert a list of moves to algebraic notation strings
     * 
     * @param moves List of ChessMove objects
     * @return List of algebraic notation strings
     */
    fun movesToAlgebraic(moves: List<ChessMove>): List<String> = 
        moves.map { it.algebraic }
    
    /**
     * Parse a list of algebraic notation strings to ChessMove objects
     * 
     * @param algebraicMoves List of algebraic notation strings
     * @return List of ChessMove objects (invalid moves are filtered out)
     */
    fun algebraicToMoves(algebraicMoves: List<String>): List<ChessMove> = 
        algebraicMoves.mapNotNull { ChessMove.fromAlgebraic(it) }
    
    /**
     * Validate that a ChessState is consistent
     * 
     * @param state The ChessState to validate
     * @return true if the state appears valid
     */
    fun validateChessState(state: ChessState): Boolean {
        return state.isValidFen() && 
               (state.legalMoves?.all { it.isValid() } ?: true)
    }
    
    /**
     * Create a standard starting position ChessState
     * 
     * @return ChessState representing the standard chess starting position
     */
    fun standardStartingPosition(): ChessState = ChessState(
        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        activeColor = PieceColor.WHITE
    )
}