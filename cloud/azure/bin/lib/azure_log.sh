#! /usr/bin/env bash

readonly AZURE_LOG_CONTAINER_NAME="civiformdeploymentlogstoragecontainer"
readonly AZURE_LOG_FILE_NAME="civiform_deployment_log.txt"

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
    --auth-mode login \
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
  echo "Uploading deployment log file..."
  az storage blob upload \
    --auth-mode login \
    --account-name "${AZURE_LOG_STORAGE_ACCOUNT_NAME}" \
    --container-name "${AZURE_LOG_CONTAINER_NAME}" \
    --name "${AZURE_LOG_FILE_NAME}" \
    --file "${LOG_TEMPFILE}"
  echo "Done uploading deployment log file."

  rm "${LOG_TEMPFILE}"
}

#######################################
# Returns a successful status code if the deployment logfile already exists.
# Globals read:
#   AZURE_LOG_STORAGE_ACCOUNT_NAME
#   AZURE_LOG_CONTAINER_NAME
#   AZURE_LOG_FILE_NAME
#######################################
function azure_log::log_file_exists() {
  az storage blob exists \
    --auth-mode login \
    --account-name "${AZURE_LOG_STORAGE_ACCOUNT_NAME}" \
    --container-name "${AZURE_LOG_CONTAINER_NAME}" \
    --name "${AZURE_LOG_FILE_NAME}" \
    | grep -q true
}

#######################################
# Initialize the deploy log in Azure blob storage.
# Globals read:
#   AZURE_LOG_STORAGE_ACCOUNT_NAME
#   AZURE_LOG_CONTAINER_NAME
#   AZURE_LOG_FILE_NAME
#   AZURE_LOCATION
#   AZURE_RESOURCE_GROUP
#   AZURE_SUBSCRIPTION
#######################################
function azure_log::initialize_log_file() {
  echo "Start azure_log::initialize_log_file"

  echo "Creating deploy log storage account..."
  az storage account create \
    --name "${AZURE_LOG_STORAGE_ACCOUNT_NAME}" \
    --resource-group "${AZURE_RESOURCE_GROUP}" \
    --location "${AZURE_LOCATION}" \
    --sku Standard_ZRS \
    --encryption-services blob
  echo "Done creating deploy log storage account."

  echo "Creating deploy log storage container..."
  az storage container create \
    --auth-mode login \
    --account-name "${AZURE_LOG_STORAGE_ACCOUNT_NAME}" \
    --name "${AZURE_LOG_CONTAINER_NAME}"
  echo "Done creating deploy log storage container."

  echo "Granting current user access deploy log storage account..."
  local CURRENT_USER_ID="$(az ad signed-in-user show --query objectId -o tsv)"
  az role assignment create \
    --role "Storage Blob Data Contributor" \
    --assignee "${CURRENT_USER_ID}" \
    --scope "/subscriptions/${AZURE_SUBSCRIPTION}/resourceGroups/${AZURE_RESOURCE_GROUP}/providers/Microsoft.Storage/storageAccounts/${AZURE_LOG_STORAGE_ACCOUNT_NAME}"
  echo "Done granting current user access deploy log storage account."

  # If the logfile already exists, we fetch it and append the initialized
  # event into the log. Otherwise we create a new file and upload it.
  if azure_log::log_file_exists; then
    echo "Existing logfile found, updating..."
    azure_log::fetch_log_file
  else
    echo "No existing logfile found, creating a new one..."
    export LOG_TEMPFILE="$(mktemp)"
  fi

  log::initialized "$(azure::get_current_user_id)"
  azure_log::upload_log_file

  echo "Done azure_log::initialize_log_file"
}
