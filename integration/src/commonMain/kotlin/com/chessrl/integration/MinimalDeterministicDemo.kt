package com.chessrl.integration

import kotlin.random.Random

/**
 * Minimal demonstration of deterministic seeding system that works independently
 * of the complex integration layer. This shows the core concepts without dependencies.
 */
object MinimalDeterministicDemo {
    
    /**
     * Run the minimal deterministic demo
     */
    fun runDemo() {
        println("🎲 Minimal Deterministic Training Demo")
        println("=" * 50)
        
        // Demo 1: Basic seed manager functionality
        demonstrateSeedManager()
        
        // Demo 2: Deterministic neural network initialization
        demonstrateDeterministicNeuralNetwork()
        
        // Demo 3: Deterministic experience sampling
        demonstrateDeterministicSampling()
        
        // Demo 4: Reproducibility verification
        demonstrateReproducibility()
        
        println("\n✅ Minimal Deterministic Demo completed!")
    }
    
    /**
     * Demo 1: Basic seed manager functionality
     */
    private fun demonstrateSeedManager() {
        println("\n📋 Demo 1: Seed Manager Functionality")
        println("-" * 40)
        
        try {
            // Initialize with fixed seed
            val seedManager = SeedManager.initializeWithSeed(12345L)
            
            println("✅ Seed manager initialized")
            println("   Master seed: ${seedManager.getMasterSeed()}")
            println("   Deterministic mode: ${seedManager.isDeterministic()}")
            
            // Get component-specific random generators
            val nnRandom = seedManager.getNeuralNetworkRandom()
            val explorationRandom = seedManager.getExplorationRandom()
            val replayRandom = seedManager.getReplayBufferRandom()
            
            println("\n🎲 Component random values:")
            println("   Neural Network: ${nnRandom.nextDouble()}")
            println("   Exploration: ${explorationRandom.nextDouble()}")
            println("   Replay Buffer: ${replayRandom.nextDouble()}")
            
            // Validate seed consistency
            val validation = seedManager.validateSeedConsistency()
            println("\n🔍 Seed validation: ${if (validation.isValid) "✅ Valid" else "❌ Invalid"}")
            
        } catch (e: Exception) {
            println("❌ Demo 1 failed: ${e.message}")
        }
    }
    
    /**
     * Demo 2: Deterministic neural network initialization
     */
    private fun demonstrateDeterministicNeuralNetwork() {
        println("\n📋 Demo 2: Deterministic Neural Network Initialization")
        println("-" * 40)
        
        try {
            val seedManager = SeedManager.initializeWithSeed(54321L)
            val nnRandom = seedManager.getNeuralNetworkRandom()
            
            // Generate deterministic weights
            val layerSize = 10
            val weights1 = SeededOperations.generateWeights(layerSize, WeightInitType.HE, nnRandom)
            
            println("✅ Generated ${weights1.size} weights using He initialization")
            println("   First 5 weights: ${weights1.take(5).joinToString(", ") { "%.4f".format(it) }}")
            
            // Generate more weights with different initialization
            val weights2 = SeededOperations.generateWeights(layerSize, WeightInitType.XAVIER, nnRandom)
            
            println("✅ Generated ${weights2.size} weights using Xavier initialization")
            println("   First 5 weights: ${weights2.take(5).joinToString(", ") { "%.4f".format(it) }}")
            
            // Verify weights are different but deterministic
            val allFinite = weights1.all { it.isFinite() } && weights2.all { it.isFinite() }
            println("🔍 All weights finite: ${if (allFinite) "✅ Yes" else "❌ No"}")
            
        } catch (e: Exception) {
            println("❌ Demo 2 failed: ${e.message}")
        }
    }
    
    /**
     * Demo 3: Deterministic experience sampling
     */
    private fun demonstrateDeterministicSampling() {
        println("\n📋 Demo 3: Deterministic Experience Sampling")
        println("-" * 40)
        
        try {
            val seedManager = SeedManager.initializeWithSeed(98765L)
            val replayRandom = seedManager.getReplayBufferRandom()
            
            // Create sample data
            val experiences = (1..20).map { i ->
                MockExperience(
                    id = i,
                    state = doubleArrayOf(i.toDouble()),
                    action = i,
                    reward = i * 0.1,
                    done = i == 20
                )
            }
            
            println("✅ Created ${experiences.size} mock experiences")
            
            // Deterministic sampling
            val sampleIndices = (0 until experiences.size).toMutableList()
            SeededOperations.shuffleList(sampleIndices, replayRandom)
            
            val sample = sampleIndices.take(5).map { experiences[it] }
            
            println("✅ Sampled 5 experiences deterministically:")
            sample.forEach { exp ->
                println("   Experience ${exp.id}: reward=${exp.reward}")
            }
            
            // Test different sampling strategies
            val uniformSample = SeededOperations.sampleFromArray(experiences.toTypedArray(), 3, replayRandom)
            println("✅ Uniform sample of 3: ${uniformSample.map { it.id }}")
            
        } catch (e: Exception) {
            println("❌ Demo 3 failed: ${e.message}")
        }
    }
    
    /**
     * Demo 4: Reproducibility verification
     */
    private fun demonstrateReproducibility() {
        println("\n📋 Demo 4: Reproducibility Verification")
        println("-" * 40)
        
        try {
            val testSeed = 77777L
            
            // Run 1
            val result1 = runDeterministicSimulation(testSeed)
            
            // Run 2 with same seed
            val result2 = runDeterministicSimulation(testSeed)
            
            // Compare results
            val identical = result1.contentEquals(result2)
            
            println("✅ Run 1 results: ${result1.joinToString(", ") { "%.4f".format(it) }}")
            println("✅ Run 2 results: ${result2.joinToString(", ") { "%.4f".format(it) }}")
            println("🔍 Results identical: ${if (identical) "✅ Yes" else "❌ No"}")
            
            if (identical) {
                println("🎉 Reproducibility verified!")
            } else {
                println("⚠️ Reproducibility failed - check seed implementation")
            }
            
        } catch (e: Exception) {
            println("❌ Demo 4 failed: ${e.message}")
        }
    }
    
    /**
     * Run a deterministic simulation for reproducibility testing
     */
    private fun runDeterministicSimulation(seed: Long): DoubleArray {
        val seedManager = SeedManager.initializeWithSeed(seed)
        val random = seedManager.getGeneralRandom()
        
        // Simulate some stochastic operations
        val results = DoubleArray(5)
        
        // Neural network weight initialization
        results[0] = SeededOperations.nextGaussian(random)
        
        // Exploration decision
        results[1] = random.nextDouble()
        
        // Reward calculation with noise
        results[2] = 0.5 + SeededOperations.nextGaussian(random) * 0.1
        
        // Action selection probability
        results[3] = random.nextDouble()
        
        // Experience sampling weight
        results[4] = SeededOperations.nextGaussian(random) * 0.2
        
        return results
    }
    
    /**
     * Mock experience class for demonstration
     */
    data class MockExperience(
        val id: Int,
        val state: DoubleArray,
        val action: Int,
        val reward: Double,
        val done: Boolean
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            
            other as MockExperience
            
            if (id != other.id) return false
            if (!state.contentEquals(other.state)) return false
            if (action != other.action) return false
            if (reward != other.reward) return false
            if (done != other.done) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = id
            result = 31 * result + state.contentHashCode()
            result = 31 * result + action
            result = 31 * result + reward.hashCode()
            result = 31 * result + done.hashCode()
            return result
        }
    }
    
    /**
     * Utility function for creating separator lines
     */
    private operator fun String.times(count: Int): String {
        return this.repeat(count)
    }
}

/**
 * Entry point for the minimal demo
 */
fun main() {
    MinimalDeterministicDemo.runDemo()
}