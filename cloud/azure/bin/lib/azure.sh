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
  az webapp list --resource-group="${}" --query "[].name" -o tsv
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
    --resource-group "${1}"
    --name "${2}" \
    --query "defaultHostname" \
    -o tsv
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
    --resource-group "${1}"
    --name "${2}" \
    --slot "canary" \
    --query "defaultHostname" \
    -o tsv
}

#######################################
# Set base urls for app service canary slot. For now, this
# step must be done via CLI because Terraform doesn't support the
# slot settings feature.
# See https://github.com/hashicorp/terraform-provider-azurerm/pull/12809
# Arguments:
#   1. The resource group name
#   2. The app service app name
#######################################
function azure::set_app_base_url_canary() {
  local CANARY_URL="$(azure::get_canary_url "${2}")"

  echo "Setting base URL for canary slot to "${CANARY_URL}""
  az webapp config appsettings set \
    --resource_group "${1}" \
    --name "${2}" \
    --slot "canary"
    --slot-settings "BASE_URL=${CANARY_URL}"
}


#######################################
# Set base urls for app service primary slot. For now, this
# step must be done via CLI because Terraform doesn't support the
# slot settings feature.
# See https://github.com/hashicorp/terraform-provider-azurerm/pull/12809
# Arguments:
#   1. The resource group name
#   2. The app service app name
#   3. The custom hostname for the primary slot (e.g. https://staging-azure.civiform.dev)
#######################################
function azure::set_app_base_url_primary() {
  echo "Setting base URL for canary slot to "${CANARY_URL}""
  az webapp config appsettings set \
    --resource_group "${1}" \
    --name "${2}" \
    --slot-settings "BASE_URL=${3}"
}

#######################################
# Sets the canary slot to point to a new container tag
# Arguments:
#   1. The resource group name
#   2. The app service app name
#   3. The new tag version
#######################################
function azure::set_new_container_tag(){
  az webapp config container set \
    --resource_group "${1}" \
    --name "${2}" \
    --slot "canary"
    --docker-custom-image-name "DOCKER|${CIVIFORM_CONTAINER_NAME}:${2}"
}
