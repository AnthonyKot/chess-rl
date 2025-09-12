package com.chessrl.chess

/**
 * Interactive game browser for manual inspection of played games
 * Provides a console-based interface for navigating through games
 */

/**
 * Interactive game browser with navigation and analysis capabilities
 */
class InteractiveGameBrowser {
    private var currentGame: PGNGame? = null
    private var gameVisualizer: GameVisualizer? = null
    private var gameAnalyzer: GameAnalyzer? = null
    private var analysisResult: GameAnalysisResult? = null
    private val gameDatabase = mutableListOf<PGNGame>()
    private var currentGameIndex = 0
    
    /**
     * Load a single game
     */
    fun loadGame(game: PGNGame) {
        currentGame = game
        gameVisualizer = GameVisualizer(game)
        gameAnalyzer = GameAnalyzer()
        analysisResult = null
        println("Loaded game: ${game.getSummary()}")
    }
    
    /**
     * Load multiple games
     */
    fun loadGames(games: List<PGNGame>) {
        gameDatabase.clear()
        gameDatabase.addAll(games)
        currentGameIndex = 0
        
        if (games.isNotEmpty()) {
            loadGame(games[0])
            println("Loaded ${games.size} games. Current game: 1/${games.size}")
        }
    }
    
    /**
     * Start interactive browsing session
     */
    fun startInteractiveSession() {
        if (currentGame == null) {
            println("No game loaded. Please load a game first.")
            return
        }
        
        println("=== Interactive Chess Game Browser ===")
        println("Type 'help' for available commands")
        println()
        
        showCurrentPosition()
        
        // Simulate interactive loop (in real implementation, this would read from console)
        // For now, we'll provide the interface methods that could be called
    }
    
    /**
     * Process user command
     */
    fun processCommand(command: String): BrowserResponse {
        val parts = command.trim().lowercase().split(" ")
        val cmd = parts[0]
        
        return when (cmd) {
            "help", "h" -> showHelp()
            "next", "n" -> nextMove()
            "prev", "previous", "p" -> previousMove()
            "goto", "g" -> {
                val moveNumber = parts.getOrNull(1)?.toIntOrNull()
                if (moveNumber != null) {
                    goToMove(moveNumber)
                } else {
                    BrowserResponse.error("Invalid move number. Usage: goto <number>")
                }
            }
            "start", "s" -> goToStart()
            "end", "e" -> goToEnd()
            "show", "display", "d" -> showCurrentPosition()
            "analyze", "a" -> analyzeCurrentPosition()
            "game", "gameinfo" -> showGameInfo()
            "moves", "movelist" -> showMoveList()
            "export" -> {
                val format = parts.getOrNull(1) ?: "pgn"
                exportGame(format)
            }
            "load" -> {
                val gameNumber = parts.getOrNull(1)?.toIntOrNull()
                if (gameNumber != null) {
                    loadGameFromDatabase(gameNumber)
                } else {
                    showGameDatabase()
                }
            }
            "search" -> {
                val query = parts.drop(1).joinToString(" ")
                searchGames(query)
            }
            "annotate" -> {
                val annotation = parts.drop(1).joinToString(" ")
                annotateCurrentMove(annotation)
            }
            "quality" -> {
                val quality = parts.getOrNull(1)
                setMoveQuality(quality)
            }
            "variations" -> showVariations()
            "tactics" -> showTacticalElements()
            "position" -> showPositionAnalysis()
            "material" -> showMaterialBalance()
            "threats" -> showThreats()
            "quit", "exit", "q" -> BrowserResponse.success("Goodbye!", shouldExit = true)
            else -> BrowserResponse.error("Unknown command: $cmd. Type 'help' for available commands.")
        }
    }
    
    /**
     * Show help information
     */
    private fun showHelp(): BrowserResponse {
        val help = """
        === Interactive Game Browser Commands ===
        
        Navigation:
          next, n              - Move to next position
          prev, p              - Move to previous position  
          goto <number>, g     - Go to specific move number
          start, s             - Go to starting position
          end, e               - Go to final position
        
        Display:
          show, d              - Show current position
          game                 - Show game information
          moves                - Show move list
          position             - Show detailed position analysis
          material             - Show material balance
          threats              - Show current threats
        
        Analysis:
          analyze, a           - Analyze current position
          tactics              - Show tactical elements
          variations           - Show variations (if available)
        
        Annotation:
          annotate <text>      - Add annotation to current move
          quality <quality>    - Set move quality (brilliant, excellent, good, neutral, dubious, mistake, blunder)
        
        Game Management:
          load [number]        - Load game from database or show game list
          search <query>       - Search games in database
          export [format]      - Export current game (pgn, fen, analysis)
        
        Other:
          help, h              - Show this help
          quit, q              - Exit browser
        """.trimIndent()
        
        return BrowserResponse.success(help)
    }
    
    /**
     * Move to next position
     */
    private fun nextMove(): BrowserResponse {
        val visualizer = gameVisualizer ?: return BrowserResponse.error("No game loaded")
        
        val result = visualizer.next()
        return if (result.success) {
            BrowserResponse.success(result.message, visualization = result.visualization)
        } else {
            BrowserResponse.error(result.message)
        }
    }
    
    /**
     * Move to previous position
     */
    private fun previousMove(): BrowserResponse {
        val visualizer = gameVisualizer ?: return BrowserResponse.error("No game loaded")
        
        val result = visualizer.previous()
        return if (result.success) {
            BrowserResponse.success(result.message, visualization = result.visualization)
        } else {
            BrowserResponse.error(result.message)
        }
    }
    
    /**
     * Go to specific move
     */
    private fun goToMove(moveNumber: Int): BrowserResponse {
        val visualizer = gameVisualizer ?: return BrowserResponse.error("No game loaded")
        
        val result = visualizer.goToMove(moveNumber)
        return if (result.success) {
            BrowserResponse.success(result.message, visualization = result.visualization)
        } else {
            BrowserResponse.error(result.message)
        }
    }
    
    /**
     * Go to start of game
     */
    private fun goToStart(): BrowserResponse {
        val visualizer = gameVisualizer ?: return BrowserResponse.error("No game loaded")
        
        val result = visualizer.goToMove(0)
        return BrowserResponse.success("At starting position", visualization = result.visualization)
    }
    
    /**
     * Go to end of game
     */
    private fun goToEnd(): BrowserResponse {
        val visualizer = gameVisualizer ?: return BrowserResponse.error("No game loaded")
        val game = currentGame ?: return BrowserResponse.error("No game loaded")
        
        val result = visualizer.goToMove(game.moves.size)
        return BrowserResponse.success("At final position", visualization = result.visualization)
    }
    
    /**
     * Show current position
     */
    private fun showCurrentPosition(): BrowserResponse {
        val visualizer = gameVisualizer ?: return BrowserResponse.error("No game loaded")
        
        val visualization = visualizer.getCurrentVisualization()
        return BrowserResponse.success("Current position", visualization = visualization)
    }
    
    /**
     * Analyze current position
     */
    private fun analyzeCurrentPosition(): BrowserResponse {
        val game = currentGame ?: return BrowserResponse.error("No game loaded")
        val analyzer = gameAnalyzer ?: return BrowserResponse.error("No analyzer available")
        
        if (analysisResult == null) {
            println("Analyzing game... This may take a moment.")
            analysisResult = analyzer.analyzeGame(game)
        }
        
        val visualizer = gameVisualizer ?: return BrowserResponse.error("No visualizer available")
        val analysis = visualizer.getDetailedAnalysis()
        
        return BrowserResponse.success(analysis)
    }
    
    /**
     * Show game information
     */
    private fun showGameInfo(): BrowserResponse {
        val game = currentGame ?: return BrowserResponse.error("No game loaded")
        
        val info = """
        === Game Information ===
        Event: ${game.getEvent()}
        Site: ${game.getSite()}
        Date: ${game.getDate()}
        Round: ${game.getRound()}
        White: ${game.getWhite()}
        Black: ${game.getBlack()}
        Result: ${game.getResult()}
        
        Total moves: ${game.moves.size}
        """.trimIndent()
        
        return BrowserResponse.success(info)
    }
    
    /**
     * Show move list
     */
    private fun showMoveList(): BrowserResponse {
        val game = currentGame ?: return BrowserResponse.error("No game loaded")
        
        val moveList = StringBuilder()
        moveList.appendLine("=== Move List ===")
        
        for (i in game.moves.indices step 2) {
            val moveNumber = (i / 2) + 1
            val whiteMove = game.moves[i].toAlgebraic()
            val blackMove = if (i + 1 < game.moves.size) game.moves[i + 1].toAlgebraic() else ""
            
            moveList.appendLine("$moveNumber. $whiteMove $blackMove")
        }
        
        return BrowserResponse.success(moveList.toString())
    }
    
    /**
     * Export current game
     */
    private fun exportGame(format: String): BrowserResponse {
        val game = currentGame ?: return BrowserResponse.error("No game loaded")
        val exporter = GameExporter()
        
        val export = when (format.lowercase()) {
            "pgn" -> exporter.exportToPGN(game)
            "fen" -> exporter.exportToFENSequence(game).toString()
            "analysis" -> {
                if (analysisResult == null) {
                    val analyzer = GameAnalyzer()
                    analysisResult = analyzer.analyzeGame(game)
                }
                analysisResult!!.generateReport()
            }
            "html" -> {
                if (analysisResult == null) {
                    val analyzer = GameAnalyzer()
                    analysisResult = analyzer.analyzeGame(game)
                }
                exporter.exportAnalysisReport(analysisResult!!, ReportFormat.HTML)
            }
            else -> return BrowserResponse.error("Unknown export format: $format. Available: pgn, fen, analysis, html")
        }
        
        return BrowserResponse.success("Exported game in $format format:\n\n$export")
    }
    
    /**
     * Load game from database
     */
    private fun loadGameFromDatabase(gameNumber: Int): BrowserResponse {
        if (gameDatabase.isEmpty()) {
            return BrowserResponse.error("No games in database")
        }
        
        if (gameNumber < 1 || gameNumber > gameDatabase.size) {
            return BrowserResponse.error("Invalid game number. Available: 1-${gameDatabase.size}")
        }
        
        currentGameIndex = gameNumber - 1
        loadGame(gameDatabase[currentGameIndex])
        
        return BrowserResponse.success("Loaded game $gameNumber: ${currentGame!!.getSummary()}")
    }
    
    /**
     * Show game database
     */
    private fun showGameDatabase(): BrowserResponse {
        if (gameDatabase.isEmpty()) {
            return BrowserResponse.error("No games in database")
        }
        
        val list = StringBuilder()
        list.appendLine("=== Game Database ===")
        list.appendLine("Total games: ${gameDatabase.size}")
        list.appendLine()
        
        for ((index, game) in gameDatabase.withIndex()) {
            val marker = if (index == currentGameIndex) "* " else "  "
            list.appendLine("$marker${index + 1}. ${game.getSummary()}")
        }
        
        return BrowserResponse.success(list.toString())
    }
    
    /**
     * Search games in database
     */
    private fun searchGames(query: String): BrowserResponse {
        if (gameDatabase.isEmpty()) {
            return BrowserResponse.error("No games in database")
        }
        
        if (query.isBlank()) {
            return BrowserResponse.error("Please provide a search query")
        }
        
        val results = gameDatabase.mapIndexedNotNull { index, game ->
            val summary = game.getSummary().lowercase()
            if (summary.contains(query.lowercase())) {
                "${index + 1}. ${game.getSummary()}"
            } else null
        }
        
        return if (results.isEmpty()) {
            BrowserResponse.error("No games found matching '$query'")
        } else {
            val resultText = "=== Search Results for '$query' ===\n" + results.joinToString("\n")
            BrowserResponse.success(resultText)
        }
    }
    
    /**
     * Annotate current move
     */
    private fun annotateCurrentMove(annotation: String): BrowserResponse {
        val visualizer = gameVisualizer ?: return BrowserResponse.error("No game loaded")
        
        if (annotation.isBlank()) {
            return BrowserResponse.error("Please provide annotation text")
        }
        
        val currentVisualization = visualizer.getCurrentVisualization()
        val moveAnnotation = MoveAnnotation(
            text = annotation,
            quality = MoveQuality.NEUTRAL
        )
        
        visualizer.addMoveAnnotation(currentVisualization.moveNumber, moveAnnotation)
        
        return BrowserResponse.success("Added annotation to move ${currentVisualization.moveNumber}: $annotation")
    }
    
    /**
     * Set move quality
     */
    private fun setMoveQuality(qualityStr: String?): BrowserResponse {
        val visualizer = gameVisualizer ?: return BrowserResponse.error("No game loaded")
        
        val quality = when (qualityStr?.lowercase()) {
            "brilliant" -> MoveQuality.BRILLIANT
            "excellent" -> MoveQuality.EXCELLENT
            "good" -> MoveQuality.GOOD
            "neutral" -> MoveQuality.NEUTRAL
            "dubious" -> MoveQuality.DUBIOUS
            "mistake" -> MoveQuality.MISTAKE
            "blunder" -> MoveQuality.BLUNDER
            else -> return BrowserResponse.error("Invalid quality. Available: brilliant, excellent, good, neutral, dubious, mistake, blunder")
        }
        
        val currentVisualization = visualizer.getCurrentVisualization()
        val existingAnnotation = visualizer.getMoveAnnotation(currentVisualization.moveNumber)
        val newAnnotation = MoveAnnotation(
            text = existingAnnotation?.text ?: "",
            quality = quality
        )
        
        visualizer.addMoveAnnotation(currentVisualization.moveNumber, newAnnotation)
        
        return BrowserResponse.success("Set move ${currentVisualization.moveNumber} quality to ${quality.description}")
    }
    
    /**
     * Show variations (placeholder)
     */
    private fun showVariations(): BrowserResponse {
        return BrowserResponse.success("Variations feature not yet implemented")
    }
    
    /**
     * Show tactical elements
     */
    private fun showTacticalElements(): BrowserResponse {
        if (analysisResult == null) {
            val game = currentGame ?: return BrowserResponse.error("No game loaded")
            val analyzer = GameAnalyzer()
            analysisResult = analyzer.analyzeGame(game)
        }
        
        val visualizer = gameVisualizer ?: return BrowserResponse.error("No visualizer available")
        val currentVisualization = visualizer.getCurrentVisualization()
        
        if (currentVisualization.moveNumber == 0) {
            return BrowserResponse.success("No tactical elements in starting position")
        }
        
        val moveAnalysis = analysisResult!!.moveAnalyses[currentVisualization.moveNumber - 1]
        
        val tactics = StringBuilder()
        tactics.appendLine("=== Tactical Elements ===")
        tactics.appendLine("Move: ${moveAnalysis.move.toAlgebraic()}")
        tactics.appendLine("Quality: ${moveAnalysis.quality.description}")
        
        if (moveAnalysis.tacticalElements.isNotEmpty()) {
            tactics.appendLine("Tactical elements:")
            for (element in moveAnalysis.tacticalElements) {
                tactics.appendLine("  - ${element.name.lowercase().replace("_", " ")}")
            }
        } else {
            tactics.appendLine("No special tactical elements")
        }
        
        if (moveAnalysis.createsThreats.isNotEmpty()) {
            tactics.appendLine("Creates threats:")
            for (threat in moveAnalysis.createsThreats) {
                tactics.appendLine("  - $threat")
            }
        }
        
        return BrowserResponse.success(tactics.toString())
    }
    
    /**
     * Show position analysis
     */
    private fun showPositionAnalysis(): BrowserResponse {
        val visualizer = gameVisualizer ?: return BrowserResponse.error("No game loaded")
        val analysis = visualizer.getDetailedAnalysis()
        return BrowserResponse.success(analysis)
    }
    
    /**
     * Show material balance
     */
    private fun showMaterialBalance(): BrowserResponse {
        val visualizer = gameVisualizer ?: return BrowserResponse.error("No game loaded")
        val board = visualizer.getCurrentVisualization().board
        
        val whitePieces = board.getPiecesOfColor(PieceColor.WHITE)
        val blackPieces = board.getPiecesOfColor(PieceColor.BLACK)
        
        val whiteMaterial = calculateMaterial(whitePieces.map { it.second })
        val blackMaterial = calculateMaterial(blackPieces.map { it.second })
        
        val balance = StringBuilder()
        balance.appendLine("=== Material Balance ===")
        balance.appendLine("White: $whiteMaterial points (${whitePieces.size} pieces)")
        balance.appendLine("Black: $blackMaterial points (${blackPieces.size} pieces)")
        balance.appendLine("Balance: ${if (whiteMaterial > blackMaterial) "White +" else if (blackMaterial > whiteMaterial) "Black +" else "Equal"}${kotlin.math.abs(whiteMaterial - blackMaterial)}")
        
        return BrowserResponse.success(balance.toString())
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
     * Show current threats
     */
    private fun showThreats(): BrowserResponse {
        val visualizer = gameVisualizer ?: return BrowserResponse.error("No game loaded")
        val board = visualizer.getCurrentVisualization().board
        
        val threats = StringBuilder()
        threats.appendLine("=== Current Threats ===")
        
        val whiteInCheck = board.isInCheck(PieceColor.WHITE)
        val blackInCheck = board.isInCheck(PieceColor.BLACK)
        
        if (whiteInCheck) {
            threats.appendLine("White king is in check!")
        }
        
        if (blackInCheck) {
            threats.appendLine("Black king is in check!")
        }
        
        if (!whiteInCheck && !blackInCheck) {
            threats.appendLine("No immediate checks")
        }
        
        // Add more sophisticated threat detection here
        
        return BrowserResponse.success(threats.toString())
    }
}

/**
 * Browser response for command processing
 */
data class BrowserResponse(
    val success: Boolean,
    val message: String,
    val visualization: GameVisualization? = null,
    val shouldExit: Boolean = false
) {
    companion object {
        fun success(message: String, visualization: GameVisualization? = null, shouldExit: Boolean = false): BrowserResponse {
            return BrowserResponse(true, message, visualization, shouldExit)
        }
        
        fun error(message: String): BrowserResponse {
            return BrowserResponse(false, message)
        }
    }
    
    /**
     * Display the response
     */
    fun display() {
        if (success) {
            println(message)
            visualization?.let { 
                println()
                println(it.render()) 
            }
        } else {
            println("Error: $message")
        }
    }
}

/**
 * Game browser session manager
 */
class GameBrowserSession {
    private val browser = InteractiveGameBrowser()
    private var isRunning = false
    
    /**
     * Start a browsing session with a single game
     */
    fun startSession(game: PGNGame) {
        browser.loadGame(game)
        runSession()
    }
    
    /**
     * Start a browsing session with multiple games
     */
    fun startSession(games: List<PGNGame>) {
        browser.loadGames(games)
        runSession()
    }
    
    /**
     * Run the interactive session
     */
    private fun runSession() {
        isRunning = true
        browser.startInteractiveSession()
        
        // In a real implementation, this would read from console input
        // For now, we provide the interface for command processing
        println("Session started. Use processCommand() to interact with the browser.")
    }
    
    /**
     * Process a command and return the response
     */
    fun processCommand(command: String): BrowserResponse {
        val response = browser.processCommand(command)
        
        if (response.shouldExit) {
            isRunning = false
        }
        
        return response
    }
    
    /**
     * Check if session is running
     */
    fun isRunning(): Boolean = isRunning
    
    /**
     * Stop the session
     */
    fun stop() {
        isRunning = false
        println("Browser session ended.")
    }
}