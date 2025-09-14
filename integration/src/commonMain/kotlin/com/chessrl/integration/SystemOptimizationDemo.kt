package com.chessrl.integration

/**
 * System Optimization Demo
 * Demonstrates comprehensive system optimization capabilities
 */
class SystemOptimizationDemo {
    
    fun runComprehensiveOptimizationDemo() {
        println("=== Chess RL Bot System Optimization Demo ===\n")
        
        val coordinator = SystemOptimizationCoordinator()
        
        // 1. Run system optimization
        println("1. Running comprehensive system optimization...")
        val optimizationConfig = SystemOptimizationCoordinator.SystemOptimizationConfig(
            optimizeJVMTraining = true,
            optimizeNativeDeployment = true,
            optimizeHyperparameters = true,
            enablePerformanceMonitoring = true,
            hyperparameterBudget = 20
        )
        
        val optimizationResult = coordinator.performSystemOptimization(optimizationConfig)
        
        // 2. Generate optimization guidelines
        println("\n2. Generating optimization guidelines...")
        val guidelines = coordinator.generateOptimizationGuidelines()
        printOptimizationGuidelines(guidelines)
        
        // 3. Demonstrate individual optimization components
        println("\n3. Demonstrating individual optimization components...")
        demonstrateJVMOptimization()
        demonstrateNativeOptimization()
        demonstrateHyperparameterOptimization()
        demonstratePerformanceMonitoring()
        
        // 4. Show performance benchmarks
        println("\n4. Running performance benchmarks...")
        val benchmarkSuite = PerformanceBenchmarkSuite()
        val benchmarkResults = benchmarkSuite.runBenchmarks()
        printBenchmarkResults(benchmarkResults)
        
        println("\n=== System Optimization Demo Complete ===")
    }
    
    private fun printOptimizationGuidelines(guidelines: OptimizationGuidelines) {
        println("Generated Optimization Guidelines:")
        
        println("\nJVM Training Guidelines:")
        guidelines.jvmTrainingGuidelines.forEachIndexed { index, guideline ->
            println("  ${index + 1}. $guideline")
        }
        
        println("\nNative Deployment Guidelines:")
        guidelines.nativeDeploymentGuidelines.forEachIndexed { index, guideline ->
            println("  ${index + 1}. $guideline")
        }
        
        println("\nHyperparameter Tuning Guidelines:")
        guidelines.hyperparameterTuningGuidelines.forEachIndexed { index, guideline ->
            println("  ${index + 1}. $guideline")
        }
        
        println("\nPerformance Monitoring Guidelines:")
        guidelines.performanceMonitoringGuidelines.forEachIndexed { index, guideline ->
            println("  ${index + 1}. $guideline")
        }
        
        println("\nBest Practices:")
        guidelines.bestPractices.forEachIndexed { index, practice ->
            println("  ${index + 1}. $practice")
        }
    }
    
    private fun demonstrateJVMOptimization() {
        println("\n--- JVM Training Optimization Demo ---")
        
        val jvmOptimizer = JVMTrainingOptimizer()
        val config = JVMTrainingOptimizer.JVMOptimizationConfig(
            batchSize = 64,
            memoryPoolSize = 50000,
            concurrentThreads = 4
        )
        
        println("Optimizing JVM training with config:")
        println("  - Batch size: ${config.batchSize}")
        println("  - Memory pool size: ${config.memoryPoolSize}")
        println("  - Concurrent threads: ${config.concurrentThreads}")
        
        val result = jvmOptimizer.optimizeForJVMTraining(config)
        
        println("JVM Optimization Results:")
        println("  - Recommended batch size: ${result.recommendedConfig.batchSize}")
        println("  - Recommended memory pool: ${result.recommendedConfig.memoryPoolSize}")
        println("  - Recommended threads: ${result.recommendedConfig.concurrentThreads}")
        println("  - Throughput improvement: ${String.format("%.1f", result.batchOptimization.throughputImprovement * 100)}%")
        println("  - Memory reduction: ${String.format("%.1f", result.memoryOptimization.memoryUsageReduction * 100)}%")
        println("  - Scalability factor: ${String.format("%.2f", result.concurrentOptimization.scalabilityFactor)}x")
    }
    
    private fun demonstrateNativeOptimization() {
        println("\n--- Native Deployment Optimization Demo ---")
        
        val nativeOptimizer = NativeDeploymentOptimizer()
        val config = NativeDeploymentOptimizer.NativeOptimizationConfig(
            optimizeForInference = true,
            minimizeMemoryFootprint = true,
            optimizeGameplayPath = true
        )
        
        println("Optimizing native deployment with config:")
        println("  - Optimize inference: ${config.optimizeForInference}")
        println("  - Minimize memory: ${config.minimizeMemoryFootprint}")
        println("  - Optimize gameplay: ${config.optimizeGameplayPath}")
        
        val result = nativeOptimizer.optimizeForNativeDeployment(config)
        
        println("Native Optimization Results:")
        println("  - Inference speedup: ${String.format("%.2f", result.inferenceOptimization.speedupFactor)}x")
        println("  - Memory footprint: ${result.memoryOptimization.memoryFootprint / 1024 / 1024}MB")
        println("  - Memory reduction: ${String.format("%.1f", result.memoryOptimization.memoryReduction * 100)}%")
        println("  - Gameplay speedup: ${String.format("%.2f", result.gameplayOptimization.gameplaySpeedup)}x")
        println("  - Deployment ready: ${result.deploymentReady}")
    }
    
    private fun demonstrateHyperparameterOptimization() {
        println("\n--- Hyperparameter Optimization Demo ---")
        
        val hyperparameterOptimizer = HyperparameterOptimizer()
        val baseConfig = HyperparameterOptimizer.HyperparameterConfig(
            learningRate = 0.001,
            batchSize = 64,
            explorationRate = 0.1,
            networkArchitecture = HyperparameterOptimizer.NetworkArchitecture.DEFAULT
        )
        
        println("Optimizing hyperparameters with base config:")
        println("  - Learning rate: ${baseConfig.learningRate}")
        println("  - Batch size: ${baseConfig.batchSize}")
        println("  - Exploration rate: ${baseConfig.explorationRate}")
        println("  - Network architecture: ${baseConfig.networkArchitecture}")
        
        val result = hyperparameterOptimizer.optimizeHyperparameters(baseConfig, optimizationBudget = 15)
        
        println("Hyperparameter Optimization Results:")
        println("  - Optimal learning rate: ${result.optimalConfig.learningRate}")
        println("  - Optimal batch size: ${result.optimalConfig.batchSize}")
        println("  - Optimal exploration rate: ${String.format("%.3f", result.optimalConfig.explorationRate)}")
        println("  - Optimal architecture: ${result.optimalConfig.networkArchitecture}")
        println("  - Improvement factor: ${String.format("%.2f", result.improvementFactor)}x")
        println("  - A/B test confidence: ${String.format("%.1f", result.abTestResult.confidenceLevel * 100)}%")
        println("  - Candidates evaluated: ${result.optimizationHistory.size}")
    }
    
    private fun demonstratePerformanceMonitoring() {
        println("\n--- Performance Monitoring Demo ---")
        
        val monitor = PerformanceMonitor()
        
        println("Starting performance monitoring...")
        monitor.startMonitoring()
        
        // Simulate some workload
        println("Simulating training workload...")
        repeat(20) {
            val snapshot = monitor.collectPerformanceSnapshot()
            if (it % 5 == 0) {
                println(
                    "  Snapshot ${it + 1}: score=${String.format("%.3f", snapshot.overallScore)}, " +
                        "eff=${String.format("%.3f", snapshot.trainingEfficiency)}"
                )
            }
            
            // Simulate work
            Thread.sleep(50)
        }
        
        println("Generating performance report...")
        val report = monitor.generatePerformanceReport()
        
        println("Performance Monitoring Results:")
        println("  - Overall score: ${String.format("%.3f", report.currentPerformance.overallScore)}")
        println("  - Win rate: ${String.format("%.1f", report.currentPerformance.winRate * 100)}%")
        println("  - Avg reward: ${String.format("%.3f", report.currentPerformance.averageReward)}")
        println("  - Training efficiency: ${String.format("%.3f", report.currentPerformance.trainingEfficiency)}")
        println("  - Convergence indicator: ${String.format("%.3f", report.currentPerformance.convergenceIndicator)}")
        println("  - Identified bottlenecks: ${report.identifiedBottlenecks.size}")
        println("  - Optimization recommendations: ${report.optimizationRecommendations.size}")
        
        if (report.identifiedBottlenecks.isNotEmpty()) {
            println("  Bottlenecks:")
            report.identifiedBottlenecks.forEach { bottleneck ->
                println("    - ${bottleneck.type}: ${bottleneck.description}")
            }
        }
        
        if (report.optimizationRecommendations.isNotEmpty()) {
            println("  Recommendations:")
            report.optimizationRecommendations.take(3).forEach { recommendation ->
                println(
                    "    - ${recommendation.category}: expected improvement ${String.format(
                        "%.2f",
                        recommendation.expectedImprovement
                    )}, effort ${recommendation.implementationEffort}"
                )
            }
        }
        
        monitor.stopMonitoring()
    }
    
    private fun printBenchmarkResults(results: BenchmarkResults) {
        println("Performance Benchmark Results:")
        println("  Neural Network:")
        println("    - Duration: ${results.neuralNetwork.duration}ms")
        println("    - Operations/sec: ${String.format("%.0f", results.neuralNetwork.operationsPerSecond)}")
        println("    - Memory usage: ${results.neuralNetwork.memoryUsage / 1024 / 1024}MB")
        println("    - Passed: ${results.neuralNetwork.passed}")
        
        println("  Chess Engine:")
        println("    - Duration: ${results.chessEngine.duration}ms")
        println("    - Operations/sec: ${String.format("%.0f", results.chessEngine.operationsPerSecond)}")
        println("    - Memory usage: ${results.chessEngine.memoryUsage / 1024 / 1024}MB")
        println("    - Passed: ${results.chessEngine.passed}")
        
        println("  RL Algorithm:")
        println("    - Duration: ${results.rlAlgorithm.duration}ms")
        println("    - Operations/sec: ${String.format("%.0f", results.rlAlgorithm.operationsPerSecond)}")
        println("    - Memory usage: ${results.rlAlgorithm.memoryUsage / 1024 / 1024}MB")
        println("    - Passed: ${results.rlAlgorithm.passed}")
        
        println("  Integration:")
        println("    - Duration: ${results.integration.duration}ms")
        println("    - Operations/sec: ${String.format("%.0f", results.integration.operationsPerSecond)}")
        println("    - Memory usage: ${results.integration.memoryUsage / 1024 / 1024}MB")
        println("    - Passed: ${results.integration.passed}")
        
        println("  Overall Score: ${String.format("%.1f", results.overallScore)}")
    }
    
    fun runQuickOptimizationDemo() {
        println("=== Quick System Optimization Demo ===\n")
        
        val coordinator = SystemOptimizationCoordinator()
        
        // Quick optimization with reduced scope
        val config = SystemOptimizationCoordinator.SystemOptimizationConfig(
            optimizeJVMTraining = true,
            optimizeNativeDeployment = false, // Skip for quick demo
            optimizeHyperparameters = true,
            enablePerformanceMonitoring = true,
            hyperparameterBudget = 5 // Reduced for quick demo
        )
        
        val result = coordinator.performSystemOptimization(config)
        
        println("Quick Optimization Summary:")
        println("  - Total speedup: ${String.format("%.2f", result.optimizationSummary.totalSpeedupFactor)}x")
        println("  - Memory reduction: ${String.format("%.1f", result.optimizationSummary.totalMemoryReduction * 100)}%")
        println("  - Key improvements: ${result.optimizationSummary.keyImprovements.size}")
        println("  - Optimization time: ${result.totalOptimizationTime}ms")
        
        println("\n=== Quick Demo Complete ===")
    }
}

/**
 * Main function to run the optimization demo
 */
fun main() {
    val demo = SystemOptimizationDemo()
    
    // Run comprehensive demo
    demo.runComprehensiveOptimizationDemo()
    
    println("\n" + "=".repeat(50) + "\n")
    
    // Run quick demo
    demo.runQuickOptimizationDemo()
}
