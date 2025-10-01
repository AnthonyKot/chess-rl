package com.chessrl.nn

import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.measureTime

class PerformanceBenchmark {
    
    @Test
    fun benchmarkNeuralNetworkPerformance() {
        println("=== Neural Network Performance Benchmark ===")
        
        // Warmup iterations (stabilize JIT / caches)
        val warmups = 3
        println("Warmup iterations: $warmups")
        repeat(warmups) {
            runQuickBenchmark()
        }
        
        // Run actual benchmarks
        val results = mutableListOf<BenchmarkResult>()
        
        results.add(benchmarkNetworkCreation())
        results.add(benchmarkForwardPass())
        results.add(benchmarkBackwardPass())
        results.add(benchmarkTrainingIteration())
        results.add(benchmarkLargeNetworkTraining())
        
        printResults(results)
    }
    
    private fun benchmarkNetworkCreation(): BenchmarkResult {
        val iterations = 1000
        val time = measureTime {
            repeat(iterations) {
                FeedforwardNetwork(
                    _layers = listOf(
                        DenseLayer(10, 20, ReLUActivation()),
                        DenseLayer(20, 10, ReLUActivation()),
                        DenseLayer(10, 1, LinearActivation())
                    )
                )
            }
        }
        return BenchmarkResult("Network Creation", iterations, time)
    }
    
    private fun benchmarkForwardPass(): BenchmarkResult {
        val network = createTestNetwork()
        val input = doubleArrayOf(0.1, 0.2, 0.3, 0.4, 0.5)
        val iterations = 10000
        
        val time = measureTime {
            repeat(iterations) {
                network.forward(input)
            }
        }
        return BenchmarkResult("Forward Pass", iterations, time)
    }
    
    private fun benchmarkBackwardPass(): BenchmarkResult {
        val network = createTestNetwork()
        val input = doubleArrayOf(0.1, 0.2, 0.3, 0.4, 0.5)
        val target = doubleArrayOf(0.8)
        val iterations = 5000
        
        val time = measureTime {
            repeat(iterations) {
                val output = network.forward(input)
                MSELoss().computeLoss(output, target)
                val gradient = MSELoss().computeGradient(output, target)
                network.backward(gradient)
            }
        }
        return BenchmarkResult("Backward Pass", iterations, time)
    }
    
    private fun benchmarkTrainingIteration(): BenchmarkResult {
        val network = createTestNetwork()
        val optimizer = SGDOptimizer(learningRate = 0.01)
        val (inputs, targets) = SyntheticDatasets.generateMultiDimensionalPolynomial(
            samples = 100, 
            inputDim = 5,
            quadraticCoeffs = DoubleArray(5) { 0.1 },
            linearCoeffs = DoubleArray(5) { 0.5 }
        )
        val iterations = 1000
        
        val time = measureTime {
            repeat(iterations) {
                // Create mini-batch
                val batchSize = 10
                val batchInputs = Array(batchSize) { inputs[it % inputs.size] }
                val batchTargets = Array(batchSize) { targets[it % targets.size] }
                
                for (i in 0 until batchSize) {
                    val output = network.forward(batchInputs[i])
                    MSELoss().computeLoss(output, batchTargets[i])
                    network.backward(batchTargets[i])
                }
                
                optimizer.updateWeights(network.layers)
            }
        }
        return BenchmarkResult("Training Iteration", iterations, time)
    }
    
    private fun benchmarkLargeNetworkTraining(): BenchmarkResult {
        val network = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(50, 100, ReLUActivation()),
                DenseLayer(100, 100, ReLUActivation()),
                DenseLayer(100, 50, ReLUActivation()),
                DenseLayer(50, 1, LinearActivation())
            )
        )
        val optimizer = SGDOptimizer(learningRate = 0.001)
        val (inputs, targets) = SyntheticDatasets.generateMultiDimensionalPolynomial(
            samples = 500, 
            inputDim = 50,
            quadraticCoeffs = DoubleArray(50) { 0.1 },
            linearCoeffs = DoubleArray(50) { 0.5 }
        )
        val iterations = 100
        
        val time = measureTime {
            repeat(iterations) {
                // Create mini-batch
                val batchSize = 32
                val batchInputs = Array(batchSize) { inputs[it % inputs.size] }
                val batchTargets = Array(batchSize) { targets[it % targets.size] }
                
                for (i in 0 until batchSize) {
                    val output = network.forward(batchInputs[i])
                    MSELoss().computeLoss(output, batchTargets[i])
                    network.backward(batchTargets[i])
                }
                
                optimizer.updateWeights(network.layers)
            }
        }
        return BenchmarkResult("Large Network Training", iterations, time)
    }
    
    private fun runQuickBenchmark() {
        val network = createTestNetwork()
        val input = doubleArrayOf(0.1, 0.2, 0.3, 0.4, 0.5)
        repeat(100) {
            network.forward(input)
        }
    }
    
    private fun createTestNetwork(): FeedforwardNetwork {
        return FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(5, 10, ReLUActivation()),
                DenseLayer(10, 5, ReLUActivation()),
                DenseLayer(5, 1, LinearActivation())
            )
        )
    }
    
    private fun printResults(results: List<BenchmarkResult>) {
        println("\n=== Benchmark Results ===")
        println("Platform: ${getPlatformInfo()}")
        println("Runtime: ${platformRuntimeInfo()}")
        println("Hardware: ${platformHardwareInfo()}")
        println("Timestamp: ${getCurrentTimestamp()}")
        println()
        
        for (result in results) {
            val avgTimeMs = result.totalTime.inWholeMilliseconds.toDouble() / result.iterations
            val avgTimeMicros = result.totalTime.inWholeMicroseconds.toDouble() / result.iterations
            val opsPerSecond = 1000.0 / avgTimeMs
            
            println("${result.operation}:")
            println("  Total time: ${result.totalTime}")
            println("  Iterations: ${result.iterations}")
            println("  Avg time per op: ${avgTimeMs.formatLocal(3)} ms (${avgTimeMicros.formatLocal(1)} μs)")
            println("  Operations/sec: ${opsPerSecond.formatLocal(0)}")
            println()
        }
    }
    
    private fun getPlatformInfo(): String {
        return "Multiplatform Kotlin"
    }
    
    private fun getCurrentTimestamp(): String {
        return getCurrentTimeMillis().toString()
    }
    
    data class BenchmarkResult(
        val operation: String,
        val iterations: Int,
        val totalTime: Duration
    )
}

// Formatting helper that mirrors the production extension.
private fun Double.formatLocal(digits: Int): String {
    val multiplier = when (digits) {
        0 -> 1.0
        1 -> 10.0
        2 -> 100.0
        3 -> 1000.0
        4 -> 10000.0
        else -> {
            var result = 1.0
            repeat(digits) { result *= 10.0 }
            result
        }
    }
    val rounded = kotlin.math.round(this * multiplier) / multiplier
    return rounded.toString()
}
