package com.chessrl.nn

import kotlin.test.*

/**
 * Test for the training infrastructure demo
 */
class TrainingInfrastructureDemoTest {
    
    @Test
    fun testDemoRuns() {
        // Test that the demo runs without throwing exceptions
        assertDoesNotThrow {
            TrainingInfrastructureDemo.runDemo()
        }
    }
    
    @Test
    fun testIndividualDemoComponents() {
        // Test that we can create and use the main components
        
        // Test dataset creation
        val inputs = Array(10) { i -> doubleArrayOf(i.toDouble()) }
        val targets = Array(10) { i -> doubleArrayOf(i * 2.0) }
        val dataset = InMemoryDataset(inputs, targets)
        
        assertEquals(10, dataset.size())
        assertTrue(dataset.hasNext())
        
        // Test network creation with advanced features
        val layers = listOf(
            DenseLayer(1, 5, ReLUActivation()),
            DenseLayer(5, 1, LinearActivation())
        )
        
        val network = FeedforwardNetwork(
            _layers = layers,
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(0.01),
            regularization = L2Regularization(0.001),
            learningRateScheduler = ExponentialDecayScheduler(0.95, 10)
        )
        
        // Test training with comprehensive features
        val history = network.train(
            dataset = dataset,
            epochs = 5,
            batchSize = 5,
            validationDataset = null,
            validationFrequency = 1
        )
        
        assertEquals(5, history.totalEpochs)
        assertTrue(history.metrics.isNotEmpty())
        
        // Test evaluation
        val metrics = network.evaluate(dataset)
        assertEquals(10, metrics.sampleCount)
        assertTrue(metrics.loss >= 0)
        
        // Test serialization
        val serializer = SimpleModelSerializer()
        val serializedData = serializer.serialize(network)
        assertTrue(serializedData.isNotEmpty())
        
        val networkData = serializer.deserialize(serializedData)
        assertEquals(1, networkData.config.inputSize)
        assertEquals(1, networkData.config.outputSize)
    }
    
    @Test
    fun testHyperparameterUtilities() {
        // Test grid search
        val gridConfigs = HyperparameterUtils.gridSearch(
            learningRates = listOf(0.01, 0.1),
            batchSizes = listOf(16, 32),
            hiddenLayerConfigs = listOf(listOf(10)),
            optimizers = listOf("SGD"),
            regularizationTypes = listOf(null),
            epochs = 10
        )
        
        assertEquals(4, gridConfigs.size) // 2 * 2 * 1 * 1 * 1
        
        // Test random search
        val randomConfigs = HyperparameterUtils.randomSearch(count = 3, epochs = 10)
        assertEquals(3, randomConfigs.size)
        assertTrue(randomConfigs.all { it.epochs == 10 })
        
        // Test learning rate scheduling configs
        val schedulingConfigs = HyperparameterUtils.learningRateSchedulingConfigs(
            baseLearningRate = 0.01,
            epochs = 50
        )
        
        assertEquals(4, schedulingConfigs.size)
        assertTrue(schedulingConfigs.all { it.learningRate == 0.01 })
        assertTrue(schedulingConfigs.all { it.epochs == 50 })
    }
    
    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Expected no exception, but got: ${e.message}")
        }
    }
}