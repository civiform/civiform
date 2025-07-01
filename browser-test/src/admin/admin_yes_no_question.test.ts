import {test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'

test.describe('Yes/no options', () => {
  test('Renders options correctly', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'north_star_applicant_ui')
    await enableFeatureFlag(page, 'yes_no_question_enabled')

    await test.step('Go to edit page for yes/no question', async () => {
      await adminQuestions.gotoAdminQuestionsPage()

      await page.click('#create-question-button')
      await page.click('#create-yes_no-question')
      await waitForPageJsLoad(page)
    })

    await test.step('Expect renders properly', async () => {
      await validateScreenshot(page, 'yes-no-question-admin-options')
    })
  })
})
