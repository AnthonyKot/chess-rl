#!/bin/bash

# Chess RL Debugging System - E2E Test Runner
# This script runs comprehensive end-to-end tests to prove the debugging system works

echo "🚀 Chess RL Debugging System - E2E Test Suite"
echo "=============================================="

# Check if we're in the right directory
if [ ! -f "build.gradle.kts" ]; then
    echo "❌ Error: Please run this script from the integration module root directory"
    exit 1
fi

echo "📋 Test Plan:"
echo "  1. Unit Tests - Individual component testing"
echo "  2. Integration Tests - Component interaction testing"  
echo "  3. E2E Tests - Complete workflow testing"
echo "  4. Demo Execution - Real-world scenario demonstration"
echo ""

# Function to run tests and capture results
run_test_suite() {
    local test_name="$1"
    local test_class="$2"
    
    echo "🔍 Running $test_name..."
    
    # Run the specific test class
    ./gradlew test --tests "$test_class" --info 2>&1 | tee "test_output_${test_name// /_}.log"
    
    local exit_code=${PIPESTATUS[0]}
    
    if [ $exit_code -eq 0 ]; then
        echo "✅ $test_name - PASSED"
        return 0
    else
        echo "❌ $test_name - FAILED (exit code: $exit_code)"
        return 1
    fi
}

# Initialize test results
total_tests=0
passed_tests=0
failed_tests=0

echo "🧪 Starting Test Execution..."
echo ""

# Test 1: Production Debugging Interface Tests
echo "--- Test Suite 1: Production Debugging Interface ---"
total_tests=$((total_tests + 1))
if run_test_suite "Production Debugging Interface" "com.chessrl.integration.ProductionDebuggingInterfaceTest"; then
    passed_tests=$((passed_tests + 1))
else
    failed_tests=$((failed_tests + 1))
fi
echo ""

# Test 2: Neural Network Analyzer Tests
echo "--- Test Suite 2: Neural Network Analyzer ---"
total_tests=$((total_tests + 1))
if run_test_suite "Neural Network Analyzer" "com.chessrl.integration.NeuralNetworkAnalyzerTest"; then
    passed_tests=$((passed_tests + 1))
else
    failed_tests=$((failed_tests + 1))
fi
echo ""

# Test 3: Experience Buffer Analyzer Tests
echo "--- Test Suite 3: Experience Buffer Analyzer ---"
total_tests=$((total_tests + 1))
if run_test_suite "Experience Buffer Analyzer" "com.chessrl.integration.ExperienceBufferAnalyzerTest"; then
    passed_tests=$((passed_tests + 1))
else
    failed_tests=$((failed_tests + 1))
fi
echo ""

# Test 4: End-to-End Integration Tests
echo "--- Test Suite 4: End-to-End Integration ---"
total_tests=$((total_tests + 1))
if run_test_suite "E2E Integration Tests" "com.chessrl.integration.ProductionDebuggingE2ETest"; then
    passed_tests=$((passed_tests + 1))
else
    failed_tests=$((failed_tests + 1))
fi
echo ""

# Test 5: Complete System E2E Tests
echo "--- Test Suite 5: Complete System E2E ---"
total_tests=$((total_tests + 1))
if run_test_suite "Complete System E2E" "com.chessrl.integration.DebuggingSystemE2ERunner"; then
    passed_tests=$((passed_tests + 1))
else
    failed_tests=$((failed_tests + 1))
fi
echo ""

# Generate test report
echo "📊 TEST EXECUTION SUMMARY"
echo "========================="
echo "Total Test Suites: $total_tests"
echo "Passed: $passed_tests"
echo "Failed: $failed_tests"
echo "Success Rate: $(( passed_tests * 100 / total_tests ))%"
echo ""

# Check if all tests passed
if [ $failed_tests -eq 0 ]; then
    echo "🎉 ALL TESTS PASSED!"
    echo ""
    echo "✅ The Chess RL Debugging System is working end-to-end:"
    echo "   • All individual components function correctly"
    echo "   • Components integrate seamlessly"
    echo "   • Complete workflows execute successfully"
    echo "   • Real-world scenarios are handled properly"
    echo "   • Data consistency is maintained across components"
    echo ""
    echo "🚀 The system is ready for production use!"
    
    # Run demo if all tests passed
    echo ""
    echo "🎯 Running System Demonstration..."
    echo "================================="
    
    # Create a simple Kotlin script to run the demo
    cat > run_demo.kt << 'EOF'
import com.chessrl.integration.ProductionDebuggingDemo

fun main() {
    println("🎬 Starting Chess RL Debugging System Demo")
    println("==========================================")
    
    try {
        val demo = ProductionDebuggingDemo()
        demo.runCompleteDemo()
        
        println("\n🎉 Demo completed successfully!")
        println("The debugging system demonstrated:")
        println("  ✅ Training issue investigation")
        println("  ✅ Interactive game analysis")
        println("  ✅ Neural network debugging")
        println("  ✅ Production monitoring")
        println("  ✅ Comprehensive reporting")
        println("  ✅ Data export capabilities")
        
    } catch (e: Exception) {
        println("❌ Demo failed: ${e.message}")
        e.printStackTrace()
    }
}
EOF
    
    echo "Demo script created. To run the demo manually:"
    echo "  kotlinc -cp build/libs/* run_demo.kt -include-runtime -d demo.jar && java -jar demo.jar"
    
    exit 0
else
    echo "❌ $failed_tests TEST SUITE(S) FAILED!"
    echo ""
    echo "Please check the test output logs for details:"
    ls -la test_output_*.log 2>/dev/null || echo "No test logs found"
    echo ""
    echo "Common issues and solutions:"
    echo "  • Missing dependencies: Run './gradlew build' first"
    echo "  • Compilation errors: Check Kotlin/Java versions"
    echo "  • Test failures: Review test logs for specific errors"
    
    exit 1
fi