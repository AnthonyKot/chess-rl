package com.chessrl.integration.backend

import com.chessrl.integration.logging.ChessRLLogger

/**
 * Backend selector and factory with graceful fallback to manual backend
 */
object BackendSelector {
    
    private val logger = ChessRLLogger.forComponent("BackendSelector")
    
    /**
     * Parse backend selection from CLI arguments
     * Looks for --nn flag followed by backend name
     */
    fun parseBackendFromArgs(args: Array<String>): BackendType {
        val backendIndex = args.indexOf("--nn")
        if (backendIndex >= 0 && backendIndex + 1 < args.size) {
            val backendName = args[backendIndex + 1]
            val backendType = BackendType.fromString(backendName)
            if (backendType != null) {
                logger.info("Backend selected from CLI: ${backendType.name}")
                return backendType
            } else {
                logger.warn("Unknown backend type '$backendName', falling back to manual")
            }
        }
        return BackendType.getDefault()
    }
    
    /**
     * Parse backend selection from profile configuration
     */
    fun parseBackendFromProfile(profileConfig: Map<String, Any>): BackendType {
        val backendName = profileConfig["nnBackend"] as? String
        return if (backendName != null) {
            val backendType = BackendType.fromString(backendName)
            if (backendType != null) {
                logger.info("Backend selected from profile: ${backendType.name}")
                backendType
            } else {
                logger.warn("Unknown backend type '$backendName' in profile, falling back to manual")
                BackendType.getDefault()
            }
        } else {
            BackendType.getDefault()
        }
    }
    
    /**
     * Determine backend type with precedence: CLI args > profile config > default
     */
    fun selectBackend(args: Array<String>, profileConfig: Map<String, Any> = emptyMap()): BackendType {
        // CLI args have highest precedence
        val cliBackend = parseBackendFromArgs(args)
        if (cliBackend != BackendType.getDefault() || args.contains("--nn")) {
            return cliBackend
        }
        
        // Profile config has second precedence
        val profileBackend = parseBackendFromProfile(profileConfig)
        if (profileBackend != BackendType.getDefault()) {
            return profileBackend
        }
        
        // Default fallback
        return BackendType.getDefault()
    }
    
    /**
     * Validate backend availability and log warnings for unavailable backends
     */
    fun validateBackendAvailability(backendType: BackendType): ValidationResult {
        val issues = mutableListOf<String>()
        
        when (backendType) {
            BackendType.MANUAL -> {
                // Manual backend is always available
                logger.debug("Manual backend validation: OK")
            }
            BackendType.DL4J -> {
                // Check if DL4J classes are available
                try {
                    Class.forName("org.deeplearning4j.nn.multilayer.MultiLayerNetwork")
                    logger.debug("DL4J backend validation: OK")
                } catch (e: ClassNotFoundException) {
                    issues.add("DL4J classes not found on classpath. Add deeplearning4j-core dependency.")
                    logger.warn("DL4J backend not available: ${e.message}")
                }
            }
            BackendType.KOTLINDL -> {
                // Check if KotlinDL classes are available
                try {
                    Class.forName("org.jetbrains.kotlinx.dl.api.core.Sequential")
                    logger.debug("KotlinDL backend validation: OK")
                } catch (e: ClassNotFoundException) {
                    issues.add("KotlinDL classes not found on classpath. Add kotlindl dependency.")
                    logger.warn("KotlinDL backend not available: ${e.message}")
                }
            }
            BackendType.RL4J -> {
                // Check if RL4J classes are available using the availability checker
                if (RL4JAvailability.isAvailable()) {
                    logger.debug("RL4J backend validation: OK")
                } else {
                    issues.add("RL4J classes not found on classpath. Set enableRL4J=true in gradle.properties and rebuild.")
                    logger.warn("RL4J backend not available")
                }
            }
        }
        
        return ValidationResult(
            isValid = issues.isEmpty(),
            backend = backendType.name,
            issues = issues
        )
    }
    
    /**
     * Select backend with fallback handling
     * If primary backend is not available, falls back to manual backend
     */
    fun selectBackendWithFallback(
        primaryType: BackendType,
        fallbackType: BackendType = BackendType.MANUAL
    ): Pair<BackendType, List<String>> {
        val validation = validateBackendAvailability(primaryType)
        
        return if (validation.isValid) {
            logger.info("Using ${primaryType.name} backend")
            Pair(primaryType, emptyList())
        } else {
            val warnings = mutableListOf<String>()
            warnings.add("Primary backend ${primaryType.name} not available: ${validation.issues.joinToString(", ")}")
            
            if (primaryType != fallbackType) {
                val fallbackValidation = validateBackendAvailability(fallbackType)
                if (fallbackValidation.isValid) {
                    warnings.add("Falling back to ${fallbackType.name} backend")
                    logger.warn("Primary backend ${primaryType.name} failed, using fallback ${fallbackType.name}")
                    Pair(fallbackType, warnings)
                } else {
                    warnings.add("Fallback backend ${fallbackType.name} also not available: ${fallbackValidation.issues.joinToString(", ")}")
                    logger.error("Both primary and fallback backends unavailable")
                    throw RuntimeException("No neural network backend available: ${warnings.joinToString("; ")}")
                }
            } else {
                logger.error("Manual backend validation failed - this should not happen")
                throw RuntimeException("Manual backend not available: ${validation.issues.joinToString(", ")}")
            }
        }
    }
}

/**
 * Validation result for backend availability
 */
data class ValidationResult(
    val isValid: Boolean,
    val backend: String,
    val issues: List<String>
)