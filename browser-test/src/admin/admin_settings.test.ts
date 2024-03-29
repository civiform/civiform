import {test} from '../support/civiform_fixtures'
import {loginAsAdmin, validateScreenshot} from '../support'

test.describe(
  'Managing system-wide settings',
  {tag: ['@uses-fixtures']},
  () => {
    test('Displays the settings page', async ({page, adminSettings}) => {
      await loginAsAdmin(page)

      await test.step('Go to admin settings page and take screenshot', async () => {
        await adminSettings.gotoAdminSettings()
        await validateScreenshot(page, 'admin-settings-page')
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

        await page.reload()

        await validateScreenshot(page, 'admin-settings-page-narrow')
      })
    })

    test('Updates settings on save', async ({page, adminSettings}) => {
      await loginAsAdmin(page)

      await adminSettings.gotoAdminSettings()

      await test.step('button check', async () => {
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
  },
)
