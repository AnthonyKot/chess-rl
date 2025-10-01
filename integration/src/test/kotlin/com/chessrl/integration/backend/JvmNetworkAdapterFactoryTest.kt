package com.chessrl.integration.backend

import kotlin.test.*

class JvmNetworkAdapterFactoryTest {
    
    private val config = BackendConfig(
        inputSize = 839,
        outputSize = 4096,
        hiddenLayers = listOf(512, 256),
        learningRate = 0.001,
        batchSize = 32
    )
    
    @Test
    fun testCreateManualAdapter() {
        val adapter = JvmNetworkAdapterFactory.createAdapter(BackendType.MANUAL, config)
        assertEquals("manual", adapter.getBackendName())
        assertTrue(adapter is ManualNetworkAdapter)
    }
    
    @Test
    fun testCreateDl4jAdapter() {
        val adapter = JvmNetworkAdapterFactory.createAdapter(BackendType.DL4J, config)
        assertEquals("dl4j", adapter.getBackendName())
        assertTrue(adapter is Dl4jNetworkAdapter)
    }
    
    @Test
    fun testCreateAdapterWithFallback() {
        val (adapter, actualBackend) = JvmNetworkAdapterFactory.createAdapterWithFallback(
            BackendType.DL4J, 
            config
        )
        
        assertEquals(BackendType.DL4J, actualBackend)
        assertEquals("dl4j", adapter.getBackendName())
    }
    
    @Test
    fun testValidateAdapter() {
        val adapter = JvmNetworkAdapterFactory.createAdapter(BackendType.DL4J, config)
        val result = JvmNetworkAdapterFactory.validateAdapter(adapter)
        
        assertTrue(result.isValid, "Adapter validation should pass: ${result.issues}")
        assertEquals("dl4j", result.backend)
    }
    
    @Test
    fun testAdapterForwardPass() {
        val adapter = JvmNetworkAdapterFactory.createAdapter(BackendType.DL4J, config)
        val input = DoubleArray(config.inputSize) { 0.1 }
        
        val output = adapter.forward(input)
        
        assertEquals(config.outputSize, output.size)
        assertTrue(output.all { it.isFinite() })
    }
    
    @Test
    fun testAdapterTraining() {
        val adapter = JvmNetworkAdapterFactory.createAdapter(BackendType.DL4J, config)
        val inputs = Array(2) { DoubleArray(config.inputSize) { kotlin.random.Random.nextDouble() } }
        val targets = Array(2) { DoubleArray(config.outputSize) { kotlin.random.Random.nextDouble() } }
        
        val loss = adapter.trainBatch(inputs, targets)
        
        assertTrue(loss.isFinite())
        assertTrue(loss >= 0.0)
    }
}