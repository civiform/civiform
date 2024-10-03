import {expect, test} from '../support/civiform_fixtures'
import {enableFeatureFlag, validateAccessibility} from '../support'

test.describe('North Star Ineligible Page Tests', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'show_not_production_banner_enabled')
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test('View "Not Production" banner', async ({page}) => {
    await test.step('Verify banner', async () => {
      await expect(
        page.getByText(
          'This site is for testing purposes only. Do not enter personal information.',
        ),
      ).toBeVisible()
      await expect(
        page.getByText(
          'To apply to a program or service go to City of TestCity.',
        ),
      ).toBeVisible()

      await validateAccessibility(page)
    })
  })
})
