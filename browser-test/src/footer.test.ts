import {test, expect} from './support/civiform_fixtures'
import {validateAccessibility, validateScreenshot} from './support'

test.describe('the footer', () => {
  test('does not have civiform version when feature flag is disabled', async ({
    page,
  }) => {
    const footerLocator = page.locator('footer')

    await validateScreenshot(footerLocator, 'footer-no-version')
    await validateAccessibility(page)

    await test.step('Footer does not have version text', async () => {
      await expect(footerLocator).not.toContainText('CiviForm version:')
    })
  })
})
