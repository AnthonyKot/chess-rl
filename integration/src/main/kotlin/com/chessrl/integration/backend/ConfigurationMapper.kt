package com.chessrl.integration.backend

import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger

/**
 * Maps ChessRLConfig to backend-specific configurations.
 * 
 * Ensures identical hyperparameter values are used across different neural network backends
 * (Manual DQN, RL4J, etc.) for fair performance comparisons.
 */
object ConfigurationMapper {
    
    private val logger = ChessRLLogger.forComponent("ConfigurationMapper")
    
    /**
     * Maps ChessRLConfig to Manual DQN backend configuration.
     * 
     * @param config The unified chess RL configuration
     * @return Manual DQN configuration with mapped parameters
     */
    fun toManualConfig(config: ChessRLConfig): ManualDQNConfig {
        logger.debug("Mapping configuration for Manual DQN backend")
        
        return ManualDQNConfig(
            learningRate = config.learningRate,
            batchSize = config.batchSize,
            gamma = config.gamma,
            targetUpdateFrequency = config.targetUpdateFrequency,
            maxExperienceBuffer = config.maxExperienceBuffer,
            hiddenLayers = config.hiddenLayers,
            explorationRate = config.explorationRate,
            doubleDqn = config.doubleDqn,
            optimizer = config.optimizer,
            seed = config.seed
        )
    }
    
    /**
     * Maps ChessRLConfig to RL4J QLearning configuration.
     * 
     * Note: This method requires RL4J classes to be available on the classpath.
     * Use RL4JAvailability.validateAvailability() before calling this method.
     * 
     * @param config The unified chess RL configuration
     * @return RL4J QLearning configuration with mapped parameters
     * @throws IllegalStateException if RL4J classes are not available
     * @throws IllegalArgumentException if configuration parameters are invalid
     */
    fun toRL4JConfig(config: ChessRLConfig): Any {
        logger.debug("Mapping configuration for RL4J backend")
        
        // Validate configuration parameters first (before checking RL4J availability)
        validateRL4JParameters(config)
        
        // Validate RL4J availability after parameter validation
        RL4JAvailability.validateAvailability()
        
        try {
            // Use reflection to create RL4J configuration to avoid compile-time dependency
            val qLearningClass = Class.forName("org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning")
            
            // Get the builder
            val builderMethod = qLearningClass.getDeclaredClasses()
                .find { it.simpleName == "QLConfiguration" }
                ?.getMethod("builder")
            
            val builder = builderMethod?.invoke(null)
                ?: throw IllegalStateException("Could not create RL4J configuration builder")
            
            // Set parameters using reflection
            val builderClass = builder.javaClass
            
            // Map core parameters
            builderClass.getMethod("seed", Long::class.java)
                .invoke(builder, config.seed ?: System.currentTimeMillis())
            
            builderClass.getMethod("learningRate", Double::class.java)
                .invoke(builder, config.learningRate)
            
            builderClass.getMethod("gamma", Double::class.java)
                .invoke(builder, config.gamma)
            
            // Map exploration parameters (RL4J uses epsilon decay)
            builderClass.getMethod("epsilonNbStep", Int::class.java)
                .invoke(builder, 1000) // Steps to decay epsilon
            
            builderClass.getMethod("minEpsilon", Double::class.java)
                .invoke(builder, config.explorationRate) // Final epsilon value
            
            // Map experience replay parameters
            builderClass.getMethod("expRepMaxSize", Int::class.java)
                .invoke(builder, config.maxExperienceBuffer)
            
            builderClass.getMethod("batchSize", Int::class.java)
                .invoke(builder, config.batchSize)
            
            // Map target network update frequency
            builderClass.getMethod("targetDqnUpdateFreq", Int::class.java)
                .invoke(builder, config.targetUpdateFrequency)
            
            // Build the configuration
            val buildMethod = builderClass.getMethod("build")
            val rl4jConfig = buildMethod.invoke(builder)
            
            logger.info("Successfully mapped configuration for RL4J backend")
            logConfigurationMapping(config, "RL4J")
            
            return rl4jConfig
            
        } catch (e: Exception) {
            logger.error("Failed to create RL4J configuration: ${e.message}")
            throw IllegalStateException("Failed to create RL4J configuration. Ensure RL4J dependencies are available.", e)
        }
    }
    
    /**
     * Validates parameters specifically for RL4J backend.
     * 
     * @param config Configuration to validate
     * @throws IllegalArgumentException if parameters are invalid for RL4J
     */
    private fun validateRL4JParameters(config: ChessRLConfig) {
        val errors = mutableListOf<String>()
        
        // RL4J-specific validation
        if (config.learningRate <= 0.0) {
            errors.add("Learning rate must be positive for RL4J backend, got: ${config.learningRate}")
        }
        
        if (config.learningRate > 1.0) {
            errors.add("Learning rate must be <= 1.0 for RL4J backend, got: ${config.learningRate}")
        }
        
        if (config.batchSize <= 0) {
            errors.add("Batch size must be positive for RL4J backend, got: ${config.batchSize}")
        }
        
        if (config.gamma <= 0.0 || config.gamma >= 1.0) {
            errors.add("Gamma must be in range (0,1) for RL4J backend, got: ${config.gamma}")
        }
        
        if (config.targetUpdateFrequency <= 0) {
            errors.add("Target update frequency must be positive for RL4J backend, got: ${config.targetUpdateFrequency}")
        }
        
        if (config.maxExperienceBuffer <= config.batchSize) {
            errors.add("Experience buffer size (${config.maxExperienceBuffer}) must be larger than batch size (${config.batchSize}) for RL4J backend")
        }
        
        if (config.explorationRate < 0.0 || config.explorationRate > 1.0) {
            errors.add("Exploration rate must be in range [0,1] for RL4J backend, got: ${config.explorationRate}")
        }
        
        // Additional validation for edge cases
        if (config.batchSize > config.maxExperienceBuffer / 2) {
            errors.add("Batch size (${config.batchSize}) should be significantly smaller than experience buffer (${config.maxExperienceBuffer}) for effective training")
        }
        
        if (config.targetUpdateFrequency > 10000) {
            errors.add("Target update frequency (${config.targetUpdateFrequency}) is very high and may slow learning")
        }
        
        if (config.hiddenLayers.isEmpty()) {
            errors.add("Hidden layers cannot be empty for RL4J backend")
        }
        
        if (config.hiddenLayers.any { it <= 0 }) {
            errors.add("All hidden layer sizes must be positive for RL4J backend, got: ${config.hiddenLayers}")
        }
        
        if (config.hiddenLayers.any { it > 2048 }) {
            errors.add("Hidden layer sizes are very large (${config.hiddenLayers.filter { it > 2048 }}) and may cause memory issues")
        }
        
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException("Invalid configuration for RL4J backend: ${errors.joinToString("; ")}")
        }
    }
    
    /**
     * Validates that identical parameters are used across backends.
     * 
     * @param config The configuration to validate
     * @param manualConfig Manual backend configuration
     * @param rl4jConfig RL4J backend configuration (as Any to avoid compile-time dependency)
     * @return List of inconsistencies found
     */
    fun validateParameterConsistency(
        config: ChessRLConfig,
        manualConfig: ManualDQNConfig,
        rl4jConfig: Any
    ): List<String> {
        val inconsistencies = mutableListOf<String>()
        
        // Validate Manual backend consistency
        if (manualConfig.learningRate != config.learningRate) {
            inconsistencies.add("Learning rate mismatch: Manual=${manualConfig.learningRate}, Config=${config.learningRate}")
        }
        
        if (manualConfig.batchSize != config.batchSize) {
            inconsistencies.add("Batch size mismatch: Manual=${manualConfig.batchSize}, Config=${config.batchSize}")
        }
        
        if (manualConfig.gamma != config.gamma) {
            inconsistencies.add("Gamma mismatch: Manual=${manualConfig.gamma}, Config=${config.gamma}")
        }
        
        if (manualConfig.targetUpdateFrequency != config.targetUpdateFrequency) {
            inconsistencies.add("Target update frequency mismatch: Manual=${manualConfig.targetUpdateFrequency}, Config=${config.targetUpdateFrequency}")
        }
        
        if (manualConfig.maxExperienceBuffer != config.maxExperienceBuffer) {
            inconsistencies.add("Experience buffer mismatch: Manual=${manualConfig.maxExperienceBuffer}, Config=${config.maxExperienceBuffer}")
        }
        
        if (manualConfig.hiddenLayers != config.hiddenLayers) {
            inconsistencies.add("Hidden layers mismatch: Manual=${manualConfig.hiddenLayers}, Config=${config.hiddenLayers}")
        }
        
        // Validate RL4J backend consistency (using reflection to avoid compile-time dependency)
        try {
            val rl4jClass = rl4jConfig.javaClass
            
            val learningRate = rl4jClass.getMethod("getLearningRate").invoke(rl4jConfig) as Double
            if (learningRate != config.learningRate) {
                inconsistencies.add("Learning rate mismatch: RL4J=$learningRate, Config=${config.learningRate}")
            }
            
            val gamma = rl4jClass.getMethod("getGamma").invoke(rl4jConfig) as Double
            if (gamma != config.gamma) {
                inconsistencies.add("Gamma mismatch: RL4J=$gamma, Config=${config.gamma}")
            }
            
            val batchSize = rl4jClass.getMethod("getBatchSize").invoke(rl4jConfig) as Int
            if (batchSize != config.batchSize) {
                inconsistencies.add("Batch size mismatch: RL4J=$batchSize, Config=${config.batchSize}")
            }
            
            val expRepMaxSize = rl4jClass.getMethod("getExpRepMaxSize").invoke(rl4jConfig) as Int
            if (expRepMaxSize != config.maxExperienceBuffer) {
                inconsistencies.add("Experience buffer mismatch: RL4J=$expRepMaxSize, Config=${config.maxExperienceBuffer}")
            }
            
            val targetUpdateFreq = rl4jClass.getMethod("getTargetDqnUpdateFreq").invoke(rl4jConfig) as Int
            if (targetUpdateFreq != config.targetUpdateFrequency) {
                inconsistencies.add("Target update frequency mismatch: RL4J=$targetUpdateFreq, Config=${config.targetUpdateFrequency}")
            }
            
        } catch (e: Exception) {
            inconsistencies.add("Could not validate RL4J configuration consistency: ${e.message}")
        }
        
        return inconsistencies
    }
    
    /**
     * Logs configuration mapping for debugging and verification.
     */
    private fun logConfigurationMapping(config: ChessRLConfig, backendName: String) {
        logger.info("Configuration mapped for $backendName backend:")
        logger.info("  Learning Rate: ${config.learningRate}")
        logger.info("  Batch Size: ${config.batchSize}")
        logger.info("  Gamma: ${config.gamma}")
        logger.info("  Target Update Frequency: ${config.targetUpdateFrequency}")
        logger.info("  Max Experience Buffer: ${config.maxExperienceBuffer}")
        logger.info("  Hidden Layers: ${config.hiddenLayers}")
        logger.info("  Exploration Rate: ${config.explorationRate}")
        logger.info("  Double DQN: ${config.doubleDqn}")
        logger.info("  Optimizer: ${config.optimizer}")
        logger.info("  Seed: ${config.seed ?: "random"}")
    }
    
    /**
     * Prints configuration summary for both backends at startup.
     * 
     * @param config The unified configuration
     * @param backendType The selected backend type
     */
    fun printConfigurationSummary(config: ChessRLConfig, backendType: BackendType) {
        println("\n=== Configuration Summary ===")
        println("Selected Backend: ${backendType.name}")
        println("Core Hyperparameters:")
        println("  Learning Rate: ${config.learningRate}")
        println("  Batch Size: ${config.batchSize}")
        println("  Gamma: ${config.gamma}")
        println("  Target Update Frequency: ${config.targetUpdateFrequency}")
        println("  Max Experience Buffer: ${config.maxExperienceBuffer}")
        println("  Hidden Layers: ${config.hiddenLayers.joinToString("x")}")
        println("  Exploration Rate: ${config.explorationRate}")
        println("  Double DQN: ${config.doubleDqn}")
        println("  Optimizer: ${config.optimizer}")
        println("  Seed: ${config.seed ?: "random"}")
        println("==============================\n")
    }
}

/**
 * Manual DQN backend configuration data class.
 * 
 * This represents the configuration structure expected by the manual DQN implementation.
 */
data class ManualDQNConfig(
    val learningRate: Double,
    val batchSize: Int,
    val gamma: Double,
    val targetUpdateFrequency: Int,
    val maxExperienceBuffer: Int,
    val hiddenLayers: List<Int>,
    val explorationRate: Double,
    val doubleDqn: Boolean,
    val optimizer: String,
    val seed: Long?
)