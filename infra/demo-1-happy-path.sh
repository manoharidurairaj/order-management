#!/usr/bin/env bash
# Demo 1 — "Show the pipeline running": sends a small batch of real orders
# through the live system so you can watch them move through their full
# lifecycle (Placed -> Confirmed -> Preparing -> Ready -> Out for Delivery
# -> Delivered) on the live dashboard, in real time.
#
# Tip: run ./reset-demo.sh first for a clean "0 -> N" story on the dashboard.
#
# Usage: ./demo-1-happy-path.sh [orders] [rate-per-second]
#   orders            how many orders to send (default 20)
#   rate-per-second   how fast to send them   (default 2)

set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

ORDERS="${1:-20}"
RATE="${2:-2}"

step() { echo; echo "==> $1"; }

if [ "$(docker inspect --format='{{.State.Running}}' order-management-load-generator 2>/dev/null)" != "true" ]; then
  step "load-generator isn't running — starting it (reset-demo.sh leaves it stopped by default)"
  docker start order-management-load-generator >/dev/null
  until [ "$(docker inspect --format='{{.State.Health.Status}}' order-management-load-generator 2>/dev/null)" = "healthy" ]; do
    sleep 2
  done
fi

step "Open the live dashboard now: http://localhost:8084"
echo "    (Grafana, for the infra-level view: http://localhost:3000)"
sleep 3

step "Sending ${ORDERS} orders at ${RATE}/sec — watch the dashboard"
curl -s -X POST http://localhost:8085/load/rate \
  -H "Content-Type: application/json" \
  -d "{\"ordersPerSecond\": ${RATE}, \"maxOrders\": ${ORDERS}}"
echo

echo "    Watch for: the state-distribution bars filling in left to right as"
echo "    orders advance, Total/Active/Delivered tiles climbing, and the"
echo "    throughput chart ticking up."

SLEEP_SECONDS=$(( ORDERS / RATE + 15 ))
step "Waiting ~${SLEEP_SECONDS}s for all ${ORDERS} orders to reach Delivered"
sleep "${SLEEP_SECONDS}"

step "Final load-generator status:"
curl -s http://localhost:8085/load/status
echo

step "Done — every order placed should now show Delivered on the dashboard."
