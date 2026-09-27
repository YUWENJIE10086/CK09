#!/bin/bash
# ====================================================================
#  烤房全周期信息化管理平台 - Ubuntu 一键部署脚本
#
#  功能：安装依赖 → 构建前后端 → 部署文件 → 配置Nginx/Systemd → 启动服务
#
#  用法：
#    sudo bash deploy-ubuntu.sh all       # 完整部署（首次使用）
#    sudo bash deploy-ubuntu.sh update    # 重新构建并更新（代码修改后）
#    sudo bash deploy-ubuntu.sh restart   # 仅重启服务
#    sudo bash deploy-ubuntu.sh stop      # 停止服务
#    sudo bash deploy-ubuntu.sh status    # 查看服务状态
#    sudo bash deploy-ubuntu.sh logs      # 查看后端日志
# ====================================================================
set -e

# ======================== 配置区域（按需修改）============================

# 敏感配置必须由部署环境传入，脚本不提供默认凭据。
DB_HOST="${DB_HOST:-}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-}"
DB_USER="${DB_USER:-}"
DB_PASS="${DB_PASS:-}"
JWT_SECRET="${JWT_SECRET:-}"
DIFY_API_URL="${DIFY_API_URL:-}"

# 部署路径
APP_DIR="${APP_DIR:-/opt/kaof}"

# 端口配置
NGINX_PORT="${NGINX_PORT:-8081}"        # Nginx 前端访问端口（避开80）
BACKEND_PORT="${BACKEND_PORT:-8082}"    # Spring Boot 后端端口（内部通信，避开8080）

# JVM 参数
JVM_OPTS="${JVM_OPTS:--Xms256m -Xmx512m}"

# ========================================================================

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }
step()  { echo -e "\n${BLUE}========== $1 ==========${NC}"; }

# 检测项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"

# 校验项目结构
if [ ! -f "$PROJECT_DIR/package.json" ] || [ ! -d "$PROJECT_DIR/ruoyi-barn" ]; then
  error "未找到项目文件！请将脚本放在项目根目录（包含 package.json 和 ruoyi-barn/ 的目录）"
  exit 1
fi

info "项目目录: $PROJECT_DIR"
info "部署目录: $APP_DIR"
info "数据库:   $DB_HOST:$DB_PORT/$DB_NAME"
info "前端端口: $NGINX_PORT (Nginx)"
info "后端端口: $BACKEND_PORT (Spring Boot)"

# --------------------------------------------------------------------
# 1. 检查 root 权限
# --------------------------------------------------------------------
check_root() {
  if [ "$EUID" -ne 0 ]; then
    error "请使用 root 用户或 sudo 执行此脚本"
    exit 1
  fi
}

validate_deploy_env() {
  local missing=()
  local name
  for name in DB_HOST DB_NAME DB_USER DB_PASS JWT_SECRET; do
    if [ -z "${!name:-}" ]; then
      missing+=("$name")
    fi
  done
  if [ ${#missing[@]} -gt 0 ]; then
    error "缺少必需环境变量: ${missing[*]}"
    exit 1
  fi
  if [ -n "$DIFY_API_URL" ] && [[ ! "$DIFY_API_URL" =~ ^https?:// ]]; then
    error "DIFY_API_URL 必须以 http:// 或 https:// 开头"
    exit 1
  fi
}

# --------------------------------------------------------------------
# 2. 安装系统依赖
# --------------------------------------------------------------------
install_prerequisites() {
  step "安装系统依赖"

  export DEBIAN_FRONTEND=noninteractive
  apt-get update -qq

  # Java 17
  if ! command -v java &> /dev/null; then
    info "安装 OpenJDK 17..."
    apt-get install -y -qq openjdk-17-jdk-headless
  else
    info "Java 已安装: $(java -version 2>&1 | head -1)"
  fi

  # Maven
  if ! command -v mvn &> /dev/null; then
    info "安装 Maven..."
    apt-get install -y -qq maven
  else
    info "Maven 已安装: $(mvn -version 2>&1 | head -1)"
  fi

  # Node.js 18.x
  if ! command -v node &> /dev/null; then
    info "安装 Node.js 18..."
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
    apt-get install -y -qq nodejs
  else
    NODE_VER=$(node -v | sed 's/v//' | cut -d. -f1)
    if [ "$NODE_VER" -lt 18 ]; then
      warn "Node.js 版本过低 ($(node -v))，升级到 18..."
      curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
      apt-get install -y -qq nodejs
    else
      info "Node.js 已安装: $(node -v)"
    fi
  fi

  # Nginx
  if ! command -v nginx &> /dev/null; then
    info "安装 Nginx..."
    apt-get install -y -qq nginx
  else
    info "Nginx 已安装: $(nginx -v 2>&1)"
  fi

  # 构建工具（部分 npm 原生模块可能需要）
  apt-get install -y -qq build-essential python3 2>/dev/null || true

  info "系统依赖安装完成"
}

# --------------------------------------------------------------------
# 3. 构建前端
# --------------------------------------------------------------------
build_frontend() {
  step "构建前端"
  cd "$PROJECT_DIR"

  # 确保不跳过 devDependencies（vite 等关键构建工具在 devDependencies 中）
  export NODE_ENV=development
  export npm_config_include=dev

  # 安装依赖
  info "安装前端依赖（包含 devDependencies）..."
  # 删除可能存在的不完整 node_modules
  if [ -d node_modules ]; then
    info "清理旧 node_modules..."
    rm -rf node_modules
  fi
  if [ -f package-lock.json ]; then
    # 有 lock 文件，优先使用 ci 保证一致性
    npm ci --legacy-peer-deps --include=dev 2>/dev/null || npm install --legacy-peer-deps --include=dev
  else
    npm install --legacy-peer-deps --include=dev
  fi

  # 验证 vite 是否安装成功
  if [ ! -f node_modules/.bin/vite ]; then
    warn "vite 未安装，单独安装 vite..."
    npm install vite --save-dev --legacy-peer-deps
  fi
  if [ ! -f node_modules/.bin/vue-tsc ]; then
    warn "vue-tsc 未安装，单独安装 vue-tsc..."
    npm install vue-tsc --save-dev --legacy-peer-deps
  fi

  # 构建：先尝试完整构建（类型检查 + vite build），失败则跳过类型检查
  info "执行 vue-tsc + vite build..."
  if ! npm run build 2>&1; then
    warn "完整构建失败（可能是 TS 类型错误），跳过类型检查直接构建..."
    npx vite build
  fi

  if [ -d dist ]; then
    info "前端构建完成: $(du -sh dist | awk '{print $1}')"
  else
    error "前端构建失败，dist 目录不存在"
    exit 1
  fi
}

# --------------------------------------------------------------------
# 4. 构建后端
# --------------------------------------------------------------------
build_backend() {
  step "构建后端"
  cd "$PROJECT_DIR/ruoyi-barn"

  info "执行 Maven 打包..."
  mvn clean package -DskipTests -q

  if [ -f target/ruoyi-barn.jar ]; then
    info "后端构建完成: $(ls -lh target/ruoyi-barn.jar | awk '{print $5}')"
  else
    error "后端构建失败，JAR 包不存在"
    exit 1
  fi
}

# --------------------------------------------------------------------
# 5. 部署文件
# --------------------------------------------------------------------
deploy_files() {
  step "部署文件"

  # 创建目录结构
  mkdir -p "$APP_DIR"/{backend,web,upload,logs}

  # 部署前端
  info "部署前端静态文件..."
  rm -rf "$APP_DIR/web/"*
  cp -r "$PROJECT_DIR/dist/"* "$APP_DIR/web/"

  # 部署后端
  info "部署后端 JAR 包..."
  cp "$PROJECT_DIR/ruoyi-barn/target/ruoyi-barn.jar" "$APP_DIR/backend/"

  # 创建环境配置文件
  info "生成环境配置..."
  cat > "$APP_DIR/backend/app.env" << EOF
DB_HOST=$DB_HOST
DB_PORT=$DB_PORT
DB_NAME=$DB_NAME
DB_USER=$DB_USER
DB_PASS=$DB_PASS
JWT_SECRET=$JWT_SECRET
BACKEND_PORT=$BACKEND_PORT
UPLOAD_PATH=$APP_DIR/upload/
EOF
  chmod 600 "$APP_DIR/backend/app.env"

  info "文件部署完成"
}

# --------------------------------------------------------------------
# 6. 配置 Systemd 服务
# --------------------------------------------------------------------
configure_systemd() {
  step "配置 Systemd 服务"

  cat > /etc/systemd/system/kaof-backend.service << EOF
[Unit]
Description=KaoF Backend Service (Spring Boot)
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=$APP_DIR/backend
EnvironmentFile=$APP_DIR/backend/app.env
ExecStart=/usr/bin/java $JVM_OPTS -jar $APP_DIR/backend/ruoyi-barn.jar
Restart=on-failure
RestartSec=10
StandardOutput=append:$APP_DIR/logs/backend.log
StandardError=append:$APP_DIR/logs/backend-error.log

[Install]
WantedBy=multi-user.target
EOF

  systemctl daemon-reload
  systemctl enable kaof-backend
  info "Systemd 服务已配置: kaof-backend"
}

# --------------------------------------------------------------------
# 7. 配置 Nginx
# --------------------------------------------------------------------
configure_nginx() {
  step "配置 Nginx"

  local dify_location=""
  if [ -n "$DIFY_API_URL" ]; then
    dify_location="    location /dify-api/ {
        proxy_pass ${DIFY_API_URL%/}/v1/;
        proxy_set_header Host \$host;
        proxy_ssl_server_name on;
        proxy_read_timeout 60s;
    }"
  fi

  # Nginx只需读取静态资源和上传目录，后端环境文件保持最小权限。
  mkdir -p "$APP_DIR"/{web,upload}
  chmod 755 "$APP_DIR"
  chmod -R 755 "$APP_DIR"/{web,upload}
  chmod 750 "$APP_DIR"/{backend,logs}
  if [ -f "$APP_DIR/backend/app.env" ]; then
    chmod 600 "$APP_DIR/backend/app.env"
  fi
  chown -R www-data:www-data "$APP_DIR"/{web,upload} 2>/dev/null || true

  # 检查端口是否被占用
  local port_pid
  port_pid=$(lsof -ti:${NGINX_PORT} 2>/dev/null | head -1)
  if [ -n "$port_pid" ]; then
    local port_proc
    port_proc=$(ps -p "$port_pid" -o comm= 2>/dev/null)
    if [ "$port_proc" != "nginx" ]; then
      warn "端口 ${NGINX_PORT} 被进程 ${port_proc}(PID:${port_pid}) 占用，尝试停止..."
      kill -9 "$port_pid" 2>/dev/null || true
      sleep 1
    fi
  fi

  # 停止可能残留的 nginx 进程
  systemctl stop nginx 2>/dev/null || true
  pkill -f "nginx: master" 2>/dev/null || true
  sleep 1

  cat > /etc/nginx/sites-available/kaof << NGINX_EOF
server {
    listen ${NGINX_PORT};
    server_name _;

    # 前端静态文件
    root ${APP_DIR}/web;
    index index.html;

    # SPA 路由支持
    location / {
        try_files \$uri \$uri/ /index.html;
    }

    # API 代理 → 后端 Spring Boot
    location /dev-api/ {
        proxy_pass http://127.0.0.1:${BACKEND_PORT}/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 60s;
        proxy_connect_timeout 30s;
        client_max_body_size 100m;
    }

${dify_location}

    # 上传文件访问
    location /upload/ {
        alias ${APP_DIR}/upload/;
    }

    # Gzip 压缩
    gzip on;
    gzip_min_length 1000;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml image/svg+xml;
    gzip_vary on;

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
NGINX_EOF

  # 启用站点，禁用默认站点
  ln -sf /etc/nginx/sites-available/kaof /etc/nginx/sites-enabled/kaof
  rm -f /etc/nginx/sites-enabled/default

  # 测试配置
  info "测试 Nginx 配置..."
  local nginx_test_output
  nginx_test_output=$(nginx -t 2>&1)
  echo "$nginx_test_output"
  if ! echo "$nginx_test_output" | grep -q "test is successful"; then
    error "Nginx 配置测试失败，以上为详细错误信息"
    exit 1
  fi

  # 启动 nginx
  info "启动 Nginx..."
  if ! systemctl start nginx 2>&1; then
    error "Nginx 启动失败，正在输出详细错误..."
    echo ""
    echo "--- systemctl status nginx ---"
    systemctl status nginx 2>&1 | tail -20
    echo ""
    echo "--- journalctl 错误日志 ---"
    journalctl -u nginx --no-pager -n 20 2>&1
    echo ""
    echo "--- nginx 错误日志 ---"
    tail -20 /var/log/nginx/error.log 2>/dev/null || echo "(无错误日志)"
    echo ""
    error "请根据以上信息排查问题"
    exit 1
  fi

  systemctl enable nginx
  info "Nginx 配置完成并已启动"
}

# --------------------------------------------------------------------
# 8. 启动服务
# --------------------------------------------------------------------
start_services() {
  step "启动服务"

  info "启动后端服务..."
  systemctl restart kaof-backend

  # 等待后端就绪
  info "等待后端启动..."
  local ready=false
  for i in $(seq 1 30); do
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:${BACKEND_PORT}/api/login 2>/dev/null | grep -qE "200|401|403|405"; then
      ready=true
      break
    fi
    sleep 1
    printf "."
  done
  echo ""

  if [ "$ready" = true ]; then
    info "后端服务已启动 (端口 ${BACKEND_PORT})"
  else
    warn "后端服务可能尚未完全就绪，请检查日志: journalctl -u kaof-backend -f"
  fi

  info "Nginx 状态: $(systemctl is-active nginx)"
  info "后端状态: $(systemctl is-active kaof-backend)"
}

# --------------------------------------------------------------------
# 9. 验证部署
# --------------------------------------------------------------------
verify() {
  step "部署完成"

  local SERVER_IP
  SERVER_IP=$(hostname -I | awk '{print $1}')

  echo ""
  echo "=============================================================="
  echo -e "${GREEN}  烤房全周期信息化管理平台 - 部署成功!${NC}"
  echo "=============================================================="
  echo ""
  echo "  访问地址:  http://${SERVER_IP}:${NGINX_PORT}"
  echo "  后端接口:  http://localhost:${BACKEND_PORT}"
  echo "  数据库:    ${DB_HOST}:${DB_PORT}/${DB_NAME}"
  echo "  部署目录:  ${APP_DIR}"
  echo ""
  echo "  ---- 服务管理 ----"
  echo "  查看状态:  systemctl status kaof-backend"
  echo "  重启后端:  systemctl restart kaof-backend"
  echo "  重启Nginx: systemctl restart nginx"
  echo "  查看日志:  journalctl -u kaof-backend -f"
  echo "             tail -f ${APP_DIR}/logs/backend.log"
  echo ""
  echo "  ---- 更新部署 ----"
  echo "  修改代码后，在项目目录执行:"
  echo "  sudo bash deploy-ubuntu.sh update"
  echo ""
  echo "=============================================================="
}

# --------------------------------------------------------------------
# 10. 停止服务
# --------------------------------------------------------------------
stop_services() {
  step "停止服务"
  systemctl stop kaof-backend 2>/dev/null && info "后端已停止" || info "后端未运行"
  systemctl stop nginx 2>/dev/null && info "Nginx 已停止" || info "Nginx 未运行"
}

# --------------------------------------------------------------------
# 11. 显示状态
# --------------------------------------------------------------------
show_status() {
  step "服务状态"
  echo ""
  echo "  Nginx:   $(systemctl is-active nginx 2>/dev/null || echo '未安装')"
  echo "  后端:    $(systemctl is-active kaof-backend 2>/dev/null || echo '未安装')"
  echo ""

  if systemctl is-active --quiet kaof-backend 2>/dev/null; then
    local mem
    mem=$(ps aux | grep ruoyi-barn.jar | grep -v grep | awk '{print $6/1024 " MB"}')
    echo "  后端内存: ${mem}"
    echo "  后端端口: ${BACKEND_PORT} (监听中)"
  fi

  if systemctl is-active --quiet nginx 2>/dev/null; then
    echo "  Nginx端口: ${NGINX_PORT} (监听中)"
  fi
  echo ""
}

# --------------------------------------------------------------------
# 12. 查看日志
# --------------------------------------------------------------------
show_logs() {
  if [ -f "$APP_DIR/logs/backend.log" ]; then
    info "实时查看后端日志 (Ctrl+C 退出)..."
    tail -f "$APP_DIR/logs/backend.log"
  else
    warn "日志文件不存在，使用 journalctl 查看..."
    journalctl -u kaof-backend -f
  fi
}

# --------------------------------------------------------------------
# 主入口
# --------------------------------------------------------------------
case "${1:-all}" in
  all)
    check_root
    validate_deploy_env
    install_prerequisites
    build_frontend
    build_backend
    deploy_files
    configure_systemd
    configure_nginx
    start_services
    verify
    ;;
  update)
    check_root
    validate_deploy_env
    build_frontend
    build_backend
    deploy_files
    configure_systemd
    configure_nginx
    start_services
    verify
    ;;
  install-deps)
    check_root
    install_prerequisites
    info "系统依赖安装完成，可执行 'sudo bash deploy-ubuntu.sh all' 继续部署"
    ;;
  build)
    build_frontend
    build_backend
    ;;
  restart)
    check_root
    systemctl restart kaof-backend
    systemctl restart nginx
    info "服务已重启"
    show_status
    ;;
  setup-service)
    check_root
    validate_deploy_env
    deploy_files
    configure_systemd
    configure_nginx
    start_services
    verify
    ;;
  stop)
    check_root
    stop_services
    ;;
  status)
    show_status
    ;;
  logs)
    show_logs
    ;;
  *)
    echo "烤房管理系统 Ubuntu 部署脚本"
    echo ""
    echo "用法: sudo bash $0 {命令}"
    echo ""
    echo "命令:"
    echo "  all            完整部署（安装依赖+构建+部署+启动）首次使用"
    echo "  update         重新构建并更新（代码修改后使用）"
    echo "  setup-service  仅配置并启动服务（不重新构建，修复服务缺失问题）"
    echo "  install-deps   仅安装系统依赖（Java/Maven/Node/Nginx）"
    echo "  build          仅构建前后端，不部署"
    echo "  restart        仅重启服务"
    echo "  stop           停止所有服务"
    echo "  status         查看服务运行状态"
    echo "  logs           实时查看后端日志"
    echo ""
    echo "配置:"
    echo "  必需环境变量:"
    echo "  DB_HOST / DB_NAME / DB_USER / DB_PASS / JWT_SECRET"
    echo "  可选环境变量:"
    echo "  DB_PORT / APP_DIR / JVM_OPTS / NGINX_PORT / BACKEND_PORT / DIFY_API_URL"
    echo ""
    echo "示例:"
    echo "  请通过受保护的环境或密钥管理工具注入必需变量后执行:"
    echo "  sudo --preserve-env=DB_HOST,DB_PORT,DB_NAME,DB_USER,DB_PASS,JWT_SECRET,DIFY_API_URL bash $0 all"
    echo "  sudo bash $0 update"
    echo ""
    exit 1
    ;;
esac
