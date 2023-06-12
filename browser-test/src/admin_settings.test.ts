import {BASE_URL} from './support/config'
import {createTestContext, loginAsAdmin, validateScreenshot} from './support'

describe('Managing system-wide settings', () => {
  const ctx = createTestContext()

  it('Displays the settings page', async () => {
    const {page} = ctx
    await loginAsAdmin(page)
    await page.goto(BASE_URL + `/admin/settings`)

    await page.waitForSelector('h1:has-text("Settings")')
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
})
