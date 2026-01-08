/*

This barrel file is solely for backwards compatibility with existing tests.

Do NOT add functions to this file.

*/

export {AdminQuestions} from './admin_questions'
export {AdminPredicates} from './admin_predicates'
export {AdminPrograms} from './admin_programs'
export {AdminProgramStatuses} from './admin_program_statuses'
export {AdminSettings} from './admin_settings'
export {AdminTranslations} from './admin_translations'
export {AdminProgramImage} from './admin_program_image'
export {AdminTIGroups} from './admin_ti_groups'
export {ApplicantFileQuestion} from './applicant_file_question'
export {ApplicantQuestions} from './applicant_questions'
export {ClientInformation, TIDashboard} from './ti_dashboard'
export {clickAndWaitForModal, dismissModal, waitForPageJsLoad} from './wait'
export {validateScreenshot} from './screenshots'
export {extractEmailsForRecipient, supportsEmailInspection} from './email'
export {
  loginAsAdmin,
  loginAsCiviformAndProgramAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  loginAsTrustedIntermediary,
  logout,
  testUserDisplayName,
} from './auth'
export {
  isLocalDevEnvironment,
  dismissToast,
  selectApplicantLanguage,
  disableFeatureFlag,
  enableFeatureFlag,
  closeWarningMessage,
  validateAccessibility,
  normalizeElements,
  validateToastHidden,
  validateToastMessage,
  validateToastLayoutCentered,
  throttle,
} from './helpers'

// Do NOT add functions to this file.
