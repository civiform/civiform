import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
} from '../support'

test.describe('Applicant application index', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test('verify markdown on program cards', async ({
    page,
    adminPrograms,
    applicantQuestions,
  }) => {
    const programWithExternalLink = 'Program with markdown'

    await test.step('As admin, create program with markdown', async () => {
      await loginAsAdmin(page)
      // Create program that has an external link and markdown in the program description.
      const programDescriptionWithMarkdown =
        '# Program description\n' +
        'Some things to know:\n' +
        '* Thing 1\n' +
        '* Thing 2\n' +
        '\n' +
        'For more info go to our [website](https://www.example.com)\n'
      await adminPrograms.addProgram(
        programWithExternalLink,
        programDescriptionWithMarkdown,
        'https://external.com/do_not_show',
      )
      await adminPrograms.publishProgram(programWithExternalLink)
      await logout(page)
    })

    await test.step('As applicant, verify markdown', async () => {
      await applicantQuestions.expectProgramsPage()

      const cardHtml = await page.innerHTML(
        '.cf-application-card:has-text("' + programWithExternalLink + '")',
      )
      // North Star program cards do not have a link to the external site
      expect(cardHtml).not.toContain('https://external.com/do_not_show')

      // Verify markdown was parsed correctly
      // h1 set in markdown should be changed to h2
      expect(cardHtml).toContain('<h2>Program description</h2>')
      // Lists are formatted correctly
      expect(cardHtml).toContain(
        '<ul class="list-disc mx-8"><li>Thing 1</li><li>Thing 2</li></ul>',
      )
      // Text links are formatted correctly with an icon
      expect(cardHtml).toContain(
        '<a href="https://www.example.com" class="text-blue-900 font-bold opacity-75 underline hover:opacity-100" target="_blank" aria-label="opens in a new tab" rel="nofollow noopener noreferrer">website<svg',
      )

      await validateAccessibility(page)
    })
  })
})
