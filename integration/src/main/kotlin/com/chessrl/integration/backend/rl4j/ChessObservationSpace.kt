package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.logging.ChessRLLogger

/**
 * Chess observation space for RL4J integration.
 * 
 * Defines the observation space for chess positions as a 839-dimensional
 * continuous space. This implements RL4J's ObservationSpace interface directly
 * to provide proper type compatibility with RL4J trainers.
 */
class ChessObservationSpace : org.deeplearning4j.rl4j.space.ObservationSpace<ChessObservation> {
    
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
    override fun getName(): String = "ChessObservationSpace"
    
    /**
     * Get the shape of observations in this space.
     * 
     * @return Array containing the dimensions [839]
     */
    override fun getShape(): IntArray = intArrayOf(DIMENSIONS)
    
    /**
     * Get the low bounds for observations in this space.
     * 
     * Chess features are typically normalized to [0,1] or [-1,1] range,
     * but we use conservative bounds to handle various encoding schemes.
     * 
     * @return INDArray of minimum values for each dimension
     */
    override fun getLow(): org.nd4j.linalg.api.ndarray.INDArray {
        return org.nd4j.linalg.factory.Nd4j.create(DoubleArray(DIMENSIONS) { -10.0 })
    }
    
    /**
     * Get the high bounds for observations in this space.
     * 
     * @return INDArray of maximum values for each dimension
     */
    override fun getHigh(): org.nd4j.linalg.api.ndarray.INDArray {
        return org.nd4j.linalg.factory.Nd4j.create(DoubleArray(DIMENSIONS) { 10.0 })
    }
    
    /**
     * Get the low bounds as a DoubleArray for convenience.
     * 
     * @return Array of minimum values for each dimension
     */
    fun getLowArray(): DoubleArray = DoubleArray(DIMENSIONS) { -10.0 }
    
    /**
     * Get the high bounds as a DoubleArray for convenience.
     * 
     * @return Array of maximum values for each dimension
     */
    fun getHighArray(): DoubleArray = DoubleArray(DIMENSIONS) { 10.0 }
    
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
        
        val data = observation.getDataArray()
        if (data.size != DIMENSIONS) {
            logger.warn("Observation has wrong dimensions: ${data.size}, expected $DIMENSIONS")
            return false
        }
        
        val low = getLowArray()
        val high = getHighArray()
        
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
        val low = getLowArray()
        val high = getHighArray()
        val data = DoubleArray(DIMENSIONS) { 
            random.nextDouble(low[it], high[it])
        }
        return ChessObservation(data)
    }
    

    

    
    override fun toString(): String {
        val low = getLowArray()
        val high = getHighArray()
        return "ChessObservationSpace(dimensions=$DIMENSIONS, bounds=[${low[0]}, ${high[0]}])"
    }
}