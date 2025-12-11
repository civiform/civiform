import {expect, test} from '../support/civiform_fixtures'
import {
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateScreenshot,
  validateAccessibility,
  loginAsTrustedIntermediary,
  ClientInformation,
  selectApplicantLanguage,
} from '../support'

test.describe('North Star Ineligible Page Tests', () => {
  const programName = 'Pet Assistance Program'
  const eligibilityQuestionId = 'eligibility-q'
  const questionText = 'How many pets do you have?'

  test.beforeEach(
    async ({page, adminQuestions, adminPrograms, adminPredicates}) => {
      await test.step('Setup: Create program with eligibility condition', async () => {
        await loginAsAdmin(page)

        await adminQuestions.addNumberQuestion({
          questionName: eligibilityQuestionId,
          questionText: questionText,
        })

        await adminPrograms.addProgram(programName)
        await adminPrograms.editProgramBlockUsingSpec(programName, {
          name: 'Screen 1',
          description: 'first screen',
          questions: [{name: eligibilityQuestionId}],
        })

        await adminPrograms.goToEditBlockEligibilityPredicatePage(
          programName,
          'Screen 1',
        )
        await adminPredicates.addPredicates({
          questionName: eligibilityQuestionId,
          scalar: 'number',
          operator: 'is greater than',
          value: '0',
        })

        await adminPrograms.gotoAdminProgramsPage()
        await adminPrograms.publishProgram(programName)

        await logout(page)
      })
    },
  )

  test('As applicant, fill out application and view ineligible page', async ({
    page,
    applicantQuestions,
  }) => {
    await loginAsTestUser(page)

    await test.step('Setup: submit application', async () => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled=*/ true,
      )

      await applicantQuestions.answerNumberQuestion('0')
      await applicantQuestions.clickContinue()
    })

    await test.step('Expect ineligible page part 1', async () => {
      await applicantQuestions.expectIneligiblePage(/* northStar= */ true)
      await expect(page.getByText(questionText)).toBeVisible()
    })

    await validateScreenshot(page, 'ineligible', {
      fullPage: false,
      mobileScreenshot: true,
    })

    await validateAccessibility(page)

    await test.step('Go back and update answers to become eligible', async () => {
      await applicantQuestions.clickEditMyResponses()
      await applicantQuestions.clickEdit()

      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickContinue()

      await applicantQuestions.submitFromReviewPage()
    })

    await test.step('Expect application submitted page', async () => {
      await applicantQuestions.expectConfirmationPage()
    })
  })

  test('As applicant, view review page, then view ineligible page', async ({
    page,
    applicantQuestions,
  }) => {
    await loginAsTestUser(page)

    await test.step('Setup: submit application', async () => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled=*/ true,
      )

      await applicantQuestions.answerNumberQuestion('0')
      await applicantQuestions.clickContinue()
    })

    // When North Star is finalized, this test should navigate question -> review -> ineligible
    // Until then, the test must navigate question -> submit -> ineligible -> review -> ineligible
    await test.step('Expect ineligible page', async () => {
      await applicantQuestions.expectIneligiblePage(/* northStar= */ true)
      await expect(page.getByText(questionText)).toBeVisible()
    })

    await test.step('Go back to the review page and re-submit', async () => {
      await applicantQuestions.clickEditMyResponses()
      await applicantQuestions.clickSubmitApplication()
    })

    await test.step('Expect ineligible page again', async () => {
      await applicantQuestions.expectIneligiblePage(/* northStar= */ true)
      await expect(page.getByText(questionText)).toBeVisible()
    })
  })

  test('As applicant, start answering questions, then view the alert with eligibility message on ineligible page', async ({
    page,
    adminPrograms,
    adminPredicates,
    applicantQuestions,
  }) => {
    await test.step('Add an eligibility message with special character for markdown', async () => {
      const eligibilityMsg =
        'This is *a* **customized** eligibility [message](https://staging-aws.civiform.dev)'
      await loginAsAdmin(page)
      await adminPrograms.editProgram(programName)
      await adminPrograms.goToEditBlockEligibilityPredicatePage(
        programName,
        'Screen 1',
      )
      await adminPredicates.updateEligibilityMessage(eligibilityMsg)
      await adminPrograms.publishProgram(programName)
      await logout(page)
    })

    await test.step('View the ineligible page with markdown-compatible eligibility message', async () => {
      await loginAsTestUser(page)
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled=*/ true,
      )
      await applicantQuestions.answerNumberQuestion('0')
      await applicantQuestions.clickContinue()
      await validateScreenshot(page.getByRole('alert'), 'eligibility-msg')
    })
  })

  test('As TI, view ineligible page', async ({
    page,
    applicantQuestions,
    tiDashboard,
  }) => {
    await loginAsTrustedIntermediary(page)

    await test.step('Create client', async () => {
      await tiDashboard.gotoTIDashboardPage(page)

      const client: ClientInformation = {
        emailAddress: 'test@sample.com',
        firstName: 'first',
        middleName: 'middle',
        lastName: 'last',
        dobDate: '2021-06-10',
      }
      await tiDashboard.createClient(client)
    })

    await test.step('Answer questions', async () => {
      await tiDashboard.clickOnViewApplications()
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled=*/ true,
      )
      await applicantQuestions.answerNumberQuestion('0')
      await applicantQuestions.clickContinue()
    })

    await test.step('Expect client to be ineligible', async () => {
      await expect(
        page
          .getByText('Your client may not be eligible for this program')
          .and(page.getByRole('heading')),
      ).toBeVisible()
      await expect(page.getByText('Apply to another program')).toBeVisible()
      await expect(page.getByText('Edit my responses')).toBeVisible()
      const href = await page.getByText('program details').getAttribute('href')
      expect(href).toContain('/pet-assistance-program')

      await page.click('#header-return-home')
      await tiDashboard.clickOnViewApplications()
    })

    await test.step('Go back and update answers to become eligible', async () => {
      // Click "Continue" on the program card
      await applicantQuestions.clickApplyProgramButton(programName)

      // All questions have been answered
      await applicantQuestions.expectReviewPage()

      // Edit the block (there is only one block)
      await applicantQuestions.clickEdit()
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickContinue()
      await applicantQuestions.expectMayBeEligibileAlertToBeVisible()
      await applicantQuestions.submitFromReviewPage()
    })

    await test.step('Expect application submitted page', async () => {
      await applicantQuestions.expectConfirmationPage()
    })
  })

  test('Applicant ineligible page renders right to left', async ({
    page,
    applicantQuestions,
  }) => {
    await loginAsTestUser(page)

    await test.step('Setup: submit application', async () => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled=*/ true,
      )

      await test.step('Setup: set language to Arabic', async () => {
        await selectApplicantLanguage(page, 'ar')
      })

      await applicantQuestions.answerNumberQuestion('0')
      await page.click('text="متابعة"')
    })

    await validateScreenshot(page, 'ineligible-right-to-left', {
      fullPage: false,
      mobileScreenshot: true,
    })

    await validateAccessibility(page)
  })

  test('Changing language on ineligible page after block edit redirects to block edit', async ({
    page,
    applicantQuestions,
  }) => {
    await loginAsTestUser(page)

    await test.step('Setup: start application', async () => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled=*/ true,
      )
      await applicantQuestions.answerNumberQuestion('0')
      await applicantQuestions.clickContinue()
    })

    await test.step('Expect ineligible page', async () => {
      await applicantQuestions.expectIneligiblePage(/* northStar= */ true)
      await expect(page.getByText(questionText)).toBeVisible()
    })

    await test.step('Setup: set language to French', async () => {
      await selectApplicantLanguage(page, 'fr')
    })

    await test.step('Expect first block edit', async () => {
      await applicantQuestions.validateQuestionIsOnPage(questionText)
      expect(page.url().split('/').pop()).toEqual('edit?isFromUrlCall=false')
    })
  })

  test('Changing language on ineligible page after block review redirects to block review', async ({
    page,
    applicantQuestions,
  }) => {
    await loginAsTestUser(page)

    await test.step('Setup: start application', async () => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled=*/ true,
      )
      await applicantQuestions.answerNumberQuestion('0')
      await applicantQuestions.clickContinue()
    })

    await test.step('Expect ineligible page', async () => {
      await applicantQuestions.expectIneligiblePage(/* northStar= */ true)
      await expect(page.getByText(questionText)).toBeVisible()
    })

    await test.step('Go back to the review page and re-submit first block', async () => {
      await applicantQuestions.clickEditMyResponses()
      // Review and submit first question
      await applicantQuestions.clickEdit()
      await applicantQuestions.clickContinue()
    })

    await test.step('Expect ineligible page again', async () => {
      await applicantQuestions.expectIneligiblePage(/* northStar= */ true)
      await expect(page.getByText(questionText)).toBeVisible()
    })

    await test.step('Setup: set language to French', async () => {
      await selectApplicantLanguage(page, 'fr')
    })

    await test.step('Expect block review page', async () => {
      await applicantQuestions.validateQuestionIsOnPage(questionText)
      expect(page.url().split('/').pop()).toEqual('review?isFromUrlCall=false')
    })
  })

  test('Changing language on ineligible page after application submit redirects to review', async ({
    page,
    applicantQuestions,
  }) => {
    await loginAsTestUser(page)

    await test.step('Setup: submit application', async () => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled=*/ true,
      )
      await applicantQuestions.answerNumberQuestion('0')
      await applicantQuestions.clickContinue()
    })

    await test.step('Expect ineligible page', async () => {
      await applicantQuestions.expectIneligiblePage(/* northStar= */ true)
      await expect(page.getByText(questionText)).toBeVisible()
    })

    await test.step('Go back to the review page and submit', async () => {
      await applicantQuestions.clickEditMyResponses()
      await applicantQuestions.clickSubmitApplication()
    })

    await test.step('Expect ineligible page again', async () => {
      await applicantQuestions.expectIneligiblePage(/* northStar= */ true)
      await expect(page.getByText(questionText)).toBeVisible()
    })

    await test.step('Setup: set language to French', async () => {
      await selectApplicantLanguage(page, 'fr')
    })

    await test.step('Expect review page', async () => {
      await applicantQuestions.expectReviewPage()
    })
  })
})
