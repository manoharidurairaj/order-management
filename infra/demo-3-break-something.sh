#!/usr/bin/env bash
# Demo 3 — "Break something on purpose": starts a steady stream of orders,
# then kills order-pipeline-service (the component that consumes Kafka
# events and drives every order through its state machine) while orders are
# actively in flight — then brings it back and shows that nothing was lost
# or double-processed: Kafka holds the backlog while the consumer is down,
# and every order still reaches Delivered exactly once after recovery.
#
# Tip: run ./reset-demo.sh first for a clean before/after count.
#
# Usage: ./demo-3-break-something.sh [rate-per-second] [outage-seconds]
#   rate-per-second   steady orders/sec while the pipeline is up and during
#                      the outage (default 3) — stops the moment the outage
#                      window ends, so recovery drains a fixed, known
#                      backlog instead of chasing still-arriving orders
#   outage-seconds    how long the pipeline stays down (default 20)

set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

RATE="${1:-3}"
OUTAGE_SECONDS="${2:-20}"

step() { echo; echo "==> $1"; }

if [ "$(docker inspect --format='{{.State.Running}}' order-management-load-generator 2>/dev/null)" != "true" ]; then
  step "load-generator isn't running — starting it (reset-demo.sh leaves it stopped by default)"
  docker start order-management-load-generator >/dev/null
  until [ "$(docker inspect --format='{{.State.Health.Status}}' order-management-load-generator 2>/dev/null)" = "healthy" ]; do
    sleep 2
  done
fi

step "Open the live dashboard: http://localhost:8084"
sleep 3

step "Starting a steady stream: ${RATE} orders/sec (uncapped)"
curl -s -X POST http://localhost:8085/load/rate \
  -H "Content-Type: application/json" \
  -d "{\"ordersPerSecond\": ${RATE}}"
echo
sleep 5

step "Orders flowing normally. KILLING order-pipeline-service now (docker stop)"
docker stop order-management-order-pipeline-service
echo "    order-pipeline-service is DOWN. Ingestion is still up and still accepting"
echo "    orders — they queue up in Kafka with nothing consuming them."

step "Letting the backlog build for ${OUTAGE_SECONDS}s"
echo "    Watch the dashboard: Active/Total keep climbing from ingestion, but"
echo "    nothing advances past Placed while the pipeline is down."
sleep "${OUTAGE_SECONDS}"

step "Stopping new orders now — so what we're about to recover is a fixed,"
echo "    known backlog rather than a moving target"
curl -s -X POST http://localhost:8085/load/stop
echo

step "Consumer lag right now — proof nothing is lost, it's queued in Kafka waiting:"
docker exec order-management-kafka kafka-consumer-groups --describe --group order-pipeline-service --bootstrap-server kafka:9092 \
  || echo "  (group not visible yet — fine, means the pipeline hadn't consumed anything before it died)"

step "RECOVERING — restarting order-pipeline-service"
docker start order-management-order-pipeline-service

step "Waiting for it to report healthy again"
until [ "$(docker inspect --format='{{.State.Health.Status}}' order-management-order-pipeline-service 2>/dev/null)" = "healthy" ]; do
  sleep 2
done
echo "    order-pipeline-service is back and healthy."

step "Watch the dashboard now: the backlog should drain fast — bars catching"
echo "    up toward Delivered as the consumer works through everything Kafka held."
sleep 25

step "Consumer lag after recovery (should be back down near 0):"
docker exec order-management-kafka kafka-consumer-groups --describe --group order-pipeline-service --bootstrap-server kafka:9092 || true

step "Final tallies — compare these by hand:"
echo
echo "  Load generator — orders ACCEPTED by ingestion (never resets, cumulative):"
curl -s http://localhost:8085/load/status
echo
echo "  Dashboard — orders the pipeline has actually recorded, plus dlqCount:"
curl -s http://localhost:8084/api/dashboard/snapshot
echo

step "Done. If 'ordersAccepted' above matches 'totalOrdersPlaced', and 'dlqCount'"
echo "    is 0, every order survived the outage and completed exactly once —"
echo "    nothing lost, nothing double-processed. If dashboard numbers are still"
echo "    catching up, give it another 10-20s and re-check:"
echo "    curl -s http://localhost:8084/api/dashboard/snapshot"
