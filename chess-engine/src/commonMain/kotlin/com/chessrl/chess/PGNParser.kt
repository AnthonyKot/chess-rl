package com.chessrl.chess

/**
 * PGN (Portable Game Notation) parser and game replay capabilities
 * Supports loading chess games from PGN format and converting between notations
 */

/**
 * Represents a parsed PGN game
 */
data class PGNGame(
    val headers: Map<String, String>,
    val moves: List<Move>,
    val moveText: String
) {
    fun getHeader(key: String): String? = headers[key]
    
    fun getEvent(): String = headers["Event"] ?: "?"
    fun getSite(): String = headers["Site"] ?: "?"
    fun getDate(): String = headers["Date"] ?: "????.??.??"
    fun getRound(): String = headers["Round"] ?: "?"
    fun getWhite(): String = headers["White"] ?: "?"
    fun getBlack(): String = headers["Black"] ?: "?"
    fun getResult(): String = headers["Result"] ?: "*"
    
    /**
     * Get a summary of the game
     */
    fun getSummary(): String {
        return "${getWhite()} vs ${getBlack()}, ${getEvent()} (${getDate()}), Result: ${getResult()}, ${moves.size} moves"
    }
    
    /**
     * Replay the game and return all positions
     */
    fun replay(): List<String> {
        val parser = PGNParser()
        return parser.gameToFEN(moves)
    }
}

/**
 * Extension property for piece type symbols
 */
val PieceType.symbol: Char
    get() = when (this) {
        PieceType.KING -> 'K'
        PieceType.QUEEN -> 'Q'
        PieceType.ROOK -> 'R'
        PieceType.BISHOP -> 'B'
        PieceType.KNIGHT -> 'N'
        PieceType.PAWN -> 'P'
    }

/**
 * PGN parser for loading chess games
 */
class PGNParser {
    
    /**
     * Parse a single PGN game string into a list of moves
     */
    fun parseGame(pgn: String): PGNGame {
        val lines = pgn.trim().lines()
        val headers = mutableMapOf<String, String>()
        val moveTextBuilder = StringBuilder()
        
        var inHeaders = true
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            if (trimmedLine.isEmpty()) {
                inHeaders = false
                continue
            }
            
            if (inHeaders && trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                // Parse header
                val headerContent = trimmedLine.substring(1, trimmedLine.length - 1)
                val parts = headerContent.split(" ", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0]
                    val value = parts[1].removeSurrounding("\"")
                    headers[key] = value
                }
            } else {
                // Move text
                inHeaders = false
                moveTextBuilder.append(trimmedLine).append(" ")
            }
        }
        
        val moveText = moveTextBuilder.toString().trim()
        val moves = parseMoveText(moveText)
        
        return PGNGame(headers, moves, moveText)
    }
    
    /**
     * Parse multiple PGN games from a database file content
     */
    fun parseDatabase(pgnContent: String): List<PGNGame> {
        val games = mutableListOf<PGNGame>()
        val gameStrings = pgnContent.split("\n\n\n").filter { it.trim().isNotEmpty() }
        
        for (gameString in gameStrings) {
            try {
                val game = parseGame(gameString)
                games.add(game)
            } catch (e: Exception) {
                // Skip malformed games
                println("Warning: Skipped malformed game: ${e.message}")
            }
        }
        
        return games
    }
    
    /**
     * Parse move text into a list of moves
     */
    private fun parseMoveText(moveText: String): List<Move> {
        val moves = mutableListOf<Move>()
        val board = ChessBoard()
        
        // Clean up the move text
        val cleanText = moveText
            .replace(Regex("\\{[^}]*\\}"), "") // Remove comments
            .replace(Regex("\\([^)]*\\)"), "") // Remove variations
            .replace(Regex("\\$\\d+"), "") // Remove NAGs (Numeric Annotation Glyphs)
            .replace(Regex("[+#!?]+"), "") // Remove check/mate/annotation symbols
        
        // Split into tokens
        val tokens = cleanText.split(Regex("\\s+")).filter { it.isNotEmpty() }
        
        for (token in tokens) {
            // Skip move numbers
            if (token.matches(Regex("\\d+\\."))) continue
            
            // Skip result indicators
            if (token in listOf("1-0", "0-1", "1/2-1/2", "*")) break
            
            try {
                val move = parseAlgebraicMove(token, board)
                if (move != null) {
                    board.makeMove(move)
                    board.switchActiveColor()
                    moves.add(move)
                }
            } catch (e: Exception) {
                println("Warning: Could not parse move '$token': ${e.message}")
            }
        }
        
        return moves
    }
    
    /**
     * Parse algebraic notation move (e.g., "Nf3", "exd5", "O-O")
     */
    private fun parseAlgebraicMove(algebraic: String, board: ChessBoard): Move? {
        val cleanMove = algebraic.replace(Regex("[+#!?]+"), "")
        
        // Handle castling
        when (cleanMove) {
            "O-O", "0-0" -> {
                val kingPos = board.findKing(board.getActiveColor()) ?: return null
                return Move(kingPos, Position(kingPos.rank, 6))
            }
            "O-O-O", "0-0-0" -> {
                val kingPos = board.findKing(board.getActiveColor()) ?: return null
                return Move(kingPos, Position(kingPos.rank, 2))
            }
        }
        
        // Handle regular moves
        return parseStandardAlgebraicMove(cleanMove, board)
    }
    
    /**
     * Parse standard algebraic notation (SAN) move
     */
    private fun parseStandardAlgebraicMove(san: String, board: ChessBoard): Move? {
        val activeColor = board.getActiveColor()
        val allMoves = board.getAllValidMoves(activeColor)
        
        // Try to match the SAN with possible moves
        for (move in allMoves) {
            if (moveMatchesSAN(move, san, board)) {
                return move
            }
        }
        
        return null
    }
    
    /**
     * Check if a move matches the given SAN notation
     */
    private fun moveMatchesSAN(move: Move, san: String, board: ChessBoard): Boolean {
        val piece = board.getPieceAt(move.from) ?: return false
        val capturedPiece = board.getPieceAt(move.to)
        
        // Build expected SAN for this move
        val expectedSAN = buildSAN(move, piece, capturedPiece, board)
        
        return expectedSAN == san
    }
    
    /**
     * Build SAN notation for a move
     */
    fun buildSAN(move: Move, piece: Piece, capturedPiece: Piece?, board: ChessBoard): String {
        val result = StringBuilder()
        
        // Piece designation (except for pawns)
        if (piece.type != PieceType.PAWN) {
            result.append(piece.type.symbol)
            
            // Add disambiguation if needed
            val disambiguation = getDisambiguation(move, piece, board)
            result.append(disambiguation)
        } else if (capturedPiece != null || isEnPassant(move, board)) {
            // Pawn captures include the file
            result.append(('a' + move.from.file))
        }
        
        // Capture indicator
        if (capturedPiece != null || isEnPassant(move, board)) {
            result.append('x')
        }
        
        // Destination square
        result.append(move.to.toAlgebraic())
        
        // Promotion
        if (move.isPromotion()) {
            result.append('=')
            result.append(move.promotion!!.symbol)
        }
        
        return result.toString()
    }
    
    /**
     * Get disambiguation string for a move (when multiple pieces can reach the same square)
     */
    private fun getDisambiguation(move: Move, piece: Piece, board: ChessBoard): String {
        val activeColor = board.getActiveColor()
        val allMoves = board.getAllValidMoves(activeColor)
        
        // Find other pieces of the same type that can move to the same destination
        val conflictingMoves = allMoves.filter { otherMove ->
            otherMove != move &&
            otherMove.to == move.to &&
            board.getPieceAt(otherMove.from)?.type == piece.type
        }
        
        if (conflictingMoves.isEmpty()) return ""
        
        // Check if file disambiguation is sufficient
        val sameFile = conflictingMoves.any { it.from.file == move.from.file }
        if (!sameFile) {
            return ('a' + move.from.file).toString()
        }
        
        // Check if rank disambiguation is sufficient
        val sameRank = conflictingMoves.any { it.from.rank == move.from.rank }
        if (!sameRank) {
            return ('1' + move.from.rank).toString()
        }
        
        // Use full square disambiguation
        return move.from.toAlgebraic()
    }
    
    /**
     * Check if a move is en passant
     */
    private fun isEnPassant(move: Move, board: ChessBoard): Boolean {
        val piece = board.getPieceAt(move.from) ?: return false
        val capturedPiece = board.getPieceAt(move.to)
        val gameState = board.getGameState()
        
        return piece.type == PieceType.PAWN &&
               capturedPiece == null &&
               move.from.file != move.to.file &&
               gameState.enPassantTarget == move.to
    }
    
    /**
     * Convert a list of moves to FEN positions
     */
    fun gameToFEN(moves: List<Move>, startingFEN: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"): List<String> {
        val positions = mutableListOf<String>()
        val board = ChessBoard()
        
        board.fromFEN(startingFEN)
        positions.add(board.toFEN())
        
        for (move in moves) {
            board.makeMove(move)
            board.switchActiveColor()
            positions.add(board.toFEN())
        }
        
        return positions
    }
    
    /**
     * Convert moves to PGN format
     */
    fun movesToPGN(moves: List<Move>, headers: Map<String, String> = emptyMap()): String {
        val result = StringBuilder()
        
        // Add headers
        val defaultHeaders = mapOf(
            "Event" to "?",
            "Site" to "?",
            "Date" to "????.??.??",
            "Round" to "?",
            "White" to "?",
            "Black" to "?",
            "Result" to "*"
        )
        
        val allHeaders = defaultHeaders + headers
        for ((key, value) in allHeaders) {
            result.appendLine("[$key \"$value\"]")
        }
        result.appendLine()
        
        // Add moves
        val board = ChessBoard()
        var moveNumber = 1
        
        for ((index, move) in moves.withIndex()) {
            val piece = board.getPieceAt(move.from) ?: continue
            val capturedPiece = board.getPieceAt(move.to)
            
            if (index % 2 == 0) {
                result.append("$moveNumber. ")
            }
            
            val san = buildSAN(move, piece, capturedPiece, board)
            result.append(san)
            
            board.makeMove(move)
            board.switchActiveColor()
            
            if (index % 2 == 1) {
                result.append(" ")
                moveNumber++
            } else {
                result.append(" ")
            }
        }
        
        result.append(headers["Result"] ?: "*")
        
        return result.toString()
    }
}

/**
 * Game replay engine for step-by-step analysis
 */
class GameReplay(private val game: PGNGame) {
    private var currentMoveIndex = 0
    private val board = ChessBoard()
    
    init {
        reset()
    }
    
    /**
     * Reset to starting position
     */
    fun reset() {
        currentMoveIndex = 0
        board.initializeStandardPosition()
    }
    
    /**
     * Move to next position
     */
    fun next(): Boolean {
        if (currentMoveIndex >= game.moves.size) return false
        
        val move = game.moves[currentMoveIndex]
        board.makeMove(move)
        board.switchActiveColor()
        currentMoveIndex++
        
        return true
    }
    
    /**
     * Move to previous position
     */
    fun previous(): Boolean {
        if (currentMoveIndex <= 0) return false
        
        // Rebuild board up to previous position
        reset()
        repeat(currentMoveIndex - 1) {
            next()
        }
        currentMoveIndex--
        
        return true
    }
    
    /**
     * Jump to specific move number
     */
    fun goToMove(moveNumber: Int): Boolean {
        if (moveNumber < 0 || moveNumber > game.moves.size) return false
        
        reset()
        repeat(moveNumber) {
            if (!next()) return false
        }
        
        return true
    }
    
    /**
     * Get current position
     */
    fun getCurrentPosition(): ChessBoard = board.copy()
    
    /**
     * Get current move number
     */
    fun getCurrentMoveNumber(): Int = currentMoveIndex
    
    /**
     * Get current move (if any)
     */
    fun getCurrentMove(): Move? {
        return if (currentMoveIndex > 0 && currentMoveIndex <= game.moves.size) {
            game.moves[currentMoveIndex - 1]
        } else null
    }
    
    /**
     * Check if at start of game
     */
    fun isAtStart(): Boolean = currentMoveIndex == 0
    
    /**
     * Check if at end of game
     */
    fun isAtEnd(): Boolean = currentMoveIndex >= game.moves.size
    
    /**
     * Get game progress as percentage
     */
    fun getProgress(): Double {
        return if (game.moves.isEmpty()) 100.0 
               else (currentMoveIndex.toDouble() / game.moves.size) * 100.0
    }
    
    /**
     * Get detailed position analysis
     */
    fun getPositionAnalysis(): String {
        val result = StringBuilder()
        
        result.appendLine("=== Position Analysis ===")
        result.appendLine("Game: ${game.getSummary()}")
        result.appendLine("Move: ${currentMoveIndex}/${game.moves.size}")
        result.appendLine("Progress: ${"%.1f".format(getProgress())}%")
        result.appendLine()
        
        if (currentMoveIndex > 0) {
            val lastMove = game.moves[currentMoveIndex - 1]
            result.appendLine("Last move: ${lastMove.toAlgebraic()}")
        }
        
        result.appendLine()
        result.append(board.getPositionDescription())
        
        return result.toString()
    }
}

/**
 * Notation converter for different chess formats
 */
class NotationConverter {
    
    /**
     * Convert algebraic notation to coordinate notation
     */
    fun algebraicToCoordinate(algebraic: String): String? {
        val move = Move.fromAlgebraic(algebraic) ?: return null
        return move.toAlgebraic()
    }
    
    /**
     * Convert coordinate notation to algebraic notation
     */
    fun coordinateToAlgebraic(coordinate: String, board: ChessBoard): String? {
        val move = Move.fromAlgebraic(coordinate) ?: return null
        val piece = board.getPieceAt(move.from) ?: return null
        val capturedPiece = board.getPieceAt(move.to)
        
        val parser = PGNParser()
        return parser.buildSAN(move, piece, capturedPiece, board)
    }
    
    /**
     * Convert FEN to board description
     */
    fun fenToDescription(fen: String): String {
        val board = ChessBoard()
        board.clearBoard()
        
        return if (board.fromFEN(fen)) {
            board.getPositionDescription()
        } else {
            "Invalid FEN: $fen"
        }
    }
    
    /**
     * Convert move list to different formats
     */
    fun convertMoveList(moves: List<Move>, fromFormat: String, toFormat: String, board: ChessBoard? = null): List<String> {
        val workingBoard = board?.copy() ?: ChessBoard()
        val result = mutableListOf<String>()
        
        for (move in moves) {
            val converted = when (toFormat.lowercase()) {
                "coordinate" -> move.toAlgebraic()
                "algebraic", "san" -> {
                    val piece = workingBoard.getPieceAt(move.from) ?: continue
                    val capturedPiece = workingBoard.getPieceAt(move.to)
                    val parser = PGNParser()
                    parser.buildSAN(move, piece, capturedPiece, workingBoard)
                }
                else -> move.toAlgebraic()
            }
            
            result.add(converted)
            workingBoard.makeMove(move)
            workingBoard.switchActiveColor()
        }
        
        return result
    }
}