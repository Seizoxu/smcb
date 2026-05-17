#!/bin/bash
set -e

if [ -z "$1" ]; then
    echo "Usage: ./restore.sh <backup-file>"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Backup file not found: $BACKUP_FILE"
    exit 1
fi

echo "WARNING: This will ERASE the current MySQL volume."
read -p "Continue? (y/n): " CONFIRM

if [ "$CONFIRM" != "y" ]; then
    echo "Aborted."
    exit 1
fi


docker compose stop db

docker run --rm \
  -v smcb_mysql-data:/volume \
  -v "$(dirname "$BACKUP_FILE")":/backup \
  alpine \
  sh -c "rm -rf /volume/* && tar xzf /backup/$(basename "$BACKUP_FILE") -C /volume"

docker compose start db

echo "Restore complete."
