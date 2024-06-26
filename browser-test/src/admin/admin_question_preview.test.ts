import {expect, test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'

test.describe('Admin question preview', {tag: ['@northstar']}, () => {
  const questionName = 'test-question'

  test.beforeEach(async ({page}) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'north_star_applicant_ui')
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

      await validateScreenshot(page, 'north-star-address-question')
    })
  })

  // TODO(#7891): Support checkbox question previews

  test('Preview currency question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addCurrencyQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(page, 'north-star-currency-question')
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

      await validateScreenshot(page, 'north-star-date-question')
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

      await validateScreenshot(page, 'north-star-dropdown-question')
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

      await validateScreenshot(page, 'north-star-email-question')
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

      await validateScreenshot(page, 'north-star-enumerator-question')
    })
  })

  // TODO(#7849): Support file upload question previews

  test('Preview ID question', async ({page, adminQuestions}) => {
    await test.step('Create question', async () => {
      await adminQuestions.addIdQuestion({
        questionName: questionName,
      })
    })

    await test.step('Expect preview renders properly', async () => {
      await adminQuestions.gotoQuestionEditPage(questionName)

      await validateScreenshot(page, 'north-star-id-question')
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

      await validateScreenshot(page, 'north-star-name-question')
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

      await validateScreenshot(page, 'north-star-number-question')
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

      await validateScreenshot(page, 'north-star-radio-question')
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

      await validateScreenshot(page, 'north-star-static-question')
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

      await validateScreenshot(page, 'north-star-phone-question')
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

      await validateScreenshot(page, 'north-star-text-question')
    })
  })
})
