#! /usr/bin/env bash

# CHARSET is a regex pattern that matches the acceptable characters to 
# use when generating a secret value
readonly CHARSET='A-Za-z0-9!#$%&()*+,-./:;<=>?@[\]^_{|}~'
readonly KEY_VAULT_SECRETS_OFFICER_GUID="b86a8fe4-44ce-4948-aee5-eccb2c155cd7"


#######################################
# Create key vault
# Arguments:
#   1: The resource group name for the key vault
#   2: The region (e.g. EastUS) to create the key vault in
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
# Assign the role 'Key Vault Secrets officer' to the current user
# Arguments:
#   1. The resource group to scope the role assignment to
#######################################
function key_vault::assign_secrets_officer_role_to_user() { 
  local SUBSCRIPTION_ID="$(az account show --query id -o tsv)"
  azure::ensure_role_assignment \
    "${1}" \
    ${KEY_VAULT_SECRETS_OFFICER_GUID} \
    "subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${1}" 
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
    --value "${3}" > /dev/null
}

#######################################
# Add a secret to the key vault
# Arguments:
#   1: The name of the key vault
#   2: The name of the secret (used to identify it e.g. "postgres-password")
#######################################
function key_vault::add_secret_from_input() {
  local SECRET
  echo "Please enter the value for ${2}: "
  read -s SECRET
  unset REPLY
  key_vault::add_secret "${1}" "${2}" "${SECRET}"  
  echo "Stored secret value for ${2} in key vault ${1}"
}

#######################################
# For each argument after the first, generates a secret value and adds it to the
# key vault, using the argument as the secret name.
# Arguments:
#   1: The name of the key vault
#   2..n: Names of the secrets to be created (e.g. "postgres-password")
#######################################
function key_vault::add_generated_secrets() {
  local VAULT_NAME="${1}"
  shift;
  for key in "$@";
  do
    echo "Generating secret: ${key}"
    local SECRET_VALUE="$(head /dev/urandom | \
      LC_CTYPE=ALL tr -dc "${CHARSET}" | \
      LC_CTYPE=ALL cut -c -40)"
    echo "Setting secret: ${key}"
    key_vault::add_secret "${VAULT_NAME}" "${key}" "${SECRET_VALUE}"
  done
}

#######################################
# Get a secret value from the key vault. 
# Ouputs the string that the secret is set to, ex. eRZE*8d-$EM*0tSxKXIp63yVnY~t2zI:=[Bm#FB*
# Arguments:
#   1: The name of the key vault
#   2: The name of the secret (used to identify it e.g. "postgres-password")
#######################################
function key_vault::get_secret_value() {
  az keyvault secret show --vault-name "${1}" --name "${2}" --query value -o tsv
}

#######################################
# Succeeds if the secret exists in the keyvault
# Arguments:
#   1: The name of the key vault
#   2: The name of the secret (used to identify it e.g. "postgres-password")
#######################################
function key_vault::has_secret() {
  local SECRET_RESULT="$(az keyvault secret show \
    --vault-name "${1}" \
    --name "${2}" \
    --query value \
    -o tsv 2>&1 >/dev/null)"
  
  echo "${SECRET_RESULT}" | grep -q -v "SecretNotFound"
}
