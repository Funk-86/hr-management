# 一键生成 HR 升级包（Windows）
# 用法（在仓库根或本目录）:
#   .\deploy\upgrade\pack-upgrade.ps1
#   .\deploy\upgrade\pack-upgrade.ps1 -BackendOnly
#   .\deploy\upgrade\pack-upgrade.ps1 -FrontendOnly
#   .\deploy\upgrade\pack-upgrade.ps1 -Config .\hr-upgrade.json

param(
    [string]$Config = "",
    [switch]$BackendOnly,
    [switch]$FrontendOnly,
    [switch]$SkipUpload,
    # 由 IDEA 插件显式传入：true / false（优先于配置文件）
    [string]$IncludeBackend = "",
    [string]$IncludeFrontend = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = $PSScriptRoot
$RepoRoot = Resolve-Path (Join-Path $ScriptDir "..\..")
Set-Location $RepoRoot

function Read-JsonFile([string]$Path) {
    if (-not (Test-Path $Path)) { return $null }
    return (Get-Content $Path -Raw -Encoding UTF8 | ConvertFrom-Json)
}

$CfgPath = $Config
if (-not $CfgPath) {
    foreach ($c in @(".\hr-upgrade.json", ".\deploy\upgrade\hr-upgrade.json")) {
        if (Test-Path $c) { $CfgPath = $c; break }
    }
}
$Cfg = if ($CfgPath) { Read-JsonFile $CfgPath } else { $null }

$DoBackend = $true
$DoFrontend = $true
if ($Cfg -ne $null) {
    if ($null -ne $Cfg.includeBackend) { $DoBackend = [bool]$Cfg.includeBackend }
    if ($null -ne $Cfg.includeFrontend) { $DoFrontend = [bool]$Cfg.includeFrontend }
}
if ($IncludeBackend -ne "") { $DoBackend = @('1', 'true', 'yes') -contains $IncludeBackend.ToLowerInvariant() }
if ($IncludeFrontend -ne "") { $DoFrontend = @('1', 'true', 'yes') -contains $IncludeFrontend.ToLowerInvariant() }
if ($BackendOnly) { $DoFrontend = $false; $DoBackend = $true }
if ($FrontendOnly) { $DoBackend = $false; $DoFrontend = $true }

$FrontendPath = $env:FRONTEND_PATH
if ($Cfg -ne $null -and $Cfg.frontendPath) { $FrontendPath = [string]$Cfg.frontendPath }
if (-not $FrontendPath) { $FrontendPath = "D:\vue\vue-vben-admin-main" }
$SkipTests = $true
$SkipFeInstall = $false
$OutputDir = Join-Path $RepoRoot "dist\upgrades"
$JarName = "hr-management.jar"
if ($Cfg -ne $null) {
    if ($null -ne $Cfg.skipTests) { $SkipTests = [bool]$Cfg.skipTests }
    if ($null -ne $Cfg.skipFrontendInstall) { $SkipFeInstall = [bool]$Cfg.skipFrontendInstall }
    if ($Cfg.outputDir) { $OutputDir = Join-Path $RepoRoot ([string]$Cfg.outputDir) }
    if ($Cfg.jarName) { $JarName = [string]$Cfg.jarName }
}

$Stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$PkgName = "hr-upgrade-$Stamp"
$PkgDir = Join-Path $OutputDir $PkgName
New-Item -ItemType Directory -Force -Path (Join-Path $PkgDir "backend") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $PkgDir "frontend") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $PkgDir "bin") | Out-Null

Write-Host "=== pack $PkgName ===" -ForegroundColor Cyan
Write-Host ("repo: " + $RepoRoot)
Write-Host ("backend=" + $DoBackend + " frontend=" + $DoFrontend)

if ($DoBackend) {
    Write-Host ">>> Maven package" -ForegroundColor Cyan
    $mvnArgs = @("-DskipTests=$SkipTests", "package")
    & mvn @mvnArgs
    if ($LASTEXITCODE -ne 0) { throw "Maven package failed" }
    $Jar = Get-ChildItem (Join-Path $RepoRoot "target\*.jar") |
        Where-Object { $_.Name -notmatch '\.original$' -and $_.Name -notmatch 'sources|javadoc' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $Jar) { throw "jar not found under target/" }
    Copy-Item $Jar.FullName (Join-Path $PkgDir "backend\$JarName") -Force
    Write-Host ("backend jar: " + $Jar.Name)
}

if ($DoFrontend) {
    if (-not (Test-Path $FrontendPath)) {
        throw ("frontend path not found: " + $FrontendPath + " (set frontendPath in hr-upgrade.json)")
    }
    Write-Host (">>> build frontend: " + $FrontendPath) -ForegroundColor Cyan
    Push-Location $FrontendPath
    if (-not $SkipFeInstall) { pnpm install }
    pnpm build:antd
    if ($LASTEXITCODE -ne 0) { Pop-Location; throw "frontend build failed" }
    Pop-Location
    $Dist = Join-Path $FrontendPath "apps\web-antd\dist"
    if (-not (Test-Path $Dist)) { throw ("frontend dist not found: " + $Dist) }
    Copy-Item "$Dist\*" (Join-Path $PkgDir "frontend") -Recurse -Force
    Write-Host "frontend dist copied"
}

$Tpl = Join-Path $ScriptDir "templates"
Copy-Item (Join-Path $Tpl "apply.sh") (Join-Path $PkgDir "bin\apply.sh") -Force
Copy-Item (Join-Path $Tpl "apply.ps1") (Join-Path $PkgDir "bin\apply.ps1") -Force
Copy-Item (Join-Path $Tpl "rollback.sh") (Join-Path $PkgDir "bin\rollback.sh") -Force
Copy-Item (Join-Path $Tpl "rollback.ps1") (Join-Path $PkgDir "bin\rollback.ps1") -Force
Copy-Item (Join-Path $Tpl "README.txt") (Join-Path $PkgDir "README.txt") -Force

$ManifestLines = @(
    "name=$PkgName",
    ("created=" + (Get-Date -Format "yyyy-MM-dd HH:mm:ss")),
    ("includeBackend=" + $DoBackend),
    ("includeFrontend=" + $DoFrontend),
    ("repo=" + $RepoRoot),
    ("host=" + $env:COMPUTERNAME)
)
$ManifestPath = Join-Path $PkgDir "MANIFEST.txt"
$ManifestLines | Set-Content -LiteralPath $ManifestPath -Encoding UTF8

$ZipPath = Join-Path $OutputDir ($PkgName + ".zip")
if (Test-Path -LiteralPath $ZipPath) {
    Remove-Item -LiteralPath $ZipPath -Force
}
Compress-Archive -LiteralPath $PkgDir -DestinationPath $ZipPath -Force

Write-Host ""
Write-Host "Upgrade package ready" -ForegroundColor Green
Write-Host ("  dir: " + $PkgDir)
Write-Host ("  zip: " + $ZipPath)

# optional scp upload
$Upload = if ($Cfg -ne $null) { $Cfg.upload } else { $null }
if (-not $SkipUpload -and $Upload -and $Upload.enabled) {
    $HostName = [string]$Upload.host
    $Port = if ($Upload.port) { [int]$Upload.port } else { 22 }
    $User = [string]$Upload.user
    $RemoteDir = [string]$Upload.remoteDir
    $Key = [string]$Upload.privateKeyPath
    if (-not $HostName -or -not $User -or -not $RemoteDir) {
        Write-Host "upload config incomplete, skip" -ForegroundColor Yellow
    } else {
        Write-Host (">>> scp to " + $User + "@" + $HostName + ":" + $RemoteDir) -ForegroundColor Cyan
        $ScpArgs = @("-P", "$Port", $ZipPath, ($User + "@" + $HostName + ":" + $RemoteDir + "/"))
        if ($Key) { $ScpArgs = @("-i", $Key) + $ScpArgs }
        & scp @ScpArgs
        if ($LASTEXITCODE -ne 0) { throw "scp failed (install OpenSSH client)" }
        Write-Host "upload done" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Next: unzip on server and run bin/apply.sh or bin/apply.ps1" -ForegroundColor Cyan
