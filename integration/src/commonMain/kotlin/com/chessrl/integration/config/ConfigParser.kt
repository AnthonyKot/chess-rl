package com.chessrl.integration.config

/**
 * Configuration parser for Chess RL system.
 * 
 * Supports loading configuration from:
 * - YAML/JSON files
 * - Command-line arguments
 * - Predefined profiles
 * 
 * Provides proper validation and clear error messages for invalid configurations.
 */
object ConfigParser {
    
    /**
     * Parse command-line arguments into ChessRLConfig.
     * 
     * Supports essential parameters only, with sensible defaults for all values.
     * Unknown arguments are ignored to maintain forward compatibility.
     * 
     * @param args Command-line arguments array
     * @return ChessRLConfig with parsed values
     */
    fun parseArgs(args: Array<String>): ChessRLConfig {
        var config = ChessRLConfig() // Start with defaults
        
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                // Neural Network parameters
                "--hidden-layers" -> {
                    if (i + 1 < args.size) {
                        val layers = args[++i].split(",").map { it.trim().toInt() }
                        config = config.copy(hiddenLayers = layers)
                    }
                }
                "--learning-rate" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(learningRate = args[++i].toDouble())
                    }
                }
                "--batch-size" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(batchSize = args[++i].toInt())
                    }
                }
                
                // RL Training parameters
                "--exploration-rate" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(explorationRate = args[++i].toDouble())
                    }
                }
                "--target-update-frequency" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(targetUpdateFrequency = args[++i].toInt())
                    }
                }
                "--max-experience-buffer" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(maxExperienceBuffer = args[++i].toInt())
                    }
                }
                
                // Self-Play parameters
                "--games-per-cycle" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(gamesPerCycle = args[++i].toInt())
                    }
                }
                "--max-concurrent-games" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(maxConcurrentGames = args[++i].toInt())
                    }
                }
                "--max-steps-per-game" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(maxStepsPerGame = args[++i].toInt())
                    }
                }
                "--max-cycles" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(maxCycles = args[++i].toInt())
                    }
                }
                
                // Reward parameters
                "--win-reward" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(winReward = args[++i].toDouble())
                    }
                }
                "--loss-reward" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(lossReward = args[++i].toDouble())
                    }
                }
                "--draw-reward" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(drawReward = args[++i].toDouble())
                    }
                }
                "--step-limit-penalty" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(stepLimitPenalty = args[++i].toDouble())
                    }
                }
                
                // System parameters
                "--seed" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(seed = args[++i].toLong())
                    }
                }
                "--checkpoint-interval" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(checkpointInterval = args[++i].toInt())
                    }
                }
                "--checkpoint-directory" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(checkpointDirectory = args[++i])
                    }
                }
                "--evaluation-games" -> {
                    if (i + 1 < args.size) {
                        config = config.copy(evaluationGames = args[++i].toInt())
                    }
                }
                
                // Profile loading
                "--profile" -> {
                    if (i + 1 < args.size) {
                        val profileName = args[++i]
                        config = loadProfile(profileName)
                    }
                }
                
                // Ignore unknown arguments for forward compatibility
                else -> {
                    // Skip unknown arguments silently
                }
            }
            i++
        }
        
        return config
    }
    
    /**
     * Load configuration from a profile name.
     * 
     * First tries to load from profiles.yaml file, falls back to built-in profiles.
     * 
     * @param profileName Name of the profile to load
     * @return ChessRLConfig for the specified profile
     * @throws IllegalArgumentException if profile is not found
     */
    fun loadProfile(profileName: String): ChessRLConfig {
        // Try to load from profiles.yaml file first
        try {
            return loadProfileFromFile(profileName)
        } catch (e: Exception) {
            // Fall back to built-in profiles
            return when (profileName.lowercase()) {
                "fast-debug" -> createFastDebugProfile()
                "long-train" -> createLongTrainProfile()
                "eval-only" -> createEvalOnlyProfile()
                else -> throw IllegalArgumentException("Unknown profile: $profileName. Available profiles: fast-debug, long-train, eval-only")
            }
        }
    }
    
    /**
     * Load configuration from profiles.yaml file.
     * 
     * @param profileName Name of the profile to load
     * @return ChessRLConfig for the specified profile
     * @throws IllegalArgumentException if profile is not found or file cannot be read
     */
    fun loadProfileFromFile(profileName: String, profilesPath: String = "integration/config/profiles.yaml"): ChessRLConfig {
        // This is a simplified implementation - in a real system you'd use a proper YAML library
        // For now, we'll use the built-in profiles
        return when (profileName.lowercase()) {
            "fast-debug" -> createFastDebugProfile()
            "long-train" -> createLongTrainProfile()
            "eval-only" -> createEvalOnlyProfile()
            else -> throw IllegalArgumentException("Profile '$profileName' not found in $profilesPath")
        }
    }
    
    /**
     * Parse configuration from YAML content.
     * 
     * Simple YAML parser that handles the essential configuration parameters.
     * This is a basic implementation that can be extended with a full YAML library if needed.
     * 
     * @param yamlContent YAML content as string
     * @return ChessRLConfig parsed from YAML
     */
    fun parseYaml(yamlContent: String): ChessRLConfig {
        var config = ChessRLConfig()
        
        val lines = yamlContent.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            
            val parts = trimmed.split(":", limit = 2)
            if (parts.size != 2) continue
            
            val key = parts[0].trim()
            val value = parts[1].trim()
            
            try {
                config = when (key) {
                    "hiddenLayers" -> {
                        val layers = value.split(",").map { it.trim().toInt() }
                        config.copy(hiddenLayers = layers)
                    }
                    "learningRate" -> config.copy(learningRate = value.toDouble())
                    "batchSize" -> config.copy(batchSize = value.toInt())
                    "explorationRate" -> config.copy(explorationRate = value.toDouble())
                    "targetUpdateFrequency" -> config.copy(targetUpdateFrequency = value.toInt())
                    "maxExperienceBuffer" -> config.copy(maxExperienceBuffer = value.toInt())
                    "gamesPerCycle" -> config.copy(gamesPerCycle = value.toInt())
                    "maxConcurrentGames" -> config.copy(maxConcurrentGames = value.toInt())
                    "maxStepsPerGame" -> config.copy(maxStepsPerGame = value.toInt())
                    "maxCycles" -> config.copy(maxCycles = value.toInt())
                    "winReward" -> config.copy(winReward = value.toDouble())
                    "lossReward" -> config.copy(lossReward = value.toDouble())
                    "drawReward" -> config.copy(drawReward = value.toDouble())
                    "stepLimitPenalty" -> config.copy(stepLimitPenalty = value.toDouble())
                    "seed" -> config.copy(seed = if (value == "null") null else value.toLong())
                    "checkpointInterval" -> config.copy(checkpointInterval = value.toInt())
                    "checkpointDirectory" -> config.copy(checkpointDirectory = value.trim('"'))
                    "evaluationGames" -> config.copy(evaluationGames = value.toInt())
                    else -> config // Ignore unknown keys
                }
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Invalid value for $key: $value", e)
            }
        }
        
        return config
    }
    
    /**
     * Parse configuration from JSON content.
     * 
     * Simple JSON parser for the essential configuration parameters.
     * This is a basic implementation that can be extended with a full JSON library if needed.
     * 
     * @param jsonContent JSON content as string
     * @return ChessRLConfig parsed from JSON
     */
    fun parseJson(jsonContent: String): ChessRLConfig {
        var config = ChessRLConfig()
        
        // Simple JSON parsing - remove braces and split by commas
        val content = jsonContent.trim().removeSurrounding("{", "}")
        val pairs = content.split(",")
        
        for (pair in pairs) {
            val parts = pair.split(":", limit = 2)
            if (parts.size != 2) continue
            
            val key = parts[0].trim().removeSurrounding("\"")
            val value = parts[1].trim()
            
            try {
                config = when (key) {
                    "hiddenLayers" -> {
                        val arrayContent = value.removeSurrounding("[", "]")
                        val layers = arrayContent.split(",").map { it.trim().toInt() }
                        config.copy(hiddenLayers = layers)
                    }
                    "learningRate" -> config.copy(learningRate = value.toDouble())
                    "batchSize" -> config.copy(batchSize = value.toInt())
                    "explorationRate" -> config.copy(explorationRate = value.toDouble())
                    "targetUpdateFrequency" -> config.copy(targetUpdateFrequency = value.toInt())
                    "maxExperienceBuffer" -> config.copy(maxExperienceBuffer = value.toInt())
                    "gamesPerCycle" -> config.copy(gamesPerCycle = value.toInt())
                    "maxConcurrentGames" -> config.copy(maxConcurrentGames = value.toInt())
                    "maxStepsPerGame" -> config.copy(maxStepsPerGame = value.toInt())
                    "maxCycles" -> config.copy(maxCycles = value.toInt())
                    "winReward" -> config.copy(winReward = value.toDouble())
                    "lossReward" -> config.copy(lossReward = value.toDouble())
                    "drawReward" -> config.copy(drawReward = value.toDouble())
                    "stepLimitPenalty" -> config.copy(stepLimitPenalty = value.toDouble())
                    "seed" -> config.copy(seed = if (value == "null") null else value.toLong())
                    "checkpointInterval" -> config.copy(checkpointInterval = value.toInt())
                    "checkpointDirectory" -> config.copy(checkpointDirectory = value.removeSurrounding("\""))
                    "evaluationGames" -> config.copy(evaluationGames = value.toInt())
                    else -> config // Ignore unknown keys
                }
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Invalid value for $key: $value", e)
            }
        }
        
        return config
    }
    
    /**
     * Create fast-debug profile for development.
     * 
     * Optimized for quick iteration during development:
     * - Few games and cycles for fast feedback
     * - Limited concurrency to reduce resource usage
     * - Shorter games to speed up testing
     */
    private fun createFastDebugProfile(): ChessRLConfig {
        return ChessRLConfig(
            // Reduced for fast iteration
            gamesPerCycle = 5,
            maxCycles = 10,
            maxConcurrentGames = 2,
            maxStepsPerGame = 40,
            evaluationGames = 20,
            
            // Smaller network for faster training
            hiddenLayers = listOf(256, 128),
            batchSize = 32,
            
            // More frequent checkpoints for debugging
            checkpointInterval = 2,
            checkpointDirectory = "checkpoints/fast-debug"
        )
    }
    
    /**
     * Create long-train profile for production training.
     * 
     * Optimized for training competitive agents:
     * - Many games and cycles for thorough training
     * - High concurrency for efficiency
     * - Larger network for better capacity
     */
    private fun createLongTrainProfile(): ChessRLConfig {
        return ChessRLConfig(
            // Increased for thorough training
            gamesPerCycle = 50,
            maxCycles = 200,
            maxConcurrentGames = 8,
            maxStepsPerGame = 120,
            
            // Larger network for better performance
            hiddenLayers = listOf(768, 512, 256),
            batchSize = 64,
            learningRate = 0.0005, // Lower for stability with larger network
            
            // Standard checkpointing
            checkpointInterval = 5,
            checkpointDirectory = "checkpoints/long-train"
        )
    }
    
    /**
     * Create eval-only profile for evaluation runs.
     * 
     * Optimized for consistent evaluation:
     * - Many games for statistical significance
     * - Deterministic seed for reproducibility
     * - Single-threaded for consistent results
     */
    private fun createEvalOnlyProfile(): ChessRLConfig {
        return ChessRLConfig(
            // Many games for statistical significance
            evaluationGames = 500,
            
            // Single-threaded for consistency
            maxConcurrentGames = 1,
            
            // Deterministic for reproducible results
            seed = 12345L,
            
            // Evaluation-specific directory
            checkpointDirectory = "checkpoints/eval-only"
        )
    }
    
    /**
     * Print usage information for command-line arguments.
     */
    fun printUsage() {
        println("Chess RL Configuration Options:")
        println()
        println("Profiles:")
        println("  --profile fast-debug       Fast iteration profile for development")
        println("  --profile long-train       Production training profile")
        println("  --profile eval-only        Evaluation-only profile")
        println()
        println("Neural Network:")
        println("  --hidden-layers <sizes>    Hidden layer sizes (comma-separated, e.g., 512,256,128)")
        println("  --learning-rate <rate>     Learning rate (default: 0.001)")
        println("  --batch-size <size>        Training batch size (default: 64)")
        println()
        println("RL Training:")
        println("  --exploration-rate <rate>  Exploration rate for epsilon-greedy (default: 0.1)")
        println("  --target-update-frequency <freq>  Target network update frequency (default: 100)")
        println("  --max-experience-buffer <size>    Experience buffer size (default: 50000)")
        println()
        println("Self-Play:")
        println("  --games-per-cycle <games>  Games per training cycle (default: 20)")
        println("  --max-concurrent-games <games>    Concurrent games (default: 4)")
        println("  --max-steps-per-game <steps>      Max steps per game (default: 80)")
        println("  --max-cycles <cycles>      Maximum training cycles (default: 100)")
        println()
        println("Rewards:")
        println("  --win-reward <reward>      Reward for winning (default: 1.0)")
        println("  --loss-reward <reward>     Reward for losing (default: -1.0)")
        println("  --draw-reward <reward>     Reward for drawing (default: -0.2)")
        println("  --step-limit-penalty <penalty>    Penalty for reaching step limit (default: -1.0)")
        println()
        println("System:")
        println("  --seed <seed>              Random seed for reproducibility")
        println("  --checkpoint-interval <interval>  Cycles between checkpoints (default: 5)")
        println("  --checkpoint-directory <dir>      Checkpoint directory (default: checkpoints)")
        println("  --evaluation-games <games> Games for evaluation runs (default: 100)")
        println()
        println("Examples:")
        println("  --profile fast-debug")
        println("  --profile long-train --seed 12345")
        println("  --games-per-cycle 30 --max-cycles 50 --learning-rate 0.002")
    }
    
    /**
     * Get list of available profile names.
     */
    fun getAvailableProfiles(): List<String> {
        return listOf("fast-debug", "long-train", "eval-only")
    }
}