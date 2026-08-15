# 升级包（Windows / Linux）

不用每次手动 `mvn package`、拷 jar、拷前端 dist 到服务器——用本目录脚本一键打出升级包，服务器上执行 `apply` 即可。

## 一键打包

| 系统 | 命令 |
|------|------|
| Windows | `.\deploy\upgrade\pack-upgrade.ps1` |
| Linux/macOS | `./deploy/upgrade/pack-upgrade.sh` |

常用参数：

```text
-BackendOnly / --backend-only     只打后端
-FrontendOnly / --frontend-only   只打前端
-SkipUpload / --skip-upload       跳过 SCP
-Config / --config path           指定配置文件
```

产物：`dist/upgrades/hr-upgrade-时间戳/` 与同名 `.zip`（Linux 无 zip 时为 `.tar.gz`）。

包内结构：

```text
hr-upgrade-xxxx/
  backend/hr-management.jar
  frontend/                 # 前端 dist
  bin/apply.sh | apply.ps1
  bin/rollback.sh | rollback.ps1
  MANIFEST.txt
  README.txt
```

## IDEA 插件

见仓库 [`idea-plugin/README.md`](../../idea-plugin/README.md)：安装后 **Tools → HR Upgrade → 生成升级包**。

## 配置文件

复制示例到仓库根目录：

```bash
cp deploy/upgrade/hr-upgrade.example.json hr-upgrade.json
```

`upload.enabled=true` 时可在打包后自动 `scp` 到服务器（本机需有 `scp`）。

## 服务器应用

```bash
HR_HOME=/opt/hr HR_USE_DOCKER=1 ./bin/apply.sh
# 或
HR_HOME=/opt/hr HR_SERVICE=hr-management ./bin/apply.sh
```

会自动备份旧 jar / 前端到 `$HR_HOME/backup/时间戳`。
