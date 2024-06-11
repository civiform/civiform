import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  disableFeatureFlag,
  validateAccessibility,
  validateScreenshot,
} from './support'

test.describe('the footer', () => {
  test('does not have civiform version when feature flag is disabled', async ({
    page,
  }) => {
    const footerLocator = page.locator('footer')

    await disableFeatureFlag(page, 'show_civiform_image_tag_on_landing_page')
    await validateScreenshot(footerLocator, 'footer-no-version')
    await validateAccessibility(page)

    await test.step('Footer does not have version text', async () => {
      await expect(footerLocator).not.toContainText('CiviForm version:')
    })
  })

  test('has civiform version when feature flag is enabled', async ({page}) => {
    const footerLocator = page.locator('footer')

    await enableFeatureFlag(page, 'show_civiform_image_tag_on_landing_page')
    await validateScreenshot(footerLocator, 'footer-with-version')
    await validateAccessibility(page)

    await test.step('Footer does has version text', async () => {
      await expect(footerLocator).toContainText('CiviForm version:')
    })
  })
})
