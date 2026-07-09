import {Page} from '@playwright/test'
import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'
import {SAMPLE_QUESTIONS, Seeding} from '../../support/seeding'

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

test.describe('Static text question for applicant flow', () => {
  test.beforeEach(async ({page, adminQuestions, adminPrograms, seeding}) => {
    await setUpForSingleQuestion(
      programName,
      page,
      seeding,
      adminQuestions,
      adminPrograms,
    )
  })

  test('parses markdown', async ({page, applicantQuestions}) => {
    await applicantQuestions.applyProgram(programName)
    await validateScreenshot(
      page.getByTestId('staticQuestionRoot'),
      'markdown-text',
      {fullPage: false},
    )
    await verifyMarkdownHtml(page)
  })

  test('has no accessiblity violations', async ({page, applicantQuestions}) => {
    await applicantQuestions.applyProgram(programName)
    await validateAccessibility(page)
  })
})

async function setUpForSingleQuestion(
  programName: string,
  page: Page,
  seeding: Seeding,
  adminQuestions: AdminQuestions,
  adminPrograms: AdminPrograms,
) {
  await seeding.seedQuestions()
  // As admin, create program with static text question. The tests assert the
  // rendering of this bespoke markdown, which the seeded sample static
  // content question does not contain, so it is still created via the UI.
  await loginAsAdmin(page)
  await adminQuestions.addStaticQuestion({
    questionName: 'static-text-q',
    questionText: staticText,
    markdownText: markdownText,
  })
  // Must add an answerable question for text to show.
  await adminPrograms.addAndPublishProgramWithQuestions(
    ['static-text-q', SAMPLE_QUESTIONS.email],
    programName,
  )
  await logout(page)
}

async function verifyMarkdownHtml(page: Page) {
  expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
    '<p>Hello, I am some static text!<br>',
  )
  expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
    '<a href="https://www.example.com" class="usa-link usa-link--external" target="_blank" aria-label="This is a link, opens in a new tab" rel="nofollow noopener noreferrer">This is a link</a>',
  )
  expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
    '<ul class="usa-list margin-r-4"><li>Item 1</li><li>Item 2<br>&nbsp;</li></ul>',
  )
  expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
    '<p>There are some empty lines below this that should be preserved<br>&nbsp;</p>\n<p>&nbsp;</p>',
  )
  expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
    '<p>This link should be autodetected: <a href="https://www.example.com" class="usa-link usa-link--external" target="_blank" aria-label="https://www.example.com, opens in a new tab" rel="nofollow noopener noreferrer">https://www.example.com</a>',
  )
  expect(await page.innerHTML('.cf-applicant-question-text')).toContain(
    '<strong>Last line of content should be bold</strong>',
  )
}
