import {test, expect} from '../support/civiform_fixtures'
import {
  disableFeatureFlag,
  enableFeatureFlag,
  validateAccessibility,
  validateScreenshot,
} from '../support'

test.describe('developer tools', () => {
  test.afterEach(async ({page}) => {
    // Ensure the 'staging_disable_demo_mode_logins' flag is DISABLED for each test
    // unless a specific test intends to enable it.
    await disableFeatureFlag(page, 'staging_disable_demo_mode_logins')
  })

  test('dev link exists', async ({page}) => {
    const header = page.locator('nav')

    await test.step('link shown in the header', async () => {
      await expect(header.getByText('DevTools')).toBeInViewport()
      await validateAccessibility(page)
    })

    await test.step('modal appears on click', async () => {
      await header.getByText('DevTools').click()
      await validateScreenshot(page, 'dev-tools-modal')
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

    await test.step('navigating to dev tools URL unsuccessful', async () => {
      await page.goto(`/dev/seed`)
      expect(page.url()).toContain('/programs')
      expect(await page.innerText('h1')).not.toContain('Dev tools')
    })
  })

  test('dev tools page loads', async ({page}) => {
    await enableFeatureFlag(page, 'ADMIN_UI_MIGRATION_SC_ENABLED')
    await page.goto('/dev/seed')
    await validateScreenshot(page, 'dev-tools-page-migrated')
    await validateAccessibility(page)
    await disableFeatureFlag(page, 'ADMIN_UI_MIGRATION_SC_ENABLED')
  })
})
