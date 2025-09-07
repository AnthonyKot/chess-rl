#!/bin/bash

# Performance Benchmark Script
# Compares JVM vs Native compilation performance

set -e

echo "=== Neural Network Performance Comparison ==="
echo "Comparing JVM vs Native compilation performance"
echo

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean > /dev/null 2>&1

# Create results directory
mkdir -p benchmark-results
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_DIR="benchmark-results/benchmark_$TIMESTAMP"
mkdir -p "$RESULTS_DIR"

echo "Results will be saved to: $RESULTS_DIR"
echo

# Function to run benchmark and capture output
run_benchmark() {
    local platform=$1
    local gradle_task=$2
    local output_file=$3
    
    echo "Running $platform benchmark..."
    echo "Task: $gradle_task"
    
    # Build first
    echo "  Building $platform..."
    if ./gradlew :nn-package:$gradle_task > "${output_file}.build.log" 2>&1; then
        echo "  Build successful"
    else
        echo "  Build failed - check ${output_file}.build.log"
        return 1
    fi
    
    # Run benchmark
    echo "  Running benchmark..."
    if ./gradlew :nn-package:${gradle_task} --tests "com.chessrl.nn.PerformanceBenchmark.benchmarkNeuralNetworkPerformance" --info > "$output_file" 2>&1; then
        echo "  Benchmark completed successfully"
    else
        echo "  Benchmark failed - check $output_file"
        return 1
    fi
    return 0
}

# Run JVM benchmark
echo "=== JVM Benchmark ==="
if run_benchmark "JVM" "jvmTest" "$RESULTS_DIR/jvm_results.txt"; then
    echo "JVM benchmark completed"
else
    echo "JVM benchmark failed"
fi
echo

# Run Native benchmark (if supported on this platform)
echo "=== Native Benchmark ==="
NATIVE_TARGET=""

# Use generic native test task
NATIVE_TARGET="nativeTest"
echo "Using nativeTest task for native compilation"

if [[ -n "$NATIVE_TARGET" ]]; then
    if run_benchmark "Native" "$NATIVE_TARGET" "$RESULTS_DIR/native_results.txt"; then
        echo "Native benchmark completed"
    else
        echo "Native benchmark failed"
    fi
else
    echo "Native benchmark skipped"
fi

echo
echo "=== Benchmark Summary ==="

# Extract and compare key metrics
extract_metrics() {
    local file=$1
    local platform=$2
    
    if [[ -f "$file" ]]; then
        echo "=== $platform Results ==="
        
        # Extract platform info
        if grep -q "Platform:" "$file"; then
            grep "Platform:" "$file"
        fi
        
        # Extract key benchmark results
        echo "Key Performance Metrics:"
        
        # Network Creation
        if grep -A 3 "Network Creation:" "$file" > /dev/null; then
            echo "  Network Creation:"
            grep -A 2 "Avg time per op:" "$file" | head -1 | sed 's/^/    /'
            grep -A 3 "Operations/sec:" "$file" | head -1 | sed 's/^/    /'
        fi
        
        # Forward Pass
        if grep -A 3 "Forward Pass:" "$file" > /dev/null; then
            echo "  Forward Pass:"
            grep -A 2 "Forward Pass:" "$file" | grep "Avg time per op:" | sed 's/^/    /'
            grep -A 3 "Forward Pass:" "$file" | grep "Operations/sec:" | sed 's/^/    /'
        fi
        
        # Training Iteration
        if grep -A 3 "Training Iteration:" "$file" > /dev/null; then
            echo "  Training Iteration:"
            grep -A 2 "Training Iteration:" "$file" | grep "Avg time per op:" | sed 's/^/    /'
            grep -A 3 "Training Iteration:" "$file" | grep "Operations/sec:" | sed 's/^/    /'
        fi
        
        echo
    else
        echo "$platform results not found at $file"
        echo
    fi
}

# Show results
extract_metrics "$RESULTS_DIR/jvm_results.txt" "JVM"
extract_metrics "$RESULTS_DIR/native_results.txt" "Native"

# Create comparison summary
echo "=== Performance Comparison Summary ==="
echo "Full results available in: $RESULTS_DIR"
echo "  - jvm_results.txt: Complete JVM benchmark output"
echo "  - native_results.txt: Complete Native benchmark output"
echo "  - *.build.log: Build logs for each platform"
echo

# Simple comparison if both results exist
if [[ -f "$RESULTS_DIR/jvm_results.txt" ]] && [[ -f "$RESULTS_DIR/native_results.txt" ]]; then
    echo "Both JVM and Native benchmarks completed successfully!"
    echo "Compare the 'Avg time per op' and 'Operations/sec' metrics above to see performance differences."
    echo
    echo "Generally:"
    echo "  - Lower 'Avg time per op' = faster execution"
    echo "  - Higher 'Operations/sec' = better throughput"
else
    echo "Note: Complete comparison requires both JVM and Native results"
fi

echo
echo "Benchmark completed at $(date)"