#! /usr/bin/env bash

readonly CIVIFORM_CONTAINER_NAME="civiform/civiform"

#######################################
# Create resource group
# Arguments:
#   1: The resource group name 
#   2: The location of the resource group
#######################################
function azure::create_resource_group() {
  if [[ $(az group exists --name "${1}") == false ]]; then
    az group create --name "${1}" --location "${2}"
  fi
}

#######################################
# Gets the app name for the terraform-managed app service deployment
# Note: This function works assuming there is only one app service
# application in the resource group
# Arguments:
#   1: The resource group name for the terraform deployment
#######################################
function azure::get_app_name() {
  az webapp list --resource-group="${1}" --query "[].name" -o tsv
}

#######################################
# Gets the app name for the terraform-managed app-service deployment
# Note: This function works assuming there is only one app service
# application in the resource group
# Arguments:
#   1: The resource group name for the terraform deployment
#   2. The app service application name
#######################################
function azure::get_primary_url() {
  az webapp show \
    --resource-group "${1}" \
    --name "${2}" \
    --query "defaultHostName" \
    --output tsv
}

#######################################
# Gets the app name for the terraform-managed app-service deployment
# Note: This function works assuming there is only one app service
# application in the resource group
# Arguments:
#   1: The resource group name for the terraform deployment
#   2. The app service application name
#######################################
function azure::get_canary_url() {
  az webapp show \
    --resource-group "${1}" \
    --name "${2}" \
    --slot "canary" \
    --query "defaultHostName" \
    --output tsv
}

#######################################
# Sets a slot setting for app service canary slot. For now, this
# step must be done via CLI because Terraform doesn't support the
# slot settings feature.
# See https://github.com/hashicorp/terraform-provider-azurerm/pull/12809
# Arguments:
#   1. The slot name (either "primary" or "canary")
#   2. The app service app name
#   3. The key for the slot setting (e.g. BASE_URL)
#   4. The value for the slot setting (e.g. https://staging-azure.civiform.dev)
#   5. The resource group name
#######################################
function azure::slot_setting {
  echo "Setting ${3} for ${1} slot to ${4}"
  if [[ "${1}" == "canary" ]] ; then
    az webapp config appsettings set \
      --name "${2}" \
      --slot "canary" \
      --slot-settings "${3}=${4}" \
      --resource-group "${5}"  \
      --output "none" 
  elif [[ "${1}" == "primary" ]] ; then
    az webapp config appsettings set \
      --name "${2}" \
      --slot-settings "${3}=${4}" \
      --resource-group "${5}" \
      --output "none" 
  else 
    echo "${1} is not a valid slot option." >&2
  fi
}

#######################################
# Sets the canary slot to point to a new container tag.
# Arguments:
#   1. The resource group name
#   2. The app service app name
#   3. The new tag version
#######################################
function azure::set_new_container_tag(){
  az webapp config container set \
    --resource-group "${1}" \
    --name "${2}" \
    --slot "canary" \
    --docker-custom-image-name "DOCKER|${CIVIFORM_CONTAINER_NAME}:${3}"
}

#######################################
# Swap the canary slot into production.
# Arguments:
#   1. The resource group name
#   2. The app service app name
#######################################
function azure::swap_deployment_slot() {
  az webapp deployment slot swap --slot "canary" \
    --resource-group "${1}" \
    --name "${2}"
}

#######################################
# Writes the current signed in Azure user to stdout with whitespace replaced
# by underscores for suitability as a token in the deploy log.
#######################################
function azure::get_current_user_id() {
  az ad signed-in-user show --query mail | sed -E 's/ +/_/g'
}
