#! /usr/bin/env bash

#######################################
# Starts a bash shell in the civiform-shell container
#######################################
function docker::run_shell_container() {
  # Start up the "civiform" service with the shell overrides.
  # Use the compose project "civiform-shell".
  docker-compose \
    -f docker-compose.yml \
    -f docker-compose.dev.yml \
    --profile shell \
    up civiform-shell \
    --no-deps \
    --wait \
    -d
}

#######################################
# Executes a bash command in the running civiform-shell container
# Arguments:
#   @: command to run
#######################################
function docker::run_shell_command() {
  # Sends a command to the running "civiform-shell" container.
  docker exec -it civiform-shell "$@"
}

#######################################
# Stops the civiform-shell container
#######################################
function docker::stop_shell_container() {
  # Stop the compose project "civiform-shell".
  docker-compose \
    -f docker-compose.yml \
    -f docker-compose.dev.yml \
    --profile shell \
    stop civiform-shell
}

#######################################
# Deletes the civiform-shell container
#######################################
function docker::remove_shell_container() {
  # Deletes the containers for the "civiform-shell" project.
  docker-compose \
    -f docker-compose.yml \
    -f docker-compose.dev.yml \
    --profile shell \
    down civiform-shell
}

#######################################
# Starts the services needed for the unit test suite or exits
# successfully if they are already up.
#######################################
function docker::ensure_unit_test_env() {
  docker-compose \
    -f test-support/unit-test-docker-compose.yml \
    up \
    -d
}

#######################################
# Stops the services needed for the unit test suite
#######################################
function docker::remove_unit_test_env() {
  docker-compose \
    -f test-support/unit-test-docker-compose.yml \
    down
}

#######################################
# Set DOCKER_NETWORK_NAME to the network name used by the
# dev environment.
# Globals:
#   DOCKER_NETWORK_NAME
#######################################
function docker::set_network_name_dev() {
  export DOCKER_NETWORK_NAME="$(basename $(pwd))_default"
}

#######################################
# Set DOCKER_NETWORK_NAME to the network name used by the
# browser tests.
# Globals:
#   DOCKER_NETWORK_NAME
#######################################
function docker::set_network_name_browser_tests() {
  export DOCKER_NETWORK_NAME="browser-test_default"
}

#######################################
# Login to Docker Hub.
# Globals:
#   DOCKER_HUB_ACCESS_TOKEN
#   DOCKER_HUB_USERNAME
#######################################
function docker::do_dockerhub_login() {
  echo $DOCKER_HUB_ACCESS_TOKEN \
    | docker login \
      --username $DOCKER_HUB_USERNAME \
      --password-stdin docker.io
}
