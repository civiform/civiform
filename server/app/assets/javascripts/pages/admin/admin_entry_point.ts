/**
 * Entry point for admin bundle. Admin bundle is served on all pages that are
 * accessed by CiviForm and Program admins.
 */
import '@/components/shared/modal'
import * as main from '@/main'
import * as radio from '@/radio'
import * as toast from '@/toast'
import * as adminApiKeys from '@/admin_api_keys'
import * as adminApplicationView from '@/admin_application_view'
import * as legacyAdminPredicates from '@/admin_predicate_configuration'
import * as adminPredicateEdit from '@/admin_predicate_edit'
import * as adminProgramImage from '@/admin_program_image'
import * as legacyAdminProgramImage from '@/legacy_admin_program_image'
import * as adminPrograms from '@/admin_programs'
import * as adminProgramStatusesView from '@/admin_program_statuses_view'
import * as adminSettingsView from '@/admin_settings_view'
import * as adminValidation from '@/admin_validation'
import * as apiDocs from '@/api_docs'
import * as devIcons from '@/dev_icons'
import * as map from '@/mapquestion/map'
import * as modal from '@/modal'
import * as northStarModal from '@/north_star_modal'
import * as questionBank from '@/questionBank'
import PreviewController, * as preview from '@/preview'
import {SessionTimeoutHandler} from '@/session'
import * as enumerator from '@/enumerator'
import * as phoneNumber from '@/phone'
import * as adminQuestionEdit from '@/admin_question_edit'
import * as adminExportView from '@/admin_export_view'
import * as adminImportView from '@/admin_import_view'
import * as trustedIntermediaryController from '@/admin_trusted_intermediary_list'
import * as legacyFileUpload from '@/legacy_file_upload'
import * as azureUpload from '@/azure_upload'
import htmx from '@/htmx'

import {AdminProgramApiBridge} from '@/admin_program_api_bridge'
import {AlertContainer} from '@/global/shared/alert_container'
import {featureFlags} from '@/global/shared/feature_flags'
import {FormValidation} from '@/global/shared/form_validation'

// Ensure the object path exists
window.app = window.app || {}
window.app.data = window.app.data || {}
window.app.scripts = window.app.scripts || {}

// Attach so the specific page can manage init
window.app.scripts.AdminProgramApiBridge = AdminProgramApiBridge
window.app.scripts.AdminPredicateEdit = adminPredicateEdit.AdminPredicateEdit

window.addEventListener('load', () => {
  initializeEverything()
  htmx.on('htmx:afterSettle', () => {
    afterSettle()
  })
})

function initializeEverything(): void {
  const AZURE_ADMIN_FILEUPLOAD_FORM_ID = 'image-file-upload-form'

  main.init()
  radio.init()
  toast.init()
  AlertContainer.init()
  adminApiKeys.init()
  adminApplicationView.init()
  legacyAdminPredicates.init()
  adminPredicateEdit.init()
  if (featureFlags().isFileUploadQuestionImprovementsEnabled) {
    adminProgramImage.init()
  } else {
    legacyAdminProgramImage.init()
  }
  adminPrograms.init()
  adminProgramStatusesView.init()
  adminSettingsView.init()
  if (featureFlags().isAdminUiMigrationScEnabled) {
    // Thymeleaf pages opt in via data-form-type and use the Constraint
    // Validation API driven FormValidation.
    FormValidation.init()
  } else {
    // Legacy j2html question edit page.
    adminValidation.init()
  }
  apiDocs.init()
  devIcons.init()
  modal.init()
  northStarModal.init()
  questionBank.init()
  preview.init()
  enumerator.init()
  phoneNumber.init()
  adminQuestionEdit.init()
  adminExportView.init()
  adminImportView.init()
  trustedIntermediaryController.init()
  legacyFileUpload.init()
  azureUpload.init(AZURE_ADMIN_FILEUPLOAD_FORM_ID)
  SessionTimeoutHandler.init()
}

function afterSettle(): void {
  PreviewController.updateListeners()
  map.init()
  enumerator.updateListeners()
}
