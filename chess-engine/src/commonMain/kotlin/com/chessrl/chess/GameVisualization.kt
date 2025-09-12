package com.chessrl.chess

/**
 * Comprehensive game visualization and replay tools
 * Provides step-by-step move visualization, game analysis, and interactive browsing
 */

/**
 * Enhanced game replay with visualization capabilities
 */
class GameVisualizer(private val game: PGNGame) {
    private var currentMoveIndex = 0
    private val board = ChessBoard()
    private val positionHistory = mutableListOf<String>()
    private val moveAnnotations = mutableMapOf<Int, MoveAnnotation>()
    
    init {
        reset()
        buildPositionHistory()
    }
    
    /**
     * Reset to starting position
     */
    fun reset() {
        currentMoveIndex = 0
        board.initializeStandardPosition()
    }
    
    /**
     * Build complete position history for efficient navigation
     */
    private fun buildPositionHistory() {
        positionHistory.clear()
        val tempBoard = ChessBoard()
        positionHistory.add(tempBoard.toFEN())
        
        for (move in game.moves) {
            tempBoard.makeMove(move)
            tempBoard.switchActiveColor()
            positionHistory.add(tempBoard.toFEN())
        }
    }
    
    /**
     * Move to next position with visualization
     */
    fun next(): VisualizationResult {
        if (currentMoveIndex >= game.moves.size) {
            return VisualizationResult(false, "Already at end of game", getCurrentVisualization())
        }
        
        val move = game.moves[currentMoveIndex]
        val piece = board.getPieceAt(move.from)!!
        val capturedPiece = board.getPieceAt(move.to)
        
        board.makeMove(move)
        board.switchActiveColor()
        currentMoveIndex++
        
        val visualization = getCurrentVisualization()
        val moveDescription = describeMoveWithContext(move, piece, capturedPiece, currentMoveIndex)
        
        return VisualizationResult(true, moveDescription, visualization)
    }
    
    /**
     * Move to previous position
     */
    fun previous(): VisualizationResult {
        if (currentMoveIndex <= 0) {
            return VisualizationResult(false, "Already at start of game", getCurrentVisualization())
        }
        
        currentMoveIndex--
        board.fromFEN(positionHistory[currentMoveIndex])
        
        val visualization = getCurrentVisualization()
        val moveDescription = if (currentMoveIndex > 0) {
            val move = game.moves[currentMoveIndex - 1]
            val tempBoard = ChessBoard()
            tempBoard.fromFEN(positionHistory[currentMoveIndex - 1])
            val piece = tempBoard.getPieceAt(move.from)!!
            val capturedPiece = tempBoard.getPieceAt(move.to)
            describeMoveWithContext(move, piece, capturedPiece, currentMoveIndex)
        } else {
            "Starting position"
        }
        
        return VisualizationResult(true, moveDescription, visualization)
    }
    
    /**
     * Jump to specific move number
     */
    fun goToMove(moveNumber: Int): VisualizationResult {
        if (moveNumber < 0 || moveNumber > game.moves.size) {
            return VisualizationResult(false, "Invalid move number", getCurrentVisualization())
        }
        
        currentMoveIndex = moveNumber
        board.fromFEN(positionHistory[currentMoveIndex])
        
        val visualization = getCurrentVisualization()
        val moveDescription = if (currentMoveIndex > 0) {
            val move = game.moves[currentMoveIndex - 1]
            val tempBoard = ChessBoard()
            tempBoard.fromFEN(positionHistory[currentMoveIndex - 1])
            val piece = tempBoard.getPieceAt(move.from)!!
            val capturedPiece = tempBoard.getPieceAt(move.to)
            describeMoveWithContext(move, piece, capturedPiece, currentMoveIndex)
        } else {
            "Starting position"
        }
        
        return VisualizationResult(true, moveDescription, visualization)
    }
    
    /**
     * Get current visualization with highlighted squares
     */
    fun getCurrentVisualization(): GameVisualization {
        val highlights = mutableSetOf<Position>()
        
        // Highlight last move if available
        if (currentMoveIndex > 0) {
            val lastMove = game.moves[currentMoveIndex - 1]
            highlights.add(lastMove.from)
            highlights.add(lastMove.to)
        }
        
        return GameVisualization(
            board = board.copy(),
            highlights = highlights,
            moveNumber = currentMoveIndex,
            totalMoves = game.moves.size,
            gameInfo = game.getSummary(),
            currentMove = if (currentMoveIndex > 0) game.moves[currentMoveIndex - 1] else null,
            annotation = moveAnnotations[currentMoveIndex]
        )
    }
    
    /**
     * Add annotation to a specific move
     */
    fun addMoveAnnotation(moveNumber: Int, annotation: MoveAnnotation) {
        moveAnnotations[moveNumber] = annotation
    }
    
    /**
     * Get move annotation
     */
    fun getMoveAnnotation(moveNumber: Int): MoveAnnotation? = moveAnnotations[moveNumber]
    
    /**
     * Describe a move with full context
     */
    private fun describeMoveWithContext(move: Move, piece: Piece, capturedPiece: Piece?, moveNumber: Int): String {
        val colorStr = if ((moveNumber - 1) % 2 == 0) "White" else "Black"
        val moveNum = ((moveNumber - 1) / 2) + 1
        val pieceStr = piece.type.name.lowercase().replaceFirstChar { it.uppercase() }
        
        val captureStr = if (capturedPiece != null) {
            " captures ${capturedPiece.type.name.lowercase()}"
        } else ""
        
        val promotionStr = if (move.isPromotion()) {
            " and promotes to ${move.promotion!!.name.lowercase()}"
        } else ""
        
        val checkStr = if (board.isInCheck(board.getActiveColor())) {
            if (board.isCheckmate(board.getActiveColor())) " - Checkmate!"
            else " - Check!"
        } else ""
        
        return "$moveNum. $colorStr $pieceStr from ${move.from.toAlgebraic()} to ${move.to.toAlgebraic()}$captureStr$promotionStr$checkStr"
    }
    
    /**
     * Get detailed position analysis
     */
    fun getDetailedAnalysis(): String {
        val result = StringBuilder()
        
        result.appendLine("=== Detailed Position Analysis ===")
        result.appendLine("Game: ${game.getSummary()}")
        result.appendLine("Position: ${currentMoveIndex}/${game.moves.size}")
        result.appendLine("Progress: ${"%.1f".format(getProgress())}%")
        result.appendLine()
        
        if (currentMoveIndex > 0) {
            val lastMove = game.moves[currentMoveIndex - 1]
            result.appendLine("Last move: ${lastMove.toAlgebraic()}")
            
            val annotation = moveAnnotations[currentMoveIndex]
            if (annotation != null) {
                result.appendLine("Annotation: ${annotation.text}")
                result.appendLine("Quality: ${annotation.quality}")
            }
        }
        
        result.appendLine()
        result.append(board.getPositionDescription())
        
        // Add tactical analysis
        result.appendLine()
        result.append(getTacticalAnalysis())
        
        return result.toString()
    }
    
    /**
     * Get tactical analysis of current position
     */
    private fun getTacticalAnalysis(): String {
        val result = StringBuilder()
        result.appendLine("=== Tactical Analysis ===")
        
        val whiteInCheck = board.isInCheck(PieceColor.WHITE)
        val blackInCheck = board.isInCheck(PieceColor.BLACK)
        
        if (whiteInCheck) {
            if (board.isCheckmate(PieceColor.WHITE)) {
                result.appendLine("White is in checkmate!")
            } else {
                result.appendLine("White is in check")
            }
        }
        
        if (blackInCheck) {
            if (board.isCheckmate(PieceColor.BLACK)) {
                result.appendLine("Black is in checkmate!")
            } else {
                result.appendLine("Black is in check")
            }
        }
        
        if (board.isStalemate(PieceColor.WHITE)) {
            result.appendLine("White is in stalemate")
        }
        
        if (board.isStalemate(PieceColor.BLACK)) {
            result.appendLine("Black is in stalemate")
        }
        
        // Material count
        val whitePieces = board.getPiecesOfColor(PieceColor.WHITE)
        val blackPieces = board.getPiecesOfColor(PieceColor.BLACK)
        
        val whiteMaterial = calculateMaterial(whitePieces.map { it.second })
        val blackMaterial = calculateMaterial(blackPieces.map { it.second })
        
        result.appendLine()
        result.appendLine("Material Balance:")
        result.appendLine("  White: $whiteMaterial points")
        result.appendLine("  Black: $blackMaterial points")
        result.appendLine("  Advantage: ${if (whiteMaterial > blackMaterial) "White +" else "Black +"}${kotlin.math.abs(whiteMaterial - blackMaterial)}")
        
        return result.toString()
    }
    
    /**
     * Calculate material value
     */
    private fun calculateMaterial(pieces: List<Piece>): Int {
        return pieces.sumOf { piece ->
            when (piece.type) {
                PieceType.PAWN -> 1
                PieceType.KNIGHT -> 3
                PieceType.BISHOP -> 3
                PieceType.ROOK -> 5
                PieceType.QUEEN -> 9
                PieceType.KING -> 0
            }.toInt()
        }
    }
    
    /**
     * Get game progress as percentage
     */
    fun getProgress(): Double {
        return if (game.moves.isEmpty()) 100.0 
               else (currentMoveIndex.toDouble() / game.moves.size) * 100.0
    }
    
    /**
     * Check if at start of game
     */
    fun isAtStart(): Boolean = currentMoveIndex == 0
    
    /**
     * Check if at end of game
     */
    fun isAtEnd(): Boolean = currentMoveIndex >= game.moves.size
}

/**
 * Result of a visualization operation
 */
data class VisualizationResult(
    val success: Boolean,
    val message: String,
    val visualization: GameVisualization
)

/**
 * Complete game visualization data
 */
data class GameVisualization(
    val board: ChessBoard,
    val highlights: Set<Position>,
    val moveNumber: Int,
    val totalMoves: Int,
    val gameInfo: String,
    val currentMove: Move?,
    val annotation: MoveAnnotation?
) {
    /**
     * Render the visualization as ASCII with highlights
     */
    fun render(): String {
        val result = StringBuilder()
        
        result.appendLine("=== Game Visualization ===")
        result.appendLine(gameInfo)
        result.appendLine("Move: $moveNumber/$totalMoves")
        
        if (currentMove != null) {
            result.appendLine("Current move: ${currentMove.toAlgebraic()}")
        }
        
        if (annotation != null) {
            result.appendLine("Annotation: ${annotation.text}")
        }
        
        result.appendLine()
        result.append(board.toASCIIWithHighlights(highlights))
        
        return result.toString()
    }
    
    /**
     * Render compact version
     */
    fun renderCompact(): String {
        val result = StringBuilder()
        
        result.appendLine("Move $moveNumber/$totalMoves")
        if (currentMove != null) {
            result.appendLine("Last: ${currentMove.toAlgebraic()}")
        }
        result.appendLine()
        result.append(board.toASCII())
        
        return result.toString()
    }
}

/**
 * Move annotation for analysis
 */
data class MoveAnnotation(
    val text: String,
    val quality: MoveQuality,
    val symbols: List<String> = emptyList(),
    val variations: List<List<Move>> = emptyList()
)

/**
 * Move quality assessment
 */
enum class MoveQuality(val symbol: String, val description: String) {
    BRILLIANT("!!", "Brilliant move"),
    EXCELLENT("!", "Excellent move"),
    GOOD("!?", "Interesting move"),
    DUBIOUS("?!", "Dubious move"),
    MISTAKE("?", "Mistake"),
    BLUNDER("??", "Blunder"),
    NEUTRAL("", "Normal move")
}