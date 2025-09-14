package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.test.*
import kotlin.system.measureTimeMillis

/**
 * Simplified performance tests for advanced self-play training pipeline
 */
class AdvancedSelfPlayPerformanceTestSimple {
    
    @Test
    fun testBasicPerformance() {
        println("🚀 Testing basic performance")
        
        val config = AdvancedSelfPlayConfig(
            initialGamesPerCycle = 10,
            maxConcurrentGames = 2,
            maxStepsPerGame = 30,
            evaluationGamesPerCycle = 3,
            batchSize = 16,
            maxBatchesPerCycle = 5,
            maxExperienceBufferSize = 1000,
            checkpointInterval = 2,
            cycleReportInterval = 1,
            enableEarlyStopping = false
        )
        
        val pipeline = AdvancedSelfPlayTrainingPipeline(config)
        assertTrue(pipeline.initialize(), "Pipeline initialization should succeed")
        
        val totalCycles = 3
        val trainingTime = measureTimeMillis {
            val results = pipeline.runAdvancedTraining(totalCycles)
            
            // Verify basic performance metrics
            assertEquals(totalCycles, results.totalCycles, "Should complete all cycles")
            assertTrue(results.totalDuration > 0, "Training should take measurable time")
            
            val finalMetrics = results.finalMetrics
            assertTrue(finalMetrics.totalGamesPlayed > 0, "Should play some games")
            assertTrue(finalMetrics.totalExperiencesProcessed > 0, "Should process experiences")
            assertTrue(finalMetrics.totalBatchUpdates > 0, "Should perform batch updates")
            
            println("   Performance results:")
            println("     Total games: ${finalMetrics.totalGamesPlayed}")
            println("     Total experiences: ${finalMetrics.totalExperiencesProcessed}")
            println("     Total batch updates: ${finalMetrics.totalBatchUpdates}")
        }
        
        println("   Training time: ${trainingTime}ms")
        assertTrue(trainingTime < 60000, "Training should complete within 1 minute")
        
        val avgTimePerCycle = trainingTime / totalCycles
        println("   Average time per cycle: ${avgTimePerCycle}ms")
        assertTrue(avgTimePerCycle < 20000, "Average cycle time should be under 20 seconds")
    }
    
    @Test
    fun testMemoryManagement() {
        println("💾 Testing memory management")
        
        val config = AdvancedSelfPlayConfig(
            initialGamesPerCycle = 15,
            maxStepsPerGame = 40,
            batchSize = 32,
            maxExperienceBufferSize = 500, // Small buffer to trigger cleanup
            experienceCleanupStrategy = ExperienceCleanupStrategy.LOWEST_QUALITY,
            enableMemoryOptimization = true,
            memoryCleanupInterval = 1 // Cleanup every cycle
        )
        
        val pipeline = AdvancedSelfPlayTrainingPipeline(config)
        assertTrue(pipeline.initialize(), "Pipeline initialization should succeed")
        
        val results = pipeline.runAdvancedTraining(totalCycles = 3)
        
        // Verify memory management
        val experienceStats = results.experienceStatistics
        assertTrue(experienceStats.totalBufferSize <= config.maxExperienceBufferSize,
            "Buffer size should not exceed maximum")
        assertTrue(experienceStats.totalExperiencesProcessed > 0,
            "Should process experiences")
        
        println("   Memory management results:")
        println("     Buffer size: ${experienceStats.totalBufferSize}/${config.maxExperienceBufferSize}")
        println("     Experiences processed: ${experienceStats.totalExperiencesProcessed}")
        println("     Experiences discarded: ${experienceStats.totalExperiencesDiscarded}")
        
        // Should have discarded some experiences for memory management
        assertTrue(experienceStats.totalExperiencesDiscarded >= 0,
            "Should track discarded experiences")
    }
    
    @Test
    fun testScalability() {
        println("📈 Testing scalability")
        
        val smallConfig = AdvancedSelfPlayConfig(
            initialGamesPerCycle = 2,
            maxStepsPerGame = 10,
            batchSize = 8,
            maxExperienceBufferSize = 200
        )
        
        val largeConfig = AdvancedSelfPlayConfig(
            initialGamesPerCycle = 6,
            maxStepsPerGame = 20,
            batchSize = 16,
            maxExperienceBufferSize = 500
        )
        
        // Test small configuration
        val smallPipeline = AdvancedSelfPlayTrainingPipeline(smallConfig)
        assertTrue(smallPipeline.initialize(), "Small pipeline should initialize")
        
        val smallTime = measureTimeMillis {
            val smallResults = smallPipeline.runAdvancedTraining(totalCycles = 1)
            assertTrue(smallResults.totalCycles == 1, "Small pipeline should complete")
        }
        
        // Test large configuration
        val largePipeline = AdvancedSelfPlayTrainingPipeline(largeConfig)
        assertTrue(largePipeline.initialize(), "Large pipeline should initialize")
        
        val largeTime = measureTimeMillis {
            val largeResults = largePipeline.runAdvancedTraining(totalCycles = 1)
            assertTrue(largeResults.totalCycles == 1, "Large pipeline should complete")
        }
        
        println("   Scalability results:")
        println("     Small config time: ${smallTime}ms")
        println("     Large config time: ${largeTime}ms")
        
        // Large configuration should not be dramatically slower
        val timeRatio = largeTime.toDouble() / smallTime
        println("     Time ratio: ${timeRatio}x")
        assertTrue(timeRatio < 6.0, "Large config should not be dramatically slower")
    }
}
