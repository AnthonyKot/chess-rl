#!/bin/bash

echo "=== Neural Network Performance Comparison ==="
echo "Comparing JVM vs Native performance"
echo

# Create results directory
mkdir -p benchmark-results
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_DIR="benchmark-results/comparison_$TIMESTAMP"
mkdir -p "$RESULTS_DIR"

echo "Results will be saved to: $RESULTS_DIR"
echo

# Run JVM benchmark
echo "=== Running JVM Benchmark ==="
echo "Building and running JVM tests..."
./gradlew :nn-package:jvmTest > "$RESULTS_DIR/jvm_output.txt" 2>&1
JVM_EXIT_CODE=$?

if [ $JVM_EXIT_CODE -eq 0 ]; then
    echo "JVM benchmark completed successfully"
else
    echo "JVM benchmark failed (exit code: $JVM_EXIT_CODE)"
fi

# Run Native benchmark
echo
echo "=== Running Native Benchmark ==="
echo "Building and running Native tests..."
./gradlew :nn-package:nativeTest > "$RESULTS_DIR/native_output.txt" 2>&1
NATIVE_EXIT_CODE=$?

if [ $NATIVE_EXIT_CODE -eq 0 ]; then
    echo "Native benchmark completed successfully"
else
    echo "Native benchmark failed (exit code: $NATIVE_EXIT_CODE)"
fi

echo
echo "=== Results Summary ==="
echo "JVM Exit Code: $JVM_EXIT_CODE"
echo "Native Exit Code: $NATIVE_EXIT_CODE"
echo
echo "Full outputs saved to:"
echo "  - $RESULTS_DIR/jvm_output.txt"
echo "  - $RESULTS_DIR/native_output.txt"
echo

# Try to extract any performance metrics from the outputs
echo "=== Performance Metrics ==="
if [ -f "$RESULTS_DIR/jvm_output.txt" ]; then
    echo "JVM Results:"
    if grep -q "Benchmark Results" "$RESULTS_DIR/jvm_output.txt"; then
        grep -A 20 "Benchmark Results" "$RESULTS_DIR/jvm_output.txt" | head -20
    else
        echo "  No benchmark results found in JVM output"
    fi
fi

echo
if [ -f "$RESULTS_DIR/native_output.txt" ]; then
    echo "Native Results:"
    if grep -q "Benchmark Results" "$RESULTS_DIR/native_output.txt"; then
        grep -A 20 "Benchmark Results" "$RESULTS_DIR/native_output.txt" | head -20
    else
        echo "  No benchmark results found in Native output"
    fi
fi

echo
echo "Comparison completed at $(date)"