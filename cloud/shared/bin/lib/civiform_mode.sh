#######################################
# Returns
# Globals:
#  CIVIFORM_MODE
#######################################
function civiform_mode::is_test() {
  [[ "${CIVIFORM_MODE}" == "test" ]]
}

function civiform_mode::use_local_backend() {
  [[ "${USE_LOCAL_BACKEND}" == true ]]
}

function civiform_mode::skip_confirmations() {
  [[ "${SKIP_CONFIRMATIONS}" == true ]]
}
