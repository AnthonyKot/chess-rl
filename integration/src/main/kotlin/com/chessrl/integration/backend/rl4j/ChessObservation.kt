package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.logging.ChessRLLogger
import org.deeplearning4j.rl4j.observation.Observation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

/**
 * Chess observation wrapper for RL4J integration.
 *
 * Wraps the 839-dimensional chess state vector in RL4J's Observation class so the
 * native RL4J policies can consume our encoding without any reflection-based plumbing.
 */
class ChessObservation(stateVector: DoubleArray) : Observation(createIndArray(stateVector)) {

    private val stateVector: DoubleArray = stateVector.copyOf()
    private var legalActionMask: BooleanArray? = null

    companion object {
        private val logger = ChessRLLogger.forComponent("ChessObservation")
        const val EXPECTED_DIMENSIONS = 839

        private fun createIndArray(source: DoubleArray): INDArray {
            RL4JAvailability.validateAvailability()
            require(source.size == EXPECTED_DIMENSIONS) {
                "Chess observation must have exactly $EXPECTED_DIMENSIONS features, got: ${source.size}"
            }
            val array = Nd4j.create(source.copyOf())
            return array.reshape(1L, EXPECTED_DIMENSIONS.toLong())
        }
    }

    init {
        logger.debug("Created chess observation with ${stateVector.size} features")
    }

    /**
     * Get the raw state vector as DoubleArray.
     */
    fun getDataArray(): DoubleArray = stateVector.copyOf()

    /**
     * Provide access to the underlying INDArray used by RL4J.
     */
    fun getINDArray(): INDArray = data

    fun withLegalActions(legalActions: Collection<Int>): ChessObservation {
        val mask = BooleanArray(ChessActionSpace.ACTION_COUNT)
        legalActions.forEach { action ->
            if (action in 0 until ChessActionSpace.ACTION_COUNT) {
                mask[action] = true
            }
        }
        legalActionMask = mask
        return this
    }

    fun getLegalActionMask(): BooleanArray? = legalActionMask?.copyOf()

    fun duplicate(): ChessObservation = ChessObservation(stateVector.copyOf()).also { clone ->
        clone.legalActionMask = legalActionMask?.copyOf()
    }

    fun getDimensions(): Int = EXPECTED_DIMENSIONS

    fun isValid(): Boolean {
        if (stateVector.any { it.isNaN() || it.isInfinite() }) {
            logger.warn("Invalid observation: contains NaN or infinite values")
            return false
        }
        return true
    }

    override fun toArray(): DoubleArray = stateVector.copyOf()

    override fun isSkipped(): Boolean = false

    override fun dup(): ChessObservation = ChessObservation(stateVector.copyOf()).also { clone ->
        clone.legalActionMask = legalActionMask?.copyOf()
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

    override fun hashCode(): Int = stateVector.contentHashCode()
}
