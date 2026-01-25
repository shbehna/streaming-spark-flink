# Generate HTML slides from Markdown using Marp CLI
# Usage: .\generate-slides.ps1

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$outputFile = Join-Path $scriptDir "streaming-spark-flink.html"

# Generate HTML from the Markdown file
$mdFile = Join-Path $scriptDir "streaming-spark-flink.md"
Write-Host "Generating slides from $mdFile..."

npx @marp-team/marp-cli@latest "$mdFile" --output "$outputFile"

Write-Host "Slides generated successfully at: $outputFile"

# Open the generated HTML file in the default browser
Start-Process "$outputFile"
