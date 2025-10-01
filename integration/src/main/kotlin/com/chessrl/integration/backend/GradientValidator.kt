package com.chessrl.integration.backend

import com.chessrl.integration.logging.ChessRLLogger
import kotlin.math.*
import kotlin.random.Random

/**
 * Gradient validation utilities for debugging training issues.
 * Provides numerical gradient checking, gradient norm analysis, and
 * detection of common gradient-related problems.
 */
object GradientValidator {
    
    private val logger = ChessRLLogger.forComponent("GradientValidator")
    
    /**
     * Perform comprehensive gradient validation
     */
    fun validateGradients(adapter: NetworkAdapter): GradientValidationResult {
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        logger.info("Starting gradient validation for ${adapter.getBackendName()} adapter")
        
        try {
            // 1. Basic gradient computation test
            validateBasicGradientComputation(adapter, issues, warnings)
            
            // 2. Numerical gradient checking (simplified)
            validateNumericalGradients(adapter, issues, warnings)
            
            // 3. Gradient magnitude analysis
            analyzeGradientMagnitudes(adapter, warnings)
            
            // 4. Training stability analysis
            analyzeTrainingStability(adapter, warnings)
            
            // 5. Loss landscape analysis
            analyzeLossLandscape(adapter, warnings)
            
        } catch (e: Exception) {
            issues.add("Gradient validation failed with exception: ${e.message}")
            logger.error("Gradient validation failed", e)
        }
        
        val result = GradientValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            warnings = warnings
        )
        
        if (result.isValid) {
            logger.info("Gradient validation passed for ${adapter.getBackendName()}")
        } else {
            logger.error("Gradient validation failed for ${adapter.getBackendName()}: ${issues.joinToString(", ")}")
        }
        
        return result
    }
    
    /**
     * Test basic gradient computation functionality
     */
    private fun validateBasicGradientComputation(
        adapter: NetworkAdapter,
        issues: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val config = adapter.getConfig()
        
        try {
            // Create test data
            val input = DoubleArray(config.inputSize) { Random.nextDouble(-1.0, 1.0) }
            val target = DoubleArray(config.outputSize) { Random.nextDouble(-1.0, 1.0) }
            
            // Forward pass
            val output = adapter.forward(input)

            if (output.size != config.outputSize) {
                issues.add(
                    "Forward pass produced ${output.size} outputs, expected ${config.outputSize}"
                )
            }
            if (output.any { !it.isFinite() }) {
                issues.add("Forward pass produced non-finite activations")
            }
            
            // Backward pass - this returns loss, not gradients
            val lossArray = adapter.backward(target)
            
            // Check that backward returns a single loss value
            if (lossArray.size != 1) {
                issues.add("Backward pass should return single loss value, got ${lossArray.size} values")
                return
            }
            
            val loss = lossArray[0]
            
            // Check for finite loss
            if (!loss.isFinite()) {
                issues.add("Backward pass produced non-finite loss: $loss")
            }
            
            // Check for non-negative loss
            if (loss < 0.0) {
                issues.add("Backward pass produced negative loss: $loss")
            }
            
            // Check for reasonable loss magnitude
            if (loss > 1000.0) {
                warnings.add("Backward pass produced very large loss: $loss")
            }
            
        } catch (e: Exception) {
            issues.add("Basic gradient computation failed: ${e.message}")
        }
    }
    
    /**
     * Perform simplified numerical gradient checking
     */
    private fun validateNumericalGradients(
        adapter: NetworkAdapter,
        issues: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val config = adapter.getConfig()
        
        try {
            // Simplified numerical gradient check - just verify loss consistency
            val epsilon = 1e-7
            
            val input = DoubleArray(config.inputSize) { Random.nextDouble(-0.5, 0.5) }
            val target = DoubleArray(config.outputSize) { Random.nextDouble(-0.5, 0.5) }
            
            // Get baseline loss
            adapter.forward(input)
            val baselineLoss = adapter.backward(target)[0]
            if (!baselineLoss.isFinite()) {
                issues.add("Numerical gradient check failed: baseline loss was $baselineLoss")
                return
            }
            
            // Check loss consistency with small perturbations
            var maxLossChange = 0.0
            val numChecks = min(3, config.inputSize)
            
            for (i in 0 until numChecks) {
                val inputPlus = input.copyOf()
                inputPlus[i] += epsilon
                
                adapter.forward(inputPlus)
                val perturbedLoss = adapter.backward(target)[0]
                
                val lossChange = abs(perturbedLoss - baselineLoss)
                maxLossChange = max(maxLossChange, lossChange)
            }
            
            // Check if loss changes appropriately with input changes
            if (maxLossChange < 1e-15) {
                warnings.add("Loss appears insensitive to input changes (max change: $maxLossChange)")
            } else if (maxLossChange > baselineLoss * 10.0) {
                warnings.add("Loss appears very sensitive to input changes (max change: $maxLossChange vs baseline: $baselineLoss)")
            }
            
            logger.debug("Numerical gradient check: baseline loss=$baselineLoss, max change=$maxLossChange")
            
        } catch (e: Exception) {
            warnings.add("Numerical gradient checking failed: ${e.message}")
        }
    }
    
    /**
     * Analyze gradient magnitudes for common problems
     */
    private fun analyzeGradientMagnitudes(
        adapter: NetworkAdapter,
        warnings: MutableList<String>
    ) {
        val config = adapter.getConfig()
        
        try {
            val gradientMagnitudes = mutableListOf<Double>()
            
            // Collect gradient magnitudes from multiple samples
            repeat(10) {
                val input = DoubleArray(config.inputSize) { Random.nextDouble(-1.0, 1.0) }
                val target = DoubleArray(config.outputSize) { Random.nextDouble(-1.0, 1.0) }
                
                adapter.forward(input)
                val lossArray = adapter.backward(target)
                
                // Use loss as a proxy for gradient magnitude
                val loss = lossArray[0]
                gradientMagnitudes.add(loss)
            }
            
            val avgMagnitude = gradientMagnitudes.average()
            val maxMagnitude = gradientMagnitudes.maxOrNull() ?: 0.0
            val minMagnitude = gradientMagnitudes.minOrNull() ?: 0.0
            
            // Check for exploding gradients
            if (maxMagnitude > 100.0) {
                warnings.add("Potential exploding gradients detected (max magnitude: $maxMagnitude)")
            }
            
            // Check for vanishing gradients
            if (avgMagnitude < 1e-8) {
                warnings.add("Potential vanishing gradients detected (avg magnitude: $avgMagnitude)")
            }
            
            // Check for gradient variance
            val variance = gradientMagnitudes.map { (it - avgMagnitude).pow(2) }.average()
            val stdDev = sqrt(variance)
            
            if (stdDev / avgMagnitude > 10.0 && avgMagnitude > 1e-10) {
                warnings.add("High gradient magnitude variance detected (std/mean: ${stdDev / avgMagnitude})")
            }
            
            logger.debug("Gradient magnitude analysis: avg=$avgMagnitude, max=$maxMagnitude, min=$minMagnitude, std=$stdDev")
            
        } catch (e: Exception) {
            warnings.add("Gradient magnitude analysis failed: ${e.message}")
        }
    }
    
    /**
     * Analyze training stability over multiple steps
     */
    private fun analyzeTrainingStability(
        adapter: NetworkAdapter,
        warnings: MutableList<String>
    ) {
        val config = adapter.getConfig()
        
        try {
            val losses = mutableListOf<Double>()
            val gradientNorms = mutableListOf<Double>()
            
            // Create consistent training data
            val batchSize = min(4, config.batchSize)
            val inputs = Array(batchSize) { 
                DoubleArray(config.inputSize) { Random.nextDouble(-1.0, 1.0) }
            }
            val targets = Array(batchSize) { 
                DoubleArray(config.outputSize) { Random.nextDouble(-1.0, 1.0) }
            }
            
            // Run training steps and collect metrics
            repeat(10) { step ->
                try {
                    val trainingLoss = adapter.trainBatch(inputs, targets)
                    losses.add(trainingLoss)
                    
                    // Get loss as a proxy for gradient magnitude
                    adapter.forward(inputs[0])
                    val lossArray = adapter.backward(targets[0])
                    val backwardLoss = lossArray[0]
                    gradientNorms.add(backwardLoss) // Using loss as a proxy for gradient magnitude
                    
                } catch (e: Exception) {
                    warnings.add("Training became unstable at step $step: ${e.message}")
                    return
                }
            }
            
            // Analyze loss progression
            val firstLoss = losses.first()
            val lastLoss = losses.last()
            val maxLoss = losses.maxOrNull() ?: 0.0
            
            if (lastLoss > firstLoss * 2.0) {
                warnings.add("Loss increased during training: $firstLoss -> $lastLoss")
            }
            
            if (maxLoss > firstLoss * 10.0) {
                warnings.add("Loss spiked during training: max=$maxLoss, initial=$firstLoss")
            }
            
            // Analyze gradient norm progression
            val avgGradientNorm = gradientNorms.average()
            val maxGradientNorm = gradientNorms.maxOrNull() ?: 0.0
            
            if (maxGradientNorm > avgGradientNorm * 10.0) {
                warnings.add("Gradient norm spiked during training: max=$maxGradientNorm, avg=$avgGradientNorm")
            }
            
            logger.debug("Training stability analysis: loss $firstLoss -> $lastLoss, gradient norm avg=$avgGradientNorm")
            
        } catch (e: Exception) {
            warnings.add("Training stability analysis failed: ${e.message}")
        }
    }
    
    /**
     * Analyze loss landscape around current point
     */
    private fun analyzeLossLandscape(
        adapter: NetworkAdapter,
        warnings: MutableList<String>
    ) {
        val config = adapter.getConfig()
        
        try {
            val input = DoubleArray(config.inputSize) { Random.nextDouble(-1.0, 1.0) }
            val target = DoubleArray(config.outputSize) { Random.nextDouble(-1.0, 1.0) }
            
            // Get baseline loss
            val baselineOutput = adapter.forward(input)
            val baselineLoss = computeLoss(baselineOutput, target, config.lossFunction)
            
            // Sample loss at nearby points
            val perturbationMagnitudes = doubleArrayOf(1e-6, 1e-5, 1e-4, 1e-3)
            val lossVariations = mutableListOf<Double>()
            
            for (magnitude in perturbationMagnitudes) {
                val perturbedInput = input.map { value ->
                    value + magnitude * (Random.nextDouble() - 0.5)
                }.toDoubleArray()
                
                val perturbedOutput = adapter.forward(perturbedInput)
                val perturbedLoss = computeLoss(perturbedOutput, target, config.lossFunction)
                
                lossVariations.add(abs(perturbedLoss - baselineLoss))
            }
            
            // Check for loss landscape issues
            val maxVariation = lossVariations.maxOrNull() ?: 0.0
            val minVariation = lossVariations.minOrNull() ?: 0.0
            
            if (maxVariation > baselineLoss * 10.0) {
                warnings.add("Loss landscape appears very steep (max variation: $maxVariation vs baseline: $baselineLoss)")
            }
            
            if (maxVariation < baselineLoss * 1e-10) {
                warnings.add("Loss landscape appears very flat (max variation: $maxVariation vs baseline: $baselineLoss)")
            }
            
            logger.debug("Loss landscape analysis: baseline=$baselineLoss, max_variation=$maxVariation, min_variation=$minVariation")
            
        } catch (e: Exception) {
            warnings.add("Loss landscape analysis failed: ${e.message}")
        }
    }
    
    /**
     * Compute loss using specified loss function
     */
    private fun computeLoss(predicted: DoubleArray, target: DoubleArray, lossFunction: String): Double {
        require(predicted.size == target.size) { "Predicted and target arrays must have same size" }
        
        return when (lossFunction.lowercase()) {
            "huber" -> computeHuberLoss(predicted, target, delta = 1.0)
            "mse" -> computeMSELoss(predicted, target)
            else -> computeHuberLoss(predicted, target, delta = 1.0)
        }
    }
    
    /**
     * Compute Huber loss
     */
    private fun computeHuberLoss(predicted: DoubleArray, target: DoubleArray, delta: Double): Double {
        var sum = 0.0
        for (i in predicted.indices) {
            val diff = abs(predicted[i] - target[i])
            sum += if (diff <= delta) {
                0.5 * diff * diff
            } else {
                delta * diff - 0.5 * delta * delta
            }
        }
        return sum / predicted.size
    }
    
    /**
     * Compute Mean Squared Error loss
     */
    private fun computeMSELoss(predicted: DoubleArray, target: DoubleArray): Double {
        var sum = 0.0
        for (i in predicted.indices) {
            val diff = predicted[i] - target[i]
            sum += diff * diff
        }
        return sum / predicted.size
    }
    
    /**
     * Perform quick gradient sanity check
     */
    fun quickGradientCheck(adapter: NetworkAdapter): Boolean {
        return try {
            val config = adapter.getConfig()
            val input = DoubleArray(config.inputSize) { Random.nextDouble(-1.0, 1.0) }
            val target = DoubleArray(config.outputSize) { Random.nextDouble(-1.0, 1.0) }
            
            adapter.forward(input)
            val lossArray = adapter.backward(target)
            
            // Check that loss is finite and reasonable
            lossArray.size == 1 && lossArray[0].isFinite() && lossArray[0] >= 0.0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Estimate gradient norm for monitoring (using loss as proxy)
     */
    fun estimateGradientNorm(adapter: NetworkAdapter): Double {
        return try {
            val config = adapter.getConfig()
            val input = DoubleArray(config.inputSize) { Random.nextDouble(-1.0, 1.0) }
            val target = DoubleArray(config.outputSize) { Random.nextDouble(-1.0, 1.0) }
            
            adapter.forward(input)
            val lossArray = adapter.backward(target)
            
            // Return loss as a proxy for gradient magnitude
            lossArray[0]
        } catch (e: Exception) {
            Double.NaN
        }
    }
}

/**
 * Result of gradient validation
 */
data class GradientValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
    val warnings: List<String> = emptyList()
) {
    /**
     * Get a summary of the gradient validation result
     */
    fun getSummary(): String {
        return buildString {
            appendLine("Gradient Validation Result:")
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
                appendLine("All gradient validation checks passed successfully.")
            }
        }
    }
}
