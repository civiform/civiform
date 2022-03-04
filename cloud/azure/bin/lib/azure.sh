#! /usr/bin/env bash

# CHARSET is a regex pattern that matches the acceptable characters to 
# use when generating a secret value
readonly CHARSET='A-Za-z0-9!"#$%&'\''()*+,-./:;<=>?@[\]^_`{|}~'

readonly KEY_VAULT_SECRETS_OFFICER_GUID="b86a8fe4-44ce-4948-aee5-eccb2c155cd7"

#######################################
# Create resource group
# Arguments:
#   1: The resource group name 
#   2: The location of the resource group
#######################################
function azure::create_resource_group() {
  if [ $(az group exists --name "${1}") = false ]; then
    az group create --name "${1}" --location "${2}"
  fi
}

