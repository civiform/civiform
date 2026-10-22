import {test, expect} from '../support/civiform_fixtures'
import {
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
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

    await page.goto('/programs')

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

  test('dev tools buttons appear inside demo banner when banner_type is demo', async ({
    page,
    adminSettings,
  }) => {
    await loginAsAdmin(page)
    await adminSettings.gotoAdminSettings()
    await adminSettings.setEnumSetting('BANNER_TYPE', 'demo')
    await adminSettings.saveChanges()
    await logout(page)

    const header = page.locator('nav')
    const demoBanner = page.locator('.cf-demo-banner')

    await test.step('DevTools button is hidden in header and shown in demo banner', async () => {
      await expect(header.getByText('DevTools')).toHaveCount(0)
      await expect(demoBanner).toBeVisible()
      await expect(demoBanner.getByText('View as:')).toBeVisible()
      await expect(demoBanner.locator('#resident')).toBeDisabled()
      await expect(demoBanner.locator('#admin')).toBeVisible()
      await expect(demoBanner.locator('#program-admin')).toBeVisible()
      await expect(demoBanner.locator('#dual-admin')).toHaveCount(0)
      await expect(demoBanner.locator('#trusted-intermediary')).toBeVisible()
      await expect(demoBanner.locator('#additional-tools')).toHaveAttribute(
        'href',
        '/dev/seed',
      )
      await expect(
        demoBanner.locator('#additional-tools svg.usa-icon'),
      ).toBeVisible()
      await validateAccessibility(page)
    })

    await test.step('buttons hide when staging_disable_demo_mode_logins is enabled', async () => {
      await enableFeatureFlag(page, 'staging_disable_demo_mode_logins')
      await page.goto('/programs')
      await expect(demoBanner).toBeVisible()
      await expect(demoBanner.locator('#admin')).toHaveCount(0)
      await expect(demoBanner.locator('#additional-tools')).toHaveCount(0)
    })

    await disableFeatureFlag(page, 'staging_disable_demo_mode_logins')
    await page.goto('/programs')
    await loginAsAdmin(page)
    await adminSettings.gotoAdminSettings()
    await adminSettings.setEnumSetting('BANNER_TYPE', 'none')
    await adminSettings.saveChanges()
  })
})
