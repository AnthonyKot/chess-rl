#!/bin/bash

# Performance Comparison Script for Chess RL Training Pipeline
# Compares JVM vs Native execution times for the same training workload

set -e

echo "🏁 Chess RL Training Pipeline Performance Comparison"
echo "=================================================="
echo "Comparing JVM vs Native execution times"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to run tests and measure time
run_test_with_timing() {
    local platform=$1
    local gradle_task=$2
    local test_filter=$3
    
    echo -e "${BLUE}Running $platform tests...${NC}"
    echo "Task: $gradle_task"
    echo "Filter: $test_filter"
    echo ""
    
    # Record start time
    start_time=$(date +%s.%N)
    
    # Run the test
    if ./gradlew $gradle_task --tests="$test_filter" --console=plain > "${platform}_test_output.log" 2>&1; then
        # Record end time
        end_time=$(date +%s.%N)
        
        # Calculate duration
        duration=$(echo "$end_time - $start_time" | bc -l)
        
        echo -e "${GREEN}✅ $platform tests completed${NC}"
        echo "Duration: ${duration}s"
        echo ""
        
        # Return duration for comparison
        echo $duration
    else
        echo -e "${RED}❌ $platform tests failed${NC}"
        echo "Check ${platform}_test_output.log for details"
        echo ""
        echo "0"
    fi
}

# Function to extract test statistics from log
extract_test_stats() {
    local platform=$1
    local log_file="${platform}_test_output.log"
    
    if [ -f "$log_file" ]; then
        echo "📊 $platform Test Statistics:"
        
        # Extract compilation time
        compile_time=$(grep "BUILD SUCCESSFUL" "$log_file" | tail -1 | grep -o '[0-9]\+s' | head -1 || echo "N/A")
        echo "  Compilation Time: $compile_time"
        
        # Count test methods
        test_count=$(grep -c "Test.*PASSED\|Test.*FAILED" "$log_file" 2>/dev/null || echo "N/A")
        echo "  Tests Executed: $test_count"
        
        # Look for any performance indicators in the output
        if grep -q "Training completed" "$log_file"; then
            echo "  Training Status: ✅ Completed"
        else
            echo "  Training Status: ❓ Unknown"
        fi
        
        echo ""
    fi
}

# Check if bc is available for calculations
if ! command -v bc &> /dev/null; then
    echo -e "${YELLOW}⚠️  'bc' calculator not found. Installing basic timing...${NC}"
    # Fallback timing method without bc
    timing_method="basic"
else
    timing_method="precise"
fi

echo "🧪 Test Configuration:"
echo "  - Small neural networks (16-32 units)"
echo "  - Short episodes (8-15 steps)"
echo "  - Small batches (4-8 experiences)"
echo "  - Limited episodes (3-6 per test)"
echo ""

# Test 1: End-to-End Training Pipeline Test
echo "=" * 60
echo "Test 1: End-to-End Training Pipeline"
echo "=" * 60

# Check if JVM tests are available
if ./gradlew integration:tasks | grep -q "jvmTest"; then
    echo "🔍 JVM tests available"
    jvm_duration=$(run_test_with_timing "JVM" "integration:jvmTest" "*EndToEndTraining*")
    extract_test_stats "JVM"
else
    echo -e "${YELLOW}⚠️  JVM tests not available, skipping JVM comparison${NC}"
    jvm_duration="0"
fi

# Run Native tests
echo "🔍 Native tests"
native_duration=$(run_test_with_timing "Native" "integration:nativeTest" "*EndToEndTraining*")
extract_test_stats "Native"

# Test 2: Training Controller Test
echo "=" * 60
echo "Test 2: Training Controller"
echo "=" * 60

if [ "$jvm_duration" != "0" ]; then
    jvm_controller_duration=$(run_test_with_timing "JVM" "integration:jvmTest" "*TrainingController*")
    extract_test_stats "JVM"
else
    jvm_controller_duration="0"
fi

native_controller_duration=$(run_test_with_timing "Native" "integration:nativeTest" "*TrainingController*")
extract_test_stats "Native"

# Test 3: Chess Training Pipeline Test
echo "=" * 60
echo "Test 3: Chess Training Pipeline"
echo "=" * 60

if [ "$jvm_duration" != "0" ]; then
    jvm_pipeline_duration=$(run_test_with_timing "JVM" "integration:jvmTest" "*ChessTrainingPipeline*")
    extract_test_stats "JVM"
else
    jvm_pipeline_duration="0"
fi

native_pipeline_duration=$(run_test_with_timing "Native" "integration:nativeTest" "*ChessTrainingPipeline*")
extract_test_stats "Native"

# Performance Summary
echo "=" * 60
echo "🏆 PERFORMANCE COMPARISON SUMMARY"
echo "=" * 60

if [ "$timing_method" = "precise" ] && [ "$jvm_duration" != "0" ]; then
    echo "📈 Execution Time Comparison:"
    echo ""
    
    printf "%-25s | %-12s | %-12s | %-10s\n" "Test Suite" "JVM (s)" "Native (s)" "Ratio"
    echo "----------------------------------------------------------------"
    
    # End-to-End Test
    if [ "$jvm_duration" != "0" ] && [ "$native_duration" != "0" ]; then
        ratio=$(echo "scale=2; $native_duration / $jvm_duration" | bc -l)
        printf "%-25s | %-12.2f | %-12.2f | %-10s\n" "End-to-End Training" "$jvm_duration" "$native_duration" "${ratio}x"
    fi
    
    # Controller Test
    if [ "$jvm_controller_duration" != "0" ] && [ "$native_controller_duration" != "0" ]; then
        ratio=$(echo "scale=2; $native_controller_duration / $jvm_controller_duration" | bc -l)
        printf "%-25s | %-12.2f | %-12.2f | %-10s\n" "Training Controller" "$jvm_controller_duration" "$native_controller_duration" "${ratio}x"
    fi
    
    # Pipeline Test
    if [ "$jvm_pipeline_duration" != "0" ] && [ "$native_pipeline_duration" != "0" ]; then
        ratio=$(echo "scale=2; $native_pipeline_duration / $jvm_pipeline_duration" | bc -l)
        printf "%-25s | %-12.2f | %-12.2f | %-10s\n" "Chess Pipeline" "$jvm_pipeline_duration" "$native_pipeline_duration" "${ratio}x"
    fi
    
    echo ""
    
    # Calculate overall performance
    total_jvm=$(echo "$jvm_duration + $jvm_controller_duration + $jvm_pipeline_duration" | bc -l)
    total_native=$(echo "$native_duration + $native_controller_duration + $native_pipeline_duration" | bc -l)
    
    if [ "$total_jvm" != "0" ] && [ "$total_native" != "0" ]; then
        overall_ratio=$(echo "scale=2; $total_native / $total_jvm" | bc -l)
        echo "🎯 Overall Performance:"
        printf "   Total JVM Time:    %.2fs\n" "$total_jvm"
        printf "   Total Native Time: %.2fs\n" "$total_native"
        printf "   Native vs JVM:     %.2fx\n" "$overall_ratio"
        echo ""
        
        # Performance interpretation
        if (( $(echo "$overall_ratio < 1.0" | bc -l) )); then
            echo -e "${GREEN}🚀 Native is faster than JVM${NC}"
        elif (( $(echo "$overall_ratio > 1.5" | bc -l) )); then
            echo -e "${RED}⚠️  Native is significantly slower than JVM${NC}"
        else
            echo -e "${YELLOW}⚖️  Native and JVM have similar performance${NC}"
        fi
    fi
    
else
    echo "📊 Native Test Results:"
    echo "   End-to-End Training: ${native_duration}s"
    echo "   Training Controller: ${native_controller_duration}s"
    echo "   Chess Pipeline: ${native_pipeline_duration}s"
    echo ""
    if [ "$jvm_duration" = "0" ]; then
        echo -e "${YELLOW}ℹ️  JVM tests not available for comparison${NC}"
    fi
fi

# System Information
echo "💻 System Information:"
echo "   OS: $(uname -s)"
echo "   Architecture: $(uname -m)"
echo "   Kotlin Version: $(./gradlew --version | grep "Kotlin version" | cut -d' ' -f3 || echo "Unknown")"
echo "   Java Version: $(java -version 2>&1 | head -1 | cut -d'"' -f2 || echo "Unknown")"
echo ""

# Recommendations
echo "💡 Performance Recommendations:"
echo ""
if [ "$timing_method" = "precise" ] && [ "$jvm_duration" != "0" ]; then
    echo "Based on the performance comparison:"
    echo "• For development and testing: Use the faster platform"
    echo "• For production deployment: Consider memory usage and startup time"
    echo "• For CI/CD pipelines: Balance speed vs resource usage"
else
    echo "• Native build provides predictable performance"
    echo "• No JVM startup overhead"
    echo "• Better memory control for long-running training"
fi
echo "• Consider using JVM for development (faster iteration)"
echo "• Use Native for production deployment (better resource control)"
echo ""

# Cleanup
echo "🧹 Cleaning up temporary files..."
rm -f JVM_test_output.log Native_test_output.log

echo -e "${GREEN}✅ Performance comparison completed!${NC}"