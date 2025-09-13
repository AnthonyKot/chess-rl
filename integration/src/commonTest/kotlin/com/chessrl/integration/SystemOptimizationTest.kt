package com.chessrl.integration

import kotlin.test.*

class SystemOptimizationTest {
    
    @Test
    fun testJVMTrainingOptimization() {
        val optimizer = JVMTrainingOptimizer()
        val config = JVMTrainingOptimizer.JVMOptimizationConfig(
            batchSize = 64,
            memoryPoolSize = 10000,
            concurrentThreads = 2
        )
        
        val result = optimizer.optimizeForJVMTraining(config)
        
        // Verify optimization results
        assertNotNull(result.memoryOptimization)
        assertNotNull(result.batchOptimization)
        assertNotNull(result.concurrentOptimization)
        assertNotNull(result.recommendedConfig)
        
        // Check that optimization provides improvements
        assertTrue(result.batchOptimization.optimalBatchSize in 32..128)
        assertTrue(result.concurrentOptimization.optimalThreadCount >= 1)
        assertTrue(result.totalOptimizationTime > 0)
        
        println("JVM Optimization completed in ${result.totalOptimizationTime}ms")
        println("Optimal batch size: ${result.batchOptimization.optimalBatchSize}")
        println("Optimal thread count: ${result.concurrentOptimization.optimalThreadCount}")
    }
    
    @Test
    fun testNativeDeploymentOptimization() {
        val optimizer = NativeDeploymentOptimizer()
        val config = NativeDeploymentOptimizer.NativeOptimizationConfig(
            optimizeForInference = true,
            minimizeMemoryFootprint = true,
            optimizeGameplayPath = true
        )
        
        val result = optimizer.optimizeForNativeDeployment(config)
        
        // Verify optimization results
        assertNotNull(result.inferenceOptimization)
        assertNotNull(result.memoryOptimization)
        assertNotNull(result.gameplayOptimization)
        
        // Check optimization effectiveness
        assertTrue(result.inferenceOptimization.speedupFactor >= 1.0)
        assertTrue(result.memoryOptimization.memoryReduction >= 0.0)
        assertTrue(result.gameplayOptimization.gameplaySpeedup >= 1.0)
        assertTrue(result.totalOptimizationTime > 0)
        
        println("Native Optimization completed in ${result.totalOptimizationTime}ms")
        println("Inference speedup: ${result.inferenceOptimization.speedupFactor}x")
        println("Memory reduction: ${result.memoryOptimization.memoryReduction * 100}%")
        println("Gameplay speedup: ${result.gameplayOptimization.gameplaySpeedup}x")
    }
    
    @Test
    fun testHyperparameterOptimization() {
        val optimizer = HyperparameterOptimizer()
        val baseConfig = HyperparameterOptimizer.HyperparameterConfig(
            learningRate = 0.001,
            batchSize = 64,
            explorationRate = 0.1
        )
        
        val result = optimizer.optimizeHyperparameters(baseConfig, optimizationBudget = 10)
        
        // Verify optimization results
        assertNotNull(result.optimalConfig)
        assertNotNull(result.optimizationHistory)
        assertNotNull(result.abTestResult)
        
        // Check that optimization found improvements
        assertTrue(result.optimizationHistory.isNotEmpty())
        assertTrue(result.improvementFactor >= 1.0)
        assertTrue(result.totalOptimizationTime > 0)
        
        // Verify optimal config is reasonable
        assertTrue(result.optimalConfig.learningRate > 0.0)
        assertTrue(result.optimalConfig.batchSize > 0)
        assertTrue(result.optimalConfig.explorationRate >= 0.0)
        
        println("Hyperparameter Optimization completed in ${result.totalOptimizationTime}ms")
        println("Improvement factor: ${result.improvementFactor}x")
        println("Optimal learning rate: ${result.optimalConfig.learningRate}")
        println("Optimal batch size: ${result.optimalConfig.batchSize}")
    }
    
    @Test
    fun testPerformanceMonitoring() {
        val monitor = PerformanceMonitor()
        
        // Start monitoring
        monitor.startMonitoring()
        
        // Simulate some work
        repeat(10) {
            val snapshot = monitor.collectPerformanceSnapshot()
            assertNotNull(snapshot.metrics)
            assertNotNull(snapshot.profiling)
            assertNotNull(snapshot.resources)
            assertTrue(snapshot.timestamp > 0)
        }
        
        // Generate performance report
        val report = monitor.generatePerformanceReport()
        assertNotNull(report.currentPerformance)
        assertNotNull(report.historicalAnalysis)
        assertNotNull(report.identifiedBottlenecks)
        assertNotNull(report.optimizationRecommendations)
        assertNotNull(report.benchmarkResults)
        
        // Stop monitoring
        monitor.stopMonitoring()
        
        println("Performance monitoring test completed")
        println("Current throughput: ${report.currentPerformance.metrics.throughput} ops/sec")
        println("Identified bottlenecks: ${report.identifiedBottlenecks.size}")
        println("Optimization recommendations: ${report.optimizationRecommendations.size}")
    }
    
    @Test
    fun testSystemOptimizationCoordinator() {
        val coordinator = SystemOptimizationCoordinator()
        val config = SystemOptimizationCoordinator.SystemOptimizationConfig(
            optimizeJVMTraining = true,
            optimizeNativeDeployment = true,
            optimizeHyperparameters = true,
            enablePerformanceMonitoring = true,
            hyperparameterBudget = 5 // Reduced for testing
        )
        
        val result = coordinator.performSystemOptimization(config)
        
        // Verify all optimizations were performed
        assertNotNull(result.jvmOptimization)
        assertNotNull(result.nativeOptimization)
        assertNotNull(result.hyperparameterOptimization)
        assertNotNull(result.performanceReport)
        assertNotNull(result.optimizationSummary)
        
        // Check optimization summary
        assertTrue(result.optimizationSummary.totalSpeedupFactor >= 1.0)
        assertTrue(result.optimizationSummary.keyImprovements.isNotEmpty())
        assertTrue(result.totalOptimizationTime > 0)
        
        println("System optimization completed in ${result.totalOptimizationTime}ms")
        println("Total speedup factor: ${result.optimizationSummary.totalSpeedupFactor}x")
        println("Key improvements: ${result.optimizationSummary.keyImprovements.size}")
    }
    
    @Test
    fun testOptimizationGuidelines() {
        val coordinator = SystemOptimizationCoordinator()
        val guidelines = coordinator.generateOptimizationGuidelines()
        
        // Verify all guideline categories are present
        assertTrue(guidelines.jvmTrainingGuidelines.isNotEmpty())
        assertTrue(guidelines.nativeDeploymentGuidelines.isNotEmpty())
        assertTrue(guidelines.hyperparameterTuningGuidelines.isNotEmpty())
        assertTrue(guidelines.performanceMonitoringGuidelines.isNotEmpty())
        assertTrue(guidelines.bestPractices.isNotEmpty())
        
        println("Generated optimization guidelines:")
        println("JVM guidelines: ${guidelines.jvmTrainingGuidelines.size}")
        println("Native guidelines: ${guidelines.nativeDeploymentGuidelines.size}")
        println("Hyperparameter guidelines: ${guidelines.hyperparameterTuningGuidelines.size}")
        println("Monitoring guidelines: ${guidelines.performanceMonitoringGuidelines.size}")
        println("Best practices: ${guidelines.bestPractices.size}")
    }
    
    @Test
    fun testPerformanceBenchmarkSuite() {
        val benchmarkSuite = PerformanceBenchmarkSuite()
        val results = benchmarkSuite.runBenchmarks()
        
        // Verify all benchmarks ran
        assertNotNull(results.neuralNetwork)
        assertNotNull(results.chessEngine)
        assertNotNull(results.rlAlgorithm)
        assertNotNull(results.integration)
        
        // Check benchmark results
        assertTrue(results.neuralNetwork.duration > 0)
        assertTrue(results.chessEngine.duration > 0)
        assertTrue(results.rlAlgorithm.duration > 0)
        assertTrue(results.integration.duration > 0)
        
        assertTrue(results.neuralNetwork.operationsPerSecond > 0)
        assertTrue(results.chessEngine.operationsPerSecond > 0)
        assertTrue(results.rlAlgorithm.operationsPerSecond > 0)
        assertTrue(results.integration.operationsPerSecond > 0)
        
        assertTrue(results.overallScore >= 0.0)
        
        println("Benchmark results:")
        println("Neural Network: ${results.neuralNetwork.operationsPerSecond} ops/sec")
        println("Chess Engine: ${results.chessEngine.operationsPerSecond} ops/sec")
        println("RL Algorithm: ${results.rlAlgorithm.operationsPerSecond} ops/sec")
        println("Integration: ${results.integration.operationsPerSecond} ops/sec")
        println("Overall score: ${results.overallScore}")
    }
    
    @Test
    fun testMemoryOptimization() {
        val memoryManager = TrainingMemoryManager()
        val config = JVMTrainingOptimizer.JVMOptimizationConfig(
            memoryPoolSize = 10000,
            batchSize = 64
        )
        
        val result = memoryManager.optimizeMemoryUsage(config)
        
        // Verify memory optimization results
        assertTrue(result.optimalPoolSize > 0)
        assertTrue(result.memoryUsageReduction >= 0.0)
        assertTrue(result.allocationRate >= 0.0)
        
        println("Memory optimization results:")
        println("Optimal pool size: ${result.optimalPoolSize}")
        println("Memory usage reduction: ${result.memoryUsageReduction * 100}%")
        println("GC optimization effective: ${result.gcOptimizationEffective}")
    }
    
    @Test
    fun testBatchProcessingOptimization() {
        val batchProcessor = OptimizedBatchProcessor()
        val config = JVMTrainingOptimizer.JVMOptimizationConfig(batchSize = 64)
        
        val result = batchProcessor.optimizeBatchProcessing(config)
        
        // Verify batch optimization results
        assertTrue(result.optimalBatchSize in 32..128)
        assertTrue(result.throughputImprovement >= 0.0)
        assertTrue(result.memoryEfficiency > 0.0)
        assertTrue(result.processingTime > 0)
        
        println("Batch processing optimization results:")
        println("Optimal batch size: ${result.optimalBatchSize}")
        println("Throughput improvement: ${result.throughputImprovement * 100}%")
        println("Memory efficiency: ${result.memoryEfficiency}")
    }
    
    @Test
    fun testConcurrentTrainingOptimization() {
        val concurrentManager = ConcurrentTrainingManager()
        val config = JVMTrainingOptimizer.JVMOptimizationConfig(concurrentThreads = 4)
        
        val result = concurrentManager.optimizeConcurrentTraining(config)
        
        // Verify concurrent optimization results
        assertTrue(result.optimalThreadCount >= 1)
        assertTrue(result.scalabilityFactor >= 1.0)
        assertTrue(result.resourceUtilization >= 0.0)
        assertTrue(result.synchronizationOverhead >= 0.0)
        
        println("Concurrent training optimization results:")
        println("Optimal thread count: ${result.optimalThreadCount}")
        println("Scalability factor: ${result.scalabilityFactor}x")
        println("Resource utilization: ${result.resourceUtilization * 100}%")
    }
}