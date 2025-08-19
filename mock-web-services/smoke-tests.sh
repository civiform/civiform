#! /usr/bin/env bash

# DOC: Simple smoke test of endpoints to verify they return 200.
# DOC:
# DOC: These are not meant to be exhaustive of all scenarios.
# DOC:
# DOC: Start a test server with `python app.py` before running.

function assert_200 {
  local url="${1}"
  local data="${2}"

  local actual_http_code

  if [[ -n "${data}" ]]; then
    actual_http_code=$(curl --silent --output /dev/null --write-out "%{http_code}" --request POST --url "${url}" --data "${data}" --header "Content-Type: application/json")
  else
    actual_http_code=$(curl --silent --output /dev/null --write-out "%{http_code}" --request GET --url "${url}")
  fi

  # ansi color codes
  local green="\e[30;42m"
  local red="\e[30;41m"
  local reset="\e[0m"

  if [[ "200" == "${actual_http_code}" ]]; then
    echo -e "${green} PASS ${reset} ${url}"
  else
    echo -e "${red} FAIL ${reset} Expected HTTP 200 was HTTP ${actual_http_code}"
  fi
}

assert_200 "http://localhost:8000/esri/findAddressCandidates?address=Legit%20Address"
assert_200 "http://localhost:8000/esri/serviceAreaFeatures?geometry=\\{\"y\":100\\}"
assert_200 "http://localhost:8000/api-bridge/health-check"
assert_200 "http://localhost:8000/api-bridge/discovery"
assert_200 "http://localhost:8000/api-bridge/bridge/success" "{\"payload\": {\"accountNumber\": 1111}}"
assert_200 "http://localhost:8000/geojson/data"
