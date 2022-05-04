#!/usr/bin/env bash

#######################################
# Check if variable is both a boolean and true.
# Exits with error message and status code 2 if variable is not a boolean.
# Arguments:
#   1: the variable name
#######################################
function truth::is_enabled() {
  truth::assert_is_boolean "${1}"

  [[ "${!1}" == "true" ]]
}

#######################################
# Check if variable is both a boolean and false.
# Exits with error message and status code 2 if variable is not a boolean.
# Arguments:
#   1: the variable name
#######################################
function truth::is_disabled() {
  ! truth::is_enabled "${1}"
}

#######################################
# Set boolean variable to true.
# Arguments:
#   1: the variable name
#######################################
function truth::enable() {
  truth::assert_is_boolean "${1}"

  eval "${1}=true"
}

#######################################
# Set boolean variable to false.
# Arguments:
#   1: the variable name
#######################################
function truth::disable() {
  truth::assert_is_boolean "${1}"

  eval "${1}=false"
}

#######################################
# Declare a variable as true.
# Arguments:
#   1: the variable name
#######################################
function truth::declare_var_true() {
  truth::assert_not_set "${1}" \
    "Cannot declare variable '${1}' it is already set."

  eval "${1}=true"
}

#######################################
# Declare a variable as false.
# Arguments:
#   1: the variable name
#######################################
function truth::declare_var_false() {
  truth::assert_not_set "${1}" \
    "Cannot declare variable '${1}' it is already set."

  eval "${1}=false"
}

#######################################
# Assert that a variable is set and return status code 2 if it is not.
# Arguments:
#   1: the variable name
#   2: the error message to print if the variable is not set
#######################################
function truth::assert_is_set() {
  if [[ -z ${!1+x} ]]; then
    out::error "${2}"
    exit 2
  fi
}

#######################################
# Assert that a variable is not set and return status code 2 if it is.
# Arguments:
#   1: the variable name
#   2: the error message to print if the variable is already set
#######################################
function truth::assert_not_set() {
  if [[ ! -z ${!1+x} ]]; then
    out::error "${2}"
    exit 2
  fi
}

#######################################
# Assert that a variable contains 'true' or 'false'
# Arguments:
#   1: the variable name
#######################################
function truth::assert_is_boolean() {
  local var_name="${1}"
  local value="${!var_name}"

  truth::assert_is_set "${var_name}" \
    "Expected ${var_name} to be a boolean but it was unset." \
    "Declare it or check spelling."

  if [[ "${value}" == "true" ]]; then
    return 0
  elif [[ "${value}" == "false" ]]; then
    return 0
  else
    out::error "Expected variable '${var_name}' to be a boolean" \
      "but it contained '${value}'"
    exit 2
  fi
}
