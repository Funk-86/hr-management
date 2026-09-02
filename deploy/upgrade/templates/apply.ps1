# 在 Windows 服务器解压升级包后执行：.\bin\apply.ps1
# 环境变量（可选）：
#   HR_HOME     应用根目录
#   HR_SERVICE  Windows 服务名；为空则尝试 docker compose

$ErrorActionPreference = "Stop"
$PkgDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$HrHome = if ($env:HR_HOME) { $env:HR_HOME } else { (Resolve-Path (Join-Path $PkgDir "..")).Path }
$Ts = Get-Date -Format "yyyyMMdd-HHmmss"
$BackupDir = Join-Path $HrHome "backup\$Ts"

Write-Host "=== HR 升级包应用 ===" -ForegroundColor Cyan
Write-Host "包目录: $PkgDir"
Write-Host "应用目录: $HrHome"
Write-Host "备份目录: $BackupDir"
New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null

function Stop-App {
    if ($env:HR_USE_DOCKER -eq "1" -or ((Test-Path (Join-Path $HrHome "docker-compose.yml")) -and -not $env:HR_SERVICE)) {
        if (Test-Path (Join-Path $HrHome "docker-compose.yml")) {
            Write-Host ">>> 停止 docker compose"
            Push-Location $HrHome
            docker compose stop backend nginx 2>$null
            if ($LASTEXITCODE -ne 0) { docker compose stop }
            Pop-Location
            return
        }
    }
    if ($env:HR_SERVICE) {
        Write-Host ">>> Stop-Service $($env:HR_SERVICE)"
        Stop-Service -Name $env:HR_SERVICE -Force -ErrorAction SilentlyContinue
        return
    }
    Write-Host ">>> 未配置 HR_SERVICE，跳过停止（请自行停服）" -ForegroundColor Yellow
}

function Start-App {
    if ($env:HR_USE_DOCKER -eq "1" -or ((Test-Path (Join-Path $HrHome "docker-compose.yml")) -and -not $env:HR_SERVICE)) {
        if (Test-Path (Join-Path $HrHome "docker-compose.yml")) {
            Write-Host ">>> 启动 docker compose"
            Push-Location $HrHome
            docker compose up -d
            Pop-Location
            return
        }
    }
    if ($env:HR_SERVICE) {
        Write-Host ">>> Start-Service $($env:HR_SERVICE)"
        Start-Service -Name $env:HR_SERVICE
        return
    }
    Write-Host ">>> 未配置启动方式，请手动启动后端" -ForegroundColor Yellow
}

Stop-App

$JarSrc = Join-Path $PkgDir "backend\hr-management.jar"
if (Test-Path $JarSrc) {
    $BackendDir = Join-Path $HrHome "backend"
    New-Item -ItemType Directory -Force -Path $BackendDir | Out-Null
    $Old1 = Join-Path $BackendDir "hr-management.jar"
    $Old2 = Join-Path $HrHome "hr-management.jar"
    if (Test-Path $Old1) { Copy-Item $Old1 (Join-Path $BackupDir "hr-management.jar") -Force }
    elseif (Test-Path $Old2) { Copy-Item $Old2 (Join-Path $BackupDir "hr-management.jar") -Force }
    Write-Host ">>> 替换后端 jar"
    Copy-Item $JarSrc $Old1 -Force
    Copy-Item $JarSrc $Old2 -Force -ErrorAction SilentlyContinue
}

$FeSrc = Join-Path $PkgDir "frontend"
if ((Test-Path $FeSrc) -and (Get-ChildItem $FeSrc -ErrorAction SilentlyContinue | Measure-Object).Count -gt 0) {
    $Dest = $null
    foreach ($c in @("frontend-dist", "deploy\frontend-dist", "html")) {
        $p = Join-Path $HrHome $c
        if (Test-Path $p) { $Dest = $p; break }
    }
    if (-not $Dest) {
        $Dest = Join-Path $HrHome "frontend-dist"
        New-Item -ItemType Directory -Force -Path $Dest | Out-Null
    }
    if ((Test-Path $Dest) -and (Get-ChildItem $Dest -ErrorAction SilentlyContinue | Measure-Object).Count -gt 0) {
        $FeBak = Join-Path $BackupDir "frontend"
        New-Item -ItemType Directory -Force -Path $FeBak | Out-Null
        Copy-Item "$Dest\*" $FeBak -Recurse -Force -ErrorAction SilentlyContinue
    }
    Write-Host ">>> 替换前端静态文件 -> $Dest"
    Get-ChildItem $Dest -Force | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
    Copy-Item "$FeSrc\*" $Dest -Recurse -Force
}

$Manifest = Join-Path $PkgDir "MANIFEST.txt"
if (Test-Path $Manifest) {
    Copy-Item $Manifest (Join-Path $HrHome "LAST_UPGRADE.txt") -Force
}

Start-App

Write-Host ""
Write-Host "升级完成。备份: $BackupDir" -ForegroundColor Green
Write-Host "回滚: .\bin\rollback.ps1 -BackupDir `"$BackupDir`"" -ForegroundColor Green
