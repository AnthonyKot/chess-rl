package com.chessrl.rl

import kotlin.random.Random
import kotlin.test.*

class ExperienceReplayTest {
    
    private fun createTestExperience(id: Int): Experience<String, String> {
        return Experience(
            state = "state$id",
            action = "action$id",
            reward = id.toDouble(),
            nextState = "nextState$id",
            done = id % 5 == 0
        )
    }
    
    @Test
    fun testCircularBufferBasicOperations() {
        val buffer = CircularExperienceBuffer<String, String>(maxSize = 3)
        
        assertEquals(0, buffer.size())
        assertEquals(3, buffer.capacity())
        assertFalse(buffer.isFull())
        
        // Add experiences
        val exp1 = createTestExperience(1)
        val exp2 = createTestExperience(2)
        val exp3 = createTestExperience(3)
        
        buffer.add(exp1)
        assertEquals(1, buffer.size())
        assertFalse(buffer.isFull())
        
        buffer.add(exp2)
        buffer.add(exp3)
        assertEquals(3, buffer.size())
        assertTrue(buffer.isFull())
        
        // Verify all experiences are stored
        val allExperiences = buffer.getAllExperiences()
        assertEquals(3, allExperiences.size)
        assertTrue(allExperiences.contains(exp1))
        assertTrue(allExperiences.contains(exp2))
        assertTrue(allExperiences.contains(exp3))
    }
    
    @Test
    fun testCircularBufferOverwrite() {
        val buffer = CircularExperienceBuffer<String, String>(maxSize = 2)
        
        val exp1 = createTestExperience(1)
        val exp2 = createTestExperience(2)
        val exp3 = createTestExperience(3)
        
        buffer.add(exp1)
        buffer.add(exp2)
        assertTrue(buffer.isFull())
        
        // Adding third experience should overwrite first
        buffer.add(exp3)
        assertEquals(2, buffer.size())
        
        val allExperiences = buffer.getAllExperiences()
        assertFalse(allExperiences.contains(exp1))  // Should be overwritten
        assertTrue(allExperiences.contains(exp2))
        assertTrue(allExperiences.contains(exp3))
    }
    
    @Test
    fun testCircularBufferSampling() {
        val buffer = CircularExperienceBuffer<String, String>(maxSize = 5)
        
        // Add experiences
        repeat(5) { i ->
            buffer.add(createTestExperience(i + 1))
        }
        
        // Test sampling
        val random = Random(42)
        val sample = buffer.sample(3, random)
        assertEquals(3, sample.size)
        
        // All sampled experiences should be from the buffer
        val allExperiences = buffer.getAllExperiences()
        sample.forEach { experience ->
            assertTrue(allExperiences.contains(experience))
        }
        
        // Test sampling more than available
        val largeSample = buffer.sample(10, random)
        assertEquals(5, largeSample.size)  // Should return all available
    }
    
    @Test
    fun testCircularBufferSamplingFromEmptyBuffer() {
        val buffer = CircularExperienceBuffer<String, String>(maxSize = 5)
        
        assertFailsWith<IllegalArgumentException> {
            buffer.sample(1)
        }
    }
    
    @Test
    fun testCircularBufferClear() {
        val buffer = CircularExperienceBuffer<String, String>(maxSize = 3)
        
        buffer.add(createTestExperience(1))
        buffer.add(createTestExperience(2))
        assertEquals(2, buffer.size())
        
        buffer.clear()
        assertEquals(0, buffer.size())
        assertFalse(buffer.isFull())
    }
    
    @Test
    fun testPrioritizedBufferBasicOperations() {
        val buffer = PrioritizedExperienceBuffer<String, String>(maxSize = 3)
        
        assertEquals(0, buffer.size())
        assertEquals(3, buffer.capacity())
        assertFalse(buffer.isFull())
        
        // Add experiences
        val exp1 = createTestExperience(1)
        val exp2 = createTestExperience(2)
        val exp3 = createTestExperience(3)
        
        buffer.add(exp1)
        assertEquals(1, buffer.size())
        
        buffer.add(exp2)
        buffer.add(exp3)
        assertEquals(3, buffer.size())
        assertTrue(buffer.isFull())
    }
    
    @Test
    fun testPrioritizedBufferSampling() {
        val buffer = PrioritizedExperienceBuffer<String, String>(maxSize = 5)
        
        // Add experiences
        repeat(5) { i ->
            buffer.add(createTestExperience(i + 1))
        }
        
        // Test sampling
        val random = Random(42)
        val sample = buffer.sample(3, random)
        assertEquals(3, sample.size)
        
        // Test sampling more than available
        val largeSample = buffer.sample(10, random)
        assertEquals(5, largeSample.size)
    }
    
    @Test
    fun testPrioritizedBufferPriorityUpdate() {
        val buffer = PrioritizedExperienceBuffer<String, String>(maxSize = 3)
        
        buffer.add(createTestExperience(1))
        buffer.add(createTestExperience(2))
        buffer.add(createTestExperience(3))
        
        // Update priorities
        val indices = listOf(0, 1, 2)
        val tdErrors = listOf(0.1, 0.5, 0.2)
        
        // This should not throw an exception
        buffer.updatePriorities(indices, tdErrors)
        
        // Test sampling after priority update
        val sample = buffer.sample(2)
        assertEquals(2, sample.size)
    }
    
    @Test
    fun testPrioritizedBufferImportanceSampling() {
        val buffer = PrioritizedExperienceBuffer<String, String>(maxSize = 3)
        
        buffer.add(createTestExperience(1))
        buffer.add(createTestExperience(2))
        buffer.add(createTestExperience(3))
        
        val sampledIndices = listOf(0, 1)
        val weights = buffer.getImportanceSamplingWeights(sampledIndices)
        
        assertEquals(2, weights.size)
        weights.forEach { weight ->
            assertTrue(weight > 0.0, "Importance sampling weights should be positive")
        }
    }
    
    @Test
    fun testPrioritizedBufferInvalidInputs() {
        val buffer = PrioritizedExperienceBuffer<String, String>(maxSize = 3)
        
        buffer.add(createTestExperience(1))
        
        // Test mismatched indices and TD errors
        assertFailsWith<IllegalArgumentException> {
            buffer.updatePriorities(listOf(0, 1), listOf(0.1))  // Different sizes
        }
    }
    
    @Test
    fun testBufferConstructorValidation() {
        assertFailsWith<IllegalArgumentException> {
            CircularExperienceBuffer<String, String>(maxSize = 0)
        }
        
        assertFailsWith<IllegalArgumentException> {
            CircularExperienceBuffer<String, String>(maxSize = -1)
        }
        
        assertFailsWith<IllegalArgumentException> {
            PrioritizedExperienceBuffer<String, String>(maxSize = 0)
        }
        
        assertFailsWith<IllegalArgumentException> {
            PrioritizedExperienceBuffer<String, String>(maxSize = 5, alpha = -0.1)
        }
        
        assertFailsWith<IllegalArgumentException> {
            PrioritizedExperienceBuffer<String, String>(maxSize = 5, beta = -0.1)
        }
    }
    
    @Test
    fun testSamplingValidation() {
        val buffer = CircularExperienceBuffer<String, String>(maxSize = 3)
        buffer.add(createTestExperience(1))
        
        assertFailsWith<IllegalArgumentException> {
            buffer.sample(0)  // Batch size must be positive
        }
        
        assertFailsWith<IllegalArgumentException> {
            buffer.sample(-1)  // Batch size must be positive
        }
    }
}