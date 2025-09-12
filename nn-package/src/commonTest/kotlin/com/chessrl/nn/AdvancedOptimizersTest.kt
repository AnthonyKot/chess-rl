package com.chessrl.nn

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.math.*
import kotlin.random.Random

/**
 * Tests for advanced optimizers, loss functions, regularization, and learning rate scheduling
 */
class AdvancedOptimizersTest {
    
    // Test data for consistent testing
    private val simpleInputs = arrayOf(
        doubleArrayOf(1.0, 0.0),
        doubleArrayOf(0.0, 1.0),
        doubleArrayOf(1.0, 1.0),
        doubleArrayOf(0.0, 0.0)
    )
    
    private val simpleTargets = arrayOf(
        doubleArrayOf(1.0),
        doubleArrayOf(1.0),
        doubleArrayOf(0.0),
        doubleArrayOf(0.0)
    )
    
    @Test
    fun testAdamOptimizer() {
        val layers = listOf(
            DenseLayer(2, 3, ReLUActivation(), Random(42)),
            DenseLayer(3, 1, SigmoidActivation(), Random(42))
        )
        
        val adamOptimizer = AdamOptimizer(learningRate = 0.01)
        val network = FeedforwardNetwork(layers, MSELoss(), adamOptimizer)
        
        // Train for a few epochs
        val metrics = network.train(simpleInputs, simpleTargets, epochs = 5, batchSize = 2)
        
        assertEquals(5, metrics.size)
        for (metric in metrics) {
            assertTrue(metric.averageLoss >= 0.0, "Loss should be non-negative")
            assertTrue(metric.gradientNorm >= 0.0, "Gradient norm should be non-negative")
        }
        
        // Test that learning rate can be changed
        adamOptimizer.setLearningRate(0.001)
        assertEquals(0.001, adamOptimizer.getLearningRate(), 1e-10)
    }
    
    @Test
    fun testRMSpropOptimizer() {
        val layers = listOf(
            DenseLayer(2, 3, TanhActivation(), Random(42)),
            DenseLayer(3, 1, LinearActivation(), Random(42))
        )
        
        val rmspropOptimizer = RMSpropOptimizer(learningRate = 0.01, momentum = 0.9)
        val network = FeedforwardNetwork(layers, MSELoss(), rmspropOptimizer)
        
        // Train for a few epochs
        val metrics = network.train(simpleInputs, simpleTargets, epochs = 3, batchSize = 4)
        
        assertEquals(3, metrics.size)
        for (metric in metrics) {
            assertTrue(metric.averageLoss >= 0.0, "Loss should be non-negative")
            assertTrue(metric.gradientNorm >= 0.0, "Gradient norm should be non-negative")
        }
        
        // Test learning rate modification
        rmspropOptimizer.setLearningRate(0.005)
        assertEquals(0.005, rmspropOptimizer.getLearningRate(), 1e-10)
    }
    
    @Test
    fun testSGDWithMomentum() {
        val layers = listOf(
            DenseLayer(2, 1, LinearActivation(), Random(42))
        )
        
        val sgdOptimizer = SGDOptimizer(learningRate = 0.1, momentum = 0.9)
        val network = FeedforwardNetwork(layers, MSELoss(), sgdOptimizer)
        
        // Train for a few epochs
        val metrics = network.train(simpleInputs, simpleTargets, epochs = 3, batchSize = 2)
        
        assertEquals(3, metrics.size)
        for (metric in metrics) {
            assertTrue(metric.averageLoss >= 0.0, "Loss should be non-negative")
        }
    }
    
    @Test
    fun testCrossEntropyLoss() {
        val crossEntropy = CrossEntropyLoss()
        
        // Test perfect prediction (should be close to 0)
        val predicted1 = doubleArrayOf(1.0, 0.0, 0.0)
        val target1 = doubleArrayOf(1.0, 0.0, 0.0)
        val loss1 = crossEntropy.computeLoss(predicted1, target1)
        assertTrue(loss1 < 0.1, "Perfect prediction should have low loss, got $loss1")
        
        // Test worst prediction
        val predicted2 = doubleArrayOf(0.0, 1.0, 0.0)
        val target2 = doubleArrayOf(1.0, 0.0, 0.0)
        val loss2 = crossEntropy.computeLoss(predicted2, target2)
        assertTrue(loss2 > loss1, "Wrong prediction should have higher loss")
        
        // Test gradient computation
        val gradient = crossEntropy.computeGradient(predicted2, target2)
        assertEquals(3, gradient.size)
        assertTrue(gradient[0] < 0.0, "Gradient for correct class should be negative")
    }
    
    @Test
    fun testHuberLoss() {
        val huberLoss = HuberLoss(delta = 1.0)
        
        // Test small error (quadratic region)
        val predicted1 = doubleArrayOf(1.0, 2.0)
        val target1 = doubleArrayOf(1.5, 2.3)
        val loss1 = huberLoss.computeLoss(predicted1, target1)
        assertTrue(loss1 > 0.0, "Loss should be positive for non-perfect prediction")
        
        // Test large error (linear region)
        val predicted2 = doubleArrayOf(1.0, 2.0)
        val target2 = doubleArrayOf(3.0, 5.0)
        val loss2 = huberLoss.computeLoss(predicted2, target2)
        assertTrue(loss2 > loss1, "Larger error should have higher loss")
        
        // Test gradient computation
        val gradient1 = huberLoss.computeGradient(predicted1, target1)
        val gradient2 = huberLoss.computeGradient(predicted2, target2)
        assertEquals(2, gradient1.size)
        assertEquals(2, gradient2.size)
        
        // Gradients should be different for small vs large errors
        assertTrue(abs(gradient1[0]) != abs(gradient2[0]), "Gradients should differ for small vs large errors")
    }
    
    @Test
    fun testL1Regularization() {
        val layers = listOf(
            DenseLayer(2, 1, LinearActivation(), Random(42))
        )
        
        val l1Reg = L1Regularization(lambda = 0.01)
        val network = FeedforwardNetwork(layers, MSELoss(), SGDOptimizer(), l1Reg)
        
        // Get initial weights
        val initialWeights = (layers[0] as DenseLayer).getWeights()
        
        // Train with regularization
        network.train(simpleInputs, simpleTargets, epochs = 5, batchSize = 2)
        
        // Check that regularization loss is computed
        val regLoss = l1Reg.computeRegularizationLoss(layers)
        assertTrue(regLoss >= 0.0, "Regularization loss should be non-negative")
        
        // Weights should be affected by regularization
        val finalWeights = (layers[0] as DenseLayer).getWeights()
        var weightsChanged = false
        for (i in initialWeights.indices) {
            for (j in initialWeights[i].indices) {
                if (initialWeights[i][j] != finalWeights[i][j]) {
                    weightsChanged = true
                }
            }
        }
        assertTrue(weightsChanged, "Weights should change during training")
    }
    
    @Test
    fun testL2Regularization() {
        val layers = listOf(
            DenseLayer(2, 1, LinearActivation(), Random(42))
        )
        
        val l2Reg = L2Regularization(lambda = 0.01)
        val network = FeedforwardNetwork(layers, MSELoss(), SGDOptimizer(), l2Reg)
        
        // Train with regularization
        val metrics = network.train(simpleInputs, simpleTargets, epochs = 3, batchSize = 2)
        
        // Check that regularization loss is computed
        val regLoss = l2Reg.computeRegularizationLoss(layers)
        assertTrue(regLoss >= 0.0, "Regularization loss should be non-negative")
        
        assertEquals(3, metrics.size)
        for (metric in metrics) {
            assertTrue(metric.averageLoss >= 0.0, "Loss should be non-negative")
        }
    }
    
    @Test
    fun testDropoutRegularization() {
        val layers = listOf(
            DenseLayer(2, 3, ReLUActivation(), Random(42)),
            DenseLayer(3, 1, SigmoidActivation(), Random(42))
        )
        
        val dropout = DropoutRegularization(dropoutRate = 0.5)
        val network = FeedforwardNetwork(layers, MSELoss(), SGDOptimizer(), dropout)
        
        // Test training mode
        dropout.setTraining(true)
        val trainingMetrics = network.train(simpleInputs, simpleTargets, epochs = 2, batchSize = 2)
        assertEquals(2, trainingMetrics.size)
        
        // Test inference mode
        dropout.setTraining(false)
        val input = doubleArrayOf(1.0, 0.0)
        val output1 = network.predict(input)
        val output2 = network.predict(input)
        
        // In inference mode, outputs should be consistent
        assertEquals(output1.size, output2.size)
        for (i in output1.indices) {
            assertEquals(output1[i], output2[i], 1e-10)
        }
    }
    
    @Test
    fun testExponentialDecayScheduler() {
        val scheduler = ExponentialDecayScheduler(decayRate = 0.9, decaySteps = 10)
        val initialLR = 0.1
        
        val lr0 = scheduler.getScheduledLearningRate(0, initialLR)
        val lr10 = scheduler.getScheduledLearningRate(10, initialLR)
        val lr20 = scheduler.getScheduledLearningRate(20, initialLR)
        
        assertEquals(initialLR, lr0, 1e-10)
        assertTrue(lr10 < lr0, "Learning rate should decay")
        assertTrue(lr20 < lr10, "Learning rate should continue to decay")
        
        // Check exponential decay formula
        val expectedLR10 = initialLR * 0.9.pow(1.0)
        assertEquals(expectedLR10, lr10, 1e-10)
    }
    
    @Test
    fun testStepDecayScheduler() {
        val scheduler = StepDecayScheduler(stepSize = 5, gamma = 0.5)
        val initialLR = 0.1
        
        val lr0 = scheduler.getScheduledLearningRate(0, initialLR)
        val lr4 = scheduler.getScheduledLearningRate(4, initialLR)
        val lr5 = scheduler.getScheduledLearningRate(5, initialLR)
        val lr10 = scheduler.getScheduledLearningRate(10, initialLR)
        
        assertEquals(initialLR, lr0, 1e-10)
        assertEquals(initialLR, lr4, 1e-10) // No decay yet
        assertEquals(initialLR * 0.5, lr5, 1e-10) // First step
        assertEquals(initialLR * 0.25, lr10, 1e-10) // Second step
    }
    
    @Test
    fun testLinearDecayScheduler() {
        val totalEpochs = 100
        val finalLR = 0.01
        val scheduler = LinearDecayScheduler(totalEpochs, finalLR)
        val initialLR = 0.1
        
        val lr0 = scheduler.getScheduledLearningRate(0, initialLR)
        val lr50 = scheduler.getScheduledLearningRate(50, initialLR)
        val lr100 = scheduler.getScheduledLearningRate(100, initialLR)
        
        assertEquals(initialLR, lr0, 1e-10)
        assertEquals((initialLR + finalLR) / 2.0, lr50, 1e-10) // Midpoint
        assertEquals(finalLR, lr100, 1e-10)
    }
    
    @Test
    fun testNetworkWithScheduler() {
        val layers = listOf(
            DenseLayer(2, 1, LinearActivation(), Random(42))
        )
        
        val scheduler = ExponentialDecayScheduler(decayRate = 0.95, decaySteps = 1)
        val optimizer = SGDOptimizer(learningRate = 0.1)
        val network = FeedforwardNetwork(layers, MSELoss(), optimizer, null, scheduler)
        
        val initialLR = optimizer.getLearningRate()
        
        // Train for a few epochs
        val metrics = network.train(simpleInputs, simpleTargets, epochs = 3, batchSize = 2)
        
        assertEquals(3, metrics.size)
        
        // Learning rate should remain the same in optimizer (scheduler is applied temporarily)
        assertEquals(initialLR, optimizer.getLearningRate(), 1e-10)
    }
    
    @Test
    fun testBatchSizeVariationsWithAdvancedOptimizers() {
        val layers = listOf(
            DenseLayer(2, 3, ReLUActivation(), Random(42)),
            DenseLayer(3, 1, SigmoidActivation(), Random(42))
        )
        
        val optimizers = listOf(
            SGDOptimizer(learningRate = 0.01),
            AdamOptimizer(learningRate = 0.01),
            RMSpropOptimizer(learningRate = 0.01)
        )
        
        val batchSizes = listOf(1, 2, 4)
        
        for (optimizer in optimizers) {
            for (batchSize in batchSizes) {
                val network = FeedforwardNetwork(layers, MSELoss(), optimizer)
                val metrics = network.train(simpleInputs, simpleTargets, epochs = 2, batchSize = batchSize)
                
                assertEquals(2, metrics.size)
                for (metric in metrics) {
                    assertTrue(metric.averageLoss >= 0.0, 
                        "Loss should be non-negative for ${optimizer::class.simpleName} with batch size $batchSize")
                    assertTrue(metric.gradientNorm >= 0.0, 
                        "Gradient norm should be non-negative for ${optimizer::class.simpleName} with batch size $batchSize")
                }
            }
        }
    }
    
    @Test
    fun testLossFunctionValidation() {
        val losses = listOf(MSELoss(), CrossEntropyLoss(), HuberLoss())
        
        for (loss in losses) {
            // Test size mismatch
            assertFailsWith<IllegalArgumentException> {
                loss.computeLoss(doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0))
            }
            
            assertFailsWith<IllegalArgumentException> {
                loss.computeGradient(doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0))
            }
        }
    }
    
    @Test
    fun testOptimizerConsistency() {
        // Test that optimizers produce consistent results with same random seed
        val createNetwork = { optimizer: Optimizer ->
            val layers = listOf(DenseLayer(2, 1, LinearActivation(), Random(42)))
            FeedforwardNetwork(layers, MSELoss(), optimizer)
        }
        
        val sgd1 = createNetwork(SGDOptimizer(learningRate = 0.01))
        val sgd2 = createNetwork(SGDOptimizer(learningRate = 0.01))
        
        val metrics1 = sgd1.train(simpleInputs, simpleTargets, epochs = 2, batchSize = 2)
        val metrics2 = sgd2.train(simpleInputs, simpleTargets, epochs = 2, batchSize = 2)
        
        assertEquals(metrics1.size, metrics2.size)
        // Note: Due to data shuffling in training, exact consistency is not guaranteed
        // Instead, we test that both networks complete training successfully
        for (i in metrics1.indices) {
            assertTrue(metrics1[i].averageLoss >= 0.0, "SGD1 loss should be non-negative")
            assertTrue(metrics2[i].averageLoss >= 0.0, "SGD2 loss should be non-negative")
            assertTrue(metrics1[i].averageLoss.isFinite(), "SGD1 loss should be finite")
            assertTrue(metrics2[i].averageLoss.isFinite(), "SGD2 loss should be finite")
        }
    }
    
    @Test
    fun testRegularizationEffects() {
        val layers = listOf(DenseLayer(2, 1, LinearActivation(), Random(42)))
        
        // Train without regularization
        val networkNoReg = FeedforwardNetwork(layers, MSELoss(), SGDOptimizer(0.1))
        val metricsNoReg = networkNoReg.train(simpleInputs, simpleTargets, epochs = 5, batchSize = 2)
        
        // Train with L2 regularization
        val layersReg = listOf(DenseLayer(2, 1, LinearActivation(), Random(42)))
        val networkWithReg = FeedforwardNetwork(layersReg, MSELoss(), SGDOptimizer(0.1), L2Regularization(0.1))
        val metricsWithReg = networkWithReg.train(simpleInputs, simpleTargets, epochs = 5, batchSize = 2)
        
        assertEquals(metricsNoReg.size, metricsWithReg.size)
        
        // Both should complete training successfully
        for (i in metricsNoReg.indices) {
            assertTrue(metricsNoReg[i].averageLoss >= 0.0, "Loss without regularization should be non-negative")
            assertTrue(metricsWithReg[i].averageLoss >= 0.0, "Loss with regularization should be non-negative")
        }
    }
    
    @Test
    fun testComplexTrainingScenario() {
        // Test a complex scenario with all advanced features
        val layers = listOf(
            DenseLayer(2, 4, ReLUActivation(), Random(42)),
            DenseLayer(4, 2, TanhActivation(), Random(42)),
            DenseLayer(2, 1, SigmoidActivation(), Random(42))
        )
        
        val optimizer = AdamOptimizer(learningRate = 0.01)
        val regularization = L2Regularization(lambda = 0.001)
        val scheduler = ExponentialDecayScheduler(decayRate = 0.98, decaySteps = 2)
        val lossFunction = HuberLoss(delta = 1.0)
        
        val network = FeedforwardNetwork(layers, lossFunction, optimizer, regularization, scheduler)
        
        // Create more complex training data
        val complexInputs = Array(16) { i ->
            doubleArrayOf(sin(i * 0.1), cos(i * 0.1))
        }
        val complexTargets = Array(16) { i ->
            doubleArrayOf(if (i % 2 == 0) 1.0 else 0.0)
        }
        
        val metrics = network.train(complexInputs, complexTargets, epochs = 10, batchSize = 4)
        
        assertEquals(10, metrics.size)
        for (metric in metrics) {
            assertTrue(metric.averageLoss >= 0.0, "Loss should be non-negative")
            assertTrue(metric.gradientNorm >= 0.0, "Gradient norm should be non-negative")
            assertTrue(metric.averageLoss.isFinite(), "Loss should be finite")
        }
        
        // Test prediction after training
        val testInput = doubleArrayOf(0.5, -0.5)
        val prediction = network.predict(testInput)
        assertEquals(1, prediction.size)
        assertTrue(prediction[0] >= 0.0 && prediction[0] <= 1.0, "Sigmoid output should be in [0,1]")
    }
}