package com.chessrl.integration.output

import com.chessrl.integration.*
import kotlin.test.*

class NoiseReductionIntegrationTest {
    
    @Test
    fun `ValidationAggregator should reduce repetitive validation messages`() {
        val aggregator = ValidationAggregator()
        
        // Create the same validation issue multiple times
        val issue = ValidationIssue(
            type = IssueType.EXPLODING_GRADIENTS,
            severity = IssueSeverity.HIGH,
            message = "Gradient norm too high: 15.0",
            value = 15.0
        )
        
        // Add the same issue 5 times
        repeat(5) {
            val result = ValidationResult(
                cycle = it + 1,
                timestamp = System.currentTimeMillis(),
                isValid = false,
                issues = listOf(issue),
                warnings = emptyList(),
                recommendations = emptyList(),
                trainingMetrics = createTrainingMetrics(),
                gameAnalysis = createGameAnalysis(),
                smoothedGradientNorm = null,
                smoothedPolicyEntropy = null,
                smoothedLoss = null
            )
            aggregator.addValidationResult(result)
        }
        
        // Should have only one aggregated message with count of 5
        val messages = aggregator.getAggregatedMessages()
        assertEquals(1, messages.size)
        assertEquals(5, messages[0].count)
        assertEquals("Gradient norm too high: 15.0", messages[0].message)
    }
    
    @Test
    fun `TrainingValidator should integrate with ValidationAggregator`() {
        val validator = TrainingValidator()
        
        // Create training metrics that will trigger validation issues
        val badMetrics = TrainingMetrics(
            batchCount = 10,
            averageLoss = Double.NaN, // This should trigger a validation issue
            averageGradientNorm = 15.0, // This should trigger exploding gradients
            averagePolicyEntropy = 0.05, // This should trigger policy collapse
            averageReward = 0.5,
            experienceBufferSize = 1000
        )
        
        // Run validation multiple times with the same bad metrics
        repeat(3) { cycle ->
            validator.validateTrainingCycle(
                cycle = cycle + 1,
                trainingMetrics = badMetrics,
                gameResults = emptyList()
            )
        }
        
        // Get aggregated messages - should have consolidated the repeated issues
        val aggregatedMessages = validator.getAggregatedValidationMessages()
        
        // Should have messages but they should be aggregated with counts
        assertTrue(aggregatedMessages.isNotEmpty())
        
        // Check that at least one message has a count > 1 (indicating aggregation)
        val hasAggregatedMessage = aggregatedMessages.any { it.count > 1 }
        assertTrue(hasAggregatedMessage, "Expected at least one aggregated message with count > 1")
    }
    
    @Test
    fun `ValidationAggregator should provide summary statistics`() {
        val aggregator = ValidationAggregator()
        
        // Add various types of validation messages
        val errorIssue = ValidationIssue(
            type = IssueType.EXPLODING_GRADIENTS,
            severity = IssueSeverity.HIGH,
            message = "Error message",
            value = 10.0
        )
        
        val warningIssue = ValidationIssue(
            type = IssueType.GAMES_TOO_SHORT,
            severity = IssueSeverity.MEDIUM,
            message = "Warning message",
            value = 5.0
        )
        
        val result = ValidationResult(
            cycle = 1,
            timestamp = System.currentTimeMillis(),
            isValid = false,
            issues = listOf(errorIssue, warningIssue),
            warnings = listOf("A warning"),
            recommendations = listOf("A recommendation"),
            trainingMetrics = createTrainingMetrics(),
            gameAnalysis = createGameAnalysis(),
            smoothedGradientNorm = null,
            smoothedPolicyEntropy = null,
            smoothedLoss = null
        )
        
        aggregator.addValidationResult(result)
        val stats = aggregator.getSummaryStats()
        
        assertEquals(4, stats.totalUniqueMessages) // 2 issues + 1 warning + 1 recommendation
        assertEquals(4, stats.totalOccurrences)
        assertEquals(1, stats.errorCount) // High severity issue
        assertEquals(2, stats.warningCount) // Medium severity issue + warning
        assertEquals(1, stats.infoCount) // Recommendation
    }
    
    // Helper methods
    
    private fun createTrainingMetrics(): TrainingMetrics {
        return TrainingMetrics(
            batchCount = 10,
            averageLoss = 1.0,
            averageGradientNorm = 2.0,
            averagePolicyEntropy = 1.5,
            averageReward = 0.5,
            experienceBufferSize = 1000
        )
    }
    
    private fun createGameAnalysis(): GameAnalysis {
        return GameAnalysis(
            averageGameLength = 50.0,
            gameCompletionRate = 0.8,
            drawRate = 0.3,
            stepLimitRate = 0.1,
            qualityScore = 0.7
        )
    }
}