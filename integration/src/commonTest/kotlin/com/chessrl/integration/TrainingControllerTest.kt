package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*
import kotlin.test.*

/**
 * Tests for the training controller with comprehensive functionality testing
 */
class TrainingControllerTest {
    
    private lateinit var controller: TrainingController
    
    @BeforeTest
    fun setup() {
        // Create controller with test configuration
        controller = TrainingController(
            config = TrainingControllerConfig(
                agentType = AgentType.DQN,
                hiddenLayers = listOf(32, 16), // Small network for testing
                learningRate = 0.01,
                explorationRate = 0.2,
                maxStepsPerEpisode = 15,
                batchSize = 8,
                maxBufferSize = 50,
                progressReportInterval = 5,
                checkpointInterval = 10,
                enableEarlyStopping = false
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
        assertFalse(status.isTraining)
        assertFalse(status.isPaused)
        assertEquals(0, status.currentEpisode)
        assertEquals(0, status.totalSteps)
        
        println("✅ Controller initialization verified")
    }
    
    @Test
    fun testTrainingLifecycle() {
        println("Testing training lifecycle...")
        
        // Initialize controller
        assertTrue(controller.initialize())
        
        // Check initial status
        var status = controller.getTrainingStatus()
        assertFalse(status.isTraining)
        
        // Start training
        val results = controller.startTraining(episodes = 3)
        assertNotNull(results, "Training should return results")
        
        // Check final status
        status = controller.getTrainingStatus()
        assertFalse(status.isTraining, "Training should be completed")
        
        // Verify results
        assertEquals(3, results.totalEpisodes)
        assertTrue(results.totalSteps > 0)
        
        println("✅ Training lifecycle verified")
    }
    
    @Test
    fun testTrainingStatusTracking() {
        println("Testing training status tracking...")
        
        // Initialize controller
        assertTrue(controller.initialize())
        
        // Get initial status
        val initialStatus = controller.getTrainingStatus()
        assertEquals(0, initialStatus.currentEpisode)
        assertEquals(0, initialStatus.totalSteps)
        assertEquals(0.0, initialStatus.averageReward)
        assertEquals(Double.NEGATIVE_INFINITY, initialStatus.bestPerformance)
        
        // Run training
        val results = controller.startTraining(episodes = 2)
        assertNotNull(results)
        
        // Status should reflect completed training
        val finalStatus = controller.getTrainingStatus()
        assertFalse(finalStatus.isTraining)
        
        println("✅ Training status tracking verified")
    }
    
    @Test
    fun testTrainingTest() {
        println("Testing training test functionality...")
        
        // Run training test
        val testPassed = controller.runTrainingTest(episodes = 2)
        assertTrue(testPassed, "Training test should pass")
        
        println("✅ Training test functionality verified")
    }
    
    @Test
    fun testGameDemonstration() {
        println("Testing game demonstration...")
        
        // Initialize controller
        assertTrue(controller.initialize())
        
        // Run game demonstration
        val demonstration = controller.demonstrateGame()
        assertNotNull(demonstration, "Game demonstration should return results")
        
        // Verify demonstration results
        assertTrue(demonstration.totalMoves >= 0)
        assertNotNull(demonstration.finalStatus)
        assertNotNull(demonstration.moves)
        assertNotNull(demonstration.chessMetrics)
        
        // Check move structure
        for (move in demonstration.moves) {
            assertTrue(move.step > 0)
            assertTrue(move.reward.isFinite())
            assertNotNull(move.gameStatus)
            assertNotNull(move.move)
        }
        
        // Verify chess metrics
        val chessMetrics = demonstration.chessMetrics
        assertTrue(chessMetrics.gameLength >= 0)
        assertTrue(chessMetrics.moveCount >= 0)
        assertTrue(chessMetrics.captureCount >= 0)
        
        println("✅ Game demonstration verified")
    }
    
    @Test
    fun testPerformanceAnalysis() {
        println("Testing performance analysis...")
        
        // Initialize controller
        assertTrue(controller.initialize())
        
        // Run performance analysis
        val analysis = controller.analyzePerformance()
        assertNotNull(analysis, "Performance analysis should return results")
        
        // Verify analysis results
        assertTrue(analysis.testsPerformed > 0)
        assertTrue(analysis.averageMaxQValue.isFinite())
        assertTrue(analysis.averageMinQValue.isFinite())
        assertTrue(analysis.averageEntropy >= 0.0)
        assertTrue(analysis.averageValidActions > 0.0)
        
        // Check test results
        assertEquals(analysis.testsPerformed, analysis.testResults.size)
        
        for (test in analysis.testResults) {
            assertTrue(test.testIndex >= 0)
            assertTrue(test.validActions > 0)
            assertTrue(test.maxQValue.isFinite())
            assertTrue(test.minQValue.isFinite())
            assertTrue(test.avgQValue.isFinite())
            assertTrue(test.maxProbability >= 0.0 && test.maxProbability <= 1.0)
            assertTrue(test.entropyScore >= 0.0)
        }
        
        println("✅ Performance analysis verified")
    }
    
    @Test
    fun testDifferentAgentTypes() {
        println("Testing different agent types...")
        
        // Test DQN agent
        val dqnController = TrainingController(
            config = TrainingControllerConfig(
                agentType = AgentType.DQN,
                hiddenLayers = listOf(16, 8),
                learningRate = 0.01
            )
        )
        
        assertTrue(dqnController.initialize())
        val dqnResults = dqnController.startTraining(episodes = 2)
        assertNotNull(dqnResults)
        
        // Test Policy Gradient agent
        val pgController = TrainingController(
            config = TrainingControllerConfig(
                agentType = AgentType.POLICY_GRADIENT,
                hiddenLayers = listOf(16, 8),
                learningRate = 0.01,
                temperature = 1.0
            )
        )
        
        assertTrue(pgController.initialize())
        val pgResults = pgController.startTraining(episodes = 2)
        assertNotNull(pgResults)
        
        println("✅ Different agent types verified")
    }
    
    @Test
    fun testConfigurationVariations() {
        println("Testing configuration variations...")
        
        // Test with position rewards enabled
        val positionRewardController = TrainingController(
            config = TrainingControllerConfig(
                hiddenLayers = listOf(16),
                enablePositionRewards = true,
                winReward = 2.0,
                lossReward = -2.0,
                drawReward = 0.5
            )
        )
        
        assertTrue(positionRewardController.initialize())
        val positionResults = positionRewardController.startTraining(episodes = 2)
        assertNotNull(positionResults)
        
        // Test with different batch configuration
        val batchController = TrainingController(
            config = TrainingControllerConfig(
                hiddenLayers = listOf(16),
                batchSize = 4,
                batchTrainingFrequency = 1,
                maxBufferSize = 20
            )
        )
        
        assertTrue(batchController.initialize())
        val batchResults = batchController.startTraining(episodes = 3)
        assertNotNull(batchResults)
        
        println("✅ Configuration variations verified")
    }
    
    @Test
    fun testErrorHandling() {
        println("Testing error handling...")
        
        // Test starting training without initialization
        val uninitializedController = TrainingController()
        val results = uninitializedController.startTraining(episodes = 1)
        assertNull(results, "Training should fail without initialization")
        
        // Test double training start
        assertTrue(controller.initialize())
        val firstResults = controller.startTraining(episodes = 1)
        assertNotNull(firstResults)
        
        // Second training should work (first one completed)
        val secondResults = controller.startTraining(episodes = 1)
        assertNotNull(secondResults)
        
        println("✅ Error handling verified")
    }
    
    @Test
    fun testTrainingControlOperations() {
        println("Testing training control operations...")
        
        // Initialize controller
        assertTrue(controller.initialize())
        
        // Test pause/resume on non-running training
        controller.pauseTraining() // Should handle gracefully
        controller.resumeTraining() // Should handle gracefully
        controller.stopTraining() // Should handle gracefully
        
        // These operations should not crash
        val status = controller.getTrainingStatus()
        assertFalse(status.isTraining)
        assertFalse(status.isPaused)
        
        println("✅ Training control operations verified")
    }
    
    @Test
    fun testComprehensiveIntegration() {
        println("Testing comprehensive integration...")
        
        // Initialize controller
        assertTrue(controller.initialize())
        
        // Run game demonstration
        val demonstration = controller.demonstrateGame()
        assertNotNull(demonstration)
        
        // Run performance analysis
        val analysis = controller.analyzePerformance()
        assertNotNull(analysis)
        
        // Run training test
        val testPassed = controller.runTrainingTest(episodes = 2)
        assertTrue(testPassed)
        
        // Run full training
        val results = controller.startTraining(episodes = 3)
        assertNotNull(results)
        
        // Verify all components worked together
        assertTrue(results.totalEpisodes > 0)
        assertTrue(results.totalSteps > 0)
        assertTrue(results.episodeHistory.isNotEmpty())
        
        println("✅ Comprehensive integration verified")
    }
    
    @Test
    fun testTrainingResultsValidation() {
        println("Testing training results validation...")
        
        // Initialize and run training
        assertTrue(controller.initialize())
        val results = controller.startTraining(episodes = 4)
        assertNotNull(results)
        
        // Validate results structure
        assertEquals(4, results.totalEpisodes)
        assertTrue(results.totalSteps > 0)
        assertTrue(results.trainingDuration >= 0)
        assertTrue(results.bestPerformance.isFinite())
        assertEquals(4, results.episodeHistory.size)
        
        // Validate episode history
        for (i in 0 until 4) {
            val episode = results.episodeHistory[i]
            assertEquals(i + 1, episode.episode)
            assertTrue(episode.steps > 0)
            assertTrue(episode.reward.isFinite())
            assertTrue(episode.duration >= 0)
            assertTrue(episode.bufferSize >= 0)
            assertTrue(episode.explorationRate >= 0.0)
        }
        
        // Validate final metrics
        val finalMetrics = results.finalMetrics
        assertEquals(4, finalMetrics.totalEpisodes)
        assertTrue(finalMetrics.totalSteps > 0)
        assertTrue(finalMetrics.averageReward.isFinite())
        assertTrue(finalMetrics.averageGameLength > 0)
        
        // Validate game outcome rates
        val totalRate = finalMetrics.winRate + finalMetrics.drawRate + finalMetrics.lossRate
        assertTrue(kotlin.math.abs(totalRate - 1.0) < 0.01)
        
        println("✅ Training results validation verified")
    }
}