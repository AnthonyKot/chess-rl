package com.chessrl.nn

import kotlin.math.abs
import kotlin.random.Random

/**
 * Lightweight demo helpers referenced by legacy showcase tests.
 * They exercise the higher level APIs without asserting on results.
 */
object AdvancedOptimizersDemo {
    private fun xorDataset(): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        val inputs = arrayOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(1.0, 1.0)
        )
        val targets = arrayOf(
            doubleArrayOf(0.0),
            doubleArrayOf(1.0),
            doubleArrayOf(1.0),
            doubleArrayOf(0.0)
        )
        return inputs to targets
    }

    fun demonstrateOptimizers() {
        val (inputs, targets) = xorDataset()
        val network = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(2, 8, ReLUActivation(), Random(1)),
                DenseLayer(8, 1, SigmoidActivation(), Random(2))
            ),
            lossFunction = CrossEntropyLoss(),
            optimizer = AdamOptimizer(learningRate = 0.05)
        )
        network.train(inputs, targets, epochs = 10, batchSize = 4)
    }

    fun demonstrateLossFunctions() {
        val mse = MSELoss()
        val crossEntropy = CrossEntropyLoss()
        val prediction = doubleArrayOf(0.25, 0.75)
        val target = doubleArrayOf(0.0, 1.0)
        require(mse.computeLoss(prediction, target) >= 0.0)
        require(crossEntropy.computeLoss(prediction, target) >= 0.0)
    }

    fun demonstrateRegularization() {
        val (inputs, targets) = xorDataset()
        val network = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(2, 6, ReLUActivation(), Random(3)),
                DenseLayer(6, 1, SigmoidActivation(), Random(4))
            ),
            lossFunction = MSELoss(),
            optimizer = SGDOptimizer(learningRate = 0.1, momentum = 0.8),
            regularization = L2Regularization(lambda = 0.001)
        )
        network.train(inputs, targets, epochs = 8, batchSize = 4)
    }

    fun demonstrateLearningRateScheduling() {
        val (inputs, targets) = xorDataset()
        val scheduler = ExponentialDecayScheduler(decayRate = 0.9, decaySteps = 2)
        val optimizer = SGDOptimizer(learningRate = 0.1)
        val network = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(2, 4, ReLUActivation(), Random(5)),
                DenseLayer(4, 1, SigmoidActivation(), Random(6))
            ),
            lossFunction = MSELoss(),
            optimizer = optimizer,
            learningRateScheduler = scheduler
        )
        network.train(inputs, targets, epochs = 6, batchSize = 2)
    }

    fun demonstrateComplexScenario() {
        val (inputs, targets) = xorDataset()
        val network = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(2, 16, ReLUActivation(), Random(7)),
                DenseLayer(16, 8, ReLUActivation(), Random(8)),
                DenseLayer(8, 1, SigmoidActivation(), Random(9))
            ),
            lossFunction = CrossEntropyLoss(),
            optimizer = RMSpropOptimizer(learningRate = 0.02),
            regularization = L1Regularization(lambda = 0.001)
        )
        val history = network.train(inputs, targets, epochs = 12, batchSize = 4)
        require(history.isNotEmpty())
    }
}

object TrainingInfrastructureDemo {
    fun runDemo() {
        val dataset = buildDataset(samples = 20)
        val network = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(1, 12, ReLUActivation(), Random(21)),
                DenseLayer(12, 6, ReLUActivation(), Random(22)),
                DenseLayer(6, 1, LinearActivation(), Random(23))
            ),
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(learningRate = 0.01),
            regularization = L2Regularization(lambda = 0.0005),
            learningRateScheduler = StepDecayScheduler(stepSize = 2, gamma = 0.5)
        )

        // Basic training cycle
        val history = network.train(
            dataset = dataset,
            epochs = 5,
            batchSize = 5,
            validationDataset = null,
            validationFrequency = 1
        )
        require(history.totalEpochs == 5)

        // Evaluation & serialization round trip
        val metrics = network.evaluate(dataset)
        require(metrics.sampleCount == dataset.size())

        val serializer = SimpleModelSerializer()
        val serialized = serializer.serialize(network)
        require(serialized.isNotEmpty())
        val deserialized = serializer.deserialize(serialized)
        require(deserialized.config.inputSize == 1)
    }

    private fun buildDataset(samples: Int): InMemoryDataset {
        val inputs = Array(samples) { i -> doubleArrayOf(i.toDouble() / samples) }
        val targets = Array(samples) { i -> doubleArrayOf((2.0 * i + 1).toDouble() / samples) }
        ensureStrictMonotonic(inputs)
        return InMemoryDataset(inputs, targets)
    }

    private fun ensureStrictMonotonic(inputs: Array<DoubleArray>) {
        for (i in 1 until inputs.size) {
            require(abs(inputs[i][0] - inputs[i - 1][0]) > 1e-9)
        }
    }
}
