package com.chessrl.integration

import com.chessrl.integration.adapter.ChessEngineFactory
import com.chessrl.integration.backend.BackendFactory
import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.backend.LearningBackend
import com.chessrl.integration.backend.LearningSession
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.error.*
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.integration.CheckpointManager
import com.chessrl.integration.CheckpointConfig
import com.chessrl.integration.CheckpointMetadata
import com.chessrl.rl.*
import kotlin.math.abs
import kotlin.random.Random
import com.chessrl.integration.opponent.OpponentKind
import com.chessrl.integration.opponent.OpponentSelector

/**
 * Core training pipeline that orchestrates self-play games and agent training.
 * 
 * Uses structured concurrency to run multiple self-play games in parallel,
 * then trains the agent on collected experiences using DQN algorithm.
 * Provides automatic multi-process training with fallback to sequential execution.
 * 
 * Key Features:
 * - Multi-process self-play with 3-4x speedup and automatic fallback
 * - Structured logging with consistent format and appropriate levels
 * - Comprehensive error handling with automatic recovery
 * - Built-in checkpointing and model evaluation
 * - System health monitoring with actionable recommendations
 * 
 * @param config Training configuration with essential parameters
 * @param backend Learning backend (defaults to DQN implementation)
 */
class TrainingPipeline(
    private val config: ChessRLConfig,
    private val backend: LearningBackend = BackendFactory.createBackend(BackendType.MANUAL, config)
) {
    
    // Core components
    private var session: LearningSession? = null
    private var environment: ChessEnvironment? = null
    
    // Consolidated components
    private val trainingValidator = TrainingValidator(config)
    private val metricsCollector = MetricsCollector(config)
    private val logger = ChessRLLogger.forComponent("TrainingPipeline")
    private val errorHandler = ErrorHandler()
    
    // Agent locks for sequential self-play (when multi-process is not available)
    private lateinit var mainAgentLock: Any
    private lateinit var opponentAgentLock: Any
    
    // Training state
    private var isTraining = false
    private var currentCycle = 0
    private val cycleHistory = mutableListOf<TrainingCycleResult>()
    private lateinit var experienceManager: ExperienceManager
    private lateinit var evalRandom: Random
    private lateinit var checkpointManager: CheckpointManager
    private var metricsHeaderWritten = false
    
    // Metrics tracking
    private var bestPerformance = Double.NEGATIVE_INFINITY
    private var totalGamesPlayed = 0
    private var totalExperiencesCollected = 0
    
    // Spam control flags
    private var hasShownMultiProcessFallback = false
    private var hasShownSequentialFallback = false
    
    /**
     * Initializes all components required for training.
     * 
     * Sets up the learning backend, chess environment, experience management,
     * checkpoint system, and validates configuration parameters.
     * 
     * @return true if initialization successful, false otherwise
     * @throws ConfigurationError if configuration is invalid
     * @throws InitializationError if component setup fails
     */
    fun initialize(): Boolean {
        return try {
            val startTime = System.currentTimeMillis()
            logger.info("Initializing Training Pipeline")
            ChessRLLogger.logConfiguration(config)
            
            // Validate configuration with error handling
            val validation = config.validate()
            validation.printResults()
            
            if (!validation.isValid) {
                val firstError = validation.errors.firstOrNull()
                if (firstError != null) {
                    throw ConfigurationError.InvalidParameter("configuration", "invalid", firstError)
                } else {
                    validation.throwIfInvalid()
                }
            }
            
            // Initialize seed (deterministic if specified, random otherwise)
            if (config.seed != null) {
                SafeExecution.withErrorHandling("seed initialization") {
                    SeedManager.initializeWithSeed(config.seed)
                    logger.info("Deterministic mode initialized with seed: ${config.seed}")
                } ?: logger.warn("Failed to initialize deterministic mode, continuing with random seed")
            } else {
                SafeExecution.withErrorHandling("random seed initialization") {
                    SeedManager.initializeWithRandomSeed()
                    logger.info("Random seed mode initialized")
                } ?: logger.warn("Failed to initialize random seed mode")
            }
            
            // Initialize learning backend/session with error handling
            val learningSession = SafeExecution.withErrorHandling("backend initialization") {
                backend.createSession(config)
            } ?: throw TrainingError.InitializationFailed("learning backend", RuntimeException("Backend creation failed"))
            
            session = learningSession
            val mainAgent = learningSession.mainAgent
            
            // Create chess environment
            val engineAdapter = ChessEngineFactory.create(config.engine)
            environment = ChessEnvironment(
                rewardConfig = ChessRewardConfig(
                    winReward = config.winReward,
                    lossReward = config.lossReward,
                    drawReward = config.drawReward,
                    stepLimitPenalty = config.stepLimitPenalty,
                    enablePositionRewards = false, // Keep simple for reliability
                    gameLengthNormalization = false,
                    enableEarlyAdjudication = config.trainEarlyAdjudication,
                    resignMaterialThreshold = config.trainResignMaterialThreshold,
                    noProgressPlies = config.trainNoProgressPlies
                ),
                adapter = engineAdapter
            )
            
            // Initialize consolidated components with error handling
            SafeExecution.withErrorHandling("metrics collector initialization") {
                metricsCollector.initialize()
            } ?: logger.warn("Metrics collector initialization failed, continuing without metrics")
            
            // Initialize experience manager with seeded replay random if available
            val replayRnd = SafeExecution.withGracefulDegradation(
                operation = "replay buffer random initialization",
                fallback = Random.Default
            ) {
                SeedManager.getInstance().getReplayBufferRandom()
            }
            
            experienceManager = ExperienceManager(
                maxBufferSize = config.maxExperienceBuffer,
                cleanupRatio = 0.1,
                random = replayRnd
            )

            // Initialize evaluation RNG (seeded when available)
            evalRandom = SafeExecution.withGracefulDegradation(
                operation = "evaluation random initialization",
                fallback = Random.Default
            ) {
                SeedManager.getInstance().createSeededRandom("evaluation")
            }

            // Initialize agent locks for sequential fallback (private lock objects, not the agent instances)
            mainAgentLock = Any()
            opponentAgentLock = Any()

            // Initialize checkpoint manager (compat mode with simple saves retained)
            checkpointManager = CheckpointManager(
                CheckpointConfig(
                    baseDirectory = config.checkpointDirectory,
                    maxVersions = config.checkpointMaxVersions,
                    compressionEnabled = config.checkpointCompressionEnabled,
                    validationEnabled = config.checkpointValidationEnabled,
                    autoCleanupEnabled = config.checkpointAutoCleanupEnabled
                )
            )

            // Set up valid action provider for DQN masking
            mainAgent.setNextActionProvider { state ->
                environment?.getValidActions(state) ?: emptyList()
            }
            
            val duration = System.currentTimeMillis() - startTime
            ChessRLLogger.logInitialization("Training Pipeline", true, duration = duration)
            true
            
        } catch (e: Exception) {
            ChessRLLogger.logInitialization("Training Pipeline", false, e.message)
            logger.error("Initialization failed", e)
            false
        }
    }

    /**
     * Load initial model weights into the current session's agents (for resume).
     * Should be called after initialize() returns true.
     */
    fun loadInitialModel(modelPath: String, loadOpponent: Boolean = true): Boolean {
        val s = this.session ?: return false
        return try {
            s.mainAgent.load(modelPath)
            if (loadOpponent) runCatching { s.opponentAgent.load(modelPath) }
            logger.info("Resumed training from model: $modelPath")
            true
        } catch (e: Exception) {
            logger.warn("Failed to load initial model for resume: ${e.message}")
            false
        }
    }
    
    private fun appendCycleMetrics(path: String, m: CycleMetrics) {
        try {
            val header = "cycle,timestamp,games,avg_len,min_len,max_len,win_rate,draw_rate,loss_rate,avg_reward,avg_loss,avg_grad,avg_entropy,buffer_size,buffer_util,total_games,total_experiences\n"
            if (!metricsHeaderWritten) {
                appendTextFile(path, header)
                metricsHeaderWritten = true
            }
            val line = listOf(
                m.cycle,
                m.timestamp,
                m.gamesPlayed,
                String.format("%.2f", m.avgEpisodeLength),
                m.minEpisodeLength,
                m.maxEpisodeLength,
                String.format("%.4f", m.winRate),
                String.format("%.4f", m.drawRate),
                String.format("%.4f", m.lossRate),
                String.format("%.6f", m.averageReward),
                String.format("%.6f", m.averageLoss),
                String.format("%.6f", m.averageGradientNorm),
                String.format("%.6f", m.averagePolicyEntropy),
                m.experienceBufferSize,
                String.format("%.4f", m.bufferUtilization),
                m.totalGamesPlayed,
                m.totalExperiencesCollected
            ).joinToString(",") + "\n"
            appendTextFile(path, line)
        } catch (_: Throwable) {
            // best-effort metrics export
        }
    }
    /**
     * Runs the complete training process for the configured number of cycles.
     * 
     * Executes self-play games, collects experiences, trains the agent, and
     * automatically saves checkpoints. Uses multi-process parallelism when
     * available with automatic fallback to sequential execution.
     * 
     * @return TrainingResults with game outcomes and training metrics
     * @throws IllegalStateException if pipeline not initialized or already training
     */
    fun runTraining(): TrainingResults {
        if (isTraining) {
            throw IllegalStateException("Training is already running")
        }

        val session = this.session ?: throw IllegalStateException("Pipeline not initialized")
        
        logger.info("Starting Training Pipeline")
        logger.info("Total Cycles: ${config.maxCycles}")
        logger.info("=".repeat(60))
        
        isTraining = true
        currentCycle = 0
        cycleHistory.clear()
        experienceManager.clear()
        bestPerformance = Double.NEGATIVE_INFINITY
        totalGamesPlayed = 0
        totalExperiencesCollected = 0
        
        val startTime = System.currentTimeMillis()
        
        return try {
            // Main training loop with error recovery
            for (cycle in 1..config.maxCycles) {
                currentCycle = cycle
                
                logger.info("")
                logger.info("Training Cycle $cycle/${config.maxCycles}")
                logger.info("-".repeat(40))
                
                // Reset error counts at the beginning of each cycle
                errorHandler.resetErrorCounts()
                
                // Execute training cycle with error handling
                val cycleResult = SafeExecution.withErrorHandling("training cycle $cycle") {
                    runTrainingCycle(session)
                }
                
                if (cycleResult == null) {
                    logger.error("Training cycle $cycle failed completely, attempting to continue")
                    continue
                }
                
                cycleHistory.add(cycleResult)
                
                // Collect metrics and validate training
                val collected = metricsCollector.collectCycleMetrics(
                    cycle = cycle,
                    gameResults = cycleResult.gameResults,
                    trainingMetrics = cycleResult.trainingMetrics,
                    experienceManager = experienceManager
                )
                
                // Validate metrics for accuracy
                val metricsIssues = metricsCollector.validateMetrics(collected)
                if (metricsIssues.isNotEmpty()) {
                    logger.warn("Metrics validation issues detected:")
                    metricsIssues.forEach { issue ->
                        logger.warn("  - $issue")
                    }
                }
                
                // Append metrics to file if configured
                config.metricsFile?.let { path ->
                    appendCycleMetrics(path, collected)
                }
                
                val validationResult = trainingValidator.validateTrainingCycle(
                    cycle = cycle,
                    trainingMetrics = cycleResult.trainingMetrics,
                    gameResults = cycleResult.gameResults
                )
                
                // Report validation issues
                if (!validationResult.isValid) {
                    logger.warn("Validation issues detected:")
                    validationResult.issues.forEach { issue ->
                        logger.warn("  ${issue.severity}: ${issue.message}")
                    }
                    if (validationResult.recommendations.isNotEmpty()) {
                        logger.info("  Recommendations:")
                        validationResult.recommendations.forEach { rec ->
                            logger.info("  - $rec")
                        }
                    }
                }
                
                // Update best performance
                if (cycleResult.averageReward > bestPerformance) {
                    bestPerformance = cycleResult.averageReward
                    logger.info("🏆 New best performance: $bestPerformance")
                    
                    // Save best model
                    saveCheckpoint(session, cycle, bestPerformance, isBest = true)
                }
                
                // Regular checkpointing
                if (cycle % config.checkpointInterval == 0) {
                    saveCheckpoint(session, cycle, cycleResult.averageReward, isBest = false)
                }
                
                // Update opponent strategy (copy main agent periodically)
                if (cycle % 5 == 0) {
                    updateOpponentStrategy(session)
                }
                
                // Progress reporting
                if (!config.summaryOnly && cycle % config.logInterval == 0) {
                    metricsCollector.reportProgress(cycle)
                }
                
                // Early stopping check (simple convergence detection)
                if (shouldStopEarly()) {
                    logger.info("🎯 Early stopping triggered at cycle $cycle")
                    break
                }
            }
            
            val endTime = System.currentTimeMillis()
            val totalDuration = endTime - startTime
            
            // Save final checkpoint
            saveCheckpoint(session, currentCycle, bestPerformance, isBest = false)
            
            // Finalize metrics collection
            metricsCollector.finalize()
            
            logger.info("")
            logger.info("🏁 Training Completed!")
            logger.info("Total cycles: $currentCycle")
            logger.info("Total duration: ${totalDuration}ms")
            logger.info("Best performance: $bestPerformance")
            logger.info("Total games played: $totalGamesPlayed")
            logger.info("Total experiences collected: $totalExperiencesCollected")
            
            TrainingResults(
                totalCycles = currentCycle,
                totalDuration = totalDuration,
                bestPerformance = bestPerformance,
                totalGamesPlayed = totalGamesPlayed,
                totalExperiencesCollected = totalExperiencesCollected,
                cycleHistory = cycleHistory.toList(),
                finalMetrics = calculateFinalMetrics()
            )
            
        } catch (e: Exception) {
            logger.error("Training failed", e)
            throw e
        } finally {
            isTraining = false
        }
    }
    
    /**
     * Execute a single training cycle with structured concurrency.
     */
    private fun runTrainingCycle(
        session: LearningSession
    ): TrainingCycleResult {
        val cycleStartTime = System.currentTimeMillis()
        
        // Clear any stale action masks from previous cycles
        // (ValidActionRegistry removed - using direct environment calls)
        
        // Phase 1: Concurrent self-play game generation
        logger.debug("🎮 Phase 1: Self-play generation (${config.gamesPerCycle} games)")
        val gameResults = runSelfPlayGames(session)
        
        // Phase 2: Experience processing and training
        logger.debug("🧠 Phase 2: Experience processing and training")
        val trainingMetrics = processExperiencesAndTrain(gameResults, session)
        
        // Phase 3: Performance evaluation
        logger.debug("📊 Phase 3: Performance evaluation")
        val evaluationMetrics = evaluatePerformance(session.mainAgent)
        
        val cycleEndTime = System.currentTimeMillis()
        val cycleDuration = cycleEndTime - cycleStartTime
        
        // Update totals
        totalGamesPlayed += gameResults.size
        totalExperiencesCollected += gameResults.sumOf { it.experiences.size }
        
        val result = TrainingCycleResult(
            cycle = currentCycle,
            gamesPlayed = gameResults.size,
            experiencesCollected = gameResults.sumOf { it.experiences.size },
            averageGameLength = gameResults.map { it.gameLength }.average(),
            averageReward = trainingMetrics.averageReward,
            winRate = evaluationMetrics.winRate,
            drawRate = evaluationMetrics.drawRate,
            cycleDuration = cycleDuration,
            gameResults = gameResults,
            trainingMetrics = trainingMetrics
        )
        
        // Log structured training cycle results
        ChessRLLogger.logTrainingCycle(
            cycle = currentCycle,
            totalCycles = config.maxCycles,
            gamesPlayed = result.gamesPlayed,
            avgReward = result.averageReward,
            winRate = result.winRate,
            avgGameLength = result.averageGameLength,
            duration = cycleDuration
        )
        
        return result
    }
    
    private fun runSelfPlayGames(session: LearningSession): List<SelfPlayGameResult> {
        val allowMultiProcess = config.maxConcurrentGames > 1 && config.nnBackend != com.chessrl.integration.backend.BackendType.RL4J
        if (!allowMultiProcess && config.maxConcurrentGames > 1 && config.nnBackend == com.chessrl.integration.backend.BackendType.RL4J) {
            logger.info("RL4J backend detected; forcing sequential self-play to avoid multi-process issues")
        }
        return if (allowMultiProcess) {
            runMultiProcessSelfPlay(session)
        } else {
            runSequentialSelfPlay(session)
        }
    }
    
    private fun runMultiProcessSelfPlay(session: LearningSession): List<SelfPlayGameResult> {
        // Try multi-process approach first
        return try {
            // Save current model for worker processes
            val tempModelPath = "${config.checkpointDirectory}/temp_model_${System.currentTimeMillis()}.json"
            session.saveCheckpoint(tempModelPath)
            
            // Use JVM-specific multi-process implementation
            val multiProcessSelfPlay = createMultiProcessSelfPlay()
            
            if (multiProcessSelfPlay != null) {
                // Use reflection to call methods
                val isAvailableMethod = multiProcessSelfPlay::class.java.getMethod("isAvailable")
                val isAvailable = isAvailableMethod.invoke(multiProcessSelfPlay) as Boolean
                
                if (isAvailable) {
                    logger.info("Using multi-process self-play (${config.maxConcurrentGames} concurrent games)")
                    val startTime = System.currentTimeMillis()
                    
                    val runSelfPlayGamesMethod = multiProcessSelfPlay::class.java.getMethod("runSelfPlayGames", String::class.java)
                    @Suppress("UNCHECKED_CAST")
                    val results = runSelfPlayGamesMethod.invoke(multiProcessSelfPlay, tempModelPath) as List<SelfPlayGameResult>
                    val duration = System.currentTimeMillis() - startTime
                    
                    // Get speedup factor
                    val getSpeedupFactorMethod = multiProcessSelfPlay::class.java.getMethod("getSpeedupFactor")
                    val speedupFactor = getSpeedupFactorMethod.invoke(multiProcessSelfPlay) as Double
                    
                    // Log performance
                    ChessRLLogger.logSelfPlayPerformance(
                        approach = "multi-process",
                        gamesPlayed = results.size,
                        totalDuration = duration,
                        avgGameDuration = if (results.isNotEmpty()) results.map { it.gameDuration }.average().toLong() else 0L,
                        speedupFactor = speedupFactor
                    )
                    val gamesPerSec = if (duration > 0) results.size.toDouble() / (duration / 1000.0) else 0.0
                    logger.info("Throughput: ${"%.2f".format(gamesPerSec)} games/sec")
                
                // Cleanup temp model
                try {
                    java.io.File(tempModelPath).delete()
                } catch (e: Exception) {
                    logger.warn("Failed to cleanup temp model: ${e.message}")
                }
                
                    reportSelfPlayResults(results)
                    results
                } else {
                    if (!hasShownMultiProcessFallback) {
                        logger.warn("Multi-process not available, falling back to sequential execution for all cycles")
                        hasShownMultiProcessFallback = true
                    }
                    runSequentialSelfPlay(session)
                }
            } else {
                if (!hasShownMultiProcessFallback) {
                    logger.warn("Multi-process not available, falling back to sequential execution for all cycles")
                    hasShownMultiProcessFallback = true
                }
                runSequentialSelfPlay(session)
            }
            
        } catch (e: Exception) {
            val error = MultiProcessError.ProcessStartFailed(-1, e)
            val result = errorHandler.handleError(error)
            
            if (result.shouldUseFallback) {
                if (!hasShownMultiProcessFallback) {
                    logger.info("Multi-process execution failed, falling back to sequential execution for remaining cycles")
                    hasShownMultiProcessFallback = true
                }
                runSequentialSelfPlay(session)
            } else {
                throw e
            }
        }
    }
    
    private fun runSequentialSelfPlay(session: LearningSession): List<SelfPlayGameResult> {
        if (!hasShownSequentialFallback) {
            logger.info("Using sequential self-play (${config.gamesPerCycle} games per cycle)")
            hasShownSequentialFallback = true
        }
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<SelfPlayGameResult>()
        
        repeat(config.gamesPerCycle) { _ ->
            val gameId = totalGamesPlayed + results.size + 1
            
            SafeExecution.withErrorHandling("self-play game $gameId") {
                runSelfPlayGame(
                    gameId = gameId,
                    whiteAgent = session.mainAgent,
                    blackAgent = session.opponentAgent,
                    environment = createGameEnvironment(),
                    whiteLock = mainAgentLock,
                    blackLock = opponentAgentLock
                )
            }?.let { results.add(it) }
        }

        val duration = System.currentTimeMillis() - startTime
        ChessRLLogger.logSelfPlayPerformance(
            approach = "sequential",
            gamesPlayed = results.size,
            totalDuration = duration,
            avgGameDuration = if (results.isNotEmpty()) results.map { it.gameDuration }.average().toLong() else 0L
        )

        reportSelfPlayResults(results)
        return results
    }
    
    private fun reportSelfPlayResults(results: List<SelfPlayGameResult>) {
        val outcomes = results.groupingBy { it.gameOutcome }.eachCount()
        val wins = outcomes[GameOutcome.WHITE_WINS] ?: 0
        val losses = outcomes[GameOutcome.BLACK_WINS] ?: 0
        val draws = outcomes[GameOutcome.DRAW] ?: 0
        val ongoing = outcomes[GameOutcome.ONGOING] ?: 0
        val avgLength = if (results.isNotEmpty()) results.map { it.gameLength }.average() else 0.0

        val summary = buildString {
            append("Self-play completed: $wins wins, $losses losses, $draws draws")
            if (ongoing > 0) append(", $ongoing ongoing")
        }
        logger.info(summary)
        if (ongoing > 0) {
            logger.warn("$ongoing game(s) ended without a recorded result (ONGOING). Consider increasing --max-steps or enabling early adjudication.")
        }
        // Termination breakdown
        if (results.isNotEmpty()) {
            val termCounts = results.groupingBy { it.terminationReason }.eachCount()
            val ended = termCounts[EpisodeTerminationReason.GAME_ENDED] ?: 0
            val stepLimit = termCounts[EpisodeTerminationReason.STEP_LIMIT] ?: 0
            val manual = termCounts[EpisodeTerminationReason.MANUAL] ?: 0
            logger.info("Terminations: game_ended=$ended, step_limit=$stepLimit, manual=$manual")
        }
        logger.info("Average game length: ${"%.1f".format(avgLength)} moves")
    }
    
    /**
     * Create multi-process self-play manager (JVM-specific)
     * Returns null on non-JVM platforms
     */
    private fun createMultiProcessSelfPlay(): Any? {
        return try {
            // Use reflection to avoid compile-time dependency on JVM-specific class
            val clazz = Class.forName("com.chessrl.integration.MultiProcessSelfPlay")
            // Prefer 1-arg constructor (enabled by @JvmOverloads)
            val ctor = try {
                clazz.getConstructor(ChessRLConfig::class.java)
            } catch (_: NoSuchMethodException) {
                // Fallback to 2-arg if needed
                clazz.getConstructor(ChessRLConfig::class.java, String::class.java)
            }
            // Create instance, adding default Java path if 2-arg constructor
            if (ctor.parameterCount == 1) ctor.newInstance(config)
            else ctor.newInstance(config, System.getProperty("java.home") + "/bin/java")
        } catch (e: Exception) {
            logger.debug("Multi-process support unavailable: ${e::class.simpleName}: ${e.message}")
            null // Not available on this platform
        }
    }
    
    /**
     * Run a single self-play game with proper error handling.
     */
    private fun runSelfPlayGame(
        gameId: Int,
        whiteAgent: ChessAgent,
        blackAgent: ChessAgent,
        environment: ChessEnvironment,
        whiteLock: Any,
        blackLock: Any
    ): SelfPlayGameResult? {
        return try {
            val startTime = System.currentTimeMillis()
            val experiences = mutableListOf<Experience<DoubleArray, Int>>()
            val selectionRandom = runCatching { SeedManager.getInstance().createSeededRandom("trainOpponentSelection-$gameId") }
                .getOrNull() ?: Random.Default
            val opponentSelection = OpponentSelector.select(config.trainOpponentType, config.trainOpponentDepth, selectionRandom)
            val normalizedOpponentType = config.trainOpponentType?.lowercase()
            if (normalizedOpponentType != null && normalizedOpponentType in setOf("random", "mixed", "hybrid")) {
                val opponentLabel = when (opponentSelection.kind) {
                    OpponentKind.SELF -> "self"
                    OpponentKind.HEURISTIC -> "heuristic"
                    OpponentKind.MINIMAX -> "minimax(d=${opponentSelection.minimaxDepth ?: config.trainOpponentDepth})"
                }
                logger.debug("Game $gameId opponent selection: $opponentLabel")
            }
            val actionEncoder = ChessActionEncoder()
            val minimaxTeacher = if (opponentSelection.kind == OpponentKind.MINIMAX) {
                val depth = opponentSelection.minimaxDepth ?: config.trainOpponentDepth
                val teacherDepth = if (depth <= 0) 2 else depth
                val rnd = runCatching { SeedManager.getInstance().createSeededRandom("minimaxTeacher-$gameId") }.getOrNull()
                com.chessrl.teacher.MinimaxTeacher(depth = teacherDepth, random = rnd ?: Random.Default)
            } else null
            
            // Reset environment
            var state = environment.reset()
            var stepCount = 0
            
            // Game loop
            var stalledNoEncodableMoves = false
            while (!environment.isTerminal(state) && stepCount < config.maxStepsPerGame) {
                // Determine current player
                val currentAgent = if (stepCount % 2 == 0) whiteAgent else blackAgent
                
                // Get valid actions
                val validActions = environment.getValidActions(state)
                if (validActions.isEmpty()) {
                    // No encodable actions for a non-terminal board position; treat as stalled
                    stalledNoEncodableMoves = true
                    break
                }
                
                // Agent selects action (with per-agent lock for thread safety)
                val lock = if (currentAgent === whiteAgent) whiteLock else blackLock
                val action = if (currentAgent === blackAgent && opponentSelection.kind != OpponentKind.SELF) {
                    when (opponentSelection.kind) {
                        OpponentKind.MINIMAX -> {
                            val move = minimaxTeacher!!.act(environment.getCurrentBoard()).bestMove
                            val idx = actionEncoder.encodeMove(move)
                            if (idx in validActions) idx else validActions.first()
                        }
                        OpponentKind.HEURISTIC -> {
                            val idx = BaselineHeuristicOpponent.selectAction(environment, validActions)
                            if (idx >= 0) idx else validActions.first()
                        }
                        else -> validActions.first()
                    }
                } else {
                    synchronized(lock) { currentAgent.selectAction(state, validActions) }
                }
                
                // Take step in environment
                val stepResult = environment.step(action)
                
                // Create experience
                val experience = Experience(
                    state = state,
                    action = action,
                    reward = stepResult.reward,
                    nextState = stepResult.nextState,
                    done = stepResult.done
                )
                
                experiences.add(experience)
                
                // Valid actions are now handled directly by the environment
                // (ValidActionRegistry removed - using direct environment calls)
                
                // Update state
                state = stepResult.nextState
                stepCount++
                
                if (stepResult.done) break
            }
            
            // Apply step limit penalty if game hit the limit
            if (stepCount >= config.maxStepsPerGame && !environment.isTerminal(state) && experiences.isNotEmpty()) {
                val lastExperience = experiences.last()
                val penalizedExperience = lastExperience.copy(
                    reward = lastExperience.reward + config.stepLimitPenalty,
                    done = true
                )
                experiences[experiences.lastIndex] = penalizedExperience
            }
            
            val endTime = System.currentTimeMillis()
            val gameDuration = endTime - startTime
            
            // Determine game outcome
            var gameOutcome = determineGameOutcome(environment, stepCount >= config.maxStepsPerGame)
            var terminationReason = if (stepCount >= config.maxStepsPerGame) {
                EpisodeTerminationReason.STEP_LIMIT
            } else {
                EpisodeTerminationReason.GAME_ENDED
            }
            // Normalize unresolved outcomes caused by empty encodable moves
            if (stalledNoEncodableMoves && gameOutcome == GameOutcome.ONGOING) {
                gameOutcome = GameOutcome.DRAW
                terminationReason = EpisodeTerminationReason.MANUAL
                // Apply a small penalty to discourage stalled states
                if (experiences.isNotEmpty()) {
                    val last = experiences.last()
                    experiences[experiences.lastIndex] = last.copy(reward = last.reward + config.stepLimitPenalty, done = true)
                }
            }
            
            SelfPlayGameResult(
                gameId = gameId,
                gameLength = stepCount,
                gameOutcome = gameOutcome,
                terminationReason = terminationReason,
                gameDuration = gameDuration,
                experiences = experiences,
                chessMetrics = environment.getChessMetrics(),
                finalPosition = environment.getCurrentBoard().toFEN()
            )
            
        } catch (e: Exception) {
            val error = TrainingError.SelfPlayFailed(gameId, e)
            errorHandler.handleError(error)
            null
        }
    }
    
    /**
     * Process collected experiences and train the agent.
     */
    private fun processExperiencesAndTrain(
        gameResults: List<SelfPlayGameResult>,
        session: LearningSession
    ): TrainingMetrics {
        
        // Collect all experiences and add to experience manager
        val newExperiences = gameResults.flatMap { it.experiences }
        experienceManager.addExperiences(newExperiences)
        
        // Perform cleanup if needed
        experienceManager.performCleanup()
        
        // Train agent on collected experiences
        var totalLoss = 0.0
        var totalGradientNorm = 0.0
        var totalPolicyEntropy = 0.0
        var batchCount = 0
        
        if (experienceManager.getBufferSize() >= config.batchSize) {
            val numBatches = (experienceManager.getBufferSize() / config.batchSize)
                .coerceAtLeast(1)
                .coerceAtMost(config.maxBatchesPerCycle)
            
            repeat(numBatches) { batchIndex ->
                SafeExecution.withErrorHandling("batch training $batchIndex") {
                    // Sample batch from experience manager
                    val batch = experienceManager.sampleBatch(config.batchSize)
                    
                    // Train agent on batch
                    val updateResult = session.trainOnBatch(batch)
                    
                    totalLoss += updateResult.loss
                    totalGradientNorm += updateResult.gradientNorm
                    totalPolicyEntropy += updateResult.policyEntropy
                    batchCount++
                } ?: run {
                    // Batch training failed, but we can continue with other batches
                    val error = TrainingError.BatchTrainingFailed(config.batchSize, RuntimeException("Batch training failed"))
                    errorHandler.handleError(error)
                }
            }
        }
        
        // Calculate training metrics
        val avgLoss = if (batchCount > 0) totalLoss / batchCount else 0.0
        val avgGradientNorm = if (batchCount > 0) totalGradientNorm / batchCount else 0.0
        val avgPolicyEntropy = if (batchCount > 0) totalPolicyEntropy / batchCount else 0.0
        val avgReward = if (newExperiences.isNotEmpty()) newExperiences.map { it.reward }.average() else 0.0
        
        // Log training metrics
        ChessRLLogger.logTrainingMetrics(
            batchCount = batchCount,
            avgLoss = avgLoss,
            avgGradientNorm = avgGradientNorm,
            avgPolicyEntropy = avgPolicyEntropy,
            bufferSize = experienceManager.getBufferSize(),
            bufferCapacity = config.maxExperienceBuffer
        )
        
        return TrainingMetrics(
            batchCount = batchCount,
            averageLoss = avgLoss,
            averageGradientNorm = avgGradientNorm,
            averagePolicyEntropy = avgPolicyEntropy,
            averageReward = avgReward,
            experienceBufferSize = experienceManager.getBufferSize()
        )
    }
    
    /**
     * Evaluate agent performance against baseline.
     */
    private fun evaluatePerformance(agent: ChessAgent): EvaluationMetrics {
        logger.debug("Evaluating agent performance...")

        val evaluationGames = config.evaluationGames

        // Choose a deterministic starting color using seeded RNG (falls back to true)
        val startWhite = runCatching { evalRandom.nextBoolean() }.getOrElse { true }
        var agentWins = 0
        var draws = 0
        var losses = 0
        val gameLengths = mutableListOf<Int>()
        
        repeat(evaluationGames) { gameIndex ->
            SafeExecution.withErrorHandling("evaluation game $gameIndex") {
                val agentPlaysWhite = if (startWhite) (gameIndex % 2 == 0) else (gameIndex % 2 != 0)
                val result = runEvaluationGame(agent, agentPlaysWhite)
                gameLengths.add(result.gameLength)
                when (result.outcome) {
                    GameOutcome.WHITE_WINS -> if (agentPlaysWhite) agentWins++ else losses++
                    GameOutcome.BLACK_WINS -> if (agentPlaysWhite) losses++ else agentWins++
                    GameOutcome.DRAW -> draws++
                    else -> Unit
                }
            } ?: run {
                // Evaluation game failed, but we can continue with other games
                val error = TrainingError.EvaluationFailed(RuntimeException("Evaluation game $gameIndex failed"))
                errorHandler.handleError(error)
            }
        }
        
        val total = (agentWins + draws + losses)
        val winRate = if (total > 0) agentWins.toDouble() / total else 0.0
        val drawRate = if (total > 0) draws.toDouble() / total else 0.0
        val lossRate = if (total > 0) losses.toDouble() / total else 0.0
        val averageGameLength = if (gameLengths.isNotEmpty()) gameLengths.average() else 0.0
        
        val result = EvaluationMetrics(
            gamesPlayed = total,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            averageGameLength = averageGameLength
        )
        
        // Log evaluation results
        ChessRLLogger.logEvaluation(
            gamesPlayed = total,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            avgGameLength = averageGameLength
        )
        if (total == 0) {
            logger.warn("No evaluation games completed this cycle. Consider increasing --evaluation-games or checking for evaluation errors.")
        }
        
        return result
    }
    
    /**
     * Run a single evaluation game against a baseline opponent.
     */
    private fun runEvaluationGame(agent: ChessAgent, agentPlaysWhite: Boolean): EvaluationGameResult {
        val evalEnvironment = createGameEnvironment()
        var state = evalEnvironment.reset()
        var stepCount = 0
        
        // Create baseline opponent
        val baselineOpponent = HeuristicChessAgent(agent.getConfig())
        
        while (!evalEnvironment.isTerminal(state) && stepCount < config.maxStepsPerGame) {
            val validActions = evalEnvironment.getValidActions(state)
            if (validActions.isEmpty()) break
            
            // White moves on even steps from initial position
            val agentToMove = ((stepCount % 2 == 0) && agentPlaysWhite) || ((stepCount % 2 != 0) && !agentPlaysWhite)
            val action = if (agentToMove) agent.selectAction(state, validActions)
                         else baselineOpponent.selectAction(state, validActions)
            
            val stepResult = evalEnvironment.step(action)
            state = stepResult.nextState
            stepCount++
            
            if (stepResult.done) break
        }
        
        val outcome = determineGameOutcome(evalEnvironment, stepCount >= config.maxStepsPerGame)
        return EvaluationGameResult(outcome, stepCount)
    }
    
    /**
     * Helper functions
     */
    
    private fun createGameEnvironment(): ChessEnvironment {
        return ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = config.winReward,
                lossReward = config.lossReward,
                drawReward = config.drawReward,
                stepLimitPenalty = config.stepLimitPenalty,
                enablePositionRewards = false,
                gameLengthNormalization = false,
                enableEarlyAdjudication = config.trainEarlyAdjudication,
                resignMaterialThreshold = config.trainResignMaterialThreshold,
                noProgressPlies = config.trainNoProgressPlies
            ),
            adapter = ChessEngineFactory.create(config.engine)
        )
    }
    
    private fun determineGameOutcome(environment: ChessEnvironment, hitStepLimit: Boolean): GameOutcome {
        return if (hitStepLimit) {
            GameOutcome.DRAW // Treat step limit as draw
        } else {
            // Respect early adjudication when present; fall back to computed status otherwise
            val gameStatus = environment.getEffectiveGameStatus()
            when {
                gameStatus.name.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
                gameStatus.name.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
                gameStatus.name.contains("DRAW") -> GameOutcome.DRAW
                else -> GameOutcome.ONGOING
            }
        }
    }
    
    private fun updateOpponentStrategy(session: LearningSession) {
        logger.debug("Updating opponent strategy (backend: ${backend.id})")
        session.updateOpponent()
    }

    private fun saveCheckpoint(session: LearningSession, cycle: Int, performance: Double, isBest: Boolean) {
        val checkpointDir = config.checkpointDirectory
        val compatPath = if (isBest) {
            "$checkpointDir/best_model.json"
        } else {
            "$checkpointDir/checkpoint_cycle_${cycle}.json"
        }

        // 1) Compatibility save using simple filenames
        val compatSuccess = SafeExecution.withErrorHandling("checkpoint save (compat)") {
            if (isBest) session.saveBest(compatPath) else session.saveCheckpoint(compatPath)
            true
        } != null

        // 2) Rich checkpoint via CheckpointManager (versioned + metadata)
        SafeExecution.withErrorHandling("checkpoint save (manager)") {
            val seedCfg = runCatching { SeedManager.getInstance().getSeedConfiguration() }.getOrNull()
            val meta = CheckpointMetadata(
                cycle = cycle,
                performance = performance,
                description = if (isBest) "Best checkpoint at cycle $cycle" else "Checkpoint at cycle $cycle",
                isBest = isBest,
                seedConfiguration = seedCfg,
                configSnapshot = config
            )
            checkpointManager.createCheckpoint(
                agent = session.mainAgent,
                version = cycle,
                metadata = meta
            )
        }

        val type = if (isBest) "best" else "regular"
        ChessRLLogger.logCheckpoint(
            type = type,
            path = if (compatSuccess) compatPath else "failed",
            cycle = cycle,
            performance = performance,
            success = compatSuccess
        )

        // Remove duplicate checkpoint logging - CheckpointManager already logs this

        if (!compatSuccess) {
            val error = TrainingError.CheckpointFailed(compatPath, RuntimeException("Checkpoint save failed"))
            errorHandler.handleError(error)
        }
    }
    

    
    private fun shouldStopEarly(): Boolean {
        // Simple convergence detection: stop if performance hasn't improved in last 20 cycles
        if (cycleHistory.size < 20) return false
        
        val recentPerformance = cycleHistory.takeLast(10).map { it.averageReward }.average()
        val earlierPerformance = cycleHistory.dropLast(10).takeLast(10).map { it.averageReward }.average()
        
        // Stop if improvement is less than 1%
        return abs(recentPerformance - earlierPerformance) / abs(earlierPerformance) < 0.01
    }
    
    private fun calculateFinalMetrics(): FinalTrainingMetrics {
        if (cycleHistory.isEmpty()) {
            return FinalTrainingMetrics(
                totalCycles = 0,
                averageReward = 0.0,
                bestReward = 0.0,
                averageWinRate = 0.0,
                averageGameLength = 0.0,
                convergenceCycle = null
            )
        }
        
        val rewards = cycleHistory.map { it.averageReward }
        val winRates = cycleHistory.map { it.winRate }
        val gameLengths = cycleHistory.map { it.averageGameLength }
        
        return FinalTrainingMetrics(
            totalCycles = cycleHistory.size,
            averageReward = rewards.average(),
            bestReward = rewards.maxOrNull() ?: 0.0,
            averageWinRate = winRates.average(),
            averageGameLength = gameLengths.average(),
            convergenceCycle = detectConvergenceCycle()
        )
    }
    
    private fun detectConvergenceCycle(): Int? {
        if (cycleHistory.size < 20) return null
        
        // Look for the cycle where performance stabilized
        for (i in 10 until cycleHistory.size - 10) {
            val beforeAvg = cycleHistory.subList(i - 10, i).map { it.averageReward }.average()
            val afterAvg = cycleHistory.subList(i, i + 10).map { it.averageReward }.average()
            
            if (abs(afterAvg - beforeAvg) / abs(beforeAvg) < 0.05) {
                return i
            }
        }
        
        return null
    }
    
    /**
     * Stop training gracefully.
     */
    fun stopTraining() {
        if (isTraining) {
            isTraining = false
            logger.info("🛑 Training stopped by user")
        }
    }
    
    /**
     * Get current training status.
     */
    fun getTrainingStatus(): TrainingStatus {
        return TrainingStatus(
            isTraining = isTraining,
            currentCycle = currentCycle,
            totalCycles = config.maxCycles,
            bestPerformance = bestPerformance,
            totalGamesPlayed = totalGamesPlayed,
            totalExperiencesCollected = totalExperiencesCollected,
            experienceBufferSize = experienceManager.getBufferSize()
        )
    }
    
    /**
     * Get system health status and error statistics
     */
    fun getSystemHealth(): SystemHealthStatus {
        val errorStats = errorHandler.getErrorStats()
        val totalErrors = errorStats.values.sum()
        val criticalErrors = errorStats.filterKeys { key ->
            key.contains("CRITICAL") || key.contains("INIT_FAILED")
        }.values.sum()
        
        val healthLevel = when {
            criticalErrors > 0 -> HealthLevel.CRITICAL
            totalErrors > 20 -> HealthLevel.POOR
            totalErrors > 10 -> HealthLevel.DEGRADED
            totalErrors > 0 -> HealthLevel.GOOD
            else -> HealthLevel.EXCELLENT
        }
        
        val recommendations = mutableListOf<String>()
        
        if (errorStats.containsKey("MULTIPROCESS_START_FAILED")) {
            recommendations.add("Consider reducing maxConcurrentGames or using sequential mode")
        }
        if (errorStats.containsKey("TRAINING_BATCH_FAILED")) {
            recommendations.add("Consider reducing batch size or learning rate")
        }
        if (errorStats.containsKey("MODEL_INFERENCE_FAILED")) {
            recommendations.add("Check neural network architecture and input data")
        }
        if (totalErrors > 15) {
            recommendations.add("Consider restarting training with different configuration")
        }
        
        return SystemHealthStatus(
            healthLevel = healthLevel,
            totalErrors = totalErrors,
            criticalErrors = criticalErrors,
            errorBreakdown = errorStats,
            recommendations = recommendations
        )
    }
}

/**
 * Data classes for training results and metrics
 */

data class TrainingResults(
    val totalCycles: Int,
    val totalDuration: Long,
    val bestPerformance: Double,
    val totalGamesPlayed: Int,
    val totalExperiencesCollected: Int,
    val cycleHistory: List<TrainingCycleResult>,
    val finalMetrics: FinalTrainingMetrics
)

data class TrainingCycleResult(
    val cycle: Int,
    val gamesPlayed: Int,
    val experiencesCollected: Int,
    val averageGameLength: Double,
    val averageReward: Double,
    val winRate: Double,
    val drawRate: Double,
    val cycleDuration: Long,
    val gameResults: List<SelfPlayGameResult>,
    val trainingMetrics: TrainingMetrics
)

data class TrainingMetrics(
    val batchCount: Int,
    val averageLoss: Double,
    val averageGradientNorm: Double,
    val averagePolicyEntropy: Double,
    val averageReward: Double,
    val experienceBufferSize: Int
)

data class EvaluationMetrics(
    val gamesPlayed: Int,
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val averageGameLength: Double
)

data class EvaluationGameResult(
    val outcome: GameOutcome,
    val gameLength: Int
)

data class FinalTrainingMetrics(
    val totalCycles: Int,
    val averageReward: Double,
    val bestReward: Double,
    val averageWinRate: Double,
    val averageGameLength: Double,
    val convergenceCycle: Int?
)

data class TrainingStatus(
    val isTraining: Boolean,
    val currentCycle: Int,
    val totalCycles: Int,
    val bestPerformance: Double,
    val totalGamesPlayed: Int,
    val totalExperiencesCollected: Int,
    val experienceBufferSize: Int
)

data class SystemHealthStatus(
    val healthLevel: HealthLevel,
    val totalErrors: Int,
    val criticalErrors: Int,
    val errorBreakdown: Map<String, Int>,
    val recommendations: List<String>
)

enum class HealthLevel {
    EXCELLENT,  // No errors
    GOOD,       // Few minor errors
    DEGRADED,   // Some errors but system functional
    POOR,       // Many errors, performance impacted
    CRITICAL    // Critical errors, system may fail
}
