package com.chessrl.nn

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * End-to-end learning verification on the classic XOR problem.
 *
 * Proves the network can learn a non-linearly separable task using
 * gradient descent (Adam) and that loss decreases with training.
 */
class XORLearningE2ETest {

    @Test
    fun learnsXORWithHiddenLayer() {
        val rng = Random(42)

        // XOR dataset: inputs in {0,1}^2, labels in {0,1} as one-hot over 2 classes
        val inputs = arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(1.0, 1.0)
        )
        val targets = arrayOf(
            doubleArrayOf(1.0, 0.0), // 0 xor 0 -> 0
            doubleArrayOf(0.0, 1.0), // 0 xor 1 -> 1
            doubleArrayOf(0.0, 1.0), // 1 xor 0 -> 1
            doubleArrayOf(1.0, 0.0)  // 1 xor 1 -> 0
        )

        // Small MLP: 2 -> 8 -> 2 with ReLU and linear output (softmax in loss)
        val net = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(2, 8, ReLUActivation(), rng),
                DenseLayer(8, 2, LinearActivation(), rng)
            ),
            lossFunction = SoftmaxCrossEntropyLoss(),
            optimizer = AdamOptimizer(learningRate = 0.05)
        )

        // Initial loss
        val initialLoss = averageLoss(net, inputs, targets)

        // Train for a modest number of epochs
        val epochs = 300
        repeat(epochs) { epoch ->
            // Shuffle order each epoch for robustness
            val order = (inputs.indices).shuffled(rng)
            val batchInputs = Array(order.size) { i -> inputs[order[i]] }
            val batchTargets = Array(order.size) { i -> targets[order[i]] }
            net.trainBatch(TrainingBatch(batchInputs, batchTargets), epoch + 1)
        }

        val finalLoss = averageLoss(net, inputs, targets)
        val acc = accuracy(net, inputs, targets)

        // Assert clear learning signal
        assertTrue(finalLoss < initialLoss * 0.5, "Expected final loss to drop by >50% (initial=$initialLoss, final=$finalLoss)")
        assertTrue(acc >= 0.95, "Expected >=95% accuracy on XOR after training, got ${acc * 100.0}%")
    }

    private fun averageLoss(net: FeedforwardNetwork, xs: Array<DoubleArray>, ys: Array<DoubleArray>): Double {
        var total = 0.0
        for (i in xs.indices) {
            val yHat = net.predict(xs[i])
            total += SoftmaxCrossEntropyLoss().computeLoss(yHat, ys[i])
        }
        return total / xs.size
    }

    private fun accuracy(net: FeedforwardNetwork, xs: Array<DoubleArray>, ys: Array<DoubleArray>): Double {
        var correct = 0
        for (i in xs.indices) {
            val yHat = net.predict(xs[i])
            val pred = yHat.indices.maxByOrNull { yHat[it] } ?: 0
            val label = ys[i].indices.maxByOrNull { ys[i][it] } ?: 0
            if (pred == label) correct++
        }
        return correct.toDouble() / xs.size
    }
}

