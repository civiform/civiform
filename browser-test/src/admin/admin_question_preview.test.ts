import {expect, test} from '../support/civiform_fixtures'
import {loginAsAdmin} from '../support'

test.describe('Admin question preview', () => {
  const questionName = 'test-question'

  test('Edit existing radio button question options', async ({
    page,
    adminQuestions,
  }) => {
    await test.step('Setup: add radio button question', async () => {
      await loginAsAdmin(page)
      await adminQuestions.addRadioButtonQuestion({
        questionName: questionName,
        options: [{adminName: 'a', text: 'Old Option'}],
      })
    })

    await test.step('Verify Old Option is visible', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      const previewDiv = page.locator('#sample-question')
      await expect(previewDiv.locator('text=Old Option')).toBeVisible()
    })

    await test.step('Change the option text and verify the preview updates', async () => {
      await adminQuestions.changeMultiOptionAnswer(0, 'New Option')

      const previewDiv = page.locator('#sample-question')
      await expect(previewDiv.locator('text=New Option')).toBeVisible()
    })
  })
})
