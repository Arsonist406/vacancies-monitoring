#!/usr/bin/env bash
set -o pipefail

if ! docker info >/dev/null 2>&1; then
  echo "Docker engine is down. Starting Docker Desktop..."
  open -a Docker
  until docker info >/dev/null 2>&1; do
    sleep 1
  done
  echo "Docker is running"
fi

echo "Starting build+tests (clean+verify)"
LOG=$(./mvnw -q -B clean verify -Dexec.skip=true 2>&1)
CODE=$?
if [ $CODE -eq 0 ]; then
  echo "OK: build+tests passed"
else
  echo "FAIL:"
  echo "$LOG" | grep -B2 -A 40 -E "ERROR|Tests run:.*(Failures: [1-9]|Errors: [1-9])"
fi
exit $CODE
