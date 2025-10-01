package com.chessrl.integration.backend

import com.chessrl.rl.NeuralNetwork
import com.chessrl.rl.TrainableNeuralNetwork
import com.chessrl.rl.SynchronizableNetwork

/**
 * Unified neural network adapter interface that implements all RL framework interfaces.
 * This interface provides a standardized way to interact with different neural network
 * backends while maintaining compatibility with the existing RL algorithms.
 */
interface NetworkAdapter : NeuralNetwork, TrainableNeuralNetwork, SynchronizableNetwork {
    
    // Core neural network operations (inherited from NeuralNetwork)
    override fun forward(input: DoubleArray): DoubleArray
    override fun backward(target: DoubleArray): DoubleArray
    override fun predict(input: DoubleArray): DoubleArray = forward(input)
    
    // Batch training for efficiency (inherited from TrainableNeuralNetwork)
    override fun trainBatch(inputs: Array<DoubleArray>, targets: Array<DoubleArray>): Double
    
    // Weight synchronization for target networks (inherited from SynchronizableNetwork)
    override fun copyWeightsTo(target: NeuralNetwork)
    
    // Model persistence
    fun save(path: String)
    fun load(path: String)
    
    // Backend identification and configuration
    fun getBackendName(): String
    fun getConfig(): BackendConfig
    
    // Performance and debugging
    fun getParameterCount(): Long
    fun getMemoryUsage(): Long
    fun validateGradients(): Boolean
}