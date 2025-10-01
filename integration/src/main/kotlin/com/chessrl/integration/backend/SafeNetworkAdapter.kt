package com.chessrl.integration.backend

import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.rl.NeuralNetwork
import kotlin.math.abs

/**
 * Safe wrapper for neural network adapters that provides fallback error handling
 * and recovery mechanisms for production use. This wrapper ensures that training
 * can continue even if the primary backend encounters issues.
 */
class SafeNetworkAdapter(
    private val primary: NetworkAdapter,
    private val fallback: NetworkAdapter? = null,
    private val config: SafetyConfig = SafetyConfig()
) : NetworkAdapter {
    
    private val logger = ChessRLLogger.forComponent("SafeNetworkAdapter")
    private var primaryFailureCount = 0
    private var fallbackActive = false
    
    init {
        logger.info("SafeNetworkAdapter initialized with primary: ${primary.getBackendName()}, fallback: ${fallback?.getBackendName() ?: "none"}")
    }
    
    override fun forward(input: DoubleArray): DoubleArray {
        return executeWithFallback("forward") {
            val output = it.forward(input)
            validateOutput(output, "forward")
            output
        }
    }
    
    override fun backward(target: DoubleArray): DoubleArray {
        return executeWithFallback("backward") {
            val lossArray = it.backward(target)
            validateLossArray(lossArray, "backward")
            lossArray
        }
    }
    
    override fun trainBatch(inputs: Array<DoubleArray>, targets: Array<DoubleArray>): Double {
        return executeWithFallback("trainBatch") {
            val loss = it.trainBatch(inputs, targets)
            validateLoss(loss)
            loss
        }
    }
    
    override fun copyWeightsTo(target: NeuralNetwork) {
        executeWithFallback("copyWeightsTo") {
            it.copyWeightsTo(target)
            Unit // Return Unit for consistency with executeWithFallback
        }
    }
    
    override fun save(path: String) {
        executeWithFallback("save") {
            it.save(path)
            Unit
        }
    }
    
    override fun load(path: String) {
        executeWithFallback("load") {
            it.load(path)
            Unit
        }
    }
    
    override fun getBackendName(): String {
        val activeName = getActiveAdapter().getBackendName()
        return if (fallbackActive) {
            "safe($activeName-fallback)"
        } else {
            "safe($activeName)"
        }
    }
    
    override fun getConfig(): BackendConfig = getActiveAdapter().getConfig()
    
    override fun getParameterCount(): Long {
        return executeWithFallback("getParameterCount") {
            it.getParameterCount()
        }
    }
    
    override fun getMemoryUsage(): Long {
        return executeWithFallback("getMemoryUsage") {
            it.getMemoryUsage()
        }
    }
    
    override fun validateGradients(): Boolean {
        return try {
            executeWithFallback("validateGradients") {
                it.validateGradients()
            }
        } catch (e: Exception) {
            logger.warn("Gradient validation failed: ${e.message}")
            false
        }
    }
    
    /**
     * Execute an operation with fallback handling
     */
    private fun <T> executeWithFallback(operationName: String, operation: (NetworkAdapter) -> T): T {
        val activeAdapter = getActiveAdapter()
        
        return try {
            val result = operation(activeAdapter)
            
            // Reset failure count on successful operation
            if (activeAdapter == primary) {
                primaryFailureCount = 0
            }
            
            result
            
        } catch (e: Exception) {
            handleOperationFailure(operationName, e, activeAdapter)
            
            // Try fallback if available and not already using it
            if (fallback != null && activeAdapter == primary && !fallbackActive) {
                logger.warn("Primary backend failed for $operationName, attempting fallback")
                
                try {
                    val result = operation(fallback)
                    activateFallback()
                    result
                } catch (fallbackException: Exception) {
                    logger.error("Both primary and fallback backends failed for $operationName")
                    throw RuntimeException(
                        "Both primary (${primary.getBackendName()}) and fallback (${fallback.getBackendName()}) backends failed for $operationName. " +
                        "Primary error: ${e.message}, Fallback error: ${fallbackException.message}",
                        e
                    )
                }
            } else {
                // No fallback available or already using fallback
                throw RuntimeException(
                    "Backend ${activeAdapter.getBackendName()} failed for $operationName: ${e.message}",
                    e
                )
            }
        }
    }
    
    /**
     * Handle operation failure and update failure tracking
     */
    private fun handleOperationFailure(operationName: String, exception: Exception, adapter: NetworkAdapter) {
        if (adapter == primary) {
            primaryFailureCount++
            logger.warn("Primary backend failure #$primaryFailureCount for $operationName: ${exception.message}")
            
            if (primaryFailureCount >= config.maxPrimaryFailures) {
                logger.error("Primary backend has failed $primaryFailureCount times, marking as unreliable")
            }
        } else {
            logger.error("Fallback backend failed for $operationName: ${exception.message}")
        }
    }
    
    /**
     * Activate fallback backend
     */
    private fun activateFallback() {
        if (!fallbackActive && fallback != null) {
            fallbackActive = true
            logger.warn("Switched to fallback backend: ${fallback.getBackendName()}")
        }
    }
    
    /**
     * Get the currently active adapter
     */
    private fun getActiveAdapter(): NetworkAdapter {
        return if (fallbackActive && fallback != null) {
            fallback
        } else {
            primary
        }
    }
    
    /**
     * Validate output arrays for common issues
     */
    private fun validateOutput(output: DoubleArray, operationName: String) {
        // Check for non-finite values
        val nonFiniteCount = output.count { !it.isFinite() }
        if (nonFiniteCount > 0) {
            throw RuntimeException("$operationName produced $nonFiniteCount non-finite values out of ${output.size}")
        }
        
        // Check for extreme values that might indicate numerical instability
        val maxAbs = output.maxOfOrNull { abs(it) } ?: 0.0
        if (maxAbs > config.maxOutputValue) {
            if (config.strictValidation) {
                throw RuntimeException("$operationName produced extreme output value: $maxAbs (max allowed: ${config.maxOutputValue})")
            } else {
                logger.warn("$operationName produced large output value: $maxAbs")
            }
        }
        
        // Check for all-zero outputs (might indicate dead network)
        if (output.all { abs(it) < config.minOutputVariance }) {
            if (config.strictValidation) {
                throw RuntimeException("$operationName produced near-zero outputs (max abs: ${output.maxOfOrNull { abs(it) }})")
            } else {
                logger.warn("$operationName produced very small outputs, network might be dead")
            }
        }
    }
    
    /**
     * Validate loss values
     */
    private fun validateLoss(loss: Double) {
        if (!loss.isFinite()) {
            throw RuntimeException("Training produced non-finite loss: $loss")
        }
        
        if (loss < 0.0) {
            throw RuntimeException("Training produced negative loss: $loss")
        }
        
        if (loss > config.maxLossValue) {
            if (config.strictValidation) {
                throw RuntimeException("Training produced excessive loss: $loss (max allowed: ${config.maxLossValue})")
            } else {
                logger.warn("Training produced high loss: $loss")
            }
        }
    }
    
    /**
     * Validate loss array from backward method
     */
    private fun validateLossArray(lossArray: DoubleArray, operationName: String) {
        if (lossArray.size != 1) {
            throw RuntimeException("$operationName should return single loss value, got ${lossArray.size} values")
        }
        
        val loss = lossArray[0]
        validateLoss(loss)
    }
    
    /**
     * Get safety statistics
     */
    fun getSafetyStats(): SafetyStats {
        return SafetyStats(
            primaryBackend = primary.getBackendName(),
            fallbackBackend = fallback?.getBackendName(),
            primaryFailureCount = primaryFailureCount,
            fallbackActive = fallbackActive,
            activeBackend = getActiveAdapter().getBackendName()
        )
    }
    
    /**
     * Reset failure tracking (useful for testing or recovery)
     */
    fun resetFailureTracking() {
        primaryFailureCount = 0
        fallbackActive = false
        logger.info("Failure tracking reset, switched back to primary backend")
    }
    
    /**
     * Force switch to fallback backend (for testing)
     */
    fun forceFallback() {
        if (fallback != null) {
            activateFallback()
            logger.info("Forced switch to fallback backend")
        } else {
            throw IllegalStateException("No fallback backend available")
        }
    }
    
    /**
     * Check if the adapter is currently healthy
     */
    fun isHealthy(): Boolean {
        return try {
            // Perform a simple health check
            val testInput = DoubleArray(getConfig().inputSize) { 0.1 }
            val output = forward(testInput)
            output.all { it.isFinite() }
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Configuration for SafeNetworkAdapter safety mechanisms
 */
data class SafetyConfig(
    val maxPrimaryFailures: Int = 3,
    val maxOutputValue: Double = 1000.0,
    val maxLossValue: Double = 10000.0,
    val minOutputVariance: Double = 1e-10,
    val strictValidation: Boolean = false
)

/**
 * Statistics about SafeNetworkAdapter operation
 */
data class SafetyStats(
    val primaryBackend: String,
    val fallbackBackend: String?,
    val primaryFailureCount: Int,
    val fallbackActive: Boolean,
    val activeBackend: String
) {
    fun getSummary(): String {
        return buildString {
            appendLine("SafeNetworkAdapter Statistics:")
            appendLine("Primary Backend: $primaryBackend")
            appendLine("Fallback Backend: ${fallbackBackend ?: "none"}")
            appendLine("Primary Failures: $primaryFailureCount")
            appendLine("Fallback Active: $fallbackActive")
            appendLine("Active Backend: $activeBackend")
        }
    }
}