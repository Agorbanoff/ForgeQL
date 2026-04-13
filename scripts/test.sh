#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
BACKEND_ROOT=$(CDPATH= cd -- "$PROJECT_ROOT/backend" && pwd)
FRONTEND_ROOT=$(CDPATH= cd -- "$PROJECT_ROOT/frontend" && pwd)

echo "Running backend tests..."
cd "$BACKEND_ROOT"
mvn test

echo "Running frontend tests..."
cd "$FRONTEND_ROOT"
npm ci
npm test
