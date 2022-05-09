#!/usr/bin/env bash

set -euo pipefail

START_TIME=$(date +%s)
DEADLINE=$(($START_TIME + 500))
SERVER_URL="http://civiform:9000"

echo "Polling to check server start"

until $(curl --output /dev/null --silent --head --fail --max-time 2 "${SERVER_URL}"); do
  if (($(date +%s) > "${DEADLINE}")); then
    echo "Deadline exceeded waiting for server start"
    exit 1
  fi
done

echo "Detected server start"

debug=0
for arg; do
  shift
  # if debug flag, set the var and leave it out of the forwarded args list
  [ "$arg" = "--debug" ] && debug=1 && continue
  set -- "$@" "$arg"
done

if (($debug == 1)); then
  DEBUG="pw:api" BASE_URL="${SERVER_URL}" yarn test "$@"
else
  BASE_URL="${SERVER_URL}" yarn test "$@"
fi
