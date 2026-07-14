import {expect, test} from '../support/civiform_fixtures'
import {disableFeatureFlag, enableFeatureFlag, loginAsAdmin} from '../support'
import {QuestionDisplayMode} from '../support/admin_questions'
import {SAMPLE_QUESTIONS} from '../support/seeding'

test.describe('Question display mode', () => {
  test.beforeEach(async ({page, seeding}) => {
    await seeding.seedQuestions()
    await loginAsAdmin(page)
  })

  test('Set display mode on question', async ({page, adminQuestions}) => {
    // Setting the display mode in the question creation form is the behavior
    // under test, so these questions are created via the UI.
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
    await disableFeatureFlag(page, 'API_BRIDGE_ENABLED')

    await test.step('Check seeded text question has no display mode section', async () => {
      await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.text)

      await expect(
        page.getByRole('group', {name: 'Display Mode'}),
      ).not.toBeAttached()
    })
  })
})
