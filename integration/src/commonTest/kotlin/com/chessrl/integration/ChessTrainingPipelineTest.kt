package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*
import kotlin.test.*

/**
 * Comprehensive tests for the chess training pipeline with efficient batching
 */
class ChessTrainingPipelineTest {
    
    private lateinit var agent: ChessAgent
    private lateinit var environment: ChessEnvironment
    private lateinit var pipeline: ChessTrainingPipeline
    
    @BeforeTest
    fun setup() {
        // Create test agent with small network for fast testing
        agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(32, 16),
            learningRate = 0.01,
            explorationRate = 0.2,
            config = ChessAgentConfig(
                batchSize = 8,
                maxBufferSize = 100
            )
        )
        
        // Create test environment
        environment = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0
            )
        )
        
        // Create pipeline with test configuration
        pipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 20, // Short episodes for testing
                batchSize = 8,
                batchTrainingFrequency = 2, // Train every 2 episodes
                maxBufferSize = 50,
                progressReportInterval = 5,
                checkpointInterval = 10,
                enableEarlyStopping = false
            )
        )
    }
    
    @Test
    fun testPipelineInitialization() {
        // Test that pipeline initializes correctly
        assertNotNull(pipeline)
        
        // Test getting initial statistics
        val stats = pipeline.getCurrentStatistics()
        assertEquals(0, stats.currentEpisode)
        assertEquals(0, stats.totalSteps)
        assertEquals(0.0, stats.averageReward)
        assertEquals(0, stats.bufferSize)
    }
    
    @Test
    fun testSingleEpisodeTraining() {
        println("Testing single episode training...")
        
        // Run training for 1 episode
        val results = pipeline.train(totalEpisodes = 1)
        
        // Verify results
        assertNotNull(results)
        assertEquals(1, results.totalEpisodes)
        assertTrue(results.totalSteps > 0)
        assertTrue(results.episodeHistory.isNotEmpty())
        
        // Check episode metrics
        val episodeMetrics = results.episodeHistory.first()
        assertEquals(1, episodeMetrics.episode)
        assertTrue(episodeMetrics.steps > 0)
        assertNotNull(episodeMetrics.gameResult)
        assertTrue(episodeMetrics.duration >= 0)
        
        println("✅ Single episode training completed successfully")
    }
    
    @Test
    fun testMultipleEpisodeTraining() {
        println("Testing multiple episode training...")
        
        // Run training for 5 episodes
        val results = pipeline.train(totalEpisodes = 5)
        
        // Verify results
        assertNotNull(results)
        assertEquals(5, results.totalEpisodes)
        assertTrue(results.totalSteps > 0)
        assertEquals(5, results.episodeHistory.size)
        
        // Check that episodes are numbered correctly
        for (i in 0 until 5) {
            assertEquals(i + 1, results.episodeHistory[i].episode)
        }
        
        // Verify final metrics
        val finalMetrics = results.finalMetrics
        assertEquals(5, finalMetrics.totalEpisodes)
        assertTrue(finalMetrics.totalSteps > 0)
        assertTrue(finalMetrics.averageGameLength > 0)
        
        println("✅ Multiple episode training completed successfully")
    }
    
    @Test
    fun testBatchTraining() {
        println("Testing batch training functionality...")
        
        // Configure pipeline for frequent batch training
        val batchPipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 15,
                batchSize = 4,
                batchTrainingFrequency = 1, // Train every episode
                maxBufferSize = 20,
                progressReportInterval = 2
            )
        )
        
        // Run training
        val results = batchPipeline.train(totalEpisodes = 4)
        
        // Verify batch training occurred
        assertNotNull(results)
        assertTrue(results.batchHistory.isNotEmpty(), "Batch training should have occurred")
        
        // Check batch metrics
        val batchMetrics = results.batchHistory.first()
        assertTrue(batchMetrics.batchSize > 0)
        assertTrue(batchMetrics.updatesPerformed > 0)
        assertTrue(batchMetrics.averageLoss >= 0.0)
        assertTrue(batchMetrics.averageGradientNorm >= 0.0)
        assertTrue(batchMetrics.averagePolicyEntropy >= 0.0)
        assertTrue(batchMetrics.bufferUtilization >= 0.0)
        
        println("✅ Batch training functionality verified")
    }
    
    @Test
    fun testExperienceBufferManagement() {
        println("Testing experience buffer management...")
        
        // Create pipeline with small buffer
        val smallBufferPipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 10,
                batchSize = 4,
                maxBufferSize = 15, // Small buffer to test overflow
                batchTrainingFrequency = 1
            )
        )
        
        // Run enough episodes to overflow buffer
        val results = smallBufferPipeline.train(totalEpisodes = 5)
        
        // Verify buffer management
        assertNotNull(results)
        assertTrue(results.finalMetrics.finalBufferSize <= 15, "Buffer should not exceed max size")
        
        // Check that training still works with buffer overflow
        assertTrue(results.totalSteps > 0)
        assertTrue(results.episodeHistory.isNotEmpty())
        
        println("✅ Experience buffer management verified")
    }
    
    @Test
    fun testTrainingStatistics() {
        println("Testing training statistics collection...")
        
        // Run training
        val results = pipeline.train(totalEpisodes = 3)
        
        // Verify statistics
        assertNotNull(results)
        
        val finalMetrics = results.finalMetrics
        assertEquals(3, finalMetrics.totalEpisodes)
        assertTrue(finalMetrics.totalSteps > 0)
        
        // Check reward statistics
        assertTrue(finalMetrics.totalReward.isFinite())
        assertTrue(finalMetrics.averageReward.isFinite())
        assertTrue(finalMetrics.averageGameLength > 0)
        
        // Check game outcome rates
        assertTrue(finalMetrics.winRate >= 0.0 && finalMetrics.winRate <= 1.0)
        assertTrue(finalMetrics.drawRate >= 0.0 && finalMetrics.drawRate <= 1.0)
        assertTrue(finalMetrics.lossRate >= 0.0 && finalMetrics.lossRate <= 1.0)
        
        // Rates should sum to approximately 1.0
        val totalRate = finalMetrics.winRate + finalMetrics.drawRate + finalMetrics.lossRate
        assertTrue(kotlin.math.abs(totalRate - 1.0) < 0.01, "Game outcome rates should sum to 1.0")
        
        println("✅ Training statistics collection verified")
    }
    
    @Test
    fun testCurrentStatisticsTracking() {
        println("Testing current statistics tracking...")
        
        // Get initial statistics
        val initialStats = pipeline.getCurrentStatistics()
        assertEquals(0, initialStats.currentEpisode)
        assertEquals(0, initialStats.totalSteps)
        
        // Run partial training and check intermediate statistics
        // Note: This is a simplified test since we can't easily interrupt training
        val results = pipeline.train(totalEpisodes = 2)
        
        // Verify that statistics were updated
        assertNotNull(results)
        assertTrue(results.totalSteps > 0)
        
        println("✅ Current statistics tracking verified")
    }
    
    @Test
    fun testSamplingStrategies() {
        println("Testing different sampling strategies...")
        
        // Test UNIFORM sampling
        val uniformPipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 10,
                batchSize = 4,
                samplingStrategy = SamplingStrategy.UNIFORM,
                batchTrainingFrequency = 1
            )
        )
        
        val uniformResults = uniformPipeline.train(totalEpisodes = 3)
        assertNotNull(uniformResults)
        assertTrue(uniformResults.totalSteps > 0)
        
        // Test RECENT sampling
        val recentPipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 10,
                batchSize = 4,
                samplingStrategy = SamplingStrategy.RECENT,
                batchTrainingFrequency = 1
            )
        )
        
        val recentResults = recentPipeline.train(totalEpisodes = 3)
        assertNotNull(recentResults)
        assertTrue(recentResults.totalSteps > 0)
        
        // Test MIXED sampling
        val mixedPipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 10,
                batchSize = 4,
                samplingStrategy = SamplingStrategy.MIXED,
                batchTrainingFrequency = 1
            )
        )
        
        val mixedResults = mixedPipeline.train(totalEpisodes = 3)
        assertNotNull(mixedResults)
        assertTrue(mixedResults.totalSteps > 0)
        
        println("✅ Sampling strategies verified")
    }
    
    @Test
    fun testTrainingValidation() {
        println("Testing training validation...")
        
        // Create pipeline with validation thresholds
        val validationPipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 10,
                batchSize = 4,
                batchTrainingFrequency = 1,
                gradientClipThreshold = 5.0,
                minPolicyEntropy = 0.1
            )
        )
        
        // Run training - should complete without throwing validation errors
        val results = validationPipeline.train(totalEpisodes = 3)
        
        assertNotNull(results)
        assertTrue(results.totalSteps > 0)
        
        println("✅ Training validation verified")
    }
    
    @Test
    fun testPerformanceTracking() {
        println("Testing performance tracking...")
        
        // Run training
        val results = pipeline.train(totalEpisodes = 4)
        
        // Verify performance tracking
        assertNotNull(results)
        assertTrue(results.bestPerformance.isFinite())
        
        // Check that best performance is reasonable
        val rewards = results.episodeHistory.map { it.reward }
        val maxReward = rewards.maxOrNull() ?: Double.NEGATIVE_INFINITY
        assertEquals(maxReward, results.bestPerformance, 0.001)
        
        println("✅ Performance tracking verified")
    }
    
    @Test
    fun testChessSpecificMetrics() {
        println("Testing chess-specific metrics collection...")
        
        // Run training
        val results = pipeline.train(totalEpisodes = 2)
        
        // Verify chess metrics are collected
        assertNotNull(results)
        assertTrue(results.episodeHistory.isNotEmpty())
        
        val episodeMetrics = results.episodeHistory.first()
        val chessMetrics = episodeMetrics.chessMetrics
        
        // Verify chess metrics structure
        assertTrue(chessMetrics.gameLength >= 0)
        assertTrue(chessMetrics.totalMaterialValue >= 0)
        assertTrue(chessMetrics.moveCount >= 0)
        assertTrue(chessMetrics.captureCount >= 0)
        assertTrue(chessMetrics.checkCount >= 0)
        
        println("✅ Chess-specific metrics collection verified")
    }
    
    @Test
    fun testErrorHandling() {
        println("Testing error handling...")
        
        // Test with invalid configuration
        try {
            val invalidPipeline = ChessTrainingPipeline(
                agent = agent,
                environment = environment,
                config = TrainingPipelineConfig(
                    maxStepsPerEpisode = 0, // Invalid
                    batchSize = -1 // Invalid
                )
            )
            
            // Should handle gracefully
            val results = invalidPipeline.train(totalEpisodes = 1)
            // Test passes if no exception is thrown
            
        } catch (e: Exception) {
            // Expected for some invalid configurations
            assertTrue(e.message?.isNotEmpty() == true)
        }
        
        println("✅ Error handling verified")
    }
    
    @Test
    fun testTrainingPipelineIntegration() {
        println("Testing complete training pipeline integration...")
        
        // Create a comprehensive test configuration
        val integrationPipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 15,
                batchSize = 6,
                batchTrainingFrequency = 2,
                updatesPerBatch = 2,
                maxBufferSize = 30,
                progressReportInterval = 3,
                samplingStrategy = SamplingStrategy.MIXED,
                gradientClipThreshold = 10.0,
                minPolicyEntropy = 0.05
            )
        )
        
        // Run comprehensive training
        val results = integrationPipeline.train(totalEpisodes = 6)
        
        // Verify all components worked together
        assertNotNull(results)
        assertEquals(6, results.totalEpisodes)
        assertTrue(results.totalSteps > 0)
        assertTrue(results.trainingDuration >= 0)
        
        // Verify episode history
        assertEquals(6, results.episodeHistory.size)
        for (i in 0 until 6) {
            val episode = results.episodeHistory[i]
            assertEquals(i + 1, episode.episode)
            assertTrue(episode.steps > 0)
            assertTrue(episode.duration >= 0)
        }
        
        // Verify batch training occurred
        assertTrue(results.batchHistory.isNotEmpty())
        
        // Verify final metrics are comprehensive
        val finalMetrics = results.finalMetrics
        assertEquals(6, finalMetrics.totalEpisodes)
        assertTrue(finalMetrics.totalSteps > 0)
        assertTrue(finalMetrics.averageGameLength > 0)
        assertTrue(finalMetrics.totalBatchUpdates >= 0)
        
        println("✅ Complete training pipeline integration verified")
    }
}