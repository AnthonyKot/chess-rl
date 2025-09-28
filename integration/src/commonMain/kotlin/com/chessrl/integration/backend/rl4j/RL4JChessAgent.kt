package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.ChessAgent
import com.chessrl.integration.ChessAgentConfig
import com.chessrl.integration.ChessAgentMetrics
import com.chessrl.integration.ChessEnvironment
import com.chessrl.integration.backend.BackendConfig
import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.rl.Experience
import com.chessrl.rl.PolicyUpdateResult
import org.deeplearning4j.rl4j.learning.IEpochTrainer
import org.deeplearning4j.rl4j.learning.ILearning
import org.deeplearning4j.rl4j.learning.listener.TrainingListener
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense
import org.deeplearning4j.rl4j.network.dqn.IDQN
import org.deeplearning4j.rl4j.policy.DQNPolicy
import org.deeplearning4j.rl4j.util.IDataManager
import org.nd4j.linalg.factory.Nd4j
import kotlin.math.exp
import kotlin.math.max

/**
 * RL4J-based chess agent that delegates all learning to RL4J's native trainers
 * and replay buffers. The manual experience pipeline is bypassed entirely –
 * training is executed through `QLearningDiscreteDense.train()`.
 */
@Suppress("DEPRECATION") // TODO: migrate to RL4J's newer agent/builder APIs once upgraded
class RL4JChessAgent(
    private val config: ChessAgentConfig,
    private val backendConfig: BackendConfig? = null
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

        val mapper = RL4JConfigurationMapper(config, backendConfig)
        val mapping = mapper.build()
        mapping.warnings.forEach { warning ->
            logger.warn("RL4J configuration warning: $warning")
        }

        logger.info(
            "Initializing RL4J backend version summary: rl4j=${rl4jVersion()}, dl4j=${dl4jVersion()}, nd4j=${nd4jVersion()}" +
                ", hyperparameters={batchSize=${config.batchSize}, buffer=${config.maxBufferSize}, lr=${config.learningRate}," +
                " gamma=${config.gamma}, epsilon=${config.explorationRate}, targetUpdate=${config.targetUpdateFrequency}}"
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
        val listener = RL4JTrainingMetricsListener()
        qLearning.addListener(listener)
        trainingListener = listener
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

private class RL4JTrainingMetricsListener : TrainingListener {

    private var totalReward: Double = 0.0
    private var totalSteps: Int = 0
    private var episodeCount: Int = 0
    private var lastReward: Double = 0.0

    override fun onTrainingStart(): TrainingListener.ListenerResponse {
        reset()
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
}

private fun IDQN<*>.latestScore(): Double = runCatching { getLatestScore() }.getOrDefault(0.0)

private fun rl4jVersion(): String = QLearningDiscreteDense::class.java.pkgVersion()
private fun dl4jVersion(): String = IDQN::class.java.pkgVersion()
private fun nd4jVersion(): String = Nd4j::class.java.pkgVersion()

private fun Class<*>.pkgVersion(): String = this.`package`?.implementationVersion ?: "unknown"
