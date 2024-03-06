import {test, expect} from './fixtures/custom_fixture'
import {
  enableFeatureFlag,
  disableFeatureFlag,
  validateAccessibility,
  validateScreenshot,
} from './support'

test.describe('the footer', {tag: ['@migrated']}, () => {
  test.describe('without civiform version feature flag', () => {
    test('does not have civiform version', async ({page}) => {
      await disableFeatureFlag(page, 'show_civiform_image_tag_on_landing_page')
      await validateScreenshot(page.locator('footer'), 'footer-no-version')
      await validateAccessibility(page)

      expect(await page.textContent('html')).not.toContain('CiviForm version:')
    })
  })

  test.describe('with civiform version feature flag', () => {
    test('has civiform version', async ({page}) => {
      await enableFeatureFlag(page, 'show_civiform_image_tag_on_landing_page')
      await validateScreenshot(page.locator('footer'), 'footer-with-version')
      await validateAccessibility(page)     

      expect(await page.textContent('html')).toContain('CiviForm version:')
    })
  })
})
