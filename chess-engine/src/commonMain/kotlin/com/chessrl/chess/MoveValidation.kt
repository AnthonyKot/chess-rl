package com.chessrl.chess

/**
 * Move validation system for chess pieces
 * Implements piece-specific movement rules and basic move generation
 */

/**
 * Interface for move validation
 */
interface MoveValidator {
    fun isValidMove(board: ChessBoard, move: Move): Boolean
    fun getValidMoves(board: ChessBoard, position: Position): List<Move>
}

/**
 * Pawn move validator
 */
class PawnValidator : MoveValidator {
    override fun isValidMove(board: ChessBoard, move: Move): Boolean {
        val piece = board.getPieceAt(move.from) ?: return false
        if (piece.type != PieceType.PAWN) return false
        
        val direction = if (piece.color == PieceColor.WHITE) 1 else -1
        val startRank = if (piece.color == PieceColor.WHITE) 1 else 6
        val promotionRank = if (piece.color == PieceColor.WHITE) 7 else 0
        
        val rankDiff = move.to.rank - move.from.rank
        val fileDiff = move.to.file - move.from.file
        
        // Forward move
        if (fileDiff == 0) {
            val targetPiece = board.getPieceAt(move.to)
            if (targetPiece != null) return false // Can't move forward to occupied square
            
            // Single step forward
            if (rankDiff == direction) {
                // Check for promotion
                if (move.to.rank == promotionRank) {
                    return move.promotion != null && move.promotion != PieceType.PAWN && move.promotion != PieceType.KING
                } else {
                    return move.promotion == null
                }
            }
            
            // Double step from starting position
            if (rankDiff == 2 * direction && move.from.rank == startRank) {
                return move.promotion == null
            }
            
            return false
        }
        
        // Diagonal capture
        if (kotlin.math.abs(fileDiff) == 1 && rankDiff == direction) {
            val targetPiece = board.getPieceAt(move.to)
            
            // Regular capture
            if (targetPiece != null && targetPiece.color != piece.color) {
                // Check for promotion
                if (move.to.rank == promotionRank) {
                    return move.promotion != null && move.promotion != PieceType.PAWN && move.promotion != PieceType.KING
                } else {
                    return move.promotion == null
                }
            }
            
            // En passant capture
            val gameState = board.getGameState()
            if (gameState.enPassantTarget == move.to) {
                return move.promotion == null
            }
            
            return false
        }
        
        return false
    }
    
    override fun getValidMoves(board: ChessBoard, position: Position): List<Move> {
        val piece = board.getPieceAt(position) ?: return emptyList()
        if (piece.type != PieceType.PAWN) return emptyList()
        
        val moves = mutableListOf<Move>()
        val direction = if (piece.color == PieceColor.WHITE) 1 else -1
        val startRank = if (piece.color == PieceColor.WHITE) 1 else 6
        val promotionRank = if (piece.color == PieceColor.WHITE) 7 else 0
        
        // Forward moves
        val oneStep = Position(position.rank + direction, position.file)
        if (oneStep.isValid() && board.getPieceAt(oneStep) == null) {
            if (oneStep.rank == promotionRank) {
                // Promotion moves
                moves.add(Move(position, oneStep, PieceType.QUEEN))
                moves.add(Move(position, oneStep, PieceType.ROOK))
                moves.add(Move(position, oneStep, PieceType.BISHOP))
                moves.add(Move(position, oneStep, PieceType.KNIGHT))
            } else {
                moves.add(Move(position, oneStep))
                
                // Double step from starting position
                if (position.rank == startRank) {
                    val twoStep = Position(position.rank + 2 * direction, position.file)
                    if (twoStep.isValid() && board.getPieceAt(twoStep) == null) {
                        moves.add(Move(position, twoStep))
                    }
                }
            }
        }
        
        // Diagonal captures
        for (fileOffset in listOf(-1, 1)) {
            val capturePos = Position(position.rank + direction, position.file + fileOffset)
            if (capturePos.isValid()) {
                val targetPiece = board.getPieceAt(capturePos)
                
                // Regular capture
                if (targetPiece != null && targetPiece.color != piece.color) {
                    if (capturePos.rank == promotionRank) {
                        // Promotion captures
                        moves.add(Move(position, capturePos, PieceType.QUEEN))
                        moves.add(Move(position, capturePos, PieceType.ROOK))
                        moves.add(Move(position, capturePos, PieceType.BISHOP))
                        moves.add(Move(position, capturePos, PieceType.KNIGHT))
                    } else {
                        moves.add(Move(position, capturePos))
                    }
                }
                
                // En passant capture
                val gameState = board.getGameState()
                if (gameState.enPassantTarget == capturePos) {
                    moves.add(Move(position, capturePos))
                }
            }
        }
        
        return moves
    }
}

/**
 * Rook move validator
 */
class RookValidator : MoveValidator {
    override fun isValidMove(board: ChessBoard, move: Move): Boolean {
        val piece = board.getPieceAt(move.from) ?: return false
        if (piece.type != PieceType.ROOK) return false
        
        // Rook moves horizontally or vertically
        val rankDiff = move.to.rank - move.from.rank
        val fileDiff = move.to.file - move.from.file
        
        if (rankDiff != 0 && fileDiff != 0) return false // Not horizontal or vertical
        if (rankDiff == 0 && fileDiff == 0) return false // Not moving
        
        // Check path is clear
        val rankStep = if (rankDiff == 0) 0 else if (rankDiff > 0) 1 else -1
        val fileStep = if (fileDiff == 0) 0 else if (fileDiff > 0) 1 else -1
        
        var currentRank = move.from.rank + rankStep
        var currentFile = move.from.file + fileStep
        
        while (currentRank != move.to.rank || currentFile != move.to.file) {
            if (board.getPieceAt(Position(currentRank, currentFile)) != null) {
                return false // Path blocked
            }
            currentRank += rankStep
            currentFile += fileStep
        }
        
        // Check destination
        val targetPiece = board.getPieceAt(move.to)
        return targetPiece == null || targetPiece.color != piece.color
    }
    
    override fun getValidMoves(board: ChessBoard, position: Position): List<Move> {
        val piece = board.getPieceAt(position) ?: return emptyList()
        if (piece.type != PieceType.ROOK) return emptyList()
        
        val moves = mutableListOf<Move>()
        
        // Horizontal and vertical directions
        val directions = listOf(
            Pair(0, 1),   // Right
            Pair(0, -1),  // Left
            Pair(1, 0),   // Up
            Pair(-1, 0)   // Down
        )
        
        for ((rankStep, fileStep) in directions) {
            var currentRank = position.rank + rankStep
            var currentFile = position.file + fileStep
            
            while (currentRank in 0..7 && currentFile in 0..7) {
                val targetPos = Position(currentRank, currentFile)
                val targetPiece = board.getPieceAt(targetPos)
                
                if (targetPiece == null) {
                    moves.add(Move(position, targetPos))
                } else {
                    if (targetPiece.color != piece.color) {
                        moves.add(Move(position, targetPos)) // Capture
                    }
                    break // Can't move further
                }
                
                currentRank += rankStep
                currentFile += fileStep
            }
        }
        
        return moves
    }
}

/**
 * Bishop move validator
 */
class BishopValidator : MoveValidator {
    override fun isValidMove(board: ChessBoard, move: Move): Boolean {
        val piece = board.getPieceAt(move.from) ?: return false
        if (piece.type != PieceType.BISHOP) return false
        
        // Bishop moves diagonally
        val rankDiff = move.to.rank - move.from.rank
        val fileDiff = move.to.file - move.from.file
        
        if (kotlin.math.abs(rankDiff) != kotlin.math.abs(fileDiff)) return false // Not diagonal
        if (rankDiff == 0 && fileDiff == 0) return false // Not moving
        
        // Check path is clear
        val rankStep = if (rankDiff > 0) 1 else -1
        val fileStep = if (fileDiff > 0) 1 else -1
        
        var currentRank = move.from.rank + rankStep
        var currentFile = move.from.file + fileStep
        
        while (currentRank != move.to.rank || currentFile != move.to.file) {
            if (board.getPieceAt(Position(currentRank, currentFile)) != null) {
                return false // Path blocked
            }
            currentRank += rankStep
            currentFile += fileStep
        }
        
        // Check destination
        val targetPiece = board.getPieceAt(move.to)
        return targetPiece == null || targetPiece.color != piece.color
    }
    
    override fun getValidMoves(board: ChessBoard, position: Position): List<Move> {
        val piece = board.getPieceAt(position) ?: return emptyList()
        if (piece.type != PieceType.BISHOP) return emptyList()
        
        val moves = mutableListOf<Move>()
        
        // Diagonal directions
        val directions = listOf(
            Pair(1, 1),   // Up-right
            Pair(1, -1),  // Up-left
            Pair(-1, 1),  // Down-right
            Pair(-1, -1)  // Down-left
        )
        
        for ((rankStep, fileStep) in directions) {
            var currentRank = position.rank + rankStep
            var currentFile = position.file + fileStep
            
            while (currentRank in 0..7 && currentFile in 0..7) {
                val targetPos = Position(currentRank, currentFile)
                val targetPiece = board.getPieceAt(targetPos)
                
                if (targetPiece == null) {
                    moves.add(Move(position, targetPos))
                } else {
                    if (targetPiece.color != piece.color) {
                        moves.add(Move(position, targetPos)) // Capture
                    }
                    break // Can't move further
                }
                
                currentRank += rankStep
                currentFile += fileStep
            }
        }
        
        return moves
    }
}

/**
 * Knight move validator
 */
class KnightValidator : MoveValidator {
    override fun isValidMove(board: ChessBoard, move: Move): Boolean {
        val piece = board.getPieceAt(move.from) ?: return false
        if (piece.type != PieceType.KNIGHT) return false
        
        // Knight moves in L-shape: 2 squares in one direction, 1 in perpendicular
        val rankDiff = kotlin.math.abs(move.to.rank - move.from.rank)
        val fileDiff = kotlin.math.abs(move.to.file - move.from.file)
        
        val isValidLShape = (rankDiff == 2 && fileDiff == 1) || (rankDiff == 1 && fileDiff == 2)
        if (!isValidLShape) return false
        
        // Check destination
        val targetPiece = board.getPieceAt(move.to)
        return targetPiece == null || targetPiece.color != piece.color
    }
    
    override fun getValidMoves(board: ChessBoard, position: Position): List<Move> {
        val piece = board.getPieceAt(position) ?: return emptyList()
        if (piece.type != PieceType.KNIGHT) return emptyList()
        
        val moves = mutableListOf<Move>()
        
        // All possible knight moves (L-shapes)
        val knightMoves = listOf(
            Pair(2, 1), Pair(2, -1),
            Pair(-2, 1), Pair(-2, -1),
            Pair(1, 2), Pair(1, -2),
            Pair(-1, 2), Pair(-1, -2)
        )
        
        for ((rankOffset, fileOffset) in knightMoves) {
            val targetPos = Position(position.rank + rankOffset, position.file + fileOffset)
            
            if (targetPos.isValid()) {
                val targetPiece = board.getPieceAt(targetPos)
                if (targetPiece == null || targetPiece.color != piece.color) {
                    moves.add(Move(position, targetPos))
                }
            }
        }
        
        return moves
    }
}

/**
 * Queen move validator (combines rook and bishop movement)
 */
class QueenValidator : MoveValidator {
    private val rookValidator = RookValidator()
    private val bishopValidator = BishopValidator()
    
    override fun isValidMove(board: ChessBoard, move: Move): Boolean {
        val piece = board.getPieceAt(move.from) ?: return false
        if (piece.type != PieceType.QUEEN) return false
        
        // Queen moves like rook or bishop
        return rookValidator.isValidMove(board, move) || bishopValidator.isValidMove(board, move)
    }
    
    override fun getValidMoves(board: ChessBoard, position: Position): List<Move> {
        val piece = board.getPieceAt(position) ?: return emptyList()
        if (piece.type != PieceType.QUEEN) return emptyList()
        
        val moves = mutableListOf<Move>()
        
        // All directions (horizontal, vertical, diagonal)
        val directions = listOf(
            Pair(0, 1), Pair(0, -1),     // Horizontal
            Pair(1, 0), Pair(-1, 0),     // Vertical
            Pair(1, 1), Pair(1, -1),     // Diagonal
            Pair(-1, 1), Pair(-1, -1)    // Diagonal
        )
        
        for ((rankStep, fileStep) in directions) {
            var currentRank = position.rank + rankStep
            var currentFile = position.file + fileStep
            
            while (currentRank in 0..7 && currentFile in 0..7) {
                val targetPos = Position(currentRank, currentFile)
                val targetPiece = board.getPieceAt(targetPos)
                
                if (targetPiece == null) {
                    moves.add(Move(position, targetPos))
                } else {
                    if (targetPiece.color != piece.color) {
                        moves.add(Move(position, targetPos)) // Capture
                    }
                    break // Can't move further
                }
                
                currentRank += rankStep
                currentFile += fileStep
            }
        }
        
        return moves
    }
}

/**
 * King move validator
 */
class KingValidator : MoveValidator {
    override fun isValidMove(board: ChessBoard, move: Move): Boolean {
        val piece = board.getPieceAt(move.from) ?: return false
        if (piece.type != PieceType.KING) return false
        
        val rankDiff = kotlin.math.abs(move.to.rank - move.from.rank)
        val fileDiff = kotlin.math.abs(move.to.file - move.from.file)
        
        // Regular king move (one square in any direction)
        if (rankDiff <= 1 && fileDiff <= 1 && (rankDiff != 0 || fileDiff != 0)) {
            val targetPiece = board.getPieceAt(move.to)
            return targetPiece == null || targetPiece.color != piece.color
        }
        
        // Castling (special case)
        if (rankDiff == 0 && fileDiff == 2) {
            return isValidCastling(board, move)
        }
        
        return false
    }
    
    private fun isValidCastling(board: ChessBoard, move: Move): Boolean {
        val piece = board.getPieceAt(move.from) ?: return false
        val gameState = board.getGameState()
        
        // Must be king's first move
        val isKingside = move.to.file > move.from.file
        val canCastle = when {
            piece.color == PieceColor.WHITE && isKingside -> gameState.whiteCanCastleKingside
            piece.color == PieceColor.WHITE && !isKingside -> gameState.whiteCanCastleQueenside
            piece.color == PieceColor.BLACK && isKingside -> gameState.blackCanCastleKingside
            piece.color == PieceColor.BLACK && !isKingside -> gameState.blackCanCastleQueenside
            else -> false
        }
        
        if (!canCastle) return false
        
        // Check if king is in check
        if (board.isInCheck(piece.color)) return false
        
        // Check path is clear and king doesn't pass through check
        val fileStep = if (isKingside) 1 else -1
        var currentFile = move.from.file + fileStep
        
        while (currentFile != move.to.file) {
            val pos = Position(move.from.rank, currentFile)
            if (board.getPieceAt(pos) != null) return false // Path blocked
            
            // TODO: Check if king would be in check at this position
            // This requires implementing attack detection
            
            currentFile += fileStep
        }
        
        // Check if destination square is empty
        if (board.getPieceAt(move.to) != null) return false
        
        // Check if rook is present
        val rookFile = if (isKingside) 7 else 0
        val rookPos = Position(move.from.rank, rookFile)
        val rook = board.getPieceAt(rookPos)
        
        return rook != null && rook.type == PieceType.ROOK && rook.color == piece.color
    }
    
    override fun getValidMoves(board: ChessBoard, position: Position): List<Move> {
        val piece = board.getPieceAt(position) ?: return emptyList()
        if (piece.type != PieceType.KING) return emptyList()
        
        val moves = mutableListOf<Move>()
        
        // Regular king moves (one square in any direction)
        for (rankOffset in -1..1) {
            for (fileOffset in -1..1) {
                if (rankOffset == 0 && fileOffset == 0) continue
                
                val targetPos = Position(position.rank + rankOffset, position.file + fileOffset)
                if (targetPos.isValid()) {
                    val targetPiece = board.getPieceAt(targetPos)
                    if (targetPiece == null || targetPiece.color != piece.color) {
                        moves.add(Move(position, targetPos))
                    }
                }
            }
        }
        
        // Castling moves
        val gameState = board.getGameState()
        
        // Kingside castling
        if ((piece.color == PieceColor.WHITE && gameState.whiteCanCastleKingside) ||
            (piece.color == PieceColor.BLACK && gameState.blackCanCastleKingside)) {
            val kingsideCastle = Move(position, Position(position.rank, position.file + 2))
            if (isValidMove(board, kingsideCastle)) {
                moves.add(kingsideCastle)
            }
        }
        
        // Queenside castling
        if ((piece.color == PieceColor.WHITE && gameState.whiteCanCastleQueenside) ||
            (piece.color == PieceColor.BLACK && gameState.blackCanCastleQueenside)) {
            val queensideCastle = Move(position, Position(position.rank, position.file - 2))
            if (isValidMove(board, queensideCastle)) {
                moves.add(queensideCastle)
            }
        }
        
        return moves
    }
}

/**
 * Main move validation system
 */
class MoveValidationSystem {
    private val validators = mapOf(
        PieceType.PAWN to PawnValidator(),
        PieceType.ROOK to RookValidator(),
        PieceType.BISHOP to BishopValidator(),
        PieceType.KNIGHT to KnightValidator(),
        PieceType.QUEEN to QueenValidator(),
        PieceType.KING to KingValidator()
    )
    
    /**
     * Validate a move
     */
    fun isValidMove(board: ChessBoard, move: Move): Boolean {
        if (!move.from.isValid() || !move.to.isValid()) return false
        
        val piece = board.getPieceAt(move.from) ?: return false
        val validator = validators[piece.type] ?: return false
        
        // Check if it's the correct player's turn
        if (piece.color != board.getActiveColor()) return false
        
        return validator.isValidMove(board, move)
    }
    
    /**
     * Get all valid moves for a piece at a position
     */
    fun getValidMoves(board: ChessBoard, position: Position): List<Move> {
        val piece = board.getPieceAt(position) ?: return emptyList()
        val validator = validators[piece.type] ?: return emptyList()
        
        // Only return moves if it's this piece's turn
        if (piece.color != board.getActiveColor()) return emptyList()
        
        return validator.getValidMoves(board, position)
    }
    
    /**
     * Get all valid moves for a color
     */
    fun getAllValidMoves(board: ChessBoard, color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        
        for (rank in 0..7) {
            for (file in 0..7) {
                val position = Position(rank, file)
                val piece = board.getPieceAt(position)
                
                if (piece != null && piece.color == color) {
                    moves.addAll(getValidMoves(board, position))
                }
            }
        }
        
        return moves
    }
}