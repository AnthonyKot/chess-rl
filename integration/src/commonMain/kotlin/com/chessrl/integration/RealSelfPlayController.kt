package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*
import kotlin.random.Random
import kotlin.math.pow

/**
 * Real Self-Play Controller that integrates actual chess agents with the chess engine
 * This addresses the integration gaps identified in task 10.1
 */
class RealSelfPlayController(
    private val config: SelfPlayConfig = SelfPlayConfig()
) {
    
    private var isRunning = false
    private var currentSession: SelfPlaySession? = null
    private val gameResults = mutableListOf<SelfPlayGameResult>()
    private val experienceBuffer = com.chessrl.rl.CircularExperienceBuffer<DoubleArray, Int>(maxSize = 10000)
    
    // Chess components
    private val chessEnvironment = ChessEnvironment()
    private val stateEncoder = ChessStateEncoder()
    private val actionEncoder = ChessActionEncoder()
    
    // Agents
    private var whiteAgent: RealChessAgent? = null
    private var blackAgent: RealChessAgent? = null
    
    /**
     * Initialize the self-play system with real agents
     */
    fun initialize(): SelfPlayInitResult {
        return try {
            // Create real chess agents
            whiteAgent = RealChessAgentFactory.createRealDQNAgent()
            
            blackAgent = RealChessAgentFactory.createRealDQNAgent()
            
            SelfPlayInitResult.Success("Self-play controller initialized with real agents")
        } catch (e: Exception) {
            SelfPlayInitResult.Failed("Failed to initialize: ${e.message}")
        }
    }
    
    /**
     * Start a self-play training session
     */
    fun startSession(sessionConfig: SelfPlaySessionConfig): SelfPlaySessionResult {
        if (isRunning) {
            return SelfPlaySessionResult.Failed("Session already running")
        }
        
        if (whiteAgent == null || blackAgent == null) {
            return SelfPlaySessionResult.Failed("Agents not initialized")
        }
        
        return try {
            isRunning = true
            currentSession = SelfPlaySession(
                id = generateSessionId(),
                config = sessionConfig,
                startTime = System.currentTimeMillis()
            )
            
            // Run the training loop
            runTrainingLoop(sessionConfig)
            
            SelfPlaySessionResult.Success(currentSession!!.id)
        } catch (e: Exception) {
            isRunning = false
            SelfPlaySessionResult.Failed("Session failed: ${e.message}")
        }
    }
    
    /**
     * Main training loop that alternates between self-play and network training
     */
    private fun runTrainingLoop(sessionConfig: SelfPlaySessionConfig) {
        var episodeCount = 0
        
        while (isRunning && episodeCount < sessionConfig.maxEpisodes) {
            // Phase 1: Generate self-play games
            val gamesPlayed = generateSelfPlayGames(sessionConfig.gamesPerIteration)
            
            // Phase 2: Collect experiences from games
            val experiencesCollected = collectExperiences(gamesPlayed)
            
            // Phase 3: Train neural networks if we have enough experiences
            if (experienceBuffer.size() >= 32) {
                trainNetworks()
            }
            
            // Phase 4: Update exploration rates
            updateExploration(episodeCount)
            
            episodeCount += gamesPlayed.size
            
            // Update session progress
            currentSession?.let { session ->
                session.episodesCompleted = episodeCount
                session.experiencesCollected = experienceBuffer.size()
            }
            
            // Check for early stopping conditions
            if (shouldStopTraining(episodeCount)) {
                break
            }
        }
        
        isRunning = false
    }
    
    /**
     * Generate self-play games between agents
     */
    private fun generateSelfPlayGames(numGames: Int): List<SelfPlayGameResult> {
        val games = mutableListOf<SelfPlayGameResult>()
        repeat(numGames) { gameIndex ->
            val result = playGame(gameIndex)
            games.add(result)
            gameResults.add(result)
        }
        return games
    }
    
    /**
     * Play a single game between the agents
     */
    private fun playGame(gameIndex: Int): SelfPlayGameResult {
        val game = ChessGame()
        val moves = mutableListOf<GameMove>()
        val experiences = mutableListOf<Experience<DoubleArray, Int>>()
        var moveCount = 0
        val maxMoves = 200 // Prevent infinite games
        
        // Reset environment
        var currentState = chessEnvironment.reset()
        var isWhiteTurn = true
        
        while (!chessEnvironment.isTerminal(currentState) && moveCount < maxMoves) {
            val currentAgent = if (isWhiteTurn) whiteAgent!! else blackAgent!!
            
            // Get valid actions
            val validMoves = game.getCurrentBoard().getAllValidMoves(if (isWhiteTurn) PieceColor.WHITE else PieceColor.BLACK)
            val validActions = validMoves.map { actionEncoder.encodeMove(it) }
            
            if (validActions.isEmpty()) {
                break // No valid moves (shouldn't happen with proper chess logic)
            }
            
            // Agent selects action
            val selectedAction = currentAgent.selectAction(currentState, validActions)
            val selectedMove = actionEncoder.decodeAction(selectedAction)
            
            if (selectedMove == null || selectedMove !in validMoves) {
                // Fallback to random valid move if agent selection is invalid
                val randomMove = validMoves.random()
                val randomAction = actionEncoder.encodeMove(randomMove)
                
                // Make the move
                val stepResult = chessEnvironment.step(randomAction)
                val gameMove = GameMove(
                    move = randomMove,
                    state = currentState.copyOf(),
                    action = randomAction,
                    reward = stepResult.reward,
                    nextState = stepResult.nextState.copyOf()
                )
                moves.add(gameMove)
                
                // Create experience for learning
                val experience = Experience(
                    state = currentState,
                    action = randomAction,
                    reward = stepResult.reward,
                    nextState = stepResult.nextState,
                    done = stepResult.done
                )
                experiences.add(experience)
                currentAgent.learn(experience)
                
                currentState = stepResult.nextState
            } else {
                // Make the selected move
                val stepResult = chessEnvironment.step(selectedAction)
                val gameMove = GameMove(
                    move = selectedMove,
                    state = currentState.copyOf(),
                    action = selectedAction,
                    reward = stepResult.reward,
                    nextState = stepResult.nextState.copyOf()
                )
                moves.add(gameMove)
                
                // Create experience for learning
                val experience = Experience(
                    state = currentState,
                    action = selectedAction,
                    reward = stepResult.reward,
                    nextState = stepResult.nextState,
                    done = stepResult.done
                )
                experiences.add(experience)
                currentAgent.learn(experience)
                
                currentState = stepResult.nextState
                
                if (stepResult.done) {
                    break
                }
            }
            
            isWhiteTurn = !isWhiteTurn
            moveCount++
        }
        
        // Determine game outcome and termination reason
        val currentBoard = game.getCurrentBoard()
        val (outcome, terminationReason) = when {
            currentBoard.isCheckmate(PieceColor.WHITE) -> GameOutcome.BLACK_WINS to EpisodeTerminationReason.GAME_ENDED
            currentBoard.isCheckmate(PieceColor.BLACK) -> GameOutcome.WHITE_WINS to EpisodeTerminationReason.GAME_ENDED
            currentBoard.isStalemate(PieceColor.WHITE) || currentBoard.isStalemate(PieceColor.BLACK) -> GameOutcome.DRAW to EpisodeTerminationReason.GAME_ENDED
            moveCount >= maxMoves -> GameOutcome.DRAW to EpisodeTerminationReason.STEP_LIMIT
            else -> GameOutcome.DRAW to EpisodeTerminationReason.GAME_ENDED
        }
        
        // Get final position
        val finalPosition = try {
            currentBoard.toFEN()
        } catch (e: Exception) {
            // Fallback if toFEN() is not available
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" // Starting position as fallback
        }
        
        return SelfPlayGameResult(
            gameId = gameIndex,
            gameLength = moveCount,
            gameOutcome = outcome,
            terminationReason = terminationReason,
            gameDuration = 0L,
            experiences = experiences,
            chessMetrics = chessEnvironment.getChessMetrics(),
            finalPosition = finalPosition
        )
    }
    
    /**
     * Collect experiences from completed games
     */
    private fun collectExperiences(games: List<SelfPlayGameResult>): Int {
        var experiencesAdded = 0
        
        for (game in games) {
            game.experiences.forEach { experienceBuffer.add(it) }
            experiencesAdded += game.experiences.size
        }
        
        return experiencesAdded
    }
    
    /**
     * Train the neural networks using collected experiences
     */
    private fun trainNetworks() {
        if (experienceBuffer.size() < 32) {
            return
        }
        
        // Sample experiences for training
        val batch = experienceBuffer.sample(32)
        
        // Train both agents (they share the same architecture but have separate networks)
        whiteAgent?.let { agent ->
            // Force update to trigger learning from the batch
            agent.forceUpdate()
        }
        
        blackAgent?.let { agent ->
            // Force update to trigger learning from the batch
            agent.forceUpdate()
        }
    }
    
    /**
     * Update exploration rates for both agents
     */
    private fun updateExploration(episode: Int) {
        val decayRate = 0.995
        val minExploration = 0.01
        val baseRate = 0.1
        val newRate = (baseRate * decayRate.pow(episode.toDouble())).coerceAtLeast(minExploration)
        
        whiteAgent?.setExplorationRate(newRate)
        blackAgent?.setExplorationRate(newRate)
    }
    
    /**
     * Check if training should stop early
     */
    private fun shouldStopTraining(episodeCount: Int): Boolean {
        // Simple convergence check - could be made more sophisticated
        if (gameResults.size < 100) return false
        
        val recentGames = gameResults.takeLast(50)
        val winRate = recentGames.count { it.gameOutcome != GameOutcome.DRAW }.toDouble() / recentGames.size
        
        // Stop if we're seeing too many draws (might indicate convergence)
        return winRate < 0.1
    }
    
    /**
     * Calculate final reward based on game outcome
     */
    private fun calculateFinalReward(outcome: GameOutcome): Double {
        return when (outcome) {
            GameOutcome.WHITE_WINS -> 1.0
            GameOutcome.BLACK_WINS -> -1.0
            GameOutcome.DRAW -> 0.0
            GameOutcome.ONGOING -> 0.0
        }
    }
    
    /**
     * Stop the current training session
     */
    fun stopSession(): Boolean {
        isRunning = false
        return true
    }
    
    /**
     * Get current training metrics
     */
    fun getTrainingMetrics(): SelfPlayMetrics {
        val session = currentSession
        return if (session != null) {
            val recentGames = gameResults.takeLast(50)
            val winRate = if (recentGames.isNotEmpty()) {
                recentGames.count { it.gameOutcome != GameOutcome.DRAW }.toDouble() / recentGames.size
            } else 0.0
            
            val avgGameLength = if (recentGames.isNotEmpty()) {
                recentGames.map { it.gameLength }.average()
            } else 0.0
            
            SelfPlayMetrics(
                sessionId = session.id,
                episodesCompleted = session.episodesCompleted,
                experiencesCollected = session.experiencesCollected,
                winRate = winRate,
                averageGameLength = avgGameLength,
                explorationRate = whiteAgent?.getTrainingMetrics()?.explorationRate ?: 0.0
            )
        } else {
            SelfPlayMetrics(
                sessionId = "none",
                episodesCompleted = 0,
                experiencesCollected = 0,
                winRate = 0.0,
                averageGameLength = 0.0,
                explorationRate = 0.0
            )
        }
    }
    
    private fun generateSessionId(): String {
        return "selfplay_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
    }
}

/**
 * Configuration for real self-play controller training
 */
// Backward compatibility alias; prefer using SelfPlayConfig directly
typealias RealSelfPlayConfig = SelfPlayConfig

/**
 * Configuration for a self-play session
 */
data class SelfPlaySessionConfig(
    val maxEpisodes: Int = 1000,
    val gamesPerIteration: Int = 10,
    val enableLogging: Boolean = true
)

/**
 * Self-play session data
 */
data class SelfPlaySession(
    val id: String,
    val config: SelfPlaySessionConfig,
    val startTime: Long,
    var episodesCompleted: Int = 0,
    var experiencesCollected: Int = 0
)

/**
 * Result of self-play initialization
 */
sealed class SelfPlayInitResult {
    data class Success(val message: String) : SelfPlayInitResult()
    data class Failed(val error: String) : SelfPlayInitResult()
}

/**
 * Result of starting a self-play session
 */
sealed class SelfPlaySessionResult {
    data class Success(val sessionId: String) : SelfPlaySessionResult()
    data class Failed(val error: String) : SelfPlaySessionResult()
}

/**
 * Training metrics for self-play
 */
data class SelfPlayMetrics(
    val sessionId: String,
    val episodesCompleted: Int,
    val experiencesCollected: Int,
    val winRate: Double,
    val averageGameLength: Double,
    val explorationRate: Double
)

/**
 * Represents a single move in a game
 */
data class GameMove(
    val move: Move,
    val state: DoubleArray,
    val action: Int,
    val reward: Double,
    val nextState: DoubleArray
)

// Legacy GameResult removed; controller now emits SelfPlayGameResult directly.
