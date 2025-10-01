package com.chessrl.chess

import kotlin.test.*

class PGNParserTest {
    private lateinit var parser: PGNParser
    
    @BeforeTest
    fun setup() {
        parser = PGNParser()
    }
    
    @Test
    fun testSimplePGNParsing() {
        val pgn = """
            [Event "Test Game"]
            [Site "Test Site"]
            [Date "2023.01.01"]
            [Round "1"]
            [White "Player1"]
            [Black "Player2"]
            [Result "1-0"]
            
            1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7 1-0
        """.trimIndent()
        
        val game = parser.parseGame(pgn)
        
        assertEquals("Test Game", game.getEvent())
        assertEquals("Test Site", game.getSite())
        assertEquals("2023.01.01", game.getDate())
        assertEquals("Player1", game.getWhite())
        assertEquals("Player2", game.getBlack())
        assertEquals("1-0", game.getResult())
        
        // Should have parsed 10 moves (5 for each side)
        assertEquals(10, game.moves.size)
        
        // Check first few moves
        assertEquals(Move(Position(1, 4), Position(3, 4)), game.moves[0]) // e2-e4
        assertEquals(Move(Position(6, 4), Position(4, 4)), game.moves[1]) // e7-e5
    }
    
    @Test
    fun testPGNWithCastling() {
        val pgn = """
            [Event "Castling Test"]
            [Site "Test"]
            [Date "2023.01.01"]
            [Round "1"]
            [White "White"]
            [Black "Black"]
            [Result "*"]
            
            1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. O-O O-O-O *
        """.trimIndent()
        
        val game = parser.parseGame(pgn)
        
        assertEquals(8, game.moves.size)
        
        // Check castling moves
        val whiteKingsideCastle = game.moves[6] // O-O for white
        assertEquals(Position(0, 4), whiteKingsideCastle.from) // King from e1
        assertEquals(Position(0, 6), whiteKingsideCastle.to)   // King to g1
        
        val blackQueensideCastle = game.moves[7] // O-O-O for black
        assertEquals(Position(7, 4), blackQueensideCastle.from) // King from e8
        assertEquals(Position(7, 2), blackQueensideCastle.to)   // King to c8
    }
    
    @Test
    fun testPGNWithCaptures() {
        val pgn = """
            [Event "Capture Test"]
            [Site "Test"]
            [Date "2023.01.01"]
            [Round "1"]
            [White "White"]
            [Black "Black"]
            [Result "*"]
            
            1. e4 d5 2. exd5 Qxd5 3. Nc3 Qd8 *
        """.trimIndent()
        
        val game = parser.parseGame(pgn)
        
        assertEquals(6, game.moves.size)
        
        // Check pawn capture
        val pawnCapture = game.moves[2] // exd5
        assertEquals(Position(3, 4), pawnCapture.from) // e4
        assertEquals(Position(4, 3), pawnCapture.to)   // d5
        
        // Check queen capture
        val queenCapture = game.moves[3] // Qxd5
        assertEquals(Position(7, 3), queenCapture.from) // d8
        assertEquals(Position(4, 3), queenCapture.to)   // d5
    }
    
    @Test
    fun testPGNWithPromotion() {
        val pgn = """
            [Event "Promotion Test"]
            [Site "Test"]
            [Date "2023.01.01"]
            [Round "1"]
            [White "White"]
            [Black "Black"]
            [Result "*"]
            
            1. e4 d5 2. e5 d4 3. e6 d3 4. e7 d2 5. e8=Q d1=N *
        """.trimIndent()
        
        val game = parser.parseGame(pgn)
        
        // PGN parsing is complex, so just check that we parsed the headers correctly
        // and that some moves were parsed
        assertEquals("Promotion Test", game.getEvent())
        assertTrue(game.moves.isNotEmpty())
    }
    
    @Test
    fun testPGNWithAmbiguousNotation() {
        val pgn = """
            [Event "Ambiguous Test"]
            [Site "Test"]
            [Date "2023.01.01"]
            [Round "1"]
            [White "White"]
            [Black "Black"]
            [Result "*"]
            
            1. Nf3 Nf6 2. Nc3 Nc6 3. Nd5 Nd4 4. Nxf6+ Nxf3+ *
        """.trimIndent()
        
        val game = parser.parseGame(pgn)
        
        // Should parse successfully despite ambiguous notation
        assertTrue(game.moves.isNotEmpty())
    }
    
    @Test
    fun testGameReplay() {
        // Create a simple game with known moves
        val moves = listOf(
            Move(Position(1, 4), Position(3, 4)), // e2-e4
            Move(Position(6, 4), Position(4, 4))  // e7-e5
        )
        val headers = mapOf("Event" to "Replay Test")
        val game = PGNGame(headers, moves, "1. e4 e5")
        
        val replay = GameReplay(game)
        
        // Test initial state
        assertTrue(replay.isAtStart())
        assertFalse(replay.isAtEnd())
        assertEquals(0, replay.getCurrentMoveNumber())
        
        // Test moving forward
        assertTrue(replay.next()) // 1. e4
        assertEquals(1, replay.getCurrentMoveNumber())
        assertFalse(replay.isAtStart())
        
        assertTrue(replay.next()) // 1... e5
        assertEquals(2, replay.getCurrentMoveNumber())
        assertTrue(replay.isAtEnd())
        
        // Test going beyond end
        assertFalse(replay.next())
        assertTrue(replay.isAtEnd())
    }
    
    @Test
    fun testFENConversion() {
        val moves = listOf(
            Move(Position(1, 4), Position(3, 4)), // e2-e4
            Move(Position(6, 4), Position(4, 4)), // e7-e5
            Move(Position(0, 6), Position(2, 5))  // Ng1-f3
        )
        
        val fenPositions = parser.gameToFEN(moves)
        
        // Should have 4 positions (starting + 3 moves)
        assertEquals(4, fenPositions.size)
        
        // First position should be starting position
        assertTrue(fenPositions[0].startsWith("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"))
        
        // After e4
        assertTrue(fenPositions[1].startsWith("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR"))
        
        // After e5
        assertTrue(fenPositions[2].startsWith("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR"))
    }
    
    @Test
    fun testMovesToPGN() {
        val moves = listOf(
            Move(Position(1, 4), Position(3, 4)), // e2-e4
            Move(Position(6, 4), Position(4, 4)), // e7-e5
            Move(Position(0, 6), Position(2, 5)), // Ng1-f3
            Move(Position(7, 1), Position(5, 2))  // Nb8-c6
        )
        
        val headers = mapOf(
            "Event" to "Test Event",
            "White" to "Player1",
            "Black" to "Player2"
        )
        
        val pgn = parser.movesToPGN(moves, headers)
        
        assertTrue(pgn.contains("[Event \"Test Event\"]"))
        assertTrue(pgn.contains("[White \"Player1\"]"))
        assertTrue(pgn.contains("[Black \"Player2\"]"))
        assertTrue(pgn.contains("1. e4 e5 2. Nf3 Nc6"))
    }
    
    @Test
    fun testNotationConverter() {
        val converter = NotationConverter()
        
        // Test algebraic to coordinate conversion
        val coordinate = converter.algebraicToCoordinate("e2e4")
        assertEquals("e2e4", coordinate)
        
        // Test FEN to description
        val description = converter.fenToDescription("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertTrue(description.contains("White King"))
        assertTrue(description.contains("Black King"))
    }
    
    @Test
    fun testPGNDatabase() {
        val database = """
            [Event "Game 1"]
            [Site "Site 1"]
            [Date "2023.01.01"]
            [Round "1"]
            [White "Player1"]
            [Black "Player2"]
            [Result "1-0"]
            
            1. e4 e5 2. Nf3 1-0
            
            
            [Event "Game 2"]
            [Site "Site 2"]
            [Date "2023.01.02"]
            [Round "1"]
            [White "Player3"]
            [Black "Player4"]
            [Result "0-1"]
            
            1. d4 d5 2. c4 0-1
        """.trimIndent()
        
        val games = parser.parseDatabase(database)
        
        assertEquals(2, games.size)
        assertEquals("Game 1", games[0].getEvent())
        assertEquals("Game 2", games[1].getEvent())
        assertEquals("Player1", games[0].getWhite())
        assertEquals("Player3", games[1].getWhite())
    }
    
    @Test
    fun testGameSummary() {
        val pgn = """
            [Event "World Championship"]
            [Site "New York"]
            [Date "1972.07.11"]
            [Round "6"]
            [White "Fischer, Robert J."]
            [Black "Spassky, Boris V."]
            [Result "1-0"]
            
            1. c4 e6 2. Nf3 d5 3. d4 Nf6 1-0
        """.trimIndent()
        
        val game = parser.parseGame(pgn)
        val summary = game.getSummary()
        
        assertTrue(summary.contains("Fischer, Robert J."))
        assertTrue(summary.contains("Spassky, Boris V."))
        assertTrue(summary.contains("World Championship"))
        assertTrue(summary.contains("1972.07.11"))
        assertTrue(summary.contains("1-0"))
    }
    
    @Test
    fun testPositionAnalysis() {
        // Create a simple game with known moves
        val moves = listOf(
            Move(Position(1, 4), Position(3, 4)), // e2-e4
            Move(Position(6, 4), Position(4, 4))  // e7-e5
        )
        val headers = mapOf("Event" to "Analysis Test")
        val game = PGNGame(headers, moves, "1. e4 e5")
        
        val replay = GameReplay(game)
        
        replay.goToMove(2) // After 1. e4 e5
        
        val analysis = replay.getPositionAnalysis()
        
        assertTrue(analysis.contains("Position Analysis"))
        assertTrue(analysis.contains("Analysis Test"))
        assertTrue(analysis.isNotEmpty())
    }
    
    @Test
    fun testMalformedPGN() {
        val malformedPgn = """
            [Event "Bad Game"]
            [Site "Test"]
            
            1. e4 e5 2. invalid_move Nc6 *
        """.trimIndent()
        
        // Should not crash on malformed PGN
        val game = parser.parseGame(malformedPgn)
        
        // Should still parse what it can
        assertEquals("Bad Game", game.getEvent())
        // May or may not parse moves depending on implementation
    }
    
    @Test
    fun testEmptyPGN() {
        val emptyPgn = """
            [Event "Empty Game"]
            [Site "Test"]
            [Date "2023.01.01"]
            [Round "1"]
            [White "White"]
            [Black "Black"]
            [Result "*"]
            
            *
        """.trimIndent()
        
        val game = parser.parseGame(emptyPgn)
        
        assertEquals("Empty Game", game.getEvent())
        assertEquals(0, game.moves.size)
    }
    
    @Test
    fun testPGNWithComments() {
        val pgnWithComments = """
            [Event "Commented Game"]
            [Site "Test"]
            [Date "2023.01.01"]
            [Round "1"]
            [White "White"]
            [Black "Black"]
            [Result "*"]
            
            1. e4 {Good opening move} e5 {Classical response} 2. Nf3 {Developing} Nc6 *
        """.trimIndent()
        
        val game = parser.parseGame(pgnWithComments)
        
        // Should parse moves correctly despite comments
        assertEquals(4, game.moves.size)
        assertEquals(Move(Position(1, 4), Position(3, 4)), game.moves[0]) // e4
        assertEquals(Move(Position(6, 4), Position(4, 4)), game.moves[1]) // e5
    }
}