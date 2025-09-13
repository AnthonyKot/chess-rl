package com.chessrl.integration

/**
 * Command-line interface for deterministic chess RL training with comprehensive seed management.
 * Provides CLI commands for training, testing, and seed management operations.
 */
object DeterministicTrainingCLI {
    
    /**
     * Main CLI entry point
     */
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            printUsage()
            return
        }
        
        when (args[0].lowercase()) {
            "train" -> handleTrainCommand(args.drop(1).toTypedArray())
            "test" -> handleTestCommand(args.drop(1).toTypedArray())
            "demo" -> handleDemoCommand(args.drop(1).toTypedArray())
            "seed" -> handleSeedCommand(args.drop(1).toTypedArray())
            "config" -> handleConfigCommand(args.drop(1).toTypedArray())
            "help", "--help", "-h" -> printUsage()
            else -> {
                println("❌ Unknown command: ${args[0]}")
                printUsage()
            }
        }
    }
    
    /**
     * Handle train command
     */
    private fun handleTrainCommand(args: Array<String>) {
        println("🚀 Starting Chess RL Deterministic Training")
        println("=" * 50)
        
        try {
            // Parse configuration from CLI arguments
            val config = ConfigurationParser.parseCliArguments(args)
            
            // Validate configuration
            val validation = config.validate()
            if (!validation.isValid) {
                println("❌ Configuration validation failed:")
                validation.errors.forEach { println("   - $it") }
                return
            }
            
            if (validation.warnings.isNotEmpty()) {
                println("⚠️ Configuration warnings:")
                validation.warnings.forEach { println("   - $it") }
                println()
            }
            
            // Display configuration summary
            displayConfigurationSummary(config)
            
            // Create and initialize controller
            val controller = DeterministicTrainingController(config)
            
            if (!controller.initialize()) {
                println("❌ Failed to initialize training controller")
                return
            }
            
            // Start training
            val results = controller.startTraining(config.episodes)
            
            if (results != null) {
                displayTrainingResults(results, config)
            } else {
                println("❌ Training failed to complete")
            }
            
        } catch (e: Exception) {
            println("❌ Training command failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Handle test command for CI/CD
     */
    private fun handleTestCommand(args: Array<String>) {
        println("🧪 Running Deterministic Test Mode")
        println("=" * 40)
        
        try {
            var testSeed = 12345L
            var episodes = 5
            
            // Parse test-specific arguments
            var i = 0
            while (i < args.size) {
                when (args[i]) {
                    "--seed" -> {
                        if (i + 1 < args.size) {
                            testSeed = args[++i].toLong()
                        }
                    }
                    "--episodes" -> {
                        if (i + 1 < args.size) {
                            episodes = args[++i].toInt()
                        }
                    }
                }
                i++
            }
            
            println("🎲 Test seed: $testSeed")
            println("📊 Test episodes: $episodes")
            println()
            
            // Create test configuration
            val testConfig = ConfigurationParser.createTestConfiguration(testSeed)
                .copy(episodes = episodes)
            
            // Run deterministic test
            val controller = DeterministicTrainingController(testConfig)
            val testResult = controller.runDeterministicTest(episodes, testSeed)
            
            // Display test results
            displayTestResults(testResult)
            
            // Exit with appropriate code for CI/CD
            if (testResult.success) {
                println("✅ Deterministic test PASSED")
            } else {
                println("❌ Deterministic test FAILED")
                kotlin.system.exitProcess(1)
            }
            
        } catch (e: Exception) {
            println("❌ Test command failed: ${e.message}")
            kotlin.system.exitProcess(1)
        }
    }
    
    /**
     * Handle demo command
     */
    private fun handleDemoCommand(args: Array<String>) {
        println("🎭 Running Deterministic Training Demo")
        println("=" * 40)
        
        try {
            DeterministicTrainingDemo.runDemo()
        } catch (e: Exception) {
            println("❌ Demo command failed: ${e.message}")
        }
    }
    
    /**
     * Handle seed management commands
     */
    private fun handleSeedCommand(args: Array<String>) {
        if (args.isEmpty()) {
            println("❌ Seed command requires subcommand")
            printSeedUsage()
            return
        }
        
        when (args[0].lowercase()) {
            "generate" -> generateRandomSeed()
            "validate" -> validateSeedConfiguration(args.drop(1).toTypedArray())
            "info" -> displaySeedInfo(args.drop(1).toTypedArray())
            else -> {
                println("❌ Unknown seed subcommand: ${args[0]}")
                printSeedUsage()
            }
        }
    }
    
    /**
     * Handle configuration commands
     */
    private fun handleConfigCommand(args: Array<String>) {
        if (args.isEmpty()) {
            println("❌ Config command requires subcommand")
            printConfigUsage()
            return
        }
        
        when (args[0].lowercase()) {
            "validate" -> validateConfiguration(args.drop(1).toTypedArray())
            "create" -> createConfiguration(args.drop(1).toTypedArray())
            "show" -> showConfiguration(args.drop(1).toTypedArray())
            else -> {
                println("❌ Unknown config subcommand: ${args[0]}")
                printConfigUsage()
            }
        }
    }
    
    // Seed management functions
    
    private fun generateRandomSeed() {
        val randomSeed = kotlin.random.Random.nextLong()
        println("🎲 Generated random seed: $randomSeed")
        println("   Use with: --seed $randomSeed --deterministic")
    }
    
    private fun validateSeedConfiguration(args: Array<String>) {
        try {
            var seed: Long? = null
            
            // Parse seed from arguments
            var i = 0
            while (i < args.size) {
                when (args[i]) {
                    "--seed" -> {
                        if (i + 1 < args.size) {
                            seed = args[++i].toLong()
                        }
                    }
                }
                i++
            }
            
            if (seed == null) {
                println("❌ No seed specified for validation")
                println("   Usage: seed validate --seed <seed_value>")
                return
            }
            
            // Initialize seed manager and validate
            val seedManager = SeedManager.initializeWithSeed(seed)
            val validation = seedManager.validateSeedConsistency()
            
            println("🔍 Seed Validation Results:")
            println("   Seed: $seed")
            println("   Valid: ${validation.isValid}")
            
            if (validation.issues.isNotEmpty()) {
                println("   Issues:")
                validation.issues.forEach { println("     - $it") }
            } else {
                println("   ✅ No issues detected")
            }
            
            println("   Component seeds: ${validation.componentSeeds.size}")
            
        } catch (e: Exception) {
            println("❌ Seed validation failed: ${e.message}")
        }
    }
    
    private fun displaySeedInfo(args: Array<String>) {
        try {
            var seed: Long? = null
            
            // Parse seed from arguments
            var i = 0
            while (i < args.size) {
                when (args[i]) {
                    "--seed" -> {
                        if (i + 1 < args.size) {
                            seed = args[++i].toLong()
                        }
                    }
                }
                i++
            }
            
            if (seed == null) {
                println("❌ No seed specified")
                println("   Usage: seed info --seed <seed_value>")
                return
            }
            
            // Initialize seed manager and display info
            val seedManager = SeedManager.initializeWithSeed(seed)
            val summary = seedManager.getSeedSummary()
            val componentSeeds = seedManager.getAllComponentSeeds()
            
            println("🎲 Seed Information:")
            println("   Master Seed: ${summary.masterSeed}")
            println("   Deterministic Mode: ${summary.isDeterministicMode}")
            println("   Component Count: ${summary.componentCount}")
            println("   Seed Events: ${summary.seedEvents}")
            
            println("\n🔧 Component Seeds:")
            componentSeeds.forEach { (component, componentSeed) ->
                println("   $component: $componentSeed")
            }
            
        } catch (e: Exception) {
            println("❌ Failed to display seed info: ${e.message}")
        }
    }
    
    // Configuration management functions
    
    private fun validateConfiguration(args: Array<String>) {
        try {
            val config = ConfigurationParser.parseCliArguments(args)
            val validation = config.validate()
            
            println("🔍 Configuration Validation:")
            println("   Valid: ${validation.isValid}")
            
            if (validation.errors.isNotEmpty()) {
                println("   Errors:")
                validation.errors.forEach { println("     - $it") }
            }
            
            if (validation.warnings.isNotEmpty()) {
                println("   Warnings:")
                validation.warnings.forEach { println("     - $it") }
            }
            
            if (validation.isValid && validation.warnings.isEmpty()) {
                println("   ✅ Configuration is valid with no warnings")
            }
            
        } catch (e: Exception) {
            println("❌ Configuration validation failed: ${e.message}")
        }
    }
    
    private fun createConfiguration(args: Array<String>) {
        try {
            val config = ConfigurationParser.parseCliArguments(args)
            val summary = config.getSummary()
            
            println("📋 Created Configuration:")
            println("   Name: ${summary.name}")
            println("   Description: ${summary.description}")
            println("   Seed: ${summary.seedConfig.seed}")
            println("   Deterministic: ${summary.seedConfig.deterministicMode}")
            println("   Episodes: ${summary.trainingParams.episodes}")
            println("   Batch Size: ${summary.trainingParams.batchSize}")
            println("   Learning Rate: ${summary.trainingParams.learningRate}")
            println("   Hidden Layers: ${summary.networkConfig.hiddenLayers}")
            println("   Optimizer: ${summary.networkConfig.optimizer}")
            
        } catch (e: Exception) {
            println("❌ Failed to create configuration: ${e.message}")
        }
    }
    
    private fun showConfiguration(args: Array<String>) {
        try {
            if (args.isEmpty()) {
                // Show default configurations
                println("📋 Available Default Configurations:")
                
                val testConfig = ConfigurationParser.createTestConfiguration()
                println("\n🧪 Test Configuration:")
                displayConfigurationSummary(testConfig)
                
                val prodConfig = ConfigurationParser.createProductionConfiguration()
                println("\n🏭 Production Configuration:")
                displayConfigurationSummary(prodConfig)
                
            } else {
                // Show parsed configuration
                val config = ConfigurationParser.parseCliArguments(args)
                println("📋 Parsed Configuration:")
                displayConfigurationSummary(config)
            }
            
        } catch (e: Exception) {
            println("❌ Failed to show configuration: ${e.message}")
        }
    }
    
    // Display functions
    
    private fun displayConfigurationSummary(config: TrainingConfiguration) {
        val summary = config.getSummary()
        
        println("   Name: ${summary.name}")
        if (summary.description.isNotEmpty()) {
            println("   Description: ${summary.description}")
        }
        
        println("   🎲 Seed Configuration:")
        println("      Seed: ${summary.seedConfig.seed ?: "random"}")
        println("      Deterministic: ${summary.seedConfig.deterministicMode}")
        
        println("   🏋️ Training Parameters:")
        println("      Episodes: ${summary.trainingParams.episodes}")
        println("      Batch Size: ${summary.trainingParams.batchSize}")
        println("      Learning Rate: ${summary.trainingParams.learningRate}")
        println("      Exploration Rate: ${summary.trainingParams.explorationRate}")
        
        println("   🧠 Network Configuration:")
        println("      Hidden Layers: ${summary.networkConfig.hiddenLayers}")
        println("      Activation: ${summary.networkConfig.activationFunction}")
        println("      Optimizer: ${summary.networkConfig.optimizer}")
        println("      Weight Init: ${summary.networkConfig.weightInitialization}")
        
        println("   📊 Monitoring:")
        println("      Progress Interval: ${summary.monitoringConfig.progressReportInterval}")
        println("      Real-time Monitoring: ${summary.monitoringConfig.enableRealTimeMonitoring}")
        println("      Metrics Format: ${summary.monitoringConfig.metricsOutputFormat}")
    }
    
    private fun displayTrainingResults(results: TrainingResults, config: TrainingConfiguration) {
        println("\n🏁 Training Results:")
        println("   Episodes Completed: ${results.totalEpisodes}")
        println("   Total Steps: ${results.totalSteps}")
        println("   Training Duration: ${results.trainingDuration}ms")
        println("   Best Performance: ${results.bestPerformance}")
        
        val finalMetrics = results.finalMetrics
        println("\n📈 Final Metrics:")
        println("   Average Reward: ${finalMetrics.averageReward}")
        println("   Win Rate: ${(finalMetrics.winRate * 100).toInt()}%")
        println("   Draw Rate: ${(finalMetrics.drawRate * 100).toInt()}%")
        println("   Loss Rate: ${(finalMetrics.lossRate * 100).toInt()}%")
        
        // Display reproducibility information
        if (config.deterministicMode && config.seed != null) {
            println("\n🔄 Reproducibility:")
            println("   This run can be reproduced using:")
            println("   --seed ${config.seed} --deterministic --episodes ${config.episodes}")
        }
    }
    
    private fun displayTestResults(result: DeterministicTestResult) {
        println("🧪 Test Results:")
        println("   Success: ${result.success}")
        println("   Seed: ${result.seed}")
        println("   Episodes: ${result.episodes}")
        println("   Duration: ${result.duration}ms")
        
        if (result.success) {
            println("   Final Performance: ${result.finalPerformance}")
        } else {
            println("   Error: ${result.error}")
        }
    }
    
    // Usage functions
    
    private fun printUsage() {
        println("Chess RL Deterministic Training CLI")
        println("Usage: chess-rl <command> [options]")
        println()
        println("Commands:")
        println("  train     Start deterministic training")
        println("  test      Run deterministic test mode (for CI/CD)")
        println("  demo      Run comprehensive demo")
        println("  seed      Seed management operations")
        println("  config    Configuration management")
        println("  help      Show this help message")
        println()
        println("Training Options:")
        ConfigurationParser.printUsage()
        println()
        println("Examples:")
        println("  chess-rl train --seed 12345 --deterministic --episodes 1000")
        println("  chess-rl test --seed 12345 --episodes 5")
        println("  chess-rl demo")
        println("  chess-rl seed generate")
        println("  chess-rl config validate --episodes 100 --batch-size 32")
    }
    
    private fun printSeedUsage() {
        println("Seed Management Commands:")
        println("  generate    Generate a random seed")
        println("  validate    Validate seed configuration")
        println("  info        Display seed information")
        println()
        println("Examples:")
        println("  chess-rl seed generate")
        println("  chess-rl seed validate --seed 12345")
        println("  chess-rl seed info --seed 12345")
    }
    
    private fun printConfigUsage() {
        println("Configuration Management Commands:")
        println("  validate    Validate configuration parameters")
        println("  create      Create and display configuration")
        println("  show        Show default or parsed configuration")
        println()
        println("Examples:")
        println("  chess-rl config validate --episodes 100 --batch-size 32")
        println("  chess-rl config create --seed 12345 --episodes 1000")
        println("  chess-rl config show")
    }
    
    /**
     * Utility function for creating separator lines
     */
    private operator fun String.times(count: Int): String {
        return this.repeat(count)
    }
}

/**
 * Entry point for the CLI application
 */
fun main(args: Array<String>) {
    DeterministicTrainingCLI.main(args)
}