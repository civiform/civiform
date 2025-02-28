import {test} from '../../support/civiform_fixtures'
import {
  ClientInformation,
  loginAsAdmin,
  loginAsTestUser,
  loginAsTrustedIntermediary,
  logout,
  validateAccessibility,
  validateScreenshot,
  waitForPageJsLoad,
} from '../../support'
import {ProgramVisibility} from '../../support/admin_programs'

test.describe('Applicant navigation flow', () => {
  test.describe('navigation with common intake', () => {
    // Create two programs, one is common intake
    const commonIntakeProgramName = 'Test Common Intake Form Program'
    const secondProgramName = 'Test Regular Program with Eligibility Conditions'
    const eligibilityQuestionId = 'nav-predicate-number-q'
    const secondProgramCorrectAnswer = '5'

    test.beforeEach(
      async ({page, adminQuestions, adminPredicates, adminPrograms}) => {
        await loginAsAdmin(page)

        // Add questions
        await adminQuestions.addNumberQuestion({
          questionName: eligibilityQuestionId,
        })

        // Set up common intake form
        await adminPrograms.addProgram(
          commonIntakeProgramName,
          'program description',
          'short program description',
          'https://usa.gov',
          ProgramVisibility.PUBLIC,
          'admin description',
          /* isCommonIntake= */ true,
        )

        await adminPrograms.editProgramBlock(
          commonIntakeProgramName,
          'first description',
          [eligibilityQuestionId],
        )

        // Set up another program
        await adminPrograms.addProgram(secondProgramName)

        await adminPrograms.editProgramBlock(
          secondProgramName,
          'first description',
          [eligibilityQuestionId],
        )

        await adminPrograms.goToEditBlockEligibilityPredicatePage(
          secondProgramName,
          'Screen 1',
        )
        await adminPredicates.addPredicates({
          questionName: 'nav-predicate-number-q',
          scalar: 'number',
          operator: 'is equal to',
          value: secondProgramCorrectAnswer,
        })

        await adminPrograms.publishAllDrafts()
        await logout(page)
      },
    )

    test('does not show eligible programs or upsell on confirmation page when no programs are eligible and signed in', async ({
      page,
      applicantQuestions,
    }) => {
      await loginAsTestUser(page)
      // Fill out common intake form, with non-eligible response
      await applicantQuestions.applyProgram(commonIntakeProgramName)
      await applicantQuestions.answerNumberQuestion('4')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectCommonIntakeReviewPage()
      await applicantQuestions.clickSubmit()

      await applicantQuestions.expectCommonIntakeConfirmationPage(
        /* wantUpsell= */ false,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [],
      )

      await validateScreenshot(
        page.locator('main'),
        'cif-ineligible-signed-in-confirmation-page',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)
    })

    test('shows eligible programs and no upsell on confirmation page when programs are eligible and signed in', async ({
      page,
      applicantQuestions,
    }) => {
      await loginAsTestUser(page)
      // Fill out common intake form, with eligible response
      await applicantQuestions.applyProgram(commonIntakeProgramName)
      await applicantQuestions.answerNumberQuestion(secondProgramCorrectAnswer)
      await applicantQuestions.clickNext()
      await applicantQuestions.expectCommonIntakeReviewPage()
      await applicantQuestions.clickSubmit()

      await applicantQuestions.expectCommonIntakeConfirmationPage(
        /* wantUpsell= */ false,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [secondProgramName],
      )

      await validateScreenshot(
        page.locator('main'),
        'cif-eligible-signed-in-confirmation-page',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)
    })

    test('does not show eligible programs and shows upsell on confirmation page when no programs are eligible and a guest user', async ({
      page,
      applicantQuestions,
    }) => {
      // Fill out common intake form, with non-eligible response
      await applicantQuestions.applyProgram(commonIntakeProgramName)
      await applicantQuestions.answerNumberQuestion('4')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectCommonIntakeReviewPage()
      await applicantQuestions.clickSubmit()

      await applicantQuestions.expectCommonIntakeConfirmationPage(
        /* wantUpsell= */ true,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [],
      )

      await validateScreenshot(
        page.locator('main'),
        'cif-ineligible-guest-confirmation-page',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)
    })

    test('shows eligible programs and upsell on confirmation page when programs are eligible and a guest user', async ({
      page,
      applicantQuestions,
    }) => {
      // Fill out common intake form, with eligible response
      await applicantQuestions.applyProgram(commonIntakeProgramName)
      await applicantQuestions.answerNumberQuestion(secondProgramCorrectAnswer)
      await applicantQuestions.clickNext()
      await applicantQuestions.expectCommonIntakeReviewPage()
      await applicantQuestions.clickSubmit()

      await applicantQuestions.expectCommonIntakeConfirmationPage(
        /* wantUpsell= */ true,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [secondProgramName],
      )

      await validateScreenshot(
        page.locator('main'),
        'cif-eligible-guest-confirmation-page',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
      await validateAccessibility(page)

      await page.click('button:has-text("Apply to programs")')
      await validateScreenshot(
        page.locator('main'),
        'cif-submission-guest-login-prompt-modal',
        /* fullPage= */ false,
        /* mobileScreenshot= */ true,
      )
    })

    test('shows intake form as submitted after completion', async ({
      page,
      applicantQuestions,
    }) => {
      // Fill out common intake form, with eligible response
      await applicantQuestions.applyProgram(commonIntakeProgramName)
      await applicantQuestions.answerNumberQuestion(secondProgramCorrectAnswer)
      await applicantQuestions.clickNext()
      await applicantQuestions.expectCommonIntakeReviewPage()
      await applicantQuestions.clickSubmit()

      await applicantQuestions.expectCommonIntakeConfirmationPage(
        /* wantUpsell= */ true,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [secondProgramName],
      )

      await page.click('button:has-text("Apply to programs")')
      await page.click('button:has-text("Continue without an account")')
      await validateScreenshot(
        page.locator('main'),
        'cif-shows-submitted',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    test('does not show eligible programs and shows TI text on confirmation page when no programs are eligible and a TI', async ({
      page,
      tiDashboard,
      applicantQuestions,
    }) => {
      // Create trusted intermediary client
      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await waitForPageJsLoad(page)
      const client: ClientInformation = {
        emailAddress: 'fake@sample.com',
        firstName: 'first',
        middleName: 'middle',
        lastName: 'last',
        dobDate: '2021-05-10',
      }
      await tiDashboard.createClient(client)
      await tiDashboard.expectDashboardContainClient(client)
      await tiDashboard.clickOnViewApplications()

      // Fill out common intake form, with non-eligible response
      await applicantQuestions.applyProgram(commonIntakeProgramName)
      await applicantQuestions.answerNumberQuestion('4')
      await applicantQuestions.clickNext()
      await applicantQuestions.expectCommonIntakeReviewPage()
      await applicantQuestions.clickSubmit()

      await applicantQuestions.expectCommonIntakeConfirmationPage(
        /* wantUpsell= */ false,
        /* wantTrustedIntermediary= */ true,
        /* wantEligiblePrograms= */ [],
      )

      await validateScreenshot(
        page.locator('main'),
        'cif-ineligible-ti-confirmation-page',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    test('shows eligible programs and TI text on confirmation page when programs are eligible and a TI', async ({
      page,
      tiDashboard,
      applicantQuestions,
    }) => {
      // Create trusted intermediary client
      await loginAsTrustedIntermediary(page)
      await tiDashboard.gotoTIDashboardPage(page)
      await waitForPageJsLoad(page)
      const client: ClientInformation = {
        emailAddress: 'fake@sample.com',
        firstName: 'first',
        middleName: 'middle',
        lastName: 'last',
        dobDate: '2021-05-10',
      }
      await tiDashboard.createClient(client)
      await tiDashboard.expectDashboardContainClient(client)
      await tiDashboard.clickOnViewApplications()

      // Fill out common intake form, with eligible response
      await applicantQuestions.applyProgram(commonIntakeProgramName)
      await applicantQuestions.answerNumberQuestion(secondProgramCorrectAnswer)
      await applicantQuestions.clickNext()
      await applicantQuestions.expectCommonIntakeReviewPage()
      await applicantQuestions.clickSubmit()

      await applicantQuestions.expectCommonIntakeConfirmationPage(
        /* wantUpsell= */ false,
        /* wantTrustedIntermediary= */ true,
        /* wantEligiblePrograms= */ [secondProgramName],
      )
      await validateScreenshot(
        page.locator('main'),
        'cif-eligible-ti-confirmation-page',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })
  })
})
