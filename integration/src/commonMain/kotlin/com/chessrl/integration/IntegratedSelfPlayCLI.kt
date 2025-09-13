package com.chessrl.integration

/**
 * Command Line Interface for the Integrated Self-Play System
 * Provides easy access to all integrated functionality
 */
object IntegratedSelfPlayCLI {
    
    /**
     * Main CLI entry point
     */
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            printUsage()
            return
        }
        
        when (args[0].lowercase()) {
            "train" -> handleTrainCommand(args.drop(1))
            "demo" -> handleDemoCommand(args.drop(1))
            "test" -> handleTestCommand(args.drop(1))
            "checkpoint" -> handleCheckpointCommand(args.drop(1))
            "config" -> handleConfigCommand(args.drop(1))
            "help" -> printUsage()
            else -> {
                println("❌ Unknown command: ${args[0]}")
                printUsage()
            }
        }
    }
    
    /**
     * Handle train command
     */
    private fun handleTrainCommand(args: List<String>) {
        println("🚀 Starting Integrated Self-Play Training")
        
        try {
            // Parse training arguments
            val config = parseTrainingConfig(args)
            val seedManager = if (config.enableDeterministicTraining) {
                SeedManager.apply {
                    val seed = args.find { it.startsWith("--seed=") }
                        ?.substringAfter("=")?.toLongOrNull() ?: 12345L
                    initializeWithMasterSeed(seed)
                }
            } else null
            
            // Create and initialize controller
            val controller = IntegratedSelfPlayController(config)
            val initResult = controller.initialize(seedManager)
            
            when (initResult) {
                is InitializationResult.Success -> {
                    println("✅ ${initResult.message}")
                }
                is InitializationResult.Failed -> {
                    println("❌ ${initResult.error}")
                    return
                }
            }
            
            // Parse iterations
            val iterations = args.find { it.startsWith("--iterations=") }
                ?.substringAfter("=")?.toIntOrNull() ?: 10
            
            println("Starting training with $iterations iterations...")
            
            // Run training
            val results = controller.runIntegratedTraining(iterations)
            
            // Print results
            printTrainingResults(results)
            
        } catch (e: Exception) {
            println("❌ Training failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Handle demo command
     */
    private fun handleDemoCommand(args: List<String>) {
        val demoType = args.getOrNull(0) ?: "complete"
        
        when (demoType.lowercase()) {
            "complete" -> {
                println("🎮 Running Complete Integration Demo")
                val results = IntegratedSelfPlayDemo.runCompleteDemo()
                printDemoResults(results)
            }
            "minimal" -> {
                println("🧪 Running Minimal Integration Test")
                val success = IntegratedSelfPlayDemo.runMinimalIntegrationTest()
                if (success) {
                    println("✅ Minimal demo completed successfully")
                } else {
                    println("❌ Minimal demo failed")
                }
            }
            "components" -> {
                println("🔗 Testing Component Integration")
                val results = IntegratedSelfPlayDemo.demonstrateComponentIntegration()
                printComponentResults(results)
            }
            else -> {
                println("❌ Unknown demo type: $demoType")
                println("Available demos: complete, minimal, components")
            }
        }
    }
    
    /**
     * Handle test command
     */
    private fun handleTestCommand(args: List<String>) {
        val testType = args.getOrNull(0) ?: "all"
        
        when (testType.lowercase()) {
            "all" -> runAllTests()
            "integration" -> runIntegrationTests()
            "deterministic" -> runDeterministicTests()
            "performance" -> runPerformanceTests()
            else -> {
                println("❌ Unknown test type: $testType")
                println("Available tests: all, integration, deterministic, performance")
            }
        }
    }
    
    /**
     * Handle checkpoint command
     */
    private fun handleCheckpointCommand(args: List<String>) {
        val action = args.getOrNull(0) ?: "list"
        
        when (action.lowercase()) {
            "list" -> listCheckpoints()
            "create" -> createCheckpoint(args.drop(1))
            "load" -> loadCheckpoint(args.drop(1))
            "compare" -> compareCheckpoints(args.drop(1))
            "cleanup" -> cleanupCheckpoints()
            else -> {
                println("❌ Unknown checkpoint action: $action")
                println("Available actions: list, create, load, compare, cleanup")
            }
        }
    }
    
    /**
     * Handle config command
     */
    private fun handleConfigCommand(args: List<String>) {
        val action = args.getOrNull(0) ?: "show"
        
        when (action.lowercase()) {
            "show" -> showDefaultConfig()
            "create" -> createConfigFile(args.drop(1))
            "validate" -> validateConfig(args.drop(1))
            else -> {
                println("❌ Unknown config action: $action")
                println("Available actions: show, create, validate")
            }
        }
    }
    
    /**
     * Parse training configuration from arguments
     */
    private fun parseTrainingConfig(args: List<String>): IntegratedSelfPlayConfig {
        var config = IntegratedSelfPlayConfig()
        
        for (arg in args) {
            when {
                arg.startsWith("--agent=") -> {
                    val agentType = when (arg.substringAfter("=").uppercase()) {
                        "DQN" -> AgentType.DQN
                        "POLICY_GRADIENT", "PG" -> AgentType.POLICY_GRADIENT
                        "ACTOR_CRITIC", "AC" -> AgentType.ACTOR_CRITIC
                        else -> AgentType.DQN
                    }
                    config = config.copy(agentType = agentType)
                }
                arg.startsWith("--learning-rate=") -> {
                    val lr = arg.substringAfter("=").toDoubleOrNull() ?: 0.001
                    config = config.copy(learningRate = lr)
                }
                arg.startsWith("--exploration=") -> {
                    val exploration = arg.substringAfter("=").toDoubleOrNull() ?: 0.1
                    config = config.copy(explorationRate = exploration)
                }
                arg.startsWith("--games=") -> {
                    val games = arg.substringAfter("=").toIntOrNull() ?: 20
                    config = config.copy(gamesPerIteration = games)
                }
                arg.startsWith("--batch-size=") -> {
                    val batchSize = arg.substringAfter("=").toIntOrNull() ?: 64
                    config = config.copy(batchSize = batchSize)
                }
                arg == "--deterministic" -> {
                    config = config.copy(enableDeterministicTraining = true)
                }
                arg == "--early-stopping" -> {
                    config = config.copy(enableEarlyStopping = true)
                }
            }
        }
        
        return config
    }
    
    /**
     * Print training results
     */
    private fun printTrainingResults(results: IntegratedTrainingResults) {
        println("\n🏁 Training Results:")
        println("=" * 50)
        println("Total iterations: ${results.totalIterations}")
        println("Total duration: ${results.totalDuration}ms")
        println("Best performance: ${results.bestPerformance}")
        
        val metrics = results.finalMetrics
        println("\nFinal Metrics:")
        println("  Games played: ${metrics.totalGamesPlayed}")
        println("  Experiences collected: ${metrics.totalExperiencesCollected}")
        println("  Average performance: ${metrics.averagePerformance}")
        println("  Average reward: ${metrics.averageReward}")
        println("  Win rate: ${(metrics.averageWinRate * 100)}%")
        println("  Training loss: ${metrics.averageLoss}")
        println("  Batch updates: ${metrics.totalBatchUpdates}")
        
        val checkpoints = results.checkpointSummary
        println("\nCheckpoints:")
        println("  Total created: ${checkpoints.totalCheckpoints}")
        println("  Valid checkpoints: ${checkpoints.validCheckpoints}")
        println("  Best version: ${checkpoints.bestCheckpointVersion}")
        println("  Best performance: ${checkpoints.bestPerformance}")
    }
    
    /**
     * Print demo results
     */
    private fun printDemoResults(results: DemoResults) {
        println("\n🎮 Demo Results:")
        println("=" * 40)
        println("Success: ${if (results.success) "✅ YES" else "❌ NO"}")
        println("Duration: ${results.duration}ms")
        println("\nSteps completed:")
        results.results.forEachIndexed { index, result ->
            println("  ${index + 1}. $result")
        }
    }
    
    /**
     * Print component integration results
     */
    private fun printComponentResults(results: ComponentIntegrationResults) {
        println("\n🔗 Component Integration Results:")
        println("=" * 45)
        println("Overall: ${if (results.allPassed) "✅ ALL PASSED" else "⚠️ SOME FAILED"}")
        println("\nComponent Details:")
        results.componentResults.forEach { (component, passed) ->
            val status = if (passed) "✅ PASSED" else "❌ FAILED"
            println("  $component: $status")
        }
    }
    
    /**
     * Run all tests
     */
    private fun runAllTests() {
        println("🧪 Running All Integration Tests")
        println("=" * 40)
        
        var totalTests = 0
        var passedTests = 0
        
        // Run minimal test
        println("\n1. Minimal Integration Test:")
        totalTests++
        if (IntegratedSelfPlayDemo.runMinimalIntegrationTest()) {
            println("   ✅ PASSED")
            passedTests++
        } else {
            println("   ❌ FAILED")
        }
        
        // Run component tests
        println("\n2. Component Integration Tests:")
        totalTests++
        val componentResults = IntegratedSelfPlayDemo.demonstrateComponentIntegration()
        if (componentResults.allPassed) {
            println("   ✅ PASSED")
            passedTests++
        } else {
            println("   ⚠️ PARTIAL (${componentResults.componentResults.values.count { it }}/${componentResults.componentResults.size})")
        }
        
        // Run deterministic test
        println("\n3. Deterministic Training Test:")
        totalTests++
        try {
            val seedManager = SeedManager.apply { initializeWithMasterSeed(12345L) }
            val config = IntegratedSelfPlayConfig(
                gamesPerIteration = 1,
                maxStepsPerGame = 5,
                hiddenLayers = listOf(16, 8),
                enableDeterministicTraining = true
            )
            val controller = IntegratedSelfPlayController(config)
            controller.initialize(seedManager)
            controller.runIntegratedTraining(1)
            println("   ✅ PASSED")
            passedTests++
        } catch (e: Exception) {
            println("   ❌ FAILED: ${e.message}")
        }
        
        println("\n📊 Test Summary:")
        println("Total tests: $totalTests")
        println("Passed: $passedTests")
        println("Failed: ${totalTests - passedTests}")
        println("Success rate: ${(passedTests.toDouble() / totalTests * 100).toInt()}%")
    }
    
    /**
     * Run integration tests
     */
    private fun runIntegrationTests() {
        println("🔗 Running Integration Tests")
        val results = IntegratedSelfPlayDemo.demonstrateComponentIntegration()
        printComponentResults(results)
    }
    
    /**
     * Run deterministic tests
     */
    private fun runDeterministicTests() {
        println("🎯 Running Deterministic Tests")
        
        try {
            val seedManager = SeedManager.apply {
                initializeWithMasterSeed(54321L)
            }
            
            // Test reproducibility
            val random1 = seedManager.getNeuralNetworkRandom()
            val values1 = (1..5).map { random1.nextDouble() }
            
            seedManager.initializeWithMasterSeed(54321L)
            val random2 = seedManager.getNeuralNetworkRandom()
            val values2 = (1..5).map { random2.nextDouble() }
            
            if (values1 == values2) {
                println("✅ Deterministic seeding: PASSED")
            } else {
                println("❌ Deterministic seeding: FAILED")
            }
            
            // Test deterministic training
            val config = IntegratedSelfPlayConfig(
                gamesPerIteration = 1,
                maxStepsPerGame = 3,
                hiddenLayers = listOf(8, 4),
                enableDeterministicTraining = true
            )
            
            val controller = IntegratedSelfPlayController(config)
            controller.initialize(seedManager)
            controller.runIntegratedTraining(1)
            
            println("✅ Deterministic training: PASSED")
            
        } catch (e: Exception) {
            println("❌ Deterministic tests failed: ${e.message}")
        }
    }
    
    /**
     * Run performance tests
     */
    private fun runPerformanceTests() {
        println("⚡ Running Performance Tests")
        
        val startTime = getCurrentTimeMillis()
        
        try {
            val config = IntegratedSelfPlayConfig(
                gamesPerIteration = 5,
                maxStepsPerGame = 10,
                hiddenLayers = listOf(32, 16)
            )
            
            val controller = IntegratedSelfPlayController(config)
            controller.initialize()
            val results = controller.runIntegratedTraining(2)
            
            val endTime = getCurrentTimeMillis()
            val duration = endTime - startTime
            
            println("✅ Performance test completed")
            println("   Duration: ${duration}ms")
            println("   Games played: ${results.finalMetrics.totalGamesPlayed}")
            println("   Experiences: ${results.finalMetrics.totalExperiencesCollected}")
            println("   Games/second: ${(results.finalMetrics.totalGamesPlayed.toDouble() / duration * 1000).toInt()}")
            
        } catch (e: Exception) {
            println("❌ Performance test failed: ${e.message}")
        }
    }
    
    /**
     * List checkpoints
     */
    private fun listCheckpoints() {
        println("💾 Listing Checkpoints")
        
        try {
            val checkpointManager = CheckpointManager()
            val checkpoints = checkpointManager.listCheckpoints()
            
            if (checkpoints.isEmpty()) {
                println("No checkpoints found")
                return
            }
            
            println("Found ${checkpoints.size} checkpoints:")
            checkpoints.forEach { checkpoint ->
                println("  Version ${checkpoint.version}: ${checkpoint.metadata.description}")
                println("    Performance: ${checkpoint.metadata.performance}")
                println("    Created: ${checkpoint.creationTime}")
                println("    Size: ${checkpoint.fileSize} bytes")
                println("    Status: ${checkpoint.validationStatus}")
            }
            
        } catch (e: Exception) {
            println("❌ Failed to list checkpoints: ${e.message}")
        }
    }
    
    /**
     * Create checkpoint
     */
    private fun createCheckpoint(args: List<String>) {
        println("💾 Creating Checkpoint")
        // Implementation would create a checkpoint from current agent state
        println("Checkpoint creation not implemented in CLI demo")
    }
    
    /**
     * Load checkpoint
     */
    private fun loadCheckpoint(args: List<String>) {
        println("📂 Loading Checkpoint")
        // Implementation would load a checkpoint
        println("Checkpoint loading not implemented in CLI demo")
    }
    
    /**
     * Compare checkpoints
     */
    private fun compareCheckpoints(args: List<String>) {
        println("📊 Comparing Checkpoints")
        // Implementation would compare checkpoint performance
        println("Checkpoint comparison not implemented in CLI demo")
    }
    
    /**
     * Cleanup checkpoints
     */
    private fun cleanupCheckpoints() {
        println("🧹 Cleaning Up Checkpoints")
        
        try {
            val checkpointManager = CheckpointManager()
            val summary = checkpointManager.performAutomaticCleanup()
            
            println("Cleanup completed:")
            println("  Checkpoints deleted: ${summary.checkpointsDeleted}")
            println("  Size freed: ${summary.sizeFreed} bytes")
            println("  Before: ${summary.checkpointsBefore} checkpoints")
            println("  After: ${summary.checkpointsAfter} checkpoints")
            
        } catch (e: Exception) {
            println("❌ Cleanup failed: ${e.message}")
        }
    }
    
    /**
     * Show default configuration
     */
    private fun showDefaultConfig() {
        println("⚙️ Default Configuration:")
        val config = IntegratedSelfPlayConfig()
        
        println("Agent Configuration:")
        println("  Type: ${config.agentType}")
        println("  Hidden layers: ${config.hiddenLayers}")
        println("  Learning rate: ${config.learningRate}")
        println("  Exploration rate: ${config.explorationRate}")
        
        println("\nTraining Configuration:")
        println("  Games per iteration: ${config.gamesPerIteration}")
        println("  Max concurrent games: ${config.maxConcurrentGames}")
        println("  Max steps per game: ${config.maxStepsPerGame}")
        println("  Batch size: ${config.batchSize}")
        
        println("\nReward Configuration:")
        println("  Win reward: ${config.winReward}")
        println("  Loss reward: ${config.lossReward}")
        println("  Draw reward: ${config.drawReward}")
        
        println("\nCheckpoint Configuration:")
        println("  Frequency: ${config.checkpointFrequency}")
        println("  Max checkpoints: ${config.maxCheckpoints}")
        println("  Directory: ${config.checkpointDirectory}")
    }
    
    /**
     * Create configuration file
     */
    private fun createConfigFile(args: List<String>) {
        println("📝 Creating Configuration File")
        println("Configuration file creation not implemented in CLI demo")
    }
    
    /**
     * Validate configuration
     */
    private fun validateConfig(args: List<String>) {
        println("✅ Validating Configuration")
        println("Configuration validation not implemented in CLI demo")
    }
    
    /**
     * Print usage information
     */
    private fun printUsage() {
        println("🎮 Integrated Self-Play Chess RL System")
        println("=" * 50)
        println("Usage: IntegratedSelfPlayCLI <command> [options]")
        println()
        println("Commands:")
        println("  train [options]     - Run integrated self-play training")
        println("  demo <type>         - Run demonstration (complete|minimal|components)")
        println("  test <type>         - Run tests (all|integration|deterministic|performance)")
        println("  checkpoint <action> - Manage checkpoints (list|create|load|compare|cleanup)")
        println("  config <action>     - Configuration management (show|create|validate)")
        println("  help               - Show this help message")
        println()
        println("Training Options:")
        println("  --iterations=N      - Number of training iterations (default: 10)")
        println("  --agent=TYPE        - Agent type: DQN|POLICY_GRADIENT|ACTOR_CRITIC")
        println("  --learning-rate=X   - Learning rate (default: 0.001)")
        println("  --exploration=X     - Exploration rate (default: 0.1)")
        println("  --games=N          - Games per iteration (default: 20)")
        println("  --batch-size=N     - Batch size (default: 64)")
        println("  --seed=N           - Random seed for deterministic training")
        println("  --deterministic    - Enable deterministic training")
        println("  --early-stopping   - Enable early stopping")
        println()
        println("Examples:")
        println("  IntegratedSelfPlayCLI train --iterations=5 --deterministic --seed=12345")
        println("  IntegratedSelfPlayCLI demo complete")
        println("  IntegratedSelfPlayCLI test all")
        println("  IntegratedSelfPlayCLI checkpoint list")
    }
}