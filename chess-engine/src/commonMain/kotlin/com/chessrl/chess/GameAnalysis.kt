package com.chessrl.chess

/**
 * Game analysis tools for move quality assessment and position evaluation
 */

/**
 * Game analyzer for comprehensive game analysis
 */
class GameAnalyzer {
    
    /**
     * Analyze a complete game
     */
    fun analyzeGame(game: PGNGame): GameAnalysisResult {
        val moveAnalyses = mutableListOf<MoveAnalysis>()
        val board = ChessBoard()
        
        for ((index, move) in game.moves.withIndex()) {
            val piece = board.getPieceAt(move.from)!!
            val capturedPiece = board.getPieceAt(move.to)
            val isWhiteMove = index % 2 == 0
            
            val analysis = analyzeMove(board, move, piece, capturedPiece, isWhiteMove)
            moveAnalyses.add(analysis)
            
            board.makeMove(move)
            board.switchActiveColor()
        }
        
        return GameAnalysisResult(
            game = game,
            moveAnalyses = moveAnalyses,
            gameStatistics = calculateGameStatistics(moveAnalyses),
            openingAnalysis = analyzeOpening(game.moves.take(10)),
            endgameAnalysis = analyzeEndgame(game.moves.takeLast(10), board)
        )
    }
    
    /**
     * Analyze a single move
     */
    fun analyzeMove(board: ChessBoard, move: Move, piece: Piece, capturedPiece: Piece?, isWhiteMove: Boolean): MoveAnalysis {
        val beforePosition = board.copy()
        
        // Execute move
        board.makeMove(move)
        board.switchActiveColor()
        val afterPosition = board.copy()
        
        // Restore position for analysis
        board.fromFEN(beforePosition.toFEN())
        
        val tacticalElements = findTacticalElements(beforePosition, move, piece, capturedPiece)
        val positionalFactors = evaluatePositionalFactors(beforePosition, afterPosition, move, piece)
        val moveQuality = assessMoveQuality(tacticalElements, positionalFactors, capturedPiece)
        
        return MoveAnalysis(
            move = move,
            piece = piece,
            capturedPiece = capturedPiece,
            isWhiteMove = isWhiteMove,
            tacticalElements = tacticalElements,
            positionalFactors = positionalFactors,
            quality = moveQuality,
            materialChange = calculateMaterialChange(capturedPiece),
            createsThreats = findThreats(afterPosition, if (isWhiteMove) PieceColor.WHITE else PieceColor.BLACK),
            defendsThreats = findDefendedThreats(beforePosition, afterPosition, move)
        )
    }
    
    /**
     * Find tactical elements in a move
     */
    private fun findTacticalElements(board: ChessBoard, move: Move, piece: Piece, capturedPiece: Piece?): List<TacticalElement> {
        val elements = mutableListOf<TacticalElement>()
        
        // Capture
        if (capturedPiece != null) {
            elements.add(TacticalElement.CAPTURE)
        }
        
        // Check
        val tempBoard = board.copy()
        tempBoard.makeMove(move)
        tempBoard.switchActiveColor()
        val opponentColor = if (piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        if (tempBoard.isInCheck(opponentColor)) {
            elements.add(TacticalElement.CHECK)
            
            // Checkmate
            if (tempBoard.isCheckmate(opponentColor)) {
                elements.add(TacticalElement.CHECKMATE)
            }
        }
        
        // Castling
        if (piece.type == PieceType.KING && kotlin.math.abs(move.to.file - move.from.file) == 2) {
            elements.add(TacticalElement.CASTLING)
        }
        
        // En passant
        if (piece.type == PieceType.PAWN && capturedPiece == null && move.from.file != move.to.file) {
            elements.add(TacticalElement.EN_PASSANT)
        }
        
        // Promotion
        if (move.isPromotion()) {
            elements.add(TacticalElement.PROMOTION)
        }
        
        // Pin detection (simplified)
        if (createsPinOrSkewer(board, move, piece)) {
            elements.add(TacticalElement.PIN)
        }
        
        // Fork detection (simplified)
        if (createsFork(tempBoard, move.to, piece)) {
            elements.add(TacticalElement.FORK)
        }
        
        return elements
    }
    
    /**
     * Evaluate positional factors
     */
    private fun evaluatePositionalFactors(beforeBoard: ChessBoard, afterBoard: ChessBoard, move: Move, piece: Piece): PositionalFactors {
        return PositionalFactors(
            centralControl = evaluateCentralControl(afterBoard, piece.color) - evaluateCentralControl(beforeBoard, piece.color),
            kingSafety = evaluateKingSafety(afterBoard, piece.color) - evaluateKingSafety(beforeBoard, piece.color),
            pieceActivity = evaluatePieceActivity(afterBoard, piece.color) - evaluatePieceActivity(beforeBoard, piece.color),
            pawnStructure = evaluatePawnStructure(afterBoard, piece.color) - evaluatePawnStructure(beforeBoard, piece.color),
            development = evaluateDevelopment(afterBoard, piece.color) - evaluateDevelopment(beforeBoard, piece.color)
        )
    }
    
    /**
     * Assess overall move quality
     */
    private fun assessMoveQuality(tacticalElements: List<TacticalElement>, positionalFactors: PositionalFactors, capturedPiece: Piece?): MoveQuality {
        var score = 0
        
        // Tactical bonuses
        if (TacticalElement.CHECKMATE in tacticalElements) return MoveQuality.BRILLIANT
        if (TacticalElement.CHECK in tacticalElements) score += 2
        if (TacticalElement.CAPTURE in tacticalElements) {
            score += when (capturedPiece?.type) {
                PieceType.QUEEN -> 5
                PieceType.ROOK -> 3
                PieceType.BISHOP, PieceType.KNIGHT -> 2
                PieceType.PAWN -> 1
                else -> 0
            }
        }
        if (TacticalElement.FORK in tacticalElements) score += 3
        if (TacticalElement.PIN in tacticalElements) score += 2
        if (TacticalElement.PROMOTION in tacticalElements) score += 3
        if (TacticalElement.CASTLING in tacticalElements) score += 1
        
        // Positional bonuses
        score += (positionalFactors.centralControl * 0.5).toInt()
        score += (positionalFactors.kingSafety * 0.3).toInt()
        score += (positionalFactors.pieceActivity * 0.4).toInt()
        score += (positionalFactors.development * 0.3).toInt()
        
        return when {
            score >= 8 -> MoveQuality.BRILLIANT
            score >= 5 -> MoveQuality.EXCELLENT
            score >= 2 -> MoveQuality.GOOD
            score >= 0 -> MoveQuality.NEUTRAL
            score >= -2 -> MoveQuality.DUBIOUS
            score >= -5 -> MoveQuality.MISTAKE
            else -> MoveQuality.BLUNDER
        }
    }
    
    /**
     * Calculate material change from a move
     */
    private fun calculateMaterialChange(capturedPiece: Piece?): Int {
        return capturedPiece?.let { piece ->
            when (piece.type) {
                PieceType.PAWN -> 1
                PieceType.KNIGHT -> 3
                PieceType.BISHOP -> 3
                PieceType.ROOK -> 5
                PieceType.QUEEN -> 9
                PieceType.KING -> 0
            }
        } ?: 0
    }
    
    /**
     * Find threats created by a move
     */
    private fun findThreats(board: ChessBoard, color: PieceColor): List<String> {
        val threats = mutableListOf<String>()
        
        val opponentColor = if (color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        val opponentPieces = board.getPiecesOfColor(opponentColor)
        
        for ((position, piece) in opponentPieces) {
            if (board.isSquareAttacked(position, color)) {
                threats.add("Attacks ${piece.type.name.lowercase()} on ${position.toAlgebraic()}")
            }
        }
        
        return threats
    }
    
    /**
     * Find defended threats
     */
    private fun findDefendedThreats(beforeBoard: ChessBoard, afterBoard: ChessBoard, move: Move): List<String> {
        // Simplified implementation - could be expanded
        return emptyList()
    }
    
    /**
     * Check if move creates a pin or skewer (simplified)
     */
    private fun createsPinOrSkewer(board: ChessBoard, move: Move, piece: Piece): Boolean {
        // Simplified implementation for sliding pieces
        return piece.type in listOf(PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN)
    }
    
    /**
     * Check if move creates a fork (simplified)
     */
    private fun createsFork(board: ChessBoard, position: Position, piece: Piece): Boolean {
        if (piece.type != PieceType.KNIGHT) return false
        
        val opponentColor = if (piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        val attackedSquares = getKnightAttacks(position)
        val attackedPieces = attackedSquares.mapNotNull { pos ->
            board.getPieceAt(pos)?.takeIf { it.color == opponentColor }
        }
        
        return attackedPieces.size >= 2
    }
    
    /**
     * Get knight attack squares
     */
    private fun getKnightAttacks(position: Position): List<Position> {
        val attacks = mutableListOf<Position>()
        val knightMoves = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        
        for ((rankOffset, fileOffset) in knightMoves) {
            val newRank = position.rank + rankOffset
            val newFile = position.file + fileOffset
            if (newRank in 0..7 && newFile in 0..7) {
                attacks.add(Position(newRank, newFile))
            }
        }
        
        return attacks
    }
    
    /**
     * Evaluate central control (simplified)
     */
    private fun evaluateCentralControl(board: ChessBoard, color: PieceColor): Double {
        val centralSquares = listOf(
            Position(3, 3), Position(3, 4), Position(4, 3), Position(4, 4)
        )
        
        return centralSquares.count { pos ->
            board.isSquareAttacked(pos, color)
        }.toDouble()
    }
    
    /**
     * Evaluate king safety (simplified)
     */
    private fun evaluateKingSafety(board: ChessBoard, color: PieceColor): Double {
        val king = board.findKing(color) ?: return -10.0
        val opponentColor = if (color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        
        var safety = 0.0
        
        // Penalty for being in check
        if (board.isInCheck(color)) {
            safety -= 5.0
        }
        
        // Bonus for castling rights
        val gameState = board.getGameState()
        if (color == PieceColor.WHITE) {
            if (gameState.whiteCanCastleKingside || gameState.whiteCanCastleQueenside) {
                safety += 1.0
            }
        } else {
            if (gameState.blackCanCastleKingside || gameState.blackCanCastleQueenside) {
                safety += 1.0
            }
        }
        
        return safety
    }
    
    /**
     * Evaluate piece activity (simplified)
     */
    private fun evaluatePieceActivity(board: ChessBoard, color: PieceColor): Double {
        val pieces = board.getPiecesOfColor(color)
        return pieces.sumOf { (position, piece) ->
            when (piece.type) {
                PieceType.KNIGHT -> evaluateKnightActivity(position)
                PieceType.BISHOP -> evaluateBishopActivity(position)
                else -> 0.0
            }
        }
    }
    
    /**
     * Evaluate knight activity
     */
    private fun evaluateKnightActivity(position: Position): Double {
        // Knights are better in the center
        val centerDistance = kotlin.math.abs(position.rank - 3.5) + kotlin.math.abs(position.file - 3.5)
        return 4.0 - centerDistance
    }
    
    /**
     * Evaluate bishop activity
     */
    private fun evaluateBishopActivity(position: Position): Double {
        // Bishops are better on long diagonals
        return if (position.rank + position.file == 7 || position.rank - position.file == 0) 2.0 else 1.0
    }
    
    /**
     * Evaluate pawn structure (simplified)
     */
    private fun evaluatePawnStructure(board: ChessBoard, color: PieceColor): Double {
        val pawns = board.getPiecesOfColor(color).filter { it.second.type == PieceType.PAWN }
        var score = 0.0
        
        // Penalty for doubled pawns
        val fileGroups = pawns.groupBy { it.first.file }
        for ((_, pawnsInFile) in fileGroups) {
            if (pawnsInFile.size > 1) {
                score -= (pawnsInFile.size - 1) * 0.5
            }
        }
        
        return score
    }
    
    /**
     * Evaluate development (simplified)
     */
    private fun evaluateDevelopment(board: ChessBoard, color: PieceColor): Double {
        val pieces = board.getPiecesOfColor(color)
        var development = 0.0
        
        val backRank = if (color == PieceColor.WHITE) 0 else 7
        
        for ((position, piece) in pieces) {
            if (piece.type in listOf(PieceType.KNIGHT, PieceType.BISHOP) && position.rank != backRank) {
                development += 1.0
            }
        }
        
        return development
    }
    
    /**
     * Calculate game statistics
     */
    private fun calculateGameStatistics(moveAnalyses: List<MoveAnalysis>): GameStatistics {
        val whiteMoves = moveAnalyses.filterIndexed { index, _ -> index % 2 == 0 }
        val blackMoves = moveAnalyses.filterIndexed { index, _ -> index % 2 == 1 }
        
        return GameStatistics(
            totalMoves = moveAnalyses.size,
            whiteStatistics = calculatePlayerStatistics(whiteMoves),
            blackStatistics = calculatePlayerStatistics(blackMoves),
            tacticalMoves = moveAnalyses.count { it.tacticalElements.isNotEmpty() },
            captures = moveAnalyses.count { it.capturedPiece != null },
            checks = moveAnalyses.count { TacticalElement.CHECK in it.tacticalElements },
            castling = moveAnalyses.count { TacticalElement.CASTLING in it.tacticalElements }
        )
    }
    
    /**
     * Calculate statistics for one player
     */
    private fun calculatePlayerStatistics(moves: List<MoveAnalysis>): PlayerStatistics {
        val qualityCounts = moves.groupingBy { it.quality }.eachCount()
        
        return PlayerStatistics(
            totalMoves = moves.size,
            brilliantMoves = qualityCounts[MoveQuality.BRILLIANT] ?: 0,
            excellentMoves = qualityCounts[MoveQuality.EXCELLENT] ?: 0,
            goodMoves = qualityCounts[MoveQuality.GOOD] ?: 0,
            neutralMoves = qualityCounts[MoveQuality.NEUTRAL] ?: 0,
            dubiousMoves = qualityCounts[MoveQuality.DUBIOUS] ?: 0,
            mistakes = qualityCounts[MoveQuality.MISTAKE] ?: 0,
            blunders = qualityCounts[MoveQuality.BLUNDER] ?: 0,
            averageQuality = calculateAverageQuality(moves),
            materialGained = moves.sumOf { it.materialChange }
        )
    }
    
    /**
     * Calculate average move quality
     */
    private fun calculateAverageQuality(moves: List<MoveAnalysis>): Double {
        if (moves.isEmpty()) return 0.0
        
        val qualityScores = moves.map { move ->
            when (move.quality) {
                MoveQuality.BRILLIANT -> 5.0
                MoveQuality.EXCELLENT -> 4.0
                MoveQuality.GOOD -> 3.0
                MoveQuality.NEUTRAL -> 2.0
                MoveQuality.DUBIOUS -> 1.0
                MoveQuality.MISTAKE -> 0.0
                MoveQuality.BLUNDER -> -1.0
            }
        }
        
        return qualityScores.average()
    }
    
    /**
     * Analyze opening (simplified)
     */
    private fun analyzeOpening(openingMoves: List<Move>): OpeningAnalysis {
        // Simplified opening analysis
        val developmentMoves = openingMoves.take(10).count { move ->
            // This is a simplified check - in reality, you'd need to track piece types
            move.from.rank in listOf(0, 1, 6, 7) && move.to.rank in listOf(2, 3, 4, 5)
        }
        
        return OpeningAnalysis(
            name = "Unknown Opening", // Would need opening database
            developmentMoves = developmentMoves,
            centralControl = openingMoves.count { move ->
                move.to in listOf(Position(3, 3), Position(3, 4), Position(4, 3), Position(4, 4))
            },
            kingSafety = 0 // Would need more sophisticated analysis
        )
    }
    
    /**
     * Analyze endgame (simplified)
     */
    private fun analyzeEndgame(endgameMoves: List<Move>, finalBoard: ChessBoard): EndgameAnalysis {
        val totalPieces = finalBoard.getPiecesOfColor(PieceColor.WHITE).size + 
                         finalBoard.getPiecesOfColor(PieceColor.BLACK).size
        
        return EndgameAnalysis(
            type = if (totalPieces <= 6) "Basic Endgame" else "Complex Endgame",
            piecesRemaining = totalPieces,
            kingActivity = 0.0, // Would need more sophisticated analysis
            pawnEndgame = finalBoard.getPiecesOfColor(PieceColor.WHITE).all { it.second.type in listOf(PieceType.KING, PieceType.PAWN) } &&
                         finalBoard.getPiecesOfColor(PieceColor.BLACK).all { it.second.type in listOf(PieceType.KING, PieceType.PAWN) }
        )
    }
}

/**
 * Tactical elements that can occur in a move
 */
enum class TacticalElement {
    CAPTURE,
    CHECK,
    CHECKMATE,
    CASTLING,
    EN_PASSANT,
    PROMOTION,
    PIN,
    SKEWER,
    FORK,
    DISCOVERED_ATTACK,
    DOUBLE_ATTACK
}

/**
 * Positional factors evaluation
 */
data class PositionalFactors(
    val centralControl: Double,
    val kingSafety: Double,
    val pieceActivity: Double,
    val pawnStructure: Double,
    val development: Double
)

/**
 * Analysis of a single move
 */
data class MoveAnalysis(
    val move: Move,
    val piece: Piece,
    val capturedPiece: Piece?,
    val isWhiteMove: Boolean,
    val tacticalElements: List<TacticalElement>,
    val positionalFactors: PositionalFactors,
    val quality: MoveQuality,
    val materialChange: Int,
    val createsThreats: List<String>,
    val defendsThreats: List<String>
) {
    /**
     * Get a summary of the move analysis
     */
    fun getSummary(): String {
        val result = StringBuilder()
        
        result.append("${move.toAlgebraic()} (${quality.description})")
        
        if (tacticalElements.isNotEmpty()) {
            result.append(" - ${tacticalElements.joinToString(", ") { it.name.lowercase().replace("_", " ") }}")
        }
        
        if (materialChange > 0) {
            result.append(" - Gains $materialChange material")
        }
        
        if (createsThreats.isNotEmpty()) {
            result.append(" - Creates threats")
        }
        
        return result.toString()
    }
}

/**
 * Complete game analysis result
 */
data class GameAnalysisResult(
    val game: PGNGame,
    val moveAnalyses: List<MoveAnalysis>,
    val gameStatistics: GameStatistics,
    val openingAnalysis: OpeningAnalysis,
    val endgameAnalysis: EndgameAnalysis
) {
    /**
     * Generate a comprehensive analysis report
     */
    fun generateReport(): String {
        val result = StringBuilder()
        
        result.appendLine("=== Comprehensive Game Analysis ===")
        result.appendLine()
        result.appendLine("Game: ${game.getSummary()}")
        result.appendLine()
        
        // Game statistics
        result.appendLine("=== Game Statistics ===")
        result.appendLine("Total moves: ${gameStatistics.totalMoves}")
        result.appendLine("Tactical moves: ${gameStatistics.tacticalMoves}")
        result.appendLine("Captures: ${gameStatistics.captures}")
        result.appendLine("Checks: ${gameStatistics.checks}")
        result.appendLine("Castling: ${gameStatistics.castling}")
        result.appendLine()
        
        // White statistics
        result.appendLine("=== White Statistics ===")
        result.append(gameStatistics.whiteStatistics.getReport())
        result.appendLine()
        
        // Black statistics
        result.appendLine("=== Black Statistics ===")
        result.append(gameStatistics.blackStatistics.getReport())
        result.appendLine()
        
        // Opening analysis
        result.appendLine("=== Opening Analysis ===")
        result.appendLine("Opening: ${openingAnalysis.name}")
        result.appendLine("Development moves: ${openingAnalysis.developmentMoves}")
        result.appendLine("Central control: ${openingAnalysis.centralControl}")
        result.appendLine()
        
        // Endgame analysis
        result.appendLine("=== Endgame Analysis ===")
        result.appendLine("Type: ${endgameAnalysis.type}")
        result.appendLine("Pieces remaining: ${endgameAnalysis.piecesRemaining}")
        result.appendLine("Pawn endgame: ${endgameAnalysis.pawnEndgame}")
        result.appendLine()
        
        // Key moves
        result.appendLine("=== Key Moves ===")
        val keyMoves = moveAnalyses.filter { 
            it.quality in listOf(MoveQuality.BRILLIANT, MoveQuality.EXCELLENT, MoveQuality.BLUNDER) 
        }
        
        for ((index, analysis) in keyMoves.withIndex()) {
            val moveNumber = moveAnalyses.indexOf(analysis) + 1
            val colorStr = if (analysis.isWhiteMove) "White" else "Black"
            result.appendLine("$moveNumber. $colorStr: ${analysis.getSummary()}")
        }
        
        return result.toString()
    }
}

/**
 * Game statistics
 */
data class GameStatistics(
    val totalMoves: Int,
    val whiteStatistics: PlayerStatistics,
    val blackStatistics: PlayerStatistics,
    val tacticalMoves: Int,
    val captures: Int,
    val checks: Int,
    val castling: Int
)

/**
 * Player statistics
 */
data class PlayerStatistics(
    val totalMoves: Int,
    val brilliantMoves: Int,
    val excellentMoves: Int,
    val goodMoves: Int,
    val neutralMoves: Int,
    val dubiousMoves: Int,
    val mistakes: Int,
    val blunders: Int,
    val averageQuality: Double,
    val materialGained: Int
) {
    fun getReport(): String {
        val result = StringBuilder()
        result.appendLine("Total moves: $totalMoves")
        result.appendLine("Brilliant: $brilliantMoves, Excellent: $excellentMoves, Good: $goodMoves")
        result.appendLine("Neutral: $neutralMoves, Dubious: $dubiousMoves")
        result.appendLine("Mistakes: $mistakes, Blunders: $blunders")
        result.appendLine("Average quality: ${(averageQuality * 100).toInt() / 100.0}")
        result.appendLine("Material gained: $materialGained")
        return result.toString()
    }
}

/**
 * Opening analysis
 */
data class OpeningAnalysis(
    val name: String,
    val developmentMoves: Int,
    val centralControl: Int,
    val kingSafety: Int
)

/**
 * Endgame analysis
 */
data class EndgameAnalysis(
    val type: String,
    val piecesRemaining: Int,
    val kingActivity: Double,
    val pawnEndgame: Boolean
)