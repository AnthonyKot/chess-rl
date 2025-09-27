package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.ChessEnvironment
import com.chessrl.integration.logging.ChessRLLogger

/**
 * Chess MDP (Markov Decision Process) wrapper for RL4J integration.
 * 
 * This class adapts our existing ChessEnvironment to RL4J's MDP interface,
 * enabling RL4J algorithms to train on chess while maintaining our existing
 * game logic, state encoding, and reward computation.
 */
class ChessMDP(private val chessEnvironment: ChessEnvironment) {
    
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
    fun reset(): ChessObservation {
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
     * @return StepReply containing next observation, reward, done flag, and info
     */
    fun step(action: Int): StepReply {
        logger.debug("Executing action: $action")
        
        if (isDoneFlag) {
            logger.warn("Attempted to step in terminal state")
            return StepReply(
                observation = currentObservation ?: reset(),
                reward = 0.0,
                isDone = true,
                info = mapOf("error" to "Environment is already terminal")
            )
        }
        
        try {
            // Validate action
            if (!actionSpace.contains(action)) {
                logger.warn("Invalid action $action, falling back to legal move")
                return handleIllegalAction(action)
            }
            
            // Get legal actions before attempting the move
            val legalActions = chessEnvironment.getValidActions(currentObservation?.getData() ?: DoubleArray(839))
            
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
            
            return StepReply(
                observation = currentObservation!!,
                reward = stepResult.reward,
                isDone = stepResult.done,
                info = mapOf(
                    "originalAction" to action,
                    "actualAction" to actualAction,
                    "legalActions" to legalActions.size,
                    "wasIllegal" to (action != actualAction)
                )
            )
            
        } catch (e: Exception) {
            logger.error("Failed to execute step: ${e.message}")
            
            // Return safe fallback state
            return StepReply(
                observation = currentObservation ?: reset(),
                reward = -1.0, // Penalty for error
                isDone = true,
                info = mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }
    
    /**
     * Handle illegal action by selecting a fallback legal move.
     */
    private fun handleIllegalAction(action: Int): StepReply {
        val legalActions = chessEnvironment.getValidActions(currentObservation?.getData() ?: DoubleArray(839))
        
        if (legalActions.isEmpty()) {
            // Game is over
            isDoneFlag = true
            return StepReply(
                observation = currentObservation ?: reset(),
                reward = 0.0,
                isDone = true,
                info = mapOf(
                    "error" to "No legal actions available",
                    "originalAction" to action,
                    "actualAction" to action,
                    "legalActions" to 0,
                    "wasIllegal" to true
                )
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
    private fun stepWithFallback(originalAction: Int, fallbackAction: Int, legalActions: List<Int>): StepReply {
        try {
            // Execute the fallback action
            val stepResult = chessEnvironment.step(fallbackAction)
            
            // Update state
            currentObservation = ChessObservation(stepResult.nextState)
            isDoneFlag = stepResult.done
            
            logger.debug("Fallback step completed: reward=${stepResult.reward}, done=${stepResult.done}")
            
            return StepReply(
                observation = currentObservation!!,
                reward = stepResult.reward,
                isDone = stepResult.done,
                info = mapOf(
                    "originalAction" to originalAction,
                    "actualAction" to fallbackAction,
                    "legalActions" to legalActions.size,
                    "wasIllegal" to true
                )
            )
            
        } catch (e: Exception) {
            logger.error("Failed to execute fallback step: ${e.message}")
            
            // Return safe fallback state
            return StepReply(
                observation = currentObservation ?: reset(),
                reward = -1.0, // Penalty for error
                isDone = true,
                info = mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "originalAction" to originalAction,
                    "actualAction" to fallbackAction,
                    "legalActions" to legalActions.size,
                    "wasIllegal" to true
                )
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
    fun isDone(): Boolean = isDoneFlag
    
    /**
     * Get the observation space for this MDP.
     * 
     * @return ChessObservationSpace instance
     */
    fun getObservationSpace(): ChessObservationSpace = observationSpace
    
    /**
     * Get the action space for this MDP.
     * 
     * @return ChessActionSpace instance
     */
    fun getActionSpace(): ChessActionSpace = actionSpace
    
    /**
     * Close the environment and clean up resources.
     */
    fun close() {
        logger.debug("Closing chess MDP")
        // Chess environment doesn't require explicit cleanup
    }
    
    /**
     * Create a new instance of this MDP.
     * 
     * @return New ChessMDP instance with fresh chess environment
     */
    fun newInstance(): ChessMDP {
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
     * Create an RL4J-compatible MDP instance using reflection.
     * 
     * This method creates an actual RL4J MDP implementation that can be used
     * with RL4J algorithms, while avoiding compile-time dependencies.
     * 
     * @return RL4J MDP instance
     * @throws IllegalStateException if RL4J classes are not available
     */
    fun toRL4JMDP(): Any {
        try {
            // This would create an actual RL4J MDP implementation
            // For now, we return this instance as it implements the required interface
            logger.info("Creating RL4J-compatible MDP wrapper")
            return RL4JMDPWrapper(this)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create RL4J MDP wrapper", e)
        }
    }
    
    override fun toString(): String {
        return "ChessMDP(done=$isDoneFlag, hasObservation=${currentObservation != null})"
    }
}

/**
 * Step reply containing the result of an MDP step.
 */
data class StepReply(
    val observation: ChessObservation,
    val reward: Double,
    val isDone: Boolean,
    val info: Map<String, Any> = emptyMap()
)

/**
 * Wrapper class that implements RL4J's MDP interface using reflection.
 * 
 * This allows us to create RL4J-compatible MDP instances without compile-time
 * dependencies on RL4J classes.
 */
private class RL4JMDPWrapper(private val chessMDP: ChessMDP) {
    
    companion object {
        private val logger = ChessRLLogger.forComponent("RL4JMDPWrapper")
    }
    
    init {
        logger.debug("Created RL4J MDP wrapper")
    }
    
    // These methods would be called by RL4J via reflection
    fun reset(): Any {
        val observation = chessMDP.reset()
        return observation.getINDArray()
    }
    
    fun step(action: Int): Any {
        val stepReply = chessMDP.step(action)
        
        // Create RL4J StepReply using reflection
        try {
            val stepReplyClass = Class.forName("org.deeplearning4j.rl4j.mdp.MDP\$StepReply")
            val constructor = stepReplyClass.constructors.first()
            
            return constructor.newInstance(
                stepReply.observation.getINDArray(),
                stepReply.reward,
                stepReply.isDone,
                stepReply.info
            )
        } catch (e: Exception) {
            logger.error("Failed to create RL4J StepReply: ${e.message}")
            throw IllegalStateException("Failed to create RL4J StepReply", e)
        }
    }
    
    fun isDone(): Boolean = chessMDP.isDone()
    
    fun getObservationSpace(): Any = chessMDP.getObservationSpace().toRL4JObservationSpace()
    
    fun getActionSpace(): Any = chessMDP.getActionSpace().toRL4JDiscreteSpace()
    
    fun close() = chessMDP.close()
    
    fun newInstance(): Any = chessMDP.newInstance().toRL4JMDP()
}