#! /usr/bin/env bash

#######################################
# Create resource group
# Arguments:
#   1: The resource group name for the key vault
#   2: The location of the key vault
#######################################
function key_vault::create_resource_group() {
    if [ $(az group exists --name "${1}") = false ]; then
        az group create --name "${1}" -l "${2}"
    fi
}

#######################################
# Create key vault
# Arguments:
#   1: The resource group name for the key vault
#   2: The location of the key vault
#   3: The name of the key vault 
#######################################
function key_vault::create_vault() {
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
function key_vault::add_secret() {
    az keyvault secret set \
        --vault-name "${1}" \
        --name "${2}" \
        --value "${3}"
}

#######################################
# Generates and adds secrets to the key vault
# Arguments:
#   1: The name of the key vault
#   2..n: Names of the secrets to be created (e.g. "postgres-password")
#######################################
function key_vault::add_generated_secrets() {
    local vault_name="${1}"
    charset='A-Za-z0-9!"#$%&'\''()*+,-./:;<=>?@[\]^_`{|}~'
    shift;
    for key in "$@";
    do
        echo "Generating secret: ${key}"
        secret_value="$(head /dev/urandom | tr -dc "${charset}" | cut -c -40)"
        echo "Setting secret: ${key}"
        key_vault::add_secret "${vault_name}" "${key}" "${secret_value}"
    done
}
