package com.chessrl.integration

import com.chessrl.rl.Experience
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class DQNLearningSmokeTest {
    @Test
    fun dqnLearnsFromSyntheticBatch() {
        // Small network and buffers for fast training
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.1,
            config = ChessAgentConfig(batchSize = 16, maxBufferSize = 256)
        )

        val rng = Random(1234)
        val stateSize = ChessStateEncoder.TOTAL_FEATURES
        val actions = listOf(0, 1, 2, 3, 4)

        // Baseline Q-values for a random state
        val baselineState = DoubleArray(stateSize) { rng.nextDouble() * 0.01 }
        val initialQ = agent.getQValues(baselineState, actions)

        // Generate a synthetic batch of experiences large enough to trigger an update
        val batchSize = 32
        repeat(batchSize) { i ->
            val state = DoubleArray(stateSize) { rng.nextDouble() }
            val nextState = DoubleArray(stateSize) { rng.nextDouble() }
            val action = actions[rng.nextInt(actions.size)]
            val reward = if (i % 5 == 0) 1.0 else -0.1 // occasional positive reward
            val done = i == batchSize - 1

            val exp = Experience(
                state = state,
                action = action,
                reward = reward,
                nextState = nextState,
                done = done
            )
            agent.learn(exp)
        }

        // Ensure an update happens even if the done flag did not trigger it
        agent.forceUpdate()

        // Post-training Q-values for the same baseline state
        val finalQ = agent.getQValues(baselineState, actions)

        // Check buffer grew
        val metrics = agent.getTrainingMetrics()
        assertTrue(metrics.experienceBufferSize >= batchSize / 2)

        // Check that at least one Q-value changed meaningfully
        val changed = actions.any { a ->
            val before = initialQ[a] ?: 0.0
            val after = finalQ[a] ?: 0.0
            kotlin.math.abs(before - after) > 1e-6
        }
        assertTrue(changed, "Expected at least one Q-value to change after training")
    }
}

