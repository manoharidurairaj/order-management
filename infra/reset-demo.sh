#!/usr/bin/env bash
# Resets the order-pipeline demo to a clean slate: stops the load generator,
# clears the Kafka topics/consumer-group offsets, truncates MySQL, and
# flushes Redis's idempotency keys. Infra containers (kafka, mysql, redis,
# prometheus, grafana) are left running throughout.
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
docker-compose stop order-ingestion-service order-pipeline-service dashboard-service load-generator

step "Deleting Kafka topics (orders.lifecycle, orders.dlq)"
docker exec ordermgmt-kafka kafka-topics --delete --topic orders.lifecycle --bootstrap-server kafka:9092 || echo "  (orders.lifecycle already gone)"
docker exec ordermgmt-kafka kafka-topics --delete --topic orders.dlq --bootstrap-server kafka:9092 || echo "  (orders.dlq already gone)"

step "Deleting stale consumer group offsets"
docker exec ordermgmt-kafka kafka-consumer-groups --delete --group order-pipeline-service --bootstrap-server kafka:9092 || echo "  (order-pipeline-service group already gone)"
docker exec ordermgmt-kafka kafka-consumer-groups --delete --group dashboard-service --bootstrap-server kafka:9092 || echo "  (dashboard-service group already gone)"

step "Truncating MySQL orders / order_history"
docker exec ordermgmt-mysql mysql -uordermgmt -pordermgmt orders -e "TRUNCATE TABLE order_history; TRUNCATE TABLE orders;"

step "Flushing Redis (idempotency keys)"
docker exec ordermgmt-redis redis-cli FLUSHALL

step "Restarting app services"
docker-compose start order-ingestion-service order-pipeline-service dashboard-service

if $WITH_LOAD; then
  step "Restarting load generator"
  docker-compose start load-generator
else
  step "Load generator left stopped (pass --with-load to also start it)"
fi

step "Done — check http://localhost:8084, every tile should read 0"
