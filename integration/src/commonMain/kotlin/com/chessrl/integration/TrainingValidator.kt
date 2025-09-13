package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.math.*

/**
 * Comprehensive training validation framework for chess RL
 * Provides validation for policy updates, convergence analysis, and training issue detection
 */
class TrainingValidator(
    private val config: ValidationConfig = ValidationConfig()
) {
    
    private val validationHistory = mutableListOf<ValidationSnapshot>()
    private val issueHistory = mutableListOf<TrainingIssueEvent>()
    
    /**
     * Validate a policy update and detect potential issues
     */
    fun validatePolicyUpdate(
        beforeMetrics: RLMetrics,
        afterMetrics: RLMetrics,
        updateResult: PolicyUpdateResult,
        episodeNumber: Int
    ): PolicyValidationResult {
        
        val issues = mutableListOf<ValidationIssue>()
        val warnings = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // Numerical stability checks
        validateNumericalStability(updateResult, issues, warnings)
        
        // Gradient analysis
        validateGradients(updateResult, issues, warnings, recommendations)
        
        // Policy entropy analysis
        validatePolicyEntropy(updateResult, issues, warnings, recommendations)
        
        // Learning progress analysis
        validateLearningProgress(beforeMetrics, afterMetrics, issues, warnings, recommendations)
        
        // Q-value analysis (for DQN)
        updateResult.qValueMean?.let { qMean ->
            updateResult.targetValueMean?.let { targetMean ->
                validateQValues(qMean, targetMean, issues, warnings, recommendations)
            }
        }
        
        // Create validation result
        val result = PolicyValidationResult(
            episodeNumber = episodeNumber,
            isValid = issues.isEmpty(),
            issues = issues,
            warnings = warnings,
            recommendations = recommendations,
            updateMetrics = updateResult,
            beforeMetrics = beforeMetrics,
            afterMetrics = afterMetrics,
            timestamp = getCurrentTimeMillis()
        )
        
        // Record validation snapshot
        recordValidationSnapshot(result)
        
        // Record issues if any
        if (issues.isNotEmpty()) {
            recordTrainingIssues(issues, episodeNumber)
        }
        
        return result
    }
    
    /**
     * Analyze training convergence based on historical metrics
     */
    fun analyzeConvergence(
        trainingHistory: List<RLMetrics>,
        windowSize: Int = config.convergenceWindowSize
    ): ConvergenceAnalysis {
        
        if (trainingHistory.size < windowSize) {
            return ConvergenceAnalysis(
                status = com.chessrl.rl.ConvergenceStatus.INSUFFICIENT_DATA,
                confidence = 0.0,
                trendDirection = TrendDirection.UNKNOWN,
                stabilityScore = 0.0,
                recommendations = listOf("Need at least $windowSize episodes for convergence analysis")
            )
        }
        
        val recentMetrics = trainingHistory.takeLast(windowSize)
        val rewards = recentMetrics.map { it.averageReward }
        val losses = recentMetrics.map { it.policyLoss }
        
        // Calculate trend and stability
        val rewardTrend = calculateTrend(rewards)
        val lossTrend = calculateTrend(losses)
        val rewardStability = calculateStability(rewards)
        val lossStability = calculateStability(losses)
        
        // Determine convergence status
        val status = determineConvergenceStatus(rewardTrend, lossTrend, rewardStability, lossStability)
        val confidence = calculateConvergenceConfidence(rewardStability, lossStability)
        val trendDirection = determineTrendDirection(rewardTrend)
        val stabilityScore = (rewardStability + lossStability) / 2.0
        
        // Generate recommendations
        val recommendations = generateConvergenceRecommendations(status, rewardTrend, lossTrend, stabilityScore)
        
        return ConvergenceAnalysis(
            status = status,
            confidence = confidence,
            trendDirection = trendDirection,
            stabilityScore = stabilityScore,
            rewardTrend = rewardTrend,
            lossTrend = lossTrend,
            rewardStability = rewardStability,
            lossStability = lossStability,
            recommendations = recommendations
        )
    }
    
    /**
     * Detect and classify training issues
     */
    fun detectTrainingIssues(
        metrics: RLMetrics,
        updateResult: PolicyUpdateResult? = null
    ): List<TrainingIssueDetection> {
        
        val detections = mutableListOf<TrainingIssueDetection>()
        
        // Gradient-related issues
        updateResult?.let { result ->
            if (result.gradientNorm > config.explodingGradientThreshold) {
                detections.add(TrainingIssueDetection(
                    issue = TrainingIssue.EXPLODING_GRADIENTS,
                    severity = IssueSeverity.HIGH,
                    value = result.gradientNorm,
                    threshold = config.explodingGradientThreshold,
                    description = "Gradient norm (${result.gradientNorm}) exceeds threshold",
                    recommendations = listOf(
                        "Apply gradient clipping",
                        "Reduce learning rate",
                        "Check network architecture"
                    )
                ))
            }
            
            if (result.gradientNorm < config.vanishingGradientThreshold) {
                detections.add(TrainingIssueDetection(
                    issue = TrainingIssue.VANISHING_GRADIENTS,
                    severity = IssueSeverity.MEDIUM,
                    value = result.gradientNorm,
                    threshold = config.vanishingGradientThreshold,
                    description = "Gradient norm (${result.gradientNorm}) below threshold",
                    recommendations = listOf(
                        "Increase learning rate",
                        "Use different activation functions",
                        "Check network initialization"
                    )
                ))
            }
            
            // Policy collapse detection
            if (result.policyEntropy < config.policyCollapseThreshold) {
                detections.add(TrainingIssueDetection(
                    issue = TrainingIssue.POLICY_COLLAPSE,
                    severity = IssueSeverity.HIGH,
                    value = result.policyEntropy,
                    threshold = config.policyCollapseThreshold,
                    description = "Policy entropy (${result.policyEntropy}) indicates policy collapse",
                    recommendations = listOf(
                        "Increase exploration rate",
                        "Add entropy regularization",
                        "Use different exploration strategy"
                    )
                ))
            }
        }
        
        // Exploration issues
        if (metrics.explorationRate < config.insufficientExplorationThreshold) {
            detections.add(TrainingIssueDetection(
                issue = TrainingIssue.EXPLORATION_INSUFFICIENT,
                severity = IssueSeverity.MEDIUM,
                value = metrics.explorationRate,
                threshold = config.insufficientExplorationThreshold,
                description = "Exploration rate (${metrics.explorationRate}) may be too low",
                recommendations = listOf(
                    "Increase exploration rate",
                    "Use different exploration strategy",
                    "Add exploration bonus"
                )
            ))
        }
        
        // Q-value overestimation (for DQN)
        metrics.qValueStats?.let { qStats ->
            if (qStats.meanQValue > config.qValueOverestimationThreshold) {
                detections.add(TrainingIssueDetection(
                    issue = TrainingIssue.VALUE_OVERESTIMATION,
                    severity = IssueSeverity.MEDIUM,
                    value = qStats.meanQValue,
                    threshold = config.qValueOverestimationThreshold,
                    description = "Mean Q-value (${qStats.meanQValue}) indicates overestimation",
                    recommendations = listOf(
                        "Use double DQN",
                        "Reduce learning rate",
                        "Increase target network update frequency"
                    )
                ))
            }
        }
        
        // Learning rate issues based on loss behavior
        if (metrics.policyLoss.isNaN() || metrics.policyLoss.isInfinite()) {
            detections.add(TrainingIssueDetection(
                issue = TrainingIssue.LEARNING_RATE_TOO_HIGH,
                severity = IssueSeverity.HIGH,
                value = metrics.policyLoss,
                threshold = Double.NaN,
                description = "Loss is NaN/Infinite, likely due to high learning rate",
                recommendations = listOf(
                    "Reduce learning rate significantly",
                    "Check gradient clipping",
                    "Restart training with lower learning rate"
                )
            ))
        }
        
        return detections
    }
    
    /**
     * Get validation statistics and summary
     */
    fun getValidationSummary(): ValidationSummary {
        val totalValidations = validationHistory.size
        val validValidations = validationHistory.count { it.result.isValid }
        val totalIssues = issueHistory.size
        
        val issuesByType = issueHistory.flatMap { event -> 
            event.issues.map { issue -> 
                when (issue.type) {
                    IssueType.EXPLODING_GRADIENTS -> TrainingIssue.EXPLODING_GRADIENTS
                    IssueType.VANISHING_GRADIENTS -> TrainingIssue.VANISHING_GRADIENTS
                    IssueType.POLICY_COLLAPSE -> TrainingIssue.POLICY_COLLAPSE
                    IssueType.NUMERICAL_INSTABILITY -> TrainingIssue.LEARNING_RATE_TOO_HIGH
                    IssueType.LOSS_EXPLOSION -> TrainingIssue.LEARNING_RATE_TOO_HIGH
                    IssueType.Q_VALUE_OVERESTIMATION -> TrainingIssue.VALUE_OVERESTIMATION
                    IssueType.EXPLORATION_INSUFFICIENT -> TrainingIssue.EXPLORATION_INSUFFICIENT
                }
            }
        }.groupBy { it }.mapValues { it.value.size }
        
        val recentIssues = issueHistory.takeLast(10)
        
        return ValidationSummary(
            totalValidations = totalValidations,
            validValidations = validValidations,
            validationRate = if (totalValidations > 0) validValidations.toDouble() / totalValidations else 0.0,
            totalIssues = totalIssues,
            issuesByType = issuesByType,
            recentIssues = recentIssues,
            lastValidation = validationHistory.lastOrNull()?.result
        )
    }
    
    /**
     * Clear validation history (for testing or reset)
     */
    fun clearHistory() {
        validationHistory.clear()
        issueHistory.clear()
    }
    
    // Private helper methods
    
    private fun validateNumericalStability(
        updateResult: PolicyUpdateResult,
        issues: MutableList<ValidationIssue>,
        warnings: MutableList<String>
    ) {
        if (updateResult.loss.isNaN() || updateResult.loss.isInfinite()) {
            issues.add(ValidationIssue(
                type = IssueType.NUMERICAL_INSTABILITY,
                severity = IssueSeverity.HIGH,
                message = "Loss is NaN or Infinite: ${updateResult.loss}",
                value = updateResult.loss
            ))
        }
        
        if (updateResult.gradientNorm.isNaN() || updateResult.gradientNorm.isInfinite()) {
            issues.add(ValidationIssue(
                type = IssueType.NUMERICAL_INSTABILITY,
                severity = IssueSeverity.HIGH,
                message = "Gradient norm is NaN or Infinite: ${updateResult.gradientNorm}",
                value = updateResult.gradientNorm
            ))
        }
    }
    
    private fun validateGradients(
        updateResult: PolicyUpdateResult,
        issues: MutableList<ValidationIssue>,
        warnings: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        when {
            updateResult.gradientNorm > config.explodingGradientThreshold -> {
                issues.add(ValidationIssue(
                    type = IssueType.EXPLODING_GRADIENTS,
                    severity = IssueSeverity.HIGH,
                    message = "Gradient norm too high: ${updateResult.gradientNorm}",
                    value = updateResult.gradientNorm
                ))
                recommendations.add("Apply gradient clipping or reduce learning rate")
            }
            updateResult.gradientNorm < config.vanishingGradientThreshold -> {
                warnings.add("Gradient norm very low: ${updateResult.gradientNorm}")
                recommendations.add("Consider increasing learning rate or changing architecture")
            }
            updateResult.gradientNorm > config.gradientWarningThreshold -> {
                warnings.add("Gradient norm elevated: ${updateResult.gradientNorm}")
            }
        }
    }
    
    private fun validatePolicyEntropy(
        updateResult: PolicyUpdateResult,
        issues: MutableList<ValidationIssue>,
        warnings: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        when {
            updateResult.policyEntropy < config.policyCollapseThreshold -> {
                issues.add(ValidationIssue(
                    type = IssueType.POLICY_COLLAPSE,
                    severity = IssueSeverity.HIGH,
                    message = "Policy entropy too low: ${updateResult.policyEntropy}",
                    value = updateResult.policyEntropy
                ))
                recommendations.add("Increase exploration or add entropy regularization")
            }
            updateResult.policyEntropy < config.entropyWarningThreshold -> {
                warnings.add("Policy entropy low: ${updateResult.policyEntropy}")
                recommendations.add("Monitor exploration and consider entropy bonus")
            }
        }
    }
    
    private fun validateLearningProgress(
        beforeMetrics: RLMetrics,
        afterMetrics: RLMetrics,
        issues: MutableList<ValidationIssue>,
        warnings: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        val rewardChange = afterMetrics.averageReward - beforeMetrics.averageReward
        val lossChange = afterMetrics.policyLoss - beforeMetrics.policyLoss
        
        // Check for reward stagnation
        if (abs(rewardChange) < config.rewardStagnationThreshold && 
            afterMetrics.explorationRate < config.insufficientExplorationThreshold) {
            warnings.add("Reward stagnation with low exploration")
            recommendations.add("Increase exploration rate or change strategy")
        }
        
        // Check for loss explosion
        if (lossChange > config.lossExplosionThreshold) {
            issues.add(ValidationIssue(
                type = IssueType.LOSS_EXPLOSION,
                severity = IssueSeverity.HIGH,
                message = "Loss increased dramatically: ${lossChange}",
                value = lossChange
            ))
            recommendations.add("Reduce learning rate immediately")
        }
    }
    
    private fun validateQValues(
        qMean: Double,
        targetMean: Double,
        issues: MutableList<ValidationIssue>,
        warnings: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        if (qMean > config.qValueOverestimationThreshold) {
            warnings.add("Q-values may be overestimated: mean = $qMean")
            recommendations.add("Consider using double DQN or reducing learning rate")
        }
        
        val qTargetDiff = abs(qMean - targetMean)
        if (qTargetDiff > config.qTargetDivergenceThreshold) {
            warnings.add("Q-values diverging from targets: diff = $qTargetDiff")
            recommendations.add("Update target network more frequently")
        }
    }
    
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
    
    private fun calculateStability(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        
        // Stability score: higher is more stable (inverse of coefficient of variation)
        return if (abs(mean) > 1e-8) {
            1.0 / (1.0 + stdDev / abs(mean))
        } else {
            if (stdDev < 1e-8) 1.0 else 0.0
        }
    }
    
    private fun determineConvergenceStatus(
        rewardTrend: Double,
        lossTrend: Double,
        rewardStability: Double,
        lossStability: Double
    ): com.chessrl.rl.ConvergenceStatus {
        val isStable = rewardStability > config.stabilityThreshold && lossStability > config.stabilityThreshold
        val isImproving = rewardTrend > config.improvementThreshold
        val isLossDecreasing = lossTrend < -config.improvementThreshold
        
        return when {
            isStable && (isImproving || isLossDecreasing) -> com.chessrl.rl.ConvergenceStatus.CONVERGED
            isImproving || isLossDecreasing -> com.chessrl.rl.ConvergenceStatus.IMPROVING
            isStable -> com.chessrl.rl.ConvergenceStatus.CONVERGED
            else -> com.chessrl.rl.ConvergenceStatus.STAGNANT
        }
    }
    
    private fun calculateConvergenceConfidence(rewardStability: Double, lossStability: Double): Double {
        return (rewardStability + lossStability) / 2.0
    }
    
    private fun determineTrendDirection(trend: Double): TrendDirection {
        return when {
            trend > config.improvementThreshold -> TrendDirection.IMPROVING
            trend < -config.improvementThreshold -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }
    }
    
    private fun generateConvergenceRecommendations(
        status: com.chessrl.rl.ConvergenceStatus,
        rewardTrend: Double,
        lossTrend: Double,
        stabilityScore: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        when (status) {
            com.chessrl.rl.ConvergenceStatus.CONVERGED -> {
                recommendations.add("Training appears to have converged")
                if (stabilityScore < 0.8) {
                    recommendations.add("Consider reducing learning rate for better stability")
                }
            }
            com.chessrl.rl.ConvergenceStatus.IMPROVING -> {
                recommendations.add("Training is progressing well, continue current settings")
                if (rewardTrend < 0.01) {
                    recommendations.add("Progress is slow, consider increasing learning rate")
                }
            }
            com.chessrl.rl.ConvergenceStatus.STAGNANT -> {
                recommendations.add("Training has stagnated")
                recommendations.add("Consider increasing exploration rate")
                recommendations.add("Try different learning rate or network architecture")
                if (lossTrend > 0) {
                    recommendations.add("Loss is increasing, reduce learning rate")
                }
            }
            com.chessrl.rl.ConvergenceStatus.INSUFFICIENT_DATA -> {
                recommendations.add("Continue training to gather more data")
            }
        }
        
        return recommendations
    }
    
    private fun recordValidationSnapshot(result: PolicyValidationResult) {
        val snapshot = ValidationSnapshot(
            timestamp = getCurrentTimeMillis(),
            episodeNumber = result.episodeNumber,
            result = result
        )
        
        validationHistory.add(snapshot)
        
        // Limit history size
        if (validationHistory.size > config.maxHistorySize) {
            validationHistory.removeAt(0)
        }
    }
    
    private fun recordTrainingIssues(issues: List<ValidationIssue>, episodeNumber: Int) {
        val event = TrainingIssueEvent(
            timestamp = getCurrentTimeMillis(),
            episodeNumber = episodeNumber,
            issues = issues
        )
        
        issueHistory.add(event)
        
        // Limit history size
        if (issueHistory.size > config.maxHistorySize) {
            issueHistory.removeAt(0)
        }
    }
}

/**
 * Configuration for training validation
 */
data class ValidationConfig(
    // Gradient thresholds
    val explodingGradientThreshold: Double = 10.0,
    val vanishingGradientThreshold: Double = 1e-6,
    val gradientWarningThreshold: Double = 5.0,
    
    // Policy entropy thresholds
    val policyCollapseThreshold: Double = 0.1,
    val entropyWarningThreshold: Double = 0.5,
    
    // Exploration thresholds
    val insufficientExplorationThreshold: Double = 0.01,
    
    // Q-value thresholds
    val qValueOverestimationThreshold: Double = 100.0,
    val qTargetDivergenceThreshold: Double = 50.0,
    
    // Learning progress thresholds
    val rewardStagnationThreshold: Double = 0.001,
    val lossExplosionThreshold: Double = 10.0,
    
    // Convergence analysis
    val convergenceWindowSize: Int = 50,
    val stabilityThreshold: Double = 0.7,
    val improvementThreshold: Double = 0.001,
    
    // History management
    val maxHistorySize: Int = 1000
)

/**
 * Result of policy update validation
 */
data class PolicyValidationResult(
    val episodeNumber: Int,
    val isValid: Boolean,
    val issues: List<ValidationIssue>,
    val warnings: List<String>,
    val recommendations: List<String>,
    val updateMetrics: PolicyUpdateResult,
    val beforeMetrics: RLMetrics,
    val afterMetrics: RLMetrics,
    val timestamp: Long
)

/**
 * Validation issue with details
 */
data class ValidationIssue(
    val type: IssueType,
    val severity: IssueSeverity,
    val message: String,
    val value: Double,
    val threshold: Double? = null
)

/**
 * Types of validation issues
 */
enum class IssueType {
    NUMERICAL_INSTABILITY,
    EXPLODING_GRADIENTS,
    VANISHING_GRADIENTS,
    POLICY_COLLAPSE,
    LOSS_EXPLOSION,
    Q_VALUE_OVERESTIMATION,
    EXPLORATION_INSUFFICIENT
}

/**
 * Severity levels for issues
 */
enum class IssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Convergence analysis result
 */
data class ConvergenceAnalysis(
    val status: com.chessrl.rl.ConvergenceStatus,
    val confidence: Double,
    val trendDirection: TrendDirection,
    val stabilityScore: Double,
    val rewardTrend: Double = 0.0,
    val lossTrend: Double = 0.0,
    val rewardStability: Double = 0.0,
    val lossStability: Double = 0.0,
    val recommendations: List<String>
)

/**
 * Trend direction for metrics
 */
// Using shared TrendDirection from SharedDataClasses.kt

/**
 * Training issue detection result
 */
data class TrainingIssueDetection(
    val issue: TrainingIssue,
    val severity: IssueSeverity,
    val value: Double,
    val threshold: Double,
    val description: String,
    val recommendations: List<String>
)

/**
 * Validation summary statistics
 */
data class ValidationSummary(
    val totalValidations: Int,
    val validValidations: Int,
    val validationRate: Double,
    val totalIssues: Int,
    val issuesByType: Map<TrainingIssue, Int>,
    val recentIssues: List<TrainingIssueEvent>,
    val lastValidation: PolicyValidationResult?
)

/**
 * Validation snapshot for history tracking
 */
data class ValidationSnapshot(
    val timestamp: Long,
    val episodeNumber: Int,
    val result: PolicyValidationResult
)

/**
 * Training issue event for history tracking
 */
data class TrainingIssueEvent(
    val timestamp: Long,
    val episodeNumber: Int,
    val issues: List<ValidationIssue>
)