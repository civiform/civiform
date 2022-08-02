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

describe('Static text question for applicant flow', () => {
  const staticText = 'Hello, I am some static text!'
  const programName = 'test program for static text'
  let pageObject: Page
  let applicantQuestions: ApplicantQuestions

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
    // As admin, create program with static text question.
    await loginAsAdmin(pageObject)
    const adminQuestions = new AdminQuestions(pageObject)
    const adminPrograms = new AdminPrograms(pageObject)
    applicantQuestions = new ApplicantQuestions(pageObject)

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

    await logout(pageObject)
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  it('displays static text', async () => {
    await loginAsGuest(pageObject)
    await selectApplicantLanguage(pageObject, 'English')

    await applicantQuestions.applyProgram(programName)

    applicantQuestions.seeStaticQuestion(staticText)
  })

  it('has no accessiblity violations', async () => {
    await loginAsGuest(pageObject)
    await selectApplicantLanguage(pageObject, 'English')

    await applicantQuestions.applyProgram(programName)

    await validateAccessibility(pageObject)
  })
})
