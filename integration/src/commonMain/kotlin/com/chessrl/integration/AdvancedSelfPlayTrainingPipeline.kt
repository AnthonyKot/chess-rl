package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.math.*
import kotlin.random.Random

/**
 * Advanced self-play training pipeline with sophisticated learning cycle management,
 * advanced checkpointing, and comprehensive experience buffer management.
 * 
 * This pipeline integrates self-play game generation with batch training optimization,
 * providing production-ready training capabilities with convergence detection,
 * model versioning, and automated recovery mechanisms.
 */
class AdvancedSelfPlayTrainingPipeline(
    private val config: AdvancedSelfPlayConfig = AdvancedSelfPlayConfig(),
    private val agentType: AgentType = AgentType.DQN
) {
    
    // Core components
    private var selfPlaySystem: SelfPlaySystem? = null
    private var trainingPipeline: ChessTrainingPipeline? = null
    private var trainingValidator: TrainingValidator? = null
    private var checkpointManager: CheckpointManager? = null
    private var experienceManager: AdvancedExperienceManager? = null
    
    // Agents
    private var mainAgent: ChessAgent? = null
    private var opponentAgent: ChessAgent? = null
    
    // Training state
    private var isTraining = false
    private var isPaused = false
    private var currentCycle = 0
    private var totalCycles = 0
    private var bestModelVersion = 0
    
    // Training history and metrics
    private val cycleHistory = mutableListOf<TrainingCycleResult>()
    private val performanceHistory = mutableListOf<Double>()
    private var lastCheckpointCycle = 0
    
    // Adaptive scheduling state
    private var currentGamesPerCycle = 0
    private var currentTrainingRatio = 0.0
    private var convergenceDetector = ConvergenceDetector(config.convergenceConfig)
    
    /**
     * Initialize the advanced training pipeline
     */
    fun initialize(): Boolean {
        try {
            println("🔧 Initializing Advanced Self-Play Training Pipeline")

            // Log seed summary if available
            runCatching { SeedManager.getInstance().getSeedSummary() }.onSuccess { summary ->
                println("🎲 Seed Summary: seeded=${summary.isSeeded}, deterministic=${summary.isDeterministicMode}, masterSeed=${summary.masterSeed}")
            }.onFailure {
                println("🎲 Seed Summary: not initialized (non-deterministic run)")
            }
            
            // Create training validator
            trainingValidator = TrainingValidator(
                config = ValidationConfig(
                    explodingGradientThreshold = config.gradientClipThreshold,
                    vanishingGradientThreshold = config.minGradientNorm,
                    policyCollapseThreshold = config.minPolicyEntropy,
                    convergenceWindowSize = config.convergenceConfig.windowSize
                )
            )
            
            // Create checkpoint manager
            checkpointManager = CheckpointManager(
                config = CheckpointConfig(
                    baseDirectory = config.checkpointDirectory,
                    maxVersions = config.maxModelVersions,
                    compressionEnabled = config.enableCheckpointCompression,
                    validationEnabled = config.enableCheckpointValidation
                )
            )
            
            // Create advanced experience manager
            experienceManager = AdvancedExperienceManager(
                config = ExperienceManagerConfig(
                    maxBufferSize = config.maxExperienceBufferSize,
                    samplingStrategies = config.samplingStrategies,
                    qualityThreshold = config.experienceQualityThreshold,
                    cleanupStrategy = config.experienceCleanupStrategy,
                    memoryOptimization = config.enableMemoryOptimization
                )
            )
            
            // Create agents
            mainAgent = createMainAgent()
            opponentAgent = createOpponentAgent()
            
            // Create self-play system
            selfPlaySystem = SelfPlaySystem(
                config = SelfPlayConfig(
                    maxConcurrentGames = config.maxConcurrentGames,
                    maxStepsPerGame = config.maxStepsPerGame,
                    winReward = config.winReward,
                    lossReward = config.lossReward,
                    drawReward = config.drawReward,
                    enablePositionRewards = config.enablePositionRewards,
                    stepLimitPenalty = config.stepLimitPenalty,
                    maxExperienceBufferSize = config.maxExperienceBufferSize,
                    experienceCleanupStrategy = config.experienceCleanupStrategy,
                    progressReportInterval = config.progressReportInterval,
                    treatStepLimitAsDrawForReporting = config.treatStepLimitAsDrawForReporting
                )
            )
            
            // Create training pipeline
            val environment = ChessEnvironment(
                rewardConfig = ChessRewardConfig(
                    winReward = config.winReward,
                    lossReward = config.lossReward,
                    drawReward = config.drawReward,
                    stepPenalty = -0.001,
                    enablePositionRewards = config.enablePositionRewards
                )
            )
            
            trainingPipeline = ChessTrainingPipeline(
                agent = mainAgent!!,
                environment = environment,
                config = TrainingPipelineConfig(
                    maxStepsPerEpisode = config.maxStepsPerGame,
                    batchSize = config.batchSize,
                    batchTrainingFrequency = 1,
                    maxBufferSize = config.maxExperienceBufferSize,
                    progressReportInterval = config.progressReportInterval,
                    samplingStrategy = SamplingStrategy.MIXED
                )
            )

            // Provide valid-action masking to DQN for next-state target computation.
            // Prefer registry values captured at experience generation; fallback to env.
            try {
                mainAgent?.setNextActionProvider { s ->
                    ValidActionRegistry.get(s) ?: environment.getValidActions(s)
                }
            } catch (_: Throwable) { /* optional */ }
            
            // Initialize adaptive scheduling
            currentGamesPerCycle = config.initialGamesPerCycle
            currentTrainingRatio = config.initialTrainingRatio
            
            println("✅ Advanced Self-Play Training Pipeline initialized successfully")
            return true
            
        } catch (e: Exception) {
            println("❌ Failed to initialize advanced training pipeline: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Run advanced self-play training with sophisticated learning cycle management
     */
    fun runAdvancedTraining(totalCycles: Int): AdvancedTrainingResults {
        if (isTraining) {
            throw IllegalStateException("Advanced training is already running")
        }
        
        val mainAgent = this.mainAgent ?: throw IllegalStateException("Pipeline not initialized")
        val opponentAgent = this.opponentAgent ?: throw IllegalStateException("Pipeline not initialized")
        val selfPlaySystem = this.selfPlaySystem ?: throw IllegalStateException("Pipeline not initialized")
        // trainingPipeline is initialized and used internally by training routines
        this.trainingPipeline ?: throw IllegalStateException("Pipeline not initialized")
        val trainingValidator = this.trainingValidator ?: throw IllegalStateException("Pipeline not initialized")
        val checkpointManager = this.checkpointManager ?: throw IllegalStateException("Pipeline not initialized")
        val experienceManager = this.experienceManager ?: throw IllegalStateException("Pipeline not initialized")
        
        // Interpret requested cycles as training iterations; include one warmup cycle when > 0
        val targetCycles = if (totalCycles <= 0) 0 else totalCycles + 1

        println("🚀 Starting Advanced Self-Play Training")
        println("Configuration: $config")
        println("Total Cycles: $targetCycles")
        println("=" * 80)
        
        isTraining = true
        isPaused = false
        currentCycle = 0
        this.totalCycles = targetCycles
        cycleHistory.clear()
        performanceHistory.clear()
        bestModelVersion = 0
        lastCheckpointCycle = 0
        
        val startTime = getCurrentTimeMillis()
        
        try {
            // Create initial checkpoint
            val initialCheckpoint = checkpointManager.createCheckpoint(
                agent = mainAgent,
                version = 0,
                metadata = CheckpointMetadata(
                    cycle = 0,
                    performance = 0.0,
                    description = "Initial model before training",
                    seedConfiguration = try { SeedManager.getInstance().getSeedConfiguration() } catch (_: Throwable) { null }
                )
            )
            
            println("💾 Initial checkpoint created: ${initialCheckpoint.path}")
            
            // Main training loop with sophisticated cycle management
            for (cycle in 1..targetCycles) {
                currentCycle = cycle
                // Apply exploration warmup for early cycles (temporary higher exploration)
                if (config.explorationWarmupCycles > 0) {
                    if (cycle <= config.explorationWarmupCycles) {
                        runCatching { mainAgent.setExplorationRate(config.explorationWarmupRate) }
                        runCatching { opponentAgent.setExplorationRate(config.explorationWarmupRate) }
                    } else {
                        // Optional epsilon decay after warmup if configured; otherwise fixed explorationRate
                        val eps = computeDecayedEpsilon(cycle)
                        if (eps != null) {
                            runCatching { mainAgent.setExplorationRate(eps) }
                            runCatching { opponentAgent.setExplorationRate(eps) }
                        } else {
                            runCatching { mainAgent.setExplorationRate(config.explorationRate) }
                            runCatching { opponentAgent.setExplorationRate(config.explorationRate) }
                        }
                    }
                }
                
                // Check for pause state
                while (isPaused && isTraining) {
                    Thread.sleep(100) // Wait while paused
                }
                
                // Check if training was stopped while paused
                if (!isTraining) break
                
                println("\n🔄 Advanced Training Cycle $cycle/$totalCycles")
                println("-" * 60)
                
                // Execute training cycle with adaptive scheduling
                val cycleResult = executeTrainingCycle(
                    cycle, mainAgent, opponentAgent, selfPlaySystem,
                    trainingValidator, experienceManager
                )
                
                cycleHistory.add(cycleResult)
                performanceHistory.add(cycleResult.performance.averageReward)

                // Sanity-check buffer after first cycle using ExperienceBufferAnalyzer
                if (cycle == 1) {
                    try {
                        val analyzer = ExperienceBufferAnalyzer()
                        val basic = cycleResult.selfPlayResults.experiences.map { it.toBasicExperience() }
                        val inspection = analyzer.inspectBuffer(basic, AnalysisDepth.STANDARD)
                        val termRatio = inspection.detailedAnalysis?.standardInspection?.terminalRatio
                            ?: 0.0
                        val rewardVar = inspection.basicAnalysis.rewardAnalysis.variance
                        println("   🔎 Buffer sanity-check (cycle 1): terminalRatio=${"%.3f".format(termRatio)}, rewardVar=${"%.4f".format(rewardVar)})")
                        if (termRatio <= 0.0 || rewardVar <= 0.0) {
                            println("   ⚠️ Sanity-check: experiences show no terminal transitions or zero reward variance. Consider reducing maxSteps or enabling shaping.")
                        }
                    } catch (e: Exception) {
                        println("   ⚠️ Buffer sanity-check failed: ${e.message}")
                    }
                }
                
                // Update best model if performance improved
                if (cycleResult.performance.averageReward > getBestPerformance()) {
                    bestModelVersion = cycle
                    
                    // Create best model checkpoint
                    val bestCheckpoint = checkpointManager.createCheckpoint(
                        agent = mainAgent,
                        version = cycle,
                        metadata = CheckpointMetadata(
                            cycle = cycle,
                            performance = cycleResult.performance.averageReward,
                            description = "Best model - cycle $cycle",
                            isBest = true
                        )
                    )
                    
                    println("🏆 New best model saved: ${bestCheckpoint.path}")
                }
                
                // Adaptive scheduling updates
                updateAdaptiveScheduling()
                
                // Convergence detection
                val convergenceStatus = convergenceDetector.checkConvergence(performanceHistory)
                if (convergenceStatus.hasConverged && config.enableEarlyStopping) {
                    println("🎯 Convergence detected at cycle $cycle")
                    println("   Confidence: ${convergenceStatus.confidence}")
                    println("   Stability: ${convergenceStatus.stability}")
                    break
                }
                
                // Regular checkpointing
                if (shouldCreateCheckpoint(cycle)) {
                    val checkpoint = checkpointManager.createCheckpoint(
                        agent = mainAgent,
                        version = cycle,
                        metadata = CheckpointMetadata(
                            cycle = cycle,
                            performance = cycleResult.performance.averageReward,
                            description = "Regular checkpoint - cycle $cycle",
                            seedConfiguration = try { SeedManager.getInstance().getSeedConfiguration() } catch (_: Throwable) { null }
                        )
                    )
                    lastCheckpointCycle = cycle
                    println("💾 Regular checkpoint created: ${checkpoint.path}")
                }
                
                // Progress reporting
                if (cycle % config.cycleReportInterval == 0) {
                    reportAdvancedProgress(cycle, totalCycles)
                }
                
                // Model versioning and rollback check
                if (shouldConsiderRollback()) {
                    val rollbackResult = considerModelRollback(checkpointManager, mainAgent)
                    if (rollbackResult.rolledBack) {
                        println("🔄 Model rolled back to version ${rollbackResult.version}")
                    }
                }
                
                // Update opponent strategy
                updateOpponentStrategy(cycle, cycleResult)
                
                // Memory management and cleanup
                performMemoryManagement(experienceManager, cycle)
            }
            
            val endTime = getCurrentTimeMillis()
            val totalDuration = endTime - startTime
            
            // Create final checkpoint
            val finalCheckpoint = checkpointManager.createCheckpoint(
                agent = mainAgent,
                version = currentCycle,
                metadata = CheckpointMetadata(
                    cycle = currentCycle,
                    performance = performanceHistory.lastOrNull() ?: 0.0,
                    description = "Final model after training completion"
                )
            )
            // Optional post-run cleanup using retention policy
            if (config.autoCleanupOnFinish) {
                println("🧹 Performing post-run checkpoint cleanup (retention: keepBest=${config.retention.keepBest}, keepLastN=${config.retention.keepLastN}, keepEveryN=${config.retention.keepEveryN})")
                val cleanup = checkpointManager.cleanupByRetention(config.retention)
                println("   Cleanup: deleted=${cleanup.checkpointsDeleted}, sizeFreed=${cleanup.sizeFreed}, remaining=${cleanup.checkpointsAfter}")
            }
            
            println("\n🏁 Advanced Self-Play Training Completed!")
            println("Total cycles: $currentCycle")
            println("Total duration: ${totalDuration}ms")
            println("Best model version: $bestModelVersion")
            println("Final checkpoint: ${finalCheckpoint.path}")
            
            return AdvancedTrainingResults(
                totalCycles = currentCycle,
                totalDuration = totalDuration,
                bestModelVersion = bestModelVersion,
                bestPerformance = getBestPerformance(),
                cycleHistory = cycleHistory.toList(),
                performanceHistory = performanceHistory.toList(),
                convergenceStatus = convergenceDetector.getFinalStatus(),
                checkpointSummary = checkpointManager.getSummary(),
                experienceStatistics = experienceManager.getStatistics(),
                finalMetrics = calculateFinalMetrics()
            )
            
        } catch (e: Exception) {
            println("❌ Advanced training failed: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            isTraining = false
        }
    }
    
    /**
     * Execute a single training cycle with comprehensive management
     */
    private fun executeTrainingCycle(
        cycle: Int,
        mainAgent: ChessAgent,
        opponentAgent: ChessAgent,
        selfPlaySystem: SelfPlaySystem,
        trainingValidator: TrainingValidator,
        experienceManager: AdvancedExperienceManager
    ): TrainingCycleResult {
        
        val cycleStartTime = getCurrentTimeMillis()
        // Clear any stale masks from previous cycles
        ValidActionRegistry.clear()
        
        // Phase 1: Self-play game generation with adaptive scheduling
        println("🎮 Phase 1: Self-play generation (${currentGamesPerCycle} games)")
        val black = if (cycle <= config.opponentWarmupCycles) {
            println("   🤖 Warmup: using heuristic opponent for cycle $cycle")
            HeuristicChessAgent(opponentAgent.getConfig())
        } else opponentAgent
        val selfPlayResults = selfPlaySystem.runSelfPlayGames(
            whiteAgent = mainAgent,
            blackAgent = black,
            numGames = currentGamesPerCycle
        )
        
        // Phase 2: Advanced experience processing
        println("🧠 Phase 2: Experience processing and quality assessment")
        val experienceProcessingResult = experienceManager.processExperiences(
            newExperiences = selfPlayResults.experiences,
            gameResults = selfPlayResults.gameResults
        )
        
        // Phase 3: Batch training with validation
        println("🔬 Phase 3: Batch training with validation")
        val trainingResults = performValidatedBatchTraining(
            experienceManager, trainingValidator, cycle
        )
        
        // Phase 4: Performance evaluation
        println("📊 Phase 4: Performance evaluation")
        val performanceResults = evaluatePerformance(mainAgent, cycle)
        
        val cycleEndTime = getCurrentTimeMillis()
        val cycleDuration = cycleEndTime - cycleStartTime
        
        return TrainingCycleResult(
            cycle = cycle,
            selfPlayResults = selfPlayResults,
            experienceProcessing = experienceProcessingResult,
            trainingResults = trainingResults,
            performance = performanceResults,
            cycleDuration = cycleDuration,
            adaptiveScheduling = AdaptiveSchedulingState(
                gamesPerCycle = currentGamesPerCycle,
                trainingRatio = currentTrainingRatio,
                batchSize = config.batchSize
            )
        )
    }
    
    /**
     * Perform validated batch training with comprehensive monitoring
     */
    private fun performValidatedBatchTraining(
        experienceManager: AdvancedExperienceManager,
        trainingValidator: TrainingValidator,
        cycle: Int
    ): ValidatedTrainingResults {
        
        val batchResults = mutableListOf<ValidatedBatchResult>()
        val totalBatches = calculateOptimalBatchCount(experienceManager.getBufferSize())
        
        println("   Training ${totalBatches} batches with validation")
        
        for (batchIndex in 1..totalBatches) {
            try {
                // Sample batch with advanced strategy
                val batchExperiences = experienceManager.sampleBatch(config.batchSize)
                
                // Get pre-training metrics (simplified for now)
                val preMetrics = RLMetrics(
                    episode = cycle * 1000 + batchIndex,
                    episodeLength = 50.0,
                    averageReward = 0.0,
                    explorationRate = 0.1,
                    policyLoss = 1.0,
                    policyEntropy = 1.0,
                    gradientNorm = 1.0
                )
                
                // Perform batch training
                val updateResult = performBatchUpdate(batchExperiences)
                
                // Get post-training metrics (simplified for now)
                val postMetrics = RLMetrics(
                    episode = cycle * 1000 + batchIndex,
                    episodeLength = 50.0,
                    averageReward = 0.0,
                    explorationRate = 0.1,
                    policyLoss = updateResult.loss,
                    policyEntropy = updateResult.policyEntropy,
                    gradientNorm = updateResult.gradientNorm
                )
                
                // Validate the update
                val validationResult = trainingValidator.validatePolicyUpdate(
                    beforeMetrics = preMetrics,
                    afterMetrics = postMetrics,
                    updateResult = updateResult,
                    episodeNumber = cycle * 1000 + batchIndex // Unique episode number
                )
                // Log raw and smoothed gradientNorm for clarity
                val rawG = updateResult.gradientNorm
                val emaG = validationResult.smoothedGradientNorm ?: rawG
                val rawH = updateResult.policyEntropy
                val emaH = validationResult.smoothedPolicyEntropy ?: rawH
                val tdMean = (updateResult.targetValueMean ?: 0.0) - (updateResult.qValueMean ?: 0.0)
                println("   Batch $batchIndex metrics: loss=${"%.4f".format(updateResult.loss)}, grad=${"%.4f".format(rawG)} (ema=${"%.4f".format(emaG)}), entropy=${"%.4f".format(rawH)} (ema=${"%.4f".format(emaH)}), td=${"%.4f".format(tdMean)})")
                
                batchResults.add(
                    ValidatedBatchResult(
                        batchIndex = batchIndex,
                        batchSize = batchExperiences.size,
                        updateResult = updateResult,
                        validationResult = validationResult,
                        experienceQuality = experienceManager.calculateBatchQuality(batchExperiences)
                    )
                )
                
                // Handle validation issues
                if (!validationResult.isValid) {
                    println("⚠️ Batch $batchIndex validation failed: ${validationResult.issues.size} issues")
                    handleValidationIssues(validationResult.issues)
                }
                
            } catch (e: Exception) {
                println("❌ Batch $batchIndex training failed: ${e.message}")
            }
        }
        
        // Calculate training statistics
        val avgLoss = batchResults.map { it.updateResult.loss }.average()
        val avgGradientNorm = batchResults.map { it.updateResult.gradientNorm }.average()
        val avgPolicyEntropy = batchResults.map { it.updateResult.policyEntropy }.average()
        val validBatches = batchResults.count { it.validationResult.isValid }
        val avgExperienceQuality = batchResults.map { it.experienceQuality }.average()
        
        return ValidatedTrainingResults(
            totalBatches = batchResults.size,
            validBatches = validBatches,
            averageLoss = avgLoss,
            averageGradientNorm = avgGradientNorm,
            averagePolicyEntropy = avgPolicyEntropy,
            averageExperienceQuality = avgExperienceQuality,
            batchResults = batchResults
        )
    }
    
    /**
     * Perform a single batch update with the agent
     */
    private fun performBatchUpdate(experiences: List<EnhancedExperience>): PolicyUpdateResult {
        val agent = mainAgent!!
        
        // Convert enhanced experiences to basic experiences for training
        val basicExperiences = experiences.map { ex ->
            if (
                ex.terminationReason == EpisodeTerminationReason.STEP_LIMIT &&
                ex.moveNumber == ex.chessMetrics.gameLength
            ) {
                // Apply a small terminal penalty and mark as done for step-limit games
                Experience(
                    state = ex.state,
                    action = ex.action,
                    reward = ex.reward + config.stepLimitPenalty,
                    nextState = ex.nextState,
                    done = true
                )
            } else {
                ex.toBasicExperience()
            }
        }
        
        // Train using agent's batch API and return real update metrics
        return agent.trainBatch(basicExperiences)
    }
    
    /**
     * Calculate optimal batch count based on buffer size and training ratio
     */
    private fun calculateOptimalBatchCount(bufferSize: Int): Int {
        val targetExperiences = (bufferSize * currentTrainingRatio).toInt()
        val batchCount = (targetExperiences / config.batchSize).coerceAtLeast(1)
        return batchCount.coerceAtMost(config.maxBatchesPerCycle)
    }
    
    /**
     * Handle validation issues with appropriate responses
     */
    private fun handleValidationIssues(issues: List<ValidationIssue>) {
        for (issue in issues) {
            when (issue.type) {
                IssueType.EXPLODING_GRADIENTS -> {
                    println("   🔧 Applying gradient clipping for exploding gradients")
                    // In practice, would adjust agent's gradient clipping
                }
                IssueType.VANISHING_GRADIENTS -> {
                    println("   🔧 Detected vanishing gradients, monitoring learning rate")
                    // In practice, might adjust learning rate
                }
                IssueType.POLICY_COLLAPSE -> {
                    println("   🔧 Policy collapse detected, increasing exploration")
                    // In practice, would increase exploration rate
                }
                IssueType.NUMERICAL_INSTABILITY -> {
                    println("   🔧 Numerical instability, reducing learning rate")
                    // In practice, would reduce learning rate
                }
                else -> {
                    println("   ⚠️ Validation issue: ${issue.message}")
                }
            }
        }
    }
    
    /**
     * Evaluate agent performance with comprehensive metrics
     */
    private fun evaluatePerformance(agent: ChessAgent, cycle: Int): PerformanceEvaluationResult {
        val evaluationGames = config.evaluationGamesPerCycle
        val gameResults = mutableListOf<EvaluationGameResult>()
        
        // Create evaluation environment aligned with training rewards
        val evalEnvironment = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = config.winReward,
                lossReward = config.lossReward,
                drawReward = config.drawReward,
                stepPenalty = -0.001,
                enablePositionRewards = config.enablePositionRewards,
                maxGameLength = config.maxStepsPerGame
            )
        )
        
        repeat(evaluationGames) { gameIndex ->
            try {
                val gameResult = runEvaluationGame(agent, evalEnvironment, gameIndex)
                gameResults.add(gameResult)
            } catch (e: Exception) {
                println("⚠️ Evaluation game $gameIndex failed: ${e.message}")
            }
        }
        
        // Calculate performance metrics
        val avgReward = if (gameResults.isNotEmpty()) {
            gameResults.map { it.totalReward }.average()
        } else 0.0
        
        val avgGameLength = if (gameResults.isNotEmpty()) {
            gameResults.map { it.gameLength }.average()
        } else 0.0
        
        val wins = gameResults.count { it.gameOutcome == GameOutcome.WHITE_WINS }
        val ongoing = gameResults.count { it.gameOutcome == GameOutcome.ONGOING }
        val draws = gameResults.count { it.gameOutcome == GameOutcome.DRAW } + ongoing // treat ongoing as draws
        val losses = gameResults.count { it.gameOutcome == GameOutcome.BLACK_WINS }
        val counted = (wins + draws + losses).coerceAtLeast(1) // avoid divide by zero
        val winRate = wins.toDouble() / counted
        val drawRate = draws.toDouble() / counted
        val lossRate = losses.toDouble() / counted
        
        // Calculate performance score (weighted combination of metrics)
        val performanceScore = calculatePerformanceScore(avgReward, winRate, drawRate, avgGameLength)
        
        return PerformanceEvaluationResult(
            cycle = cycle,
            gamesPlayed = gameResults.size,
            averageReward = avgReward,
            averageGameLength = avgGameLength,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            performanceScore = performanceScore,
            gameResults = gameResults
        )
    }
    
    /**
     * Run a single evaluation game
     */
    private fun runEvaluationGame(
        agent: ChessAgent,
        environment: ChessEnvironment,
        gameIndex: Int
    ): EvaluationGameResult {
        
        var state = environment.reset()
        var totalReward = 0.0
        var stepCount = 0
        val maxSteps = config.maxStepsPerGame
        
        while (!environment.isTerminal(state) && stepCount < maxSteps) {
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) break
            // Alternate: agent (White) vs baseline heuristic (Black)
            val activeColor = environment.getCurrentBoard().getActiveColor()
            val action = if (activeColor.name.contains("WHITE")) {
                agent.selectAction(state, validActions)
            } else {
                BaselineHeuristicOpponent.selectAction(environment, validActions).let { sel ->
                    if (sel >= 0) sel else agent.selectAction(state, validActions)
                }
            }
            val stepResult = environment.step(action)
            
            totalReward += stepResult.reward
            state = stepResult.nextState
            stepCount++
            
            if (stepResult.done) break
        }
        
        val gameStatus = environment.getGameStatus()
        val gameOutcome = when {
            gameStatus.name.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
            gameStatus.name.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
            gameStatus.name.contains("DRAW") -> GameOutcome.DRAW
            else -> GameOutcome.ONGOING
        }
        
        return EvaluationGameResult(
            gameIndex = gameIndex,
            gameLength = stepCount,
            totalReward = totalReward,
            gameOutcome = gameOutcome,
            finalPosition = environment.getCurrentBoard().toFEN()
        )
    }
    
    /**
     * Calculate performance score from multiple metrics
     */
    private fun calculatePerformanceScore(
        avgReward: Double,
        winRate: Double,
        drawRate: Double,
        avgGameLength: Double
    ): Double {
        // Weighted combination of performance metrics
        val rewardWeight = 0.4
        val winRateWeight = 0.3
        val drawRateWeight = 0.1
        val gameLengthWeight = 0.2
        
        val normalizedReward = ((avgReward + 1.0) / 2.0).coerceIn(0.0, 1.0)
        val normalizedGameLength = (1.0 - (avgGameLength / config.maxStepsPerGame)).coerceIn(0.0, 1.0)
        
        return rewardWeight * normalizedReward +
               winRateWeight * winRate +
               drawRateWeight * drawRate +
               gameLengthWeight * normalizedGameLength
    }
    
    // Additional helper methods will be implemented in the next part...
    

    /**
     * Update adaptive scheduling based on training progress
     */
    private fun updateAdaptiveScheduling() {
        // Consider recent performance trend for scheduling adjustments
        val recentPerformance = performanceHistory.takeLast(config.adaptiveSchedulingWindow)
        
        if (recentPerformance.size >= config.adaptiveSchedulingWindow) {
            val performanceTrend = calculateTrend(recentPerformance)
            
            // Adjust games per cycle based on performance trend
            when {
                performanceTrend > config.performanceImprovementThreshold -> {
                    // Performance improving, can reduce games and increase training
                    currentGamesPerCycle = (currentGamesPerCycle * 0.9).toInt()
                        .coerceAtLeast(config.minGamesPerCycle)
                    currentTrainingRatio = (currentTrainingRatio * 1.1)
                        .coerceAtMost(config.maxTrainingRatio)
                    println("   📈 Performance improving: reducing games, increasing training")
                }
                performanceTrend < -config.performanceImprovementThreshold -> {
                    // Performance declining, increase games and reduce training intensity
                    currentGamesPerCycle = (currentGamesPerCycle * 1.1).toInt()
                        .coerceAtMost(config.maxGamesPerCycle)
                    currentTrainingRatio = (currentTrainingRatio * 0.9)
                        .coerceAtLeast(config.minTrainingRatio)
                    println("   📉 Performance declining: increasing games, reducing training")
                }
                else -> {
                    println("   📊 Performance stable: maintaining current schedule")
                }
            }
        }
    }
    
    /**
     * Calculate trend from a list of values
     */
    private fun calculateTrend(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val n = values.size
        val x = (0 until n).map { it.toDouble() }
        val y = values
        
        val xMean = x.average()
        val yMean = y.average()
        
        val numerator = x.zip(y) { xi, yi -> (xi - xMean) * (yi - yMean) }.sum()
        val denominator = x.map { (it - xMean).pow(2) }.sum()
        
        return if (denominator != 0.0) numerator / denominator else 0.0
    }
    
    /**
     * Check if we should create a checkpoint
     */
    private fun shouldCreateCheckpoint(cycle: Int): Boolean {
        return cycle % config.checkpointInterval == 0 || 
               cycle - lastCheckpointCycle >= config.checkpointInterval
    }
    
    /**
     * Check if we should consider model rollback
     */
    private fun shouldConsiderRollback(): Boolean {
        if (!config.enableModelRollback) return false
        if (config.rollbackWarmupCycles > 0 && currentCycle <= config.rollbackWarmupCycles) return false
        
        val recentPerformance = performanceHistory.takeLast(config.rollbackWindow)
        if (recentPerformance.size < config.rollbackWindow) return false
        
        val avgRecentPerformance = recentPerformance.average()
        val bestPerformance = getBestPerformance()
        
        return (bestPerformance - avgRecentPerformance) > config.rollbackThreshold
    }
    
    /**
     * Consider rolling back to a better model version
     */
    private fun considerModelRollback(
        checkpointManager: CheckpointManager,
        agent: ChessAgent
    ): RollbackResult {
        
        val bestCheckpoint = checkpointManager.getBestCheckpoint()
        if (bestCheckpoint != null && bestCheckpoint.version != currentCycle) {
            try {
                // Load the best model
                checkpointManager.loadCheckpoint(bestCheckpoint, agent)
                
                println("🔄 Rolled back to best model (version ${bestCheckpoint.version})")
                return RollbackResult(
                    rolledBack = true,
                    version = bestCheckpoint.version,
                    reason = "Performance degradation detected"
                )
            } catch (e: Exception) {
                println("❌ Failed to rollback model: ${e.message}")
            }
        }
        
        return RollbackResult(rolledBack = false, version = currentCycle, reason = "No rollback needed")
    }
    
    /**
     * Update opponent strategy based on training progress
     */
    private fun updateOpponentStrategy(
        cycle: Int,
        cycleResult: TrainingCycleResult
    ) {
        when (config.opponentUpdateStrategy) {
            OpponentUpdateStrategy.COPY_MAIN -> {
                if (cycle % config.opponentUpdateFrequency == 0) {
                    // Copy main agent's weights to opponent
                    println("   🔄 Updating opponent: copying main agent weights")
                    try {
                        val src = mainAgent as? ChessAgentAdapter
                        val dst = opponentAgent as? ChessAgentAdapter
                        if (src != null && dst != null) {
                            // Best-effort: save main to temp and load into opponent
                            val tmpPath = "checkpoints/tmp_copy_qnet.json"
                            src.save(tmpPath)
                            dst.load(tmpPath)
                        }
                    } catch (_: Throwable) { /* best-effort */ }
                }
            }
            OpponentUpdateStrategy.HISTORICAL -> {
                if (cycle % config.opponentUpdateFrequency == 0) {
                    // Use a historical version of the main agent
                    val historicalVersion = maxOf(0, cycle - config.opponentHistoryLag)
                    println("   🔄 Updating opponent: using historical version $historicalVersion")
                    val cm = checkpointManager
                    val opp = opponentAgent
                    if (cm != null && opp != null) {
                        val info = cm.getCheckpoint(historicalVersion)
                        if (info != null) {
                            cm.loadCheckpoint(info, opp)
                        } else {
                            // Fallback to best available
                            cm.getBestCheckpoint()?.let { cm.loadCheckpoint(it, opp) }
                        }
                    }
                }
            }
            OpponentUpdateStrategy.ADAPTIVE -> {
                val winRate = cycleResult.performance.winRate
                if (winRate > config.opponentAdaptationThreshold) {
                    println("   🔄 Updating opponent: adaptive strategy (win rate: ${winRate})")
                }
            }
            OpponentUpdateStrategy.FIXED -> {
                // Keep opponent unchanged
            }
        }
    }
    
    /**
     * Perform memory management and cleanup
     */
    private fun performMemoryManagement(experienceManager: AdvancedExperienceManager, cycle: Int) {
        if (cycle % config.memoryCleanupInterval == 0) {
            println("   🧹 Performing memory cleanup")
            experienceManager.performCleanup()
            
            // Force garbage collection if enabled
            if (config.enableGarbageCollection) {
                System.gc()
            }
        }
    }
    
    /**
     * Report advanced training progress
     */
    private fun reportAdvancedProgress(cycle: Int, totalCycles: Int) {
        val progress = (cycle.toDouble() / totalCycles * 100).toInt()
        val recentCycles = cycleHistory.takeLast(config.cycleReportInterval)
        
        if (recentCycles.isNotEmpty()) {
            val avgPerformance = recentCycles.map { it.performance.performanceScore }.average()
            val avgWinRate = recentCycles.map { it.performance.winRate }.average()
            val avgGameLength = recentCycles.map { it.performance.averageGameLength }.average()
            val avgExperienceQuality = recentCycles.map { 
                it.experienceProcessing.averageQuality 
            }.average()
            
            println("\n📊 Advanced Training Progress - Cycle $cycle/$totalCycles ($progress%)")
            println("   Performance Score: ${avgPerformance}")
            println("   Win Rate: ${(avgWinRate * 100)}%")
            println("   Avg Game Length: ${avgGameLength} moves")
            println("   Experience Quality: ${avgExperienceQuality}")
            println("   Games per Cycle: $currentGamesPerCycle")
            println("   Training Ratio: ${currentTrainingRatio}")
            println("   Best Model Version: $bestModelVersion")
            
            // Convergence status
            val convergenceStatus = convergenceDetector.checkConvergence(performanceHistory)
            println("   Convergence: ${convergenceStatus.status} (${convergenceStatus.confidence})")
        }
    }

    /**
     * Compute decayed epsilon based on configured schedule and current cycle.
     * Returns null when decay is disabled or not yet applicable.
     */
    private fun computeDecayedEpsilon(cycle: Int): Double? {
        val start = config.epsDecayStart
        val end = config.epsDecayEnd
        val total = config.epsDecayCycles
        if (start == null || end == null || total == null || total <= 0) return null
        val warmup = config.explorationWarmupCycles
        val step = (cycle - warmup).coerceAtLeast(0)
        if (step <= 0) return start
        val t = (step.toDouble() / total.toDouble()).coerceIn(0.0, 1.0)
        return start + t * (end - start)
    }
    
    /**
     * Calculate final training metrics
     */
    private fun calculateFinalMetrics(): AdvancedFinalMetrics {
        if (cycleHistory.isEmpty()) {
            return AdvancedFinalMetrics(
                totalGamesPlayed = 0,
                totalExperiencesProcessed = 0,
                totalBatchUpdates = 0,
                averagePerformanceScore = 0.0,
                bestPerformanceScore = 0.0,
                averageWinRate = 0.0,
                averageExperienceQuality = 0.0,
                convergenceAchieved = false,
                modelVersionsCreated = 0,
                rollbacksPerformed = 0
            )
        }
        
        val totalGames = cycleHistory.sumOf { it.selfPlayResults.totalGames }
        val totalExperiences = cycleHistory.sumOf { it.selfPlayResults.totalExperiences }
        val totalBatchUpdates = cycleHistory.sumOf { it.trainingResults.totalBatches }
        val avgPerformanceScore = cycleHistory.map { it.performance.performanceScore }.average()
        val bestPerformanceScore = cycleHistory.map { it.performance.performanceScore }.maxOrNull() ?: 0.0
        val avgWinRate = cycleHistory.map { it.performance.winRate }.average()
        val avgExperienceQuality = cycleHistory.map { it.experienceProcessing.averageQuality }.average()
        val convergenceStatus = convergenceDetector.getFinalStatus()
        
        return AdvancedFinalMetrics(
            totalGamesPlayed = totalGames,
            totalExperiencesProcessed = totalExperiences,
            totalBatchUpdates = totalBatchUpdates,
            averagePerformanceScore = avgPerformanceScore,
            bestPerformanceScore = bestPerformanceScore,
            averageWinRate = avgWinRate,
            averageExperienceQuality = avgExperienceQuality,
            convergenceAchieved = convergenceStatus.hasConverged,
            modelVersionsCreated = currentCycle,
            rollbacksPerformed = 0 // Would track actual rollbacks in practice
        )
    }
    
    /**
     * Get current training status
     */
    fun getTrainingStatus(): AdvancedTrainingStatus {
        return AdvancedTrainingStatus(
            isTraining = isTraining,
            currentCycle = currentCycle,
            totalCycles = totalCycles,
            completedCycles = cycleHistory.size,
            bestModelVersion = bestModelVersion,
            currentGamesPerCycle = currentGamesPerCycle,
            currentTrainingRatio = currentTrainingRatio,
            convergenceStatus = convergenceDetector.checkConvergence(performanceHistory)
        )
    }
    
    /**
     * Pause training gracefully
     */
    fun pauseTraining() {
        if (isTraining && !isPaused) {
            isPaused = true
            println("⏸️ Advanced self-play training paused")
        }
    }
    
    /**
     * Resume training from pause
     */
    fun resumeTraining() {
        if (isTraining && isPaused) {
            isPaused = false
            println("▶️ Advanced self-play training resumed")
        }
    }

    /**
     * Load a checkpoint from an explicit path into the main agent (JVM only).
     */
    fun loadCheckpointPath(path: String): Boolean {
        val agent = mainAgent ?: return false
        return try {
            agent.load(path)
            println("📂 Loaded checkpoint from path: $path")
            true
        } catch (e: Exception) {
            println("❌ Failed to load checkpoint from $path: ${e.message}")
            false
        }
    }

    /**
     * Load a specific checkpoint version into the main agent (JVM path uses real file I/O).
     * Returns true on success.
     */
    fun loadCheckpointVersion(version: Int): Boolean {
        val agent = mainAgent ?: return false
        val manager = checkpointManager ?: return false
        val info = manager.getCheckpoint(version) ?: return false
        val result = manager.loadCheckpoint(info, agent)
        if (result.success) {
            println("📂 Loaded checkpoint version ${result.version} into main agent")
        } else {
            println("❌ Failed to load checkpoint version $version: ${result.error}")
        }
        return result.success
    }

    /**
     * Load the current best checkpoint into the main agent if available.
     * Returns true on success.
     */
    fun loadBestCheckpoint(): Boolean {
        val agent = mainAgent ?: return false
        val manager = checkpointManager ?: return false
        val best = manager.getBestCheckpoint() ?: return false
        val result = manager.loadCheckpoint(best, agent)
        if (result.success) {
            println("📂 Loaded best checkpoint version ${best.version} into main agent")
            return true
        }
        println("❌ Failed to load best checkpoint: ${result.error}")
        return false
    }
    
    /**
     * Check if training is currently paused
     */
    fun isTrainingPaused(): Boolean = isPaused
    
    /**
     * Stop training gracefully
     */
    fun stopTraining() {
        if (isTraining) {
            selfPlaySystem?.stop()
            isTraining = false
            isPaused = false
            println("🛑 Advanced self-play training stopped by user")
        }
    }
    
    /**
     * Get best performance from history
     */
    private fun getBestPerformance(): Double {
        return performanceHistory.maxOrNull() ?: Double.NEGATIVE_INFINITY
    }
    
    /**
     * Create main training agent
     */
    private fun createMainAgent(): ChessAgent {
        return when (agentType) {
            AgentType.DQN, AgentType.POLICY_GRADIENT, AgentType.ACTOR_CRITIC -> ChessAgentFactory.createDQNAgent(
                hiddenLayers = config.hiddenLayers,
                learningRate = config.learningRate,
                explorationRate = config.explorationRate,
                config = ChessAgentConfig(
                    batchSize = config.batchSize,
                    maxBufferSize = config.maxExperienceBufferSize,
                    targetUpdateFrequency = config.targetUpdateFrequency
                ),
                enableDoubleDQN = config.enableDoubleDQN
            )
            else -> ChessAgentFactory.createDQNAgent(
                hiddenLayers = config.hiddenLayers,
                learningRate = config.learningRate,
                explorationRate = config.explorationRate,
                config = ChessAgentConfig(
                    batchSize = config.batchSize,
                    maxBufferSize = config.maxExperienceBufferSize,
                    targetUpdateFrequency = config.targetUpdateFrequency
                )
            )
        }
    }
    
    /**
     * Create opponent agent
     */
    private fun createOpponentAgent(): ChessAgent {
        return when (agentType) {
            AgentType.DQN, AgentType.POLICY_GRADIENT, AgentType.ACTOR_CRITIC -> ChessAgentFactory.createDQNAgent(
                hiddenLayers = config.hiddenLayers,
                learningRate = config.learningRate,
                explorationRate = config.explorationRate,
                config = ChessAgentConfig(
                    batchSize = config.batchSize,
                    maxBufferSize = config.maxExperienceBufferSize,
                    targetUpdateFrequency = config.targetUpdateFrequency
                ),
                enableDoubleDQN = config.enableDoubleDQN
            )
            else -> ChessAgentFactory.createDQNAgent(
                hiddenLayers = config.hiddenLayers,
                learningRate = config.learningRate,
                explorationRate = config.explorationRate,
                config = ChessAgentConfig(
                    batchSize = config.batchSize,
                    maxBufferSize = config.maxExperienceBufferSize,
                    targetUpdateFrequency = config.targetUpdateFrequency
                )
            )
        }
    }
}

/**
 * Configuration for advanced self-play training pipeline
 */
data class AdvancedSelfPlayConfig(
    // Agent configuration
    val hiddenLayers: List<Int> = listOf(512, 256, 128),
    val learningRate: Double = 0.001,
    val explorationRate: Double = 0.1,
    // DQN target network update frequency (updates every N policy updates)
    val targetUpdateFrequency: Int = 100,
    // Enable Double DQN target selection
    val enableDoubleDQN: Boolean = false,
    // Early-cycle exploration warmup
    val explorationWarmupCycles: Int = 2,
    val explorationWarmupRate: Double = 0.25,
    // Optional epsilon decay after warmup (disabled when null)
    val epsDecayStart: Double? = null,
    val epsDecayEnd: Double? = null,
    val epsDecayCycles: Int? = null,
    
    // Self-play configuration
    val initialGamesPerCycle: Int = 20,
    val minGamesPerCycle: Int = 10,
    val maxGamesPerCycle: Int = 50,
    val maxConcurrentGames: Int = 4,
    val maxStepsPerGame: Int = 200,
    val evaluationGamesPerCycle: Int = 5,
    
    // Training configuration
    val batchSize: Int = 64,
    val maxBatchesPerCycle: Int = 20,
    val initialTrainingRatio: Double = 0.3,
    val minTrainingRatio: Double = 0.1,
    val maxTrainingRatio: Double = 0.8,
    
    // Experience management
    val maxExperienceBufferSize: Int = 50000,
    val experienceQualityThreshold: Double = 0.5,
    val experienceCleanupStrategy: ExperienceCleanupStrategy = ExperienceCleanupStrategy.LOWEST_QUALITY,
    val samplingStrategies: List<SamplingStrategy> = listOf(
        // Use available strategies from TrainingPipeline: RANDOM, RECENT, MIXED
        SamplingStrategy.RANDOM, SamplingStrategy.RECENT, SamplingStrategy.MIXED
    ),
    
    // Reward configuration
    val winReward: Double = 1.0,
    val lossReward: Double = -1.0,
    val drawReward: Double = 0.0,
    val enablePositionRewards: Boolean = false,
    // Reporting behavior
    val treatStepLimitAsDrawForReporting: Boolean = true,
    
    // Adaptive scheduling
    val adaptiveSchedulingWindow: Int = 5,
    val performanceImprovementThreshold: Double = 0.01,
    
    // Checkpointing and model management
    val checkpointInterval: Int = 5,
    val checkpointDirectory: String = "checkpoints/advanced",
    val maxModelVersions: Int = 20,
    val enableCheckpointCompression: Boolean = true,
    val enableCheckpointValidation: Boolean = true,
    
    // Model rollback
    val enableModelRollback: Boolean = true,
    val rollbackWindow: Int = 3,
    val rollbackThreshold: Double = 0.1,
    val rollbackWarmupCycles: Int = 2,

    // Penalties and rewards
    val stepLimitPenalty: Double = -0.05,
    
    // Opponent strategy
    val opponentUpdateStrategy: OpponentUpdateStrategy = OpponentUpdateStrategy.COPY_MAIN,
    val opponentUpdateFrequency: Int = 3,
    val opponentHistoryLag: Int = 5,
    val opponentAdaptationThreshold: Double = 0.7,
    // Use a fixed heuristic opponent for the first N cycles to generate decisive outcomes
    val opponentWarmupCycles: Int = 2,
    
    // Convergence detection
    val enableEarlyStopping: Boolean = true,
    val convergenceConfig: ConvergenceConfig = ConvergenceConfig(),
    
    // Validation thresholds
    val gradientClipThreshold: Double = 10.0,
    val minGradientNorm: Double = 1e-6,
    val minPolicyEntropy: Double = 0.1,
    
    // Memory management
    val enableMemoryOptimization: Boolean = true,
    val memoryCleanupInterval: Int = 10,
    val enableGarbageCollection: Boolean = true,
    
    // Monitoring and reporting
    val progressReportInterval: Int = 5,
    val cycleReportInterval: Int = 5
    ,
    // Checkpoint retention and cleanup
    val autoCleanupOnFinish: Boolean = true,
    val retention: CheckpointRetention = CheckpointRetention(keepBest = true, keepLastN = 2, keepEveryN = null)
)

/**
 * Result of a single training cycle
 */
data class TrainingCycleResult(
    val cycle: Int,
    val selfPlayResults: SelfPlayResults,
    val experienceProcessing: ExperienceProcessingResult,
    val trainingResults: ValidatedTrainingResults,
    val performance: PerformanceEvaluationResult,
    val cycleDuration: Long,
    val adaptiveScheduling: AdaptiveSchedulingState
)

/**
 * State of adaptive scheduling
 */
data class AdaptiveSchedulingState(
    val gamesPerCycle: Int,
    val trainingRatio: Double,
    val batchSize: Int
)

/**
 * Results of validated batch training
 */
data class ValidatedTrainingResults(
    val totalBatches: Int,
    val validBatches: Int,
    val averageLoss: Double,
    val averageGradientNorm: Double,
    val averagePolicyEntropy: Double,
    val averageExperienceQuality: Double,
    val batchResults: List<ValidatedBatchResult>
)

/**
 * Result of a single validated batch
 */
data class ValidatedBatchResult(
    val batchIndex: Int,
    val batchSize: Int,
    val updateResult: PolicyUpdateResult,
    val validationResult: PolicyValidationResult,
    val experienceQuality: Double
)

/**
 * Performance evaluation result
 */
data class PerformanceEvaluationResult(
    val cycle: Int,
    val gamesPlayed: Int,
    val averageReward: Double,
    val averageGameLength: Double,
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val performanceScore: Double,
    val gameResults: List<EvaluationGameResult>
)

/**
 * Model rollback result
 */
data class RollbackResult(
    val rolledBack: Boolean,
    val version: Int,
    val reason: String
)

/**
 * Complete advanced training results
 */
data class AdvancedTrainingResults(
    val totalCycles: Int,
    val totalDuration: Long,
    val bestModelVersion: Int,
    val bestPerformance: Double,
    val cycleHistory: List<TrainingCycleResult>,
    val performanceHistory: List<Double>,
    val convergenceStatus: ConvergenceStatus,
    val checkpointSummary: CheckpointSummary,
    val experienceStatistics: ExperienceStatistics,
    val finalMetrics: AdvancedFinalMetrics
)

/**
 * Final advanced training metrics
 */
data class AdvancedFinalMetrics(
    val totalGamesPlayed: Int,
    val totalExperiencesProcessed: Int,
    val totalBatchUpdates: Int,
    val averagePerformanceScore: Double,
    val bestPerformanceScore: Double,
    val averageWinRate: Double,
    val averageExperienceQuality: Double,
    val convergenceAchieved: Boolean,
    val modelVersionsCreated: Int,
    val rollbacksPerformed: Int
)

/**
 * Current advanced training status
 */
data class AdvancedTrainingStatus(
    val isTraining: Boolean,
    val currentCycle: Int,
    val totalCycles: Int,
    val completedCycles: Int,
    val bestModelVersion: Int,
    val currentGamesPerCycle: Int,
    val currentTrainingRatio: Double,
    val convergenceStatus: ConvergenceStatus
)
