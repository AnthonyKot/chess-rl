package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.ChessEnvironment
import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.logging.ChessRLLogger
import java.util.LinkedHashMap

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

    private var episodeIndex: Int = 0
    private var episodeStepCount: Int = 0
    private var episodeIllegalAttempts: Int = 0
    private var episodeFallbackActions: Int = 0
    private var episodeIllegalLogged: Boolean = false
    
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
            reportEpisodeMetrics("reset")
            resetEpisodeCounters()
            episodeIndex += 1

            val stateVector = chessEnvironment.reset()
            isDoneFlag = false

            val legalActions = chessEnvironment.getValidActions(stateVector)
            actionSpace.updateLegalActions(legalActions)
            currentObservation = ChessObservation(stateVector).withLegalActions(legalActions)

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
            val info = enrichInfo(
                mapOf(
                    "error" to "Environment is already terminal",
                    "originalAction" to action,
                    "actualAction" to action,
                    "legal" to 0,
                    "legalCount" to 0,
                    "illegal" to true,
                    "illegalAttempt" to true,
                    "fallbackUsed" to false,
                    "reason" to "terminal-state"
                )
            )
            return org.deeplearning4j.gym.StepReply(
                currentObservation ?: reset(),
                0.0,
                true,
                createInfoObject(info)
            )
        }

        return try {
            if (!actionSpace.contains(action)) {
                logger.warn("Invalid action $action, falling back to legal move")
                return handleIllegalAction(action)
            }

            val currentState = currentObservation?.getDataArray() ?: DoubleArray(ChessObservation.EXPECTED_DIMENSIONS)
            val legalActions = chessEnvironment.getValidActions(currentState)
            actionSpace.updateLegalActions(legalActions)

            if (legalActions.isEmpty()) {
            logIllegalAttempt(action, fallbackUsed = false, legalActions = 0)
            recordIllegalAttempt(fallbackUsed = false)
            isDoneFlag = true
            currentObservation = currentObservation?.withLegalActions(emptyList())
                actionSpace.updateLegalActions(emptyList())
                val info = enrichInfo(
                    mapOf(
                        "originalAction" to action,
                        "actualAction" to action,
                        "legal" to 0,
                        "legalCount" to 0,
                        "illegal" to true,
                        "illegalAttempt" to true,
                        "fallbackUsed" to false,
                        "reason" to "no-legal-actions"
                    )
                )
                reportEpisodeMetrics("no-legal-actions")
                resetEpisodeCounters()
                return org.deeplearning4j.gym.StepReply(
                    currentObservation ?: ChessObservation(currentState),
                    0.0,
                    true,
                    createInfoObject(info)
                )
            }

            val illegalAttempt = action !in legalActions
            val actualAction = if (!illegalAttempt) {
                action
            } else {
                logIllegalAttempt(action, fallbackUsed = true, legalActions = legalActions.size)
                recordIllegalAttempt(fallbackUsed = true)
                selectFallbackAction(legalActions)
            }

            executeAction(action, actualAction, legalActions, illegalAttempt)
        } catch (e: Exception) {
            logger.error("Failed to execute step: ${e.message}")
            val info = enrichInfo(
                mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "originalAction" to action,
                    "actualAction" to action,
                    "legal" to 0,
                    "legalCount" to 0,
                    "illegal" to true,
                    "illegalAttempt" to true,
                    "fallbackUsed" to false,
                    "reason" to "exception"
                )
            )

            org.deeplearning4j.gym.StepReply(
                currentObservation ?: reset(),
                -1.0,
                true,
                createInfoObject(info)
            )
        }
    }
    
    /**
     * Handle illegal action by selecting a fallback legal move.
     */
    private fun handleIllegalAction(action: Int): org.deeplearning4j.gym.StepReply<ChessObservation> {
        val legalActions = chessEnvironment.getValidActions(currentObservation?.getDataArray() ?: DoubleArray(ChessObservation.EXPECTED_DIMENSIONS))

        if (legalActions.isEmpty()) {
            actionSpace.updateLegalActions(emptyList())
            logIllegalAttempt(action, fallbackUsed = false, legalActions = 0)
            recordIllegalAttempt(fallbackUsed = false)
            isDoneFlag = true
            currentObservation = currentObservation?.withLegalActions(emptyList())
            val info = enrichInfo(
                mapOf(
                    "error" to "No legal actions available",
                    "originalAction" to action,
                    "actualAction" to action,
                    "legal" to 0,
                    "legalCount" to 0,
                    "illegal" to true,
                    "illegalAttempt" to true,
                    "fallbackUsed" to false,
                    "reason" to "illegal-no-legal"
                )
            )
            reportEpisodeMetrics("illegal-without-fallback")
            resetEpisodeCounters()
            return org.deeplearning4j.gym.StepReply(
                currentObservation ?: ChessObservation(DoubleArray(ChessObservation.EXPECTED_DIMENSIONS)),
                0.0,
                true,
                createInfoObject(info)
            )
        }

        logIllegalAttempt(action, fallbackUsed = true, legalActions = legalActions.size)
        recordIllegalAttempt(fallbackUsed = true)
        val fallbackAction = selectFallbackAction(legalActions)
        logger.debug("Using fallback action $fallbackAction for illegal action $action")

        return executeAction(action, fallbackAction, legalActions, illegalAttempt = true)
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

    private fun executeAction(
        originalAction: Int,
        actualAction: Int,
        legalActionsBefore: List<Int>,
        illegalAttempt: Boolean
    ): org.deeplearning4j.gym.StepReply<ChessObservation> {
        val stepResult = chessEnvironment.step(actualAction)
        episodeStepCount += 1

        val nextLegalActions = if (stepResult.done) {
            emptyList()
        } else {
            chessEnvironment.getValidActions(stepResult.nextState)
        }

        actionSpace.updateLegalActions(nextLegalActions)
        val nextObservation = ChessObservation(stepResult.nextState).withLegalActions(nextLegalActions)
        currentObservation = nextObservation
        isDoneFlag = stepResult.done

        logger.debug(
            "Step completed: reward=${stepResult.reward}, done=${stepResult.done}, illegal=$illegalAttempt, fallback=${illegalAttempt && legalActionsBefore.isNotEmpty()}"
        )

        val info = enrichInfo(
            mapOf(
                "originalAction" to originalAction,
                "actualAction" to actualAction,
                "legal" to legalActionsBefore.size,
                "legalCount" to legalActionsBefore.size,
                "illegal" to illegalAttempt,
                "illegalAttempt" to illegalAttempt,
                "fallbackUsed" to (illegalAttempt && legalActionsBefore.isNotEmpty()),
                "nextLegalCount" to nextLegalActions.size
            )
        )

        if (stepResult.done) {
            reportEpisodeMetrics("terminal-step")
            resetEpisodeCounters()
        }

        return org.deeplearning4j.gym.StepReply(
            nextObservation,
            stepResult.reward,
            stepResult.done,
            createInfoObject(info)
        )
    }

    private fun recordIllegalAttempt(fallbackUsed: Boolean) {
        episodeIllegalAttempts += 1
        if (fallbackUsed) {
            episodeFallbackActions += 1
        }
    }

    private fun resetEpisodeCounters() {
        episodeStepCount = 0
        episodeIllegalAttempts = 0
        episodeFallbackActions = 0
        episodeIllegalLogged = false
    }

    private fun reportEpisodeMetrics(reason: String) {
        if (episodeStepCount == 0 && episodeIllegalAttempts == 0 && episodeFallbackActions == 0) {
            return
        }
        logger.info(
            "Episode summary [$reason] idx=$episodeIndex steps=$episodeStepCount illegalAttempts=$episodeIllegalAttempts fallbackActions=$episodeFallbackActions"
        )
    }

    private fun enrichInfo(base: Map<String, Any?>): Map<String, Any?> {
        val enriched = LinkedHashMap<String, Any?>(base.size + 5)
        enriched.putAll(base)
        enriched["episodeIndex"] = episodeIndex
        enriched["episodeStep"] = episodeStepCount
        enriched["episodeIllegalAttempts"] = episodeIllegalAttempts
        enriched["episodeFallbacks"] = episodeFallbackActions
        currentObservation?.getLegalActionMask()?.let { mask ->
            enriched["legalMaskCount"] = mask.count { it }
        }
        return enriched
    }

    private fun logIllegalAttempt(action: Int, fallbackUsed: Boolean, legalActions: Int) {
        val message = "Illegal action $action attempted (legalCount=$legalActions, fallbackUsed=$fallbackUsed)"
        if (!episodeIllegalLogged) {
            logger.warn(message)
            episodeIllegalLogged = true
        } else {
            logger.debug(message)
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
    private fun createInfoObject(data: Map<String, Any?>): Any {
        val filtered = data.filterValues { it != null }
        return HashMap(filtered)
    }
    
    override fun toString(): String {
        return "ChessMDP(done=$isDoneFlag, hasObservation=${currentObservation != null})"
    }
}
