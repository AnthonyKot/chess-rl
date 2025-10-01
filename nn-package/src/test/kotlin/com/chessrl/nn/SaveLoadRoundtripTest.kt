package com.chessrl.nn

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue
import java.nio.file.Files
import java.nio.file.Path

class SaveLoadRoundtripTest {

    @Test
    fun `feedforward network save-load roundtrip preserves outputs`() {
        // Build a small deterministic network
        val layers = listOf(
            DenseLayer(inputSize = 4, outputSize = 5, activation = ReLUActivation()),
            DenseLayer(inputSize = 5, outputSize = 3, activation = LinearActivation())
        )
        val net = FeedforwardNetwork(
            _layers = layers,
            lossFunction = MSELoss(),
            optimizer = SGDOptimizer(learningRate = 0.01)
        )

        val input = doubleArrayOf(0.5, -0.2, 0.1, 0.0)
        val outBefore = net.forward(input).copyOf()

        // Save to temp file
        val tmpDir: Path = Files.createTempDirectory("chessrl_nn_roundtrip_")
        val modelPath = tmpDir.resolve("model.json").toString()
        net.save(modelPath)

        // Perturb by training briefly to change weights
        val batch = TrainingBatch(
            inputs = arrayOf(input, input, input),
            targets = arrayOf(doubleArrayOf(0.0, 1.0, 0.0), doubleArrayOf(0.0, 1.0, 0.0), doubleArrayOf(0.0, 1.0, 0.0))
        )
        net.trainBatch(batch, epoch = 1)
        val outAfterTrain = net.forward(input)
        // Sanity: outputs should have changed after training
        var anyDiff = false
        for (i in outBefore.indices) {
            if (kotlin.math.abs(outBefore[i] - outAfterTrain[i]) > 1e-9) {
                anyDiff = true
                break
            }
        }
        assertTrue(anyDiff, "Training should change network outputs before reload")

        // Reload from saved file
        net.load(modelPath)
        val outAfterLoad = net.forward(input)

        // Verify exact match with pre-training saved outputs (tolerance)
        for (i in outBefore.indices) {
            val diff = kotlin.math.abs(outBefore[i] - outAfterLoad[i])
            assertTrue(diff < 1e-9, "Output mismatch at $i: diff=$diff")
        }
    }
}

