package com.chessrl.integration

import com.chessrl.rl.Experience
import kotlin.test.Test
import kotlin.test.assertTrue

class PolicyGradientVerificationTest {

    @Test
    fun testPolicyGradientIncreasesPreferredActionProbability() {
        // Tiny PG agent
        val agent = RealChessAgentFactory.createRealPolicyGradientAgent(
            inputSize = 6,
            outputSize = 4,
            hiddenLayers = listOf(8),
            learningRate = 0.02,
            temperature = 1.0,
            batchSize = 4
        )

        val state = DoubleArray(6) { i -> (i + 1) * 0.1 }
        val valid = listOf(0, 1, 2, 3)

        val before = agent.getActionProbabilities(state, valid)
        val p2Before = before[2] ?: 0.0
        val p1Before = before[1] ?: 0.0

        // Train with experiences favoring action 2 (positive return) and penalizing action 1
        repeat(8) {
            agent.learn(Experience(state, 2, 1.0, state, false))
        }
        repeat(4) {
            agent.learn(Experience(state, 1, -0.5, state, false))
        }

        // PG path updates immediately in learn(); no forceUpdate required
        val after = agent.getActionProbabilities(state, valid)
        val p2After = after[2] ?: 0.0
        val p1After = after[1] ?: 0.0

        // Probabilities remain a proper distribution
        val sumAfter = after.values.sum()
        assertTrue(sumAfter in 0.99..1.01, "Probabilities should sum to ~1.0, was $sumAfter")

        // Preferred action increases in probability, penalized action decreases
        assertTrue(p2After > p2Before, "Expected p(action=2) to increase: before=$p2Before after=$p2After")
        assertTrue(p1After < p1Before, "Expected p(action=1) to decrease: before=$p1Before after=$p1After")
    }
}

