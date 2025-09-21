package com.chessrl.integration.output

import com.chessrl.integration.ValidationResult
import com.chessrl.integration.ValidationIssue
import com.chessrl.integration.IssueSeverity

/**
 * Consolidates and manages validation messages to reduce spam and repetitive output.
 * Tracks validation issues over time and provides aggregated summaries with repeat counts.
 */
class ValidationAggregator {
    
    // Map of message content to aggregated validation data
    private val messageHistory = mutableMapOf<String, AggregatedValidationMessage>()
    
    // Track when we last reported aggregated messages to avoid spam
    private var lastReportTime: Long = 0
    private val minReportInterval = 5000L // 5 seconds minimum between reports
    
    /**
     * Add a validation result to the aggregator for processing
     */
    fun addValidationResult(result: ValidationResult) {
        val currentTime = System.currentTimeMillis()
        
        // Process all issues from the validation result
        result.issues.forEach { issue ->
            addValidationIssue(issue, currentTime)
        }
        
        // Process warnings as validation messages
        result.warnings.forEach { warning ->
            addValidationMessage(warning, ValidationSeverity.WARNING, currentTime)
        }
        
        // Process recommendations as info messages
        result.recommendations.forEach { recommendation ->
            addValidationMessage("Recommendation: $recommendation", ValidationSeverity.INFO, currentTime)
        }
    }
    
    /**
     * Add a single validation issue to the aggregator
     */
    private fun addValidationIssue(issue: ValidationIssue, timestamp: Long) {
        val severity = mapIssueSeverityToValidationSeverity(issue.severity)
        addValidationMessage(issue.message, severity, timestamp)
    }
    
    /**
     * Add a validation message with specified severity
     */
    private fun addValidationMessage(message: String, severity: ValidationSeverity, timestamp: Long) {
        val existing = messageHistory[message]
        
        if (existing != null) {
            // Update existing message with new occurrence
            messageHistory[message] = existing.copy(
                count = existing.count + 1,
                lastSeen = timestamp,
                severity = maxOf(existing.severity, severity) // Use higher severity
            )
        } else {
            // Create new aggregated message
            messageHistory[message] = AggregatedValidationMessage(
                message = message,
                severity = severity,
                count = 1,
                firstSeen = timestamp,
                lastSeen = timestamp
            )
        }
    }
    
    /**
     * Get aggregated messages that should be reported, filtering by recency and importance
     */
    fun getAggregatedMessages(): List<AggregatedValidationMessage> {
        val currentTime = System.currentTimeMillis()
        
        // Don't report too frequently to avoid spam
        if (currentTime - lastReportTime < minReportInterval && messageHistory.isNotEmpty()) {
            return emptyList()
        }
        
        // Get messages sorted by severity and recency
        val messages = messageHistory.values
            .filter { shouldReportMessage(it, currentTime) }
            .sortedWith(compareByDescending<AggregatedValidationMessage> { it.severity.ordinal }
                .thenByDescending { it.count }
                .thenByDescending { it.lastSeen })
        
        if (messages.isNotEmpty()) {
            lastReportTime = currentTime
        }
        
        return messages
    }
    
    /**
     * Get new messages since the last detailed report
     */
    fun getNewMessages(sinceTime: Long): List<AggregatedValidationMessage> {
        return messageHistory.values
            .filter { it.firstSeen > sinceTime }
            .sortedWith(compareByDescending<AggregatedValidationMessage> { it.severity.ordinal }
                .thenByDescending { it.count })
    }
    
    /**
     * Get messages that have changed since the last report
     */
    fun getChangedMessages(sinceTime: Long): List<AggregatedValidationMessage> {
        return messageHistory.values
            .filter { it.lastSeen > sinceTime && it.firstSeen <= sinceTime }
            .sortedWith(compareByDescending<AggregatedValidationMessage> { it.severity.ordinal }
                .thenByDescending { it.count })
    }
    
    /**
     * Clear old validation messages to prevent memory buildup
     */
    fun clearOldMessages(olderThanMs: Long = 300000L) { // Default 5 minutes
        val cutoffTime = System.currentTimeMillis() - olderThanMs
        
        messageHistory.entries.removeAll { (_, message) ->
            message.lastSeen < cutoffTime && message.severity != ValidationSeverity.ERROR
        }
    }
    
    /**
     * Clear all validation history
     */
    fun clearHistory() {
        messageHistory.clear()
        lastReportTime = 0
    }
    
    /**
     * Get summary statistics about validation messages
     */
    fun getSummaryStats(): ValidationAggregatorStats {
        val totalMessages = messageHistory.size
        val totalOccurrences = messageHistory.values.sumOf { it.count }
        val errorCount = messageHistory.values.count { it.severity == ValidationSeverity.ERROR }
        val warningCount = messageHistory.values.count { it.severity == ValidationSeverity.WARNING }
        val infoCount = messageHistory.values.count { it.severity == ValidationSeverity.INFO }
        
        return ValidationAggregatorStats(
            totalUniqueMessages = totalMessages,
            totalOccurrences = totalOccurrences,
            errorCount = errorCount,
            warningCount = warningCount,
            infoCount = infoCount
        )
    }
    
    /**
     * Check if a message should be reported based on severity and timing
     */
    private fun shouldReportMessage(message: AggregatedValidationMessage, currentTime: Long): Boolean {
        val timeSinceLastSeen = currentTime - message.lastSeen
        
        return when (message.severity) {
            ValidationSeverity.ERROR -> {
                // Always report errors if they're recent (within 30 seconds)
                timeSinceLastSeen < 30000L
            }
            ValidationSeverity.WARNING -> {
                // Report warnings if recent (within 60 seconds) or if they've occurred multiple times
                timeSinceLastSeen < 60000L || message.count >= 3
            }
            ValidationSeverity.INFO -> {
                // Report info messages only if they're very recent (within 10 seconds) or frequent
                timeSinceLastSeen < 10000L || message.count >= 5
            }
        }
    }
    
    /**
     * Map from ValidationIssue severity to ValidationSeverity
     */
    private fun mapIssueSeverityToValidationSeverity(issueSeverity: IssueSeverity): ValidationSeverity {
        return when (issueSeverity) {
            IssueSeverity.LOW -> ValidationSeverity.INFO
            IssueSeverity.MEDIUM -> ValidationSeverity.WARNING
            IssueSeverity.HIGH, IssueSeverity.CRITICAL -> ValidationSeverity.ERROR
        }
    }
    
    /**
     * Extension function to compare ValidationSeverity by priority
     */
    private fun maxOf(severity1: ValidationSeverity, severity2: ValidationSeverity): ValidationSeverity {
        return if (severity1.ordinal >= severity2.ordinal) severity1 else severity2
    }
}

/**
 * Statistics about validation message aggregation
 */
data class ValidationAggregatorStats(
    val totalUniqueMessages: Int,
    val totalOccurrences: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int
)