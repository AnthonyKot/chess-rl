package com.chessrl.integration

/**
 * Comprehensive training configuration system with seed management,
 * CLI argument parsing, and configuration file support.
 */
data class TrainingConfiguration(
    // Seed configuration
    val seed: Long? = null,
    val deterministicMode: Boolean = false,
    val enableSeedLogging: Boolean = true,
    
    // Training parameters
    val episodes: Int = 1000,
    val maxStepsPerEpisode: Int = 200,
    val batchSize: Int = 64,
    val learningRate: Double = 0.001,
    val explorationRate: Double = 0.1,
    
    // Neural network configuration
    val hiddenLayers: List<Int> = listOf(512, 256, 128),
    val activationFunction: String = "relu",
    val optimizer: String = "adam",
    val weightInitialization: String = "he",
    
    // Experience replay configuration
    val maxBufferSize: Int = 50000,
    val replayBatchSize: Int = 32,
    val replaySamplingStrategy: String = "uniform",
    
    // Checkpointing configuration
    val checkpointInterval: Int = 1000,
    val maxCheckpoints: Int = 20,
    val checkpointDirectory: String = "checkpoints",
    val enableCheckpointValidation: Boolean = true,
    
    // Monitoring configuration
    val progressReportInterval: Int = 100,
    val enableRealTimeMonitoring: Boolean = true,
    val metricsOutputFormat: String = "console", // console, csv, json
    val metricsOutputFile: String? = null,
    
    // Environment configuration
    val winReward: Double = 1.0,
    val lossReward: Double = -1.0,
    val drawReward: Double = 0.0,
    val enablePositionRewards: Boolean = false,
    
    // Performance configuration
    val parallelGames: Int = 1,
    val enableJvmOptimizations: Boolean = true,
    val memoryManagementMode: String = "auto", // auto, conservative, aggressive
    
    // Debugging configuration
    val enableDebugMode: Boolean = false,
    val verboseLogging: Boolean = false,
    val enableTrainingValidation: Boolean = true,
    
    // Additional metadata
    val configurationName: String = "default",
    val description: String = "",
    val tags: List<String> = emptyList()
) {
    
    /**
     * Validate configuration parameters
     */
    fun validate(): ConfigurationValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate basic parameters
        if (episodes <= 0) errors.add("Episodes must be positive")
        if (maxStepsPerEpisode <= 0) errors.add("Max steps per episode must be positive")
        if (batchSize <= 0) errors.add("Batch size must be positive")
        if (learningRate <= 0.0 || learningRate > 1.0) errors.add("Learning rate must be between 0 and 1")
        if (explorationRate < 0.0 || explorationRate > 1.0) errors.add("Exploration rate must be between 0 and 1")
        
        // Validate neural network configuration
        if (hiddenLayers.isEmpty()) warnings.add("No hidden layers specified")
        if (hiddenLayers.any { it <= 0 }) errors.add("All hidden layer sizes must be positive")
        
        // Validate supported options
        val supportedActivations = setOf("relu", "sigmoid", "tanh", "linear")
        if (activationFunction !in supportedActivations) {
            errors.add("Unsupported activation function: $activationFunction")
        }
        
        val supportedOptimizers = setOf("sgd", "adam", "rmsprop")
        if (optimizer !in supportedOptimizers) {
            errors.add("Unsupported optimizer: $optimizer")
        }
        
        val supportedWeightInit = setOf("xavier", "he", "uniform", "zero")
        if (weightInitialization !in supportedWeightInit) {
            errors.add("Unsupported weight initialization: $weightInitialization")
        }
        
        val supportedSamplingStrategies = setOf("uniform", "recent", "mixed")
        if (replaySamplingStrategy !in supportedSamplingStrategies) {
            errors.add("Unsupported replay sampling strategy: $replaySamplingStrategy")
        }
        
        val supportedMetricsFormats = setOf("console", "csv", "json")
        if (metricsOutputFormat !in supportedMetricsFormats) {
            errors.add("Unsupported metrics output format: $metricsOutputFormat")
        }
        
        val supportedMemoryModes = setOf("auto", "conservative", "aggressive")
        if (memoryManagementMode !in supportedMemoryModes) {
            errors.add("Unsupported memory management mode: $memoryManagementMode")
        }
        
        // Validate buffer and batch sizes
        if (replayBatchSize > maxBufferSize) {
            warnings.add("Replay batch size ($replayBatchSize) is larger than buffer size ($maxBufferSize)")
        }
        
        if (batchSize > maxBufferSize) {
            warnings.add("Training batch size ($batchSize) is larger than buffer size ($maxBufferSize)")
        }
        
        // Validate checkpoint configuration
        if (checkpointInterval <= 0) errors.add("Checkpoint interval must be positive")
        if (maxCheckpoints <= 0) errors.add("Max checkpoints must be positive")
        
        // Validate reward configuration
        if (winReward <= lossReward) {
            warnings.add("Win reward ($winReward) should be greater than loss reward ($lossReward)")
        }
        
        // Performance warnings
        if (parallelGames > 8) {
            warnings.add("High parallel games count ($parallelGames) may impact performance")
        }
        
        if (hiddenLayers.any { it > 1024 }) {
            warnings.add("Large hidden layers may require significant memory")
        }
        
        return ConfigurationValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Get seed configuration
     */
    fun getSeedConfig(): SeedConfig {
        return SeedConfig(
            seed = seed,
            deterministicMode = deterministicMode,
            enableLogging = enableSeedLogging
        )
    }
    
    /**
     * Create a copy with updated seed
     */
    fun withSeed(newSeed: Long): TrainingConfiguration {
        return copy(seed = newSeed, deterministicMode = true)
    }
    
    /**
     * Create a copy with deterministic mode enabled
     */
    fun withDeterministicMode(): TrainingConfiguration {
        return copy(deterministicMode = true)
    }
    
    /**
     * Get configuration summary for logging
     */
    fun getSummary(): ConfigurationSummary {
        return ConfigurationSummary(
            name = configurationName,
            description = description,
            seedConfig = getSeedConfig(),
            trainingParams = TrainingParams(
                episodes = episodes,
                batchSize = batchSize,
                learningRate = learningRate,
                explorationRate = explorationRate
            ),
            networkConfig = NetworkConfig(
                hiddenLayers = hiddenLayers,
                activationFunction = activationFunction,
                optimizer = optimizer,
                weightInitialization = weightInitialization
            ),
            bufferConfig = BufferConfig(
                maxBufferSize = maxBufferSize,
                replayBatchSize = replayBatchSize,
                samplingStrategy = replaySamplingStrategy
            ),
            monitoringConfig = TrainingMonitoringConfig(
                progressReportInterval = progressReportInterval,
                enableRealTimeMonitoring = enableRealTimeMonitoring,
                metricsOutputFormat = metricsOutputFormat
            )
        )
    }
}

/**
 * Configuration validation result
 */
data class ConfigurationValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Seed-specific configuration
 */
data class SeedConfig(
    val seed: Long?,
    val deterministicMode: Boolean,
    val enableLogging: Boolean
)

/**
 * Configuration summary components
 */
data class ConfigurationSummary(
    val name: String,
    val description: String,
    val seedConfig: SeedConfig,
    val trainingParams: TrainingParams,
    val networkConfig: NetworkConfig,
    val bufferConfig: BufferConfig,
    val monitoringConfig: TrainingMonitoringConfig
)

data class TrainingParams(
    val episodes: Int,
    val batchSize: Int,
    val learningRate: Double,
    val explorationRate: Double
)

data class NetworkConfig(
    val hiddenLayers: List<Int>,
    val activationFunction: String,
    val optimizer: String,
    val weightInitialization: String
)

data class BufferConfig(
    val maxBufferSize: Int,
    val replayBatchSize: Int,
    val samplingStrategy: String
)

data class TrainingMonitoringConfig(
    val progressReportInterval: Int,
    val enableRealTimeMonitoring: Boolean,
    val metricsOutputFormat: String
)

/**
 * Configuration builder for programmatic configuration creation
 */
class TrainingConfigurationBuilder {
    private var config = TrainingConfiguration()
    
    fun seed(seed: Long) = apply { config = config.copy(seed = seed, deterministicMode = true) }
    fun deterministicMode(enabled: Boolean = true) = apply { config = config.copy(deterministicMode = enabled) }
    fun episodes(episodes: Int) = apply { config = config.copy(episodes = episodes) }
    fun batchSize(size: Int) = apply { config = config.copy(batchSize = size) }
    fun learningRate(rate: Double) = apply { config = config.copy(learningRate = rate) }
    fun explorationRate(rate: Double) = apply { config = config.copy(explorationRate = rate) }
    fun hiddenLayers(layers: List<Int>) = apply { config = config.copy(hiddenLayers = layers) }
    fun optimizer(optimizer: String) = apply { config = config.copy(optimizer = optimizer) }
    fun checkpointInterval(interval: Int) = apply { config = config.copy(checkpointInterval = interval) }
    fun enableDebugMode(enabled: Boolean = true) = apply { config = config.copy(enableDebugMode = enabled) }
    fun name(name: String) = apply { config = config.copy(configurationName = name) }
    fun description(desc: String) = apply { config = config.copy(description = desc) }
    
    fun build(): TrainingConfiguration = config
}

/**
 * CLI argument parser for training configuration
 */
object ConfigurationParser {
    
    /**
     * Parse CLI arguments into training configuration
     */
    fun parseCliArguments(args: Array<String>): TrainingConfiguration {
        val builder = TrainingConfigurationBuilder()
        
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--seed" -> {
                    if (i + 1 < args.size) {
                        builder.seed(args[++i].toLong())
                    }
                }
                "--deterministic" -> {
                    builder.deterministicMode(true)
                }
                "--episodes" -> {
                    if (i + 1 < args.size) {
                        builder.episodes(args[++i].toInt())
                    }
                }
                "--batch-size" -> {
                    if (i + 1 < args.size) {
                        builder.batchSize(args[++i].toInt())
                    }
                }
                "--learning-rate" -> {
                    if (i + 1 < args.size) {
                        builder.learningRate(args[++i].toDouble())
                    }
                }
                "--exploration-rate" -> {
                    if (i + 1 < args.size) {
                        builder.explorationRate(args[++i].toDouble())
                    }
                }
                "--optimizer" -> {
                    if (i + 1 < args.size) {
                        builder.optimizer(args[++i])
                    }
                }
                "--checkpoint-interval" -> {
                    if (i + 1 < args.size) {
                        builder.checkpointInterval(args[++i].toInt())
                    }
                }
                "--debug" -> {
                    builder.enableDebugMode(true)
                }
                "--name" -> {
                    if (i + 1 < args.size) {
                        builder.name(args[++i])
                    }
                }
                "--description" -> {
                    if (i + 1 < args.size) {
                        builder.description(args[++i])
                    }
                }
            }
            i++
        }
        
        return builder.build()
    }
    
    /**
     * Create configuration for deterministic testing
     */
    fun createTestConfiguration(seed: Long = 12345L): TrainingConfiguration {
        return TrainingConfigurationBuilder()
            .seed(seed)
            .deterministicMode(true)
            .episodes(10)
            .batchSize(16)
            .learningRate(0.01)
            .explorationRate(0.05)
            .checkpointInterval(5)
            .enableDebugMode(true)
            .name("test-config")
            .description("Deterministic configuration for testing")
            .build()
    }
    
    /**
     * Create default production configuration
     */
    fun createProductionConfiguration(): TrainingConfiguration {
        return TrainingConfigurationBuilder()
            .episodes(10000)
            .batchSize(64)
            .learningRate(0.001)
            .explorationRate(0.1)
            .hiddenLayers(listOf(512, 256, 128))
            .optimizer("adam")
            .checkpointInterval(1000)
            .name("production-config")
            .description("Default production configuration")
            .build()
    }
    
    /**
     * Print usage information
     */
    fun printUsage() {
        println("Chess RL Training Configuration Options:")
        println("  --seed <long>              Set random seed for deterministic runs")
        println("  --deterministic            Enable deterministic mode")
        println("  --episodes <int>           Number of training episodes")
        println("  --batch-size <int>         Training batch size")
        println("  --learning-rate <double>   Learning rate for optimizer")
        println("  --exploration-rate <double> Exploration rate for epsilon-greedy")
        println("  --optimizer <string>       Optimizer type (sgd, adam, rmsprop)")
        println("  --checkpoint-interval <int> Episodes between checkpoints")
        println("  --debug                    Enable debug mode")
        println("  --name <string>            Configuration name")
        println("  --description <string>     Configuration description")
        println()
        println("Example:")
        println("  --seed 12345 --deterministic --episodes 1000 --batch-size 32")
    }
}
