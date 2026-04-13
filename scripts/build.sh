#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
BACKEND_ROOT=$(CDPATH= cd -- "$PROJECT_ROOT/backend" && pwd)
FRONTEND_ROOT=$(CDPATH= cd -- "$PROJECT_ROOT/frontend" && pwd)

echo "Building backend jar..."
cd "$BACKEND_ROOT"
mvn clean package -DskipTests

echo "Building frontend assets..."
cd "$FRONTEND_ROOT"
npm ci
npm run build

echo "Building Docker images..."
cd "$PROJECT_ROOT"
docker compose build
