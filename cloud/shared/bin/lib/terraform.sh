#! /usr/bin/env bash

#######################################
# Generates terraform variable files and runs terraform init and apply.
# Also initializes the storage bucket for tfstate if it's not setup yet.
# Globals:
#   TERRAFORM_TEMPLATE_DIR
#   BACKEND_VARS_FILENAME
#   TF_VAR_FILENAME
#######################################
function terraform::perform_apply() {
  if [[ "${CIVIFORM_MODE}" == "dev" ]]; then
    terraform -chdir="${TERRAFORM_TEMPLATE_DIR}" init -upgrade
  else
    "cloud/${CIVIFORM_CLOUD_PROVIDER}/bin/setup_tf_shared_state" \
      "${TERRAFORM_TEMPLATE_DIR}/${BACKEND_VARS_FILENAME}"

    terraform \
      -chdir="${TERRAFORM_TEMPLATE_DIR}" \
      init \
      -upgrade \
      -backend-config="${BACKEND_VARS_FILENAME}"
  fi
  
  terraform \
    -chdir="${TERRAFORM_TEMPLATE_DIR}" \
    apply \
    -var-file="${TF_VAR_FILENAME}"
}

#######################################
# Copies the terraform backend_override to backend_override.tf (used to 
# make backend local instead of a shared state for dev deploys)
# Globals:
#   TERRAFORM_TEMPLATE_DIR
#######################################
function terraform::copy_override() {
    cp "${TERRAFORM_TEMPLATE_DIR}/backend_override" \
      "${TERRAFORM_TEMPLATE_DIR}/backend_override.tf"
}
