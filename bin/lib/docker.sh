#! /usr/bin/env bash

#######################################
# Starts a bash shell in the civiform-shell container
#######################################
function docker::run_shell_container() {
  # Allocate a TTY for better output even though not strictly needed.
  docker-compose \
    -f docker-compose.yml \
    -f docker-compose.dev.yml \
    -f docker-compose.shell.yml \
    up civiform \
    --no-deps \
    -d
}

#######################################
# Executes a bash command in the running civiform-shell container
# Arguments:
#   @: command to run
#######################################
function docker::run_shell_command() {
  # Allocate a TTY for better output even though not strictly needed.
  docker exec -it civiform-shell "$@"
}

#######################################
# Stops the civiform-shell container
#######################################
function docker::stop_shell_container() {
  # Allocate a TTY for better output even though not strictly needed.
  docker-compose \
    -f docker-compose.yml \
    -f docker-compose.dev.yml \
    -f docker-compose.shell.yml \
    stop civiform
}

#######################################
# Runs a command in the civiform-dev container using the default sbt entrypoint
# and volume mounts for sbt caching.
# Arguments:
#   @: optional arguments passed to sbt
#######################################
function docker::run_dev_sbt_command() {
  # Allocate a TTY for better output even though not strictly needed.
  docker run -it \
    --network test-support_default \
    -v "$(pwd)/universal-application-tool-0.0.1:/usr/src/universal-application-tool-0.0.1" \
    -v "$(pwd)/sbt_cache/coursier:/root/.cache/coursier" \
    -v ~/.sbt:/root/.sbt \
    -v ~/.ivy:/root/.ivy2 \
    civiform-dev \
    $@
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
