package com.chessrl.chess

/**
 * Chess Engine Package - Core chess game logic and data structures
 * This package provides complete chess game functionality including
 * board representation, move validation, and game state management.
 */

/**
 * Chess board representation and core game logic
 */
data class ChessBoard(
    private val pieces: Array<Array<Piece?>> = Array(8) { Array(8) { null } },
    private val gameState: GameState = GameState()
) {
    init {
        // Initialize with standard chess starting position if empty
        if (isEmpty()) {
            initializeStandardPosition()
        }
    }
    
    /**
     * Initialize the board with the standard chess starting position
     */
    fun initializeStandardPosition() {
        // Clear the board first
        clearBoard()
        
        // Place white pieces
        pieces[0][0] = Piece(PieceType.ROOK, PieceColor.WHITE)
        pieces[0][1] = Piece(PieceType.KNIGHT, PieceColor.WHITE)
        pieces[0][2] = Piece(PieceType.BISHOP, PieceColor.WHITE)
        pieces[0][3] = Piece(PieceType.QUEEN, PieceColor.WHITE)
        pieces[0][4] = Piece(PieceType.KING, PieceColor.WHITE)
        pieces[0][5] = Piece(PieceType.BISHOP, PieceColor.WHITE)
        pieces[0][6] = Piece(PieceType.KNIGHT, PieceColor.WHITE)
        pieces[0][7] = Piece(PieceType.ROOK, PieceColor.WHITE)
        
        // Place white pawns
        for (file in 0..7) {
            pieces[1][file] = Piece(PieceType.PAWN, PieceColor.WHITE)
        }
        
        // Place black pieces
        pieces[7][0] = Piece(PieceType.ROOK, PieceColor.BLACK)
        pieces[7][1] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
        pieces[7][2] = Piece(PieceType.BISHOP, PieceColor.BLACK)
        pieces[7][3] = Piece(PieceType.QUEEN, PieceColor.BLACK)
        pieces[7][4] = Piece(PieceType.KING, PieceColor.BLACK)
        pieces[7][5] = Piece(PieceType.BISHOP, PieceColor.BLACK)
        pieces[7][6] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
        pieces[7][7] = Piece(PieceType.ROOK, PieceColor.BLACK)
        
        // Place black pawns
        for (file in 0..7) {
            pieces[6][file] = Piece(PieceType.PAWN, PieceColor.BLACK)
        }
    }
    
    /**
     * Clear all pieces from the board
     */
    fun clearBoard() {
        for (rank in 0..7) {
            for (file in 0..7) {
                pieces[rank][file] = null
            }
        }
    }
    
    /**
     * Check if the board is empty
     */
    fun isEmpty(): Boolean {
        for (rank in 0..7) {
            for (file in 0..7) {
                if (pieces[rank][file] != null) {
                    return false
                }
            }
        }
        return true
    }
    
    /**
     * Get piece at a specific position
     */
    fun getPieceAt(position: Position): Piece? {
        if (!isValidPosition(position)) return null
        return pieces[position.rank][position.file]
    }
    
    /**
     * Set piece at a specific position
     */
    fun setPieceAt(position: Position, piece: Piece?) {
        if (isValidPosition(position)) {
            pieces[position.rank][position.file] = piece
        }
    }
    
    /**
     * Check if a position is valid on the chess board
     */
    fun isValidPosition(position: Position): Boolean {
        return position.rank in 0..7 && position.file in 0..7
    }
    
    /**
     * Get all pieces of a specific color
     */
    fun getPiecesOfColor(color: PieceColor): List<Pair<Position, Piece>> {
        val result = mutableListOf<Pair<Position, Piece>>()
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = pieces[rank][file]
                if (piece != null && piece.color == color) {
                    result.add(Pair(Position(rank, file), piece))
                }
            }
        }
        return result
    }
    
    /**
     * Find the king of a specific color
     */
    fun findKing(color: PieceColor): Position? {
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = pieces[rank][file]
                if (piece != null && piece.type == PieceType.KING && piece.color == color) {
                    return Position(rank, file)
                }
            }
        }
        return null
    }
    
    /**
     * Execute a move on the board (basic version without full validation)
     */
    fun makeMove(move: Move): MoveResult {
        if (!isValidPosition(move.from) || !isValidPosition(move.to)) {
            return MoveResult.INVALID_MOVE
        }
        
        val piece = getPieceAt(move.from) ?: return MoveResult.INVALID_MOVE
        val capturedPiece = getPieceAt(move.to)
        
        // Handle special moves
        when {
            // Castling
            piece.type == PieceType.KING && kotlin.math.abs(move.to.file - move.from.file) == 2 -> {
                executeCastling(move)
            }
            // En passant
            piece.type == PieceType.PAWN && capturedPiece == null && move.from.file != move.to.file -> {
                executeEnPassant(move)
            }
            // Regular move
            else -> {
                setPieceAt(move.from, null)
                setPieceAt(move.to, if (move.isPromotion()) Piece(move.promotion!!, piece.color) else piece)
            }
        }
        
        // Update game state
        updateGameStateAfterMove(move, piece, capturedPiece)
        
        return MoveResult.SUCCESS
    }
    
    /**
     * Execute a move with full legal validation
     */
    fun makeLegalMove(move: Move): MoveResult {
        val legalValidator = LegalMoveValidator()
        if (!legalValidator.isLegalMove(this, move)) {
            return MoveResult.INVALID_MOVE
        }
        
        return makeMove(move)
    }
    
    /**
     * Execute castling move
     */
    private fun executeCastling(move: Move) {
        val king = getPieceAt(move.from)!!
        val isKingside = move.to.file > move.from.file
        val rookFromFile = if (isKingside) 7 else 0
        val rookToFile = if (isKingside) move.to.file - 1 else move.to.file + 1
        
        val rookFrom = Position(move.from.rank, rookFromFile)
        val rookTo = Position(move.from.rank, rookToFile)
        val rook = getPieceAt(rookFrom)!!
        
        // Move king
        setPieceAt(move.from, null)
        setPieceAt(move.to, king)
        
        // Move rook
        setPieceAt(rookFrom, null)
        setPieceAt(rookTo, rook)
    }
    
    /**
     * Execute en passant capture
     */
    private fun executeEnPassant(move: Move) {
        val pawn = getPieceAt(move.from)!!
        val capturedPawnRank = move.from.rank
        val capturedPawnFile = move.to.file
        val capturedPawnPos = Position(capturedPawnRank, capturedPawnFile)
        
        // Move pawn
        setPieceAt(move.from, null)
        setPieceAt(move.to, pawn)
        
        // Remove captured pawn
        setPieceAt(capturedPawnPos, null)
    }
    
    /**
     * Update game state after a move
     */
    private fun updateGameStateAfterMove(move: Move, piece: Piece, capturedPiece: Piece?) {
        // Update castling rights
        when (piece.type) {
            PieceType.KING -> {
                if (piece.color == PieceColor.WHITE) {
                    gameState.whiteCanCastleKingside = false
                    gameState.whiteCanCastleQueenside = false
                } else {
                    gameState.blackCanCastleKingside = false
                    gameState.blackCanCastleQueenside = false
                }
            }
            PieceType.ROOK -> {
                when {
                    piece.color == PieceColor.WHITE && move.from == Position(0, 0) -> 
                        gameState.whiteCanCastleQueenside = false
                    piece.color == PieceColor.WHITE && move.from == Position(0, 7) -> 
                        gameState.whiteCanCastleKingside = false
                    piece.color == PieceColor.BLACK && move.from == Position(7, 0) -> 
                        gameState.blackCanCastleQueenside = false
                    piece.color == PieceColor.BLACK && move.from == Position(7, 7) -> 
                        gameState.blackCanCastleKingside = false
                }
            }
            else -> {}
        }
        
        // Update castling rights if rook is captured
        when (move.to) {
            Position(0, 0) -> gameState.whiteCanCastleQueenside = false
            Position(0, 7) -> gameState.whiteCanCastleKingside = false
            Position(7, 0) -> gameState.blackCanCastleQueenside = false
            Position(7, 7) -> gameState.blackCanCastleKingside = false
        }
        
        // Update en passant target
        gameState.enPassantTarget = if (piece.type == PieceType.PAWN && 
                                       kotlin.math.abs(move.to.rank - move.from.rank) == 2) {
            Position((move.from.rank + move.to.rank) / 2, move.from.file)
        } else {
            null
        }
        
        // Update halfmove clock
        if (piece.type == PieceType.PAWN || capturedPiece != null) {
            gameState.halfmoveClock = 0
        } else {
            gameState.halfmoveClock++
        }
        
        // Update fullmove number
        if (piece.color == PieceColor.BLACK) {
            gameState.fullmoveNumber++
        }
    }
    
    /**
     * Convert board to FEN (Forsyth-Edwards Notation)
     */
    fun toFEN(): String {
        val boardStr = StringBuilder()
        
        // Board position (from rank 8 to rank 1)
        for (rank in 7 downTo 0) {
            var emptyCount = 0
            for (file in 0..7) {
                val piece = pieces[rank][file]
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        boardStr.append(emptyCount)
                        emptyCount = 0
                    }
                    boardStr.append(piece.toFENChar())
                }
            }
            if (emptyCount > 0) {
                boardStr.append(emptyCount)
            }
            if (rank > 0) {
                boardStr.append('/')
            }
        }
        
        // Active color
        val activeColor = if (gameState.activeColor == PieceColor.WHITE) "w" else "b"
        
        // Castling rights
        val castling = gameState.getCastlingRights()
        
        // En passant target
        val enPassant = gameState.enPassantTarget?.toAlgebraic() ?: "-"
        
        // Halfmove clock and fullmove number
        val halfmove = gameState.halfmoveClock
        val fullmove = gameState.fullmoveNumber
        
        return "$boardStr $activeColor $castling $enPassant $halfmove $fullmove"
    }
    
    /**
     * Load board from FEN (Forsyth-Edwards Notation)
     */
    fun fromFEN(fen: String): Boolean {
        val parts = fen.trim().split(" ")
        if (parts.size < 1) return false
        
        val boardPart = parts[0]
        clearBoard()
        
        val ranks = boardPart.split("/")
        if (ranks.size != 8) return false
        
        try {
            // Parse board position
            for ((rankIndex, rankStr) in ranks.withIndex()) {
                val rank = 7 - rankIndex // FEN starts from rank 8
                var file = 0
                
                for (char in rankStr) {
                    if (char.isDigit()) {
                        file += char.digitToInt()
                    } else {
                        val piece = Piece.fromFENChar(char) ?: return false
                        if (file >= 8) return false
                        pieces[rank][file] = piece
                        file++
                    }
                }
                if (file != 8) return false
            }
            
            // Parse game state if available
            if (parts.size >= 2) {
                gameState.activeColor = if (parts[1] == "w") PieceColor.WHITE else PieceColor.BLACK
            }
            
            if (parts.size >= 3) {
                gameState.setCastlingRights(parts[2])
            }
            
            if (parts.size >= 4 && parts[3] != "-") {
                gameState.enPassantTarget = Position.fromAlgebraic(parts[3])
            } else {
                gameState.enPassantTarget = null
            }
            
            if (parts.size >= 5) {
                gameState.halfmoveClock = parts[4].toIntOrNull() ?: 0
            }
            
            if (parts.size >= 6) {
                gameState.fullmoveNumber = parts[5].toIntOrNull() ?: 1
            }
            
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Get current game state
     */
    fun getGameState(): GameState = gameState.copy()
    
    /**
     * Get active color (whose turn it is)
     */
    fun getActiveColor(): PieceColor = gameState.activeColor
    
    /**
     * Switch active color
     */
    fun switchActiveColor() {
        gameState.activeColor = if (gameState.activeColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        if (gameState.activeColor == PieceColor.WHITE) {
            gameState.fullmoveNumber++
        }
    }
    
    fun getAllValidMoves(color: PieceColor): List<Move> {
        val validator = MoveValidationSystem()
        return validator.getAllValidMoves(this, color)
    }
    
    fun isInCheck(color: PieceColor): Boolean {
        val detector = GameStateDetector()
        return detector.isInCheck(this, color)
    }
    
    /**
     * Check if the current position is checkmate
     */
    fun isCheckmate(color: PieceColor): Boolean {
        val detector = GameStateDetector()
        return detector.isCheckmate(this, color)
    }
    
    /**
     * Check if the current position is stalemate
     */
    fun isStalemate(color: PieceColor): Boolean {
        val detector = GameStateDetector()
        return detector.isStalemate(this, color)
    }
    
    /**
     * Get the current game status
     */
    fun getGameStatus(gameHistory: List<String> = emptyList()): GameStatus {
        val detector = GameStateDetector()
        return detector.getGameStatus(this, gameHistory)
    }
    
    /**
     * Check if a square is attacked by the given color
     */
    fun isSquareAttacked(position: Position, attackingColor: PieceColor): Boolean {
        val detector = GameStateDetector()
        return detector.isSquareAttacked(this, position, attackingColor)
    }
    
    /**
     * Validate a move using the move validation system
     */
    fun isValidMove(move: Move): Boolean {
        val validator = MoveValidationSystem()
        return validator.isValidMove(this, move)
    }
    
    /**
     * Get valid moves for a piece at a position
     */
    fun getValidMovesForPiece(position: Position): List<Move> {
        val validator = MoveValidationSystem()
        return validator.getValidMoves(this, position)
    }
    
    /**
     * Render the board as ASCII art for console display
     */
    fun toASCII(): String {
        val result = StringBuilder()
        
        // Top border with file labels
        result.appendLine("  +---+---+---+---+---+---+---+---+")
        result.appendLine("  | a | b | c | d | e | f | g | h |")
        result.appendLine("  +---+---+---+---+---+---+---+---+")
        
        // Board rows (from rank 8 to rank 1)
        for (rank in 7 downTo 0) {
            val rankNumber = rank + 1
            result.append("$rankNumber |")
            
            for (file in 0..7) {
                val piece = pieces[rank][file]
                val pieceChar = piece?.toFENChar() ?: ' '
                result.append(" $pieceChar |")
            }
            result.append(" $rankNumber")
            result.appendLine()
            result.appendLine("  +---+---+---+---+---+---+---+---+")
        }
        
        // Bottom border with file labels
        result.appendLine("  | a | b | c | d | e | f | g | h |")
        result.appendLine("  +---+---+---+---+---+---+---+---+")
        
        // Game state information
        result.appendLine()
        result.appendLine("Active Color: ${if (gameState.activeColor == PieceColor.WHITE) "White" else "Black"}")
        result.appendLine("Castling Rights: ${gameState.getCastlingRights()}")
        result.appendLine("En Passant: ${gameState.enPassantTarget?.toAlgebraic() ?: "-"}")
        result.appendLine("Halfmove Clock: ${gameState.halfmoveClock}")
        result.appendLine("Fullmove Number: ${gameState.fullmoveNumber}")
        result.appendLine("FEN: ${toFEN()}")
        
        return result.toString()
    }
    
    /**
     * Render the board with highlighted squares
     */
    fun toASCIIWithHighlights(highlights: Set<Position>): String {
        val result = StringBuilder()
        
        // Top border with file labels
        result.appendLine("  +---+---+---+---+---+---+---+---+")
        result.appendLine("  | a | b | c | d | e | f | g | h |")
        result.appendLine("  +---+---+---+---+---+---+---+---+")
        
        // Board rows (from rank 8 to rank 1)
        for (rank in 7 downTo 0) {
            val rankNumber = rank + 1
            result.append("$rankNumber |")
            
            for (file in 0..7) {
                val position = Position(rank, file)
                val piece = pieces[rank][file]
                val pieceChar = piece?.toFENChar() ?: ' '
                
                if (position in highlights) {
                    result.append("[${pieceChar}]")
                } else {
                    result.append(" $pieceChar |")
                }
            }
            result.append(" $rankNumber")
            result.appendLine()
            result.appendLine("  +---+---+---+---+---+---+---+---+")
        }
        
        // Bottom border with file labels
        result.appendLine("  | a | b | c | d | e | f | g | h |")
        result.appendLine("  +---+---+---+---+---+---+---+---+")
        
        return result.toString()
    }
    
    /**
     * Get a detailed description of the current position
     */
    fun getPositionDescription(): String {
        val result = StringBuilder()
        
        result.appendLine("=== Chess Position Analysis ===")
        result.appendLine()
        
        // Piece count
        val whitePieces = getPiecesOfColor(PieceColor.WHITE)
        val blackPieces = getPiecesOfColor(PieceColor.BLACK)
        
        result.appendLine("Piece Count:")
        result.appendLine("  White: ${whitePieces.size} pieces")
        result.appendLine("  Black: ${blackPieces.size} pieces")
        result.appendLine()
        
        // Piece breakdown
        result.appendLine("White Pieces:")
        for ((position, piece) in whitePieces.sortedBy { it.first.rank * 8 + it.first.file }) {
            result.appendLine("  ${piece.type.name.lowercase().replaceFirstChar { it.uppercase() }} on ${position.toAlgebraic()}")
        }
        result.appendLine()
        
        result.appendLine("Black Pieces:")
        for ((position, piece) in blackPieces.sortedBy { it.first.rank * 8 + it.first.file }) {
            result.appendLine("  ${piece.type.name.lowercase().replaceFirstChar { it.uppercase() }} on ${position.toAlgebraic()}")
        }
        result.appendLine()
        
        // King positions
        val whiteKing = findKing(PieceColor.WHITE)
        val blackKing = findKing(PieceColor.BLACK)
        result.appendLine("King Positions:")
        result.appendLine("  White King: ${whiteKing?.toAlgebraic() ?: "Not found"}")
        result.appendLine("  Black King: ${blackKing?.toAlgebraic() ?: "Not found"}")
        result.appendLine()
        
        // Game state
        result.appendLine("Game State:")
        result.appendLine("  Active Color: ${if (gameState.activeColor == PieceColor.WHITE) "White" else "Black"}")
        result.appendLine("  Castling Rights: ${gameState.getCastlingRights()}")
        result.appendLine("  En Passant Target: ${gameState.enPassantTarget?.toAlgebraic() ?: "None"}")
        result.appendLine("  Halfmove Clock: ${gameState.halfmoveClock}")
        result.appendLine("  Fullmove Number: ${gameState.fullmoveNumber}")
        result.appendLine()
        result.appendLine("FEN: ${toFEN()}")
        
        return result.toString()
    }
    
    /**
     * Create a copy of the current board
     */
    fun copy(): ChessBoard {
        val newBoard = ChessBoard(gameState = gameState.copy())
        newBoard.clearBoard()
        for (rank in 0..7) {
            for (file in 0..7) {
                newBoard.pieces[rank][file] = pieces[rank][file]
            }
        }
        return newBoard
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChessBoard) return false
        
        if (gameState != other.gameState) return false
        
        for (rank in 0..7) {
            for (file in 0..7) {
                if (pieces[rank][file] != other.pieces[rank][file]) {
                    return false
                }
            }
        }
        return true
    }
    
    override fun hashCode(): Int {
        var result = pieces.contentDeepHashCode()
        result = 31 * result + gameState.hashCode()
        return result
    }
}

/**
 * Chess piece representation
 */
data class Piece(val type: PieceType, val color: PieceColor) {
    /**
     * Convert piece to FEN character representation
     */
    fun toFENChar(): Char {
        val baseChar = when (type) {
            PieceType.PAWN -> 'p'
            PieceType.ROOK -> 'r'
            PieceType.KNIGHT -> 'n'
            PieceType.BISHOP -> 'b'
            PieceType.QUEEN -> 'q'
            PieceType.KING -> 'k'
        }
        return if (color == PieceColor.WHITE) baseChar.uppercaseChar() else baseChar
    }
    
    companion object {
        /**
         * Create piece from FEN character
         */
        fun fromFENChar(char: Char): Piece? {
            val color = if (char.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            val type = when (char.lowercaseChar()) {
                'p' -> PieceType.PAWN
                'r' -> PieceType.ROOK
                'n' -> PieceType.KNIGHT
                'b' -> PieceType.BISHOP
                'q' -> PieceType.QUEEN
                'k' -> PieceType.KING
                else -> return null
            }
            return Piece(type, color)
        }
    }
}

/**
 * Chess move representation
 */
data class Move(val from: Position, val to: Position, val promotion: PieceType? = null) {
    /**
     * Convert move to algebraic notation (e.g., "e2e4")
     */
    fun toAlgebraic(): String {
        val moveStr = "${from.toAlgebraic()}${to.toAlgebraic()}"
        return when (promotion) {
            PieceType.QUEEN -> "${moveStr}q"
            PieceType.ROOK -> "${moveStr}r"
            PieceType.BISHOP -> "${moveStr}b"
            PieceType.KNIGHT -> "${moveStr}n"
            else -> moveStr
        }
    }
    
    /**
     * Check if this is a promotion move
     */
    fun isPromotion(): Boolean = promotion != null
    
    companion object {
        /**
         * Create move from algebraic notation (e.g., "e2e4" or "e7e8q")
         */
        fun fromAlgebraic(algebraic: String): Move? {
            if (algebraic.length < 4) return null
            
            val fromPos = Position.fromAlgebraic(algebraic.substring(0, 2)) ?: return null
            val toPos = Position.fromAlgebraic(algebraic.substring(2, 4)) ?: return null
            
            val promotion = if (algebraic.length == 5) {
                when (algebraic[4].lowercaseChar()) {
                    'q' -> PieceType.QUEEN
                    'r' -> PieceType.ROOK
                    'b' -> PieceType.BISHOP
                    'n' -> PieceType.KNIGHT
                    else -> null
                }
            } else null
            
            return Move(fromPos, toPos, promotion)
        }
    }
}

/**
 * Board position representation
 * rank: 0-7 (0 = 1st rank, 7 = 8th rank)
 * file: 0-7 (0 = a-file, 7 = h-file)
 */
data class Position(val rank: Int, val file: Int) {
    /**
     * Convert to algebraic notation (e.g., "e4")
     */
    fun toAlgebraic(): String {
        val fileChar = ('a' + file)
        val rankChar = ('1' + rank)
        return "$fileChar$rankChar"
    }
    
    /**
     * Check if position is valid on chess board
     */
    fun isValid(): Boolean {
        return rank in 0..7 && file in 0..7
    }
    
    companion object {
        /**
         * Create position from algebraic notation (e.g., "e4")
         */
        fun fromAlgebraic(algebraic: String): Position? {
            if (algebraic.length != 2) return null
            
            val file = algebraic[0].lowercaseChar() - 'a'
            val rank = algebraic[1] - '1'
            
            val position = Position(rank, file)
            return if (position.isValid()) position else null
        }
    }
}

/**
 * Piece types
 */
enum class PieceType { PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING }

/**
 * Piece colors
 */
enum class PieceColor { WHITE, BLACK }

/**
 * Move result status
 */
enum class MoveResult { SUCCESS, INVALID_MOVE, ILLEGAL_POSITION }

/**
 * Move history entry with additional information
 */
data class MoveHistoryEntry(
    val move: Move,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val boardStateBefore: String, // FEN before move
    val boardStateAfter: String,  // FEN after move
    val moveNumber: Int,
    val isWhiteMove: Boolean
) {
    /**
     * Convert to algebraic notation with context
     */
    fun toAlgebraicNotation(): String {
        val moveStr = move.toAlgebraic()
        val captureStr = if (capturedPiece != null) "x" else ""
        val pieceStr = if (piece.type == PieceType.PAWN) "" else piece.type.name.first().toString()
        
        return "$pieceStr${move.from.toAlgebraic()}$captureStr${move.to.toAlgebraic()}" +
                if (move.isPromotion()) "=${move.promotion!!.name.first()}" else ""
    }
    
    /**
     * Get a detailed description of this move
     */
    fun getDescription(): String {
        val colorStr = if (isWhiteMove) "White" else "Black"
        val pieceStr = piece.type.name.lowercase().replaceFirstChar { it.uppercase() }
        val captureStr = if (capturedPiece != null) {
            " captures ${capturedPiece.type.name.lowercase()}"
        } else ""
        val promotionStr = if (move.isPromotion()) {
            " and promotes to ${move.promotion!!.name.lowercase()}"
        } else ""
        
        return "$moveNumber. $colorStr $pieceStr from ${move.from.toAlgebraic()} to ${move.to.toAlgebraic()}$captureStr$promotionStr"
    }
}

/**
 * Chess game with move history and analysis tools
 */
class ChessGame {
    private var board = ChessBoard()
    private val moveHistory = mutableListOf<MoveHistoryEntry>()
    private var currentMoveNumber = 1
    
    /**
     * Get current board position
     */
    fun getCurrentBoard(): ChessBoard = board.copy()
    
    /**
     * Make a move and record it in history
     */
    fun makeMove(move: Move): MoveResult {
        val piece = board.getPieceAt(move.from) ?: return MoveResult.INVALID_MOVE
        val capturedPiece = board.getPieceAt(move.to)
        val boardBefore = board.toFEN()
        val isWhiteMove = board.getActiveColor() == PieceColor.WHITE
        
        val result = board.makeMove(move)
        if (result == MoveResult.SUCCESS) {
            board.switchActiveColor()
            val boardAfter = board.toFEN()
            
            val historyEntry = MoveHistoryEntry(
                move = move,
                piece = piece,
                capturedPiece = capturedPiece,
                boardStateBefore = boardBefore,
                boardStateAfter = boardAfter,
                moveNumber = if (isWhiteMove) currentMoveNumber else currentMoveNumber,
                isWhiteMove = isWhiteMove
            )
            
            moveHistory.add(historyEntry)
            
            if (!isWhiteMove) {
                currentMoveNumber++
            }
        }
        
        return result
    }
    
    /**
     * Get move history
     */
    fun getMoveHistory(): List<MoveHistoryEntry> = moveHistory.toList()
    
    /**
     * Get move history as PGN-style notation
     */
    fun getMoveHistoryPGN(): String {
        val result = StringBuilder()
        
        for (i in moveHistory.indices step 2) {
            val whiteMove = moveHistory[i]
            val blackMove = if (i + 1 < moveHistory.size) moveHistory[i + 1] else null
            
            result.append("${whiteMove.moveNumber}. ${whiteMove.toAlgebraicNotation()}")
            if (blackMove != null) {
                result.append(" ${blackMove.toAlgebraicNotation()}")
            }
            result.append(" ")
        }
        
        return result.toString().trim()
    }
    
    /**
     * Get detailed move history
     */
    fun getMoveHistoryDetailed(): String {
        val result = StringBuilder()
        result.appendLine("=== Move History ===")
        result.appendLine()
        
        if (moveHistory.isEmpty()) {
            result.appendLine("No moves played yet.")
            return result.toString()
        }
        
        for (entry in moveHistory) {
            result.appendLine(entry.getDescription())
        }
        
        result.appendLine()
        result.appendLine("PGN: ${getMoveHistoryPGN()}")
        
        return result.toString()
    }
    
    /**
     * Reset game to starting position
     */
    fun reset() {
        board = ChessBoard()
        moveHistory.clear()
        currentMoveNumber = 1
    }
    
    /**
     * Load position from FEN
     */
    fun loadFromFEN(fen: String): Boolean {
        val newBoard = ChessBoard()
        if (newBoard.fromFEN(fen)) {
            board = newBoard
            moveHistory.clear()
            currentMoveNumber = 1
            return true
        }
        return false
    }
    
    /**
     * Get current position as ASCII
     */
    fun displayBoard(): String = board.toASCII()
    
    /**
     * Get position analysis
     */
    fun analyzePosition(): String = board.getPositionDescription()
    
    /**
     * Display board with move highlights
     */
    fun displayBoardWithLastMove(): String {
        if (moveHistory.isEmpty()) {
            return board.toASCII()
        }
        
        val lastMove = moveHistory.last().move
        val highlights = setOf(lastMove.from, lastMove.to)
        return board.toASCIIWithHighlights(highlights)
    }
}

/**
 * Utility class for chess position validation and debugging
 */
object ChessValidator {
    /**
     * Validate a FEN string
     */
    fun validateFEN(fen: String): ValidationResult {
        val issues = mutableListOf<String>()
        
        try {
            val testBoard = ChessBoard()
            testBoard.clearBoard()
            
            if (!testBoard.fromFEN(fen)) {
                issues.add("Invalid FEN format")
            } else {
                // Additional validation checks
                val whiteKing = testBoard.findKing(PieceColor.WHITE)
                val blackKing = testBoard.findKing(PieceColor.BLACK)
                
                if (whiteKing == null) {
                    issues.add("White king not found")
                }
                if (blackKing == null) {
                    issues.add("Black king not found")
                }
                
                // Check for too many pieces
                val whitePieces = testBoard.getPiecesOfColor(PieceColor.WHITE)
                val blackPieces = testBoard.getPiecesOfColor(PieceColor.BLACK)
                
                if (whitePieces.size > 16) {
                    issues.add("Too many white pieces (${whitePieces.size})")
                }
                if (blackPieces.size > 16) {
                    issues.add("Too many black pieces (${blackPieces.size})")
                }
            }
        } catch (e: Exception) {
            issues.add("Exception during validation: ${e.message}")
        }
        
        return ValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            fen = fen
        )
    }
    
    /**
     * Compare two positions
     */
    fun comparePositions(fen1: String, fen2: String): String {
        val board1 = ChessBoard()
        val board2 = ChessBoard()
        
        board1.clearBoard()
        board2.clearBoard()
        
        if (!board1.fromFEN(fen1) || !board2.fromFEN(fen2)) {
            return "Error: Invalid FEN string(s)"
        }
        
        val result = StringBuilder()
        result.appendLine("=== Position Comparison ===")
        result.appendLine()
        result.appendLine("Position 1:")
        result.appendLine(board1.toASCII())
        result.appendLine()
        result.appendLine("Position 2:")
        result.appendLine(board2.toASCII())
        result.appendLine()
        
        if (board1 == board2) {
            result.appendLine("Positions are identical.")
        } else {
            result.appendLine("Positions differ:")
            
            // Find differences
            for (rank in 0..7) {
                for (file in 0..7) {
                    val pos = Position(rank, file)
                    val piece1 = board1.getPieceAt(pos)
                    val piece2 = board2.getPieceAt(pos)
                    
                    if (piece1 != piece2) {
                        val piece1Str = piece1?.toFENChar()?.toString() ?: "empty"
                        val piece2Str = piece2?.toFENChar()?.toString() ?: "empty"
                        result.appendLine("  ${pos.toAlgebraic()}: $piece1Str -> $piece2Str")
                    }
                }
            }
        }
        
        return result.toString()
    }
}

/**
 * Validation result for FEN strings and positions
 */
data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
    val fen: String
) {
    fun getReport(): String {
        val result = StringBuilder()
        result.appendLine("=== FEN Validation Report ===")
        result.appendLine("FEN: $fen")
        result.appendLine("Valid: $isValid")
        
        if (issues.isNotEmpty()) {
            result.appendLine("Issues:")
            for (issue in issues) {
                result.appendLine("  - $issue")
            }
        }
        
        return result.toString()
    }
}

/**
 * Game state information beyond just piece positions
 */
data class GameState(
    var activeColor: PieceColor = PieceColor.WHITE,
    var whiteCanCastleKingside: Boolean = true,
    var whiteCanCastleQueenside: Boolean = true,
    var blackCanCastleKingside: Boolean = true,
    var blackCanCastleQueenside: Boolean = true,
    var enPassantTarget: Position? = null,
    var halfmoveClock: Int = 0,
    var fullmoveNumber: Int = 1
) {
    /**
     * Get castling rights as FEN string
     */
    fun getCastlingRights(): String {
        val rights = StringBuilder()
        if (whiteCanCastleKingside) rights.append('K')
        if (whiteCanCastleQueenside) rights.append('Q')
        if (blackCanCastleKingside) rights.append('k')
        if (blackCanCastleQueenside) rights.append('q')
        return if (rights.isEmpty()) "-" else rights.toString()
    }
    
    /**
     * Set castling rights from FEN string
     */
    fun setCastlingRights(rights: String) {
        whiteCanCastleKingside = 'K' in rights
        whiteCanCastleQueenside = 'Q' in rights
        blackCanCastleKingside = 'k' in rights
        blackCanCastleQueenside = 'q' in rights
    }
}