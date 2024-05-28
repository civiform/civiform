import {Page} from '@playwright/test'
import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('Number question for applicant flow', () => {
  const numberInputError = 'div.cf-question-number-error'

  test.describe('single number question', () => {
    const programName = 'Test program for single number'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await setUpForSingleQuestion(
        programName,
        page,
        adminQuestions,
        adminPrograms,
      )
      await disableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test('validate screenshot', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'number')
    })

    test('validate screenshot with errors', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'number-errors')
    })

    test('with valid number submits successfully', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('8')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with no input does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      // Leave field blank.
      await applicantQuestions.clickNext()

      const numberId = '.cf-question-number'
      expect(await page.innerText(numberId)).toContain(
        'This question is required.',
      )
    })

    test('with non-numeric inputs does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      const testValues = ['12e3', '12E3', '-123', '1.23']

      for (const testValue of testValues) {
        await applicantQuestions.answerNumberQuestion(testValue)
        await applicantQuestions.clickNext()

        await expect(page.locator(numberInputError)).toBeVisible()
        await applicantQuestions.answerNumberQuestion('')
      }
    })
  })

  test.describe('multiple number questions', () => {
    const programName = 'Test program for multiple numbers'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)

      await adminQuestions.addNumberQuestion({
        questionName: 'my-number-q',
      })
      await adminQuestions.addNumberQuestion({
        questionName: 'your-number-q',
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['my-number-q'],
        'your-number-q', // optional
      )
      await adminPrograms.publishAllDrafts()

      await logout(page)
    })

    test('with valid numbers submits successfully', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('100', 0)
      await applicantQuestions.answerNumberQuestion('33', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with unanswered optional question submits successfully', async ({
      applicantQuestions,
    }) => {
      // Only answer required question.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('33', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with first invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('-10', 0)
      await applicantQuestions.answerNumberQuestion('33', 1)
      await applicantQuestions.clickNext()

      // Fix me! ESLint: playwright/prefer-web-first-assertions
      // Directly switching to the best practice method fails
      // because of a locator stict mode violation. That is it
      // returns multiple elements.
      //
      // Recommended prefer-web-first-assertions fix:
      //   await expect(page.locator(numberInputError)).toBeVisible()
      expect(await page.isHidden(numberInputError)).toEqual(false)
    })

    test('with second invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('10', 0)
      await applicantQuestions.answerNumberQuestion('-5', 1)
      await applicantQuestions.clickNext()

      // Fix me! ESLint: playwright/prefer-web-first-assertions
      // Directly switching to the best practice method fails
      // because of a locator stict mode violation. That is it
      // returns multiple elements.
      //
      // Recommended prefer-web-first-assertions fix:
      //   await expect(page.locator(numberInputError + ' >> nth=1')).toBeVisible()
      expect(await page.isHidden(numberInputError + ' >> nth=1')).toEqual(false)
    })

    test('has no accessiblity violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })

  test.describe(
    'single number question with North Star flag enabled',
    {tag: ['@northstar']},
    () => {
      const programName = 'Test program for single number'

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
        await applicantQuestions.applyProgram(programName)

        await test.step('Screenshot without errors', async () => {
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'number-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ true,
          )
        })

        await test.step('Screenshot with errors', async () => {
          await applicantQuestions.clickContinue()
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'number-errors-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ true,
          )
        })
      })
    },
  )

  async function setUpForSingleQuestion(
    programName: string,
    page: Page,
    adminQuestions: AdminQuestions,
    adminPrograms: AdminPrograms,
  ) {
    // As admin, create program with single number question.
    await loginAsAdmin(page)

    await adminQuestions.addNumberQuestion({
      questionName: 'fave-number-q',
    })
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['fave-number-q'],
      programName,
    )

    await logout(page)
  }
})
