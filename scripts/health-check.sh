#!/usr/bin/env bash
# Health check script for iemodo-lite stack
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
exit_code=0

check() {
  local name="$1" url="$2" expected="$3"
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || true)
  if [ "$status" = "$expected" ]; then
    echo "  OK  $name ($status)"
  else
    echo "FAIL  $name (expected $expected, got $status)"
    exit_code=1
  fi
}

echo "=== Iemodo Lite Health Check ==="
echo ""

check "Health endpoint"    "$BASE_URL/actuator/health"  503
check "Prometheus metrics" "$BASE_URL/actuator/prometheus" 200

echo ""
if [ "$exit_code" -eq 0 ]; then
  echo "All checks passed."
else
  echo "Some checks failed."
fi
exit "$exit_code"
