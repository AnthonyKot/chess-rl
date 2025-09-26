package com.chessrl.integration.backend

import com.chessrl.integration.logging.ChessRLLogger
import kotlin.math.*
import kotlin.random.Random

/**
 * Comprehensive validation utilities for neural network adapters.
 * Provides thorough testing of adapter functionality including dimension checks,
 * finite value validation, and gradient validation.
 */
object AdapterValidator {
    
    private val logger = ChessRLLogger.forComponent("AdapterValidator")
    
    /**
     * Perform comprehensive validation of a network adapter
     */
    fun validateAdapter(adapter: NetworkAdapter): AdapterValidationResult {
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val config = adapter.getConfig()
        
        logger.info("Starting comprehensive validation for ${adapter.getBackendName()} adapter")
        
        try {
            // 1. Basic configuration validation
            validateConfiguration(config, issues)
            
            // 2. Forward pass validation
            validateForwardPass(adapter, issues, warnings)
            
            // 3. Batch training validation
            validateBatchTraining(adapter, issues, warnings)
            
            // 4. Weight synchronization validation
            validateWeightSynchronization(adapter, issues, warnings)
            
            // 5. Save/load validation
            validateSaveLoad(adapter, issues, warnings)
            
            // 6. Performance characteristics validation
            validatePerformanceCharacteristics(adapter, warnings)
            
            // 7. Gradient validation
            validateGradients(adapter, issues, warnings)
            
        } catch (e: Exception) {
            issues.add("Validation failed with exception: ${e.message}")
            logger.error("Adapter validation failed with exception", e)
        }
        
        val result = AdapterValidationResult(
            isValid = issues.isEmpty(),
            backend = adapter.getBackendName(),
            issues = issues,
            warnings = warnings
        )
        
        if (result.isValid) {
            logger.info("Adapter validation passed for ${adapter.getBackendName()}")
        } else {
            logger.error("Adapter validation failed for ${adapter.getBackendName()}: ${issues.joinToString(", ")}")
        }
        
        return result
    }
    
    /**
     * Validate basic configuration parameters
     */
    private fun validateConfiguration(config: BackendConfig, issues: MutableList<String>) {
        if (config.inputSize <= 0) {
            issues.add("Input size must be positive, got ${config.inputSize}")
        }
        
        if (config.outputSize <= 0) {
            issues.add("Output size must be positive, got ${config.outputSize}")
        }
        
        if (config.hiddenLayers.any { it <= 0 }) {
            issues.add("All hidden layer sizes must be positive, got ${config.hiddenLayers}")
        }
        
        if (config.learningRate <= 0.0 || config.learningRate > 1.0) {
            issues.add("Learning rate must be in (0, 1], got ${config.learningRate}")
        }
        
        if (config.l2Regularization < 0.0) {
            issues.add("L2 regularization must be non-negative, got ${config.l2Regularization}")
        }
        
        if (config.batchSize <= 0) {
            issues.add("Batch size must be positive, got ${config.batchSize}")
        }
        
        if (config.gradientClipping <= 0.0) {
            issues.add("Gradient clipping must be positive, got ${config.gradientClipping}")
        }
    }
    
    /**
     * Validate forward pass functionality
     */
    private fun validateForwardPass(
        adapter: NetworkAdapter, 
        issues: MutableList<String>, 
        warnings: MutableList<String>
    ) {
        val config = adapter.getConfig()
        
        try {
            // Test with random input
            val testInput = DoubleArray(config.inputSize) { Random.nextDouble(-1.0, 1.0) }
            val output = adapter.forward(testInput)
            
            // Check output dimensions
            if (output.size != config.outputSize) {
                issues.add("Forward pass output size ${output.size} doesn't match expected ${config.outputSize}")
                return
            }
            
            // Check for finite values
            val nonFiniteCount = output.count { !it.isFinite() }
            if (nonFiniteCount > 0) {
                issues.add("Forward pass produced $nonFiniteCount non-finite values out of ${output.size}")
            }
            
            // Check output range (warn if values are extreme)
            val maxAbs = output.maxOfOrNull { abs(it) } ?: 0.0
            if (maxAbs > 1000.0) {
                warnings.add("Forward pass produced large output values (max absolute: $maxAbs)")
            }
            
            // Test with edge cases
            validateForwardPassEdgeCases(adapter, issues, warnings)
            
        } catch (e: Exception) {
            issues.add("Forward pass validation failed: ${e.message}")
        }
    }
    
    /**
     * Test forward pass with edge case inputs
     */
    private fun validateForwardPassEdgeCases(
        adapter: NetworkAdapter,
        issues: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val config = adapter.getConfig()
        
        // Test with zero input
        try {
            val zeroInput = DoubleArray(config.inputSize) { 0.0 }
            val zeroOutput = adapter.forward(zeroInput)
            if (zeroOutput.any { !it.isFinite() }) {
                issues.add("Forward pass with zero input produced non-finite values")
            }
        } catch (e: Exception) {
            issues.add("Forward pass with zero input failed: ${e.message}")
        }
        
        // Test with small positive input
        try {
            val smallInput = DoubleArray(config.inputSize) { 0.001 }
            val smallOutput = adapter.forward(smallInput)
            if (smallOutput.any { !it.isFinite() }) {
                warnings.add("Forward pass with small input produced non-finite values")
            }
        } catch (e: Exception) {
            warnings.add("Forward pass with small input failed: ${e.message}")
        }
        
        // Test with large input (but not extreme)
        try {
            val largeInput = DoubleArray(config.inputSize) { 10.0 }
            val largeOutput = adapter.forward(largeInput)
            if (largeOutput.any { !it.isFinite() }) {
                warnings.add("Forward pass with large input produced non-finite values")
            }
        } catch (e: Exception) {
            warnings.add("Forward pass with large input failed: ${e.message}")
        }
    }
    
    /**
     * Validate batch training functionality
     */
    private fun validateBatchTraining(
        adapter: NetworkAdapter,
        issues: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val config = adapter.getConfig()
        
        try {
            // Create small training batch
            val batchSize = min(4, config.batchSize)
            val inputs = Array(batchSize) { 
                DoubleArray(config.inputSize) { Random.nextDouble(-1.0, 1.0) }
            }
            val targets = Array(batchSize) { 
                DoubleArray(config.outputSize) { Random.nextDouble(-1.0, 1.0) }
            }
            
            // Test batch training
            val loss = adapter.trainBatch(inputs, targets)
            
            // Check loss is finite
            if (!loss.isFinite()) {
                issues.add("Batch training produced non-finite loss: $loss")
            }
            
            // Check loss is non-negative
            if (loss < 0.0) {
                issues.add("Batch training produced negative loss: $loss")
            }
            
            // Test multiple training steps to check for stability
            validateTrainingStability(adapter, inputs, targets, warnings)
            
        } catch (e: Exception) {
            issues.add("Batch training validation failed: ${e.message}")
        }
    }
    
    /**
     * Test training stability over multiple steps
     */
    private fun validateTrainingStability(
        adapter: NetworkAdapter,
        inputs: Array<DoubleArray>,
        targets: Array<DoubleArray>,
        warnings: MutableList<String>
    ) {
        try {
            val losses = mutableListOf<Double>()
            
            // Run several training steps
            repeat(5) {
                val loss = adapter.trainBatch(inputs, targets)
                losses.add(loss)
                
                if (!loss.isFinite()) {
                    warnings.add("Training became unstable at step ${it + 1}")
                    return
                }
            }
            
            // Check if loss is exploding
            val maxLoss = losses.maxOrNull() ?: 0.0
            if (maxLoss > 1000.0) {
                warnings.add("Training loss appears to be exploding (max: $maxLoss)")
            }
            
            // Check if loss is decreasing (at least not increasing dramatically)
            val firstLoss = losses.first()
            val lastLoss = losses.last()
            if (lastLoss > firstLoss * 10.0) {
                warnings.add("Training loss increased dramatically from $firstLoss to $lastLoss")
            }
            
        } catch (e: Exception) {
            warnings.add("Training stability test failed: ${e.message}")
        }
    }
    
    /**
     * Validate weight synchronization functionality
     */
    private fun validateWeightSynchronization(
        adapter: NetworkAdapter,
        issues: MutableList<String>,
        warnings: MutableList<String>
    ) {
        try {
            // Create a second adapter of the same type for testing
            val targetAdapter = createAdapterOfSameType(adapter)
            
            // Get initial outputs from both networks
            val testInput = DoubleArray(adapter.getConfig().inputSize) { Random.nextDouble(-1.0, 1.0) }
            val sourceOutput1 = adapter.forward(testInput)
            val targetOutput1 = targetAdapter.forward(testInput)
            
            // Outputs should be different initially (different random initialization)
            val initialDifference = computeOutputDifference(sourceOutput1, targetOutput1)
            
            // Copy weights
            adapter.copyWeightsTo(targetAdapter)
            
            // Get outputs after weight copying
            val sourceOutput2 = adapter.forward(testInput)
            val targetOutput2 = targetAdapter.forward(testInput)
            
            // Outputs should now be identical (or very close)
            val finalDifference = computeOutputDifference(sourceOutput2, targetOutput2)
            
            if (finalDifference > 1e-10) {
                issues.add("Weight synchronization failed: output difference after copying is $finalDifference")
            }
            
            // Source output should be unchanged by the copy operation
            val sourceDifference = computeOutputDifference(sourceOutput1, sourceOutput2)
            if (sourceDifference > 1e-10) {
                warnings.add("Source network output changed during weight copying: difference $sourceDifference")
            }
            
        } catch (e: UnsupportedOperationException) {
            // This is expected for cross-backend weight copying
            warnings.add("Weight synchronization not supported: ${e.message}")
        } catch (e: Exception) {
            issues.add("Weight synchronization validation failed: ${e.message}")
        }
    }
    
    /**
     * Create another adapter of the same type for testing
     */
    private fun createAdapterOfSameType(adapter: NetworkAdapter): NetworkAdapter {
        val backendName = adapter.getBackendName()
        
        // Handle SafeNetworkAdapter by extracting the underlying backend type
        val actualBackendName = if (backendName.startsWith("safe(") && backendName.endsWith(")")) {
            val innerName = backendName.substring(5, backendName.length - 1)
            // Remove any additional suffixes like "-fallback"
            if (innerName.contains("-")) {
                innerName.substringBefore("-")
            } else {
                innerName
            }
        } else {
            backendName
        }
        
        val backendType = BackendType.fromString(actualBackendName)
            ?: throw IllegalArgumentException("Unknown backend type: $actualBackendName (from ${adapter.getBackendName()})")
        
        return when (backendType) {
            BackendType.MANUAL -> ManualNetworkAdapter(adapter.getConfig())
            BackendType.DL4J -> {
                // Use reflection to create DL4J adapter if available
                try {
                    val clazz = Class.forName("com.chessrl.integration.backend.Dl4jNetworkAdapter")
                    val constructor = clazz.getConstructor(BackendConfig::class.java)
                    constructor.newInstance(adapter.getConfig()) as NetworkAdapter
                } catch (e: Exception) {
                    throw UnsupportedOperationException("Cannot create DL4J adapter for testing: ${e.message}")
                }
            }
            BackendType.KOTLINDL -> {
                throw UnsupportedOperationException("KotlinDL adapter not yet implemented")
            }
        }
    }
    
    /**
     * Compute the difference between two output arrays
     */
    private fun computeOutputDifference(output1: DoubleArray, output2: DoubleArray): Double {
        require(output1.size == output2.size) { "Output arrays must have same size" }
        
        var sumSquaredDiff = 0.0
        for (i in output1.indices) {
            val diff = output1[i] - output2[i]
            sumSquaredDiff += diff * diff
        }
        return sqrt(sumSquaredDiff / output1.size)
    }
    
    /**
     * Validate save/load functionality
     */
    private fun validateSaveLoad(
        adapter: NetworkAdapter,
        issues: MutableList<String>,
        warnings: MutableList<String>
    ) {
        try {
            // Create a temporary file path for testing
            val tempPath = "temp_validation_model_${System.currentTimeMillis()}"
            
            // Get initial output
            val testInput = DoubleArray(adapter.getConfig().inputSize) { Random.nextDouble(-1.0, 1.0) }
            val originalOutput = adapter.forward(testInput)
            
            // Save the model
            adapter.save(tempPath)
            
            // Modify the network by training it
            val batchInputs = Array(2) { DoubleArray(adapter.getConfig().inputSize) { Random.nextDouble(-1.0, 1.0) } }
            val batchTargets = Array(2) { DoubleArray(adapter.getConfig().outputSize) { Random.nextDouble(-1.0, 1.0) } }
            adapter.trainBatch(batchInputs, batchTargets)
            
            // Get modified output
            val modifiedOutput = adapter.forward(testInput)
            
            // Load the saved model
            adapter.load(tempPath)
            
            // Get restored output
            val restoredOutput = adapter.forward(testInput)
            
            // Check that restored output matches original
            val restorationDifference = computeOutputDifference(originalOutput, restoredOutput)
            if (restorationDifference > 1e-10) {
                issues.add("Save/load round-trip failed: output difference is $restorationDifference")
            }
            
            // Check that modification actually changed the network
            val modificationDifference = computeOutputDifference(originalOutput, modifiedOutput)
            if (modificationDifference < 1e-10) {
                warnings.add("Training did not appear to modify the network (difference: $modificationDifference)")
            }
            
            // Clean up temporary files
            cleanupTempFiles(tempPath)
            
        } catch (e: Exception) {
            issues.add("Save/load validation failed: ${e.message}")
        }
    }
    
    /**
     * Clean up temporary files created during validation
     */
    private fun cleanupTempFiles(basePath: String) {
        try {
            // Try to delete common file extensions
            val extensions = listOf("", ".json", ".zip", ".optimizer", ".meta.json")
            for (ext in extensions) {
                val path = "$basePath$ext"
                try {
                    com.chessrl.integration.deleteFile(path)
                } catch (e: Exception) {
                    // Ignore cleanup failures
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup failures
        }
    }
    
    /**
     * Validate performance characteristics
     */
    private fun validatePerformanceCharacteristics(
        adapter: NetworkAdapter,
        warnings: MutableList<String>
    ) {
        try {
            // Check parameter count is reasonable
            val paramCount = adapter.getParameterCount()
            if (paramCount <= 0) {
                warnings.add("Parameter count is not positive: $paramCount")
            }
            
            // Check memory usage is reasonable
            val memoryUsage = adapter.getMemoryUsage()
            if (memoryUsage <= 0) {
                warnings.add("Memory usage is not positive: $memoryUsage")
            }
            
            // Rough sanity check: memory usage should be at least parameter count * 8 bytes
            if (memoryUsage < paramCount * 8) {
                warnings.add("Memory usage ($memoryUsage bytes) seems too low for parameter count ($paramCount)")
            }
            
        } catch (e: Exception) {
            warnings.add("Performance characteristics validation failed: ${e.message}")
        }
    }
    
    /**
     * Validate gradient computation
     */
    private fun validateGradients(
        adapter: NetworkAdapter,
        issues: MutableList<String>,
        warnings: MutableList<String>
    ) {
        try {
            // Use the adapter's built-in gradient validation
            val isValid = adapter.validateGradients()
            if (!isValid) {
                issues.add("Adapter's gradient validation failed")
            }
            
            // Additional gradient validation using numerical differentiation
            val gradientValidation = GradientValidator.validateGradients(adapter)
            if (!gradientValidation.isValid) {
                issues.addAll(gradientValidation.issues.map { "Gradient validation: $it" })
            }
            warnings.addAll(gradientValidation.warnings.map { "Gradient validation: $it" })
            
        } catch (e: Exception) {
            warnings.add("Gradient validation failed: ${e.message}")
        }
    }
}

/**
 * Result of comprehensive adapter validation
 */
data class AdapterValidationResult(
    val isValid: Boolean,
    val backend: String,
    val issues: List<String>,
    val warnings: List<String> = emptyList()
) {
    /**
     * Get a summary of the validation result
     */
    fun getSummary(): String {
        return buildString {
            appendLine("Validation Result for $backend backend:")
            appendLine("Status: ${if (isValid) "PASSED" else "FAILED"}")
            
            if (issues.isNotEmpty()) {
                appendLine("Issues (${issues.size}):")
                issues.forEach { appendLine("  - $it") }
            }
            
            if (warnings.isNotEmpty()) {
                appendLine("Warnings (${warnings.size}):")
                warnings.forEach { appendLine("  - $it") }
            }
            
            if (isValid && warnings.isEmpty()) {
                appendLine("All validation checks passed successfully.")
            }
        }
    }
}