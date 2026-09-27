#!/bin/bash
# Usage: ./rebuild.sh <service-name>
# Example: ./rebuild.sh auth-service

set -e
SERVICE=${1:-auth-service}
cd ~/java_prg/stk

echo "🔨 Building $SERVICE..."
mvn -pl "$SERVICE" -am clean package -DskipTests -q

echo "🐳 Docker build $SERVICE..."
docker compose build "$SERVICE" 2>&1 | tail -3

echo "🚀 Restarting $SERVICE..."
docker compose up -d --force-recreate "$SERVICE" 2>&1 | tail -2

echo "✅ Done! $SERVICE is up."
