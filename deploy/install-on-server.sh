#!/bin/bash
# Usage on server: bash /opt/install-hr.sh
set -eu

OPT_DIR=/opt
APP_DIR=/opt/hr-management
DEPLOY_DIR=/opt/hr-management/deploy

echo "=== 1. check zip files ==="
if [ ! -f "$OPT_DIR/hr-backend.zip" ]; then
  echo "missing $OPT_DIR/hr-backend.zip"
  exit 1
fi
if [ ! -f "$OPT_DIR/web-frontend-dist.zip" ]; then
  echo "missing $OPT_DIR/web-frontend-dist.zip"
  exit 1
fi

echo "=== 2. check unzip / docker ==="
if ! command -v unzip >/dev/null 2>&1; then
  dnf install -y unzip || yum install -y unzip
fi
if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found, install docker first"
  exit 1
fi

echo "=== 3. unzip backend ==="
rm -rf "$APP_DIR"
unzip -o "$OPT_DIR/hr-backend.zip" -d "$OPT_DIR"
if [ ! -d "$APP_DIR" ]; then
  echo "after unzip, $APP_DIR not found"
  exit 1
fi

echo "=== 4. unzip frontend ==="
mkdir -p "$DEPLOY_DIR/frontend-dist"
rm -rf "$DEPLOY_DIR/frontend-dist/"*
unzip -o "$OPT_DIR/web-frontend-dist.zip" -d "$DEPLOY_DIR/frontend-dist"
if [ ! -f "$DEPLOY_DIR/frontend-dist/index.html" ]; then
  echo "frontend index.html missing"
  exit 1
fi

echo "=== 5. prepare .env ==="
cd "$DEPLOY_DIR"
if [ ! -f .env ]; then
  if [ -f .env.example ]; then
    cp .env.example .env
  else
    touch .env
  fi
  echo "created $DEPLOY_DIR/.env — edit it then start compose"
  echo "  vi $DEPLOY_DIR/.env"
  echo "  cd $DEPLOY_DIR && docker compose up --build -d"
  exit 0
fi

echo "=== 6. start docker compose ==="
docker compose up --build -d
docker compose ps
echo "done. open http://39.105.67.125"