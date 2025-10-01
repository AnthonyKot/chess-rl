package com.chessrl.integration.backend

/**
 * Neural network backend enumeration for pluggable NN implementations
 */
enum class BackendType {
    MANUAL,    // FeedforwardNetwork implementation (legacy/manual fallback)
    DL4J,      // DeepLearning4J library backend (default)
    KOTLINDL,  // KotlinDL library backend
    RL4J;      // RL4J reinforcement learning backend
    
    companion object {
        /**
         * Parse backend type from string, case-insensitive
         */
        fun fromString(name: String): BackendType? {
            return values().find { it.name.lowercase() == name.lowercase() }
        }
        
        /**
         * Get default backend type
         */
        fun getDefault(): BackendType = RL4J
    }
}
