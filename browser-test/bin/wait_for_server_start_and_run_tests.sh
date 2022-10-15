#!/usr/bin/env bash

set -euo pipefail

START_TIME=$(date +%s)
DEADLINE=$(($START_TIME + 500))

# Allow callers to override BASE_URL if they want (e.g. for run-browser-tests-local).
# Defaults civiform:9000 when running from within docker.
export BASE_URL="${BASE_URL:-http://civiform:9000}"

export TEST_USER_AUTH_STRATEGY=fake-oidc
export TEST_USER_LOGIN=testuser
export TEST_USER_PASSWORD=anotsecretpassword
# The display name returned by test_oidc_provider.js is <username>@example.com.
export TEST_USER_DISPLAY_NAME=testuser@example.com

if ! output="$(node -v)"; then
  echo output
  echo "You must have node installed locally to run this command. Go to https://nodejs.org/en/download/package-manager/ for installation instructions."
  exit 1
fi

# Sanity check that this script was called from within browser-test directory.
if [ ! -d "image_snapshots" ]; then
  echo output
  echo "$(basename "$0") must be run from within browser-test directory."
  exit 1
fi

# Install any new packages not built into the image
# Also saves any package-lock.json changes back to your local filesystem.
npm install --force --quiet

echo "Polling to check server start. Server url: ${BASE_URL}"

until $(curl --output /dev/null --silent --head --fail --max-time 2 "${BASE_URL}"); do
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
  DEBUG="pw:api" npm test "$@"
else
  npm test "$@"
fi
