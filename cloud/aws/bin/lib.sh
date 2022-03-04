#! /usr/bin/env bash

#######################################
# Creates the access key via aws
# Arguments:
#   1. username of the aws user to create access key under
#######################################
function aws::create_access_key() {
  aws iam create-access-key --user-name "${1}"
}

#######################################
# Get the access key id from the create access key command
# Arguments:
#   1. the result json of the create_access_key call
#######################################
function aws::parse_access_key_id() {
  ACCES_KEY_ID_PATTERN="\"AccessKeyId\"\: \"([^\"]+)\""
  if [[ "${1}" =~ $ACCES_KEY_ID_PATTERN ]]; then
    echo "${BASH_REMATCH[1]}"
  else
    echo "no access key id found"
  fi
}

#######################################
# Get the secret key from the create access key command
# Arguments:
#   1. the result json of the create_access_key call
#######################################
function aws::parse_access_secret_key() {
  SECRET_ACCESS_KEY_PATTERN="\"SecretAccessKey\"\: \"([^\"]+)\""
  if [[ "${1}" =~ $SECRET_ACCESS_KEY_PATTERN ]]; then
    echo "${BASH_REMATCH[1]}"
  else
    echo "no secret access key found"
  fi
}

