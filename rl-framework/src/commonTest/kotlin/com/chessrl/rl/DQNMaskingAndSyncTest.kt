package com.chessrl.rl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FixedNetwork(private val outputs: DoubleArray) : TrainableNeuralNetwork, SynchronizableNetwork {
    var syncCopyCount = 0
    override fun forward(input: DoubleArray): DoubleArray = outputs.copyOf()
    override fun backward(target: DoubleArray): DoubleArray = doubleArrayOf(0.0)
    override fun trainBatch(inputs: Array<DoubleArray>, targets: Array<DoubleArray>): Double {
        // no-op training; return simple MSE
        var total = 0.0
        for (i in inputs.indices) {
            total += mse(outputs, targets[i])
        }
        return total / inputs.size
    }
    override fun copyWeightsTo(target: NeuralNetwork) {
        syncCopyCount++
    }
    private fun mse(a: DoubleArray, b: DoubleArray): Double {
        var s = 0.0
        val n = minOf(a.size, b.size)
        for (i in 0 until n) { val d = a[i] - b[i]; s += d*d }
        return s / n
    }
}

class DQNMaskingAndSyncTest {

    @Test
    fun `illegal action masking limits next-state max to valid actions`() {
        val actionSpace = 4
        val qNet = FixedNetwork(DoubleArray(actionSpace) { 0.0 })
        // Target network will produce: [5.0, 100.0, 1.0, -3.0]
        val targetNet = FixedNetwork(doubleArrayOf(5.0, 100.0, 1.0, -3.0))
        val replay = CircularExperienceBuffer<DoubleArray, Int>(maxSize = 16)

        val gamma = 0.5
        val dqn = DQNAlgorithm(qNet, targetNet, replay, gamma = gamma, targetUpdateFrequency = 1000, batchSize = 1)
        // Only actions 0 and 2 are valid in next state; the large 100.0 at index 1 must be ignored
        dqn.setNextActionProvider { _ -> listOf(0, 2) }

        val exp = Experience(
            state = doubleArrayOf(0.0),
            action = 0,
            reward = 1.0,
            nextState = doubleArrayOf(0.0),
            done = false
        )

        val result = dqn.updatePolicy(listOf(exp))
        // Target should be 1.0 + gamma * max(5.0, 1.0) = 1.0 + 0.5 * 5.0 = 3.5
        assertEquals(3.5, result.targetValueMean!!, 1e-9)
    }

    @Test
    fun `target sync cadence triggers copy at frequency`() {
        val qNet = FixedNetwork(doubleArrayOf(0.0, 0.0))
        val targetNet = FixedNetwork(doubleArrayOf(0.0, 0.0))
        val replay = CircularExperienceBuffer<DoubleArray, Int>(maxSize = 16)
        val freq = 2
        val dqn = DQNAlgorithm(qNet, targetNet, replay, targetUpdateFrequency = freq, batchSize = 1)

        val exp = Experience(
            state = doubleArrayOf(0.0),
            action = 0,
            reward = 0.0,
            nextState = doubleArrayOf(0.0),
            done = true
        )

        // Two updates should trigger one sync
        dqn.updatePolicy(listOf(exp))
        dqn.updatePolicy(listOf(exp))
        assertTrue(qNet.syncCopyCount >= 1, "Expected at least one target sync copy")
    }
}

