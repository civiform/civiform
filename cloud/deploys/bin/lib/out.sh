#!/usr/bin/env bash

#######################################
# Print to stderr.
# Arguments:
#   @: the argument array is printed to stderr
#######################################
function out::error() {
  echo "${@}" >&2
}
