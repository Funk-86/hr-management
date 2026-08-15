param(
    [Parameter(Mandatory = $true)]
    [string]$BackupDir
)
$ErrorActionPreference = "Stop"
$PkgDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$HrHome = if ($env:HR_HOME) { $env:HR_HOME } else { Resolve-Path (Join-Path $PkgDir "..") }

if (-not (Test-Path $BackupDir)) {
    Write-Host "备份目录不存在: $BackupDir" -ForegroundColor Red
    exit 1
}

Write-Host "=== 回滚到 $BackupDir ===" -ForegroundColor Cyan
$Jar = Join-Path $BackupDir "hr-management.jar"
if (Test-Path $Jar) {
    New-Item -ItemType Directory -Force -Path (Join-Path $HrHome "backend") | Out-Null
    Copy-Item $Jar (Join-Path $HrHome "backend\hr-management.jar") -Force
    Copy-Item $Jar (Join-Path $HrHome "hr-management.jar") -Force -ErrorAction SilentlyContinue
    Write-Host "已恢复 jar"
}
$Fe = Join-Path $BackupDir "frontend"
if (Test-Path $Fe) {
    $Dest = Join-Path $HrHome "frontend-dist"
    if (Test-Path (Join-Path $HrHome "deploy\frontend-dist")) {
        $Dest = Join-Path $HrHome "deploy\frontend-dist"
    }
    New-Item -ItemType Directory -Force -Path $Dest | Out-Null
    Get-ChildItem $Dest -Force | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
    Copy-Item "$Fe\*" $Dest -Recurse -Force
    Write-Host "已恢复前端 -> $Dest"
}
Write-Host "请手动重启服务" -ForegroundColor Yellow
