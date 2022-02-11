#! /usr/bin/env bash

#######################################
# Create resource group
# Arguments:
#   1: The resource group name for the key vault
#   2: The location of the key vault
#######################################
function key_vault::create_resource_group(){
    az group create --name "${1}" -l "${2}"
}

#######################################
# Create key vault
# Arguments:
#   1: The resource group name for the key vault
#   2: The location of the key vault
#   3: The name of the key vault 
#######################################
function key_vault::create_vault(){
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
#   2: The name of the secret (i.e. "postgres-password")
#   3: The value of the secret
#######################################
function key_vault::add_secret(){
    az keyvault secret set \
        --vault-name "${1}" \
        --name "${2}" \
        --value "${3}"

#######################################
# Generates and adds secrets to the key vault
# Arguments:
#   1: The name of the key vault
#   2..n: The name of the secret (i.e. "postgres-password")
#######################################
function key_vault::add_generated_secrets(){
    for ${@:2} 
    do
        secret_value = "$(head -1 /dev/urandom | cut -c -40)"
        key_vault::add_secret "${1}" "${2}" "${secret_value}"
    done
