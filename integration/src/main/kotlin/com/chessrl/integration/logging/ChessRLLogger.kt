package com.chessrl.integration.logging

import kotlin.math.roundToInt

/**
 * Structured logging system for Chess RL training.
 * Provides consistent log format and appropriate levels for different events.
 */
object ChessRLLogger {
    
    enum class LogLevel(val priority: Int, val prefix: String) {
        DEBUG(0, "🔍"),
        INFO(1, "ℹ️"),
        WARN(2, "⚠️"),
        ERROR(3, "❌")
    }
    
    private var currentLevel = LogLevel.INFO
    private var enableTimestamps = true
    private var enableColors = true
    
    /**
     * Configure logging behavior
     */
    fun configure(
        level: LogLevel = LogLevel.INFO,
        timestamps: Boolean = true,
        colors: Boolean = true
    ) {
        currentLevel = level
        enableTimestamps = timestamps
        enableColors = colors
    }
    
    /**
     * Log debug information (detailed execution flow)
     */
    fun debug(message: String, vararg args: Any?) {
        log(LogLevel.DEBUG, message, *args)
    }
    
    /**
     * Log general information (training progress, results)
     */
    fun info(message: String, vararg args: Any?) {
        log(LogLevel.INFO, message, *args)
    }
    
    /**
     * Log warnings (non-fatal issues, fallbacks)
     */
    fun warn(message: String, vararg args: Any?) {
        log(LogLevel.WARN, message, *args)
    }
    
    /**
     * Log errors (failures, exceptions)
     */
    fun error(message: String, throwable: Throwable? = null, vararg args: Any?) {
        log(LogLevel.ERROR, message, *args)
        throwable?.let {
            log(LogLevel.ERROR, "Exception: ${it.message}")
            if (currentLevel == LogLevel.DEBUG) {
                it.printStackTrace()
            }
        }
    }
    
    /**
     * Structured logging for training events
     */
    fun logTrainingCycle(
        cycle: Int,
        totalCycles: Int,
        gamesPlayed: Int,
        avgReward: Double,
        winRate: Double,
        avgGameLength: Double,
        duration: Long
    ) {
        info("Training cycle $cycle/$totalCycles completed")
        info("  Games: $gamesPlayed, Win rate: ${(winRate * 100).roundToInt()}%, Avg length: ${"%.1f".format(avgGameLength)}")
        info("  Avg reward: ${"%.4f".format(avgReward)}, Duration: ${formatDuration(duration)}")
    }
    
    /**
     * Structured logging for evaluation results
     */
    fun logEvaluation(
        gamesPlayed: Int,
        winRate: Double,
        drawRate: Double,
        lossRate: Double,
        avgGameLength: Double
    ) {
        info("Evaluation completed: $gamesPlayed games")
        info("  Win: ${(winRate * 100).roundToInt()}%, Draw: ${(drawRate * 100).roundToInt()}%, Loss: ${(lossRate * 100).roundToInt()}%")
        info("  Average game length: ${"%.1f".format(avgGameLength)} moves")
    }
    
    /**
     * Structured logging for training metrics
     */
    fun logTrainingMetrics(
        batchCount: Int,
        avgLoss: Double,
        avgGradientNorm: Double,
        avgPolicyEntropy: Double,
        bufferSize: Int,
        bufferCapacity: Int
    ) {
        debug("Training metrics:")
        debug("  Batches: $batchCount, Loss: ${"%.4f".format(avgLoss)}")
        debug("  Gradient norm: ${"%.4f".format(avgGradientNorm)}, Policy entropy: ${"%.4f".format(avgPolicyEntropy)}")
        debug("  Buffer: $bufferSize/$bufferCapacity (${(bufferSize.toDouble() / bufferCapacity * 100).roundToInt()}%)")
    }
    
    /**
     * Structured logging for self-play performance
     */
    fun logSelfPlayPerformance(
        approach: String,
        gamesPlayed: Int,
        totalDuration: Long,
        avgGameDuration: Long,
        speedupFactor: Double? = null
    ) {
        info("Self-play performance ($approach):")
        info("  Games: $gamesPlayed, Total: ${formatDuration(totalDuration)}, Avg per game: ${formatDuration(avgGameDuration)}")
        speedupFactor?.let {
            info("  Speedup: ${"%.1f".format(it)}x compared to sequential")
        }
    }
    
    /**
     * Structured logging for system initialization
     */
    fun logInitialization(
        component: String,
        success: Boolean,
        details: String? = null,
        duration: Long? = null
    ) {
        if (success) {
            info("✅ $component initialized successfully")
            details?.let { info("   $it") }
            duration?.let { info("   Initialization time: ${formatDuration(it)}") }
        } else {
            error("❌ $component initialization failed")
            details?.let { error("   $it") }
        }
    }
    
    /**
     * Structured logging for checkpoint operations
     */
    fun logCheckpoint(
        type: String, // "best", "regular", "resume"
        path: String,
        cycle: Int? = null,
        performance: Double? = null,
        success: Boolean = true
    ) {
        if (success) {
            info("💾 Saved $type checkpoint: $path")
            cycle?.let { info("   Cycle: $it") }
            performance?.let { info("   Performance: ${"%.4f".format(it)}") }
        } else {
            warn("Failed to save $type checkpoint: $path")
        }
    }
    
    /**
     * Structured logging for configuration
     */
    fun logConfiguration(config: Any) {
        info("Configuration:")
        val configString = config.toString()
        configString.lines().forEach { line ->
            if (line.trim().isNotEmpty()) {
                info("  $line")
            }
        }
    }
    
    /**
     * Core logging function with formatting
     */
    private fun log(level: LogLevel, message: String, vararg args: Any?) {
        if (level.priority < currentLevel.priority) return
        
        val formattedMessage = if (args.isNotEmpty()) {
            try {
                message.format(*args)
            } catch (e: Exception) {
                "$message [formatting error: ${e.message}]"
            }
        } else {
            message
        }
        
        val timestamp = if (enableTimestamps) {
            "[${getCurrentTimestamp()}] "
        } else {
            ""
        }
        
        val prefix = if (enableColors) level.prefix else level.name
        val logLine = "$timestamp$prefix $formattedMessage"
        
        when (level) {
            LogLevel.ERROR -> System.err.println(logLine)
            else -> println(logLine)
        }
    }
    
    /**
     * Format duration in human-readable format
     */
    private fun formatDuration(millis: Long): String {
        return when {
            millis < 1000 -> "${millis}ms"
            millis < 60000 -> "${"%.1f".format(millis / 1000.0)}s"
            millis < 3600000 -> "${millis / 60000}m ${(millis % 60000) / 1000}s"
            else -> "${millis / 3600000}h ${(millis % 3600000) / 60000}m"
        }
    }
    
    /**
     * Get current timestamp string
     */
    private fun getCurrentTimestamp(): String {
        // Simple timestamp - could be enhanced with proper date formatting
        return System.currentTimeMillis().toString().takeLast(8)
    }
    
    /**
     * Create a scoped logger for a specific component
     */
    fun forComponent(componentName: String): ComponentLogger {
        return ComponentLogger(componentName)
    }
}

/**
 * Component-specific logger that prefixes all messages with component name
 */
class ComponentLogger(private val componentName: String) {
    
    fun debug(message: String, vararg args: Any?) {
        ChessRLLogger.debug("[$componentName] $message", *args)
    }
    
    fun info(message: String, vararg args: Any?) {
        ChessRLLogger.info("[$componentName] $message", *args)
    }
    
    fun warn(message: String, vararg args: Any?) {
        ChessRLLogger.warn("[$componentName] $message", *args)
    }
    
    fun error(message: String, throwable: Throwable? = null, vararg args: Any?) {
        ChessRLLogger.error("[$componentName] $message", throwable, *args)
    }
}