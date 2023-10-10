import {
  createTestContext,
  loginAsAdmin,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('Static text question for applicant flow', () => {
  const staticText =
    'Hello, I am some static text!'
  const markdownText =
  '[This is a link](www.example.com)\n'+
  'This is a list:\n'+
  '*Item 1\n'+
  '# This is a header 1 that should be changed to header 2\n'+
  'There are some empty lines below this that should be preserved\n'+
  '\n'+
  '\n'+
  'Last line of content'
  const programName = 'Test program for static text'
  const ctx = createTestContext(/* clearDb= */ false)

  beforeAll(async () => {
    const {page, adminQuestions, adminPrograms} = ctx
    // As admin, create program with static text question.
    await loginAsAdmin(page)

    await adminQuestions.addStaticQuestion({
      questionName: 'static-text-q',
      questionText: staticText,
      markdownText: markdownText
    })
    // Must add an answerable question for text to show.
    await adminQuestions.addEmailQuestion({questionName: 'partner-email-q'})
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['static-text-q', 'partner-email-q'],
      programName,
    )
  })

  it('displays static text', async () => {
    const {page, applicantQuestions} = ctx
    await applicantQuestions.applyProgram(programName)

    await applicantQuestions.seeStaticQuestion(staticText)
    await validateScreenshot(page, 'markdown-text')


  })

  it('has no accessiblity violations', async () => {
    const {page, applicantQuestions} = ctx
    await applicantQuestions.applyProgram(programName)

    await validateAccessibility(page)
  })

  describe('markdown parsing', () => {
    it('parses markdown', async () => {
      
      // test for links
      // lists (formatting)
      // bold
      // h1 -> h2
      // formatting on autodetect links
      // preserves lines (whitespace)
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await validateScreenshot(page, 'markdown-text')

      expect(await page.innerHTML('.cf-applicant-question-text')).toContain('<p>Hello, I am some static text!</p>')
      expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
        '<a class="text-blue-600 hover:text-blue-500 underline" target="_blank" href="http://www.example.com">This is a link</a>'
      )
      expect(await page.innerHTML('.cf-applicant-question-text')).toContain('<ul class=\"list-disc mx-8\"><li>Welcome to this test program.</li><li>It contains one of every question type.<br>&nbsp;</li></ul>')
      expect(await page.innerHTML('.cf-applicant-question-text')).toContain('<h2>This is a header 1 that should be changed to header 2</h2>')
      expect(await page.innerHTML('.cf-applicant-question-text')).toContain('<br>&nbsp;<br>&nbsp;')
    })
  })
})
