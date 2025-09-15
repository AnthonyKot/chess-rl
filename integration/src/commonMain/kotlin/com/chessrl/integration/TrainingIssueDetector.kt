package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.math.*

/**
 * Training issue detector for automated quality assurance.
 * 
 * Provides:
 * - Integration with existing training validation framework
 * - Automated detection of training issues (gradient problems, policy collapse, etc.)
 * - Game quality assessment with automated recommendations
 * - Learning curve analysis with convergence detection
 */
class TrainingIssueDetector(
    private val config: IssueDetectionConfig
) {
    
    // Issue detection state
    private val detectionHistory = mutableListOf<IssueDetectionResult>()
    private val issuePatterns = mutableMapOf<IssueType, IssuePattern>()
    private var baselineMetrics: TrainingCycleMetrics? = null
    
    // Predictive analysis
    private val trendAnalyzer = TrendAnalyzer()
    private val anomalyDetector = AnomalyDetector()
    
    /**
     * Initialize the issue detector
     */
    fun initialize() {
        initializeIssuePatterns()
        println("🔍 Training Issue Detector initialized")
    }
    
    /**
     * Detect training issues in current cycle
     */
    fun detectIssues(
        currentMetrics: TrainingCycleMetrics,
        trainingHistory: List<TrainingCycleMetrics>
    ): List<DetectedIssue> {
        
        val detectedIssues = mutableListOf<DetectedIssue>()
        
        // Set baseline if not set
        if (baselineMetrics == null && trainingHistory.isNotEmpty()) {
            baselineMetrics = trainingHistory.first()
        }
        
        // Gradient-related issues
        detectedIssues.addAll(detectGradientIssues(currentMetrics))
        
        // Policy-related issues
        detectedIssues.addAll(detectPolicyIssues(currentMetrics, trainingHistory))
        
        // Performance-related issues
        detectedIssues.addAll(detectPerformanceIssues(currentMetrics, trainingHistory))
        
        // Training stability issues
        detectedIssues.addAll(detectStabilityIssues(currentMetrics, trainingHistory))
        
        // Game quality issues
        detectedIssues.addAll(detectGameQualityIssues(currentMetrics))
        
        // Convergence issues
        detectedIssues.addAll(detectConvergenceIssues(trainingHistory))
        
        // Predictive issues (if enabled)
        if (config.enablePredictiveAnalysis) {
            detectedIssues.addAll(detectPredictiveIssues(currentMetrics, trainingHistory))
        }
        
        // Record detection result
        val detectionResult = IssueDetectionResult(
            cycle = currentMetrics.cycle,
            timestamp = getCurrentTimeMillis(),
            detectedIssues = detectedIssues,
            metricsAnalyzed = currentMetrics
        )
        detectionHistory.add(detectionResult)
        
        // Update issue patterns
        updateIssuePatterns(detectedIssues)
        
        return detectedIssues
    }
    
    /**
     * Detect gradient-related issues
     */
    private fun detectGradientIssues(metrics: TrainingCycleMetrics): List<DetectedIssue> {
        val issues = mutableListOf<DetectedIssue>()
        
        // Exploding gradients
        val explodingThreshold = config.issueThresholds["gradient_norm_high"] ?: 10.0
        if (metrics.gradientNorm > explodingThreshold) {
            issues.add(DetectedIssue(
                type = IssueType.EXPLODING_GRADIENTS,
                severity = calculateSeverity(metrics.gradientNorm, explodingThreshold, 20.0),
                description = "Gradient norm (${formatDecimal(metrics.gradientNorm, 3)}) exceeds threshold (${explodingThreshold})",
                timestamp = getCurrentTimeMillis(),
                cycle = metrics.cycle,
                affectedMetrics = listOf("gradientNorm", "averageLoss"),
                suggestedActions = listOf(
                    "Apply gradient clipping with max norm ${explodingThreshold / 2}",
                    "Reduce learning rate by 50%",
                    "Check network architecture for instability",
                    "Verify input data normalization"
                ),
                confidence = 0.9
            ))
        }
        
        // Vanishing gradients
        val vanishingThreshold = config.issueThresholds["gradient_norm_low"] ?: 1e-6
        if (metrics.gradientNorm < vanishingThreshold) {
            issues.add(DetectedIssue(
                type = IssueType.VANISHING_GRADIENTS,
                severity = calculateSeverity(vanishingThreshold, metrics.gradientNorm, vanishingThreshold * 10),
                description = "Gradient norm (${formatDecimal(metrics.gradientNorm, 8)}) below threshold (${vanishingThreshold})",
                timestamp = getCurrentTimeMillis(),
                cycle = metrics.cycle,
                affectedMetrics = listOf("gradientNorm", "policyEntropy"),
                suggestedActions = listOf(
                    "Increase learning rate by 2x",
                    "Use different activation functions (ReLU, LeakyReLU)",
                    "Check network initialization (Xavier, He)",
                    "Reduce network depth if too deep"
                ),
                confidence = 0.8
            ))
        }
        
        return issues
    }
    
    /**
     * Detect policy-related issues
     */
    private fun detectPolicyIssues(
        metrics: TrainingCycleMetrics,
        history: List<TrainingCycleMetrics>
    ): List<DetectedIssue> {
        val issues = mutableListOf<DetectedIssue>()
        
        // Policy collapse (low entropy)
        val entropyThreshold = config.issueThresholds["policy_entropy_low"] ?: 0.1
        if (metrics.policyEntropy < entropyThreshold) {
            issues.add(DetectedIssue(
                type = IssueType.POLICY_COLLAPSE,
                severity = calculateSeverity(entropyThreshold, metrics.policyEntropy, entropyThreshold * 2),
                description = "Policy entropy (${formatDecimal(metrics.policyEntropy, 3)}) indicates policy collapse",
                timestamp = getCurrentTimeMillis(),
                cycle = metrics.cycle,
                affectedMetrics = listOf("policyEntropy", "winRate", "moveAccuracy"),
                suggestedActions = listOf(
                    "Increase exploration rate to ${minOf(0.3, metrics.policyEntropy * 10)}",
                    "Add entropy regularization with coefficient 0.01",
                    "Use different exploration strategy (Boltzmann, UCB)",
                    "Reduce training frequency to allow more exploration"
                ),
                confidence = 0.85
            ))
        }
        
        // Entropy collapse trend
        if (history.size >= 5) {
            val recentEntropies = history.takeLast(5).map { it.policyEntropy }
            val entropyTrend = calculateTrend(recentEntropies)
            if (entropyTrend < -0.05 && metrics.policyEntropy < 0.5) {
                issues.add(DetectedIssue(
                    type = IssueType.POLICY_COLLAPSE,
                    severity = 0.7,
                    description = "Policy entropy declining rapidly (trend: ${formatDecimal(entropyTrend, 4)})",
                    timestamp = getCurrentTimeMillis(),
                    cycle = metrics.cycle,
                    affectedMetrics = listOf("policyEntropy"),
                    suggestedActions = listOf(
                        "Implement entropy bonus in reward function",
                        "Use curiosity-driven exploration",
                        "Adjust exploration decay schedule"
                    ),
                    confidence = 0.75
                ))
            }
        }
        
        return issues
    }
    
    /**
     * Detect performance-related issues
     */
    private fun detectPerformanceIssues(
        metrics: TrainingCycleMetrics,
        history: List<TrainingCycleMetrics>
    ): List<DetectedIssue> {
        val issues = mutableListOf<DetectedIssue>()
        
        // Low win rate
        val winRateThreshold = config.issueThresholds["win_rate_low"] ?: 0.2
        if (metrics.winRate < winRateThreshold && metrics.cycle > 10) {
            issues.add(DetectedIssue(
                type = IssueType.EXPLORATION_INSUFFICIENT,
                severity = calculateSeverity(winRateThreshold, metrics.winRate, winRateThreshold * 2),
                description = "Win rate (${formatPercentage(metrics.winRate)}) below expected threshold",
                timestamp = getCurrentTimeMillis(),
                cycle = metrics.cycle,
                affectedMetrics = listOf("winRate", "averageReward"),
                suggestedActions = listOf(
                    "Increase exploration rate",
                    "Adjust reward function scaling",
                    "Check opponent strength balance",
                    "Verify state representation quality"
                ),
                confidence = 0.8
            ))
        }
        
        // Performance stagnation
        if (history.size >= 10) {
            val recentWinRates = history.takeLast(10).map { it.winRate }
            val winRateVariance = calculateVariance(recentWinRates)
            val winRateTrend = calculateTrend(recentWinRates)
            
            if (winRateVariance < 0.001 && abs(winRateTrend) < 0.001) {
                issues.add(DetectedIssue(
                    type = IssueType.EXPLORATION_INSUFFICIENT,
                    severity = 0.6,
                    description = "Performance stagnation detected (variance: ${formatDecimal(winRateVariance, 6)})",
                    timestamp = getCurrentTimeMillis(),
                    cycle = metrics.cycle,
                    affectedMetrics = listOf("winRate", "averageReward"),
                    suggestedActions = listOf(
                        "Increase exploration to break stagnation",
                        "Implement curriculum learning",
                        "Add noise to training process",
                        "Consider different network architecture"
                    ),
                    confidence = 0.7
                ))
            }
        }
        
        return issues
    }
    
    /**
     * Detect training stability issues
     */
    private fun detectStabilityIssues(
        metrics: TrainingCycleMetrics,
        history: List<TrainingCycleMetrics>
    ): List<DetectedIssue> {
        val issues = mutableListOf<DetectedIssue>()
        
        // High loss
        val lossThreshold = config.issueThresholds["loss_high"] ?: 5.0
        if (metrics.averageLoss > lossThreshold) {
            issues.add(DetectedIssue(
                type = IssueType.LOSS_EXPLOSION,
                severity = calculateSeverity(metrics.averageLoss, lossThreshold, lossThreshold * 2),
                description = "Average loss (${formatDecimal(metrics.averageLoss, 4)}) unusually high",
                timestamp = getCurrentTimeMillis(),
                cycle = metrics.cycle,
                affectedMetrics = listOf("averageLoss", "lossVariance"),
                suggestedActions = listOf(
                    "Reduce learning rate by 50%",
                    "Check for numerical instability",
                    "Verify target computation correctness",
                    "Apply gradient clipping"
                ),
                confidence = 0.8
            ))
        }
        
        // Loss oscillation
        if (history.size >= 5) {
            val recentLosses = history.takeLast(5).map { it.averageLoss }
            val lossVariance = calculateVariance(recentLosses)
            val avgLoss = recentLosses.average()
            
            if (lossVariance > avgLoss * 0.5) {
                issues.add(DetectedIssue(
                    type = IssueType.NUMERICAL_INSTABILITY,
                    severity = 0.7,
                    description = "Loss oscillation detected (variance: ${formatDecimal(lossVariance, 4)})",
                    timestamp = getCurrentTimeMillis(),
                    cycle = metrics.cycle,
                    affectedMetrics = listOf("averageLoss", "lossVariance"),
                    suggestedActions = listOf(
                        "Reduce learning rate for stability",
                        "Increase batch size",
                        "Use learning rate scheduling",
                        "Apply momentum or adaptive optimizers"
                    ),
                    confidence = 0.75
                ))
            }
        }
        
        return issues
    }
    
    /**
     * Detect game quality issues
     */
    private fun detectGameQualityIssues(
        metrics: TrainingCycleMetrics
    ): List<DetectedIssue> {
        val issues = mutableListOf<DetectedIssue>()
        
        // Low game quality
        if (metrics.averageGameQuality < 0.3 && metrics.cycle > 5) {
            issues.add(DetectedIssue(
                type = IssueType.EXPLORATION_INSUFFICIENT,
                severity = 0.6,
                description = "Game quality (${formatDecimal(metrics.averageGameQuality, 3)}) below acceptable level",
                timestamp = getCurrentTimeMillis(),
                cycle = metrics.cycle,
                affectedMetrics = listOf("averageGameQuality", "moveAccuracy", "strategicDepth"),
                suggestedActions = listOf(
                    "Improve reward function design",
                    "Increase training data diversity",
                    "Add position-based rewards",
                    "Implement game quality filtering"
                ),
                confidence = 0.7
            ))
        }
        
        // Poor move accuracy
        if (metrics.moveAccuracy < 0.4 && metrics.cycle > 10) {
            issues.add(DetectedIssue(
                type = IssueType.Q_VALUE_OVERESTIMATION,
                severity = 0.5,
                description = "Move accuracy (${formatPercentage(metrics.moveAccuracy)}) indicates poor decision making",
                timestamp = getCurrentTimeMillis(),
                cycle = metrics.cycle,
                affectedMetrics = listOf("moveAccuracy", "tacticalAccuracy"),
                suggestedActions = listOf(
                    "Improve state representation",
                    "Add tactical training scenarios",
                    "Increase network capacity",
                    "Use better move evaluation metrics"
                ),
                confidence = 0.65
            ))
        }
        
        return issues
    }
    
    /**
     * Detect convergence issues
     */
    private fun detectConvergenceIssues(history: List<TrainingCycleMetrics>): List<DetectedIssue> {
        val issues = mutableListOf<DetectedIssue>()
        
        if (history.size < 20) return issues
        
        val recentMetrics = history.takeLast(20)
        val winRates = recentMetrics.map { it.winRate }
        val rewards = recentMetrics.map { it.averageReward }
        
        // Convergence stagnation
        val winRateStability = calculateStability(winRates)
        val rewardStability = calculateStability(rewards)
        
        if (winRateStability > 0.95 && rewardStability > 0.95) {
            val avgWinRate = winRates.average()
            if (avgWinRate < 0.7) {
                issues.add(DetectedIssue(
                    type = IssueType.EXPLORATION_INSUFFICIENT,
                    severity = 0.6,
                    description = "Training converged to suboptimal policy (win rate: ${formatPercentage(avgWinRate)})",
                    timestamp = getCurrentTimeMillis(),
                    cycle = history.last().cycle,
                    affectedMetrics = listOf("winRate", "averageReward"),
                    suggestedActions = listOf(
                        "Restart training with higher exploration",
                        "Use different exploration strategy",
                        "Implement population-based training",
                        "Add curriculum learning"
                    ),
                    confidence = 0.8
                ))
            }
        }
        
        return issues
    }
    
    /**
     * Detect predictive issues using trend analysis
     */
    private fun detectPredictiveIssues(
        metrics: TrainingCycleMetrics,
        history: List<TrainingCycleMetrics>
    ): List<DetectedIssue> {
        val issues = mutableListOf<DetectedIssue>()
        
        if (history.size < 10) return issues
        
        // Predict potential gradient explosion
        val recentGradients = history.takeLast(5).map { it.gradientNorm }
        val gradientTrend = calculateTrend(recentGradients)
        
        if (gradientTrend > 0.5 && metrics.gradientNorm > 3.0) {
            issues.add(DetectedIssue(
                type = IssueType.EXPLODING_GRADIENTS,
                severity = 0.4,
                description = "Gradient norm trending upward (trend: ${formatDecimal(gradientTrend, 3)}), potential explosion risk",
                timestamp = getCurrentTimeMillis(),
                cycle = metrics.cycle,
                affectedMetrics = listOf("gradientNorm"),
                suggestedActions = listOf(
                    "Preemptively apply gradient clipping",
                    "Monitor gradient norm closely",
                    "Consider reducing learning rate"
                ),
                confidence = 0.6
            ))
        }
        
        // Predict policy collapse
        val recentEntropies = history.takeLast(5).map { it.policyEntropy }
        val entropyTrend = calculateTrend(recentEntropies)
        
        if (entropyTrend < -0.02 && metrics.policyEntropy < 0.3) {
            issues.add(DetectedIssue(
                type = IssueType.POLICY_COLLAPSE,
                severity = 0.3,
                description = "Policy entropy declining rapidly, collapse risk in ${estimateCyclesToCollapse(entropyTrend, metrics.policyEntropy)} cycles",
                timestamp = getCurrentTimeMillis(),
                cycle = metrics.cycle,
                affectedMetrics = listOf("policyEntropy"),
                suggestedActions = listOf(
                    "Increase exploration rate now",
                    "Add entropy regularization",
                    "Monitor entropy closely"
                ),
                confidence = 0.5
            ))
        }
        
        return issues
    }
    
    /**
     * Perform comprehensive diagnosis of training issues
     */
    fun performComprehensiveDiagnosis(
        trainingHistory: List<TrainingCycleMetrics>,
        issueHistory: List<DetectedIssue>
    ): ComprehensiveDiagnosis {
        
        val diagnosis = ComprehensiveDiagnosis(
            overallHealth = assessOverallHealth(issueHistory),
            criticalIssues = identifyCriticalIssues(issueHistory),
            recurringIssues = identifyRecurringIssues(issueHistory),
            rootCauseAnalysis = performRootCauseAnalysis(issueHistory),
            recommendedActions = generateDiagnosisRecommendations(issueHistory),
            prognosis = generatePrognosis(trainingHistory)
        )
        
        return diagnosis
    }
    
    /**
     * Finalize issue detection
     */
    fun finalize() {
        println("🔍 Issue detection finalized. Total detections: ${detectionHistory.size}")
    }
    
    // Private helper methods
    
    private fun initializeIssuePatterns() {
        // Initialize patterns for different issue types
        issuePatterns[IssueType.EXPLODING_GRADIENTS] = IssuePattern(
            frequency = 0.0,
            severity = 0.0,
            lastOccurrence = 0L
        )
        // Add more patterns as needed
    }
    
    private fun updateIssuePatterns(issues: List<DetectedIssue>) {
        for (issue in issues) {
            val pattern = issuePatterns[issue.type] ?: IssuePattern(0.0, 0.0, 0L)
            issuePatterns[issue.type] = pattern.copy(
                frequency = pattern.frequency + 1,
                severity = maxOf(pattern.severity, issue.severity),
                lastOccurrence = issue.timestamp
            )
        }
    }
    
    private fun calculateSeverity(value: Double, threshold: Double, maxValue: Double): Double {
        val ratio = abs(value - threshold) / abs(maxValue - threshold)
        return ratio.coerceIn(0.0, 1.0)
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
    
    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
    
    private fun calculateStability(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        val variance = calculateVariance(values)
        val stdDev = sqrt(variance)
        
        return if (abs(mean) > 1e-8) {
            1.0 / (1.0 + stdDev / abs(mean))
        } else {
            if (stdDev < 1e-8) 1.0 else 0.0
        }
    }
    
    private fun estimateCyclesToCollapse(trend: Double, currentValue: Double): Int {
        if (trend >= 0) return Int.MAX_VALUE
        val collapseThreshold = 0.05
        val cyclesToCollapse = (currentValue - collapseThreshold) / abs(trend)
        return cyclesToCollapse.toInt().coerceIn(1, 100)
    }
    
    private fun assessOverallHealth(
        issueHistory: List<DetectedIssue>
    ): HealthAssessment {
        val recentIssues = issueHistory.takeLast(10)
        val criticalIssues = recentIssues.count { it.severity > 0.8 }
        val totalIssues = recentIssues.size
        
        val healthScore = when {
            criticalIssues > 0 -> 0.3
            totalIssues > 5 -> 0.6
            totalIssues > 2 -> 0.8
            else -> 1.0
        }
        
        return HealthAssessment(
            score = healthScore,
            status = when {
                healthScore >= 0.8 -> "HEALTHY"
                healthScore >= 0.6 -> "WARNING"
                else -> "CRITICAL"
            },
            summary = "Training health score: ${formatDecimal(healthScore, 2)}"
        )
    }
    
    private fun identifyCriticalIssues(issueHistory: List<DetectedIssue>): List<DetectedIssue> {
        return issueHistory.filter { it.severity > 0.8 }.takeLast(5)
    }
    
    private fun identifyRecurringIssues(issueHistory: List<DetectedIssue>): List<RecurringIssue> {
        val issueGroups = issueHistory.groupBy { it.type }
        return issueGroups.mapNotNull { (type, issues) ->
            if (issues.size >= 3) {
                RecurringIssue(
                    type = type,
                    occurrences = issues.size,
                    averageSeverity = issues.map { it.severity }.average(),
                    firstOccurrence = issues.minByOrNull { it.timestamp }?.timestamp ?: 0L,
                    lastOccurrence = issues.maxByOrNull { it.timestamp }?.timestamp ?: 0L
                )
            } else null
        }
    }
    
    private fun performRootCauseAnalysis(
        issueHistory: List<DetectedIssue>
    ): RootCauseAnalysis {
        // Simplified root cause analysis
        val commonCauses = mutableListOf<String>()
        
        if (issueHistory.any { it.type == IssueType.EXPLODING_GRADIENTS }) {
            commonCauses.add("Learning rate may be too high")
        }
        
        if (issueHistory.any { it.type == IssueType.POLICY_COLLAPSE }) {
            commonCauses.add("Insufficient exploration")
        }
        
        return RootCauseAnalysis(
            primaryCauses = commonCauses.take(3),
            contributingFactors = listOf("Network architecture", "Hyperparameter settings"),
            confidence = 0.7
        )
    }
    
    private fun generateDiagnosisRecommendations(
        issueHistory: List<DetectedIssue>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        val recentIssues = issueHistory.takeLast(10)
        val issueTypes = recentIssues.map { it.type }.toSet()
        
        if (IssueType.EXPLODING_GRADIENTS in issueTypes) {
            recommendations.add("Implement gradient clipping immediately")
        }
        
        if (IssueType.POLICY_COLLAPSE in issueTypes) {
            recommendations.add("Increase exploration rate and add entropy regularization")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Continue current training approach")
        }
        
        return recommendations
    }
    
    private fun generatePrognosis(
        trainingHistory: List<TrainingCycleMetrics>
    ): TrainingPrognosis {
        val recentPerformance = trainingHistory.takeLast(10).map { it.winRate }
        val performanceTrend = if (recentPerformance.size >= 2) calculateTrend(recentPerformance) else 0.0
        
        val outlook = when {
            performanceTrend > 0.01 -> "POSITIVE"
            performanceTrend < -0.01 -> "NEGATIVE"
            else -> "STABLE"
        }
        
        return TrainingPrognosis(
            outlook = outlook,
            expectedImprovement = performanceTrend,
            estimatedCyclesToTarget = if (performanceTrend > 0) ((0.8 - recentPerformance.average()) / performanceTrend).toInt() else null,
            confidence = 0.6
        )
    }
    
    // Utility methods
    
    private fun formatDecimal(value: Double, places: Int): String {
        return "%.${places}f".format(value)
    }
    
    private fun formatPercentage(value: Double): String {
        return "${(value * 100).toInt()}%"
    }
}

// Supporting classes

class TrendAnalyzer {
    // Implementation for trend analysis
}

class AnomalyDetector {
    // Implementation for anomaly detection
}

// Data classes

data class IssueDetectionResult(
    val cycle: Int,
    val timestamp: Long,
    val detectedIssues: List<DetectedIssue>,
    val metricsAnalyzed: TrainingCycleMetrics
)

data class IssuePattern(
    val frequency: Double,
    val severity: Double,
    val lastOccurrence: Long
)

data class ComprehensiveDiagnosis(
    val overallHealth: HealthAssessment,
    val criticalIssues: List<DetectedIssue>,
    val recurringIssues: List<RecurringIssue>,
    val rootCauseAnalysis: RootCauseAnalysis,
    val recommendedActions: List<String>,
    val prognosis: TrainingPrognosis
)

data class HealthAssessment(
    val score: Double,
    val status: String,
    val summary: String
)

data class RecurringIssue(
    val type: IssueType,
    val occurrences: Int,
    val averageSeverity: Double,
    val firstOccurrence: Long,
    val lastOccurrence: Long
)

data class RootCauseAnalysis(
    val primaryCauses: List<String>,
    val contributingFactors: List<String>,
    val confidence: Double
)

data class TrainingPrognosis(
    val outlook: String,
    val expectedImprovement: Double,
    val estimatedCyclesToTarget: Int?,
    val confidence: Double
)
