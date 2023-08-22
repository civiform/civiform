import {
  createTestContext,
  loginAsAdmin,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('Static text question for applicant flow', () => {
  const staticText = 'Hello, I am some static text!'
  const accordionText = '\n### Accordion Title \n> This is some content.'
  const programName = 'Test program for static text'
  const ctx = createTestContext(/* clearDb= */ false)

  beforeAll(async () => {
    const {page, adminQuestions, adminPrograms} = ctx
    // As admin, create program with static text question.
    await loginAsAdmin(page)

    await adminQuestions.addStaticQuestion({
      questionName: 'static-text-q',
      questionText: staticText,
      accordionText: accordionText,
    })
    // Must add an answerable question for text to show.
    await adminQuestions.addEmailQuestion({questionName: 'partner-email-q'})
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['static-text-q', 'partner-email-q'],
      programName,
    )
  })

  it('validate screenshot when accordion open', async () => {
    const {page, applicantQuestions} = ctx
    await applicantQuestions.applyProgram(programName)

    const headerButton = page.locator('.cf-accordion-header')
    await headerButton.click()

    // Wait for the accordion to open, so we don't screenshot during the opening,
    // causing inconsistent screenshots.
    await page.waitForTimeout(300) // ms
    await validateScreenshot(page, 'static-text-accordion-open')
  })

  it('validate screenshot when accordion closed', async () => {
    const {page, applicantQuestions} = ctx
    await applicantQuestions.applyProgram(programName)

    await page.waitForTimeout(300) // ms
    await validateScreenshot(page, 'static-text-accordion-closed')
  })

  it('displays static text', async () => {
    const {applicantQuestions} = ctx
    await applicantQuestions.applyProgram(programName)

    await applicantQuestions.seeStaticQuestion(staticText)
  })

  it('displays the accordion', async () => {
    const {page, applicantQuestions} = ctx
    await applicantQuestions.applyProgram(programName)

    expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
      'cf-accordion',
    )
  })

  it('expands accordion when accordion header clicked', async () => {
    const {page, applicantQuestions} = ctx
    await applicantQuestions.applyProgram(programName)
    const headerButton = page.locator('.cf-accordion-header')
    await headerButton.click()

    expect(await headerButton.getAttribute('aria-expanded')).toBe('true')
  })

  it('has no accessiblity violations', async () => {
    const {page, applicantQuestions} = ctx
    await applicantQuestions.applyProgram(programName)

    await validateAccessibility(page)
  })
})
