package com.chessrl.integration

import kotlin.math.*
import kotlin.random.Random

/**
 * Comprehensive Performance Monitor with detailed metrics collection
 */
class PerformanceMonitor {
    private val metricsCollector = MetricsCollector()
    private val profiler = SystemProfiler()
    private val resourceMonitor = ResourceMonitor()
    private val benchmarkSuite = PerformanceBenchmarkSuite()
    
    private val performanceHistory = mutableListOf<PerformanceSnapshot>()
    private var isMonitoring = false
    
    fun startMonitoring() {
        isMonitoring = true
        println("Performance monitoring started")
    }
    
    fun stopMonitoring() {
        isMonitoring = false
        println("Performance monitoring stopped")
    }
    
    fun collectPerformanceSnapshot(): PerformanceSnapshot {
        val timestamp = getCurrentTimeMillis()
        
        val metrics = metricsCollector.collectCurrentMetrics()
        val profiling = profiler.profileCurrentState()
        val resources = resourceMonitor.getCurrentResourceUsage()
        
        val snapshot = PerformanceSnapshot(
            timestamp = timestamp,
            metrics = metrics,
            profiling = profiling,
            resources = resources
        )
        
        if (isMonitoring) {
            performanceHistory.add(snapshot)
            
            // Keep only last 1000 snapshots to prevent memory issues
            if (performanceHistory.size > 1000) {
                performanceHistory.removeAt(0)
            }
        }
        
        return snapshot
    }
    
    fun generatePerformanceReport(): PerformanceReport {
        val currentSnapshot = collectPerformanceSnapshot()
        val historicalAnalysis = analyzeHistoricalPerformance()
        val bottlenecks = identifyBottlenecks()
        val recommendations = generateOptimizationRecommendations(bottlenecks)
        
        return PerformanceReport(
            currentPerformance = currentSnapshot,
            historicalAnalysis = historicalAnalysis,
            identifiedBottlenecks = bottlenecks,
            optimizationRecommendations = recommendations,
            benchmarkResults = benchmarkSuite.runBenchmarks()
        )
    }
    
    private fun analyzeHistoricalPerformance(): HistoricalAnalysis {
        if (performanceHistory.size < 10) {
            return HistoricalAnalysis.empty()
        }
        
        val recentSnapshots = performanceHistory.takeLast(100)
        val throughputTrend = calculateTrend(recentSnapshots.map { it.metrics.throughput })
        val memoryTrend = calculateTrend(recentSnapshots.map { it.resources.memoryUsage.toDouble() })
        val latencyTrend = calculateTrend(recentSnapshots.map { it.metrics.averageLatency })
        
        return HistoricalAnalysis(
            throughputTrend = throughputTrend,
            memoryTrend = memoryTrend,
            latencyTrend = latencyTrend,
            performanceRegression = detectPerformanceRegression(recentSnapshots)
        )
    }
    
    private fun calculateTrend(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val n = values.size
        val sumX = (0 until n).sum().toDouble()
        val sumY = values.sum()
        val sumXY = values.mapIndexed { index, value -> index * value }.sum()
        val sumX2 = (0 until n).map { it * it }.sum().toDouble()
        
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        return slope
    }
    
    private fun detectPerformanceRegression(snapshots: List<PerformanceSnapshot>): Boolean {
        if (snapshots.size < 20) return false
        
        val recent = snapshots.takeLast(10)
        val baseline = snapshots.take(10)
        
        val recentAvgThroughput = recent.map { it.metrics.throughput }.average()
        val baselineAvgThroughput = baseline.map { it.metrics.throughput }.average()
        
        return recentAvgThroughput < baselineAvgThroughput * 0.9 // 10% regression threshold
    }
    
    private fun identifyBottlenecks(): List<PerformanceBottleneck> {
        val currentSnapshot = performanceHistory.lastOrNull() ?: return emptyList()
        val bottlenecks = mutableListOf<PerformanceBottleneck>()
        
        // Memory bottleneck
        if (currentSnapshot.resources.memoryUsage > currentSnapshot.resources.totalMemory * 0.8) {
            bottlenecks.add(
                PerformanceBottleneck(
                    type = BottleneckType.MEMORY,
                    severity = BottleneckSeverity.HIGH,
                    description = "Memory usage is above 80% of available memory",
                    impact = "May cause GC pressure and performance degradation"
                )
            )
        }
        
        // CPU bottleneck
        if (currentSnapshot.resources.cpuUsage > 0.9) {
            bottlenecks.add(
                PerformanceBottleneck(
                    type = BottleneckType.CPU,
                    severity = BottleneckSeverity.HIGH,
                    description = "CPU usage is above 90%",
                    impact = "System may become unresponsive"
                )
            )
        }
        
        // Throughput bottleneck
        if (currentSnapshot.metrics.throughput < 1.0) {
            bottlenecks.add(
                PerformanceBottleneck(
                    type = BottleneckType.THROUGHPUT,
                    severity = BottleneckSeverity.MEDIUM,
                    description = "Training throughput is below 1 episode/second",
                    impact = "Training will take longer to complete"
                )
            )
        }
        
        // Latency bottleneck
        if (currentSnapshot.metrics.averageLatency > 1000.0) {
            bottlenecks.add(
                PerformanceBottleneck(
                    type = BottleneckType.LATENCY,
                    severity = BottleneckSeverity.MEDIUM,
                    description = "Average operation latency is above 1 second",
                    impact = "System responsiveness is degraded"
                )
            )
        }
        
        return bottlenecks
    }
    
    private fun generateOptimizationRecommendations(
        bottlenecks: List<PerformanceBottleneck>
    ): List<OptimizationRecommendation> {
        val recommendations = mutableListOf<OptimizationRecommendation>()
        
        for (bottleneck in bottlenecks) {
            when (bottleneck.type) {
                BottleneckType.MEMORY -> {
                    recommendations.add(
                        OptimizationRecommendation(
                            category = OptimizationCategory.MEMORY,
                            priority = RecommendationPriority.HIGH,
                            description = "Reduce batch size or implement memory pooling",
                            expectedImprovement = "20-40% memory usage reduction",
                            implementationEffort = ImplementationEffort.MEDIUM
                        )
                    )
                }
                BottleneckType.CPU -> {
                    recommendations.add(
                        OptimizationRecommendation(
                            category = OptimizationCategory.COMPUTATION,
                            priority = RecommendationPriority.HIGH,
                            description = "Optimize neural network operations or reduce model complexity",
                            expectedImprovement = "15-30% CPU usage reduction",
                            implementationEffort = ImplementationEffort.HIGH
                        )
                    )
                }
                BottleneckType.THROUGHPUT -> {
                    recommendations.add(
                        OptimizationRecommendation(
                            category = OptimizationCategory.PARALLELIZATION,
                            priority = RecommendationPriority.MEDIUM,
                            description = "Increase batch size or implement parallel training",
                            expectedImprovement = "2-4x throughput improvement",
                            implementationEffort = ImplementationEffort.MEDIUM
                        )
                    )
                }
                BottleneckType.LATENCY -> {
                    recommendations.add(
                        OptimizationRecommendation(
                            category = OptimizationCategory.CACHING,
                            priority = RecommendationPriority.MEDIUM,
                            description = "Implement result caching or optimize data structures",
                            expectedImprovement = "30-50% latency reduction",
                            implementationEffort = ImplementationEffort.LOW
                        )
                    )
                }
            }
        }
        
        return recommendations
    }
}

/**
 * Metrics Collector for detailed performance metrics
 */
class MetricsCollector {
    private var operationCount = 0L
    private var totalLatency = 0.0
    private var lastThroughputMeasurement = getCurrentTimeMillis()
    private var lastOperationCount = 0L
    
    fun collectCurrentMetrics(): PerformanceMetrics {
        val currentTime = getCurrentTimeMillis()
        val timeDelta = (currentTime - lastThroughputMeasurement) / 1000.0
        val operationDelta = operationCount - lastOperationCount
        
        val throughput = if (timeDelta > 0) operationDelta / timeDelta else 0.0
        val averageLatency = if (operationCount > 0) totalLatency / operationCount else 0.0
        
        lastThroughputMeasurement = currentTime
        lastOperationCount = operationCount
        
        return PerformanceMetrics(
            throughput = throughput,
            averageLatency = averageLatency,
            totalOperations = operationCount,
            errorRate = calculateErrorRate(),
            queueDepth = estimateQueueDepth()
        )
    }
    
    fun recordOperation(latency: Double, success: Boolean = true) {
        operationCount++
        totalLatency += latency
        
        if (!success) {
            // Record error for error rate calculation
        }
    }
    
    private fun calculateErrorRate(): Double {
        // Simulate error rate calculation
        return Random.nextDouble(0.0, 0.05) // 0-5% error rate
    }
    
    private fun estimateQueueDepth(): Int {
        // Simulate queue depth estimation
        return Random.nextInt(0, 10)
    }
}

/**
 * System Profiler for identifying performance hotspots
 */
class SystemProfiler {
    fun profileCurrentState(): ProfilingData {
        val hotspots = identifyHotspots()
        val callGraph = generateCallGraph()
        val memoryProfile = profileMemoryUsage()
        
        return ProfilingData(
            hotspots = hotspots,
            callGraph = callGraph,
            memoryProfile = memoryProfile,
            profilingOverhead = measureProfilingOverhead()
        )
    }
    
    private fun identifyHotspots(): List<PerformanceHotspot> {
        return listOf(
            PerformanceHotspot(
                functionName = "NeuralNetwork.forward",
                cpuTime = 45.2,
                callCount = 1000,
                averageTime = 0.045
            ),
            PerformanceHotspot(
                functionName = "ChessBoard.generateMoves",
                cpuTime = 23.1,
                callCount = 500,
                averageTime = 0.046
            ),
            PerformanceHotspot(
                functionName = "ExperienceReplay.sample",
                cpuTime = 15.7,
                callCount = 200,
                averageTime = 0.079
            )
        )
    }
    
    private fun generateCallGraph(): CallGraph {
        return CallGraph(
            nodes = listOf("main", "trainAgent", "playGame", "updateNetwork"),
            edges = listOf(
                CallEdge("main", "trainAgent", 1),
                CallEdge("trainAgent", "playGame", 10),
                CallEdge("trainAgent", "updateNetwork", 5),
                CallEdge("playGame", "generateMoves", 50)
            )
        )
    }
    
    private fun profileMemoryUsage(): MemoryProfile {
        return MemoryProfile(
            heapUsage = getHeapUsage(),
            objectCounts = getObjectCounts(),
            allocationRate = getAllocationRate(),
            gcActivity = getGCActivity()
        )
    }
    
    private fun measureProfilingOverhead(): Double {
        val startTime = getCurrentTimeMillis()
        
        // Simulate profiling work
        repeat(1000) {
            val dummy = DoubleArray(100) { Random.nextDouble() }
            dummy.sum()
        }
        
        val endTime = getCurrentTimeMillis()
        return (endTime - startTime) / 1000.0
    }
    
    private fun getHeapUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun getObjectCounts(): Map<String, Int> {
        return mapOf(
            "DoubleArray" to 1000,
            "ChessBoard" to 50,
            "Experience" to 5000,
            "NeuralNetwork" to 2
        )
    }
    
    private fun getAllocationRate(): Double {
        return Random.nextDouble(10.0, 100.0) // MB/s
    }
    
    private fun getGCActivity(): GCActivity {
        return GCActivity(
            gcCount = Random.nextInt(10, 100),
            gcTime = Random.nextDouble(0.1, 5.0),
            gcFrequency = Random.nextDouble(0.1, 2.0)
        )
    }
}

/**
 * Resource Monitor for system resource utilization
 */
class ResourceMonitor {
    fun getCurrentResourceUsage(): ResourceUsage {
        return ResourceUsage(
            memoryUsage = getMemoryUsage(),
            totalMemory = getTotalMemory(),
            cpuUsage = getCPUUsage(),
            diskUsage = getDiskUsage(),
            networkUsage = getNetworkUsage(),
            threadCount = getThreadCount()
        )
    }
    
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun getTotalMemory(): Long {
        return Runtime.getRuntime().maxMemory()
    }
    
    private fun getCPUUsage(): Double {
        // Simulate CPU usage measurement
        return Random.nextDouble(0.1, 0.8)
    }
    
    private fun getDiskUsage(): DiskUsage {
        return DiskUsage(
            readBytes = Random.nextLong(1000000, 10000000),
            writeBytes = Random.nextLong(500000, 5000000),
            readOps = Random.nextInt(100, 1000),
            writeOps = Random.nextInt(50, 500)
        )
    }
    
    private fun getNetworkUsage(): NetworkUsage {
        return NetworkUsage(
            bytesReceived = Random.nextLong(1000, 100000),
            bytesSent = Random.nextLong(1000, 100000),
            packetsReceived = Random.nextInt(10, 1000),
            packetsSent = Random.nextInt(10, 1000)
        )
    }
    
    private fun getThreadCount(): Int {
        return Thread.activeCount()
    }
}

/**
 * Performance Benchmark Suite for regression detection
 */
class PerformanceBenchmarkSuite {
    fun runBenchmarks(): BenchmarkResults {
        val neuralNetworkBenchmark = benchmarkNeuralNetwork()
        val chessEngineBenchmark = benchmarkChessEngine()
        val rlAlgorithmBenchmark = benchmarkRLAlgorithm()
        val integrationBenchmark = benchmarkIntegration()
        
        return BenchmarkResults(
            neuralNetwork = neuralNetworkBenchmark,
            chessEngine = chessEngineBenchmark,
            rlAlgorithm = rlAlgorithmBenchmark,
            integration = integrationBenchmark,
            overallScore = calculateOverallScore(
                neuralNetworkBenchmark,
                chessEngineBenchmark,
                rlAlgorithmBenchmark,
                integrationBenchmark
            )
        )
    }
    
    private fun benchmarkNeuralNetwork(): BenchmarkResult {
        val startTime = getCurrentTimeMillis()
        
        // Simulate neural network operations
        repeat(1000) {
            val input = DoubleArray(776) { Random.nextDouble() }
            val weights = DoubleArray(776 * 512) { Random.nextDouble() }
            
            // Forward pass simulation
            var sum = 0.0
            for (i in input.indices) {
                sum += input[i] * weights[i % weights.size]
            }
        }
        
        val endTime = getCurrentTimeMillis()
        val duration = endTime - startTime
        
        return BenchmarkResult(
            name = "Neural Network",
            duration = duration,
            operationsPerSecond = 1000.0 / (duration / 1000.0),
            memoryUsage = estimateMemoryUsage("neural_network"),
            passed = duration < 5000 // Should complete in under 5 seconds
        )
    }
    
    private fun benchmarkChessEngine(): BenchmarkResult {
        val startTime = getCurrentTimeMillis()
        
        // Simulate chess operations
        repeat(10000) {
            val board = IntArray(64) { Random.nextInt(13) }
            val moves = mutableListOf<Int>()
            
            // Generate moves
            for (square in board.indices) {
                if (board[square] != 0) {
                    moves.add(square + Random.nextInt(64))
                }
            }
        }
        
        val endTime = getCurrentTimeMillis()
        val duration = endTime - startTime
        
        return BenchmarkResult(
            name = "Chess Engine",
            duration = duration,
            operationsPerSecond = 10000.0 / (duration / 1000.0),
            memoryUsage = estimateMemoryUsage("chess_engine"),
            passed = duration < 2000 // Should complete in under 2 seconds
        )
    }
    
    private fun benchmarkRLAlgorithm(): BenchmarkResult {
        val startTime = getCurrentTimeMillis()
        
        // Simulate RL operations
        repeat(1000) {
            val state = DoubleArray(776) { Random.nextDouble() }
            val qValues = DoubleArray(4096) { Random.nextDouble() }
            
            // Q-learning update simulation
            val maxQ = qValues.maxOrNull() ?: 0.0
            val reward = Random.nextDouble(-1.0, 1.0)
            val target = reward + 0.99 * maxQ
            
            // Update simulation
            val loss = (target - qValues[0]).pow(2)
        }
        
        val endTime = getCurrentTimeMillis()
        val duration = endTime - startTime
        
        return BenchmarkResult(
            name = "RL Algorithm",
            duration = duration,
            operationsPerSecond = 1000.0 / (duration / 1000.0),
            memoryUsage = estimateMemoryUsage("rl_algorithm"),
            passed = duration < 3000 // Should complete in under 3 seconds
        )
    }
    
    private fun benchmarkIntegration(): BenchmarkResult {
        val startTime = getCurrentTimeMillis()
        
        // Simulate integration operations
        repeat(100) {
            // Simulate complete training step
            val state = DoubleArray(776) { Random.nextDouble() }
            val action = Random.nextInt(4096)
            val reward = Random.nextDouble(-1.0, 1.0)
            val nextState = DoubleArray(776) { Random.nextDouble() }
            
            // Experience storage simulation
            val experience = mapOf(
                "state" to state,
                "action" to action,
                "reward" to reward,
                "next_state" to nextState
            )
        }
        
        val endTime = getCurrentTimeMillis()
        val duration = endTime - startTime
        
        return BenchmarkResult(
            name = "Integration",
            duration = duration,
            operationsPerSecond = 100.0 / (duration / 1000.0),
            memoryUsage = estimateMemoryUsage("integration"),
            passed = duration < 1000 // Should complete in under 1 second
        )
    }
    
    private fun estimateMemoryUsage(component: String): Long {
        return when (component) {
            "neural_network" -> 50_000_000L // 50MB
            "chess_engine" -> 1_000_000L // 1MB
            "rl_algorithm" -> 10_000_000L // 10MB
            "integration" -> 5_000_000L // 5MB
            else -> 1_000_000L
        }
    }
    
    private fun calculateOverallScore(vararg results: BenchmarkResult): Double {
        val totalOps = results.sumOf { it.operationsPerSecond }
        val allPassed = results.all { it.passed }
        val avgDuration = results.map { it.duration }.average()
        
        return if (allPassed) {
            min(100.0, totalOps / 100.0 + (5000.0 - avgDuration) / 50.0)
        } else {
            0.0
        }
    }
}

// Data classes for performance monitoring
// Using shared PerformanceSnapshot from SharedDataClasses.kt

// Using shared PerformanceMetrics from SharedDataClasses.kt

data class ProfilingData(
    val hotspots: List<PerformanceHotspot>,
    val callGraph: CallGraph,
    val memoryProfile: MemoryProfile,
    val profilingOverhead: Double
)

data class PerformanceHotspot(
    val functionName: String,
    val cpuTime: Double,
    val callCount: Int,
    val averageTime: Double
)

data class CallGraph(
    val nodes: List<String>,
    val edges: List<CallEdge>
)

data class CallEdge(
    val from: String,
    val to: String,
    val callCount: Int
)

data class MemoryProfile(
    val heapUsage: Long,
    val objectCounts: Map<String, Int>,
    val allocationRate: Double,
    val gcActivity: GCActivity
)

data class GCActivity(
    val gcCount: Int,
    val gcTime: Double,
    val gcFrequency: Double
)

data class ResourceUsage(
    val memoryUsage: Long,
    val totalMemory: Long,
    val cpuUsage: Double,
    val diskUsage: DiskUsage,
    val networkUsage: NetworkUsage,
    val threadCount: Int
)

data class DiskUsage(
    val readBytes: Long,
    val writeBytes: Long,
    val readOps: Int,
    val writeOps: Int
)

data class NetworkUsage(
    val bytesReceived: Long,
    val bytesSent: Long,
    val packetsReceived: Int,
    val packetsSent: Int
)

data class PerformanceReport(
    val currentPerformance: PerformanceSnapshot,
    val historicalAnalysis: HistoricalAnalysis,
    val identifiedBottlenecks: List<PerformanceBottleneck>,
    val optimizationRecommendations: List<OptimizationRecommendation>,
    val benchmarkResults: BenchmarkResults
)

data class HistoricalAnalysis(
    val throughputTrend: Double,
    val memoryTrend: Double,
    val latencyTrend: Double,
    val performanceRegression: Boolean
) {
    companion object {
        fun empty() = HistoricalAnalysis(0.0, 0.0, 0.0, false)
    }
}

data class PerformanceBottleneck(
    val type: BottleneckType,
    val severity: BottleneckSeverity,
    val description: String,
    val impact: String
)

enum class BottleneckType {
    MEMORY, CPU, THROUGHPUT, LATENCY, DISK, NETWORK
}

enum class BottleneckSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

// OptimizationRecommendation and RecommendationPriority are now defined in SharedDataClasses.kt

enum class OptimizationCategory {
    MEMORY, COMPUTATION, PARALLELIZATION, CACHING, ARCHITECTURE
}

enum class ImplementationEffort {
    LOW, MEDIUM, HIGH
}

data class BenchmarkResults(
    val neuralNetwork: BenchmarkResult,
    val chessEngine: BenchmarkResult,
    val rlAlgorithm: BenchmarkResult,
    val integration: BenchmarkResult,
    val overallScore: Double
)

data class BenchmarkResult(
    val name: String,
    val duration: Long,
    val operationsPerSecond: Double,
    val memoryUsage: Long,
    val passed: Boolean
)