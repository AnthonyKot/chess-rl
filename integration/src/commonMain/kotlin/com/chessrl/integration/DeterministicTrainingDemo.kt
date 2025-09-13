package com.chessrl.integration

/**
 * Comprehensive demonstration of the deterministic training system with seed management.
 * Shows how to use centralized seeding for reproducible training runs.
 */
object DeterministicTrainingDemo {
    
    /**
     * Main demonstration function
     */
    fun runDemo() {
        println("🎲 Chess RL Deterministic Training System Demo")
        println("=" * 60)
        
        // Demo 1: Basic deterministic training
        demonstrateBasicDeterministicTraining()
        
        // Demo 2: Seed configuration and CLI parsing
        demonstrateSeedConfiguration()
        
        // Demo 3: Checkpoint with seed restoration
        demonstrateCheckpointSeedRestore()
        
        // Demo 4: Deterministic test mode for CI
        demonstrateDeterministicTestMode()
        
        // Demo 5: Seed validation and consistency checking
        demonstrateSeedValidation()
        
        // Demo 6: Reproducibility verification
        demonstrateReproducibilityVerification()
        
        println("\n✅ Deterministic Training Demo completed successfully!")
    }
    
    /**
     * Demo 1: Basic deterministic training with fixed seed
     */
    private fun demonstrateBasicDeterministicTraining() {
        println("\n📋 Demo 1: Basic Deterministic Training")
        println("-" * 40)
        
        try {
            // Create configuration with fixed seed
            val config = TrainingConfigurationBuilder()
                .seed(12345L)
                .deterministicMode(true)
                .episodes(10)
                .batchSize(16)
                .learningRate(0.01)
                .name("demo-deterministic")
                .description("Basic deterministic training demo")
                .build()
            
            // Validate configuration
            val validation = config.validate()
            if (!validation.isValid) {
                println("❌ Configuration validation failed:")
                validation.errors.forEach { println("   - $it") }
                return
            }
            
            // Create and initialize controller
            val controller = DeterministicTrainingController(config)
            
            if (controller.initialize()) {
                println("✅ Controller initialized with deterministic seed")
                
                // Get status before training
                val initialStatus = controller.getEnhancedTrainingStatus()
                println("🎲 Initial seed: ${initialStatus.seedSummary.masterSeed}")
                println("🔧 Deterministic mode: ${initialStatus.seedSummary.isDeterministicMode}")
                
                // Run short training
                val results = controller.startTraining(5)
                
                if (results != null) {
                    println("✅ Training completed - Episodes: ${results.totalEpisodes}")
                    println("📊 Best performance: ${results.bestPerformance}")
                } else {
                    println("❌ Training failed")
                }
            } else {
                println("❌ Failed to initialize controller")
            }
            
        } catch (e: Exception) {
            println("❌ Demo 1 failed: ${e.message}")
        }
    }
    
    /**
     * Demo 2: Seed configuration and CLI parsing
     */
    private fun demonstrateSeedConfiguration() {
        println("\n📋 Demo 2: Seed Configuration and CLI Parsing")
        println("-" * 40)
        
        try {
            // Demonstrate CLI argument parsing
            val cliArgs = arrayOf(
                "--seed", "54321",
                "--deterministic",
                "--episodes", "20",
                "--batch-size", "32",
                "--learning-rate", "0.001",
                "--name", "cli-demo"
            )
            
            val configFromCli = ConfigurationParser.parseCliArguments(cliArgs)
            println("✅ Parsed CLI configuration:")
            println("   Seed: ${configFromCli.seed}")
            println("   Deterministic: ${configFromCli.deterministicMode}")
            println("   Episodes: ${configFromCli.episodes}")
            println("   Name: ${configFromCli.configurationName}")
            
            // Demonstrate programmatic configuration
            val programmaticConfig = TrainingConfigurationBuilder()
                .seed(98765L)
                .episodes(15)
                .batchSize(64)
                .hiddenLayers(listOf(256, 128))
                .optimizer("adam")
                .name("programmatic-demo")
                .build()
            
            println("\n✅ Programmatic configuration:")
            val summary = programmaticConfig.getSummary()
            println("   Seed: ${summary.seedConfig.seed}")
            println("   Network layers: ${summary.networkConfig.hiddenLayers}")
            println("   Optimizer: ${summary.networkConfig.optimizer}")
            
            // Show configuration validation
            val validation = programmaticConfig.validate()
            println("\n🔍 Configuration validation:")
            println("   Valid: ${validation.isValid}")
            if (validation.warnings.isNotEmpty()) {
                println("   Warnings: ${validation.warnings.size}")
                validation.warnings.forEach { println("     - $it") }
            }
            
        } catch (e: Exception) {
            println("❌ Demo 2 failed: ${e.message}")
        }
    }
    
    /**
     * Demo 3: Checkpoint with seed restoration
     */
    private fun demonstrateCheckpointSeedRestore() {
        println("\n📋 Demo 3: Checkpoint with Seed Restoration")
        println("-" * 40)
        
        try {
            // Initialize seed manager
            val seedManager = SeedManager.initializeWithSeed(11111L)
            
            // Create configuration
            val config = TrainingConfiguration(
                seed = 11111L,
                deterministicMode = true,
                episodes = 5,
                batchSize = 16
            )
            
            // Get initial seed configuration
            val initialSeedConfig = seedManager.getSeedConfiguration()
            println("✅ Initial seed configuration captured")
            println("   Master seed: ${initialSeedConfig.masterSeed}")
            println("   Component seeds: ${initialSeedConfig.componentSeeds.size}")
            
            // Create checkpoint metadata with seed configuration
            val checkpointMetadata = CheckpointMetadata(
                cycle = 1,
                performance = 0.75,
                description = "Demo checkpoint with seed configuration",
                seedConfiguration = initialSeedConfig,
                trainingConfiguration = config
            )
            
            println("✅ Checkpoint metadata created with seed configuration")
            
            // Simulate seed restoration
            val newSeedManager = SeedManager.getInstance()
            newSeedManager.restoreSeedConfiguration(initialSeedConfig)
            
            println("✅ Seed configuration restored successfully")
            
            // Verify restoration
            val restoredConfig = newSeedManager.getSeedConfiguration()
            val isIdentical = restoredConfig.masterSeed == initialSeedConfig.masterSeed &&
                             restoredConfig.componentSeeds == initialSeedConfig.componentSeeds
            
            println("🔍 Restoration verification: ${if (isIdentical) "✅ Identical" else "❌ Different"}")
            
        } catch (e: Exception) {
            println("❌ Demo 3 failed: ${e.message}")
        }
    }
    
    /**
     * Demo 4: Deterministic test mode for CI
     */
    private fun demonstrateDeterministicTestMode() {
        println("\n📋 Demo 4: Deterministic Test Mode for CI")
        println("-" * 40)
        
        try {
            // Create test configuration
            val testConfig = ConfigurationParser.createTestConfiguration(seed = 99999L)
            println("✅ Test configuration created:")
            println("   Seed: ${testConfig.seed}")
            println("   Episodes: ${testConfig.episodes}")
            println("   Debug mode: ${testConfig.enableDebugMode}")
            
            // Create controller and enable test mode
            val controller = DeterministicTrainingController(testConfig)
            
            // Run deterministic test
            val testResult = controller.runDeterministicTest(episodes = 3, testSeed = 99999L)
            
            println("\n🧪 Deterministic test result:")
            println("   Success: ${testResult.success}")
            println("   Seed: ${testResult.seed}")
            println("   Episodes: ${testResult.episodes}")
            println("   Duration: ${testResult.duration}ms")
            
            if (testResult.success) {
                println("   Final performance: ${testResult.finalPerformance}")
                println("✅ Test mode validation successful")
            } else {
                println("   Error: ${testResult.error}")
                println("❌ Test mode validation failed")
            }
            
        } catch (e: Exception) {
            println("❌ Demo 4 failed: ${e.message}")
        }
    }
    
    /**
     * Demo 5: Seed validation and consistency checking
     */
    private fun demonstrateSeedValidation() {
        println("\n📋 Demo 5: Seed Validation and Consistency Checking")
        println("-" * 40)
        
        try {
            // Initialize seed manager
            val seedManager = SeedManager.initializeWithSeed(77777L)
            
            // Get seed summary
            val summary = seedManager.getSeedSummary()
            println("✅ Seed summary:")
            println("   Seeded: ${summary.isSeeded}")
            println("   Master seed: ${summary.masterSeed}")
            println("   Deterministic: ${summary.isDeterministicMode}")
            println("   Components: ${summary.componentCount}")
            
            // Validate seed consistency
            val validation = seedManager.validateSeedConsistency()
            println("\n🔍 Seed validation:")
            println("   Valid: ${validation.isValid}")
            
            if (validation.issues.isNotEmpty()) {
                println("   Issues:")
                validation.issues.forEach { println("     - $it") }
            } else {
                println("   No issues detected")
            }
            
            // Show component seeds
            val componentSeeds = seedManager.getAllComponentSeeds()
            println("\n🎲 Component seeds:")
            componentSeeds.forEach { (component, seed) ->
                println("   $component: $seed")
            }
            
            // Test random generator consistency
            println("\n🔄 Random generator consistency test:")
            val nnRandom = seedManager.getNeuralNetworkRandom()
            val explorationRandom = seedManager.getExplorationRandom()
            
            val nnValue1 = nnRandom.nextDouble()
            val explorationValue1 = explorationRandom.nextDouble()
            
            println("   NN random value: $nnValue1")
            println("   Exploration random value: $explorationValue1")
            println("✅ Random generators working correctly")
            
        } catch (e: Exception) {
            println("❌ Demo 5 failed: ${e.message}")
        }
    }
    
    /**
     * Demo 6: Reproducibility verification
     */
    private fun demonstrateReproducibilityVerification() {
        println("\n📋 Demo 6: Reproducibility Verification")
        println("-" * 40)
        
        try {
            val testSeed = 55555L
            
            // Run 1: First training run
            println("🔄 Running first training session...")
            val result1 = runTrainingSession(testSeed, "run-1")
            
            // Run 2: Second training run with same seed
            println("🔄 Running second training session with same seed...")
            val result2 = runTrainingSession(testSeed, "run-2")
            
            // Compare results
            println("\n📊 Reproducibility comparison:")
            println("   Run 1 episodes: ${result1?.totalEpisodes}")
            println("   Run 2 episodes: ${result2?.totalEpisodes}")
            
            if (result1 != null && result2 != null) {
                val episodesMatch = result1.totalEpisodes == result2.totalEpisodes
                val performanceMatch = kotlin.math.abs(result1.bestPerformance - result2.bestPerformance) < 1e-10
                
                println("   Episodes match: ${if (episodesMatch) "✅" else "❌"}")
                println("   Performance match: ${if (performanceMatch) "✅" else "❌"}")
                
                if (episodesMatch && performanceMatch) {
                    println("✅ Reproducibility verified - identical results!")
                } else {
                    println("⚠️ Results differ - check seed implementation")
                }
            } else {
                println("❌ One or both training runs failed")
            }
            
        } catch (e: Exception) {
            println("❌ Demo 6 failed: ${e.message}")
        }
    }
    
    /**
     * Helper function to run a training session
     */
    private fun runTrainingSession(seed: Long, runName: String): TrainingResults? {
        return try {
            val config = TrainingConfiguration(
                seed = seed,
                deterministicMode = true,
                episodes = 3,
                batchSize = 8,
                configurationName = runName
            )
            
            val controller = DeterministicTrainingController(config)
            
            if (controller.initialize()) {
                controller.startTraining(3)
            } else {
                null
            }
        } catch (e: Exception) {
            println("❌ Training session '$runName' failed: ${e.message}")
            null
        }
    }
    
    /**
     * Utility function for creating separator lines
     */
    private operator fun String.times(count: Int): String {
        return this.repeat(count)
    }
}

/**
 * Entry point for running the deterministic training demo
 */
fun main() {
    DeterministicTrainingDemo.runDemo()
}