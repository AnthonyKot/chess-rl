package com.chessrl.integration.backend

/**
 * Neural network backend enumeration for pluggable NN implementations
 */
enum class BackendType {
    MANUAL,    // FeedforwardNetwork implementation (legacy/manual fallback)
    DL4J,      // DeepLearning4J library backend (default)
    KOTLINDL;  // KotlinDL library backend
    
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
        fun getDefault(): BackendType = DL4J
    }
}
