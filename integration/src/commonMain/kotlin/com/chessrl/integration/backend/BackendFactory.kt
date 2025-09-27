package com.chessrl.integration.backend

import com.chessrl.integration.backend.rl4j.RL4JLearningBackend
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger

/**
 * Factory for creating learning backends based on backend type.
 * 
 * This factory handles backend-specific configuration and initialization,
 * including error handling for missing dependencies.
 */
object BackendFactory {
    
    private val logger = ChessRLLogger.forComponent("BackendFactory")
    
    /**
     * Create a learning backend based on the specified type.
     * 
     * @param backendType The type of backend to create
     * @param config The chess RL configuration
     * @return A learning backend instance
     * @throws IllegalStateException if the backend is not available
     */
    fun createBackend(backendType: BackendType, config: ChessRLConfig): LearningBackend {
        logger.info("Creating learning backend: ${backendType.name}")
        
        // Validate backend availability
        val validation = BackendSelector.validateBackendAvailability(backendType)
        if (!validation.isValid) {
            val errorMessage = "Backend ${backendType.name} is not available: ${validation.issues.joinToString(", ")}"
            logger.error(errorMessage)
            throw IllegalStateException(errorMessage)
        }
        
        return when (backendType) {
            BackendType.MANUAL -> {
                logger.info("Creating DQN backend with manual neural network")
                DqnLearningBackend(BackendType.MANUAL)
            }
            BackendType.DL4J -> {
                logger.info("Creating DQN backend with DL4J neural network")
                DqnLearningBackend(BackendType.DL4J)
            }
            BackendType.KOTLINDL -> {
                logger.info("Creating DQN backend with KotlinDL neural network")
                DqnLearningBackend(BackendType.KOTLINDL)
            }
            BackendType.RL4J -> {
                logger.info("Creating RL4J backend")
                RL4JLearningBackend()
            }
        }
    }
    
    /**
     * Create a backend with fallback handling.
     * If the primary backend is not available, falls back to manual backend.
     * 
     * @param primaryType The preferred backend type
     * @param config The chess RL configuration
     * @param fallbackType The fallback backend type (default: MANUAL)
     * @return A pair of (selected backend, list of warnings)
     */
    fun createBackendWithFallback(
        primaryType: BackendType,
        config: ChessRLConfig,
        fallbackType: BackendType = BackendType.MANUAL
    ): Pair<LearningBackend, List<String>> {
        val warnings = mutableListOf<String>()
        
        try {
            val backend = createBackend(primaryType, config)
            logger.info("Successfully created ${primaryType.name} backend")
            return Pair(backend, warnings)
        } catch (e: IllegalStateException) {
            warnings.add("Primary backend ${primaryType.name} not available: ${e.message}")
            logger.warn("Primary backend ${primaryType.name} failed, attempting fallback to ${fallbackType.name}")
            
            if (primaryType != fallbackType) {
                try {
                    val fallbackBackend = createBackend(fallbackType, config)
                    warnings.add("Using fallback backend: ${fallbackType.name}")
                    logger.info("Successfully created fallback ${fallbackType.name} backend")
                    return Pair(fallbackBackend, warnings)
                } catch (fallbackException: IllegalStateException) {
                    warnings.add("Fallback backend ${fallbackType.name} also not available: ${fallbackException.message}")
                    logger.error("Both primary and fallback backends failed")
                    throw RuntimeException("No backend available: ${warnings.joinToString("; ")}")
                }
            } else {
                logger.error("Primary backend same as fallback, no alternatives available")
                throw RuntimeException("Backend ${primaryType.name} not available and no fallback configured")
            }
        }
    }
    
    /**
     * Get available backend types on the current system.
     * 
     * @return List of backend types that are available
     */
    fun getAvailableBackends(): List<BackendType> {
        return BackendType.values().filter { backendType ->
            BackendSelector.validateBackendAvailability(backendType).isValid
        }
    }
    
    /**
     * Check if a specific backend type is available.
     * 
     * @param backendType The backend type to check
     * @return true if the backend is available, false otherwise
     */
    fun isBackendAvailable(backendType: BackendType): Boolean {
        return BackendSelector.validateBackendAvailability(backendType).isValid
    }
}