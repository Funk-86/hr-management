#!/usr/bin/env bash
# 一键生成 HR 升级包（Linux / macOS）
# 用法:
#   ./deploy/upgrade/pack-upgrade.sh
#   ./deploy/upgrade/pack-upgrade.sh --backend-only
#   ./deploy/upgrade/pack-upgrade.sh --frontend-only
#   ./deploy/upgrade/pack-upgrade.sh --config ./hr-upgrade.json

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

CONFIG=""
BACKEND_ONLY=0
FRONTEND_ONLY=0
SKIP_UPLOAD=0
SKIP_REMOTE_APPLY=0
FORCE_BACKEND=""
FORCE_FRONTEND=""
FORCE_REMOTE_APPLY=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --config) CONFIG="$2"; shift 2 ;;
    --backend-only) BACKEND_ONLY=1; shift ;;
    --frontend-only) FRONTEND_ONLY=1; shift ;;
    --skip-upload) SKIP_UPLOAD=1; shift ;;
    --skip-remote-apply) SKIP_REMOTE_APPLY=1; shift ;;
    --include-backend) FORCE_BACKEND="$2"; shift 2 ;;
    --include-frontend) FORCE_FRONTEND="$2"; shift 2 ;;
    --remote-apply) FORCE_REMOTE_APPLY="$2"; shift 2 ;;
    *) echo "未知参数: $1"; exit 1 ;;
  esac
done

json_get() {
  # json_get <file> <key> <default>
  local file="$1" key="$2" def="${3:-}"
  if [[ ! -f "$file" ]]; then echo "$def"; return; fi
  if command -v python3 >/dev/null 2>&1; then
    python3 - "$file" "$key" "$def" <<'PY'
import json,sys
path,key,default=sys.argv[1],sys.argv[2],sys.argv[3]
try:
    with open(path,encoding='utf-8') as f: data=json.load(f)
except Exception:
    print(default); sys.exit(0)
cur=data
for part in key.split('.'):
    if isinstance(cur,dict) and part in cur: cur=cur[part]
    else: print(default); sys.exit(0)
if isinstance(cur,bool): print('true' if cur else 'false')
elif cur is None: print(default)
else: print(cur)
PY
  else
    echo "$def"
  fi
}

CFG=""
if [[ -n "$CONFIG" && -f "$CONFIG" ]]; then CFG="$CONFIG"
elif [[ -f "./hr-upgrade.json" ]]; then CFG="./hr-upgrade.json"
elif [[ -f "./deploy/upgrade/hr-upgrade.json" ]]; then CFG="./deploy/upgrade/hr-upgrade.json"
fi

INCLUDE_BACKEND=true
INCLUDE_FRONTEND=true
if [[ -n "$CFG" ]]; then
  INCLUDE_BACKEND="$(json_get "$CFG" includeBackend true)"
  INCLUDE_FRONTEND="$(json_get "$CFG" includeFrontend true)"
fi
[[ -n "$FORCE_BACKEND" ]] && INCLUDE_BACKEND="$FORCE_BACKEND"
[[ -n "$FORCE_FRONTEND" ]] && INCLUDE_FRONTEND="$FORCE_FRONTEND"
[[ "$BACKEND_ONLY" == "1" ]] && INCLUDE_FRONTEND=false && INCLUDE_BACKEND=true
[[ "$FRONTEND_ONLY" == "1" ]] && INCLUDE_BACKEND=false && INCLUDE_FRONTEND=true

FRONTEND_PATH="$(json_get "$CFG" frontendPath "${FRONTEND_PATH:-}")"
[[ -z "$FRONTEND_PATH" ]] && FRONTEND_PATH="../vue-vben-admin-main"
SKIP_TESTS="$(json_get "$CFG" skipTests true)"
SKIP_FE_INSTALL="$(json_get "$CFG" skipFrontendInstall false)"
OUTPUT_REL="$(json_get "$CFG" outputDir dist/upgrades)"
JAR_NAME="$(json_get "$CFG" jarName hr-management.jar)"
OUTPUT_DIR="$REPO_ROOT/$OUTPUT_REL"

STAMP="$(date +%Y%m%d-%H%M%S)"
PKG_NAME="hr-upgrade-$STAMP"
PKG_DIR="$OUTPUT_DIR/$PKG_NAME"
mkdir -p "$PKG_DIR/backend" "$PKG_DIR/frontend" "$PKG_DIR/bin"

echo "=== 生成升级包 $PKG_NAME ==="
echo "仓库: $REPO_ROOT"
echo "后端: $INCLUDE_BACKEND  前端: $INCLUDE_FRONTEND"

if [[ "$INCLUDE_BACKEND" == "true" ]]; then
  echo ">>> Maven package"
  if [[ "$SKIP_TESTS" == "true" ]]; then
    mvn -DskipTests package
  else
    mvn package
  fi
  JAR="$(ls -1t "$REPO_ROOT"/target/*.jar 2>/dev/null | grep -vE 'original|sources|javadoc' | head -n1 || true)"
  [[ -n "$JAR" && -f "$JAR" ]] || { echo "未找到 target/*.jar"; exit 1; }
  cp -f "$JAR" "$PKG_DIR/backend/$JAR_NAME"
  echo "已放入后端: $(basename "$JAR") -> backend/$JAR_NAME"
fi

if [[ "$INCLUDE_FRONTEND" == "true" ]]; then
  [[ -d "$FRONTEND_PATH" ]] || { echo "前端目录不存在: $FRONTEND_PATH"; exit 1; }
  echo ">>> 构建前端: $FRONTEND_PATH"
  (
    cd "$FRONTEND_PATH"
    if [[ "$SKIP_FE_INSTALL" != "true" ]]; then pnpm install; fi
    pnpm build:antd
  )
  DIST="$FRONTEND_PATH/apps/web-antd/dist"
  [[ -d "$DIST" ]] || { echo "未找到前端 dist: $DIST"; exit 1; }
  cp -a "$DIST"/. "$PKG_DIR/frontend/"
  echo "已放入前端 dist"
fi

cp -f "$SCRIPT_DIR/templates/apply.sh" "$PKG_DIR/bin/apply.sh"
cp -f "$SCRIPT_DIR/templates/apply.ps1" "$PKG_DIR/bin/apply.ps1"
cp -f "$SCRIPT_DIR/templates/rollback.sh" "$PKG_DIR/bin/rollback.sh"
cp -f "$SCRIPT_DIR/templates/rollback.ps1" "$PKG_DIR/bin/rollback.ps1"
cp -f "$SCRIPT_DIR/templates/README.txt" "$PKG_DIR/README.txt"
mkdir -p "$PKG_DIR/deploy"
if [[ -f "$REPO_ROOT/deploy/Dockerfile.backend" ]]; then
  cp -f "$REPO_ROOT/deploy/Dockerfile.backend" "$PKG_DIR/deploy/Dockerfile.backend"
fi

cat > "$PKG_DIR/MANIFEST.txt" <<EOF
name=$PKG_NAME
created=$(date '+%Y-%m-%d %H:%M:%S')
includeBackend=$INCLUDE_BACKEND
includeFrontend=$INCLUDE_FRONTEND
repo=$REPO_ROOT
host=$(hostname 2>/dev/null || echo unknown)
EOF
chmod +x "$PKG_DIR/bin/"*.sh

cat > "$PKG_DIR/README.txt" <<'EOF'
HR 升级包使用说明
================
1. 将 zip 上传到服务器（如 /opt/hr-management/upgrades/）
2. 解压后进入目录
3. Docker 部署:
   chmod +x bin/*.sh
   HR_HOME=/opt/hr-management/deploy HR_USE_DOCKER=1 ./bin/apply.sh
4. 可选环境变量:
   HR_HOME         含 docker-compose.yml 的目录（一般为 .../deploy）
   HR_SERVICE      systemd 服务名
   HR_USE_DOCKER=1 使用 docker compose 停启/重建
EOF

ZIP_PATH="$OUTPUT_DIR/$PKG_NAME.zip"
rm -f "$ZIP_PATH"
(
  cd "$OUTPUT_DIR"
  if command -v zip >/dev/null 2>&1; then
    zip -r "$PKG_NAME.zip" "$PKG_NAME" >/dev/null
  else
    tar -czf "${PKG_NAME}.tar.gz" "$PKG_NAME"
    ZIP_PATH="$OUTPUT_DIR/${PKG_NAME}.tar.gz"
  fi
)

echo ""
echo "升级包已生成:"
echo "  目录: $PKG_DIR"
echo "  压缩: $ZIP_PATH"

UPLOAD_ENABLED="$(json_get "$CFG" upload.enabled false)"
DO_REMOTE_APPLY=0
if [[ "$SKIP_UPLOAD" != "1" && "$UPLOAD_ENABLED" == "true" ]]; then
  HOST="$(json_get "$CFG" upload.host "")"
  PORT="$(json_get "$CFG" upload.port 22)"
  USER="$(json_get "$CFG" upload.user "")"
  REMOTE="$(json_get "$CFG" upload.remoteDir "")"
  KEY="$(json_get "$CFG" upload.privateKeyPath "")"
  HR_HOME_REMOTE="$(json_get "$CFG" upload.hrHome "/opt/hr-management/deploy")"
  APPLY_CFG="$(json_get "$CFG" upload.applyAfterUpload false)"
  if [[ "$FORCE_REMOTE_APPLY" == "true" ]]; then DO_REMOTE_APPLY=1
  elif [[ "$FORCE_REMOTE_APPLY" == "false" ]]; then DO_REMOTE_APPLY=0
  elif [[ "$APPLY_CFG" == "true" ]]; then DO_REMOTE_APPLY=1
  fi
  if [[ "$SKIP_REMOTE_APPLY" == "1" ]]; then DO_REMOTE_APPLY=0; fi

  if [[ -z "$HOST" || -z "$USER" || -z "$REMOTE" ]]; then
    echo "upload 配置不完整，跳过上传"
  else
    SSH_OPTS=(-p "$PORT" -o StrictHostKeyChecking=accept-new)
    SCP_OPTS=(-P "$PORT")
    [[ -n "$KEY" ]] && SSH_OPTS+=(-i "$KEY") && SCP_OPTS+=(-i "$KEY")

    echo ">>> 准备远程目录 ${USER}@${HOST}:$REMOTE"
    ssh "${SSH_OPTS[@]}" "${USER}@${HOST}" "mkdir -p '$REMOTE'"

    echo ">>> SCP 上传到 ${USER}@${HOST}:$REMOTE"
    scp "${SCP_OPTS[@]}" "$ZIP_PATH" "${USER}@${HOST}:${REMOTE}/"
    echo "上传完成"

    if [[ "$DO_REMOTE_APPLY" == "1" ]]; then
      ZIP_NAME="$(basename "$ZIP_PATH")"
      echo ">>> SSH 远程 apply HR_HOME=$HR_HOME_REMOTE"
      ssh "${SSH_OPTS[@]}" "${USER}@${HOST}" bash -s <<EOF
set -e
cd '$REMOTE'
unzip -o '$ZIP_NAME'
cd '$PKG_NAME'
chmod +x bin/*.sh
HR_HOME='$HR_HOME_REMOTE' HR_USE_DOCKER=1 ./bin/apply.sh
EOF
      echo "远程应用完成"
    fi
  fi
fi

echo ""
if [[ "$DO_REMOTE_APPLY" == "1" ]]; then
  echo "完成：已上传并在服务器应用升级包。"
else
  echo "下一步: 服务器解压后执行"
  echo "  HR_HOME=/opt/hr-management/deploy HR_USE_DOCKER=1 ./bin/apply.sh"
fi
