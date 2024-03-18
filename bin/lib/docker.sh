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
# Runs docker compose with the browser test development settings.
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
# Runs docker compose with the browser test local development settings.
# Arguments:
#   @: arguments for compose
#######################################
function docker::compose_browser_test_dev_local() {
  docker compose \
    -f docker-compose.yml \
    -f browser-test/browser-test-compose.yml \
    -f browser-test/browser-test-compose.dev.yml \
    -f browser-test/browser-test-compose.dev_local.yml \
    "$@"
}

#######################################
# Runs docker compose up to bring up the Python-based container
# needed for env-var-docs. Additionally, installs dependencies
# and installs (or updates, if the container is already running)
# the env-var-docs parser-package.
#######################################
function docker::compose_env_var_docs_up() {
  docker compose \
    -f env-var-docs/docker-compose.env-var-docs.yml \
    up -d
  docker exec civiform-vars-parser-package env-var-docs/update-container-parser-package
}

#######################################
# Runs docker compose down to tear down the civiform-vars-parser-package container.
#######################################
function docker::compose_env_var_docs_down() {
  docker compose \
    -f env-var-docs/docker-compose.env-var-docs.yml \
    down --remove-orphans
}

#######################################
# Executes a shell command in the running civiform-vars-parser-package
# container. Passes in a superset of environment variables used by the various
# env-var-docs commands. Before running the command, checks if parser-package
# has any updates, and if so, reinstalls the package.
# Arguments:
#   @: command to run
#######################################
function docker::run_env_var_docs_command() {
  docker exec civiform-vars-parser-package env-var-docs/update-container-parser-package

  # If we're running in a tty, use -t so we get pretty colors
  TTY_FLAG=""
  if [[ -t 0 ]]; then
    TTY_FLAG="-t"
  fi

  docker exec \
    ${TTY_FLAG} \
    -e APPLICATION_CONF_PATH="${APPLICATION_CONF_PATH:-server/conf/application.conf}" \
    -e ENV_VAR_DOCS_PATH="${ENV_VAR_DOCS_PATH:-server/conf/env-var-docs.json}" \
    -e LOCAL_OUTPUT="${LOCAL_OUTPUT:-true}" \
    -e RELEASE_VERSION="${RELEASE_VERSION}" \
    -e GITHUB_ACCESS_TOKEN="${GITHUB_ACCESS_TOKEN}" \
    -e TARGET_REPO="${TARGET_REPO}" \
    -e TARGET_PATH="${TARGET_PATH}" \
    civiform-vars-parser-package "$@"
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
# the dev environment. Use a special project name for env-var-docs, since
# we run this from the civiform directory and want to have it be a separate
# project.
# Globals:
#   DOCKER_NETWORK_NAME
#   COMPOSE_PROJECT_NAME
#######################################
function docker::set_project_name_env_var_docs() {
  export COMPOSE_PROJECT_NAME="env-var-docs"
  export DOCKER_NETWORK_NAME="env-var-docs_default"
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

#######################################
# Runs sbt command inside dev civiform container. The container should be
# already running.
# Arguments:
#   The function takes optional params
#   1. The port to connect the debugger to
#   2. The command to run.
# If no command to run is passed sbt starts in interactive shell mode.
# Globals:
#   COMPOSE_PROJECT_NAME
#######################################
function docker::dev_and_test_server_sbt_command() {

  local server_container_name="${COMPOSE_PROJECT_NAME}-civiform-1"
  local server_container_ip=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $server_container_name)

  # -Dsbt.offline tells sbt to run in "offline" mode and not re-download dependancies.
  docker exec -it $server_container_name ./entrypoint.sh -jvm-debug "$server_container_ip:$1" -Dsbt.offline $2
}

function docker::check_disk_space() {
  # TODO: Figure out the right way to do this on Linux.
  # Should be easier because it uses the actual file system.
  # Note that this is not really accurate for MacOS because it's
  # summing up a list of things that might have some overlap, but it's
  # close enough for detecting if we're close to running out of space.
  settings_json="${HOME}/Library/Group Containers/group.com.docker/settings.json"
  if [[ -f "${settings_json}" && "$(uname -s)" == "Darwin" ]]; then
    max_size=$(jq '.diskSizeMiB' "$settings_json" | awk '{print $1 * 1024 * 1024}')
    used=0
    while IFS= read -r size; do
      # Extract size and unit from the size string
      if [[ $size =~ ([0-9.]+)([A-Z]+) ]]; then
        size_value="${BASH_REMATCH[1]}"
        unit="${BASH_REMATCH[2]}"
        bytes=$(convert_to_bytes $size_value $unit)
        # Accumulate using awk to handle large sums and floating-point values
        used=$(echo $used $bytes | awk '{print $1 + $2}')
      fi
    done < <(docker system df --format '{{.Size}}')
    percentage_used=$(awk -v used="$used" -v max_size="$max_size" 'BEGIN {printf "%.0f", (used / max_size * 100)}')
    if [[ $percentage_used -gt 90 ]]; then
      echo "Warning: Docker is nearly out of disk space. Current usage estimate: ${percentage_used}%. Would you like to run 'docker image prune' to free up space? [Y/n] "
      read answer
      if [[ "${answer}" =~ ^([Yy]|[yY][eE][sS])$ ]] || [[ -z "${answer}" ]]; then
        docker image prune -f
      fi
    fi
  fi
}

# Uses awk in order to do floating point math
function convert_to_bytes() {
  local size=$1
  local unit=$2
  echo | awk -v size="$size" -v unit="$unit" '{
        if (unit == "KB") size *= 1024;
        else if (unit == "MB") size *= 1024^2;
        else if (unit == "GB") size *= 1024^3;
        else if (unit == "B") size = size;
        else {print "Unknown unit: " unit; exit 1}
        print size;
    }'
}
