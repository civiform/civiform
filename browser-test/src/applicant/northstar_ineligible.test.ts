import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateScreenshot,
  validateAccessibility,
  loginAsTrustedIntermediary,
  waitForPageJsLoad,
  ClientInformation,
} from '../support'
import {ProgramVisibility} from '../support/admin_programs'

test.describe('North Star Ineligible Page Tests', {tag: ['@northstar']}, () => {
  const programName = 'Pet Assistance Program'
  const eligibilityQuestionId = 'eligibility-q'
  const questionText = 'How many pets do you have?'

  test.beforeEach(
    async ({
      page,
      adminQuestions,
      adminPrograms,
      adminPredicates,
      tiDashboard,
    }) => {
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

  test('As applicant, view ineligible page', async ({page, applicantQuestions}) => {
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
      expect(await page.textContent('html')).toContain(
        'Application confirmation',
      )
      expect(await page.textContent('html')).toContain(programName)
    })
  })

  test('As TI, view ineligible page', async ({page, applicantQuestions, tiDashboard}) => {
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

      await tiDashboard.gotoTIDashboardPage(page)
      await tiDashboard.clickOnViewApplications()
      await applicantQuestions.seeEligibilityTag(programName, false)
    })

    await test.step('Go back and update answers to become eligible', async () => {
      await tiDashboard.goToProgramsPageForCurrentClient()
      await tiDashboard.clickOnViewApplications()
      await applicantQuestions.applyProgram(programName) // "Continue"
      
      await applicantQuestions.clickReview()

      await applicantQuestions.answerNumberQuestion('1')
      await applicantQuestions.clickContinue()

      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    await test.step('Expect application submitted page', async () => {
      expect(await page.textContent('html')).toContain(
        'Application confirmation',
      )
      expect(await page.textContent('html')).toContain(programName)
    })

    await test.step('Expect client is eligible in TI dashboard', async () => {
      await tiDashboard.goToProgramsPageForCurrentClient()
      // await tiDashboard.gotoTIDashboardPage(page)
      await tiDashboard.clickOnViewApplications()
      await applicantQuestions.seeEligibilityTag(programName, true)
    })
  })

  // test('correctly handles eligibility', async ({
  //     page,
  //     tiDashboard,
  //     applicantQuestions,
  //   }) => {
  //     await loginAsTrustedIntermediary(page)
  //     await tiDashboard.gotoTIDashboardPage(page)
  //     await tiDashboard.clickOnViewApplications()

  //     // Verify TI gets navigated to the ineligible page with TI text.
  //     await applicantQuestions.applyProgram(fullProgramName)
  //     await applicantQuestions.answerNumberQuestion('1')
  //     await applicantQuestions.clickNext()
  //     await tiDashboard.expectIneligiblePage()

  //     // Verify the 'may not qualify' tag shows on the program page
  //     await tiDashboard.gotoTIDashboardPage(page)
  //     await tiDashboard.clickOnViewApplications()
  //     await applicantQuestions.seeEligibilityTag(fullProgramName, false)
  //     await applicantQuestions.clickApplyProgramButton(fullProgramName)

  //     // Verify the summary page shows the ineligible toast and the correct question is marked ineligible.
  //     await applicantQuestions.expectQuestionIsNotEligible(
  //       AdminQuestions.NUMBER_QUESTION_TEXT,
  //     )

  //     // Change answer to one that passes eligibility and verify 'may qualify' tag appears on home page and as a toast.
  //     await applicantQuestions.clickEdit()
  //     await applicantQuestions.answerNumberQuestion('5')
  //     await applicantQuestions.clickNext()
  //     await tiDashboard.gotoTIDashboardPage(page)
  //     await tiDashboard.clickOnViewApplications()
  //     await applicantQuestions.seeEligibilityTag(fullProgramName, true)
  //   })
})
