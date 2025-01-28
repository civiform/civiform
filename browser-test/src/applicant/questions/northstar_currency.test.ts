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

test.describe('currency applicant flow', {tag: ['@northstar']}, () => {
  const validCurrency = '1000'
  // Not enough decimals.
  const invalidCurrency = '1.0'
  const currencyError =
    'Error: Currency must be one of the following formats: 1000 1,000 1000.30 1,000.30'

  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

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

      await test.step('Screenshot without errors', async () => {
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'currency-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
      })

      await test.step('Screenshot with errors', async () => {
        await applicantQuestions.clickContinue()
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'currency-errors-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
      })
    })

    test('has no accessiblity violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })

    test('with valid currency does submit', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCurrencyQuestion(validCurrency)
      await applicantQuestions.clickContinue()

      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    test('with invalid currency does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await expect(page.getByText(currencyError)).toBeHidden()

      // Input has not enough decimal points.
      await applicantQuestions.answerCurrencyQuestion(invalidCurrency)
      await applicantQuestions.clickContinue()

      // The block should be displayed still with the error shown.
      await expect(page.getByText(currencyError)).toBeVisible()
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
      await applicantQuestions.clickContinue()

      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    test('with unanswered optional question submits', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 1)
      await applicantQuestions.clickContinue()

      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    test('with first invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await expect(page.getByText(currencyError)).toBeHidden()

      await applicantQuestions.answerCurrencyQuestion(invalidCurrency, 0)
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 1)
      await applicantQuestions.clickContinue()

      await expect(page.getByText(currencyError)).toBeVisible()
    })

    test('with second invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await expect(page.getByText(currencyError)).toBeHidden()

      await applicantQuestions.answerCurrencyQuestion(validCurrency, 0)
      await applicantQuestions.answerCurrencyQuestion(invalidCurrency, 1)
      await applicantQuestions.clickContinue()

      await expect(page.getByText(currencyError)).toBeVisible()
    })
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
