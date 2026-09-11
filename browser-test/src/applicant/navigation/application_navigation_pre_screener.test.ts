import {test} from '../../support/civiform_fixtures'
import {expect} from '@playwright/test'
import {
  ClientInformation,
  loginAsAdmin,
  loginAsTestUser,
  loginAsTrustedIntermediary,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
  waitForPageJsLoad,
} from '../../support'
import {ProgramVisibility} from '../../support/admin_programs'
import {CardSectionName} from '../../support/applicant_program_list'

test.describe('Applicant navigation flow', () => {
  test.describe('navigation with pre-screener', () => {
    // Create two programs, one is pre-screener
    const preScreenerProgramName = 'Test Pre-Screener Form Program'
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

        // Set up pre-screener form
        await adminPrograms.addPreScreener(
          preScreenerProgramName,
          'short program description',
          ProgramVisibility.PUBLIC,
        )

        await adminPrograms.editProgramBlock(
          preScreenerProgramName,
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
      // Fill out pre-screener form, with non-eligible response
      await applicantQuestions.applyProgram(
        preScreenerProgramName,
        // pre-screener programs skip the program overview page
        /* showProgramOverviewPage= */ false,
      )
      await applicantQuestions.answerNumberQuestion('4')
      await applicantQuestions.clickContinue()
      await applicantQuestions.clickSubmitApplication()

      await applicantQuestions.expectPreScreenerConfirmationPage(
        /* wantUpsell= */ false,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [],
      )

      await validateAccessibility(page)
    })

    test('shows eligible programs and no upsell on confirmation page when programs are eligible and signed in', async ({
      page,
      applicantQuestions,
    }) => {
      await loginAsTestUser(page)
      // Fill out pre-screener form, with eligible response
      await applicantQuestions.applyProgram(
        preScreenerProgramName,
        // pre-screener programs skip the program overview page
        /* showProgramOverviewPage= */ false,
      )
      await applicantQuestions.answerNumberQuestion(secondProgramCorrectAnswer)
      await applicantQuestions.clickContinue()
      await applicantQuestions.clickSubmitApplication()

      await applicantQuestions.expectPreScreenerConfirmationPage(
        /* wantUpsell= */ false,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [secondProgramName],
      )

      await validateAccessibility(page)
    })

    test('does not show eligible programs and shows upsell on confirmation page when no programs are eligible and a guest user', async ({
      page,
      applicantQuestions,
    }) => {
      // Fill out pre-screener form, with non-eligible response
      await applicantQuestions.applyProgram(
        preScreenerProgramName,
        // pre-screener programs skip the program overview page
        /* showProgramOverviewPage= */ false,
      )
      await applicantQuestions.answerNumberQuestion('4')
      await applicantQuestions.clickContinue()
      await applicantQuestions.clickSubmitApplication()

      await applicantQuestions.expectPreScreenerConfirmationPage(
        /* wantUpsell= */ true,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [],
      )

      await validateScreenshot(page, 'cif-ineligible-guest-confirmation-page', {
        mobileScreenshot: true,
      })
      await validateAccessibility(page)
    })

    test('shows eligible programs and upsell on confirmation page when programs are eligible and a guest user', async ({
      page,
      applicantQuestions,
    }) => {
      // Fill out pre-screener form, with eligible response
      await applicantQuestions.applyProgram(
        preScreenerProgramName,
        // pre-screener programs skip the program overview page
        /* showProgramOverviewPage= */ false,
      )
      await applicantQuestions.answerNumberQuestion(secondProgramCorrectAnswer)
      await applicantQuestions.clickContinue()
      await applicantQuestions.clickSubmitApplication()

      await applicantQuestions.expectPreScreenerConfirmationPage(
        /* wantUpsell= */ true,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [secondProgramName],
      )
      await validateAccessibility(page)

      await page.click('text="Apply to programs"')
      await applicantQuestions.expectLoginModal()
    })

    test('renders upsell page right to left correctly', async ({
      page,
      applicantQuestions,
    }) => {
      // Fill out pre-screener form, with eligible response
      await applicantQuestions.applyProgram(
        preScreenerProgramName,
        // pre-screener programs skip the program overview page
        /* showProgramOverviewPage= */ false,
      )
      await applicantQuestions.answerNumberQuestion(secondProgramCorrectAnswer)
      await applicantQuestions.clickContinue()
      await applicantQuestions.clickSubmitApplication()

      await applicantQuestions.expectPreScreenerConfirmationPage(
        /* wantUpsell= */ true,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [secondProgramName],
      )
      await selectApplicantLanguage(page, 'ar')
      await validateAccessibility(page)

      await validateScreenshot(
        page.locator('main'),
        'pre-screener-upsell-right-to-left',
        {
          mobileScreenshot: true,
        },
      )
    })

    test('shows pre-screener form as submitted after completion', async ({
      page,
      applicantQuestions,
      applicantProgramList,
    }) => {
      // Fill out pre-screener form, with eligible response
      await applicantQuestions.applyProgram(
        preScreenerProgramName,
        // pre-screener programs skip the program overview page
        /* showProgramOverviewPage= */ false,
      )
      await applicantQuestions.answerNumberQuestion(secondProgramCorrectAnswer)
      await applicantQuestions.clickContinue()
      await applicantQuestions.clickSubmitApplication()

      await applicantQuestions.expectPreScreenerConfirmationPage(
        /* wantUpsell= */ true,
        /* wantTrustedIntermediary= */ false,
        /* wantEligiblePrograms= */ [secondProgramName],
      )

      await page.click('text="Apply to programs"')
      await page.click('text="Continue without an account"')

      await expect(
        applicantProgramList
          .getCardLocator(
            CardSectionName.MyApplications,
            preScreenerProgramName,
          )
          .locator('div.bg-primary-lighter'),
      ).toBeVisible()
      await applicantProgramList.expectSubmittedTag(
        CardSectionName.MyApplications,
        preScreenerProgramName,
      )
      // Validate hidden label for accessibility.
      await expect(page.getByText('For your information: ')).toBeVisible()
      await validateAccessibility(page)
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

      // Fill out pre-screener form, with non-eligible response
      await applicantQuestions.applyProgram(
        preScreenerProgramName,
        // pre-screener programs skip the program overview page
        /* showProgramOverviewPage= */ false,
      )
      await applicantQuestions.answerNumberQuestion('4')
      await applicantQuestions.clickContinue()
      await applicantQuestions.clickSubmitApplication()

      await applicantQuestions.expectPreScreenerConfirmationPage(
        /* wantUpsell= */ false,
        /* wantTrustedIntermediary= */ true,
        /* wantEligiblePrograms= */ [],
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

      // Fill out pre-screener form, with eligible response
      await applicantQuestions.applyProgram(
        preScreenerProgramName,
        // pre-screener programs skip the program overview page
        /* showProgramOverviewPage= */ false,
      )
      await applicantQuestions.answerNumberQuestion(secondProgramCorrectAnswer)
      await applicantQuestions.clickContinue()
      await applicantQuestions.clickSubmitApplication()

      await applicantQuestions.expectPreScreenerConfirmationPage(
        /* wantUpsell= */ false,
        /* wantTrustedIntermediary= */ true,
        /* wantEligiblePrograms= */ [secondProgramName],
      )
    })
  })
})
