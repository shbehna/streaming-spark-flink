#!/bin/bash
# Script to install dependencies and run the Generator App

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Generator App - Starting..."
echo ""

# Check if requirements are installed
if ! python -c "import flask" 2>/dev/null; then
    echo "[INSTALL] Installing dependencies..."
    pip install -r requirements.txt
    echo "[SUCCESS] Dependencies installed"
    echo ""
fi

# Start the application
echo "[START] Starting Flask application on http://localhost:5000"
echo "Press Ctrl+C to stop"
echo ""

python app.py
