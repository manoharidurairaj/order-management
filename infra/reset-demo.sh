#!/usr/bin/env bash
# Resets the order-pipeline demo to a clean slate: stops the load generator,
# clears the Kafka topics/consumer-group offsets, truncates MySQL, and
# flushes Redis's idempotency keys. Infra containers (kafka, mysql, redis,
# prometheus, grafana) are left running throughout.
#
# Container names here match the root-level docker-compose.yml exactly
# (both use the "order-management" project name and container prefix), so
# this script works the same whether the stack was started from here
# (infra/docker-compose.yml, built from local source) or from the repo
# root (docker-compose.yml, pulled from Docker Hub) — no detection needed.
#
# Every docker operation below targets containers by their explicit
# `order-management-*` name (`docker exec`/`docker stop`/`docker start`)
# rather than going through `docker compose ... -p order-management`. A
# Compose project name is whatever the stack happened to be brought up
# with — e.g. plain `docker compose up` from infra/ with no `-p` flag
# defaults to the directory name ("infra"), not "order-management" — so a
# hardcoded `-p order-management` can silently match zero containers and
# then hard-fail on dependency resolution. Explicit container names sidestep
# that entirely regardless of how the stack was started.
#
# See README.md > "Resetting for a fresh demo" for what each step does and why.
#
# Usage:
#   ./reset-demo.sh              # reset only
#   ./reset-demo.sh --with-load  # reset, then also restart the load generator

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

WITH_LOAD=false
if [[ "${1:-}" == "--with-load" ]]; then
  WITH_LOAD=true
fi

step() { echo; echo "==> $1"; }

step "Stopping load generator"
curl -s -X POST http://localhost:8085/load/stop || echo "  (load generator not reachable — already stopped?)"

step "Stopping app services (order-ingestion-service, order-pipeline-service, dashboard-service, load-generator)"
docker stop order-management-order-ingestion-service order-management-order-pipeline-service order-management-dashboard-service order-management-load-generator

delete_topic() {
  local topic="$1"
  docker exec order-management-kafka kafka-topics --delete --topic "$topic" --bootstrap-server kafka:9092 \
    || echo "  ($topic already gone)"
  # Deletion is asynchronous — the CLI call above returns once the delete
  # is *requested*, not once the log segments are actually purged. If
  # dashboard-service (auto-offset-reset: earliest) reconnects before the
  # topic is truly gone, it replays whatever old messages are still on
  # disk and rebuilds the exact same pre-reset counts instead of starting
  # from zero. Poll `--describe` until the topic is confirmed absent.
  for _ in $(seq 1 15); do
    if ! docker exec order-management-kafka kafka-topics --describe --topic "$topic" --bootstrap-server kafka:9092 >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "  ($topic still present after retries — it may not be fully purged before services restart)"
}

step "Deleting Kafka topics (orders.lifecycle, orders.dlq)"
delete_topic orders.lifecycle
delete_topic orders.dlq

delete_consumer_group() {
  local group="$1"
  local out
  # Consumers stopped above leave the group asynchronously — a container
  # can be SIGKILLed by the docker stop grace period before its
  # LeaveGroupRequest lands, so the broker may still report the group as
  # non-empty for a few seconds after `docker compose stop` returns. Retry
  # instead of guessing a grace period that's always long enough.
  for _ in $(seq 1 15); do
    if out=$(docker exec order-management-kafka kafka-consumer-groups --delete --group "$group" --bootstrap-server kafka:9092 2>&1); then
      return 0
    fi
    if echo "$out" | grep -q "GroupIdNotFoundException"; then
      echo "  ($group group already gone)"
      return 0
    fi
    if echo "$out" | grep -q "GroupNotEmptyException"; then
      sleep 2
      continue
    fi
    echo "$out"
    return 1
  done
  echo "$out"
  echo "  ($group group still not empty after retries — leftover members will time out on their own)"
  return 1
}

step "Deleting stale consumer group offsets"
delete_consumer_group order-pipeline-service || true
delete_consumer_group dashboard-service || true

step "Truncating MySQL orders / order_history"
docker exec order-management-mysql mysql -uordermgmt -pordermgmt orders -e "TRUNCATE TABLE order_history; TRUNCATE TABLE orders;"

step "Flushing Redis (idempotency keys)"
docker exec order-management-redis redis-cli FLUSHALL

step "Restarting app services"
docker start order-management-order-ingestion-service order-management-order-pipeline-service order-management-dashboard-service

if $WITH_LOAD; then
  step "Restarting load generator"
  docker start order-management-load-generator
else
  step "Load generator left stopped (pass --with-load to also start it)"
fi

step "Done — check http://localhost:8084, every tile should read 0"
