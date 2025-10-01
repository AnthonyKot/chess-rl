package com.chessrl.integration.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for JVM-specific configuration parser functionality.
 */
class JvmConfigParserTest {
    
    @Test
    fun testParseProfileData() {
        val profileData = mapOf(
            "learningRate" to "0.002",
            "batchSize" to "128",
            "hiddenLayers" to "1024,512,256",
            "gamesPerCycle" to "30",
            "seed" to "12345"
        )
        
        val config = JvmConfigParser.parseProfileData(profileData)
        
        assertEquals(0.002, config.learningRate)
        assertEquals(128, config.batchSize)
        assertEquals(listOf(1024, 512, 256), config.hiddenLayers)
        assertEquals(30, config.gamesPerCycle)
        assertEquals(12345L, config.seed)
    }
    
    @Test
    fun testParseProfileDataWithLegacyParameters() {
        val profileData = mapOf(
            "episodes" to "150",  // Legacy: maps to maxCycles
            "parallelGames" to "6",  // Legacy: maps to maxConcurrentGames
            "gamesPerIteration" to "25"  // Legacy: maps to gamesPerCycle
        )
        
        val config = JvmConfigParser.parseProfileData(profileData)
        
        assertEquals(150, config.maxCycles)
        assertEquals(6, config.maxConcurrentGames)
        assertEquals(25, config.gamesPerCycle)
    }
    
    @Test
    fun testParseProfileDataIgnoresUnknownKeys() {
        val profileData = mapOf(
            "learningRate" to "0.001",
            "unknownParameter" to "someValue",
            "anotherUnknown" to "123",
            "batchSize" to "64"
        )
        
        val config = JvmConfigParser.parseProfileData(profileData)
        
        assertEquals(0.001, config.learningRate)
        assertEquals(64, config.batchSize)
        // Should not throw exception for unknown parameters
    }
    
    @Test
    fun testParseProfileDataInvalidValues() {
        val profileData = mapOf(
            "learningRate" to "invalid-number"
        )
        
        assertFailsWith<IllegalArgumentException> {
            JvmConfigParser.parseProfileData(profileData)
        }
    }
    
    @Test
    fun testLoadProfileFallsBackToBuiltIn() {
        // This should fall back to built-in profiles when file loading fails
        val config = JvmConfigParser.loadProfile("fast-debug")
        
        assertEquals(5, config.gamesPerCycle)
        assertEquals(10, config.maxCycles)
        assertEquals(2, config.maxConcurrentGames)
    }
    
    @Test
    fun testGetAllProfilesIncludesBuiltIn() {
        val allProfiles = JvmConfigParser.getAllProfiles()
        
        // Should include at least the built-in profiles
        assertTrue(allProfiles.containsKey("fast-debug"))
        assertTrue(allProfiles.containsKey("long-train"))
        assertTrue(allProfiles.containsKey("eval-only"))
        
        // Verify the profiles are valid
        val fastDebug = allProfiles["fast-debug"]!!
        assertEquals(5, fastDebug.gamesPerCycle)
        
        val longTrain = allProfiles["long-train"]!!
        assertEquals(50, longTrain.gamesPerCycle)
        
        val evalOnly = allProfiles["eval-only"]!!
        assertEquals(500, evalOnly.evaluationGames)
    }
    
    @Test
    fun testParseHiddenLayersFromProfile() {
        // Test the private method through parseProfileData
        val profileData = mapOf("hiddenLayers" to "768,512,256,128")
        val config = JvmConfigParser.parseProfileData(profileData)
        
        assertEquals(listOf(768, 512, 256, 128), config.hiddenLayers)
    }
}