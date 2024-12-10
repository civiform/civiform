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
    async ({
      page,
      adminPrograms,
      adminProgramStatuses,
      applicantQuestions,
      request,
    }) => {
      await test.step('Clear database', async () => {
        await request.post('/dev/seed/clear')
      })
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

  test('single application updated with bulk status select', async ({
    page,
    adminPrograms,
  }) => {
    // Default page shows all applications.
    await adminPrograms.expectApplicationCountForBulkStatus(1)
    await adminPrograms.expectApplicationHasStatusStringForBulkStatus(
      testUserDisplayName(),
      'None',
    )
    await page.locator('#selectAll').check()
    await page.locator('#bulk-status-selector').selectOption('Approved')
    await page.getByRole('button', {name: 'Status change'}).click()
    await waitForPageJsLoad(page)
    await adminPrograms.expectApplicationHasStatusStringForBulkStatus(
      testUserDisplayName(),
      approvedStatusName,
    )
  })
  test('if more than 100 applications, only the first page of applications undergo status update', async ({
    page,
    adminPrograms,
    applicantQuestions,
    adminProgramMigration,
  }) => {
    // There is already 1 application from the beforeEach, so apply 105 more times.
    for (let i = 0; i < 105; i++) {
      await logout(page)

      // Submit an application as a guest.
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantQuestions.submitFromReviewPage()
    }
    const id = await adminPrograms.getApplicationId()

    await logout(page)
    await loginAsProgramAdmin(page)
    // Navigate to the applications list
    await adminPrograms.viewApplications(programName)
    await page.locator('#selectAll').check()
    await page.locator('#bulk-status-selector').selectOption('Approved')
    await page.getByRole('button', {name: 'Status change'}).click()
    await waitForPageJsLoad(page)

    await adminProgramMigration.expectAlert(
      'Status update success',
      'usa-alert--info',
    )

    for (let i = 0; i < 100; i++) {
      await adminPrograms.expectApplicationHasStatusStringForBulkStatus(
        `Guest (${id - i})`,
        approvedStatusName,
      )
    }
    await page.locator('.usa-pagination__button:has-text("2")').click()
    // last applicant
    await adminPrograms.expectApplicationHasStatusStringForBulkStatus(
      testUserDisplayName(),
      'None',
    )
  })
})
