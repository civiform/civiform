import {test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  validateAccessibility,
  validateScreenshot,
} from '../support'

test.describe('North Star footer', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test('renders footer', async ({page}) => {
    await validateScreenshot(page.locator('footer'), 'north-star-footer')
    await validateAccessibility(page)
  })
})
