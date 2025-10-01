package com.chessrl.chess

import com.github.bhlangonijr.chesslib.Board as LibBoard
import com.github.bhlangonijr.chesslib.Side as LibSide
import com.github.bhlangonijr.chesslib.Square as LibSquare
import com.github.bhlangonijr.chesslib.Piece as LibPiece
import com.github.bhlangonijr.chesslib.PieceType as LibPieceType
import com.github.bhlangonijr.chesslib.move.Move as LibMove
import kotlin.collections.linkedSetOf

/**
 * Advanced chess rules and game state detection
 * Implements check detection, checkmate, stalemate, and special moves
 */

/**
 * Game state detector for advanced chess rules
 */
class GameStateDetector {
    
    /**
     * Check if a king is in check
     */
    fun isInCheck(board: ChessBoard, color: PieceColor): Boolean {
        val manual = isInCheckManual(board, color)
        return manual
    }
    
    /**
     * Check if a square is attacked by the given color
     */
    fun isSquareAttacked(board: ChessBoard, position: Position, attackingColor: PieceColor): Boolean {
        val manual = isSquareAttackedManual(board, position, attackingColor)
        return manual
    }
    
    /**
     * Check if a piece can attack a specific square
     */
    private fun canPieceAttackSquare(board: ChessBoard, piecePosition: Position, targetPosition: Position): Boolean {
        val piece = board.getPieceAt(piecePosition) ?: return false
        
        return when (piece.type) {
            PieceType.PAWN -> canPawnAttack(piecePosition, targetPosition, piece.color)
            PieceType.ROOK -> canRookAttack(board, piecePosition, targetPosition)
            PieceType.BISHOP -> canBishopAttack(board, piecePosition, targetPosition)
            PieceType.KNIGHT -> canKnightAttack(piecePosition, targetPosition)
            PieceType.QUEEN -> canQueenAttack(board, piecePosition, targetPosition)
            PieceType.KING -> canKingAttack(piecePosition, targetPosition)
        }
    }
    
    private fun canPawnAttack(pawnPos: Position, targetPos: Position, pawnColor: PieceColor): Boolean {
        val direction = if (pawnColor == PieceColor.WHITE) 1 else -1
        val rankDiff = targetPos.rank - pawnPos.rank
        val fileDiff = kotlin.math.abs(targetPos.file - pawnPos.file)
        
        // Pawn attacks diagonally one square forward
        return rankDiff == direction && fileDiff == 1
    }
    
    private fun canRookAttack(board: ChessBoard, rookPos: Position, targetPos: Position): Boolean {
        val rankDiff = targetPos.rank - rookPos.rank
        val fileDiff = targetPos.file - rookPos.file
        
        // Rook attacks horizontally or vertically
        if (rankDiff != 0 && fileDiff != 0) return false
        
        // Check if path is clear
        val rankStep = if (rankDiff == 0) 0 else if (rankDiff > 0) 1 else -1
        val fileStep = if (fileDiff == 0) 0 else if (fileDiff > 0) 1 else -1
        
        var currentRank = rookPos.rank + rankStep
        var currentFile = rookPos.file + fileStep
        
        while (currentRank != targetPos.rank || currentFile != targetPos.file) {
            if (board.getPieceAt(Position(currentRank, currentFile)) != null) {
                return false // Path blocked
            }
            currentRank += rankStep
            currentFile += fileStep
        }
        
        return true
    }
    
    private fun canBishopAttack(board: ChessBoard, bishopPos: Position, targetPos: Position): Boolean {
        val rankDiff = targetPos.rank - bishopPos.rank
        val fileDiff = targetPos.file - bishopPos.file
        
        // Bishop attacks diagonally
        if (kotlin.math.abs(rankDiff) != kotlin.math.abs(fileDiff)) return false
        
        // Check if path is clear
        val rankStep = if (rankDiff > 0) 1 else -1
        val fileStep = if (fileDiff > 0) 1 else -1
        
        var currentRank = bishopPos.rank + rankStep
        var currentFile = bishopPos.file + fileStep
        
        while (currentRank != targetPos.rank || currentFile != targetPos.file) {
            if (board.getPieceAt(Position(currentRank, currentFile)) != null) {
                return false // Path blocked
            }
            currentRank += rankStep
            currentFile += fileStep
        }
        
        return true
    }
    
    private fun canKnightAttack(knightPos: Position, targetPos: Position): Boolean {
        val rankDiff = kotlin.math.abs(targetPos.rank - knightPos.rank)
        val fileDiff = kotlin.math.abs(targetPos.file - knightPos.file)
        
        // Knight attacks in L-shape
        return (rankDiff == 2 && fileDiff == 1) || (rankDiff == 1 && fileDiff == 2)
    }
    
    private fun canQueenAttack(board: ChessBoard, queenPos: Position, targetPos: Position): Boolean {
        // Queen attacks like rook or bishop
        return canRookAttack(board, queenPos, targetPos) || canBishopAttack(board, queenPos, targetPos)
    }
    
    private fun canKingAttack(kingPos: Position, targetPos: Position): Boolean {
        val rankDiff = kotlin.math.abs(targetPos.rank - kingPos.rank)
        val fileDiff = kotlin.math.abs(targetPos.file - kingPos.file)
        
        // King attacks one square in any direction
        return rankDiff <= 1 && fileDiff <= 1 && (rankDiff != 0 || fileDiff != 0)
    }
    
    /**
     * Check if the current position is checkmate
     */
    fun isCheckmate(board: ChessBoard, color: PieceColor): Boolean {
        val manual = isCheckmateManual(board, color)
        if (manual) return true
        return try {
            toChessLibBoard(board, color).isMated
        } catch (_: Exception) {
            manual
        }
    }
    
    /**
     * Check if the current position is stalemate
     */
    fun isStalemate(board: ChessBoard, color: PieceColor): Boolean {
        val manual = isStalemateManual(board, color)
        if (manual) return true
        return try {
            toChessLibBoard(board, color).isStaleMate
        } catch (_: Exception) {
            manual
        }
    }
    
    /**
     * Get all legal moves for a color (moves that don't leave king in check)
     */
    fun getAllLegalMoves(board: ChessBoard, color: PieceColor): List<Move> {
        val manualMoves = getAllLegalMovesManual(board, color)
        val libMoves = try {
            toChessLibBoard(board, color).legalMoves().map { it.toEngineMove() }
        } catch (_: Exception) {
            emptyList()
        }
        if (libMoves.isEmpty()) return manualMoves
        if (manualMoves.isEmpty()) return libMoves
        val combined = linkedSetOf<Move>()
        combined.addAll(manualMoves)
        combined.addAll(libMoves)
        return combined.toList()
    }
    
    /**
     * Check if a move is legal (doesn't leave own king in check)
     */
    fun isMoveLegal(board: ChessBoard, move: Move): Boolean {
        val piece = board.getPieceAt(move.from) ?: return false
        val manual = isMoveLegalManual(board, move, piece.color)
        if (!manual) return false
        return try {
            val libBoard = toChessLibBoard(board, piece.color)
            val libMove = move.toLibMove(piece.color)
            libBoard.legalMoves().any { it == libMove } || manual
        } catch (_: Exception) {
            manual
        }
    }

    private fun isInCheckManual(board: ChessBoard, color: PieceColor): Boolean {
        val kingPosition = board.findKing(color) ?: return false
        return isSquareAttackedManual(board, kingPosition, color.opposite())
    }

    private fun getAllLegalMovesManual(board: ChessBoard, color: PieceColor): List<Move> {
        val validator = MoveValidationSystem()
        val pseudoLegalMoves = validator.getAllValidMovesIgnoringTurn(board, color)
        if (pseudoLegalMoves.isEmpty()) return emptyList()
        val legalMoves = mutableListOf<Move>()
        for (move in pseudoLegalMoves) {
            val testBoard = board.deepCopy()
            val result = testBoard.makeMove(move)
            if (result != MoveResult.SUCCESS) continue
            if (!isInCheckManual(testBoard, color)) {
                legalMoves.add(move)
            }
        }
        return legalMoves
    }

    private fun isMoveLegalManual(board: ChessBoard, move: Move, color: PieceColor): Boolean {
        val piece = board.getPieceAt(move.from) ?: return false
        if (piece.color != color) return false
        val validator = MoveValidationSystem()
        val pseudoLegalMoves = validator.getAllValidMovesIgnoringTurn(board, color)
        if (!pseudoLegalMoves.contains(move)) return false
        val testBoard = board.deepCopy()
        val result = testBoard.makeMove(move)
        if (result != MoveResult.SUCCESS) return false
        return !isInCheckManual(testBoard, color)
    }

    private fun isSquareAttackedManual(board: ChessBoard, position: Position, attackingColor: PieceColor): Boolean {
        for (rank in 0..7) {
            for (file in 0..7) {
                val piecePosition = Position(rank, file)
                val piece = board.getPieceAt(piecePosition)

                if (piece != null && piece.color == attackingColor) {
                    if (canPieceAttackSquare(board, piecePosition, position)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isDrawByInsufficientMaterialManual(board: ChessBoard): Boolean {
        val whitePieces = board.getPiecesOfColor(PieceColor.WHITE)
        val blackPieces = board.getPiecesOfColor(PieceColor.BLACK)

        val whiteNonKing = whitePieces.filter { it.second.type != PieceType.KING }
        val blackNonKing = blackPieces.filter { it.second.type != PieceType.KING }
        val whiteHasKing = whitePieces.any { it.second.type == PieceType.KING }
        val blackHasKing = blackPieces.any { it.second.type == PieceType.KING }

        // No material or only kings on the board
        if (whiteNonKing.isEmpty() && blackNonKing.isEmpty()) {
            return true
        }

        // King + minor vs king
        val whiteSingleMinor = whiteNonKing.size == 1 && whiteNonKing[0].second.type in setOf(PieceType.BISHOP, PieceType.KNIGHT)
        val blackSingleMinor = blackNonKing.size == 1 && blackNonKing[0].second.type in setOf(PieceType.BISHOP, PieceType.KNIGHT)

        if (whiteHasKing && blackHasKing) {
            if (whiteSingleMinor && blackNonKing.isEmpty()) return true
            if (blackSingleMinor && whiteNonKing.isEmpty()) return true

            // Opposite kings each with a bishop on same color squares
            if (whiteSingleMinor && blackSingleMinor) {
                val whiteColor = (whiteNonKing[0].first.rank + whiteNonKing[0].first.file) % 2
                val blackColor = (blackNonKing[0].first.rank + blackNonKing[0].first.file) % 2
                if (whiteNonKing[0].second.type == PieceType.BISHOP &&
                    blackNonKing[0].second.type == PieceType.BISHOP &&
                    whiteColor == blackColor) {
                    return true
                }
            }
        }

        return false
    }

    private fun isCheckmateManual(board: ChessBoard, color: PieceColor): Boolean {
        if (!isInCheckManual(board, color)) return false
        return getAllLegalMovesManual(board, color).isEmpty()
    }

    private fun isStalemateManual(board: ChessBoard, color: PieceColor): Boolean {
        if (isInCheckManual(board, color)) return false
        return getAllLegalMovesManual(board, color).isEmpty()
    }
    
    /**
     * Check if castling is legal
     */
    fun isCastlingLegal(board: ChessBoard, move: Move): Boolean {
        val piece = board.getPieceAt(move.from) ?: return false
        if (piece.type != PieceType.KING) return false
        
        val gameState = board.getGameState()
        val isKingside = move.to.file > move.from.file
        
        // Check castling rights
        val canCastle = when {
            piece.color == PieceColor.WHITE && isKingside -> gameState.whiteCanCastleKingside
            piece.color == PieceColor.WHITE && !isKingside -> gameState.whiteCanCastleQueenside
            piece.color == PieceColor.BLACK && isKingside -> gameState.blackCanCastleKingside
            piece.color == PieceColor.BLACK && !isKingside -> gameState.blackCanCastleQueenside
            else -> false
        }
        
        if (!canCastle) return false
        
        // King must not be in check
        if (isInCheck(board, piece.color)) return false
        
        // Path must be clear and king must not pass through check
        val fileStep = if (isKingside) 1 else -1
        var currentFile = move.from.file + fileStep
        
        while (currentFile != move.to.file) {
            val pos = Position(move.from.rank, currentFile)
            
            // Square must be empty
            if (board.getPieceAt(pos) != null) return false
            
            // King must not pass through check
            if (isSquareAttacked(board, pos, piece.color.opposite())) return false
            
            currentFile += fileStep
        }
        
        // Destination square must not be attacked
        if (isSquareAttacked(board, move.to, piece.color.opposite())) return false
        
        // Check if rook is present
        val rookFile = if (isKingside) 7 else 0
        val rookPos = Position(move.from.rank, rookFile)
        val rook = board.getPieceAt(rookPos)
        
        return rook != null && rook.type == PieceType.ROOK && rook.color == piece.color
    }
    
    /**
     * Check for draw conditions
     */
    fun isDrawByInsufficientMaterial(board: ChessBoard): Boolean {
        val manual = isDrawByInsufficientMaterialManual(board)
        val lib = try {
            toChessLibBoard(board).isInsufficientMaterial
        } catch (_: Exception) {
            false
        }
        return manual || lib
    }
    
    /**
     * Check for threefold repetition (simplified - would need full game history)
     */
    fun isDrawByRepetition(board: ChessBoard, gameHistory: List<String>): Boolean {
        val currentFEN = board.toFEN().split(" ").take(4).joinToString(" ") // Position only, ignore clocks
        return gameHistory.count { it == currentFEN } >= 3
    }
    
    /**
     * Check for fifty-move rule
     */
    fun isDrawByFiftyMoveRule(board: ChessBoard): Boolean {
        val manual = board.getGameState().halfmoveClock >= 100
        return manual || (runCatching { toChessLibBoard(board).halfMoveCounter >= 100 }.getOrNull() ?: false)
    }
    
    /**
     * Get the current game status
     */
    fun getGameStatus(board: ChessBoard, gameHistory: List<String> = emptyList()): GameStatus {
        val activeColor = board.getActiveColor()
        
        // Check for checkmate
        if (isCheckmate(board, activeColor)) {
            return if (activeColor == PieceColor.WHITE) GameStatus.BLACK_WINS else GameStatus.WHITE_WINS
        }
        
        // Check for stalemate
        if (isStalemate(board, activeColor)) {
            return GameStatus.DRAW_STALEMATE
        }
        
        // Check for draw conditions (honour clock-based draws before static material)
        if (isDrawByFiftyMoveRule(board)) {
            return GameStatus.DRAW_FIFTY_MOVE_RULE
        }
        
        if (isDrawByRepetition(board, gameHistory)) {
            return GameStatus.DRAW_REPETITION
        }

        if (isDrawByInsufficientMaterial(board)) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL
        }
        
        // Check if in check
        if (isInCheck(board, activeColor)) {
            return GameStatus.IN_CHECK
        }
        
        return GameStatus.ONGOING
    }
}

/**
 * Enhanced move validation that includes legal move checking
 */
class LegalMoveValidator {
    private val gameStateDetector = GameStateDetector()
    private val moveValidator = MoveValidationSystem()
    
    /**
     * Check if a move is both valid and legal
     */
    fun isLegalMove(board: ChessBoard, move: Move): Boolean {
        // First check if move is valid according to piece rules
        if (!moveValidator.isValidMove(board, move)) return false
        
        // Then check if move is legal (doesn't leave king in check)
        return gameStateDetector.isMoveLegal(board, move)
    }
    
    /**
     * Get all legal moves for a piece
     */
    fun getLegalMoves(board: ChessBoard, position: Position): List<Move> {
        val pseudoLegalMoves = moveValidator.getValidMoves(board, position)
        return pseudoLegalMoves.filter { gameStateDetector.isMoveLegal(board, it) }
    }
    
    /**
     * Get all legal moves for a color
     */
    fun getAllLegalMoves(board: ChessBoard, color: PieceColor): List<Move> {
        return gameStateDetector.getAllLegalMoves(board, color)
    }
}

/**
 * Game status enumeration
 */
enum class GameStatus {
    ONGOING,
    IN_CHECK,
    WHITE_WINS,
    BLACK_WINS,
    DRAW_STALEMATE,
    DRAW_INSUFFICIENT_MATERIAL,
    DRAW_FIFTY_MOVE_RULE,
    DRAW_REPETITION;
    
    val isGameOver: Boolean
        get() = this != ONGOING && this != IN_CHECK
    
    val isWin: Boolean
        get() = this == WHITE_WINS || this == BLACK_WINS
    
    val isDraw: Boolean
        get() = this == DRAW_STALEMATE || this == DRAW_INSUFFICIENT_MATERIAL || 
                this == DRAW_FIFTY_MOVE_RULE || this == DRAW_REPETITION
}

/**
 * Extension function to get opposite color
 */
fun PieceColor.opposite(): PieceColor {
    return if (this == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
}

// region Chesslib helpers

private fun toChessLibBoard(board: ChessBoard, activeColor: PieceColor? = null): LibBoard {
    val fenParts = board.toFEN().split(" ").toMutableList()
    if (activeColor != null && fenParts.size >= 2) {
        fenParts[1] = if (activeColor == PieceColor.WHITE) "w" else "b"
    }
    val fen = fenParts.joinToString(" ")
    return LibBoard().apply { loadFromFen(fen) }
}

private fun Position.toLibSquare(): LibSquare = LibSquare.fromValue(toAlgebraic().uppercase())

private fun LibSquare.toPosition(): Position {
    val value = value()
    val file = value[0].lowercaseChar() - 'a'
    val rank = value[1] - '1'
    return Position(rank, file)
}

private fun PieceColor.toSide(): LibSide = if (this == PieceColor.WHITE) LibSide.WHITE else LibSide.BLACK

private fun Move.toLibMove(color: PieceColor): LibMove {
    val fromSquare = from.toLibSquare()
    val toSquare = to.toLibSquare()
    val promotionPiece = promotion?.toLibPiece(color)
    return if (promotionPiece != null) LibMove(fromSquare, toSquare, promotionPiece) else LibMove(fromSquare, toSquare)
}

private fun LibMove.toEngineMove(): Move {
    val promotionType = promotion?.toEnginePieceType()
    return Move(from.toPosition(), to.toPosition(), promotionType)
}

private fun PieceType.toLibPiece(color: PieceColor): LibPiece? = when (this) {
    PieceType.QUEEN -> if (color == PieceColor.WHITE) LibPiece.WHITE_QUEEN else LibPiece.BLACK_QUEEN
    PieceType.ROOK -> if (color == PieceColor.WHITE) LibPiece.WHITE_ROOK else LibPiece.BLACK_ROOK
    PieceType.BISHOP -> if (color == PieceColor.WHITE) LibPiece.WHITE_BISHOP else LibPiece.BLACK_BISHOP
    PieceType.KNIGHT -> if (color == PieceColor.WHITE) LibPiece.WHITE_KNIGHT else LibPiece.BLACK_KNIGHT
    else -> null
}

private fun LibPiece.toEnginePieceType(): PieceType? = when (this.pieceType) {
    LibPieceType.QUEEN -> PieceType.QUEEN
    LibPieceType.ROOK -> PieceType.ROOK
    LibPieceType.BISHOP -> PieceType.BISHOP
    LibPieceType.KNIGHT -> PieceType.KNIGHT
    LibPieceType.PAWN -> PieceType.PAWN
    LibPieceType.KING -> PieceType.KING
    LibPieceType.NONE -> null
}

// endregion
