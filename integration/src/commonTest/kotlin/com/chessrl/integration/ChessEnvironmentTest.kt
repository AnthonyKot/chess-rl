package com.chessrl.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Integration package tests
 */
class ChessEnvironmentTest {
    
    @Test
    fun testChessEnvironmentCreation() {
        val environment = ChessEnvironment()
        assertNotNull(environment, "Chess environment should be created successfully")
    }
    
    @Test
    fun testStateSize() {
        val environment = ChessEnvironment()
        assertEquals(775, environment.getStateSize())
    }
    
    @Test
    fun testActionSize() {
        val environment = ChessEnvironment()
        assertEquals(4096, environment.getActionSize())
    }
    
    @Test
    fun testReset() {
        val environment = ChessEnvironment()
        val initialState = environment.reset()
        assertEquals(775, initialState.size)
    }
}