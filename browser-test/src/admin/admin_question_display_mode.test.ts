import {expect, test} from '../support/civiform_fixtures'
import {disableFeatureFlag, enableFeatureFlag, loginAsAdmin} from '../support'
import {QuestionDisplayMode} from '../support/admin_questions'

test.describe('Question display mode', () => {
  test.beforeEach(async ({page}) => {
    await loginAsAdmin(page)
  })

  test('Set display mode on question', async ({page, adminQuestions}) => {
    const visibleQuestionName: string = 'text-question-visible'
    const hiddenQuestionName: string = 'text-question-hidden'

    await enableFeatureFlag(page, 'API_BRIDGE_ENABLED')

    await test.step('Add visible text question', async () => {
      await adminQuestions.addTextQuestion({
        questionName: visibleQuestionName,
        displayMode: QuestionDisplayMode.VISIBLE,
      })

      await adminQuestions.gotoQuestionEditPage(visibleQuestionName)
      await adminQuestions.expectDisplayModeCheck(QuestionDisplayMode.VISIBLE)
    })

    await test.step('Add hidden text question', async () => {
      await adminQuestions.addTextQuestion({
        questionName: hiddenQuestionName,
        displayMode: QuestionDisplayMode.HIDDEN,
      })

      await adminQuestions.gotoQuestionEditPage(hiddenQuestionName)
      await adminQuestions.expectDisplayModeCheck(QuestionDisplayMode.HIDDEN)
    })
  })

  test('Defaults to visible display mode', async ({page, adminQuestions}) => {
    const noDisplayModeQuestionName: string = 'text-question-no-display-mode'

    await disableFeatureFlag(page, 'API_BRIDGE_ENABLED')

    await test.step('Add visible text question', async () => {
      await adminQuestions.addTextQuestion({
        questionName: noDisplayModeQuestionName,
      })

      await adminQuestions.gotoQuestionEditPage(noDisplayModeQuestionName)

      await expect(
        page.getByRole('group', {name: 'Display Mode'}),
      ).not.toBeAttached()
    })
  })
})
