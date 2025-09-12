package com.chessrl.nn

import kotlin.math.*
import kotlin.random.Random

/**
 * Demonstration of advanced optimizers, loss functions, regularization, and learning rate scheduling
 */
object AdvancedOptimizersDemo {
    
    /**
     * Demonstrates different optimizers on a simple regression problem
     */
    fun demonstrateOptimizers() {
        println("=== Advanced Optimizers Demonstration ===")
        
        // Create a simple regression dataset: y = 2x + 1 + noise
        val dataSize = 100
        val inputs = Array(dataSize) { i ->
            val x = (i - dataSize/2) / 10.0
            doubleArrayOf(x)
        }
        val targets = Array(dataSize) { i ->
            val x = inputs[i][0]
            val y = 2.0 * x + 1.0 + Random.nextGaussian() * 0.1
            doubleArrayOf(y)
        }
        
        // Test different optimizers
        val optimizers = listOf(
            "SGD" to SGDOptimizer(learningRate = 0.01),
            "SGD+Momentum" to SGDOptimizer(learningRate = 0.01, momentum = 0.9),
            "Adam" to AdamOptimizer(learningRate = 0.01),
            "RMSprop" to RMSpropOptimizer(learningRate = 0.01)
        )
        
        for ((name, optimizer) in optimizers) {
            println("\n--- Testing $name ---")
            
            val layers = listOf(
                DenseLayer(1, 10, ReLUActivation(), Random(42)),
                DenseLayer(10, 1, LinearActivation(), Random(42))
            )
            
            val network = FeedforwardNetwork(layers, MSELoss(), optimizer)
            val metrics = network.train(inputs, targets, epochs = 50, batchSize = 16)
            
            val finalLoss = metrics.last().averageLoss
            println("Final loss: ${finalLoss}")
            
            // Test prediction
            val testInput = doubleArrayOf(2.0)
            val prediction = network.predict(testInput)
            val expected = 2.0 * 2.0 + 1.0 // y = 2x + 1
            println("Prediction for x=2.0: ${prediction[0]}, Expected: ${expected}")
        }
    }
    
    /**
     * Demonstrates different loss functions
     */
    fun demonstrateLossFunctions() {
        println("\n=== Loss Functions Demonstration ===")
        
        val predicted = doubleArrayOf(0.8, 0.1, 0.1)
        val target = doubleArrayOf(1.0, 0.0, 0.0)
        
        val lossFunctions = listOf(
            "MSE" to MSELoss(),
            "CrossEntropy" to CrossEntropyLoss(),
            "Huber" to HuberLoss(delta = 1.0)
        )
        
        for ((name, lossFunction) in lossFunctions) {
            val loss = lossFunction.computeLoss(predicted, target)
            println("$name Loss: ${loss}")
        }
    }
    
    /**
     * Demonstrates regularization effects
     */
    fun demonstrateRegularization() {
        println("\n=== Regularization Demonstration ===")
        
        // Create overfitting-prone scenario with small dataset
        val inputs = Array(20) { i ->
            val x = i / 10.0
            doubleArrayOf(x, x * x)
        }
        val targets = Array(20) { i ->
            val x = inputs[i][0]
            doubleArrayOf(sin(x))
        }
        
        val regularizations = listOf(
            "None" to null,
            "L1" to L1Regularization(lambda = 0.01),
            "L2" to L2Regularization(lambda = 0.01),
            "Dropout" to DropoutRegularization(dropoutRate = 0.3)
        )
        
        for ((name, regularization) in regularizations) {
            println("\n--- Testing $name Regularization ---")
            
            val layers = listOf(
                DenseLayer(2, 20, ReLUActivation(), Random(42)),
                DenseLayer(20, 10, ReLUActivation(), Random(42)),
                DenseLayer(10, 1, LinearActivation(), Random(42))
            )
            
            val network = FeedforwardNetwork(
                layers, 
                MSELoss(), 
                AdamOptimizer(learningRate = 0.001),
                regularization
            )
            
            val metrics = network.train(inputs, targets, epochs = 100, batchSize = 10)
            val finalLoss = metrics.last().averageLoss
            println("Final loss: ${finalLoss}")
        }
    }
    
    /**
     * Demonstrates learning rate scheduling
     */
    fun demonstrateLearningRateScheduling() {
        println("\n=== Learning Rate Scheduling Demonstration ===")
        
        // Simple classification dataset
        val inputs = Array(200) { i ->
            val x1 = Random.nextGaussian()
            val x2 = Random.nextGaussian()
            doubleArrayOf(x1, x2)
        }
        val targets = Array(200) { i ->
            val x1 = inputs[i][0]
            val x2 = inputs[i][1]
            val label = if (x1 * x1 + x2 * x2 < 1.0) 1.0 else 0.0
            doubleArrayOf(label)
        }
        
        val schedulers = listOf(
            "None" to null,
            "Exponential Decay" to ExponentialDecayScheduler(decayRate = 0.95, decaySteps = 10),
            "Step Decay" to StepDecayScheduler(stepSize = 25, gamma = 0.5),
            "Linear Decay" to LinearDecayScheduler(totalEpochs = 100, finalLearningRate = 0.001)
        )
        
        for ((name, scheduler) in schedulers) {
            println("\n--- Testing $name ---")
            
            val layers = listOf(
                DenseLayer(2, 8, ReLUActivation(), Random(42)),
                DenseLayer(8, 4, ReLUActivation(), Random(42)),
                DenseLayer(4, 1, SigmoidActivation(), Random(42))
            )
            
            val network = FeedforwardNetwork(
                layers,
                MSELoss(),
                AdamOptimizer(learningRate = 0.01),
                null,
                scheduler
            )
            
            val metrics = network.train(inputs, targets, epochs = 50, batchSize = 32)
            val finalLoss = metrics.last().averageLoss
            println("Final loss: ${finalLoss}")
            
            // Show learning rate progression for first few epochs
            if (scheduler != null) {
                print("Learning rates: ")
                for (epoch in 0..4) {
                    val lr = scheduler.getScheduledLearningRate(epoch, 0.01)
                    print("${lr} ")
                }
                println()
            }
        }
    }
    
    /**
     * Comprehensive demonstration combining all features
     */
    fun demonstrateComplexScenario() {
        println("\n=== Complex Training Scenario ===")
        
        // Create a more complex dataset
        val dataSize = 500
        val inputs = Array(dataSize) { i ->
            val x1 = (Random.nextDouble() - 0.5) * 4.0
            val x2 = (Random.nextDouble() - 0.5) * 4.0
            doubleArrayOf(x1, x2)
        }
        val targets = Array(dataSize) { i ->
            val x1 = inputs[i][0]
            val x2 = inputs[i][1]
            val y = sin(x1) * cos(x2) + Random.nextGaussian() * 0.1
            doubleArrayOf(y)
        }
        
        println("Training complex function: sin(x1) * cos(x2)")
        
        val layers = listOf(
            DenseLayer(2, 32, ReLUActivation(), Random(42)),
            DenseLayer(32, 16, TanhActivation(), Random(42)),
            DenseLayer(16, 8, ReLUActivation(), Random(42)),
            DenseLayer(8, 1, LinearActivation(), Random(42))
        )
        
        val optimizer = AdamOptimizer(learningRate = 0.001)
        val regularization = L2Regularization(lambda = 0.001)
        val scheduler = ExponentialDecayScheduler(decayRate = 0.98, decaySteps = 20)
        val lossFunction = HuberLoss(delta = 1.0)
        
        val network = FeedforwardNetwork(layers, lossFunction, optimizer, regularization, scheduler)
        
        println("Training with Adam optimizer, L2 regularization, exponential decay, and Huber loss...")
        val metrics = network.train(inputs, targets, epochs = 100, batchSize = 64)
        
        println("Training completed!")
        println("Initial loss: ${metrics.first().averageLoss}")
        println("Final loss: ${metrics.last().averageLoss}")
        
        // Test some predictions
        val testCases = listOf(
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(1.57, 1.57) // π/2, π/2
        )
        
        println("\nTest predictions:")
        for (testInput in testCases) {
            val prediction = network.predict(testInput)
            val expected = sin(testInput[0]) * cos(testInput[1])
            println("Input: [${testInput[0]}, ${testInput[1]}] -> Predicted: ${prediction[0]}, Expected: ${expected}")
        }
    }
    
    /**
     * Run all demonstrations
     */
    fun runAllDemonstrations() {
        demonstrateOptimizers()
        demonstrateLossFunctions()
        demonstrateRegularization()
        demonstrateLearningRateScheduling()
        demonstrateComplexScenario()
        
        println("\n=== All demonstrations completed successfully! ===")
    }
}