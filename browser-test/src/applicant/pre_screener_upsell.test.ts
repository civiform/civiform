import {expect, test} from '../support/civiform_fixtures'
import {
  ClientInformation,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateScreenshot,
  validateAccessibility,
  loginAsTrustedIntermediary,
  waitForPageJsLoad,
} from '../support'
import {ProgramVisibility} from '../support/admin_programs'

test.describe(
  'North Star Pre-Screener Upsell Tests',
  {tag: ['@northstar']},
  () => {
    const programName = 'Pre-Screener Program'
    const eligibleProgram1 = 'Eligible Program 1'

    test.beforeEach(async ({page, adminPrograms}) => {
      await loginAsAdmin(page)

      await test.step('Setup: Publish pre-screener program', async () => {
        await adminPrograms.addPreScreenerNS(
          programName,
          'Short description',
          ProgramVisibility.PUBLIC,
        )
        await adminPrograms.publishProgram(programName)
        await adminPrograms.expectActiveProgram(programName)
      })
    })

    test('view application submitted page with one eligible program', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await test.step('Setup: publish one program', async () => {
        await adminPrograms.addProgram(eligibleProgram1)
        await adminPrograms.publishProgram(eligibleProgram1)
        await logout(page)
      })

      await loginAsTestUser(page)

      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await test.step('Setup: submit application', async () => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      await test.step('Verify output', async () => {
        await expect(page.getByText(eligibleProgram1)).toBeVisible()
        await expect(
          page.getByText('Programs you may qualify for'),
        ).toBeVisible()

        await validateScreenshot(
          page,
          'upsell-north-star-pre-screener',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )

        await validateAccessibility(page)
      })

      await test.step('Click "Apply to Programs" and return to homepage', async () => {
        await applicantQuestions.clickApplyToProgramsButton()
        await applicantQuestions.expectProgramsPage()
      })
    })

    test('As guest, validate login link in alert', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await test.step('Setup: publish one program', async () => {
        await adminPrograms.addProgram(eligibleProgram1)
        await adminPrograms.publishProgram(eligibleProgram1)
        await logout(page)
      })

      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await test.step('Setup: submit application', async () => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      await test.step('Validate the sign in link logs the user in and navigates to the home page', async () => {
        await expect(
          page.getByText('To access your application later, create an account'),
        ).toBeVisible()
        // Validate help text for accessibility.
        await expect(
          page.getByLabel(
            'For your information: To access your application later, create an account',
          ),
        ).toBeVisible()

        await loginAsTestUser(
          page,
          'a:has-text("Sign in to an existing account")',
        )
        await applicantQuestions.expectProgramsPage()
      })
    })

    test('view application submitted page with zero eligible programs', async ({
      page,
      applicantQuestions,
    }) => {
      await logout(page) // Log out as admin
      await loginAsTestUser(page)

      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await test.step('Setup: submit application', async () => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      await expect(
        page.getByText(
          'The pre-screener could not find programs you may qualify for at this time',
        ),
      ).toBeVisible()

      // TODO(#8178): Click "Edit my responses" and verify after behavior is finalized by UX.
      // Then return to the pre-screener ineligible page

      await test.step('Click "Apply to Programs" and return to homepage', async () => {
        await applicantQuestions.clickApplyToProgramsButton()
        await applicantQuestions.expectProgramsPage()
      })
    })

    test('As a guest, clicking on apply to more programs brings up login dialog', async ({
      page,
      applicantQuestions,
    }) => {
      await logout(page) // Log out as admin

      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await test.step('Setup: submit application', async () => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      await applicantQuestions.clickApplyToProgramsButton()

      await validateScreenshot(
        page,
        'upsell-north-star-pre-screener-login',
        /* fullPage= */ false,
      )

      await validateAccessibility(page)
    })

    test('As TI, view application submitted page with one eligible program', async ({
      page,
      tiDashboard,
      adminPrograms,
      applicantQuestions,
    }) => {
      await test.step('Setup: publish one program', async () => {
        await adminPrograms.addProgram(eligibleProgram1)
        await adminPrograms.publishProgram(eligibleProgram1)
        await logout(page)
      })

      await test.step('Create client', async () => {
        await loginAsTrustedIntermediary(page)
        await tiDashboard.gotoTIDashboardPage(page)
        await waitForPageJsLoad(page)
        const client: ClientInformation = {
          emailAddress: 'test@sample.com',
          firstName: 'first',
          middleName: 'middle',
          lastName: 'last',
          dobDate: '2021-06-10',
        }
        await tiDashboard.createClient(client, true)
        await tiDashboard.expectDashboardContainClient(client)
      })

      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await test.step('Setup: submit application', async () => {
        await tiDashboard.clickOnViewApplications()
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      await test.step('Verify output', async () => {
        await expect(
          page.getByText('Programs your client may qualify for'),
        ).toBeVisible()
        await expect(page.getByText(eligibleProgram1)).toBeVisible()
      })
    })

    test('As TI, view application submitted page with 0 eligible programs', async ({
      page,
      tiDashboard,
      applicantQuestions,
    }) => {
      await logout(page) // Log out as admin

      await test.step('Create client', async () => {
        await loginAsTrustedIntermediary(page)
        await tiDashboard.gotoTIDashboardPage(page)
        await waitForPageJsLoad(page)
        const client: ClientInformation = {
          emailAddress: 'test@sample.com',
          firstName: 'first',
          middleName: 'middle',
          lastName: 'last',
          dobDate: '2021-06-10',
        }
        await tiDashboard.createClient(client, true)
        await tiDashboard.expectDashboardContainClient(client)
      })

      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await test.step('Setup: submit application', async () => {
        await tiDashboard.clickOnViewApplications()
        // Validate accessibility label
        await expect(
          page.getByText(
            'For your information: You are applying for last, first. Are you trying to apply for a different client?',
          ),
        ).toBeHidden()
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      await expect(
        page.getByText(
          'The pre-screener could not find programs your client may qualify for at this time',
        ),
      ).toBeVisible()

      // TODO(#8178): Click "Edit my responses" and verify after behavior is finalized by UX
    })

    test('applies color theming on submitted page', async ({
      page,
      adminSettings,
      applicantQuestions,
    }) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
      await enableFeatureFlag(page, 'CUSTOM_THEME_COLORS_ENABLED')
      await adminSettings.gotoAdminSettings()

      await adminSettings.setStringSetting('THEME_COLOR_PRIMARY', '#6d4bfa')
      await adminSettings.setStringSetting(
        'THEME_COLOR_PRIMARY_DARK',
        '#a72f10',
      )

      await adminSettings.saveChanges()
      await logout(page)

      await test.step('Setup: submit application', async () => {
        await applicantQuestions.clickApplyProgramButton(programName)
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      await validateScreenshot(
        page,
        'submitted-page-theme',
        /* fullPage= */ true,
      )
    })
  },
)
