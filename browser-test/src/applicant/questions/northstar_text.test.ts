import {test, expect} from '../../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('Text question for applicant flow', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

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
    })

    test('validate screenshot', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await test.step('Screenshot without errors', async () => {
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'text-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
      })

      await test.step('Screenshot with errors', async () => {
        await applicantQuestions.clickContinue()
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'text-errors-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
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

    test('with text submits successfully', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerTextQuestion('I love CiviForm!')
      await applicantQuestions.clickContinue()

      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    test('with empty text does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      // Click "Continue" without inputting anything
      await applicantQuestions.clickContinue()

      await expect(
        page.getByText('Must contain at least 5 characters.'),
      ).toBeVisible()
    })

    test('with too short text does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerTextQuestion('hi')
      await applicantQuestions.clickContinue()

      await expect(
        page.getByText('Must contain at least 5 characters.'),
      ).toBeVisible()
    })

    test('with too long text does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerTextQuestion(
        'A long string that exceeds the character limit',
      )
      await applicantQuestions.clickContinue()

      await expect(
        page.getByText('Must contain at most 20 characters.'),
      ).toBeVisible()
    })

    test('hitting enter on text does not trigger submission', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerTextQuestion('I love CiviForm!', 0)

      // Ensure that clicking enter while on text input doesn't trigger form
      // submission.
      await page.focus('input[type=text]')
      await page.keyboard.press('Enter')
      await expect(page.locator('input[type=text]')).toBeVisible()

      // Check that pressing Enter on button works.
      await page.focus('button:has-text("Continue")')
      await page.keyboard.press('Enter')
      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)

      // Go back to question and ensure that "Review" button is also clickable
      // via Enter.
      await applicantQuestions.clickEdit()
      await page.focus('text="Review and submit"')
      await page.keyboard.press('Enter')
      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
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

    test('text that is too long is cut off at 10k characters', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      let largeString = ''
      for (let i = 0; i < 1000; i++) {
        largeString += '1234567890'
      }
      await applicantQuestions.answerTextQuestion(
        // 10k characters + extra characters that should be trimmed
        largeString + 'xxxxxxx',
      )
      await applicantQuestions.clickContinue()

      // Scroll to bottom so end of text is in view.
      await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))

      // Should display answered question with "x"s cut off from the end.
      await validateScreenshot(page.locator('main'), 'text-max')

      // Form should submit with partial text entry.
      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
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

    test('with both selections submits successfully', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerTextQuestion('I love CiviForm!', 0)
      await applicantQuestions.answerTextQuestion('You love CiviForm!', 1)
      await applicantQuestions.clickContinue()

      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    test('with unanswered optional question submits successfully', async ({
      applicantQuestions,
    }) => {
      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerTextQuestion('You love CiviForm!', 1)
      await applicantQuestions.clickContinue()

      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    test('with first invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerTextQuestion(
        'A long string that exceeds the character limit',
        0,
      )
      await applicantQuestions.answerTextQuestion('You love CiviForm!', 1)
      await applicantQuestions.clickContinue()

      const textId = '.cf-question-text'
      expect(await page.innerText(textId)).toContain(
        'Must contain at most 20 characters.',
      )
    })

    test('with second invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerTextQuestion('I love CiviForm!', 0)
      await applicantQuestions.answerTextQuestion(
        'A long string that exceeds the character limit',
        1,
      )
      await applicantQuestions.clickContinue()

      const textId = `.cf-question-text >> nth=1`
      expect(await page.innerText(textId)).toContain(
        'Must contain at most 20 characters.',
      )
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
  })
})
