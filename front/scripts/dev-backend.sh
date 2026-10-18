#!/bin/sh
# Runs the backend (the repository root) against a file-based H2 database kept in
# .dev-data/, so local frontend work needs neither PostgreSQL nor manual setup.
# Data and the JWT secret persist across restarts; delete .dev-data/ to start over.
set -eu

WEB_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="${BACKEND_DIR:-$WEB_DIR/..}"
DATA_DIR="$WEB_DIR/.dev-data"

mkdir -p "$DATA_DIR"
SECRET_FILE="$DATA_DIR/jwt-secret"
[ -f "$SECRET_FILE" ] || openssl rand -base64 32 > "$SECRET_FILE"

cd "$BACKEND_DIR"
# The H2 driver is test-scoped in the backend, hence useTestClasspath.
JWT_SECRET_BASE64="$(cat "$SECRET_FILE")" \
DB_URL="jdbc:h2:file:$DATA_DIR/db/cracks;MODE=PostgreSQL;AUTO_SERVER=TRUE" \
DB_USERNAME=sa \
DB_PASSWORD= \
DB_DDL_AUTO=update \
VIDEO_STORAGE_DIR="$DATA_DIR/videos" \
exec sh mvnw -q spring-boot:run -Dspring-boot.run.useTestClasspath=true
