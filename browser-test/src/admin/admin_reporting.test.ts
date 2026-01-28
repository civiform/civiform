import {
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  validateAccessibility,
  validateScreenshot,
} from '../support'
import {test} from '../support/civiform_fixtures'

test.describe('Admin Reporting', () => {
  const reportingUrl = '/admin/reporting'

  test('USWDS present when ADMIN_UI_MIGRATION_SC_ENABLED enabled', async ({
    page,
  }) => {
    await enableFeatureFlag(page, 'ADMIN_UI_MIGRATION_SC_ENABLED')
    await loginAsAdmin(page)
    await page.goto(reportingUrl)
    await validateScreenshot(page, 'admin-reporting-page-flag-enabled', {
      fullPage: true,
    })
    await validateAccessibility(page)
  })

  test('USWDS not present when ADMIN_UI_MIGRATION_SC_ENABLED disabled', async ({
    page,
  }) => {
    await disableFeatureFlag(page, 'ADMIN_UI_MIGRATION_SC_ENABLED')
    await loginAsAdmin(page)
    await page.goto(reportingUrl)
    await validateScreenshot(page, 'admin-reporting-page-flag-disabled', {
      fullPage: true,
    })
  })
})
