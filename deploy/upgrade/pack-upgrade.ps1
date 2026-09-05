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
    [switch]$SkipRemoteApply,
    # 由 IDEA 插件显式传入：true / false（优先于配置文件）
    [string]$IncludeBackend = "",
    [string]$IncludeFrontend = "",
    # 插件：是否在 SCP 后 SSH 远程 apply（空=跟配置；true/false 覆盖）
    [string]$RemoteApply = ""
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
# 保证 shell 脚本是 LF，避免服务器 bash 报 set: pipefail / 路径带 \r
$utf8NoBomLocal = New-Object System.Text.UTF8Encoding $false
foreach ($shName in @("apply.sh", "rollback.sh")) {
    $shPath = Join-Path $PkgDir ("bin\" + $shName)
    if (Test-Path $shPath) {
        $raw = [System.IO.File]::ReadAllText($shPath) -replace "`r`n", "`n" -replace "`r", "`n"
        [System.IO.File]::WriteAllText($shPath, $raw, $utf8NoBomLocal)
    }
}

# Docker 部署配套：升级时可覆盖服务器 Dockerfile（支持 prebuilt jar）
New-Item -ItemType Directory -Force -Path (Join-Path $PkgDir "deploy") | Out-Null
$Df = Join-Path $RepoRoot "deploy\Dockerfile.backend"
if (Test-Path $Df) {
    Copy-Item $Df (Join-Path $PkgDir "deploy\Dockerfile.backend") -Force
}

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

# Compress-Archive 在 Windows 上常因文件被占用失败；改用系统 tar（Win10+ 自带）
function Compress-UpgradeZip([string]$SourceDir, [string]$DestZip) {
    $parent = Split-Path $SourceDir -Parent
    $name = Split-Path $SourceDir -Leaf
    if (Get-Command tar.exe -ErrorAction SilentlyContinue) {
        # -a 按扩展名选 zip；-C 进入父目录再压文件夹，避免路径过长/占用问题
        $tarArgs = @("-a", "-cf", $DestZip, "-C", $parent, $name)
        & tar.exe @tarArgs
        if ($LASTEXITCODE -ne 0) { throw "tar zip failed, exit=$LASTEXITCODE" }
        return
    }
    # 回退：.NET ZipFile（比 Compress-Archive 更稳）
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    Start-Sleep -Seconds 2
    [System.IO.Compression.ZipFile]::CreateFromDirectory($SourceDir, $DestZip)
}

Write-Host ">>> zip package" -ForegroundColor Cyan
Start-Sleep -Milliseconds 800
$zipOk = $false
for ($i = 1; $i -le 3; $i++) {
    try {
        if (Test-Path -LiteralPath $ZipPath) { Remove-Item -LiteralPath $ZipPath -Force }
        Compress-UpgradeZip -SourceDir $PkgDir -DestZip $ZipPath
        $zipOk = $true
        break
    } catch {
        Write-Host ("zip attempt $i failed: " + $_.Exception.Message) -ForegroundColor Yellow
        Start-Sleep -Seconds 2
    }
}
if (-not $zipOk) {
    throw "zip failed after retries (file locked?). Close Explorer preview / antivirus scan on dist\upgrades and retry."
}

Write-Host ""
Write-Host "Upgrade package ready" -ForegroundColor Green
Write-Host ("  dir: " + $PkgDir)
Write-Host ("  zip: " + $ZipPath)

# optional scp upload + SSH remote apply
$Upload = if ($Cfg -ne $null) { $Cfg.upload } else { $null }
if (-not $SkipUpload -and $Upload -and $Upload.enabled) {
    $HostName = [string]$Upload.host
    $Port = if ($Upload.port) { [int]$Upload.port } else { 22 }
    $User = [string]$Upload.user
    $RemoteDir = [string]$Upload.remoteDir
    $Key = [string]$Upload.privateKeyPath
    $HrHome = if ($Upload.hrHome) { [string]$Upload.hrHome } else { "/opt/hr-management/deploy" }
    $DoRemoteApply = $false
    if ($RemoteApply -eq "true") { $DoRemoteApply = $true }
    elseif ($RemoteApply -eq "false") { $DoRemoteApply = $false }
    elseif ($Upload.applyAfterUpload) { $DoRemoteApply = [bool]$Upload.applyAfterUpload }
    if ($SkipRemoteApply) { $DoRemoteApply = $false }

    if (-not $HostName -or -not $User -or -not $RemoteDir) {
        Write-Host "upload config incomplete, skip" -ForegroundColor Yellow
    } else {
        Write-Host (">>> mkdir remote " + $RemoteDir) -ForegroundColor Cyan
        # BatchMode：无终端时不卡在口令/确认；超时避免 IDEA 后台任务一直转圈
        $SshBase = @(
            "-p", "$Port",
            "-o", "BatchMode=yes",
            "-o", "ConnectTimeout=15",
            "-o", "ServerAliveInterval=5",
            "-o", "ServerAliveCountMax=3",
            "-o", "StrictHostKeyChecking=accept-new"
        )
        if ($Key) { $SshBase = @("-i", $Key) + $SshBase }
        & ssh @SshBase ($User + "@" + $HostName) ("mkdir -p " + $RemoteDir)
        if ($LASTEXITCODE -ne 0) {
            throw "ssh mkdir failed (exit=$LASTEXITCODE). Check: key passphrase? security group 22? ssh root@$HostName"
        }

        Write-Host (">>> scp to " + $User + "@" + $HostName + ":" + $RemoteDir) -ForegroundColor Cyan
        $ScpArgs = @("-P", "$Port", $ZipPath, ($User + "@" + $HostName + ":" + $RemoteDir + "/"))
        if ($Key) { $ScpArgs = @("-i", $Key) + $ScpArgs }
        & scp @ScpArgs
        if ($LASTEXITCODE -ne 0) { throw "scp failed (install OpenSSH client)" }
        Write-Host "upload done" -ForegroundColor Green

        if ($DoRemoteApply) {
            $ZipName = Split-Path $ZipPath -Leaf
            Write-Host (">>> SSH remote apply HR_HOME=" + $HrHome) -ForegroundColor Cyan
            # 必须用 LF：PowerShell here-string 默认 CRLF，会导致 bash 报 set: - 与路径带 \r
            $RemoteLines = @(
                "set -euo pipefail",
                "cd '$RemoteDir'",
                "unzip -o '$ZipName'",
                "cd '$PkgName'",
                "sed -i 's/\r`$//' bin/*.sh 2>/dev/null || true",
                "chmod +x bin/*.sh",
                "HR_HOME='$HrHome' HR_USE_DOCKER=1 ./bin/apply.sh"
            )
            $RemoteCmd = ($RemoteLines -join "`n") + "`n"
            $RemoteCmd = $RemoteCmd -replace "`r", ""

            $TmpSh = Join-Path $env:TEMP ("hr-remote-apply-" + [guid]::NewGuid().ToString("N") + ".sh")
            $utf8NoBom = New-Object System.Text.UTF8Encoding $false
            [System.IO.File]::WriteAllText($TmpSh, $RemoteCmd, $utf8NoBom)
            try {
                $RemoteSh = ($RemoteDir.TrimEnd("/") + "/_hr_remote_apply.sh")
                $ScpSh = @("-P", "$Port", $TmpSh, ($User + "@" + $HostName + ":" + $RemoteSh))
                if ($Key) { $ScpSh = @("-i", $Key) + $ScpSh }
                & scp @ScpSh
                if ($LASTEXITCODE -ne 0) { throw "scp remote apply script failed" }
                & ssh @SshBase ($User + "@" + $HostName) ("bash '" + $RemoteSh + "'")
                if ($LASTEXITCODE -ne 0) { throw "remote apply failed" }
                Write-Host "remote apply done" -ForegroundColor Green
            } finally {
                Remove-Item -LiteralPath $TmpSh -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

Write-Host ""
if (-not $SkipUpload -and $Upload -and $Upload.enabled -and (
        ($RemoteApply -eq "true") -or ($Upload.applyAfterUpload -and $RemoteApply -ne "false")
    ) -and -not $SkipRemoteApply) {
    Write-Host "Done: package uploaded and applied on server." -ForegroundColor Cyan
} else {
    Write-Host "Next: unzip on server and run: HR_HOME=/opt/hr-management/deploy HR_USE_DOCKER=1 ./bin/apply.sh" -ForegroundColor Cyan
}
