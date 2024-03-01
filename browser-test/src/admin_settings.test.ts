import {test} from '@playwright/test'
import {
  createTestContext,
  loginAsAdmin,
  validateScreenshot,
  AdminSettings,
} from './support'

test.describe('Managing system-wide settings', () => {
  const ctx = createTestContext()

  test('Displays the settings page', async () => {
    const {page} = ctx
    await loginAsAdmin(page)

    const adminSettings = new AdminSettings(page)
    await adminSettings.gotoAdminSettings()

    await validateScreenshot(page, 'admin-settings-page')

    // Jump to a specfific section
    await page.click('a:has-text("Branding")')
    await validateScreenshot(
      page,
      'admin-settings-page-scrolled',
      /* fullPage= */ false,
    )
  })

  test('Displays the settings page in a narrow viewport', async () => {
    const {page} = ctx

    // We know the header will start to wrap at smaller widths
    await page.setViewportSize({
      width: 768,
      height: 720,
    })

    await loginAsAdmin(page)
    const adminSettings = new AdminSettings(page)
    await adminSettings.gotoAdminSettings()

    await validateScreenshot(page, 'admin-settings-page-narrow')
  })

  test('Updates settings on save', async () => {
    const {page} = ctx
    await loginAsAdmin(page)

    const adminSettings = new AdminSettings(page)
    await adminSettings.gotoAdminSettings()

    await adminSettings.disableSetting('CF_OPTIONAL_QUESTIONS')
    await adminSettings.saveChanges()
    await adminSettings.expectDisabled('CF_OPTIONAL_QUESTIONS')

    await adminSettings.enableSetting('CF_OPTIONAL_QUESTIONS')
    await adminSettings.saveChanges()
    await adminSettings.expectEnabled('CF_OPTIONAL_QUESTIONS')

    await adminSettings.enableSetting('CF_OPTIONAL_QUESTIONS')
    await adminSettings.saveChanges(/* expectUpdated= */ false)
  })
})
