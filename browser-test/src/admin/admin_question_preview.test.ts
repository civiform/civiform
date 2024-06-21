import {test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'

test.describe('Admin question preview', () => {
  const questionName = 'color'

  test('Preview text question', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addTextQuestion({
      questionName: questionName,
      questionText: 'What is your favorite color?',
      helpText: 'Some ideas: red, blue, yellow',
      markdown: true,
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await adminQuestions.gotoQuestionEditPage(questionName)

    // TODO ssandbekkhaug Question text is not updated on page load
    await validateScreenshot(page, 'text-question')
  })

  // TODO ssandbekkhaug test other question types?
})
