package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.logging.ChessRLLogger

/**
 * Chess observation space for RL4J integration.
 * 
 * Defines the observation space for chess positions as a 839-dimensional
 * continuous space. This extends RL4J's ObservationSpace interface to provide
 * chess-specific observation space information.
 */
class ChessObservationSpace {
    
    companion object {
        private val logger = ChessRLLogger.forComponent("ChessObservationSpace")
        const val DIMENSIONS = 839
    }
    
    init {
        logger.debug("Created chess observation space with $DIMENSIONS dimensions")
    }
    
    /**
     * Get the name of this observation space.
     * 
     * @return "ChessObservationSpace"
     */
    fun getName(): String = "ChessObservationSpace"
    
    /**
     * Get the shape of observations in this space.
     * 
     * @return Array containing the dimensions [839]
     */
    fun getShape(): IntArray = intArrayOf(DIMENSIONS)
    
    /**
     * Get the low bounds for observations in this space.
     * 
     * Chess features are typically normalized to [0,1] or [-1,1] range,
     * but we use conservative bounds to handle various encoding schemes.
     * 
     * @return Array of minimum values for each dimension
     */
    fun getLow(): DoubleArray = DoubleArray(DIMENSIONS) { -10.0 }
    
    /**
     * Get the high bounds for observations in this space.
     * 
     * @return Array of maximum values for each dimension
     */
    fun getHigh(): DoubleArray = DoubleArray(DIMENSIONS) { 10.0 }
    
    /**
     * Check if an observation is valid for this space.
     * 
     * @param observation The observation to validate
     * @return true if observation is valid, false otherwise
     */
    fun contains(observation: ChessObservation): Boolean {
        if (!observation.isValid()) {
            logger.warn("Observation failed internal validation")
            return false
        }
        
        val data = observation.getData()
        if (data.size != DIMENSIONS) {
            logger.warn("Observation has wrong dimensions: ${data.size}, expected $DIMENSIONS")
            return false
        }
        
        val low = getLow()
        val high = getHigh()
        
        for (i in data.indices) {
            if (data[i] < low[i] || data[i] > high[i]) {
                logger.warn("Observation value ${data[i]} at index $i is outside bounds [${low[i]}, ${high[i]}]")
                return false
            }
        }
        
        return true
    }
    
    /**
     * Sample a random observation from this space.
     * 
     * Note: This generates a random observation for testing purposes only.
     * In actual chess training, observations come from the chess environment.
     * 
     * @return Random chess observation
     */
    fun sample(): ChessObservation {
        val random = kotlin.random.Random.Default
        val data = DoubleArray(DIMENSIONS) { 
            random.nextDouble(getLow()[it], getHigh()[it])
        }
        return ChessObservation(data)
    }
    
    /**
     * Create an observation space compatible with RL4J's ObservationSpace interface.
     * 
     * This method uses reflection to create an RL4J-compatible observation space
     * without compile-time dependency on RL4J classes.
     * 
     * @return RL4J ObservationSpace instance
     * @throws IllegalStateException if RL4J classes are not available
     */
    fun toRL4JObservationSpace(): Any {
        try {
            // Try to create ArrayObservationSpace using reflection
            val arrayObservationSpaceClass = Class.forName("org.deeplearning4j.rl4j.space.ArrayObservationSpace")
            val constructor = arrayObservationSpaceClass.getConstructor(IntArray::class.java)
            return constructor.newInstance(getShape())
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("RL4J classes not found. Ensure RL4J dependencies are available.", e)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create RL4J observation space", e)
        }
    }
    
    override fun toString(): String {
        return "ChessObservationSpace(dimensions=$DIMENSIONS, bounds=[${getLow()[0]}, ${getHigh()[0]}])"
    }
}