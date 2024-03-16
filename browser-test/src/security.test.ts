import {test, expect} from './fixtures/custom_fixture'
import {gotoEndpoint, loginAsAdmin} from './support'
import {BrowserErrorWatcher} from './support/browser_error_watcher'
import {BASE_URL} from './support/config'

test.describe('applicant security', {tag: ['@migrated']}, () => {
  test('applicant cannot access admin pages', async ({page}) => {
    // this test visits page that returns 401 which triggers BrowserErrorWatcher.
    // Silencing error on that page.
    const browserErrorWatcher = new BrowserErrorWatcher(page)
    browserErrorWatcher.ignoreErrorsFromUrl(/\/admin\/programs/)
    const response = await gotoEndpoint(page, '/admin/programs')
    expect(response!.status()).toBe(403)
  })

  test('redirects to program index page when not logged in (guest)', async ({
    page,
  }) => {
    await page.goto(BASE_URL)
    expect(await page.innerHTML('body')).toMatch(
      /Save time applying for programs and services/,
    )
  })

  test('redirects to program dashboard when logged in as admin', async ({
    page,
  }) => {
    await loginAsAdmin(page)
    await page.goto(BASE_URL)
    expect(await page.innerHTML('body')).toMatch(/Program dashboard/)
    expect(await page.innerHTML('body')).toMatch(
      /Create, edit and publish programs/,
    )
  })
})
