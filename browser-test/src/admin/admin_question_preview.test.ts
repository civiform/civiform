import {expect, test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'

test.describe('File upload question preview', () => {
  test('File upload preview', async ({page, adminQuestions}) => {
    const fileUploadQuestionName = 'File Upload Question'

    await loginAsAdmin(page)
    await test.step('Create question', async () => {
      await adminQuestions.addFileUploadQuestion({
        questionName: fileUploadQuestionName,
      })
    })

    await test.step('Expect preview renders properly multi-file disabled', async () => {
      await adminQuestions.gotoQuestionEditPage(fileUploadQuestionName)

      await validateScreenshot(
        page.locator('#sample-question'),
        'file-question-preview-single-file',
      )
    })

    await enableFeatureFlag(page, 'multiple_file_upload_enabled')

    await test.step('Expect preview renders properly multi-file enabled', async () => {
      await adminQuestions.gotoQuestionEditPage(fileUploadQuestionName)

      await validateScreenshot(
        page.locator('#sample-question'),
        'file-question-preview',
      )
    })
  })
})

test.describe('Admin question preview', {tag: ['@northstar']}, () => {
  const questionName = 'test-question'

  test.beforeEach(async ({page}) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'north_star_applicant_ui')
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

      await validateScreenshot(page, 'north-star-question-preview-page')
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

      // The address question needs extra time to render
      await page.waitForSelector('[data-load-question="true"]')

      await validateScreenshot(
        page.locator('#question-fragment'),
        'north-star-address-question',
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
        'north-star-checkbox-question',
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
        'north-star-currency-question',
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
        'north-star-date-question',
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
        'north-star-dropdown-question',
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
        'north-star-email-question',
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
        'north-star-enumerator-question',
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
        'north-star-file-question-preview',
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
        'north-star-id-question',
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
        'north-star-name-question',
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
        'north-star-number-question',
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
        'north-star-radio-question',
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
        'north-star-static-question',
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
        'north-star-phone-question',
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
        'north-star-text-question',
      )
    })
  })
})
