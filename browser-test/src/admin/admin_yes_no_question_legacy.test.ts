import {expect, test} from '../support/civiform_fixtures'
import {
  disableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'

test.describe('Yes/no options', () => {
  test.beforeEach(async ({page}) => {
    await disableFeatureFlag(
      page,
      'ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED',
    )
  })

  test('Renders options correctly', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)

    await test.step('Go to edit page for yes/no question', async () => {
      await adminQuestions.gotoAdminQuestionsPage()

      await page.click('#create-question-button')
      await page.click('#create-yes_no-question')
      await waitForPageJsLoad(page)
    })

    await test.step('Expect renders properly', async () => {
      const questionSettings = page.getByTestId('question-settings')
      await expect(page.getByTestId('yes-no-options-label')).toBeVisible()

      await validateScreenshot(
        questionSettings,
        'yes-no-question-admin-options',
      )
    })
  })
})
