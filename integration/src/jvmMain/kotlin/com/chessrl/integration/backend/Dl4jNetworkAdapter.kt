package com.chessrl.integration.backend

import com.chessrl.rl.NeuralNetwork
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.GradientNormalization
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.learning.config.IUpdater
import org.nd4j.linalg.learning.config.RmsProp
import org.nd4j.linalg.learning.config.Sgd
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.nd4j.linalg.ops.transforms.Transforms
import java.io.File
import kotlin.math.abs
import com.chessrl.integration.writeTextFile

/**
 * DL4J-based neural network adapter implementation.
 * Provides optimized neural network operations using the DeepLearning4J library.
 * 
 * ## Data Type Mapping and Precision
 * 
 * This adapter handles the conversion between Kotlin's DoubleArray and DL4J's INDArray:
 * - **Input Conversion**: DoubleArray → INDArray (float32 precision by default in DL4J)
 * - **Precision Loss**: DL4J defaults to float32, so double precision is reduced to float32
 * - **Batching**: Multiple DoubleArrays are stacked into a single INDArray with shape [batchSize, inputSize]
 * - **Masking Compatibility**: Action masking is preserved through consistent array indexing
 * - **Value/Policy Heads**: Output dimensions remain consistent (4096 for chess action space)
 * 
 * ## Memory Layout
 * - Input: [batchSize, 839] for chess board features
 * - Output: [batchSize, 4096] for chess action Q-values
 * - Internal: All computations use float32 for performance
 * 
 * ## Threading Considerations
 * - DL4J operations are thread-safe for inference
 * - Training operations should be synchronized externally if needed
 */
class Dl4jNetworkAdapter(
    private val config: BackendConfig
) : NetworkAdapter {
    
    private var network: MultiLayerNetwork = createMultiLayerNetwork(config)
    
    init {
        network.init()
        // Add score listener for debugging (optional)
        network.setListeners(ScoreIterationListener(100))
    }
    
    override fun forward(input: DoubleArray): DoubleArray {
        require(input.size == config.inputSize) { 
            "Input size ${input.size} doesn't match expected ${config.inputSize}" 
        }
        
        // Convert DoubleArray to INDArray with explicit float32 precision
        // Note: DL4J uses float32 by default, so double precision is reduced
        val inputArray = convertDoubleArrayToINDArray(input, 1, config.inputSize)
        
        val output = network.output(inputArray)
        val result = convertINDArrayToDoubleArray(output)
        
        require(result.size == config.outputSize) { 
            "Output size ${result.size} doesn't match expected ${config.outputSize}" 
        }
        
        // Validate output contains finite values
        require(result.all { it.isFinite() }) {
            "Forward pass produced non-finite values"
        }
        
        return result
    }
    
    override fun backward(target: DoubleArray): DoubleArray {
        throw UnsupportedOperationException(
            "DL4J adapter trains via trainBatch; backward() is not supported"
        )
    }
    
    override fun trainBatch(inputs: Array<DoubleArray>, targets: Array<DoubleArray>): Double {
        require(inputs.size == targets.size) { "Batch inputs and targets must have same size" }
        require(inputs.isNotEmpty()) { "Batch cannot be empty" }
        
        val batchSize = inputs.size
        
        // Validate input dimensions
        for (i in inputs.indices) {
            require(inputs[i].size == config.inputSize) {
                "Input $i size ${inputs[i].size} doesn't match expected ${config.inputSize}"
            }
            require(targets[i].size == config.outputSize) {
                "Target $i size ${targets[i].size} doesn't match expected ${config.outputSize}"
            }
        }
        
        // Convert batch data to INDArray format with proper precision handling
        // DL4J uses float32 internally, so we convert from double to float precision
        val inputBatch = convertBatchToINDArray(inputs, batchSize, config.inputSize)
        val targetBatch = convertBatchToINDArray(targets, batchSize, config.outputSize)
        
        // Validate input data contains finite values
        require(isINDArrayFinite(inputBatch)) {
            "Input batch contains non-finite values"
        }
        require(isINDArrayFinite(targetBatch)) {
            "Target batch contains non-finite values"
        }
        
        // Create DataSet for training
        val dataSet = DataSet(inputBatch, targetBatch)
        
        // Perform forward pass to get predictions before training
        val predictionsBefore = network.output(inputBatch)
        
        // Train the network with gradient clipping applied via configuration
        network.fit(dataSet)
        
        // Calculate loss for monitoring using the configured loss function
        val loss = when (config.lossFunction.lowercase()) {
            "huber" -> computeHuberLoss(predictionsBefore, targetBatch, delta = 1.0)
            "mse" -> computeMSELoss(predictionsBefore, targetBatch)
            else -> computeHuberLoss(predictionsBefore, targetBatch, delta = 1.0)
        }
        
        // Validate that training didn't produce invalid gradients
        require(validateGradients()) {
            "Training produced invalid gradients (NaN or infinite values)"
        }
        
        return loss
    }
    
    override fun copyWeightsTo(target: NeuralNetwork) {
        if (target is Dl4jNetworkAdapter) {
            try {
                // Serialize the source network (including updater state) and restore into the target
                val byteArray = java.io.ByteArrayOutputStream().use { baos ->
                    org.deeplearning4j.util.ModelSerializer.writeModel(network, baos, true)
                    baos.toByteArray()
                }

                val restored = java.io.ByteArrayInputStream(byteArray).use { bais ->
                    org.deeplearning4j.util.ModelSerializer.restoreMultiLayerNetwork(bais, true)
                }

                target.network = restored

            } catch (e: Exception) {
                throw RuntimeException("Failed to copy weights to DL4J target network: ${e.message}", e)
            }
        } else {
            throw UnsupportedOperationException(
                "Cannot copy weights from DL4J backend to ${target::class.simpleName} backend. " +
                "Cross-backend weight synchronization is not supported. " +
                "Use the same backend type for both source and target networks."
            )
        }
    }
    
    override fun save(path: String) {
        try {
            // Generate appropriate save path with correct extension for DL4J backend
            val savePath = CheckpointCompatibility.generateSavePath(path, BackendType.DL4J)
            
            // Save with updater state for complete checkpoint preservation
            // This includes optimizer state (momentum buffers, velocity buffers, etc.)
            org.deeplearning4j.util.ModelSerializer.writeModel(network, File(savePath), true)
            
            // Save additional metadata for compatibility checking
            saveModelMetadata(savePath)
            
        } catch (e: Exception) {
            throw RuntimeException(
                "Failed to save DL4J model to $path: ${e.message}. " +
                "Ensure the directory exists and you have write permissions. " +
                "DL4J models are saved in ZIP format.", 
                e
            )
        }
    }
    
    override fun load(path: String) {
        try {
            // Resolve checkpoint path and validate compatibility
            val resolution = CheckpointCompatibility.resolveCheckpointPath(path, BackendType.DL4J)
            
            when (resolution) {
                is CheckpointResolution.Success -> {
                    // Validate file exists and is readable
                    val file = File(resolution.path)
                    require(file.exists()) {
                        "DL4J model file not found: ${resolution.path}. " +
                        "DL4J models are saved in ZIP format."
                    }
                    require(file.canRead()) {
                        "Cannot read DL4J model file: ${resolution.path}. Check file permissions."
                    }
                    
                    // Load the network with updater state
                    network = org.deeplearning4j.util.ModelSerializer.restoreMultiLayerNetwork(file)
                    
                    // Validate loaded network configuration matches expected configuration
                    validateLoadedNetwork()
                }
                is CheckpointResolution.FormatMismatch -> {
                    throw RuntimeException(
                        CheckpointCompatibility.getFormatMismatchMessage(resolution.path, BackendType.DL4J)
                    )
                }
                is CheckpointResolution.NotFound -> {
                    throw RuntimeException(resolution.message)
                }
            }
            
        } catch (e: Exception) {
            // Re-throw RuntimeExceptions as-is to preserve helpful error messages
            if (e is RuntimeException) {
                throw e
            }
            
            // Provide helpful error messages for other exceptions
            val errorMessage = when {
                e.message?.contains("ZIP") == true || e.message?.contains("zip") == true -> {
                    "Failed to load DL4J model from ZIP file: ${e.message}. " +
                    "The file may be corrupted or incompatible."
                }
                else -> {
                    "Failed to load DL4J model from $path: ${e.message}"
                }
            }
            throw RuntimeException(errorMessage, e)
        }
    }
    
    override fun getBackendName(): String = "dl4j"
    override fun getConfig(): BackendConfig = config
    
    override fun getParameterCount(): Long = network.numParams()
    
    override fun getMemoryUsage(): Long {
        // Estimate memory usage based on parameters and workspace
        val paramMemory = network.numParams() * 4 // 4 bytes per float
        val workspaceMemory = config.batchSize * (config.inputSize + config.outputSize) * 4
        return paramMemory + workspaceMemory
    }
    
    override fun validateGradients(): Boolean {
        // Basic gradient validation - check if parameters are finite
        val params = network.params()
        // Check for NaN or infinite values in parameters
        val paramsArray = params.toDoubleVector()
        return paramsArray.all { it.isFinite() }
    }
    
    /**
     * Create MultiLayerNetwork with the specified configuration.
     * Includes proper Adam optimizer setup, L2 regularization, and gradient clipping.
     */
    private fun createMultiLayerNetwork(config: BackendConfig): MultiLayerNetwork {
        val builder = NeuralNetConfiguration.Builder()
            .seed(config.seed)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .updater(createUpdater(config))
            .l2(config.l2Regularization)
            // Add gradient clipping for DQN stability
            .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
            .gradientNormalizationThreshold(config.gradientClipping)
            .list()
        
        // Build network layers based on configuration
        var layerIndex = 0
        if (config.hiddenLayers.isNotEmpty()) {
            // Input layer (first hidden layer)
            builder.layer(layerIndex++, DenseLayer.Builder()
                .nIn(config.inputSize)
                .nOut(config.hiddenLayers[0])
                .activation(Activation.RELU)
                .weightInit(getWeightInit(config.weightInitType))
                .build())

            // Additional hidden layers
            for (i in 1 until config.hiddenLayers.size) {
                builder.layer(layerIndex++, DenseLayer.Builder()
                    .nIn(config.hiddenLayers[i - 1])
                    .nOut(config.hiddenLayers[i])
                    .activation(Activation.RELU)
                    .weightInit(getWeightInit(config.weightInitType))
                    .build())
            }

            // Output layer with identity activation (for Q-values)
            // Note: We use MSE as base loss function since DL4J doesn't have built-in Huber loss
            // Huber loss is computed manually in the loss calculation methods
            builder.layer(layerIndex, OutputLayer.Builder()
                .nIn(config.hiddenLayers.last())
                .nOut(config.outputSize)
                .activation(Activation.IDENTITY)
                .lossFunction(LossFunctions.LossFunction.MSE) // Base loss, Huber computed manually
                .weightInit(getWeightInit(config.weightInitType))
                .build())
        } else {
            // No hidden layers: direct input to output
            builder.layer(layerIndex, OutputLayer.Builder()
                .nIn(config.inputSize)
                .nOut(config.outputSize)
                .activation(Activation.IDENTITY)
                .lossFunction(LossFunctions.LossFunction.MSE) // Base loss, Huber computed manually
                .weightInit(getWeightInit(config.weightInitType))
                .build())
        }

        return MultiLayerNetwork(builder.build())
    }
    
    /**
     * Create optimizer based on configuration
     */
    private fun createUpdater(config: BackendConfig): IUpdater {
        return when (config.optimizer.lowercase()) {
            "adam" -> Adam.builder()
                .learningRate(config.learningRate)
                .beta1(config.beta1)
                .beta2(config.beta2)
                .epsilon(config.epsilon)
                .build()
            "sgd" -> Sgd.builder()
                .learningRate(config.learningRate)
                .build()
            "rmsprop" -> RmsProp.builder()
                .learningRate(config.learningRate)
                .rmsDecay(config.beta2)
                .epsilon(config.epsilon)
                .build()
            else -> Adam.builder()
                .learningRate(config.learningRate)
                .beta1(config.beta1)
                .beta2(config.beta2)
                .epsilon(config.epsilon)
                .build()
        }
    }
    
    /**
     * Get weight initialization method
     */
    private fun getWeightInit(weightInitType: String): WeightInit {
        return when (weightInitType.lowercase()) {
            "he" -> WeightInit.RELU
            "xavier" -> WeightInit.XAVIER
            "uniform" -> WeightInit.UNIFORM
            else -> WeightInit.RELU
        }
    }
    

    
    // ========== Data Conversion Methods ==========
    
    /**
     * Convert DoubleArray to INDArray with proper shape and precision handling.
     * DL4J uses float32 internally, so double precision is reduced.
     */
    private fun convertDoubleArrayToINDArray(input: DoubleArray, batchSize: Int, featureSize: Int): INDArray {
        val array = Nd4j.create(batchSize.toLong(), featureSize.toLong())
        for (i in input.indices) {
            array.putScalar(longArrayOf(0, i.toLong()), input[i])
        }
        return array
    }
    
    /**
     * Convert batch of DoubleArrays to INDArray with shape [batchSize, featureSize].
     * Maintains consistent indexing for action masking compatibility.
     */
    private fun convertBatchToINDArray(batch: Array<DoubleArray>, batchSize: Int, featureSize: Int): INDArray {
        val array = Nd4j.create(batchSize.toLong(), featureSize.toLong())
        for (i in batch.indices) {
            for (j in batch[i].indices) {
                array.putScalar(longArrayOf(i.toLong(), j.toLong()), batch[i][j])
            }
        }
        return array
    }
    
    /**
     * Convert INDArray back to DoubleArray, handling precision conversion.
     */
    private fun convertINDArrayToDoubleArray(array: INDArray): DoubleArray {
        return array.toDoubleVector()
    }
    
    /**
     * Check if INDArray contains only finite values (no NaN or infinite values).
     */
    private fun isINDArrayFinite(array: INDArray): Boolean {
        val flattened = array.reshape(-1)
        val length = flattened.length()
        for (i in 0 until length.toInt()) {
            val value = flattened.getDouble(i.toLong())
            if (!value.isFinite()) {
                return false
            }
        }
        return true
    }
    
    // ========== Loss Function Implementations ==========
    
    /**
     * Compute Huber loss manually for DQN compatibility.
     * Huber loss is more robust to outliers than MSE, which is important for Q-learning.
     * 
     * Formula:
     * - If |error| <= delta: 0.5 * error^2
     * - If |error| > delta: delta * |error| - 0.5 * delta^2
     * 
     * @param predictions Network predictions
     * @param targets Target Q-values
     * @param delta Huber loss threshold (typically 1.0 for DQN)
     */
    private fun computeHuberLoss(predictions: INDArray, targets: INDArray, delta: Double = 1.0): Double {
        require(predictions.shape().contentEquals(targets.shape())) {
            "Predictions and targets must have same shape"
        }
        
        val diff = predictions.sub(targets)
        val absDiff = Transforms.abs(diff)
        
        // Flatten arrays for element-wise computation
        val flatAbsDiff = absDiff.reshape(-1)
        val flatDiff = diff.reshape(-1)
        
        var totalLoss = 0.0
        val numElements = flatAbsDiff.length()
        
        for (i in 0 until numElements.toInt()) {
            val absDiffVal = flatAbsDiff.getDouble(i.toLong())
            val diffVal = flatDiff.getDouble(i.toLong())
            
            totalLoss += if (absDiffVal <= delta) {
                // Quadratic region: 0.5 * error^2
                0.5 * diffVal * diffVal
            } else {
                // Linear region: delta * |error| - 0.5 * delta^2
                delta * absDiffVal - 0.5 * delta * delta
            }
        }
        
        return totalLoss / numElements.toDouble()
    }
    
    /**
     * Compute Mean Squared Error loss.
     */
    private fun computeMSELoss(predictions: INDArray, targets: INDArray): Double {
        require(predictions.shape().contentEquals(targets.shape())) {
            "Predictions and targets must have same shape"
        }
        
        val diff = predictions.sub(targets)
        val squaredDiff = diff.mul(diff)
        return squaredDiff.meanNumber().toDouble()
    }
    
    // ========== Checkpoint Compatibility and Persistence Methods ==========
    
    /**
     * Save model metadata for compatibility checking and debugging.
     * This helps identify the backend type and configuration when loading models.
     */
    private fun saveModelMetadata(modelPath: String) {
        try {
            val metadataPath = "${modelPath}.meta.json"
            val metadata = buildString {
                appendLine("{")
                appendLine("  \"backend\": \"dl4j\",")
                appendLine("  \"version\": \"1.0\",")
                appendLine("  \"format\": \"zip\",")
                appendLine("  \"timestamp\": \"${System.currentTimeMillis()}\",")
                appendLine("  \"config\": {")
                appendLine("    \"inputSize\": ${config.inputSize},")
                appendLine("    \"outputSize\": ${config.outputSize},")
                appendLine("    \"hiddenLayers\": [${config.hiddenLayers.joinToString(", ")}],")
                appendLine("    \"learningRate\": ${config.learningRate},")
                appendLine("    \"optimizer\": \"${config.optimizer}\",")
                appendLine("    \"lossFunction\": \"${config.lossFunction}\"")
                appendLine("  },")
                appendLine("  \"parameterCount\": ${getParameterCount()}")
                appendLine("}")
            }
            
            writeTextFile(metadataPath, metadata)
        } catch (e: Exception) {
            // Metadata save is best-effort, don't fail the main save operation
            println("Warning: Failed to save model metadata: ${e.message}")
        }
    }
    

    
    /**
     * Validate that the loaded network configuration matches expected configuration.
     * This helps catch configuration mismatches early.
     */
    private fun validateLoadedNetwork() {
        try {
            // Check input/output dimensions by performing a test forward pass
            val testInput = DoubleArray(config.inputSize) { 0.0 }
            val testOutput = forward(testInput)
            
            require(testOutput.size == config.outputSize) {
                "Loaded network output size (${testOutput.size}) doesn't match expected size (${config.outputSize}). " +
                "The loaded model may have been trained with different configuration."
            }
            
            // Validate parameter count matches expected architecture
            val actualParamCount = getParameterCount()
            val expectedParamCount = calculateExpectedParameterCount()
            
            if (actualParamCount != expectedParamCount) {
                println("Warning: Loaded network parameter count ($actualParamCount) differs from expected ($expectedParamCount). " +
                       "This may indicate a different network architecture.")
            }
            
        } catch (e: Exception) {
            throw RuntimeException("Loaded network validation failed: ${e.message}", e)
        }
    }
    
    /**
     * Calculate expected parameter count based on configuration.
     * Used for validation when loading models.
     */
    private fun calculateExpectedParameterCount(): Long {
        var paramCount = 0L
        
        if (config.hiddenLayers.isNotEmpty()) {
            // Input to first hidden layer
            paramCount += (config.inputSize + 1) * config.hiddenLayers[0] // +1 for bias
            
            // Hidden to hidden layers
            for (i in 1 until config.hiddenLayers.size) {
                paramCount += (config.hiddenLayers[i-1] + 1) * config.hiddenLayers[i] // +1 for bias
            }
            
            // Last hidden to output
            paramCount += (config.hiddenLayers.last() + 1) * config.outputSize // +1 for bias
        } else {
            // Direct input to output
            paramCount += (config.inputSize + 1) * config.outputSize // +1 for bias
        }
        
        return paramCount
    }
}
