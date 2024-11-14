import {test} from '../support/civiform_fixtures'
import {
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  testUserDisplayName,
  validateAccessibility,
  validateScreenshot,
} from '../support'

test.describe('with program statuses', () => {
  const programName = 'Applicant with statuses program'
  const approvedStatusName = 'Approved'

  test.beforeEach(
    async ({page, adminPrograms, adminProgramStatuses, applicantQuestions}) => {
      await loginAsAdmin(page)

      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
      await adminProgramStatuses.createStatus(approvedStatusName)
      await adminPrograms.publishProgram(programName)
      await adminPrograms.expectActiveProgram(programName)
      await logout(page)

      // Submit an application as a test user so that we can navigate back to the applications page.
      await loginAsTestUser(page)
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantQuestions.submitFromReviewPage()
      await logout(page)

      // Navigate to the submitted application as the program admin and set a status.
      await loginAsProgramAdmin(page)
      await adminPrograms.viewApplications(programName)
      await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
      const modal =
        await adminPrograms.setStatusOptionAndAwaitModal(approvedStatusName)
      await adminPrograms.confirmStatusUpdateModal(modal)
      await logout(page)
    },
  )

  test('displays status and passes accessibility checks', async ({page}) => {
    await loginAsTestUser(page)
    await validateAccessibility(page)
    await validateScreenshot(page, 'program-list-with-status')
  })
})
