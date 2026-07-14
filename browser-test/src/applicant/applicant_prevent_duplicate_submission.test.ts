import {expect, test} from '../support/civiform_fixtures'
import {
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateAccessibility,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'
import {SAMPLE_QUESTIONS} from '../support/seeding'

test.describe('Prevent Duplicate Submission', () => {
  const programName = 'Test Program Name'

  test.beforeEach(async ({page, adminPrograms, seeding}) => {
    await test.step('Create program', async () => {
      await seeding.seedQuestions()
      await loginAsAdmin(page)

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'First screen',
        questions: [{name: SAMPLE_QUESTIONS.number}],
      })

      await adminPrograms.publishProgram(programName)
      await logout(page)
    })
  })

  test('View Prevent Duplicate Submission modal', async ({
    applicantQuestions,
    page,
    applicantProgramOverview,
  }) => {
    await test.step('As applicant, submit an application', async () => {
      await loginAsTestUser(page)
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantProgramOverview.startApplicationFromProgramOverviewPage(
        programName,
      )
      await applicantQuestions.answerNumberQuestion('0')
      await applicantQuestions.clickContinue()
      await applicantQuestions.submitFromReviewPage()
      await applicantQuestions.gotoApplicantHomePage()
    })

    await test.step('Submit another application to the same program without changing anything', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantQuestions.submitFromReviewPage()
      // Wait for the page to finish loading
      await waitForPageJsLoad(page)
    })

    await test.step('Verify modal', async () => {
      await expect(
        page.getByText('There are no changes to save for the ' + programName),
      ).toBeVisible()

      await validateScreenshot(
        page.locator('#duplicate-submission-modal .usa-modal__content'),
        'prevent-duplicate-submission',
      )
      await validateAccessibility(page)
    })

    await test.step('Verify continue button', async () => {
      // Verify continue button closes the modal
      await applicantQuestions.clickContinueEditing()
      await applicantQuestions.expectReviewPage()
    })

    await test.step('Verify "Close" (x) button', async () => {
      // Show the modal again
      await applicantQuestions.submitFromReviewPage()
      await waitForPageJsLoad(page)

      // Verify close (x) button closes the modal
      await page.locator('[aria-label="Close"] >> visible=true').click()
      await applicantQuestions.expectReviewPage()
    })

    await test.step('Verify exit button', async () => {
      // Show the modal again
      await applicantQuestions.submitFromReviewPage()
      await waitForPageJsLoad(page)

      // Verify the exit button returns to the home page
      await applicantQuestions.clickExitApplication()
      await waitForPageJsLoad(page)

      // Verify user sees home page
      await applicantQuestions.expectProgramsPage()
    })
  })
})
