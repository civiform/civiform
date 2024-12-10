import {test} from '../support/civiform_fixtures'
import {
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  enableFeatureFlag,
  waitForPageJsLoad,
  testUserDisplayName,
} from '../support'

test.describe('with program statuses', () => {
  const programName = 'Applicant with statuses program'
  const approvedStatusName = 'Approved'
  const rejectedStatusName = 'Rejected'
  const reapplyStatusName = 'Reapply'

  test.beforeEach(
    async ({page, adminPrograms, adminProgramStatuses, applicantQuestions}) => {
      await enableFeatureFlag(page, 'bulk_status_update_enabled')
      await loginAsAdmin(page)

      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
      await adminProgramStatuses.createStatus(approvedStatusName)
      await adminProgramStatuses.createStatus(rejectedStatusName)
      await adminProgramStatuses.createStatus(reapplyStatusName)
      await adminPrograms.publishProgram(programName)
      await adminPrograms.expectActiveProgram(programName)
      await logout(page)

      // Submit an application as a test user so that we can navigate back to the applications page.
      await loginAsTestUser(page)
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantQuestions.submitFromReviewPage()
      await logout(page)
      await loginAsProgramAdmin(page)
      await adminPrograms.viewApplications(programName)
    },
  )

  //   test('bulks status checkbox is visible', async ({page}) => {
  //     await page.getByRole('checkbox', { name: 'selectall' }).isVisible()
  //   })

  test('application without status appears in default filter and without statuses filter', async ({
    page,
    adminPrograms,
  }) => {
    // Default page shows all applications.
    await adminPrograms.expectApplicationCountForBulkStatus(1)
    await adminPrograms.expectApplicationHasStatusStringForBulkStatus(
      testUserDisplayName(),
      'None',
    )
    await page.getByRole('checkbox', {name: 'statusText'}).check()
    await page.selectOption(`.cf-program-admin-status-selector`, {
      label: approvedStatusName,
    })
    await page.getByRole('button', {name: 'Status change'}).click()
    await waitForPageJsLoad(page)
    await adminPrograms.expectApplicationHasStatusStringForBulkStatus(
      testUserDisplayName(),
      approvedStatusName,
    )
  })
})
