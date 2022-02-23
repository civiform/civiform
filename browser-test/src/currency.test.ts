import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  startSession,
  resetSession,
} from './support'

describe('currency applicant flow', () => {
  const validCurrency = '1000'
  // Not enough decimals.
  const invalidCurrency = '1.0'
  let pageObject

  beforeAll(async () => {
    const { page } = await startSession()
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  describe('single currency question', () => {
    let applicantQuestions
    const programName = 'test program for single currency'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addCurrencyQuestion({ questionName: 'currency-q' })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['currency-q'],
        programName
      )

      await logout(pageObject)
    })

    it('with valid currency does submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCurrencyQuestion(validCurrency)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with invalid currency does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      const error = await pageObject.$('.cf-currency-value-error')
      expect(await error.isHidden()).toEqual(true)

      // Input has not enough decimal points.
      await applicantQuestions.answerCurrencyQuestion(invalidCurrency)
      await applicantQuestions.clickNext()

      // The block should be displayed still with the error shown.
      expect(await error.isHidden()).toEqual(false)
    })
  })

  describe('multiple currency questions', () => {
    let applicantQuestions
    const programName = 'test program for multiple currencies'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addCurrencyQuestion({
        questionName: 'currency-a-q',
      })
      await adminQuestions.addCurrencyQuestion({
        questionName: 'currency-b-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['currency-a-q', 'currency-b-q'],
        programName
      )

      await logout(pageObject)
    })

    it('with valid currencies does submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 0)
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with first invalid does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      const error = await pageObject.$('.cf-currency-value-error >> nth=0')
      expect(await error.isHidden()).toEqual(true)

      await applicantQuestions.answerCurrencyQuestion(invalidCurrency, 0)
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 1)
      await applicantQuestions.clickNext()

      expect(await error.isHidden()).toEqual(false)
    })

    it('with second invalid does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      const error = await pageObject.$('.cf-currency-value-error >> nth=1')
      expect(await error.isHidden()).toEqual(true)

      await applicantQuestions.answerCurrencyQuestion(validCurrency, 0)
      await applicantQuestions.answerCurrencyQuestion(invalidCurrency, 1)
      await applicantQuestions.clickNext()

      expect(await error.isHidden()).toEqual(false)
    })
  })
})
