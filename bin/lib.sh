#! /usr/bin/env bash

# This file loads shared code for most shell scripts. All scripts
# in the base bin/ directory should begin by sourcing it.

if [[ "${KEEP_ORIGINAL_PWD}" != "true" ]]; then
  pushd $(git rev-parse --show-toplevel) >/dev/null
fi

set -e
set +x

# Optionally alias docker to podman if user wishes.
if [[ "${USE_PODMAN_FOR_CIVIFORM}" == "1" || "${USE_PODMAN_FOR_CIVIFORM}" == "true" ]]; then
  shopt -s expand_aliases
  alias docker=podman
fi

readonly LIB_DIR="${BASH_SOURCE%/*}/lib"

# Control the name that `docker compose` preprends resources with.
export COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:=$(basename $(pwd))}"
export DOCKER_NETWORK_NAME="${DOCKER_NETWORK_NAME:=${COMPOSE_PROJECT_NAME}_default}"

if [[ "${SOURCED_LIB}" != "true" ]]; then
  source "${LIB_DIR}/out.sh"
  source "${LIB_DIR}/truth.sh"
  source "${LIB_DIR}/docker.sh"
  source "${LIB_DIR}/emulators.sh"

  SOURCED_LIB="true"
fi

SBT_VERSION=$(grep 'sbt.version' ${LIB_DIR}/../../server/project/build.properties)
export SBT_VERSION=${SBT_VERSION#sbt.version=}
