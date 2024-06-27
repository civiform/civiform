import {test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateAccessibility,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'

test.describe('Prevent Duplicate Submission', {tag: ['@northstar']}, () => {
  const programName = 'Test Program Name'

  test.beforeEach(async ({page, adminPrograms, applicantQuestions}) => {
    await test.step('Create program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
      await adminPrograms.publishProgram(programName)
      await logout(page)
    })

    await test.step('As applicant, submit an application', async () => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await loginAsTestUser(page)
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.gotoApplicantHomePage()
    })
  })

  test('View Prevent Duplicate Submission page', async ({
    applicantQuestions,
    page,
  }) => {
    await test.step('Submit another application to the same program without changing anything', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
      // Wait for the page to finish loading
      await waitForPageJsLoad(page)

      await validateScreenshot(page, 'north-star-prevent-duplicate-submission')
      await validateAccessibility(page)
    })
  })
})
