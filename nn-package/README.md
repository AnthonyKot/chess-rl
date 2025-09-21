# Neural Network Package

A clean, focused neural network implementation in Kotlin designed specifically for reinforcement learning applications. This package provides feed-forward networks with essential training algorithms and model serialization.

## Purpose

The neural network package serves as the function approximator for the DQN algorithm in the chess RL system. It provides:

- **Feed-forward Networks**: Configurable hidden layers with ReLU activation
- **Training Algorithms**: Backpropagation with Adam optimizer
- **Model Serialization**: Save/load trained models in JSON format
- **Batch Processing**: Efficient batch training support

## Key Classes

### NeuralNetwork
The core neural network implementation with configurable architecture.

```kotlin
// Create a network with custom architecture
val network = NeuralNetwork(
    inputSize = 776,  // Chess position encoding
    hiddenLayers = listOf(512, 256, 128),
    outputSize = 4096,  // Chess action space
    learningRate = 0.001
)

// Forward pass
val input = doubleArrayOf(/* chess position features */)
val output = network.forward(input)

// Training step
val target = doubleArrayOf(/* target Q-values */)
val loss = network.train(input, target)
```

### Key Methods
- `forward(input: DoubleArray): DoubleArray`: Forward pass through network
- `train(input: DoubleArray, target: DoubleArray): Double`: Training step with backpropagation
- `saveToJson(): String`: Serialize model to JSON
- `loadFromJson(json: String)`: Load model from JSON
- `copy(): NeuralNetwork`: Create a copy of the network

## Usage Examples

### Basic Network Training

```kotlin
import com.chessrl.nn.NeuralNetwork

// Create network for chess RL
val network = NeuralNetwork(
    inputSize = 776,
    hiddenLayers = listOf(512, 256, 128),
    outputSize = 4096,
    learningRate = 0.001
)

// Training loop
for (epoch in 1..1000) {
    val input = generateChessPosition()
    val target = calculateTargetQValues()
    
    val loss = network.train(input, target)
    
    if (epoch % 100 == 0) {
        println("Epoch $epoch, Loss: $loss")
    }
}
```

### Model Serialization

```kotlin
// Save trained model
val modelJson = network.saveToJson()
File("model.json").writeText(modelJson)

// Load model
val loadedNetwork = NeuralNetwork(776, listOf(512, 256, 128), 4096)
val json = File("model.json").readText()
loadedNetwork.loadFromJson(json)

// Verify loaded model works
val testInput = doubleArrayOf(/* test data */)
val output = loadedNetwork.forward(testInput)
```

### Batch Processing

```kotlin
// Process multiple inputs efficiently
val batchInputs = listOf(
    doubleArrayOf(/* position 1 */),
    doubleArrayOf(/* position 2 */),
    doubleArrayOf(/* position 3 */)
)

val batchOutputs = batchInputs.map { input ->
    network.forward(input)
}
```

## API Reference

### NeuralNetwork Constructor
```kotlin
NeuralNetwork(
    inputSize: Int,           // Input layer size
    hiddenLayers: List<Int>,  // Hidden layer sizes
    outputSize: Int,          // Output layer size
    learningRate: Double = 0.001  // Learning rate for Adam optimizer
)
```

### Core Methods
- `forward(input: DoubleArray): DoubleArray`
  - Performs forward pass through the network
  - Returns network output (Q-values for RL)

- `train(input: DoubleArray, target: DoubleArray): Double`
  - Performs one training step with backpropagation
  - Returns mean squared error loss

- `saveToJson(): String`
  - Serializes network weights and architecture to JSON
  - Includes metadata for verification

- `loadFromJson(json: String)`
  - Loads network weights from JSON string
  - Validates architecture compatibility

- `copy(): NeuralNetwork`
  - Creates a deep copy of the network
  - Useful for target networks in DQN

## Integration Points

### With RL Framework
The neural network integrates seamlessly with the RL framework:

```kotlin
// Used in DQN algorithm
val qNetwork = NeuralNetwork(stateSize, hiddenLayers, actionSize)
val targetNetwork = qNetwork.copy()

// Q-value prediction
val qValues = qNetwork.forward(state)
val action = qValues.indices.maxByOrNull { qValues[it] } ?: 0

// Training update
val targetQValues = calculateTargets(experiences, targetNetwork)
val loss = qNetwork.train(state, targetQValues)
```

### With Chess Environment
Designed specifically for chess state encoding:

```kotlin
// Chess-specific network architecture
val chessNetwork = NeuralNetwork(
    inputSize = 776,    // 8x8x12 + additional features
    hiddenLayers = listOf(768, 512, 256),  // Balanced architecture
    outputSize = 4096,  // All possible chess moves
    learningRate = 0.0005
)

// Process chess position
val encodedPosition = chessEnvironment.encodeState()
val qValues = chessNetwork.forward(encodedPosition)
val bestMove = selectBestLegalMove(qValues, legalMoves)
```

## Performance Characteristics

### Training Performance
- **Forward Pass**: ~1-2ms for typical chess network
- **Training Step**: ~3-5ms including backpropagation
- **Memory Usage**: ~50-100MB for standard architecture
- **Batch Processing**: Linear scaling with batch size

### Architecture Recommendations
- **Small Networks**: [256, 128] for fast training
- **Balanced Networks**: [512, 256, 128] for good performance
- **Large Networks**: [768, 512, 256] for maximum capacity

## Testing

The package includes comprehensive tests covering:

- ✅ **XOR Learning**: Validates basic learning capability
- ✅ **Gradient Updates**: Ensures proper backpropagation
- ✅ **Model Serialization**: Round-trip save/load testing
- ✅ **Architecture Validation**: Input/output size checking
- ✅ **Numerical Stability**: Gradient explosion/vanishing detection

Run tests with:
```bash
./gradlew :nn-package:test
```

## Implementation Details

### Network Architecture
- **Activation Function**: ReLU for hidden layers, linear for output
- **Optimizer**: Adam with default parameters (β₁=0.9, β₂=0.999)
- **Weight Initialization**: Xavier/Glorot initialization
- **Loss Function**: Mean Squared Error (MSE)

### Numerical Stability
- Gradient clipping to prevent explosion
- Proper weight initialization to prevent vanishing gradients
- Numerical precision handling for edge cases

### Memory Management
- Efficient matrix operations
- Minimal memory allocation during forward/backward passes
- Proper cleanup of intermediate computations

## Future Enhancements

Potential improvements for advanced use cases:
- **Additional Optimizers**: RMSprop, SGD with momentum
- **Regularization**: L1/L2 regularization, dropout
- **Advanced Architectures**: Convolutional layers for board representation
- **Hardware Acceleration**: GPU support for large-scale training

## Dependencies

- Kotlin Multiplatform
- Kotlin Test Framework
- No external dependencies for core functionality

## Platform Support

- ✅ **JVM**: Full support with optimized performance
- ✅ **Native**: Kotlin/Native compatibility
- ✅ **Cross-platform**: Consistent behavior across platforms

The neural network package provides a solid foundation for reinforcement learning applications while maintaining simplicity and reliability.