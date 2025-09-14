package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.test.*

/**
 * Comprehensive integration tests for the advanced self-play training pipeline
 * covering enhanced training schedule, checkpointing, and experience management.
 */
class AdvancedSelfPlayTrainingPipelineTest {
    
    private lateinit var pipeline: AdvancedSelfPlayTrainingPipeline
    private lateinit var config: AdvancedSelfPlayConfig
    
    @BeforeTest
    fun setup() {
        config = AdvancedSelfPlayConfig(
            initialGamesPerCycle = 2,
            minGamesPerCycle = 1,
            maxGamesPerCycle = 4,
            maxConcurrentGames = 2,
            maxStepsPerGame = 20,
            evaluationGamesPerCycle = 1,
            batchSize = 8,
            maxBatchesPerCycle = 2,
            maxExperienceBufferSize = 400,
            checkpointInterval = 2,
            cycleReportInterval = 2,
            enableEarlyStopping = false, // Disable for testing
            convergenceConfig = ConvergenceConfig(windowSize = 3)
        )
        
        pipeline = AdvancedSelfPlayTrainingPipeline(config)
    }
    
    @Test
    fun testPipelineInitialization() {
        println("🧪 Testing pipeline initialization")
        
        val initResult = pipeline.initialize()
        assertTrue(initResult, "Pipeline should initialize successfully")
        
        val status = pipeline.getTrainingStatus()
        assertFalse(status.isTraining, "Pipeline should not be training initially")
        assertEquals(0, status.currentCycle, "Current cycle should be 0")
        assertEquals(0, status.totalCycles, "Total cycles should be 0")
    }
    
    @Test
    fun testShortTrainingCycle() {
        println("🧪 Testing short training cycle")
        
        assertTrue(pipeline.initialize(), "Pipeline initialization should succeed")
        
        val results = pipeline.runAdvancedTraining(totalCycles = 2)
        
        // Verify training completed
        assertEquals(3, results.totalCycles, "Should complete 3 training cycles")
        assertTrue(results.totalDuration > 0, "Training should take some time")
        assertTrue(results.cycleHistory.size == 3, "Should have 3 cycle results")
        assertTrue(results.performanceHistory.size == 3, "Should have 3 performance measurements")
        
        // Verify cycle structure
        for ((index, cycle) in results.cycleHistory.withIndex()) {
            assertEquals(index + 1, cycle.cycle, "Cycle number should match")
            assertTrue(cycle.selfPlayResults.totalGames > 0, "Should play some games")
            assertTrue(cycle.selfPlayResults.totalExperiences > 0, "Should collect experiences")
            assertTrue(cycle.trainingResults.totalBatches > 0, "Should perform batch training")
            assertTrue(cycle.performance.gamesPlayed > 0, "Should evaluate performance")
            assertTrue(cycle.cycleDuration > 0, "Cycle should take some time")
        }
        
        // Verify final metrics
        val finalMetrics = results.finalMetrics
        assertTrue(finalMetrics.totalGamesPlayed > 0, "Should play games overall")
        assertTrue(finalMetrics.totalExperiencesProcessed > 0, "Should process experiences")
        assertTrue(finalMetrics.totalBatchUpdates > 0, "Should perform batch updates")
        assertTrue(finalMetrics.modelVersionsCreated > 0, "Should create model versions")
    }
    
    @Test
    fun testAdaptiveScheduling() {
        println("🧪 Testing adaptive scheduling")
        
        assertTrue(pipeline.initialize(), "Pipeline initialization should succeed")
        
        val results = pipeline.runAdvancedTraining(totalCycles = 2)
        
        // Verify adaptive scheduling changes
        val schedulingStates = results.cycleHistory.map { it.adaptiveScheduling }
        
        // Check that scheduling parameters are within bounds
        for (state in schedulingStates) {
            assertTrue(state.gamesPerCycle >= config.minGamesPerCycle, 
                "Games per cycle should be >= minimum")
            assertTrue(state.gamesPerCycle <= config.maxGamesPerCycle, 
                "Games per cycle should be <= maximum")
            assertTrue(state.trainingRatio >= config.minTrainingRatio, 
                "Training ratio should be >= minimum")
            assertTrue(state.trainingRatio <= config.maxTrainingRatio, 
                "Training ratio should be <= maximum")
            assertEquals(config.batchSize, state.batchSize, "Batch size should remain constant")
        }
        
        println("   Adaptive scheduling states:")
        schedulingStates.forEachIndexed { index, state ->
            println("   Cycle ${index + 1}: games=${state.gamesPerCycle}, ratio=${state.trainingRatio}")
        }
    }
    
    @Test
    fun testExperienceProcessing() {
        println("🧪 Testing experience processing")
        
        assertTrue(pipeline.initialize(), "Pipeline initialization should succeed")
        
        val results = pipeline.runAdvancedTraining(totalCycles = 1)
        
        // Verify experience processing
        for (cycle in results.cycleHistory) {
            val experienceProcessing = cycle.experienceProcessing
            
            assertTrue(experienceProcessing.totalExperiences > 0, 
                "Should process some experiences")
            assertTrue(experienceProcessing.acceptedExperiences >= 0, 
                "Accepted experiences should be non-negative")
            assertTrue(experienceProcessing.discardedExperiences >= 0, 
                "Discarded experiences should be non-negative")
            assertEquals(experienceProcessing.totalExperiences, 
                experienceProcessing.acceptedExperiences + experienceProcessing.discardedExperiences,
                "Total should equal accepted + discarded")
            
            assertTrue(experienceProcessing.averageQuality >= 0.0, 
                "Average quality should be non-negative")
            assertTrue(experienceProcessing.averageQuality <= 1.0, 
                "Average quality should be <= 1.0")
            
            // Verify quality distribution
            val qualityDist = experienceProcessing.qualityDistribution
            assertTrue(qualityDist.isNotEmpty(), "Quality distribution should not be empty")
            
            // Verify buffer updates
            val bufferUpdates = experienceProcessing.bufferUpdates
            assertTrue(bufferUpdates.addedToMain >= 0, "Added to main should be non-negative")
            assertTrue(bufferUpdates.mainBufferSize >= 0, "Main buffer size should be non-negative")
            
            println("   Cycle ${cycle.cycle}: processed ${experienceProcessing.totalExperiences}, " +
                   "accepted ${experienceProcessing.acceptedExperiences}, " +
                   "quality ${experienceProcessing.averageQuality}")
        }
    }
    
    @Test
    fun testValidatedBatchTraining() {
        println("🧪 Testing validated batch training")
        
        assertTrue(pipeline.initialize(), "Pipeline initialization should succeed")
        
        val results = pipeline.runAdvancedTraining(totalCycles = 1)
        
        // Verify batch training validation
        for (cycle in results.cycleHistory) {
            val trainingResults = cycle.trainingResults
            
            assertTrue(trainingResults.totalBatches > 0, "Should perform some batch training")
            assertTrue(trainingResults.validBatches >= 0, "Valid batches should be non-negative")
            assertTrue(trainingResults.validBatches <= trainingResults.totalBatches, 
                "Valid batches should not exceed total batches")
            
            // Verify training metrics are reasonable
            assertFalse(trainingResults.averageLoss.isNaN(), "Average loss should not be NaN")
            assertFalse(trainingResults.averageLoss.isInfinite(), "Average loss should not be infinite")
            assertTrue(trainingResults.averageLoss >= 0.0, "Average loss should be non-negative")
            
            assertFalse(trainingResults.averageGradientNorm.isNaN(), "Gradient norm should not be NaN")
            assertFalse(trainingResults.averageGradientNorm.isInfinite(), "Gradient norm should not be infinite")
            assertTrue(trainingResults.averageGradientNorm >= 0.0, "Gradient norm should be non-negative")
            
            assertTrue(trainingResults.averagePolicyEntropy >= 0.0, "Policy entropy should be non-negative")
            
            assertTrue(trainingResults.averageExperienceQuality >= 0.0, 
                "Experience quality should be non-negative")
            assertTrue(trainingResults.averageExperienceQuality <= 1.0, 
                "Experience quality should be <= 1.0")
            
            // Verify batch results
            assertTrue(trainingResults.batchResults.isNotEmpty(), "Should have batch results")
            for (batchResult in trainingResults.batchResults) {
                assertTrue(batchResult.batchSize > 0, "Batch size should be positive")
                assertTrue(batchResult.experienceQuality >= 0.0, "Experience quality should be non-negative")
                assertNotNull(batchResult.updateResult, "Should have update result")
                assertNotNull(batchResult.validationResult, "Should have validation result")
            }
            
            println("   Cycle ${cycle.cycle}: ${trainingResults.totalBatches} batches, " +
                   "${trainingResults.validBatches} valid, loss ${trainingResults.averageLoss}")
        }
    }
    
    @Test
    fun testPerformanceEvaluation() {
        println("🧪 Testing performance evaluation")
        
        assertTrue(pipeline.initialize(), "Pipeline initialization should succeed")
        
        val results = pipeline.runAdvancedTraining(totalCycles = 1)
        
        // Verify performance evaluation
        for (cycle in results.cycleHistory) {
            val performance = cycle.performance
            
            assertEquals(cycle.cycle, performance.cycle, "Performance cycle should match")
            assertTrue(performance.gamesPlayed > 0, "Should play evaluation games")
            assertTrue(performance.gamesPlayed <= config.evaluationGamesPerCycle, 
                "Should not exceed max evaluation games")
            
            // Verify performance metrics
            assertTrue(performance.averageGameLength > 0, "Average game length should be positive")
            assertTrue(performance.averageGameLength <= config.maxStepsPerGame, 
                "Average game length should not exceed max steps")
            
            assertTrue(performance.winRate >= 0.0, "Win rate should be non-negative")
            assertTrue(performance.winRate <= 1.0, "Win rate should be <= 1.0")
            assertTrue(performance.drawRate >= 0.0, "Draw rate should be non-negative")
            assertTrue(performance.drawRate <= 1.0, "Draw rate should be <= 1.0")
            assertTrue(performance.lossRate >= 0.0, "Loss rate should be non-negative")
            assertTrue(performance.lossRate <= 1.0, "Loss rate should be <= 1.0")
            
            // Rates should sum to approximately 1.0 (allowing for rounding)
            val totalRate = performance.winRate + performance.drawRate + performance.lossRate
            assertTrue(kotlin.math.abs(totalRate - 1.0) < 0.1, "Win/draw/loss rates should sum to ~1.0")
            
            assertTrue(performance.performanceScore >= 0.0, "Performance score should be non-negative")
            assertTrue(performance.performanceScore <= 1.0, "Performance score should be <= 1.0")
            
            // Verify game results
            assertEquals(performance.gamesPlayed, performance.gameResults.size, 
                "Game results count should match games played")
            
            for (gameResult in performance.gameResults) {
                assertTrue(gameResult.gameLength > 0, "Game length should be positive")
                assertTrue(gameResult.gameLength <= config.maxStepsPerGame, 
                    "Game length should not exceed max steps")
                assertNotNull(gameResult.gameOutcome, "Game outcome should not be null")
                assertNotNull(gameResult.finalPosition, "Final position should not be null")
            }
            
            println("   Cycle ${cycle.cycle}: ${performance.gamesPlayed} games, " +
                   "win rate ${performance.winRate}, score ${performance.performanceScore}")
        }
    }
    
    @Test
    fun testCheckpointManagement() {
        println("🧪 Testing checkpoint management")
        
        val checkpointConfig = AdvancedSelfPlayConfig(
            initialGamesPerCycle = 3,
            maxStepsPerGame = 30,
            evaluationGamesPerCycle = 2,
            batchSize = 8,
            maxBatchesPerCycle = 3,
            checkpointInterval = 1, // Checkpoint every cycle
            maxModelVersions = 5
        )
        
        val testPipeline = AdvancedSelfPlayTrainingPipeline(checkpointConfig)
        assertTrue(testPipeline.initialize(), "Pipeline initialization should succeed")
        
        val results = testPipeline.runAdvancedTraining(totalCycles = 3)
        
        // Verify checkpoint creation
        val checkpointSummary = results.checkpointSummary
        assertTrue(checkpointSummary.totalCheckpoints > 0, "Should create checkpoints")
        assertTrue(checkpointSummary.totalCreated >= 3, "Should create at least 3 checkpoints")
        assertTrue(checkpointSummary.validCheckpoints >= 0, "Valid checkpoints should be non-negative")
        assertTrue(checkpointSummary.bestCheckpointVersion != null, "Should have a best checkpoint")
        assertTrue(checkpointSummary.totalSize > 0, "Total checkpoint size should be positive")
        
        println("   Checkpoints: ${checkpointSummary.totalCheckpoints} total, " +
               "${checkpointSummary.validCheckpoints} valid, " +
               "best version ${checkpointSummary.bestCheckpointVersion}")
    }
    
    @Test
    fun testExperienceStatistics() {
        println("🧪 Testing experience statistics")
        
        assertTrue(pipeline.initialize(), "Pipeline initialization should succeed")
        
        val results = pipeline.runAdvancedTraining(totalCycles = 2)
        
        // Verify experience statistics
        val experienceStats = results.experienceStatistics
        assertTrue(experienceStats.totalBufferSize >= 0, "Total buffer size should be non-negative")
        assertTrue(experienceStats.totalExperiencesProcessed > 0, "Should process some experiences")
        assertTrue(experienceStats.totalExperiencesDiscarded >= 0, "Discarded experiences should be non-negative")
        assertTrue(experienceStats.averageQuality >= 0.0, "Average quality should be non-negative")
        assertTrue(experienceStats.averageQuality <= 1.0, "Average quality should be <= 1.0")
        assertTrue(experienceStats.memoryUtilization >= 0.0, "Memory utilization should be non-negative")
        
        // Verify quality histogram
        assertTrue(experienceStats.qualityHistogram.isNotEmpty(), "Quality histogram should not be empty")
        for ((quality, count) in experienceStats.qualityHistogram) {
            assertTrue(quality >= 0.0, "Quality bucket should be non-negative")
            assertTrue(quality <= 1.0, "Quality bucket should be <= 1.0")
            assertTrue(count > 0, "Quality count should be positive")
        }
        
        // Verify sampling statistics
        assertTrue(experienceStats.samplingStatistics.isNotEmpty(), "Sampling statistics should not be empty")
        for ((_, stats) in experienceStats.samplingStatistics) {
            assertTrue(stats.timesUsed >= 0, "Times used should be non-negative")
            assertTrue(stats.totalExperiencesSampled >= 0, "Total sampled should be non-negative")
            assertTrue(stats.averageQuality >= 0.0, "Average quality should be non-negative")
        }
        
        println("   Experience stats: ${experienceStats.totalBufferSize} buffer size, " +
               "${experienceStats.totalExperiencesProcessed} processed, " +
               "quality ${experienceStats.averageQuality}")
    }
    
    @Test
    fun testConvergenceDetection() {
        println("🧪 Testing convergence detection")
        
        assertTrue(pipeline.initialize(), "Pipeline initialization should succeed")
        
        val results = pipeline.runAdvancedTraining(totalCycles = 6) // More cycles for convergence analysis
        
        // Verify convergence status
        val convergenceStatus = results.convergenceStatus
        assertNotNull(convergenceStatus, "Should have convergence status")
        assertTrue(convergenceStatus.confidence >= 0.0, "Confidence should be non-negative")
        assertTrue(convergenceStatus.confidence <= 1.0, "Confidence should be <= 1.0")
        assertTrue(convergenceStatus.stability >= 0.0, "Stability should be non-negative")
        assertTrue(convergenceStatus.stability <= 1.0, "Stability should be <= 1.0")
        assertNotNull(convergenceStatus.status, "Status should not be null")
        assertTrue(convergenceStatus.recommendations.isNotEmpty(), "Should have recommendations")
        
        println("   Convergence: ${convergenceStatus.hasConverged}, " +
               "confidence ${convergenceStatus.confidence}, " +
               "stability ${convergenceStatus.stability}")
        println("   Status: ${convergenceStatus.status}")
        println("   Recommendations: ${convergenceStatus.recommendations.joinToString("; ")}")
    }
    
    @Test
    fun testTrainingStatusMonitoring() {
        println("🧪 Testing training status monitoring")
        
        assertTrue(pipeline.initialize(), "Pipeline initialization should succeed")
        
        // Check initial status
        val initialStatus = pipeline.getTrainingStatus()
        assertFalse(initialStatus.isTraining, "Should not be training initially")
        assertEquals(0, initialStatus.currentCycle, "Current cycle should be 0")
        assertEquals(0, initialStatus.totalCycles, "Total cycles should be 0")
        
        // Start training in a separate thread (simulated)
        pipeline.runAdvancedTraining(totalCycles = 2)
        
        // Check final status
        val finalStatus = pipeline.getTrainingStatus()
        assertFalse(finalStatus.isTraining, "Should not be training after completion")
        assertTrue(finalStatus.completedCycles > 0, "Should have completed some cycles")
        
        println("   Final status: ${finalStatus.completedCycles} cycles completed, " +
               "best model version ${finalStatus.bestModelVersion}")
    }
    
    @Test
    fun testErrorHandling() {
        println("🧪 Testing error handling")
        
        // Test training without initialization
        assertFailsWith<IllegalStateException> {
            pipeline.runAdvancedTraining(totalCycles = 1)
        }
        
        // Test double initialization
        assertTrue(pipeline.initialize(), "First initialization should succeed")
        assertTrue(pipeline.initialize(), "Second initialization should also succeed")
        
        // Test training with zero cycles
        val zeroResults = pipeline.runAdvancedTraining(totalCycles = 0)
        assertEquals(0, zeroResults.totalCycles, "Should complete 0 cycles")
        assertEquals(0, zeroResults.cycleHistory.size, "Should have no cycle history")
    }
    
    @Test
    fun testMemoryManagement() {
        println("🧪 Testing memory management")
        
        val memoryConfig = AdvancedSelfPlayConfig(
            initialGamesPerCycle = 5,
            maxExperienceBufferSize = 100, // Small buffer to trigger cleanup
            experienceCleanupStrategy = ExperienceCleanupStrategy.LOWEST_QUALITY,
            enableMemoryOptimization = true,
            memoryCleanupInterval = 1 // Cleanup every cycle
        )
        
        val testPipeline = AdvancedSelfPlayTrainingPipeline(memoryConfig)
        assertTrue(testPipeline.initialize(), "Pipeline initialization should succeed")
        
        val results = testPipeline.runAdvancedTraining(totalCycles = 3)
        
        // Verify memory management occurred
        val experienceStats = results.experienceStatistics
        assertTrue(experienceStats.totalBufferSize <= memoryConfig.maxExperienceBufferSize, 
            "Buffer size should not exceed maximum")
        
        // Check that some experiences were processed and potentially discarded
        assertTrue(experienceStats.totalExperiencesProcessed > 0, "Should process experiences")
        
        println("   Memory management: ${experienceStats.totalBufferSize} buffer size, " +
               "${experienceStats.totalExperiencesDiscarded} discarded")
    }
}
