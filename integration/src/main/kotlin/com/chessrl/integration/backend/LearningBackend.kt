package com.chessrl.integration.backend

import com.chessrl.integration.ChessAgent
import com.chessrl.integration.ChessAgentConfig
import com.chessrl.integration.ChessAgentFactory
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.rl.Experience
import com.chessrl.rl.PolicyUpdateResult

/**
 * Defines the abstraction layer between the training pipeline and the
 * underlying learning framework. Backends are responsible for instantiating
 * agents, applying training updates, and handling persistence, allowing the
 * pipeline to remain agnostic of the concrete RL implementation (DQN, DL4J, etc.).
 */
interface LearningBackend {
    /** Identifier used for CLI/config selection (e.g. "dqn", "dl4j"). */
    val id: String

    /** Create a new learning session bound to the supplied configuration. */
    fun createSession(config: ChessRLConfig): LearningSession
}

/**
 * Represents a live training session. The pipeline interacts with the session
 * to access agents, apply training updates, refresh opponents, and persist
 * checkpoints. Implementations can map these calls to any underlying framework.
 */
interface LearningSession : AutoCloseable {
    val config: ChessRLConfig
    val mainAgent: ChessAgent
    val opponentAgent: ChessAgent

    /** Apply a single training update on the provided batch of experiences. */
    fun trainOnBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult

    /** Refresh the opponent policy (e.g., copy weights or rebuild opponent). */
    fun updateOpponent()

    /** Persist the current main agent to a checkpoint path. */
    fun saveCheckpoint(path: String)

    /** Persist the current best agent snapshot. */
    fun saveBest(path: String)

    override fun close() { /* default no-op */ }
}

/**
 * Default backend using the existing DQN implementation from the rl-framework
 * module. This keeps behaviour identical to previous revisions while exposing
 * the pluggable abstraction for future backends (e.g., DL4J).
 * 
 * Now supports pluggable neural network backends via BackendType parameter.
 */
class DqnLearningBackend(
    private val nnBackendType: BackendType = BackendType.MANUAL
) : LearningBackend {
    override val id: String = "dqn"

    override fun createSession(config: ChessRLConfig): LearningSession {
        val agentConfig = ChessAgentConfig(
            batchSize = config.batchSize,
            maxBufferSize = config.maxExperienceBuffer,
            targetUpdateFrequency = config.targetUpdateFrequency
        )
        
        // Create agents with specified neural network backend
        val mainAgent = if (nnBackendType != BackendType.MANUAL) {
            ChessAgentFactory.createSeededDQNAgent(
                backendType = nnBackendType,
                hiddenLayers = config.hiddenLayers,
                learningRate = config.learningRate,
                optimizer = config.optimizer,
                explorationRate = config.explorationRate,
                config = agentConfig,
                replayType = config.replayType,
                gamma = config.gamma,
                enableDoubleDQN = config.doubleDqn,
                trainingConfig = config
            )
        } else {
            // Use existing method for manual backend (backward compatibility)
            ChessAgentFactory.createSeededDQNAgent(
                hiddenLayers = config.hiddenLayers,
                learningRate = config.learningRate,
                optimizer = config.optimizer,
                explorationRate = config.explorationRate,
                config = agentConfig,
                replayType = config.replayType,
                gamma = config.gamma,
                enableDoubleDQN = config.doubleDqn
            )
        }

        val opponentAgent = if (nnBackendType != BackendType.MANUAL) {
            ChessAgentFactory.createSeededDQNAgent(
                backendType = nnBackendType,
                hiddenLayers = config.hiddenLayers,
                learningRate = config.learningRate,
                optimizer = config.optimizer,
                explorationRate = config.explorationRate,
                config = agentConfig,
                replayType = config.replayType,
                gamma = config.gamma,
                enableDoubleDQN = config.doubleDqn,
                trainingConfig = config
            )
        } else {
            // Use existing method for manual backend (backward compatibility)
            ChessAgentFactory.createSeededDQNAgent(
                hiddenLayers = config.hiddenLayers,
                learningRate = config.learningRate,
                optimizer = config.optimizer,
                explorationRate = config.explorationRate,
                config = agentConfig,
                replayType = config.replayType,
                gamma = config.gamma,
                enableDoubleDQN = config.doubleDqn
            )
        }
        
        return DqnLearningSession(config, mainAgent, opponentAgent)
    }
}

private class DqnLearningSession(
    override val config: ChessRLConfig,
    override val mainAgent: ChessAgent,
    override val opponentAgent: ChessAgent
) : LearningSession {

    override fun trainOnBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        return mainAgent.trainBatch(experiences)
    }

    override fun updateOpponent() {
        // For now we simply keep the opponent aligned with exploration settings.
        runCatching {
            opponentAgent.setExplorationRate(mainAgent.getConfig().explorationRate)
        }
    }

    override fun saveCheckpoint(path: String) {
        mainAgent.save(path)
    }

    override fun saveBest(path: String) {
        mainAgent.save(path)
    }
}
