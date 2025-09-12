#!/bin/bash

# JVM vs Native Performance Comparison for Chess RL Training Pipeline
# Focuses on Training Controller test (the slowest test) for accurate comparison

set -e

echo "🏁 JVM vs Native Performance Comparison"
echo "======================================="
echo "Comparing Training Controller Test Performance"
echo "Test: *TrainingController* (slowest test - 195s on Native)"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to run test and measure time
run_platform_test() {
    local platform=$1
    local gradle_task=$2
    
    echo -e "${BLUE}🔄 Running $platform Training Controller Test...${NC}"
    echo "Task: $gradle_task"
    echo ""
    
    # Record start time (using date for cross-platform compatibility)
    start_time=$(date +%s)
    
    # Run the test with timing
    if time ./gradlew $gradle_task --tests="*TrainingController*" --console=plain > "${platform}_controller_test.log" 2>&1; then
        # Record end time
        end_time=$(date +%s)
        
        # Calculate duration
        duration=$((end_time - start_time))
        
        echo -e "${GREEN}✅ $platform test completed${NC}"
        echo "Duration: ${duration}s"
        
        # Extract additional metrics
        extract_test_metrics "$platform" "${platform}_controller_test.log"
        
        echo $duration
    else
        echo -e "${RED}❌ $platform test failed${NC}"
        echo "Check ${platform}_controller_test.log for details"
        echo ""
        echo "0"
    fi
}

# Function to extract test metrics from log
extract_test_metrics() {
    local platform=$1
    local log_file=$2
    
    if [ -f "$log_file" ]; then
        echo ""
        echo "📊 $platform Detailed Metrics:"
        
        # Extract build time
        build_time=$(grep "BUILD SUCCESSFUL" "$log_file" | tail -1 | grep -o '[0-9]\+s\|[0-9]\+m [0-9]\+s' | head -1 || echo "N/A")
        echo "  Build Time: $build_time"
        
        # Extract compilation time (approximate)
        if grep -q "compileKotlin" "$log_file"; then
            echo "  Compilation: ✅ Completed"
        else
            echo "  Compilation: ❓ Unknown"
        fi
        
        # Count test methods executed
        test_methods=$(grep -c "✅\|❌\|PASSED\|FAILED" "$log_file" 2>/dev/null || echo "0")
        echo "  Test Methods: $test_methods"
        
        # Check for performance indicators
        if grep -q "Training completed\|Training.*successful" "$log_file"; then
            echo "  Training Status: ✅ Successful"
        else
            echo "  Training Status: ❓ Unknown"
        fi
        
        # Memory usage indicators (if available)
        if grep -q -i "memory\|heap\|gc" "$log_file"; then
            echo "  Memory Info: ✅ Available in log"
        else
            echo "  Memory Info: ❌ Not available"
        fi
        
        echo ""
    fi
}

# System information
echo "💻 System Information:"
echo "   OS: $(uname -s)"
echo "   Architecture: $(uname -m)"
echo "   Processor: $(sysctl -n machdep.cpu.brand_string 2>/dev/null || echo "Unknown")"
echo "   Memory: $(sysctl -n hw.memsize 2>/dev/null | awk '{print int($1/1024/1024/1024)"GB"}' || echo "Unknown")"
echo "   Java Version: $(java -version 2>&1 | head -1 | cut -d'"' -f2 || echo "Unknown")"
echo ""

echo "🧪 Test Configuration:"
echo "   Test Suite: TrainingControllerTest"
echo "   Test Methods: ~10 comprehensive test methods"
echo "   Neural Networks: Small (16-32 units for testing)"
echo "   Episodes: Limited (3-6 per test method)"
echo "   Expected Duration: ~3-4 minutes per platform"
echo ""

# Warm up JVM (optional)
echo "🔥 JVM Warm-up (optional quick compilation)..."
./gradlew integration:compileKotlinJvm > /dev/null 2>&1 || echo "JVM compilation warm-up completed"

echo ""
echo "=" * 60
echo "🚀 Starting Performance Comparison"
echo "=" * 60

# Test 1: JVM Performance
echo ""
echo "📋 Test 1: JVM (Kotlin/JVM) Performance"
echo "-" * 40

jvm_duration=$(run_platform_test "JVM" "integration:jvmTest")

# Test 2: Native Performance  
echo ""
echo "📋 Test 2: Native (Kotlin/Native) Performance"
echo "-" * 40

native_duration=$(run_platform_test "Native" "integration:nativeTest")

# Performance Comparison Analysis
echo ""
echo "=" * 60
echo "🏆 PERFORMANCE COMPARISON RESULTS"
echo "=" * 60

if [ "$jvm_duration" != "0" ] && [ "$native_duration" != "0" ]; then
    echo "📈 Training Controller Test Results:"
    echo ""
    printf "%-15s | %-12s | %-15s\n" "Platform" "Duration (s)" "Performance"
    echo "------------------------------------------------"
    printf "%-15s | %-12s | %-15s\n" "JVM" "$jvm_duration" "$([ $jvm_duration -lt $native_duration ] && echo "🚀 Faster" || echo "⚖️ Baseline")"
    printf "%-15s | %-12s | %-15s\n" "Native" "$native_duration" "$([ $native_duration -lt $jvm_duration ] && echo "🚀 Faster" || echo "⚖️ Baseline")"
    echo ""
    
    # Calculate performance ratio
    if command -v bc &> /dev/null; then
        if [ "$jvm_duration" -gt 0 ]; then
            ratio=$(echo "scale=2; $native_duration / $jvm_duration" | bc -l)
            echo "🎯 Performance Ratio (Native/JVM): ${ratio}x"
            
            if (( $(echo "$ratio < 0.8" | bc -l) )); then
                echo -e "${GREEN}🏆 Native is significantly faster (>20% improvement)${NC}"
            elif (( $(echo "$ratio < 1.0" | bc -l) )); then
                echo -e "${GREEN}✅ Native is faster${NC}"
            elif (( $(echo "$ratio < 1.2" | bc -l) )); then
                echo -e "${YELLOW}⚖️ Similar performance (within 20%)${NC}"
            else
                echo -e "${RED}⚠️ Native is slower${NC}"
            fi
        fi
    else
        # Simple comparison without bc
        if [ "$native_duration" -lt "$jvm_duration" ]; then
            improvement=$((jvm_duration - native_duration))
            echo -e "${GREEN}✅ Native is faster by ${improvement}s${NC}"
        elif [ "$jvm_duration" -lt "$native_duration" ]; then
            slowdown=$((native_duration - jvm_duration))
            echo -e "${YELLOW}⚠️ JVM is faster by ${slowdown}s${NC}"
        else
            echo -e "${YELLOW}⚖️ Similar performance${NC}"
        fi
    fi
    
    echo ""
    
    # Detailed Analysis
    echo "🔍 Detailed Analysis:"
    echo ""
    
    # Startup time analysis
    echo "Startup Characteristics:"
    echo "• JVM: Includes JVM startup overhead + JIT warm-up"
    echo "• Native: Immediate execution, no startup overhead"
    echo ""
    
    # Memory analysis
    echo "Memory Characteristics:"
    echo "• JVM: Garbage collection, heap management"
    echo "• Native: Direct memory management, no GC pauses"
    echo ""
    
    # Performance characteristics
    echo "Performance Characteristics:"
    if [ "$native_duration" -lt "$jvm_duration" ]; then
        echo "• Native shows better performance for this workload"
        echo "• Benefits: No JVM overhead, predictable execution"
        echo "• Suitable for: Production deployment, resource-constrained environments"
    elif [ "$jvm_duration" -lt "$native_duration" ]; then
        echo "• JVM shows better performance for this workload"
        echo "• Benefits: JIT optimization, mature runtime"
        echo "• Suitable for: Development, environments with JVM optimization"
    else
        echo "• Both platforms show similar performance"
        echo "• Choice depends on deployment requirements"
    fi
    
else
    echo -e "${RED}❌ Unable to complete comparison - one or both tests failed${NC}"
    echo "JVM Duration: ${jvm_duration}s"
    echo "Native Duration: ${native_duration}s"
fi

echo ""
echo "💡 Recommendations:"
echo ""

if [ "$jvm_duration" != "0" ] && [ "$native_duration" != "0" ]; then
    echo "Based on the performance comparison:"
    echo ""
    
    if [ "$native_duration" -lt "$jvm_duration" ]; then
        echo "🎯 **Recommended for Production: Native**"
        echo "   • Faster execution time"
        echo "   • Lower memory overhead"
        echo "   • No JVM dependencies"
        echo "   • Predictable performance"
        echo ""
        echo "🔧 **Recommended for Development: JVM**"
        echo "   • Faster compilation during development"
        echo "   • Better debugging tools"
        echo "   • Faster iteration cycle"
    else
        echo "🎯 **Recommended for Production: JVM**"
        echo "   • Better runtime performance"
        echo "   • JIT optimization benefits"
        echo "   • Mature ecosystem"
        echo ""
        echo "🔧 **Consider Native for:**"
        echo "   • Containerized deployments"
        echo "   • Resource-constrained environments"
        echo "   • Predictable memory usage requirements"
    fi
else
    echo "• Run both tests successfully to get recommendations"
    echo "• Check log files for detailed error information"
fi

echo ""
echo "📊 Summary:"
echo "• This comparison uses the most comprehensive test (TrainingController)"
echo "• Results represent real-world chess RL training performance"
echo "• Both platforms are suitable for chess RL training"
echo "• Choice depends on deployment requirements and performance priorities"

# Cleanup
echo ""
echo "🧹 Cleaning up temporary files..."
rm -f JVM_controller_test.log Native_controller_test.log

echo ""
echo -e "${GREEN}✅ JVM vs Native performance comparison completed!${NC}"
echo ""
echo "📝 Key Takeaways:"
echo "• Use this data to inform platform choice for your deployment"
echo "• Consider both performance and operational requirements"
echo "• Both platforms provide excellent chess RL training capabilities"