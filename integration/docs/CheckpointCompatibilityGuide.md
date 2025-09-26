# Checkpoint Compatibility Guide

This guide explains the checkpoint format compatibility rules between different neural network backends in the Chess RL Training System.

## Backend Formats

### Manual Backend (`--nn manual`)
- **File Format**: JSON (`.json`)
- **Content**: Human-readable JSON with weights, biases, and network architecture
- **Optimizer State**: Saved separately in `.optimizer` companion file
- **Example**: `model.json` + `model.json.optimizer`

### DL4J Backend (`--nn dl4j`)
- **File Format**: ZIP (`.zip`)
- **Content**: Binary serialized MultiLayerNetwork with complete state
- **Optimizer State**: Included in ZIP file (momentum buffers, velocity buffers, etc.)
- **Metadata**: Additional `.meta.json` file with backend information
- **Example**: `model.zip` + `model.zip.meta.json`

### KotlinDL Backend (`--nn kotlindl`) [Future]
- **File Format**: H5 (`.h5`) or SavedModel directory
- **Content**: TensorFlow/Keras compatible format
- **Optimizer State**: Included in model file

## Compatibility Rules

### ✅ Same Backend Loading
- **Manual → Manual**: ✅ Full compatibility
- **DL4J → DL4J**: ✅ Full compatibility including optimizer state
- **KotlinDL → KotlinDL**: ✅ Full compatibility (when implemented)

### ❌ Cross-Backend Loading
- **Manual → DL4J**: ❌ Not supported
- **DL4J → Manual**: ❌ Not supported
- **Manual ↔ KotlinDL**: ❌ Not supported
- **DL4J ↔ KotlinDL**: ❌ Not supported

## Error Messages and Troubleshooting

### Loading JSON with DL4J Backend
```bash
# This will fail:
./gradlew run --args="--nn dl4j --load model.json"
```
**Error**: "Cannot load JSON format model with DL4J backend. JSON models are from the manual backend. Use --nn manual to load JSON models, or use a DL4J model (.zip format)."

**Solution**: Use the correct backend:
```bash
./gradlew run --args="--nn manual --load model.json"
```

### Loading ZIP with Manual Backend
```bash
# This will fail:
./gradlew run --args="--nn manual --load model.zip"
```
**Error**: "Cannot load ZIP format model with manual backend. ZIP models are from the DL4J backend. Use --nn dl4j to load ZIP models, or use a manual model (.json format)."

**Solution**: Use the correct backend:
```bash
./gradlew run --args="--nn dl4j --load model.zip"
```

### File Not Found
```bash
# This will fail:
./gradlew run --args="--nn dl4j --load nonexistent_model"
```
**Error**: "DL4J model file not found: nonexistent_model.zip. DL4J models are saved in ZIP format."

**Solution**: Check the file path and ensure the model exists.

## Automatic File Extension Handling

### DL4J Backend
- `--load model` → Looks for `model.zip`
- `--load model.zip` → Uses `model.zip` directly
- `--save model` → Creates `model.zip`
- `--save model.zip` → Creates `model.zip`

### Manual Backend
- `--load model` → Looks for `model.json`
- `--load model.json` → Uses `model.json` directly
- `--save model` → Creates `model.json`
- `--save model.json` → Creates `model.json`

## Checkpoint Resolution Priority

When loading a model without specifying the backend, the system uses this priority:

1. **CLI Flag**: `--nn manual|dl4j|kotlindl`
2. **Profile Configuration**: `nnBackend: dl4j` in YAML
3. **Default**: `manual` (maintains backward compatibility)

## Migration Between Backends

Currently, there is no automatic conversion between backend formats. To switch backends:

1. **Train a new model** with the desired backend
2. **Use the same training configuration** to ensure equivalent architecture
3. **Validate performance** on your evaluation tasks

### Example Migration
```bash
# Original training with manual backend
./gradlew run --args="--profile config/train.yaml --nn manual --save manual_model"

# Retrain with DL4J backend using same configuration
./gradlew run --args="--profile config/train.yaml --nn dl4j --save dl4j_model"
```

## Best Practices

### 1. Consistent Backend Usage
- Use the same backend throughout a training pipeline
- Document which backend was used for each model
- Include backend information in model naming: `model_dl4j_v1.zip`

### 2. Backup Strategy
- Save models in multiple formats if switching backends
- Keep metadata files (`.meta.json`) for debugging
- Use version control for configuration files

### 3. Performance Testing
- Benchmark different backends on your specific hardware
- Test both training and inference performance
- Validate numerical accuracy between backends

### 4. Configuration Management
```yaml
# profile.yaml
nnBackend: dl4j  # Specify backend in configuration
network:
  inputSize: 839
  outputSize: 4096
  hiddenLayers: [512, 256]
  learningRate: 0.001
```

## Debugging Checkpoint Issues

### Check File Format
```bash
# Check if file is JSON (manual backend)
head -n 5 model.json

# Check if file is ZIP (DL4J backend)
file model.zip
unzip -l model.zip
```

### Validate Metadata
```bash
# Check DL4J metadata
cat model.zip.meta.json

# Check manual optimizer state
cat model.json.optimizer
```

### Test Loading
```bash
# Test with explicit backend specification
./gradlew run --args="--nn manual --load model.json --eval-baseline"
./gradlew run --args="--nn dl4j --load model.zip --eval-baseline"
```

## Implementation Details

### Weight Synchronization
- **Manual Backend**: Direct weight/bias copying between FeedforwardNetwork instances
- **DL4J Backend**: Parameter copying using `params()` and `setParams()` with updater state handling
- **Cross-Backend**: Not supported due to different internal representations

### Optimizer State Preservation
- **Manual Backend**: Configuration parameters saved in companion `.optimizer` file
- **DL4J Backend**: Complete optimizer state (momentum buffers, etc.) saved in ZIP file
- **Loading**: Optimizer state restored when available, fresh state created otherwise

### Validation
- **Architecture Matching**: Input/output dimensions validated on load
- **Parameter Count**: Expected vs actual parameter count comparison
- **Gradient Validation**: Forward/backward pass validation after loading