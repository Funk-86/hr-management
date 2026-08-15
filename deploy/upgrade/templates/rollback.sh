#!/usr/bin/env bash
# 用法: ./bin/rollback.sh /path/to/backup/20260810-120000
set -euo pipefail
PKG_DIR="$(cd "$(dirname "$0")/.." && pwd)"
HR_HOME="${HR_HOME:-$(cd "$PKG_DIR/.." && pwd)}"
BACKUP_DIR="${1:-}"

if [[ -z "$BACKUP_DIR" || ! -d "$BACKUP_DIR" ]]; then
  echo "用法: $0 <backup_dir>"
  exit 1
fi

echo "=== 回滚到 $BACKUP_DIR ==="
if [[ -f "$BACKUP_DIR/hr-management.jar" ]]; then
  mkdir -p "$HR_HOME/backend"
  cp -f "$BACKUP_DIR/hr-management.jar" "$HR_HOME/backend/hr-management.jar"
  cp -f "$BACKUP_DIR/hr-management.jar" "$HR_HOME/hr-management.jar" 2>/dev/null || true
  echo "已恢复 jar"
fi
if [[ -d "$BACKUP_DIR/frontend" ]]; then
  DEST="$HR_HOME/frontend-dist"
  [[ -d "$HR_HOME/deploy/frontend-dist" ]] && DEST="$HR_HOME/deploy/frontend-dist"
  mkdir -p "$DEST"
  rm -rf "${DEST:?}/"*
  cp -a "$BACKUP_DIR/frontend/." "$DEST/"
  echo "已恢复前端 -> $DEST"
fi
echo "请手动重启服务（systemctl / docker compose）"
