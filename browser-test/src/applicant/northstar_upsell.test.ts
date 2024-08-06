import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateScreenshot,
  validateAccessibility,
} from '../support'

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

    await expect( page.textContent('html')).toContain('Application confirmation')
    await expect( page.textContent('html')).toContain(programName)
    await expect( page.textContent('html')).toContain(customConfirmationText)

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
      await applicantQuestions.clickApplyToAnotherProgramButton()
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

    await test.step('Validate that login dialog is shown when user clicks on apply to another program', async () => {
      await applicantQuestions.clickApplyToAnotherProgramButton()
      await expect(page.getByTestId('login')).toContainText(
        'Create an account or sign in',
      )
      await validateScreenshot(page, 'upsell-north-star-login')

      await validateAccessibility(page)
    })
  })
})
