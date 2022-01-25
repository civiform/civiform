#!/usr/bin/env bash

readonly LIB_DIR="${BASH_SOURCE%/*}/lib"

if [[ "${SOURCED_LIB}" != "true" ]]; then
  source "${LIB_DIR}/out.sh"
  source "${LIB_DIR}/truth.sh"

  SOURCED_LIB="true"
fi
