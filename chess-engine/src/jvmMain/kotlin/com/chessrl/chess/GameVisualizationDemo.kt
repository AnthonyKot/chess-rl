package com.chessrl.chess

/**
 * Demo showcasing the comprehensive game visualization and replay tools
 */

fun main() {
    println("=== Chess Game Visualization and Replay Tools Demo ===")
    println()
    
    // Create a sample game
    val sampleGame = createSampleGame()
    
    // Demo 1: Game Visualization
    println("1. GAME VISUALIZATION DEMO")
    println("=" * 50)
    demoGameVisualization(sampleGame)
    
    println("\n" + "=" * 80 + "\n")
    
    // Demo 2: Game Analysis
    println("2. GAME ANALYSIS DEMO")
    println("=" * 50)
    demoGameAnalysis(sampleGame)
    
    println("\n" + "=" * 80 + "\n")
    
    // Demo 3: Game Export
    println("3. GAME EXPORT DEMO")
    println("=" * 50)
    demoGameExport(sampleGame)
    
    println("\n" + "=" * 80 + "\n")
    
    // Demo 4: Interactive Browser
    println("4. INTERACTIVE BROWSER DEMO")
    println("=" * 50)
    demoInteractiveBrowser(sampleGame)
    
    println("\n" + "=" * 80 + "\n")
    
    // Demo 5: Multiple Games
    println("5. MULTIPLE GAMES DEMO")
    println("=" * 50)
    demoMultipleGames()
}

/**
 * Create a sample game for demonstration
 */
fun createSampleGame(): PGNGame {
    val headers = mapOf(
        "Event" to "Demo Tournament",
        "Site" to "Demo City",
        "Date" to "2024.01.15",
        "Round" to "1",
        "White" to "Alice",
        "Black" to "Bob",
        "Result" to "1-0"
    )
    
    // Scholar's Mate game
    val moves = listOf(
        Move.fromAlgebraic("e2e4")!!,  // 1. e4
        Move.fromAlgebraic("e7e5")!!,  // 1... e5
        Move.fromAlgebraic("f1c4")!!,  // 2. Bc4
        Move.fromAlgebraic("b8c6")!!,  // 2... Nc6
        Move.fromAlgebraic("d1h5")!!,  // 3. Qh5 (dubious early queen move)
        Move.fromAlgebraic("g8f6")!!,  // 3... Nf6?? (blunder)
        Move.fromAlgebraic("h5f7")!!   // 4. Qxf7# (checkmate)
    )
    
    return PGNGame(headers, moves, "1. e4 e5 2. Bc4 Nc6 3. Qh5 Nf6?? 4. Qxf7# 1-0")
}

/**
 * Demo game visualization features
 */
fun demoGameVisualization(game: PGNGame) {
    println("Creating game visualizer...")
    val visualizer = GameVisualizer(game)
    
    println("Game: ${game.getSummary()}")
    println()
    
    // Show starting position
    println("Starting position:")
    val startViz = visualizer.getCurrentVisualization()
    println(startViz.renderCompact())
    
    println("\nNavigating through the game...")
    
    // Step through first few moves
    for (i in 1..3) {
        val result = visualizer.next()
        if (result.success) {
            println("\n${result.message}")
            println(result.visualization.renderCompact())
        }
    }
    
    // Jump to end
    println("\nJumping to final position...")
    val endResult = visualizer.goToMove(game.moves.size)
    if (endResult.success) {
        println(endResult.message)
        println(endResult.visualization.render())
    }
    
    // Show detailed analysis
    println("\nDetailed position analysis:")
    println(visualizer.getDetailedAnalysis())
}

/**
 * Demo game analysis features
 */
fun demoGameAnalysis(game: PGNGame) {
    println("Analyzing game...")
    val analyzer = GameAnalyzer()
    val analysisResult = analyzer.analyzeGame(game)
    
    println("Analysis complete!")
    println()
    
    // Show game statistics
    println("GAME STATISTICS:")
    val stats = analysisResult.gameStatistics
    println("Total moves: ${stats.totalMoves}")
    println("Tactical moves: ${stats.tacticalMoves}")
    println("Captures: ${stats.captures}")
    println("Checks: ${stats.checks}")
    println()
    
    // Show player statistics
    println("WHITE PLAYER STATISTICS:")
    println(stats.whiteStatistics.getReport())
    
    println("BLACK PLAYER STATISTICS:")
    println(stats.blackStatistics.getReport())
    
    // Show key moves
    println("KEY MOVES:")
    val keyMoves = analysisResult.moveAnalyses.filter { 
        it.quality in listOf(MoveQuality.BRILLIANT, MoveQuality.EXCELLENT, MoveQuality.BLUNDER) 
    }
    
    for (analysis in keyMoves) {
        val moveNumber = analysisResult.moveAnalyses.indexOf(analysis) + 1
        val colorStr = if (analysis.isWhiteMove) "White" else "Black"
        println("$moveNumber. $colorStr: ${analysis.getSummary()}")
    }
    
    // Show tactical elements
    println("\nTACTICAL ELEMENTS FOUND:")
    val tacticalMoves = analysisResult.moveAnalyses.filter { it.tacticalElements.isNotEmpty() }
    for (analysis in tacticalMoves) {
        val moveNumber = analysisResult.moveAnalyses.indexOf(analysis) + 1
        val elements = analysis.tacticalElements.joinToString(", ") { it.name.lowercase().replace("_", " ") }
        println("Move $moveNumber: ${analysis.move.toAlgebraic()} - $elements")
    }
}

/**
 * Demo game export features
 */
fun demoGameExport(game: PGNGame) {
    val exporter = GameExporter()
    val analyzer = GameAnalyzer()
    val analysisResult = analyzer.analyzeGame(game)
    
    println("Exporting game in various formats...")
    println()
    
    // PGN Export
    println("1. PGN EXPORT:")
    val pgn = exporter.exportToPGN(game)
    println(pgn.take(300) + if (pgn.length > 300) "..." else "")
    println()
    
    // PGN with Analysis
    println("2. PGN WITH ANALYSIS:")
    val pgnWithAnalysis = exporter.exportToPGN(game, true, analysisResult)
    println(pgnWithAnalysis.take(400) + if (pgnWithAnalysis.length > 400) "..." else "")
    println()
    
    // FEN Sequence
    println("3. FEN SEQUENCE EXPORT:")
    val fenExport = exporter.exportToFENSequence(game)
    println("Game: ${fenExport.gameInfo}")
    println("Positions: ${fenExport.positions.size}")
    println("First 3 positions:")
    fenExport.positions.take(3).forEachIndexed { index, fen ->
        println("  $index: $fen")
    }
    println()
    
    // Move List
    println("4. MOVE LIST EXPORT (Algebraic):")
    val moveList = exporter.exportMoveList(game, MoveNotation.ALGEBRAIC)
    println("Moves: ${moveList.moves.joinToString(" ")}")
    println()
    
    // Analysis Report
    println("5. ANALYSIS REPORT (Text):")
    val textReport = exporter.exportAnalysisReport(analysisResult, ReportFormat.TEXT)
    println(textReport.take(500) + if (textReport.length > 500) "..." else "")
    println()
    
    // Position Diagrams
    println("6. POSITION DIAGRAMS:")
    val diagrams = exporter.exportPositionDiagrams(game, 3)
    println("Generated ${diagrams.diagrams.size} diagrams")
    println("First diagram:")
    println(diagrams.diagrams.first().diagram.take(200) + "...")
    println()
    
    // Multi-format Export
    println("7. MULTI-FORMAT EXPORT:")
    val formats = setOf(ExportFormat.PGN, ExportFormat.FEN_SEQUENCE, ExportFormat.ANALYSIS_JSON)
    val multiExport = exporter.exportMultiFormat(game, analysisResult, formats)
    println("Available formats: ${multiExport.getAvailableFormats().joinToString(", ")}")
    println("JSON analysis preview:")
    val jsonExport = multiExport.getExport(ExportFormat.ANALYSIS_JSON)
    println(jsonExport?.take(200) + "...")
}

/**
 * Demo interactive browser features
 */
fun demoInteractiveBrowser(game: PGNGame) {
    println("Creating interactive game browser...")
    val browser = InteractiveGameBrowser()
    browser.loadGame(game)
    
    println("Game loaded: ${game.getSummary()}")
    println()
    
    // Simulate various commands
    val commands = listOf(
        "show",
        "next",
        "next", 
        "analyze",
        "material",
        "tactics",
        "goto 5",
        "annotate Blunder by Black!",
        "quality blunder",
        "export pgn",
        "moves",
        "game"
    )
    
    println("Simulating interactive commands:")
    for (command in commands) {
        println("\n> $command")
        val response = browser.processCommand(command)
        
        if (response.success) {
            // Show abbreviated response
            val message = response.message
            if (message.length > 200) {
                println(message.take(200) + "...")
            } else {
                println(message)
            }
            
            // Show visualization if available (abbreviated)
            response.visualization?.let { viz ->
                if (command == "show" || command.startsWith("goto") || command == "next") {
                    println("\nBoard position:")
                    val rendered = viz.renderCompact()
                    println(rendered.take(300) + if (rendered.length > 300) "..." else "")
                }
            }
        } else {
            println("Error: ${response.message}")
        }
    }
    
    println("\nBrowser demo complete!")
}

/**
 * Demo multiple games functionality
 */
fun demoMultipleGames() {
    println("Creating multiple games for database demo...")
    
    val games = listOf(
        createSampleGame(),
        createGame2(),
        createGame3()
    )
    
    val browser = InteractiveGameBrowser()
    browser.loadGames(games)
    
    println("Loaded ${games.size} games into browser")
    println()
    
    // Demo database commands
    val commands = listOf(
        "load",
        "search Alice",
        "load 2",
        "game",
        "load 3",
        "export fen"
    )
    
    println("Simulating database commands:")
    for (command in commands) {
        println("\n> $command")
        val response = browser.processCommand(command)
        
        if (response.success) {
            val message = response.message
            if (message.length > 300) {
                println(message.take(300) + "...")
            } else {
                println(message)
            }
        } else {
            println("Error: ${response.message}")
        }
    }
}

/**
 * Create additional sample games
 */
fun createGame2(): PGNGame {
    val headers = mapOf(
        "Event" to "Demo Tournament",
        "Site" to "Demo City", 
        "Date" to "2024.01.16",
        "White" to "Charlie",
        "Black" to "Diana",
        "Result" to "0-1"
    )
    
    val moves = listOf(
        Move.fromAlgebraic("d2d4")!!,
        Move.fromAlgebraic("g8f6")!!,
        Move.fromAlgebraic("c2c4")!!,
        Move.fromAlgebraic("e7e6")!!
    )
    
    return PGNGame(headers, moves, "1. d4 Nf6 2. c4 e6")
}

fun createGame3(): PGNGame {
    val headers = mapOf(
        "Event" to "Demo Tournament",
        "Site" to "Demo City",
        "Date" to "2024.01.17", 
        "White" to "Eve",
        "Black" to "Frank",
        "Result" to "1/2-1/2"
    )
    
    val moves = listOf(
        Move.fromAlgebraic("e2e4")!!,
        Move.fromAlgebraic("c7c5")!!,
        Move.fromAlgebraic("g1f3")!!,
        Move.fromAlgebraic("d7d6")!!
    )
    
    return PGNGame(headers, moves, "1. e4 c5 2. Nf3 d6")
}

/**
 * String repeat extension for formatting
 */
private operator fun String.times(n: Int): String = this.repeat(n)
