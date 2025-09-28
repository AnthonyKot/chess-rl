package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.ChessEnvironment
import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.logging.ChessRLLogger

/**
 * Chess MDP (Markov Decision Process) wrapper for RL4J integration.
 * 
 * This class adapts our existing ChessEnvironment to RL4J's MDP interface,
 * enabling RL4J algorithms to train on chess while maintaining our existing
 * game logic, state encoding, and reward computation.
 * 
 * Implements RL4J's MDP interface directly for proper type compatibility.
 */
class ChessMDP(private val chessEnvironment: ChessEnvironment) : org.deeplearning4j.rl4j.mdp.MDP<ChessObservation, Int, org.deeplearning4j.rl4j.space.DiscreteSpace> {
    
    companion object {
        private val logger = ChessRLLogger.forComponent("ChessMDP")
    }
    
    private val observationSpace = ChessObservationSpace()
    private val actionSpace = ChessActionSpace()
    private var isDoneFlag = false
    private var currentObservation: ChessObservation? = null
    
    init {
        logger.debug("Created ChessMDP wrapper for chess environment")
    }
    
    /**
     * Reset the environment to initial state.
     * 
     * @return Initial chess observation (standard starting position)
     */
    override fun reset(): ChessObservation {
        logger.debug("Resetting chess environment")
        
        try {
            val stateVector = chessEnvironment.reset()
            isDoneFlag = false
            
            currentObservation = ChessObservation(stateVector)
            
            logger.debug("Environment reset successfully, observation dimensions: ${stateVector.size}")
            return currentObservation!!
            
        } catch (e: Exception) {
            logger.error("Failed to reset chess environment: ${e.message}")
            throw IllegalStateException("Failed to reset chess environment", e)
        }
    }
    
    /**
     * Execute an action in the environment.
     * 
     * @param action The action to execute (0-4095)
     * @return RL4J StepReply containing next observation, reward, done flag, and info
     */
    override fun step(action: Int): org.deeplearning4j.gym.StepReply<ChessObservation> {
        logger.debug("Executing action: $action")
        
        if (isDoneFlag) {
            logger.warn("Attempted to step in terminal state")
            return org.deeplearning4j.gym.StepReply(
                currentObservation ?: reset(),
                0.0,
                true,
                createInfoObject(mapOf(
                    "error" to "Environment is already terminal",
                    "legal" to 0,
                    "illegal" to true
                ))
            )
        }
        
        try {
            // Validate action
            if (!actionSpace.contains(action)) {
                logger.warn("Invalid action $action, falling back to legal move")
                return handleIllegalAction(action)
            }
            
            // Get legal actions before attempting the move
            val legalActions = chessEnvironment.getValidActions(currentObservation?.getDataArray() ?: DoubleArray(839))
            
            val actualAction = if (action in legalActions) {
                action
            } else {
                logger.warn("Illegal action $action attempted, falling back to best legal move")
                selectFallbackAction(legalActions)
            }
            
            // Execute the action
            val stepResult = chessEnvironment.step(actualAction)
            
            // Update state
            currentObservation = ChessObservation(stepResult.nextState)
            isDoneFlag = stepResult.done
            
            logger.debug("Step completed: reward=${stepResult.reward}, done=${stepResult.done}")
            
            return org.deeplearning4j.gym.StepReply(
                currentObservation!!,
                stepResult.reward,
                stepResult.done,
                createInfoObject(mapOf(
                    "originalAction" to action,
                    "actualAction" to actualAction,
                    "legal" to legalActions.size,
                    "illegal" to (action != actualAction)
                ))
            )
            
        } catch (e: Exception) {
            logger.error("Failed to execute step: ${e.message}")
            
            // Return safe fallback state
            return org.deeplearning4j.gym.StepReply(
                currentObservation ?: reset(),
                -1.0, // Penalty for error
                true,
                createInfoObject(mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "originalAction" to action,
                    "legal" to 0,
                    "illegal" to true
                ))
            )
        }
    }
    
    /**
     * Handle illegal action by selecting a fallback legal move.
     */
    private fun handleIllegalAction(action: Int): org.deeplearning4j.gym.StepReply<ChessObservation> {
        val legalActions = chessEnvironment.getValidActions(currentObservation?.getDataArray() ?: DoubleArray(839))
        
        if (legalActions.isEmpty()) {
            // Game is over
            isDoneFlag = true
            return org.deeplearning4j.gym.StepReply(
                currentObservation ?: reset(),
                0.0,
                true,
                createInfoObject(mapOf(
                    "error" to "No legal actions available",
                    "originalAction" to action,
                    "actualAction" to action,
                    "legal" to 0,
                    "illegal" to true
                ))
            )
        }
        
        val fallbackAction = selectFallbackAction(legalActions)
        logger.info("Using fallback action $fallbackAction for illegal action $action")
        
        // Execute the fallback action but preserve the illegal action information
        return stepWithFallback(action, fallbackAction, legalActions)
    }
    
    /**
     * Execute a step with fallback action, preserving original action information.
     */
    private fun stepWithFallback(originalAction: Int, fallbackAction: Int, legalActions: List<Int>): org.deeplearning4j.gym.StepReply<ChessObservation> {
        try {
            // Execute the fallback action
            val stepResult = chessEnvironment.step(fallbackAction)
            
            // Update state
            currentObservation = ChessObservation(stepResult.nextState)
            isDoneFlag = stepResult.done
            
            logger.debug("Fallback step completed: reward=${stepResult.reward}, done=${stepResult.done}")
            
            return org.deeplearning4j.gym.StepReply(
                currentObservation!!,
                stepResult.reward,
                stepResult.done,
                createInfoObject(mapOf(
                    "originalAction" to originalAction,
                    "actualAction" to fallbackAction,
                    "legal" to legalActions.size,
                    "illegal" to true
                ))
            )
            
        } catch (e: Exception) {
            logger.error("Failed to execute fallback step: ${e.message}")
            
            // Return safe fallback state
            return org.deeplearning4j.gym.StepReply(
                currentObservation ?: reset(),
                -1.0, // Penalty for error
                true,
                createInfoObject(mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "originalAction" to originalAction,
                    "actualAction" to fallbackAction,
                    "legal" to legalActions.size,
                    "illegal" to true
                ))
            )
        }
    }
    
    /**
     * Select the best fallback action from legal actions.
     */
    private fun selectFallbackAction(legalActions: List<Int>): Int {
        return if (legalActions.isNotEmpty()) {
            // Try to select the best legal move based on some heuristic
            // For now, just select the first legal action
            // In a more sophisticated implementation, we could use a simple heuristic
            legalActions.first()
        } else {
            throw IllegalStateException("No legal actions available for fallback")
        }
    }
    
    /**
     * Check if the environment is in a terminal state.
     * 
     * @return true if game is over, false otherwise
     */
    override fun isDone(): Boolean = isDoneFlag
    
    /**
     * Get the observation space for this MDP.
     * 
     * @return ChessObservationSpace instance
     */
    override fun getObservationSpace(): ChessObservationSpace = observationSpace
    
    /**
     * Get the action space for this MDP.
     * 
     * @return ChessActionSpace instance
     */
    override fun getActionSpace(): ChessActionSpace = actionSpace
    
    /**
     * Close the environment and clean up resources.
     */
    override fun close() {
        logger.debug("Closing chess MDP")
        // Chess environment doesn't require explicit cleanup
    }
    
    /**
     * Create a new instance of this MDP.
     * 
     * @return New ChessMDP instance with fresh chess environment
     */
    override fun newInstance(): ChessMDP {
        logger.debug("Creating new ChessMDP instance")
        
        // Create a new chess environment instance
        // Note: ChessEnvironment doesn't have a copy method, so we create a new instance
        // This will use the same configuration as the original
        return try {
            // Create new environment with same configuration
            ChessMDP(ChessEnvironment())
        } catch (e: Exception) {
            logger.warn("Could not create new chess environment instance: ${e.message}")
            throw IllegalStateException("Failed to create new ChessMDP instance", e)
        }
    }
    
    /**
     * Get current observation without stepping.
     * 
     * @return Current observation or null if not initialized
     */
    fun getCurrentObservation(): ChessObservation? = currentObservation
    
    /**
     * Get the underlying chess environment.
     * 
     * @return The wrapped chess environment
     */
    fun getChessEnvironment(): ChessEnvironment = chessEnvironment
    

    
    /**
     * Create an info object for RL4J StepReply info field.
     * 
     * RL4J StepReply constructor accepts Object for info, so we can pass a Map.
     * This provides useful debugging information without requiring JSONObject.
     * 
     * @param data Map of key-value pairs to include in the info object
     * @return Info object for RL4J compatibility
     */
    private fun createInfoObject(data: Map<String, Any>): Any {
        // Return a HashMap which RL4J should be able to handle
        return HashMap(data)
    }
    
    override fun toString(): String {
        return "ChessMDP(done=$isDoneFlag, hasObservation=${currentObservation != null})"
    }
}

