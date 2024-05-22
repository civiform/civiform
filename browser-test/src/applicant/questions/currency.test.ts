import {Page} from '@playwright/test'
import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminPrograms,
  AdminQuestions,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('currency applicant flow', () => {
  const validCurrency = '1000'
  // Not enough decimals.
  const invalidCurrency = '1.0'

  test.describe('single currency question', () => {
    const programName = 'Test program for single currency'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await setUpSingleCurrencyQuestion(
        programName,
        page,
        adminQuestions,
        adminPrograms,
      )
    })

    test('validate screenshot', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'currency')
    })

    test('validate screenshot with errors', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'currency-errors')
    })

    test('with valid currency does submit', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCurrencyQuestion(validCurrency)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with invalid currency does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      const currencyError = '.cf-currency-value-error'
      // When there are no validation errors, the div still exists but is hidden.
      await expect(page.locator(currencyError)).toBeHidden()

      // Input has not enough decimal points.
      await applicantQuestions.answerCurrencyQuestion(invalidCurrency)
      await applicantQuestions.clickNext()

      // The block should be displayed still with the error shown.
      await expect(page.locator(currencyError)).toBeVisible()
    })
  })

  test.describe('multiple currency questions', () => {
    const programName = 'Test program for multiple currencies'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)

      await adminQuestions.addCurrencyQuestion({
        questionName: 'currency-a-q',
      })
      await adminQuestions.addCurrencyQuestion({
        questionName: 'currency-b-q',
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['currency-b-q'],
        'currency-a-q', // optional
      )
      await adminPrograms.publishAllDrafts()

      await logout(page)
    })

    test('with valid currencies does submit', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 0)
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with unanswered optional question submits', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with first invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      const currencyError = '.cf-currency-value-error >> nth=0'
      // When there are no validation errors, the div still exists but is hidden.
      await expect(page.locator(currencyError)).toBeHidden()

      await applicantQuestions.answerCurrencyQuestion(invalidCurrency, 0)
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 1)
      await applicantQuestions.clickNext()

      await expect(page.locator(currencyError)).toBeVisible()
    })

    test('with second invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      const currencyError = '.cf-currency-value-error >> nth=1'
      // When there are no validation errors, the div still exists but is hidden.
      await expect(page.locator(currencyError)).toBeHidden()

      await applicantQuestions.answerCurrencyQuestion(validCurrency, 0)
      await applicantQuestions.answerCurrencyQuestion(invalidCurrency, 1)
      await applicantQuestions.clickNext()

      await expect(page.locator(currencyError)).toBeVisible()
    })

    test('has no accessibility violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })

  test.describe('single currency question with north star flag enabled', () => {
    const programName = 'Test program for single currency'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await setUpSingleCurrencyQuestion(
        programName,
        page,
        adminQuestions,
        adminPrograms,
      )
      await enableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test(
      'validate screenshot',
      {tag: ['@northstar']},
      async ({page, applicantQuestions}) => {
        await applicantQuestions.applyProgram(programName)

        await test.step('Screenshot without errors', async () => {
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'currency-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ true,
          )
        })

        await test.step('Screenshot with errors', async () => {
          await applicantQuestions.clickContinue()
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'currency-errors-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ true,
          )
        })
      },
    )
  })

  async function setUpSingleCurrencyQuestion(
    programName: string,
    page: Page,
    adminQuestions: AdminQuestions,
    adminPrograms: AdminPrograms,
  ) {
    await loginAsAdmin(page)

    await adminQuestions.addCurrencyQuestion({questionName: 'currency-q'})
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['currency-q'],
      programName,
    )

    await logout(page)
  }
})
