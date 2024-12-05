import {test, expect} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  testUserDisplayName,
  validateAccessibility,
  validateScreenshot,
} from '../support'

test.describe('with program statuses', {tag: ['@northstar']}, () => {
  const programName = 'Applicant with statuses program'
  const approvedStatusName = 'Approved'

  test.beforeEach(
    async ({page, adminPrograms, adminProgramStatuses, applicantQuestions}) => {
      await enableFeatureFlag(page, 'bulk_status_update_enabled')
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
      await adminPrograms.viewApplicationForApplicantForBulkStatus(
        testUserDisplayName(),
      )
      const modal =
        await adminPrograms.setStatusOptionAndAwaitModalForBulkStatus(
          approvedStatusName,
        )
      await adminPrograms.confirmStatusUpdateModalForBulkStatus(modal)
      await logout(page)

      await enableFeatureFlag(page, 'north_star_applicant_ui')
    },
  )

  test.describe('application status', () => {
    test('submitted with admin status only shows admin status', async ({
      page,
    }) => {
      await loginAsTestUser(page)

      const locator = page.locator('.cf-application-card')

      await expect(locator.getByText('Submitted on 1/1/30')).toBeHidden()
      await expect(locator.getByText(approvedStatusName)).toBeVisible()

      await validateScreenshot(locator, 'program-card-with-status-northstar')
      await validateAccessibility(page)
    })
  })
})
