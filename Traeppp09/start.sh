#!/usr/bin/env bash
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/ruoyi-barn"
RUN_DIR="$SCRIPT_DIR/.run"
BACKEND_PORT="${BACKEND_PORT:-8082}"
FRONTEND_PORT="${FRONTEND_PORT:-5000}"

required_vars=(DB_HOST DB_NAME DB_USER DB_PASS JWT_SECRET)
for name in "${required_vars[@]}"; do
  if [ -z "${!name:-}" ]; then
    echo "错误：启动前必须设置环境变量 $name"
    exit 1
  fi
done
export DB_PORT="${DB_PORT:-3306}"
export UPLOAD_PATH="${UPLOAD_PATH:-$SCRIPT_DIR/upload/}"

if ! command -v java >/dev/null 2>&1; then
  echo "错误：未找到 Java，请安装 JDK 17"
  exit 1
fi
if ! command -v npm >/dev/null 2>&1; then
  echo "错误：未找到 npm，请安装 Node.js"
  exit 1
fi
if [ ! -f "$BACKEND_DIR/target/ruoyi-barn.jar" ]; then
  echo "错误：后端 JAR 不存在，请先执行 ./rebuild.sh"
  exit 1
fi
if [ ! -x "$SCRIPT_DIR/node_modules/.bin/vite" ]; then
  echo "错误：前端依赖不存在，请先执行 npm ci"
  exit 1
fi

mkdir -p "$RUN_DIR" "$UPLOAD_PATH"

if ! lsof -i:"$BACKEND_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "启动后端..."
  (
    cd "$BACKEND_DIR" || exit 1
    nohup java -jar target/ruoyi-barn.jar > /tmp/kaof-backend.log 2>&1 &
    echo $! > "$RUN_DIR/backend.pid"
  )
else
  echo "后端端口 $BACKEND_PORT 已在使用"
fi

if ! lsof -i:"$FRONTEND_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "启动前端..."
  (
    cd "$SCRIPT_DIR" || exit 1
    nohup npm run dev -- --port "$FRONTEND_PORT" > /tmp/kaof-frontend.log 2>&1 &
    echo $! > "$RUN_DIR/frontend.pid"
  )
else
  echo "前端端口 $FRONTEND_PORT 已在使用"
fi

echo "前端: http://localhost:$FRONTEND_PORT"
echo "后端: http://localhost:$BACKEND_PORT"

