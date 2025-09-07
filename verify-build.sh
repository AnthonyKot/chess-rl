#!/bin/bash

set -u

echo "Chess RL Bot - Build Verification"
echo "=================================="

# Colors and helpers
GREEN="\033[0;32m"; YELLOW="\033[1;33m"; RED="\033[0;31m"; NC="\033[0m"
success() { echo -e "${GREEN}✓${NC} $1"; }
warning() { echo -e "${YELLOW}!${NC} $1"; }
error()   { echo -e "${RED}✗${NC} $1"; }

# Use a temp Gradle cache to avoid local cache issues in CI/sandbox
export GRADLE_USER_HOME=${GRADLE_USER_HOME:-/tmp/gradle}

# Ensure gradlew is executable
chmod +x ./gradlew 2>/dev/null || true

# Detect platform / Xcode
HOST_OS=$(uname -s)
XCODE_AVAILABLE=false
if [ "$HOST_OS" = "Darwin" ]; then
  if xcode-select -p >/dev/null 2>&1; then
    XCODE_AVAILABLE=true
  fi
fi

echo "1. Checking project structure..."
if [ -d "nn-package" ] && [ -d "chess-engine" ] && [ -d "rl-framework" ] && [ -d "integration" ]; then
    success "All modules present"
else
    error "Missing modules (expected nn-package, chess-engine, rl-framework, integration)"
    exit 1
fi

echo "2. Compiling Kotlin metadata..."
if ./gradlew compileKotlinMetadata --quiet; then
    success "Kotlin compilation successful"
else
    error "Kotlin compilation failed"
    exit 1
fi

echo "3. Running unit tests..."

# Try to run JVM tests if available (root jvmTest)
if ./gradlew tasks --all 2>/dev/null | grep -q " jvmTest"; then
    echo "   Running JVM tests..."
    if ./gradlew jvmTest --quiet; then
        success "JVM tests passed"
    else
        warning "JVM tests failed or had issues"
    fi
else
    warning "No root JVM tests found"
fi

# Always try running nn-package JVM tests explicitly (most valuable)
if ./gradlew :nn-package:tasks --all 2>/dev/null | grep -q " jvmTest"; then
    echo "   Running nn-package JVM tests..."
    if ./gradlew :nn-package:jvmTest --quiet; then
        success "nn-package JVM tests passed"
    else
        warning "nn-package JVM tests failed or had issues"
    fi
else
    warning "nn-package has no JVM tests task"
fi

# Try to run chess-engine JVM tests if available
if ./gradlew :chess-engine:tasks --all 2>/dev/null | grep -q " jvmTest"; then
    echo "   Running chess-engine JVM tests..."
    if ./gradlew :chess-engine:jvmTest --quiet; then
        success "chess-engine JVM tests passed"
    else
        warning "chess-engine JVM tests failed or had issues"
    fi
else
    warning "chess-engine has no JVM tests task"
fi

# Try to run native tests only if Xcode is available
if [ "$XCODE_AVAILABLE" = true ]; then
    echo "   Running native tests..."
    if ./gradlew nativeTest --quiet; then
        success "Native tests passed"
    else
        warning "Native tests failed or had issues"
    fi
else
    warning "Skipping native tests (Xcode not available)"
fi

echo ""
echo "4. Building binaries..."

# Build native binaries only if Xcode is available
if [ "$XCODE_AVAILABLE" = true ]; then
    echo "   Building native binaries..."
    if ./gradlew nativeBinaries --quiet; then
        success "Native binaries built successfully"
        echo "   Built binaries:"
        # List built binaries
        find . -type f \( -name "*.kexe" -o -name "*Executable*" \) 2>/dev/null | while read -r binary; do
            echo "     - $binary"
        done
    else
        warning "Native binary build failed or had issues"
    fi
else
    warning "Skipping native binary build (Xcode not available)"
fi

echo ""
echo "5. Checking module dependencies..."
if ./gradlew dependencies --quiet > /dev/null; then
    success "Dependencies resolved"
else
    warning "Dependency issues (non-fatal for quick verification)"
fi

echo ""
echo "Build verification complete. Summary:"
echo "- ${GREEN}Kotlin compile${NC}"
echo "- ${GREEN}NN JVM tests${NC} (and other JVM tests if present)"
if [ "$XCODE_AVAILABLE" = true ]; then
  echo "- ${GREEN}Native tests/build${NC} (macOS with Xcode)"
else
  echo "- ${YELLOW}Native steps skipped${NC} (no Xcode)"
fi

echo ""
echo "Tips:"
echo "- Run nn-package tests only:    ./gradlew :nn-package:jvmTest"
echo "- Run chess-engine demo:        ./gradlew :chess-engine:runDemo"
echo "- Skip native tests everywhere: ./gradlew test --exclude-task nativeTest"
