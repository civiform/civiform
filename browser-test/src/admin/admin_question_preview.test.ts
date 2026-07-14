import {expect, test} from '../support/civiform_fixtures'
import {loginAsAdmin, validateScreenshot} from '../support'
import {SAMPLE_QUESTIONS} from '../support/seeding'

test.describe('Admin question preview', () => {
  test.beforeEach(async ({page, seeding}) => {
    await seeding.seedQuestions()
    await loginAsAdmin(page)
  })

  test('Preview whole page', async ({page, adminQuestions}) => {
    // Question type doesn't matter for this test case
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.currency)

    await validateScreenshot(page, 'question-preview-page')
  })

  test('Preview address question', async ({page, adminQuestions}) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.address)

    await validateScreenshot(
      page.locator('#question-fragment'),
      'address-question',
    )
  })

  test('Preview checkbox question', async ({page, adminQuestions}) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.checkbox)

    await validateScreenshot(
      page.locator('#question-fragment'),
      'checkbox-question',
    )
  })

  test('Preview currency question', async ({page, adminQuestions}) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.currency)

    await validateScreenshot(
      page.locator('#question-fragment'),
      'currency-question',
    )
  })

  test('Preview date question', async ({page, adminQuestions}) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.date)

    await validateScreenshot(
      page.locator('#question-fragment'),
      'date-question',
    )
  })

  test('Preview dropdown question', async ({page, adminQuestions}) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.dropdown)

    await validateScreenshot(
      page.locator('#question-fragment'),
      'dropdown-question',
    )
  })

  test('Preview email question', async ({page, adminQuestions}) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.email)

    await validateScreenshot(
      page.locator('#question-fragment'),
      'email-question',
    )
  })

  // TODO(#7859): Fully support enumerator question previews
  test('Preview enumerator question', async ({page, adminQuestions}) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.enumerator)

    await validateScreenshot(
      page.locator('#question-fragment'),
      'enumerator-question',
    )
  })

  test('File upload preview', async ({page, adminQuestions}) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.fileUpload)

    await validateScreenshot(
      page.locator('#sample-question'),
      'file-question-preview',
    )
  })

  test('Preview ID question', async ({page, adminQuestions}) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.id)

    await validateScreenshot(page.locator('#question-fragment'), 'id-question')
  })

  test('Preview name question', async ({page, adminQuestions}) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.name)

    await validateScreenshot(
      page.locator('#question-fragment'),
      'name-question',
    )
  })

  test('Preview number question', async ({page, adminQuestions}) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.number)

    await validateScreenshot(
      page.locator('#question-fragment'),
      'number-question',
    )
  })

  test('Edit existing radio button question options', async ({
    page,
    adminQuestions,
  }) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.radioButton)

    await test.step('Verify existing option is visible', async () => {
      const previewDiv = page.locator('#sample-question')
      await expect(
        previewDiv.locator('text=Winter (will hide next block)'),
      ).toBeVisible()
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
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.staticContent)

    await validateScreenshot(
      page.locator('#question-fragment'),
      'static-question',
    )
  })

  test('Preview phone number question', async ({page, adminQuestions}) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.phone)

    await validateScreenshot(
      page.locator('#question-fragment'),
      'phone-question',
    )
  })

  test('Preview text question', async ({page, adminQuestions}) => {
    await adminQuestions.gotoQuestionEditPage(SAMPLE_QUESTIONS.text)

    await validateScreenshot(
      page.locator('#question-fragment'),
      'text-question',
    )
  })
})
