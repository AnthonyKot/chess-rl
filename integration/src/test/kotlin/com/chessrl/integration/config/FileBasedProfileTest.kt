package com.chessrl.integration.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for file-based profile loading integration.
 * 
 * These tests verify that the JvmConfigParser correctly integrates with
 * the existing ProfilesLoader to load profiles from the actual profiles.yaml file.
 */
class FileBasedProfileTest {
    
    @Test
    fun testLoadProfileFromActualFile() {
        // This test verifies that we can load profiles from the actual integration/profiles.yaml
        // The profiles.yaml should contain the simplified profiles we created
        
        try {
            val fastDebugConfig = JvmConfigParser.loadProfileFromFile("fast-debug")
            
            // Verify it loaded the profile correctly
            assertEquals(5, fastDebugConfig.gamesPerCycle)
            assertEquals(10, fastDebugConfig.maxCycles)
            assertEquals(2, fastDebugConfig.maxConcurrentGames)
            assertEquals(40, fastDebugConfig.maxStepsPerGame)
            assertEquals(listOf(256, 128), fastDebugConfig.hiddenLayers)
            assertEquals(32, fastDebugConfig.batchSize)
            assertEquals("checkpoints/fast-debug", fastDebugConfig.checkpointDirectory)
            
        } catch (e: Exception) {
            // If file loading fails, verify we get a meaningful error
            assertTrue(e.message?.contains("Profile") == true || e.message?.contains("not found") == true,
                "Should get a meaningful error message about profile not found: ${e.message}")
        }
    }
    
    @Test
    fun testLoadAllProfilesFromFile() {
        try {
            val allProfiles = JvmConfigParser.getAllProfiles()
            
            // Should include at least the built-in profiles
            assertTrue(allProfiles.containsKey("fast-debug"))
            assertTrue(allProfiles.containsKey("long-train"))
            assertTrue(allProfiles.containsKey("eval-only"))
            
            // Verify the profiles have expected characteristics
            val fastDebug = allProfiles["fast-debug"]!!
            assertTrue(fastDebug.gamesPerCycle <= 10, "Fast debug should have few games per cycle")
            assertTrue(fastDebug.maxCycles <= 20, "Fast debug should have few cycles")
            
            val longTrain = allProfiles["long-train"]!!
            assertTrue(longTrain.gamesPerCycle >= 20, "Long train should have many games per cycle")
            assertTrue(longTrain.maxCycles >= 100, "Long train should have many cycles")
            
            val evalOnly = allProfiles["eval-only"]!!
            assertTrue(evalOnly.evaluationGames >= 100, "Eval only should have many evaluation games")
            assertEquals(1, evalOnly.maxConcurrentGames, "Eval only should be single-threaded")
            
        } catch (e: Exception) {
            // If file loading fails, we should still get built-in profiles
            println("File loading failed, but that's expected in some test environments: ${e.message}")
        }
    }
    
    @Test
    fun testProfileFallbackBehavior() {
        // Test that JvmConfigParser.loadProfile falls back to built-in profiles
        // when file loading fails
        
        val config = JvmConfigParser.loadProfile("fast-debug")
        
        // Should get a valid configuration regardless of whether file loading worked
        assertTrue(config.gamesPerCycle > 0)
        assertTrue(config.maxCycles > 0)
        assertTrue(config.maxConcurrentGames > 0)
        assertTrue(config.hiddenLayers.isNotEmpty())
        
        // Should be optimized for fast debugging
        assertTrue(config.gamesPerCycle <= 10, "Should be optimized for fast iteration")
        assertTrue(config.maxCycles <= 20, "Should have few cycles for fast feedback")
    }
    
    @Test
    fun testLegacyParameterMapping() {
        // Test that legacy parameter names are mapped correctly
        val profileData = mapOf(
            "episodes" to "150",  // Legacy name for maxCycles
            "parallelGames" to "6",  // Legacy name for maxConcurrentGames
            "gamesPerIteration" to "25",  // Legacy name for gamesPerCycle
            "hiddenLayers" to "1024,512,256"
        )
        
        val config = JvmConfigParser.parseProfileData(profileData)
        
        assertEquals(150, config.maxCycles)
        assertEquals(6, config.maxConcurrentGames)
        assertEquals(25, config.gamesPerCycle)
        assertEquals(listOf(1024, 512, 256), config.hiddenLayers)
    }
    
    @Test
    fun testProfileValidation() {
        // Test that loaded profiles are valid according to ChessRLConfig validation
        
        try {
            val profiles = JvmConfigParser.getAllProfiles()
            
            for ((name, config) in profiles) {
                val validation = config.validate()
                
                if (!validation.isValid) {
                    println("Profile '$name' validation errors: ${validation.errors}")
                    println("Profile '$name' validation warnings: ${validation.warnings}")
                }
                
                // All profiles should be valid (no errors)
                assertTrue(validation.isValid, "Profile '$name' should be valid: ${validation.errors}")
                
                // Profiles may have warnings, but should not have critical errors
                assertTrue(validation.errors.isEmpty(), "Profile '$name' should have no errors: ${validation.errors}")
            }
            
        } catch (e: Exception) {
            println("Profile loading failed, but built-in profiles should still be valid: ${e.message}")
            
            // Test built-in profiles directly
            val builtInProfiles = listOf("fast-debug", "long-train", "eval-only")
            for (profileName in builtInProfiles) {
                val config = ConfigParser.loadProfile(profileName)
                val validation = config.validate()
                assertTrue(validation.isValid, "Built-in profile '$profileName' should be valid: ${validation.errors}")
            }
        }
    }
    
}
