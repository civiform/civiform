import {expect, test} from '../support/civiform_fixtures'
import {
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateAccessibility,
} from '../support'

test.describe('with program statuses', () => {
  const programName = 'Applicant with statuses program'
  const approvedStatusName = 'Approved'

  test.beforeEach(
    async ({page, adminPrograms, adminProgramStatuses, applicantQuestions}) => {
      await loginAsAdmin(page)

      await test.step('Setup: Publish program as admin', async () => {
        await adminPrograms.addProgram(programName)
        await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
        await adminProgramStatuses.createStatus(approvedStatusName)
        await adminPrograms.publishProgram(programName)
        await adminPrograms.expectActiveProgram(programName)
        await logout(page)
      })

      await loginAsTestUser(page)

      await test.step('Setup: submit application', async () => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.submitFromReviewPage()
      })
    },
  )

  test('view application submitted page', async ({page}) => {
    expect(await page.textContent('html')).toContain('Application confirmation')
    expect(await page.textContent('html')).toContain(programName)
  })

  test('passes accessibility checks', async ({page}) => {
    await validateAccessibility(page)
  })
})
