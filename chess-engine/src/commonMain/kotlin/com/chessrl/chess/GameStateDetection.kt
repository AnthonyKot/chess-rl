package com.chessrl.chess

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
        val kingPosition = board.findKing(color) ?: return false
        return isSquareAttacked(board, kingPosition, color.opposite())
    }
    
    /**
     * Check if a square is attacked by the given color
     */
    fun isSquareAttacked(board: ChessBoard, position: Position, attackingColor: PieceColor): Boolean {
        // Check all pieces of the attacking color to see if any can attack the position
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
        // Must be in check to be checkmate
        if (!isInCheck(board, color)) return false
        
        // If there are any legal moves, it's not checkmate
        return getAllLegalMoves(board, color).isEmpty()
    }
    
    /**
     * Check if the current position is stalemate
     */
    fun isStalemate(board: ChessBoard, color: PieceColor): Boolean {
        // Must not be in check to be stalemate
        if (isInCheck(board, color)) return false
        
        // If there are no legal moves, it's stalemate
        return getAllLegalMoves(board, color).isEmpty()
    }
    
    /**
     * Get all legal moves for a color (moves that don't leave king in check)
     */
    fun getAllLegalMoves(board: ChessBoard, color: PieceColor): List<Move> {
        val validator = MoveValidationSystem()
        val pseudoLegalMoves = validator.getAllValidMoves(board, color)
        val legalMoves = mutableListOf<Move>()
        
        for (move in pseudoLegalMoves) {
            if (isMoveLegal(board, move)) {
                legalMoves.add(move)
            }
        }
        
        return legalMoves
    }
    
    /**
     * Check if a move is legal (doesn't leave own king in check)
     */
    fun isMoveLegal(board: ChessBoard, move: Move): Boolean {
        val piece = board.getPieceAt(move.from) ?: return false
        
        // Make the move on a copy of the board
        val testBoard = board.copy()
        val result = testBoard.makeMove(move)
        
        if (result != MoveResult.SUCCESS) return false
        
        // Check if the move leaves own king in check
        return !isInCheck(testBoard, piece.color)
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
        val whitePieces = board.getPiecesOfColor(PieceColor.WHITE)
        val blackPieces = board.getPiecesOfColor(PieceColor.BLACK)
        
        // King vs King
        if (whitePieces.size == 1 && blackPieces.size == 1) return true
        
        // King and Bishop vs King
        if ((whitePieces.size == 2 && blackPieces.size == 1) || 
            (whitePieces.size == 1 && blackPieces.size == 2)) {
            val allPieces = whitePieces + blackPieces
            val nonKingPieces = allPieces.filter { it.second.type != PieceType.KING }
            if (nonKingPieces.size == 1 && nonKingPieces[0].second.type == PieceType.BISHOP) {
                return true
            }
        }
        
        // King and Knight vs King
        if ((whitePieces.size == 2 && blackPieces.size == 1) || 
            (whitePieces.size == 1 && blackPieces.size == 2)) {
            val allPieces = whitePieces + blackPieces
            val nonKingPieces = allPieces.filter { it.second.type != PieceType.KING }
            if (nonKingPieces.size == 1 && nonKingPieces[0].second.type == PieceType.KNIGHT) {
                return true
            }
        }
        
        // King and Bishop vs King and Bishop (same color squares)
        if (whitePieces.size == 2 && blackPieces.size == 2) {
            val whiteBishops = whitePieces.filter { it.second.type == PieceType.BISHOP }
            val blackBishops = blackPieces.filter { it.second.type == PieceType.BISHOP }
            
            if (whiteBishops.size == 1 && blackBishops.size == 1) {
                val whiteBishopSquareColor = (whiteBishops[0].first.rank + whiteBishops[0].first.file) % 2
                val blackBishopSquareColor = (blackBishops[0].first.rank + blackBishops[0].first.file) % 2
                
                if (whiteBishopSquareColor == blackBishopSquareColor) {
                    return true
                }
            }
        }
        
        return false
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
        return board.getGameState().halfmoveClock >= 100 // 50 moves = 100 half-moves
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
        
        // Check for draw conditions
        if (isDrawByInsufficientMaterial(board)) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL
        }
        
        if (isDrawByFiftyMoveRule(board)) {
            return GameStatus.DRAW_FIFTY_MOVE_RULE
        }
        
        if (isDrawByRepetition(board, gameHistory)) {
            return GameStatus.DRAW_REPETITION
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
