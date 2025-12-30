# Script to build and run the Spark Stock Processor
# Uses the specific JDK at C:\Program Files\Java\jdk-25.0.1

$ErrorActionPreference = "Stop"

$JDK_PATH = "C:\Program Files\Java\jdk-21.0.2"

if (-not (Test-Path $JDK_PATH)) {
    Write-Error "JDK not found at $JDK_PATH"
    exit 1
}

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

Write-Host "`nBuild successful! Starting Spark processor..." -ForegroundColor Green
Write-Host "Connecting to socket at localhost:9999" -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop`n" -ForegroundColor Yellow

& java -jar target\spark-stock-processor-1.0-SNAPSHOT.jar
