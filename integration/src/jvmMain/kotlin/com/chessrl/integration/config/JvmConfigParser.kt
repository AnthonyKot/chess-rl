package com.chessrl.integration.config

import com.chessrl.integration.ProfilesLoader

/**
 * JVM-specific configuration parser that integrates with the existing ProfilesLoader.
 * 
 * This provides actual file-based profile loading using the existing YAML parser
 * that handles the nested structure of profiles.yaml.
 */
object JvmConfigParser {
    
    /**
     * Load configuration from profiles.yaml file using the existing ProfilesLoader.
     * 
     * This properly handles the nested YAML structure:
     * profiles:
     *   profile-name:
     *     key: value
     * 
     * @param profileName Name of the profile to load
     * @param profilesPaths List of paths to try for profiles.yaml
     * @return ChessRLConfig for the specified profile
     * @throws IllegalArgumentException if profile is not found
     */
    fun loadProfileFromFile(
        profileName: String, 
        profilesPaths: List<String> = listOf("profiles.yaml", "integration/profiles.yaml")
    ): ChessRLConfig {
        val profiles = ProfilesLoader.loadProfiles(profilesPaths)
        val profileData = profiles[profileName] 
            ?: throw IllegalArgumentException("Profile '$profileName' not found in ${profilesPaths.joinToString(", ")}. Available profiles: ${profiles.keys.joinToString(", ")}")
        
        return parseProfileData(profileData)
    }
    
    /**
     * Parse profile data from ProfilesLoader into ChessRLConfig.
     * 
     * Maps the string-based profile data to typed configuration parameters.
     * 
     * @param profileData Map of configuration keys to string values
     * @return ChessRLConfig with parsed values
     */
    fun parseProfileData(profileData: Map<String, String>): ChessRLConfig {
        var config = ChessRLConfig()
        
        for ((key, value) in profileData) {
            try {
                config = when (key) {
                    "hiddenLayers" -> {
                        val layers = parseHiddenLayersFromProfile(value)
                        config.copy(hiddenLayers = layers)
                    }
                    "learningRate" -> config.copy(learningRate = value.toDouble())
                    "batchSize" -> config.copy(batchSize = value.toInt())
                    "explorationRate" -> config.copy(explorationRate = value.toDouble())
                    "targetUpdateFrequency" -> config.copy(targetUpdateFrequency = value.toInt())
                    "doubleDqn" -> config.copy(doubleDqn = value.equals("true", true))
                    "gamma" -> config.copy(gamma = value.toDouble())
                    "maxExperienceBuffer" -> config.copy(maxExperienceBuffer = value.toInt())
                    "replayType" -> config.copy(replayType = value.trim())
                    "maxBatchesPerCycle" -> config.copy(maxBatchesPerCycle = value.toInt())
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
                    // checkpoint manager + logging
                    "checkpointMaxVersions" -> config.copy(checkpointMaxVersions = value.toInt())
                    "checkpointValidationEnabled" -> config.copy(checkpointValidationEnabled = value.equals("true", true))
                    "checkpointCompressionEnabled" -> config.copy(checkpointCompressionEnabled = value.equals("true", true))
                    "checkpointAutoCleanupEnabled" -> config.copy(checkpointAutoCleanupEnabled = value.equals("true", true))
                    "logInterval" -> config.copy(logInterval = value.toInt())
                    "summaryOnly" -> config.copy(summaryOnly = value.equals("true", true))
                    "metricsFile" -> config.copy(metricsFile = value.trim('"'))
                    
                    // Legacy parameter mappings for backward compatibility
                    "episodes" -> config.copy(maxCycles = value.toInt())
                    "parallelGames" -> config.copy(maxConcurrentGames = value.toInt())
                    "gamesPerIteration" -> config.copy(gamesPerCycle = value.toInt())
                    
                    else -> {
                        // Ignore unknown keys for forward compatibility
                        config
                    }
                }
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Invalid value for $key: $value in profile", e)
            }
        }
        
        return config
    }
    
    /**
     * Parse hidden layers from profile format.
     * 
     * Handles the format used in profiles.yaml: "512,256,128"
     * 
     * @param value String representation of hidden layers
     * @return List of layer sizes
     */
    private fun parseHiddenLayersFromProfile(value: String): List<Int> {
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
    
    /**
     * Enhanced profile loading that tries file first, then falls back to built-in.
     * 
     * @param profileName Name of the profile to load
     * @return ChessRLConfig for the specified profile
     */
    fun loadProfile(profileName: String): ChessRLConfig {
        return try {
            // Try to load from file first
            loadProfileFromFile(profileName)
        } catch (e: Exception) {
            // Fall back to built-in profiles from ConfigParser
            ConfigParser.loadProfile(profileName)
        }
    }
    
    /**
     * Get all available profiles from files and built-in.
     * 
     * @param profilesPaths List of paths to try for profiles.yaml
     * @return Map of profile names to their configurations
     */
    fun getAllProfiles(profilesPaths: List<String> = listOf("profiles.yaml", "integration/profiles.yaml")): Map<String, ChessRLConfig> {
        val result = mutableMapOf<String, ChessRLConfig>()
        
        // Add file-based profiles
        try {
            val profiles = ProfilesLoader.loadProfiles(profilesPaths)
            for ((name, data) in profiles) {
                try {
                    result[name] = parseProfileData(data)
                } catch (e: Exception) {
                    // Skip invalid profiles
                    println("Warning: Skipping invalid profile '$name': ${e.message}")
                }
            }
        } catch (e: Exception) {
            // No file-based profiles available
        }
        
        // Add built-in profiles (these will override file-based ones with same names)
        for (profileName in ConfigParser.getAvailableProfiles()) {
            result[profileName] = ConfigParser.loadProfile(profileName)
        }
        
        return result
    }
}
