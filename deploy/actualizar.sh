#!/usr/bin/env bash
# Baja lo último de main en el backend y la web, y reconstruye los contenedores. Los datos se conservan.
# Uso, desde la raíz del repositorio del backend:  bash deploy/actualizar.sh
set -euo pipefail
cd "$(dirname "$0")/.."
git pull --ff-only origin main
git -C ../ViaDeep-webplatform pull --ff-only origin main
docker compose -f docker-compose.yml -f deploy/docker-compose.prod.yml --profile app up -d --build
docker compose -f docker-compose.yml -f deploy/docker-compose.prod.yml --profile app ps
