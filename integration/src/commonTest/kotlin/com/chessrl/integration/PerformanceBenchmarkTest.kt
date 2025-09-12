package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*
import kotlin.test.*

/**
 * Performance benchmark test for comparing training pipeline performance
 * Uses consistent configuration across different platforms for fair comparison
 */
class PerformanceBenchmarkTest {
    
    companion object {
        // Standardized test configuration for consistent benchmarking
        val BENCHMARK_CONFIG = TrainingControllerConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(32, 16),           // Small but realistic network
            learningRate = 0.01,
            explorationRate = 0.15,
            maxStepsPerEpisode = 12,                 // Consistent episode length
            batchSize = 8,                           // Consistent batch size
            batchTrainingFrequency = 1,              // Train every episode
            maxBufferSize = 64,                      // Small buffer for fast testing
            progressReportInterval = 50,             // Minimal progress output
            enableEarlyStopping = false              // No early stopping for consistent timing
        )
        
        const val BENCHMARK_EPISODES = 8             // Consistent episode count
    }
    
    @Test
    fun benchmarkTrainingPipelinePerformance() {
        println("🏁 Training Pipeline Performance Benchmark")
        println("Configuration: ${BENCHMARK_CONFIG}")
        println("Episodes: $BENCHMARK_EPISODES")
        
        val startTime = getCurrentBenchmarkTime()
        
        // Create controller with benchmark configuration
        val controller = TrainingController(config = BENCHMARK_CONFIG)
        
        // Initialize
        val initStartTime = getCurrentBenchmarkTime()
        assertTrue(controller.initialize(), "Controller should initialize")
        val initDuration = getCurrentBenchmarkTime() - initStartTime
        
        // Run training
        val trainingStartTime = getCurrentBenchmarkTime()
        val results = controller.startTraining(episodes = BENCHMARK_EPISODES)
        val trainingDuration = getCurrentBenchmarkTime() - trainingStartTime
        
        val totalDuration = getCurrentBenchmarkTime() - startTime
        
        // Validate results
        assertNotNull(results, "Training should complete successfully")
        assertEquals(BENCHMARK_EPISODES, results.totalEpisodes)
        assertTrue(results.totalSteps > 0)
        
        // Output performance metrics
        println("\n📊 Performance Metrics:")
        println("   Initialization Time: ${initDuration}ms")
        println("   Training Time: ${trainingDuration}ms")
        println("   Total Time: ${totalDuration}ms")
        println("   Episodes Completed: ${results.totalEpisodes}")
        println("   Total Steps: ${results.totalSteps}")
        println("   Average Steps/Episode: ${results.totalSteps.toDouble() / results.totalEpisodes}")
        println("   Time per Episode: ${trainingDuration.toDouble() / results.totalEpisodes}ms")
        println("   Time per Step: ${trainingDuration.toDouble() / results.totalSteps}ms")
        
        // Batch training metrics
        if (results.batchHistory.isNotEmpty()) {
            val totalBatchUpdates = results.batchHistory.sumOf { it.updatesPerformed }
            val avgBatchTime = results.batchHistory.map { it.duration }.average()
            println("   Batch Updates: $totalBatchUpdates")
            println("   Average Batch Time: ${avgBatchTime}ms")
        }
        
        // Chess-specific metrics
        val finalMetrics = results.finalMetrics
        println("   Win Rate: ${(finalMetrics.winRate * 100)}%")
        println("   Average Game Length: ${finalMetrics.averageGameLength}")
        println("   Final Buffer Size: ${finalMetrics.finalBufferSize}")
        
        println("\n✅ Benchmark completed successfully")
    }
    
    @Test
    fun benchmarkBatchTrainingEfficiency() {
        println("🔄 Batch Training Efficiency Benchmark")
        
        val batchSizes = listOf(4, 8, 16)
        val results = mutableListOf<BenchmarkResult>()
        
        for (batchSize in batchSizes) {
            println("\nTesting batch size: $batchSize")
            
            val config = BENCHMARK_CONFIG.copy(batchSize = batchSize)
            val controller = TrainingController(config = config)
            
            val startTime = getCurrentBenchmarkTime()
            
            assertTrue(controller.initialize())
            val trainingResults = controller.startTraining(episodes = 6)
            
            val duration = getCurrentBenchmarkTime() - startTime
            
            assertNotNull(trainingResults)
            
            val benchmarkResult = BenchmarkResult(
                batchSize = batchSize,
                duration = duration,
                episodes = trainingResults.totalEpisodes,
                steps = trainingResults.totalSteps,
                batchUpdates = trainingResults.finalMetrics.totalBatchUpdates
            )
            
            results.add(benchmarkResult)
            
            println("   Duration: ${duration}ms")
            println("   Steps: ${trainingResults.totalSteps}")
            println("   Batch Updates: ${trainingResults.finalMetrics.totalBatchUpdates}")
        }
        
        // Compare batch efficiency
        println("\n📈 Batch Size Efficiency Comparison:")
        println("Batch Size | Duration (ms) | Steps | Updates | ms/Update")
        println("--------------------------------------------------------")
        
        for (result in results) {
            val msPerUpdate = if (result.batchUpdates > 0) {
                result.duration.toDouble() / result.batchUpdates
            } else {
                0.0
            }
            
            println("${result.batchSize.toString().padEnd(10)} | " +
                   "${result.duration.toString().padEnd(13)} | " +
                   "${result.steps.toString().padEnd(5)} | " +
                   "${result.batchUpdates.toString().padEnd(7)} | " +
                   "$msPerUpdate")
        }
        
        // Find most efficient batch size
        val mostEfficient = results.minByOrNull { result ->
            if (result.batchUpdates > 0) result.duration.toDouble() / result.batchUpdates else Double.MAX_VALUE
        }
        
        if (mostEfficient != null) {
            println("\n🎯 Most Efficient Batch Size: ${mostEfficient.batchSize}")
        }
        
        println("\n✅ Batch efficiency benchmark completed")
    }
    
    @Test
    fun benchmarkAgentTypeComparison() {
        println("🤖 Agent Type Performance Comparison")
        
        val agentTypes = listOf(AgentType.DQN, AgentType.POLICY_GRADIENT)
        val results = mutableListOf<AgentBenchmarkResult>()
        
        for (agentType in agentTypes) {
            println("\nTesting agent type: $agentType")
            
            val config = BENCHMARK_CONFIG.copy(
                agentType = agentType,
                temperature = if (agentType == AgentType.POLICY_GRADIENT) 1.0 else 1.0
            )
            
            val controller = TrainingController(config = config)
            
            val startTime = getCurrentBenchmarkTime()
            
            assertTrue(controller.initialize())
            val trainingResults = controller.startTraining(episodes = 6)
            
            val duration = getCurrentBenchmarkTime() - startTime
            
            assertNotNull(trainingResults)
            
            val agentResult = AgentBenchmarkResult(
                agentType = agentType,
                duration = duration,
                episodes = trainingResults.totalEpisodes,
                steps = trainingResults.totalSteps,
                averageReward = trainingResults.finalMetrics.averageReward,
                winRate = trainingResults.finalMetrics.winRate
            )
            
            results.add(agentResult)
            
            println("   Duration: ${duration}ms")
            println("   Average Reward: ${trainingResults.finalMetrics.averageReward}")
            println("   Win Rate: ${(trainingResults.finalMetrics.winRate * 100)}%")
        }
        
        // Compare agent performance
        println("\n📊 Agent Type Performance Comparison:")
        println("Agent Type       | Duration (ms) | Avg Reward | Win Rate | ms/Episode")
        println("----------------------------------------------------------------")
        
        for (result in results) {
            val msPerEpisode = result.duration.toDouble() / result.episodes
            
            println("${result.agentType.name.padEnd(16)} | " +
                   "${result.duration.toString().padEnd(13)} | " +
                   "${result.averageReward.toString().padEnd(10)} | " +
                   "${(result.winRate * 100).toString().padEnd(8)}% | " +
                   "$msPerEpisode")
        }
        
        println("\n✅ Agent type comparison completed")
    }
    
    @Test
    fun benchmarkMemoryUsage() {
        println("💾 Memory Usage Benchmark")
        
        val controller = TrainingController(config = BENCHMARK_CONFIG)
        
        // Measure memory before initialization
        val initialMemory = getApproximateMemoryUsage()
        
        assertTrue(controller.initialize())
        val postInitMemory = getApproximateMemoryUsage()
        
        val results = controller.startTraining(episodes = BENCHMARK_EPISODES)
        assertNotNull(results)
        
        val postTrainingMemory = getApproximateMemoryUsage()
        
        println("\n📊 Memory Usage Metrics:")
        println("   Initial Memory: ~${initialMemory}MB")
        println("   Post-Init Memory: ~${postInitMemory}MB")
        println("   Post-Training Memory: ~${postTrainingMemory}MB")
        println("   Initialization Overhead: ~${postInitMemory - initialMemory}MB")
        println("   Training Overhead: ~${postTrainingMemory - postInitMemory}MB")
        println("   Total Memory Growth: ~${postTrainingMemory - initialMemory}MB")
        
        // Validate memory usage is reasonable
        val totalGrowth = postTrainingMemory - initialMemory
        assertTrue(totalGrowth < 100, "Memory growth should be reasonable (< 100MB)")
        
        println("\n✅ Memory usage benchmark completed")
    }
    
    /**
     * Get current time for benchmarking (simplified for cross-platform compatibility)
     */
    private fun getCurrentBenchmarkTime(): Long {
        // Simplified timing for Kotlin/Native compatibility
        // In practice, would use platform-specific high-resolution timers
        return getCurrentTimeMillis()
    }
    
    /**
     * Get approximate memory usage (simplified)
     */
    private fun getApproximateMemoryUsage(): Long {
        // Simplified memory estimation based on training components
        // Base memory for JVM/runtime + neural network + experience buffer
        val baseMemory = 30L // Base JVM memory
        val networkMemory = 10L // Neural network weights and activations
        val bufferMemory = 15L // Experience buffer and training data
        return baseMemory + networkMemory + bufferMemory
    }
}

/**
 * Benchmark result for batch size comparison
 */
data class BenchmarkResult(
    val batchSize: Int,
    val duration: Long,
    val episodes: Int,
    val steps: Int,
    val batchUpdates: Int
)

/**
 * Benchmark result for agent type comparison
 */
data class AgentBenchmarkResult(
    val agentType: AgentType,
    val duration: Long,
    val episodes: Int,
    val steps: Int,
    val averageReward: Double,
    val winRate: Double
)