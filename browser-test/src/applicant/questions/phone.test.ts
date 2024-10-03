import {Page} from '@playwright/test'
import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('phone question for applicant flow', () => {
  test.describe('single phone question', () => {
    const programName = 'Test program for single phone q'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await setUpForSingleQuestion(
        programName,
        page,
        adminQuestions,
        adminPrograms,
      )
    })

    test('validate screenshot', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'phone')
    })

    test('validate screenshot with errors', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'phone-errors')
    })

    test('with phone submits successfully', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('4256373270')
      await validateScreenshot(page, 'phone-format-usa')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with canada phone submits successfully', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('2507274212')

      await validateScreenshot(page, 'phone-format-ca')

      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()
    })

    test('with empty phone does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      // Click next without inputting anything
      await applicantQuestions.clickNext()

      const textId = '.cf-question-phone'
      expect(await page.innerText(textId)).toContain('Phone number is required')
    })

    test('invalid phone numbers', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('1234567890')

      await applicantQuestions.clickNext()

      const countryCodeId = '.cf-question-phone'
      expect(await page.innerText(countryCodeId)).toContain(
        'This phone number is invalid',
      )
    })

    test('555 fake phone numbers', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('5553231234')

      await applicantQuestions.clickNext()
      const countryCodeId = '.cf-question-phone'
      expect(await page.innerText(countryCodeId)).toContain(
        'This phone number is invalid',
      )
    })

    test('invalid length of phone number when only valid characters are included', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('123###1212')

      await applicantQuestions.clickNext()
      const countryCodeId = '.cf-question-phone'
      expect(await page.innerText(countryCodeId)).toContain(
        'This phone number is invalid',
      )
    })

    test('invalid characters in phone numbers', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('123###1212121')

      await applicantQuestions.clickNext()
      const countryCodeId = '.cf-question-phone'
      expect(await page.innerText(countryCodeId)).toContain(
        'This phone number is invalid',
      )
    })

    test('incorrect length of phone number', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('615974')

      await applicantQuestions.clickNext()
      const countryCodeId = '.cf-question-phone'
      expect(await page.innerText(countryCodeId)).toContain(
        'Phone number is required',
      )
    })

    test('hitting enter on phone does not trigger submission', async ({
      page,
      applicantQuestions,
    }) => {
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

    test('has no accessiblity violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })

  test.describe('multiple phone questions', () => {
    const programName = 'Test program for multiple phone qs'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
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

    test('with both selections submits successfully', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('2507274212', 0)
      await applicantQuestions.answerPhoneQuestion('4256373270', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with unanswered optional question submits successfully', async ({
      applicantQuestions,
    }) => {
      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerPhoneQuestion('4256373270', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with first invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
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

    test('with second invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
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

  test.describe(
    'single phone question with North Star flag enabled',
    {tag: ['@northstar']},
    () => {
      const programName = 'Test program for single phone q'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await setUpForSingleQuestion(
          programName,
          page,
          adminQuestions,
          adminPrograms,
        )
        await enableFeatureFlag(page, 'north_star_applicant_ui')
      })

      test('validate screenshot', async ({page, applicantQuestions}) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await test.step('Screenshot without errors', async () => {
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'phone-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ true,
          )
        })

        await test.step('Screenshot with errors', async () => {
          await applicantQuestions.clickContinue()
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'phone-errors-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ true,
          )
        })
      })

      test('has no accessiblity violations', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await validateAccessibility(page)
      })
    },
  )

  async function setUpForSingleQuestion(
    programName: string,
    page: Page,
    adminQuestions: AdminQuestions,
    adminPrograms: AdminPrograms,
  ) {
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
  }
})
