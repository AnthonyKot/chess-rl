package com.chessrl.integration.output

import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.integration.logging.ComponentLogger

/**
 * Central coordinator for all training output and progress reporting.
 * Manages structured output with configurable verbosity, trend analysis, and professional formatting.
 */
class OutputManager(
    private val config: OutputConfig = OutputConfig()
) {
    private val formatManager = FormatManager(config.decimalPlaces)
    private val metricsTracker = MetricsTracker(config.trendWindowSize)
    private val logger: ComponentLogger = ChessRLLogger.forComponent("OutputManager")
    
    // Error handling state
    private var formattingErrorCount = 0
    private val maxFormattingErrors = 5
    
    init {
        // Configure the underlying logger based on our config
        ChessRLLogger.configure(
            level = when (config.logLevel) {
                LogLevel.DEBUG -> ChessRLLogger.LogLevel.DEBUG
                LogLevel.INFO -> ChessRLLogger.LogLevel.INFO
                LogLevel.WARN -> ChessRLLogger.LogLevel.WARN
                LogLevel.ERROR -> ChessRLLogger.LogLevel.ERROR
            }
        )
    }
    
    /**
     * Report configuration summary at startup showing profile, overrides, and key parameters
     */
    fun reportConfigSummary(
        profiles: List<String> = emptyList(),
        overrides: Map<String, Any> = emptyMap(),
        keyParameters: Map<String, Any> = emptyMap()
    ) {
        if (config.outputMode == OutputMode.SUMMARY_ONLY) {
            return // Skip config summary in summary-only mode
        }
        
        try {
            val summary = formatManager.formatConfigSummary(profiles, overrides, keyParameters)
            println(summary)
            println() // Add blank line after config summary
        } catch (e: Exception) {
            handleFormattingError("config summary", e)
            // Fallback to simple output
            println("🚀 Training Configuration")
            if (profiles.isNotEmpty()) {
                println("Profiles: ${profiles.joinToString(", ")}")
            }
            if (keyParameters.isNotEmpty()) {
                keyParameters.forEach { (key, value) ->
                    println("  $key = $value")
                }
            }
            println()
        }
    }
    
    /**
     * Report cycle progress with one-line summaries and consistent formatting
     */
    fun reportCycleProgress(data: CycleProgressData) {
        // Update metrics tracker with new data
        metricsTracker.updateMetrics(data)
        
        // Get trend data for display
        val trendData = metricsTracker.getTrendData()
        val eta = metricsTracker.getETA(data.totalCycles - data.cycleNumber)
        
        when (config.outputMode) {
            OutputMode.SUMMARY_ONLY -> {
                // Only report checkpoints and errors in summary-only mode
                data.checkpointEvent?.let { event ->
                    reportCheckpointEvent(event)
                }
            }
            
            OutputMode.VERBOSE -> {
                // Always show detailed output in verbose mode
                reportDetailedCycleProgress(data, trendData, eta)
            }
            
            OutputMode.STANDARD -> {
                // Show detailed blocks at specified intervals, one-liners otherwise
                if (data.cycleNumber % config.logInterval == 0) {
                    reportDetailedCycleProgress(data, trendData, eta)
                } else {
                    reportCycleSummary(data, trendData, eta)
                }
            }
        }
    }
    
    /**
     * Report final training summary with totals and averages
     */
    fun reportFinalSummary(data: FinalSummaryData) {
        try {
            val summary = formatManager.formatFinalSummary(data)
            println()
            println(summary)
        } catch (e: Exception) {
            handleFormattingError("final summary", e)
            // Fallback to simple output
            println()
            println("🏁 Training Completed!")
            println("Total cycles: ${data.totalCycles}")
            println("Total duration: ${data.totalDuration}")
            println("Best performance: ${data.bestPerformance}")
            println("Total games played: ${data.totalGamesPlayed}")
        }
    }
    
    /**
     * Report checkpoint events with performance information
     */
    fun reportCheckpointEvent(event: CheckpointEvent) {
        try {
            val message = formatManager.formatCheckpointEvent(event)
            println(message)
        } catch (e: Exception) {
            handleFormattingError("checkpoint event", e)
            // Fallback to simple output
            val typeIcon = when (event.type) {
                CheckpointType.BEST -> "🏆"
                CheckpointType.REGULAR -> "💾"
                CheckpointType.FINAL -> "🏁"
            }
            println("$typeIcon Saved ${event.type.name.lowercase()} checkpoint")
        }
    }
    
    /**
     * Report validation results with aggregated messages
     */
    fun reportValidationResults(results: List<AggregatedValidationMessage>) {
        if (results.isEmpty() || config.outputMode == OutputMode.SUMMARY_ONLY) {
            return
        }
        
        try {
            // Only show header if we have messages to report
            if (results.isNotEmpty()) {
                println("Validation Issues:")
                results.forEach { validation ->
                    val severityIcon = when (validation.severity) {
                        ValidationSeverity.INFO -> "ℹ️"
                        ValidationSeverity.WARNING -> "⚠️"
                        ValidationSeverity.ERROR -> "❌"
                    }
                    val countSuffix = if (validation.count > 1) " (×${validation.count})" else ""
                    println("  $severityIcon ${validation.message}$countSuffix")
                }
            }
        } catch (e: Exception) {
            handleFormattingError("validation results", e)
            // Fallback to simple output
            println("Validation issues detected (${results.size} total)")
        }
    }
    
    /**
     * Report validation results from TrainingValidator with spam reduction
     */
    fun reportValidationResults(validator: com.chessrl.integration.TrainingValidator) {
        val aggregatedMessages = validator.getAggregatedValidationMessages()
        reportValidationResults(aggregatedMessages)
    }
    
    /**
     * Report error with context and recovery information
     */
    fun reportError(message: String, throwable: Throwable? = null) {
        logger.error(message, throwable)
    }
    
    /**
     * Flush any pending output
     */
    fun flush() {
        System.out.flush()
        System.err.flush()
    }
    
    /**
     * Report one-line cycle summary with trend indicators
     */
    private fun reportCycleSummary(data: CycleProgressData, trendData: TrendData?, eta: kotlin.time.Duration?) {
        try {
            val summary = formatManager.formatCycleSummaryWithTrends(data, trendData, eta)
            println(summary)
            
            // Report checkpoint event if present
            data.checkpointEvent?.let { event ->
                reportCheckpointEvent(event)
            }
        } catch (e: Exception) {
            handleFormattingError("cycle summary", e)
            // Fallback to simple output without trends
            try {
                val fallbackSummary = formatManager.formatCycleSummary(data, trendData?.bestPerformanceDelta)
                println(fallbackSummary)
            } catch (e2: Exception) {
                // Ultimate fallback
                println("Cycle ${data.cycleNumber}/${data.totalCycles} - ${data.gamesPlayed} games - reward: ${data.averageReward}")
            }
        }
    }
    
    /**
     * Report detailed cycle progress for verbose mode or intervals
     */
    private fun reportDetailedCycleProgress(data: CycleProgressData, trendData: TrendData?, eta: kotlin.time.Duration?) {
        try {
            // Show cycle header
            println()
            println("Training Cycle ${data.cycleNumber}/${data.totalCycles}")
            println("-".repeat(40))
            
            // Show one-line summary with trends
            val summary = formatManager.formatCycleSummaryWithTrends(data, trendData, eta)
            println(summary)
            
            // Show additional details in verbose mode
            if (config.outputMode == OutputMode.VERBOSE) {
                println()
                println("Detailed Metrics:")
                val (wins, draws, losses) = data.winDrawLoss
                val total = wins + draws + losses
                if (total > 0) {
                    val winRate = wins.toDouble() / total * 100
                    val drawRate = draws.toDouble() / total * 100
                    val lossRate = losses.toDouble() / total * 100
                    println("  Game outcomes: ${formatManager.formatNumber(winRate, 1)}% win, ${formatManager.formatNumber(drawRate, 1)}% draw, ${formatManager.formatNumber(lossRate, 1)}% loss")
                }
                println("  Buffer utilization: ${formatManager.formatPercentage(data.bufferUtilization.utilizationPercent / 100.0)}")
                println("  Training efficiency: ${data.batchesProcessed} batches processed")
                
                // Show trend information in verbose mode
                trendData?.let { trends ->
                    println()
                    println("Trend Analysis:")
                    println("  Reward trend: ${formatManager.formatTrendIndicator(trends.rewardTrend)}")
                    println("  Win rate trend: ${formatManager.formatTrendIndicator(trends.winRateTrend)}")
                    println("  Loss trend: ${formatManager.formatTrendIndicator(trends.lossTrend)}")
                    
                    val movingAvgs = trends.movingAverages
                    println("  Moving averages: reward=${formatManager.formatNumber(movingAvgs.reward)}, " +
                           "winRate=${formatManager.formatPercentage(movingAvgs.winRate)}, " +
                           "loss=${formatManager.formatNumber(movingAvgs.loss)}")
                }
            }
            
            // Report checkpoint event if present
            data.checkpointEvent?.let { event ->
                println()
                reportCheckpointEvent(event)
            }
            
        } catch (e: Exception) {
            handleFormattingError("detailed cycle progress", e)
            // Fallback to simple output
            reportCycleSummary(data, trendData, eta)
        }
    }
    

    
    /**
     * Handle formatting errors with graceful degradation
     */
    private fun handleFormattingError(context: String, error: Throwable) {
        formattingErrorCount++
        
        if (formattingErrorCount <= maxFormattingErrors) {
            logger.warn("Formatting error in $context: ${error.message}")
            
            if (formattingErrorCount == maxFormattingErrors) {
                logger.warn("Maximum formatting errors reached, suppressing further formatting error messages")
            }
        }
        
        // Log the error for debugging but continue with fallback formatting
        if (config.logLevel == LogLevel.DEBUG) {
            logger.error("Formatting error details", error)
        }
    }
    
    /**
     * Get current configuration
     */
    fun getConfig(): OutputConfig = config
    
    /**
     * Get current best performance from metrics tracker
     */
    fun getBestPerformance(): Double? = metricsTracker.getCurrentBestPerformance()
    
    /**
     * Reset metrics tracking (useful for testing)
     */
    fun resetMetricsTracking() {
        metricsTracker.clear()
    }
    
    /**
     * Get metrics tracker for advanced usage
     */
    fun getMetricsTracker(): MetricsTracker = metricsTracker
}