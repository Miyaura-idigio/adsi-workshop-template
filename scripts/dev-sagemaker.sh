#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 既存プロセス停止
echo "=== Stopping existing processes ==="
lsof -ti:3000 | xargs kill -9 2>/dev/null || true
lsof -ti:3001 | xargs kill -9 2>/dev/null || true
lsof -ti:8080 | xargs kill -9 2>/dev/null || true
sleep 1

# 環境変数
export SAGEMAKER=1
export NEXT_PUBLIC_BASE_PATH="/codeeditor/default/absports/3000"

# Backend 起動
echo "=== Starting Backend (port 8080) ==="
cd "$PROJECT_ROOT/backend"
mvn spring-boot:run -q &
BACKEND_PID=$!

# Frontend ビルド + 起動
echo "=== Building Frontend ==="
cd "$PROJECT_ROOT/frontend"
npx next build

echo "=== Starting Frontend (port 3001) ==="
npx next start -H 127.0.0.1 -p 3001 &
NEXT_PID=$!

sleep 2

echo "=== Starting SageMaker Proxy (port 3000) ==="
node scripts/sagemaker-proxy.mjs &
PROXY_PID=$!

echo ""
echo "=== All services started ==="
echo "Backend:  http://localhost:8080 (PID: $BACKEND_PID)"
echo "Next.js:  http://127.0.0.1:3001 (PID: $NEXT_PID)"
echo "Proxy:    http://localhost:3000 (PID: $PROXY_PID)"
echo ""
echo "Open in browser: PORTS tab → globe icon on 3000 → change 'ports' to 'absports'"
echo ""
echo "To stop: kill $BACKEND_PID $NEXT_PID $PROXY_PID"

wait
