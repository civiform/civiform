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

  test.beforeEach(async ({page, adminPrograms, applicantQuestions}) => {
    await loginAsAdmin(page)

    await test.step('Setup: Publish program as admin', async () => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.publishProgram(programName)
      await adminPrograms.expectActiveProgram(programName)
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
  })

  test('view application submitted page', async ({page}) => {
    expect(await page.textContent('html')).toContain('Application confirmation')
    expect(await page.textContent('html')).toContain(programName)

    await validateScreenshot(
      page,
      'upsell-north-star',
      /* fullPage= */ true,
      /* mobileScreenshot= */ true,
    )
  })

  test('passes accessibility checks', async ({page}) => {
    await validateAccessibility(page)
  })
})
