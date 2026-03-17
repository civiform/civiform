import {test, expect} from '../support/civiform_fixtures'
import {disableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'

test.describe('Managing system-wide settings', () => {
  test.beforeEach(async ({page}) => {
    await loginAsAdmin(page)
    await disableFeatureFlag(page, 'allow_civiform_admin_access_programs')
  })

  test('Navigates to a section via anchor link', async ({
    page,
    adminSettings,
  }) => {
    await adminSettings.gotoAdminSettings()
    await page.locator('a:has-text("Branding")').click()
    await expect(page.locator('h2#branding')).toBeInViewport()
  })

  test('Header wraps at narrow width', async ({page, adminSettings}) => {
    await page.setViewportSize({width: 768, height: 720})
    await adminSettings.gotoAdminSettings()

    const header = page.locator('#admin-settings-side-nav h1')
    await validateScreenshot(header, 'admin-settings-header-narrow')
  })

  test('Updates settings on save', async ({adminSettings}) => {
    await adminSettings.gotoAdminSettings()

    await test.step('button check', async () => {
      await adminSettings.enableSetting('ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS')
      await adminSettings.saveChanges()
      await adminSettings.expectEnabled('ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS')

      await adminSettings.disableSetting('ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS')
      await adminSettings.saveChanges()
      await adminSettings.expectDisabled('ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS')

      await adminSettings.disableSetting('ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS')
      await adminSettings.saveChanges(/* expectUpdated= */ false)
    })
  })

  test('Validates theme settings', async ({adminSettings}) => {
    await adminSettings.gotoAdminSettings()

    await test.step('contrast ratio not met on primary color', async () => {
      await adminSettings.setStringSetting('THEME_COLOR_PRIMARY', '#19baff')
      await adminSettings.saveChanges(
        /* expectUpdated= */ false,
        /* expectError= */ true,
      )
      await adminSettings.expectColorContrastErrorVisible()
    })

    await test.step('contrast ratio met on primary color', async () => {
      await adminSettings.setStringSetting('THEME_COLOR_PRIMARY', '#01587d')
      await adminSettings.saveChanges(/* expectUpdated= */ true)
    })

    await test.step('contrast ratio not met on primary dark color', async () => {
      await adminSettings.setStringSetting(
        'THEME_COLOR_PRIMARY_DARK',
        '#19baff',
      )
      await adminSettings.saveChanges(
        /* expectUpdated= */ false,
        /* expectError= */ true,
      )
      await adminSettings.expectColorContrastErrorVisible()
    })

    await test.step('contrast ratio met on primary dark color', async () => {
      await adminSettings.setStringSetting(
        'THEME_COLOR_PRIMARY_DARK',
        '#01587d',
      )
      await adminSettings.saveChanges(/* expectUpdated= */ true)
    })

    await test.step('computes validation on 3-digit hex code', async () => {
      await adminSettings.setStringSetting('THEME_COLOR_PRIMARY_DARK', '#EEE')
      await adminSettings.saveChanges(
        /* expectUpdated= */ false,
        /* expectError= */ true,
      )
    })

    await test.step('can remove settings', async () => {
      await adminSettings.setStringSetting('THEME_COLOR_PRIMARY', '')
      await adminSettings.setStringSetting('THEME_COLOR_PRIMARY_DARK', '')
      await adminSettings.saveChanges(
        /* expectUpdated= */ true,
        /* expectError= */ false,
      )
    })
  })
})
