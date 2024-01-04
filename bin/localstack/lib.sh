#! /usr/bin/env bash

APPLICANT_BUCKET_NAME="civiform-local-s3"
PUBLIC_BUCKET_NAME="civiform-local-s3-public"

#######################################
# Run an AWS CLI command against Localstack.
# If a second argument is passed for the endpoint, the function assumes
# the command is running in the browser test environment.
# Arguments:
#   1: The command to run.
# Globals:
#   DOCKER_NETWORK_NAME
#######################################
function localstack::run_command() {
  local localstack_command="${1}"

  echo "Running localstack command:"
  echo "  aws-cli ${localstack_command}"

  # The command variable is referenced without quotes to allow word splitting.
  docker run --rm \
    --network "${DOCKER_NETWORK_NAME}" \
    -e "AWS_DEFAULT_REGION=us-west-2" \
    -e "AWS_ACCESS_KEY_ID=test" \
    -e "AWS_SECRET_ACCESS_KEY=test" \
    amazon/aws-cli \
    --endpoint-url="http://localhost.localstack.cloud:4566" \
    $localstack_command
}
