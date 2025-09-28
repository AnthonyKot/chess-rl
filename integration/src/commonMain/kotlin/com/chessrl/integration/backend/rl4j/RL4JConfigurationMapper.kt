package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.ChessAgentConfig
import com.chessrl.integration.backend.BackendConfig
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning
import org.deeplearning4j.rl4j.network.configuration.DQNDenseNetworkConfiguration
import org.nd4j.linalg.learning.config.Adam

/**
 * Maps our ChessAgentConfig and BackendConfig into RL4J configuration objects.
 * Replaces the previous reflection-based configuration creation so we can
 * instantiate RL4J components through their public builders.
 */
@Suppress("DEPRECATION") // TODO: switch to RL4J's new configuration builders when upgrading RL4J
class RL4JConfigurationMapper(
    private val agentConfig: ChessAgentConfig,
    private val backendConfig: BackendConfig?
) {

    data class MappingResult(
        val qLearningConfig: QLearning.QLConfiguration,
        val networkConfig: DQNDenseNetworkConfiguration,
        val warnings: List<String>
    )

    private val warnings = mutableListOf<String>()

    fun build(): MappingResult {
        val qConfig = buildQLearningConfiguration()
        val networkConfig = buildNetworkConfiguration()
        return MappingResult(qConfig, networkConfig, warnings.toList())
    }

    private fun buildQLearningConfiguration(): QLearning.QLConfiguration {
        val batchSize = agentConfig.batchSize
        require(batchSize > 0) { "batchSize must be > 0 (was $batchSize)" }

        val bufferSize = agentConfig.maxBufferSize
        require(bufferSize >= batchSize) {
            "maxBufferSize ($bufferSize) must be >= batchSize ($batchSize) for RL4J"
        }

        val targetUpdate = agentConfig.targetUpdateFrequency
        require(targetUpdate > 0) {
            "targetUpdateFrequency must be > 0 (was $targetUpdate)"
        }

        val gamma = agentConfig.gamma
        require(gamma in GAMMA_RANGE) { "gamma must be in ${GAMMA_RANGE.start}..${GAMMA_RANGE.endInclusive} (was $gamma)" }

        val explorationRate = agentConfig.explorationRate
        require(explorationRate in 0.0..1.0) { "explorationRate must be between 0 and 1 (was $explorationRate)" }

        if (batchSize > DEFAULT_MAX_BATCH_WARNING_THRESHOLD) {
            warnings += "Batch size $batchSize is large; RL4J may stall with limited memory"
        }

        val minEpsilon = explorationRate.coerceAtLeast(DEFAULT_MIN_EPSILON)
        if (minEpsilon != explorationRate) {
            warnings += "Using explorationRate=$explorationRate as epsilon start; min epsilon clamped to $minEpsilon"
        }

        return QLearning.QLConfiguration.builder()
            .seed(DEFAULT_SEED)
            .maxEpochStep(DEFAULT_MAX_EPOCH_STEP)
            .maxStep(DEFAULT_MAX_STEP)
            .expRepMaxSize(bufferSize)
            .batchSize(batchSize)
            .targetDqnUpdateFreq(agentConfig.targetUpdateFrequency)
            .updateStart(DEFAULT_WARMUP_STEPS)
            .rewardFactor(1.0)
            .gamma(gamma)
            .errorClamp(DEFAULT_ERROR_CLAMP)
            .minEpsilon(minEpsilon.toFloat())
            .epsilonNbStep(DEFAULT_EPSILON_DECAY_STEPS)
            .doubleDQN(false)
            .build()
    }

    private fun buildNetworkConfiguration(): DQNDenseNetworkConfiguration {
        val configuredHiddenLayers = backendConfig?.hiddenLayers?.takeIf { it.isNotEmpty() } ?: DEFAULT_HIDDEN_LAYERS
        val layerCount = configuredHiddenLayers.size.coerceAtLeast(1).coerceAtMost(MAX_SUPPORTED_LAYERS)
        val hiddenNodesPerLayer = configuredHiddenLayers.first()

        if (configuredHiddenLayers.size > MAX_SUPPORTED_LAYERS) {
            warnings += "RL4J dense factory currently supports at most $MAX_SUPPORTED_LAYERS layers efficiently; trimming to $layerCount"
        }

        val l2 = backendConfig?.l2Regularization ?: DEFAULT_L2
        val learningRate = backendConfig?.learningRate ?: agentConfig.learningRate

        return DQNDenseNetworkConfiguration.builder()
            .l2(l2)
            .updater(Adam(learningRate))
            .numHiddenNodes(hiddenNodesPerLayer)
            .numLayers(layerCount)
            .build()
    }

    private companion object {
        const val DEFAULT_SEED: Int = 12345
        const val DEFAULT_MAX_EPOCH_STEP: Int = 600
        const val DEFAULT_MAX_STEP: Int = 200_000
        const val DEFAULT_WARMUP_STEPS: Int = 1000
        const val DEFAULT_ERROR_CLAMP: Double = 1.0
        const val DEFAULT_EPSILON_DECAY_STEPS: Int = 50_000
        const val DEFAULT_MIN_EPSILON: Double = 0.01
        const val DEFAULT_L2: Double = 1e-4
        val DEFAULT_HIDDEN_LAYERS = listOf(512, 256)
        const val MAX_SUPPORTED_LAYERS: Int = 3
        const val DEFAULT_MAX_BATCH_WARNING_THRESHOLD: Int = 256
        val GAMMA_RANGE = 0.0..0.99999
    }
}
