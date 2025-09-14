#!/bin/bash

# Performance Benchmark Script
# Compares JVM vs Native compilation performance

set -e

echo "=== Neural Network Performance Comparison ==="
echo "Comparing JVM vs Native compilation performance"
echo

echo "Pinning JVM flags for consistency..."
export JAVA_TOOL_OPTIONS="-Xms1g -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
export GRADLE_OPTS="-Xms1g -Xmx1g"

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean > /dev/null 2>&1

# Create results directory
mkdir -p benchmark-results
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_DIR="benchmark-results/benchmark_$TIMESTAMP"
mkdir -p "$RESULTS_DIR"

echo "Results will be saved to: $RESULTS_DIR"

# Collect environment metadata
{
  echo "=== Benchmark Environment Metadata ==="
  echo "Date: $(date -u)"
  echo "OS: $(uname -srm 2>/dev/null || echo unknown)"
  echo "Kernel: $(uname -v 2>/dev/null || echo unknown)"
  echo "CPU: $( (sysctl -n machdep.cpu.brand_string 2>/dev/null) || (lscpu | grep 'Model name' | sed 's/Model name:\s*//' 2>/dev/null) || echo unknown )"
  echo "Cores: $( (sysctl -n hw.logicalcpu 2>/dev/null) || (nproc 2>/dev/null) || echo unknown )"
  echo "Memory: $( (sysctl -n hw.memsize 2>/dev/null) || (grep MemTotal /proc/meminfo 2>/dev/null | awk '{print $2" KB"}') || echo unknown )"
  echo "Java: $(java -version 2>&1 | tr '\n' ' ' | sed 's/\s\+/ /g')"
  echo "Gradle: $(./gradlew -v | head -n 3 | tr '\n' ' ' | sed 's/\s\+/ /g')"
  echo "JAVA_TOOL_OPTIONS=$JAVA_TOOL_OPTIONS"
  echo "GRADLE_OPTS=$GRADLE_OPTS"
} | tee "$RESULTS_DIR/metadata.txt"
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
    if ./gradlew :nn-package:${gradle_task} --tests "com.chessrl.nn.PerformanceBenchmark.benchmarkNeuralNetworkTraining" --info > "$output_file" 2>&1; then
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
        
        # XOR Training block
        if grep -q "--- XOR Problem Training Benchmark ---" "$file"; then
            echo "  XOR Training:"
            grep -A 3 "--- XOR Problem Training Benchmark ---" "$file" | grep -E "(XOR Training|Average per epoch)" | sed 's/^/    /'
        fi

        # Polynomial Regression block
        if grep -q "--- Polynomial Regression Benchmark ---" "$file"; then
            echo "  Polynomial Regression:"
            grep -A 3 "--- Polynomial Regression Benchmark ---" "$file" | grep -E "(Polynomial Regression|Average per epoch)" | sed 's/^/    /'
        fi

        # Large Network block
        if grep -q "--- Large Network Training Benchmark ---" "$file"; then
            echo "  Large Network:"
            grep -A 3 "--- Large Network Training Benchmark ---" "$file" | grep -E "(Large Network|Average per epoch)" | sed 's/^/    /'
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
