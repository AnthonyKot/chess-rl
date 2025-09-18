package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.chess.PieceColor
import kotlin.random.Random
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Concurrent self-play system for chess RL training
 * 
 * This system manages multiple concurrent games between agents, collecting
 * experiences with enhanced metadata for training. It supports configurable
 * parallelism levels and sophisticated experience collection strategies.
 */
class SelfPlaySystem(
    private val config: SelfPlayConfig = SelfPlayConfig()
) {
    
    // Game execution state
    private var isRunning = false
    private var gameCount = 0
    private val activeGames = mutableMapOf<Int, SelfPlayGame>()
    
    // Experience collection
    private val experienceBuffer = mutableListOf<EnhancedExperience>()
    private val gameResults = mutableListOf<SelfPlayGameResult>()
    
    // Statistics tracking
    private var totalGamesCompleted = 0
    private var totalExperiencesCollected = 0
    private val gameOutcomes = mutableMapOf<GameOutcome, Int>()
    
    /**
     * Run concurrent self-play games and collect experiences
     */
    fun runSelfPlayGames(
        whiteAgent: ChessAgent,
        blackAgent: ChessAgent,
        numGames: Int
    ): SelfPlayResults {
        
        println("🎮 Starting self-play system: $numGames games with ${config.maxConcurrentGames} concurrent games")
        
        isRunning = true
        val startTime = getCurrentTimeMillis()
        
        var executor: java.util.concurrent.ExecutorService? = null
        try {
            // Initialize tracking
            experienceBuffer.clear()
            gameResults.clear()
            gameOutcomes.clear()
            totalGamesCompleted = 0
            totalExperiencesCollected = 0
            
            // Prepare a fixed thread pool for per-game execution
            executor = Executors.newFixedThreadPool(config.maxConcurrentGames)

            // Run games in batches to manage concurrency
            var gamesRemaining = numGames
            
            while (gamesRemaining > 0 && isRunning) {
                val batchSize = minOf(config.maxConcurrentGames, gamesRemaining)
                
                println("🔄 Starting batch of $batchSize games (${gamesRemaining} remaining)")
                
                // Start concurrent games
                val gamesBatch = startGamesBatch(whiteAgent, blackAgent, batchSize)
                
                // Wait for all games in batch to complete
                val batchResults = waitForBatchCompletion(gamesBatch, executor)
                
                // Process results
                processBatchResults(batchResults)
                // Optional repetition sampling/logging every N games
                val period = 10 // fixed sampling period
                if (totalGamesCompleted > 0 && totalGamesCompleted % period == 0) {
                    val sample = gameResults.takeLast(minOf(3, gameResults.size))
                    for (gr in sample) {
                        println("   🔁 Repetition sample: gameId=${gr.gameId} maxRepeat=${gr.localRepeatMax} positionsRepeated>=2=${gr.localPositionsRepeated}")
                    }
                }
                
                gamesRemaining -= batchSize
                
                // Progress reporting
                if (totalGamesCompleted % config.progressReportInterval == 0) {
                    reportProgress()
                }
            }
            
            val endTime = getCurrentTimeMillis()
            val totalDuration = endTime - startTime
            
            println("✅ Self-play completed: $totalGamesCompleted games, $totalExperiencesCollected experiences")
            
            return SelfPlayResults(
                totalGames = totalGamesCompleted,
                totalExperiences = totalExperiencesCollected,
                totalDuration = totalDuration,
                gameResults = gameResults.toList(),
                experiences = experienceBuffer.toList(),
                gameOutcomes = gameOutcomes.toMap(),
                averageGameLength = if (gameResults.isNotEmpty()) {
                    gameResults.map { it.gameLength }.average()
                } else 0.0,
                experienceQualityMetrics = calculateExperienceQuality()
            )
            
        } catch (e: Exception) {
            println("❌ Self-play failed: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            isRunning = false
            activeGames.clear()
            executor?.shutdownNow()
        }
    }

    // Repetition stats helpers removed (now computed per game)
    
    /**
     * Start a batch of concurrent games
     */
    private fun startGamesBatch(
        whiteAgent: ChessAgent,
        blackAgent: ChessAgent,
        batchSize: Int
    ): List<SelfPlayGame> {
        
        val games = mutableListOf<SelfPlayGame>()
        
        repeat(batchSize) { _ ->
            val gameId = ++gameCount
            
            // Create independent environments for each game
            val environment = ChessEnvironment(
                rewardConfig = ChessRewardConfig(
                    winReward = config.winReward,
                    lossReward = config.lossReward,
                    drawReward = config.drawReward,
                    stepPenalty = config.stepPenalty,
                    stepLimitPenalty = config.stepLimitPenalty,
                    enablePositionRewards = config.enablePositionRewards,
                    gameLengthNormalization = config.gameLengthNormalization,
                    enableEarlyAdjudication = true,
                    resignMaterialThreshold = 9,
                    noProgressPlies = 40
                )
            )
            
            // Create game instance
            val game = SelfPlayGame(
                gameId = gameId,
                whiteAgent = whiteAgent,
                blackAgent = blackAgent,
                environment = environment,
                config = config
            )
            
            games.add(game)
            activeGames[gameId] = game
        }
        
        return games
    }
    
    /**
     * Wait for all games in batch to complete and collect results
     */
    private fun waitForBatchCompletion(games: List<SelfPlayGame>, executor: java.util.concurrent.ExecutorService): List<SelfPlayGameResult> {
        val tasks = games.map { game ->
            Callable {
                try {
                    val result = game.playGame()
                    // Remove from active games after completion
                    synchronized(activeGames) { activeGames.remove(game.gameId) }
                    result
                } catch (e: Exception) {
                    println("⚠️ Game ${game.gameId} failed: ${e.message}")
                    synchronized(activeGames) { activeGames.remove(game.gameId) }
                    null
                }
            }
        }
        val futures: List<Future<SelfPlayGameResult?>> = tasks.map { executor.submit(it) }
        val results = mutableListOf<SelfPlayGameResult>()
        for (f in futures) {
            try {
                f.get()?.let { results.add(it) }
            } catch (ie: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        return results
    }
    
    /**
     * Process results from a completed batch of games
     */
    private fun processBatchResults(batchResults: List<SelfPlayGameResult>) {
        for (result in batchResults) {
            // Add to game results
            gameResults.add(result)
            totalGamesCompleted++
            
            // Track game outcomes (optionally treat step-limit as draw for reporting)
            val outcome = if (
                config.treatStepLimitAsDrawForReporting &&
                result.terminationReason == EpisodeTerminationReason.STEP_LIMIT &&
                result.gameOutcome == GameOutcome.ONGOING
            ) GameOutcome.DRAW else result.gameOutcome
            gameOutcomes[outcome] = gameOutcomes.getOrDefault(outcome, 0) + 1
            
            // Process experiences with enhanced metadata
            val enhancedExperiences = enhanceExperiences(result)
            experienceBuffer.addAll(enhancedExperiences)
            totalExperiencesCollected += enhancedExperiences.size
            
            // Trim experience buffer if it exceeds maximum size
            if (experienceBuffer.size > config.maxExperienceBufferSize) {
                val excessCount = experienceBuffer.size - config.maxExperienceBufferSize
                
                // Remove oldest experiences (or use more sophisticated strategy)
                when (config.experienceCleanupStrategy) {
                    ExperienceCleanupStrategy.OLDEST_FIRST -> {
                        repeat(excessCount) {
                            experienceBuffer.removeAt(0)
                        }
                    }
                    ExperienceCleanupStrategy.LOWEST_QUALITY -> {
                        // Remove experiences with lowest quality scores
                        experienceBuffer.sortBy { it.qualityScore }
                        repeat(excessCount) {
                            experienceBuffer.removeAt(0)
                        }
                    }
                    ExperienceCleanupStrategy.RANDOM -> {
                        repeat(excessCount) {
                            val randomIndex = Random.nextInt(experienceBuffer.size)
                            experienceBuffer.removeAt(randomIndex)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Enhance experiences with additional metadata and quality metrics
     */
    private fun enhanceExperiences(gameResult: SelfPlayGameResult): List<EnhancedExperience> {
        val enhancedExperiences = mutableListOf<EnhancedExperience>()
        
        for ((index, experience) in gameResult.experiences.withIndex()) {
            val enhancedExperience = EnhancedExperience(
                // Core experience data
                state = experience.state,
                action = experience.action,
                reward = experience.reward,
                nextState = experience.nextState,
                done = experience.done,
                
                // Enhanced metadata
                gameId = gameResult.gameId,
                moveNumber = index + 1,
                playerColor = if (index % 2 == 0) PieceColor.WHITE else PieceColor.BLACK,
                gameOutcome = gameResult.gameOutcome,
                terminationReason = gameResult.terminationReason,
                
                // Quality metrics
                qualityScore = calculateExperienceQuality(experience, gameResult, index),
                isFromWinningGame = gameResult.gameOutcome == GameOutcome.WHITE_WINS || 
                                   gameResult.gameOutcome == GameOutcome.BLACK_WINS,
                isFromDrawGame = gameResult.gameOutcome == GameOutcome.DRAW,
                isEarlyGame = index < gameResult.gameLength * 0.3,
                isMidGame = index >= gameResult.gameLength * 0.3 && index < gameResult.gameLength * 0.7,
                isEndGame = index >= gameResult.gameLength * 0.7,
                
                // Chess-specific metadata
                chessMetrics = gameResult.chessMetrics
            )
            
            enhancedExperiences.add(enhancedExperience)
        }
        
        return enhancedExperiences
    }
    
    /**
     * Calculate quality score for an experience
     */
    private fun calculateExperienceQuality(
        experience: Experience<DoubleArray, Int>,
        gameResult: SelfPlayGameResult,
        moveIndex: Int
    ): Double {
        var qualityScore = 0.5 // Base score
        
        // Higher quality for experiences from completed games
        when (gameResult.terminationReason) {
            EpisodeTerminationReason.GAME_ENDED -> qualityScore += 0.3
            EpisodeTerminationReason.STEP_LIMIT -> qualityScore += 0.1
            EpisodeTerminationReason.MANUAL -> qualityScore += 0.0
        }
        
        // Higher quality for experiences from decisive games
        when (gameResult.gameOutcome) {
            GameOutcome.WHITE_WINS, GameOutcome.BLACK_WINS -> qualityScore += 0.2
            GameOutcome.DRAW -> qualityScore += 0.1
            GameOutcome.ONGOING -> qualityScore += 0.0
        }
        
        // Higher quality for experiences with significant rewards
        val absReward = kotlin.math.abs(experience.reward)
        if (absReward > 0.5) qualityScore += 0.1
        if (absReward > 1.0) qualityScore += 0.1
        
        // Higher quality for endgame experiences (more decisive)
        val gameProgress = moveIndex.toDouble() / gameResult.gameLength
        if (gameProgress > 0.7) qualityScore += 0.1
        
        return qualityScore.coerceIn(0.0, 1.0)
    }
    
    /**
     * Calculate overall experience quality metrics
     */
    private fun calculateExperienceQuality(): ExperienceQualityMetrics {
        if (experienceBuffer.isEmpty()) {
            return ExperienceQualityMetrics(
                averageQualityScore = 0.0,
                highQualityExperiences = 0,
                mediumQualityExperiences = 0,
                lowQualityExperiences = 0,
                experiencesFromWins = 0,
                experiencesFromDraws = 0,
                experiencesFromIncomplete = 0
            )
        }
        
        val qualityScores = experienceBuffer.map { it.qualityScore }
        val averageQuality = qualityScores.average()
        
        val highQuality = experienceBuffer.count { it.qualityScore >= 0.7 }
        val mediumQuality = experienceBuffer.count { it.qualityScore >= 0.4 && it.qualityScore < 0.7 }
        val lowQuality = experienceBuffer.count { it.qualityScore < 0.4 }
        
        val fromWins = experienceBuffer.count { it.isFromWinningGame }
        val fromDraws = experienceBuffer.count { it.isFromDrawGame }
        val fromIncomplete = experienceBuffer.size - fromWins - fromDraws
        
        return ExperienceQualityMetrics(
            averageQualityScore = averageQuality,
            highQualityExperiences = highQuality,
            mediumQualityExperiences = mediumQuality,
            lowQualityExperiences = lowQuality,
            experiencesFromWins = fromWins,
            experiencesFromDraws = fromDraws,
            experiencesFromIncomplete = fromIncomplete
        )
    }
    
    /**
     * Report progress during self-play
     */
    private fun reportProgress() {
        val winRate = gameOutcomes.getOrDefault(GameOutcome.WHITE_WINS, 0) + 
                     gameOutcomes.getOrDefault(GameOutcome.BLACK_WINS, 0)
        val drawRate = gameOutcomes.getOrDefault(GameOutcome.DRAW, 0)
        val totalFinished = winRate + drawRate
        
        val avgGameLength = if (gameResults.isNotEmpty()) {
            gameResults.map { it.gameLength }.average()
        } else 0.0
        
        println("📊 Self-Play Progress:")
        println("  Games completed: $totalGamesCompleted")
        println("  Experiences collected: $totalExperiencesCollected")
        println("  Win rate: ${if (totalFinished > 0) winRate.toDouble() / totalFinished * 100 else 0.0}%")
        println("  Draw rate: ${if (totalFinished > 0) drawRate.toDouble() / totalFinished * 100 else 0.0}%")
        println("  Average game length: ${avgGameLength} moves")
        println("  Active games: ${activeGames.size}")
    }
    
    /**
     * Stop self-play system gracefully
     */
    fun stop() {
        isRunning = false
        println("🛑 Self-play system stopping...")
    }
    
    /**
     * Get current self-play statistics
     */
    fun getCurrentStatistics(): SelfPlayStatistics {
        return SelfPlayStatistics(
            totalGamesCompleted = totalGamesCompleted,
            totalExperiencesCollected = totalExperiencesCollected,
            activeGames = activeGames.size,
            gameOutcomes = gameOutcomes.toMap(),
            averageGameLength = if (gameResults.isNotEmpty()) {
                gameResults.map { it.gameLength }.average()
            } else 0.0,
            experienceBufferSize = experienceBuffer.size,
            experienceQualityMetrics = calculateExperienceQuality()
        )
    }
    
    /**
     * Get collected experiences for training
     */
    fun getExperiences(): List<EnhancedExperience> = experienceBuffer.toList()
    
    /**
     * Clear experience buffer
     */
    fun clearExperiences() {
        experienceBuffer.clear()
        totalExperiencesCollected = 0
    }
    
    /**
     * Get game results
     */
    fun getGameResults(): List<SelfPlayGameResult> = gameResults.toList()
}

/**
 * Individual self-play game execution
 */
class SelfPlayGame(
    val gameId: Int,
    private val whiteAgent: ChessAgent,
    private val blackAgent: ChessAgent,
    private val environment: ChessEnvironment,
    private val config: SelfPlayConfig
) {
    
    /**
     * Play a complete game between two agents
     */
    fun playGame(): SelfPlayGameResult {
        val startTime = getCurrentTimeMillis()
        val experiences = mutableListOf<Experience<DoubleArray, Int>>()
        val fenCounts = mutableMapOf<String, Int>()
        
        // Reset environment
        var state = environment.reset()
        fenCounts[environment.getCurrentBoard().toFEN()] = 1
        var stepCount = 0
        var gameResult = "ongoing"
        
        // Game loop
        while (!environment.isTerminal(state) && stepCount < config.maxStepsPerGame) {
            // Determine current player
            val currentAgent = if (stepCount % 2 == 0) whiteAgent else blackAgent
            
            // Get valid actions
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) {
                gameResult = "no_moves"
                break
            }
            
            // Agent selects action; allow heuristic agent to use baseline evaluator
            val action = if (currentAgent is HeuristicChessAgent) {
                val sel = BaselineHeuristicOpponent.selectAction(environment, validActions)
                if (sel >= 0) sel else synchronized(currentAgent) { currentAgent.selectAction(state, validActions) }
            } else {
                synchronized(currentAgent) { currentAgent.selectAction(state, validActions) }
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

            // Register valid actions for the resulting next state to enable correct DQN masking during replay
            runCatching {
                val nextValid = environment.getValidActions(stepResult.nextState)
                ValidActionRegistry.put(stepResult.nextState, nextValid)
            }
            
            // Update state
            state = stepResult.nextState
            // Count current position FEN
            val fen = environment.getCurrentBoard().toFEN()
            fenCounts[fen] = (fenCounts[fen] ?: 0) + 1
            // Optional local threefold detection
            if (config.enableLocalThreefoldDraw && (fenCounts[fen] ?: 0) >= config.localThreefoldThreshold) {
                gameResult = "DRAW_REPETITION_LOCAL"
                break
            }
            stepCount++
            
            if (stepResult.done) {
                gameResult = stepResult.info["game_status"]?.toString() ?: "completed"
                break
            }
        }
        
        val endTime = getCurrentTimeMillis()
        val gameDuration = endTime - startTime
        
        // Determine game outcome and termination reason
        val terminationReason = when {
            gameResult.contains("mate") || gameResult.contains("draw") -> EpisodeTerminationReason.GAME_ENDED
            stepCount >= config.maxStepsPerGame -> EpisodeTerminationReason.STEP_LIMIT
            else -> EpisodeTerminationReason.MANUAL
        }
        // Apply explicit step-limit penalty to the final transition so the agent learns to avoid caps
        if (terminationReason == EpisodeTerminationReason.STEP_LIMIT && experiences.isNotEmpty()) {
            val last = experiences.last()
            val penalized = last.copy(reward = last.reward + config.stepLimitPenalty, done = true)
            experiences[experiences.lastIndex] = penalized
        }
        // Optional repetition penalty (training-only shaping)
        val localRepeatMax = fenCounts.values.maxOrNull() ?: 0
        if (config.repetitionPenalty != null && localRepeatMax >= config.repetitionPenaltyAfter && experiences.isNotEmpty()) {
            val last = experiences.last()
            val penalized = last.copy(reward = last.reward + (config.repetitionPenalty ?: 0.0))
            experiences[experiences.lastIndex] = penalized
        }
        val gameOutcome = if (terminationReason == EpisodeTerminationReason.STEP_LIMIT && config.treatStepLimitAsDrawForReporting) GameOutcome.DRAW else parseGameOutcome(gameResult)
        
        // Get chess metrics
        val chessMetrics = environment.getChessMetrics()
        // Compute simple repetition stats
        val localPositionsRepeated = fenCounts.values.count { it >= 2 }
        
        return SelfPlayGameResult(
            gameId = gameId,
            gameLength = stepCount,
            gameOutcome = gameOutcome,
            terminationReason = terminationReason,
            gameDuration = gameDuration,
            experiences = experiences,
            chessMetrics = chessMetrics,
            finalPosition = environment.getCurrentBoard().toFEN(),
            localRepeatMax = localRepeatMax,
            localPositionsRepeated = localPositionsRepeated
        )
    }
    
    /**
     * Parse game result string to outcome enum
     */
    private fun parseGameOutcome(gameResult: String): GameOutcome {
        return when {
            gameResult.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
            gameResult.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
            gameResult.contains("DRAW") -> GameOutcome.DRAW
            else -> GameOutcome.ONGOING
        }
    }
}

/**
 * Configuration for self-play system
 */
data class SelfPlayConfig(
    // Concurrency settings
    val maxConcurrentGames: Int = 4,
    
    // Game settings
    val maxStepsPerGame: Int = 200,
    // Local threefold repetition detection (optional)
    val enableLocalThreefoldDraw: Boolean = false,
    val localThreefoldThreshold: Int = 3,
    
    // Reward configuration
    val winReward: Double = 1.0,
    val lossReward: Double = -1.0,
    val drawReward: Double = 0.0,
    val enablePositionRewards: Boolean = false,
    // Per-step penalty during self-play generation
    val stepPenalty: Double = -0.001,
    // Control game-length normalization of terminal rewards
    val gameLengthNormalization: Boolean = true,
    // Additional penalty applied when a game hits the step limit (final transition)
    val stepLimitPenalty: Double = -0.05,
    // Optional repetition penalty (training-only shaping)
    val repetitionPenalty: Double? = null,
    val repetitionPenaltyAfter: Int = 2,
    
    // Experience collection
    val maxExperienceBufferSize: Int = 50000,
    val experienceCleanupStrategy: ExperienceCleanupStrategy = ExperienceCleanupStrategy.OLDEST_FIRST,
    
    // Monitoring
    val progressReportInterval: Int = 10,
    
    // Reporting behavior
    val treatStepLimitAsDrawForReporting: Boolean = true
)

/**
 * Strategy for cleaning up experience buffer when full
 */
enum class ExperienceCleanupStrategy {
    OLDEST_FIRST,      // Remove oldest experiences first
    LOWEST_QUALITY,    // Remove experiences with lowest quality scores
    RANDOM             // Remove random experiences
}



/**
 * Enhanced experience with additional metadata
 */
data class EnhancedExperience(
    // Core experience data
    val state: DoubleArray,
    val action: Int,
    val reward: Double,
    val nextState: DoubleArray,
    val done: Boolean,
    
    // Enhanced metadata
    val gameId: Int,
    val moveNumber: Int,
    val playerColor: PieceColor,
    val gameOutcome: GameOutcome,
    val terminationReason: EpisodeTerminationReason,
    
    // Quality metrics
    val qualityScore: Double,
    val isFromWinningGame: Boolean,
    val isFromDrawGame: Boolean,
    val isEarlyGame: Boolean,
    val isMidGame: Boolean,
    val isEndGame: Boolean,
    
    // Chess-specific metadata
    val chessMetrics: ChessMetrics
) {
    /**
     * Convert to basic experience for training
     */
    fun toBasicExperience(): Experience<DoubleArray, Int> {
        val terminalByStepLimit =
            terminationReason == EpisodeTerminationReason.STEP_LIMIT &&
            moveNumber == chessMetrics.gameLength
        return Experience(
            state = state,
            action = action,
            reward = reward,
            nextState = nextState,
            done = done || terminalByStepLimit
        )
    }
}



/**
 * Complete results from self-play session
 */
data class SelfPlayResults(
    val totalGames: Int,
    val totalExperiences: Int,
    val totalDuration: Long,
    val gameResults: List<SelfPlayGameResult>,
    val experiences: List<EnhancedExperience>,
    val gameOutcomes: Map<GameOutcome, Int>,
    val averageGameLength: Double,
    val experienceQualityMetrics: ExperienceQualityMetrics
)

/**
 * Current self-play statistics
 */
data class SelfPlayStatistics(
    val totalGamesCompleted: Int,
    val totalExperiencesCollected: Int,
    val activeGames: Int,
    val gameOutcomes: Map<GameOutcome, Int>,
    val averageGameLength: Double,
    val experienceBufferSize: Int,
    val experienceQualityMetrics: ExperienceQualityMetrics
)

/**
 * Experience quality metrics
 */
data class ExperienceQualityMetrics(
    val averageQualityScore: Double,
    val highQualityExperiences: Int,
    val mediumQualityExperiences: Int,
    val lowQualityExperiences: Int,
    val experiencesFromWins: Int,
    val experiencesFromDraws: Int,
    val experiencesFromIncomplete: Int
)
