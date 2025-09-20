package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Test-compatibility helpers and lightweight shims for integration validation tests.
 * These provide a minimal API surface expected by older tests without reintroducing
 * heavy debugging infrastructure.
 */

// ------------------------------
// Chess validation (lightweight)
// ------------------------------

data class ChessGameResult(
    val outcome: String,
    val gameLength: Int,
    val moves: List<String>,
    val qualityScore: Double
)

enum class ChessIssueType {
    GAMES_TOO_SHORT,
    GAMES_TOO_LONG,
    LOW_MOVE_DIVERSITY
}

data class ChessIssue(
    val type: ChessIssueType,
    val message: String
)

data class ChessValidationResult(
    val issues: List<ChessIssue>,
    val warnings: List<String> = emptyList()
)

class ChessTrainingValidator {
    fun validate(games: List<ChessGameResult>): ChessValidationResult {
        if (games.isEmpty()) return ChessValidationResult(emptyList())

        val issues = mutableListOf<ChessIssue>()

        val avgLen = games.map { it.gameLength }.average()
        if (avgLen < 10.0) issues.add(ChessIssue(ChessIssueType.GAMES_TOO_SHORT, "Average game too short: $avgLen"))
        if (avgLen > 150.0) issues.add(ChessIssue(ChessIssueType.GAMES_TOO_LONG, "Average game too long: $avgLen"))

        val allMoves = games.flatMap { it.moves }
        if (allMoves.isNotEmpty()) {
            val diversity = allMoves.toSet().size.toDouble() / allMoves.size.toDouble()
            if (diversity < 0.3) issues.add(ChessIssue(ChessIssueType.LOW_MOVE_DIVERSITY, "Low move diversity: $diversity"))
        }

        return ChessValidationResult(issues)
    }

    // Backward-compat alias sometimes used in older tests
    fun validateGames(games: List<ChessGameResult>): ChessValidationResult = validate(games)
}

// ------------------------------
// Training debugger (minimal)
// ------------------------------

data class DebuggerConfig(
    val significantLossChangeThreshold: Double = 2.0,
    val significantEntropyChangeThreshold: Double = 0.3,
    val minEpisodeLengthThreshold: Int = 10,
    val maxEpisodeLengthThreshold: Int = 200
)

enum class LossDirection { INCREASING, DECREASING, STABLE }
enum class LossStability { STABLE, VOLATILE, UNSTABLE }
enum class GradientMagnitude { VANISHING, LOW, NORMAL, HIGH, EXPLODING }
enum class GradientHealth { HEALTHY, PROBLEMATIC }
enum class PolicyShift { MINIMAL, SIGNIFICANT }
enum class ExperienceQuality { GOOD, POOR }
enum class QValueHealth { NORMAL, OVERESTIMATED, UNDERESTIMATED }
enum class ConvergenceIssue {
    INSUFFICIENT_DATA,
    REWARD_STAGNATION,
    POLICY_COLLAPSE,
    TRAINING_INSTABILITY,
    INSUFFICIENT_EXPLORATION,
    NORMAL_CONVERGENCE
}

data class LossAnalysis(
    val lossDirection: LossDirection,
    val lossStability: LossStability,
    val lossChange: Double
)

data class GradientAnalysis(
    val gradientMagnitude: GradientMagnitude,
    val gradientHealth: GradientHealth
)

data class PolicyAnalysis(
    val policyShift: PolicyShift
)

data class ExperienceAnalysis(
    val quality: ExperienceQuality
)

data class QValueAnalysis(
    val qValueHealth: QValueHealth,
    val qTargetDivergence: Double,
    val overestimationBias: Double
)

data class PolicyUpdateAnalysis(
    val lossAnalysis: LossAnalysis,
    val gradientAnalysis: GradientAnalysis,
    val policyAnalysis: PolicyAnalysis,
    val experienceAnalysis: ExperienceAnalysis,
    val qValueAnalysis: QValueAnalysis? = null,
    val insights: List<String> = emptyList()
)

data class ConvergenceDebugAnalysis(
    val issue: ConvergenceIssue,
    val analysis: String,
    val recommendations: List<String>,
    val diagnostics: Map<String, Double> = emptyMap()
)

data class EpisodeDebugAnalysis(
    val episodeNumber: Int,
    val episodeLength: Int,
    val qualityIssues: List<String>,
    val recommendations: List<String>,
    val rewardDistribution: RewardDistribution,
    val stateActionAnalysis: StateActionAnalysis
)

data class RewardDistribution(
    val mean: Double,
    val sparsity: Double,
    val range: Double
)

data class StateActionAnalysis(
    val uniqueActions: Int,
    val actionDiversity: Double,
    val mostFrequentAction: Int?,
    val actionDistribution: Map<Int, Int>
)

enum class TrainingHealth { EXCELLENT, GOOD, FAIR, POOR }
enum class StabilityLevel { HIGH, MODERATE, LOW, UNKNOWN }

data class StabilityAnalysis(
    val stabilityLevel: StabilityLevel,
    val rewardStability: Double,
    val lossStability: Double,
    val entropyStability: Double
)

data class TrainingDiagnosticReport(
    val overallHealth: TrainingHealth,
    val commonIssues: List<String>,
    val performanceMetrics: Map<String, Double>,
    val stabilityAnalysis: StabilityAnalysis,
    val recommendations: List<String>
)

class TrainingDebugger(private val config: DebuggerConfig) {
    fun clearHistory() { /* no-op shim */ }

    fun analyzePolicyUpdate(
        updateResult: PolicyUpdateResult,
        before: RLMetrics,
        after: RLMetrics,
        experiences: List<Experience<DoubleArray, Int>>,
        episode: Int
    ): PolicyUpdateAnalysis {
        val lossDir = when {
            updateResult.loss.isNaN() -> LossDirection.INCREASING
            updateResult.loss > before.policyLoss -> LossDirection.INCREASING
            updateResult.loss < before.policyLoss -> LossDirection.DECREASING
            else -> LossDirection.STABLE
        }
        val lossChange = if (updateResult.loss.isNaN() || before.policyLoss.isNaN()) Double.NaN else updateResult.loss - before.policyLoss
        val lossStability = when {
            updateResult.loss.isNaN() -> LossStability.UNSTABLE
            abs(lossChange) > config.significantLossChangeThreshold -> LossStability.VOLATILE
            else -> LossStability.STABLE
        }

        val gradMag = when {
            updateResult.gradientNorm < 1e-6 -> GradientMagnitude.VANISHING
            updateResult.gradientNorm < 0.1 -> GradientMagnitude.LOW
            updateResult.gradientNorm < 5.0 -> GradientMagnitude.NORMAL
            updateResult.gradientNorm < 12.0 -> GradientMagnitude.HIGH
            else -> GradientMagnitude.EXPLODING
        }
        val gradHealth = if (gradMag in listOf(GradientMagnitude.EXPLODING, GradientMagnitude.VANISHING)) GradientHealth.PROBLEMATIC else GradientHealth.HEALTHY

        val policyShift = if (abs(after.policyEntropy - before.policyEntropy) > config.significantEntropyChangeThreshold) PolicyShift.SIGNIFICANT else PolicyShift.MINIMAL

        val nonZeroRewards = experiences.count { it.reward != 0.0 }
        val quality = if (nonZeroRewards.toDouble() / max(1, experiences.size) < 0.2) ExperienceQuality.POOR else ExperienceQuality.GOOD

        val qAnalysis = if (updateResult.qValueMean != null && updateResult.targetValueMean != null) {
            val div = updateResult.qValueMean - updateResult.targetValueMean
            val health = when {
                div > 50.0 -> QValueHealth.OVERESTIMATED
                div < -50.0 -> QValueHealth.UNDERESTIMATED
                else -> QValueHealth.NORMAL
            }
            QValueAnalysis(health, qTargetDivergence = abs(div), overestimationBias = max(0.0, div))
        } else null

        val insights = mutableListOf<String>()
        if (lossStability == LossStability.STABLE) insights.add("Loss appears stable")
        if (gradHealth == GradientHealth.PROBLEMATIC) insights.add("Gradients need immediate attention")
        if (policyShift == PolicyShift.MINIMAL) insights.add("Policy change minimal")
        if (qAnalysis?.qValueHealth == QValueHealth.OVERESTIMATED) insights.add("Q-values appear overestimated")

        return PolicyUpdateAnalysis(
            lossAnalysis = LossAnalysis(lossDir, lossStability, lossChange),
            gradientAnalysis = GradientAnalysis(gradMag, gradHealth),
            policyAnalysis = PolicyAnalysis(policyShift),
            experienceAnalysis = ExperienceAnalysis(quality),
            qValueAnalysis = qAnalysis,
            insights = insights
        )
    }

    fun debugConvergenceIssues(history: List<RLMetrics>, windowSize: Int): ConvergenceDebugAnalysis {
        if (history.size < windowSize) {
            return ConvergenceDebugAnalysis(
                issue = ConvergenceIssue.INSUFFICIENT_DATA,
                analysis = "Need at least $windowSize points for analysis",
                recommendations = listOf("Continue training to collect more data")
            )
        }

        val recent = history.takeLast(windowSize)
        val rewards = recent.map { it.averageReward }
        val entropy = recent.map { it.policyEntropy }
        val loss = recent.map { it.policyLoss }

        fun variance(xs: List<Double>): Double {
            val m = xs.average()
            return xs.map { (it - m) * (it - m) }.average()
        }

        val rewardVar = variance(rewards)
        val lossVar = variance(loss)
        val meanEntropy = entropy.average()

        return when {
            rewards.all { abs(it - rewards.first()) < 1e-6 } -> ConvergenceDebugAnalysis(
                ConvergenceIssue.REWARD_STAGNATION,
                analysis = "Rewards appear stagnated",
                recommendations = listOf("Review reward function", "Increase exploration"),
                diagnostics = mapOf("reward_variance" to rewardVar)
            )
            meanEntropy < 0.1 -> ConvergenceDebugAnalysis(
                ConvergenceIssue.POLICY_COLLAPSE,
                analysis = "Policy entropy indicates over-exploitation",
                recommendations = listOf("Increase exploration rate")
            )
            lossVar > 1.0 -> ConvergenceDebugAnalysis(
                ConvergenceIssue.TRAINING_INSTABILITY,
                analysis = "Loss shows significant instability",
                recommendations = listOf("Reduce learning rate")
            )
            recent.first().explorationRate > 0.3 && recent.last().explorationRate < 0.05 && rewards.last() < rewards.first() -> ConvergenceDebugAnalysis(
                ConvergenceIssue.INSUFFICIENT_EXPLORATION,
                analysis = "Exploration decayed too quickly",
                recommendations = listOf("Tune exploration decay schedule")
            )
            else -> ConvergenceDebugAnalysis(
                ConvergenceIssue.NORMAL_CONVERGENCE,
                analysis = "Training appears to be converging normally",
                recommendations = listOf("Keep current settings")
            )
        }
    }

    fun debugEpisode(
        episodeMetrics: RLMetrics,
        experiences: List<Experience<DoubleArray, Int>>,
        updateResult: PolicyUpdateResult?
    ): EpisodeDebugAnalysis {
        val length = experiences.size
        val rewards = experiences.map { it.reward }
        val mean = if (rewards.isNotEmpty()) rewards.average() else 0.0
        val nonZero = rewards.count { it != 0.0 }
        val sparsity = 1.0 - (nonZero.toDouble() / max(1, rewards.size))
        val range = if (rewards.isEmpty()) 0.0 else (rewards.maxOrNull()!! - rewards.minOrNull()!!)

        val issues = mutableListOf<String>()
        val recs = mutableListOf<String>()
        if (length < config.minEpisodeLengthThreshold) issues.add("Episode too short")
        if (length > config.maxEpisodeLengthThreshold) issues.add("Episode too long")
        if (sparsity > 0.9) issues.add("Rewards very sparse")
        if (issues.any { it.contains("short") }) recs.add("Avoid terminating early")
        if (issues.any { it.contains("long") }) recs.add("Consider time limits")
        if (issues.any { it.contains("sparse") }) recs.add("Consider reward shaping")

        val actionCounts = experiences.groupingBy { it.action }.eachCount()
        val uniqueActions = actionCounts.keys.size
        val actionDiversity = uniqueActions.toDouble() / max(1, length)
        val mostCommon = actionCounts.maxByOrNull { it.value }?.key

        return EpisodeDebugAnalysis(
            episodeNumber = episodeMetrics.episode,
            episodeLength = length,
            qualityIssues = issues,
            recommendations = recs,
            rewardDistribution = RewardDistribution(mean, sparsity, range),
            stateActionAnalysis = StateActionAnalysis(uniqueActions, actionDiversity, mostCommon, actionCounts)
        )
    }

    fun generateDiagnosticReport(
        trainingHistory: List<RLMetrics>,
        recentExperiences: List<Experience<DoubleArray, Int>>
    ): TrainingDiagnosticReport {
        val rewards = trainingHistory.map { it.averageReward }
        val trend = if (rewards.size >= 2) rewards.last() - rewards.first() else 0.0
        val entropy = trainingHistory.map { it.policyEntropy }
        fun stability(xs: List<Double>): Double {
            if (xs.size < 2) return 0.0
            val m = xs.average()
            val varc = xs.map { (it - m) * (it - m) }.average()
            val std = kotlin.math.sqrt(varc)
            return if (abs(m) < 1e-8) if (std < 1e-8) 1.0 else 0.0 else 1.0 / (1.0 + std / abs(m))
        }
        val stability = StabilityAnalysis(
            stabilityLevel = when {
                stability(rewards) > 0.7 -> StabilityLevel.HIGH
                stability(rewards) > 0.4 -> StabilityLevel.MODERATE
                stability(rewards) > 0.1 -> StabilityLevel.LOW
                else -> StabilityLevel.UNKNOWN
            },
            rewardStability = stability(rewards),
            lossStability = stability(trainingHistory.map { it.policyLoss }),
            entropyStability = stability(entropy)
        )
        val health = when {
            trend > 0.3 && stability.stabilityLevel == StabilityLevel.HIGH -> TrainingHealth.EXCELLENT
            trend >= 0.0 -> TrainingHealth.GOOD
            trend > -0.2 -> TrainingHealth.FAIR
            else -> TrainingHealth.POOR
        }
        val commonIssues = mutableListOf<String>()
        if (trainingHistory.any { it.gradientNorm > 10.0 }) commonIssues.add("Exploding gradients detected")
        if (trainingHistory.any { it.policyEntropy < 0.1 }) commonIssues.add("Policy collapse risk")

        val perf = mapOf(
            "average_reward" to (if (rewards.isEmpty()) 0.0 else rewards.average()),
            "reward_trend" to trend
        )

        val recs = mutableListOf<String>()
        if (commonIssues.isNotEmpty()) recs.add("Address detected issues")
        if (health == TrainingHealth.FAIR || health == TrainingHealth.POOR) recs.add("Tune learning rate or exploration")

        return TrainingDiagnosticReport(
            overallHealth = health,
            commonIssues = commonIssues,
            performanceMetrics = perf,
            stabilityAnalysis = stability,
            recommendations = recs
        )
    }
}
