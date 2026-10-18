#!/usr/bin/env sh
# Nightly PostgreSQL backup. Run from deploy/ via cron on the VM, e.g.:
#   0 2 * * * cd /opt/nearby/deploy && sh scripts/backup.sh >> backups/backup.log 2>&1
# Then sync deploy/backups/ to off-site storage (rclone, az storage blob upload-batch, ...).
set -eu

KEEP_DAYS="${KEEP_DAYS:-14}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"

docker compose exec -T postgres sh -c \
  'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom --file="/backups/nearby-'"$STAMP"'.dump"'

find ./backups -name 'nearby-*.dump' -mtime +"$KEEP_DAYS" -delete
echo "Backup nearby-$STAMP.dump done"
