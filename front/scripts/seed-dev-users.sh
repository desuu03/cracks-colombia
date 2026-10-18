#!/bin/sh
# Creates one mock user per role in the local backend. Public registration only
# allows DEPORTISTA and OJEADOR_ENTRENADOR, so the moderator and admin are
# registered through the API (to get real password hashes) and then promoted in
# the database. Safe to run repeatedly.
#
#   DB_KIND=h2 (default)  promotes in the H2 file database of scripts/dev-backend.sh
#   DB_KIND=postgres      promotes with psql; set PGHOST/PGUSER/PGDATABASE/... as needed
set -eu

WEB_DIR="$(cd "$(dirname "$0")/.." && pwd)"
API_URL="${API_URL:-http://localhost:8080}"
PASSWORD="${SEED_PASSWORD:-Cracks2026!}"
DB_KIND="${DB_KIND:-h2}"

register() { # email fullName role birthDate|"" guardianJson|null
  birth=null
  [ -n "$4" ] && birth="\"$4\""
  body=$(printf '{"email":"%s","password":"%s","fullName":"%s","role":"%s","birthDate":%s,"guardianConsent":%s,"dataProcessingConsent":true}' \
    "$1" "$PASSWORD" "$2" "$3" "$birth" "$5")
  status=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API_URL/auth/register" \
    -H 'Content-Type: application/json' -d "$body") || status=000
  case "$status" in
    201) echo "creado     $1" ;;
    409) echo "ya existía $1" ;;
    000) echo "No se pudo conectar con $API_URL. ¿Está corriendo el backend?" >&2; exit 1 ;;
    *)   echo "Error $status registrando $1" >&2; exit 1 ;;
  esac
}

register deportista@cracks.test       "Juan Pérez"     DEPORTISTA         2004-03-15 null
register deportista.menor@cracks.test "Sofía Gómez"    DEPORTISTA         2010-08-20 \
  '{"guardianFullName":"Marta Gómez","guardianEmail":"acudiente@cracks.test","accepted":true}'
register entrenador@cracks.test       "Carlos Ramírez" OJEADOR_ENTRENADOR "" null
register moderador@cracks.test        "Laura Moderadora" OJEADOR_ENTRENADOR "" null
register admin@cracks.test            "Admin Cracks"   OJEADOR_ENTRENADOR "" null

PROMOTE_SQL="UPDATE user_accounts SET role = 'MODERADOR' WHERE email = 'moderador@cracks.test';
UPDATE user_accounts SET role = 'ADMIN' WHERE email = 'admin@cracks.test';"

case "$DB_KIND" in
  h2)
    H2_JAR=$(ls "$HOME"/.m2/repository/com/h2database/h2/*/h2-*.jar | sort -V | tail -1)
    java -cp "$H2_JAR" org.h2.tools.Shell \
      -url "jdbc:h2:file:$WEB_DIR/.dev-data/db/cracks;MODE=PostgreSQL;AUTO_SERVER=TRUE" \
      -user sa -password "" -sql "$PROMOTE_SQL" > /dev/null
    ;;
  postgres)
    psql -v ON_ERROR_STOP=1 -q -c "$PROMOTE_SQL"
    ;;
  *)
    echo "DB_KIND desconocido: $DB_KIND (usa h2 o postgres)" >&2; exit 1 ;;
esac
echo "Roles MODERADOR y ADMIN asignados. Contraseña de todos: $PASSWORD"
