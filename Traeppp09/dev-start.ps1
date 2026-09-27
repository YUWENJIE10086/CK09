<#
.SYNOPSIS
    烤房项目 - 本地开发环境一键启动脚本
.DESCRIPTION
    启动后端 Spring Boot 服务 (端口 8082) 和前端 Vite 开发服务器 (端口 4000)
    数据库连接信息从环境变量读取
.NOTES
    前端访问地址: http://localhost:4000
    后端 API:     http://localhost:8082
#>

$ErrorActionPreference = "Stop"

# ===== 路径配置 =====
$PROJECT_DIR = $PSScriptRoot
$BACKEND_DIR = "$PROJECT_DIR\ruoyi-barn"
$JAR_PATH = "$BACKEND_DIR\target\ruoyi-barn.jar"

# ===== 端口配置 =====
$FRONTEND_PORT = 5000
$BACKEND_PORT = 8082

# ===== 检查必需环境 =====
$requiredEnv = @("DB_HOST", "DB_NAME", "DB_USER", "DB_PASS", "JWT_SECRET")
foreach ($name in $requiredEnv) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        Write-Host "[ERROR] Missing required environment variable: $name" -ForegroundColor Red
        exit 1
    }
}

$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCommand) {
    Write-Host "[ERROR] Java not found. Please install JDK 17." -ForegroundColor Red
    exit 1
}

# ===== 检查 jar 包是否存在 =====
$needBuild = $false
if (-not (Test-Path $JAR_PATH)) {
    Write-Host "[INFO] Backend jar not found, need to build..." -ForegroundColor Yellow
    $needBuild = $true
}

# ===== 如果需要构建后端 =====
if ($needBuild) {
    Write-Host "`n========== Building Backend ==========" -ForegroundColor Cyan
    $mvnCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if (-not $mvnCommand) { $mvnCommand = Get-Command mvn -ErrorAction SilentlyContinue }
    if (-not $mvnCommand) {
        Write-Host "[ERROR] Maven not found." -ForegroundColor Red
        exit 1
    }
    Set-Location $BACKEND_DIR
    & $mvnCommand.Source clean package -DskipTests 2>&1 | Select-Object -Last 5
    if (-not (Test-Path $JAR_PATH)) {
        Write-Host "[ERROR] Backend build failed!" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] Backend built successfully." -ForegroundColor Green
}

# ===== 创建 upload 目录 =====
$uploadDir = "$PROJECT_DIR\upload"
if (-not (Test-Path $uploadDir)) {
    New-Item -ItemType Directory -Path $uploadDir -Force | Out-Null
    Write-Host "[INFO] Created upload directory: $uploadDir" -ForegroundColor Yellow
}

# ===== 启动后端（带守护，异常退出自动重启）=====
Write-Host "`n========== Starting Backend (Port $BACKEND_PORT) ==========" -ForegroundColor Cyan
$backendJob = Start-Process -FilePath "powershell.exe" `
    -ArgumentList "-ExecutionPolicy", "Bypass", "-File", "`"$PROJECT_DIR\backend-guard.ps1`"" `
    -WorkingDirectory $PROJECT_DIR `
    -PassThru `
    -WindowStyle Normal

Write-Host "[OK] Backend guard PID: $($backendJob.Id)" -ForegroundColor Green
Write-Host "     Waiting for backend to start..." -ForegroundColor Yellow

# 等待后端就绪
$ready = $false
for ($i = 1; $i -le 30; $i++) {
    Start-Sleep -Seconds 2
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$BACKEND_PORT/api/login" -Method GET -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        $ready = $true
        break
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -in @(401, 403, 405, 404)) {
            $ready = $true
            break
        }
    }
    Write-Host "     ...waiting ($i/30)" -ForegroundColor DarkGray
}

if ($ready) {
    Write-Host "[OK] Backend is ready at http://localhost:$BACKEND_PORT" -ForegroundColor Green
} else {
    Write-Host "[WARN] Backend may not be ready yet. Check the backend window." -ForegroundColor Yellow
}

# ===== 启动前端 =====
Write-Host "`n========== Starting Frontend (Port $FRONTEND_PORT) ==========" -ForegroundColor Cyan
Set-Location $PROJECT_DIR

# 确保依赖已安装
if (-not (Test-Path "$PROJECT_DIR\node_modules\vite")) {
    Write-Host "[INFO] Installing frontend dependencies..." -ForegroundColor Yellow
    npm install --legacy-peer-deps --include=dev
}

$frontendJob = Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c", "npx vite --port $FRONTEND_PORT --host" `
    -WorkingDirectory $PROJECT_DIR `
    -PassThru `
    -WindowStyle Normal

Write-Host "[OK] Frontend PID: $($frontendJob.Id)" -ForegroundColor Green
Start-Sleep -Seconds 3

# ===== 输出信息 =====
Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  Local Dev Environment Started!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Frontend:  http://localhost:$FRONTEND_PORT" -ForegroundColor White
Write-Host "  Backend:   http://localhost:$BACKEND_PORT" -ForegroundColor White
Write-Host "  Database:  configured by environment variables" -ForegroundColor White
Write-Host ""
Write-Host "  Backend PID:  $($backendJob.Id)" -ForegroundColor DarkGray
Write-Host "  Frontend PID: $($frontendJob.Id)" -ForegroundColor DarkGray
Write-Host ""
Write-Host "  Press Ctrl+C in this window to stop both services." -ForegroundColor Yellow
Write-Host "  Or close the backend/frontend windows directly." -ForegroundColor DarkGray
Write-Host "==========================================================" -ForegroundColor Green

# ===== 等待退出 =====
try {
    Wait-Process -Id $backendJob.Id, $frontendJob.Id
} catch {
    Write-Host "`n[INFO] Services stopped." -ForegroundColor Yellow
}
