package com.chessrl.integration.backend

import com.chessrl.integration.AlphaZeroSample
import com.chessrl.integration.ChessAgent
import com.chessrl.integration.MCTSChessAgent
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.rl.Experience
import com.chessrl.rl.PolicyUpdateResult

/**
 * LearningSession for the AlphaZero backend.
 *
 * [trainOnBatch] satisfies the [LearningSession] interface for checkpoint compatibility
 * but is a no-op — actual training is done via [trainOnAlphaZeroSamples].
 *
 * The opponent network is a weight-copy of the main network, updated every few cycles.
 */
class AlphaZeroLearningSession(
    override val config: ChessRLConfig,
    override val mainAgent: MCTSChessAgent,
    override val opponentAgent: MCTSChessAgent
) : LearningSession {

    private val logger = ChessRLLogger.forComponent("AlphaZeroSession")

    /**
     * Train the main network on a batch of AlphaZero samples.
     * Returns a [PolicyUpdateResult] with combined policy + value loss.
     */
    fun trainOnAlphaZeroSamples(samples: List<AlphaZeroSample>): PolicyUpdateResult {
        if (samples.isEmpty()) return PolicyUpdateResult(0.0, 0.0, 0.0)
        val (policyLoss, valueLoss) = mainAgent.network.trainBatch(samples)
        val totalLoss = policyLoss + valueLoss
        logger.debug("AlphaZero training: policyLoss=%.4f valueLoss=%.4f".format(policyLoss, valueLoss))
        return PolicyUpdateResult(
            loss = totalLoss,
            gradientNorm = 0.0,
            policyEntropy = 0.0,
            valueError = valueLoss
        )
    }

    /** Copies main network weights to the opponent network. */
    override fun updateOpponent() {
        mainAgent.network.copyWeightsTo(opponentAgent.network)
        logger.debug("Opponent network updated from main network")
    }

    override fun trainOnBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult =
        PolicyUpdateResult(0.0, 0.0, 0.0)

    override fun saveCheckpoint(path: String) = mainAgent.save(path)
    override fun saveBest(path: String) = mainAgent.save(path)
    override fun close() {}
}
