import {Page} from 'playwright'
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
  validateAccessibility,
} from './support'

describe('Id question for applicant flow', () => {
  let pageObject: Page

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  describe('single id question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for single id'

    beforeAll(async () => {
      // As admin, create program with single id question.
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addIdQuestion({
        questionName: 'id-q',
        minNum: 5,
        maxNum: 5,
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['id-q'],
        programName,
      )

      await logout(pageObject)
    })

    it('with id submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('12345')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with empty id does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)

      // Click next without inputting anything
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await pageObject.innerText(identificationId)).toContain(
        'This question is required.',
      )
    })

    it('with too short id does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('123')
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await pageObject.innerText(identificationId)).toContain(
        'Must contain at least 5 characters.',
      )
    })

    it('with too long id does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('123456')
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await pageObject.innerText(identificationId)).toContain(
        'Must contain at most 5 characters.',
      )
    })

    it('with non-numeric characters does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('abcde')
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await pageObject.innerText(identificationId)).toContain(
        'Must contain only numbers.',
      )
    })
  })

  describe('multiple id questions', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for multiple ids'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

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

      await logout(pageObject)
    })

    it('with both id inputs submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('12345', 0)
      await applicantQuestions.answerIdQuestion('67890', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with unanswered optional question submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('67890', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with first invalid does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('abcde', 0)
      await applicantQuestions.answerIdQuestion('67890', 1)
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await pageObject.innerText(identificationId)).toContain(
        'Must contain only numbers.',
      )
    })

    it('with second invalid does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('67890', 0)
      await applicantQuestions.answerIdQuestion('abcde', 1)
      await applicantQuestions.clickNext()

      const identificationId = `.cf-question-id >> nth=1`
      expect(await pageObject.innerText(identificationId)).toContain(
        'Must contain only numbers.',
      )
    })

    it('has no accessiblity violations', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(pageObject)
    })
  })
})
