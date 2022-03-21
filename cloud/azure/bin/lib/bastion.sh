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

#######################################
# Get the command to restore db data.
# More details on the psql version # https://www.postgresql.org/download/linux/ubuntu/
# the pg_restore is symlinked to that via a pg_wrapper
# Arguments:
#   1: the postgres host to connect to 
#   2: the vaultname where secrets are stored
#   3: path of the dump data
#######################################
function bastion::get_pg_restore_command() {
  local DB_PASSWORD=$(bastion::get_pg_password "${2}")
  echo "export DEBIAN_FRONTEND='noninteractive'; \
    sudo sh -c 'echo \"deb http://apt.postgresql.org/pub/repos/apt \
      \$(lsb_release -cs)-pgdg main\" > /etc/apt/sources.list.d/pgdg.list'; \
    wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc \
      | sudo apt-key add -; \
    yes | sudo apt-get update > /dev/null; \
    yes | sudo apt-get install postgresql-14 > /dev/null; \
    PGPASSWORD='${DB_PASSWORD}' /usr/lib/postgresql/14/bin/pg_restore \
      -U psqladmin@${1} \
      -d postgres \
      -h ${1} \
      -a < ${3}"
}

#######################################
# Copy a file to the bastion 
# Arguments:
#   1: the ip of the vm you will connect to
#   2: the key name to use to connect to
#   3: location of the file you want to copy
#   4: destination location on the bastion 
#######################################
function bastion::scp_to_bastion() {
  scp -i "${2}" "${3}" "adminuser@${1}:${4}"
}

#######################################
# Add ssh keys to bastion
# Arguments:
#   1: the resource group name
#   2: the key file to create and add to host
#######################################
function bastion::setup_bastion_ssh() {
  echo "Add my ip address to the security group for ssh"
  bastion::allow_ip_security_group "${1}"

  if ! [[ -f "${2}" ]]; then
    echo "Creating a new ssh key"
    ssh-keygen -q -t rsa -b 4096 -N '' -f "${2}"
  fi

  echo "Adding the public key to the bastion vm"
  bastion::update_bastion_ssh_keys "${1}" "${2}"
}

#######################################
# Remove ssh keys from bastion
# Arguments:
#   1: the resource group name
#   2: the key file to remove
#######################################
function bastion::teardown_bastion_ssh() {
  echo "Removing the ssh keys from your machine"
  bastion::remove_ssh_key "${2}"

  echo "Update bastion security group to deny all ssh request"
  bastion::deny_ip_security_group "${1}"
}
