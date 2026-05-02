#!/bin/bash

# Start asset-radar frontend development server
# Run this in a separate terminal after backend is running

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🎨 Asset-Radar Frontend Development Server"
echo "=========================================="
echo ""

cd "$PROJECT_DIR/frontend"

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
  echo "1️⃣  Installing dependencies..."
  npm install
  echo "   ✅ Dependencies installed"
  echo ""
fi

echo "2️⃣  Starting frontend dev server..."
echo ""
echo "   Frontend will start at: http://localhost:5173"
echo "   API backend: http://localhost:8080"
echo ""
echo "   Features:"
echo "   - Real-time dashboard with asset prices (SSE stream)"
echo "   - Portfolio recommendations section with color-coded actions"
echo "   - Alerts display"
echo "   - Analytics page with charts"
echo ""
echo "   HMR enabled: Changes auto-reload in browser"
echo ""

npm run dev
