# HR Upgrade Packer（IDEA 插件）

在 IntelliJ IDEA 中一键：**本机打包 → SCP 上传 → SSH 在服务器 Docker 更新**。

## 功能

- 菜单：**Tools → HR Upgrade → 生成升级包 / 远程更新**（`Ctrl+Alt+Shift+U`）
- 本机：Maven 打 jar、`pnpm build:antd` 打前端
- 可选：SCP 上传到服务器
- 可选：**SSH 直连解压并 `apply`**（覆盖前端 + 用预编译 jar 重建 backend 镜像）

## 远程更新前置

1. 本机 SSH 免密登录服务器（`ssh root@你的IP` 无需密码）
2. 仓库根目录 `hr-upgrade.json`（可从 `deploy/upgrade/hr-upgrade.example.json` 复制）示例：

```json
{
  "frontendPath": "D:/vue/vue-vben-admin-main",
  "upload": {
    "enabled": true,
    "host": "39.105.67.125",
    "port": 22,
    "user": "root",
    "remoteDir": "/opt/hr-management/upgrades",
    "privateKeyPath": "C:/Users/你/.ssh/id_rsa",
    "hrHome": "/opt/hr-management/deploy",
    "applyAfterUpload": true
  }
}
```

3. 服务器已安装 `unzip`、Docker Compose（你当前线上环境已具备）

## 使用

1. IDEA 打开 **hr-management 仓库根目录**
2. **Tools → HR Upgrade → 生成升级包 / 远程更新**
3. 勾选：
   - 包含后端 / 前端
   - **上传后 SSH 直连服务器自动更新**（默认勾）
4. 等待进度条结束

服务器上会发生：

1. 解压升级包  
2. 覆盖 `deploy/frontend-dist`  
3. 将 jar 写入 `deploy/prebuilt/app.jar`  
4. `docker compose up -d --build backend nginx`（Dockerfile 检测到 prebuilt 则跳过 Maven）  
5. 删除 `prebuilt/app.jar`，避免下次误用  

## 构建插件

```bash
cd idea-plugin
./gradlew.bat buildPlugin
```

产物：`idea-plugin/build/distributions/hr-upgrade-idea-plugin-1.0.3.zip`  
Settings → Plugins → Install Plugin from Disk → 选该 zip → 重启 IDEA。

## 命令行（不用插件）

```powershell
.\deploy\upgrade\pack-upgrade.ps1 -RemoteApply true
```

```bash
./deploy/upgrade/pack-upgrade.sh --remote-apply true
```

仅打包不上传：加 `-SkipUpload` / `--skip-upload`。
