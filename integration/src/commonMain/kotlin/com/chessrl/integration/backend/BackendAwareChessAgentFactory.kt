package com.chessrl.integration.backend

import com.chessrl.integration.*
import com.chessrl.integration.backend.rl4j.*
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.rl.*

/**
 * Extended ChessAgentFactory with backend support
 * Routes agent creation based on neural network backend type while preserving existing manual workflow
 */
object BackendAwareChessAgentFactory {
    
    private val logger = ChessRLLogger.forComponent("BackendAwareChessAgentFactory")
    private val optimizerSupport: Map<BackendType, Set<String>> = mapOf(
        BackendType.MANUAL to setOf("adam", "sgd", "rmsprop"),
        BackendType.DL4J to setOf("adam", "sgd", "rmsprop"),
        BackendType.KOTLINDL to setOf("adam"), // placeholder until backend implemented
        BackendType.RL4J to setOf("adam", "sgd", "rmsprop") // RL4J supports same optimizers as DL4J
    )
    
    /**
     * Create a DQN agent with specified backend
     * Falls back to manual backend if specified backend is not available
     */
    fun createDQNAgent(
        backendType: BackendType,
        backendConfig: BackendConfig,
        agentConfig: ChessAgentConfig,
        enableDoubleDQN: Boolean = false,
        replayType: String = "UNIFORM",
        gamma: Double = 0.99
    ): ChessAgent {
        // Validate and select backend with fallback
        val (selectedBackend, warnings) = BackendSelector.selectBackendWithFallback(backendType)
        
        // Log any warnings about backend fallback
        warnings.forEach { warning ->
            logger.warn(warning)
            println("Warning: $warning")
        }

        validateBackendCapabilities(selectedBackend, backendConfig).forEach { warning ->
            logger.warn(warning)
            println("Warning: $warning")
        }
        
        return when (selectedBackend) {
            BackendType.MANUAL -> createManualDQNAgent(backendConfig, agentConfig, enableDoubleDQN, replayType, gamma)
            BackendType.DL4J -> createDl4jDQNAgent(backendConfig, agentConfig, enableDoubleDQN, replayType, gamma)
            BackendType.KOTLINDL -> createKotlinDlDQNAgent(backendConfig, agentConfig, enableDoubleDQN, replayType, gamma)
            BackendType.RL4J -> createRL4JDQNAgent(backendConfig, agentConfig, enableDoubleDQN, replayType, gamma)
        }
    }
    
    /**
     * Create a seeded DQN agent with specified backend
     */
    fun createSeededDQNAgent(
        backendType: BackendType,
        backendConfig: BackendConfig,
        agentConfig: ChessAgentConfig,
        seedManager: SeedManager = SeedManager.getInstance(),
        enableDoubleDQN: Boolean = false,
        replayType: String = "UNIFORM",
        gamma: Double = 0.99
    ): ChessAgent {
        // Validate and select backend with fallback
        val (selectedBackend, warnings) = BackendSelector.selectBackendWithFallback(backendType)
        
        // Log any warnings about backend fallback
        warnings.forEach { warning ->
            logger.warn(warning)
            println("Warning: $warning")
        }

        validateBackendCapabilities(selectedBackend, backendConfig).forEach { warning ->
            logger.warn(warning)
            println("Warning: $warning")
        }
        
        return when (selectedBackend) {
            BackendType.MANUAL -> createSeededManualDQNAgent(backendConfig, agentConfig, seedManager, enableDoubleDQN, replayType, gamma)
            BackendType.DL4J -> createSeededDl4jDQNAgent(backendConfig, agentConfig, seedManager, enableDoubleDQN, replayType, gamma)
            BackendType.KOTLINDL -> createSeededKotlinDlDQNAgent(backendConfig, agentConfig, seedManager, enableDoubleDQN, replayType, gamma)
            BackendType.RL4J -> createSeededRL4JDQNAgent(backendConfig, agentConfig, seedManager, enableDoubleDQN, replayType, gamma)
        }
    }
    
    /**
     * Create manual backend DQN agent (existing implementation)
     */
    private fun createManualDQNAgent(
        backendConfig: BackendConfig,
        agentConfig: ChessAgentConfig,
        enableDoubleDQN: Boolean,
        replayType: String,
        gamma: Double
    ): ChessAgent {
        logger.info("Creating DQN agent with manual backend")
        
        return RealChessAgentFactory.createRealDQNAgent(
            inputSize = backendConfig.inputSize,
            outputSize = backendConfig.outputSize,
            hiddenLayers = backendConfig.hiddenLayers,
            learningRate = backendConfig.learningRate,
            explorationRate = agentConfig.explorationRate,
            batchSize = agentConfig.batchSize,
            maxBufferSize = agentConfig.maxBufferSize,
            targetUpdateFrequency = agentConfig.targetUpdateFrequency,
            doubleDqn = enableDoubleDQN,
            replayType = replayType,
            gamma = gamma,
            lossFunction = backendConfig.lossFunction,
            optimizer = backendConfig.optimizer,
            l2Regularization = backendConfig.l2Regularization,
            beta1 = backendConfig.beta1,
            beta2 = backendConfig.beta2,
            epsilon = backendConfig.epsilon,
            momentum = backendConfig.momentum,
            weightInitType = backendConfig.weightInitType
        ).let { realAgent ->
            ChessAgentAdapter(realAgent, agentConfig)
        }
    }
    
    /**
     * Create seeded manual backend DQN agent (existing implementation)
     */
    private fun createSeededManualDQNAgent(
        backendConfig: BackendConfig,
        agentConfig: ChessAgentConfig,
        seedManager: SeedManager,
        enableDoubleDQN: Boolean,
        replayType: String,
        gamma: Double
    ): ChessAgent {
        logger.info("Creating seeded DQN agent with manual backend")
        
        return RealChessAgentFactory.createSeededDQNAgent(
            inputSize = backendConfig.inputSize,
            outputSize = backendConfig.outputSize,
            hiddenLayers = backendConfig.hiddenLayers,
            learningRate = backendConfig.learningRate,
            explorationRate = agentConfig.explorationRate,
            batchSize = agentConfig.batchSize,
            maxBufferSize = agentConfig.maxBufferSize,
            neuralNetworkRandom = seedManager.getNeuralNetworkRandom(),
            explorationRandom = seedManager.getExplorationRandom(),
            replayBufferRandom = seedManager.getReplayBufferRandom(),
            replayType = replayType,
            targetUpdateFrequency = agentConfig.targetUpdateFrequency,
            gamma = gamma,
            doubleDqn = enableDoubleDQN,
            weightInitType = backendConfig.weightInitType,
            lossFunction = backendConfig.lossFunction,
            optimizer = backendConfig.optimizer,
            l2Regularization = backendConfig.l2Regularization,
            beta1 = backendConfig.beta1,
            beta2 = backendConfig.beta2,
            epsilon = backendConfig.epsilon,
            momentum = backendConfig.momentum
        ).let { realAgent ->
            ChessAgentAdapter(realAgent, agentConfig)
        }
    }
    
    /**
     * Create DL4J backend DQN agent
     * Uses DL4J NetworkAdapter with identical DQN algorithm configuration as manual backend
     */
    private fun createDl4jDQNAgent(
        backendConfig: BackendConfig,
        agentConfig: ChessAgentConfig,
        enableDoubleDQN: Boolean,
        replayType: String,
        gamma: Double
    ): ChessAgent {
        logger.info("Creating DQN agent with DL4J backend")
        
        // Create DL4J network adapters for Q-network and target network
        val qNetworkAdapter = JvmNetworkAdapterFactory.createDl4jAdapter(backendConfig)
        val targetNetworkAdapter = JvmNetworkAdapterFactory.createDl4jAdapter(backendConfig)
        
        // Create experience replay buffer (identical to manual backend)
        val experienceReplay: ExperienceReplay<DoubleArray, Int> = when (replayType.uppercase()) {
            "PRIORITIZED" -> PrioritizedExperienceBuffer(maxSize = agentConfig.maxBufferSize)
            else -> CircularExperienceBuffer(maxSize = agentConfig.maxBufferSize)
        }
        
        // Create DQN algorithm with identical configuration as manual backend
        val dqnAlgorithm = DQNAlgorithm(
            qNetwork = qNetworkAdapter,
            targetNetwork = targetNetworkAdapter,
            experienceReplay = experienceReplay,
            gamma = gamma,
            targetUpdateFrequency = agentConfig.targetUpdateFrequency,
            batchSize = agentConfig.batchSize,
            doubleDQN = enableDoubleDQN
        )
        
        // Create exploration strategy (identical to manual backend)
        val explorationStrategy = EpsilonGreedyStrategy<Int>(agentConfig.explorationRate)
        
        // Create DL4J-specific agent implementation
        val dl4jAgent = Dl4jChessAgent(
            algorithm = dqnAlgorithm,
            explorationStrategy = explorationStrategy,
            qNetworkAdapter = qNetworkAdapter,
            targetNetworkAdapter = targetNetworkAdapter,
            config = agentConfig
        )
        
        return Dl4jChessAgentAdapter(dl4jAgent, agentConfig)
    }
    
    /**
     * Create seeded DL4J backend DQN agent
     * Uses DL4J NetworkAdapter with seeded initialization and identical DQN algorithm configuration
     */
    private fun createSeededDl4jDQNAgent(
        backendConfig: BackendConfig,
        agentConfig: ChessAgentConfig,
        seedManager: SeedManager,
        enableDoubleDQN: Boolean,
        replayType: String,
        gamma: Double
    ): ChessAgent {
        logger.info("Creating seeded DQN agent with DL4J backend")
        
        // Create seeded DL4J network adapters for Q-network and target network
        // Note: DL4J uses its own seeding mechanism, but we ensure consistent initialization
        val qNetworkAdapter = JvmNetworkAdapterFactory.createSeededDl4jAdapter(backendConfig, seedManager)
        val targetNetworkAdapter = JvmNetworkAdapterFactory.createSeededDl4jAdapter(backendConfig, seedManager)
        
        // Create seeded experience replay buffer (identical to manual backend)
        val experienceReplay: ExperienceReplay<DoubleArray, Int> = when (replayType.uppercase()) {
            "PRIORITIZED" -> PrioritizedExperienceBuffer(maxSize = agentConfig.maxBufferSize)
            else -> SeededCircularExperienceBuffer(
                maxSize = agentConfig.maxBufferSize,
                random = seedManager.getReplayBufferRandom()
            )
        }
        
        // Create DQN algorithm with identical configuration as manual backend
        val dqnAlgorithm = DQNAlgorithm(
            qNetwork = qNetworkAdapter,
            targetNetwork = targetNetworkAdapter,
            experienceReplay = experienceReplay,
            gamma = gamma,
            targetUpdateFrequency = agentConfig.targetUpdateFrequency,
            batchSize = agentConfig.batchSize,
            doubleDQN = enableDoubleDQN
        )
        
        // Create seeded exploration strategy (identical to manual backend)
        val explorationStrategy = SeededEpsilonGreedyStrategy<Int>(
            epsilon = agentConfig.explorationRate,
            random = seedManager.getExplorationRandom()
        )
        
        // Create seeded DL4J-specific agent implementation
        val dl4jAgent = SeededDl4jChessAgent(
            algorithm = dqnAlgorithm,
            explorationStrategy = explorationStrategy,
            qNetworkAdapter = qNetworkAdapter,
            targetNetworkAdapter = targetNetworkAdapter,
            config = agentConfig,
            seedManager = seedManager
        )
        
        return SeededDl4jChessAgentAdapter(dl4jAgent, agentConfig)
    }
    
    /**
     * Create KotlinDL backend DQN agent (placeholder - will be implemented in later tasks)
     */
    @Suppress("UNUSED_PARAMETER")
    private fun createKotlinDlDQNAgent(
        backendConfig: BackendConfig,
        agentConfig: ChessAgentConfig,
        enableDoubleDQN: Boolean,
        replayType: String,
        gamma: Double
    ): ChessAgent {
        logger.info("Creating DQN agent with KotlinDL backend")
        
        // TODO: Implement KotlinDL backend in task 17
        // For now, this is a placeholder that will throw an error
        throw UnsupportedOperationException("KotlinDL backend not yet implemented. Use --nn manual instead.")
    }
    
    /**
     * Create seeded KotlinDL backend DQN agent (placeholder - will be implemented in later tasks)
     */
    @Suppress("UNUSED_PARAMETER")
    private fun createSeededKotlinDlDQNAgent(
        backendConfig: BackendConfig,
        agentConfig: ChessAgentConfig,
        seedManager: SeedManager,
        enableDoubleDQN: Boolean,
        replayType: String,
        gamma: Double
    ): ChessAgent {
        logger.info("Creating seeded DQN agent with KotlinDL backend")
        
        // TODO: Implement KotlinDL backend in task 17
        // For now, this is a placeholder that will throw an error
        throw UnsupportedOperationException("KotlinDL backend not yet implemented. Use --nn manual instead.")
    }
    
    /**
     * Create backend configuration from existing parameters
     */
    fun createBackendConfig(
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        inputSize: Int = ChessStateEncoder.TOTAL_FEATURES,
        outputSize: Int = 4096,
        batchSize: Int = 32,
        lossFunction: String = "huber",
        optimizer: String = "adam",
        l2Regularization: Double = 0.001,
        beta1: Double = 0.9,
        beta2: Double = 0.999,
        epsilon: Double = 1e-8,
        momentum: Double = 0.0,
        weightInitType: String = "he"
    ): BackendConfig {
        return BackendConfig(
            inputSize = inputSize,
            outputSize = outputSize,
            hiddenLayers = hiddenLayers,
            learningRate = learningRate,
            batchSize = batchSize,
            lossFunction = lossFunction,
            optimizer = optimizer.lowercase(),
            l2Regularization = l2Regularization,
            beta1 = beta1,
            beta2 = beta2,
            epsilon = epsilon,
            momentum = momentum,
            weightInitType = weightInitType
        )
    }

    /**
     * Create RL4J backend DQN agent
     * Uses RL4J QLearning with ChessMDP wrapper
     */
    private fun createRL4JDQNAgent(
        backendConfig: BackendConfig,
        agentConfig: ChessAgentConfig,
        enableDoubleDQN: Boolean,
        replayType: String,
        gamma: Double
    ): ChessAgent {
        logger.info("Creating DQN agent with RL4J backend")
        
        // Validate RL4J availability
        RL4JAvailability.validateAvailability()
        
        if (enableDoubleDQN) {
            logger.debug("RL4J backend placeholder currently ignores enableDoubleDQN=true")
        }
        if (!replayType.equals("UNIFORM", ignoreCase = true)) {
            logger.debug("RL4J backend placeholder does not yet support replay type '$replayType'")
        }

        createRL4JConfiguration(backendConfig, agentConfig, gamma).also {
            logger.debug("Generated RL4J configuration placeholder: ${it::class.simpleName}")
        }
        
        // Create RL4J agent
        val rl4jAgent = RL4JChessAgent(
            config = agentConfig
        )
        rl4jAgent.initialize()
        
        return RL4JChessAgentAdapter(rl4jAgent, agentConfig)
    }
    
    /**
     * Create seeded RL4J backend DQN agent
     * Uses RL4J QLearning with ChessMDP wrapper and seeded initialization
     */
    private fun createSeededRL4JDQNAgent(
        backendConfig: BackendConfig,
        agentConfig: ChessAgentConfig,
        seedManager: SeedManager,
        enableDoubleDQN: Boolean,
        replayType: String,
        gamma: Double
    ): ChessAgent {
        logger.info("Creating seeded DQN agent with RL4J backend")
        
        // Validate RL4J availability
        RL4JAvailability.validateAvailability()
        
        if (enableDoubleDQN) {
            logger.debug("RL4J backend placeholder currently ignores enableDoubleDQN=true (seeded path)")
        }
        if (!replayType.equals("UNIFORM", ignoreCase = true)) {
            logger.debug("RL4J backend placeholder does not yet support replay type '$replayType' (seeded path)")
        }

        createSeededRL4JConfiguration(backendConfig, agentConfig, gamma, seedManager).also {
            logger.debug("Generated seeded RL4J configuration placeholder: ${it::class.simpleName}")
        }
        
        // Create seeded RL4J agent
        val rl4jAgent = RL4JChessAgent(
            config = agentConfig
        )
        rl4jAgent.initialize()
        
        return RL4JChessAgentAdapter(rl4jAgent, agentConfig)
    }

    /**
     * Create RL4J configuration from backend config.
     */
    private fun createRL4JConfiguration(
        backendConfig: BackendConfig,
        agentConfig: ChessAgentConfig,
        gamma: Double
    ): Any {
        try {
            // Use reflection to create RL4J configuration
            val qLearningClass = Class.forName("org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning")
            val configClass = qLearningClass.getDeclaredClasses()
                .find { it.simpleName == "QLConfiguration" }
                ?: throw IllegalStateException("Could not find QLConfiguration class")
            
            val builderMethod = configClass.getMethod("builder")
            val builder = builderMethod.invoke(null)
            val builderClass = builder.javaClass
            
            // Set configuration parameters
            builderClass.getMethod("seed", Long::class.java)
                .invoke(builder, System.currentTimeMillis())
            
            builderClass.getMethod("learningRate", Double::class.java)
                .invoke(builder, backendConfig.learningRate)
            
            builderClass.getMethod("gamma", Double::class.java)
                .invoke(builder, gamma)
            
            builderClass.getMethod("epsilonNbStep", Int::class.java)
                .invoke(builder, 1000) // Steps to decay epsilon
            
            builderClass.getMethod("minEpsilon", Double::class.java)
                .invoke(builder, agentConfig.explorationRate)
            
            builderClass.getMethod("expRepMaxSize", Int::class.java)
                .invoke(builder, agentConfig.maxBufferSize)
            
            builderClass.getMethod("batchSize", Int::class.java)
                .invoke(builder, agentConfig.batchSize)
            
            builderClass.getMethod("targetDqnUpdateFreq", Int::class.java)
                .invoke(builder, agentConfig.targetUpdateFrequency)
            
            // Build the configuration
            val buildMethod = builderClass.getMethod("build")
            return buildMethod.invoke(builder)
            
        } catch (e: Exception) {
            logger.error("Failed to create RL4J configuration: ${e.message}")
            throw RuntimeException("Failed to create RL4J configuration", e)
        }
    }
    
    /**
     * Create seeded RL4J configuration from backend config.
     */
    private fun createSeededRL4JConfiguration(
        backendConfig: BackendConfig,
        agentConfig: ChessAgentConfig,
        gamma: Double,
        seedManager: SeedManager
    ): Any {
        try {
            // Use reflection to create RL4J configuration
            val qLearningClass = Class.forName("org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning")
            val configClass = qLearningClass.getDeclaredClasses()
                .find { it.simpleName == "QLConfiguration" }
                ?: throw IllegalStateException("Could not find QLConfiguration class")
            
            val builderMethod = configClass.getMethod("builder")
            val builder = builderMethod.invoke(null)
            val builderClass = builder.javaClass
            
            // Set configuration parameters with seed
            builderClass.getMethod("seed", Long::class.java)
                .invoke(builder, seedManager.getNeuralNetworkRandom().nextLong())
            
            builderClass.getMethod("learningRate", Double::class.java)
                .invoke(builder, backendConfig.learningRate)
            
            builderClass.getMethod("gamma", Double::class.java)
                .invoke(builder, gamma)
            
            builderClass.getMethod("epsilonNbStep", Int::class.java)
                .invoke(builder, 1000) // Steps to decay epsilon
            
            builderClass.getMethod("minEpsilon", Double::class.java)
                .invoke(builder, agentConfig.explorationRate)
            
            builderClass.getMethod("expRepMaxSize", Int::class.java)
                .invoke(builder, agentConfig.maxBufferSize)
            
            builderClass.getMethod("batchSize", Int::class.java)
                .invoke(builder, agentConfig.batchSize)
            
            builderClass.getMethod("targetDqnUpdateFreq", Int::class.java)
                .invoke(builder, agentConfig.targetUpdateFrequency)
            
            // Build the configuration
            val buildMethod = builderClass.getMethod("build")
            return buildMethod.invoke(builder)
            
        } catch (e: Exception) {
            logger.error("Failed to create seeded RL4J configuration: ${e.message}")
            throw RuntimeException("Failed to create seeded RL4J configuration", e)
        }
    }

    private fun validateBackendCapabilities(backendType: BackendType, backendConfig: BackendConfig): List<String> {
        val warnings = mutableListOf<String>()
        val optimizerName = backendConfig.optimizer.lowercase()
        val supportedOptimizers = optimizerSupport[backendType]
        if (supportedOptimizers != null && optimizerName !in supportedOptimizers) {
            warnings += "Optimizer '$optimizerName' is not supported by ${backendType.name.lowercase()} backend. Supported optimizers: ${supportedOptimizers.joinToString(", ")}"
        }
        return warnings
    }
}
