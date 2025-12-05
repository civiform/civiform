import {expect, test} from '../support/civiform_fixtures'
import {enableFeatureFlag, validateAccessibility} from '../support'

test.describe('North Star Ineligible Page Tests', () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'show_not_production_banner_enabled')
  })

  test('View "Not Production" banner', async ({page}) => {
    await test.step('Verify banner', async () => {
      await expect(
        page.getByText(
          'This site is for testing purposes only. Do not enter personal information.',
        ),
      ).toBeVisible()

      await validateAccessibility(page)
    })
  })
})
