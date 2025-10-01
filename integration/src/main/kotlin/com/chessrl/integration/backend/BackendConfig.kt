package com.chessrl.integration.backend

/**
 * Backend configuration for network architecture and training parameters
 */
data class BackendConfig(
    val inputSize: Int = 839,
    val outputSize: Int = 4096,
    val hiddenLayers: List<Int> = listOf(512, 256),
    val learningRate: Double = 0.001,
    val l2Regularization: Double = 0.001,
    val lossFunction: String = "huber", // "huber" or "mse"
    val optimizer: String = "adam",     // "adam", "sgd", "rmsprop"
    val batchSize: Int = 32,
    val gradientClipping: Double = 1.0,
    val momentum: Double = 0.0,
    val beta1: Double = 0.9,
    val beta2: Double = 0.999,
    val epsilon: Double = 1e-8,
    val weightInitType: String = "he",  // "he", "xavier", "uniform"
    val seed: Long = 42L                // Random seed for reproducible initialization
)
