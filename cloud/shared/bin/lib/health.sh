#! /usr/bin/env bash

#######################################
# Check if given endpoint is healthy
# Args:
#   1: The url for the healthcheck endpoint
#   2: (Optional) Amount of time to wait before timing out, default
#######################################
function health::wait_for_success() {
    local TIMEOUT=300 # 5 min
    local START_TIME="$(date +%s)"

    if [[-z "${2}"]];
    then
      TIMEOUT=${2}
    fi
    local DEADLINE="$(($START_TIME + $TIMEOUT))"
    echo "Polling ${1} for successful response"
    until [[ health::get_status "${1}" == "200" ]] ;  do
        if (( "$(date +%s)" > "${DEADLINE}" )); then
            echo "Deadline exceeded waiting for healthy endpoint" >&2
            exit 1
        fi
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
