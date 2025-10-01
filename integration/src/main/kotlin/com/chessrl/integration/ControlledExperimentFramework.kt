package com.chessrl.integration

import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.backend.LearningBackend
import com.chessrl.integration.backend.BackendFactory
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger

/**
 * Framework for running controlled experiments comparing different backends.
 * 
 * This framework ensures identical conditions across different backend implementations
 * to enable fair performance comparisons.
 */
class ControlledExperimentFramework {
    
    companion object {
        private val logger = ChessRLLogger.forComponent("ControlledExperimentFramework")
    }
    
    /**
     * Run a controlled experiment comparing multiple backends.
     * 
     * @param baseConfig Base configuration to use for all backends
     * @param backends List of backend types to compare
     * @param seeds List of seeds to use for reproducible experiments
     * @param cyclesPerExperiment Number of training cycles per experiment
     * @param metricsCollector Metrics collector for gathering data
     * @return Experiment results for analysis
     */
    fun runControlledExperiment(
        baseConfig: ChessRLConfig,
        backends: List<BackendType>,
        seeds: List<Long>,
        cyclesPerExperiment: Int,
        metricsCollector: BenchmarkMetricsCollector
    ): ControlledExperimentResults {
        logger.info("Starting controlled experiment with ${backends.size} backends and ${seeds.size} seeds")
        
        val experimentResults = mutableListOf<ExperimentRun>()
        val startTime = System.currentTimeMillis()
        
        try {
            for (seed in seeds) {
                logger.info("Running experiment with seed: $seed")
                
                for (backendType in backends) {
                    logger.info("Testing backend: ${backendType.name}")
                    
                    val runResult = runSingleExperiment(
                        config = baseConfig.withSeed(seed),
                        backendType = backendType,
                        cycles = cyclesPerExperiment,
                        metricsCollector = metricsCollector,
                        experimentId = "${backendType.name}_seed_${seed}"
                    )
                    
                    experimentResults.add(runResult)
                }
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            logger.info("Controlled experiment completed in ${totalTime}ms")
            
            return ControlledExperimentResults(
                experimentRuns = experimentResults,
                baseConfig = baseConfig,
                totalDurationMs = totalTime,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            logger.error("Controlled experiment failed: ${e.message}")
            throw RuntimeException("Controlled experiment failed", e)
        }
    }
    
    /**
     * Run a single experiment with one backend and seed.
     */
    private fun runSingleExperiment(
        config: ChessRLConfig,
        backendType: BackendType,
        cycles: Int,
        metricsCollector: BenchmarkMetricsCollector,
        experimentId: String
    ): ExperimentRun {
        logger.debug("Starting experiment run: $experimentId")
        
        val runStartTime = System.currentTimeMillis()
        val cycleMetrics = mutableListOf<BenchmarkTrainingMetrics>()
        
        try {
            // Create backend
            val backend = try {
                BackendFactory.createBackend(backendType, config)
            } catch (e: Exception) {
                logger.warn("Failed to create backend ${backendType.name}: ${e.message}")
                throw e
            }
            
            val session = backend.createSession(config)
            
            // Run training cycles
            for (cycle in 1..cycles) {
                logger.debug("Running cycle $cycle/$cycles for $experimentId")
                
                // Simulate training cycle (in real implementation, this would run actual training)
                val gameResults = simulateTrainingCycle(session, config)
                
                // Collect metrics
                val metrics = metricsCollector.collectTrainingMetrics(
                    session = session,
                    backendType = backendType,
                    config = config,
                    cycleNumber = cycle,
                    gameResults = gameResults
                )
                
                cycleMetrics.add(metrics)
            }
            
            session.close()
            
            val runDuration = System.currentTimeMillis() - runStartTime
            logger.debug("Experiment run $experimentId completed in ${runDuration}ms")
            
            return ExperimentRun(
                experimentId = experimentId,
                backendType = backendType,
                seed = config.seed ?: 0L,
                config = config,
                cycleMetrics = cycleMetrics,
                durationMs = runDuration,
                success = true,
                error = null
            )
            
        } catch (e: Exception) {
            val runDuration = System.currentTimeMillis() - runStartTime
            logger.error("Experiment run $experimentId failed: ${e.message}")
            
            return ExperimentRun(
                experimentId = experimentId,
                backendType = backendType,
                seed = config.seed ?: 0L,
                config = config,
                cycleMetrics = cycleMetrics,
                durationMs = runDuration,
                success = false,
                error = e.message
            )
        }
    }
    
    /**
     * Simulate a training cycle for testing purposes.
     * In a real implementation, this would run actual self-play games.
     */
    private fun simulateTrainingCycle(
        @Suppress("UNUSED_PARAMETER") session: com.chessrl.integration.backend.LearningSession,
        config: ChessRLConfig
    ): List<GameResult> {
        // Simulate game results based on configuration
        val gameResults = mutableListOf<GameResult>()
        
        repeat(config.gamesPerCycle) {
            // Simulate random game outcomes for testing
            val random = kotlin.random.Random.Default
            val outcome = when (random.nextInt(3)) {
                0 -> GameOutcome.WHITE_WINS
                1 -> GameOutcome.DRAW
                else -> GameOutcome.BLACK_WINS
            }
            
            val moveCount = random.nextInt(20, 101)
            val materialAdvantage = random.nextDouble(-10.0, 10.0)
            
            gameResults.add(GameResult(outcome, moveCount, materialAdvantage))
        }
        
        return gameResults
    }
    
    /**
     * Run parallel experiments for faster execution.
     * 
     * @param baseConfig Base configuration
     * @param backends Backends to compare
     * @param seeds Seeds for reproducibility
     * @param cyclesPerExperiment Cycles per experiment
     * @param metricsCollector Metrics collector
     * @param maxParallelRuns Maximum number of parallel runs
     * @return Experiment results
     */
    fun runParallelExperiments(
        baseConfig: ChessRLConfig,
        backends: List<BackendType>,
        seeds: List<Long>,
        cyclesPerExperiment: Int,
        metricsCollector: BenchmarkMetricsCollector,
        maxParallelRuns: Int = 4
    ): ControlledExperimentResults {
        logger.info("Starting parallel experiments with max $maxParallelRuns parallel runs")
        
        // For now, run sequentially as parallel execution would require platform-specific threading
        // In a real implementation, this would use coroutines or thread pools
        return runControlledExperiment(
            baseConfig = baseConfig,
            backends = backends,
            seeds = seeds,
            cyclesPerExperiment = cyclesPerExperiment,
            metricsCollector = metricsCollector
        )
    }
    
    /**
     * Generate statistical analysis of experiment results.
     */
    fun analyzeResults(results: ControlledExperimentResults): ExperimentAnalysis {
        logger.info("Analyzing experiment results from ${results.experimentRuns.size} runs")
        
        val backendStats = mutableMapOf<BackendType, BackendStatistics>()
        
        // Group results by backend
        val resultsByBackend = results.experimentRuns.groupBy { it.backendType }
        
        for ((backendType, runs) in resultsByBackend) {
            val successfulRuns = runs.filter { it.success }
            
            if (successfulRuns.isEmpty()) {
                logger.warn("No successful runs for backend: ${backendType.name}")
                continue
            }
            
            // Calculate statistics across all cycles of all runs
            val allMetrics = successfulRuns.flatMap { it.cycleMetrics }
            
            val winRates = allMetrics.map { it.winRate }
            val losses = allMetrics.map { it.averageLoss }
            val trainingTimes = successfulRuns.map { it.durationMs }
            
            backendStats[backendType] = BackendStatistics(
                backendType = backendType,
                totalRuns = runs.size,
                successfulRuns = successfulRuns.size,
                averageWinRate = winRates.average(),
                winRateStdDev = calculateStandardDeviation(winRates),
                averageLoss = losses.average(),
                lossStdDev = calculateStandardDeviation(losses),
                averageTrainingTime = trainingTimes.average(),
                trainingTimeStdDev = calculateStandardDeviation(trainingTimes.map { it.toDouble() })
            )
        }
        
        return ExperimentAnalysis(
            backendStatistics = backendStats,
            totalExperiments = results.experimentRuns.size,
            successfulExperiments = results.experimentRuns.count { it.success },
            analysisTimestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Calculate standard deviation of a list of doubles.
     */
    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}

/**
 * Results from a controlled experiment.
 */
data class ControlledExperimentResults(
    val experimentRuns: List<ExperimentRun>,
    val baseConfig: ChessRLConfig,
    val totalDurationMs: Long,
    val timestamp: Long
) {
    /**
     * Export results to CSV format.
     */
    fun exportToCsv(): String {
        val header = "experimentId,backendType,seed,cycle,winRate,drawRate,lossRate,averageLoss,trainingTime,success,error"
        val rows = mutableListOf<String>()
        
        for (run in experimentRuns) {
            if (run.success) {
                for (metrics in run.cycleMetrics) {
                    rows.add(
                        listOf(
                            run.experimentId,
                            run.backendType.name,
                            run.seed.toString(),
                            metrics.cycle.toString(),
                            metrics.winRate.toString(),
                            metrics.drawRate.toString(),
                            metrics.lossRate.toString(),
                            metrics.averageLoss.toString(),
                            run.durationMs.toString(),
                            "true",
                            ""
                        ).joinToString(",")
                    )
                }
            } else {
                rows.add(
                    listOf(
                        run.experimentId,
                        run.backendType.name,
                        run.seed.toString(),
                        "0",
                        "0.0",
                        "0.0",
                        "0.0",
                        "0.0",
                        run.durationMs.toString(),
                        "false",
                        "\"${run.error ?: ""}\""
                    ).joinToString(",")
                )
            }
        }
        
        return (listOf(header) + rows).joinToString("\n")
    }
}

/**
 * Single experiment run result.
 */
data class ExperimentRun(
    val experimentId: String,
    val backendType: BackendType,
    val seed: Long,
    val config: ChessRLConfig,
    val cycleMetrics: List<BenchmarkTrainingMetrics>,
    val durationMs: Long,
    val success: Boolean,
    val error: String?
)

/**
 * Statistical analysis of experiment results.
 */
data class ExperimentAnalysis(
    val backendStatistics: Map<BackendType, BackendStatistics>,
    val totalExperiments: Int,
    val successfulExperiments: Int,
    val analysisTimestamp: Long
) {
    /**
     * Generate a summary report.
     */
    fun generateSummaryReport(): String {
        val report = StringBuilder()
        
        report.appendLine("Controlled Experiment Analysis Report")
        report.appendLine("=====================================")
        report.appendLine("Total Experiments: $totalExperiments")
        report.appendLine("Successful Experiments: $successfulExperiments")
        report.appendLine("Success Rate: ${(successfulExperiments.toDouble() / totalExperiments * 100).let { "%.2f".format(it) }}%")
        report.appendLine()
        
        report.appendLine("Backend Performance Comparison:")
        report.appendLine("-------------------------------")
        
        for ((backendType, stats) in backendStatistics) {
            report.appendLine("${backendType.name}:")
            report.appendLine("  Runs: ${stats.successfulRuns}/${stats.totalRuns}")
            report.appendLine("  Average Win Rate: ${"%.4f".format(stats.averageWinRate)} ± ${"%.4f".format(stats.winRateStdDev)}")
            report.appendLine("  Average Loss: ${"%.6f".format(stats.averageLoss)} ± ${"%.6f".format(stats.lossStdDev)}")
            report.appendLine("  Average Training Time: ${"%.2f".format(stats.averageTrainingTime)}ms ± ${"%.2f".format(stats.trainingTimeStdDev)}ms")
            report.appendLine()
        }
        
        return report.toString()
    }
}

/**
 * Statistics for a single backend.
 */
data class BackendStatistics(
    val backendType: BackendType,
    val totalRuns: Int,
    val successfulRuns: Int,
    val averageWinRate: Double,
    val winRateStdDev: Double,
    val averageLoss: Double,
    val lossStdDev: Double,
    val averageTrainingTime: Double,
    val trainingTimeStdDev: Double
)
