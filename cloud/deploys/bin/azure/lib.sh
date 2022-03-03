#! /usr/bin/env bash

# CHARSET is a regex pattern that matches the acceptable characters to 
# use when generating a secret value
readonly CHARSET='A-Za-z0-9!"#$%&'\''()*+,-./:;<=>?@[\]^_`{|}~'
readonly BLOB_DATA_CONTRIBUTOR_GUID="ba92f5b4-2d11-453d-a403-e96b0029c9fe"
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
# Assign the role 'Key Vault Secrets officer' to the current user
#######################################
function azure::assign_secrets_officer_role_to_user() {
  local USER_ID=$(az ad signed-in-user show --query objectId -o tsv)
  local SUBSCRIPTION_ID=$(az account show --query id -o tsv)
  local ROLE_ASSIGNMENTS=$(az role assignment list --assignee ${USER_ID})
  if [[ ("Key Vault Secrets Officer" == *$ROLE_ASSIGNMENTS*) ]];
  then 
    echo "Current user already has Key Vault Secrets Officer role"
  else
    az role assignment create \
      --role ${KEY_VAULT_SECRETS_OFFICER_GUID} \
      --scope "subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${resource_group}" \
      --assignee-object-id "${USER_ID}" \
      --assignee-principal-type "User"
  fi
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
    --value "${3}" > /dev/null
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
    local SECRET_VALUE="$(head /dev/urandom | \
      LC_CTYPE=ALL tr -dc "${CHARSET}" | \
      LC_CTYPE=ALL cut -c -40)"
    echo "Setting secret: ${key}"
    azure::add_secret "${VAULT_NAME}" "${key}" "${SECRET_VALUE}"
  done
}

#######################################
# Get a secret value from the key vault. 
# Ouputs the string that the secret is set to, ex. eRZE*8d-$EM*0tSxKXIp63yVnY~t2zI:=[Bm#FB*
# Arguments:
#   1: The name of the key vault
#   2: The name of the secret (used to identify it e.g. "postgres-password")
#######################################
function azure::get_secret_value() {
  az keyvault secret show --vault-name "${1}" --name "${2}" --query value -o tsv
}

#######################################
# Assign the role 'Storage Blob Data Contributor' to the current user
#######################################
function azure::assign_storage_blob_data_contributor_role_to_user() {
  local USER_ID=$(az ad signed-in-user show --query objectId -o tsv)
  local SUBSCRIPTION_ID=$(az account show --query id -o tsv)
  local ROLE_ASSIGNMENTS=$(az role assignment list --assignee ${USER_ID})
  if [[ ("Storage Blob Data Contributor" == *$ROLE_ASSIGNMENTS*) ]];
  then 
    echo "Current user already has Storage Blob Data Contributor role"
  else
    az role assignment create \
      --role  "${BLOB_DATA_CONTRIBUTOR_GUID}" \
      --scope "subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${resource_group}" \
      --assignee-object-id "${USER_ID}" \
      --assignee-principal-type "User"
  fi
}

#######################################
# Create storage account
# Arguments:
#   1: The resource group name for the storage account
#   2: The region (e.g. EastUS) to create the storage account in
#   3: The name of the storage account
#######################################
function azure::create_storage_account {
  az storage account create \
    --resource-group "${1}" \
    --location "${2}" \
    --name "${3}" \
    --allow-blob-public-access false \
    --min-tls-version "TLS1_2"
}

#######################################
# Create storage container
# Arguments:
#   1: The storage account name to create the container in
#   2: The name of the container to create
#######################################
function azure::create_storage_container {
  az storage container create \
    --account-name "${1}" \
    --name "${2}" 
}

#######################################
# Upload blob
# Arguments:
#   1: The storage account name
#   2. The name of the container to upload to
#   3: The path to the file to upload
#######################################
function azure::upload_blob {
  az storage blob upload \
    --account-name "${1}" \
    --container-name "${2}" \
    --file "${3}"
}
