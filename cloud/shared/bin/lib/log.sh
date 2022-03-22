#! /usr/bin/env bash

#######################################
# Write the last successfully deployed image tag to stdout, not including
# the tag passed to the function.
# Arguments:
#   1: The tag to ignore when fiding the previously successful tag.
#######################################
function log::get_last_deployed_image_tag() {
  if [[ -z "${1}" ]]; then
    out::error "Must specify currently deployed tag tag."
    exit 1
  fi

  grep 'DEPLOY SUCCESS' "${LOG_TEMPFILE}" \
    | grep -v "${1}" \
    | tail -n1 \
    | awk '{ print $4 }'
}

#######################################
# Record a deployment success event in the log.
# Args:
#   1: The tag of the deployed server binary.
#   2: Identifier for the user who initiated the command.
# Globals read:
#   LOG_TEMPFILE
#######################################
function log::deploy_succeeded() {
  log::ensure_log_file_fetched
  log::check_log_entry_args "$@"

  echo "$(log::timestamp) DEPLOY SUCCESS ${1} ${2}" >> "${LOG_TEMPFILE}"
}

#######################################
# Record a rollback failure event in the log.
# Args:
#   1: The tag of the server binary to roll back to.
#   2: Identifier for the user who initiated the command.
# Globals read:
#   LOG_TEMPFILE
#######################################
function log::rollback_failed() {
  log::ensure_log_file_fetched
  log::check_log_entry_args "$@"

  echo "$(log::timestamp) ROLLBACK FAILED ${1} ${2}" >> "${LOG_TEMPFILE}"
}

#######################################
# Record a rollback success event in the log.
# Args:
#   1: The tag of the server binary to roll back to.
#   2: Identifier for the user who initiated the command.
# Globals read:
#   LOG_TEMPFILE
#######################################
function log::rollback_succeeded() {
  log::ensure_log_file_fetched
  log::check_log_entry_args "$@"

  echo "$(log::timestamp) ROLLBACK SUCCESS ${1} ${2}" >> "${LOG_TEMPFILE}"
}

#######################################
# Record a deployment failure event in the log.
# Args:
#   1: The tag of the deployed server binary.
#   2: Identifier for the user who initiated the command.
# Globals read:
#   LOG_TEMPFILE
#######################################
function log::deploy_failed() {
  log::ensure_log_file_fetched
  log::check_log_entry_args "$@"

  echo "$(log::timestamp) DEPLOY FAILED ${1} ${2}" >> "${LOG_TEMPFILE}"
}

#######################################
# Record the initialization event in the log.
# Args:
#   1: Identifier for the user who initiated the command.
# Globals read:
#   LOG_TEMPFILE
#######################################
function log::initialized() {
  log::ensure_log_file_fetched
  log::validate_token "${1}"

  echo "$(log::timestamp) INITIALIZED SUCCESS ${1}" >> "${LOG_TEMPFILE}"
}

#######################################
# Check that at least two arguments exist and do not contain spaces.
# Writes an error message to stderr and exits with status 1 upon failure.
# Args:
#   1..2: Strings suitable as tokens in the deployment log.
#######################################
function log::check_log_entry_args() {
  for i in {1..2}; do
    if [[ -z "${!i}" ]]; then
      out::error "Missing arguments to deployment log function."
      exit 1
    fi

    log::validate_token "${!i}"
  done

  if [[ "$#" -gt 2 ]]; then
      out::error "Too many arguments to deployment log function."
      exit 1
  fi
}

#######################################
# Validate a token for suitability in the deployment log.
# Checks to ensure the token does not contain spaces.
# Args:
#   1: The token to validate.
#######################################
function log::validate_token() {
  local SPACE_MATCHING_REGEX="[[:space:]]+"
  if [[ ${1} =~ $SPACE_MATCHING_REGEX ]]; then
    out::error "Invalid arg for deployment log entry: '${1}'"
    exit 1
  fi
}

#######################################
# Check that the deployment log tempfile exists.
# Writes an error message to stderr and exits with status 1 upon failure.
# Globals read:
#   LOG_TEMPFILE
#######################################
function log::ensure_log_file_fetched() {
  if ! [[ -f "${LOG_TEMPFILE}" ]]; then
     out::error "Attempted to write to logfile but none found."
     exit 1
  fi
}

#######################################
# Writes the current timestamp in unix seconds to stdout.
#######################################
function log::timestamp() {
  date +%s
}

