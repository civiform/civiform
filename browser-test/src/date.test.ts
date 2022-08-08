import { Page } from 'playwright'
import { AdminPrograms, AdminQuestions, ApplicantQuestions, loginAsAdmin, loginAsGuest, logout, resetSession, selectApplicantLanguage, startSession, validateAccessibility, validateScreenshot, } from './support'

describe('Date question for applicant flow', () => {
  let pageObject: Page

  beforeAll(async () => {
    const { page } = await startSession()
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  describe('single date question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for single date'

    beforeAll(async () => {
      // As admin, create program with single date question.
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addDateQuestion({ questionName: 'general-date-q' })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['general-date-q'],
        programName
      )

      await logout(pageObject)
    })

    it('with filled in date submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDateQuestion('2022-05-02')
      await applicantQuestions.clickNext()
      await validateScreenshot(pageObject)

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with no answer does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      // Click next without selecting anything.
      await applicantQuestions.clickNext()

      // Check required error is present
      const dateId = '.cf-question-date'
      expect(await pageObject.innerText(dateId)).toContain(
        'This question is required.'
      )
      await validateScreenshot(pageObject)
    })
  })

  describe('multiple date questions', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for multiple date questions'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addDateQuestion({ questionName: 'birthday-date-q' })
      await adminQuestions.addDateQuestion({ questionName: 'todays-date-q' })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['birthday-date-q'],
        'todays-date-q' // optional
      )
      await adminPrograms.gotoAdminProgramsPage()
      await validateScreenshot(pageObject)
      await adminPrograms.publishAllPrograms()

      await logout(pageObject)
    })

    it('with valid dates submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDateQuestion('2022-07-04', 0)
      await applicantQuestions.answerDateQuestion('1990-10-10', 1)
      await applicantQuestions.clickNext()

      await validateScreenshot(pageObject)
      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with unanswered optional question submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Only answer second question.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDateQuestion('1990-10-10', 1)
      await applicantQuestions.clickNext()

      await validateScreenshot(pageObject)
      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('has no accessiblity violations', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(pageObject)
    })
  })
})
