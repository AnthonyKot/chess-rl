package com.chessrl.chess

import kotlin.test.*

/**
 * Tests for game visualization and replay tools
 */
class GameVisualizationTest {
    
    private fun createTestGame(): PGNGame {
        val headers = mapOf(
            "Event" to "Test Game",
            "Site" to "Test",
            "Date" to "2024.01.01",
            "White" to "Player1",
            "Black" to "Player2",
            "Result" to "1-0"
        )
        
        val moves = listOf(
            Move.fromAlgebraic("e2e4")!!,
            Move.fromAlgebraic("e7e5")!!,
            Move.fromAlgebraic("g1f3")!!,
            Move.fromAlgebraic("b8c6")!!,
            Move.fromAlgebraic("f1b5")!!
        )
        
        return PGNGame(headers, moves, "1. e4 e5 2. Nf3 Nc6 3. Bb5")
    }
    
    @Test
    fun testGameVisualizerInitialization() {
        val game = createTestGame()
        val visualizer = GameVisualizer(game)
        
        assertTrue(visualizer.isAtStart())
        assertFalse(visualizer.isAtEnd())
        assertEquals(0.0, visualizer.getProgress())
        
        val visualization = visualizer.getCurrentVisualization()
        assertEquals(0, visualization.moveNumber)
        assertEquals(5, visualization.totalMoves)
        assertNull(visualization.currentMove)
    }
    
    @Test
    fun testGameVisualizerNavigation() {
        val game = createTestGame()
        val visualizer = GameVisualizer(game)
        
        // Test next move
        val nextResult = visualizer.next()
        assertTrue(nextResult.success)
        assertEquals(1, visualizer.getCurrentVisualization().moveNumber)
        assertFalse(visualizer.isAtStart())
        
        // Test previous move
        val prevResult = visualizer.previous()
        assertTrue(prevResult.success)
        assertEquals(0, visualizer.getCurrentVisualization().moveNumber)
        assertTrue(visualizer.isAtStart())
        
        // Test goto move
        val gotoResult = visualizer.goToMove(3)
        assertTrue(gotoResult.success)
        assertEquals(3, visualizer.getCurrentVisualization().moveNumber)
        assertEquals(60.0, visualizer.getProgress())
    }
    
    @Test
    fun testGameVisualizerBoundaries() {
        val game = createTestGame()
        val visualizer = GameVisualizer(game)
        
        // Test going past end
        repeat(10) { visualizer.next() }
        assertTrue(visualizer.isAtEnd())
        
        val nextResult = visualizer.next()
        assertFalse(nextResult.success)
        assertTrue(nextResult.message.contains("end"))
        
        // Test going before start
        visualizer.goToMove(0)
        val prevResult = visualizer.previous()
        assertFalse(prevResult.success)
        assertTrue(prevResult.message.contains("start"))
    }
    
    @Test
    fun testMoveAnnotations() {
        val game = createTestGame()
        val visualizer = GameVisualizer(game)
        
        visualizer.next() // Move to first move
        
        val annotation = MoveAnnotation("Excellent opening move", MoveQuality.EXCELLENT)
        visualizer.addMoveAnnotation(1, annotation)
        
        val retrievedAnnotation = visualizer.getMoveAnnotation(1)
        assertNotNull(retrievedAnnotation)
        assertEquals("Excellent opening move", retrievedAnnotation.text)
        assertEquals(MoveQuality.EXCELLENT, retrievedAnnotation.quality)
    }
    
    @Test
    fun testVisualizationHighlights() {
        val game = createTestGame()
        val visualizer = GameVisualizer(game)
        
        visualizer.next() // e2e4
        
        val visualization = visualizer.getCurrentVisualization()
        assertEquals(2, visualization.highlights.size) // from and to squares
        assertTrue(visualization.highlights.contains(Position(1, 4))) // e2
        assertTrue(visualization.highlights.contains(Position(3, 4))) // e4
    }
    
    @Test
    fun testDetailedAnalysis() {
        val game = createTestGame()
        val visualizer = GameVisualizer(game)
        
        visualizer.next()
        val analysis = visualizer.getDetailedAnalysis()
        
        assertTrue(analysis.contains("Detailed Position Analysis"))
        assertTrue(analysis.contains("Game:"))
        assertTrue(analysis.contains("Position:"))
        assertTrue(analysis.contains("Progress:"))
        assertTrue(analysis.contains("Last move:"))
    }
}

/**
 * Tests for game analysis tools
 */
class GameAnalysisTest {
    
    private fun createTestGame(): PGNGame {
        val headers = mapOf(
            "Event" to "Test Game",
            "White" to "Player1",
            "Black" to "Player2",
            "Result" to "1-0"
        )
        
        val moves = listOf(
            Move.fromAlgebraic("e2e4")!!,
            Move.fromAlgebraic("e7e5")!!,
            Move.fromAlgebraic("d1h5")!!, // Early queen move (dubious)
            Move.fromAlgebraic("b8c6")!!,
            Move.fromAlgebraic("h5f7")!! // Checkmate
        )
        
        return PGNGame(headers, moves, "1. e4 e5 2. Qh5 Nc6 3. Qxf7#")
    }
    
    @Test
    fun testGameAnalyzer() {
        val game = createTestGame()
        val analyzer = GameAnalyzer()
        
        val result = analyzer.analyzeGame(game)
        
        assertEquals(game, result.game)
        assertEquals(5, result.moveAnalyses.size)
        assertNotNull(result.gameStatistics)
        assertNotNull(result.openingAnalysis)
        assertNotNull(result.endgameAnalysis)
    }
    
    @Test
    fun testMoveAnalysis() {
        val game = createTestGame()
        val analyzer = GameAnalyzer()
        val board = ChessBoard()
        
        // Analyze first move (e4)
        val move = game.moves[0]
        val piece = board.getPieceAt(move.from)!!
        val analysis = analyzer.analyzeMove(board, move, piece, null, true)
        
        assertEquals(move, analysis.move)
        assertEquals(piece, analysis.piece)
        assertNull(analysis.capturedPiece)
        assertTrue(analysis.isWhiteMove)
        assertEquals(0, analysis.materialChange)
    }
    
    @Test
    fun testTacticalElementDetection() {
        val game = createTestGame()
        val analyzer = GameAnalyzer()
        val result = analyzer.analyzeGame(game)
        
        // Last move should be checkmate
        val lastMoveAnalysis = result.moveAnalyses.last()
        assertTrue(lastMoveAnalysis.tacticalElements.contains(TacticalElement.CHECKMATE))
        assertTrue(lastMoveAnalysis.tacticalElements.contains(TacticalElement.CAPTURE))
    }
    
    @Test
    fun testGameStatistics() {
        val game = createTestGame()
        val analyzer = GameAnalyzer()
        val result = analyzer.analyzeGame(game)
        
        val stats = result.gameStatistics
        assertEquals(5, stats.totalMoves)
        assertTrue(stats.tacticalMoves > 0) // Should have tactical moves
        assertTrue(stats.captures > 0) // Should have captures
    }
    
    @Test
    fun testPlayerStatistics() {
        val game = createTestGame()
        val analyzer = GameAnalyzer()
        val result = analyzer.analyzeGame(game)
        
        val whiteStats = result.gameStatistics.whiteStatistics
        val blackStats = result.gameStatistics.blackStatistics
        
        assertEquals(3, whiteStats.totalMoves) // White plays moves 1, 3, 5
        assertEquals(2, blackStats.totalMoves) // Black plays moves 2, 4
        
        assertTrue(whiteStats.materialGained > 0) // White captures pawn on f7
    }
    
    @Test
    fun testAnalysisReport() {
        val game = createTestGame()
        val analyzer = GameAnalyzer()
        val result = analyzer.analyzeGame(game)
        
        val report = result.generateReport()
        
        assertTrue(report.contains("Comprehensive Game Analysis"))
        assertTrue(report.contains("Game Statistics"))
        assertTrue(report.contains("White Statistics"))
        assertTrue(report.contains("Black Statistics"))
        assertTrue(report.contains("Opening Analysis"))
        assertTrue(report.contains("Endgame Analysis"))
        assertTrue(report.contains("Key Moves"))
    }
}

/**
 * Tests for game export utilities
 */
class GameExportTest {
    
    private fun createTestGame(): PGNGame {
        val headers = mapOf(
            "Event" to "Test Tournament",
            "Site" to "Test City",
            "Date" to "2024.01.01",
            "White" to "Alice",
            "Black" to "Bob",
            "Result" to "1-0"
        )
        
        val moves = listOf(
            Move.fromAlgebraic("e2e4")!!,
            Move.fromAlgebraic("e7e5")!!,
            Move.fromAlgebraic("g1f3")!!
        )
        
        return PGNGame(headers, moves, "1. e4 e5 2. Nf3")
    }
    
    @Test
    fun testPGNExport() {
        val game = createTestGame()
        val exporter = GameExporter()
        
        val pgn = exporter.exportToPGN(game)
        
        assertTrue(pgn.contains("[Event \"Test Tournament\"]"))
        assertTrue(pgn.contains("[White \"Alice\"]"))
        assertTrue(pgn.contains("[Black \"Bob\"]"))
        assertTrue(pgn.contains("1. e4 e5 2. Nf3"))
    }
    
    @Test
    fun testFENSequenceExport() {
        val game = createTestGame()
        val exporter = GameExporter()
        
        val fenExport = exporter.exportToFENSequence(game)
        
        assertEquals(game.getSummary(), fenExport.gameInfo)
        assertEquals(4, fenExport.positions.size) // Starting + 3 moves
        assertEquals(3, fenExport.moveCount)
        assertTrue(fenExport.startingPosition.contains("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"))
    }
    
    @Test
    fun testMoveListExport() {
        val game = createTestGame()
        val exporter = GameExporter()
        
        val algebraicExport = exporter.exportMoveList(game, MoveNotation.ALGEBRAIC)
        val coordinateExport = exporter.exportMoveList(game, MoveNotation.COORDINATE)
        
        assertEquals(MoveNotation.ALGEBRAIC, algebraicExport.notation)
        assertEquals(3, algebraicExport.moves.size)
        assertTrue(algebraicExport.moves.contains("e4"))
        
        assertEquals(MoveNotation.COORDINATE, coordinateExport.notation)
        assertTrue(coordinateExport.moves.contains("e2e4"))
    }
    
    @Test
    fun testAnalysisReportExport() {
        val game = createTestGame()
        val analyzer = GameAnalyzer()
        val analysisResult = analyzer.analyzeGame(game)
        val exporter = GameExporter()
        
        val textReport = exporter.exportAnalysisReport(analysisResult, ReportFormat.TEXT)
        val htmlReport = exporter.exportAnalysisReport(analysisResult, ReportFormat.HTML)
        val jsonReport = exporter.exportAnalysisReport(analysisResult, ReportFormat.JSON)
        
        assertTrue(textReport.contains("Comprehensive Game Analysis"))
        assertTrue(htmlReport.contains("<html>"))
        assertTrue(htmlReport.contains("Chess Game Analysis"))
        assertTrue(jsonReport.contains("{"))
        assertTrue(jsonReport.contains("\"game\""))
    }
    
    @Test
    fun testPositionDiagramsExport() {
        val game = createTestGame()
        val exporter = GameExporter()
        
        val diagrams = exporter.exportPositionDiagrams(game, 2)
        
        assertEquals(game.getSummary(), diagrams.gameInfo)
        assertTrue(diagrams.diagrams.size >= 2) // Starting position + at least one more
        
        val firstDiagram = diagrams.diagrams.first()
        assertEquals(0, firstDiagram.moveNumber)
        assertEquals("Starting position", firstDiagram.description)
        assertTrue(firstDiagram.diagram.contains("8 |"))
    }
    
    @Test
    fun testMultiFormatExport() {
        val game = createTestGame()
        val analyzer = GameAnalyzer()
        val analysisResult = analyzer.analyzeGame(game)
        val exporter = GameExporter()
        
        val formats = setOf(ExportFormat.PGN, ExportFormat.FEN_SEQUENCE, ExportFormat.ANALYSIS_TEXT)
        val multiExport = exporter.exportMultiFormat(game, analysisResult, formats)
        
        assertEquals(game.getSummary(), multiExport.gameInfo)
        assertEquals(formats, multiExport.getAvailableFormats())
        
        assertNotNull(multiExport.getExport(ExportFormat.PGN))
        assertNotNull(multiExport.getExport(ExportFormat.FEN_SEQUENCE))
        assertNotNull(multiExport.getExport(ExportFormat.ANALYSIS_TEXT))
        assertNull(multiExport.getExport(ExportFormat.ANALYSIS_HTML))
    }
}

/**
 * Tests for interactive game browser
 */
class InteractiveGameBrowserTest {
    
    private fun createTestGame(): PGNGame {
        val headers = mapOf(
            "Event" to "Test Game",
            "White" to "Alice",
            "Black" to "Bob",
            "Result" to "1-0"
        )
        
        val moves = listOf(
            Move.fromAlgebraic("e2e4")!!,
            Move.fromAlgebraic("e7e5")!!,
            Move.fromAlgebraic("g1f3")!!,
            Move.fromAlgebraic("b8c6")!!
        )
        
        return PGNGame(headers, moves, "1. e4 e5 2. Nf3 Nc6")
    }
    
    @Test
    fun testBrowserInitialization() {
        val browser = InteractiveGameBrowser()
        val game = createTestGame()
        
        browser.loadGame(game)
        
        val response = browser.processCommand("show")
        assertTrue(response.success)
        assertNotNull(response.visualization)
    }
    
    @Test
    fun testNavigationCommands() {
        val browser = InteractiveGameBrowser()
        browser.loadGame(createTestGame())
        
        // Test next command
        val nextResponse = browser.processCommand("next")
        assertTrue(nextResponse.success)
        assertEquals(1, nextResponse.visualization?.moveNumber)
        
        // Test previous command
        val prevResponse = browser.processCommand("prev")
        assertTrue(prevResponse.success)
        assertEquals(0, prevResponse.visualization?.moveNumber)
        
        // Test goto command
        val gotoResponse = browser.processCommand("goto 2")
        assertTrue(gotoResponse.success)
        assertEquals(2, gotoResponse.visualization?.moveNumber)
    }
    
    @Test
    fun testInformationCommands() {
        val browser = InteractiveGameBrowser()
        browser.loadGame(createTestGame())
        
        // Test game info
        val gameResponse = browser.processCommand("game")
        assertTrue(gameResponse.success)
        assertTrue(gameResponse.message.contains("Alice"))
        assertTrue(gameResponse.message.contains("Bob"))
        
        // Test move list
        val movesResponse = browser.processCommand("moves")
        assertTrue(movesResponse.success)
        assertTrue(movesResponse.message.contains("1. e2e4"))
        
        // Test help
        val helpResponse = browser.processCommand("help")
        assertTrue(helpResponse.success)
        assertTrue(helpResponse.message.contains("Navigation"))
    }
    
    @Test
    fun testAnalysisCommands() {
        val browser = InteractiveGameBrowser()
        browser.loadGame(createTestGame())
        
        browser.processCommand("next") // Move to first position
        
        val analyzeResponse = browser.processCommand("analyze")
        assertTrue(analyzeResponse.success)
        assertTrue(analyzeResponse.message.contains("Position Analysis"))
        
        val materialResponse = browser.processCommand("material")
        assertTrue(materialResponse.success)
        assertTrue(materialResponse.message.contains("Material Balance"))
    }
    
    @Test
    fun testAnnotationCommands() {
        val browser = InteractiveGameBrowser()
        browser.loadGame(createTestGame())
        
        browser.processCommand("next") // Move to first position
        
        val annotateResponse = browser.processCommand("annotate Good opening move")
        assertTrue(annotateResponse.success)
        assertTrue(annotateResponse.message.contains("Good opening move"))
        
        val qualityResponse = browser.processCommand("quality excellent")
        assertTrue(qualityResponse.success)
        assertTrue(qualityResponse.message.contains("excellent"))
    }
    
    @Test
    fun testExportCommands() {
        val browser = InteractiveGameBrowser()
        browser.loadGame(createTestGame())
        
        val pgnResponse = browser.processCommand("export pgn")
        assertTrue(pgnResponse.success)
        assertTrue(pgnResponse.message.contains("[Event"))
        
        val fenResponse = browser.processCommand("export fen")
        assertTrue(fenResponse.success)
        assertTrue(fenResponse.message.contains("FEN Sequence"))
    }
    
    @Test
    fun testErrorHandling() {
        val browser = InteractiveGameBrowser()
        browser.loadGame(createTestGame())
        
        // Test invalid command
        val invalidResponse = browser.processCommand("invalid")
        assertFalse(invalidResponse.success)
        assertTrue(invalidResponse.message.contains("Unknown command"))
        
        // Test invalid goto
        val invalidGotoResponse = browser.processCommand("goto abc")
        assertFalse(invalidGotoResponse.success)
        assertTrue(invalidGotoResponse.message.contains("Invalid move number"))
        
        // Test invalid quality
        val invalidQualityResponse = browser.processCommand("quality invalid")
        assertFalse(invalidQualityResponse.success)
        assertTrue(invalidQualityResponse.message.contains("Invalid quality"))
    }
    
    @Test
    fun testMultipleGames() {
        val browser = InteractiveGameBrowser()
        val games = listOf(createTestGame(), createTestGame())
        
        browser.loadGames(games)
        
        val loadResponse = browser.processCommand("load")
        assertTrue(loadResponse.success)
        assertTrue(loadResponse.message.contains("2"))
        
        val load2Response = browser.processCommand("load 2")
        assertTrue(load2Response.success)
        assertTrue(load2Response.message.contains("Loaded game 2"))
    }
    
    @Test
    fun testSearchCommand() {
        val browser = InteractiveGameBrowser()
        val games = listOf(createTestGame())
        browser.loadGames(games)
        
        val searchResponse = browser.processCommand("search Alice")
        assertTrue(searchResponse.success)
        assertTrue(searchResponse.message.contains("Alice"))
        
        val noResultsResponse = browser.processCommand("search NonExistent")
        assertFalse(noResultsResponse.success)
        assertTrue(noResultsResponse.message.contains("No games found"))
    }
    
    @Test
    fun testQuitCommand() {
        val browser = InteractiveGameBrowser()
        browser.loadGame(createTestGame())
        
        val quitResponse = browser.processCommand("quit")
        assertTrue(quitResponse.success)
        assertTrue(quitResponse.shouldExit)
    }
}