#!/usr/bin/env bash

set -e
set -o pipefail

export TF_VAR_FILENAME="setup.auto.tfvars"
export BACKEND_VARS_FILENAME="backend_vars"

if [[ "${SOURCED_CLOUD_LIB}" != "true" ]]; then
  readonly CLOUD_LIB_DIR="${BASH_SOURCE%/*}/lib"
  source "bin/lib/out.sh"
  source "${CLOUD_LIB_DIR}/health.sh"
  source "${CLOUD_LIB_DIR}/log.sh"
  source "${CLOUD_LIB_DIR}/terraform.sh"

  SOURCED_CLOUD_LIB="true"
fi
