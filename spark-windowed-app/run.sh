#!/bin/bash
# Script to build and run the Spark Windowed Stock Processor
# Uses JDK from environment variable JDK21_PATH

set -e

JDK_PATH="${JDK21_PATH}"

if [ -z "$JDK_PATH" ]; then
    echo "ERROR: JDK21_PATH environment variable is not set" >&2
    exit 1
fi

if [ ! -d "$JDK_PATH" ]; then
    echo "ERROR: JDK not found at $JDK_PATH" >&2
    exit 1
fi

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
echo "Cleaning checkpoint and state directories..."
rm -rf checkpoint spark-warehouse metastore_db derby.log 2>/dev/null || true
echo "State cleaned"

echo ""
echo "Build successful! Starting Spark Windowed processor..."
echo "Connecting to Kafka at localhost:9092"
echo "Press Ctrl+C to stop"
echo ""

java -jar target/spark-windowed-stock-processor-1.0-SNAPSHOT.jar
