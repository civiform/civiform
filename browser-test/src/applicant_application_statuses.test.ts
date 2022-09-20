import {
  createTestContext,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  selectApplicantLanguage,
  testUserDisplayName,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('with program statuses', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  const programName = 'applicant-with-statuses-program'
  const approvedStatusName = 'Approved'
  
  beforeAll(async () => {
    const {page, adminPrograms, adminQuestions, adminProgramStatuses, applicantQuestions} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'application_status_tracking_enabled')

    // Add a program with a single question that is used for asserting downloaded content.
    await adminPrograms.addProgram(programName)
    await adminPrograms.gotoDraftProgramManageStatusesPage(
      programName,
    )
    await adminProgramStatuses.createStatus(approvedStatusName)
    await adminPrograms.publishProgram(programName)
    await adminPrograms.expectActiveProgram(programName)
    await logout(page)

    // Submit an application as a test user so that we can navigate back to the applications page..
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')
    await applicantQuestions.clickApplyProgramButton(programName)
    await applicantQuestions.submitFromPreviewPage()
    await logout(page)

    // Navigate to the submitted application as the program admin and set a status.
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)
    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    const modal = await adminPrograms.setStatusOptionAndAwaitModal(
      approvedStatusName
    )
    await adminPrograms.confirmStatusUpdateModal(modal)
    await logout(page)
  })

  it('displays status and passes accessibility checks', async () => {
    const {page} = ctx
    await loginAsTestUser(page)
    await validateAccessibility(page)
    await validateScreenshot(page, 'program-list-with-status')
  })
})
