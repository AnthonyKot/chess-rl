package com.chessrl.integration.backend

import com.chessrl.integration.SeedManager
import com.chessrl.integration.logging.ChessRLLogger

/**
 * JVM-specific network adapter factory for creating platform-specific adapters
 */
object JvmNetworkAdapterFactory {
    
    private val logger = ChessRLLogger.forComponent("JvmNetworkAdapterFactory")
    
    /**
     * Create network adapter based on backend type and configuration
     * Falls back to manual backend if alternative backend fails
     */
    fun createAdapter(backendType: BackendType, config: BackendConfig): NetworkAdapter {
        return try {
            when (backendType) {
                BackendType.MANUAL -> {
                    logger.debug("Creating manual network adapter")
                    ManualNetworkAdapter(config)
                }
                BackendType.DL4J -> {
                    logger.info("Creating DL4J network adapter")
                    Dl4jNetworkAdapter(config)
                }
                BackendType.KOTLINDL -> {
                    logger.warn("KotlinDL backend not yet implemented, falling back to manual")
                    ManualNetworkAdapter(config)
                }
            }
        } catch (e: Exception) {
            if (backendType != BackendType.MANUAL) {
                logger.warn("Failed to initialize ${backendType.name} backend: ${e.message}")
                logger.info("Falling back to manual backend")
                ManualNetworkAdapter(config)
            } else {
                throw e // Manual backend failure is fatal
            }
        }
    }
    
    /**
     * Create adapter with explicit fallback handling
     */
    fun createAdapterWithFallback(
        primaryType: BackendType, 
        config: BackendConfig,
        fallbackType: BackendType = BackendType.MANUAL
    ): Pair<NetworkAdapter, BackendType> {
        return try {
            val adapter = createAdapter(primaryType, config)
            logger.info("Successfully created ${primaryType.name} adapter")
            Pair(adapter, primaryType)
        } catch (e: Exception) {
            logger.warn("Primary backend ${primaryType.name} failed: ${e.message}")
            if (primaryType != fallbackType) {
                logger.info("Attempting fallback to ${fallbackType.name}")
                val fallbackAdapter = createAdapter(fallbackType, config)
                Pair(fallbackAdapter, fallbackType)
            } else {
                throw e
            }
        }
    }
    
    /**
     * Create DL4J network adapter with specified configuration
     */
    fun createDl4jAdapter(config: BackendConfig): NetworkAdapter {
        logger.info("Creating DL4J network adapter with config: ${config.hiddenLayers}")
        return Dl4jNetworkAdapter(config)
    }
    
    /**
     * Create seeded DL4J network adapter with deterministic initialization
     */
    fun createSeededDl4jAdapter(config: BackendConfig, seedManager: SeedManager): NetworkAdapter {
        logger.info("Creating seeded DL4J network adapter")
        // DL4J uses its own seeding mechanism, but we can ensure consistent seed
        val seededConfig = config.copy(seed = seedManager.getNeuralNetworkRandom().nextLong())
        return Dl4jNetworkAdapter(seededConfig)
    }
    
    /**
     * Create manual network adapter with specified configuration
     */
    fun createManualAdapter(config: BackendConfig): NetworkAdapter {
        logger.debug("Creating manual network adapter")
        return ManualNetworkAdapter(config)
    }
    
    /**
     * Create seeded manual network adapter with deterministic initialization
     */
    fun createSeededManualAdapter(config: BackendConfig, seedManager: SeedManager): NetworkAdapter {
        logger.debug("Creating seeded manual network adapter")
        return ManualNetworkAdapter(config, seedManager.getNeuralNetworkRandom())
    }
    
    /**
     * Validate adapter functionality with comprehensive checks
     */
    fun validateAdapter(adapter: NetworkAdapter): ValidationResult {
        val issues = mutableListOf<String>()
        
        try {
            logger.debug("Validating ${adapter.getBackendName()} adapter")
            
            // Test forward pass with correct dimensions
            val testInput = DoubleArray(adapter.getConfig().inputSize) { kotlin.random.Random.nextDouble() }
            val output = adapter.forward(testInput)
            
            if (output.size != adapter.getConfig().outputSize) {
                issues.add("Output size ${output.size} doesn't match expected ${adapter.getConfig().outputSize}")
            }
            
            // Check for finite values
            if (output.any { !it.isFinite() }) {
                issues.add("Forward pass produced non-finite values")
            }
            
            // Test batch training
            val batchSize = 4
            val inputs = Array(batchSize) { 
                DoubleArray(adapter.getConfig().inputSize) { kotlin.random.Random.nextDouble() } 
            }
            val targets = Array(batchSize) { 
                DoubleArray(adapter.getConfig().outputSize) { kotlin.random.Random.nextDouble() } 
            }
            
            val loss = adapter.trainBatch(inputs, targets)
            if (!loss.isFinite()) {
                issues.add("Training batch produced non-finite loss: $loss")
            }
            
            // Test weight synchronization if possible
            try {
                val targetAdapter = createAdapter(
                    BackendType.valueOf(adapter.getBackendName().uppercase()), 
                    adapter.getConfig()
                )
                adapter.copyWeightsTo(targetAdapter)
                logger.debug("Weight synchronization test passed")
            } catch (e: Exception) {
                issues.add("Weight synchronization failed: ${e.message}")
            }
            
        } catch (e: Exception) {
            issues.add("Validation failed with exception: ${e.message}")
        }
        
        val result = ValidationResult(
            isValid = issues.isEmpty(),
            backend = adapter.getBackendName(),
            issues = issues
        )
        
        if (result.isValid) {
            logger.info("Adapter validation passed for ${adapter.getBackendName()}")
        } else {
            logger.warn("Adapter validation failed for ${adapter.getBackendName()}: ${issues.joinToString(", ")}")
        }
        
        return result
    }
}