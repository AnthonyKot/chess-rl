package com.chessrl.integration.backend

import kotlin.test.*

/**
 * Integration test to verify RL4J backend can be selected via CLI.
 */
class RL4JCLIIntegrationTest {
    
    @Test
    fun testCLICanParseRL4JBackend() {
        // Test that CLI can parse --nn rl4j flag
        val args = arrayOf("--train", "--nn", "rl4j", "--cycles", "5")
        val selectedBackend = BackendSelector.parseBackendFromArgs(args)
        
        assertEquals(BackendType.RL4J, selectedBackend, "CLI should parse RL4J backend correctly")
    }
    
    @Test
    fun testCLICanSelectRL4JBackendWithPrecedence() {
        // Test that CLI args take precedence over profile config
        val args = arrayOf("--nn", "rl4j")
        val profile = mapOf("nnBackend" to "dl4j")
        
        val selectedBackend = BackendSelector.selectBackend(args, profile)
        assertEquals(BackendType.RL4J, selectedBackend, "CLI RL4J selection should override profile DL4J")
    }
    
    @Test
    fun testProfileCanSelectRL4JBackend() {
        // Test that profile config can select RL4J backend
        val args = arrayOf("--train")
        val profile = mapOf("nnBackend" to "rl4j")
        
        val selectedBackend = BackendSelector.selectBackend(args, profile)
        assertEquals(BackendType.RL4J, selectedBackend, "Profile should be able to select RL4J backend")
    }
    
    @Test
    fun testRL4JBackendValidationFailsWhenNotAvailable() {
        // Test that RL4J backend validation fails when dependencies are not available
        val validation = BackendSelector.validateBackendAvailability(BackendType.RL4J)
        
        assertFalse(validation.isValid, "RL4J validation should fail when dependencies not available")
        assertEquals("RL4J", validation.backend)
        assertTrue(validation.issues.isNotEmpty(), "Should have validation issues when RL4J not available")
        
        // Check that the error message mentions the gradle property
        val issueText = validation.issues.joinToString(" ")
        assertTrue(issueText.contains("enableRL4J=true"), "Error should mention enableRL4J property")
    }
    
    @Test
    fun testRL4JBackendFallbackBehavior() {
        // Test that RL4J backend falls back gracefully when not available
        val (actualBackend, warnings) = BackendSelector.selectBackendWithFallback(
            primaryType = BackendType.RL4J,
            fallbackType = BackendType.MANUAL
        )
        
        // Should fall back to MANUAL since RL4J is not available
        assertEquals(BackendType.MANUAL, actualBackend, "Should fall back to MANUAL when RL4J not available")
        assertTrue(warnings.isNotEmpty(), "Should have warnings about fallback")
        
        val warningText = warnings.joinToString(" ")
        assertTrue(warningText.contains("RL4J"), "Warning should mention RL4J")
        assertTrue(warningText.contains("fallback") || warningText.contains("Falling back"), "Warning should mention fallback")
    }
}