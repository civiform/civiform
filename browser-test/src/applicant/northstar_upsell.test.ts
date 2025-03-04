import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  disableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  testUserDisplayName,
  logout,
  validateScreenshot,
  validateAccessibility,
  AdminPrograms,
  ApplicantQuestions,
} from '../support'
import {Page} from 'playwright'

test.describe('Upsell tests', {tag: ['@northstar']}, () => {
  const programName = 'Sample program'
  const customConfirmationText =
    'Custom confirmation message for sample program'

  const relatedProgramsHeading = 'Other programs you might be interested in'
  const relatedProgramName = 'Related program'

  test.beforeEach(async ({page, adminPrograms}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await loginAsAdmin(page)

    await test.step('Setup: Publish program as admin', async () => {
      await adminPrograms.addProgram(
        programName,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        customConfirmationText,
      )
      await adminPrograms.publishProgram(programName)
      await adminPrograms.expectActiveProgram(programName)

      await logout(page)
    })
  })

  test('view application submitted page while logged in', async ({
    page,
    adminPrograms,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    // Create a second program for the related programs section
    await createRelatedProgram(page, adminPrograms)

    await loginAsTestUser(page)

    await enableFeatureFlag(page, 'application_exportable')
    await enableFeatureFlag(
      page,
      'suggest_programs_on_application_confirmation_page',
    )

    await test.step('Submit application', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantProgramOverview.startApplicationFromProgramOverviewPage(
        programName,
      )
      await applicantQuestions.clickSubmitApplication()
    })

    await validateApplicationSubmittedPage(
      page,
      /* expectRelatedProgram= */ true,
      applicantQuestions,
    )

    await validateApplicationDownloadLink(
      page,
      /* expectedDownloadApplicationLink= */ true,
    )

    await test.step('Validate screenshot', async () => {
      await validateScreenshot(
        page,
        'upsell-north-star',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    await validateAccessibility(page)

    await test.step('Validate that user can return to the homepage without logging in', async () => {
      await applicantQuestions.clickBackToHomepageButton()
      // Expect the login dialog did not appear, so the user should already see the homepage
      await page.waitForURL('**/programs')
      // Expect the user is still logged in
      await expect(page.getByRole('banner')).toContainText(
        `Logged in as ${testUserDisplayName()}`,
      )
    })
  })

  test('view application submitted page with related program cards feature disabled', async ({
    page,
    adminPrograms,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    // This test will only validate that no related programs are shown when the
    // suggest_programs_on_application_confirmation_page flag is disabled
    await createRelatedProgram(page, adminPrograms)
    await loginAsTestUser(page)

    await enableFeatureFlag(page, 'application_exportable')
    await disableFeatureFlag(
      page,
      'suggest_programs_on_application_confirmation_page',
    )

    await test.step('Submit application', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantProgramOverview.startApplicationFromProgramOverviewPage(
        programName,
      )
      await applicantQuestions.clickSubmitApplication()
    })

    await validateApplicationSubmittedPage(
      page,
      /* expectRelatedProgram= */ false,
      applicantQuestions,
    )
  })

  test('view application submitted page while logged in without download link', async ({
    page,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    // This test will only validate that the download link is no longer visible.
    await loginAsTestUser(page)

    await disableFeatureFlag(page, 'application_exportable')

    await test.step('Submit application', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantProgramOverview.startApplicationFromProgramOverviewPage(
        programName,
      )
      await applicantQuestions.clickSubmitApplication()
    })

    await validateApplicationDownloadLink(
      page,
      /* expectedDownloadApplicationLink= */ false,
    )
  })

  test('view application submitted page while logged out', async ({
    page,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    await enableFeatureFlag(page, 'application_exportable')

    await test.step('Submit application', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantProgramOverview.startApplicationFromProgramOverviewPage(
        programName,
      )
      await applicantQuestions.clickSubmitApplication()
    })

    await validateApplicationSubmittedPage(
      page,
      /* expectRelatedProgram= */ false,
      applicantQuestions,
    )

    await validateApplicationDownloadLink(
      page,
      /* expectedDownloadApplicationLink= */ true,
    )

    await test.step('Validate that login dialog is shown when user clicks on apply to another program', async () => {
      await applicantQuestions.clickBackToHomepageButton()
      await expect(page.getByText('Create an account or sign in')).toBeVisible()

      await validateScreenshot(
        page,
        'upsell-north-star-login',
        /* fullPage= */ false,
        /* mobileScreenshot= */ true,
      )

      await validateAccessibility(page)
    })
  })

  test('view application submitted page while logged out without download link', async ({
    page,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    // This test will only validate that the download link is no longer visible.
    await disableFeatureFlag(page, 'application_exportable')

    await test.step('Submit application', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantProgramOverview.startApplicationFromProgramOverviewPage(
        programName,
      )
      await applicantQuestions.clickSubmitApplication()
    })

    await validateApplicationDownloadLink(
      page,
      /* expectedDownloadApplicationLink= */ false,
    )
  })

  test('Validate login link in alert', async ({
    page,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    await test.step('Submit application', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantProgramOverview.startApplicationFromProgramOverviewPage(
        programName,
      )
      await applicantQuestions.clickSubmitApplication()
    })

    await test.step('Validate the sign in link logs the user in and navigates to the home page', async () => {
      await expect(
        page.getByText('To access your application later, create an account'),
      ).toBeVisible()

      await loginAsTestUser(
        page,
        'a:has-text("Sign in to an existing account")',
      )
      await applicantQuestions.expectProgramsPage()
    })
  })

  test('Page does not show programs the user already applied to', async ({
    page,
    adminPrograms,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    // Create a second program for the related programs section
    await createRelatedProgram(page, adminPrograms)

    await loginAsTestUser(page)

    await test.step('Submit application', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantProgramOverview.startApplicationFromProgramOverviewPage(
        programName,
      )
      await applicantQuestions.clickSubmitApplication()
    })

    await applicantQuestions.clickBackToHomepageButton()

    await test.step('Apply to related program', async () => {
      await applicantQuestions.clickApplyProgramButton(relatedProgramName)
      await applicantProgramOverview.startApplicationFromProgramOverviewPage(
        relatedProgramName,
      )
      await applicantQuestions.clickSubmitApplication()
    })

    // The user submitted an application to the first program. Expect to not
    // see that program again.
    await expect(
      page.getByRole('heading', {
        name: programName,
      }),
    ).toBeHidden()
  })

  async function validateApplicationSubmittedPage(
    page: Page,
    expectRelatedProgram: boolean,
    applicantQuestions: ApplicantQuestions,
  ) {
    await test.step('Validate application submitted page', async () => {
      await applicantQuestions.expectTitle(page, 'Application confirmation')

      await expect(
        page.getByRole('heading', {name: programName, exact: true}),
      ).toBeVisible()
      await expect(
        page.getByRole('heading', {
          name: "You've submitted your " + programName + ' application',
        }),
      ).toBeVisible()
      await applicantQuestions.expectConfirmationPage(
        /* northStarEnabled= */ true,
      )
      await expect(page.getByText(customConfirmationText)).toBeVisible()

      if (expectRelatedProgram) {
        await expect(
          page.getByRole('heading', {
            name: relatedProgramsHeading,
          }),
        ).toBeVisible()
        await expect(page.getByText(relatedProgramName)).toBeVisible()
      } else {
        await expect(
          page.getByRole('heading', {
            name: relatedProgramsHeading,
          }),
        ).toBeHidden()
      }
    })
  }

  async function validateApplicationDownloadLink(
    page: Page,
    expectApplicationDownloadLink: boolean,
  ) {
    await test.step('Validate application download link', async () => {
      if (expectApplicationDownloadLink) {
        await expect(page.getByText('Download your application')).toBeVisible()
      } else {
        await expect(page.getByText('Download your application')).toBeHidden()
      }
    })
  }

  async function createRelatedProgram(
    page: Page,
    adminPrograms: AdminPrograms,
  ) {
    await test.step('Create related program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(relatedProgramName)
      await adminPrograms.publishProgram(relatedProgramName)
      await adminPrograms.expectActiveProgram(relatedProgramName)
      await logout(page)
    })
  }
})
