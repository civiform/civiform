import {
  createTestContext,
  loginAsAdmin,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('Static text question for applicant flow', () => {
  const staticText = 'Hello, I am some static text!'
  const programName = 'Test program for static text'
  const ctx = createTestContext(/* clearDb= */ false)

  beforeAll(async () => {
    const {page, adminQuestions, adminPrograms} = ctx
    // As admin, create program with static text question.
    await loginAsAdmin(page)

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
  })

  it('validate screenshot', async () => {
    const {page, applicantQuestions} = ctx
    await applicantQuestions.applyProgram(programName)

    await validateScreenshot(page, 'static-text')
  })

  it('displays static text', async () => {
    const {applicantQuestions} = ctx
    await applicantQuestions.applyProgram(programName)

    await applicantQuestions.seeStaticQuestion(staticText)
  })

  it('has no accessiblity violations', async () => {
    const {page, applicantQuestions} = ctx
    await applicantQuestions.applyProgram(programName)

    await validateAccessibility(page)
  })
})
