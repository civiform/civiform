import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  disableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateScreenshot,
  validateAccessibility,
  AdminPrograms,
  ApplicantQuestions,
} from '../support'
import {ApplicantProgramOverview} from '../support/applicant_program_overview'
import {Page} from 'playwright'

test.describe('Upsell tests', {tag: ['@northstar']}, () => {
  const programName = 'Sample program'
  const customConfirmationText =
    'Custom confirmation message for sample program'

  const relatedProgramsHeading = 'Other programs you might be interested in'
  const relatedProgramName = 'Related program'

  test.beforeEach(async ({page, adminPrograms}) => {
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

    await enableFeatureFlag(page, 'north_star_applicant_ui')
    await enableFeatureFlag(page, 'application_exportable')
    await enableFeatureFlag(
      page,
      'suggest_programs_on_application_confirmation_page',
    )

    await submitApplication(applicantQuestions, applicantProgramOverview)

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
        'Logged in as testuser@example.com',
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

    await enableFeatureFlag(page, 'north_star_applicant_ui')
    await enableFeatureFlag(page, 'application_exportable')
    await disableFeatureFlag(
      page,
      'suggest_programs_on_application_confirmation_page',
    )

    await submitApplication(applicantQuestions, applicantProgramOverview)

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

    await enableFeatureFlag(page, 'north_star_applicant_ui')
    await disableFeatureFlag(page, 'application_exportable')

    await submitApplication(applicantQuestions, applicantProgramOverview)

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
    await enableFeatureFlag(page, 'north_star_applicant_ui')
    await enableFeatureFlag(page, 'application_exportable')

    await submitApplication(applicantQuestions, applicantProgramOverview)

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
    await enableFeatureFlag(page, 'north_star_applicant_ui')
    await disableFeatureFlag(page, 'application_exportable')

    await submitApplication(applicantQuestions, applicantProgramOverview)

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
    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await submitApplication(applicantQuestions, applicantProgramOverview)

    await test.step('Validate the login link logs the user in and navigates to the home page', async () => {
      await expect(
        page.getByText(
          'Create an account to save your application information',
        ),
      ).toBeVisible()

      await loginAsTestUser(page, 'a:has-text("Login to an existing account")')
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

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await submitApplication(applicantQuestions, applicantProgramOverview)

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

  test('program description supports markdown', async ({
    page,
    adminPrograms,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    await test.step('edit the long description to have markdown', async () => {
      await loginAsAdmin(page)
      await adminPrograms.goToProgramDescriptionPage(
        programName,
        /* createNewDraft= */ true,
      )
      await page
        .getByRole('textbox', {name: 'Long program description (optional)'})
        .fill(
          'This is the _program long description_ with markdown\n' +
            '[This is a link](https://www.example.com)\n' +
            'This is a list:\n' +
            '* Item 1\n' +
            '* Item 2\n' +
            '\n' +
            'There are some empty lines below this that should be preserved\n' +
            '\n' +
            '\n' +
            'Autodetected link: https://www.example.com\n',
        )

      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await loginAsTestUser(page)
    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await submitApplication(applicantQuestions, applicantProgramOverview)

    await validateScreenshot(
      page.locator('main'),
      'upsell-north-star-program-description-markdown',
      /* fullPage= */ false,
      /* mobileScreenshot= */ true,
    )
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
      await expect(page.getByText('Your submission information')).toBeVisible()
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

  async function submitApplication(
    applicantQuestions: ApplicantQuestions,
    applicantProgramOverview: ApplicantProgramOverview,
  ) {
    await test.step('Submit application', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantProgramOverview.startApplicationFromProgramOverviewPage(
        programName,
      )
      await applicantQuestions.clickSubmitApplication()
    })
  }
})
