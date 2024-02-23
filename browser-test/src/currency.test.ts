import {
  createTestContext,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('currency applicant flow', () => {
  const validCurrency = '1000'
  // Not enough decimals.
  const invalidCurrency = '1.0'
  const ctx = createTestContext(/* clearDb= */ false)

  describe('single currency question', () => {
    const programName = 'Test program for single currency'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

      await adminQuestions.addCurrencyQuestion({questionName: 'currency-q'})
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['currency-q'],
        programName,
      )

      await logout(page)
    })

    test('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'currency')
    })

    test('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'currency-errors')
    })

    test('with valid currency does submit', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCurrencyQuestion(validCurrency)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with invalid currency does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      const currencyError = '.cf-currency-value-error'
      // When there are no validation errors, the div still exists but is hidden.
      expect(await page.isHidden(currencyError)).toEqual(true)

      // Input has not enough decimal points.
      await applicantQuestions.answerCurrencyQuestion(invalidCurrency)
      await applicantQuestions.clickNext()

      // The block should be displayed still with the error shown.
      expect(await page.isHidden(currencyError)).toEqual(false)
    })
  })

  describe('multiple currency questions', () => {
    const programName = 'Test program for multiple currencies'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
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

    test('with valid currencies does submit', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 0)
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with unanswered optional question submits', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with first invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      const currencyError = '.cf-currency-value-error >> nth=0'
      // When there are no validation errors, the div still exists but is hidden.
      expect(await page.isHidden(currencyError)).toEqual(true)

      await applicantQuestions.answerCurrencyQuestion(invalidCurrency, 0)
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 1)
      await applicantQuestions.clickNext()

      expect(await page.isHidden(currencyError)).toEqual(false)
    })

    test('with second invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      const currencyError = '.cf-currency-value-error >> nth=1'
      // When there are no validation errors, the div still exists but is hidden.
      expect(await page.isHidden(currencyError)).toEqual(true)

      await applicantQuestions.answerCurrencyQuestion(validCurrency, 0)
      await applicantQuestions.answerCurrencyQuestion(invalidCurrency, 1)
      await applicantQuestions.clickNext()

      expect(await page.isHidden(currencyError)).toEqual(false)
    })

    test('has no accessibility violations', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })
})
