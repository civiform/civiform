import { expect, test } from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../support'

test.describe('Ineligible Page Tests', () => {
  test('View "Not Production" banner', async ({ page }) => {
    await enableFeatureFlag(page, 'show_not_production_banner_enabled')
    await page.goto('/programs')

    await test.step('Verify banner', async () => {
      await expect(
        page.getByText(
          'This site is for testing purposes only. Do not enter personal information.',
        ),
      ).toBeVisible()

      await validateAccessibility(page)
    })
  })

  test('View "Demo" banner', async ({ page, adminSettings }) => {
    await loginAsAdmin(page)
    await adminSettings.gotoAdminSettings()
    await adminSettings.setEnumSetting('BANNER_TYPE', 'demo')
    await adminSettings.saveChanges()
    await logout(page)

    await page.goto('/programs')

    await test.step('Verify demo banner', async () => {
      const demoBanner = page.getByRole('region', {
        name: 'Demo mode informational alert',
      })
      await expect(demoBanner).toBeVisible()
      await validateScreenshot(demoBanner, 'demo-banner')
      await validateAccessibility(page)
    })

    await loginAsAdmin(page)
    await adminSettings.gotoAdminSettings()
    await adminSettings.setEnumSetting('BANNER_TYPE', 'none')
    await adminSettings.saveChanges()
    await logout(page)
  })

  test('View "Demo" banner on mobile', async ({ page, adminSettings }) => {
    await loginAsAdmin(page)
    await adminSettings.gotoAdminSettings()
    await adminSettings.setEnumSetting('BANNER_TYPE', 'demo')
    await adminSettings.saveChanges()
    await logout(page)

    await page.setViewportSize({ width: 360, height: 800 })
    await page.goto('/programs')

    await test.step('Verify demo banner on mobile', async () => {
      const demoBanner = page.getByRole('region', {
        name: 'Demo mode informational alert',
      })
      await expect(demoBanner).toBeVisible()
      await validateScreenshot(demoBanner, 'demo-banner-mobile')
      await validateAccessibility(page)
    })

    await page.setViewportSize({ width: 1280, height: 720 })
    await loginAsAdmin(page)
    await adminSettings.gotoAdminSettings()
    await adminSettings.setEnumSetting('BANNER_TYPE', 'none')
    await adminSettings.saveChanges()
    await logout(page)
  })
})
