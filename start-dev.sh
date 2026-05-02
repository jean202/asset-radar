#!/bin/bash

# Start asset-radar development environment
# Prerequisites: Docker Desktop, Java 21, Node.js

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 Asset-Radar Development Environment"
echo "========================================"
echo ""

# Step 1: Start Docker infrastructure
echo "1️⃣  Starting Docker infrastructure..."
cd "$PROJECT_DIR"
docker compose up -d kafka redis postgres prometheus grafana loki promtail tempo alertmanager
echo "   ✅ Docker services started"
echo ""

# Wait for services to be ready
echo "2️⃣  Waiting for services to be ready (30s)..."
sleep 30
echo "   ✅ Services ready"
echo ""

# Step 2: Start backend
echo "3️⃣  Starting backend (Spring Boot)..."
cd "$PROJECT_DIR"
echo "   Running: ./gradlew bootRun"
echo ""
echo "   Backend will start at: http://localhost:8080"
echo "   API Swagger UI: http://localhost:8080/swagger-ui/index.html"
echo ""
echo "   (This process will keep running. Open a new terminal for next steps)"
echo ""

./gradlew bootRun
