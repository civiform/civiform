#! /usr/bin/env bash

#######################################
# Checks if the var-env-docs venv is active
#######################################
function python::env_var_docs_venv_is_active() {
  [[ "$(which python3)" =~ "env-var-docs/venv/bin/python3" ]]
}

#######################################
# Checks if the var-env-docs venv exists
#######################################
function python::env_var_docs_venv_is_installed() {
  [[ -d env-var-docs/venv ]]
}
