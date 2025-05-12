import {test, expect} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  validateAccessibility,
  validateScreenshot,
} from '../support'

test.describe('developer tools', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })
  test('dev link exists', async ({page}) => {
    const header = page.locator('nav')

    await test.step('link shown in the header', async () => {
      await expect(header.getByText('DevTools')).toBeInViewport()
      await validateScreenshot(header, 'northstar-dev-tools-in-header')
      await validateAccessibility(page)
    })

    await test.step('modal appears on click', async () => {
      await header.getByText('DevTools').click()
      await validateScreenshot(page, 'northstar-dev-tools-modal')
    })
  })
  test('not functional when disable demo mode logins is enabled', async ({
    page,
  }) => {
    await enableFeatureFlag(page, 'staging_disable_demo_mode_logins')

    const header = page.locator('nav')

    await test.step('link not shown in the header', async () => {
      await expect(header.getByText('DevTools')).not.toBeInViewport()
      await validateAccessibility(page)
    })
  })
})
