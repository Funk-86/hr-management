# HR Upgrade Packer（IDEA 插件）

在 IntelliJ IDEA / Android Studio 中一键生成智汇人事系统**跨平台升级包**（Windows / Linux 均可打、均可装）。

## 功能

- 菜单：**Tools → HR Upgrade → 生成升级包**（快捷键 `Ctrl+Alt+Shift+U`）
- 可选：只打后端 / 只打前端 / 两者都打
- 调用仓库脚本 `deploy/upgrade/pack-upgrade.ps1`（Windows）或 `pack-upgrade.sh`（Linux/macOS）
- 产出目录：`dist/upgrades/hr-upgrade-时间戳/` + `.zip`
- 包内自带 `bin/apply.sh`、`bin/apply.ps1` 与回滚脚本
- 可选：在 `hr-upgrade.json` 配置 SCP 自动上传到服务器

## 构建并安装插件

### 前置

- JDK 17+
- 已安装 Gradle 8.10+（或使用下方 wrapper）

### 构建

```bash
cd idea-plugin
gradle wrapper   # 若还没有 gradlew
./gradlew buildPlugin
# Windows: .\gradlew.bat buildPlugin
```

产物：

```
idea-plugin/build/distributions/hr-upgrade-idea-plugin-1.0.0.zip
```

### 安装到 IDEA

1. `Settings → Plugins → ⚙️ → Install Plugin from Disk…`
2. 选择上面的 zip
3. 重启 IDEA
4. 用 **Open** 打开 `hr-management` 仓库根目录（不要只打开 `idea-plugin` 子目录，否则找不到打包脚本；若只打开子目录，插件会尝试向上一级查找）

## 项目配置（可选）

在仓库根目录复制：

```bash
copy deploy\upgrade\hr-upgrade.example.json hr-upgrade.json   # Windows
# cp deploy/upgrade/hr-upgrade.example.json hr-upgrade.json  # Linux
```

关键字段：

| 字段 | 说明 |
|------|------|
| `frontendPath` | 前端 monorepo 路径 |
| `includeBackend` / `includeFrontend` | 默认是否打进包 |
| `upload.enabled` | 为 true 时打包后 `scp` 上传 |
| `upload.host/user/remoteDir` | 服务器信息 |
| `upload.privateKeyPath` | 私钥路径（可选） |

上传需要本机已安装 OpenSSH 的 `scp` 命令。

## 不用插件时（命令行同样可用）

Windows：

```powershell
.\deploy\upgrade\pack-upgrade.ps1
.\deploy\upgrade\pack-upgrade.ps1 -BackendOnly
```

Linux / macOS：

```bash
chmod +x deploy/upgrade/pack-upgrade.sh
./deploy/upgrade/pack-upgrade.sh
./deploy/upgrade/pack-upgrade.sh --backend-only
```

## 服务器上应用升级包

```bash
# Linux 示例
unzip hr-upgrade-xxxx.zip -d /opt/hr/upgrades/
cd /opt/hr/upgrades/hr-upgrade-xxxx
chmod +x bin/*.sh
HR_HOME=/opt/hr HR_USE_DOCKER=1 ./bin/apply.sh
# 或 systemd:
# HR_HOME=/opt/hr HR_SERVICE=hr-management ./bin/apply.sh
```

```powershell
# Windows 示例
Expand-Archive hr-upgrade-xxxx.zip -DestinationPath C:\hr\upgrades\
cd C:\hr\upgrades\hr-upgrade-xxxx
$env:HR_HOME='C:\hr'
.\bin\apply.ps1
```
