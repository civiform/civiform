#!/usr/bin/env bash

readonly LIB_DIR="${BASH_SOURCE%/*}/lib"

if [[ "${SOURCED_AZURE_LIB}" != "true" ]]; then
  source "${LIB_DIR}/out.sh"
  source "${LIB_DIR}/azure.sh"
  source "${LIB_DIR}/azure_log.sh"
  source "${LIB_DIR}/bastion.sh"
  source "${LIB_DIR}/storage.sh"
  source "${LIB_DIR}/key_vault.sh"
  SOURCED_AZURE_LIB="true"
fi
