#!/bin/bash
# Generate HTML slides from Markdown using Marp CLI
# Usage: ./generate-slides.sh

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_FILE="$SCRIPT_DIR/streaming-spark-flink.html"
MD_FILE="$SCRIPT_DIR/streaming-spark-flink.md"

echo "Generating slides from $MD_FILE..."

# Generate HTML from the Markdown file
npx @marp-team/marp-cli@latest "$MD_FILE" --output "$OUTPUT_FILE"

echo "Slides generated successfully at: $OUTPUT_FILE"

# Open the generated HTML file in the default browser (Windows)
if command -v cmd.exe &> /dev/null; then
    # Convert Unix path to Windows path and open with default browser
    WIN_PATH=$(wslpath -w "$OUTPUT_FILE" 2>/dev/null || cygpath -w "$OUTPUT_FILE" 2>/dev/null || echo "$OUTPUT_FILE")
    explorer.exe "$WIN_PATH"
elif command -v open &> /dev/null; then
    open "$OUTPUT_FILE"
elif command -v xdg-open &> /dev/null; then
    xdg-open "$OUTPUT_FILE"
else
    echo "Please open $OUTPUT_FILE in your browser"
fi
