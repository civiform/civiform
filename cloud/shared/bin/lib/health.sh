#! /usr/bin/env bash

#######################################
# Check if given endpoint is healthy
# Args:
#   1: The url for the healthcheck endpoint
#   2: (Optional) Amount of time to wait before timing out, default
#######################################
function health::wait_for_success() {
  local TIMEOUT=600 # 10 min
  local START_TIME="$(date +%s)"

  if [[ -n "${2}" ]]; then
    TIMEOUT=${2}
  fi

  local DEADLINE="$(($START_TIME + $TIMEOUT))"
  echo "Polling ${1} for successful response. This may take a few minutes"
  local health_status="$(health::get_status "${1}")"

  until [[ "${health_status}" -eq "200" ]]; do
    sleep 10
    local CURRENT_TIME=$(date +%s)

    if (("${CURRENT_TIME}" > "${DEADLINE}")); then
      echo "Deadline exceeded waiting for healthy endpoint" >&2
      exit 1
    fi

    echo "Time elapsed: $((${CURRENT_TIME} - ${START_TIME})) seconds"
    health_status="$(health::get_status "${1}")"
  done
}

#######################################
# Gets the response status code for a request or times out
# Args:
#   1: The url for the healthcheck endpoint
#   2: (Optional) Amount of time to wait before timing out, default
#######################################
function health::get_status() {
  curl --silent --max-time 10 --output /dev/null -w "%{http_code}" "${1}"
}
