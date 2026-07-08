#!/usr/bin/env bash
# Demo 2 — "Cause a dinner rush": establishes a modest steady baseline, then
# slams a sharp, time-boxed burst on top of it so you can watch the
# throughput spike and the pipeline absorb a heavy, bursty load — then
# watch it fall back to baseline on its own once the burst window ends.
#
# Tip: run ./reset-demo.sh first for a clean baseline to spike from.
#
# Usage: ./demo-2-dinner-rush.sh [baseline-rate] [burst-multiplier] [burst-seconds]
#   baseline-rate      steady orders/sec before the rush   (default 2)
#   burst-multiplier   how hard the rush multiplies it      (default 8)
#   burst-seconds      how long the rush lasts              (default 20)

set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

BASELINE="${1:-2}"
MULTIPLIER="${2:-8}"
DURATION="${3:-20}"

step() { echo; echo "==> $1"; }

if [ "$(docker inspect --format='{{.State.Running}}' order-management-load-generator 2>/dev/null)" != "true" ]; then
  step "load-generator isn't running — starting it (reset-demo.sh leaves it stopped by default)"
  docker start order-management-load-generator >/dev/null
  until [ "$(docker inspect --format='{{.State.Health.Status}}' order-management-load-generator 2>/dev/null)" = "healthy" ]; do
    sleep 2
  done
fi

step "Open the live dashboard: http://localhost:8084"
echo "    and Grafana for the infra-level view: http://localhost:3000"
sleep 3

step "Establishing a steady baseline: ${BASELINE} orders/sec (uncapped — runs until stopped)"
curl -s -X POST http://localhost:8085/load/rate \
  -H "Content-Type: application/json" \
  -d "{\"ordersPerSecond\": ${BASELINE}}"
echo
sleep 5

step "DINNER RUSH — bursting at ${MULTIPLIER}x (~$(( BASELINE * MULTIPLIER )) orders/sec) for ${DURATION}s"
curl -s -X POST http://localhost:8085/load/burst \
  -H "Content-Type: application/json" \
  -d "{\"multiplier\": ${MULTIPLIER}, \"durationSeconds\": ${DURATION}}"
echo

echo "    Watch for: a sharp spike on the dashboard's throughput chart and"
echo "    Grafana's request-rate panels, then a drop back to baseline the"
echo "    moment the burst window ends — nobody has to remember to turn it off."

sleep "${DURATION}"

step "Burst window over — effective rate should have dropped back to ${BASELINE}/sec on its own"
curl -s http://localhost:8085/load/status
echo

step "Stopping load generator"
curl -s -X POST http://localhost:8085/load/stop
echo

step "Done."
