#! /bin/bash

# This is file is used with `docker exec` in bin scripts by civiform engineers.
# It's not used by prod civiform.

# `npm ci` deletes and reinstalls node_modules, which is slow. Only run it when
# the package files differ from the ones node_modules was last installed from.
# The checksum file lives inside node_modules so it shares the lifetime of the volume.
readonly LOCKFILE_CHECKSUM_FILE="node_modules/.package-files.sha256"

package_files_checksum="$(cat package.json package-lock.json | sha256sum | cut -d ' ' -f 1)"

if [[ "$(cat "${LOCKFILE_CHECKSUM_FILE}" 2>/dev/null)" != "${package_files_checksum}" ]]; then
  npm ci && echo "${package_files_checksum}" >"${LOCKFILE_CHECKSUM_FILE}"
else
  echo "package.json and package-lock.json unchanged, skipping npm ci"
fi

sbt "$@"
