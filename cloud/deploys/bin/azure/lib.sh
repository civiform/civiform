#! /usr/bin/env bash

# CHARSET is a regex pattern that matches the acceptable characters to 
# use when generating a secret value
readonly CHARSET='A-Za-z0-9!"#$%&'\''()*+,-./:;<=>?@[\]^_`{|}~'

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

#######################################
# Create key vault
# Arguments:
#   1: The resource group name for the key vault
#   2: The region (e.g. EastUS) to create the key vault in
#   3: The name of the key vault 
#######################################
function azure::create_vault() {
  az keyvault create \
    --name "${3}" \
    --resource-group "${1}"\
    --location "${2}" \
    --enable-rbac-authorization
}

#######################################
# Add a secret to the key vault
# Arguments:
#   1: The name of the key vault
#   2: The name of the secret (used to identify it e.g. "postgres-password")
#   3: The value of the secret
#######################################
function azure::add_secret() {
  az keyvault secret set \
    --vault-name "${1}" \
    --name "${2}" \
    --value "${3}"
}

#######################################
# For each argument after the first, generates a secret value and adds it to the
# key vault, using the argument as the secret name.
# Arguments:
#   1: The name of the key vault
#   2..n: Names of the secrets to be created (e.g. "postgres-password")
#######################################
function azure::add_generated_secrets() {
  local VAULT_NAME="${1}"
  shift;
  for key in "$@";
  do
    echo "Generating secret: ${key}"
    local SECRET_VALUE="$(head /dev/urandom | LC_CTYPE=C tr -dc "${CHARSET}" | cut -c -40)"
    echo "Setting secret: ${key}"
    azure::add_secret "${VAULT_NAME}" "${key}" "${SECRET_VALUE}"
  done
}
