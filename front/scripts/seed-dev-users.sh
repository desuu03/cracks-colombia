#!/bin/sh
# Creates one test user per role in the local backend, plus a few athletes with
# complete profiles so the search has results. Public registration only
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
register kevin@cracks.test            "Kevin Hurtado"  DEPORTISTA         2007-06-02 null
register daniela@cracks.test          "Daniela Pantoja" DEPORTISTA        2006-09-30 null
register samuel@cracks.test           "Samuel Arrieta" DEPORTISTA         2005-04-18 null
register brayan@cracks.test           "Brayan Mena"    DEPORTISTA         2006-11-05 null

login_token() { # email
  curl -s -X POST "$API_URL/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$PASSWORD\"}" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

profile() { # email json — fills the profile through PUT /users/me
  token=$(login_token "$1")
  status=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$API_URL/users/me" \
    -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d "$2") || status=000
  [ "$status" = 200 ] && echo "perfil     $1" || { echo "Error $status guardando el perfil de $1" >&2; exit 1; }
}

profile deportista@cracks.test '{"sport":"Fútbol","position":"Delantero","region":"Quindío","heightCm":178,"weightKg":70,"trajectory":"Goleador de la Liga Quindiana Sub-20 en 2025 con 14 goles."}'
profile deportista.menor@cracks.test '{"sport":"Atletismo","position":"Velocidad 100 m","region":"Chocó","heightCm":165,"weightKg":54,"trajectory":"Campeona departamental de 100 m en 2025 con 12,1 s."}'
profile kevin@cracks.test '{"sport":"Fútbol","position":"Volante","region":"Valle del Cauca","heightCm":172,"weightKg":66,"trajectory":"Volante de marca en la liga vallecaucana."}'
profile daniela@cracks.test '{"sport":"Voleibol","position":"Armador","region":"Nariño","heightCm":181,"weightKg":68,"trajectory":"Armadora de la selección Nariño en los Juegos Nacionales Juveniles 2025."}'
profile samuel@cracks.test '{"sport":"Baloncesto","position":"Base","region":"Bolívar","heightCm":185,"weightKg":74,"trajectory":"Base titular del torneo departamental de Bolívar."}'
profile brayan@cracks.test '{"sport":"Fútbol","position":"Portero","region":"Risaralda","heightCm":188,"weightKg":80,"trajectory":"Valla menos vencida del torneo departamental Sub-20 de 2025."}'
profile entrenador@cracks.test '{"club":"Academia Cafetera FC","contactPhone":"+57 300 555 0142"}'

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
