import {
  createTestContext,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('Number question for applicant flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)
  const numberInputError = 'div.cf-question-number-error'

  describe('single number question', () => {
    const programName = 'Test program for single number'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
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
    })

    test('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'number')
    })

    test('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'number-errors')
    })

    test('with valid number submits successfully', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('8')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with no input does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      // Leave field blank.
      await applicantQuestions.clickNext()

      const numberId = '.cf-question-number'
      expect(await page.innerText(numberId)).toContain(
        'This question is required.',
      )
    })

    test('with non-numeric inputs does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      const testValues = ['12e3', '12E3', '-123', '1.23']

      for (const testValue of testValues) {
        await applicantQuestions.answerNumberQuestion(testValue)
        await applicantQuestions.clickNext()
        expect(await page.isHidden(numberInputError)).toEqual(false)
        await applicantQuestions.answerNumberQuestion('')
      }
    })
  })

  describe('multiple number questions', () => {
    const programName = 'Test program for multiple numbers'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
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

    test('with valid numbers submits successfully', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('100', 0)
      await applicantQuestions.answerNumberQuestion('33', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with unanswered optional question submits successfully', async () => {
      const {applicantQuestions} = ctx
      // Only answer required question.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('33', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with first invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('-10', 0)
      await applicantQuestions.answerNumberQuestion('33', 1)
      await applicantQuestions.clickNext()

      expect(await page.isHidden(numberInputError)).toEqual(false)
    })

    test('with second invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('10', 0)
      await applicantQuestions.answerNumberQuestion('-5', 1)
      await applicantQuestions.clickNext()

      expect(await page.isHidden(numberInputError + ' >> nth=1')).toEqual(false)
    })

    test('has no accessiblity violations', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })
})
