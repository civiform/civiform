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

describe('Static text question for applicant flow', () => {
  const staticText = 'Hello, I am some static text!'
  const programName = 'test program for static text'
  const ctx = createBrowserContext(/* clearDb= */ false)
  let applicantQuestions: ApplicantQuestions

  beforeAll(async () => {
    // As admin, create program with static text question.
    await loginAsAdmin(ctx.page)
    const adminQuestions = new AdminQuestions(ctx.page)
    const adminPrograms = new AdminPrograms(ctx.page)
    applicantQuestions = new ApplicantQuestions(ctx.page)

    await adminQuestions.addStaticQuestion({
      questionName: 'static-text-q',
      questionText: staticText,
    })
    // Must add an answerable question for text to show.
    await adminQuestions.addEmailQuestion({questionName: 'partner-email-q'})
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['static-text-q', 'partner-email-q'],
      programName,
    )
    await logout(ctx.page)
  })

  it('validate screenshot', async () => {
    await loginAsGuest(ctx.page)
    await selectApplicantLanguage(ctx.page, 'English')

    await applicantQuestions.applyProgram(programName)

    await validateScreenshot(ctx.page, 'static-text')
  })

  it('displays static text', async () => {
    await loginAsGuest(ctx.page)
    await selectApplicantLanguage(ctx.page, 'English')

    await applicantQuestions.applyProgram(programName)

    await applicantQuestions.seeStaticQuestion(staticText)
  })

  it('has no accessiblity violations', async () => {
    await loginAsGuest(ctx.page)
    await selectApplicantLanguage(ctx.page, 'English')

    await applicantQuestions.applyProgram(programName)

    await validateAccessibility(ctx.page)
  })
})
