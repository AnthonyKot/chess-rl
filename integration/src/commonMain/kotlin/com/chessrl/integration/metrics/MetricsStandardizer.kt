package com.chessrl.integration.metrics

import com.chessrl.integration.getCurrentTimeMillis
import com.chessrl.integration.ChessAgentMetrics
import com.chessrl.integration.TrainingEpisodeMetrics
import com.chessrl.rl.PolicyUpdateResult
import com.chessrl.rl.RLMetrics

data class MetricRecord(
    val timestamp: Long = getCurrentTimeMillis(),
    val category: String,
    val fields: Map<String, Any>,
    val tags: Map<String, String> = emptyMap()
)

object MetricsStandardizer {
    // Training episode metrics -> standardized keys
    fun fromEpisodeMetrics(metrics: TrainingEpisodeMetrics, extraTags: Map<String, String> = emptyMap()): MetricRecord {
        val fields = linkedMapOf<String, Any>(
            "pipeline.episode" to metrics.episode,
            "pipeline.reward" to metrics.reward,
            "pipeline.steps" to metrics.steps,
            "pipeline.duration_ms" to metrics.duration,
            "agent.avg_reward" to metrics.averageReward,
            "agent.exploration_rate" to metrics.agentMetrics.explorationRate,
            "agent.buffer.size" to metrics.agentMetrics.experienceBufferSize
        )
        return MetricRecord(category = "pipeline.episode", fields = fields, tags = extraTags)
    }

    // Policy update result -> standardized keys
    fun fromPolicyUpdate(result: PolicyUpdateResult, extraTags: Map<String, String> = emptyMap()): MetricRecord {
        val fields = linkedMapOf<String, Any>(
            "rl.loss" to result.loss,
            "rl.gradient_norm" to result.gradientNorm,
            "rl.policy_entropy" to result.policyEntropy
        )
        result.qValueMean?.let { fields["dqn.q.mean"] = it }
        result.targetValueMean?.let { fields["dqn.target.mean"] = it }
        result.valueError?.let { fields["value.loss"] = it }
        return MetricRecord(category = "rl.update", fields = fields, tags = extraTags)
    }

    // RL training metrics snapshot -> standardized keys
    fun fromRLMetrics(metrics: RLMetrics, extraTags: Map<String, String> = emptyMap()): MetricRecord {
        val fields = linkedMapOf<String, Any>(
            "rl.episode" to metrics.episode,
            "rl.avg_reward" to metrics.averageReward,
            "rl.episode_length" to metrics.episodeLength,
            "rl.exploration_rate" to metrics.explorationRate,
            "rl.policy_loss" to metrics.policyLoss,
            "rl.policy_entropy" to metrics.policyEntropy,
            "rl.gradient_norm" to metrics.gradientNorm
        )
        metrics.qValueStats?.let {
            fields["dqn.q.mean"] = it.meanQValue
            fields["dqn.q.max"] = it.maxQValue
            fields["dqn.q.min"] = it.minQValue
            fields["dqn.q.std"] = it.qValueStd
        }
        return MetricRecord(category = "rl.metrics", fields = fields, tags = extraTags)
    }
}

