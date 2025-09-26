# DL4J Data Mapping and Precision Guide

## Overview

This document explains how the DL4J neural network adapter handles data conversion between Kotlin's `DoubleArray`/`FloatArray` and DL4J's `INDArray`, with special attention to precision, masking, value/policy heads, and batching consistency.

## Data Type Mapping

### Input Conversion: DoubleArray → INDArray

```kotlin
// Kotlin input
val input: DoubleArray = doubleArrayOf(0.1, 0.2, 0.3, ...) // Size: 839 for chess

// DL4J conversion
val inputArray = Nd4j.create(batchSize.toLong(), inputSize.toLong())
for (i in input.indices) {
    inputArray.putScalar(longArrayOf(0, i.toLong()), input[i])
}
```

**Key Points:**
- **Precision Loss**: DL4J defaults to `float32` precision, so `double` values are automatically converted to `float`
- **Shape**: Input is reshaped to `[batchSize, inputSize]` format (e.g., `[1, 839]` for single inference)
- **Memory Layout**: Row-major order is preserved for consistent indexing

### Output Conversion: INDArray → DoubleArray

```kotlin
// DL4J output
val output: INDArray = network.output(inputArray) // Shape: [batchSize, outputSize]

// Kotlin conversion
val result: DoubleArray = output.toDoubleVector() // Size: 4096 for chess actions
```

**Key Points:**
- **Precision Recovery**: `float32` values are converted back to `double`, but original precision is not recovered
- **Flattening**: Multi-dimensional output is flattened to 1D array for compatibility with existing RL framework

## Batch Processing

### Batch Data Conversion

```kotlin
// Input: Array of DoubleArrays
val inputs: Array<DoubleArray> = arrayOf(
    doubleArrayOf(0.1, 0.2, ...), // Sample 1
    doubleArrayOf(0.3, 0.4, ...), // Sample 2
    // ... more samples
)

// DL4J batch conversion
val batchSize = inputs.size
val inputBatch = Nd4j.create(batchSize.toLong(), inputSize.toLong())
for (i in inputs.indices) {
    for (j in inputs[i].indices) {
        inputBatch.putScalar(longArrayOf(i.toLong(), j.toLong()), inputs[i][j])
    }
}
```

**Batch Shape Consistency:**
- Input batch: `[batchSize, 839]` for chess board features
- Output batch: `[batchSize, 4096]` for chess action Q-values
- Target batch: `[batchSize, 4096]` for training targets

## Action Masking Compatibility

### Index Preservation

The DL4J adapter maintains consistent array indexing to ensure action masking works correctly:

```kotlin
// Original action masking (manual backend)
val qValues = network.forward(boardState) // DoubleArray[4096]
val validActions = getValidMoves(board)   // List<Int>
for (i in qValues.indices) {
    if (i !in validActions) {
        qValues[i] = Double.NEGATIVE_INFINITY // Mask invalid actions
    }
}

// DL4J backend compatibility
val qValues = dl4jAdapter.forward(boardState) // DoubleArray[4096] - same indexing!
// Same masking logic works unchanged
```

**Consistency Guarantees:**
- Action index `i` in the output array corresponds to the same chess move across all backends
- Q-value ordering is preserved: `qValues[0]` always represents the same action
- Masking operations work identically regardless of internal `float32` precision

## Value/Policy Head Compatibility

### Single Output Head (Current Implementation)

```kotlin
// Network architecture: 839 → [512, 256] → 4096
// Single output head for Q-values (value estimates for each action)
val qValues = adapter.forward(boardFeatures) // DoubleArray[4096]
```

### Future Multi-Head Support

The adapter is designed to support value/policy head separation:

```kotlin
// Potential future architecture: 839 → [512, 256] → (4096 policy + 1 value)
val output = adapter.forward(boardFeatures) // DoubleArray[4097]
val policyLogits = output.sliceArray(0 until 4096)  // Action probabilities
val valueEstimate = output[4096]                     // State value
```

**Head Consistency:**
- Output dimensions remain fixed regardless of internal precision
- Head boundaries are preserved during `INDArray` ↔ `DoubleArray` conversion
- Multi-head outputs maintain correct indexing for downstream processing

## Precision Considerations

### Float32 vs Double Precision

| Aspect | Double (Kotlin) | Float32 (DL4J) | Impact |
|--------|----------------|----------------|---------|
| **Precision** | ~15 decimal digits | ~7 decimal digits | Acceptable for neural networks |
| **Range** | ±1.8e308 | ±3.4e38 | Sufficient for Q-values |
| **Memory** | 8 bytes/value | 4 bytes/value | 50% memory savings |
| **Performance** | Slower on GPU | Faster on GPU | Significant speedup |

### Precision Loss Examples

```kotlin
// Original double precision
val originalValue = 0.123456789012345

// After DL4J conversion (float32 → double)
val convertedValue = 0.12345679 // Lost precision after ~7 digits

// Impact on chess RL
val qValue1 = 0.123456789  // Original
val qValue2 = 0.123456790  // Very close alternative
// After DL4J: both become ≈ 0.1234568 (indistinguishable)
```

**Mitigation Strategies:**
- Use relative comparisons instead of exact equality
- Set appropriate tolerance thresholds for Q-value differences
- Focus on action ranking rather than absolute Q-values

## Training Batch Processing

### Gradient Computation

```kotlin
// Training batch conversion
val inputBatch = convertBatchToINDArray(inputs, batchSize, inputSize)
val targetBatch = convertBatchToINDArray(targets, batchSize, outputSize)

// DL4J training
val dataSet = DataSet(inputBatch, targetBatch)
network.fit(dataSet) // Automatic gradient computation and weight updates
```

**Training Consistency:**
- Batch gradients are computed using `float32` precision
- Weight updates maintain consistency across training steps
- Loss computation uses the same precision as forward pass

### Loss Function Precision

```kotlin
// Huber loss computation (manual implementation)
private fun computeHuberLoss(predictions: INDArray, targets: INDArray, delta: Double = 1.0): Double {
    val diff = predictions.sub(targets)
    val absDiff = Transforms.abs(diff)
    
    // Element-wise Huber loss calculation
    for (i in 0 until numElements) {
        val absDiffVal = flatAbsDiff.getDouble(i.toLong()) // float32 → double conversion
        val diffVal = flatDiff.getDouble(i.toLong())
        
        totalLoss += if (absDiffVal <= delta) {
            0.5 * diffVal * diffVal  // Quadratic region
        } else {
            delta * absDiffVal - 0.5 * delta * delta  // Linear region
        }
    }
}
```

## Performance Implications

### Memory Usage

```kotlin
// Memory comparison for chess RL (batch size = 32)
val manualMemory = 32 * (839 + 4096) * 8  // Double precision: ~1.26 MB
val dl4jMemory = 32 * (839 + 4096) * 4    // Float32 precision: ~0.63 MB
// 50% memory reduction with DL4J
```

### Computational Performance

- **CPU**: Float32 operations are generally faster than double precision
- **GPU**: Significant performance improvement with float32 (if GPU backend is used)
- **Batch Processing**: DL4J's optimized batch operations outperform manual implementations

## Best Practices

### 1. Validation and Testing

```kotlin
// Always validate data conversion
require(input.size == config.inputSize) { "Input size mismatch" }
require(output.all { it.isFinite() }) { "Non-finite output values" }

// Test precision tolerance
val tolerance = 1e-6 // Appropriate for float32 precision
assertTrue(abs(expected - actual) < tolerance)
```

### 2. Error Handling

```kotlin
// Check for precision-related issues
private fun isINDArrayFinite(array: INDArray): Boolean {
    val flattened = array.reshape(-1)
    for (i in 0 until flattened.length().toInt()) {
        if (!flattened.getDouble(i.toLong()).isFinite()) {
            return false
        }
    }
    return true
}
```

### 3. Debugging Tips

```kotlin
// Log precision loss for debugging
val originalSum = input.sum()
val convertedSum = convertINDArrayToDoubleArray(convertDoubleArrayToINDArray(input, 1, input.size)).sum()
val precisionLoss = abs(originalSum - convertedSum)
if (precisionLoss > 1e-6) {
    println("Warning: Precision loss detected: $precisionLoss")
}
```

## Conclusion

The DL4J adapter successfully handles the conversion between Kotlin's double precision arrays and DL4J's float32 INDArrays while maintaining:

- **Functional Compatibility**: All existing RL algorithms work unchanged
- **Action Masking**: Consistent indexing preserves masking logic
- **Batch Processing**: Efficient batch operations with proper shape handling
- **Acceptable Precision**: Float32 precision is sufficient for neural network training

The precision trade-off (double → float32 → double) is acceptable for chess RL applications, providing significant performance benefits while maintaining algorithmic correctness.