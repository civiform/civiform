import {
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
    await validateScreenshot(
      page.getByTestId('page-content'),
      'admin-reporting-page-flag-enabled',
    )
    await validateAccessibility(page)
  })
})
