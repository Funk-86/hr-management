# HR 系统 Docker 一键启动（Windows PowerShell）
# 用法: cd deploy && .\start.ps1

$ErrorActionPreference = "Stop"
$DeployDir = $PSScriptRoot
Set-Location $DeployDir

# 加载 .env
$EnvFile = Join-Path $DeployDir ".env"
if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim())
        }
    }
}

$FrontendPath = $env:FRONTEND_PATH
if (-not $FrontendPath) {
    $FrontendPath = "D:\vue\vue-vben-admin-main"
}

if (-not (Test-Path $FrontendPath)) {
    Write-Host "未找到前端目录: $FrontendPath" -ForegroundColor Red
    Write-Host "请复制 .env.example 为 .env 并设置 FRONTEND_PATH" -ForegroundColor Yellow
    exit 1
}

Write-Host ">>> 构建前端: $FrontendPath" -ForegroundColor Cyan
Push-Location $FrontendPath
pnpm install
pnpm build:antd
Pop-Location

$DistSrc = Join-Path $FrontendPath "apps\web-antd\dist"
$DistDst = Join-Path $DeployDir "frontend-dist"
if (-not (Test-Path $DistSrc)) {
    Write-Host "前端构建失败，未找到 dist 目录" -ForegroundColor Red
    exit 1
}

Write-Host ">>> 复制前端产物到 deploy/frontend-dist" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $DistDst | Out-Null
Copy-Item -Path "$DistSrc\*" -Destination $DistDst -Recurse -Force

if (-not (Test-Path (Join-Path $DeployDir ".env"))) {
    Copy-Item (Join-Path $DeployDir ".env.example") (Join-Path $DeployDir ".env")
    Write-Host "已生成 .env，可按需修改后重新运行" -ForegroundColor Yellow
}

Write-Host ">>> 启动 Docker Compose" -ForegroundColor Cyan
docker compose up --build -d

Write-Host ""
Write-Host "启动完成！" -ForegroundColor Green
Write-Host "  访问地址: http://localhost" -ForegroundColor Green
Write-Host "  测试账号: employee / Emp@2024" -ForegroundColor Green
Write-Host "  查看日志: docker compose logs -f" -ForegroundColor Green
