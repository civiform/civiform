import {expect, test} from '../support/civiform_fixtures'
import {disableFeatureFlag, loginAsAdmin} from '../support'

test.describe('Yes/no translations', () => {
  test.beforeEach(async ({page}) => {
    await disableFeatureFlag(
      page,
      'ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED',
    )
  })

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
