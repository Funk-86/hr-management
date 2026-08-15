#!/usr/bin/env bash
# 在服务器上解压升级包后执行：./bin/apply.sh
# 环境变量（可选）：
#   HR_HOME      应用根目录，默认升级包上一级或 /opt/hr
#   HR_SERVICE   systemd 服务名，如 hr-management；为空则尝试 docker compose
#   HR_USE_DOCKER=1  强制走 docker compose 重启

set -euo pipefail
PKG_DIR="$(cd "$(dirname "$0")/.." && pwd)"
HR_HOME="${HR_HOME:-$(cd "$PKG_DIR/.." && pwd)}"
TS="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="$HR_HOME/backup/$TS"

echo "=== HR 升级包应用 ==="
echo "包目录: $PKG_DIR"
echo "应用目录: $HR_HOME"
echo "备份目录: $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"

stop_app() {
  if [[ "${HR_USE_DOCKER:-0}" == "1" ]] || [[ -f "$HR_HOME/docker-compose.yml" && -z "${HR_SERVICE:-}" ]]; then
    if [[ -f "$HR_HOME/docker-compose.yml" ]]; then
      echo ">>> 停止 docker compose"
      (cd "$HR_HOME" && docker compose stop backend nginx 2>/dev/null || docker compose stop || true)
      return
    fi
  fi
  if [[ -n "${HR_SERVICE:-}" ]]; then
    echo ">>> systemctl stop $HR_SERVICE"
    sudo systemctl stop "$HR_SERVICE" || true
    return
  fi
  echo ">>> 未配置 HR_SERVICE，跳过停止（请自行停服）"
}

start_app() {
  if [[ "${HR_USE_DOCKER:-0}" == "1" ]] || [[ -f "$HR_HOME/docker-compose.yml" && -z "${HR_SERVICE:-}" ]]; then
    if [[ -f "$HR_HOME/docker-compose.yml" ]]; then
      echo ">>> 启动 docker compose"
      (cd "$HR_HOME" && docker compose up -d)
      return
    fi
  fi
  if [[ -n "${HR_SERVICE:-}" ]]; then
    echo ">>> systemctl start $HR_SERVICE"
    sudo systemctl start "$HR_SERVICE"
    return
  fi
  echo ">>> 未配置启动方式，请手动启动后端"
}

stop_app

if [[ -f "$PKG_DIR/backend/hr-management.jar" ]]; then
  mkdir -p "$HR_HOME/backend"
  if [[ -f "$HR_HOME/backend/hr-management.jar" ]]; then
    cp -a "$HR_HOME/backend/hr-management.jar" "$BACKUP_DIR/hr-management.jar"
  elif [[ -f "$HR_HOME/hr-management.jar" ]]; then
    cp -a "$HR_HOME/hr-management.jar" "$BACKUP_DIR/hr-management.jar"
  fi
  echo ">>> 替换后端 jar"
  cp -f "$PKG_DIR/backend/hr-management.jar" "$HR_HOME/backend/hr-management.jar"
  # 兼容 jar 放在根目录的部署
  cp -f "$PKG_DIR/backend/hr-management.jar" "$HR_HOME/hr-management.jar" 2>/dev/null || true
fi

if [[ -d "$PKG_DIR/frontend" ]] && [[ -n "$(ls -A "$PKG_DIR/frontend" 2>/dev/null || true)" ]]; then
  DEST=""
  if [[ -d "$HR_HOME/frontend-dist" ]]; then
    DEST="$HR_HOME/frontend-dist"
  elif [[ -d "$HR_HOME/deploy/frontend-dist" ]]; then
    DEST="$HR_HOME/deploy/frontend-dist"
  elif [[ -d "$HR_HOME/html" ]]; then
    DEST="$HR_HOME/html"
  else
    DEST="$HR_HOME/frontend-dist"
    mkdir -p "$DEST"
  fi
  if [[ -d "$DEST" ]] && [[ -n "$(ls -A "$DEST" 2>/dev/null || true)" ]]; then
    mkdir -p "$BACKUP_DIR/frontend"
    cp -a "$DEST/." "$BACKUP_DIR/frontend/" || true
  fi
  echo ">>> 替换前端静态文件 -> $DEST"
  mkdir -p "$DEST"
  rm -rf "${DEST:?}/"*
  cp -a "$PKG_DIR/frontend/." "$DEST/"
fi

if [[ -f "$PKG_DIR/MANIFEST.txt" ]]; then
  cp -f "$PKG_DIR/MANIFEST.txt" "$HR_HOME/LAST_UPGRADE.txt"
fi

start_app

echo ""
echo "升级完成。回滚可参考备份: $BACKUP_DIR"
echo "或执行: HR_HOME=$HR_HOME ./bin/rollback.sh $BACKUP_DIR"
