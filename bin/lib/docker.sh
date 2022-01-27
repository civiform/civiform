#! /usr/bin/env bash

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
function docker::do_duckerhub_login() {
  echo $DOCKER_HUB_ACCESS_TOKEN \
    | docker login \
      --username $DOCKER_HUB_USERNAME \
      --password-stdin docker.io
}
