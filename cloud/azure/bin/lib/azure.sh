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

