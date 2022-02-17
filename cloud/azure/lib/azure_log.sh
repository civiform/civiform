#! /usr/bin/env bash

readonly AZURE_LOG_FILE_NAME="civiform_deployment_log.txt"
readonly AZURE_STORAGE_CONTAINER_NAME="civiform_deployment_log_storage_container"
readonly AZURE_LOG_STORAGE_ACCOUNT_NAME="civiform_deployment_log_storage"

#######################################
# Download the deploy log to a temporary file.
# Globals read:
#   AZURE_LOG_STORAGE_ACCOUNT_NAME
#   AZURE_LOG_CONTAINER_NAME
#   AZURE_LOG_FILE_NAME
# Globals set:
#   LOG_TEMPFILE: the path to a tempfile containing the deploy log.
#######################################
function azure_log::fetch_log_file() {
  export LOG_TEMPFILE="$(mktemp)"

  az storage blob download \
    --account-name "${AZURE_LOG_STORAGE_ACCOUNT_NAME}" \
    --container-name "${AZURE_LOG_CONTAINER_NAME}" \
    --name "${AZURE_LOG_FILE_NAME}" \
    --file "${LOG_TEMPFILE}"
}

#######################################
# Uploads the deploy log to Azure and removes the local tempfile.
# Globals read:
#   AZURE_LOG_STORAGE_ACCOUNT_NAME
#   AZURE_LOG_CONTAINER_NAME
#   AZURE_LOG_FILE_NAME
#   LOG_TEMPFILE
#######################################
function azure_log::upload_log_file() {
  az storage blob upload \
    --account-name "${AZURE_LOG_STORAGE_ACCOUNT_NAME}" \
    --container-name "${AZURE_LOG_CONTAINER_NAME}" \
    --name "${AZURE_LOG_FILE_NAME}" \
    --file "${LOG_TEMPFILE}"

  rm "${LOG_TEMPFILE}"
}

#######################################
# Initialize the deplooy log in Azure blob storage.
# Globals read:
#   AZURE_LOG_STORAGE_ACCOUNT_NAME
#   AZURE_LOG_CONTAINER_NAME
#   AZURE_LOG_FILE_NAME
#   AZURE_LOCATION
#   AZURE_RESOURCE_GROUP
#   AZURE_SUBSCRIPTION
#######################################
function azure_log::initialize_log_file() {
  az storage account create \
    --name "${AZURE_LOG_STORAGE_ACCOUNT_NAME}" \
    --resource-group "${AZURE_RESOURCE_GROUP}" \
    --location "${AZURE_LOCATION}" \
    --sku Standard_ZRS \
    --encryption-services blob

  az ad signed-in-user show --query objectId -o tsv \
    | az role assignment create \
      --role "Storage Blob Data Contributor" \
      --assignee @- \
      --scope "/subscriptions/${AZURE_SUBSCRIPTION}/resourceGroups/${AZURE_RESOURCE_GROUP}/providers/Microsoft.Storage/storageAccounts/${AZURE_LOG_STORAGE_ACCOUNT_NAME}"

  az storage container create \
    --account-name "${AZURE_LOG_STORAGE_ACCOUNT_NAME}" \
    --name "${AZURE_STORAGE_CONTAINER_NAME}"

  export LOG_TEMPFILE="$(mktemp)"
  log::initialized "$(azure_log::get_current_user_id)"
  azure_log::upload_log_file
}

#######################################
# Writes the current signed in Azure user to stdout with whitespace replaced
# by underscores for suitability as a token in the deploy log.
#######################################
function azure_log::get_current_user_id() {
  az ad signed-in-user show | sed -E 's/ +/_/g'
}
