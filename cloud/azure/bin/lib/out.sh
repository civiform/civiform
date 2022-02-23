#!/usr/bin/env bash

#######################################
# Print to stderr.
# Arguments:
#   @: the argument array is printed to stderr
#######################################
function out::error() {
  echo "${@}" >&2
}

#######################################
# Print two strings in left-justified columns.
# Arguments:
#   1: Width of the left column.
#   2: String in the left column.
#   3: String in the right column.
#######################################
function out::column_print() {
  printf "%-${1}s %s\n" "${2}" "${3}"
}
