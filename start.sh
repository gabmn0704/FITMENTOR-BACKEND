#!/bin/sh
set -eu

if [ -n "${DATABASE_HOST:-}" ]; then
    DATABASE_URL="jdbc:postgresql://${DATABASE_HOST}:${DATABASE_PORT:-5432}/${DATABASE_NAME:-fitmentor}"
    export DATABASE_URL
fi

exec java -jar /app/app.jar