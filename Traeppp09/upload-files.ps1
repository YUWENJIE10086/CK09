<#
.SYNOPSIS
    烤房项目 - 修改文件上传部署脚本（SCP方式）
.DESCRIPTION
    使用 Windows 自带 scp/ssh 上传修改的文件到服务器并远程部署
.NOTES
    首次使用前请修改下方 ===== 配置区域 ===== 中的服务器信息
#>

# ====================== 配置区域（按需修改）======================

# 内网服务器连接信息
$ServerHost   = "192.168.1.100"       # 服务器IP（请改为实际地址）
$ServerUser   = "root"                 # SSH用户名
$ServerPort   = 22                     # SSH端口
$RemoteProjectDir = "/home/ycyc/kf"    # 服务器上项目源码目录

# 本地项目根目录
$LocalProjectDir = $PSScriptRoot

# ====================== 配置区域结束 ==============================

# 需要上传的文件列表（相对路径，对应项目根目录）
$FilesToUpload = @(
    "deploy-ubuntu.sh",
    "ruoyi-barn\src\main\resources\application.yml"
)

# 颜色输出
function Write-Info  { param($msg) Write-Host "[INFO]  $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Write-Err   { param($msg) Write-Host "[ERROR] $msg" -ForegroundColor Red }
function Write-Step  { param($msg) Write-Host "`n========== $msg ==========" -ForegroundColor Cyan }

# SSH 连接目标
$target = "${ServerUser}@${ServerHost}"

# ====================== 检查环境 ======================
Write-Step "检查环境"

$scpCmd = Get-Command scp -ErrorAction SilentlyContinue
if (-not $scpCmd) {
    Write-Err "未找到 scp 命令！"
    Write-Host ""
    Write-Host "解决方案：Windows 10/11 → 设置 → 应用 → 可选功能 → 添加 'OpenSSH 客户端'"
    exit 1
}
Write-Info "scp 可用: $($scpCmd.Source)"

# ====================== 检查本地文件 ======================
Write-Step "检查本地文件"
$allExist = $true
foreach ($file in $FilesToUpload) {
    $fullPath = Join-Path $LocalProjectDir $file
    if (Test-Path $fullPath) {
        $size = (Get-Item $fullPath).Length
        Write-Info "  [OK] $file ($size bytes)"
    } else {
        Write-Err "  [缺失] $file"
        $allExist = $false
    }
}
if (-not $allExist) {
    Write-Err "有文件缺失，请检查！"
    exit 1
}

# ====================== 上传文件 ======================
Write-Step "上传文件到服务器"

foreach ($file in $FilesToUpload) {
    $localPath = Join-Path $LocalProjectDir $file
    # 转换为远程路径（反斜杠转正斜杠）
    $remoteRelPath = $file -replace '\\', '/'
    $remotePath = "${RemoteProjectDir}/${remoteRelPath}"

    Write-Info "上传: $file"
    Write-Host "       -> ${target}:${remotePath}"

    # 先确保远程目录存在
    $remoteDir = Split-Path $remotePath -Parent
    ssh -p $ServerPort $target "mkdir -p $remoteDir" 2>$null

    # 上传文件
    scp -P $ServerPort $localPath "${target}:${remotePath}"

    if ($LASTEXITCODE -eq 0) {
        Write-Info "  [成功] $file"
    } else {
        Write-Err "  [失败] $file (exit code: $LASTEXITCODE)"
        Write-Host ""
        Write-Host "常见问题："
        Write-Host "  1. 检查服务器IP和端口是否正确"
        Write-Host "  2. 首次连接需确认主机指纹（先手动 ssh -p $ServerPort $target 一次）"
        Write-Host "  3. 检查网络是否可达"
        exit 1
    }
}

Write-Step "上传完成"

# ====================== 远程执行部署 ======================
Write-Step "远程执行部署"

$deployCmd = "cd ${RemoteProjectDir} `&`& sudo bash deploy-ubuntu.sh update"
Write-Info "执行远程部署..."
Write-Host ""

ssh -p $ServerPort $target $deployCmd
$exitCode = $LASTEXITCODE

if ($exitCode -eq 0) {
    Write-Step "部署完成"
    Write-Info "请访问 http://${ServerHost}:8081 验证服务"
} else {
    Write-Warn "远程部署可能失败 (exit code: $exitCode)"
    Write-Host "请手动登录服务器检查："
    Write-Host "  ssh -p $ServerPort $target"
    Write-Host "  cd ${RemoteProjectDir} `&`& sudo bash deploy-ubuntu.sh update"
}

Write-Host ""
Write-Host "------------------------------------------------------------"
Write-Host "  当前配置："
Write-Host "    服务器:   ${ServerUser}@${ServerHost}:${ServerPort}"
Write-Host "    项目目录: ${RemoteProjectDir}"
Write-Host "------------------------------------------------------------"
