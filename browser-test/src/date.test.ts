import {
  createTestContext,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('Date question for applicant flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  describe('single date question', () => {
    const programName = 'Test program for single date'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      // As admin, create program with single date question.
      await loginAsAdmin(page)

      await adminQuestions.addDateQuestion({questionName: 'general-date-q'})
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['general-date-q'],
        programName,
      )

      await logout(page)
    })

    test('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'date')
    })

    test('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'date-errors')
    })

    test('with filled in date submits successfully', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDateQuestion('2022-05-02')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with no answer does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      // Click next without selecting anything.
      await applicantQuestions.clickNext()

      // Check required error is present
      const dateId = '.cf-question-date'
      expect(await page.innerText(dateId)).toContain(
        'This question is required.',
      )
    })
  })

  describe('multiple date questions', () => {
    const programName = 'Test program for multiple date questions'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

      await adminQuestions.addDateQuestion({questionName: 'birthday-date-q'})
      await adminQuestions.addDateQuestion({questionName: 'todays-date-q'})

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['birthday-date-q'],
        'todays-date-q', // optional
      )
      await adminPrograms.publishAllDrafts()

      await logout(page)
    })

    test('with valid dates submits successfully', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDateQuestion('2022-07-04', 0)
      await applicantQuestions.answerDateQuestion('1990-10-10', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with unanswered optional question submits successfully', async () => {
      const {applicantQuestions} = ctx
      // Only answer second question.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDateQuestion('1990-10-10', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('has no accessiblity violations', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })
})
