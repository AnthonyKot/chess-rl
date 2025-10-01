package com.chessrl.integration.backend

import kotlin.test.*

/**
 * Tests for RL4J availability checking functionality.
 */
class RL4JAvailabilityTest {
    
    @Test
    fun testRL4JAvailabilityCheck() {
        // Test that availability check doesn't throw exceptions
        val isAvailable = RL4JAvailability.isAvailable()
        
        assertTrue(isAvailable, "RL4J should be available when enableRL4J=true")
    }
    
    @Test
    fun testRL4JAvailabilityMessage() {
        // Test that we get a meaningful message about availability
        val message = RL4JAvailability.getAvailabilityMessage()
        
        assertNotNull(message)
        assertTrue(message.isNotEmpty(), "Availability message should not be empty")
        
        if (RL4JAvailability.isAvailable()) {
            assertEquals("RL4J backend is available and ready to use", message)
        } else {
            assertTrue(message.contains("enableRL4J=true"), "Message should contain enableRL4J instructions")
        }
    }
    
    @Test
    fun testRL4JValidationThrowsWhenNotAvailable() {
        if (RL4JAvailability.isAvailable()) {
            RL4JAvailability.validateAvailability() // should not throw
        } else {
            assertFailsWith<IllegalStateException> {
                RL4JAvailability.validateAvailability()
            }
        }
    }
    
    @Test
    fun testBackendTypeIncludesRL4J() {
        // Test that RL4J is now included in the BackendType enum
        val rl4jBackend = BackendType.RL4J
        assertNotNull(rl4jBackend)
        assertEquals("RL4J", rl4jBackend.name)
    }
    
    @Test
    fun testBackendTypeFromStringSupportsRL4J() {
        // Test that BackendType.fromString supports RL4J
        val rl4jBackend = BackendType.fromString("rl4j")
        assertEquals(BackendType.RL4J, rl4jBackend)
        
        val rl4jBackendUppercase = BackendType.fromString("RL4J")
        assertEquals(BackendType.RL4J, rl4jBackendUppercase)
    }
    
    @Test
    fun testBackendSelectorValidatesRL4J() {
        // Test that BackendSelector can validate RL4J availability
        val result = BackendSelector.validateBackendAvailability(BackendType.RL4J)
        
        assertNotNull(result)
        assertEquals("RL4J", result.backend)
        
        if (RL4JAvailability.isAvailable()) {
            assertTrue(result.isValid, "RL4J validation should pass when available")
        } else {
            assertFalse(result.isValid, "RL4J validation should fail when not available")
            assertTrue(result.issues.isNotEmpty(), "Should have issues when RL4J not available")
        }
    }
}
