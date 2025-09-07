package com.chessrl.nn

/**
 * Neural Network Package - Core interfaces and data structures
 * This package provides the foundation for neural network operations
 * including training, inference, and model management.
 */

/**
 * Main neural network interface for the chess RL system
 */
interface NeuralNetwork {
    // Forward and backward propagation
    fun forward(input: DoubleArray): DoubleArray
    fun backward(target: DoubleArray): DoubleArray  // Returns loss
    
    // Training utilities  
    fun predict(input: DoubleArray): DoubleArray
    
    // Model management
    fun save(path: String)
    fun load(path: String)
}

/**
 * Layer interface for neural network components
 */
interface Layer {
    fun forward(input: DoubleArray): DoubleArray
    fun backward(gradient: DoubleArray): DoubleArray
}

/**
 * Activation function interface
 */
interface ActivationFunction {
    fun activate(x: Double): Double
    fun derivative(x: Double): Double
}