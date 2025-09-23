package com.chessrl.integration

import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.output.ValidationAggregator
import com.chessrl.rl.*
import kotlin.math.*

/**
 * Consolidated training validator that combines functionality from:
 * - RobustTrainingValidator.kt (comprehensive validation)
 * - ChessTrainingValidator.kt (chess-specific validation)
 * - TrainingValidator.kt (core policy validation)
 * 
 * Provides essential validation for training stability and chess-specific metrics
 * while keeping only the most important validation checks for reliability.
 * 
 * Now includes ValidationAggregator integration to reduce repetitive validation spam.
 */
class TrainingValidator(
    private val config: ChessRLConfig = ChessRLConfig(),
    private val validationAggregator: ValidationAggregator = ValidationAggregator()
) {
    
    // Validation history for trend analysis
    private val validationHistory = mutableListOf<ValidationResult>()
    private val gameHistory = mutableListOf<GameAnalysis>()
    
    // Smoothed metrics for stability
    private var smoothedGradientNorm: Double? = null
    private var smoothedPolicyEntropy: Double? = null
    private var smoothedLoss: Double? = null
    
    /**
     * Validate a training cycle with both policy and chess-specific checks.
     */
    fun validateTrainingCycle(
        cycle: Int,
        trainingMetrics: TrainingMetrics,
        gameResults: List<SelfPlayGameResult>
    ): ValidationResult {
        
        val timestamp = System.currentTimeMillis()
        val issues = mutableListOf<ValidationIssue>()
        val warnings = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // 1. Core policy validation
        validatePolicyMetrics(trainingMetrics, issues, warnings, recommendations)
        
        // 2. Chess-specific validation
        validateChessMetrics(gameResults, issues, warnings, recommendations)
        
        // 3. Learning progress validation
        validateLearningProgress(issues, warnings, recommendations)
        
        // Update smoothed metrics
        updateSmoothedMetrics(trainingMetrics)
        
        // Create validation result
        val result = ValidationResult(
            cycle = cycle,
            timestamp = timestamp,
            isValid = issues.isEmpty(),
            issues = issues,
            warnings = warnings,
            recommendations = recommendations,
            trainingMetrics = trainingMetrics,
            gameAnalysis = analyzeGames(gameResults),
            smoothedGradientNorm = smoothedGradientNorm,
            smoothedPolicyEntropy = smoothedPolicyEntropy,
            smoothedLoss = smoothedLoss
        )
        
        // Record validation
        validationHistory.add(result)
        
        // Add to validation aggregator to reduce spam
        validationAggregator.addValidationResult(result)
        
        // Limit history size
        if (validationHistory.size > 100) {
            validationHistory.removeAt(0)
        }
        
        return result
    }
    
    /**
     * Validate core policy training metrics.
     */
    private fun validatePolicyMetrics(
        metrics: TrainingMetrics,
        issues: MutableList<ValidationIssue>,
        warnings: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        
        // Check for numerical instability
        if (metrics.averageLoss.isNaN() || metrics.averageLoss.isInfinite()) {
            issues.add(ValidationIssue(
                type = IssueType.NUMERICAL_INSTABILITY,
                severity = IssueSeverity.HIGH,
                message = "Loss is NaN or Infinite: ${metrics.averageLoss}",
                value = metrics.averageLoss
            ))
            recommendations.add("Reduce learning rate significantly")
        }
        
        if (metrics.averageGradientNorm.isNaN() || metrics.averageGradientNorm.isInfinite()) {
            issues.add(ValidationIssue(
                type = IssueType.NUMERICAL_INSTABILITY,
                severity = IssueSeverity.HIGH,
                message = "Gradient norm is NaN or Infinite: ${metrics.averageGradientNorm}",
                value = metrics.averageGradientNorm
            ))
            recommendations.add("Apply gradient clipping")
        }
        
        // Check gradient norms (use smoothed values if available)
        val gradNorm = smoothedGradientNorm ?: metrics.averageGradientNorm
        when {
            gradNorm > 10.0 -> {
                issues.add(ValidationIssue(
                    type = IssueType.EXPLODING_GRADIENTS,
                    severity = IssueSeverity.HIGH,
                    message = "Gradient norm too high: $gradNorm",
                    value = gradNorm
                ))
                recommendations.add("Apply gradient clipping or reduce learning rate")
            }
            gradNorm < 1e-6 -> {
                warnings.add("Gradient norm very low: $gradNorm")
                recommendations.add("Consider increasing learning rate")
            }
            gradNorm > 5.0 -> {
                warnings.add("Gradient norm elevated: $gradNorm")
            }
        }
        
        // Check policy entropy (use smoothed values if available)
        val entropy = smoothedPolicyEntropy ?: metrics.averagePolicyEntropy
        when {
            entropy < 0.1 -> {
                issues.add(ValidationIssue(
                    type = IssueType.POLICY_COLLAPSE,
                    severity = IssueSeverity.HIGH,
                    message = "Policy entropy too low: $entropy",
                    value = entropy
                ))
                recommendations.add("Increase exploration rate")
            }
            entropy < 0.5 -> {
                warnings.add("Policy entropy low: $entropy")
                recommendations.add("Monitor exploration and consider entropy bonus")
            }
        }
        
        // Check loss trends
        val loss = smoothedLoss ?: metrics.averageLoss
        if (validationHistory.isNotEmpty()) {
            val previousLoss = validationHistory.last().smoothedLoss ?: validationHistory.last().trainingMetrics.averageLoss
            val lossChange = loss - previousLoss
            
            if (lossChange > 5.0) {
                issues.add(ValidationIssue(
                    type = IssueType.LOSS_EXPLOSION,
                    severity = IssueSeverity.HIGH,
                    message = "Loss increased dramatically: $lossChange",
                    value = lossChange
                ))
                recommendations.add("Reduce learning rate immediately")
            }
        }
    }
    
    /**
     * Validate chess-specific metrics.
     */
    private fun validateChessMetrics(
        gameResults: List<SelfPlayGameResult>,
        issues: MutableList<ValidationIssue>,
        warnings: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        
        if (gameResults.isEmpty()) {
            warnings.add("No games to analyze")
            return
        }
        
        // Check average game length
        val avgGameLength = gameResults.map { it.gameLength }.average()
        when {
            avgGameLength < 10.0 -> {
                issues.add(ValidationIssue(
                    type = IssueType.GAMES_TOO_SHORT,
                    severity = IssueSeverity.MEDIUM,
                    message = "Games too short (avg: $avgGameLength)",
                    value = avgGameLength
                ))
                recommendations.add("Check if agent is making illegal moves")
            }
            avgGameLength > 150.0 -> {
                warnings.add("Games very long (avg: $avgGameLength)")
                recommendations.add("Consider adjusting reward structure")
            }
        }
        
        // Check game outcomes
        val outcomes = gameResults.groupingBy { it.gameOutcome }.eachCount()
        val total = gameResults.size
        val drawRate = (outcomes[GameOutcome.DRAW] ?: 0).toDouble() / total
        
        if (drawRate > 0.7) {
            issues.add(ValidationIssue(
                type = IssueType.TOO_MANY_DRAWS,
                severity = IssueSeverity.MEDIUM,
                message = "High draw rate: ${drawRate * 100}%",
                value = drawRate
            ))
            recommendations.add("Adjust reward structure to discourage draws")
        }
        
        // Check for step limit terminations
        val stepLimitGames = gameResults.count { it.terminationReason == EpisodeTerminationReason.STEP_LIMIT }
        val stepLimitRate = stepLimitGames.toDouble() / total
        
        if (stepLimitRate > 0.5) {
            warnings.add("High step limit termination rate: ${stepLimitRate * 100}%")
            recommendations.add("Consider increasing max steps per game or improving training")
        }
        
        // Action diversity check (unique action indices across all experiences)
        val allActions: List<Int> = gameResults.flatMap { g -> g.experiences.map { it.action } }
        if (allActions.isNotEmpty()) {
            val unique = allActions.toSet().size
            val diversityRatio = unique.toDouble() / allActions.size
            if (diversityRatio < 0.1 && allActions.size > 500) {
                issues.add(ValidationIssue(
                    type = IssueType.LOW_MOVE_DIVERSITY,
                    severity = IssueSeverity.HIGH,
                    message = "Low action diversity: ${"%.2f".format(diversityRatio * 100)}%",
                    value = diversityRatio
                ))
                recommendations.add("Increase exploration rate or adjust epsilon decay")
            }
        }
    }
    
    /**
     * Validate learning progress over time.
     */
    private fun validateLearningProgress(
        issues: MutableList<ValidationIssue>,
        warnings: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        
        if (validationHistory.size < 10) return // Need more data
        
        // Check for stagnation
        val recentHistory = validationHistory.takeLast(10)
        val recentRewards = recentHistory.map { it.trainingMetrics.averageReward }
        val rewardTrend = calculateTrend(recentRewards)
        val rewardStability = calculateStability(recentRewards)
        
        // Check for learning stagnation
        if (abs(rewardTrend) < 0.001 && rewardStability > 0.8) {
            warnings.add("Training may be stagnating (trend: $rewardTrend)")
            recommendations.add("Consider increasing exploration rate or adjusting learning parameters")
        }
        
        // Check for declining performance
        if (rewardTrend < -0.01) {
            issues.add(ValidationIssue(
                type = IssueType.DECLINING_PERFORMANCE,
                severity = IssueSeverity.MEDIUM,
                message = "Performance declining (trend: $rewardTrend)",
                value = rewardTrend
            ))
            recommendations.add("Reduce learning rate or check for training instabilities")
        }
        
        // Check convergence
        if (rewardStability > 0.9 && abs(rewardTrend) < 0.005) {
            val avgReward = recentRewards.average()
            if (avgReward > 0.5) {
                warnings.add("Training appears to have converged")
                recommendations.add("Consider stopping training or reducing learning rate")
            }
        }
    }
    
    /**
     * Analyze games for quality metrics.
     */
    private fun analyzeGames(gameResults: List<SelfPlayGameResult>): GameAnalysis {
        if (gameResults.isEmpty()) {
            return GameAnalysis(
                averageGameLength = 0.0,
                gameCompletionRate = 0.0,
                drawRate = 0.0,
                stepLimitRate = 0.0,
                qualityScore = 0.0
            )
        }
        
        val avgGameLength = gameResults.map { it.gameLength }.average()
        val completedGames = gameResults.count { it.terminationReason == EpisodeTerminationReason.GAME_ENDED }
        val gameCompletionRate = completedGames.toDouble() / gameResults.size
        val drawRate = gameResults.count { it.gameOutcome == GameOutcome.DRAW }.toDouble() / gameResults.size
        val stepLimitRate = gameResults.count { it.terminationReason == EpisodeTerminationReason.STEP_LIMIT }.toDouble() / gameResults.size
        
        // Calculate overall quality score
        val lengthScore = when {
            avgGameLength < 10 -> 0.3
            avgGameLength > 200 -> 0.5
            else -> 1.0
        }
        val completionScore = gameCompletionRate
        val diversityScore = 1.0 - drawRate // Lower draw rate = higher diversity
        
        val qualityScore = (lengthScore + completionScore + diversityScore) / 3.0
        
        val analysis = GameAnalysis(
            averageGameLength = avgGameLength,
            gameCompletionRate = gameCompletionRate,
            drawRate = drawRate,
            stepLimitRate = stepLimitRate,
            qualityScore = qualityScore
        )
        
        gameHistory.add(analysis)
        
        // Limit history size
        if (gameHistory.size > 100) {
            gameHistory.removeAt(0)
        }
        
        return analysis
    }
    
    /**
     * Update smoothed metrics using exponential moving average.
     */
    private fun updateSmoothedMetrics(metrics: TrainingMetrics) {
        val alpha = 0.2 // Smoothing factor
        
        smoothedGradientNorm = if (smoothedGradientNorm == null) {
            metrics.averageGradientNorm
        } else {
            alpha * metrics.averageGradientNorm + (1 - alpha) * smoothedGradientNorm!!
        }
        
        smoothedPolicyEntropy = if (smoothedPolicyEntropy == null) {
            metrics.averagePolicyEntropy
        } else {
            alpha * metrics.averagePolicyEntropy + (1 - alpha) * smoothedPolicyEntropy!!
        }
        
        smoothedLoss = if (smoothedLoss == null) {
            metrics.averageLoss
        } else {
            alpha * metrics.averageLoss + (1 - alpha) * smoothedLoss!!
        }
    }
    
    /**
     * Calculate trend using linear regression.
     */
    private fun calculateTrend(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val n = values.size
        val x = (0 until n).map { it.toDouble() }
        val y = values
        
        val xMean = x.average()
        val yMean = y.average()
        
        val numerator = x.zip(y) { xi, yi -> (xi - xMean) * (yi - yMean) }.sum()
        val denominator = x.map { (it - xMean).pow(2) }.sum()
        
        return if (denominator != 0.0) numerator / denominator else 0.0
    }
    
    /**
     * Calculate stability (inverse of coefficient of variation).
     */
    private fun calculateStability(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        
        return if (abs(mean) > 1e-8) {
            1.0 / (1.0 + stdDev / abs(mean))
        } else {
            if (stdDev < 1e-8) 1.0 else 0.0
        }
    }
    
    /**
     * Get validation summary.
     */
    fun getValidationSummary(): ValidationSummary {
        val totalValidations = validationHistory.size
        val validValidations = validationHistory.count { it.isValid }
        val recentIssues = validationHistory.takeLast(10).flatMap { it.issues }
        
        return ValidationSummary(
            totalValidations = totalValidations,
            validValidations = validValidations,
            validationRate = if (totalValidations > 0) validValidations.toDouble() / totalValidations else 0.0,
            recentIssues = recentIssues,
            lastValidation = validationHistory.lastOrNull()
        )
    }
    
    /**
     * Get aggregated validation messages to reduce spam
     */
    fun getAggregatedValidationMessages() = validationAggregator.getAggregatedMessages()
    
    /**
     * Get new validation messages since the specified time
     */
    fun getNewValidationMessages(sinceTime: Long) = validationAggregator.getNewMessages(sinceTime)
    
    /**
     * Get changed validation messages since the specified time
     */
    fun getChangedValidationMessages(sinceTime: Long) = validationAggregator.getChangedMessages(sinceTime)
    
    /**
     * Clear old validation messages to prevent memory buildup
     */
    fun clearOldValidationMessages(olderThanMs: Long = 300000L) {
        validationAggregator.clearOldMessages(olderThanMs)
    }
    
    /**
     * Get validation aggregator statistics
     */
    fun getValidationAggregatorStats() = validationAggregator.getSummaryStats()
    
    /**
     * Clear validation history.
     */
    fun clearHistory() {
        validationHistory.clear()
        gameHistory.clear()
        validationAggregator.clearHistory()
        smoothedGradientNorm = null
        smoothedPolicyEntropy = null
        smoothedLoss = null
    }

    /**
     * Lightweight convergence analysis over RL metrics.
     * Provided for test compatibility with earlier API expectations.
     */
    fun analyzeConvergence(
        trainingHistory: List<com.chessrl.rl.RLMetrics>,
        windowSize: Int
    ): SimpleConvergenceAnalysis {
        if (trainingHistory.size < 2) {
            return SimpleConvergenceAnalysis(
                status = com.chessrl.rl.ConvergenceStatus.INSUFFICIENT_DATA,
                rewardTrend = 0.0,
                stability = 0.0
            )
        }

        val recent = trainingHistory.takeLast(windowSize.coerceAtMost(trainingHistory.size))
        val rewards = recent.map { it.averageReward }
        val trend = calculateTrend(rewards)
        val stability = calculateStability(rewards)

        val status = when {
            recent.size < 10 -> com.chessrl.rl.ConvergenceStatus.INSUFFICIENT_DATA
            stability > 0.9 && kotlin.math.abs(trend) < 0.005 -> com.chessrl.rl.ConvergenceStatus.CONVERGED
            trend > 0.0 -> com.chessrl.rl.ConvergenceStatus.IMPROVING
            else -> com.chessrl.rl.ConvergenceStatus.STAGNANT
        }

        return SimpleConvergenceAnalysis(status, trend, stability)
    }

    /**
     * Validate a policy update (compatibility helper for integration tests).
     */
    fun validatePolicyUpdate(
        beforeMetrics: com.chessrl.rl.RLMetrics,
        afterMetrics: com.chessrl.rl.RLMetrics,
        updateResult: com.chessrl.rl.PolicyUpdateResult,
        episode: Int
    ): PolicyValidationResult {
        val rlValidator = com.chessrl.rl.RLValidator()
        // Use the RL validator's checks and augment with episode/context if needed
        val core = rlValidator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult)
        val detectedIssues = rlValidator.detectTrainingIssues(afterMetrics).toMutableList()

        // Map string issues from core validator to TrainingIssue when possible
        core.issues.forEach { msg ->
            when {
                msg.contains("Exploding gradients", ignoreCase = true) -> detectedIssues.add(com.chessrl.rl.TrainingIssue.EXPLODING_GRADIENTS)
                msg.contains("Vanishing gradients", ignoreCase = true) -> detectedIssues.add(com.chessrl.rl.TrainingIssue.VANISHING_GRADIENTS)
                msg.contains("Policy collapse", ignoreCase = true) -> detectedIssues.add(com.chessrl.rl.TrainingIssue.POLICY_COLLAPSE)
                msg.contains("Insufficient exploration", ignoreCase = true) -> detectedIssues.add(com.chessrl.rl.TrainingIssue.EXPLORATION_INSUFFICIENT)
            }
        }

        return PolicyValidationResult(
            episode = episode,
            isValid = core.isValid,
            issues = detectedIssues,
            recommendations = core.recommendations
        )
    }
}

/**
 * Data classes for validation results
 */

data class ValidationResult(
    val cycle: Int,
    val timestamp: Long,
    val isValid: Boolean,
    val issues: List<ValidationIssue>,
    val warnings: List<String>,
    val recommendations: List<String>,
    val trainingMetrics: TrainingMetrics,
    val gameAnalysis: GameAnalysis,
    val smoothedGradientNorm: Double?,
    val smoothedPolicyEntropy: Double?,
    val smoothedLoss: Double?
)

data class ValidationIssue(
    val type: IssueType,
    val severity: IssueSeverity,
    val message: String,
    val value: Double
)

enum class IssueType {
    NUMERICAL_INSTABILITY,
    EXPLODING_GRADIENTS,
    VANISHING_GRADIENTS,
    POLICY_COLLAPSE,
    LOSS_EXPLOSION,
    GAMES_TOO_SHORT,
    TOO_MANY_DRAWS,
    LOW_MOVE_DIVERSITY,
    DECLINING_PERFORMANCE,
    EXPLORATION_INSUFFICIENT
}

enum class IssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class GameAnalysis(
    val averageGameLength: Double,
    val gameCompletionRate: Double,
    val drawRate: Double,
    val stepLimitRate: Double,
    val qualityScore: Double
)

data class ValidationSummary(
    val totalValidations: Int,
    val validValidations: Int,
    val validationRate: Double,
    val recentIssues: List<ValidationIssue>,
    val lastValidation: ValidationResult?
)

/**
 * Simple convergence analysis result for test compatibility.
 */
data class SimpleConvergenceAnalysis(
    val status: com.chessrl.rl.ConvergenceStatus,
    val rewardTrend: Double,
    val stability: Double
)

/**
 * Policy update validation result for integration tests (RL issue-based).
 */
data class PolicyValidationResult(
    val episode: Int,
    val isValid: Boolean,
    val issues: List<com.chessrl.rl.TrainingIssue>,
    val recommendations: List<String>
)

/**
 * Aggregate summary over multiple policy validation results.
 */
data class PolicyValidationSummary(
    val totalIssues: Int,
    val issuesByType: Map<com.chessrl.rl.TrainingIssue, Int>
) {
    companion object {
        fun from(results: List<PolicyValidationResult>): PolicyValidationSummary {
            val allIssues = results.flatMap { it.issues }
            val byType = allIssues.groupingBy { it }.eachCount()
            return PolicyValidationSummary(totalIssues = allIssues.size, issuesByType = byType)
        }
    }
}

// Convenience alias for older tests
fun summarizePolicyValidations(results: List<PolicyValidationResult>): PolicyValidationSummary =
    PolicyValidationSummary.from(results)
