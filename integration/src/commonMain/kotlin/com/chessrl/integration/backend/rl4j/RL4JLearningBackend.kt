package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.ChessAgent
import com.chessrl.integration.ChessAgentConfig
import com.chessrl.integration.backend.BackendAwareChessAgentFactory
import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.backend.LearningBackend
import com.chessrl.integration.backend.LearningSession
import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.rl.Experience
import com.chessrl.rl.PolicyUpdateResult
import java.nio.file.Files
import java.nio.file.Path

/**
 * Learning backend that delegates the entire training loop to RL4J. The manual
 * experience pipeline is bypassed: once the session receives a training
 * request, it invokes `learn()` on the underlying RL4J trainer and reuses the
 * resulting policy for subsequent calls.
 */
class RL4JLearningBackend : LearningBackend {

    private val logger = ChessRLLogger.forComponent("RL4JLearningBackend")

    override val id: String = "rl4j"

    override fun createSession(config: ChessRLConfig): LearningSession {
        logger.info("Creating RL4J learning session")

        RL4JAvailability.validateAvailability()

        val agentConfig = ChessAgentConfig(
            batchSize = config.batchSize,
            maxBufferSize = config.maxExperienceBuffer,
            learningRate = config.learningRate,
            explorationRate = config.explorationRate,
            targetUpdateFrequency = config.targetUpdateFrequency,
            gamma = config.gamma
        )

        val backendConfig = BackendAwareChessAgentFactory.createBackendConfig(
            hiddenLayers = config.hiddenLayers,
            learningRate = config.learningRate,
            optimizer = config.optimizer
        )

        val mainAgent = BackendAwareChessAgentFactory.createSeededDQNAgent(
            backendType = BackendType.RL4J,
            backendConfig = backendConfig,
            agentConfig = agentConfig,
            enableDoubleDQN = config.doubleDqn,
            replayType = config.replayType,
            gamma = config.gamma,
            trainingConfig = config
        )

        val opponentAgent = BackendAwareChessAgentFactory.createSeededDQNAgent(
            backendType = BackendType.RL4J,
            backendConfig = backendConfig,
            agentConfig = agentConfig,
            enableDoubleDQN = config.doubleDqn,
            replayType = config.replayType,
            gamma = config.gamma,
            trainingConfig = config
        )

        return RL4JLearningSession(config, mainAgent, opponentAgent)
    }
}

private class RL4JLearningSession(
    override val config: ChessRLConfig,
    override val mainAgent: ChessAgent,
    override val opponentAgent: ChessAgent
) : LearningSession {

    private val logger = ChessRLLogger.forComponent("RL4JLearningSession")
    private var trainingTriggered = false
    private var cachedResult: PolicyUpdateResult = PolicyUpdateResult(0.0, 0.0, config.explorationRate, 0.0)

    override fun trainOnBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        if (!trainingTriggered) {
            logger.info("Triggering RL4J training via learn()")
            cachedResult = mainAgent.trainBatch(emptyList())
            if (opponentAgent !== mainAgent) {
                opponentAgent.trainBatch(emptyList())
            }
            trainingTriggered = true
        } else {
            logger.debug("RL4J training already executed; returning cached result")
        }
        return cachedResult
    }

    override fun updateOpponent() {
        runCatching { opponentAgent.setExplorationRate(mainAgent.getConfig().explorationRate) }
    }

    override fun saveCheckpoint(path: String) {
        ensureParentDirectory(path)
        mainAgent.save(path)
    }

    override fun saveBest(path: String) {
        ensureParentDirectory(path)
        mainAgent.save(path)
    }

    private fun ensureParentDirectory(path: String) {
        runCatching {
            val parent = Path.of(path).parent ?: return
            Files.createDirectories(parent)
        }.onFailure {
            logger.warn("Failed to prepare checkpoint directory for $path: ${it.message}")
        }
    }
}
