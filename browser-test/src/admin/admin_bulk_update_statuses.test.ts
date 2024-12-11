import {test} from '../support/civiform_fixtures'
import {Page} from 'playwright'
import {
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  enableFeatureFlag,
  waitForPageJsLoad,
  testUserDisplayName,
  supportsEmailInspection,
  extractEmailsForRecipient,
  AdminPrograms,
  ApplicantQuestions,
  AdminProgramStatuses,
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
test.describe('when email is configured for the status and applicant, a checkbox is shown to notify the applicant', () => {
  const programWithStatusesName = 'Test program with email statuses'
  const emailStatusName = 'Email status'
  const emailBody = 'Some email content'
  const noEmailStatusName = 'No email status'
  test.beforeEach(
    async ({page, adminPrograms, applicantQuestions, adminProgramStatuses}) => {
      await setupProgramsWithStatuses(
        page,
        adminPrograms,
        applicantQuestions,
        adminProgramStatuses,
      )
      // enable bulk status feature flag
      await enableFeatureFlag(page, 'bulk_status_update_enabled')
      await loginAsProgramAdmin(page)
      await adminPrograms.viewApplications(programWithStatusesName)
    },
  )
  test('checkbox is checked for bulk status notification', async ({
    page,
    adminPrograms,
  }) => {
    const emailsBefore = supportsEmailInspection()
      ? await extractEmailsForRecipient(page, testUserDisplayName())
      : []

    await page.locator('#selectAll').check()
    await page.locator('#bulk-status-selector').selectOption(emailStatusName)
    await page.locator('#bulk-status-notification').check()
    await page.getByRole('button', {name: 'Status change'}).click()
    await waitForPageJsLoad(page)

    await adminPrograms.expectApplicationHasStatusStringForBulkStatus(
      testUserDisplayName(),
      emailStatusName,
    )

    if (supportsEmailInspection()) {
      await adminPrograms.expectEmailSent(
        emailsBefore.length,
        testUserDisplayName(),
        emailBody,
        programWithStatusesName,
      )
    }
  })
  const setupProgramsWithStatuses = async (
    page: Page,
    adminPrograms: AdminPrograms,
    applicantQuestions: ApplicantQuestions,
    adminProgramStatuses: AdminProgramStatuses,
  ) => {
    await test.step('login as admin and create program with statuses', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programWithStatusesName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(
        programWithStatusesName,
      )
      await adminProgramStatuses.createStatus(noEmailStatusName)
      await adminProgramStatuses.createStatus(emailStatusName, {
        emailBody: emailBody,
      })
      await adminPrograms.publishProgram(programWithStatusesName)
      await adminPrograms.expectActiveProgram(programWithStatusesName)
      await logout(page)
    })

    await test.step('submit an application as a guest', async () => {
      await applicantQuestions.clickApplyProgramButton(programWithStatusesName)
      await applicantQuestions.submitFromReviewPage()
      await logout(page)
    })

    await test.step('submit an application as a logged in user', async () => {
      await loginAsTestUser(page)
      await applicantQuestions.clickApplyProgramButton(programWithStatusesName)
      await applicantQuestions.submitFromReviewPage()
      await logout(page)
    })
  }
})
