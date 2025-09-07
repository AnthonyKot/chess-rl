#!/bin/bash

echo "Chess RL Bot - Build Verification"
echo "=================================="

echo "1. Checking project structure..."
if [ -d "nn-package" ] && [ -d "chess-engine" ] && [ -d "rl-framework" ] && [ -d "integration" ]; then
    echo "✓ All modules present"
else
    echo "✗ Missing modules"
    exit 1
fi

echo "2. Compiling Kotlin metadata..."
if ./gradlew compileKotlinMetadata --quiet; then
    echo "✓ Kotlin compilation successful"
else
    echo "✗ Kotlin compilation failed"
    exit 1
fi

echo "3. Running tests (excluding native)..."
if ./gradlew test --exclude-task nativeTest --quiet; then
    echo "✓ Tests passed"
else
    echo "✗ Tests failed"
    exit 1
fi

echo "4. Checking module dependencies..."
if ./gradlew dependencies --quiet > /dev/null; then
    echo "✓ Dependencies resolved"
else
    echo "✗ Dependency issues"
    exit 1
fi

echo ""
echo "Build verification completed successfully!"
echo ""
echo "Note: Native compilation requires Xcode on macOS."
echo "To build native executable, ensure Xcode is installed and run:"
echo "  ./gradlew nativeBinaries"
echo ""
echo "Project is ready for development!"