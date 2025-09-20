package com.chessrl.integration.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for ConfigParser functionality.
 */
class ConfigParserTest {
    
    @Test
    fun testParseArgsBasic() {
        val args = arrayOf(
            "--learning-rate", "0.002",
            "--batch-size", "128",
            "--games-per-cycle", "30",
            "--seed", "12345"
        )
        
        val config = ConfigParser.parseArgs(args)
        
        assertEquals(0.002, config.learningRate)
        assertEquals(128, config.batchSize)
        assertEquals(30, config.gamesPerCycle)
        assertEquals(12345L, config.seed)
    }
    
    @Test
    fun testParseArgsHiddenLayers() {
        val args = arrayOf("--hidden-layers", "1024,512,256,128")
        val config = ConfigParser.parseArgs(args)
        
        assertEquals(listOf(1024, 512, 256, 128), config.hiddenLayers)
    }
    
    @Test
    fun testParseArgsIgnoresUnknown() {
        val args = arrayOf(
            "--learning-rate", "0.001",
            "--unknown-flag", "value",
            "--batch-size", "64"
        )
        
        val config = ConfigParser.parseArgs(args)
        
        assertEquals(0.001, config.learningRate)
        assertEquals(64, config.batchSize)
        // Should not throw exception for unknown flag
    }
    
    @Test
    fun testLoadProfileFastDebug() {
        val config = ConfigParser.loadProfile("fast-debug")
        
        assertEquals(5, config.gamesPerCycle)
        assertEquals(10, config.maxCycles)
        assertEquals(2, config.maxConcurrentGames)
        assertEquals(40, config.maxStepsPerGame)
        assertEquals(20, config.evaluationGames)
        assertEquals(listOf(256, 128), config.hiddenLayers)
        assertEquals("checkpoints/fast-debug", config.checkpointDirectory)
    }
    
    @Test
    fun testLoadProfileLongTrain() {
        val config = ConfigParser.loadProfile("long-train")
        
        assertEquals(50, config.gamesPerCycle)
        assertEquals(200, config.maxCycles)
        assertEquals(8, config.maxConcurrentGames)
        assertEquals(120, config.maxStepsPerGame)
        assertEquals(listOf(768, 512, 256), config.hiddenLayers)
        assertEquals(0.0005, config.learningRate)
        assertEquals("checkpoints/long-train", config.checkpointDirectory)
    }
    
    @Test
    fun testLoadProfileEvalOnly() {
        val config = ConfigParser.loadProfile("eval-only")
        
        assertEquals(500, config.evaluationGames)
        assertEquals(1, config.maxConcurrentGames)
        assertEquals(12345L, config.seed)
        assertEquals("checkpoints/eval-only", config.checkpointDirectory)
    }
    
    @Test
    fun testLoadProfileUnknown() {
        assertFailsWith<IllegalArgumentException> {
            ConfigParser.loadProfile("unknown-profile")
        }
    }
    
    @Test
    fun testParseYamlBasic() {
        val yaml = """
            learningRate: 0.002
            batchSize: 128
            gamesPerCycle: 25
            hiddenLayers: 512,256,128
            seed: 54321
        """.trimIndent()
        
        val config = ConfigParser.parseYaml(yaml)
        
        assertEquals(0.002, config.learningRate)
        assertEquals(128, config.batchSize)
        assertEquals(25, config.gamesPerCycle)
        assertEquals(listOf(512, 256, 128), config.hiddenLayers)
        assertEquals(54321L, config.seed)
    }
    
    @Test
    fun testParseYamlIgnoresComments() {
        val yaml = """
            # This is a comment
            learningRate: 0.001
            # Another comment
            batchSize: 64
        """.trimIndent()
        
        val config = ConfigParser.parseYaml(yaml)
        
        assertEquals(0.001, config.learningRate)
        assertEquals(64, config.batchSize)
    }
    
    @Test
    fun testParseJsonBasic() {
        val json = """
            {
                "learningRate": 0.003,
                "batchSize": 32,
                "hiddenLayers": [256, 128, 64],
                "seed": 98765
            }
        """.trimIndent()
        
        val config = ConfigParser.parseJson(json)
        
        assertEquals(0.003, config.learningRate)
        assertEquals(32, config.batchSize)
        assertEquals(listOf(256, 128, 64), config.hiddenLayers)
        assertEquals(98765L, config.seed)
    }
    
    @Test
    fun testGetAvailableProfiles() {
        val profiles = ConfigParser.getAvailableProfiles()
        
        assertEquals(3, profiles.size)
        assertTrue(profiles.contains("fast-debug"))
        assertTrue(profiles.contains("long-train"))
        assertTrue(profiles.contains("eval-only"))
    }
}