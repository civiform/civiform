import {test, expect} from './support/civiform_fixtures'
import {enableFeatureFlag} from './support'

test.describe('navigating to a deep link', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test('has favicon link', async ({page, applicantQuestions}) => {
    await applicantQuestions.gotoApplicantHomePage()

    const linkTagLocator = page.locator('link[rel="icon"]')

    await expect(linkTagLocator).toHaveAttribute('href', /.+/)
  })
})
