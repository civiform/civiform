#! /usr/bin/env bash

#######################################
# Runs docker compose with the base settings.
# Arguments:
#   @: arguments for compose
#######################################
function docker::compose() {
  docker compose -f docker-compose.yml "$@"
}

#######################################
# Runs docker compose with the local development settings.
# Arguments:
#   @: arguments for compose
#######################################
function docker::compose_dev() {
  docker compose -f docker-compose.yml -f docker-compose.dev.yml "$@"
}

#######################################
# Runs docker compose with the unit test settings.
# Arguments:
#   @: arguments for compose
#######################################
function docker::compose_unit_test() {
  docker compose -f test-support/unit-test-docker-compose.yml "$@"
}

#######################################
# Runs docker compose with the unit test local development settings.
# Arguments:
#   @: arguments for compose
#######################################
function docker::compose_unit_test_dev() {
  docker compose \
    -f test-support/unit-test-docker-compose.yml \
    -f test-support/unit-test-docker-compose.dev.yml \
    "$@"
}

#######################################
# Runs docker compose with the browser test settings.
# Arguments:
#   @: arguments for compose
#######################################
function docker::compose_browser_test() {
  docker compose \
    -f docker-compose.yml \
    -f browser-test/browser-test-compose.yml \
    "$@"
}

#######################################
# Runs docker compose with the browser test local development settings.
# Arguments:
#   @: arguments for compose
#######################################
function docker::compose_browser_test_dev() {
  docker compose \
    -f docker-compose.yml \
    -f browser-test/browser-test-compose.yml \
    -f browser-test/browser-test-compose.dev.yml \
    "$@"
}

#######################################
# Starts a bash shell in the civiform-shell container
#######################################
function docker::run_shell_container() {
  # Start up the "civiform" service with the shell overrides.
  # Use the compose project "civiform-shell".
  docker::compose_dev \
    --profile shell \
    up civiform-shell \
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
  docker::compose_dev \
    --profile shell \
    exec -it civiform-shell "$@"
}

#######################################
# Stops the civiform-shell container
#######################################
function docker::stop_shell_container() {
  # Stop the compose project "civiform-shell".
  docker::compose_dev \
    --profile shell \
    stop civiform-shell
}

#######################################
# Deletes the civiform-shell container
#######################################
function docker::remove_shell_container() {
  # Deletes the containers for the "civiform-shell" project.
  docker::compose_dev \
    --profile shell \
    down civiform-shell
}

#######################################
# Set COMPOSE_PROJECT_NAME and DOCKER_NETWORK_NAME to the network name used by
# the dev environment.
# Globals:
#   DOCKER_NETWORK_NAME
#   COMPOSE_PROJECT_NAME
#######################################
function docker::set_project_name_dev() {
  export COMPOSE_PROJECT_NAME="$(basename $(pwd))"
  export DOCKER_NETWORK_NAME="${COMPOSE_PROJECT_NAME}_default"
}

#######################################
# Set COMPOSE_PROJECT_NAME and DOCKER_NETWORK_NAME to the network name used by
# the browser tests.
# Globals:
#   DOCKER_NETWORK_NAME
#   COMPOSE_PROJECT_NAME
#######################################
function docker::set_project_name_browser_tests() {
  export COMPOSE_PROJECT_NAME="$(basename $(pwd))-browser-test"
  export DOCKER_NETWORK_NAME="${COMPOSE_PROJECT_NAME}_default"
}

#######################################
# Set COMPOSE_PROJECT_NAME and DOCKER_NETWORK_NAME to the network name used by
# the unit tests.
# Globals:
#   DOCKER_NETWORK_NAME
#   COMPOSE_PROJECT_NAME
#######################################
function docker::set_project_name_unit_tests() {
  export COMPOSE_PROJECT_NAME="$(basename $(pwd))-test-support"
  export DOCKER_NETWORK_NAME="${COMPOSE_PROJECT_NAME}_default"
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
