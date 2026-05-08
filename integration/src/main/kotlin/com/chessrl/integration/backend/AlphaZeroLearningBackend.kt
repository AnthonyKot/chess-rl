package com.chessrl.integration.backend

import com.chessrl.integration.ChessAgentConfig
import com.chessrl.integration.MCTSChessAgent
import com.chessrl.integration.adapter.ChessEngineFactory
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.integration.mcts.MCTSTree
import kotlin.random.Random

/**
 * LearningBackend that creates an AlphaZero-style MCTS session.
 * Registered as BackendType.ALPHAZERO.
 */
class AlphaZeroLearningBackend : LearningBackend {
    override val id = "alphazero"

    private val logger = ChessRLLogger.forComponent("AlphaZeroBackend")

    override fun createSession(config: ChessRLConfig): AlphaZeroLearningSession {
        logger.info("Initializing AlphaZero backend (simulations=${config.mctsSimulations}, cPuct=${config.cPuct})")

        val adapter = ChessEngineFactory.create(config.engine)
        val rng = if (config.seed != null) Random(config.seed) else Random.Default

        fun buildAgent(temperature: Double): MCTSChessAgent {
            val network = DualHeadNetwork(
                trunkSizes = config.hiddenLayers,
                learningRate = config.learningRate,
                random = rng
            )
            val mctsTree = MCTSTree(
                network = network,
                adapter = adapter,
                simulations = config.mctsSimulations,
                cPuct = config.cPuct,
                dirichletAlpha = config.mctsDirichletAlpha,
                dirichletEpsilon = config.mctsDirichletEpsilon,
                random = rng
            )
            return MCTSChessAgent(
                network = network,
                mctsTree = mctsTree,
                temperature = temperature,
                config = ChessAgentConfig(
                    batchSize = config.batchSize,
                    learningRate = config.learningRate
                )
            )
        }

        val mainAgent = buildAgent(temperature = config.mctsTemperature)
        val opponentAgent = buildAgent(temperature = config.mctsTemperature)

        // Synchronize opponent to start with same weights as main
        mainAgent.network.copyWeightsTo(opponentAgent.network)

        logger.info("AlphaZero backend initialized: trunk=${config.hiddenLayers}, policy=4096, value=1")

        return AlphaZeroLearningSession(config, mainAgent, opponentAgent)
    }
}
