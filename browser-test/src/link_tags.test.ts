import {test, expect} from './support/civiform_fixtures'

test.describe('navigating to a deep link', () => {
  test('has favicon link', async ({page, applicantQuestions}) => {
    await applicantQuestions.gotoApplicantHomePage()

    const linkTagLocator = page.locator('link[rel="icon"]')

    await expect(linkTagLocator).toHaveAttribute('href', /.+/)
  })
})
