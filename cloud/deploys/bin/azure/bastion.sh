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
function bastion::bastion_ssh_exec() {
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
# Get pg password from the keyvault
# Arguments:
#   1: the vaultname where secrets are stored
#######################################
function bastion::get_pg_password() {
  az keyvault secret show \
    --name postgres-password \
    --vault-name "${1}" \
    --query value | tr -d '"'
}

#######################################
# Allow the current ip address to ssh onto the bastion
# Arguments:
#   1: the resource group name
#######################################
function bastion::allow_ip_security_group() {
  local MY_IPADDRESS=$(curl -s https://checkip.amazonaws.com)
  az network nsg rule update \
    -g "${1}" \
    --nsg-name "${1}-pblc-nsg" \
    -n "ssh-rule" \
    --access "Allow" \
    --source-address-prefixes "${MY_IPADDRESS}"
}

#######################################
# Deny all ssh request onto the bastion
# Arguments:
#   1: the resource group name
#######################################
function bastion::deny_ip_security_group() {
  az network nsg rule update \
    -g "${1}" \
    --nsg-name "${1}-pblc-nsg" \
    -n "ssh-rule" \
    --access "Deny" \
    --source-address-prefixes "*"
}

#######################################
# Get the command to connect to postgres
# Arguments:
#   1: the postgres host to connect to 
#   2: the vaultname where secrets are stored
#######################################
function bastion::get_connect_to_postgres_command() {
  local DB_PASSWORD=$(bastion::get_pg_password "${2}")
  echo "export DEBIAN_FRONTEND='noninteractive'; \
    yes | sudo apt-get update > /dev/null; \
    yes | sudo apt-get install postgresql-client > /dev/null; \
    PGPASSWORD='${DB_PASSWORD}' psql -h ${1} -d postgres -U psqladmin@${1}"
}
