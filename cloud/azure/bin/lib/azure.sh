#! /usr/bin/env bash

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
    --query "[].name" \
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
    --query "[].name" \
    -o tsv
}

#######################################
# Set base urls for app service primary and canary slot. For now, this
# step must be done via CLI because Terraform doesn't support the
# slot settings feature. 
# See https://github.com/hashicorp/terraform-provider-azurerm/pull/12809
# Arguments:
#   1. The resource group name 
#######################################
function azure::set_app_base_urls() {
  local APP_NAME="$(azure::get_app_name "${1}")"
  local PRIMARY_URL="$(azure::get_primary_url "${APP_NAME}")"
  local CANARY_URL="$(azure::get_canary_url "${APP_NAME}")"

  echo "Setting base URL for primary slot to "${PRIMARY_URL}""
  az webapp config appsettings set \
    --resource_group "${1}" \
    --name "${APP_NAME}" \
    --slot-settings "BASE_URL=${PRIMARY_URL}"

  echo "Setting base URL for canary slot to "${CANARY_URL}""
  az webapp config appsettings set \
    --resource_group "${1}" \
    --name "${APP_NAME}" \
    --slot "canary"
    --slot-settings "BASE_URL=${CANARY_URL}"

}

