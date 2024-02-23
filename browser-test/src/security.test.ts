import {createTestContext, gotoEndpoint, loginAsAdmin} from './support'
import {BASE_URL} from './support/config'

describe('applicant security', () => {
  const ctx = createTestContext()

  test('applicant cannot access admin pages', async () => {
    const {page} = ctx
    // this test visits page that returns 401 which triggers BrowserErrorWatcher.
    // Silencing error on that page.
    ctx.browserErrorWatcher.ignoreErrorsFromUrl(/\/admin\/programs/)
    const response = await gotoEndpoint(page, '/admin/programs')
    expect(response!.status()).toBe(403)
  })

  test('redirects to program index page when not logged in (guest)', async () => {
    const {page} = ctx
    await page.goto(BASE_URL)
    expect(await page.innerHTML('body')).toMatch(
      /Save time applying for programs and services/,
    )
  })

  test('redirects to program dashboard when logged in as admin', async () => {
    const {page} = ctx
    await loginAsAdmin(page)
    await page.goto(BASE_URL)
    expect(await page.innerHTML('body')).toMatch(/Program dashboard/)
    expect(await page.innerHTML('body')).toMatch(
      /Create, edit and publish programs/,
    )
  })
})
