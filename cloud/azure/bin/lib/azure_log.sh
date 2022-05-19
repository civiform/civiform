#! /usr/bin/env bash

source "cloud/shared/bin/lib.sh"

readonly AZURE_LOG_CONTAINER_NAME="civiformdeploymentlogstoragecontainer"
readonly AZURE_LOG_FILE_NAME="civiform_deployment_log.txt"

#######################################
# Download the deploy log to a temporary file.
# Arguments:
#   1: (optional) filepath for log file
# Globals read:
#   AZURE_LOG_STORAGE_ACCOUNT_NAME
#   AZURE_LOG_CONTAINER_NAME
#   AZURE_LOG_FILE_NAME
# Globals set:
#   LOG_TEMPFILE: the path to a tempfile containing the deploy log.
#######################################
function azure_log::fetch_log_file() {
  if civiform_mode::is_test; then
    return 0
  fi

  if [[ -n "${1}" ]]; then
    export LOG_TEMPFILE="${1}"
  else
    export LOG_TEMPFILE="$(mktemp)"
  fi

  az storage blob download \
    --auth-mode login \
    --account-name "${AZURE_LOG_STORAGE_ACCOUNT_NAME}" \
    --container-name "${AZURE_LOG_CONTAINER_NAME}" \
    --name "${AZURE_LOG_FILE_NAME}" \
    --file "${LOG_TEMPFILE}"
}

#######################################
# Uploads the deploy log to Azure and removes the local tempfile.
# Arguments:
#   1: (optional) filepath for log file
# Globals read:
#   AZURE_LOG_STORAGE_ACCOUNT_NAME
#   AZURE_LOG_CONTAINER_NAME
#   AZURE_LOG_FILE_NAME
#   LOG_TEMPFILE
#######################################
function azure_log::upload_log_file() {
  if civiform_mode::is_test; then
    return 0
  fi

  if [[ -n "${1}" ]]; then
    LOG_TEMPFILE="${1}"
  fi

  echo "Uploading deployment log file..."
  az storage blob upload \
    --auth-mode login \
    --account-name "${AZURE_LOG_STORAGE_ACCOUNT_NAME}" \
    --container-name "${AZURE_LOG_CONTAINER_NAME}" \
    --name "${AZURE_LOG_FILE_NAME}" \
    --file "${LOG_TEMPFILE}" \
    --overwrite "true"
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
  if civiform_mode::is_test; then
    return 0
  fi

  az storage blob exists \
    --auth-mode login \
    --account-name "${AZURE_LOG_STORAGE_ACCOUNT_NAME}" \
    --container-name "${AZURE_LOG_CONTAINER_NAME}" \
    --name "${AZURE_LOG_FILE_NAME}" \
    | grep -q true
}

#######################################
# Check role assignments and grants them if they don't exist
# Globals read:
#   AZURE_RESOURCE_GROUP
#   AZURE_SUBSCRIPTION
#   AZURE_LOG_STORAGE_ACCOUNT_NAME
#######################################
function azure_log::ensure_log_role_assignments() {
  if civiform_mode::is_test; then
    return 0
  fi

  echo "Granting current user access to deploy log storage account..."
  storage::assign_storage_account_contributor_role_to_user "${AZURE_RESOURCE_GROUP}"
  azure::ensure_role_assignment \
    "${AZURE_RESOURCE_GROUP}" \
    "Storage Blob Data Contributor" \
    "/subscriptions/${AZURE_SUBSCRIPTION}/resourceGroups/${AZURE_RESOURCE_GROUP}/providers/Microsoft.Storage/storageAccounts/${AZURE_LOG_STORAGE_ACCOUNT_NAME}"
  echo "Done granting current user access deploy log storage account."
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
  if civiform_mode::is_test; then
    return 0
  fi

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

  azure_log::ensure_log_role_assignments
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
