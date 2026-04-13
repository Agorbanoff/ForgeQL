#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

if [ ! -f "$PROJECT_ROOT/.env" ]; then
  echo "No .env file found in $PROJECT_ROOT. Docker Compose will use its built-in defaults and any exported shell variables."
fi

wait_for_service() {
  service_name=$1
  attempts=${2:-30}
  count=0

  while [ "$count" -lt "$attempts" ]; do
    container_id=$(cd "$PROJECT_ROOT" && docker compose ps -q "$service_name")

    if [ -n "$container_id" ]; then
      status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id")

      case "$status" in
        healthy|running)
          echo "$service_name is $status"
          return 0
          ;;
        exited|dead)
          echo "$service_name failed to start"
          cd "$PROJECT_ROOT"
          docker compose logs --tail=50 "$service_name"
          return 1
          ;;
      esac
    fi

    sleep 2
    count=$((count + 1))
  done

  echo "Timed out waiting for $service_name"
  cd "$PROJECT_ROOT"
  docker compose logs --tail=50 "$service_name"
  return 1
}

cd "$PROJECT_ROOT"
docker compose up -d

wait_for_service postgres
wait_for_service backend
wait_for_service frontend

docker compose ps
