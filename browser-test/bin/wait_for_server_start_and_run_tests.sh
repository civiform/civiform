#!/usr/bin/env bash

set -euo pipefail

START_TIME=$(date +%s)
DEADLINE=$(($START_TIME + 500))
SERVER_URL="http://civiform:9000"

# Install any new packages not built into the image
# Also saves any package-lock.json changes back to your local filesystem.
npm install --force --quiet

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

  # If test fails - don't abort script. Allow eslint below to run in case it can
  # provide useful findings about code.
if (($debug == 1)); then
  DEBUG="pw:api" BASE_URL="${SERVER_URL}" npm test "$@" || true
else
  BASE_URL="${SERVER_URL}" npm test "$@"  || true
fi

echo -e "\nRunning eslint..."
npx eslint --cache "src/**/*.ts"
echo "Done!"
