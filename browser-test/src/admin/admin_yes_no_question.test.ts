import {expect, test} from '../support/civiform_fixtures'
import {loginAsAdmin, validateScreenshot, waitForPageJsLoad} from '../support'

test.describe('Yes/no options', () => {
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

test.describe('Yes/no translations', () => {
  test('renders translation screen with pre-translated message only', async ({
    page,
    adminQuestions,
  }) => {
    await test.step('Create a yes/no question', async () => {
      await loginAsAdmin(page)
      await adminQuestions.gotoAdminQuestionsPage()

      await adminQuestions.addYesNoQuestion({
        questionName: 'yes-no-question',
        description: 'do you agree?',
      })
    })

    await test.step('Navigate to translation editor', async () => {
      await adminQuestions.goToQuestionTranslationPage('yes-no-question')
    })

    await test.step('Verify message and hide answer options', async () => {
      await expect(
        page.getByText('Yes/No question options are pre-translated.'),
      ).toBeVisible()
      await expect(page.getByText('Answer options')).toHaveCount(0)
    })
  })
})
