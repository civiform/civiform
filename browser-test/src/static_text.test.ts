import {
  createTestContext,
  loginAsAdmin,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('Static text question for applicant flow', () => {
  const staticText = 'Hello, I am some static text!'
  const markdownText =
    '\n[This is a link](https://www.example.com)\n' +
    'This is a list:\n' +
    '* Item 1\n' +
    '* Item 2\n' +
    '\n' +
    'There are some empty lines below this that should be preserved\n' +
    '\n' +
    '\n' +
    'This link should be autodetected: https://www.example.com\n' +
    '__Last line of content should be bold__'
  const programName = 'Test program for static text'
  const ctx = createTestContext(/* clearDb= */ false)

  beforeAll(async () => {
    const {page, adminQuestions, adminPrograms} = ctx
    // As admin, create program with static text question.
    await loginAsAdmin(page)

    await adminQuestions.addStaticQuestion({
      questionName: 'static-text-q',
      questionText: staticText,
      markdownText: markdownText,
    })
    // Must add an answerable question for text to show.
    await adminQuestions.addEmailQuestion({questionName: 'partner-email-q'})
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['static-text-q', 'partner-email-q'],
      programName,
    )
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

  it('parses markdown', async () => {
    const {page, applicantQuestions} = ctx
    await applicantQuestions.applyProgram(programName)
    await validateScreenshot(page, 'markdown-text')

    expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
      '<p>Hello, I am some static text!<br>',
    )
    expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
      '<a href="https://www.example.com" class="text-blue-900 font-bold opacity-75 underline hover:opacity-100" target="_blank" rel="nofollow noopener noreferrer">This is a link<svg',
    )
    expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
      '<ul class="list-disc mx-8"><li>Item 1</li><li>Item 2<br>&nbsp;</li></ul>',
    )
    expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
      '<p>There are some empty lines below this that should be preserved<br>&nbsp;</p>\n<p>&nbsp;</p>',
    )
    expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
      '<p>This link should be autodetected: <a href="https://www.example.com" class="text-blue-900 font-bold opacity-75 underline hover:opacity-100" target="_blank" rel="nofollow noopener noreferrer">https://www.example.com<svg',
    )
    expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
      '<strong>Last line of content should be bold</strong>',
    )
  })
})
