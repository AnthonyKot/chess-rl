package com.chessrl.integration

import com.chessrl.rl.Experience
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Minimal DQN smoke E2E:
 * - Replay buffer fills
 * - TD loss decreases over a few consecutive trainBatch updates
 * - Q-values shift on a fixed probe state
 */
class DQNSmokeE2ETest {

    @Test
    fun bufferFills_and_lossDecreases_and_qValuesShift() {
        val cfg = ChessAgentConfig(batchSize = 16, maxBufferSize = 512, targetUpdateFrequency = 50)
        // Seed for determinism in this smoke test
        val seedManager = SeedManager.initializeWithSeed(2025L)
        val agent = ChessAgentFactory.createSeededDQNAgent(
            hiddenLayers = listOf(32, 16),
            learningRate = 1e-3,
            explorationRate = 0.0, // deterministic greedy to reduce noise in loss
            config = cfg,
            seedManager = seedManager
        )

        val rng = Random(2025)
        val stateSize = ChessStateEncoder.TOTAL_FEATURES
        val actions = (0 until 8).toList()

        // Probe state for Q-value shift check
        val probe = DoubleArray(stateSize) { rng.nextDouble() * 0.01 }
        val qBefore = agent.getQValues(probe, actions)

        // Create a consistent synthetic batch to drive learning
        val batch = MutableList(cfg.batchSize) { idx ->
            val s = DoubleArray(stateSize) { rng.nextDouble() }
            val ns = DoubleArray(stateSize) { rng.nextDouble() }
            val a = actions[rng.nextInt(actions.size)]
            val r = if (idx % 4 == 0) 1.0 else -0.05
            val d = false
            Experience(state = s, action = a, reward = r, nextState = ns, done = d)
        }

        // Feed some experiences to fill replay (uses DQN's internal buffer)
        repeat(4 * cfg.batchSize) {
            val e = batch[rng.nextInt(batch.size)]
            agent.learn(e)
        }

        // Now perform a few explicit trainBatch updates and record losses
        val losses = mutableListOf<Double>()
        repeat(5) {
            val shuffled = batch.shuffled(rng)
            val result = agent.trainBatch(shuffled)
            losses += result.loss
        }

        // Replay buffer grew
        val metrics = agent.getTrainingMetrics()
        assertTrue(metrics.experienceBufferSize >= cfg.batchSize, "Replay buffer should contain at least one batch")

        // Loss shows downward trend (allow some noise; compare first vs last)
        assertTrue(losses.last() <= losses.first() || losses.zipWithNext().count { it.second <= it.first } >= 2,
            "TD loss should generally decrease over a few updates: ${losses}")

        // Q-values change on the probe state
        val qAfter = agent.getQValues(probe, actions)
        val changed = actions.any { a -> kotlin.math.abs((qAfter[a] ?: 0.0) - (qBefore[a] ?: 0.0)) > 1e-8 }
        assertTrue(changed, "Expected probe Q-values to shift after training")
    }
}
