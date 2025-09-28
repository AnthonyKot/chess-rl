package com.chessrl.integration.backend

import com.chessrl.integration.logging.ChessRLLogger

/**
 * Utility class to check RL4J availability at runtime.
 * 
 * This handles the case where RL4J dependencies may not be available
 * (e.g., when enableRL4J=false in gradle.properties).
 */
object RL4JAvailability {
    private val logger = ChessRLLogger.forComponent("RL4JAvailability")

    private val availability: Boolean by lazy {
        val result = try {
            Class.forName("org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning")
            Class.forName("org.deeplearning4j.rl4j.mdp.MDP")
            Class.forName("org.deeplearning4j.rl4j.space.DiscreteSpace")
            Class.forName("org.deeplearning4j.rl4j.observation.Observation")
            true
        } catch (e: ClassNotFoundException) {
            logger.warn("RL4J classes not found: ${e.message}")
            false
        } catch (e: NoClassDefFoundError) {
            logger.warn("RL4J class definition error: ${e.message}")
            false
        }

        if (result) {
            logger.info("RL4J classes are available")
        }
        result
    }

    /**
     * Check if RL4J classes are available on the classpath.
     */
    fun isAvailable(): Boolean = availability
    
    /**
     * Validate that RL4J is available when trying to use RL4J backend.
     * 
     * @throws IllegalStateException if RL4J is not available
     */
    fun validateAvailability() {
        if (!isAvailable()) {
            throw IllegalStateException(
                "RL4J backend selected but RL4J classes are not available. " +
                "Please set enableRL4J=true in gradle.properties and rebuild the project."
            )
        }
    }
    
    /**
     * Get a descriptive message about RL4J availability status.
     */
    fun getAvailabilityMessage(): String {
        return if (availability) {
            "RL4J backend is available and ready to use"
        } else {
            "RL4J backend is not available. To enable it:\n" +
            "1. Set enableRL4J=true in gradle.properties\n" +
            "2. Rebuild the project with: ./gradlew clean build\n" +
            "3. Run with RL4J backend: --nn rl4j"
        }
    }
}
