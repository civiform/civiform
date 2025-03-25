import {test} from '../support/civiform_fixtures'
import {disableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'

test.describe('Managing system-wide settings', () => {
  test('Displays the settings page', async ({page, adminSettings}) => {
    await loginAsAdmin(page)

    await test.step('Go to admin settings page and take screenshot', async () => {
      await page.setViewportSize({
        width: 1280,
        height: 720,
      })

      await adminSettings.gotoAdminSettings()
      await validateScreenshot(
        page,
        'admin-settings-page',
        /* fullPage= */ false,
      )
    })

    await test.step('Jump to a specific section', async () => {
      await page.click('a:has-text("Branding")')
      await validateScreenshot(
        page,
        'admin-settings-page-scrolled',
        /* fullPage= */ false,
      )
    })

    await test.step('Set viewport to a narrow width and take screenshot', async () => {
      // We know the header will start to wrap at smaller widths
      await page.setViewportSize({
        width: 768,
        height: 720,
      })

      await adminSettings.gotoAdminSettings()

      await validateScreenshot(
        page,
        'admin-settings-page-narrow',
        /* fullPage= */ false,
      )
    })
  })

  test('Updates settings on save', async ({page, adminSettings}) => {
    await loginAsAdmin(page)

    await adminSettings.gotoAdminSettings()

    await test.step('button check', async () => {
      await disableFeatureFlag(page, 'allow_civiform_admin_access')
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
})
