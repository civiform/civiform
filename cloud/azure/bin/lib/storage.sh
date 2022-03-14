#! /usr/bin/env bash

readonly BLOB_DATA_CONTRIBUTOR_GUID="ba92f5b4-2d11-453d-a403-e96b0029c9fe"

#######################################
# Assign the role 'Storage Blob Data Contributor' to the current user
# Arguments:
#   1. The resource group to scope the role assignment to
#######################################
function storage::assign_storage_blob_data_contributor_role_to_user() {
  local USER_ID="$(az ad signed-in-user show --query objectId -o tsv)"
  local SUBSCRIPTION_ID="$(az account show --query id -o tsv)"
  local ROLE_ASSIGNMENTS="$(az role assignment list --assignee ${USER_ID} --resource-group ${1})"

  if echo "${ROLE_ASSIGNMENTS}" | grep -q "Storage Blob Data Contributor"; then
    echo "Current user already has Storage Blob Data Contributor role"
  else
    az role assignment create \
      --role  "${BLOB_DATA_CONTRIBUTOR_GUID}" \
      --scope "subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${1}" \
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
function storage::create_storage_account {
  az storage account create \
    --resource-group "${1}" \
    --location "${2}" \
    --name "${3}" \
    --allow-blob-public-access false \
    --min-tls-version "TLS1_2" \
    --sku Standard_ZRS \
    --encryption-services blob
}

#######################################
# Create storage container
# Arguments:
#   1: The storage account name to create the container in
#   2: The name of the container to create
#######################################
function storage::create_storage_container {
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
function storage::upload_blob {
  az storage blob upload \
    --account-name "${1}" \
    --container-name "${2}" \
    --file "${3}"
}
