#!/usr/bin/env bash

readonly CLOUD_LIB_DIR="${BASH_SOURCE%/*}/lib"

if [[ "${SOURCED_CLOUD_LIB}" != "true" ]]; then
  source "${CLOUD_LIB_DIR}/health.sh"
  source "${CLOUD_LIB_DIR}/log.sh"
  SOURCED_CLOUD_LIB="true"
fi
