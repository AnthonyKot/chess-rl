package com.chessrl.integration

import com.chessrl.integration.backend.DqnLearningBackend
import com.chessrl.integration.backend.LearningBackend
import com.chessrl.integration.backend.LearningSession
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.rl.*
import kotlin.math.abs
import kotlin.random.Random

/**
 * Consolidated training pipeline that orchestrates self-play games, experience replay,
 * validation, and checkpointing while delegating learning logic to a pluggable backend.
 */
class TrainingPipeline(
    private val config: ChessRLConfig,
    private val backend: LearningBackend = DqnLearningBackend()
) {
    
    // Core components
    private var session: LearningSession? = null
    private var environment: ChessEnvironment? = null
    
    // Consolidated components
    private val trainingValidator = TrainingValidator(config)
    private val metricsCollector = MetricsCollector(config)
    // Per-agent locks to guard non-thread-safe selectAction implementations
    private lateinit var mainAgentLock: Any
    private lateinit var opponentAgentLock: Any
    
    // Training state
    private var isTraining = false
    private var currentCycle = 0
    private val cycleHistory = mutableListOf<TrainingCycleResult>()
    private lateinit var experienceManager: ExperienceManager
    private lateinit var evalRandom: Random
    
    // Metrics tracking
    private var bestPerformance = Double.NEGATIVE_INFINITY
    private var totalGamesPlayed = 0
    private var totalExperiencesCollected = 0
    
    /**
     * Initialize the training pipeline with agents and environment.
     */
    fun initialize(): Boolean {
        return try {
            println("🔧 Initializing Training Pipeline")
            println(config.getSummary())
            
            // Validate configuration
            val validation = config.validate()
            validation.printResults()
            validation.throwIfInvalid()
            
            // Initialize seed if specified
            config.seed?.let { seed ->
                try {
                    SeedManager.initializeWithSeed(seed)
                    println("🎲 Deterministic mode initialized with seed: $seed")
                } catch (e: Exception) {
                    println("⚠️ Failed to initialize deterministic mode: ${e.message}")
                }
            }
            
            // Initialize learning backend/session
            val learningSession = backend.createSession(config)
            session = learningSession
            val mainAgent = learningSession.mainAgent
            
            // Create chess environment
            environment = ChessEnvironment(
                rewardConfig = ChessRewardConfig(
                    winReward = config.winReward,
                    lossReward = config.lossReward,
                    drawReward = config.drawReward,
                    stepLimitPenalty = config.stepLimitPenalty,
                    enablePositionRewards = false, // Keep simple for reliability
                    gameLengthNormalization = false,
                    enableEarlyAdjudication = EnvironmentDefaults.ENABLE_EARLY_ADJUDICATION,
                    resignMaterialThreshold = EnvironmentDefaults.RESIGN_MATERIAL_THRESHOLD,
                    noProgressPlies = EnvironmentDefaults.NO_PROGRESS_PLIES
                )
            )
            
            // Initialize consolidated components
            metricsCollector.initialize()
            
            // Initialize experience manager with seeded replay random if available
            val replayRnd = runCatching { SeedManager.getInstance().getReplayBufferRandom() }
                .getOrElse { Random.Default }
            experienceManager = ExperienceManager(
                maxBufferSize = config.maxExperienceBuffer,
                cleanupRatio = 0.1,
                random = replayRnd
            )

            // Initialize evaluation RNG (seeded when available)
            evalRandom = runCatching { SeedManager.getInstance().createSeededRandom("evaluation") }
                .getOrElse { Random.Default }

            // Initialize agent locks (private lock objects, not the agent instances)
            mainAgentLock = Any()
            opponentAgentLock = Any()

            // Set up valid action provider for DQN masking
            mainAgent.setNextActionProvider { state ->
                environment?.getValidActions(state) ?: emptyList()
            }
            
            println("✅ Training Pipeline initialized successfully")
            true
            
        } catch (e: Exception) {
            println("❌ Failed to initialize training pipeline: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Run complete training with structured concurrency for self-play games.
     */
    fun runTraining(): TrainingResults {
        if (isTraining) {
            throw IllegalStateException("Training is already running")
        }

        val session = this.session ?: throw IllegalStateException("Pipeline not initialized")
        val environment = this.environment ?: throw IllegalStateException("Pipeline not initialized")
        
        println("🚀 Starting Training Pipeline")
        println("Total Cycles: ${config.maxCycles}")
        println("=" * 60)
        
        isTraining = true
        currentCycle = 0
        cycleHistory.clear()
        experienceManager.clear()
        bestPerformance = Double.NEGATIVE_INFINITY
        totalGamesPlayed = 0
        totalExperiencesCollected = 0
        
        val startTime = getCurrentTimeMillis()
        
        return try {
            // Main training loop
            for (cycle in 1..config.maxCycles) {
                currentCycle = cycle
                
                println("\n🔄 Training Cycle $cycle/${config.maxCycles}")
                println("-" * 40)
                
                // Execute training cycle with structured concurrency
                val cycleResult = runTrainingCycle(session, environment)
                cycleHistory.add(cycleResult)
                
                // Collect metrics and validate training
                metricsCollector.collectCycleMetrics(
                    cycle = cycle,
                    gameResults = cycleResult.gameResults,
                    trainingMetrics = cycleResult.trainingMetrics,
                    experienceBufferSize = experienceManager.getBufferSize()
                )
                
                val validationResult = trainingValidator.validateTrainingCycle(
                    cycle = cycle,
                    trainingMetrics = cycleResult.trainingMetrics,
                    gameResults = cycleResult.gameResults
                )
                
                // Report validation issues
                if (!validationResult.isValid) {
                    println("⚠️ Validation issues detected:")
                    validationResult.issues.forEach { issue ->
                        println("   ${issue.severity}: ${issue.message}")
                    }
                    if (validationResult.recommendations.isNotEmpty()) {
                        println("   Recommendations:")
                        validationResult.recommendations.forEach { rec ->
                            println("   - $rec")
                        }
                    }
                }
                
                // Update best performance
                if (cycleResult.averageReward > bestPerformance) {
                    bestPerformance = cycleResult.averageReward
                    println("🏆 New best performance: $bestPerformance")
                    
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
                if (cycle % 5 == 0) {
                    metricsCollector.reportProgress(cycle)
                }
                
                // Early stopping check (simple convergence detection)
                if (shouldStopEarly()) {
                    println("🎯 Early stopping triggered at cycle $cycle")
                    break
                }
            }
            
            val endTime = getCurrentTimeMillis()
            val totalDuration = endTime - startTime
            
            // Save final checkpoint
            saveCheckpoint(session, currentCycle, bestPerformance, isBest = false)
            
            // Finalize metrics collection
            metricsCollector.finalize()
            
            println("\n🏁 Training Completed!")
            println("Total cycles: $currentCycle")
            println("Total duration: ${totalDuration}ms")
            println("Best performance: $bestPerformance")
            println("Total games played: $totalGamesPlayed")
            println("Total experiences collected: $totalExperiencesCollected")
            
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
            println("❌ Training failed: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            isTraining = false
        }
    }
    
    /**
     * Execute a single training cycle with structured concurrency.
     */
    private fun runTrainingCycle(
        session: LearningSession,
        environment: ChessEnvironment
    ): TrainingCycleResult {
        val cycleStartTime = getCurrentTimeMillis()
        
        // Clear any stale action masks from previous cycles
        // (ValidActionRegistry removed - using direct environment calls)
        
        // Phase 1: Concurrent self-play game generation
        println("🎮 Phase 1: Self-play generation (${config.gamesPerCycle} games)")
        val gameResults = runSelfPlayGames(session)
        
        // Phase 2: Experience processing and training
        println("🧠 Phase 2: Experience processing and training")
        val trainingMetrics = processExperiencesAndTrain(gameResults, session)
        
        // Phase 3: Performance evaluation
        println("📊 Phase 3: Performance evaluation")
        val evaluationMetrics = evaluatePerformance(session.mainAgent)
        
        val cycleEndTime = getCurrentTimeMillis()
        val cycleDuration = cycleEndTime - cycleStartTime
        
        // Update totals
        totalGamesPlayed += gameResults.size
        totalExperiencesCollected += gameResults.sumOf { it.experiences.size }
        
        return TrainingCycleResult(
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
    }
    
    private fun runSelfPlayGames(session: LearningSession): List<SelfPlayGameResult> {
        val results = mutableListOf<SelfPlayGameResult>()
        repeat(config.gamesPerCycle) {
            runSelfPlayGame(
                gameId = totalGamesPlayed + results.size + 1,
                whiteAgent = session.mainAgent,
                blackAgent = session.opponentAgent,
                environment = createGameEnvironment(),
                whiteLock = mainAgentLock,
                blackLock = opponentAgentLock
            )?.let { results.add(it) }
        }

        val outcomes = results.groupingBy { it.gameOutcome }.eachCount()
        val wins = outcomes[GameOutcome.WHITE_WINS] ?: 0
        val losses = outcomes[GameOutcome.BLACK_WINS] ?: 0
        val draws = outcomes[GameOutcome.DRAW] ?: 0
        val avgLength = if (results.isNotEmpty()) results.map { it.gameLength }.average() else 0.0

        println("✅ Self-play completed: $wins wins, $losses losses, $draws draws")
        println("   Average game length: ${"%.1f".format(avgLength)} moves")

        return results
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
            val startTime = getCurrentTimeMillis()
            val experiences = mutableListOf<Experience<DoubleArray, Int>>()
            
            // Reset environment
            var state = environment.reset()
            var stepCount = 0
            
            // Game loop
            while (!environment.isTerminal(state) && stepCount < config.maxStepsPerGame) {
                // Determine current player
                val currentAgent = if (stepCount % 2 == 0) whiteAgent else blackAgent
                
                // Get valid actions
                val validActions = environment.getValidActions(state)
                if (validActions.isEmpty()) break
                
                // Agent selects action (with per-agent lock for thread safety)
                val lock = if (currentAgent === whiteAgent) whiteLock else blackLock
                val action = synchronized(lock) { currentAgent.selectAction(state, validActions) }
                
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
            
            val endTime = getCurrentTimeMillis()
            val gameDuration = endTime - startTime
            
            // Determine game outcome
            val gameOutcome = determineGameOutcome(environment, stepCount >= config.maxStepsPerGame)
            val terminationReason = if (stepCount >= config.maxStepsPerGame) {
                EpisodeTerminationReason.STEP_LIMIT
            } else {
                EpisodeTerminationReason.GAME_ENDED
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
            println("⚠️ Game $gameId failed: ${e.message}")
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
            val numBatches = (experienceManager.getBufferSize() / config.batchSize).coerceAtMost(10) // Limit batches per cycle
            
            repeat(numBatches) {
                try {
                    // Sample batch from experience manager
                    val batch = experienceManager.sampleBatch(config.batchSize)
                    
                    // Train agent on batch
                    val updateResult = session.trainOnBatch(batch)
                    
                    totalLoss += updateResult.loss
                    totalGradientNorm += updateResult.gradientNorm
                    totalPolicyEntropy += updateResult.policyEntropy
                    batchCount++
                    
                } catch (e: Exception) {
                    println("⚠️ Batch training failed: ${e.message}")
                }
            }
        }
        
        // Calculate training metrics
        val avgLoss = if (batchCount > 0) totalLoss / batchCount else 0.0
        val avgGradientNorm = if (batchCount > 0) totalGradientNorm / batchCount else 0.0
        val avgPolicyEntropy = if (batchCount > 0) totalPolicyEntropy / batchCount else 0.0
        val avgReward = newExperiences.map { it.reward }.average()
        
        println("   Training: ${batchCount} batches, loss=${"%.4f".format(avgLoss)}, grad=${"%.4f".format(avgGradientNorm)}, entropy=${"%.4f".format(avgPolicyEntropy)}")
        
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
        println("🔍 Evaluating agent performance...")

        val evaluationGames = config.evaluationGames

        // Choose a deterministic starting color using seeded RNG (falls back to true)
        val startWhite = runCatching { evalRandom.nextBoolean() }.getOrElse { true }
        var agentWins = 0
        var draws = 0
        var losses = 0
        
        repeat(evaluationGames) { gameIndex ->
            try {
                val agentPlaysWhite = if (startWhite) (gameIndex % 2 == 0) else (gameIndex % 2 != 0)
                val outcome = runEvaluationGame(agent, agentPlaysWhite)
                when (outcome) {
                    GameOutcome.WHITE_WINS -> if (agentPlaysWhite) agentWins++ else losses++
                    GameOutcome.BLACK_WINS -> if (agentPlaysWhite) losses++ else agentWins++
                    GameOutcome.DRAW -> draws++
                    else -> Unit
                }
            } catch (e: Exception) {
                println("⚠️ Evaluation game $gameIndex failed: ${e.message}")
            }
        }
        
        val total = (agentWins + draws + losses).coerceAtLeast(1)
        val winRate = agentWins.toDouble() / total
        val drawRate = draws.toDouble() / total
        val lossRate = losses.toDouble() / total
        
        return EvaluationMetrics(
            gamesPlayed = total,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate
        )
    }
    
    /**
     * Run a single evaluation game against a baseline opponent.
     */
    private fun runEvaluationGame(agent: ChessAgent, agentPlaysWhite: Boolean): GameOutcome {
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
        
        return determineGameOutcome(evalEnvironment, stepCount >= config.maxStepsPerGame)
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
                enableEarlyAdjudication = EnvironmentDefaults.ENABLE_EARLY_ADJUDICATION,
                resignMaterialThreshold = EnvironmentDefaults.RESIGN_MATERIAL_THRESHOLD,
                noProgressPlies = EnvironmentDefaults.NO_PROGRESS_PLIES
            )
        )
    }
    
    private fun determineGameOutcome(environment: ChessEnvironment, hitStepLimit: Boolean): GameOutcome {
        return if (hitStepLimit) {
            GameOutcome.DRAW // Treat step limit as draw
        } else {
            val gameStatus = environment.getGameStatus()
            when {
                gameStatus.name.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
                gameStatus.name.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
                gameStatus.name.contains("DRAW") -> GameOutcome.DRAW
                else -> GameOutcome.ONGOING
            }
        }
    }
    
    private fun updateOpponentStrategy(session: LearningSession) {
        println("🔄 Updating opponent strategy (backend: ${backend.id})")
        session.updateOpponent()
    }

    private fun saveCheckpoint(session: LearningSession, cycle: Int, performance: Double, isBest: Boolean) {
        try {
            val checkpointDir = config.checkpointDirectory
            val filename = if (isBest) {
                "$checkpointDir/best_model.json"
            } else {
                "$checkpointDir/checkpoint_cycle_${cycle}.json"
            }

            if (isBest) {
                session.saveBest(filename)
            } else {
                session.saveCheckpoint(filename)
            }
            
            val label = if (isBest) "best" else "regular"
            println("💾 Saved $label checkpoint: $filename (performance: ${"%.4f".format(performance)})")
            
        } catch (e: Exception) {
            println("⚠️ Failed to save checkpoint: ${e.message}")
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
            println("🛑 Training stopped by user")
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
    val lossRate: Double
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
