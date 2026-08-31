#!/bin/bash
set -eu
IP="${1:-39.105.67.125}"
DIR="$(cd "$(dirname "$0")" && pwd)/certs"
mkdir -p "$DIR"
openssl req -x509 -nodes -newkey rsa:2048 -days 825 \
  -keyout "$DIR/privkey.pem" \
  -out "$DIR/fullchain.pem" \
  -subj "/CN=${IP}" \
  -addext "subjectAltName=IP:${IP}"
chmod 600 "$DIR/privkey.pem"
chmod 644 "$DIR/fullchain.pem"
echo "certs written to $DIR"
ls -la "$DIR"