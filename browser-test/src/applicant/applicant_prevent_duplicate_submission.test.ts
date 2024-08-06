import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateAccessibility,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'

test.describe('Prevent Duplicate Submission', () => {
  const programName = 'Test Program Name'
  const questionId = 'test-q'

  test.beforeEach(async ({page, adminPrograms, adminQuestions}) => {
    await test.step('Create program', async () => {
      await loginAsAdmin(page)

      await adminQuestions.addNumberQuestion({
        questionName: questionId,
        questionText: 'How many keyboards do you own?',
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockUsingSpec(programName, {
        name: 'Screen 1',
        description: 'First screen',
        questions: [{name: questionId}],
      })

      await adminPrograms.publishProgram(programName)
      await logout(page)
    })
  })

  test('View Prevent Duplicate Submission page in old UI', async ({
    applicantQuestions,
    page,
  }) => {
    await test.step('As applicant, submit an application', async () => {
      // await loginAsTestUser(page)
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantQuestions.clickContinue()
      await applicantQuestions.answerNumberQuestion('0')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ false,
      )
      await applicantQuestions.gotoApplicantHomePage()
    })

    await test.step('Submit another application to the same program without changing anything', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ false,
      )
      // Wait for the page to finish loading
      await waitForPageJsLoad(page)

      await expect( page.innerText('html')).toContain(
        'There are no changes to save',
      )
      await validateScreenshot(page, 'prevent-duplicate-submission')
      await validateAccessibility(page)

      // Click the "Continue editing" button to return to the review page
      await page.click('#continue-editing-button')
      await applicantQuestions.expectReviewPage()
      await applicantQuestions.clickEdit()

      // Edit the application but insert the same value as before and submit.
      await applicantQuestions.answerNumberQuestion('0')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()

      // See the duplicate submissions page
      await applicantQuestions.expectDuplicatesPage()

      // Click the "Exit application" link to return to the programs page
      await page.click('text="Exit application"')
      await applicantQuestions.expectProgramsPage()
    })
  })

  test(
    'View Prevent Duplicate Submission page in North Star',
    {tag: ['@northstar']},
    async ({applicantQuestions, page}) => {
      await test.step('As applicant, submit an application', async () => {
        await enableFeatureFlag(page, 'north_star_applicant_ui')

        await loginAsTestUser(page)
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerNumberQuestion('0')
        await applicantQuestions.clickContinue()
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.gotoApplicantHomePage()
      })

      await test.step('Submit another application to the same program without changing anything', async () => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
        // Wait for the page to finish loading
        await waitForPageJsLoad(page)

        await expect( page.innerText('html')).toContain(
          'There are no changes to save',
        )
        await validateScreenshot(
          page,
          'north-star-prevent-duplicate-submission',
        )
        await validateAccessibility(page)
      })
    },
  )
})
