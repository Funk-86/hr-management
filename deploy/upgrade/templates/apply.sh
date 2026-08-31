#!/usr/bin/env bash
# 在服务器上解压升级包后执行：./bin/apply.sh
# 环境变量（可选）：
#   HR_HOME      应用根目录；Docker 部署建议设为 .../deploy（含 docker-compose.yml）
#   HR_SERVICE   systemd 服务名，如 hr-management；为空则尝试 docker compose
#   HR_USE_DOCKER=1  强制走 docker compose 重启/重建

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

is_docker_mode() {
  if [[ "${HR_USE_DOCKER:-0}" == "1" ]]; then
    return 0
  fi
  if [[ -f "$HR_HOME/docker-compose.yml" && -z "${HR_SERVICE:-}" ]]; then
    return 0
  fi
  return 1
}

stop_app() {
  if is_docker_mode; then
    if [[ -f "$HR_HOME/docker-compose.yml" ]]; then
      echo ">>> 停止 docker compose (backend/nginx)"
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
  if is_docker_mode; then
    if [[ -f "$HR_HOME/docker-compose.yml" ]]; then
      if [[ -f "$HR_HOME/prebuilt/app.jar" ]]; then
        echo ">>> docker compose up --build（使用升级包 jar）"
        (cd "$HR_HOME" && docker compose up -d --build backend nginx)
        echo ">>> 清理 prebuilt/app.jar（避免下次误用旧包）"
        rm -f "$HR_HOME/prebuilt/app.jar"
      else
        echo ">>> docker compose up -d"
        (cd "$HR_HOME" && docker compose up -d)
      fi
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

# ----- 后端 jar -----
if [[ -f "$PKG_DIR/backend/hr-management.jar" ]]; then
  mkdir -p "$HR_HOME/backend"
  if [[ -f "$HR_HOME/backend/hr-management.jar" ]]; then
    cp -a "$HR_HOME/backend/hr-management.jar" "$BACKUP_DIR/hr-management.jar"
  elif [[ -f "$HR_HOME/hr-management.jar" ]]; then
    cp -a "$HR_HOME/hr-management.jar" "$BACKUP_DIR/hr-management.jar"
  fi
  echo ">>> 替换后端 jar"
  cp -f "$PKG_DIR/backend/hr-management.jar" "$HR_HOME/backend/hr-management.jar"
  cp -f "$PKG_DIR/backend/hr-management.jar" "$HR_HOME/hr-management.jar" 2>/dev/null || true

  # Docker 源码构建镜像：写入 prebuilt，并同步升级用 Dockerfile
  if is_docker_mode && [[ -f "$HR_HOME/docker-compose.yml" ]]; then
    mkdir -p "$HR_HOME/prebuilt"
    cp -f "$PKG_DIR/backend/hr-management.jar" "$HR_HOME/prebuilt/app.jar"
    echo ">>> 已写入 $HR_HOME/prebuilt/app.jar（Docker 构建将使用预编译 jar）"
    if [[ -f "$PKG_DIR/deploy/Dockerfile.backend" ]]; then
      cp -f "$PKG_DIR/deploy/Dockerfile.backend" "$HR_HOME/Dockerfile.backend"
      echo ">>> 已更新 $HR_HOME/Dockerfile.backend"
    fi
  fi
fi

# ----- 前端静态资源 -----
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
