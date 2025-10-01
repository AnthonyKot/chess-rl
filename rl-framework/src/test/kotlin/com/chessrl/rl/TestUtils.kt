package com.chessrl.rl

import kotlin.random.Random

/**
 * Simple neural network implementation for testing
 */
class SimpleNeuralNetwork(
    private val inputSize: Int,
    private val outputSize: Int,
    private val random: Random = Random.Default,
    private var learningRate: Double = 0.05
) : TrainableNeuralNetwork, SynchronizableNetwork {

    private val weights = Array(outputSize) { DoubleArray(inputSize) { random.nextDouble(-0.1, 0.1) } }
    private val biases = DoubleArray(outputSize) { 0.0 }

    override fun forward(input: DoubleArray): DoubleArray {
        require(input.size == inputSize) { "Input size mismatch" }

        return DoubleArray(outputSize) { i ->
            var sum = biases[i]
            for (j in input.indices) {
                sum += weights[i][j] * input[j]
            }
            sum
        }
    }

    override fun backward(target: DoubleArray): DoubleArray = DoubleArray(outputSize) { 0.0 }

    override fun trainBatch(inputs: Array<DoubleArray>, targets: Array<DoubleArray>): Double {
        require(inputs.size == targets.size) { "Batch size mismatch" }
        var totalLoss = 0.0

        for (i in inputs.indices) {
            val input = inputs[i]
            val target = targets[i]
            require(input.size == inputSize) { "Input size mismatch" }
            require(target.size == outputSize) { "Target size mismatch" }

            val prediction = forward(input)
            val error = DoubleArray(outputSize) { idx -> target[idx] - prediction[idx] }

            // Simple SGD update for each output neuron
            for (out in 0 until outputSize) {
                biases[out] += learningRate * error[out]
                for (j in 0 until inputSize) {
                    weights[out][j] += learningRate * error[out] * input[j]
                }
                totalLoss += 0.5 * error[out] * error[out]
            }
        }

        return totalLoss / inputs.size
    }

    override fun copyWeightsTo(target: NeuralNetwork) {
        if (target !is SimpleNeuralNetwork) return
        for (i in 0 until outputSize) {
            target.biases[i] = biases[i]
            for (j in 0 until inputSize) {
                target.weights[i][j] = weights[i][j]
            }
        }
    }

    fun setLearningRate(rate: Double) {
        learningRate = rate
    }
}
