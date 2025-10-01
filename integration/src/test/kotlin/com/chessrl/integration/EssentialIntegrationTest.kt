package com.chessrl.integration

import com.chessrl.integration.config.ChessRLConfig
import kotlin.test.*

/**
 * Essential integration tests - fast, reliable tests for core integration functionality
 * Focuses on basic configuration and component creation
 */
class EssentialIntegrationTest {
    
    @Test
    fun testConfigurationCreation() {
        // Test default configuration
        val defaultConfig = ChessRLConfig()
        
        assertTrue(defaultConfig.maxCycles > 0, "Max cycles should be positive")
        assertTrue(defaultConfig.gamesPerCycle > 0, "Games per cycle should be positive")
        assertTrue(defaultConfig.maxConcurrentGames > 0, "Max concurrent games should be positive")
        assertTrue(defaultConfig.maxStepsPerGame > 0, "Max steps per game should be positive")
        
        // Test custom configuration
        val customConfig = ChessRLConfig(
            maxCycles = 50,
            gamesPerCycle = 20,
            maxConcurrentGames = 4,
            maxStepsPerGame = 100,
            seed = 42L
        )
        
        assertEquals(50, customConfig.maxCycles)
        assertEquals(20, customConfig.gamesPerCycle)
        assertEquals(4, customConfig.maxConcurrentGames)
        assertEquals(100, customConfig.maxStepsPerGame)
        assertEquals(42L, customConfig.seed)
    }
    
    @Test
    fun testConfigurationValidation() {
        // Test valid configuration
        val validConfig = ChessRLConfig(
            maxCycles = 10,
            gamesPerCycle = 5,
            maxConcurrentGames = 2,
            maxStepsPerGame = 50
        )
        
        val validationResult = validConfig.validate()
        assertTrue(validationResult.isValid, "Valid configuration should be valid")
        assertTrue(validationResult.errors.isEmpty(), "Valid configuration should have no errors")
        
        // Test invalid configuration
        val invalidConfig = ChessRLConfig(
            maxCycles = 0, // Invalid
            gamesPerCycle = -1, // Invalid
            maxConcurrentGames = 0, // Invalid
            maxStepsPerGame = -10 // Invalid
        )
        
        val invalidValidationResult = invalidConfig.validate()
        assertFalse(invalidValidationResult.isValid, "Invalid configuration should not be valid")
        assertTrue(invalidValidationResult.errors.isNotEmpty(), "Invalid configuration should have errors")
    }
    
    @Test
    fun testTrainingPipelineCreation() {
        val config = ChessRLConfig(
            maxCycles = 2,
            gamesPerCycle = 2,
            maxConcurrentGames = 1,
            maxStepsPerGame = 10,
            seed = 12345L
        )
        
        val pipeline = TrainingPipeline(config)
        assertNotNull(pipeline, "Training pipeline should be created successfully")
    }
    
    @Test
    fun testChessStateEncoderBasicFunctionality() {
        val encoder = ChessStateEncoder()
        val board = com.chessrl.chess.ChessBoard()
        
        val state = encoder.encode(board)
        
        assertNotNull(state, "Encoded state should not be null")
        assertTrue(state.size > 0, "Encoded state should not be empty")
        assertEquals(ChessStateEncoder.TOTAL_FEATURES, state.size, "State should have correct dimensions")
        
        // All values should be finite
        for (value in state) {
            assertTrue(value.isFinite(), "All state values should be finite")
        }
    }
    
    @Test
    fun testGameAnalyzerBasicFunctionality() {
        val analyzer = GameAnalyzer()
        
        // Test basic initialization
        analyzer.initialize()
        
        // Test that analyzer can be created without errors
        assertNotNull(analyzer, "Game analyzer should be created successfully")
    }
    
    @Test
    fun testBasicIntegrationComponents() {
        // Test that basic components can be created without errors
        val config = ChessRLConfig(seed = 12345L)
        
        // Test state encoder
        val encoder = ChessStateEncoder()
        assertNotNull(encoder, "State encoder should be created")
        
        // Test game analyzer
        val analyzer = GameAnalyzer()
        assertNotNull(analyzer, "Game analyzer should be created")
        
        // Test training pipeline
        val pipeline = TrainingPipeline(config)
        assertNotNull(pipeline, "Training pipeline should be created")
        
        // Test that we can create a chess board and encode it
        val board = com.chessrl.chess.ChessBoard()
        val state = encoder.encode(board)
        assertTrue(state.size > 0, "Encoded state should have positive size")
    }
}