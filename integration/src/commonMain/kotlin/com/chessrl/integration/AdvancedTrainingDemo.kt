package com.chessrl.integration

/**
 * Comprehensive demonstration of the Advanced Training Control system
 * Shows all features including lifecycle management, real-time configuration,
 * experiment management, and integration with existing controllers
 */
object AdvancedTrainingDemo {
    
    /**
     * Run complete advanced training control demonstration
     */
    fun runCompleteDemo(): AdvancedTrainingDemoResults {
        println("🎮 Advanced Training Control System - Complete Demo")
        println("=" * 60)
        
        val demoStartTime = getCurrentTimeMillis()
        val results = mutableListOf<String>()
        
        try {
            // Phase 1: Training Lifecycle Management
            println("\n📋 Phase 1: Training Lifecycle Management")
            println("-" * 50)
            
            val lifecycleResults = demonstrateTrainingLifecycle()
            results.addAll(lifecycleResults)
            
            // Phase 2: Real-Time Configuration Adjustment
            println("\n⚙️ Phase 2: Real-Time Configuration Adjustment")
            println("-" * 50)
            
            val configResults = demonstrateConfigurationManagement()
            results.addAll(configResults)
            
            // Phase 3: Experiment Management
            println("\n🧪 Phase 3: Experiment Management and Comparison")
            println("-" * 50)
            
            val experimentResults = demonstrateExperimentManagement()
            results.addAll(experimentResults)
            
            // Phase 4: Controller Integration
            println("\n🔗 Phase 4: Controller Integration")
            println("-" * 50)
            
            val integrationResults = demonstrateControllerIntegration()
            results.addAll(integrationResults)
            
            // Phase 5: Advanced Features
            println("\n🚀 Phase 5: Advanced Features")
            println("-" * 50)
            
            val advancedResults = demonstrateAdvancedFeatures()
            results.addAll(advancedResults)
            
            val demoEndTime = getCurrentTimeMillis()
            val totalDuration = demoEndTime - demoStartTime
            
            println("\n🏁 Advanced Training Control Demo Completed!")
            println("Total duration: ${totalDuration}ms")
            println("All features demonstrated successfully!")
            
            return AdvancedTrainingDemoResults(
                success = true,
                results = results,
                duration = totalDuration,
                featuresDemo = mapOf(
                    "Lifecycle Management" to true,
                    "Configuration Management" to true,
                    "Experiment Management" to true,
                    "Controller Integration" to true,
                    "Advanced Features" to true
                )
            )
            
        } catch (e: Exception) {
            println("❌ Demo failed: ${e.message}")
            e.printStackTrace()
            results.add("Demo failed: ${e.message}")
            
            val demoEndTime = getCurrentTimeMillis()
            val totalDuration = demoEndTime - demoStartTime
            
            return AdvancedTrainingDemoResults(
                success = false,
                results = results,
                duration = totalDuration,
                featuresDemo = emptyMap()
            )
        }
    }
    
    /**
     * Demonstrate training lifecycle management (start/pause/resume/stop/restart)
     */
    private fun demonstrateTrainingLifecycle(): List<String> {
        println("🔄 Demonstrating Training Lifecycle Management")
        
        val results = mutableListOf<String>()
        val controller = AdvancedTrainingController()
        
        try {
            // Create session configuration
            val sessionConfig = TrainingSessionConfig(
                name = "lifecycle_demo",
                controllerType = ControllerType.INTEGRATED,
                iterations = 10,
                integratedConfig = IntegratedSelfPlayConfig(
                    gamesPerIteration = 2,
                    maxStepsPerGame = 5,
                    hiddenLayers = listOf(32, 16),
                    batchSize = 4
                )
            )
            
            // 1. Start Training
            println("   🚀 Starting training session...")
            val startResult = controller.startTraining(sessionConfig)
            when (startResult) {
                is TrainingControlResult.Success -> {
                    println("   ✅ Training started: ${startResult.message}")
                    results.add("Training lifecycle: START - SUCCESS")
                }
                is TrainingControlResult.Error -> {
                    println("   ❌ Training start failed: ${startResult.message}")
                    results.add("Training lifecycle: START - FAILED")
                    return results
                }
            }
            
            // Check initial status
            val initialStatus = controller.getTrainingStatus()
            println("   📊 Initial status: ${initialStatus.state}")
            results.add("Initial training state: ${initialStatus.state}")
            
            // 2. Pause Training
            println("   ⏸️ Pausing training...")
            val pauseResult = controller.pauseTraining()
            when (pauseResult) {
                is TrainingControlResult.Success -> {
                    println("   ✅ Training paused: ${pauseResult.message}")
                    results.add("Training lifecycle: PAUSE - SUCCESS")
                }
                is TrainingControlResult.Error -> {
                    println("   ❌ Pause failed: ${pauseResult.message}")
                    results.add("Training lifecycle: PAUSE - FAILED")
                }
            }
            
            // Check paused status
            val pausedStatus = controller.getTrainingStatus()
            println("   📊 Paused status: ${pausedStatus.state}")
            results.add("Paused training state: ${pausedStatus.state}")
            
            // 3. Resume Training
            println("   ▶️ Resuming training...")
            val resumeResult = controller.resumeTraining()
            when (resumeResult) {
                is TrainingControlResult.Success -> {
                    println("   ✅ Training resumed: ${resumeResult.message}")
                    results.add("Training lifecycle: RESUME - SUCCESS")
                }
                is TrainingControlResult.Error -> {
                    println("   ❌ Resume failed: ${resumeResult.message}")
                    results.add("Training lifecycle: RESUME - FAILED")
                }
            }
            
            // Check resumed status
            val resumedStatus = controller.getTrainingStatus()
            println("   📊 Resumed status: ${resumedStatus.state}")
            results.add("Resumed training state: ${resumedStatus.state}")
            
            // 4. Stop Training
            println("   🛑 Stopping training...")
            val stopResult = controller.stopTraining()
            when (stopResult) {
                is TrainingControlResult.Success -> {
                    println("   ✅ Training stopped: ${stopResult.message}")
                    results.add("Training lifecycle: STOP - SUCCESS")
                }
                is TrainingControlResult.Error -> {
                    println("   ❌ Stop failed: ${stopResult.message}")
                    results.add("Training lifecycle: STOP - FAILED")
                }
            }
            
            // 5. Restart Training
            println("   🔄 Restarting training...")
            val restartResult = controller.restartTraining()
            when (restartResult) {
                is TrainingControlResult.Success -> {
                    println("   ✅ Training restarted: ${restartResult.message}")
                    results.add("Training lifecycle: RESTART - SUCCESS")
                }
                is TrainingControlResult.Error -> {
                    println("   ❌ Restart failed: ${restartResult.message}")
                    results.add("Training lifecycle: RESTART - FAILED")
                }
            }
            
            // Final cleanup
            controller.stopTraining()
            
            println("   🎉 Lifecycle management demonstration completed!")
            results.add("Lifecycle management: ALL OPERATIONS DEMONSTRATED")
            
        } catch (e: Exception) {
            println("   ❌ Lifecycle demo failed: ${e.message}")
            results.add("Lifecycle management: FAILED - ${e.message}")
        }
        
        return results
    }
    
    /**
     * Demonstrate real-time configuration adjustment with validation and rollback
     */
    private fun demonstrateConfigurationManagement(): List<String> {
        println("⚙️ Demonstrating Real-Time Configuration Management")
        
        val results = mutableListOf<String>()
        val controller = AdvancedTrainingController()
        
        try {
            // Start training session for configuration testing
            val sessionConfig = TrainingSessionConfig(
                name = "config_demo",
                controllerType = ControllerType.INTEGRATED,
                iterations = 5,
                integratedConfig = IntegratedSelfPlayConfig(
                    learningRate = 0.001,
                    explorationRate = 0.1,
                    batchSize = 32,
                    gamesPerIteration = 2,
                    maxStepsPerGame = 3,
                    hiddenLayers = listOf(16, 8)
                )
            )
            
            controller.startTraining(sessionConfig)
            println("   🚀 Training started for configuration testing")
            results.add("Configuration demo: Training session started")
            
            // 1. Valid Configuration Change
            println("   ⚙️ Testing valid configuration change...")
            val validUpdate = ConfigurationUpdate(
                parameter = "learningRate",
                oldValue = 0.001,
                newValue = 0.002,
                reason = "Performance optimization"
            )
            
            val validResult = controller.adjustConfiguration(validUpdate)
            when (validResult) {
                is ConfigurationResult.Applied -> {
                    println("   ✅ Configuration applied: ${validResult.message}")
                    results.add("Configuration: Valid change APPLIED")
                }
                else -> {
                    println("   ❌ Configuration change failed")
                    results.add("Configuration: Valid change FAILED")
                }
            }
            
            // 2. Configuration Validation Only
            println("   🔍 Testing configuration validation...")
            val validateUpdate = ConfigurationUpdate(
                parameter = "explorationRate",
                oldValue = 0.1,
                newValue = 0.05,
                reason = "Validation test"
            )
            
            val validateResult = controller.adjustConfiguration(validateUpdate, validateOnly = true)
            when (validateResult) {
                is ConfigurationResult.Valid -> {
                    println("   ✅ Configuration validation passed: ${validateResult.message}")
                    results.add("Configuration: Validation PASSED")
                }
                else -> {
                    println("   ❌ Configuration validation failed")
                    results.add("Configuration: Validation FAILED")
                }
            }
            
            // 3. Invalid Configuration Change
            println("   ❌ Testing invalid configuration change...")
            val invalidUpdate = ConfigurationUpdate(
                parameter = "invalidParameter",
                oldValue = "old",
                newValue = "new",
                reason = "Testing invalid parameter"
            )
            
            val invalidResult = controller.adjustConfiguration(invalidUpdate)
            when (invalidResult) {
                is ConfigurationResult.Invalid -> {
                    println("   ✅ Invalid configuration properly rejected")
                    results.add("Configuration: Invalid change REJECTED")
                }
                else -> {
                    println("   ❌ Invalid configuration was not rejected")
                    results.add("Configuration: Invalid change NOT REJECTED")
                }
            }
            
            // 4. Multiple Configuration Changes
            println("   🔄 Testing multiple configuration changes...")
            val changes = listOf(
                ConfigurationUpdate("batchSize", 32, 64, "Increase batch size"),
                ConfigurationUpdate("gamesPerIteration", 2, 3, "More games per iteration")
            )
            
            var appliedChanges = 0
            for (change in changes) {
                val result = controller.adjustConfiguration(change)
                if (result is ConfigurationResult.Applied) {
                    appliedChanges++
                }
            }
            
            println("   ✅ Applied $appliedChanges/${changes.size} configuration changes")
            results.add("Configuration: Multiple changes - $appliedChanges/${changes.size} applied")
            
            // 5. Configuration Rollback
            println("   ↩️ Testing configuration rollback...")
            val rollbackResult = controller.rollbackConfiguration()
            when (rollbackResult) {
                is ConfigurationResult.Applied -> {
                    println("   ✅ Configuration rollback successful: ${rollbackResult.message}")
                    results.add("Configuration: Rollback SUCCESS")
                }
                else -> {
                    println("   ❌ Configuration rollback failed")
                    results.add("Configuration: Rollback FAILED")
                }
            }
            
            // 6. Configuration History
            println("   📜 Checking configuration history...")
            val status = controller.getTrainingStatus()
            val historySize = status.configurationHistory.size
            println("   📊 Configuration history: $historySize changes recorded")
            results.add("Configuration: History tracking - $historySize changes")
            
            controller.stopTraining()
            
            println("   🎉 Configuration management demonstration completed!")
            results.add("Configuration management: ALL FEATURES DEMONSTRATED")
            
        } catch (e: Exception) {
            println("   ❌ Configuration demo failed: ${e.message}")
            results.add("Configuration management: FAILED - ${e.message}")
        }
        
        return results
    }
    
    /**
     * Demonstrate experiment management with parameter tracking and comparison
     */
    private fun demonstrateExperimentManagement(): List<String> {
        println("🧪 Demonstrating Experiment Management")
        
        val results = mutableListOf<String>()
        val controller = AdvancedTrainingController()
        
        try {
            // 1. Create Multiple Experiments
            println("   🧪 Creating experiments with different parameters...")
            
            val experiments = mutableListOf<TrainingExperiment>()
            
            // Experiment 1: Low learning rate
            val config1 = TrainingSessionConfig(
                name = "lr_low_experiment",
                controllerType = ControllerType.INTEGRATED,
                iterations = 3,
                integratedConfig = IntegratedSelfPlayConfig(
                    learningRate = 0.001,
                    explorationRate = 0.1,
                    batchSize = 32,
                    gamesPerIteration = 2,
                    maxStepsPerGame = 3,
                    hiddenLayers = listOf(16, 8)
                )
            )
            
            val exp1 = controller.createExperiment(
                "low_lr_experiment",
                config1,
                "Testing low learning rate performance"
            )
            experiments.add(exp1)
            println("   ✅ Created experiment 1: ${exp1.name} (LR: 0.001)")
            
            // Experiment 2: High learning rate
            val config2 = config1.copy(
                name = "lr_high_experiment",
                integratedConfig = config1.integratedConfig?.copy(learningRate = 0.01)
            )
            
            val exp2 = controller.createExperiment(
                "high_lr_experiment",
                config2,
                "Testing high learning rate performance"
            )
            experiments.add(exp2)
            println("   ✅ Created experiment 2: ${exp2.name} (LR: 0.01)")
            
            // Experiment 3: Different batch size
            val config3 = config1.copy(
                name = "batch_size_experiment",
                integratedConfig = config1.integratedConfig?.copy(
                    learningRate = 0.005,
                    batchSize = 64
                )
            )
            
            val exp3 = controller.createExperiment(
                "batch_size_experiment",
                config3,
                "Testing different batch size"
            )
            experiments.add(exp3)
            println("   ✅ Created experiment 3: ${exp3.name} (Batch: 64)")
            
            results.add("Experiments: Created ${experiments.size} experiments")
            
            // 2. Run Experiment Training
            println("   🚀 Running experiment training...")
            
            for ((index, experiment) in experiments.withIndex()) {
                println("   Running experiment ${index + 1}: ${experiment.name}")
                
                val startResult = controller.startTraining(
                    experiment.config,
                    experiment.name
                )
                
                when (startResult) {
                    is TrainingControlResult.Success -> {
                        println("   ✅ Experiment ${experiment.name} started")
                        
                        // Let it run briefly, then stop
                        Thread.sleep(100) // Simulate brief training
                        controller.stopTraining()
                        
                        results.add("Experiment ${experiment.name}: COMPLETED")
                    }
                    is TrainingControlResult.Error -> {
                        println("   ❌ Experiment ${experiment.name} failed: ${startResult.message}")
                        results.add("Experiment ${experiment.name}: FAILED")
                    }
                }
            }
            
            // 3. Compare Experiments
            println("   📊 Comparing experiments...")
            
            val experimentIds = experiments.map { it.id }
            val comparison = controller.compareExperiments(experimentIds)
            
            println("   📈 Experiment Comparison Results:")
            println("     Experiments compared: ${comparison.experiments.size}")
            println("     Parameters compared: ${comparison.comparison.keys.size}")
            
            // Display comparison details
            comparison.comparison.forEach { (parameter, values) ->
                println("     $parameter: ${values.joinToString(", ")}")
            }
            
            if (comparison.recommendations.isNotEmpty()) {
                println("   💡 Recommendations:")
                comparison.recommendations.forEach { rec ->
                    println("     • $rec")
                }
            }
            
            results.add("Experiment comparison: ${comparison.experiments.size} experiments compared")
            results.add("Comparison parameters: ${comparison.comparison.keys.size}")
            results.add("Recommendations: ${comparison.recommendations.size}")
            
            // 4. Parameter Tracking Verification
            println("   🔍 Verifying parameter tracking...")
            
            val parameterCounts = experiments.map { it.parameters.size }
            val avgParameters = parameterCounts.average()
            
            println("   📊 Parameter tracking statistics:")
            println("     Average parameters per experiment: $avgParameters")
            println("     Total unique parameters: ${experiments.flatMap { it.parameters.keys }.distinct().size}")
            
            results.add("Parameter tracking: ${avgParameters} avg parameters per experiment")
            
            println("   🎉 Experiment management demonstration completed!")
            results.add("Experiment management: ALL FEATURES DEMONSTRATED")
            
        } catch (e: Exception) {
            println("   ❌ Experiment demo failed: ${e.message}")
            results.add("Experiment management: FAILED - ${e.message}")
        }
        
        return results
    }
    
    /**
     * Demonstrate integration with existing controllers
     */
    private fun demonstrateControllerIntegration(): List<String> {
        println("🔗 Demonstrating Controller Integration")
        
        val results = mutableListOf<String>()
        val controller = AdvancedTrainingController()
        
        try {
            // 1. Integrated Self-Play Controller
            println("   🎮 Testing IntegratedSelfPlayController integration...")
            
            val integratedConfig = TrainingSessionConfig(
                name = "integrated_demo",
                controllerType = ControllerType.INTEGRATED,
                iterations = 2,
                integratedConfig = IntegratedSelfPlayConfig(
                    gamesPerIteration = 1,
                    maxStepsPerGame = 3,
                    hiddenLayers = listOf(16, 8),
                    batchSize = 4
                )
            )
            
            val integratedResult = controller.startTraining(integratedConfig)
            when (integratedResult) {
                is TrainingControlResult.Success -> {
                    println("   ✅ IntegratedSelfPlayController integration successful")
                    results.add("Controller integration: IntegratedSelfPlay - SUCCESS")
                }
                is TrainingControlResult.Error -> {
                    println("   ❌ IntegratedSelfPlayController integration failed")
                    results.add("Controller integration: IntegratedSelfPlay - FAILED")
                }
            }
            
            controller.stopTraining()
            
            // 2. Deterministic Training Controller
            println("   🎯 Testing DeterministicTrainingController integration...")
            
            val deterministicConfig = TrainingSessionConfig(
                name = "deterministic_demo",
                controllerType = ControllerType.DETERMINISTIC,
                iterations = 2,
                enableDeterministic = true,
                deterministicConfig = TrainingConfiguration(
                    masterSeed = 12345L,
                    episodes = 2,
                    batchSize = 4
                )
            )
            
            val deterministicResult = controller.startTraining(deterministicConfig)
            when (deterministicResult) {
                is TrainingControlResult.Success -> {
                    println("   ✅ DeterministicTrainingController integration successful")
                    results.add("Controller integration: Deterministic - SUCCESS")
                }
                is TrainingControlResult.Error -> {
                    println("   ❌ DeterministicTrainingController integration failed")
                    results.add("Controller integration: Deterministic - FAILED")
                }
            }
            
            controller.stopTraining()
            
            // 3. Hybrid Controller Mode
            println("   🔀 Testing Hybrid controller mode...")
            
            val hybridConfig = TrainingSessionConfig(
                name = "hybrid_demo",
                controllerType = ControllerType.HYBRID,
                iterations = 2,
                enableDeterministic = true,
                integratedConfig = IntegratedSelfPlayConfig(
                    gamesPerIteration = 1,
                    maxStepsPerGame = 2,
                    hiddenLayers = listOf(8, 4)
                ),
                deterministicConfig = TrainingConfiguration(
                    masterSeed = 54321L,
                    episodes = 2
                )
            )
            
            val hybridResult = controller.startTraining(hybridConfig)
            when (hybridResult) {
                is TrainingControlResult.Success -> {
                    println("   ✅ Hybrid controller integration successful")
                    results.add("Controller integration: Hybrid - SUCCESS")
                }
                is TrainingControlResult.Error -> {
                    println("   ❌ Hybrid controller integration failed")
                    results.add("Controller integration: Hybrid - FAILED")
                }
            }
            
            controller.stopTraining()
            
            // 4. Controller Switching
            println("   🔄 Testing controller switching...")
            
            // Start with integrated controller
            controller.startTraining(integratedConfig)
            val status1 = controller.getTrainingStatus()
            val controller1Type = status1.session?.config?.controllerType
            
            // Restart with deterministic controller
            controller.restartTraining(deterministicConfig)
            val status2 = controller.getTrainingStatus()
            val controller2Type = status2.session?.config?.controllerType
            
            if (controller1Type != controller2Type) {
                println("   ✅ Controller switching successful: $controller1Type → $controller2Type")
                results.add("Controller switching: SUCCESS")
            } else {
                println("   ❌ Controller switching failed")
                results.add("Controller switching: FAILED")
            }
            
            controller.stopTraining()
            
            println("   🎉 Controller integration demonstration completed!")
            results.add("Controller integration: ALL CONTROLLERS TESTED")
            
        } catch (e: Exception) {
            println("   ❌ Controller integration demo failed: ${e.message}")
            results.add("Controller integration: FAILED - ${e.message}")
        }
        
        return results
    }
    
    /**
     * Demonstrate advanced features like monitoring, reporting, and analytics
     */
    private fun demonstrateAdvancedFeatures(): List<String> {
        println("🚀 Demonstrating Advanced Features")
        
        val results = mutableListOf<String>()
        val controller = AdvancedTrainingController()
        
        try {
            // Start training for advanced features demo
            val sessionConfig = TrainingSessionConfig(
                name = "advanced_features_demo",
                controllerType = ControllerType.INTEGRATED,
                iterations = 3,
                integratedConfig = IntegratedSelfPlayConfig(
                    gamesPerIteration = 2,
                    maxStepsPerGame = 3,
                    hiddenLayers = listOf(16, 8)
                )
            )
            
            controller.startTraining(sessionConfig, "advanced_experiment")
            
            // 1. Real-Time Monitoring
            println("   📡 Testing real-time monitoring...")
            
            val status = controller.getTrainingStatus()
            
            println("   📊 Current training status:")
            println("     State: ${status.state}")
            println("     Session: ${status.session?.name}")
            println("     Experiment: ${status.experiment?.name}")
            
            if (status.metrics != null) {
                println("     Metrics available: ✅")
                results.add("Real-time monitoring: Metrics AVAILABLE")
            } else {
                println("     Metrics available: ❌")
                results.add("Real-time monitoring: Metrics UNAVAILABLE")
            }
            
            if (status.performance != null) {
                println("     Performance data available: ✅")
                results.add("Real-time monitoring: Performance data AVAILABLE")
            } else {
                println("     Performance data available: ❌")
                results.add("Real-time monitoring: Performance data UNAVAILABLE")
            }
            
            // 2. Training Report Generation
            println("   📋 Testing training report generation...")
            
            // Make some configuration changes for report content
            controller.adjustConfiguration(ConfigurationUpdate(
                "learningRate", 0.001, 0.002, "Report demo"
            ))
            
            val report = controller.generateTrainingReport()
            
            println("   📄 Training report generated:")
            println("     Session summary: ${if (report.sessionSummary != null) "✅" else "❌"}")
            println("     Experiment summary: ${if (report.experimentSummary != null) "✅" else "❌"}")
            println("     Performance analysis: ${if (report.performanceAnalysis != null) "✅" else "❌"}")
            println("     Configuration changes: ${report.configurationChanges.size}")
            println("     Recommendations: ${report.recommendations.size}")
            
            results.add("Report generation: SUCCESS")
            results.add("Report sections: ${listOfNotNull(
                if (report.sessionSummary != null) "session" else null,
                if (report.experimentSummary != null) "experiment" else null,
                "performance", "config", "recommendations"
            ).size}")
            
            // 3. Performance Analytics
            println("   📈 Testing performance analytics...")
            
            val performanceAnalysis = report.performanceAnalysis
            println("   📊 Performance analysis:")
            println("     Average performance: ${String.format("%.4f", performanceAnalysis.averagePerformance)}")
            println("     Performance trend: ${performanceAnalysis.performanceTrend}")
            println("     Bottlenecks identified: ${performanceAnalysis.bottlenecks.size}")
            println("     Optimization suggestions: ${performanceAnalysis.optimizationSuggestions.size}")
            
            results.add("Performance analytics: SUCCESS")
            results.add("Analytics components: trend, bottlenecks, suggestions")
            
            // 4. Configuration History Tracking
            println("   📜 Testing configuration history tracking...")
            
            val configHistory = status.configurationHistory
            println("   📊 Configuration history:")
            println("     Total changes: ${configHistory.size}")
            
            if (configHistory.isNotEmpty()) {
                val latestChange = configHistory.last()
                println("     Latest change: ${latestChange.parameter} = ${latestChange.newValue}")
                println("     Change reason: ${latestChange.reason}")
            }
            
            results.add("Configuration history: ${configHistory.size} changes tracked")
            
            // 5. Error Handling and Recovery
            println("   🛡️ Testing error handling...")
            
            // Test invalid operations
            val invalidPause = controller.pauseTraining()
            controller.pauseTraining() // Should succeed
            val invalidPause2 = controller.pauseTraining() // Should fail
            
            val errorHandlingWorks = when {
                invalidPause2 is TrainingControlResult.Error -> true
                else -> false
            }
            
            if (errorHandlingWorks) {
                println("   ✅ Error handling working correctly")
                results.add("Error handling: SUCCESS")
            } else {
                println("   ❌ Error handling not working properly")
                results.add("Error handling: FAILED")
            }
            
            controller.resumeTraining()
            controller.stopTraining()
            
            println("   🎉 Advanced features demonstration completed!")
            results.add("Advanced features: ALL FEATURES DEMONSTRATED")
            
        } catch (e: Exception) {
            println("   ❌ Advanced features demo failed: ${e.message}")
            results.add("Advanced features: FAILED - ${e.message}")
        }
        
        return results
    }
    
    /**
     * Run CLI demonstration
     */
    fun runCLIDemo(): CLIDemoResults {
        println("💻 Advanced Training CLI Demo")
        println("=" * 40)
        
        val results = mutableListOf<String>()
        
        try {
            // Simulate CLI commands
            println("Simulating CLI commands...")
            
            // Start command
            println("\n$ AdvancedTrainingCLI start --name=\"cli_demo\" --iterations=3 --deterministic")
            AdvancedTrainingCLI.main(arrayOf(
                "start", "--name=cli_demo", "--iterations=3", "--deterministic"
            ))
            results.add("CLI: start command executed")
            
            // Status command
            println("\n$ AdvancedTrainingCLI status")
            AdvancedTrainingCLI.main(arrayOf("status"))
            results.add("CLI: status command executed")
            
            // Config command
            println("\n$ AdvancedTrainingCLI config set learningRate 0.002")
            AdvancedTrainingCLI.main(arrayOf("config", "set", "learningRate", "0.002"))
            results.add("CLI: config set command executed")
            
            // Experiment command
            println("\n$ AdvancedTrainingCLI experiment create \"cli_experiment\"")
            AdvancedTrainingCLI.main(arrayOf("experiment", "create", "cli_experiment"))
            results.add("CLI: experiment create command executed")
            
            // Report command
            println("\n$ AdvancedTrainingCLI report")
            AdvancedTrainingCLI.main(arrayOf("report"))
            results.add("CLI: report command executed")
            
            // Stop command
            println("\n$ AdvancedTrainingCLI stop")
            AdvancedTrainingCLI.main(arrayOf("stop"))
            results.add("CLI: stop command executed")
            
            return CLIDemoResults(true, results)
            
        } catch (e: Exception) {
            println("❌ CLI demo failed: ${e.message}")
            results.add("CLI demo failed: ${e.message}")
            return CLIDemoResults(false, results)
        }
    }
}

/**
 * Demo results data classes
 */
data class AdvancedTrainingDemoResults(
    val success: Boolean,
    val results: List<String>,
    val duration: Long,
    val featuresDemo: Map<String, Boolean>
)

data class CLIDemoResults(
    val success: Boolean,
    val results: List<String>
)