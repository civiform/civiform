import { Page } from 'playwright'
import { AdminPrograms, AdminQuestions, ApplicantQuestions, loginAsAdmin, loginAsGuest, logout, resetSession, selectApplicantLanguage, startSession, validateAccessibility, validateScreenshot, } from './support'

describe('Email question for applicant flow', () => {
  let pageObject: Page

  beforeAll(async () => {
    const { page } = await startSession()
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  describe('single email question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for single email'

    beforeAll(async () => {
      // As admin, create program with single email question.
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addEmailQuestion({ questionName: 'general-email-q' })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['general-email-q'],
        programName
      )

      await logout(pageObject)
    })

    it('with email input submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerEmailQuestion('my_email@civiform.gov')
      await applicantQuestions.clickNext()
      await validateScreenshot(pageObject)

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with no email input does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      // Click next without inputting anything.
      await applicantQuestions.clickNext()

      const emailId = '.cf-question-email'
      expect(await pageObject.innerText(emailId)).toContain(
        'This question is required.'
      )
      await validateScreenshot(pageObject)
    })
  })

  describe('multiple email questions', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for multiple emails'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addEmailQuestion({ questionName: 'my-email-q' })
      await adminQuestions.addEmailQuestion({ questionName: 'your-email-q' })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['my-email-q'],
        'your-email-q' // optional
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllPrograms()

      await logout(pageObject)
    })

    it('with email inputs submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerEmailQuestion('your_email@civiform.gov', 0)
      await applicantQuestions.answerEmailQuestion('my_email@civiform.gov', 1)
      await applicantQuestions.clickNext()
      await validateScreenshot(pageObject)

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with unanswered optional question submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerEmailQuestion('my_email@civiform.gov', 1)
      await applicantQuestions.clickNext()

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
