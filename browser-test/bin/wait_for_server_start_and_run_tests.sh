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

# `npm ci` deletes and reinstalls node_modules, and `playwright install` checks for
# the browser, so both are slow to repeat. Only run them when the package files
# differ from the ones node_modules was last installed from. The Playwright
# version is pinned in package-lock.json, so the same checksum covers the browser.
# The checksum file lives inside node_modules so it shares the lifetime of the
# install. System deps for chromium are baked into the browser test image, so
# `--with-deps` (which runs apt-get) isn't needed here.
readonly LOCKFILE_CHECKSUM_FILE="node_modules/.package-files.sha256"

package_files_checksum="$(cat package.json package-lock.json | sha256sum | cut -d ' ' -f 1)"

if [[ "$(cat "${LOCKFILE_CHECKSUM_FILE}" 2>/dev/null)" != "${package_files_checksum}" ]]; then
  npm ci --quiet
  npx playwright install chromium
  echo "${package_files_checksum}" >"${LOCKFILE_CHECKSUM_FILE}"
else
  echo "package.json and package-lock.json unchanged, skipping npm ci and playwright install"
fi

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
