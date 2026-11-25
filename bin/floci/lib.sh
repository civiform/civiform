#! /usr/bin/env bash

APPLICANT_BUCKET_NAME="civiform-local-s3"
PUBLIC_BUCKET_NAME="civiform-local-s3-public"

#######################################
# Run an AWS CLI command against Floci.
# Arguments:
#   1: The command to run.
# Globals:
#   DOCKER_NETWORK_NAME
#######################################
function floci::run_command() {
  local floci_command="${1}"

  echo "Running Floci command:"
  echo "  aws-cli ${floci_command}"

  # The command variable is referenced without quotes to allow word splitting.
  docker run --rm \
    --network "${DOCKER_NETWORK_NAME}" \
    -e "AWS_DEFAULT_REGION=us-west-2" \
    -e "AWS_ACCESS_KEY_ID=test" \
    -e "AWS_SECRET_ACCESS_KEY=test" \
    amazon/aws-cli \
    --endpoint-url="http://localhost.floci.io:4566" \
    $floci_command
}
