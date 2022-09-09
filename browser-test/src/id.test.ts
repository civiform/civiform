import {
  createTestContext,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('Id question for applicant flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  describe('single id question', () => {
    const programName = 'test program for single id'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      // As admin, create program with single id question.
      await loginAsAdmin(page)

      await adminQuestions.addIdQuestion({
        questionName: 'id-q',
        minNum: 5,
        maxNum: 5,
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['id-q'],
        programName,
      )

      await logout(page)
    })

    it('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'id')
    })

    it('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'id-errors')
    })

    it('with id submits successfully', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('12345')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with empty id does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)

      // Click next without inputting anything
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await page.innerText(identificationId)).toContain(
        'This question is required.',
      )
    })

    it('with too short id does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('123')
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await page.innerText(identificationId)).toContain(
        'Must contain at least 5 characters.',
      )
    })

    it('with too long id does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('123456')
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await page.innerText(identificationId)).toContain(
        'Must contain at most 5 characters.',
      )
    })

    it('with non-numeric characters does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('abcde')
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await page.innerText(identificationId)).toContain(
        'Must contain only numbers.',
      )
    })
  })

  describe('multiple id questions', () => {
    const programName = 'test program for multiple ids'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

      await adminQuestions.addIdQuestion({
        questionName: 'my-id-q',
      })
      await adminQuestions.addIdQuestion({
        questionName: 'your-id-q',
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['my-id-q'],
        'your-id-q', // optional
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllPrograms()

      await logout(page)
    })

    it('with both id inputs submits successfully', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('12345', 0)
      await applicantQuestions.answerIdQuestion('67890', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with unanswered optional question submits successfully', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('67890', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with first invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('abcde', 0)
      await applicantQuestions.answerIdQuestion('67890', 1)
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await page.innerText(identificationId)).toContain(
        'Must contain only numbers.',
      )
    })

    it('with second invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('67890', 0)
      await applicantQuestions.answerIdQuestion('abcde', 1)
      await applicantQuestions.clickNext()

      const identificationId = `.cf-question-id >> nth=1`
      expect(await page.innerText(identificationId)).toContain(
        'Must contain only numbers.',
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
