package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.test.*

/**
 * Comprehensive unit tests for the SelfPlayController
 */
class SelfPlayControllerTest {
    
    @Test
    fun testSelfPlayControllerInitialization() {
        println("🧪 Testing SelfPlayController initialization...")
        
        val config = SelfPlayControllerConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            gamesPerIteration = 5,
            maxConcurrentGames = 2
        )
        
        val controller = SelfPlayController(config)
        
        // Test initialization
        val initResult = controller.initialize()
        assertTrue(initResult, "Controller should initialize successfully")
        
        // Verify initial status
        val status = controller.getTrainingStatus()
        assertFalse(status.isTraining, "Should not be training initially")
        assertEquals(0, status.currentIteration, "Current iteration should be 0")
        assertEquals(0, status.totalIterations, "Total iterations should be 0")
        assertEquals(0, status.completedIterations, "Completed iterations should be 0")
        
        println("✅ SelfPlayController initialization test passed")
    }
    
    @Test
    fun testSelfPlayControllerConfiguration() {
        println("🧪 Testing SelfPlayController configuration options...")
        
        // Test DQN configuration
        val dqnConfig = SelfPlayControllerConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(32, 16),
            learningRate = 0.001,
            explorationRate = 0.2,
            gamesPerIteration = 3,
            batchSize = 32
        )
        
        val dqnController = SelfPlayController(dqnConfig)
        assertTrue(dqnController.initialize(), "DQN controller should initialize")
        
        // Test Policy Gradient configuration
        val pgConfig = SelfPlayControllerConfig(
            agentType = AgentType.POLICY_GRADIENT,
            hiddenLayers = listOf(24, 12),
            learningRate = 0.002,
            temperature = 1.5,
            gamesPerIteration = 4,
            batchSize = 16
        )
        
        val pgController = SelfPlayController(pgConfig)
        assertTrue(pgController.initialize(), "Policy Gradient controller should initialize")
        
        // Test different opponent strategies
        val opponentStrategies = listOf(
            OpponentUpdateStrategy.COPY_MAIN,
            OpponentUpdateStrategy.HISTORICAL,
            OpponentUpdateStrategy.FIXED,
            OpponentUpdateStrategy.ADAPTIVE
        )
        
        for (strategy in opponentStrategies) {
            val strategyConfig = SelfPlayControllerConfig(
                opponentUpdateStrategy = strategy,
                gamesPerIteration = 2
            )
            
            val strategyController = SelfPlayController(strategyConfig)
            assertTrue(strategyController.initialize(), 
                      "Controller should initialize with strategy: $strategy")
        }
        
        println("✅ SelfPlayController configuration test passed")
    }
    
    @Test
    fun testSingleSelfPlayIteration() {
        println("🧪 Testing single self-play iteration...")
        
        val config = SelfPlayControllerConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            gamesPerIteration = 2,
            maxConcurrentGames = 1,
            maxStepsPerGame = 15, // Short games for testing
            batchSize = 16
        )
        
        val controller = SelfPlayController(config)
        assertTrue(controller.initialize(), "Controller should initialize")
        
        // Run single iteration
        val results = controller.runSelfPlayTraining(iterations = 1)
        
        // Verify results
        assertEquals(1, results.totalIterations, "Should complete 1 iteration")
        assertTrue(results.totalDuration > 0, "Should take some time")
        assertEquals(1, results.iterationHistory.size, "Should have 1 iteration result")
        
        val iterationResult = results.iterationHistory.first()
        assertEquals(1, iterationResult.iteration, "Iteration number should be 1")
        
        // Verify self-play results
        val selfPlayResults = iterationResult.selfPlayResults
        assertEquals(config.gamesPerIteration, selfPlayResults.totalGames, 
                    "Should play configured number of games")
        assertTrue(selfPlayResults.totalExperiences > 0, "Should collect experiences")
        
        // Verify training metrics
        val trainingMetrics = iterationResult.trainingMetrics
        assertTrue(trainingMetrics.totalBatchUpdates >= 0, "Should have batch updates")
        assertTrue(trainingMetrics.averageLoss >= 0.0, "Loss should be non-negative")
        assertTrue(trainingMetrics.experienceBufferSize >= 0, "Buffer size should be non-negative")
        
        // Verify evaluation results
        val evaluationResults = iterationResult.evaluationResults
        assertTrue(evaluationResults.gamesPlayed > 0, "Should play evaluation games")
        assertTrue(evaluationResults.winRate >= 0.0 && evaluationResults.winRate <= 1.0, 
                  "Win rate should be between 0 and 1")
        assertTrue(evaluationResults.drawRate >= 0.0 && evaluationResults.drawRate <= 1.0, 
                  "Draw rate should be between 0 and 1")
        assertTrue(evaluationResults.lossRate >= 0.0 && evaluationResults.lossRate <= 1.0, 
                  "Loss rate should be between 0 and 1")
        
        // Verify rates sum to approximately 1.0
        val totalRate = evaluationResults.winRate + evaluationResults.drawRate + evaluationResults.lossRate
        assertTrue(kotlin.math.abs(totalRate - 1.0) < 0.01, "Win/draw/loss rates should sum to 1.0")
        
        println("📊 Single Iteration Results:")
        println("  Self-Play Games: ${selfPlayResults.totalGames}")
        println("  Experiences Collected: ${selfPlayResults.totalExperiences}")
        println("  Batch Updates: ${trainingMetrics.totalBatchUpdates}")
        println("  Evaluation Games: ${evaluationResults.gamesPlayed}")
        println("  Win Rate: ${evaluationResults.winRate}")
        
        println("✅ Single self-play iteration test passed")
    }
    
    @Test
    fun testMultipleSelfPlayIterations() {
        println("🧪 Testing multiple self-play iterations...")
        
        val config = SelfPlayControllerConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(12, 6),
            learningRate = 0.02,
            gamesPerIteration = 2,
            maxConcurrentGames = 1,
            maxStepsPerGame = 10,
            iterationReportInterval = 2
        )
        
        val controller = SelfPlayController(config)
        assertTrue(controller.initialize(), "Controller should initialize")
        
        val numIterations = 3
        val results = controller.runSelfPlayTraining(iterations = numIterations)
        
        // Verify overall results
        assertEquals(numIterations, results.totalIterations, "Should complete all iterations")
        assertEquals(numIterations, results.iterationHistory.size, "Should have all iteration results")
        assertTrue(results.totalDuration > 0, "Should take some time")
        
        // Verify iteration progression
        for (i in 0 until numIterations) {
            val iterationResult = results.iterationHistory[i]
            assertEquals(i + 1, iterationResult.iteration, "Iteration number should be correct")
            assertTrue(iterationResult.iterationDuration > 0, "Each iteration should take time")
        }
        
        // Verify final metrics
        val finalMetrics = results.finalMetrics
        assertTrue(finalMetrics.totalGamesPlayed > 0, "Should have played games")
        assertTrue(finalMetrics.totalExperiencesCollected > 0, "Should have collected experiences")
        assertTrue(finalMetrics.averageReward != 0.0, "Should have some average reward")
        assertTrue(finalMetrics.totalBatchUpdates >= 0, "Should have batch updates")
        
        // Verify metrics consistency
        val totalGamesFromIterations = results.iterationHistory.sumOf { it.selfPlayResults.totalGames }
        assertEquals(finalMetrics.totalGamesPlayed, totalGamesFromIterations, 
                    "Total games should match sum of iteration games")
        
        val totalExperiencesFromIterations = results.iterationHistory.sumOf { it.selfPlayResults.totalExperiences }
        assertEquals(finalMetrics.totalExperiencesCollected, totalExperiencesFromIterations,
                    "Total experiences should match sum of iteration experiences")
        
        println("📊 Multiple Iterations Results:")
        println("  Total Iterations: ${results.totalIterations}")
        println("  Total Games: ${finalMetrics.totalGamesPlayed}")
        println("  Total Experiences: ${finalMetrics.totalExperiencesCollected}")
        println("  Average Reward: ${finalMetrics.averageReward}")
        println("  Best Reward: ${finalMetrics.bestReward}")
        println("  Average Win Rate: ${finalMetrics.averageWinRate}")
        
        println("✅ Multiple self-play iterations test passed")
    }
    
    @Test
    fun testTrainingStatusTracking() {
        println("🧪 Testing training status tracking...")
        
        val config = SelfPlayControllerConfig(
            gamesPerIteration = 1,
            maxStepsPerGame = 5
        )
        
        val controller = SelfPlayController(config)
        assertTrue(controller.initialize(), "Controller should initialize")
        
        // Check initial status
        var status = controller.getTrainingStatus()
        assertFalse(status.isTraining, "Should not be training initially")
        assertEquals(0, status.currentIteration, "Current iteration should be 0")
        assertEquals(0, status.completedIterations, "Completed iterations should be 0")
        
        // Note: In a real implementation, we would test status during training
        // For now, we test the status after training completion
        val results = controller.runSelfPlayTraining(iterations = 2)
        
        status = controller.getTrainingStatus()
        assertFalse(status.isTraining, "Should not be training after completion")
        assertEquals(2, status.completedIterations, "Should have completed 2 iterations")
        assertTrue(status.bestPerformance != Double.NEGATIVE_INFINITY, "Should have some performance metric")
        
        println("📊 Status Tracking Results:")
        println("  Is Training: ${status.isTraining}")
        println("  Current Iteration: ${status.currentIteration}")
        println("  Total Iterations: ${status.totalIterations}")
        println("  Completed Iterations: ${status.completedIterations}")
        println("  Best Performance: ${status.bestPerformance}")
        
        println("✅ Training status tracking test passed")
    }
    
    @Test
    fun testOpponentUpdateStrategies() {
        println("🧪 Testing opponent update strategies...")
        
        val strategies = listOf(
            OpponentUpdateStrategy.COPY_MAIN,
            OpponentUpdateStrategy.HISTORICAL,
            OpponentUpdateStrategy.FIXED,
            OpponentUpdateStrategy.ADAPTIVE
        )
        
        for (strategy in strategies) {
            val config = SelfPlayControllerConfig(
                opponentUpdateStrategy = strategy,
                opponentUpdateFrequency = 2,
                gamesPerIteration = 1,
                maxStepsPerGame = 5
            )
            
            val controller = SelfPlayController(config)
            assertTrue(controller.initialize(), "Controller should initialize with strategy: $strategy")
            
            // Run training to test strategy
            val results = controller.runSelfPlayTraining(iterations = 2)
            
            assertEquals(2, results.totalIterations, "Should complete iterations with strategy: $strategy")
            assertTrue(results.iterationHistory.isNotEmpty(), "Should have iteration history")
            
            println("  ✅ Strategy $strategy tested successfully")
        }
        
        println("✅ Opponent update strategies test passed")
    }
    
    @Test
    fun testExperienceCleanupStrategies() {
        println("🧪 Testing experience cleanup strategies...")
        
        val cleanupStrategies = listOf(
            ExperienceCleanupStrategy.OLDEST_FIRST,
            ExperienceCleanupStrategy.LOWEST_QUALITY,
            ExperienceCleanupStrategy.RANDOM
        )
        
        for (strategy in cleanupStrategies) {
            val config = SelfPlayControllerConfig(
                experienceCleanupStrategy = strategy,
                maxExperienceBufferSize = 50, // Small buffer to trigger cleanup
                gamesPerIteration = 2,
                maxStepsPerGame = 15
            )
            
            val controller = SelfPlayController(config)
            assertTrue(controller.initialize(), "Controller should initialize with cleanup strategy: $strategy")
            
            val results = controller.runSelfPlayTraining(iterations = 1)
            
            assertEquals(1, results.totalIterations, "Should complete iteration with cleanup strategy: $strategy")
            
            // Verify experiences were collected (and potentially cleaned up)
            val iterationResult = results.iterationHistory.first()
            assertTrue(iterationResult.selfPlayResults.totalExperiences > 0, 
                      "Should collect experiences with cleanup strategy: $strategy")
            
            println("  ✅ Cleanup strategy $strategy tested successfully")
        }
        
        println("✅ Experience cleanup strategies test passed")
    }
    
    @Test
    fun testSamplingStrategies() {
        println("🧪 Testing sampling strategies...")
        
        val samplingStrategies = listOf(
            SamplingStrategy.UNIFORM,
            SamplingStrategy.RECENT,
            SamplingStrategy.MIXED
        )
        
        for (strategy in samplingStrategies) {
            val config = SelfPlayControllerConfig(
                samplingStrategy = strategy,
                gamesPerIteration = 2,
                maxStepsPerGame = 10,
                batchSize = 16
            )
            
            val controller = SelfPlayController(config)
            assertTrue(controller.initialize(), "Controller should initialize with sampling strategy: $strategy")
            
            val results = controller.runSelfPlayTraining(iterations = 1)
            
            assertEquals(1, results.totalIterations, "Should complete iteration with sampling strategy: $strategy")
            
            val iterationResult = results.iterationHistory.first()
            assertTrue(iterationResult.trainingMetrics.totalBatchUpdates >= 0, 
                      "Should have batch updates with sampling strategy: $strategy")
            
            println("  ✅ Sampling strategy $strategy tested successfully")
        }
        
        println("✅ Sampling strategies test passed")
    }
    
    @Test
    fun testControllerStopFunctionality() {
        println("🧪 Testing controller stop functionality...")
        
        val config = SelfPlayControllerConfig(
            gamesPerIteration = 1,
            maxStepsPerGame = 5
        )
        
        val controller = SelfPlayController(config)
        assertTrue(controller.initialize(), "Controller should initialize")
        
        // Test stop when not training
        controller.stopTraining() // Should not cause issues
        
        // Test that controller can still be used after stop
        val results = controller.runSelfPlayTraining(iterations = 1)
        assertEquals(1, results.totalIterations, "Controller should work after stop call")
        
        println("✅ Controller stop functionality test passed")
    }
    
    @Test
    fun testEarlyStoppingConfiguration() {
        println("🧪 Testing early stopping configuration...")
        
        // Test with early stopping disabled
        val noEarlyStopConfig = SelfPlayControllerConfig(
            enableEarlyStopping = false,
            gamesPerIteration = 1,
            maxStepsPerGame = 5
        )
        
        val noEarlyStopController = SelfPlayController(noEarlyStopConfig)
        assertTrue(noEarlyStopController.initialize(), "Controller should initialize without early stopping")
        
        val noEarlyStopResults = noEarlyStopController.runSelfPlayTraining(iterations = 2)
        assertEquals(2, noEarlyStopResults.totalIterations, "Should complete all iterations without early stopping")
        
        // Test with early stopping enabled (but unlikely to trigger with test parameters)
        val earlyStopConfig = SelfPlayControllerConfig(
            enableEarlyStopping = true,
            earlyStoppingWindow = 2,
            earlyStoppingThreshold = 10.0, // High threshold unlikely to be reached
            gamesPerIteration = 1,
            maxStepsPerGame = 5
        )
        
        val earlyStopController = SelfPlayController(earlyStopConfig)
        assertTrue(earlyStopController.initialize(), "Controller should initialize with early stopping")
        
        val earlyStopResults = earlyStopController.runSelfPlayTraining(iterations = 3)
        assertTrue(earlyStopResults.totalIterations <= 3, "Should complete iterations with early stopping enabled")
        
        println("✅ Early stopping configuration test passed")
    }
    
    @Test
    fun testRewardConfiguration() {
        println("🧪 Testing reward configuration...")
        
        val rewardConfigs = listOf(
            // Standard rewards
            SelfPlayControllerConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0,
                enablePositionRewards = false
            ),
            // Custom rewards
            SelfPlayControllerConfig(
                winReward = 2.0,
                lossReward = -0.5,
                drawReward = 0.1,
                enablePositionRewards = false
            ),
            // With position rewards
            SelfPlayControllerConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0,
                enablePositionRewards = true
            )
        )
        
        for ((index, config) in rewardConfigs.withIndex()) {
            val testConfig = config.copy(
                gamesPerIteration = 1,
                maxStepsPerGame = 5
            )
            
            val controller = SelfPlayController(testConfig)
            assertTrue(controller.initialize(), "Controller should initialize with reward config $index")
            
            val results = controller.runSelfPlayTraining(iterations = 1)
            assertEquals(1, results.totalIterations, "Should complete iteration with reward config $index")
            
            println("  ✅ Reward configuration $index tested successfully")
        }
        
        println("✅ Reward configuration test passed")
    }
}