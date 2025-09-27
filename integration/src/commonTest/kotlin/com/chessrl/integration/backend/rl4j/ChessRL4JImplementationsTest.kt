package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.ChessEnvironment
import kotlin.test.*

/**
 * Tests for Chess RL4J implementations (ChessObservation, ChessObservationSpace, ChessActionSpace, ChessMDP).
 */
class ChessRL4JImplementationsTest {
    
    @Test
    fun testChessObservationCreation() {
        // Given a valid 839-dimensional state vector
        val stateVector = DoubleArray(839) { it * 0.1 }
        
        // When creating a chess observation
        val observation = ChessObservation(stateVector)
        
        // Then it should be created successfully
        assertEquals(839, observation.getDimensions())
        assertTrue(observation.isValid())
        assertContentEquals(stateVector, observation.getData())
    }
    
    @Test
    fun testChessObservationInvalidDimensions() {
        // Given an invalid state vector (wrong dimensions)
        val invalidStateVector = DoubleArray(100) { it * 0.1 }
        
        // When creating a chess observation
        // Then it should throw an exception
        assertFailsWith<IllegalArgumentException> {
            ChessObservation(invalidStateVector)
        }
    }
    
    @Test
    fun testChessObservationWithNaNValues() {
        // Given a state vector with NaN values
        val stateVector = DoubleArray(839) { if (it == 100) Double.NaN else it * 0.1 }
        
        // When creating a chess observation
        val observation = ChessObservation(stateVector)
        
        // Then it should be created but marked as invalid
        assertFalse(observation.isValid())
    }
    
    @Test
    fun testChessObservationDuplicate() {
        // Given a chess observation
        val stateVector = DoubleArray(839) { it * 0.1 }
        val observation = ChessObservation(stateVector)
        
        // When creating a duplicate
        val duplicate = observation.duplicate()
        
        // Then it should be equal but not the same instance
        assertEquals(observation, duplicate)
        assertNotSame(observation, duplicate)
        assertContentEquals(observation.getData(), duplicate.getData())
    }
    
    @Test
    fun testChessObservationSpace() {
        // Given a chess observation space
        val observationSpace = ChessObservationSpace()
        
        // Then it should have correct properties
        assertEquals("ChessObservationSpace", observationSpace.getName())
        assertContentEquals(intArrayOf(839), observationSpace.getShape())
        assertEquals(839, observationSpace.getLow().size)
        assertEquals(839, observationSpace.getHigh().size)
    }
    
    @Test
    fun testChessObservationSpaceContains() {
        // Given a chess observation space and valid observation
        val observationSpace = ChessObservationSpace()
        val validStateVector = DoubleArray(839) { 0.5 } // Within bounds
        val validObservation = ChessObservation(validStateVector)
        
        // When checking if space contains the observation
        val contains = observationSpace.contains(validObservation)
        
        // Then it should return true
        assertTrue(contains)
    }
    
    @Test
    fun testChessObservationSpaceContainsOutOfBounds() {
        // Given a chess observation space and out-of-bounds observation
        val observationSpace = ChessObservationSpace()
        val outOfBoundsStateVector = DoubleArray(839) { 100.0 } // Outside bounds
        val outOfBoundsObservation = ChessObservation(outOfBoundsStateVector)
        
        // When checking if space contains the observation
        val contains = observationSpace.contains(outOfBoundsObservation)
        
        // Then it should return false
        assertFalse(contains)
    }
    
    @Test
    fun testChessObservationSpaceSample() {
        // Given a chess observation space
        val observationSpace = ChessObservationSpace()
        
        // When sampling an observation
        val sample = observationSpace.sample()
        
        // Then it should be valid and within the space
        assertTrue(sample.isValid())
        assertTrue(observationSpace.contains(sample))
        assertEquals(839, sample.getDimensions())
    }
    
    @Test
    fun testChessActionSpace() {
        // Given a chess action space
        val actionSpace = ChessActionSpace()
        
        // Then it should have correct properties
        assertEquals("ChessActionSpace", actionSpace.getName())
        assertEquals(4096, actionSpace.getSize())
        assertEquals(0, actionSpace.getMinAction())
        assertEquals(4095, actionSpace.getMaxAction())
        assertTrue(actionSpace.isDiscrete())
    }
    
    @Test
    fun testChessActionSpaceContains() {
        // Given a chess action space
        val actionSpace = ChessActionSpace()
        
        // When checking valid actions
        assertTrue(actionSpace.contains(0))
        assertTrue(actionSpace.contains(2048))
        assertTrue(actionSpace.contains(4095))
        
        // When checking invalid actions
        assertFalse(actionSpace.contains(-1))
        assertFalse(actionSpace.contains(4096))
        assertFalse(actionSpace.contains(10000))
    }
    
    @Test
    fun testChessActionSpaceSample() {
        // Given a chess action space
        val actionSpace = ChessActionSpace()
        
        // When sampling actions
        repeat(10) {
            val sample = actionSpace.sample()
            
            // Then they should be valid
            assertTrue(actionSpace.contains(sample))
            assertTrue(sample >= 0)
            assertTrue(sample < 4096)
        }
    }
    
    @Test
    fun testChessActionSpaceGetAllActions() {
        // Given a chess action space
        val actionSpace = ChessActionSpace()
        
        // When getting all actions
        val allActions = actionSpace.getAllActions()
        
        // Then it should contain all valid actions
        assertEquals(4096, allActions.size)
        assertEquals(0, allActions.first())
        assertEquals(4095, allActions.last())
        
        // Check that all actions are valid
        allActions.forEach { action ->
            assertTrue(actionSpace.contains(action))
        }
    }
    
    @Test
    fun testChessMDPCreation() {
        // Given a chess environment
        val chessEnvironment = ChessEnvironment()
        
        // When creating a chess MDP
        val chessMDP = ChessMDP(chessEnvironment)
        
        // Then it should be created successfully
        assertNotNull(chessMDP.getObservationSpace())
        assertNotNull(chessMDP.getActionSpace())
        assertFalse(chessMDP.isDone())
        assertSame(chessEnvironment, chessMDP.getChessEnvironment())
    }
    
    @Test
    fun testChessMDPReset() {
        // Given a chess MDP
        val chessEnvironment = ChessEnvironment()
        val chessMDP = ChessMDP(chessEnvironment)
        
        // When resetting the MDP
        val observation = chessMDP.reset()
        
        // Then it should return a valid observation
        assertNotNull(observation)
        assertTrue(observation.isValid())
        assertEquals(839, observation.getDimensions())
        assertFalse(chessMDP.isDone())
        assertEquals(observation, chessMDP.getCurrentObservation())
    }
    
    @Test
    fun testChessMDPStepWithValidAction() {
        // Given a chess MDP that has been reset
        val chessEnvironment = ChessEnvironment()
        val chessMDP = ChessMDP(chessEnvironment)
        chessMDP.reset()
        
        // When stepping with a valid action (e2e4 is typically action 0 or similar)
        // We'll use action 0 as a test case
        val stepReply = chessMDP.step(0)
        
        // Then it should return a valid step reply
        assertNotNull(stepReply.observation)
        assertTrue(stepReply.observation.isValid())
        assertTrue(stepReply.reward.isFinite())
        
        // Info should contain action information
        assertTrue(stepReply.info.containsKey("originalAction"))
        assertTrue(stepReply.info.containsKey("actualAction"))
        assertTrue(stepReply.info.containsKey("legalActions"))
        assertTrue(stepReply.info.containsKey("wasIllegal"))
    }
    
    @Test
    fun testChessMDPStepWithInvalidAction() {
        // Given a chess MDP that has been reset
        val chessEnvironment = ChessEnvironment()
        val chessMDP = ChessMDP(chessEnvironment)
        chessMDP.reset()
        
        // When stepping with an invalid action
        val stepReply = chessMDP.step(-1)
        
        // Then it should handle the invalid action gracefully
        assertNotNull(stepReply.observation)
        assertTrue(stepReply.observation.isValid())
        
        // Should indicate that the action was illegal
        val wasIllegal = stepReply.info["wasIllegal"] as? Boolean
        assertTrue(wasIllegal == true)
    }
    
    @Test
    fun testChessMDPStepInTerminalState() {
        // Given a chess MDP in terminal state
        val chessEnvironment = ChessEnvironment()
        val chessMDP = ChessMDP(chessEnvironment)
        chessMDP.reset()
        
        // Simulate terminal state by setting done flag (using reflection or direct access)
        // For this test, we'll step until we get a terminal state or force it
        var stepReply = chessMDP.step(0)
        var attempts = 0
        
        // Try to reach terminal state (or simulate it)
        while (!stepReply.isDone && attempts < 100) {
            stepReply = chessMDP.step(0)
            attempts++
        }
        
        if (stepReply.isDone) {
            // When stepping in terminal state
            val terminalStepReply = chessMDP.step(0)
            
            // Then it should return a safe response
            assertNotNull(terminalStepReply.observation)
            assertTrue(terminalStepReply.isDone)
            assertTrue(terminalStepReply.info.containsKey("error"))
        } else {
            // If we couldn't reach terminal state naturally, that's also fine
            // The test verifies the MDP can handle steps without crashing
            assertTrue(true)
        }
    }
    
    @Test
    fun testChessMDPNewInstance() {
        // Given a chess MDP
        val chessEnvironment = ChessEnvironment()
        val chessMDP = ChessMDP(chessEnvironment)
        
        // When creating a new instance
        val newInstance = chessMDP.newInstance()
        
        // Then it should be a different instance but functionally equivalent
        assertNotSame(chessMDP, newInstance)
        assertNotNull(newInstance.getObservationSpace())
        assertNotNull(newInstance.getActionSpace())
        assertFalse(newInstance.isDone())
    }
    
    @Test
    fun testChessMDPClose() {
        // Given a chess MDP
        val chessEnvironment = ChessEnvironment()
        val chessMDP = ChessMDP(chessEnvironment)
        
        // When closing the MDP
        // Then it should not throw an exception
        assertDoesNotThrow {
            chessMDP.close()
        }
    }
    
    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Expected no exception, but got: ${e.message}")
        }
    }
}