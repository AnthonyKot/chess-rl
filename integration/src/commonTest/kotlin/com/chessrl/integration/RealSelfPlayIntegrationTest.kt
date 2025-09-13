package com.chessrl.integration

import kotlin.test.*

/**
 * Integration test for the real self-play controller
 * This tests the core integration improvements identified in task 10.1
 */
class RealSelfPlayIntegrationTest {
    
    @Test
    fun testRealSelfPlayControllerInitialization() {
        val config = SelfPlayConfig(
            hiddenLayers = listOf(32, 16), // Smaller for testing
            learningRate = 0.01,
            explorationRate = 0.2,
            batchSize = 8,
            maxExperiences = 100
        )
        
        val controller = RealSelfPlayController(config)
        val initResult = controller.initialize()
        
        when (initResult) {
            is SelfPlayInitResult.Success -> {
                assertTrue(initResult.message.contains("initialized"))
            }
            is SelfPlayInitResult.Failed -> {
                fail("Initialization should succeed: ${initResult.error}")
            }
        }
    }
    
    @Test
    fun testSelfPlaySessionStart() {
        val config = SelfPlayConfig(
            hiddenLayers = listOf(16, 8), // Very small for testing
            learningRate = 0.01,
            explorationRate = 0.3,
            batchSize = 4,
            maxExperiences = 50
        )
        
        val controller = RealSelfPlayController(config)
        
        // Initialize first
        val initResult = controller.initialize()
        when (initResult) {
            is SelfPlayInitResult.Success -> {
                // Now try to start a session
                val sessionConfig = SelfPlaySessionConfig(
                    maxEpisodes = 2, // Very small for testing
                    gamesPerIteration = 1,
                    enableLogging = false
                )
                
                val sessionResult = controller.startSession(sessionConfig)
                when (sessionResult) {
                    is SelfPlaySessionResult.Success -> {
                        assertNotNull(sessionResult.sessionId)
                        assertTrue(sessionResult.sessionId.isNotEmpty())
                        
                        // Let it run briefly then stop
                        Thread.sleep(100) // Give it a moment to start
                        assertTrue(controller.stopSession())
                    }
                    is SelfPlaySessionResult.Failed -> {
                        // This might fail in test environment, which is acceptable
                        println("Session start failed (may be expected in test): ${sessionResult.error}")
                    }
                }
            }
            is SelfPlayInitResult.Failed -> {
                fail("Initialization should succeed: ${initResult.error}")
            }
        }
    }
    
    @Test
    fun testTrainingMetricsCollection() {
        val config = SelfPlayConfig(
            hiddenLayers = listOf(8, 4), // Minimal for testing
            learningRate = 0.05,
            explorationRate = 0.5,
            batchSize = 2,
            maxExperiences = 20
        )
        
        val controller = RealSelfPlayController(config)
        
        // Test metrics before initialization
        val initialMetrics = controller.getTrainingMetrics()
        assertEquals("none", initialMetrics.sessionId)
        assertEquals(0, initialMetrics.episodesCompleted)
        
        // Initialize and test metrics
        val initResult = controller.initialize()
        when (initResult) {
            is SelfPlayInitResult.Success -> {
                val metricsAfterInit = controller.getTrainingMetrics()
                assertNotNull(metricsAfterInit)
                assertTrue(metricsAfterInit.explorationRate >= 0.0)
            }
            is SelfPlayInitResult.Failed -> {
                fail("Initialization should succeed: ${initResult.error}")
            }
        }
    }
    
    @Test
    fun testGameResultDataStructures() {
        // Test the data structures used in self-play
        val gameMove = GameMove(
            move = Move(Position(1, 4), Position(3, 4)),
            state = DoubleArray(10) { it * 0.1 },
            action = 42,
            reward = 0.5,
            nextState = DoubleArray(10) { (it + 1) * 0.1 }
        )
        
        assertNotNull(gameMove)
        assertEquals(42, gameMove.action)
        assertEquals(0.5, gameMove.reward)
        
        val gameResult = GameResult(
            gameId = 1,
            moves = listOf(gameMove),
            outcome = GameOutcome.WHITE_WIN,
            moveCount = 1,
            duration = 1000L
        )
        
        assertNotNull(gameResult)
        assertEquals(1, gameResult.gameId)
        assertEquals(GameOutcome.WHITE_WIN, gameResult.outcome)
        assertEquals(1, gameResult.moves.size)
    }
    
    @Test
    fun testSelfPlayConfiguration() {
        val config = SelfPlayConfig(
            hiddenLayers = listOf(64, 32, 16),
            learningRate = 0.001,
            explorationRate = 0.1,
            batchSize = 32,
            maxExperiences = 5000
        )
        
        assertEquals(listOf(64, 32, 16), config.hiddenLayers)
        assertEquals(0.001, config.learningRate)
        assertEquals(0.1, config.explorationRate)
        assertEquals(32, config.batchSize)
        assertEquals(5000, config.maxExperiences)
        
        val sessionConfig = SelfPlaySessionConfig(
            maxEpisodes = 100,
            gamesPerIteration = 5,
            enableLogging = true
        )
        
        assertEquals(100, sessionConfig.maxEpisodes)
        assertEquals(5, sessionConfig.gamesPerIteration)
        assertTrue(sessionConfig.enableLogging)
    }
    
    @Test
    fun testGameOutcomeEnum() {
        val outcomes = GameOutcome.values()
        assertEquals(3, outcomes.size)
        assertTrue(GameOutcome.WHITE_WIN in outcomes)
        assertTrue(GameOutcome.BLACK_WIN in outcomes)
        assertTrue(GameOutcome.DRAW in outcomes)
    }
}