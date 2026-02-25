#!/bin/bash
# Script to build and run the Flink Stock Processor
# Uses JDK from environment variable JDK25_PATH

set -e

# Set JDK path
JDK_PATH="${JDK25_PATH}"

# Verify environment variable is set
if [ -z "$JDK_PATH" ]; then
    echo "ERROR: JDK25_PATH environment variable is not set" >&2
    exit 1
fi

# Verify JDK exists
if [ ! -d "$JDK_PATH" ]; then
    echo "ERROR: JDK not found at $JDK_PATH" >&2
    exit 1
fi

# Set environment variables
export JAVA_HOME="$JDK_PATH"
export PATH="$JDK_PATH/bin:$PATH"

echo "Using JDK: $JDK_PATH"
echo "Java version:"
java -version

echo ""
echo "Building project with Maven..."
mvn clean package

if [ $? -ne 0 ]; then
    echo "ERROR: Maven build failed" >&2
    exit 1
fi

echo ""
echo "Cleaning state directories..."
rm -rf checkpoint .flink 2>/dev/null || true
echo "State cleaned"

echo ""
echo "Build successful! Starting Flink processor..."
echo "Connecting to Kafka at localhost:9092"
echo "Press Ctrl+C to stop"
echo ""

java -jar target/flink-stock-processor-1.0-SNAPSHOT.jar
