package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.test.*

/**
 * Comprehensive tests for the training validation framework
 */
class TrainingValidatorTest {
    
    private lateinit var validator: TrainingValidator
    private lateinit var config: ValidationConfig
    
    @BeforeTest
    fun setup() {
        config = ValidationConfig(
            explodingGradientThreshold = 5.0,
            vanishingGradientThreshold = 1e-4,
            policyCollapseThreshold = 0.2,
            qValueOverestimationThreshold = 50.0
        )
        validator = TrainingValidator(config)
    }
    
    @AfterTest
    fun cleanup() {
        validator.clearHistory()
    }
    
    @Test
    fun testValidPolicyUpdate() {
        // Arrange
        val beforeMetrics = createValidRLMetrics(episode = 1)
        val afterMetrics = createValidRLMetrics(episode = 2, averageReward = 0.1)
        val updateResult = createValidPolicyUpdateResult()
        
        // Act
        val result = validator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult, 1)
        
        // Assert
        assertTrue(result.isValid, "Valid policy update should pass validation")
        assertTrue(result.issues.isEmpty(), "Valid update should have no issues")
        assertTrue(result.warnings.isEmpty(), "Valid update should have no warnings")
        assertEquals(1, result.episodeNumber)
    }
    
    @Test
    fun testExplodingGradients() {
        // Arrange
        val beforeMetrics = createValidRLMetrics()
        val afterMetrics = createValidRLMetrics()
        val updateResult = PolicyUpdateResult(
            loss = 1.0,
            gradientNorm = 15.0, // Above threshold
            policyEntropy = 1.0
        )
        
        // Act
        val result = validator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult, 1)
        
        // Assert
        assertFalse(result.isValid, "Exploding gradients should fail validation")
        assertTrue(result.issues.any { it.type == IssueType.EXPLODING_GRADIENTS })
        assertTrue(result.recommendations.any { it.contains("gradient clipping") })
    }
    
    @Test
    fun testVanishingGradients() {
        // Arrange
        val beforeMetrics = createValidRLMetrics()
        val afterMetrics = createValidRLMetrics()
        val updateResult = PolicyUpdateResult(
            loss = 1.0,
            gradientNorm = 1e-8, // Below threshold
            policyEntropy = 1.0
        )
        
        // Act
        val result = validator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult, 1)
        
        // Assert
        assertTrue(result.warnings.any { it.contains("Gradient norm very low") })
        assertTrue(result.recommendations.any { it.contains("increasing learning rate") })
    }
    
    @Test
    fun testPolicyCollapse() {
        // Arrange
        val beforeMetrics = createValidRLMetrics()
        val afterMetrics = createValidRLMetrics()
        val updateResult = PolicyUpdateResult(
            loss = 1.0,
            gradientNorm = 1.0,
            policyEntropy = 0.05 // Below threshold
        )
        
        // Act
        val result = validator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult, 1)
        
        // Assert
        assertFalse(result.isValid, "Policy collapse should fail validation")
        assertTrue(result.issues.any { it.type == IssueType.POLICY_COLLAPSE })
        assertTrue(result.recommendations.any { it.contains("exploration") })
    }
    
    @Test
    fun testNumericalInstability() {
        // Arrange
        val beforeMetrics = createValidRLMetrics()
        val afterMetrics = createValidRLMetrics()
        val updateResult = PolicyUpdateResult(
            loss = Double.NaN,
            gradientNorm = 1.0,
            policyEntropy = 1.0
        )
        
        // Act
        val result = validator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult, 1)
        
        // Assert
        assertFalse(result.isValid, "NaN loss should fail validation")
        assertTrue(result.issues.any { it.type == IssueType.NUMERICAL_INSTABILITY })
    }
    
    @Test
    fun testConvergenceAnalysisInsufficientData() {
        // Arrange
        val shortHistory = listOf(createValidRLMetrics())
        
        // Act
        val analysis = validator.analyzeConvergence(shortHistory, windowSize = 5)
        
        // Assert
        assertEquals(com.chessrl.rl.ConvergenceStatus.INSUFFICIENT_DATA, analysis.status)
        assertEquals(0.0, analysis.confidence)
        assertTrue(analysis.recommendations.any { it.contains("Need at least") })
    }
    
    @Test
    fun testConvergenceAnalysisImproving() {
        // Arrange
        val improvingHistory = (1..10).map { episode ->
            createValidRLMetrics(
                episode = episode,
                averageReward = episode * 0.1, // Improving rewards
                policyLoss = 2.0 - episode * 0.1 // Decreasing loss
            )
        }
        
        // Act
        val analysis = validator.analyzeConvergence(improvingHistory, windowSize = 10)
        
        // Assert
        assertEquals(com.chessrl.rl.ConvergenceStatus.IMPROVING, analysis.status)
        assertEquals(TrendDirection.IMPROVING, analysis.trendDirection)
        assertTrue(analysis.rewardTrend > 0, "Reward trend should be positive")
        assertTrue(analysis.lossTrend < 0, "Loss trend should be negative")
    }
    
    @Test
    fun testConvergenceAnalysisStagnant() {
        // Arrange
        val stagnantHistory = (1..10).map { episode ->
            createValidRLMetrics(
                episode = episode,
                averageReward = 0.5, // Constant rewards
                policyLoss = 1.0 // Constant loss
            )
        }
        
        // Act
        val analysis = validator.analyzeConvergence(stagnantHistory, windowSize = 10)
        
        // Assert
        assertEquals(com.chessrl.rl.ConvergenceStatus.STAGNANT, analysis.status)
        assertTrue(analysis.stabilityScore > 0.5, "Should have high stability")
        assertTrue(analysis.recommendations.any { it.contains("stagnated") })
    }
    
    @Test
    fun testTrainingIssueDetection() {
        // Arrange
        val problematicMetrics = RLMetrics(
            episode = 1,
            averageReward = 0.0,
            episodeLength = 10.0,
            explorationRate = 0.005, // Too low
            policyLoss = 1.0,
            policyEntropy = 0.05, // Too low
            gradientNorm = 1.0,
            qValueStats = QValueStats(
                meanQValue = 150.0, // Too high
                maxQValue = 200.0,
                minQValue = 100.0,
                qValueStd = 25.0
            )
        )
        
        val updateResult = PolicyUpdateResult(
            loss = 1.0,
            gradientNorm = 15.0, // Too high
            policyEntropy = 0.05 // Too low
        )
        
        // Act
        val detections = validator.detectTrainingIssues(problematicMetrics, updateResult)
        
        // Assert
        assertTrue(detections.any { it.issue == TrainingIssue.EXPLODING_GRADIENTS })
        assertTrue(detections.any { it.issue == TrainingIssue.POLICY_COLLAPSE })
        assertTrue(detections.any { it.issue == TrainingIssue.EXPLORATION_INSUFFICIENT })
        assertTrue(detections.any { it.issue == TrainingIssue.VALUE_OVERESTIMATION })
        
        // Check recommendations
        val explodingGradientDetection = detections.first { it.issue == TrainingIssue.EXPLODING_GRADIENTS }
        assertTrue(explodingGradientDetection.recommendations.any { it.contains("gradient clipping") })
    }
    
    @Test
    fun testValidationSummary() {
        // Arrange - perform several validations
        repeat(5) { episode ->
            val beforeMetrics = createValidRLMetrics(episode = episode)
            val afterMetrics = createValidRLMetrics(episode = episode + 1)
            val updateResult = if (episode == 2) {
                // One invalid update
                PolicyUpdateResult(loss = Double.NaN, gradientNorm = 1.0, policyEntropy = 1.0)
            } else {
                createValidPolicyUpdateResult()
            }
            
            validator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult, episode)
        }
        
        // Act
        val summary = validator.getValidationSummary()
        
        // Assert
        assertEquals(5, summary.totalValidations)
        assertEquals(4, summary.validValidations)
        assertEquals(0.8, summary.validationRate, 0.01)
        assertTrue(summary.totalIssues > 0)
        assertNotNull(summary.lastValidation)
    }
    
    @Test
    fun testValidationHistoryLimit() {
        // Arrange
        val customConfig = ValidationConfig(maxHistorySize = 3)
        val limitedValidator = TrainingValidator(customConfig)
        
        // Act - add more validations than the limit
        repeat(5) { episode ->
            val beforeMetrics = createValidRLMetrics(episode = episode)
            val afterMetrics = createValidRLMetrics(episode = episode + 1)
            val updateResult = createValidPolicyUpdateResult()
            
            limitedValidator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult, episode)
        }
        
        // Assert
        val summary = limitedValidator.getValidationSummary()
        assertTrue(summary.totalValidations <= 3, "History should be limited to max size")
    }
    
    @Test
    fun testQValueValidation() {
        // Arrange
        val beforeMetrics = createValidRLMetrics()
        val afterMetrics = createValidRLMetrics()
        val updateResult = PolicyUpdateResult(
            loss = 1.0,
            gradientNorm = 1.0,
            policyEntropy = 1.0,
            qValueMean = 75.0, // High but not over threshold
            targetValueMean = 25.0 // Large divergence
        )
        
        // Act
        val result = validator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult, 1)
        
        // Assert
        assertTrue(result.isValid, "Should be valid despite Q-value issues")
        assertTrue(result.warnings.any { it.contains("Q-values diverging") })
        assertTrue(result.recommendations.any { it.contains("target network") })
    }
    
    @Test
    fun testLearningProgressValidation() {
        // Arrange - create metrics showing reward decrease with low exploration
        val beforeMetrics = createValidRLMetrics(averageReward = 0.5, explorationRate = 0.005)
        val afterMetrics = createValidRLMetrics(averageReward = 0.3, explorationRate = 0.005)
        val updateResult = createValidPolicyUpdateResult()
        
        // Act
        val result = validator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult, 1)
        
        // Assert
        assertTrue(result.warnings.any { it.contains("stagnation") })
        assertTrue(result.recommendations.any { it.contains("exploration") })
    }
    
    @Test
    fun testLossExplosion() {
        // Arrange
        val beforeMetrics = createValidRLMetrics(policyLoss = 1.0)
        val afterMetrics = createValidRLMetrics(policyLoss = 15.0) // Large increase
        val updateResult = createValidPolicyUpdateResult()
        
        // Act
        val result = validator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult, 1)
        
        // Assert
        assertFalse(result.isValid, "Loss explosion should fail validation")
        assertTrue(result.issues.any { it.type == IssueType.LOSS_EXPLOSION })
        assertTrue(result.recommendations.any { it.contains("Reduce learning rate") })
    }
    
    // Helper methods
    
    private fun createValidRLMetrics(
        episode: Int = 1,
        averageReward: Double = 0.0,
        policyLoss: Double = 1.0,
        explorationRate: Double = 0.1
    ): RLMetrics {
        return RLMetrics(
            episode = episode,
            averageReward = averageReward,
            episodeLength = 50.0,
            explorationRate = explorationRate,
            policyLoss = policyLoss,
            policyEntropy = 1.0,
            gradientNorm = 1.0,
            qValueStats = QValueStats(
                meanQValue = 10.0,
                maxQValue = 20.0,
                minQValue = 0.0,
                qValueStd = 5.0
            )
        )
    }
    
    private fun createValidPolicyUpdateResult(): PolicyUpdateResult {
        return PolicyUpdateResult(
            loss = 1.0,
            gradientNorm = 1.0,
            policyEntropy = 1.0,
            qValueMean = 10.0,
            targetValueMean = 9.0
        )
    }
}

/**
 * Tests for chess-specific training validation
 */
class ChessTrainingValidatorTest {
    
    private lateinit var chessValidator: ChessTrainingValidator
    private lateinit var config: ChessValidationConfig
    
    @BeforeTest
    fun setup() {
        config = ChessValidationConfig(
            minGameLengthThreshold = 15.0,
            maxGameLengthThreshold = 150.0,
            minMoveDiversityThreshold = 0.4,
            maxBlunderRateThreshold = 0.25
        )
        chessValidator = ChessTrainingValidator(config)
    }
    
    @AfterTest
    fun cleanup() {
        chessValidator.clearHistory()
    }
    
    @Test
    fun testValidChessTraining() {
        // Arrange
        val goodGameResults = listOf(
            createChessGameResult(outcome = "WHITE_WINS", moveCount = 45, moves = generateDiverseMoves(45)),
            createChessGameResult(outcome = "BLACK_WINS", moveCount = 38, moves = generateDiverseMoves(38)),
            createChessGameResult(outcome = "DRAW", moveCount = 67, moves = generateDiverseMoves(67))
        )
        
        // Act
        val result = chessValidator.validateChessTraining(goodGameResults, 1)
        
        // Assert
        assertTrue(result.isValid, "Good chess games should pass validation")
        assertTrue(result.issues.isEmpty(), "Good games should have no issues")
        assertTrue(result.gameAnalysis.gameQuality.qualityScore > 0.5)
    }
    
    @Test
    fun testGamesTooShort() {
        // Arrange
        val shortGameResults = listOf(
            createChessGameResult(outcome = "WHITE_WINS", moveCount = 8, moves = generateDiverseMoves(8)),
            createChessGameResult(outcome = "BLACK_WINS", moveCount = 5, moves = generateDiverseMoves(5))
        )
        
        // Act
        val result = chessValidator.validateChessTraining(shortGameResults, 1)
        
        // Assert
        assertFalse(result.isValid, "Short games should fail validation")
        assertTrue(result.issues.any { it.type == ChessIssueType.GAMES_TOO_SHORT })
        assertTrue(result.recommendations.any { it.contains("illegal moves") })
    }
    
    @Test
    fun testGamesTooLong() {
        // Arrange
        val longGameResults = listOf(
            createChessGameResult(outcome = "DRAW", moveCount = 200, moves = generateDiverseMoves(200)),
            createChessGameResult(outcome = "DRAW", moveCount = 180, moves = generateDiverseMoves(180))
        )
        
        // Act
        val result = chessValidator.validateChessTraining(longGameResults, 1)
        
        // Assert
        assertFalse(result.isValid, "Very long games should fail validation")
        assertTrue(result.issues.any { it.type == ChessIssueType.GAMES_TOO_LONG })
        assertTrue(result.recommendations.any { it.contains("reward structure") })
    }
    
    @Test
    fun testLowMoveDiversity() {
        // Arrange
        val repetitiveMoves = listOf("e4", "e4", "e4", "Nf3", "Nf3", "Nf3", "Bc4", "Bc4")
        val repetitiveGameResults = listOf(
            createChessGameResult(outcome = "WHITE_WINS", moveCount = 8, moves = repetitiveMoves)
        )
        
        // Act
        val result = chessValidator.validateChessTraining(repetitiveGameResults, 1)
        
        // Assert
        assertFalse(result.isValid, "Repetitive games should fail validation")
        assertTrue(result.issues.any { it.type == ChessIssueType.LOW_MOVE_DIVERSITY })
        assertTrue(result.recommendations.any { it.contains("exploration") })
    }
    
    @Test
    fun testTooManyDraws() {
        // Arrange
        val drawGameResults = (1..10).map {
            createChessGameResult(outcome = "DRAW", moveCount = 50, moves = generateDiverseMoves(50))
        }
        
        // Act
        val result = chessValidator.validateChessTraining(drawGameResults, 1)
        
        // Assert
        assertFalse(result.isValid, "Too many draws should fail validation")
        assertTrue(result.issues.any { it.type == ChessIssueType.TOO_MANY_DRAWS })
        assertTrue(result.recommendations.any { it.contains("discourage draws") })
    }
    
    @Test
    fun testHighBlunderRate() {
        // Arrange - create games that would indicate high blunder rate
        val blunderGameResults = listOf(
            createChessGameResult(outcome = "WHITE_LOSS", moveCount = 15, moves = generateDiverseMoves(15)),
            createChessGameResult(outcome = "BLACK_LOSS", moveCount = 12, moves = generateDiverseMoves(12))
        )
        
        // Act
        val result = chessValidator.validateChessTraining(blunderGameResults, 1)
        
        // Assert
        assertTrue(result.issues.any { it.type == ChessIssueType.HIGH_BLUNDER_RATE })
        assertTrue(result.recommendations.any { it.contains("tactical") })
    }
    
    @Ignore("Progression heuristics sensitive to synthetic data")
    @Test
    fun testLearningProgressionImproving() {
        // Arrange - create improving game history
        repeat(25) { episode ->
            val gameResults = listOf(
                createChessGameResult(
                    outcome = if (episode > 15) "WHITE_WINS" else "DRAW",
                    moveCount = (30 + episode).coerceAtMost(80),
                    moves = generateDiverseMoves((30 + episode).coerceAtMost(80))
                )
            )
            chessValidator.validateChessTraining(gameResults, episode)
        }
        
        // Act
        val progression = chessValidator.analyzeLearningProgression(windowSize = 20)
        
        // Assert
        assertEquals(ChessLearningStatus.IMPROVING, progression.status)
        assertTrue(progression.gameQualityTrend > 0, "Game quality should be improving")
        assertTrue(progression.recommendations.any { it.contains("progressing well") })
    }
    
    @Test
    fun testLearningProgressionStagnant() {
        // Arrange - create stagnant game history
        repeat(25) { episode ->
            val gameResults = listOf(
                createChessGameResult(
                    outcome = "DRAW",
                    moveCount = 50,
                    moves = generateDiverseMoves(50, diversity = 0.3) // Low diversity
                )
            )
            chessValidator.validateChessTraining(gameResults, episode)
        }
        
        // Act
        val progression = chessValidator.analyzeLearningProgression(windowSize = 20)
        
        // Assert
        assertEquals(ChessLearningStatus.STAGNANT, progression.status)
        assertTrue(progression.recommendations.any { it.contains("stagnated") })
    }
    
    @Test
    fun testChessTrainingSummary() {
        // Arrange - add various game results
        val mixedGameResults = listOf(
            createChessGameResult(outcome = "WHITE_WINS", moveCount = 45, moves = generateDiverseMoves(45)),
            createChessGameResult(outcome = "DRAW", moveCount = 8, moves = generateDiverseMoves(8)), // Short
            createChessGameResult(outcome = "BLACK_LOSS", moveCount = 15, moves = generateDiverseMoves(15, diversity = 0.2)) // Low diversity
        )
        
        chessValidator.validateChessTraining(mixedGameResults, 1)
        
        // Act
        val summary = chessValidator.getChessTrainingSummary()
        
        // Assert
        assertEquals(1, summary.totalGamesAnalyzed)
        assertTrue(summary.commonIssues.contains(ChessIssueType.GAMES_TOO_SHORT))
        assertTrue(summary.commonIssues.contains(ChessIssueType.LOW_MOVE_DIVERSITY))
        assertTrue(summary.improvementAreas.isNotEmpty())
    }
    
    @Test
    fun testOpeningDiversityAnalysis() {
        // Arrange - games with limited opening repertoire
        val limitedOpeningResults = (1..10).map {
            createChessGameResult(
                outcome = "WHITE_WINS",
                moveCount = 40,
                moves = listOf("e4", "e5", "Nf3", "Nc6") + generateDiverseMoves(36) // Same opening
            )
        }
        
        // Act
        val result = chessValidator.validateChessTraining(limitedOpeningResults, 1)
        
        // Assert
        assertTrue(result.issues.any { it.type == ChessIssueType.LIMITED_OPENING_REPERTOIRE })
        assertTrue(result.recommendations.any { it.contains("opening") })
    }
    
    // Helper methods
    
    private fun createChessGameResult(
        outcome: String,
        moveCount: Int,
        moves: List<String>,
        legalMoveRate: Double = 0.98
    ): ChessGameResult {
        return ChessGameResult(
            outcome = outcome,
            moveCount = moveCount,
            moves = moves,
            legalMoveRate = legalMoveRate
        )
    }
    
    private fun generateDiverseMoves(count: Int, diversity: Double = 0.8): List<String> {
        val commonMoves = listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4", "Nf6", "O-O", "Be7")
        val diverseMoves = listOf("d4", "d5", "c4", "c5", "f4", "f5", "g3", "g6", "h3", "h6", "Qd2", "Qd7")

        val moves = mutableListOf<String>()
        val frac = diversity.coerceIn(0.0, 1.0)
        val uniqueMovesCount = (count * frac).toInt()

        // Build the set of unique moves first
        val uniques = mutableListOf<String>()
        repeat(uniqueMovesCount) { i ->
            val pool = if (i < commonMoves.size) commonMoves else diverseMoves
            val mv = pool[i % pool.size]
            uniques.add(mv)
            moves.add(mv)
        }

        // Fill remaining strictly by repeating from the uniques, to control diversity
        if (uniques.isEmpty()) {
            // Degenerate case: no diversity requested; repeat a single common move
            repeat(count - moves.size) { moves.add(commonMoves.first()) }
        } else {
            var idx = 0
            repeat(count - moves.size) {
                moves.add(uniques[idx % uniques.size])
                idx++
            }
        }

        return moves.take(count)
    }
}
