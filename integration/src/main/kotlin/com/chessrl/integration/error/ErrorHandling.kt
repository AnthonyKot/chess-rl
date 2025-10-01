package com.chessrl.integration.error

import com.chessrl.integration.logging.ChessRLLogger

/**
 * Comprehensive error handling and recovery system for Chess RL training.
 * Provides structured error types, recovery mechanisms, and graceful degradation.
 */

/**
 * Base class for all Chess RL errors with structured information
 */
sealed class ChessRLError(
    message: String,
    cause: Throwable? = null,
    val errorCode: String,
    val severity: ErrorSeverity,
    val recoverable: Boolean = true
) : Exception(message, cause) {
    
    enum class ErrorSeverity {
        LOW,      // Warning level, system can continue normally
        MEDIUM,   // Error level, some functionality may be impacted
        HIGH,     // Critical error, major functionality affected
        CRITICAL  // System cannot continue, immediate intervention required
    }
}

/**
 * Configuration-related errors
 */
sealed class ConfigurationError(
    message: String,
    cause: Throwable? = null,
    errorCode: String,
    severity: ErrorSeverity = ErrorSeverity.HIGH,
    recoverable: Boolean = false
) : ChessRLError(message, cause, errorCode, severity, recoverable) {
    
    class InvalidParameter(parameter: String, value: Any?, reason: String) : ConfigurationError(
        message = "Invalid configuration parameter '$parameter' = '$value': $reason",
        errorCode = "CONFIG_INVALID_PARAM",
        severity = ErrorSeverity.HIGH
    )
    
    class MissingParameter(parameter: String) : ConfigurationError(
        message = "Required configuration parameter '$parameter' is missing",
        errorCode = "CONFIG_MISSING_PARAM",
        severity = ErrorSeverity.HIGH
    )
    
    class FileNotFound(path: String) : ConfigurationError(
        message = "Configuration file not found: $path",
        errorCode = "CONFIG_FILE_NOT_FOUND",
        severity = ErrorSeverity.HIGH
    )
    
    class ParseError(path: String, cause: Throwable) : ConfigurationError(
        message = "Failed to parse configuration file: $path",
        cause = cause,
        errorCode = "CONFIG_PARSE_ERROR",
        severity = ErrorSeverity.HIGH
    )
}

/**
 * Training-related errors
 */
sealed class TrainingError(
    message: String,
    cause: Throwable? = null,
    errorCode: String,
    severity: ErrorSeverity,
    recoverable: Boolean = true
) : ChessRLError(message, cause, errorCode, severity, recoverable) {
    
    class InitializationFailed(component: String, cause: Throwable) : TrainingError(
        message = "Failed to initialize $component",
        cause = cause,
        errorCode = "TRAINING_INIT_FAILED",
        severity = ErrorSeverity.CRITICAL,
        recoverable = false
    )
    
    class SelfPlayFailed(gameId: Int, cause: Throwable) : TrainingError(
        message = "Self-play game $gameId failed",
        cause = cause,
        errorCode = "TRAINING_SELFPLAY_FAILED",
        severity = ErrorSeverity.MEDIUM,
        recoverable = true
    )
    
    class BatchTrainingFailed(batchSize: Int, cause: Throwable) : TrainingError(
        message = "Batch training failed for batch size $batchSize",
        cause = cause,
        errorCode = "TRAINING_BATCH_FAILED",
        severity = ErrorSeverity.MEDIUM,
        recoverable = true
    )
    
    class CheckpointFailed(path: String, cause: Throwable) : TrainingError(
        message = "Failed to save checkpoint: $path",
        cause = cause,
        errorCode = "TRAINING_CHECKPOINT_FAILED",
        severity = ErrorSeverity.MEDIUM,
        recoverable = true
    )
    
    class EvaluationFailed(cause: Throwable) : TrainingError(
        message = "Agent evaluation failed",
        cause = cause,
        errorCode = "TRAINING_EVAL_FAILED",
        severity = ErrorSeverity.LOW,
        recoverable = true
    )
}

/**
 * Multi-process execution errors
 */
sealed class MultiProcessError(
    message: String,
    cause: Throwable? = null,
    errorCode: String,
    severity: ErrorSeverity,
    recoverable: Boolean = true
) : ChessRLError(message, cause, errorCode, severity, recoverable) {
    
    class ProcessStartFailed(gameId: Int, cause: Throwable) : MultiProcessError(
        message = "Failed to start worker process for game $gameId",
        cause = cause,
        errorCode = "MULTIPROCESS_START_FAILED",
        severity = ErrorSeverity.MEDIUM,
        recoverable = true
    )
    
    class ProcessTimeout(gameId: Int, timeoutMinutes: Long) : MultiProcessError(
        message = "Worker process for game $gameId timed out after $timeoutMinutes minutes",
        errorCode = "MULTIPROCESS_TIMEOUT",
        severity = ErrorSeverity.MEDIUM,
        recoverable = true
    )
    
    class ProcessCrashed(gameId: Int, exitCode: Int, errorOutput: String) : MultiProcessError(
        message = "Worker process for game $gameId crashed with exit code $exitCode: $errorOutput",
        errorCode = "MULTIPROCESS_CRASHED",
        severity = ErrorSeverity.MEDIUM,
        recoverable = true
    )
    
    class ResultParseFailed(gameId: Int, cause: Throwable) : MultiProcessError(
        message = "Failed to parse result from worker process for game $gameId",
        cause = cause,
        errorCode = "MULTIPROCESS_PARSE_FAILED",
        severity = ErrorSeverity.MEDIUM,
        recoverable = true
    )
}

/**
 * Neural network and model errors
 */
sealed class ModelError(
    message: String,
    cause: Throwable? = null,
    errorCode: String,
    severity: ErrorSeverity,
    recoverable: Boolean = true
) : ChessRLError(message, cause, errorCode, severity, recoverable) {
    
    class LoadFailed(path: String, cause: Throwable) : ModelError(
        message = "Failed to load model from: $path",
        cause = cause,
        errorCode = "MODEL_LOAD_FAILED",
        severity = ErrorSeverity.HIGH,
        recoverable = false
    )
    
    class SaveFailed(path: String, cause: Throwable) : ModelError(
        message = "Failed to save model to: $path",
        cause = cause,
        errorCode = "MODEL_SAVE_FAILED",
        severity = ErrorSeverity.MEDIUM,
        recoverable = true
    )
    
    class InferenceFailed(cause: Throwable) : ModelError(
        message = "Neural network inference failed",
        cause = cause,
        errorCode = "MODEL_INFERENCE_FAILED",
        severity = ErrorSeverity.HIGH,
        recoverable = true
    )
    
    class TrainingFailed(cause: Throwable) : ModelError(
        message = "Neural network training update failed",
        cause = cause,
        errorCode = "MODEL_TRAINING_FAILED",
        severity = ErrorSeverity.HIGH,
        recoverable = true
    )
}

/**
 * Error handler that provides recovery mechanisms and graceful degradation
 */
class ErrorHandler {
    
    private val logger = ChessRLLogger.forComponent("ErrorHandler")
    private val errorCounts = mutableMapOf<String, Int>()
    private val maxRetries = mapOf(
        "TRAINING_SELFPLAY_FAILED" to 3,
        "TRAINING_BATCH_FAILED" to 2,
        "MULTIPROCESS_START_FAILED" to 2,
        "MODEL_INFERENCE_FAILED" to 3,
        "MODEL_TRAINING_FAILED" to 2
    )
    
    /**
     * Handle an error with appropriate logging and recovery
     */
    fun handleError(error: ChessRLError): ErrorHandlingResult {
        // Log the error with appropriate level
        when (error.severity) {
            ChessRLError.ErrorSeverity.LOW -> logger.warn("${error.errorCode}: ${error.message}")
            ChessRLError.ErrorSeverity.MEDIUM -> logger.warn("${error.errorCode}: ${error.message}")
            ChessRLError.ErrorSeverity.HIGH -> logger.error("${error.errorCode}: ${error.message}", error)
            ChessRLError.ErrorSeverity.CRITICAL -> logger.error("CRITICAL ${error.errorCode}: ${error.message}", error)
        }
        
        // Track error frequency
        val count = errorCounts.getOrPut(error.errorCode) { 0 } + 1
        errorCounts[error.errorCode] = count
        
        // Determine if retry is appropriate
        val maxRetryCount = maxRetries[error.errorCode] ?: 1
        val shouldRetry = error.recoverable && count <= maxRetryCount
        
        // Suggest recovery action
        val recoveryAction = when {
            !error.recoverable -> RecoveryAction.ABORT
            !shouldRetry -> RecoveryAction.SKIP
            else -> getRecoveryAction(error)
        }
        
        if (count > maxRetryCount) {
            logger.warn("Error ${error.errorCode} exceeded max retries ($maxRetryCount), giving up")
        }
        
        return ErrorHandlingResult(
            error = error,
            recoveryAction = recoveryAction,
            retryCount = count,
            maxRetries = maxRetryCount
        )
    }
    
    /**
     * Determine appropriate recovery action for an error
     */
    private fun getRecoveryAction(error: ChessRLError): RecoveryAction {
        return when (error) {
            is TrainingError.SelfPlayFailed -> RecoveryAction.RETRY_WITH_FALLBACK
            is TrainingError.BatchTrainingFailed -> RecoveryAction.RETRY
            is TrainingError.CheckpointFailed -> RecoveryAction.CONTINUE
            is TrainingError.EvaluationFailed -> RecoveryAction.SKIP
            is MultiProcessError.ProcessStartFailed -> RecoveryAction.RETRY_WITH_FALLBACK
            is MultiProcessError.ProcessTimeout -> RecoveryAction.SKIP
            is MultiProcessError.ProcessCrashed -> RecoveryAction.SKIP
            is MultiProcessError.ResultParseFailed -> RecoveryAction.SKIP
            is ModelError.InferenceFailed -> RecoveryAction.RETRY
            is ModelError.TrainingFailed -> RecoveryAction.RETRY
            is ModelError.SaveFailed -> RecoveryAction.CONTINUE
            else -> RecoveryAction.ABORT
        }
    }
    
    /**
     * Reset error counts (useful between training cycles)
     */
    fun resetErrorCounts() {
        errorCounts.clear()
    }
    
    /**
     * Get error statistics
     */
    fun getErrorStats(): Map<String, Int> {
        return errorCounts.toMap()
    }
}

/**
 * Result of error handling with recommended action
 */
data class ErrorHandlingResult(
    val error: ChessRLError,
    val recoveryAction: RecoveryAction,
    val retryCount: Int,
    val maxRetries: Int
) {
    val shouldRetry: Boolean get() = recoveryAction in listOf(RecoveryAction.RETRY, RecoveryAction.RETRY_WITH_FALLBACK)
    val shouldContinue: Boolean get() = recoveryAction != RecoveryAction.ABORT
    val shouldUseFallback: Boolean get() = recoveryAction == RecoveryAction.RETRY_WITH_FALLBACK
}

/**
 * Recommended recovery actions
 */
enum class RecoveryAction {
    RETRY,                // Retry the same operation
    RETRY_WITH_FALLBACK, // Retry with a fallback approach (e.g., sequential instead of multi-process)
    SKIP,                // Skip this operation and continue
    CONTINUE,            // Continue despite the error
    ABORT                // Stop execution, error is too severe
}

/**
 * Utility functions for safe execution with error handling
 */
object SafeExecution {
    
    /**
     * Execute a block with automatic error handling and retry logic
     */
    fun <T> withErrorHandling(
        operation: String,
        block: () -> T
    ): T? {
        val errorHandler = ErrorHandler()
        var lastError: ChessRLError? = null
        
        for (attempt in 0 until 3) {
            try {
                return block()
            } catch (e: ChessRLError) {
                lastError = e
                val result = errorHandler.handleError(e)
                
                if (!result.shouldRetry) {
                    break
                }
                
                if (attempt < 2) { // Don't log retry on last attempt
                    ChessRLLogger.debug("Retrying $operation (attempt ${attempt + 2})")
                }
            } catch (e: Exception) {
                // Wrap unexpected exceptions
                val wrappedError = TrainingError.InitializationFailed(operation, e)
                lastError = wrappedError
                errorHandler.handleError(wrappedError)
                break
            }
        }
        
        lastError?.let { error ->
            ChessRLLogger.error("Operation '$operation' failed after retries: ${error.message}")
        }
        
        return null
    }
    
    /**
     * Execute a block that may fail, with graceful degradation
     */
    fun <T> withGracefulDegradation(
        operation: String,
        fallback: T,
        block: () -> T
    ): T {
        return withErrorHandling(operation, block) ?: fallback
    }
}

/**
 * Extension functions for easier error creation
 */
fun configurationError(parameter: String, value: Any?, reason: String): Nothing {
    throw ConfigurationError.InvalidParameter(parameter, value, reason)
}

fun trainingError(message: String, cause: Throwable? = null): Nothing {
    throw TrainingError.InitializationFailed(message, cause ?: RuntimeException(message))
}

fun modelError(operation: String, path: String, cause: Throwable): Nothing {
    when (operation) {
        "load" -> throw ModelError.LoadFailed(path, cause)
        "save" -> throw ModelError.SaveFailed(path, cause)
        else -> throw ModelError.InferenceFailed(cause)
    }
}