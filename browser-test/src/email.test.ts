import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  createBrowserContext,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('Email question for applicant flow', () => {
  const ctx = createBrowserContext(/* clearDb= */ false)

  describe('single email question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for single email'

    beforeAll(async () => {
      // As admin, create program with single email question.
      await loginAsAdmin(ctx.page)
      const adminQuestions = new AdminQuestions(ctx.page)
      const adminPrograms = new AdminPrograms(ctx.page)
      applicantQuestions = new ApplicantQuestions(ctx.page)

      await adminQuestions.addEmailQuestion({questionName: 'general-email-q'})
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['general-email-q'],
        programName,
      )

      await logout(ctx.page)
    })

    it('validate screenshot', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(ctx.page, 'email')
    })

    it('validate screenshot with errors', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(ctx.page, 'email-errors')
    })

    it('with email input submits successfully', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerEmailQuestion('my_email@civiform.gov')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with no email input does not submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      // Click next without inputting anything.
      await applicantQuestions.clickNext()

      const emailId = '.cf-question-email'
      expect(await ctx.page.innerText(emailId)).toContain(
        'This question is required.',
      )
    })
  })

  describe('multiple email questions', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for multiple emails'

    beforeAll(async () => {
      await loginAsAdmin(ctx.page)
      const adminQuestions = new AdminQuestions(ctx.page)
      const adminPrograms = new AdminPrograms(ctx.page)
      applicantQuestions = new ApplicantQuestions(ctx.page)

      await adminQuestions.addEmailQuestion({questionName: 'my-email-q'})
      await adminQuestions.addEmailQuestion({questionName: 'your-email-q'})

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['my-email-q'],
        'your-email-q', // optional
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllPrograms()

      await logout(ctx.page)
    })

    it('with email inputs submits successfully', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerEmailQuestion('your_email@civiform.gov', 0)
      await applicantQuestions.answerEmailQuestion('my_email@civiform.gov', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with unanswered optional question submits successfully', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerEmailQuestion('my_email@civiform.gov', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('has no accessiblity violations', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(ctx.page)
    })
  })
})
