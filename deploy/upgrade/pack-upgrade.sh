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
FORCE_BACKEND=""
FORCE_FRONTEND=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --config) CONFIG="$2"; shift 2 ;;
    --backend-only) BACKEND_ONLY=1; shift ;;
    --frontend-only) FRONTEND_ONLY=1; shift ;;
    --skip-upload) SKIP_UPLOAD=1; shift ;;
    --include-backend) FORCE_BACKEND="$2"; shift 2 ;;
    --include-frontend) FORCE_FRONTEND="$2"; shift 2 ;;
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
chmod +x "$PKG_DIR/bin/"*.sh

cat > "$PKG_DIR/MANIFEST.txt" <<EOF
name=$PKG_NAME
created=$(date '+%Y-%m-%d %H:%M:%S')
includeBackend=$INCLUDE_BACKEND
includeFrontend=$INCLUDE_FRONTEND
repo=$REPO_ROOT
host=$(hostname)
EOF

cat > "$PKG_DIR/README.txt" <<'EOF'
HR 升级包使用说明
================
1. 将本目录或同名 zip 上传到服务器应用目录旁（如 /opt/hr/upgrades/）
2. 解压后进入目录
3. Linux:   chmod +x bin/*.sh && HR_HOME=/opt/hr ./bin/apply.sh
   Windows: $env:HR_HOME='C:\hr'; .\bin\apply.ps1
4. 可选环境变量:
   HR_HOME       应用根目录
   HR_SERVICE    systemd / Windows 服务名
   HR_USE_DOCKER=1  使用 docker compose 停启
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
if [[ "$SKIP_UPLOAD" != "1" && "$UPLOAD_ENABLED" == "true" ]]; then
  HOST="$(json_get "$CFG" upload.host "")"
  PORT="$(json_get "$CFG" upload.port 22)"
  USER="$(json_get "$CFG" upload.user "")"
  REMOTE="$(json_get "$CFG" upload.remoteDir "")"
  KEY="$(json_get "$CFG" upload.privateKeyPath "")"
  if [[ -z "$HOST" || -z "$USER" || -z "$REMOTE" ]]; then
    echo "upload 配置不完整，跳过上传"
  else
    echo ">>> SCP 上传到 ${USER}@${HOST}:$REMOTE"
    SCP_OPTS=(-P "$PORT")
    [[ -n "$KEY" ]] && SCP_OPTS+=(-i "$KEY")
    scp "${SCP_OPTS[@]}" "$ZIP_PATH" "${USER}@${HOST}:${REMOTE}/"
    echo "上传完成"
  fi
fi

echo ""
echo "下一步: 把压缩包拷到服务器解压后执行 bin/apply.sh 或 bin/apply.ps1"
