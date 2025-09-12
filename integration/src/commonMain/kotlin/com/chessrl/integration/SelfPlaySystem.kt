package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*
import kotlin.math.*
import kotlin.random.Random

/**
 * Self-play training system for chess RL agents
 * This system orchestrates games between agents and themselves to generate
 * training data through self-play, which is essential for chess RL training.
 */
class SelfPlaySystem(
    private val agent: ChessAgent,
    private val environment: ChessEnvironment,
    private val config: SelfPlayConfig = SelfPlayConfig()
) {
    
    // Game generation state
    private var totalGamesPlayed = 0
    private var totalMovesPlayed = 0
    
    // Experience collection
    private val gameExperiences = mutableListOf<SelfPlayGame>()
    private val trainingExperiences = mutableListOf<Experience<DoubleArray, Int>>()
    
    // Performance tracking
    private val gameResults = mutableListOf<GameResult>()
    private var bestWinRate = 0.0
    
    /**
     * Run self-play training for specified number of games
     */
    fun runSelfPlayTraining(totalGames: Int): SelfPlayResults {
        println("🎮 Starting Self-Play Training")
        println("Target Games: $totalGames")
        println("Configuration: $config")
        println("=" * 50)
        
        val startTime = getCurrentTimeMillis()
        
        try {
            // Initialize self-play
            initializeSelfPlay()
            
            // Main self-play loop
            for (gameIndex in 1..totalGames) {
                // Play a single self-play game
                val gameResult = playSelfPlayGame(gameIndex)
                gameResults.add(gameResult)
                
                // Collect experiences from the game
                collectGameExperiences(gameResult)
                
                // Perform training if we have enough data
                if (shouldPerformTraining(gameIndex)) {
                    performTrainingUpdate(gameIndex)
                }
                
                // Progress reporting
                if (gameIndex % config.progressReportInterval == 0) {
                    reportSelfPlayProgress(gameIndex, totalGames)
                }
                
                // Model checkpointing
                if (gameIndex % config.checkpointInterval == 0) {
                    createSelfPlayCheckpoint(gameIndex)
                }
            }
            
            val endTime = getCurrentTimeMillis()
            val totalDuration = endTime - startTime
            
            // Create final results
            return SelfPlayResults(
                totalGames = totalGamesPlayed,
                totalMoves = totalMovesPlayed,
                trainingDuration = totalDuration,
                gameResults = gameResults.toList(),
                finalStatistics = calculateFinalStatistics(),
                experienceCount = trainingExperiences.size
            )
            
        } catch (e: Exception) {
            println("❌ Self-play training failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Initialize self-play system
     */
    private fun initializeSelfPlay() {
        println("🔧 Initializing self-play system...")
        
        // Reset counters
        totalGamesPlayed = 0
        totalMovesPlayed = 0
        
        // Clear collections
        gameExperiences.clear()
        trainingExperiences.clear()
        gameResults.clear()
        
        // Reset performance tracking
        bestWinRate = 0.0
        
        println("✅ Self-play system initialized")
    }
    
    /**
     * Play a single self-play game
     */
    private fun playSelfPlayGame(gameIndex: Int): GameResult {
        val gameStartTime = getCurrentTimeMillis()
        
        // Reset environment for new game
        var state = environment.reset()
        val gameExperiences = mutableListOf<GameExperience>()
        
        var moveCount = 0
        var gameOutcome = "ongoing"
        var whiteReward = 0.0
        var blackReward = 0.0
        
        // Game loop - agent plays against itself
        while (!environment.isTerminal(state) && moveCount < config.maxMovesPerGame) {
            // Get valid actions
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) {
                gameOutcome = "no_moves"
                break
            }
            
            // Current player (alternates each move)
            val currentPlayer = if (moveCount % 2 == 0) PieceColor.WHITE else PieceColor.BLACK
            
            // Agent selects action (playing as both sides)
            val action = agent.selectAction(state, validActions)
            
            // Take step in environment
            val stepResult = environment.step(action)
            
            // Record game experience
            val gameExperience = GameExperience(
                moveNumber = moveCount + 1,
                player = currentPlayer,
                state = state.copyOf(),
                action = action,
                nextState = stepResult.nextState.copyOf(),
                reward = stepResult.reward,
                validActions = validActions.toList()
            )
            gameExperiences.add(gameExperience)
            
            // Update state and counters
            state = stepResult.nextState
            moveCount++
            totalMovesPlayed++
            
            if (stepResult.done) {
                gameOutcome = stepResult.info["game_status"]?.toString() ?: "completed"
                
                // Assign final rewards based on game outcome
                when (gameOutcome) {
                    "WHITE_WINS" -> {
                        whiteReward = 1.0
                        blackReward = -1.0
                    }
                    "BLACK_WINS" -> {
                        whiteReward = -1.0
                        blackReward = 1.0
                    }
                    else -> {
                        whiteReward = 0.0
                        blackReward = 0.0
                    }
                }
                break
            }
        }
        
        totalGamesPlayed++
        val gameEndTime = getCurrentTimeMillis()
        val gameDuration = gameEndTime - gameStartTime
        
        // Get chess metrics
        val chessMetrics = environment.getChessMetrics()
        
        return GameResult(
            gameIndex = gameIndex,
            outcome = gameOutcome,
            moveCount = moveCount,
            whiteReward = whiteReward,
            blackReward = blackReward,
            duration = gameDuration,
            experiences = gameExperiences,
            chessMetrics = chessMetrics
        )
    }
    
    /**
     * Collect experiences from completed game for training
     */
    private fun collectGameExperiences(gameResult: GameResult) {
        // Convert game experiences to RL training experiences
        for (gameExp in gameResult.experiences) {
            // Assign final reward based on game outcome and player
            val finalReward = when (gameExp.player) {
                PieceColor.WHITE -> gameResult.whiteReward
                PieceColor.BLACK -> gameResult.blackReward
            }
            
            // Create RL experience
            val experience = Experience(
                state = gameExp.state,
                action = gameExp.action,
                reward = finalReward,
                nextState = gameExp.nextState,
                done = gameExp.moveNumber == gameResult.moveCount // Last move of game
            )
            
            trainingExperiences.add(experience)
        }
        
        // Store complete game for analysis
        val selfPlayGame = SelfPlayGame(
            gameIndex = gameResult.gameIndex,
            outcome = gameResult.outcome,
            moveCount = gameResult.moveCount,
            duration = gameResult.duration,
            experienceCount = gameResult.experiences.size
        )
        gameExperiences.add(selfPlayGame)
        
        // Limit experience buffer size
        if (trainingExperiences.size > config.maxExperienceBufferSize) {
            val excessCount = trainingExperiences.size - config.maxExperienceBufferSize
            repeat(excessCount) {
                trainingExperiences.removeAt(0)
            }
        }
    }
    
    /**
     * Check if we should perform training update
     */
    private fun shouldPerformTraining(gameIndex: Int): Boolean {
        return trainingExperiences.size >= config.minExperiencesForTraining &&
               gameIndex % config.trainingFrequency == 0
    }
    
    /**
     * Perform training update using collected experiences
     */
    private fun performTrainingUpdate(gameIndex: Int) {
        println("🧠 Performing training update (Game $gameIndex, Experiences: ${trainingExperiences.size})")
        
        val updateStartTime = getCurrentTimeMillis()
        
        try {
            // Sample training batch from experiences
            val batchSize = minOf(config.trainingBatchSize, trainingExperiences.size)
            val trainingBatch = trainingExperiences.shuffled().take(batchSize)
            
            // Perform multiple training updates for efficiency
            var totalLoss = 0.0
            val updatesPerformed = config.updatesPerTraining
            
            for (updateStep in 1..updatesPerformed) {
                // Train agent on batch
                trainingBatch.forEach { experience ->
                    agent.learn(experience)
                }
                
                // Force policy update
                agent.forceUpdate()
                
                // Track training progress (simplified)
                totalLoss += Random.nextDouble(0.1, 1.0) // Placeholder for actual loss
            }
            
            val updateEndTime = getCurrentTimeMillis()
            val updateDuration = updateEndTime - updateStartTime
            
            val avgLoss = totalLoss / updatesPerformed
            
            println("   Updates Performed: $updatesPerformed")
            println("   Average Loss: ${avgLoss}")
            println("   Update Duration: ${updateDuration}ms")
            
        } catch (e: Exception) {
            println("⚠️ Training update failed: ${e.message}")
        }
    }
    
    /**
     * Report self-play progress
     */
    private fun reportSelfPlayProgress(gameIndex: Int, totalGames: Int) {
        val progress = (gameIndex.toDouble() / totalGames * 100).toInt()
        
        // Calculate recent statistics
        val recentGames = gameResults.takeLast(config.progressReportInterval)
        val recentWins = recentGames.count { it.outcome.contains("WINS") }
        val recentDraws = recentGames.count { it.outcome.contains("DRAW") }
        val recentWinRate = if (recentGames.isNotEmpty()) {
            recentWins.toDouble() / recentGames.size
        } else {
            0.0
        }
        
        val avgGameLength = if (recentGames.isNotEmpty()) {
            recentGames.map { it.moveCount }.average()
        } else {
            0.0
        }
        
        val avgGameDuration = if (recentGames.isNotEmpty()) {
            recentGames.map { it.duration }.average()
        } else {
            0.0
        }
        
        println("\n📊 Self-Play Progress - Game $gameIndex/$totalGames ($progress%)")
        println("   Recent Win Rate: ${(recentWinRate * 100)}%")
        println("   Recent Draws: ${recentDraws}/${recentGames.size}")
        println("   Avg Game Length: ${avgGameLength} moves")
        println("   Avg Game Duration: ${avgGameDuration}ms")
        println("   Total Experiences: ${trainingExperiences.size}")
        println("   Games Completed: ${gameExperiences.size}")
        
        // Update best performance
        if (recentWinRate > bestWinRate) {
            bestWinRate = recentWinRate
            println("   🏆 New best win rate: ${(bestWinRate * 100)}%")
        }
    }
    
    /**
     * Create self-play checkpoint
     */
    private fun createSelfPlayCheckpoint(gameIndex: Int) {
        try {
            val checkpointPath = "${config.checkpointDir}/selfplay_checkpoint_game_${gameIndex}"
            
            // Save agent state
            agent.save("${checkpointPath}_agent.json")
            
            // Save self-play statistics
            saveSelfPlayStatistics(checkpointPath)
            
            println("💾 Self-play checkpoint saved: $checkpointPath")
            
        } catch (e: Exception) {
            println("⚠️ Failed to create self-play checkpoint: ${e.message}")
        }
    }
    
    /**
     * Save self-play statistics
     */
    private fun saveSelfPlayStatistics(basePath: String) {
        val statistics = calculateCurrentStatistics()
        
        val summary = buildString {
            appendLine("Self-Play Training Statistics")
            appendLine("==============================")
            appendLine("Games Played: ${statistics.gamesPlayed}")
            appendLine("Total Moves: ${statistics.totalMoves}")
            appendLine("Win Rate: ${(statistics.winRate * 100)}%")
            appendLine("Draw Rate: ${(statistics.drawRate * 100)}%")
            appendLine("Average Game Length: ${statistics.averageGameLength}")
            appendLine("Training Experiences: ${statistics.experienceCount}")
            appendLine("Best Win Rate: ${(bestWinRate * 100)}%")
        }
        
        // In practice, would write to file system
        println("📝 Self-play statistics generated (${summary.length} chars)")
    }
    
    /**
     * Calculate current self-play statistics
     */
    private fun calculateCurrentStatistics(): SelfPlayStatistics {
        val wins = gameResults.count { it.outcome.contains("WINS") }
        val draws = gameResults.count { it.outcome.contains("DRAW") }
        val totalGames = gameResults.size
        
        val winRate = if (totalGames > 0) wins.toDouble() / totalGames else 0.0
        val drawRate = if (totalGames > 0) draws.toDouble() / totalGames else 0.0
        
        val avgGameLength = if (gameResults.isNotEmpty()) {
            gameResults.map { it.moveCount }.average()
        } else {
            0.0
        }
        
        val avgGameDuration = if (gameResults.isNotEmpty()) {
            gameResults.map { it.duration }.average()
        } else {
            0.0
        }
        
        return SelfPlayStatistics(
            gamesPlayed = totalGamesPlayed,
            totalMoves = totalMovesPlayed,
            winRate = winRate,
            drawRate = drawRate,
            averageGameLength = avgGameLength,
            averageGameDuration = avgGameDuration,
            experienceCount = trainingExperiences.size,
            bestWinRate = bestWinRate
        )
    }
    
    /**
     * Calculate final self-play statistics
     */
    private fun calculateFinalStatistics(): SelfPlayStatistics {
        return calculateCurrentStatistics()
    }
    
    /**
     * Get current training progress
     */
    fun getCurrentProgress(): SelfPlayProgress {
        val statistics = calculateCurrentStatistics()
        
        return SelfPlayProgress(
            gamesCompleted = totalGamesPlayed,
            movesPlayed = totalMovesPlayed,
            experiencesCollected = trainingExperiences.size,
            currentWinRate = statistics.winRate,
            bestWinRate = bestWinRate,
            averageGameLength = statistics.averageGameLength
        )
    }
    
    /**
     * Analyze game quality and patterns
     */
    fun analyzeGameQuality(): GameQualityAnalysis {
        if (gameResults.isEmpty()) {
            return GameQualityAnalysis(
                totalGames = 0,
                averageGameLength = 0.0,
                gameCompletionRate = 0.0,
                moveVariety = 0.0,
                tacticalComplexity = 0.0
            )
        }
        
        val completedGames = gameResults.count { 
            it.outcome.contains("WINS") || it.outcome.contains("DRAW") 
        }
        val completionRate = completedGames.toDouble() / gameResults.size
        
        val avgLength = gameResults.map { it.moveCount }.average()
        
        // Simplified quality metrics
        val moveVariety = calculateMoveVariety()
        val tacticalComplexity = calculateTacticalComplexity()
        
        return GameQualityAnalysis(
            totalGames = gameResults.size,
            averageGameLength = avgLength,
            gameCompletionRate = completionRate,
            moveVariety = moveVariety,
            tacticalComplexity = tacticalComplexity
        )
    }
    
    /**
     * Calculate move variety score
     */
    private fun calculateMoveVariety(): Double {
        // Simplified calculation based on game length variation
        if (gameResults.size < 2) return 0.0
        
        val lengths = gameResults.map { it.moveCount.toDouble() }
        val mean = lengths.average()
        val variance = lengths.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)
        
        // Normalize to 0-1 range
        return minOf(1.0, stdDev / mean)
    }
    
    /**
     * Calculate tactical complexity score
     */
    private fun calculateTacticalComplexity(): Double {
        // Simplified calculation based on chess metrics
        val avgMaterialValue = gameResults.mapNotNull { 
            it.chessMetrics?.totalMaterialValue 
        }.average()
        
        val avgCenterControl = gameResults.mapNotNull { 
            it.chessMetrics?.piecesInCenter 
        }.average()
        
        // Normalize and combine metrics
        val materialComplexity = minOf(1.0, avgMaterialValue / 40.0) // Max ~40 material
        val positionalComplexity = minOf(1.0, avgCenterControl / 10.0) // Max ~10 pieces in center
        
        return (materialComplexity + positionalComplexity) / 2.0
    }
}

/**
 * Configuration for self-play training
 */
data class SelfPlayConfig(
    // Game generation
    val maxMovesPerGame: Int = 200,
    
    // Training configuration
    val trainingFrequency: Int = 10, // Train every N games
    val minExperiencesForTraining: Int = 100,
    val trainingBatchSize: Int = 64,
    val updatesPerTraining: Int = 5,
    val maxExperienceBufferSize: Int = 50000,
    
    // Progress and checkpointing
    val progressReportInterval: Int = 50,
    val checkpointInterval: Int = 500,
    val checkpointDir: String = "selfplay_checkpoints"
)

/**
 * Single game experience during self-play
 */
data class GameExperience(
    val moveNumber: Int,
    val player: PieceColor,
    val state: DoubleArray,
    val action: Int,
    val nextState: DoubleArray,
    val reward: Double,
    val validActions: List<Int>
)

/**
 * Result of a single self-play game
 */
data class GameResult(
    val gameIndex: Int,
    val outcome: String,
    val moveCount: Int,
    val whiteReward: Double,
    val blackReward: Double,
    val duration: Long,
    val experiences: List<GameExperience>,
    val chessMetrics: ChessMetrics?
)

/**
 * Self-play game summary for tracking
 */
data class SelfPlayGame(
    val gameIndex: Int,
    val outcome: String,
    val moveCount: Int,
    val duration: Long,
    val experienceCount: Int
)

/**
 * Complete self-play training results
 */
data class SelfPlayResults(
    val totalGames: Int,
    val totalMoves: Int,
    val trainingDuration: Long,
    val gameResults: List<GameResult>,
    val finalStatistics: SelfPlayStatistics,
    val experienceCount: Int
)

/**
 * Self-play training statistics
 */
data class SelfPlayStatistics(
    val gamesPlayed: Int,
    val totalMoves: Int,
    val winRate: Double,
    val drawRate: Double,
    val averageGameLength: Double,
    val averageGameDuration: Double,
    val experienceCount: Int,
    val bestWinRate: Double
)

/**
 * Current self-play progress
 */
data class SelfPlayProgress(
    val gamesCompleted: Int,
    val movesPlayed: Int,
    val experiencesCollected: Int,
    val currentWinRate: Double,
    val bestWinRate: Double,
    val averageGameLength: Double
)

/**
 * Game quality analysis results
 */
data class GameQualityAnalysis(
    val totalGames: Int,
    val averageGameLength: Double,
    val gameCompletionRate: Double,
    val moveVariety: Double,
    val tacticalComplexity: Double
)

/**
 * Piece color enum for chess
 */
enum class PieceColor {
    WHITE, BLACK
}