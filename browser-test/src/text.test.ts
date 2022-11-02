import {
  createTestContext,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('Text question for applicant flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  describe('single text question', () => {
    const programName = 'test-program-for-single-text-q'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
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

    it('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'text')
    })

    it('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'text-errors')
    })

    it('with text submits successfully', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('I love CiviForm!')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with empty text does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)

      // Click next without inputting anything
      await applicantQuestions.clickNext()

      const textId = '.cf-question-text'
      expect(await page.innerText(textId)).toContain(
        'This question is required.',
      )
    })

    it('with too short text does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('hi')
      await applicantQuestions.clickNext()

      const textId = '.cf-question-text'
      expect(await page.innerText(textId)).toContain(
        'Must contain at least 5 characters.',
      )
    })

    it('with too long text does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

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
  })

  describe('multiple text questions', () => {
    const programName = 'test-program-for-multiple-text-qs'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
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
      await adminPrograms.publishAllPrograms()

      await logout(page)
    })

    it('with both selections submits successfully', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('I love CiviForm!', 0)
      await applicantQuestions.answerTextQuestion('You love CiviForm!', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with unanswered optional question submits successfully', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('You love CiviForm!', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with first invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

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

    it('with second invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

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

    it('has no accessiblity violations', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })
})
