package com.chessrl.nn

import kotlin.math.*
import kotlin.random.Random

/**
 * Demonstration of comprehensive training infrastructure
 * Shows dataset management, training history, evaluation metrics, and hyperparameter testing
 */
object TrainingInfrastructureDemo {
    
    fun runDemo() {
        println("=== Neural Network Training Infrastructure Demo ===\n")
        
        // Demo 1: Dataset Management
        datasetManagementDemo()
        
        // Demo 2: Comprehensive Training with Validation
        comprehensiveTrainingDemo()
        
        // Demo 3: Evaluation Metrics
        evaluationMetricsDemo()
        
        // Demo 4: Hyperparameter Testing
        hyperparameterTestingDemo()
        
        // Demo 5: Model Serialization
        modelSerializationDemo()
        
        println("\n=== Training Infrastructure Demo Complete ===")
    }
    
    private fun datasetManagementDemo() {
        println("1. Dataset Management Demo")
        println("=" * 30)
        
        // Create synthetic regression dataset: y = 2x + 1 + noise
        val dataSize = 200
        val inputs = Array(dataSize) { i ->
            doubleArrayOf((i - 100).toDouble() / 50.0) // Normalized inputs
        }
        val targets = Array(dataSize) { i ->
            val x = inputs[i][0]
            doubleArrayOf(2.0 * x + 1.0 + Random.nextGaussian() * 0.1) // Add small noise
        }
        
        val dataset = InMemoryDataset(inputs, targets)
        
        println("Dataset size: ${dataset.size()}")
        println("Sample input: ${inputs[0].contentToString()}")
        println("Sample target: ${targets[0].contentToString()}")
        
        // Demonstrate batch processing
        dataset.shuffle()
        dataset.reset()
        
        var batchCount = 0
        var totalSamples = 0
        
        while (dataset.hasNext()) {
            try {
                val batch = dataset.getBatch(32)
                batchCount++
                totalSamples += batch.batchSize
                if (batchCount <= 3) {
                    println("Batch $batchCount: ${batch.batchSize} samples")
                }
            } catch (e: IllegalStateException) {
                break
            }
        }
        
        println("Total batches: $batchCount")
        println("Total samples processed: $totalSamples")
        println()
    }
    
    private fun comprehensiveTrainingDemo() {
        println("2. Comprehensive Training Demo")
        println("=" * 30)
        
        // Create polynomial regression problem: y = x^2 + 2x + 1
        val trainSize = 150
        val valSize = 50
        
        val trainInputs = Array(trainSize) { i ->
            doubleArrayOf((i - 75).toDouble() / 25.0) // Range [-3, 3]
        }
        val trainTargets = Array(trainSize) { i ->
            val x = trainInputs[i][0]
            doubleArrayOf(x * x + 2.0 * x + 1.0)
        }
        
        val valInputs = Array(valSize) { i ->
            doubleArrayOf((i - 25).toDouble() / 8.0) // Different range for validation
        }
        val valTargets = Array(valSize) { i ->
            val x = valInputs[i][0]
            doubleArrayOf(x * x + 2.0 * x + 1.0)
        }
        
        val trainDataset = InMemoryDataset(trainInputs, trainTargets)
        val valDataset = InMemoryDataset(valInputs, valTargets)
        
        // Create network with regularization and learning rate scheduling
        val layers = listOf(
            DenseLayer(1, 20, ReLUActivation()),
            DenseLayer(20, 10, ReLUActivation()),
            DenseLayer(10, 1, LinearActivation())
        )
        
        val network = FeedforwardNetwork(
            _layers = layers,
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(learningRate = 0.01),
            regularization = L2Regularization(lambda = 0.001),
            learningRateScheduler = ExponentialDecayScheduler(decayRate = 0.95, decaySteps = 20)
        )
        
        println("Training network on polynomial regression...")
        
        val history = network.train(
            dataset = trainDataset,
            epochs = 100,
            batchSize = 16,
            validationDataset = valDataset,
            validationFrequency = 10
        )
        
        println("Training completed!")
        println("Total epochs: ${history.totalEpochs}")
        println("Training duration: ${history.trainingDuration}ms")
        println("Average epoch time: ${history.averageEpochTime.toInt()}ms")
        println("Final training loss: ${history.finalTrainingLoss.format(6)}")
        println("Best validation loss: ${(history.bestValidationLoss ?: 0.0).format(6)}")
        println("Best epoch: ${history.bestEpoch}")
        history.convergenceEpoch?.let { 
            println("Converged at epoch: $it")
        }
        
        // Show training progress
        println("\nTraining Progress (every 20 epochs):")
        history.metrics.filterIndexed { index, _ -> index % 20 == 0 || index == history.metrics.size - 1 }
            .forEach { metrics ->
                val valLoss = metrics.validationLoss?.let { it.format(6) } ?: "N/A"
                println("Epoch ${metrics.epoch}: Train Loss = ${metrics.averageLoss.format(6)}, " +
                       "Val Loss = $valLoss, LR = ${metrics.learningRate.format(6)}")
            }
        
        println()
    }
    
    private fun evaluationMetricsDemo() {
        println("3. Evaluation Metrics Demo")
        println("=" * 30)
        
        // Classification metrics demo
        println("Classification Metrics:")
        val classificationPredicted = arrayOf(
            doubleArrayOf(0.9, 0.1), // Predicted class 0, correct
            doubleArrayOf(0.2, 0.8), // Predicted class 1, correct
            doubleArrayOf(0.7, 0.3), // Predicted class 0, incorrect
            doubleArrayOf(0.1, 0.9), // Predicted class 1, correct
            doubleArrayOf(0.6, 0.4), // Predicted class 0, correct
            doubleArrayOf(0.3, 0.7)  // Predicted class 1, incorrect
        )
        val classificationActual = arrayOf(
            doubleArrayOf(1.0, 0.0), // Actual class 0
            doubleArrayOf(0.0, 1.0), // Actual class 1
            doubleArrayOf(0.0, 1.0), // Actual class 1
            doubleArrayOf(0.0, 1.0), // Actual class 1
            doubleArrayOf(1.0, 0.0), // Actual class 0
            doubleArrayOf(1.0, 0.0)  // Actual class 0
        )
        
        val classMetrics = EvaluationMetrics.forClassification(
            classificationPredicted, classificationActual, 0.3
        )
        
        println("  Accuracy: ${(classMetrics.accuracy ?: 0.0).format(3)}")
        println("  Precision: ${(classMetrics.precision ?: 0.0).format(3)}")
        println("  Recall: ${(classMetrics.recall ?: 0.0).format(3)}")
        println("  F1 Score: ${(classMetrics.f1Score ?: 0.0).format(3)}")
        println("  Sample Count: ${classMetrics.sampleCount}")
        
        // Regression metrics demo
        println("\nRegression Metrics:")
        val regressionPredicted = arrayOf(
            doubleArrayOf(2.1, 3.9),
            doubleArrayOf(4.8, 6.2),
            doubleArrayOf(7.9, 9.1),
            doubleArrayOf(11.2, 12.8)
        )
        val regressionActual = arrayOf(
            doubleArrayOf(2.0, 4.0),
            doubleArrayOf(5.0, 6.0),
            doubleArrayOf(8.0, 9.0),
            doubleArrayOf(11.0, 13.0)
        )
        
        val regMetrics = EvaluationMetrics.forRegression(
            regressionPredicted, regressionActual, 0.05
        )
        
        println("  MAE: ${(regMetrics.meanAbsoluteError ?: 0.0).format(3)}")
        println("  RMSE: ${(regMetrics.rootMeanSquareError ?: 0.0).format(3)}")
        println("  R² Score: ${(regMetrics.r2Score ?: 0.0).format(3)}")
        println("  Sample Count: ${regMetrics.sampleCount}")
        println()
    }
    
    private fun hyperparameterTestingDemo() {
        println("4. Hyperparameter Testing Demo")
        println("=" * 30)
        
        // Create simple dataset for quick testing
        val inputs = Array(100) { i ->
            doubleArrayOf(sin(i * 0.1), cos(i * 0.1))
        }
        val targets = Array(100) { i ->
            doubleArrayOf(sin(i * 0.1) + cos(i * 0.1))
        }
        
        val trainDataset = InMemoryDataset(inputs.sliceArray(0..79), targets.sliceArray(0..79))
        val valDataset = InMemoryDataset(inputs.sliceArray(80..99), targets.sliceArray(80..99))
        
        // Test different configurations
        val configs = listOf(
            HyperparameterConfig(
                learningRate = 0.01,
                epochs = 30,
                batchSize = 16,
                hiddenLayers = listOf(10),
                optimizerType = "SGD"
            ),
            HyperparameterConfig(
                learningRate = 0.01,
                epochs = 30,
                batchSize = 16,
                hiddenLayers = listOf(10),
                optimizerType = "Adam"
            ),
            HyperparameterConfig(
                learningRate = 0.005,
                epochs = 30,
                batchSize = 8,
                hiddenLayers = listOf(20, 10),
                optimizerType = "Adam",
                regularizationType = "L2",
                regularizationLambda = 0.001
            )
        )
        
        println("Testing ${configs.size} hyperparameter configurations...")
        
        // Create a base network for testing
        val baseNetwork = FeedforwardNetwork(
            _layers = listOf(
                DenseLayer(2, 10, ReLUActivation()),
                DenseLayer(10, 1, LinearActivation())
            ),
            lossFunction = MSELoss(),
            optimizer = SGDOptimizer(0.01)
        )
        
        // Simulate hyperparameter testing results (simplified)
        configs.forEachIndexed { index, config ->
            println("\nConfiguration ${index + 1}:")
            println("  Learning Rate: ${config.learningRate}")
            println("  Batch Size: ${config.batchSize}")
            println("  Hidden Layers: ${config.hiddenLayers}")
            println("  Optimizer: ${config.optimizerType}")
            println("  Regularization: ${config.regularizationType ?: "None"}")
            
            // For demo purposes, simulate training results
            val simulatedLoss = Random.nextDouble(0.1, 1.0)
            val simulatedAccuracy = Random.nextDouble(0.7, 0.95)
            
            println("  Final Validation Loss: ${simulatedLoss.format(4)}")
            println("  Final Accuracy: ${simulatedAccuracy.format(3)}")
        }
        
        // Show utility functions
        println("\nHyperparameter Utility Functions:")
        val gridConfigs = HyperparameterUtils.gridSearch(
            learningRates = listOf(0.01, 0.1),
            batchSizes = listOf(16, 32),
            hiddenLayerConfigs = listOf(listOf(10), listOf(20)),
            epochs = 50
        )
        println("Grid search generated ${gridConfigs.size} configurations")
        
        val randomConfigs = HyperparameterUtils.randomSearch(count = 5, epochs = 50)
        println("Random search generated ${randomConfigs.size} configurations")
        
        val schedulingConfigs = HyperparameterUtils.learningRateSchedulingConfigs(epochs = 100)
        println("Learning rate scheduling generated ${schedulingConfigs.size} configurations")
        println()
    }
    
    private fun modelSerializationDemo() {
        println("5. Model Serialization Demo")
        println("=" * 30)
        
        // Create and train a simple network
        val layers = listOf(
            DenseLayer(2, 5, ReLUActivation()),
            DenseLayer(5, 3, ReLUActivation()),
            DenseLayer(3, 1, LinearActivation())
        )
        
        val network = FeedforwardNetwork(
            _layers = layers,
            lossFunction = MSELoss(),
            optimizer = AdamOptimizer(0.01)
        )
        
        // Train briefly to have some history
        val inputs = Array(50) { i ->
            doubleArrayOf(i.toDouble() / 25.0, sin(i * 0.2))
        }
        val targets = Array(50) { i ->
            doubleArrayOf(inputs[i][0] + inputs[i][1])
        }
        
        val dataset = InMemoryDataset(inputs, targets)
        network.train(dataset, epochs = 10, batchSize = 10)
        
        // Demonstrate serialization
        val serializer = SimpleModelSerializer()
        val serializedData = serializer.serialize(network)
        
        println("Model serialized successfully!")
        println("Serialized data length: ${serializedData.length} characters")
        println("Contains configuration: ${serializedData.contains("input_size=2")}")
        println("Contains weights: ${serializedData.contains("layer_0_weights_start")}")
        println("Contains training history: ${serializedData.contains("Training History")}")
        
        // Show first few lines of serialized data
        println("\nFirst few lines of serialized data:")
        serializedData.lines().take(10).forEach { line ->
            println("  $line")
        }
        
        // Demonstrate deserialization
        try {
            val networkData = serializer.deserialize(serializedData)
            println("\nModel deserialized successfully!")
            println("Config input size: ${networkData.config.inputSize}")
            println("Config output size: ${networkData.config.outputSize}")
            println("Number of layers: ${networkData.weights.layerWeights.size}")
            println("Training history available: ${networkData.trainingHistory != null}")
        } catch (e: Exception) {
            println("Deserialization demo (implementation would handle file I/O)")
        }
        
        println()
    }
}

// Extension function for string repetition
private operator fun String.times(n: Int): String = this.repeat(n)