#!/bin/bash
set -e

BACKUP_DIR="$(pwd)"
DATE=$(date +%F)

mkdir -p "$BACKUP_DIR"

docker compose stop db

docker run --rm \
    -v smcb_mysql-data:/volume \
    -v "$BACKUP_DIR":/backup \
    alpine \
    tar czf "/backup/mysql-backup-$DATE.tar.gz" -C /volume .


docker compose start db

echo "Backup created:"
echo "$BACKUP_DIR/mysql-backup-$DATE.tar.gz"
