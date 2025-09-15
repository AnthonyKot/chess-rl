package com.chessrl.nn

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.math.*
import kotlin.random.Random

/**
 * Comprehensive neural network validation with diverse learning problems
 * This test suite validates the neural network implementation across:
 * - Classification tasks (synthetic and structured data)
 * - Regression tasks with different complexity levels
 * - Different network architectures and hyperparameters
 * - Performance benchmarks and learning curve analysis
 */
class ValidationDemo {
    
    @Test
    fun testClassificationTasksWithSyntheticData() {
        println("=== Classification Tasks with Synthetic Data ===")
        
        // Test 1: XOR Problem (Non-linear classification)
        println("\n1. XOR Problem (Non-linear Classification)")
        val (xorInputs, xorTargets) = SyntheticDatasets.generateXORProblem(
            samplesPerClass = 200,
            noiseLevel = 0.05,
            random = Random(42)
        )
        
        val (xorTrain, _) = SyntheticDatasets.trainTestSplit(xorInputs, xorTargets, 0.8, Random(42))
        
        val xorNetwork = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(2, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 4, ReLUActivation(), Random(42)),
                DenseLayer(4, 1, SigmoidActivation(), Random(42))
            ),
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(0.01)
        )
        
        val xorMetrics = xorNetwork.train(xorTrain.first, xorTrain.second, epochs = 100, batchSize = 32)
        
        // Test accuracy on clean XOR cases
        val cleanXORCases = arrayOf(
            Pair(doubleArrayOf(0.0, 0.0), 0.0),
            Pair(doubleArrayOf(0.0, 1.0), 1.0),
            Pair(doubleArrayOf(1.0, 0.0), 1.0),
            Pair(doubleArrayOf(1.0, 1.0), 0.0)
        )
        
        var xorCorrect = 0
        for ((input, expected) in cleanXORCases) {
            val prediction = xorNetwork.predict(input)[0]
            val predicted = if (prediction > 0.5) 1.0 else 0.0
            if (predicted == expected) xorCorrect++
            println("  Input: [${input[0]}, ${input[1]}] -> Predicted: ${prediction.format(3)}, Expected: $expected")
        }
        
        println("  XOR Accuracy: $xorCorrect/4 (${(xorCorrect * 25.0).format(1)}%)")
        println("  Final Training Loss: ${xorMetrics.last().averageLoss.format(4)}")
        
        assertTrue(xorCorrect >= 3, "Should learn XOR pattern with high accuracy")
        
        // Test 2: Multi-class Circular Classification
        println("\n2. Multi-class Circular Classification")
        val circularData = generateCircularClassification(samples = 600, classes = 3, noise = 0.1, Random(42))
        val (circTrain, circTest) = SyntheticDatasets.trainTestSplit(circularData.first, circularData.second, 0.8, Random(42))
        
        val circularNetwork = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(2, 16, ReLUActivation(), Random(42)),
                DenseLayer(16, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 3, SigmoidActivation(), Random(42)) // 3 classes
            ),
            lossFunction = CrossEntropyLoss(),
            optimizer = AdamOptimizer(0.005)
        )
        
        val circMetrics = circularNetwork.train(circTrain.first, circTrain.second, epochs = 50, batchSize = 32)
        
        // Test accuracy
        var circCorrect = 0
        for (i in circTest.first.indices) {
            val prediction = circularNetwork.predict(circTest.first[i])
            if (prediction.isNotEmpty() && circTest.second[i].isNotEmpty()) {
                val predictedClass = prediction.indices.maxByOrNull { prediction[it] } ?: 0
                val actualClass = circTest.second[i].indices.maxByOrNull { circTest.second[i][it] } ?: 0
                if (predictedClass == actualClass) circCorrect++
            }
        }
        
        val circAccuracy = if (circTest.first.isNotEmpty()) circCorrect.toDouble() / circTest.first.size else 0.0
        println("  Circular Classification Accuracy: ${(circAccuracy * 100).format(1)}%")
        println("  Final Training Loss: ${circMetrics.last().averageLoss.format(4)}")
        
        assertTrue(circAccuracy > 0.3, "Should achieve reasonable accuracy on circular classification")
    }
    
    @Test
    fun testRegressionTasksWithDifferentComplexity() {
        println("\n=== Regression Tasks with Different Complexity Levels ===")
        
        // Test 1: Simple Linear Regression
        println("\n1. Simple Linear Regression")
        val linearData = generateLinearRegression(samples = 300, slope = 2.5, intercept = 1.0, noise = 0.2, Random(42))
        val (linTrain, linTest) = SyntheticDatasets.trainTestSplit(linearData.first, linearData.second, 0.8, Random(42))
        
        val linearNetwork = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(1, 1, LinearActivation(), Random(42))
            ),
            lossFunction = MSELoss(),
            optimizer = SGDOptimizer(0.01)
        )
        
        val linMetrics = linearNetwork.train(linTrain.first, linTrain.second, epochs = 50, batchSize = 16)
        
        // Calculate R² on test set
        val linR2 = calculateR2(linearNetwork, linTest.first, linTest.second)
        println("  Linear Regression R²: ${linR2.format(3)}")
        println("  Final Training Loss: ${linMetrics.last().averageLoss.format(4)}")
        
        assertTrue(linR2 > 0.8, "Should achieve high R² on linear regression")
        
        // Test 2: Polynomial Regression (Medium Complexity)
        println("\n2. Polynomial Regression (Degree 3)")
        val (polyInputs, polyTargets) = SyntheticDatasets.generatePolynomialRegression(
            samples = 400,
            coefficients = doubleArrayOf(0.5, -1.2, 0.8), // quadratic polynomial (matches function signature)
            noiseLevel = 0.15,
            random = Random(42)
        )
        
        val (polyTrain, polyTest) = SyntheticDatasets.trainTestSplit(polyInputs, polyTargets, 0.8, Random(42))
        
        val polyNetwork = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(1, 16, ReLUActivation(), Random(42)),
                DenseLayer(16, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 1, LinearActivation(), Random(42))
            ),
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(0.01)
        )
        
        val polyMetrics = polyNetwork.train(polyTrain.first, polyTrain.second, epochs = 100, batchSize = 32)
        
        val polyR2 = calculateR2(polyNetwork, polyTest.first, polyTest.second)
        println("  Polynomial Regression R²: ${polyR2.format(3)}")
        println("  Final Training Loss: ${polyMetrics.last().averageLoss.format(4)}")
        
        assertTrue(polyR2 > 0.7, "Should achieve good R² on polynomial regression")
        
        // Test 3: Multi-dimensional Regression (High Complexity)
        println("\n3. Multi-dimensional Regression")
        val (multiInputs, multiTargets) = SyntheticDatasets.generateMultiDimensionalPolynomial(
            samples = 500,
            inputDim = 5,
            quadraticCoeffs = doubleArrayOf(0.3, -0.5, 0.8, 0.2, -0.4),
            linearCoeffs = doubleArrayOf(1.2, -0.8, 0.5, 1.0, -0.3),
            constant = 0.5,
            noiseLevel = 0.1,
            random = Random(42)
        )
        
        val (multiTrain, multiTest) = SyntheticDatasets.trainTestSplit(multiInputs, multiTargets, 0.8, Random(42))
        
        val multiNetwork = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(5, 32, ReLUActivation(), Random(42)),
                DenseLayer(32, 16, ReLUActivation(), Random(42)),
                DenseLayer(16, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 1, LinearActivation(), Random(42))
            ),
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(0.005)
        )
        
        val multiMetrics = multiNetwork.train(multiTrain.first, multiTrain.second, epochs = 150, batchSize = 64)
        
        val multiR2 = calculateR2(multiNetwork, multiTest.first, multiTest.second)
        println("  Multi-dimensional Regression R²: ${multiR2.format(3)}")
        println("  Final Training Loss: ${multiMetrics.last().averageLoss.format(4)}")
        
        assertTrue(multiR2 > 0.6, "Should achieve reasonable R² on multi-dimensional regression")
        
        // Test 4: Sine Wave Regression (Non-linear)
        println("\n4. Sine Wave Regression")
        val (sineInputs, sineTargets) = SyntheticDatasets.generateSineWave(
            samples = 300,
            amplitude = 2.0,
            frequency = 1.5,
            phase = PI/4,
            noiseLevel = 0.1,
            random = Random(42)
        )
        
        val (sineTrain, sineTest) = SyntheticDatasets.trainTestSplit(sineInputs, sineTargets, 0.8, Random(42))
        
        val sineNetwork = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(1, 20, TanhActivation(), Random(42)),
                DenseLayer(20, 10, TanhActivation(), Random(42)),
                DenseLayer(10, 1, LinearActivation(), Random(42))
            ),
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(0.01)
        )
        
        val sineMetrics = sineNetwork.train(sineTrain.first, sineTrain.second, epochs = 100, batchSize = 32)
        
        val sineR2 = calculateR2(sineNetwork, sineTest.first, sineTest.second)
        println("  Sine Wave Regression R²: ${sineR2.format(3)}")
        println("  Final Training Loss: ${sineMetrics.last().averageLoss.format(4)}")
        
        assertTrue(sineR2 > 0.8, "Should achieve high R² on sine wave regression")
    }
    
    @Test
    fun testDifferentNetworkArchitectures() {
        println("\n=== Different Network Architectures ===")
        
        val (inputs, targets) = SyntheticDatasets.generatePolynomialRegression(
            samples = 400,
            coefficients = doubleArrayOf(1.0, -0.5, 0.2),
            noiseLevel = 0.1,
            random = Random(42)
        )
        
        val (trainInputs, testInputs) = SyntheticDatasets.trainTestSplit(inputs, targets, 0.8, Random(42))
        
        val architectures = listOf(
            "Shallow" to listOf(
                DenseLayer(1, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 1, LinearActivation(), Random(42))
            ),
            "Medium" to listOf(
                DenseLayer(1, 16, ReLUActivation(), Random(42)),
                DenseLayer(16, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 1, LinearActivation(), Random(42))
            ),
            "Deep" to listOf(
                DenseLayer(1, 32, ReLUActivation(), Random(42)),
                DenseLayer(32, 16, ReLUActivation(), Random(42)),
                DenseLayer(16, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 4, ReLUActivation(), Random(42)),
                DenseLayer(4, 1, LinearActivation(), Random(42))
            ),
            "Wide" to listOf(
                DenseLayer(1, 64, ReLUActivation(), Random(42)),
                DenseLayer(64, 1, LinearActivation(), Random(42))
            )
        )
        
        for ((name, layers) in architectures) {
            println("\n$name Architecture:")
            val network = FeedforwardNetwork(
                _layers = layers,
                lossFunction = MSELoss(),
                optimizer = AdamOptimizer(0.01)
            )
            
            val metrics = network.train(trainInputs.first, trainInputs.second, epochs = 50, batchSize = 32)
            val r2 = calculateR2(network, testInputs.first, testInputs.second)
            
            println("  Layers: ${layers.size}")
            println("  Parameters: ${countParameters(layers)}")
            println("  Final Loss: ${metrics.last().averageLoss.format(4)}")
            println("  Test R²: ${r2.format(3)}")
            
            assertTrue(r2 > 0.5, "$name architecture should achieve reasonable performance")
        }
    }
    
    @Test
    fun testDifferentHyperparameters() {
        println("\n=== Different Hyperparameters ===")
        
        val (inputs, targets) = SyntheticDatasets.generateXORProblem(
            samplesPerClass = 150,
            noiseLevel = 0.05,
            random = Random(42)
        )
        
        // Test different learning rates
        println("\n1. Learning Rate Comparison:")
        val learningRates = listOf(0.001, 0.01, 0.1)
        
        for (lr in learningRates) {
            val network = FeedforwardNetwork(
                _layers = listOf(
                    DenseLayer(2, 8, ReLUActivation(), Random(42)),
                    DenseLayer(8, 1, SigmoidActivation(), Random(42))
                ),
                lossFunction = MSELoss(),
                optimizer = SGDOptimizer(lr)
            )
            
            val metrics = network.train(inputs, targets, epochs = 50, batchSize = 32)
            println("  LR $lr: Final Loss = ${metrics.last().averageLoss.format(4)}")
            
            assertTrue(metrics.last().averageLoss.isFinite(), "Loss should be finite for LR $lr")
        }
        
        // Test different optimizers
        println("\n2. Optimizer Comparison:")
        val optimizers = listOf(
            "SGD" to SGDOptimizer(0.01),
            "Adam" to AdamOptimizer(0.01),
            "RMSprop" to RMSpropOptimizer(0.01)
        )
        
        for ((name, optimizer) in optimizers) {
            val network = FeedforwardNetwork(
                _layers = listOf(
                    DenseLayer(2, 8, ReLUActivation(), Random(42)),
                    DenseLayer(8, 1, SigmoidActivation(), Random(42))
                ),
                lossFunction = MSELoss(),
                optimizer = optimizer
            )
            
            val metrics = network.train(inputs, targets, epochs = 50, batchSize = 32)
            println("  $name: Final Loss = ${metrics.last().averageLoss.format(4)}")
            
            assertTrue(metrics.last().averageLoss >= 0.0, "$name optimizer should produce non-negative loss")
        }
        
        // Test different batch sizes
        println("\n3. Batch Size Comparison:")
        val batchSizes = listOf(8, 16, 32, 64)
        
        for (batchSize in batchSizes) {
            val network = FeedforwardNetwork(
                _layers = listOf(
                    DenseLayer(2, 8, ReLUActivation(), Random(42)),
                    DenseLayer(8, 1, SigmoidActivation(), Random(42))
                ),
                lossFunction = MSELoss(),
                optimizer = AdamOptimizer(0.01)
            )
            
            val metrics = network.train(inputs, targets, epochs = 30, batchSize = batchSize)
            println("  Batch Size $batchSize: Final Loss = ${metrics.last().averageLoss.format(4)}")
            
            assertTrue(metrics.last().averageLoss >= 0.0, "Batch size $batchSize should produce valid results")
        }
    }
    
    @Test
    fun testPerformanceBenchmarks() {
        println("\n=== Performance Benchmarks ===")
        
        // Benchmark 1: Training Speed vs Network Size
        println("\n1. Training Speed vs Network Size:")
        val networkSizes = listOf(
            "Small" to listOf(DenseLayer(10, 5, ReLUActivation(), Random(42)), DenseLayer(5, 1, LinearActivation(), Random(42))),
            "Medium" to listOf(DenseLayer(10, 20, ReLUActivation(), Random(42)), DenseLayer(20, 10, ReLUActivation(), Random(42)), DenseLayer(10, 1, LinearActivation(), Random(42))),
            "Large" to listOf(DenseLayer(10, 50, ReLUActivation(), Random(42)), DenseLayer(50, 25, ReLUActivation(), Random(42)), DenseLayer(25, 1, LinearActivation(), Random(42)))
        )
        
        val benchmarkInputs = Array(1000) { DoubleArray(10) { Random.nextDouble(-1.0, 1.0) } }
        val benchmarkTargets = Array(1000) { doubleArrayOf(Random.nextDouble()) }
        
        for ((name, layers) in networkSizes) {
            val network = FeedforwardNetwork(_layers = layers)
            
            val startTime = getCurrentTimeMillis()
            network.train(benchmarkInputs, benchmarkTargets, epochs = 10, batchSize = 32)
            val endTime = getCurrentTimeMillis()
            
            val trainingTime = endTime - startTime
            val parameters = countParameters(layers)
            
            println("  $name Network:")
            println("    Parameters: $parameters")
            println("    Training Time: ${trainingTime}ms")
            println("    Time per Parameter: ${(trainingTime.toDouble() / parameters).format(3)}ms")
            
            assertTrue(trainingTime > 0, "Training should take measurable time")
        }
        
        // Benchmark 2: Convergence Speed Comparison
        println("\n2. Convergence Speed Analysis:")
        val (inputs, targets) = SyntheticDatasets.generatePolynomialRegression(
            samples = 500,
            coefficients = doubleArrayOf(1.0, 0.5, 0.0), // quadratic, linear, constant
            noiseLevel = 0.1,
            random = Random(42)
        )
        
        val network = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(1, 16, ReLUActivation(), Random(42)),
                DenseLayer(16, 1, LinearActivation(), Random(42))
            ),
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(0.01)
        )
        
        val metrics = network.train(inputs, targets, epochs = 100, batchSize = 32)
        
        // Find convergence point
        val convergenceThreshold = 0.01
        var convergenceEpoch = -1
        for (i in 1 until metrics.size) {
            val improvement = metrics[i-1].averageLoss - metrics[i].averageLoss
            if (improvement < convergenceThreshold) {
                convergenceEpoch = i
                break
            }
        }
        
        println("  Initial Loss: ${metrics.first().averageLoss.format(4)}")
        println("  Final Loss: ${metrics.last().averageLoss.format(4)}")
        println("  Convergence Epoch: ${if (convergenceEpoch > 0) convergenceEpoch else "Not reached"}")
        println("  Total Improvement: ${((metrics.first().averageLoss - metrics.last().averageLoss) / metrics.first().averageLoss * 100).format(1)}%")
        
        assertTrue(metrics.last().averageLoss < metrics.first().averageLoss, "Loss should decrease during training")
    }
    
    @Test
    fun testLearningCurveAnalysis() {
        println("\n=== Learning Curve Analysis ===")
        
        val (inputs, targets) = SyntheticDatasets.generatePolynomialRegression(
            samples = 600,
            coefficients = doubleArrayOf(0.8, -0.3, 0.1),
            noiseLevel = 0.15,
            random = Random(42)
        )
        
        val (trainData, testData) = SyntheticDatasets.trainTestSplit(inputs, targets, 0.8, Random(42))
        
        val network = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(1, 12, ReLUActivation(), Random(42)),
                DenseLayer(12, 6, ReLUActivation(), Random(42)),
                DenseLayer(6, 1, LinearActivation(), Random(42))
            ),
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(0.01)
        )
        
        val trainMetrics = network.train(trainData.first, trainData.second, epochs = 80, batchSize = 32)
        
        // Calculate test losses for each epoch
        val testLosses = mutableListOf<Double>()
        val mse = MSELoss()
        
        // We need to retrain to get intermediate test losses (simplified approach)
        val analysisNetwork = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(1, 12, ReLUActivation(), Random(42)),
                DenseLayer(12, 6, ReLUActivation(), Random(42)),
                DenseLayer(6, 1, LinearActivation(), Random(42))
            ),
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(0.01)
        )
        
        // Train epoch by epoch to capture test performance
        for (epoch in 1..20) {
            analysisNetwork.train(trainData.first, trainData.second, epochs = 1, batchSize = 32)
            
            // Calculate test loss
            var testLoss = 0.0
            for (i in testData.first.indices) {
                val prediction = analysisNetwork.predict(testData.first[i])
                testLoss += mse.computeLoss(prediction, testData.second[i])
            }
            testLosses.add(testLoss / testData.first.size)
        }
        
        val learningCurve = LearningCurve(
            epochs = (1..20).toList(),
            trainLosses = trainMetrics.take(20).map { it.averageLoss },
            testLosses = testLosses,
            gradientNorms = trainMetrics.take(20).map { it.gradientNorm }
        )
        
        println("\nLearning Curve Analysis:")
        learningCurve.printSummary()
        
        // Analyze overfitting
        val finalTrainLoss = learningCurve.trainLosses.last()
        val finalTestLoss = learningCurve.testLosses.last()
        val overfittingGap = finalTestLoss - finalTrainLoss
        
        println("  Overfitting Gap: ${overfittingGap.format(4)}")
        println("  Gradient Stability: ${if (learningCurve.gradientNorms.all { it < 5.0 }) "Stable" else "Unstable"}")
        
        // Check for convergence
        val hasConverged = ConvergenceMonitor.hasConverged(learningCurve.trainLosses, patience = 3, minImprovement = 0.001)
        println("  Converged: $hasConverged")
        
        // Detect training issues
        val issues = ConvergenceMonitor.detectIssues(trainMetrics.take(20))
        if (issues.isNotEmpty()) {
            println("  Training Issues: ${issues.joinToString(", ")}")
        } else {
            println("  Training Issues: None detected")
        }
        
        assertTrue(finalTrainLoss >= 0.0 && finalTestLoss >= 0.0, "Losses should be non-negative")
        assertTrue(overfittingGap < 1.0, "Overfitting gap should be reasonable")
    }
    
    // Helper functions
    
    private fun generateCircularClassification(samples: Int, classes: Int, noise: Double, random: Random): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        val inputs = mutableListOf<DoubleArray>()
        val targets = mutableListOf<DoubleArray>()
        
        val samplesPerClass = samples / classes
        
        for (classIdx in 0 until classes) {
            val angle = 2.0 * PI * classIdx / classes
            val radius = 1.0 + classIdx * 0.5
            
            repeat(samplesPerClass) {
                val theta = angle + random.nextDouble(-PI/classes, PI/classes)
                val r = radius + random.nextGaussian() * noise
                
                val x = r * cos(theta)
                val y = r * sin(theta)
                
                inputs.add(doubleArrayOf(x, y))
                
                val target = DoubleArray(classes) { 0.0 }
                target[classIdx] = 1.0
                targets.add(target)
            }
        }
        
        // Shuffle
        val indices = (0 until inputs.size).shuffled(random)
        val shuffledInputs = Array(inputs.size) { inputs[indices[it]] }
        val shuffledTargets = Array(targets.size) { targets[indices[it]] }
        
        return Pair(shuffledInputs, shuffledTargets)
    }
    
    private fun generateLinearRegression(samples: Int, slope: Double, intercept: Double, noise: Double, random: Random): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        val inputs = Array(samples) { doubleArrayOf(random.nextDouble(-2.0, 2.0)) }
        val targets = Array(samples) { i ->
            val x = inputs[i][0]
            val y = slope * x + intercept + random.nextGaussian() * noise
            doubleArrayOf(y)
        }
        return Pair(inputs, targets)
    }
    
    private fun calculateR2(network: FeedforwardNetwork, inputs: Array<DoubleArray>, targets: Array<DoubleArray>): Double {
        var ssRes = 0.0
        var ssTot = 0.0
        
        val meanTarget = targets.map { it[0] }.average()
        
        for (i in inputs.indices) {
            val prediction = network.predict(inputs[i])[0]
            val actual = targets[i][0]
            
            ssRes += (actual - prediction) * (actual - prediction)
            ssTot += (actual - meanTarget) * (actual - meanTarget)
        }
        
        return 1.0 - (ssRes / ssTot)
    }
    
    private fun countParameters(layers: List<Layer>): Int {
        var count = 0
        for (layer in layers) {
            if (layer is DenseLayer) {
                count += layer.inputSize * layer.outputSize + layer.outputSize // weights + biases
            }
        }
        return count
    }
    
    // getCurrentTimeMillis() is already available from the common package
}
