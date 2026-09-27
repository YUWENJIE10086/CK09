#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/ruoyi-barn"

if ! command -v mvn >/dev/null 2>&1; then
  echo "错误：未找到 Maven"
  exit 1
fi
if ! command -v java >/dev/null 2>&1; then
  echo "错误：未找到 Java，请安装 JDK 17"
  exit 1
fi

echo "编译后端 JAR..."
(
  cd "$BACKEND_DIR"
  mvn clean package -DskipTests
)

echo "后端编译完成：$BACKEND_DIR/target/ruoyi-barn.jar"

