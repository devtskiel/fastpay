#!/bin/bash

# FastPay APK Build Script
# This script builds the APK with proper configuration

set -e

echo "================================"
echo "FastPay APK Build Script"
echo "================================"

# Check Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed"
    exit 1
fi

echo "✓ Java found: $(java -version 2>&1 | head -n 1)"

# Check Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "ERROR: ANDROID_HOME environment variable is not set"
    echo "Set it with: export ANDROID_HOME=/path/to/android/sdk"
    exit 1
fi

if [ ! -d "$ANDROID_HOME" ]; then
    echo "ERROR: ANDROID_HOME path does not exist: $ANDROID_HOME"
    exit 1
fi

echo "✓ Android SDK found: $ANDROID_HOME"

# Create local.properties
echo "sdk.dir=$ANDROID_HOME" > local.properties
echo "✓ Created local.properties"

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean

# Build options
BUILD_TYPE=${1:-debug}

if [ "$BUILD_TYPE" == "debug" ]; then
    echo "Building Debug APK..."
    ./gradlew assembleDebug --stacktrace
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    
elif [ "$BUILD_TYPE" == "release" ]; then
    echo "Building Release APK..."
    ./gradlew assembleRelease --stacktrace
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
    
elif [ "$BUILD_TYPE" == "bundle" ]; then
    echo "Building Release Bundle..."
    ./gradlew bundleRelease --stacktrace
    APK_PATH="app/build/outputs/bundle/release/app-release.aab"
    
else
    echo "Usage: $0 [debug|release|bundle]"
    exit 1
fi

# Check if build succeeded
if [ -f "$APK_PATH" ]; then
    echo ""
    echo "================================"
    echo "✓ Build Successful!"
    echo "================================"
    echo "Output: $APK_PATH"
    ls -lh "$APK_PATH"
    echo ""
    
    if [ "$BUILD_TYPE" == "debug" ]; then
        echo "Install on device:"
        echo "  adb install -r $APK_PATH"
    fi
else
    echo "ERROR: Build failed - APK not found at $APK_PATH"
    exit 1
fi
