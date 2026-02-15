# Script to build and run the Flink Windowed Stock Processor
# Uses JDK from environment variable JDK25_PATH

$ErrorActionPreference = "Stop"

# Set JDK path
$JDK_PATH = $env:JDK25_PATH

# Verify environment variable is set
if (-not $JDK_PATH) {
    Write-Error "JDK25_PATH environment variable is not set"
    exit 1
}

# Verify JDK exists
if (-not (Test-Path $JDK_PATH)) {
    Write-Error "JDK not found at $JDK_PATH"
    exit 1
}

# Set environment variables
$env:JAVA_HOME = $JDK_PATH
$env:PATH = "$JDK_PATH\bin;$env:PATH"

Write-Host "Using JDK: $JDK_PATH" -ForegroundColor Green
Write-Host "Java version:" -ForegroundColor Green
& java -version

Write-Host "`nBuilding project with Maven..." -ForegroundColor Cyan
mvn clean package

if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven build failed"
    exit 1
}

Write-Host "`nBuild successful! Starting Flink Windowed processor..." -ForegroundColor Green
Write-Host "Connecting to socket at localhost:9999" -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop`n" -ForegroundColor Yellow

& java -jar target\flink-windowed-stock-processor-1.0-SNAPSHOT.jar
