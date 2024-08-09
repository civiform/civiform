import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateScreenshot,
  validateAccessibility,
} from '../support'
import {Page} from 'playwright'

test.describe('Upsell tests', {tag: ['@northstar']}, () => {
  const programName = 'Sample program'
  const customConfirmationText =
    'Custom confirmation message for sample program'

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
        customConfirmationText,
      )
      await adminPrograms.publishProgram(programName)
      await adminPrograms.expectActiveProgram(programName)
      await logout(page)
    })
  })

  test('view application submitted page while logged in', async ({
    page,
    applicantQuestions,
  }) => {
    await loginAsTestUser(page)

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await test.step('Submit application', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    await validateApplicationSubmittedPage(page)

    await test.step('Validate screenshot and accessibility', async () => {
      await validateScreenshot(
        page,
        'upsell-north-star',
        /* fullPage= */ true,
        /* mobileScreenshot= */ true,
      )
    })

    await validateAccessibility(page)

    await test.step('Validate that user can click through without logging in', async () => {
      await applicantQuestions.clickBackToHomepageButton()
      await expect(page.locator('[data-testId="login"]')).toBeHidden()
    })
  })

  test('view application submitted page while logged out', async ({
    page,
    applicantQuestions,
  }) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await test.step('Submit application', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    await validateApplicationSubmittedPage(page)

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

  async function validateApplicationSubmittedPage(page: Page) {
    await test.step('Validate application submitted page', async () => {
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
    })
  }
})
