#! /usr/bin/env bash

#######################################
# Get the ip for the bastion VM 
# Arguments:
#   1: the resource group name
#######################################
function bastion::get_vm_ip() {
  az network public-ip show \
    -g "${1}" \
    -n "${1}-ip" \
    --query "ipAddress" | tr -d '"'
}

#######################################
# Get the postgres host for the psql server
# Arguments:
#   1: the resource group name
#   2: the name of the database
#######################################
function bastion::get_postgres_host() {
  az postgres server show \
    -g "${1}" \
    -n "${2}" \
    --query "fullyQualifiedDomainName" \
    | tr -d '"'
}

#######################################
# Remove the ssh key we created after you connect 
# Arguments:
#   1: the key name to remove
#######################################
function bastion::remove_ssh_key() {
  rm "${1}"
  rm "${1}".pub
}

#######################################
# Connect to the bastion
# Arguments:
#   1: the ip of the vm you will connect to
#   2: the key name to use to connect to
#   3: command to run after ssh'ing
#######################################
function bastion::bastion_ssh_connect() {
  ssh -i "${2}" "adminuser@${1}" "${3}"
}

#######################################
# Update the bastion to allow ssh given a public key
# Arguments:
#   1: the resource group name
#   2: the key name to use to connect to
#######################################
function bastion::update_bastion_ssh_keys() {
  az vm user update \
    -u adminuser \
    -g "${1}" \
    -n "${1}-bstn-vm" \
    --ssh-key-value "$(< ${2}.pub)"
}

#######################################
# Get the command to connect to postgres
# Arguments:
#   1: the postgres host to connect to 
#######################################
function bastion::get_connect_to_postgres_command() {
  # look at what shubha has done to get the password from the secret store
  
  echo "export DEBIAN_FRONTEND='noninteractive'; \
    yes | sudo apt-get update > /dev/null; \
    yes | sudo apt-get install postgresql-client > /dev/null; \
    PGPASSWORD='${db_password}' psql -h ${1} -d postgres -U psqladmin@${1}"
}
