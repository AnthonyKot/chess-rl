package com.chessrl.integration

import kotlin.test.*

/**
 * Tests for the self-play controller
 */
class SelfPlayControllerTest {
    
    private lateinit var controller: SelfPlayController
    
    @BeforeTest
    fun setup() {
        // Create controller with test configuration
        controller = SelfPlayController(
            config = SelfPlayControllerConfig(
                agentType = AgentType.DQN,
                hiddenLayers = listOf(32, 16), // Small network for testing
                learningRate = 0.01,
                explorationRate = 0.2,
                maxMovesPerGame = 50, // Short games for testing
                trainingFrequency = 3,
                minExperiencesForTraining = 10,
                trainingBatchSize = 8,
                updatesPerTraining = 2,
                maxExperienceBufferSize = 200,
                progressReportInterval = 5,
                checkpointInterval = 10
            )
        )
    }
    
    @Test
    fun testControllerInitialization() {
        println("Testing controller initialization...")
        
        // Test initialization
        val initialized = controller.initialize()
        assertTrue(initialized, "Controller should initialize successfully")
        
        // Test initial status
        val status = controller.getTrainingStatus()
        assertFalse(status.isTraining, "Should not be training initially")
        assertFalse(status.isPaused, "Should not be paused initially")
        assertEquals(0, status.gamesCompleted, "Should have 0 games completed")
        assertEquals(0, status.movesPlayed, "Should have 0 moves played")
        
        println("✅ Controller initialization verified")
    }
    
    @Test
    fun testSelfPlayTrainingLifecycle() {
        println("Testing self-play training lifecycle...")
        
        // Initialize controller
        assertTrue(controller.initialize(), "Controller should initialize")
        
        // Check initial status
        var status = controller.getTrainingStatus()
        assertFalse(status.isTraining, "Should not be training initially")
        
        // Start self-play training
        val results = controller.startSelfPlayTraining(totalGames = 5)
        assertNotNull(results, "Self-play training should return results")
        
        // Check final status
        status = controller.getTrainingStatus()
        assertFalse(status.isTraining, "Training should be completed")
        
        // Verify results
        assertTrue(results.totalGames > 0, "Should have played games")
        assertTrue(results.totalMoves > 0, "Should have made moves")
        
        println("✅ Self-play training lifecycle verified")
        println("   Games Played: ${results.totalGames}")
        println("   Total Moves: ${results.totalMoves}")
    }
    
    @Test
    fun testSelfPlayStatusTracking() {
        println("Testing self-play status tracking...")
        
        // Initialize controller
        assertTrue(controller.initialize(), "Controller should initialize")
        
        // Get initial status
        val initialStatus = controller.getTrainingStatus()
        assertEquals(0, initialStatus.gamesCompleted, "Should start with 0 games")
        assertEquals(0, initialStatus.movesPlayed, "Should start with 0 moves")
        assertEquals(0.0, initialStatus.currentWinRate, "Should start with 0 win rate")
        
        // Run self-play training
        val results = controller.startSelfPlayTraining(totalGames = 3)
        assertNotNull(results, "Training should complete")
        
        // Status should reflect completed training
        val finalStatus = controller.getTrainingStatus()
        assertFalse(finalStatus.isTraining, "Should not be training after completion")
        
        println("✅ Self-play status tracking verified")
    }
    
    @Test
    fun testSelfPlayTest() {
        println("Testing self-play test functionality...")
        
        // Run self-play test
        val testPassed = controller.runSelfPlayTest(games = 3)
        assertTrue(testPassed, "Self-play test should pass")
        
        println("✅ Self-play test functionality verified")
    }
    
    @Test
    fun testSelfPlayGameDemonstration() {
        println("Testing self-play game demonstration...")
        
        // Initialize controller
        assertTrue(controller.initialize(), "Controller should initialize")
        
        // Run game demonstration
        val demonstration = controller.demonstrateSelfPlayGame()
        assertNotNull(demonstration, "Game demonstration should succeed")
        
        // Verify demonstration content
        assertTrue(demonstration.totalMoves >= 0, "Should have move count")
        assertNotNull(demonstration.finalStatus, "Should have final status")
        assertNotNull(demonstration.moves, "Should have moves list")
        assertNotNull(demonstration.chessMetrics, "Should have chess metrics")
        
        println("✅ Self-play game demonstration verified")
        println("   Total Moves: ${demonstration.totalMoves}")
        println("   Final Status: ${demonstration.finalStatus}")
        println("   Moves Recorded: ${demonstration.moves.size}")
    }
    
    @Test
    fun testTrainingQualityAnalysis() {
        println("Testing training quality analysis...")
        
        // Initialize and run some training
        assertTrue(controller.initialize(), "Controller should initialize")
        controller.startSelfPlayTraining(totalGames = 5)
        
        // Analyze training quality
        val qualityAnalysis = controller.analyzeTrainingQuality()
        assertNotNull(qualityAnalysis, "Quality analysis should succeed")
        
        // Verify analysis components
        assertNotNull(qualityAnalysis.gameQuality, "Should have game quality analysis")
        assertTrue(qualityAnalysis.trainingEfficiency >= 0.0, "Training efficiency should be non-negative")
        assertTrue(qualityAnalysis.learningProgress >= 0.0, "Learning progress should be non-negative")
        assertTrue(qualityAnalysis.overallQualityScore >= 0.0, "Overall quality should be non-negative")
        
        println("✅ Training quality analysis verified")
        println("   Training Efficiency: ${qualityAnalysis.trainingEfficiency}")
        println("   Learning Progress: ${qualityAnalysis.learningProgress}")
        println("   Overall Quality: ${qualityAnalysis.overallQualityScore}")
    }
    
    @Test
    fun testDifferentAgentTypes() {
        println("Testing different agent types...")
        
        // Test DQN agent
        val dqnController = SelfPlayController(
            config = SelfPlayControllerConfig(
                agentType = AgentType.DQN,
                hiddenLayers = listOf(16, 8),
                learningRate = 0.01
            )
        )
        
        assertTrue(dqnController.initialize(), "DQN controller should initialize")
        val dqnResults = dqnController.startSelfPlayTraining(totalGames = 2)
        assertNotNull(dqnResults, "DQN self-play should work")
        
        // Test Policy Gradient agent
        val pgController = SelfPlayController(
            config = SelfPlayControllerConfig(
                agentType = AgentType.POLICY_GRADIENT,
                hiddenLayers = listOf(16, 8),
                learningRate = 0.01,
                temperature = 1.0
            )
        )
        
        assertTrue(pgController.initialize(), "PG controller should initialize")
        val pgResults = pgController.startSelfPlayTraining(totalGames = 2)
        assertNotNull(pgResults, "Policy Gradient self-play should work")
        
        println("✅ Different agent types verified")
        println("   DQN Games: ${dqnResults.totalGames}")
        println("   PG Games: ${pgResults.totalGames}")
    }
    
    @Test
    fun testControllerConfiguration() {
        println("Testing controller configuration...")
        
        // Test with custom configuration
        val customConfig = SelfPlayControllerConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(24, 12),
            learningRate = 0.005,
            explorationRate = 0.15,
            maxMovesPerGame = 30,
            trainingFrequency = 2,
            winReward = 2.0,
            lossReward = -2.0,
            drawReward = 0.5
        )
        
        val customController = SelfPlayController(customConfig)
        
        // Test initialization and training
        assertTrue(customController.initialize(), "Custom controller should initialize")
        val results = customController.startSelfPlayTraining(totalGames = 3)
        assertNotNull(results, "Custom controller should work")
        
        println("✅ Controller configuration verified")
        println("   Custom config games: ${results.totalGames}")
    }
    
    @Test
    fun testTrainingControlFlow() {
        println("Testing training control flow...")
        
        // Initialize controller
        assertTrue(controller.initialize(), "Controller should initialize")
        
        // Test that we can't start training twice
        controller.startSelfPlayTraining(totalGames = 1) // This will complete immediately
        
        // Status should show not training after completion
        val status = controller.getTrainingStatus()
        assertFalse(status.isTraining, "Should not be training after completion")
        
        // Should be able to start new training session
        val secondResults = controller.startSelfPlayTraining(totalGames = 2)
        assertNotNull(secondResults, "Should be able to start new training session")
        
        println("✅ Training control flow verified")
    }
    
    @Test
    fun testSelfPlayMetrics() {
        println("Testing self-play metrics...")
        
        // Initialize and run training
        assertTrue(controller.initialize(), "Controller should initialize")
        val results = controller.startSelfPlayTraining(totalGames = 4)
        assertNotNull(results, "Training should complete")
        
        // Verify metrics are reasonable
        val stats = results.finalStatistics
        assertTrue(stats.gamesPlayed > 0, "Should have played games")
        assertTrue(stats.totalMoves > 0, "Should have total moves")
        assertTrue(stats.averageGameLength > 0, "Should have average game length")
        assertTrue(stats.winRate >= 0.0 && stats.winRate <= 1.0, "Win rate should be valid")
        assertTrue(stats.drawRate >= 0.0 && stats.drawRate <= 1.0, "Draw rate should be valid")
        
        println("✅ Self-play metrics verified")
        println("   Games: ${stats.gamesPlayed}")
        println("   Moves: ${stats.totalMoves}")
        println("   Avg Length: ${stats.averageGameLength}")
        println("   Win Rate: ${(stats.winRate * 100)}%")
    }
}