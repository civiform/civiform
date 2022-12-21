import {
  createTestContext,
  gotoEndpoint,
  loginAsAdmin,
  loginAsGuest,
} from './support'
import {BASE_URL} from './support/config'

describe('applicant security', () => {
  const ctx = createTestContext()

  it('applicant cannot access another applicant data', async () => {
    const {page} = ctx
    // this test visits page that returns 401 which triggers BrowserErrorWatcher.
    // Silencing error on that page.
    ctx.browserErrorWatcher.ignoreErrorsFromUrl(/applicants\/1234\/programs/)
    await loginAsGuest(page)
    const response = await gotoEndpoint(page, '/applicants/1234/programs')
    expect(response!.status()).toBe(401)
  })

  it('admin cannot access applicant pages', async () => {
    const {page} = ctx
    // this test visits page that returns 401 which triggers BrowserErrorWatcher.
    // Silencing error on that page.
    ctx.browserErrorWatcher.ignoreErrorsFromUrl(/applicants\/1234567\/programs/)
    await loginAsAdmin(page)
    const response = await gotoEndpoint(page, '/applicants/1234567/programs')
    expect(response!.status()).toBe(401)
  })

  it('applicant cannot access admin pages', async () => {
    const {page} = ctx
    // this test visits page that returns 401 which triggers BrowserErrorWatcher.
    // Silencing error on that page.
    ctx.browserErrorWatcher.ignoreErrorsFromUrl(/\/admin\/programs/)
    await loginAsGuest(page)
    const response = await gotoEndpoint(page, '/admin/programs')
    expect(response!.status()).toBe(403)
  })

  it('redirects to login form when not logged in', async () => {
    const {page} = ctx
    await page.goto(BASE_URL)
    expect(await page.innerHTML('body')).toMatch(
      /Please log in with your (.*) account/,
    )
  })

  it('redirects to program dashboard when logged in as admin', async () => {
    const {page} = ctx
    await loginAsAdmin(page)
    await page.goto(BASE_URL)
    expect(await page.innerHTML('body')).toMatch(/Program dashboard/)
    expect(await page.innerHTML('body')).toMatch(
      /Create, edit and publish programs/,
    )
  })

  it('redirects to language page when logged in as guest', async () => {
    const {page} = ctx
    await loginAsGuest(page)
    await page.goto(BASE_URL)
    expect(await page.innerHTML('body')).toMatch(
      /Please select your preferred language/,
    )
  })
})
