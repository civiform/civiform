#!/usr/bin/env bash

set -euo pipefail

START_TIME=$(date +%s)
DEADLINE=$(($START_TIME + 500))

# Allow callers to override BASE_URL if they want (e.g. for run-browser-tests-local).
# Defaults civiform:9000 when running from within docker.
export BASE_URL="${BASE_URL:-http://civiform:9000}"

export TEST_USER_AUTH_STRATEGY="${TEST_USER_AUTH_STRATEGY:-fake-oidc}"
export TEST_USER_LOGIN="${TEST_USER_LOGIN:-testuser}"
export TEST_USER_PASSWORD="${TEST_USER_PASSWORD:-anotsecretpassword}"
# The display name returned by test_oidc_provider.js is <username>@example.com.
export TEST_USER_DISPLAY_NAME="${TEST_USER_DISPLAY_NAME:-testuser@example.com}"

if ! output="$(node -v)"; then
  echo output
  echo "You must have node installed locally to run this command. Go to https://nodejs.org/en/download/package-manager/ for installation instructions."
  exit 1
fi

# Sanity check that this script was called from within browser-test directory.
# This is done by checking whether the current directory looks like browser-test:
# it should contains image_snapshots subdirectory.
if [ ! -d "image_snapshots" ]; then
  echo output
  echo "$(basename "$0") must be run from within browser-test directory."
  exit 1
fi

# Install any new packages not built into the image
# Also saves any package-lock.json changes back to your local filesystem.
npm ci --quiet
npx playwright install --with-deps chromium

echo "Polling to check server start. Server url: ${BASE_URL}"

until $(curl --output /dev/null --silent --head --fail --max-time 2 "${BASE_URL}"); do
  if (($(date +%s) > "${DEADLINE}")); then
    echo "Deadline exceeded waiting for server start"
    exit 1
  fi
  sleep 5
done

echo "Detected server start"

# Balance shards by expected test duration instead of by test count. Replaces
# --shard=N/M with a --test-list file for that shard (see bin/shard_tests.mjs).
# Without shard-durations.json, every shard uses Playwright's own sharding.
# If the list can't be built, fail this shard instead of falling back: one
# shard on Playwright's split while the others use the balanced lists would
# run some tests twice and skip others without any test failing.
args=()
shard=""
for arg in "$@"; do
  if [[ "${arg}" =~ ^--shard=([0-9]+/[0-9]+)$ && -f shard-durations.json ]]; then
    shard="${BASH_REMATCH[1]}"
  else
    args+=("${arg}")
  fi
done

if [[ -n "${shard}" ]]; then
  shard_list_file="$(mktemp)"
  if ! node bin/shard_tests.mjs "${shard}" "${shard_list_file}" ${args[@]+"${args[@]}"}; then
    echo "Could not build the duration-balanced test list for shard ${shard}."
    echo "To turn balancing off, delete browser-test/shard-durations.json."
    exit 1
  fi
  args+=("--test-list=${shard_list_file}")
fi

npm test -- ${args[@]+"${args[@]}"}
