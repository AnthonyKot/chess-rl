package com.chessrl.rl

import kotlin.test.Test
import kotlin.test.assertTrue

class PolicyGradientEstimatedGradNormTest {
    @Test
    fun `policy gradient estimated gradient norm is positive`() {
        val inputSize = 4
        val actionSize = 3
        val policy = SimpleNeuralNetwork(inputSize, actionSize)
        val pg = PolicyGradientAlgorithm(policyNetwork = policy, valueNetwork = null, gamma = 0.99)

        val exps = listOf(
            Experience(doubleArrayOf(1.0, 0.0, 0.0, 0.0), 0, 1.0, doubleArrayOf(0.0, 0.0, 0.0, 0.0), done = true),
            Experience(doubleArrayOf(0.0, 1.0, 0.0, 0.0), 1, 0.5, doubleArrayOf(0.0, 0.0, 0.0, 0.0), done = true),
            Experience(doubleArrayOf(0.0, 0.0, 1.0, 0.0), 2, 0.2, doubleArrayOf(0.0, 0.0, 0.0, 0.0), done = true)
        )

        val result = pg.updatePolicy(exps)
        assertTrue(result.gradientNorm > 0.0, "Expected positive gradient norm estimate for PG")
    }
}

