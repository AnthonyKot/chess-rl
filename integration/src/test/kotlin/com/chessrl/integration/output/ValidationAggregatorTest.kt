package com.chessrl.integration.output

import com.chessrl.integration.*
import kotlin.test.*

class ValidationAggregatorTest {
    
    private lateinit var aggregator: ValidationAggregator
    
    @BeforeTest
    fun setup() {
        aggregator = ValidationAggregator()
    }
    
    @Test
    fun `should aggregate identical validation messages`() {
        // Create validation results with identical issues
        val issue = ValidationIssue(
            type = IssueType.EXPLODING_GRADIENTS,
            severity = IssueSeverity.HIGH,
            message = "Gradient norm too high: 15.0",
            value = 15.0
        )
        
        val result1 = createValidationResult(listOf(issue))
        val result2 = createValidationResult(listOf(issue))
        val result3 = createValidationResult(listOf(issue))
        
        // Add results to aggregator
        aggregator.addValidationResult(result1)
        aggregator.addValidationResult(result2)
        aggregator.addValidationResult(result3)
        
        // Get aggregated messages
        val messages = aggregator.getAggregatedMessages()
        
        // Should have one message with count of 3
        assertEquals(1, messages.size)
        assertEquals("Gradient norm too high: 15.0", messages[0].message)
        assertEquals(3, messages[0].count)
        assertEquals(ValidationSeverity.ERROR, messages[0].severity)
    }
    
    @Test
    fun `should handle different severity levels correctly`() {
        val highIssue = ValidationIssue(
            type = IssueType.EXPLODING_GRADIENTS,
            severity = IssueSeverity.HIGH,
            message = "High severity issue",
            value = 10.0
        )
        
        val mediumIssue = ValidationIssue(
            type = IssueType.GAMES_TOO_SHORT,
            severity = IssueSeverity.MEDIUM,
            message = "Medium severity issue",
            value = 5.0
        )
        
        val lowIssue = ValidationIssue(
            type = IssueType.LOW_MOVE_DIVERSITY,
            severity = IssueSeverity.LOW,
            message = "Low severity issue",
            value = 0.2
        )
        
        val result = createValidationResult(listOf(highIssue, mediumIssue, lowIssue))
        aggregator.addValidationResult(result)
        
        val messages = aggregator.getAggregatedMessages()
        
        // Should have 3 messages with correct severity mapping
        assertEquals(3, messages.size)
        
        val errorMessage = messages.find { it.severity == ValidationSeverity.ERROR }
        val warningMessage = messages.find { it.severity == ValidationSeverity.WARNING }
        val infoMessage = messages.find { it.severity == ValidationSeverity.INFO }
        
        assertNotNull(errorMessage)
        assertNotNull(warningMessage)
        assertNotNull(infoMessage)
        
        assertEquals("High severity issue", errorMessage.message)
        assertEquals("Medium severity issue", warningMessage.message)
        assertEquals("Low severity issue", infoMessage.message)
    }
    
    @Test
    fun `should upgrade severity when same message has higher severity`() {
        val message = "Same message text"
        
        val lowIssue = ValidationIssue(
            type = IssueType.LOW_MOVE_DIVERSITY,
            severity = IssueSeverity.LOW,
            message = message,
            value = 0.2
        )
        
        val highIssue = ValidationIssue(
            type = IssueType.EXPLODING_GRADIENTS,
            severity = IssueSeverity.HIGH,
            message = message,
            value = 10.0
        )
        
        // Add low severity first
        aggregator.addValidationResult(createValidationResult(listOf(lowIssue)))
        
        // Add high severity second
        aggregator.addValidationResult(createValidationResult(listOf(highIssue)))
        
        val messages = aggregator.getAggregatedMessages()
        
        // Should have one message with high severity and count of 2
        assertEquals(1, messages.size)
        assertEquals(message, messages[0].message)
        assertEquals(2, messages[0].count)
        assertEquals(ValidationSeverity.ERROR, messages[0].severity) // Should be upgraded to ERROR
    }
    
    @Test
    fun `should handle warnings and recommendations`() {
        val result = ValidationResult(
            cycle = 1,
            timestamp = System.currentTimeMillis(),
            isValid = false,
            issues = emptyList(),
            warnings = listOf("Warning message", "Another warning"),
            recommendations = listOf("Try this", "Or this"),
            trainingMetrics = createTrainingMetrics(),
            gameAnalysis = createGameAnalysis(),
            smoothedGradientNorm = null,
            smoothedPolicyEntropy = null,
            smoothedLoss = null
        )
        
        aggregator.addValidationResult(result)
        val messages = aggregator.getAggregatedMessages()
        
        // Should have 4 messages: 2 warnings + 2 recommendations
        assertEquals(4, messages.size)
        
        val warnings = messages.filter { it.severity == ValidationSeverity.WARNING }
        val recommendations = messages.filter { it.severity == ValidationSeverity.INFO && it.message.startsWith("Recommendation:") }
        
        assertEquals(2, warnings.size)
        assertEquals(2, recommendations.size)
    }
    
    @Test
    fun `should clear old messages`() {
        val issue = ValidationIssue(
            type = IssueType.EXPLODING_GRADIENTS,
            severity = IssueSeverity.MEDIUM, // Use medium so it can be cleared
            message = "Old message",
            value = 10.0
        )
        
        val result = createValidationResult(listOf(issue))
        aggregator.addValidationResult(result)
        
        // Verify message exists
        assertEquals(1, aggregator.getAggregatedMessages().size)
        
        // Clear old messages (use 0ms to clear everything)
        aggregator.clearOldMessages(0L)
        
        // Should be empty now
        assertEquals(0, aggregator.getAggregatedMessages().size)
    }
    
    @Test
    fun `should provide summary statistics`() {
        val highIssue = ValidationIssue(
            type = IssueType.EXPLODING_GRADIENTS,
            severity = IssueSeverity.HIGH,
            message = "Error message",
            value = 10.0
        )
        
        val mediumIssue = ValidationIssue(
            type = IssueType.GAMES_TOO_SHORT,
            severity = IssueSeverity.MEDIUM,
            message = "Warning message",
            value = 5.0
        )
        
        val result = ValidationResult(
            cycle = 1,
            timestamp = System.currentTimeMillis(),
            isValid = false,
            issues = listOf(highIssue, mediumIssue),
            warnings = listOf("Another warning"),
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
    
    @Test
    fun `should track timing correctly`() {
        val issue = ValidationIssue(
            type = IssueType.EXPLODING_GRADIENTS,
            severity = IssueSeverity.HIGH,
            message = "Timing test",
            value = 10.0
        )
        
        val startTime = System.currentTimeMillis()
        
        // Add first occurrence
        aggregator.addValidationResult(createValidationResult(listOf(issue)))
        
        // Wait a bit
        Thread.sleep(10)
        
        // Add second occurrence
        aggregator.addValidationResult(createValidationResult(listOf(issue)))
        
        val messages = aggregator.getAggregatedMessages()
        assertEquals(1, messages.size)
        
        val message = messages[0]
        assertEquals(2, message.count)
        assertTrue(message.firstSeen >= startTime)
        assertTrue(message.lastSeen >= message.firstSeen)
    }
    
    // Helper methods
    
    private fun createValidationResult(issues: List<ValidationIssue>): ValidationResult {
        return ValidationResult(
            cycle = 1,
            timestamp = System.currentTimeMillis(),
            isValid = issues.isEmpty(),
            issues = issues,
            warnings = emptyList(),
            recommendations = emptyList(),
            trainingMetrics = createTrainingMetrics(),
            gameAnalysis = createGameAnalysis(),
            smoothedGradientNorm = null,
            smoothedPolicyEntropy = null,
            smoothedLoss = null
        )
    }
    
    private fun createTrainingMetrics(): com.chessrl.integration.TrainingMetrics {
        return com.chessrl.integration.TrainingMetrics(
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