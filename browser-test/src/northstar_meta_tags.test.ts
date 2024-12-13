import {test, expect} from './support/civiform_fixtures'
import {enableFeatureFlag} from './support'

test.describe('navigating to a deep link', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test('has civiform build tag', async ({page, applicantQuestions}) => {
    await applicantQuestions.gotoApplicantHomePage()
    await expect(
      page.locator('meta[name="civiform-build-tag"]'),
    ).toHaveAttribute('content', 'dev')
  })
})
