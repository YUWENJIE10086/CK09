<#
.SYNOPSIS
    注册 Windows 计划任务 - 开机自动启动烤房项目前后端服务
.DESCRIPTION
    后端使用守护脚本（异常退出自动重启）
    前端使用 Vite 开发服务器
    计划任务名: KaofBackendGuard / KaofFrontend
#>

$PROJECT_DIR = $PSScriptRoot

# ===== 1. 创建后端守护脚本（确保内容正确）=====
$guardScript = @'
$PROJECT_DIR = $PSScriptRoot
$JAR_PATH = "$PROJECT_DIR\ruoyi-barn\target\ruoyi-barn.jar"
$JAVA_EXE = (Get-Command java -ErrorAction Stop).Source
$BACKEND_PORT = 8082
$MAX_RESTART = 100
$RESTART_DELAY = 5

$restartCount = 0

while ($true) {
    $time = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    
    # 检查端口是否已被占用
    $portInUse = $false
    try {
        $conn = Get-NetTCPConnection -LocalPort $BACKEND_PORT -State Listen -ErrorAction SilentlyContinue
        if ($conn) { $portInUse = $true }
    } catch {}

    if (-not $portInUse) {
        if ($restartCount -lt $MAX_RESTART) {
            $restartCount++
            Write-Host "[$time] Starting backend (attempt $restartCount)..."
            $proc = Start-Process -FilePath $JAVA_EXE `
                -ArgumentList "-Xmx512m", "-jar", "`"$JAR_PATH`"" `
                -WorkingDirectory $PROJECT_DIR `
                -PassThru `
                -WindowStyle Normal

            # 等待端口就绪
            $ready = $false
            for ($i = 1; $i -le 15; $i++) {
                Start-Sleep -Seconds 2
                try {
                    $conn = Get-NetTCPConnection -LocalPort $BACKEND_PORT -State Listen -ErrorAction SilentlyContinue
                    if ($conn) { $ready = $true; break }
                } catch {}
            }

            if ($ready) {
                Write-Host "[$time] Backend is ready"
                $restartCount = 0
            } else {
                Write-Host "[$time] Backend failed to start"
            }

            # 等待进程退出
            if ($proc) { $proc.WaitForExit() }
            $time = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
            Write-Host "[$time] Backend exited, restarting in $RESTART_DELAY s..."
            Start-Sleep -Seconds $RESTART_DELAY
        }
    } else {
        Start-Sleep -Seconds 10
    }
}
'@
$guardPath = "$PROJECT_DIR\backend-guard.ps1"
Set-Content -Path $guardPath -Value $guardScript -Encoding UTF8
Write-Host "[OK] Guard script created: $guardPath"

# ===== 2. 创建前端启动脚本 =====
$frontendScript = @'
Set-Location $PSScriptRoot
if (-not (Test-Path "node_modules\vite")) {
    npm install --legacy-peer-deps --include=dev
}
while ($true) {
    npx vite --port 5000 --host
    Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] Frontend exited, restarting in 3s..."
    Start-Sleep -Seconds 3
}
'@
$frontendPath = "$PROJECT_DIR\frontend-guard.ps1"
Set-Content -Path $frontendPath -Value $frontendScript -Encoding UTF8
Write-Host "[OK] Frontend script created: $frontendPath"

# ===== 3. 注册计划任务 =====
$taskUser = "$env:USERDOMAIN\$env:USERNAME"

# 后端任务
$backendAction = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument "-ExecutionPolicy Bypass -WindowStyle Normal -File `"$guardPath`""
$backendTrigger = New-ScheduledTaskTrigger -AtStartup
$backendSettings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit (New-TimeSpan -Days 365) -RestartCount 5 -RestartInterval (New-TimeSpan -Minutes 1)

Register-ScheduledTask -TaskName "KaofBackendGuard" `
    -Action $backendAction -Trigger $backendTrigger -Settings $backendSettings `
    -User $taskUser -RunLevel Highest -Force | Out-Null
Write-Host "[OK] Backend task registered: KaofBackendGuard"

# 前端任务
$frontendAction = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument "-ExecutionPolicy Bypass -WindowStyle Normal -File `"$frontendPath`""
$frontendTrigger = New-ScheduledTaskTrigger -AtStartup
$frontendSettings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit (New-TimeSpan -Days 365) -RestartCount 5 -RestartInterval (New-TimeSpan -Minutes 1)

Register-ScheduledTask -TaskName "KaofFrontend" `
    -Action $frontendAction -Trigger $frontendTrigger -Settings $frontendSettings `
    -User $taskUser -RunLevel Highest -Force | Out-Null
Write-Host "[OK] Frontend task registered: KaofFrontend"

# ===== 4. 立即启动 =====
Start-ScheduledTask -TaskName "KaofBackendGuard"
Write-Host "[OK] Backend task started"
Start-ScheduledTask -TaskName "KaofFrontend"
Write-Host "[OK] Frontend task started"

Write-Host "`n========== Done =========="
Write-Host "Backend:  http://localhost:8082"
Write-Host "Frontend: http://localhost:5000"
Write-Host "Tasks will auto-start on boot."
