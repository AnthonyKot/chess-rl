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
        var config = ChessRLConfig()

        var i = 0
        while (i < args.size) {
            val flag = args[i]
            if (!flag.startsWith("--")) {
                i++
                continue
            }

            if (flag == "--profile") {
                if (i + 1 < args.size) {
                    config = loadProfile(args[++i])
                }
            } else {
                if (i + 1 < args.size) {
                    val value = args[++i]
                    flagToKey(flag)?.let { key ->
                        config = applyConfigSetting(config, key, value)
                    }
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
     * Note: This is a placeholder for file-based profile loading.
     * The actual file parsing is handled by ProfilesLoader in the JVM module.
     * This method currently falls back to built-in profiles.
     * 
     * @param profileName Name of the profile to load
     * @param profilesPath Path to profiles file (currently unused - falls back to built-in)
     * @return ChessRLConfig for the specified profile
     * @throws IllegalArgumentException if profile is not found
     */
    fun loadProfileFromFile(profileName: String, @Suppress("UNUSED_PARAMETER") profilesPath: String = "integration/profiles.yaml"): ChessRLConfig {
        // TODO: Integrate with ProfilesLoader for actual file reading
        // For now, fall back to built-in profiles since ProfilesLoader is JVM-only
        // and this is in commonMain for multiplatform support
        return when (profileName.lowercase()) {
            "fast-debug" -> createFastDebugProfile()
            "long-train" -> createLongTrainProfile()
            "eval-only" -> createEvalOnlyProfile()
            else -> throw IllegalArgumentException("Profile '$profileName' not found. Available built-in profiles: fast-debug, long-train, eval-only")
        }
    }
    
    /**
     * Parse configuration from YAML content.
     * 
     * Handles both flat key-value pairs and array syntax for hiddenLayers.
     * This is a basic implementation focused on the essential configuration parameters.
     * 
     * @param yamlContent YAML content as string
     * @return ChessRLConfig parsed from YAML
     */
    fun parseYaml(yamlContent: String): ChessRLConfig {
        var config = ChessRLConfig()

        yamlContent.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach

            val parts = trimmed.split(":", limit = 2)
            if (parts.size != 2) return@forEach

            val key = parts[0].trim()
            val value = parts[1].trim()

            config = applyConfigSetting(config, key, value)
        }

        return config
    }
    
    /**
     * Parse hidden layers from various formats:
     * - Array format: [512, 256, 128]
     * - Comma-separated: 512,256,128
     * - Space-separated: 512 256 128
     */
    private fun parseHiddenLayers(value: String): List<Int> {
        val cleanValue = value.trim()
        
        return when {
            // Array format: [512, 256, 128]
            cleanValue.startsWith("[") && cleanValue.endsWith("]") -> {
                val arrayContent = cleanValue.removeSurrounding("[", "]")
                arrayContent.split(",").map { it.trim().toInt() }
            }
            // Comma-separated: 512,256,128
            "," in cleanValue -> {
                cleanValue.split(",").map { it.trim().toInt() }
            }
            // Space-separated: 512 256 128
            " " in cleanValue -> {
                cleanValue.split("\\s+".toRegex()).map { it.trim().toInt() }
            }
            // Single value: 512
            else -> {
                listOf(cleanValue.toInt())
            }
        }
    }

    private fun applyConfigSetting(config: ChessRLConfig, key: String, rawValue: String): ChessRLConfig {
        val value = rawValue.trim()

        return when (key) {
            "hiddenLayers" -> config.copy(hiddenLayers = parseHiddenLayers(value))
            "learningRate" -> config.copy(learningRate = value.toDoubleOrThrow(key))
            "batchSize" -> config.copy(batchSize = value.toIntOrThrow(key))
            "explorationRate" -> config.copy(explorationRate = value.toDoubleOrThrow(key))
            "targetUpdateFrequency" -> config.copy(targetUpdateFrequency = value.toIntOrThrow(key))
            "maxExperienceBuffer" -> config.copy(maxExperienceBuffer = value.toIntOrThrow(key))
            "gamesPerCycle" -> config.copy(gamesPerCycle = value.toIntOrThrow(key))
            "maxConcurrentGames" -> config.copy(maxConcurrentGames = value.toIntOrThrow(key))
            "maxStepsPerGame" -> config.copy(maxStepsPerGame = value.toIntOrThrow(key))
            "maxCycles" -> config.copy(maxCycles = value.toIntOrThrow(key))
            "winReward" -> config.copy(winReward = value.toDoubleOrThrow(key))
            "lossReward" -> config.copy(lossReward = value.toDoubleOrThrow(key))
            "drawReward" -> config.copy(drawReward = value.toDoubleOrThrow(key))
            "stepLimitPenalty" -> config.copy(stepLimitPenalty = value.toDoubleOrThrow(key))
            "seed" -> config.copy(seed = if (value.equals("null", ignoreCase = true)) null else value.toLongOrThrow(key))
            "checkpointInterval" -> config.copy(checkpointInterval = value.toIntOrThrow(key))
            "checkpointDirectory" -> config.copy(checkpointDirectory = value.trim('"'))
            "evaluationGames" -> config.copy(evaluationGames = value.toIntOrThrow(key))
            else -> config
        }
    }

    private fun String.toIntOrThrow(key: String): Int = toIntOrNull()
        ?: throw IllegalArgumentException("Invalid value for $key: $this")

    private fun String.toDoubleOrThrow(key: String): Double = toDoubleOrNull()
        ?: throw IllegalArgumentException("Invalid value for $key: $this")

    private fun String.toLongOrThrow(key: String): Long = toLongOrNull()
        ?: throw IllegalArgumentException("Invalid value for $key: $this")

    private fun flagToKey(flag: String): String? {
        val name = flag.removePrefix("--")
        if (name.isBlank()) return null

        val parts = name.split('-')
        if (parts.isEmpty()) return null

        val camel = buildString {
            parts.forEachIndexed { index, segment ->
                if (segment.isEmpty()) return@forEachIndexed
                if (index == 0) {
                    append(segment)
                } else {
                    append(segment.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                }
            }
        }

        return camel.takeIf { it.isNotBlank() }
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

        // Simple JSON parsing - handle arrays properly
        val content = jsonContent.trim().removeSurrounding("{", "}")
        
        // Parse key-value pairs while handling arrays
        val pairs = parseJsonPairs(content)

        for ((key, value) in pairs) {
            config = applyConfigSetting(config, key, value)
        }

        return config
    }
    
    /**
     * Parse JSON key-value pairs, properly handling arrays
     */
    private fun parseJsonPairs(content: String): List<Pair<String, String>> {
        val pairs = mutableListOf<Pair<String, String>>()
        var i = 0
        
        while (i < content.length) {
            // Skip whitespace
            while (i < content.length && content[i].isWhitespace()) i++
            if (i >= content.length) break
            
            // Skip comma
            if (content[i] == ',') {
                i++
                continue
            }
            
            // Parse key
            if (content[i] != '"') break
            i++ // skip opening quote
            val keyStart = i
            while (i < content.length && content[i] != '"') i++
            if (i >= content.length) break
            val key = content.substring(keyStart, i)
            i++ // skip closing quote
            
            // Skip whitespace and colon
            while (i < content.length && (content[i].isWhitespace() || content[i] == ':')) i++
            if (i >= content.length) break
            
            // Parse value
            val valueStart = i
            var valueEnd = i
            
            if (content[i] == '[') {
                // Handle array value
                var bracketCount = 0
                while (i < content.length) {
                    if (content[i] == '[') bracketCount++
                    else if (content[i] == ']') bracketCount--
                    i++
                    if (bracketCount == 0) break
                }
                valueEnd = i
            } else if (content[i] == '"') {
                // Handle string value
                i++ // skip opening quote
                while (i < content.length && content[i] != '"') i++
                if (i < content.length) i++ // skip closing quote
                valueEnd = i
            } else {
                // Handle number or other value
                while (i < content.length && content[i] != ',' && content[i] != '}') i++
                valueEnd = i
            }
            
            val value = content.substring(valueStart, valueEnd).trim()
            pairs.add(key to value)
        }
        
        return pairs
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
