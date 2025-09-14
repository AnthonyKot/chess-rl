package com.chessrl.chess

/**
 * Game export utilities for various formats (PGN, FEN sequences, analysis reports)
 */

/**
 * Game exporter for multiple formats
 */
class GameExporter {
    
    /**
     * Export game to PGN format with optional annotations
     */
    fun exportToPGN(
        game: PGNGame, 
        includeAnalysis: Boolean = false,
        analysisResult: GameAnalysisResult? = null
    ): String {
        val result = StringBuilder()
        
        // Headers
        val headers = game.headers.toMutableMap()
        
        // Add analysis metadata if available
        if (includeAnalysis && analysisResult != null) {
            headers["Annotator"] = "Chess RL Bot Analysis Engine"
            headers["Analysis"] = "Computer Analysis"
        }
        
        // Write headers
        val headerOrder = listOf("Event", "Site", "Date", "Round", "White", "Black", "Result")
        for (header in headerOrder) {
            if (headers.containsKey(header)) {
                result.appendLine("[${header} \"${headers[header]}\"]")
            }
        }
        
        // Write remaining headers
        for ((key, value) in headers) {
            if (key !in headerOrder) {
                result.appendLine("[${key} \"${value}\"]")
            }
        }
        
        result.appendLine()
        
        // Moves with optional analysis
        if (includeAnalysis && analysisResult != null) {
            result.append(exportMovesWithAnalysis(game, analysisResult))
        } else {
            result.append(game.moveText)
        }
        
        result.appendLine()
        result.appendLine()
        
        return result.toString()
    }
    
    /**
     * Export moves with analysis annotations
     */
    private fun exportMovesWithAnalysis(game: PGNGame, analysisResult: GameAnalysisResult): String {
        val result = StringBuilder()
        val board = ChessBoard()
        
        for ((index, move) in game.moves.withIndex()) {
            val analysis = analysisResult.moveAnalyses[index]
            val piece = board.getPieceAt(move.from)!!
            val capturedPiece = board.getPieceAt(move.to)
            
            // Move number for white moves
            if (index % 2 == 0) {
                val moveNumber = (index / 2) + 1
                result.append("$moveNumber. ")
            }
            
            // Move in SAN notation
            val parser = PGNParser()
            val san = parser.buildSAN(move, piece, capturedPiece, board)
            result.append(san)
            
            // Quality annotation
            result.append(analysis.quality.symbol)
            
            // Tactical annotations
            if (analysis.tacticalElements.isNotEmpty()) {
                val tacticalSymbols = analysis.tacticalElements.mapNotNull { element ->
                    when (element) {
                        TacticalElement.CHECK -> "+"
                        TacticalElement.CHECKMATE -> "#"
                        TacticalElement.CAPTURE -> "x" // Already in SAN
                        else -> null
                    }
                }.distinct()
                
                if (tacticalSymbols.isNotEmpty()) {
                    // Tactical symbols are already included in SAN or quality
                }
            }
            
            // Add comment with analysis
            if (analysis.quality != MoveQuality.NEUTRAL || analysis.tacticalElements.isNotEmpty()) {
                result.append(" { ${analysis.getSummary()} }")
            }
            
            result.append(" ")
            
            // Execute move
            board.makeMove(move)
            board.switchActiveColor()
        }
        
        // Game result
        result.append(game.getResult())
        
        return result.toString()
    }
    
    /**
     * Export game as FEN sequence
     */
    fun exportToFENSequence(game: PGNGame): FENSequenceExport {
        val parser = PGNParser()
        val positions = parser.gameToFEN(game.moves)
        
        return FENSequenceExport(
            gameInfo = game.getSummary(),
            startingPosition = positions.first(),
            positions = positions,
            moveCount = game.moves.size
        )
    }
    
    /**
     * Export game as move list in various notations
     */
    fun exportMoveList(game: PGNGame, notation: MoveNotation = MoveNotation.ALGEBRAIC): MoveListExport {
        val board = ChessBoard()
        val moves = mutableListOf<String>()
        
        for (move in game.moves) {
            val piece = board.getPieceAt(move.from)!!
            val capturedPiece = board.getPieceAt(move.to)
            
            val moveString = when (notation) {
                MoveNotation.ALGEBRAIC -> {
                    val parser = PGNParser()
                    parser.buildSAN(move, piece, capturedPiece, board)
                }
                MoveNotation.COORDINATE -> move.toAlgebraic()
                MoveNotation.LONG_ALGEBRAIC -> {
                    val captureStr = if (capturedPiece != null) "x" else "-"
                    "${piece.type.symbol}${move.from.toAlgebraic()}$captureStr${move.to.toAlgebraic()}"
                }
                MoveNotation.DESCRIPTIVE -> convertToDescriptive(move, piece, capturedPiece)
            }
            
            moves.add(moveString)
            board.makeMove(move)
            board.switchActiveColor()
        }
        
        return MoveListExport(
            gameInfo = game.getSummary(),
            notation = notation,
            moves = moves
        )
    }
    
    /**
     * Convert move to descriptive notation (simplified)
     */
    private fun convertToDescriptive(move: Move, piece: Piece, capturedPiece: Piece?): String {
        // Simplified descriptive notation - full implementation would be more complex
        val pieceStr = when (piece.type) {
            PieceType.KING -> "K"
            PieceType.QUEEN -> "Q"
            PieceType.ROOK -> "R"
            PieceType.BISHOP -> "B"
            PieceType.KNIGHT -> "N"
            PieceType.PAWN -> "P"
        }
        
        val captureStr = if (capturedPiece != null) "x" else "-"
        return "$pieceStr${move.from.toAlgebraic()}$captureStr${move.to.toAlgebraic()}"
    }
    
    /**
     * Export comprehensive analysis report
     */
    fun exportAnalysisReport(analysisResult: GameAnalysisResult, format: ReportFormat = ReportFormat.TEXT): String {
        return when (format) {
            ReportFormat.TEXT -> exportTextReport(analysisResult)
            ReportFormat.HTML -> exportHTMLReport(analysisResult)
            ReportFormat.JSON -> exportJSONReport(analysisResult)
        }
    }
    
    /**
     * Export analysis as text report
     */
    private fun exportTextReport(analysisResult: GameAnalysisResult): String {
        return analysisResult.generateReport()
    }
    
    /**
     * Export analysis as HTML report
     */
    private fun exportHTMLReport(analysisResult: GameAnalysisResult): String {
        val result = StringBuilder()
        
        result.appendLine("<!DOCTYPE html>")
        result.appendLine("<html>")
        result.appendLine("<head>")
        result.appendLine("    <title>Chess Game Analysis</title>")
        result.appendLine("    <style>")
        result.appendLine("        body { font-family: Arial, sans-serif; margin: 20px; }")
        result.appendLine("        .header { background-color: #f0f0f0; padding: 10px; border-radius: 5px; }")
        result.appendLine("        .section { margin: 20px 0; }")
        result.appendLine("        .move-quality-brilliant { color: #00aa00; font-weight: bold; }")
        result.appendLine("        .move-quality-excellent { color: #0066cc; font-weight: bold; }")
        result.appendLine("        .move-quality-blunder { color: #cc0000; font-weight: bold; }")
        result.appendLine("        table { border-collapse: collapse; width: 100%; }")
        result.appendLine("        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
        result.appendLine("        th { background-color: #f2f2f2; }")
        result.appendLine("    </style>")
        result.appendLine("</head>")
        result.appendLine("<body>")
        
        result.appendLine("    <div class=\"header\">")
        result.appendLine("        <h1>Chess Game Analysis</h1>")
        result.appendLine("        <p><strong>Game:</strong> ${analysisResult.game.getSummary()}</p>")
        result.appendLine("    </div>")
        
        // Game statistics
        result.appendLine("    <div class=\"section\">")
        result.appendLine("        <h2>Game Statistics</h2>")
        result.appendLine("        <table>")
        result.appendLine("            <tr><th>Statistic</th><th>Value</th></tr>")
        result.appendLine("            <tr><td>Total Moves</td><td>${analysisResult.gameStatistics.totalMoves}</td></tr>")
        result.appendLine("            <tr><td>Tactical Moves</td><td>${analysisResult.gameStatistics.tacticalMoves}</td></tr>")
        result.appendLine("            <tr><td>Captures</td><td>${analysisResult.gameStatistics.captures}</td></tr>")
        result.appendLine("            <tr><td>Checks</td><td>${analysisResult.gameStatistics.checks}</td></tr>")
        result.appendLine("        </table>")
        result.appendLine("    </div>")
        
        // Move analysis
        result.appendLine("    <div class=\"section\">")
        result.appendLine("        <h2>Move Analysis</h2>")
        result.appendLine("        <table>")
        result.appendLine("            <tr><th>Move #</th><th>Player</th><th>Move</th><th>Quality</th><th>Analysis</th></tr>")
        
        for ((index, analysis) in analysisResult.moveAnalyses.withIndex()) {
            val moveNumber = (index / 2) + 1
            val player = if (index % 2 == 0) "White" else "Black"
            val qualityClass = when (analysis.quality) {
                MoveQuality.BRILLIANT -> "move-quality-brilliant"
                MoveQuality.EXCELLENT -> "move-quality-excellent"
                MoveQuality.BLUNDER -> "move-quality-blunder"
                else -> ""
            }
            
            result.appendLine("            <tr>")
            result.appendLine("                <td>$moveNumber</td>")
            result.appendLine("                <td>$player</td>")
            result.appendLine("                <td>${analysis.move.toAlgebraic()}</td>")
            result.appendLine("                <td class=\"$qualityClass\">${analysis.quality.description}</td>")
            result.appendLine("                <td>${analysis.getSummary()}</td>")
            result.appendLine("            </tr>")
        }
        
        result.appendLine("        </table>")
        result.appendLine("    </div>")
        
        result.appendLine("</body>")
        result.appendLine("</html>")
        
        return result.toString()
    }
    
    /**
     * Export analysis as JSON report (simplified)
     */
    private fun exportJSONReport(analysisResult: GameAnalysisResult): String {
        val result = StringBuilder()
        
        result.appendLine("{")
        result.appendLine("  \"game\": {")
        result.appendLine("    \"white\": \"${analysisResult.game.getWhite()}\",")
        result.appendLine("    \"black\": \"${analysisResult.game.getBlack()}\",")
        result.appendLine("    \"event\": \"${analysisResult.game.getEvent()}\",")
        result.appendLine("    \"result\": \"${analysisResult.game.getResult()}\"")
        result.appendLine("  },")
        result.appendLine("  \"statistics\": {")
        result.appendLine("    \"totalMoves\": ${analysisResult.gameStatistics.totalMoves},")
        result.appendLine("    \"tacticalMoves\": ${analysisResult.gameStatistics.tacticalMoves},")
        result.appendLine("    \"captures\": ${analysisResult.gameStatistics.captures},")
        result.appendLine("    \"checks\": ${analysisResult.gameStatistics.checks}")
        result.appendLine("  },")
        result.appendLine("  \"moveAnalyses\": [")
        
        for ((index, analysis) in analysisResult.moveAnalyses.withIndex()) {
            result.appendLine("    {")
            result.appendLine("      \"moveNumber\": ${index + 1},")
            result.appendLine("      \"move\": \"${analysis.move.toAlgebraic()}\",")
            result.appendLine("      \"quality\": \"${analysis.quality.name}\",")
            result.appendLine("      \"materialChange\": ${analysis.materialChange},")
            result.appendLine("      \"tacticalElements\": [${analysis.tacticalElements.joinToString(",") { "\"${it.name}\"" }}]")
            result.append("    }")
            if (index < analysisResult.moveAnalyses.size - 1) {
                result.append(",")
            }
            result.appendLine()
        }
        
        result.appendLine("  ]")
        result.appendLine("}")
        
        return result.toString()
    }
    
    /**
     * Export game positions as chess diagrams (ASCII)
     */
    fun exportPositionDiagrams(game: PGNGame, includeEveryNthMove: Int = 5): PositionDiagramsExport {
        val diagrams = mutableListOf<PositionDiagram>()
        val board = ChessBoard()
        
        // Starting position
        diagrams.add(PositionDiagram(
            moveNumber = 0,
            description = "Starting position",
            fen = board.toFEN(),
            diagram = board.toASCII()
        ))
        
        for ((index, move) in game.moves.withIndex()) {
            board.makeMove(move)
            board.switchActiveColor()
            
            if ((index + 1) % includeEveryNthMove == 0 || index == game.moves.size - 1) {
                val moveNumber = index + 1
                val description = "After move $moveNumber: ${move.toAlgebraic()}"
                
                diagrams.add(PositionDiagram(
                    moveNumber = moveNumber,
                    description = description,
                    fen = board.toFEN(),
                    diagram = board.toASCII()
                ))
            }
        }
        
        return PositionDiagramsExport(
            gameInfo = game.getSummary(),
            diagrams = diagrams
        )
    }
    
    /**
     * Export game in multiple formats
     */
    fun exportMultiFormat(
        game: PGNGame,
        analysisResult: GameAnalysisResult? = null,
        formats: Set<ExportFormat> = setOf(ExportFormat.PGN, ExportFormat.FEN_SEQUENCE)
    ): MultiFormatExport {
        val exports = mutableMapOf<ExportFormat, String>()
        
        for (format in formats) {
            val export = when (format) {
                ExportFormat.PGN -> exportToPGN(game, analysisResult != null, analysisResult)
                ExportFormat.PGN_WITH_ANALYSIS -> exportToPGN(game, true, analysisResult)
                ExportFormat.FEN_SEQUENCE -> exportToFENSequence(game).toString()
                ExportFormat.MOVE_LIST_ALGEBRAIC -> exportMoveList(game, MoveNotation.ALGEBRAIC).toString()
                ExportFormat.MOVE_LIST_COORDINATE -> exportMoveList(game, MoveNotation.COORDINATE).toString()
                ExportFormat.ANALYSIS_TEXT -> analysisResult?.generateReport() ?: "No analysis available"
                ExportFormat.ANALYSIS_HTML -> if (analysisResult != null) exportHTMLReport(analysisResult) else "No analysis available"
                ExportFormat.ANALYSIS_JSON -> if (analysisResult != null) exportJSONReport(analysisResult) else "{\"error\": \"No analysis available\"}"
                ExportFormat.POSITION_DIAGRAMS -> exportPositionDiagrams(game).toString()
            }
            exports[format] = export
        }
        
        return MultiFormatExport(
            gameInfo = game.getSummary(),
            exports = exports
        )
    }
}

/**
 * Move notation types
 */
enum class MoveNotation {
    ALGEBRAIC,      // Nf3, e4, O-O
    COORDINATE,     // e2e4, g1f3
    LONG_ALGEBRAIC, // Ng1-f3, e2-e4
    DESCRIPTIVE     // N-KB3, P-K4 (simplified)
}

/**
 * Report formats
 */
enum class ReportFormat {
    TEXT,
    HTML,
    JSON
}

/**
 * Export formats
 */
enum class ExportFormat {
    PGN,
    PGN_WITH_ANALYSIS,
    FEN_SEQUENCE,
    MOVE_LIST_ALGEBRAIC,
    MOVE_LIST_COORDINATE,
    ANALYSIS_TEXT,
    ANALYSIS_HTML,
    ANALYSIS_JSON,
    POSITION_DIAGRAMS
}

/**
 * FEN sequence export result
 */
data class FENSequenceExport(
    val gameInfo: String,
    val startingPosition: String,
    val positions: List<String>,
    val moveCount: Int
) {
    override fun toString(): String {
        val result = StringBuilder()
        result.appendLine("=== FEN Sequence Export ===")
        result.appendLine("Game: $gameInfo")
        result.appendLine("Moves: $moveCount")
        result.appendLine()
        
        for ((index, fen) in positions.withIndex()) {
            result.appendLine("Position $index: $fen")
        }
        
        return result.toString()
    }
}

/**
 * Move list export result
 */
data class MoveListExport(
    val gameInfo: String,
    val notation: MoveNotation,
    val moves: List<String>
) {
    override fun toString(): String {
        val result = StringBuilder()
        result.appendLine("=== Move List Export (${notation.name}) ===")
        result.appendLine("Game: $gameInfo")
        result.appendLine()
        
        for ((index, move) in moves.withIndex()) {
            val moveNumber = (index / 2) + 1
            if (index % 2 == 0) {
                result.append("$moveNumber. $move ")
            } else {
                result.appendLine(move)
            }
        }
        
        return result.toString()
    }
}

/**
 * Position diagram for export
 */
data class PositionDiagram(
    val moveNumber: Int,
    val description: String,
    val fen: String,
    val diagram: String
)

/**
 * Position diagrams export result
 */
data class PositionDiagramsExport(
    val gameInfo: String,
    val diagrams: List<PositionDiagram>
) {
    override fun toString(): String {
        val result = StringBuilder()
        result.appendLine("=== Position Diagrams Export ===")
        result.appendLine("Game: $gameInfo")
        result.appendLine()
        
        for (diagram in diagrams) {
            result.appendLine("=== ${diagram.description} ===")
            result.appendLine("FEN: ${diagram.fen}")
            result.appendLine()
            result.appendLine(diagram.diagram)
            result.appendLine()
        }
        
        return result.toString()
    }
}

/**
 * Multi-format export result
 */
data class MultiFormatExport(
    val gameInfo: String,
    val exports: Map<ExportFormat, String>
) {
    /**
     * Get export for specific format
     */
    fun getExport(format: ExportFormat): String? = exports[format]
    
    /**
     * Get all available formats
     */
    fun getAvailableFormats(): Set<ExportFormat> = exports.keys
    
    /**
     * Save exports to files (would need file system access)
     */
    fun getFileExtensions(): Map<ExportFormat, String> = mapOf(
        ExportFormat.PGN to "pgn",
        ExportFormat.PGN_WITH_ANALYSIS to "pgn",
        ExportFormat.FEN_SEQUENCE to "fen",
        ExportFormat.MOVE_LIST_ALGEBRAIC to "txt",
        ExportFormat.MOVE_LIST_COORDINATE to "txt",
        ExportFormat.ANALYSIS_TEXT to "txt",
        ExportFormat.ANALYSIS_HTML to "html",
        ExportFormat.ANALYSIS_JSON to "json",
        ExportFormat.POSITION_DIAGRAMS to "txt"
    )
}
