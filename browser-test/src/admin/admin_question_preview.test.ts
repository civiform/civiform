import {expect, test} from '../support/civiform_fixtures'
import {loginAsAdmin, validateScreenshot} from '../support'

test.describe('Admin question preview', () => {
  const questionName = 'test-question'

  test.beforeEach(async ({page}) => {
    await loginAsAdmin(page)
  })

  test('Preview whole page', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      // Question type doesn't matter for this test case
      await adminQuestions.addCurrencyQuestion({
        questionName: questionName,
      })
    })

    await test.step('Use screenshot to verify layout of entire page', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(page, 'question-preview-page')
    })
  })

  test('Preview address question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addAddressQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#question-fragment'),
        'address-question',
      )
    })
  })

  test('Preview checkbox question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addCheckboxQuestion({
        questionName: questionName,
        options: [
          {adminName: 'a', text: 'a'},
          {adminName: 'b', text: 'b'},
          {adminName: 'c', text: 'c'},
        ],
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#question-fragment'),
        'checkbox-question',
      )
    })
  })

  test('Preview currency question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addCurrencyQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#question-fragment'),
        'currency-question',
      )
    })
  })

  test('Preview date question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addDateQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#question-fragment'),
        'date-question',
      )
    })
  })

  test('Preview dropdown question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addDropdownQuestion({
        questionName: questionName,
        options: [
          {adminName: 'a', text: 'a'},
          {adminName: 'b', text: 'b'},
          {adminName: 'c', text: 'c'},
        ],
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#question-fragment'),
        'dropdown-question',
      )
    })
  })

  test('Preview email question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addEmailQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#question-fragment'),
        'email-question',
      )
    })
  })

  // TODO(#7859): Fully support enumerator question previews
  test('Preview enumerator question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addEnumeratorQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#question-fragment'),
        'enumerator-question',
      )
    })
  })

  test('File upload preview', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addFileUploadQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#sample-question'),
        'file-question-preview',
      )
    })
  })

  test('Preview ID question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addIdQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#question-fragment'),
        'id-question',
      )
    })
  })

  test('Preview name question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addNameQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#question-fragment'),
        'name-question',
      )
    })
  })

  test('Preview number question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addNumberQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#question-fragment'),
        'number-question',
      )
    })
  })

  test('Edit existing radio button question options', async ({
    page,
    adminQuestions,
  }) => {
    await test.step('Create question', async () => {
      await adminQuestions.addRadioButtonQuestion({
        questionName: questionName,
        options: [
          {adminName: 'a', text: 'Old Option'},
          {adminName: 'b', text: 'b'},
          {adminName: 'c', text: 'c'},
        ],
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

      await validateScreenshot(
        page.locator('#question-fragment'),
        'radio-question',
      )
    })
  })

  test('Preview static text', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addStaticQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#question-fragment'),
        'static-question',
      )
    })
  })

  test('Preview phone number question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addPhoneQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#question-fragment'),
        'phone-question',
      )
    })
  })

  test('Preview text question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addTextQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(
        page.locator('#question-fragment'),
        'text-question',
      )
    })
  })
})
