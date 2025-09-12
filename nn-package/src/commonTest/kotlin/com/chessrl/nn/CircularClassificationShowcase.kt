package com.chessrl.nn

import kotlin.test.Test
import kotlin.math.*
import kotlin.random.Random

/**
 * Showcase test for Multi-class Circular Classification
 * This test demonstrates the neural network learning circular decision boundaries
 * and shows detailed output of the learning process.
 */
class CircularClassificationShowcase {
    
    @Test
    fun showcaseCircularClassification() {
        println("\n" + "=".repeat(60))
        println("    MULTI-CLASS CIRCULAR CLASSIFICATION SHOWCASE")
        println("=".repeat(60))
        
        // Generate circular classification data with 3 classes
        println("\n🎯 Generating circular classification dataset...")
        val (inputs, targets) = generateCircularClassification(
            samples = 600, 
            classes = 3, 
            noise = 0.1, 
            Random(42)
        )
        
        println("   ✓ Generated ${inputs.size} samples with ${targets[0].size} classes")
        
        // Show some sample data points
        println("\n📊 Sample data points:")
        for (i in 0 until minOf(5, inputs.size)) {
            val classIdx = targets[i].indices.maxByOrNull { targets[i][it] } ?: 0
            println("   Point [${inputs[i][0].format(2)}, ${inputs[i][1].format(2)}] -> Class $classIdx")
        }
        
        // Split into train/test
        val (train, test) = SyntheticDatasets.trainTestSplit(inputs, targets, 0.8, Random(42))
        println("\n📈 Data split: ${train.first.size} training, ${test.first.size} test samples")
        
        // Create network for 3-class classification
        println("\n🧠 Creating neural network...")
        val network = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(2, 12, ReLUActivation(), Random(42)),
                DenseLayer(12, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 3, SigmoidActivation(), Random(42)) // 3 output classes
            ),
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(0.005)
        )
        
        println("   ✓ Architecture: 2 inputs -> 12 -> 8 -> 3 outputs")
        println("   ✓ Activation: ReLU (hidden), Sigmoid (output)")
        println("   ✓ Loss: Mean Squared Error")
        println("   ✓ Optimizer: Adam (lr=0.005)")
        
        // Train the network
        println("\n🚀 Training network...")
        val startTime = getCurrentTimeMillis()
        val metrics = network.train(train.first, train.second, epochs = 100, batchSize = 32)
        val trainingTime = getCurrentTimeMillis() - startTime
        
        // Show training progress
        println("\n📉 Training progress:")
        val progressPoints = listOf(0, 19, 39, 59, 79, 99) // Every 20 epochs
        for (i in progressPoints) {
            if (i < metrics.size) {
                println("   Epoch ${String.format("%3d", i + 1)}: Loss = ${metrics[i].averageLoss.format(4)}")
            }
        }
        
        // Calculate training progress
        val initialLoss = metrics.first().averageLoss
        val finalLoss = metrics.last().averageLoss
        val improvement = ((initialLoss - finalLoss) / initialLoss * 100)
        
        println("\n📊 Training Summary:")
        println("   Initial Loss: ${initialLoss.format(4)}")
        println("   Final Loss:   ${finalLoss.format(4)}")
        println("   Improvement:  ${improvement.format(1)}%")
        println("   Training Time: ${trainingTime}ms")
        
        // Test accuracy on test set
        println("\n🎯 Testing on unseen data...")
        var correct = 0
        val classCorrect = IntArray(3) { 0 }
        val classTotal = IntArray(3) { 0 }
        
        for (i in test.first.indices) {
            val prediction = network.predict(test.first[i])
            val predictedClass = prediction.indices.maxByOrNull { prediction[it] } ?: 0
            val actualClass = test.second[i].indices.maxByOrNull { test.second[i][it] } ?: 0
            
            classTotal[actualClass]++
            if (predictedClass == actualClass) {
                correct++
                classCorrect[actualClass]++
            }
        }
        
        val overallAccuracy = correct.toDouble() / test.first.size
        
        println("\n📈 Test Results:")
        println("   Overall Accuracy: ${(overallAccuracy * 100).format(1)}%")
        println("   Correct Predictions: $correct/${test.first.size}")
        println("   Random Baseline: 33.3% (3 classes)")
        
        // Per-class accuracy
        println("\n📋 Per-class Performance:")
        for (i in 0 until 3) {
            val classAccuracy = if (classTotal[i] > 0) classCorrect[i].toDouble() / classTotal[i] else 0.0
            println("   Class $i: ${(classAccuracy * 100).format(1)}% accuracy (${classCorrect[i]}/${classTotal[i]} correct)")
        }
        
        // Test some specific points to show decision boundaries
        println("\n🎯 Decision Boundary Analysis:")
        val testPoints = arrayOf(
            doubleArrayOf(0.0, 0.0),     // Center
            doubleArrayOf(1.0, 0.0),     // Class 0 region (radius ~1.0, angle 0)
            doubleArrayOf(-0.75, 1.3),   // Class 1 region (radius ~1.5, angle 2π/3)
            doubleArrayOf(-0.75, -1.3)   // Class 2 region (radius ~2.0, angle 4π/3)
        )
        
        for (point in testPoints) {
            val prediction = network.predict(point)
            val predictedClass = prediction.indices.maxByOrNull { prediction[it] } ?: 0
            val confidence = prediction[predictedClass]
            
            println("   Point [${point[0].format(2)}, ${point[1].format(2)}] -> Class $predictedClass (${(confidence * 100).format(1)}% confidence)")
            println("     Raw outputs: [${prediction[0].format(3)}, ${prediction[1].format(3)}, ${prediction[2].format(3)}]")
        }
        
        // Final summary
        println("\n" + "=".repeat(60))
        println("                        RESULTS SUMMARY")
        println("=".repeat(60))
        
        if (overallAccuracy > 0.33) {
            println("✅ SUCCESS: Network learned circular classification patterns!")
            println("   • Achieved ${(overallAccuracy * 100).format(1)}% accuracy (beats random 33.3%)")
        } else {
            println("⚠️  PARTIAL: Network showed learning but needs improvement")
            println("   • Achieved ${(overallAccuracy * 100).format(1)}% accuracy")
        }
        
        if (improvement > 10.0) {
            println("✅ Training convergence: ${improvement.format(1)}% loss reduction")
        } else {
            println("⚠️  Training progress: ${improvement.format(1)}% loss reduction")
        }
        
        println("\n🎉 This demonstrates the neural network's ability to learn")
        println("   complex non-linear decision boundaries in multi-class problems!")
        println("=".repeat(60))
    }
    
    /**
     * Generate circular classification dataset with multiple classes
     */
    private fun generateCircularClassification(
        samples: Int, 
        classes: Int, 
        noise: Double, 
        random: Random
    ): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        val inputs = mutableListOf<DoubleArray>()
        val targets = mutableListOf<DoubleArray>()
        
        val samplesPerClass = samples / classes
        
        for (classIdx in 0 until classes) {
            // Each class gets a different radius and angular position
            val baseAngle = 2.0 * PI * classIdx / classes
            val baseRadius = 1.0 + classIdx * 0.5 // Increasing radius for each class
            
            repeat(samplesPerClass) {
                // Add some angular spread around the base angle
                val angleSpread = PI / classes * 0.8 // 80% of the available angular space
                val theta = baseAngle + random.nextDouble(-angleSpread/2, angleSpread/2)
                
                // Add some radial variation
                val radiusVariation = 0.3
                val r = baseRadius + random.nextDouble(-radiusVariation, radiusVariation)
                
                // Convert to Cartesian coordinates with noise
                val x = r * cos(theta) + random.nextGaussian() * noise
                val y = r * sin(theta) + random.nextGaussian() * noise
                
                inputs.add(doubleArrayOf(x, y))
                
                // One-hot encoding for the target class
                val target = DoubleArray(classes) { 0.0 }
                target[classIdx] = 1.0
                targets.add(target)
            }
        }
        
        // Shuffle the data
        val indices = (0 until inputs.size).shuffled(random)
        val shuffledInputs = Array(inputs.size) { inputs[indices[it]] }
        val shuffledTargets = Array(targets.size) { targets[indices[it]] }
        
        return Pair(shuffledInputs, shuffledTargets)
    }
}

// Using format function from SyntheticDatasets