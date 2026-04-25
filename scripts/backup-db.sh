#!/usr/bin/env bash
# PostgreSQL backup script for iemodo-lite
# Usage: ./scripts/backup-db.sh [backup_dir]
set -euo pipefail

BACKUP_DIR="${1:-./backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5433}"
DB_NAME="${DB_NAME:-iemodo}"
DB_USERNAME="${DB_USERNAME:-iemodo}"
DB_PASSWORD="${DB_PASSWORD:-iemodo123}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

mkdir -p "$BACKUP_DIR"

echo "=== Iemodo DB Backup ==="
echo "  Host: $DB_HOST:$DB_PORT"
echo "  Database: $DB_NAME"
echo "  Backup dir: $BACKUP_DIR"
echo "  Retention: $RETENTION_DAYS days"
echo ""

BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"

PGPASSWORD="$DB_PASSWORD" pg_dump \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USERNAME" \
  -d "$DB_NAME" \
  --clean \
  --if-exists \
  --no-password \
  | gzip > "$BACKUP_FILE"

echo "  Created: $BACKUP_FILE"
echo "  Size: $(du -h "$BACKUP_FILE" | cut -f1)"
echo ""

# Remove backups older than retention period
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime "+${RETENTION_DAYS}" -delete
echo "Cleaned up backups older than $RETENTION_DAYS days."
echo "=== Backup complete ==="
