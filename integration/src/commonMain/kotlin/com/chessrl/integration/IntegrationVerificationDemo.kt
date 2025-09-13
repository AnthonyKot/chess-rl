package com.chessrl.integration

import com.chessrl.rl.*

/**
 * Comprehensive verification demo that demonstrates all required integration improvements
 * are properly implemented and working together.
 */
object IntegrationVerificationDemo {
    
    /**
     * Verify all required integration improvements
     */
    fun verifyAllIntegrationRequirements(): IntegrationVerificationResults {
        println("🔍 Verifying All Required Integration Improvements")
        println("=" * 60)
        
        val results = mutableMapOf<String, VerificationResult>()
        
        // Requirement 1: Connect SelfPlayController to actual ChessAgent instances
        results["ChessAgent Connection"] = verifyChessAgentConnection()
        
        // Requirement 2: Implement proper experience flow from self-play to ExperienceReplay
        results["Experience Flow"] = verifyExperienceFlow()
        
        // Requirement 3: Create training iteration loops alternating between self-play and network training
        results["Training Iteration Loops"] = verifyTrainingIterationLoops()
        
        // Requirement 4: Integrate real neural network training with collected experiences
        results["Neural Network Integration"] = verifyNeuralNetworkIntegration()
        
        // Additional verification: End-to-end integration
        results["End-to-End Integration"] = verifyEndToEndIntegration()
        
        val allPassed = results.values.all { it.success }
        
        println("\n📊 Integration Verification Summary:")
        println("=" * 50)
        results.forEach { (requirement, result) ->
            val status = if (result.success) "✅ PASSED" else "❌ FAILED"
            println("$requirement: $status")
            if (!result.success) {
                println("  Error: ${result.error}")
            }
        }
        
        println("\nOverall Result: ${if (allPassed) "✅ ALL REQUIREMENTS MET" else "❌ SOME REQUIREMENTS FAILED"}")
        
        return IntegrationVerificationResults(allPassed, results)
    }
    
    /**
     * Requirement 1: Connect SelfPlayController to actual ChessAgent instances for real gameplay
     */
    private fun verifyChessAgentConnection(): VerificationResult {
        println("\n1️⃣ Verifying ChessAgent Connection to SelfPlayController")
        println("-" * 50)
        
        return try {
            // Create real chess agents
            println("   Creating real DQN chess agents...")
            val mainAgent = ChessAgentFactory.createDQNAgent(
                hiddenLayers = listOf(64, 32),
                config = ChessAgentConfig(batchSize = 4)
            )
            
            val opponentAgent = ChessAgentFactory.createDQNAgent(
                hiddenLayers = listOf(64, 32),
                config = ChessAgentConfig(batchSize = 4)
            )
            
            println("   ✅ Real chess agents created successfully")
            
            // Verify agents can interact with chess environment
            println("   Testing agent-environment interaction...")
            val environment = ChessEnvironment()
            val state = environment.reset()
            val validActions = environment.getValidActions(state)
            
            val mainAction = mainAgent.selectAction(state, validActions)
            val opponentAction = opponentAgent.selectAction(state, validActions)
            
            println("   ✅ Agents can select actions: main=$mainAction, opponent=$opponentAction")
            
            // Verify SelfPlayController can use these agents
            println("   Testing SelfPlayController integration...")
            val controller = IntegratedSelfPlayController(
                IntegratedSelfPlayConfig(
                    gamesPerIteration = 1,
                    maxStepsPerGame = 5,
                    hiddenLayers = listOf(32, 16)
                )
            )
            
            val initResult = controller.initialize()
            if (initResult !is InitializationResult.Success) {
                return VerificationResult(false, "Controller initialization failed")
            }
            
            println("   ✅ SelfPlayController successfully initialized with real agents")
            
            // Verify actual gameplay
            println("   Testing actual self-play gameplay...")
            val selfPlaySystem = SelfPlaySystem(
                SelfPlayConfig(
                    maxConcurrentGames = 1,
                    maxStepsPerGame = 3,
                    progressReportInterval = 1
                )
            )
            
            val gameResults = selfPlaySystem.runSelfPlayGames(mainAgent, opponentAgent, 1)
            
            if (gameResults.totalGames == 0) {
                return VerificationResult(false, "No games were played")
            }
            
            println("   ✅ Self-play games executed successfully: ${gameResults.totalGames} games, ${gameResults.totalExperiences} experiences")
            
            VerificationResult(true, "ChessAgent connection verified - real agents integrated with SelfPlayController")
            
        } catch (e: Exception) {
            println("   ❌ ChessAgent connection failed: ${e.message}")
            VerificationResult(false, "ChessAgent connection failed: ${e.message}")
        }
    }
    
    /**
     * Requirement 2: Implement proper experience flow from self-play games to ExperienceReplay buffer
     */
    private fun verifyExperienceFlow(): VerificationResult {
        println("\n2️⃣ Verifying Experience Flow from Self-Play to ExperienceReplay")
        println("-" * 50)
        
        return try {
            // Create agents with experience replay buffers
            println("   Creating agents with experience replay integration...")
            val agent = RealChessAgentFactory.createRealDQNAgent(
                inputSize = 776,
                outputSize = 4096,
                hiddenLayers = listOf(32, 16),
                maxBufferSize = 100
            )
            
            println("   ✅ Agent with experience replay buffer created")
            
            // Generate self-play experiences
            println("   Generating self-play experiences...")
            val selfPlaySystem = SelfPlaySystem(
                SelfPlayConfig(
                    maxConcurrentGames = 1,
                    maxStepsPerGame = 5
                )
            )
            
            val chessAgent = ChessAgentAdapter(agent, ChessAgentConfig())
            val gameResults = selfPlaySystem.runSelfPlayGames(chessAgent, chessAgent, 1)
            
            if (gameResults.totalExperiences == 0) {
                return VerificationResult(false, "No experiences generated from self-play")
            }
            
            println("   ✅ Self-play generated ${gameResults.totalExperiences} experiences")
            
            // Verify experiences are enhanced with metadata
            println("   Verifying experience enhancement...")
            val enhancedExperiences = gameResults.experiences
            
            if (enhancedExperiences.isEmpty()) {
                return VerificationResult(false, "No enhanced experiences found")
            }
            
            val firstExperience = enhancedExperiences.first()
            val hasMetadata = firstExperience.gameId > 0 && 
                             firstExperience.moveNumber > 0 &&
                             firstExperience.qualityScore >= 0.0
            
            if (!hasMetadata) {
                return VerificationResult(false, "Experiences missing required metadata")
            }
            
            println("   ✅ Experiences properly enhanced with metadata")
            
            // Verify experiences flow to training pipeline
            println("   Testing experience flow to training pipeline...")
            val trainingPipeline = ChessTrainingPipeline(
                agent = chessAgent,
                environment = ChessEnvironment(),
                config = TrainingPipelineConfig(batchSize = 2)
            )
            
            // Convert enhanced experiences to basic experiences and add to agent
            val basicExperiences = enhancedExperiences.map { it.toBasicExperience() }
            basicExperiences.forEach { experience ->
                chessAgent.learn(experience)
            }
            
            // Verify agent has learned from experiences
            val metrics = chessAgent.getTrainingMetrics()
            if (metrics.experienceBufferSize == 0) {
                return VerificationResult(false, "Experiences not properly added to agent buffer")
            }
            
            println("   ✅ Experiences successfully flowed to training pipeline: ${metrics.experienceBufferSize} in buffer")
            
            VerificationResult(true, "Experience flow verified - self-play experiences properly flow to ExperienceReplay buffers")
            
        } catch (e: Exception) {
            println("   ❌ Experience flow verification failed: ${e.message}")
            VerificationResult(false, "Experience flow failed: ${e.message}")
        }
    }
    
    /**
     * Requirement 3: Create training iteration loops that alternate between self-play and network training
     */
    private fun verifyTrainingIterationLoops(): VerificationResult {
        println("\n3️⃣ Verifying Training Iteration Loops (Self-Play ↔ Network Training)")
        println("-" * 50)
        
        return try {
            // Create integrated controller with small configuration for testing
            println("   Setting up integrated training controller...")
            val config = IntegratedSelfPlayConfig(
                gamesPerIteration = 2,
                maxStepsPerGame = 3,
                batchSize = 2,
                hiddenLayers = listOf(16, 8),
                iterationReportInterval = 1
            )
            
            val controller = IntegratedSelfPlayController(config)
            val initResult = controller.initialize()
            
            if (initResult !is InitializationResult.Success) {
                return VerificationResult(false, "Controller initialization failed")
            }
            
            println("   ✅ Integrated controller initialized")
            
            // Track training phases
            println("   Running training iteration loop...")
            val trainingResults = controller.runIntegratedTraining(2) // 2 iterations
            
            if (trainingResults.totalIterations == 0) {
                return VerificationResult(false, "No training iterations completed")
            }
            
            println("   ✅ Training iterations completed: ${trainingResults.totalIterations}")
            
            // Verify alternating phases occurred
            println("   Verifying alternating phases...")
            val iterationResults = trainingResults.iterationResults
            
            if (iterationResults.isEmpty()) {
                return VerificationResult(false, "No iteration results recorded")
            }
            
            // Check each iteration has both self-play and training phases
            for (iteration in iterationResults) {
                val hasSelfPlay = iteration.selfPlayResults.totalGames > 0
                val hasTraining = iteration.trainingResults.batchUpdates > 0
                
                if (!hasSelfPlay) {
                    return VerificationResult(false, "Iteration ${iteration.iteration} missing self-play phase")
                }
                
                if (!hasTraining) {
                    return VerificationResult(false, "Iteration ${iteration.iteration} missing training phase")
                }
            }
            
            println("   ✅ All iterations contain both self-play and training phases")
            
            // Verify performance tracking across iterations
            println("   Verifying performance tracking...")
            val performances = iterationResults.map { it.performance }
            val hasPerformanceData = performances.all { it >= 0.0 }
            
            if (!hasPerformanceData) {
                return VerificationResult(false, "Performance tracking missing or invalid")
            }
            
            println("   ✅ Performance tracked across iterations: ${performances}")
            
            // Verify training metrics show learning
            val finalMetrics = trainingResults.finalMetrics
            val hasLearningMetrics = finalMetrics.totalBatchUpdates > 0 && 
                                   finalMetrics.totalExperiencesCollected > 0
            
            if (!hasLearningMetrics) {
                return VerificationResult(false, "Learning metrics missing or invalid")
            }
            
            println("   ✅ Learning metrics verified: ${finalMetrics.totalBatchUpdates} updates, ${finalMetrics.totalExperiencesCollected} experiences")
            
            VerificationResult(true, "Training iteration loops verified - proper alternation between self-play and network training")
            
        } catch (e: Exception) {
            println("   ❌ Training iteration loop verification failed: ${e.message}")
            VerificationResult(false, "Training iteration loops failed: ${e.message}")
        }
    }
    
    /**
     * Requirement 4: Integrate real neural network training with collected self-play experiences
     */
    private fun verifyNeuralNetworkIntegration(): VerificationResult {
        println("\n4️⃣ Verifying Real Neural Network Training Integration")
        println("-" * 50)
        
        return try {
            // Create real neural network agent
            println("   Creating real neural network agent...")
            val realAgent = RealChessAgentFactory.createRealDQNAgent(
                inputSize = 776,
                outputSize = 4096,
                hiddenLayers = listOf(32, 16),
                learningRate = 0.01,
                batchSize = 4
            )
            
            println("   ✅ Real neural network agent created")
            
            // Verify neural network can process chess states
            println("   Testing neural network forward pass...")
            val chessState = DoubleArray(776) { kotlin.random.Random.nextDouble(-1.0, 1.0) }
            val validActions = listOf(0, 1, 2, 3, 4)
            
            val qValues = realAgent.getQValues(chessState, validActions)
            if (qValues.isEmpty() || qValues.size != validActions.size) {
                return VerificationResult(false, "Neural network forward pass failed")
            }
            
            println("   ✅ Neural network forward pass successful: ${qValues.size} Q-values")
            
            // Generate self-play experiences
            println("   Generating self-play experiences for training...")
            val chessAgent = ChessAgentAdapter(realAgent, ChessAgentConfig())
            val selfPlaySystem = SelfPlaySystem(
                SelfPlayConfig(maxStepsPerGame = 5)
            )
            
            val gameResults = selfPlaySystem.runSelfPlayGames(chessAgent, chessAgent, 1)
            val experiences = gameResults.experiences.map { it.toBasicExperience() }
            
            if (experiences.isEmpty()) {
                return VerificationResult(false, "No experiences generated for training")
            }
            
            println("   ✅ Generated ${experiences.size} experiences for training")
            
            // Test neural network learning from experiences
            println("   Testing neural network learning from experiences...")
            val initialMetrics = realAgent.getTrainingMetrics()
            val initialBufferSize = initialMetrics.experienceBufferSize
            
            // Add experiences to agent
            experiences.forEach { experience ->
                realAgent.learn(experience)
            }
            
            // Force network update
            realAgent.forceUpdate()
            
            val updatedMetrics = realAgent.getTrainingMetrics()
            val updatedBufferSize = updatedMetrics.experienceBufferSize
            
            if (updatedBufferSize <= initialBufferSize) {
                return VerificationResult(false, "Experiences not properly added to neural network")
            }
            
            println("   ✅ Neural network learned from experiences: buffer size ${initialBufferSize} → ${updatedBufferSize}")
            
            // Verify network weights can be updated
            println("   Testing neural network weight updates...")
            
            // Get Q-values before and after training
            val qValuesBefore = realAgent.getQValues(chessState, validActions)
            
            // Add more experiences and force update
            repeat(5) {
                val experience = Experience(
                    state = DoubleArray(776) { kotlin.random.Random.nextDouble(-1.0, 1.0) },
                    action = validActions.random(),
                    reward = kotlin.random.Random.nextDouble(-1.0, 1.0),
                    nextState = DoubleArray(776) { kotlin.random.Random.nextDouble(-1.0, 1.0) },
                    done = kotlin.random.Random.nextBoolean()
                )
                realAgent.learn(experience)
            }
            
            realAgent.forceUpdate()
            val qValuesAfter = realAgent.getQValues(chessState, validActions)
            
            // Check if Q-values changed (indicating network update)
            val qValuesChanged = qValuesBefore.keys.any { action ->
                kotlin.math.abs(qValuesBefore[action]!! - qValuesAfter[action]!!) > 1e-6
            }
            
            if (!qValuesChanged) {
                return VerificationResult(false, "Neural network weights not updating from training")
            }
            
            println("   ✅ Neural network weights updated from training")
            
            // Test integrated training pipeline
            println("   Testing integrated training pipeline...")
            val trainingPipeline = ChessTrainingPipeline(
                agent = chessAgent,
                environment = ChessEnvironment(),
                config = TrainingPipelineConfig(
                    maxStepsPerEpisode = 5,
                    batchSize = 2
                )
            )
            
            val episodeResult = trainingPipeline.runEpisode()
            
            if (episodeResult.steps == 0 || episodeResult.experiences.isEmpty()) {
                return VerificationResult(false, "Training pipeline episode failed")
            }
            
            println("   ✅ Training pipeline episode successful: ${episodeResult.steps} steps, ${episodeResult.experiences.size} experiences")
            
            VerificationResult(true, "Neural network integration verified - real networks training on self-play experiences")
            
        } catch (e: Exception) {
            println("   ❌ Neural network integration verification failed: ${e.message}")
            VerificationResult(false, "Neural network integration failed: ${e.message}")
        }
    }
    
    /**
     * Additional verification: End-to-end integration test
     */
    private fun verifyEndToEndIntegration(): VerificationResult {
        println("\n5️⃣ Verifying End-to-End Integration")
        println("-" * 50)
        
        return try {
            // Test complete integration with deterministic seeding
            println("   Setting up deterministic end-to-end test...")
            val seedManager = SeedManager.apply {
                initializeWithMasterSeed(99999L)
            }
            
            val config = IntegratedSelfPlayConfig(
                agentType = AgentType.DQN,
                hiddenLayers = listOf(32, 16),
                gamesPerIteration = 1,
                maxStepsPerGame = 3,
                batchSize = 2,
                enableDeterministicTraining = true,
                checkpointFrequency = 1
            )
            
            val controller = IntegratedSelfPlayController(config)
            val initResult = controller.initialize(seedManager)
            
            if (initResult !is InitializationResult.Success) {
                return VerificationResult(false, "End-to-end initialization failed")
            }
            
            println("   ✅ End-to-end system initialized with deterministic seeding")
            
            // Run complete training cycle
            println("   Running complete training cycle...")
            val trainingResults = controller.runIntegratedTraining(1)
            
            // Verify all components worked together
            val hasGames = trainingResults.finalMetrics.totalGamesPlayed > 0
            val hasExperiences = trainingResults.finalMetrics.totalExperiencesCollected > 0
            val hasTraining = trainingResults.finalMetrics.totalBatchUpdates > 0
            val hasCheckpoints = trainingResults.checkpointSummary.totalCheckpoints > 0
            
            if (!hasGames) {
                return VerificationResult(false, "End-to-end test: no games played")
            }
            
            if (!hasExperiences) {
                return VerificationResult(false, "End-to-end test: no experiences collected")
            }
            
            if (!hasTraining) {
                return VerificationResult(false, "End-to-end test: no training occurred")
            }
            
            if (!hasCheckpoints) {
                return VerificationResult(false, "End-to-end test: no checkpoints created")
            }
            
            println("   ✅ Complete training cycle successful:")
            println("      Games: ${trainingResults.finalMetrics.totalGamesPlayed}")
            println("      Experiences: ${trainingResults.finalMetrics.totalExperiencesCollected}")
            println("      Training updates: ${trainingResults.finalMetrics.totalBatchUpdates}")
            println("      Checkpoints: ${trainingResults.checkpointSummary.totalCheckpoints}")
            
            // Test reproducibility
            println("   Testing deterministic reproducibility...")
            seedManager.initializeWithMasterSeed(99999L) // Reset to same seed
            
            val controller2 = IntegratedSelfPlayController(config)
            controller2.initialize(seedManager)
            val trainingResults2 = controller2.runIntegratedTraining(1)
            
            val reproducible = trainingResults.finalMetrics.totalGamesPlayed == trainingResults2.finalMetrics.totalGamesPlayed &&
                             trainingResults.finalMetrics.totalExperiencesCollected == trainingResults2.finalMetrics.totalExperiencesCollected
            
            if (!reproducible) {
                return VerificationResult(false, "End-to-end test: not reproducible with same seed")
            }
            
            println("   ✅ Deterministic reproducibility verified")
            
            VerificationResult(true, "End-to-end integration verified - all components working together seamlessly")
            
        } catch (e: Exception) {
            println("   ❌ End-to-end integration verification failed: ${e.message}")
            VerificationResult(false, "End-to-end integration failed: ${e.message}")
        }
    }
    
    /**
     * Run a comprehensive integration demonstration
     */
    fun runComprehensiveIntegrationDemo(): ComprehensiveIntegrationResults {
        println("🎯 Running Comprehensive Integration Demonstration")
        println("=" * 60)
        
        val startTime = getCurrentTimeMillis()
        val results = mutableListOf<String>()
        
        try {
            // Phase 1: Verify all requirements
            println("\n📋 Phase 1: Requirement Verification")
            val verificationResults = verifyAllIntegrationRequirements()
            results.add("Requirement verification: ${if (verificationResults.allPassed) "PASSED" else "FAILED"}")
            
            if (!verificationResults.allPassed) {
                results.add("Failed requirements: ${verificationResults.results.filterValues { !it.success }.keys}")
            }
            
            // Phase 2: Performance demonstration
            println("\n⚡ Phase 2: Performance Demonstration")
            val performanceResults = demonstratePerformance()
            results.add("Performance demo: ${if (performanceResults.success) "PASSED" else "FAILED"}")
            results.add("Performance metrics: ${performanceResults.metrics}")
            
            // Phase 3: Scalability test
            println("\n📈 Phase 3: Scalability Test")
            val scalabilityResults = demonstrateScalability()
            results.add("Scalability test: ${if (scalabilityResults.success) "PASSED" else "FAILED"}")
            results.add("Scalability metrics: ${scalabilityResults.metrics}")
            
            val endTime = getCurrentTimeMillis()
            val totalDuration = endTime - startTime
            
            println("\n🏁 Comprehensive Integration Demo Completed!")
            println("Total duration: ${totalDuration}ms")
            
            return ComprehensiveIntegrationResults(
                success = verificationResults.allPassed,
                verificationResults = verificationResults,
                performanceResults = performanceResults,
                scalabilityResults = scalabilityResults,
                totalDuration = totalDuration,
                summary = results
            )
            
        } catch (e: Exception) {
            println("❌ Comprehensive demo failed: ${e.message}")
            results.add("Demo failed: ${e.message}")
            
            val endTime = getCurrentTimeMillis()
            val totalDuration = endTime - startTime
            
            return ComprehensiveIntegrationResults(
                success = false,
                verificationResults = IntegrationVerificationResults(false, emptyMap()),
                performanceResults = PerformanceResults(false, emptyMap()),
                scalabilityResults = ScalabilityResults(false, emptyMap()),
                totalDuration = totalDuration,
                summary = results
            )
        }
    }
    
    private fun demonstratePerformance(): PerformanceResults {
        return try {
            val startTime = getCurrentTimeMillis()
            
            val config = IntegratedSelfPlayConfig(
                gamesPerIteration = 3,
                maxStepsPerGame = 10,
                hiddenLayers = listOf(64, 32)
            )
            
            val controller = IntegratedSelfPlayController(config)
            controller.initialize()
            val results = controller.runIntegratedTraining(2)
            
            val endTime = getCurrentTimeMillis()
            val duration = endTime - startTime
            
            val metrics = mapOf(
                "duration_ms" to duration.toString(),
                "games_played" to results.finalMetrics.totalGamesPlayed.toString(),
                "experiences_collected" to results.finalMetrics.totalExperiencesCollected.toString(),
                "games_per_second" to (results.finalMetrics.totalGamesPlayed.toDouble() / duration * 1000).toString()
            )
            
            PerformanceResults(true, metrics)
        } catch (e: Exception) {
            PerformanceResults(false, mapOf("error" to e.message.toString()))
        }
    }
    
    private fun demonstrateScalability(): ScalabilityResults {
        return try {
            val results = mutableMapOf<String, String>()
            
            // Test different batch sizes
            val batchSizes = listOf(4, 8, 16)
            for (batchSize in batchSizes) {
                val startTime = getCurrentTimeMillis()
                
                val config = IntegratedSelfPlayConfig(
                    gamesPerIteration = 2,
                    maxStepsPerGame = 5,
                    batchSize = batchSize,
                    hiddenLayers = listOf(32, 16)
                )
                
                val controller = IntegratedSelfPlayController(config)
                controller.initialize()
                controller.runIntegratedTraining(1)
                
                val endTime = getCurrentTimeMillis()
                val duration = endTime - startTime
                
                results["batch_size_${batchSize}_duration_ms"] = duration.toString()
            }
            
            ScalabilityResults(true, results)
        } catch (e: Exception) {
            ScalabilityResults(false, mapOf("error" to e.message.toString()))
        }
    }
}

// Result data classes
data class VerificationResult(val success: Boolean, val message: String, val error: String? = null)

data class IntegrationVerificationResults(
    val allPassed: Boolean,
    val results: Map<String, VerificationResult>
)

data class PerformanceResults(val success: Boolean, val metrics: Map<String, String>)
data class ScalabilityResults(val success: Boolean, val metrics: Map<String, String>)

data class ComprehensiveIntegrationResults(
    val success: Boolean,
    val verificationResults: IntegrationVerificationResults,
    val performanceResults: PerformanceResults,
    val scalabilityResults: ScalabilityResults,
    val totalDuration: Long,
    val summary: List<String>
)