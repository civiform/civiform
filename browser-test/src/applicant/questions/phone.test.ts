import {test, expect} from '@playwright/test'
import {
  createTestContext,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('phone question for applicant flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  test.describe('single phone question', () => {
    const programName = 'Test program for single phone q'

    test.beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      // As admin, create program with a free form text question.
      await loginAsAdmin(page)

      await adminQuestions.addPhoneQuestion({
        questionName: 'phone-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['phone-q'],
        programName,
      )

      await logout(page)
    })

    test('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'phone')
    })

    test('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'phone-errors')
    })

    test('with phone submits successfully', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('4256373270')
      await validateScreenshot(page, 'phone-format-usa')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with canada phone submits successfully', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('2507274212')

      await validateScreenshot(page, 'phone-format-ca')

      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()
    })

    test('with empty phone does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      // Click next without inputting anything
      await applicantQuestions.clickNext()

      const textId = '.cf-question-phone'
      expect(await page.innerText(textId)).toContain('Phone number is required')
    })

    test('invalid phone numbers', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('1234567890')

      await applicantQuestions.clickNext()

      const countryCodeId = '.cf-question-phone'
      expect(await page.innerText(countryCodeId)).toContain(
        'This phone number is invalid',
      )
    })

    test('555 fake phone numbers', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('5553231234')

      await applicantQuestions.clickNext()
      const countryCodeId = '.cf-question-phone'
      expect(await page.innerText(countryCodeId)).toContain(
        'This phone number is invalid',
      )
    })

    test('invalid length of phone number when only valid characters are included', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('123###1212')

      await applicantQuestions.clickNext()
      const countryCodeId = '.cf-question-phone'
      expect(await page.innerText(countryCodeId)).toContain(
        'This phone number is invalid',
      )
    })

    test('invalid characters in phone numbers', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('123###1212121')

      await applicantQuestions.clickNext()
      const countryCodeId = '.cf-question-phone'
      expect(await page.innerText(countryCodeId)).toContain(
        'This phone number is invalid',
      )
    })

    test('incorrect length of phone number', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('615974')

      await applicantQuestions.clickNext()
      const countryCodeId = '.cf-question-phone'
      expect(await page.innerText(countryCodeId)).toContain(
        'Phone number is required',
      )
    })

    test('hitting enter on phone does not trigger submission', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('2507274212.')

      // Ensure that clicking enter while on phone input doesn't trigger form
      // submission.
      await page.focus('input[type=text]')
      await page.keyboard.press('Enter')
      await expect(page.locator('input[type=text]')).toBeVisible()

      // Check that pressing Enter on button works.
      await page.focus('button:has-text("Save and next")')
      await page.keyboard.press('Enter')
      await applicantQuestions.expectReviewPage()

      // Go back to question and ensure that "Review" button is also clickable
      // via Enter.
      await applicantQuestions.clickEdit()
      await page.focus('text="Review"')
      await page.keyboard.press('Enter')
      await applicantQuestions.expectReviewPage()
    })

    test('has no accessiblity violations', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })

  test.describe('multiple phone questions', () => {
    const programName = 'Test program for multiple phone qs'

    test.beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

      await adminQuestions.addPhoneQuestion({
        questionName: 'firstphoneq',
      })
      await adminQuestions.addPhoneQuestion({
        questionName: 'secondphoneq',
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['secondphoneq'],
        'firstphoneq', // optional
      )
      await adminPrograms.publishAllDrafts()

      await logout(page)
    })

    test('with both selections submits successfully', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('2507274212', 0)
      await applicantQuestions.answerPhoneQuestion('4256373270', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with unanswered optional question submits successfully', async () => {
      const {applicantQuestions} = ctx
      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('4256373270', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with first invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('1234567320', 0)
      await applicantQuestions.answerPhoneQuestion('4256373270', 1)
      await applicantQuestions.clickNext()

      const locatorId = '[name="applicant.firstphoneq.phone_number"]'
      const parentElement = page.locator(locatorId).locator('..')

      expect(await parentElement.innerText()).toContain(
        'This phone number is invalid',
      )
    })

    test('with second invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('4256373270', 0)
      await applicantQuestions.answerPhoneQuestion('1234567320', 1)
      await applicantQuestions.clickNext()

      const textId = `.cf-question-phone >> nth=1`
      expect(await page.innerText(textId)).toContain(
        'This phone number is invalid',
      )
    })
  })
})
