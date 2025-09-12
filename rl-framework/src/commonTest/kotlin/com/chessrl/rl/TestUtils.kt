package com.chessrl.rl

import kotlin.random.Random

/**
 * Simple neural network implementation for testing
 */
class SimpleNeuralNetwork(
    private val inputSize: Int,
    private val outputSize: Int,
    private val random: Random = Random.Default
) : NeuralNetwork {
    
    // Simple weight matrix for testing
    private val weights = Array(outputSize) { DoubleArray(inputSize) { random.nextDouble(-0.1, 0.1) } }
    private val biases = DoubleArray(outputSize) { 0.0 }
    
    override fun forward(input: DoubleArray): DoubleArray {
        require(input.size == inputSize) { "Input size mismatch" }
        
        return DoubleArray(outputSize) { i ->
            var sum = biases[i]
            for (j in input.indices) {
                sum += weights[i][j] * input[j]
            }
            // Simple activation (tanh)
            kotlin.math.tanh(sum)
        }
    }
    
    override fun backward(target: DoubleArray): DoubleArray {
        // Simplified backward pass - just return MSE loss
        // For testing, we'll just return a loss value without proper backpropagation
        require(target.size == outputSize) { "Target size mismatch" }
        
        // Return a dummy loss value
        return doubleArrayOf(0.5)
    }
}