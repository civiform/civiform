import {test, expect} from '../../support/civiform_fixtures'
import {
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('Text question for applicant flow', {tag: ['@uses-fixtures']}, () => {

  test.describe('single text question', () => {
    const programName = 'Test program for single text q'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      // As admin, create program with a free form text question.
      await loginAsAdmin(page)

      await adminQuestions.addTextQuestion({
        questionName: 'text-q',
        minNum: 5,
        maxNum: 20,
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['text-q'],
        programName,
      )

      await logout(page)
      await disableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test('validate screenshot', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'text')
    })

    test('validate screenshot with errors', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'text-errors')
    })

    test('with text submits successfully', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('I love CiviForm!')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test(
      'validate screenshot with north star flag enabled',
      {tag: ['@northstar']},
      async ({page, applicantQuestions}) => {
        await enableFeatureFlag(page, 'north_star_applicant_ui')
        await applicantQuestions.applyProgram(programName)

        await validateScreenshot(
          page,
          'text-north-star',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
      },
    )

    test(
      'validate screenshot with errors with north star flag enabled',
      {tag: ['@northstar']},
      async ({page, applicantQuestions}) => {
        await enableFeatureFlag(page, 'north_star_applicant_ui')
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.clickContinue()

        await validateScreenshot(
          page,
          'text-errors-north-star',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
      },
    )

    test('with empty text does not submit', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      // Click next without inputting anything
      await applicantQuestions.clickNext()

      const textId = '.cf-question-text'
      expect(await page.innerText(textId)).toContain(
        'This question is required.',
      )
    })

    test('with too short text does not submit', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('hi')
      await applicantQuestions.clickNext()

      const textId = '.cf-question-text'
      expect(await page.innerText(textId)).toContain(
        'Must contain at least 5 characters.',
      )
    })

    test('with too long text does not submit', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion(
        'A long string that exceeds the character limit',
      )
      await applicantQuestions.clickNext()

      const textId = '.cf-question-text'
      expect(await page.innerText(textId)).toContain(
        'Must contain at most 20 characters.',
      )
    })

    test('hitting enter on text does not trigger submission', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('I love CiviForm!', 0)

      // Ensure that clicking enter while on text input doesn't trigger form
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
  })

  test.describe('no max text question', () => {
    const programName = 'test-program-for-no-max-text-q'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      // As admin, create program with a free form text question.
      await loginAsAdmin(page)

      await adminQuestions.addTextQuestion({
        questionName: 'no-max-text-q',
        minNum: 5,
      })

      await adminPrograms.addAndPublishProgramWithQuestions(
        ['no-max-text-q'],
        programName,
      )

      await logout(page)
    })

    test('text that is too long is cut off at 10k characters', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      let largeString = ''
      for (let i = 0; i < 1000; i++) {
        largeString += '1234567890'
      }
      await applicantQuestions.answerTextQuestion(
        // 10k characters + extra characters that should be trimmed
        largeString + 'xxxxxxx',
      )
      await applicantQuestions.clickNext()

      // Scroll to bottom so end of text is in view.
      await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))

      // Should display answered question with "x"s cut off from the end.
      await validateScreenshot(page, 'text-max')

      // Form should submit with partial text entry.
      await applicantQuestions.submitFromReviewPage()
    })
  })

  test.describe('multiple text questions', () => {
    const programName = 'Test program for multiple text qs'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)

      await adminQuestions.addTextQuestion({
        questionName: 'first-text-q',
        minNum: 5,
        maxNum: 20,
      })
      await adminQuestions.addTextQuestion({
        questionName: 'second-text-q',
        minNum: 5,
        maxNum: 20,
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['second-text-q'],
        'first-text-q', // optional
      )
      await adminPrograms.publishAllDrafts()

      await logout(page)
    })

    test('with both selections submits successfully', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('I love CiviForm!', 0)
      await applicantQuestions.answerTextQuestion('You love CiviForm!', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with unanswered optional question submits successfully', async ({applicantQuestions}) => {
      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('You love CiviForm!', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with first invalid does not submit', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion(
        'A long string that exceeds the character limit',
        0,
      )
      await applicantQuestions.answerTextQuestion('You love CiviForm!', 1)
      await applicantQuestions.clickNext()

      const textId = '.cf-question-text'
      expect(await page.innerText(textId)).toContain(
        'Must contain at most 20 characters.',
      )
    })

    test('with second invalid does not submit', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('I love CiviForm!', 0)
      await applicantQuestions.answerTextQuestion(
        'A long string that exceeds the character limit',
        1,
      )
      await applicantQuestions.clickNext()

      const textId = `.cf-question-text >> nth=1`
      expect(await page.innerText(textId)).toContain(
        'Must contain at most 20 characters.',
      )
    })

    test('has no accessiblity violations', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })
})
