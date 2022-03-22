#! /usr/bin/env bash

#######################################
# Generates terraform variable files and runs terraform init and apply.
# Also initializes the storage bucket for tfstate if it's not setup yet.
# Globals:
#   TERRAFORM_TEMPLATE_DIR
#   BACKEND_VARS_FILENAME
#   TF_VAR_FILE
#######################################
function terraform::perform_apply() {
  cloud/azure/bin/setup_tf_shared_state \
    "${TERRAFORM_TEMPLATE_DIR}/${BACKEND_VARS_FILENAME}"

  terraform \
    -chdir="${TERRAFORM_TEMPLATE_DIR}" \
    init \
    -backend-config="${BACKEND_VARS_FILENAME}"

  terraform \
    -chdir="${TERRAFORM_TEMPLATE_DIR}" \
    apply \
    -var-file="${TF_VAR_FILE}"
}
