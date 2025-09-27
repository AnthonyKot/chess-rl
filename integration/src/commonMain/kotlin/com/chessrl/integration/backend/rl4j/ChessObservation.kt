package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.logging.ChessRLLogger

/**
 * Chess observation wrapper for RL4J integration.
 * 
 * Wraps the 839-dimensional chess state vector in RL4J's Observation interface.
 * This ensures compatibility with RL4J's MDP framework while maintaining
 * our existing state encoding.
 */
class ChessObservation(private val stateVector: DoubleArray) {
    
    companion object {
        private val logger = ChessRLLogger.forComponent("ChessObservation")
        const val EXPECTED_DIMENSIONS = 839
    }
    
    init {
        require(stateVector.size == EXPECTED_DIMENSIONS) {
            "Chess observation must have exactly $EXPECTED_DIMENSIONS features, got: ${stateVector.size}"
        }
        logger.debug("Created chess observation with ${stateVector.size} features")
    }
    
    /**
     * Get the raw state vector data.
     * 
     * @return 839-dimensional double array representing the chess position
     */
    fun getData(): DoubleArray = stateVector.copyOf()
    
    /**
     * Get the state vector as an INDArray for RL4J compatibility.
     * 
     * This method uses reflection to create an INDArray without compile-time
     * dependency on ND4J, allowing the code to work even when RL4J is not available.
     * 
     * @return INDArray representation of the state vector
     * @throws IllegalStateException if ND4J classes are not available
     */
    fun getINDArray(): Any {
        try {
            val nd4jClass = Class.forName("org.nd4j.linalg.factory.Nd4j")
            val createMethod = nd4jClass.getMethod("create", DoubleArray::class.java)
            return createMethod.invoke(null, stateVector)
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("ND4J classes not found. Ensure RL4J dependencies are available.", e)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create INDArray from state vector", e)
        }
    }
    
    /**
     * Create a duplicate of this observation.
     * 
     * @return New ChessObservation with copied state vector
     */
    fun duplicate(): ChessObservation {
        return ChessObservation(stateVector.copyOf())
    }
    
    /**
     * Get the number of dimensions in this observation.
     * 
     * @return Always returns 839 for chess observations
     */
    fun getDimensions(): Int = EXPECTED_DIMENSIONS
    
    /**
     * Check if this observation is valid (has correct dimensions and no NaN values).
     * 
     * @return true if observation is valid, false otherwise
     */
    fun isValid(): Boolean {
        if (stateVector.size != EXPECTED_DIMENSIONS) {
            logger.warn("Invalid observation: wrong dimensions ${stateVector.size}, expected $EXPECTED_DIMENSIONS")
            return false
        }
        
        if (stateVector.any { it.isNaN() || it.isInfinite() }) {
            logger.warn("Invalid observation: contains NaN or infinite values")
            return false
        }
        
        return true
    }
    
    override fun toString(): String {
        val preview = stateVector.take(5).joinToString(", ", "[", ", ...]")
        return "ChessObservation(dimensions=$EXPECTED_DIMENSIONS, preview=$preview)"
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChessObservation) return false
        return stateVector.contentEquals(other.stateVector)
    }
    
    override fun hashCode(): Int {
        return stateVector.contentHashCode()
    }
}