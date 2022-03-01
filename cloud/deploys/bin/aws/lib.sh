#! /usr/bin/env bash

ACCES_KEY_ID_PATTERN="\"AccessKeyId\"\: \"(\S+)\""
SECRET_ACCESS_KEY_PATTERN="\"SecretAccessKey\"\: \"(\S+)\""

#######################################
# Creates the access key via aws
# Arguments:
#######################################
function aws::create_access_key() {
  aws iam create-access-key
}

#######################################
# Creates the access key via aws
# Arguments:
#   1. the result json of the create_access_key call
#######################################
function aws::parse_access_key_id() {
  if [[ "${1}" =~ $ACCES_KEY_ID_PATTERN ]]; then
    ${BASH_REMATCH[1]}
  else
    echo "no access key id found"
  fi
}

#######################################
# Creates the access key via aws
# Arguments:
#   1. the result json of the create_access_key call
#######################################
function aws::parse_access_secret_key() {
  if [[ "${1}" =~ $SECRET_ACCESS_KEY_PATTERN ]]; then
    ${BASH_REMATCH[1]}
  else
    echo "no secret access key found"
  fi
}
