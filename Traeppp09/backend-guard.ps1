<#
.SYNOPSIS
    后端服务守护脚本 - 后台运行，异常退出后自动重启
.DESCRIPTION
    监控后端 Java 进程，如果进程退出则自动重新启动
    所有进程在后台运行，不生成任何CMD窗口
#>

$PROJECT_DIR = $PSScriptRoot
$JAR_PATH = "$PROJECT_DIR\ruoyi-barn\target\ruoyi-barn.jar"
$JAVA_EXE = (Get-Command java -ErrorAction Stop).Source
$BACKEND_PORT = if ($env:BACKEND_PORT) { [int]$env:BACKEND_PORT } else { 8082 }
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

            # 使用 WindowStyle Hidden 后台启动，不弹窗口
            $proc = Start-Process -FilePath $JAVA_EXE `
                -ArgumentList "-Xmx512m", "-jar", "`"$JAR_PATH`"" `
                -WorkingDirectory $PROJECT_DIR `
                -PassThru `
                -WindowStyle Hidden

            Write-Host "[$time] Backend PID: $($proc.Id)"

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
                $time = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
                Write-Host "[$time] Backend is ready at http://localhost:$BACKEND_PORT"
                $restartCount = 0
            } else {
                $time = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
                Write-Host "[$time] Backend failed to start within 30s"
            }

            # 等待进程退出（阻塞）
            if ($proc) { $proc.WaitForExit() }
            $time = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
            Write-Host "[$time] Backend exited, restarting in $RESTART_DELAY s..."
            Start-Sleep -Seconds $RESTART_DELAY
        }
    } else {
        Start-Sleep -Seconds 10
    }
}
