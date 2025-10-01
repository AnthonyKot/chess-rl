package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.ChessAgent
import com.chessrl.integration.ChessAgentConfig
import com.chessrl.integration.ChessAgentMetrics
import com.chessrl.integration.ChessEnvironment
import com.chessrl.integration.backend.BackendConfig
import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.integration.logging.ComponentLogger
import com.chessrl.rl.Experience
import com.chessrl.rl.PolicyUpdateResult
import com.chessrl.integration.config.ChessRLConfig
import org.deeplearning4j.rl4j.learning.IEpochTrainer
import org.deeplearning4j.rl4j.learning.ILearning
import org.deeplearning4j.rl4j.learning.listener.TrainingListener
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense
import org.deeplearning4j.rl4j.network.dqn.IDQN
import org.deeplearning4j.rl4j.policy.DQNPolicy
import org.deeplearning4j.rl4j.util.IDataManager
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * RL4J-based chess agent that delegates all learning to RL4J's native trainers
 * and replay buffers. The manual experience pipeline is bypassed entirely –
 * training is executed through `QLearningDiscreteDense.train()`.
 */
@Suppress("DEPRECATION") // TODO: migrate to RL4J's newer agent/builder APIs once upgraded
class RL4JChessAgent(
    private val config: ChessAgentConfig,
    private val backendConfig: BackendConfig? = null,
    private val trainingConfig: ChessRLConfig? = null
) : ChessAgent {

    private val logger = ChessRLLogger.forComponent("RL4JChessAgent")

    private var mdp: ChessMDP? = null
    private var trainer: QLearningDiscreteDense<ChessObservation>? = null
    private var policy: DQNPolicy<ChessObservation>? = null
    private var neuralNet: IDQN<*>? = null
    private var trainingListener: RL4JTrainingMetricsListener? = null

    private var currentExplorationRate: Double = config.explorationRate
    private var trainingCompleted = false

    private var lastUpdate: PolicyUpdateResult = PolicyUpdateResult(
        loss = 0.0,
        gradientNorm = 0.0,
        policyEntropy = currentExplorationRate,
        qValueMean = 0.0
    )

    private var metrics: ChessAgentMetrics = ChessAgentMetrics(
        averageReward = 0.0,
        explorationRate = currentExplorationRate,
        experienceBufferSize = 0,
        episodeCount = 0,
        totalReward = 0.0,
        averageLoss = 0.0,
        policyEntropy = currentExplorationRate
    )

    init {
        RL4JAvailability.validateAvailability()
    }

    private fun ensureInitialized() {
        if (trainer != null) {
            return
        }

        RL4JAvailability.validateAvailability()

        val mapper = RL4JConfigurationMapper(config, backendConfig, trainingConfig)
        val mapping = mapper.build()
        mapping.warnings.forEach { warning ->
            logger.warn("RL4J configuration warning: $warning")
        }

        currentExplorationRate = mapping.startEpsilon

        logger.info(
            "Initializing RL4J backend version summary: rl4j=${rl4jVersion()}, dl4j=${dl4jVersion()}, nd4j=${nd4jVersion()}" +
                ", hyperparameters={batchSize=${config.batchSize}, buffer=${config.maxBufferSize}, lr=${config.learningRate}," +
                " gamma=${config.gamma}, epsilon_start=${"%.3f".format(mapping.startEpsilon)}, epsilon_floor=${config.explorationRate}, targetUpdate=${config.targetUpdateFrequency}}"
        )

        val chessMdp = ChessMDP(ChessEnvironment())
        mdp = chessMdp

        val factory = DQNFactoryStdDense(mapping.networkConfig)
        val network: IDQN<*> = factory.buildDQN(
            intArrayOf(ChessObservation.EXPECTED_DIMENSIONS),
            ChessActionSpace.ACTION_COUNT
        )
        neuralNet = network

        val qLearning = QLearningDiscreteDense(chessMdp, network, mapping.qLearningConfig)
        trainer = qLearning
        val listener = RL4JTrainingMetricsListener(
            agentLogger = logger,
            maxStepTarget = mapping.qLearningConfig.maxStep,
            maxEpochStep = mapping.qLearningConfig.maxEpochStep,
            epsilonProvider = { qLearning.egPolicy?.epsilon?.toDouble() ?: currentExplorationRate },
            initialEpsilon = mapping.startEpsilon,
            minEpsilon = mapping.qLearningConfig.minEpsilon.toDouble()
        )
        qLearning.addListener(listener)
        trainingListener = listener
        qLearning.egPolicy?.let { policy ->
            val applied = setEpsilonViaMethod(policy, mapping.startEpsilon) ||
                setEpsilonViaField(policy, mapping.startEpsilon)
            if (!applied) {
                logger.debug("Unable to set initial epsilon via known hooks; RL4J default will be used")
            }
        }
        @Suppress("UNCHECKED_CAST")
        policy = qLearning.policy as DQNPolicy<ChessObservation>

        trainingCompleted = false
        lastUpdate = lastUpdate.copy(policyEntropy = currentExplorationRate, qValueMean = 0.0)
        metrics = metrics.copy(
            explorationRate = currentExplorationRate,
            policyEntropy = currentExplorationRate,
            experienceBufferSize = mapping.qLearningConfig.expRepMaxSize,
            averageLoss = 0.0,
            totalReward = 0.0,
            averageReward = 0.0,
            episodeCount = 0
        )

        logger.info(
            "Initialized RL4J trainer using native builders: layers=${mapping.networkConfig.numLayers}," +
                " hiddenNodes=${mapping.networkConfig.numHiddenNodes}, expReplaySize=${mapping.qLearningConfig.expRepMaxSize}" +
                ", seed=${mapping.qLearningConfig.seed}"
        )
    }

    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        ensureInitialized()
        val observation = ChessObservation(state)
        val selected = runCatching { policy?.nextAction(observation) }
            .onFailure { logger.warn("RL4J policy failed to select action: ${it.message}") }
            .getOrNull()

        if (selected != null && validActions.contains(selected)) {
            return selected
        }

        if (validActions.isNotEmpty()) {
            val qValues = getQValues(state, validActions)
            val best = qValues.maxByOrNull { it.value }?.key ?: validActions.first()
            if (selected != null && selected != best) {
                logger.debug("RL4J suggested illegal action $selected; using masked best $best instead")
            }
            return best
        }

        return 0
    }

    override fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        ensureInitialized()
        runTrainingIfNeeded()
        return lastUpdate
    }

    private fun runTrainingIfNeeded() {
        if (trainingCompleted) {
            logger.debug("RL4J training already completed; skipping train()")
            return
        }

        val qLearning = trainer ?: return
        val listener = trainingListener
        listener?.reset()
        logger.info("Starting RL4J training via RL4J train()")
        qLearning.train()
        @Suppress("UNCHECKED_CAST")
        policy = qLearning.policy as DQNPolicy<ChessObservation>
        trainingCompleted = true

        val epsilon = qLearning.egPolicy?.epsilon?.toDouble() ?: currentExplorationRate
        currentExplorationRate = epsilon

        val stats = listener?.snapshot()
        val loss = neuralNet?.latestScore()?.let { if (it.isFinite()) max(it, 0.0) else 0.0 } ?: 0.0
        val averageReward = stats?.averageReward() ?: 0.0

        lastUpdate = PolicyUpdateResult(
            loss = loss,
            gradientNorm = 0.0,
            policyEntropy = epsilon,
            qValueMean = averageReward
        )
        metrics = metrics.copy(
            averageReward = averageReward,
            totalReward = stats?.totalReward ?: 0.0,
            averageLoss = lastUpdate.loss,
            episodeCount = stats?.episodeCount ?: 0,
            explorationRate = epsilon,
            policyEntropy = epsilon
        )

        if (stats != null) {
            logger.info(
                "RL4J training completed: episodes=${stats.episodeCount}, totalReward=${stats.totalReward}," +
                    " avgReward=${"%.4f".format(stats.averageReward())}, totalSteps=${stats.totalSteps}," +
                    " avgLoss=${"%.4f".format(lastUpdate.loss)}, epsilon=${"%.4f".format(epsilon)}"
            )
        } else {
            logger.info(
                "RL4J training completed: epsilon=${"%.4f".format(epsilon)}, loss=${"%.4f".format(lastUpdate.loss)}" +
                    " (no TrainingListener stats available)"
            )
        }

        logger.info("RL4J training completed")
    }

    override fun save(path: String) {
        ensureInitialized()
        policy?.save(path) ?: throw IllegalStateException("RL4J policy not initialized")
        logger.info("Saved RL4J policy to $path")
    }

    override fun load(path: String) {
        RL4JAvailability.validateAvailability()
        policy = DQNPolicy.load<ChessObservation>(path)
        trainingCompleted = true
        lastUpdate = lastUpdate.copy()
        logger.info("Loaded RL4J policy from $path")
    }

    override fun getConfig(): ChessAgentConfig = config

    override fun learn(experience: Experience<DoubleArray, Int>) {
        // RL4J gathers its own experience internally; nothing to do here.
    }

    override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        ensureInitialized()
        val network = neuralNet ?: return actions.associateWith { 0.0 }
        val observation = ChessObservation(state)
        return runCatching {
            val output = network.output(observation)
            val data = output.toDoubleVector()
            actions.associateWith { index -> data.getOrNull(index) ?: 0.0 }
        }.getOrElse {
            logger.debug("Failed to compute Q-values: ${it.message}")
            actions.associateWith { 0.0 }
        }
    }

    override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        val qValues = getQValues(state, actions)
        if (qValues.isEmpty()) return emptyMap()

        val max = qValues.values.maxOrNull() ?: 0.0
        val expValues = qValues.mapValues { exp(it.value - max) }
        val total = expValues.values.sum()
        if (total == 0.0) {
            val uniform = 1.0 / actions.size.coerceAtLeast(1)
            return actions.associateWith { uniform }
        }
        return expValues.mapValues { it.value / total }
    }

    override fun getTrainingMetrics(): ChessAgentMetrics = metrics

    override fun forceUpdate() {
        @Suppress("UNCHECKED_CAST")
        val updatedPolicy = trainer?.policy as? DQNPolicy<ChessObservation>
        policy = updatedPolicy ?: policy
    }

    override fun reset() {
        trainer = null
        policy = null
        neuralNet = null
        mdp = null
        trainingListener = null
        trainingCompleted = false
        lastUpdate = PolicyUpdateResult(0.0, 0.0, currentExplorationRate, 0.0)
        metrics = metrics.copy(
            averageReward = 0.0,
            totalReward = 0.0,
            averageLoss = 0.0,
            episodeCount = 0
        )
        logger.info("RL4J agent reset; will reinitialize on next use")
    }

    override fun setExplorationRate(rate: Double) {
        currentExplorationRate = rate.coerceIn(0.0, 1.0)
        metrics = metrics.copy(
            explorationRate = currentExplorationRate,
            policyEntropy = currentExplorationRate
        )
    }

    fun getRL4JTraining(): QLearningDiscreteDense<ChessObservation>? = trainer
    fun getRL4JPolicy(): DQNPolicy<ChessObservation>? = policy
    fun getDQNNetwork(): IDQN<*>? = neuralNet

}

private class RL4JTrainingMetricsListener(
    private val agentLogger: ComponentLogger,
    private val maxStepTarget: Int,
    private val maxEpochStep: Int,
    private val epsilonProvider: () -> Double,
    private val initialEpsilon: Double,
    private val minEpsilon: Double
) : TrainingListener {

    private var totalReward: Double = 0.0
    private var totalSteps: Int = 0
    private var episodeCount: Int = 0
    private var lastReward: Double = 0.0
    private var lastLogTimeMillis: Long = 0
    private var startMessageLogged = false

    private val expectedEpisodes: Int = calculateExpectedEpisodes()

    override fun onTrainingStart(): TrainingListener.ListenerResponse {
        reset()
        lastLogTimeMillis = System.currentTimeMillis()
        if (!startMessageLogged) {
            val targetDescription = if (expectedEpisodes > 0) {
                "up to $expectedEpisodes episodes (~$maxStepTarget steps)"
            } else {
                "an open-ended number of episodes"
            }
            agentLogger.info(
                "RL4J training starting; target %s. epsilon_start=%s, epsilon_floor=%s. Progress logs every %d episodes or %ds",
                targetDescription,
                formatDouble(initialEpsilon),
                formatDouble(minEpsilon),
                PROGRESS_LOG_EVERY_EPISODES,
                PROGRESS_LOG_EVERY_MILLIS / 1000
            )
            startMessageLogged = true
        }
        return TrainingListener.ListenerResponse.CONTINUE
    }

    override fun onTrainingEnd() {
        // Nothing to do
    }

    override fun onNewEpoch(trainer: IEpochTrainer?): TrainingListener.ListenerResponse {
        return TrainingListener.ListenerResponse.CONTINUE
    }

    override fun onEpochTrainingResult(
        trainer: IEpochTrainer?,
        statEntry: IDataManager.StatEntry
    ): TrainingListener.ListenerResponse {
        episodeCount += 1
        totalReward += statEntry.reward
        totalSteps += statEntry.stepCounter
        lastReward = statEntry.reward
        if (shouldLogProgress()) {
            logProgress()
        }
        return TrainingListener.ListenerResponse.CONTINUE
    }

    override fun onTrainingProgress(learning: ILearning<*, *, *>?): TrainingListener.ListenerResponse {
        return TrainingListener.ListenerResponse.CONTINUE
    }

    fun reset() {
        totalReward = 0.0
        totalSteps = 0
        episodeCount = 0
        lastReward = 0.0
        lastLogTimeMillis = 0
    }

    fun snapshot(): MetricsSnapshot = MetricsSnapshot(
        totalReward = totalReward,
        totalSteps = totalSteps,
        episodeCount = episodeCount,
        lastReward = lastReward
    )

    data class MetricsSnapshot(
        val totalReward: Double,
        val totalSteps: Int,
        val episodeCount: Int,
        val lastReward: Double
    ) {
        fun averageReward(): Double = if (episodeCount > 0) totalReward / episodeCount else 0.0
    }

    private fun shouldLogProgress(): Boolean {
        if (episodeCount <= 0) return false
        if (episodeCount == 1) return true
        if (episodeCount % PROGRESS_LOG_EVERY_EPISODES == 0) return true
        val now = System.currentTimeMillis()
        return now - lastLogTimeMillis >= PROGRESS_LOG_EVERY_MILLIS
    }

    private fun logProgress() {
        val epsilon = safeEpsilon()
        val avgReward = if (episodeCount > 0) totalReward / episodeCount else 0.0
        val stepPercent = if (maxStepTarget > 0) {
            min(100.0, (totalSteps.toDouble() / max(maxStepTarget, 1) * 100.0))
        } else {
            null
        }
        val episodePercent = if (expectedEpisodes > 0) {
            min(100.0, (episodeCount.toDouble() / expectedEpisodes * 100.0))
        } else {
            stepPercent
        }

        val episodeText = if (expectedEpisodes > 0) {
            val percentText = episodePercent?.let { " (${formatPercent(it)})" } ?: ""
            "$episodeCount/$expectedEpisodes$percentText"
        } else {
            episodeCount.toString()
        }

        val stepText = if (maxStepTarget > 0) {
            val percentText = stepPercent?.let { " (${formatPercent(it)})" } ?: ""
            "$totalSteps/$maxStepTarget$percentText"
        } else {
            totalSteps.toString()
        }

        agentLogger.info(
            "RL4J progress: episode=%s, steps=%s, epsilon=%s, avgReward=%s, lastReward=%s",
            episodeText,
            stepText,
            formatDouble(epsilon),
            formatDouble(avgReward),
            formatDouble(lastReward)
        )
        lastLogTimeMillis = System.currentTimeMillis()
    }

    private fun safeEpsilon(): Double = runCatching { epsilonProvider() }.getOrElse { Double.NaN }

    private fun calculateExpectedEpisodes(): Int {
        if (maxStepTarget <= 0 || maxEpochStep <= 0) return 0
        return ceil(maxStepTarget.toDouble() / maxEpochStep).toInt().coerceAtLeast(1)
    }

    private fun formatDouble(value: Double): String {
        return if (value.isFinite()) "%.4f".format(value) else "-"
    }

    private fun formatPercent(value: Double): String {
        return "%.1f%%".format(value)
    }

    private companion object {
        private const val PROGRESS_LOG_EVERY_EPISODES: Int = 5
        private const val PROGRESS_LOG_EVERY_MILLIS: Long = 60_000L
    }
}

private fun IDQN<*>.latestScore(): Double = runCatching { getLatestScore() }.getOrDefault(0.0)

private fun rl4jVersion(): String = QLearningDiscreteDense::class.java.pkgVersion()
private fun dl4jVersion(): String = IDQN::class.java.pkgVersion()
private fun nd4jVersion(): String = Nd4j::class.java.pkgVersion()

private fun Class<*>.pkgVersion(): String = this.`package`?.implementationVersion ?: "unknown"

private fun setEpsilonViaMethod(policy: Any, epsilon: Double): Boolean {
    val clazz = policy.javaClass
    val attempts = listOf(
        runCatching {
            val method = clazz.getMethod("setEpsilon", java.lang.Float.TYPE)
            method.invoke(policy, epsilon.toFloat())
        },
        runCatching {
            val method = clazz.getMethod("setEpsilon", java.lang.Double.TYPE)
            method.invoke(policy, epsilon)
        }
    )
    return attempts.any { it.isSuccess }
}

private fun setEpsilonViaField(policy: Any, epsilon: Double): Boolean {
    return runCatching {
        val field = policy.javaClass.getDeclaredField("epsilon")
        field.isAccessible = true
        when (field.type) {
            java.lang.Float.TYPE -> field.setFloat(policy, epsilon.toFloat())
            java.lang.Double.TYPE -> field.setDouble(policy, epsilon)
            else -> throw IllegalStateException("Unsupported epsilon field type: ${field.type}")
        }
    }.isSuccess
}
