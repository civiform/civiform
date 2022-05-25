#! /usr/bin/env bash

#######################################
# Ensure that only one cloud provider flag was passed to the script.
# Exit with error message and status code 1 if more than
# Globals:
#   cloud_provider
#   already_set_cloud_provider
# Arguments:
#   1: the cloud provider indicated by the flag currently being processed
#######################################
truth::declare_var_false already_set_cloud_provider
function emulators::ensure_only_one_cloud_provider_flag() {
  if truth::is_enabled already_set_cloud_provider; then
    local duplicate_providers="${cloud_provider} and ${1}"
    echo "One cloud provider flag can be set but found both ${duplicate_providers}"
    exit 1
  fi

  truth::enable already_set_cloud_provider
}

#######################################
# Sets variables for running an environment with the Azurite Azure emulator.
# Sets globals:
#   cloud_provider
#   emulator
#   emulator_url
#   storage_service_name
#######################################
function emulators::set_azurite_emulator_vars() {
  cloud_provider="azure"
  emulator="azurite"
  emulator_url="http://localhost:10000"
  export STORAGE_SERVICE_NAME="azure-blob"
}

#######################################
# Sets variables for running an environment with the Localstack AWS emulator.
# Sets globals:
#   cloud_provider
#   emulator
#   emulator_url
#   storage_service_name
#######################################
function emulators::set_localstack_emulator_vars() {
  cloud_provider="aws"
  emulator="localstack"
  emulator_url="http://localhost:6645"
  export STORAGE_SERVICE_NAME="s3"
}
