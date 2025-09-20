package com.chessrl.integration.config

/**
 * Demonstration of the new Chess RL configuration system.
 * 
 * This shows how the simplified configuration system works with
 * only essential parameters and clear validation.
 */
object ConfigDemo {
    
    /**
     * Demonstrate basic configuration usage.
     */
    fun demonstrateBasicUsage() {
        println("=== Chess RL Configuration System Demo ===")
        println()
        
        // Default configuration
        val defaultConfig = ChessRLConfig()
        println("Default Configuration:")
        println(defaultConfig.getSummary())
        println()
        
        // Validate default configuration
        val validation = defaultConfig.validate()
        println("Validation Result:")
        validation.printResults()
        println()
    }
    
    /**
     * Demonstrate profile usage.
     */
    fun demonstrateProfiles() {
        println("=== Profile Demonstrations ===")
        println()
        
        // Fast debug profile
        val fastDebug = ConfigParser.loadProfile("fast-debug")
        println("Fast Debug Profile:")
        println("  Games per cycle: ${fastDebug.gamesPerCycle}")
        println("  Max cycles: ${fastDebug.maxCycles}")
        println("  Concurrent games: ${fastDebug.maxConcurrentGames}")
        println("  Hidden layers: ${fastDebug.hiddenLayers}")
        println("  Checkpoint dir: ${fastDebug.checkpointDirectory}")
        println()
        
        // Long train profile
        val longTrain = ConfigParser.loadProfile("long-train")
        println("Long Train Profile:")
        println("  Games per cycle: ${longTrain.gamesPerCycle}")
        println("  Max cycles: ${longTrain.maxCycles}")
        println("  Concurrent games: ${longTrain.maxConcurrentGames}")
        println("  Hidden layers: ${longTrain.hiddenLayers}")
        println("  Learning rate: ${longTrain.learningRate}")
        println()
        
        // Eval only profile
        val evalOnly = ConfigParser.loadProfile("eval-only")
        println("Eval Only Profile:")
        println("  Evaluation games: ${evalOnly.evaluationGames}")
        println("  Concurrent games: ${evalOnly.maxConcurrentGames}")
        println("  Seed: ${evalOnly.seed}")
        println("  Checkpoint dir: ${evalOnly.checkpointDirectory}")
        println()
    }
    
    /**
     * Demonstrate command-line argument parsing.
     */
    fun demonstrateArgParsing() {
        println("=== Command-Line Argument Parsing ===")
        println()
        
        // Simulate command-line arguments
        val args = arrayOf(
            "--profile", "fast-debug",
            "--learning-rate", "0.002",
            "--games-per-cycle", "15",
            "--seed", "42"
        )
        
        println("Parsing arguments: ${args.joinToString(" ")}")
        val config = ConfigParser.parseArgs(args)
        
        println("Parsed Configuration:")
        println("  Learning rate: ${config.learningRate}")
        println("  Games per cycle: ${config.gamesPerCycle}")
        println("  Seed: ${config.seed}")
        println("  Max cycles: ${config.maxCycles}") // From profile
        println("  Hidden layers: ${config.hiddenLayers}") // From profile
        println()
    }
    
    /**
     * Demonstrate configuration validation.
     */
    fun demonstrateValidation() {
        println("=== Configuration Validation ===")
        println()
        
        // Valid configuration
        val validConfig = ChessRLConfig(
            learningRate = 0.001,
            explorationRate = 0.1,
            gamesPerCycle = 20
        )
        
        println("Valid Configuration:")
        val validResult = validConfig.validate()
        validResult.printResults()
        println()
        
        // Invalid configuration
        val invalidConfig = ChessRLConfig(
            learningRate = -0.1,  // Invalid: negative
            explorationRate = 1.5,  // Invalid: > 1.0
            hiddenLayers = emptyList(),  // Invalid: empty
            batchSize = 0  // Invalid: zero
        )
        
        println("Invalid Configuration:")
        val invalidResult = invalidConfig.validate()
        invalidResult.printResults()
        println()
        
        // Configuration with warnings
        val warningConfig = ChessRLConfig(
            winReward = -1.0,  // Warning: win < loss
            lossReward = 1.0,
            maxConcurrentGames = 16,  // Warning: high concurrency
            hiddenLayers = listOf(2048, 1024)  // Warning: large layers
        )
        
        println("Configuration with Warnings:")
        val warningResult = warningConfig.validate()
        warningResult.printResults()
        println()
    }
    
    /**
     * Demonstrate YAML parsing.
     */
    fun demonstrateYamlParsing() {
        println("=== YAML Configuration Parsing ===")
        println()
        
        val yamlContent = """
            # Custom training configuration
            learningRate: 0.0015
            batchSize: 96
            hiddenLayers: 1024,512,256
            gamesPerCycle: 40
            maxCycles: 150
            explorationRate: 0.12
            seed: 98765
            checkpointDirectory: "checkpoints/custom"
        """.trimIndent()
        
        println("YAML Content:")
        println(yamlContent)
        println()
        
        val config = ConfigParser.parseYaml(yamlContent)
        println("Parsed Configuration:")
        println("  Learning rate: ${config.learningRate}")
        println("  Batch size: ${config.batchSize}")
        println("  Hidden layers: ${config.hiddenLayers}")
        println("  Games per cycle: ${config.gamesPerCycle}")
        println("  Max cycles: ${config.maxCycles}")
        println("  Exploration rate: ${config.explorationRate}")
        println("  Seed: ${config.seed}")
        println("  Checkpoint dir: ${config.checkpointDirectory}")
        println()
    }
    
    /**
     * Show available profiles and usage information.
     */
    fun showUsageInformation() {
        println("=== Usage Information ===")
        println()
        
        println("Available Profiles:")
        ConfigParser.getAvailableProfiles().forEach { profile ->
            println("  - $profile")
        }
        println()
        
        println("Command-Line Usage:")
        ConfigParser.printUsage()
    }
    
    /**
     * Run all demonstrations.
     */
    fun runAllDemos() {
        demonstrateBasicUsage()
        demonstrateProfiles()
        demonstrateArgParsing()
        demonstrateValidation()
        demonstrateYamlParsing()
        showUsageInformation()
    }
}