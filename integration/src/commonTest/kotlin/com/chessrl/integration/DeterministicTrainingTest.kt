package com.chessrl.integration

import com.chessrl.rl.Experience
import kotlin.test.*

/**
 * Tests for deterministic training system and seed management
 */
class DeterministicTrainingTest {
    
    @Test
    fun testSeedManagerInitialization() {
        val seedManager = SeedManager.initializeWithSeed(12345L)
        
        assertEquals(12345L, seedManager.getMasterSeed())
        assertTrue(seedManager.isDeterministic())
        
        val summary = seedManager.getSeedSummary()
        assertTrue(summary.isSeeded)
        assertEquals(12345L, summary.masterSeed)
        assertTrue(summary.isDeterministicMode)
        assertTrue(summary.componentCount > 0)
    }
    
    @Test
    fun testSeedValidation() {
        val seedManager = SeedManager.initializeWithSeed(54321L)
        
        val validation = seedManager.validateSeedConsistency()
        assertTrue(validation.isValid, "Seed validation should pass")
        assertTrue(validation.issues.isEmpty(), "No validation issues expected")
        assertNotNull(validation.masterSeed)
        assertEquals(54321L, validation.masterSeed)
    }
    
    @Test
    fun testComponentRandomGenerators() {
        val seedManager = SeedManager.initializeWithSeed(98765L)
        
        // Test that different component random generators produce different values
        val nnRandom = seedManager.getNeuralNetworkRandom()
        val explorationRandom = seedManager.getExplorationRandom()
        val replayRandom = seedManager.getReplayBufferRandom()
        
        val nnValue = nnRandom.nextDouble()
        val explorationValue = explorationRandom.nextDouble()
        val replayValue = replayRandom.nextDouble()
        
        // Values should be different (extremely unlikely to be equal)
        assertNotEquals(nnValue, explorationValue)
        assertNotEquals(nnValue, replayValue)
        assertNotEquals(explorationValue, replayValue)
        
        // All values should be in valid range [0, 1)
        assertTrue(nnValue >= 0.0 && nnValue < 1.0)
        assertTrue(explorationValue >= 0.0 && explorationValue < 1.0)
        assertTrue(replayValue >= 0.0 && replayValue < 1.0)
    }
    
    @Test
    fun testSeedConfigurationSerialization() {
        val seedManager = SeedManager.initializeWithSeed(11111L)
        
        // Get initial configuration
        val originalConfig = seedManager.getSeedConfiguration()
        
        // Create new seed manager and restore configuration
        val newSeedManager = SeedManager.getInstance()
        newSeedManager.restoreSeedConfiguration(originalConfig)
        
        // Verify restoration
        val restoredConfig = newSeedManager.getSeedConfiguration()
        
        assertEquals(originalConfig.masterSeed, restoredConfig.masterSeed)
        assertEquals(originalConfig.isDeterministicMode, restoredConfig.isDeterministicMode)
        assertEquals(originalConfig.componentSeeds, restoredConfig.componentSeeds)
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
        assertTrue(validation.isValid, "Valid configuration should pass validation")
        assertTrue(validation.errors.isEmpty(), "No errors expected for valid config")
        
        // Invalid configuration
        val invalidConfig = TrainingConfiguration(
            episodes = -1, // Invalid
            batchSize = 0, // Invalid
            learningRate = 2.0, // Invalid
            explorationRate = -0.5 // Invalid
        )
        
        val invalidValidation = invalidConfig.validate()
        assertFalse(invalidValidation.isValid, "Invalid configuration should fail validation")
        assertTrue(invalidValidation.errors.isNotEmpty(), "Errors expected for invalid config")
    }
    
    @Test
    fun testDeterministicTestMode() {
        val testSeed = 99999L
        
        // Create test configuration
        val testConfig = ConfigurationParser.createTestConfiguration(testSeed)
        
        assertEquals(testSeed, testConfig.seed)
        assertTrue(testConfig.deterministicMode)
        assertTrue(testConfig.enableDebugMode)
        assertEquals("test-config", testConfig.configurationName)
        
        // Validate test configuration
        val validation = testConfig.validate()
        assertTrue(validation.isValid, "Test configuration should be valid")
    }
    
    @Test
    fun testSeededOperations() {
        val seedManager = SeedManager.initializeWithSeed(77777L)
        val random = seedManager.getGeneralRandom()
        
        // Test seeded Gaussian generation
        val gaussian1 = SeededOperations.nextGaussian(random)
        val gaussian2 = SeededOperations.nextGaussian(random)
        
        assertNotEquals(gaussian1, gaussian2, "Sequential Gaussian values should differ")
        
        // Test seeded array shuffling
        val originalArray = arrayOf(1, 2, 3, 4, 5)
        val shuffledArray = originalArray.copyOf()
        SeededOperations.shuffleArray(shuffledArray, random)
        
        // Arrays should have same elements but potentially different order
        assertEquals(originalArray.toSet(), shuffledArray.toSet())
        
        // Test seeded weight generation
        val weights = SeededOperations.generateWeights(10, WeightInitType.HE, random)
        
        assertEquals(10, weights.size)
        assertTrue(weights.all { it.isFinite() }, "All weights should be finite")
    }
    
    @Test
    fun testSeededExplorationStrategy() {
        val seedManager = SeedManager.initializeWithSeed(33333L)
        val random = seedManager.getExplorationRandom()
        
        val strategy = SeededEpsilonGreedyStrategy<Int>(
            epsilon = 0.5,
            random = random
        )
        
        val validActions = listOf(1, 2, 3, 4, 5)
        val actionValues = mapOf(1 to 0.1, 2 to 0.8, 3 to 0.3, 4 to 0.2, 5 to 0.6)
        
        // Select multiple actions to test both exploration and exploitation
        val selectedActions = mutableListOf<Int>()
        repeat(20) {
            val action = strategy.selectAction(validActions, actionValues, random)
            selectedActions.add(action)
            assertTrue(action in validActions, "Selected action should be valid")
        }
        
        // Should have some variety in selected actions
        assertTrue(selectedActions.toSet().size > 1, "Should explore different actions")
        
        // Test exploration rate updates
        val initialRate = strategy.getExplorationRate()
        strategy.updateExploration(1)
        val updatedRate = strategy.getExplorationRate()
        
        assertTrue(updatedRate <= initialRate, "Exploration rate should decay or stay same")
    }
    
    @Test
    fun testSeededExperienceBuffer() {
        val seedManager = SeedManager.initializeWithSeed(44444L)
        val random = seedManager.getReplayBufferRandom()
        
        val buffer = SeededCircularExperienceBuffer<DoubleArray, Int>(maxSize = 10, random = random)
        
        // Add some experiences
        repeat(15) { i ->
            val experience = Experience(
                state = doubleArrayOf(i.toDouble()),
                action = i,
                reward = i * 0.1,
                nextState = doubleArrayOf((i + 1).toDouble()),
                done = i == 14
            )
            buffer.add(experience)
        }
        
        assertEquals(10, buffer.size(), "Buffer should be at max capacity")
        assertTrue(buffer.isFull(), "Buffer should be full")
        
        // Test deterministic sampling
        val sample1 = buffer.sample(5, random)
        val sample2 = buffer.sample(5, random)
        
        assertEquals(5, sample1.size)
        assertEquals(5, sample2.size)
        
        // Samples should be different (due to shuffling)
        assertNotEquals(sample1, sample2, "Different samples should be different")
    }
    
    @Test
    fun testReproducibilityWithSameSeed() {
        val testSeed = 88888L
        
        // First run
        val seedManager1 = SeedManager.initializeWithSeed(testSeed)
        val random1 = seedManager1.getNeuralNetworkRandom()
        val values1 = List(10) { random1.nextDouble() }
        
        // Second run with same seed
        val seedManager2 = SeedManager.initializeWithSeed(testSeed)
        val random2 = seedManager2.getNeuralNetworkRandom()
        val values2 = List(10) { random2.nextDouble() }
        
        // Values should be identical
        assertEquals(values1, values2, "Same seed should produce identical random sequences")
    }
    
    @Test
    fun testCheckpointMetadataWithSeedConfiguration() {
        val seedManager = SeedManager.initializeWithSeed(66666L)
        val seedConfig = seedManager.getSeedConfiguration()
        
        val trainingConfig = TrainingConfiguration(
            seed = 66666L,
            deterministicMode = true,
            episodes = 50
        )
        
        val metadata = CheckpointMetadata(
            cycle = 10,
            performance = 0.85,
            description = "Test checkpoint with seed config",
            seedConfiguration = seedConfig,
            trainingConfiguration = trainingConfig
        )
        
        assertNotNull(metadata.seedConfiguration)
        assertEquals(66666L, metadata.seedConfiguration?.masterSeed)
        assertTrue(metadata.seedConfiguration?.isDeterministicMode == true)
        
        assertNotNull(metadata.trainingConfiguration)
        assertEquals(50, metadata.trainingConfiguration?.episodes)
    }
}
