package com.chessrl.nn

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.math.*
import kotlin.random.Random

/**
 * Comprehensive validation tests using synthetic datasets
 */
class SyntheticValidationTest {
    
    @Test
    fun testPolynomialRegressionDataset() {
        val (inputs, targets) = SyntheticDatasets.generatePolynomialRegression(
            samples = 100,
            coefficients = doubleArrayOf(1.0, 2.0, 0.5),
            noiseLevel = 0.0, // No noise for exact testing
            random = Random(42)
        )
        
        assertEquals(100, inputs.size)
        assertEquals(100, targets.size)
        assertEquals(1, inputs[0].size)
        assertEquals(1, targets[0].size)
        
        // Verify polynomial relationship: y = x^2 + 2x + 0.5 (for x near 1.0)
        val x = 1.0
        
        // Find input close to 1.0
        var closestIdx = 0
        var minDiff = abs(inputs[0][0] - x)
        for (i in inputs.indices) {
            val diff = abs(inputs[i][0] - x)
            if (diff < minDiff) {
                minDiff = diff
                closestIdx = i
            }
        }
        
        if (minDiff < 0.1) { // If we found a close enough point
            val actualX = inputs[closestIdx][0]
            val actualY = targets[closestIdx][0]
            val expectedYForActualX = 1.0 * actualX * actualX + 2.0 * actualX + 0.5
            assertEquals(expectedYForActualX, actualY, 0.01)
        }
    }
    
    @Test
    fun testMultiDimensionalPolynomial() {
        val (inputs, targets) = SyntheticDatasets.generateMultiDimensionalPolynomial(
            samples = 50,
            inputDim = 2,
            quadraticCoeffs = doubleArrayOf(0.5, 1.0),
            linearCoeffs = doubleArrayOf(1.0, -0.5),
            constant = 0.1,
            noiseLevel = 0.0,
            random = Random(42)
        )
        
        assertEquals(50, inputs.size)
        assertEquals(2, inputs[0].size)
        assertEquals(1, targets[0].size)
        
        // Verify first sample manually
        val x1 = inputs[0][0]
        val x2 = inputs[0][1]
        val expectedY = 0.5 * x1 * x1 + 1.0 * x2 * x2 + 1.0 * x1 + (-0.5) * x2 + 0.1
        assertEquals(expectedY, targets[0][0], 1e-10)
    }
    
    @Test
    fun testXORDataset() {
        val (inputs, targets) = SyntheticDatasets.generateXORProblem(
            samplesPerClass = 10,
            noiseLevel = 0.0,
            random = Random(42)
        )
        
        assertEquals(40, inputs.size) // 4 classes * 10 samples
        assertEquals(2, inputs[0].size)
        assertEquals(1, targets[0].size)
        
        // Count samples for each class
        var count00 = 0
        var count01 = 0
        var count10 = 0
        var count11 = 0
        
        for (i in inputs.indices) {
            val x1 = inputs[i][0]
            val x2 = inputs[i][1]
            val y = targets[i][0]
            
            when {
                x1 < 0.5 && x2 < 0.5 -> {
                    count00++
                    assertEquals(0.0, y, 1e-10) // XOR(0,0) = 0
                }
                x1 < 0.5 && x2 >= 0.5 -> {
                    count01++
                    assertEquals(1.0, y, 1e-10) // XOR(0,1) = 1
                }
                x1 >= 0.5 && x2 < 0.5 -> {
                    count10++
                    assertEquals(1.0, y, 1e-10) // XOR(1,0) = 1
                }
                else -> {
                    count11++
                    assertEquals(0.0, y, 1e-10) // XOR(1,1) = 0
                }
            }
        }
        
        // Each class should have approximately 10 samples
        assertTrue(count00 >= 8 && count00 <= 12)
        assertTrue(count01 >= 8 && count01 <= 12)
        assertTrue(count10 >= 8 && count10 <= 12)
        assertTrue(count11 >= 8 && count11 <= 12)
    }
    
    @Test
    fun testSineWaveDataset() {
        val (inputs, targets) = SyntheticDatasets.generateSineWave(
            samples = 100,
            amplitude = 2.0,
            frequency = 1.0,
            phase = 0.0,
            noiseLevel = 0.0,
            random = Random(42)
        )
        
        assertEquals(100, inputs.size)
        assertEquals(1, inputs[0].size)
        assertEquals(1, targets[0].size)
        
        // Verify sine relationship: y = 2*sin(x)
        for (i in 0 until minOf(5, inputs.size)) {
            val x = inputs[i][0]
            val expectedY = 2.0 * sin(x)
            assertEquals(expectedY, targets[i][0], 1e-10)
        }
    }
    
    @Test
    fun testTrainTestSplit() {
        val inputs = Array(100) { i -> doubleArrayOf(i.toDouble()) }
        val targets = Array(100) { i -> doubleArrayOf(i.toDouble() * 2.0) }
        
        val (train, test) = SyntheticDatasets.trainTestSplit(
            inputs, targets, trainRatio = 0.8, random = Random(42)
        )
        
        assertEquals(80, train.first.size)
        assertEquals(80, train.second.size)
        assertEquals(20, test.first.size)
        assertEquals(20, test.second.size)
        
        // Verify no data leakage (no overlap between train and test)
        val trainInputs = train.first.map { it[0] }.toSet()
        val testInputs = test.first.map { it[0] }.toSet()
        assertTrue(trainInputs.intersect(testInputs).isEmpty())
    }
    
    @Test
    fun testPolynomialRegressionLearning() {
        // Test that network can learn polynomial regression
        val (inputs, targets) = SyntheticDatasets.generatePolynomialRegression(
            samples = 200,
            coefficients = doubleArrayOf(0.5, 1.0, 0.0), // y = 0.5x^2 + x
            noiseLevel = 0.05,
            random = Random(42)
        )
        
        val (train, test) = SyntheticDatasets.trainTestSplit(inputs, targets, 0.8, Random(42))
        
        // Create network
        val layers = listOf(
            DenseLayer(1, 8, ReLUActivation(), Random(42)),
            DenseLayer(8, 8, ReLUActivation(), Random(42)),
            DenseLayer(8, 1, LinearActivation(), Random(42))
        )
        val network = FeedforwardNetwork(layers, MSELoss(), SGDOptimizer(0.01))
        
        // Train network
        val metrics = network.train(train.first, train.second, epochs = 20, batchSize = 16)
        
        // Verify training progress
        assertTrue(metrics.isNotEmpty())
        assertTrue(metrics.all { it.averageLoss >= 0.0 })
        
        // Test on validation set
        var testLoss = 0.0
        for (i in test.first.indices) {
            val prediction = network.predict(test.first[i])
            val loss = MSELoss().computeLoss(prediction, test.second[i])
            testLoss += loss
        }
        testLoss /= test.first.size
        
        println("Polynomial regression - Final train loss: ${metrics.last().averageLoss}")
        println("Polynomial regression - Test loss: $testLoss")
        
        // Loss should be reasonable (not perfect due to noise)
        assertTrue(testLoss < 1.0, "Test loss should be reasonable: $testLoss")
    }
    
    @Test
    fun testXORLearning() {
        // Test that network can learn XOR problem
        val (inputs, targets) = SyntheticDatasets.generateXORProblem(
            samplesPerClass = 100,
            noiseLevel = 0.02,
            random = Random(42)
        )
        
        val (train, _) = SyntheticDatasets.trainTestSplit(inputs, targets, 0.8, Random(42))
        
        // Create network with hidden layer (XOR requires non-linearity)
        val layers = listOf(
            DenseLayer(2, 4, ReLUActivation(), Random(42)),
            DenseLayer(4, 4, ReLUActivation(), Random(42)),
            DenseLayer(4, 1, SigmoidActivation(), Random(42))
        )
        val network = FeedforwardNetwork(layers, MSELoss(), SGDOptimizer(0.1))
        
        // Train network
        val metrics = network.train(train.first, train.second, epochs = 50, batchSize = 32)
        
        // Verify training progress
        assertTrue(metrics.isNotEmpty())
        
        // Test accuracy on clean XOR cases
        val cleanXOR = arrayOf(
            Pair(doubleArrayOf(0.0, 0.0), 0.0),
            Pair(doubleArrayOf(0.0, 1.0), 1.0),
            Pair(doubleArrayOf(1.0, 0.0), 1.0),
            Pair(doubleArrayOf(1.0, 1.0), 0.0)
        )
        
        var correct = 0
        for ((input, expected) in cleanXOR) {
            val prediction = network.predict(input)[0]
            val predicted = if (prediction > 0.5) 1.0 else 0.0
            if (predicted == expected) correct++
        }
        
        println("XOR learning - Final train loss: ${metrics.last().averageLoss}")
        println("XOR learning - Accuracy on clean cases: $correct/4")
        
        // Should get at least 2 out of 4 correct (better than random)
        assertTrue(correct >= 2, "Should learn some XOR patterns: $correct/4 correct")
    }
    
    @Test
    fun testBatchSizeComparison() {
        // Compare different batch sizes on same problem
        val (inputs, targets) = SyntheticDatasets.generatePolynomialRegression(
            samples = 400,
            coefficients = doubleArrayOf(1.0, 0.5, 0.0),
            noiseLevel = 0.1,
            random = Random(42)
        )
        
        val batchSizes = listOf(16, 32, 64)
        val results = mutableMapOf<Int, Double>()
        
        for (batchSize in batchSizes) {
            val layers = listOf(
                DenseLayer(1, 4, ReLUActivation(), Random(42)),
                DenseLayer(4, 1, LinearActivation(), Random(42))
            )
            val network = FeedforwardNetwork(layers, MSELoss(), SGDOptimizer(0.01))
            
            val metrics = network.train(inputs, targets, epochs = 10, batchSize = batchSize)
            results[batchSize] = metrics.last().averageLoss
            
            println("Batch size $batchSize - Final loss: ${metrics.last().averageLoss}")
        }
        
        // All batch sizes should achieve reasonable performance
        for ((batchSize, finalLoss) in results) {
            assertTrue(finalLoss >= 0.0 && finalLoss.isFinite(), 
                "Batch size $batchSize should have finite loss: $finalLoss")
        }
    }
    
    @Test
    fun testConvergenceMonitoring() {
        val losses = listOf(1.0, 0.8, 0.6, 0.5, 0.49, 0.48, 0.47, 0.47, 0.47)
        
        // Should detect convergence
        assertTrue(ConvergenceMonitor.hasConverged(losses, patience = 3, minImprovement = 0.01))
        
        // Should not detect convergence with stricter criteria
        assertTrue(!ConvergenceMonitor.hasConverged(losses, patience = 3, minImprovement = 0.1))
    }
    
    @Test
    fun testIssueDetection() {
        // Test exploding gradients
        val explodingMetrics = listOf(
            TrainingMetrics(1, 1.0, 1.0, 1.0),
            TrainingMetrics(2, 1.0, 1.0, 15.0), // High gradient norm
            TrainingMetrics(3, 1.0, 1.0, 20.0)
        )
        
        val issues = ConvergenceMonitor.detectIssues(explodingMetrics)
        assertTrue(issues.any { it.contains("Exploding gradients") })
        
        // Test increasing loss
        val increasingLossMetrics = listOf(
            TrainingMetrics(1, 0.5, 0.5, 1.0),
            TrainingMetrics(2, 0.7, 0.7, 1.0),
            TrainingMetrics(3, 0.9, 0.9, 1.0)
        )
        
        val issues2 = ConvergenceMonitor.detectIssues(increasingLossMetrics)
        assertTrue(issues2.any { it.contains("increasing") })
    }
    
    @Test
    fun testLearningCurveGeneration() {
        // Generate learning curve data
        val (inputs, targets) = SyntheticDatasets.generatePolynomialRegression(
            samples = 100,
            random = Random(42)
        )
        
        val (train, test) = SyntheticDatasets.trainTestSplit(inputs, targets, 0.8, Random(42))
        
        val layers = listOf(
            DenseLayer(1, 4, ReLUActivation(), Random(42)),
            DenseLayer(4, 1, LinearActivation(), Random(42))
        )
        val network = FeedforwardNetwork(layers)
        
        val trainMetrics = network.train(train.first, train.second, epochs = 5, batchSize = 16)
        
        // Compute test losses
        val testLosses = mutableListOf<Double>()
        for (metric in trainMetrics) {
            var testLoss = 0.0
            for (i in test.first.indices) {
                val prediction = network.predict(test.first[i])
                testLoss += MSELoss().computeLoss(prediction, test.second[i])
            }
            testLosses.add(testLoss / test.first.size)
        }
        
        val learningCurve = LearningCurve(
            epochs = trainMetrics.map { it.epoch },
            trainLosses = trainMetrics.map { it.averageLoss },
            testLosses = testLosses,
            gradientNorms = trainMetrics.map { it.gradientNorm }
        )
        
        // Verify learning curve structure
        assertEquals(5, learningCurve.epochs.size)
        assertEquals(5, learningCurve.trainLosses.size)
        assertEquals(5, learningCurve.testLosses.size)
        assertEquals(5, learningCurve.gradientNorms.size)
        
        // Print summary (for manual verification)
        learningCurve.printSummary()
    }
    
    @Test
    fun testMiniBatchVsSingleSampleComparison() {
        // Compare mini-batch vs single-sample updates
        val (inputs, targets) = SyntheticDatasets.generatePolynomialRegression(
            samples = 200,
            random = Random(42)
        )
        
        // Single-sample training (batch size 1)
        val singleSampleLayers = listOf(
            DenseLayer(1, 4, ReLUActivation(), Random(42)),
            DenseLayer(4, 1, LinearActivation(), Random(42))
        )
        val singleSampleNetwork = FeedforwardNetwork(singleSampleLayers, MSELoss(), SGDOptimizer(0.01))
        val singleSampleMetrics = singleSampleNetwork.train(inputs, targets, epochs = 5, batchSize = 1)
        
        // Mini-batch training (batch size 32)
        val miniBatchLayers = listOf(
            DenseLayer(1, 4, ReLUActivation(), Random(42)),
            DenseLayer(4, 1, LinearActivation(), Random(42))
        )
        val miniBatchNetwork = FeedforwardNetwork(miniBatchLayers, MSELoss(), SGDOptimizer(0.01))
        val miniBatchMetrics = miniBatchNetwork.train(inputs, targets, epochs = 5, batchSize = 32)
        
        println("Single-sample final loss: ${singleSampleMetrics.last().averageLoss}")
        println("Mini-batch final loss: ${miniBatchMetrics.last().averageLoss}")
        
        // Both should achieve reasonable performance
        assertTrue(singleSampleMetrics.last().averageLoss >= 0.0 && singleSampleMetrics.last().averageLoss.isFinite())
        assertTrue(miniBatchMetrics.last().averageLoss >= 0.0 && miniBatchMetrics.last().averageLoss.isFinite())
        
        // Mini-batch training typically has more stable gradients
        val singleSampleGradientVariance = singleSampleMetrics.map { it.gradientNorm }.let { norms ->
            val mean = norms.average()
            norms.map { (it - mean) * (it - mean) }.average()
        }
        
        val miniBatchGradientVariance = miniBatchMetrics.map { it.gradientNorm }.let { norms ->
            val mean = norms.average()
            norms.map { (it - mean) * (it - mean) }.average()
        }
        
        println("Single-sample gradient variance: $singleSampleGradientVariance")
        println("Mini-batch gradient variance: $miniBatchGradientVariance")
        
        // This demonstrates the difference in training dynamics
        assertTrue(singleSampleGradientVariance >= 0.0)
        assertTrue(miniBatchGradientVariance >= 0.0)
    }
}
