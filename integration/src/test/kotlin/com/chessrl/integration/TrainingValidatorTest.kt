package com.chessrl.integration

import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.rl.Experience
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrainingValidatorTest {

    private val config = ChessRLConfig()
    private val validator = TrainingValidator(config)

    @Test
    fun `healthy metrics pass validation`() {
        val metrics = createTrainingMetrics(
            loss = 0.4,
            gradient = 1.2,
            entropy = 0.8,
            reward = 0.15
        )
        val games = listOf(createGameResult(GameOutcome.WHITE_WINS))

        val result = validator.validateTrainingCycle(cycle = 1, trainingMetrics = metrics, gameResults = games)

        assertTrue(result.isValid, "Expected healthy metrics to pass validation")
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `low policy entropy triggers collapse issue`() {
        val metrics = createTrainingMetrics(
            loss = 0.3,
            gradient = 0.9,
            entropy = 0.05,
            reward = 0.1
        )
        val games = listOf(createGameResult(GameOutcome.WHITE_WINS))

        val result = validator.validateTrainingCycle(cycle = 1, trainingMetrics = metrics, gameResults = games)

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.type == IssueType.POLICY_COLLAPSE })
    }

    @Test
    fun `high draw rate raises draw issue`() {
        val metrics = createTrainingMetrics(
            loss = 0.5,
            gradient = 1.0,
            entropy = 0.7,
            reward = 0.0
        )
        val games = List(12) { createGameResult(GameOutcome.DRAW) }

        val result = validator.validateTrainingCycle(cycle = 1, trainingMetrics = metrics, gameResults = games)

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.type == IssueType.TOO_MANY_DRAWS })
    }

    private fun createTrainingMetrics(
        loss: Double,
        gradient: Double,
        entropy: Double,
        reward: Double
    ): TrainingMetrics {
        return TrainingMetrics(
            batchCount = 2,
            averageLoss = loss,
            averageGradientNorm = gradient,
            averagePolicyEntropy = entropy,
            averageReward = reward,
            experienceBufferSize = 256
        )
    }

    private fun createGameResult(
        outcome: GameOutcome,
        length: Int = 40,
        termination: EpisodeTerminationReason = EpisodeTerminationReason.GAME_ENDED
    ): SelfPlayGameResult {
        val experiences = List(length.coerceAtLeast(1)) {
            Experience(
                state = DoubleArray(8) { 0.0 },
                action = 0,
                reward = 0.0,
                nextState = DoubleArray(8) { 0.0 },
                done = false
            )
        }

        val metrics = ChessMetrics(
            gameLength = length,
            totalMaterialValue = 0,
            piecesInCenter = 0,
            developedPieces = 0,
            kingSafetyScore = 0.0,
            moveCount = length,
            captureCount = 0,
            checkCount = 0
        )

        return SelfPlayGameResult(
            gameId = outcome.ordinal,
            gameLength = length,
            gameOutcome = outcome,
            terminationReason = termination,
            gameDuration = 1000L,
            experiences = experiences,
            chessMetrics = metrics,
            finalPosition = "8/8/8/8/8/8/8/8 w - - 0 1"
        )
    }
}
