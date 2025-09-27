package com.chessrl.integration.backend

import com.chessrl.integration.ChessEnvironment
import com.chessrl.integration.logging.ChessRLLogger

/**
 * Chess MDP wrapper for RL4J integration.
 * 
 * Adapts our existing ChessEnvironment to RL4J's MDP interface, ensuring
 * identical problem formulation while enabling RL4J training. This wrapper
 * maintains the same 839-dimensional state encoding and 4096-dimensional
 * action space as our custom implementation.
 * 
 * Key features:
 * - Uses existing ChessEnvironment for all game logic
 * - Maintains identical state and action encodings
 * - Provides illegal action fallback with logging
 * - Ensures compatibility with RL4J training pipeline
 * 
 * NOTE: This is a placeholder implementation. Full RL4J integration will be
 * implemented in subsequent tasks when RL4J dependencies are available.
 * 
 * @param environmentFactory Factory function to create chess environments
 */
class ChessMDP(
    private val environmentFactory: () -> ChessEnvironment = { ChessEnvironment() }
) {
    companion object {
        const val ACTION_SPACE_SIZE = 4096 // 64x64 from-to square combinations
        const val OBSERVATION_SIZE = 839   // Chess state vector dimensions
        
        private val logger = ChessRLLogger.forComponent("ChessMDP")
    }
    
    private val observationSpace = ChessObservationSpace()
    private val actionSpace = ChessActionSpace()
    private var chessEnvironment: ChessEnvironment = environmentFactory()
    private var currentState: DoubleArray = chessEnvironment.reset()
    private var isDone: Boolean = false
    
    init {
        logger.info("Initialized ChessMDP with action space size: $ACTION_SPACE_SIZE, observation size: $OBSERVATION_SIZE")
    }
    
    /**
     * Reset the chess environment to starting position.
     * 
     * Initializes a new chess game and returns the starting position
     * encoded as a 839-dimensional observation vector.
     * 
     * @return Observation vector representing the starting chess position
     */
    fun reset(): DoubleArray {
        logger.debug("Resetting chess MDP to starting position")

        chessEnvironment = environmentFactory()
        currentState = chessEnvironment.reset()
        isDone = false

        logger.debug("Reset complete, observation size: ${currentState.size}")
        return currentState
    }
    
    /**
     * Execute a chess move and return the step result.
     * 
     * Takes an action index (0-4095), decodes it to a chess move,
     * and executes it using the underlying chess environment. If the
     * action is illegal, logs the attempt and falls back to a legal move.
     * 
     * @param action Action index in range [0, 4096)
     * @return Step result containing next observation, reward, done flag
     * @throws IllegalArgumentException if action is out of valid range
     */
    fun step(action: Int): StepResult {
        require(action in 0 until ACTION_SPACE_SIZE) {
            "Action $action out of range [0, $ACTION_SPACE_SIZE)"
        }
        
        logger.debug("Executing action: $action")
        
        // Use existing chess environment step logic which handles illegal actions
        val stepResult = chessEnvironment.step(action)
        
        // Create new observation from the step result
        currentState = stepResult.nextState
        isDone = stepResult.done
        
        // Log illegal action attempts if present in step result info
        stepResult.info["error"]?.let { error ->
            logger.warn("Illegal action handled: $error")
        }
        
        logger.debug("Step complete - reward: ${stepResult.reward}, done: ${stepResult.done}")
        
        return StepResult(currentState, stepResult.reward, stepResult.done, stepResult.info)
    }
    
    /**
     * Check if the current episode is done.
     * 
     * @return true if the game has ended, false otherwise
     */
    fun isDone(): Boolean = isDone
    
    /**
     * Get the current observation.
     * 
     * @return Current observation vector
     */
    fun getCurrentObservation(): DoubleArray = currentState
    
    /**
     * Get the action space size.
     * 
     * @return Size of the action space (4096)
     */
    fun getActionSpaceSize(): Int = ACTION_SPACE_SIZE
    
    /**
     * Get the observation space size.
     * 
     * @return Size of the observation space (839)
     */
    fun getObservationSpaceSize(): Int = OBSERVATION_SIZE
    
    /**
     * Get information about legal actions in the current state.
     * 
     * @return List of legal action indices, or empty list if game is done
     */
    fun getLegalActions(): List<Int> {
        if (isDone) return emptyList()
        
        return try {
            chessEnvironment.getValidActions(currentState)
        } catch (e: Exception) {
            logger.warn("Failed to get legal actions: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Create an action mask for the current state.
     * 
     * @return Boolean array where true indicates legal actions
     */
    fun createActionMask(): BooleanArray {
        val mask = BooleanArray(ACTION_SPACE_SIZE) { false }
        
        if (!isDone) {
            val legalActions = getLegalActions()
            for (action in legalActions) {
                if (action in 0 until ACTION_SPACE_SIZE) {
                    mask[action] = true
                }
            }
        }
        
        return mask
    }
    
    /**
     * Get diagnostic information about the current MDP state.
     * 
     * @return Map containing diagnostic information
     */
    fun getDiagnosticInfo(): Map<String, Any> {
        return mapOf(
            "isDone" to isDone,
            "hasObservation" to true,
            "observationSize" to currentState.size,
            "actionSpaceSize" to ACTION_SPACE_SIZE,
            "legalActionCount" to getLegalActions().size
        )
    }
    
    /**
     * Get the observation space.
     * 
     * @return ChessObservationSpace instance
     */
    fun getObservationSpace(): ChessObservationSpace = observationSpace
    
    /**
     * Get the action space.
     * 
     * @return ChessActionSpace instance
     */
    fun getActionSpace(): ChessActionSpace = actionSpace
    
    /**
     * Close the MDP and clean up resources.
     */
    fun close() {
        // No resources to clean up for chess environment
        logger.debug("ChessMDP closed")
    }
    
    /**
     * Create a new instance of this MDP.
     * 
     * @return New ChessMDP instance with fresh chess environment
     */
    fun newInstance(): ChessMDP = ChessMDP(environmentFactory)
}

/**
 * Data class representing the result of a step in the MDP.
 */
data class StepResult(
    val nextState: DoubleArray,
    val reward: Double,
    val done: Boolean,
    val info: Map<String, Any>
)