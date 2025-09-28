package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.logging.ChessRLLogger

/**
 * Chess action space for RL4J integration.
 * 
 * Defines the action space for chess moves as a discrete space with 4096 possible actions.
 * This extends RL4J's DiscreteSpace class directly to provide proper type compatibility
 * with RL4J trainers.
 */
class ChessActionSpace : org.deeplearning4j.rl4j.space.DiscreteSpace(ACTION_COUNT) {
    
    companion object {
        private val logger = ChessRLLogger.forComponent("ChessActionSpace")
        const val ACTION_COUNT = 4096
    }
    
    init {
        logger.debug("Created chess action space with $ACTION_COUNT possible actions")
    }
    
    /**
     * Get the number of possible actions in this space.
     * 
     * @return 4096 (total number of possible chess moves in our encoding)
     */
    override fun getSize(): Int = ACTION_COUNT
    
    /**
     * Get the name of this action space.
     * 
     * @return "ChessActionSpace"
     */
    fun getName(): String = "ChessActionSpace"
    
    /**
     * Check if an action is valid for this space.
     * 
     * @param action The action to validate
     * @return true if action is in range [0, 4095], false otherwise
     */
    fun contains(action: Int): Boolean {
        val isValid = action in 0 until ACTION_COUNT
        if (!isValid) {
            logger.warn("Invalid action $action, must be in range [0, ${ACTION_COUNT - 1}]")
        }
        return isValid
    }
    

    
    /**
     * Get all possible actions in this space.
     * 
     * @return IntArray containing all actions [0, 1, 2, ..., 4095]
     */
    fun getAllActions(): IntArray {
        return IntArray(ACTION_COUNT) { it }
    }
    
    /**
     * Convert an action to a string representation.
     * 
     * @param action The action to convert
     * @return String representation of the action
     */
    fun actionToString(action: Int): String {
        return if (contains(action)) {
            "Action($action)"
        } else {
            "InvalidAction($action)"
        }
    }
    
    /**
     * Sample a random action from this space.
     * 
     * @return Random action in range [0, 4095]
     */
    fun sample(): Int {
        return kotlin.random.Random.Default.nextInt(ACTION_COUNT)
    }
    

    
    /**
     * Get the minimum action value.
     * 
     * @return 0
     */
    fun getMinAction(): Int = 0
    
    /**
     * Get the maximum action value.
     * 
     * @return 4095
     */
    fun getMaxAction(): Int = ACTION_COUNT - 1
    
    /**
     * Check if this action space is discrete.
     * 
     * @return Always true for chess actions
     */
    fun isDiscrete(): Boolean = true
    
    override fun toString(): String {
        return "ChessActionSpace(size=$ACTION_COUNT, range=[0, ${ACTION_COUNT - 1}])"
    }
}