#!/usr/bin/env bash
# HR 系统 Docker 一键启动（Linux / macOS）
# 用法: cd deploy && chmod +x start.sh && ./start.sh

set -euo pipefail
DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DEPLOY_DIR"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

FRONTEND_PATH="${FRONTEND_PATH:-../vue-vben-admin-main}"
if [[ ! -d "$FRONTEND_PATH" ]]; then
  echo "未找到前端目录: $FRONTEND_PATH"
  echo "请复制 .env.example 为 .env 并设置 FRONTEND_PATH"
  exit 1
fi

echo ">>> 构建前端: $FRONTEND_PATH"
(
  cd "$FRONTEND_PATH"
  pnpm install
  pnpm build:antd
)

DIST_SRC="$FRONTEND_PATH/apps/web-antd/dist"
DIST_DST="$DEPLOY_DIR/frontend-dist"
if [[ ! -d "$DIST_SRC" ]]; then
  echo "前端构建失败，未找到 dist 目录"
  exit 1
fi

echo ">>> 复制前端产物"
mkdir -p "$DIST_DST"
rm -rf "${DIST_DST:?}/"*
cp -r "$DIST_SRC"/. "$DIST_DST/"

if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "已生成 .env，可按需修改后重新运行"
fi

echo ">>> 启动 Docker Compose"
docker compose up --build -d

echo ""
echo "启动完成！"
echo "  访问地址: http://localhost"
echo "  测试账号: employee / Emp@2024"
echo "  查看日志: docker compose logs -f"
