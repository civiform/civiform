import {loginAsAdmin, validateAccessibility} from '../support'
import {test} from '../support/civiform_fixtures'

test.describe('Admin Reporting', () => {
  const reportingUrl = '/admin/reporting'

  test('reporting page is accessible', async ({page}) => {
    await loginAsAdmin(page)
    await page.goto(reportingUrl)
    await validateAccessibility(page)
  })
})
