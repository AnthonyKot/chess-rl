package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*
import kotlin.test.*

/**
 * End-to-end integration test for the complete chess RL training pipeline
 * This test validates the entire system from initialization to training completion
 */
class EndToEndTrainingTest {
    
    @Test
    fun testCompleteTrainingPipeline() {
        println("🚀 Testing Complete End-to-End Training Pipeline")
        
        // 1. Create and initialize training controller
        val controller = TrainingController(
            config = TrainingControllerConfig(
                agentType = AgentType.DQN,
                hiddenLayers = listOf(32, 16), // Small network for testing
                learningRate = 0.01,
                explorationRate = 0.2,
                maxStepsPerEpisode = 15,
                batchSize = 8,
                batchTrainingFrequency = 1,
                maxBufferSize = 50,
                progressReportInterval = 3,
                enableEarlyStopping = false
            )
        )
        
        // 2. Initialize controller
        assertTrue(controller.initialize(), "Controller should initialize successfully")
        
        // 3. Run game demonstration
        val demonstration = controller.demonstrateGame()
        assertNotNull(demonstration, "Game demonstration should succeed")
        assertTrue(demonstration.totalMoves >= 0)
        
        // 4. Run performance analysis
        val analysis = controller.analyzePerformance()
        assertNotNull(analysis, "Performance analysis should succeed")
        assertTrue(analysis.testsPerformed > 0)
        
        // 5. Run training test
        val testPassed = controller.runTrainingTest(episodes = 3)
        assertTrue(testPassed, "Training test should pass")
        
        // 6. Run full training session
        val results = controller.startTraining(episodes = 6)
        assertNotNull(results, "Training should complete successfully")
        
        // 7. Validate training results
        validateTrainingResults(results)
        
        println("✅ Complete end-to-end training pipeline test passed")
    }
    
    @Test
    fun testBatchTrainingEfficiency() {
        println("🔄 Testing Batch Training Efficiency")
        
        // Test different batch sizes for efficiency
        val batchSizes = listOf(4, 8, 16)
        val results = mutableListOf<Pair<Int, TrainingResults>>()
        
        for (batchSize in batchSizes) {
            println("Testing batch size: $batchSize")
            
            val agent = ChessAgentFactory.createDQNAgent(
                hiddenLayers = listOf(16, 8),
                learningRate = 0.01,
                explorationRate = 0.15,
                config = ChessAgentConfig(batchSize = batchSize)
            )
            
            val environment = ChessEnvironment()
            
            val pipeline = ChessTrainingPipeline(
                agent = agent,
                environment = environment,
                config = TrainingPipelineConfig(
                    maxStepsPerEpisode = 12,
                    batchSize = batchSize,
                    batchTrainingFrequency = 1,
                    maxBufferSize = 30,
                    progressReportInterval = 10
                )
            )
            
            val result = pipeline.train(totalEpisodes = 4)
            results.add(Pair(batchSize, result))
            
            // Validate each result
            assertEquals(4, result.totalEpisodes)
            assertTrue(result.totalSteps > 0)
        }
        
        // All batch sizes should produce valid results
        assertTrue(results.all { it.second.totalEpisodes == 4 })
        
        println("✅ Batch training efficiency test passed")
    }
    
    @Test
    fun testTrainingPipelineRobustness() {
        println("🛡️ Testing Training Pipeline Robustness")
        
        // Test with various configurations to ensure robustness
        val configurations = listOf(
            TrainingPipelineConfig(
                maxStepsPerEpisode = 5,
                batchSize = 4,
                samplingStrategy = SamplingStrategy.UNIFORM
            ),
            TrainingPipelineConfig(
                maxStepsPerEpisode = 10,
                batchSize = 8,
                samplingStrategy = SamplingStrategy.RECENT
            ),
            TrainingPipelineConfig(
                maxStepsPerEpisode = 15,
                batchSize = 6,
                samplingStrategy = SamplingStrategy.MIXED,
                updatesPerBatch = 2
            )
        )
        
        for ((index, config) in configurations.withIndex()) {
            println("Testing configuration ${index + 1}")
            
            val agent = ChessAgentFactory.createDQNAgent(
                hiddenLayers = listOf(16),
                learningRate = 0.01,
                explorationRate = 0.1
            )
            
            val environment = ChessEnvironment()
            
            val pipeline = ChessTrainingPipeline(
                agent = agent,
                environment = environment,
                config = config.copy(
                    maxBufferSize = 25,
                    progressReportInterval = 10
                )
            )
            
            val results = pipeline.train(totalEpisodes = 3)
            
            // Validate robustness
            assertNotNull(results)
            assertEquals(3, results.totalEpisodes)
            assertTrue(results.totalSteps > 0)
            assertTrue(results.trainingDuration >= 0)
        }
        
        println("✅ Training pipeline robustness test passed")
    }
    
    @Test
    fun testExperienceBufferManagement() {
        println("💾 Testing Experience Buffer Management")
        
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16),
            learningRate = 0.01,
            explorationRate = 0.2
        )
        
        val environment = ChessEnvironment()
        
        // Test with small buffer to force overflow
        val pipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 8,
                batchSize = 4,
                maxBufferSize = 12, // Small buffer
                batchTrainingFrequency = 1,
                progressReportInterval = 10
            )
        )
        
        val results = pipeline.train(totalEpisodes = 5)
        
        // Validate buffer management
        assertNotNull(results)
        assertEquals(5, results.totalEpisodes)
        assertTrue(results.finalMetrics.finalBufferSize <= 12)
        
        // Training should still work despite buffer overflow
        assertTrue(results.totalSteps > 0)
        assertTrue(results.episodeHistory.size == 5)
        
        println("✅ Experience buffer management test passed")
    }
    
    @Test
    fun testTrainingMetricsCollection() {
        println("📊 Testing Training Metrics Collection")
        
        val controller = TrainingController(
            config = TrainingControllerConfig(
                hiddenLayers = listOf(16),
                maxStepsPerEpisode = 10,
                batchSize = 4,
                progressReportInterval = 2
            )
        )
        
        assertTrue(controller.initialize())
        
        val results = controller.startTraining(episodes = 4)
        assertNotNull(results)
        
        // Validate comprehensive metrics collection
        assertEquals(4, results.totalEpisodes)
        assertTrue(results.totalSteps > 0)
        assertTrue(results.trainingDuration >= 0)
        assertTrue(results.bestPerformance.isFinite())
        
        // Validate episode history
        assertEquals(4, results.episodeHistory.size)
        for (episode in results.episodeHistory) {
            assertTrue(episode.steps > 0)
            assertTrue(episode.reward.isFinite())
            assertTrue(episode.duration >= 0)
            assertTrue(episode.bufferSize >= 0)
            assertTrue(episode.explorationRate >= 0.0)
            assertNotNull(episode.gameResult)
            assertNotNull(episode.chessMetrics)
        }
        
        // Validate final metrics
        val finalMetrics = results.finalMetrics
        assertEquals(4, finalMetrics.totalEpisodes)
        assertTrue(finalMetrics.averageReward.isFinite())
        assertTrue(finalMetrics.averageGameLength > 0)
        
        // Game outcome rates should be valid
        assertTrue(finalMetrics.winRate >= 0.0 && finalMetrics.winRate <= 1.0)
        assertTrue(finalMetrics.drawRate >= 0.0 && finalMetrics.drawRate <= 1.0)
        assertTrue(finalMetrics.lossRate >= 0.0 && finalMetrics.lossRate <= 1.0)
        
        val totalRate = finalMetrics.winRate + finalMetrics.drawRate + finalMetrics.lossRate
        assertTrue(kotlin.math.abs(totalRate - 1.0) < 0.01)
        
        println("✅ Training metrics collection test passed")
    }
    
    @Test
    fun testAgentTypeComparison() {
        println("🤖 Testing Different Agent Types")
        
        // Test DQN agent
        val dqnController = TrainingController(
            config = TrainingControllerConfig(
                agentType = AgentType.DQN,
                hiddenLayers = listOf(16),
                learningRate = 0.01,
                maxStepsPerEpisode = 8,
                batchSize = 4
            )
        )
        
        assertTrue(dqnController.initialize())
        val dqnResults = dqnController.startTraining(episodes = 3)
        assertNotNull(dqnResults)
        assertEquals(3, dqnResults.totalEpisodes)
        
        // Test Policy Gradient agent
        val pgController = TrainingController(
            config = TrainingControllerConfig(
                agentType = AgentType.POLICY_GRADIENT,
                hiddenLayers = listOf(16),
                learningRate = 0.01,
                temperature = 1.0,
                maxStepsPerEpisode = 8,
                batchSize = 4
            )
        )
        
        assertTrue(pgController.initialize())
        val pgResults = pgController.startTraining(episodes = 3)
        assertNotNull(pgResults)
        assertEquals(3, pgResults.totalEpisodes)
        
        // Both agents should produce valid results
        assertTrue(dqnResults.totalSteps > 0)
        assertTrue(pgResults.totalSteps > 0)
        
        println("✅ Agent type comparison test passed")
    }
    
    /**
     * Validate training results comprehensively
     */
    private fun validateTrainingResults(results: TrainingResults) {
        // Basic structure validation
        assertTrue(results.totalEpisodes > 0)
        assertTrue(results.totalSteps > 0)
        assertTrue(results.trainingDuration >= 0)
        assertTrue(results.bestPerformance.isFinite())
        
        // Episode history validation
        assertTrue(results.episodeHistory.isNotEmpty())
        assertEquals(results.totalEpisodes, results.episodeHistory.size)
        
        for ((index, episode) in results.episodeHistory.withIndex()) {
            assertEquals(index + 1, episode.episode)
            assertTrue(episode.steps > 0)
            assertTrue(episode.reward.isFinite())
            assertTrue(episode.duration >= 0)
            assertTrue(episode.bufferSize >= 0)
            assertTrue(episode.explorationRate >= 0.0)
            assertNotNull(episode.gameResult)
            
            // Chess metrics validation
            val chessMetrics = episode.chessMetrics
            assertTrue(chessMetrics.gameLength >= 0)
            assertTrue(chessMetrics.moveCount >= 0)
            assertTrue(chessMetrics.captureCount >= 0)
            assertTrue(chessMetrics.checkCount >= 0)
        }
        
        // Final metrics validation
        val finalMetrics = results.finalMetrics
        assertEquals(results.totalEpisodes, finalMetrics.totalEpisodes)
        assertTrue(finalMetrics.totalSteps > 0)
        assertTrue(finalMetrics.averageReward.isFinite())
        assertTrue(finalMetrics.averageGameLength > 0)
        
        // Game outcome validation
        assertTrue(finalMetrics.winRate >= 0.0 && finalMetrics.winRate <= 1.0)
        assertTrue(finalMetrics.drawRate >= 0.0 && finalMetrics.drawRate <= 1.0)
        assertTrue(finalMetrics.lossRate >= 0.0 && finalMetrics.lossRate <= 1.0)
        
        val totalRate = finalMetrics.winRate + finalMetrics.drawRate + finalMetrics.lossRate
        assertTrue(kotlin.math.abs(totalRate - 1.0) < 0.01, "Game outcome rates should sum to 1.0")
        
        // Batch training validation (if applicable)
        if (results.batchHistory.isNotEmpty()) {
            for (batch in results.batchHistory) {
                assertTrue(batch.batchSize > 0)
                assertTrue(batch.updatesPerformed > 0)
                assertTrue(batch.averageLoss >= 0.0)
                assertTrue(batch.averageGradientNorm >= 0.0)
                assertTrue(batch.averagePolicyEntropy >= 0.0)
                assertTrue(batch.bufferUtilization >= 0.0)
                assertTrue(batch.duration >= 0)
            }
        }
    }
}