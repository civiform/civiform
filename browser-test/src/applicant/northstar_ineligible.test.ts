import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateScreenshot,
  validateAccessibility,
  loginAsTrustedIntermediary,
  ClientInformation,
} from '../support'

test.describe('North Star Ineligible Page Tests', {tag: ['@northstar']}, () => {
  const programName = 'Pet Assistance Program'
  const eligibilityQuestionId = 'eligibility-q'
  const questionText = 'How many pets do you have?'

  test.beforeEach(
    async ({page, adminQuestions, adminPrograms, adminPredicates}) => {
      await loginAsAdmin(page)

      await adminQuestions.addNumberQuestion({
        questionName: eligibilityQuestionId,
        questionText: questionText,
      })

      // Add the full program.
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
    },
  )

  test('As applicant, view ineligible page', async ({
    page,
    applicantQuestions,
  }) => {
    await loginAsTestUser(page)

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await test.step('Setup: submit application', async () => {
      await applicantQuestions.applyProgram(programName)

      await applicantQuestions.answerNumberQuestion('0')
      await applicantQuestions.clickContinue()
    })

    await test.step('Expect ineligible page part 1', async () => {
      expect(await page.innerText('html')).toContain(
        'Based on your responses to the following questions, you may not qualify for the ' +
          programName,
      )

      expect(await page.textContent('html')).toContain(questionText)
    })

    await validateScreenshot(
      page,
      'northstar-ineligible',
      /* fullPage= */ false,
      /* mobileScreenshot= */ true,
    )

    await validateAccessibility(page)

    await test.step('Go back and update answers to become eligible', async () => {
      await applicantQuestions.clickGoBackAndEdit()
      await applicantQuestions.clickReview()

      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickContinue()

      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    await test.step('Expect application submitted page', async () => {
      await applicantQuestions.expectConfirmationPage()
    })
  })

  test('As TI, view ineligible page', async ({
    page,
    applicantQuestions,
    tiDashboard,
  }) => {
    await loginAsTrustedIntermediary(page)

    await enableFeatureFlag(page, 'north_star_applicant_ui')

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
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('0')
      await applicantQuestions.clickContinue()
    })

    await test.step('Expect client to be ineligible', async () => {
      expect(await page.innerText('html')).toContain(
        'Based on your responses to the following questions, your client may not qualify for the ' +
          programName,
      )

      await page.click('#header-return-home')

      await tiDashboard.clickOnViewApplications()
      await applicantQuestions.seeEligibilityTag(programName, false)
    })

    await test.step('Go back and update answers to become eligible', async () => {
      // Click "Continue" on the program card
      await applicantQuestions.clickApplyProgramButton(programName)

      await applicantQuestions.clickReview()
      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickContinue()
      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    await test.step('Expect application submitted page', async () => {
      await applicantQuestions.expectConfirmationPage()
    })

    await test.step('Expect client is eligible in TI dashboard', async () => {
      await tiDashboard.goToProgramsPageForCurrentClient()
      await tiDashboard.clickOnViewApplications()
      await applicantQuestions.seeEligibilityTag(programName, true)
    })
  })
})
