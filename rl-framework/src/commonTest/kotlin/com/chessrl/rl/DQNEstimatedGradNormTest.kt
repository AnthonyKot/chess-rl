package com.chessrl.rl

import kotlin.test.Test
import kotlin.test.assertTrue

class DQNEstimatedGradNormTest {
    @Test
    fun `estimated gradient norm is positive on non-trivial batch`() {
        val inputSize = 4
        val outputSize = 3
        val qNet = SimpleNeuralNetwork(inputSize, outputSize)
        val tgtNet = SimpleNeuralNetwork(inputSize, outputSize)
        val replay = CircularExperienceBuffer<DoubleArray, Int>(maxSize = 64)
        val dqn = DQNAlgorithm(qNet, tgtNet, replay, gamma = 0.99, targetUpdateFrequency = 1000, batchSize = 3)

        // Create experiences with varying actions/rewards to produce non-zero deltas
        val exps = listOf(
            Experience(doubleArrayOf(1.0, 0.0, 0.0, 0.0), 0, 1.0, doubleArrayOf(0.5, 0.0, 0.0, 0.0), done = false),
            Experience(doubleArrayOf(0.0, 1.0, 0.0, 0.0), 1, -0.5, doubleArrayOf(0.0, 0.5, 0.0, 0.0), done = false),
            Experience(doubleArrayOf(0.0, 0.0, 1.0, 0.0), 2, 0.3, doubleArrayOf(0.0, 0.0, 0.5, 0.0), done = true)
        )

        val result = dqn.updatePolicy(exps)
        assertTrue(result.gradientNorm > 0.0, "Expected positive gradient norm estimate")
        assertTrue(result.loss >= 0.0, "Loss should be non-negative")
    }
}

