import {
  createTestContext,
  loginAsAdmin,
  validateScreenshot,
  AdminSettings,
} from './support'

describe('Managing system-wide settings', () => {
  const ctx = createTestContext()

  it('Displays the settings page', async () => {
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
      /* screenshotOptions=  */ undefined,
      /* matchImageSnapshotOptions */ undefined,
      /* fullPage= */ false,
    )
  })

  it('Updates settings on save', async () => {
    const {page} = ctx
    await loginAsAdmin(page)

    const adminSettings = new AdminSettings(page)
    await adminSettings.gotoAdminSettings()

    await adminSettings.enableSetting('ESRI_ADDRESS_CORRECTION_ENABLED')
    await adminSettings.saveChanges()
    await adminSettings.expectEnabled('ESRI_ADDRESS_CORRECTION_ENABLED')

    await adminSettings.disableSetting('ESRI_ADDRESS_CORRECTION_ENABLED')
    await adminSettings.saveChanges()
    await adminSettings.expectDisabled('ESRI_ADDRESS_CORRECTION_ENABLED')

    await adminSettings.disableSetting('ESRI_ADDRESS_CORRECTION_ENABLED')
    await adminSettings.saveChanges(/* expectUpdated= */ false)
  })
})
