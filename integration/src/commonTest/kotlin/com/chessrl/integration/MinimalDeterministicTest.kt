package com.chessrl.integration

import kotlin.test.*

/**
 * Tests for the minimal deterministic seeding system
 */
class MinimalDeterministicTest {
    
    @Test
    fun testSeedManagerBasicFunctionality() {
        val seedManager = SeedManager.initializeWithSeed(12345L)
        
        assertEquals(12345L, seedManager.getMasterSeed())
        assertTrue(seedManager.isDeterministic())
        
        val summary = seedManager.getSeedSummary()
        assertTrue(summary.isSeeded)
        assertEquals(12345L, summary.masterSeed)
        assertTrue(summary.isDeterministicMode)
    }
    
    @Test
    fun testComponentRandomGenerators() {
        val seedManager = SeedManager.initializeWithSeed(54321L)
        
        val nnRandom = seedManager.getNeuralNetworkRandom()
        val explorationRandom = seedManager.getExplorationRandom()
        val replayRandom = seedManager.getReplayBufferRandom()
        
        // Test that generators produce values in valid range
        val nnValue = nnRandom.nextDouble()
        val explorationValue = explorationRandom.nextDouble()
        val replayValue = replayRandom.nextDouble()
        
        assertTrue(nnValue >= 0.0 && nnValue < 1.0)
        assertTrue(explorationValue >= 0.0 && explorationValue < 1.0)
        assertTrue(replayValue >= 0.0 && replayValue < 1.0)
        
        // Values should be different (extremely unlikely to be equal)
        assertNotEquals(nnValue, explorationValue)
        assertNotEquals(nnValue, replayValue)
        assertNotEquals(explorationValue, replayValue)
    }
    
    @Test
    fun testDeterministicWeightGeneration() {
        val seedManager = SeedManager.initializeWithSeed(98765L)
        val random = seedManager.getNeuralNetworkRandom()
        
        // Generate weights with different initialization types
        val heWeights = SeededOperations.generateWeights(10, WeightInitType.HE, random)
        val xavierWeights = SeededOperations.generateWeights(10, WeightInitType.XAVIER, random)
        val uniformWeights = SeededOperations.generateWeights(10, WeightInitType.UNIFORM, random)
        val zeroWeights = SeededOperations.generateWeights(10, WeightInitType.ZERO, random)
        
        // Check sizes
        assertEquals(10, heWeights.size)
        assertEquals(10, xavierWeights.size)
        assertEquals(10, uniformWeights.size)
        assertEquals(10, zeroWeights.size)
        
        // Check that all weights are finite
        assertTrue(heWeights.all { it.isFinite() })
        assertTrue(xavierWeights.all { it.isFinite() })
        assertTrue(uniformWeights.all { it.isFinite() })
        assertTrue(zeroWeights.all { it.isFinite() })
        
        // Zero weights should all be zero
        assertTrue(zeroWeights.all { it == 0.0 })
        
        // Other weights should have some variation
        assertTrue(heWeights.toSet().size > 1)
        assertTrue(xavierWeights.toSet().size > 1)
        assertTrue(uniformWeights.toSet().size > 1)
    }
    
    @Test
    fun testSeededOperations() {
        val seedManager = SeedManager.initializeWithSeed(11111L)
        val random = seedManager.getGeneralRandom()
        
        // Test Gaussian generation
        val gaussian1 = SeededOperations.nextGaussian(random)
        val gaussian2 = SeededOperations.nextGaussian(random)
        
        assertTrue(gaussian1.isFinite())
        assertTrue(gaussian2.isFinite())
        assertNotEquals(gaussian1, gaussian2)
        
        // Test array shuffling
        val originalArray = arrayOf(1, 2, 3, 4, 5)
        val shuffledArray = originalArray.copyOf()
        SeededOperations.shuffleArray(shuffledArray, random)
        
        // Should have same elements
        assertEquals(originalArray.toSet(), shuffledArray.toSet())
        
        // Test list shuffling
        val originalList = mutableListOf(1, 2, 3, 4, 5)
        val shuffledList = originalList.toMutableList()
        SeededOperations.shuffleList(shuffledList, random)
        
        // Should have same elements
        assertEquals(originalList.toSet(), shuffledList.toSet())
        
        // Test sampling
        val sampleArray = arrayOf(10, 20, 30, 40, 50)
        val sample = SeededOperations.sampleFromArray(sampleArray, 3, random)
        
        assertEquals(3, sample.size)
        assertTrue(sample.all { it in sampleArray })
    }
    
    @Test
    fun testReproducibilityWithSameSeed() {
        val testSeed = 77777L
        
        // First run
        val seedManager1 = SeedManager.initializeWithSeed(testSeed)
        val random1 = seedManager1.getNeuralNetworkRandom()
        val values1 = List(10) { random1.nextDouble() }
        
        // Second run with same seed
        val seedManager2 = SeedManager.initializeWithSeed(testSeed)
        val random2 = seedManager2.getNeuralNetworkRandom()
        val values2 = List(10) { random2.nextDouble() }
        
        // Values should be identical
        assertEquals(values1, values2)
    }
    
    @Test
    fun testSeedConfigurationSerialization() {
        val seedManager = SeedManager.initializeWithSeed(33333L)
        
        // Get original configuration
        val originalConfig = seedManager.getSeedConfiguration()
        
        // Create new seed manager and restore
        val newSeedManager = SeedManager.getInstance()
        newSeedManager.restoreSeedConfiguration(originalConfig)
        
        // Verify restoration
        val restoredConfig = newSeedManager.getSeedConfiguration()
        
        assertEquals(originalConfig.masterSeed, restoredConfig.masterSeed)
        assertEquals(originalConfig.isDeterministicMode, restoredConfig.isDeterministicMode)
        assertEquals(originalConfig.componentSeeds, restoredConfig.componentSeeds)
    }
    
    @Test
    fun testSeedValidation() {
        val seedManager = SeedManager.initializeWithSeed(44444L)
        
        val validation = seedManager.validateSeedConsistency()
        
        assertTrue(validation.isValid)
        assertTrue(validation.issues.isEmpty())
        assertEquals(44444L, validation.masterSeed)
        assertTrue(validation.componentSeeds.isNotEmpty())
    }
    
    @Test
    fun testDeterministicTestMode() {
        val seedManager = SeedManager.getInstance()
        
        seedManager.enableDeterministicTestMode(99999L)
        
        assertTrue(seedManager.isDeterministic())
        assertEquals(99999L, seedManager.getMasterSeed())
        
        val validation = seedManager.validateSeedConsistency()
        assertTrue(validation.isValid)
    }
    
    @Test
    fun testTrainingConfigurationValidation() {
        // Valid configuration
        val validConfig = TrainingConfiguration(
            seed = 12345L,
            deterministicMode = true,
            episodes = 100,
            batchSize = 32,
            learningRate = 0.001,
            explorationRate = 0.1
        )
        
        val validation = validConfig.validate()
        assertTrue(validation.isValid)
        assertTrue(validation.errors.isEmpty())
        
        // Invalid configuration
        val invalidConfig = TrainingConfiguration(
            episodes = -1,
            batchSize = 0,
            learningRate = 2.0,
            explorationRate = -0.5
        )
        
        val invalidValidation = invalidConfig.validate()
        assertFalse(invalidValidation.isValid)
        assertTrue(invalidValidation.errors.isNotEmpty())
    }
    
    @Test
    fun testConfigurationBuilder() {
        val config = TrainingConfigurationBuilder()
            .seed(55555L)
            .deterministicMode(true)
            .episodes(500)
            .batchSize(64)
            .learningRate(0.002)
            .name("test-config")
            .build()
        
        assertEquals(55555L, config.seed)
        assertTrue(config.deterministicMode)
        assertEquals(500, config.episodes)
        assertEquals(64, config.batchSize)
        assertEquals(0.002, config.learningRate)
        assertEquals("test-config", config.configurationName)
    }
}