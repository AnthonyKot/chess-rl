package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.ChessEnvironment
import com.chessrl.integration.backend.RL4JAvailability
import kotlin.test.*

/**
 * Test for the Chess MDP bridge with proper RL4J types.
 * 
 * This test verifies that ChessMDP properly implements RL4J interfaces
 * and can be used directly with RL4J trainers without type conversion errors.
 */
class ChessMDPBridgeTest {
    
    @Test
    fun testChessMDPImplementsRL4JInterfaces() {
        // Given a chess MDP
        val chessEnvironment = ChessEnvironment()
        val chessMDP = ChessMDP(chessEnvironment)
        
        // Then it should implement the RL4J MDP interface
        assertTrue(chessMDP is org.deeplearning4j.rl4j.mdp.MDP<*, *, *>)
        
        // And observation space should implement RL4J ObservationSpace
        val observationSpace = chessMDP.getObservationSpace()
        assertTrue(observationSpace is org.deeplearning4j.rl4j.space.ObservationSpace<*>)
        
        // And action space should extend RL4J DiscreteSpace
        val actionSpace = chessMDP.getActionSpace()
        assertTrue(actionSpace is org.deeplearning4j.rl4j.space.DiscreteSpace)
    }
    
    @Test
    fun testChessMDPResetAndStep() {
        // Given a chess MDP
        val chessEnvironment = ChessEnvironment()
        val chessMDP = ChessMDP(chessEnvironment)
        
        // When we reset the environment
        val initialObservation = chessMDP.reset()
        
        // Then we should get a valid chess observation
        assertNotNull(initialObservation)
        assertTrue(initialObservation is ChessObservation)
        assertTrue(initialObservation.isValid())
        assertFalse(chessMDP.isDone())
        
        // When we take a step
        val legalActions = chessEnvironment.getValidActions(initialObservation.getDataArray())
        assertTrue(legalActions.isNotEmpty(), "Should have legal actions in starting position")
        
        val action = legalActions.first()
        val stepReply = chessMDP.step(action)
        
        // Then we should get a valid RL4J StepReply
        assertNotNull(stepReply)
        assertTrue(stepReply is org.deeplearning4j.gym.StepReply<*>)
        
        // And the observation should be valid
        val nextObservation = stepReply.observation
        assertNotNull(nextObservation)
        assertTrue(nextObservation is ChessObservation)
        assertTrue(nextObservation.isValid())
        
        // And reward should be a valid number
        assertFalse(stepReply.reward.isNaN())
        assertFalse(stepReply.reward.isInfinite())
    }
    
    @Test
    fun testChessObservationSpaceImplementsRL4JInterface() {
        // Given a chess observation space
        val observationSpace = ChessObservationSpace()
        
        // Then it should implement RL4J ObservationSpace interface
        assertTrue(observationSpace is org.deeplearning4j.rl4j.space.ObservationSpace<*>)
        
        // And should have correct properties
        assertEquals("ChessObservationSpace", observationSpace.getName())
        assertContentEquals(intArrayOf(839), observationSpace.getShape())
        
        // And bounds should be INDArrays
        val low = observationSpace.getLow()
        val high = observationSpace.getHigh()
        assertNotNull(low)
        assertNotNull(high)
        assertTrue(low is org.nd4j.linalg.api.ndarray.INDArray)
        assertTrue(high is org.nd4j.linalg.api.ndarray.INDArray)
        
        // And should be able to sample observations
        val sample = observationSpace.sample()
        assertNotNull(sample)
        assertTrue(sample is ChessObservation)
        assertTrue(sample.isValid())
    }
    
    @Test
    fun testChessActionSpaceExtendsRL4JDiscreteSpace() {
        // Given a chess action space
        val actionSpace = ChessActionSpace()
        
        // Then it should extend RL4J DiscreteSpace
        assertTrue(actionSpace is org.deeplearning4j.rl4j.space.DiscreteSpace)
        
        // And should have correct size
        assertEquals(4096, actionSpace.getSize())
        
        // And should validate actions correctly
        assertTrue(actionSpace.contains(0))
        assertTrue(actionSpace.contains(4095))
        assertFalse(actionSpace.contains(-1))
        assertFalse(actionSpace.contains(4096))
        
        // And should be able to sample actions
        val sample = actionSpace.sample()
        assertTrue(sample in 0 until 4096)
    }
    
    @Test
    fun testChessObservationImplementsRL4JEncodable() {
        // Given a chess observation
        val stateVector = DoubleArray(839) { 0.5 }
        val observation = ChessObservation(stateVector)
        
        // Then it should implement RL4J Encodable interface
        assertTrue(observation is org.deeplearning4j.rl4j.space.Encodable)
        
        // And should provide correct data
        val data = observation.getData()
        assertNotNull(data)
        assertTrue(data is org.nd4j.linalg.api.ndarray.INDArray)
        
        // And should be duplicatable
        val duplicate = observation.dup()
        assertNotNull(duplicate)
        assertTrue(duplicate is org.deeplearning4j.rl4j.space.Encodable)
        assertTrue(duplicate is ChessObservation)
        
        // And should convert to array correctly
        val array = observation.toArray()
        assertNotNull(array)
        assertEquals(839, array.size)
        assertContentEquals(stateVector, array)
        
        // And should not be skipped
        assertFalse(observation.isSkipped())
    }
    
    @Test
    fun testMDPBridgeWithIllegalActions() {
        // Given a chess MDP
        val chessEnvironment = ChessEnvironment()
        val chessMDP = ChessMDP(chessEnvironment)
        
        // When we reset and try an illegal action
        chessMDP.reset()
        val illegalAction = 9999 // Definitely illegal
        val stepReply = chessMDP.step(illegalAction)
        
        // Then we should get a valid response (fallback mechanism)
        assertNotNull(stepReply)
        assertTrue(stepReply is org.deeplearning4j.gym.StepReply<*>)
        
        // And the observation should still be valid
        val observation = stepReply.observation
        assertNotNull(observation)
        assertTrue(observation is ChessObservation)
        assertTrue(observation.isValid())
        
        // And reward should be a valid number (might be penalty)
        assertFalse(stepReply.reward.isNaN())
        assertFalse(stepReply.reward.isInfinite())
    }
    
    @Test
    fun testMDPNewInstanceCreation() {
        // Given a chess MDP
        val chessEnvironment = ChessEnvironment()
        val chessMDP = ChessMDP(chessEnvironment)
        
        // When we create a new instance
        val newInstance = chessMDP.newInstance()
        
        // Then it should be a valid MDP
        assertNotNull(newInstance)
        assertTrue(newInstance is ChessMDP)
        assertTrue(newInstance is org.deeplearning4j.rl4j.mdp.MDP<*, *, *>)
        
        // And should be independent of the original
        assertNotSame(chessMDP, newInstance)
        
        // And should be able to reset and step independently
        val observation1 = chessMDP.reset()
        val observation2 = newInstance.reset()
        
        assertNotNull(observation1)
        assertNotNull(observation2)
        // Both should be valid starting positions
        assertTrue(observation1.isValid())
        assertTrue(observation2.isValid())
    }
    
    @Test
    @Suppress("UNUSED_VARIABLE")
    fun testMDPCanBeUsedWithRL4JTypes() {
        if (!RL4JAvailability.isAvailable()) {
            println("⚠️ RL4J not available, skipping real RL4J integration test")
            return
        }
        
        // Given a chess MDP
        val chessEnvironment = ChessEnvironment()
        val chessMDP = ChessMDP(chessEnvironment)
        
        // When we cast it to RL4J MDP interface
        val rl4jMDP: org.deeplearning4j.rl4j.mdp.MDP<ChessObservation, Int, org.deeplearning4j.rl4j.space.DiscreteSpace> = chessMDP
        
        // Then it should work without type conversion errors
        val observation = rl4jMDP.reset()
        assertNotNull(observation)
        assertTrue(observation.isValid())
        
        val observationSpace = rl4jMDP.getObservationSpace()
        assertNotNull(observationSpace)
        assertEquals("ChessObservationSpace", observationSpace.getName())
        
        val actionSpace = rl4jMDP.getActionSpace()
        assertNotNull(actionSpace)
        assertEquals(4096, actionSpace.getSize())
        
        // And should be able to step
        val stepReply = rl4jMDP.step(0) // Try first action
        assertNotNull(stepReply)
        assertNotNull(stepReply.observation)
        assertTrue(stepReply.observation.isValid())
        
        // And should be closeable
        rl4jMDP.close() // Should not throw
        
        println("✅ MDP bridge works correctly with real RL4J types")
    }
}