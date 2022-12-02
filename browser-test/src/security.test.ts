import {
  createTestContext,
  gotoEndpoint,
  loginAsAdmin,
  loginAsGuest,
} from './support'

describe('applicant security', () => {
  const ctx = createTestContext()

  it('applicant cannot access another applicant data', async () => {
    const {page} = ctx
    ctx.browserErrorWatcher.ignoreErrorsFromUrl(/.*applicants\/1234\/programs/)
    await loginAsGuest(page)
    const response = await gotoEndpoint(page, '/applicants/1234/programs')
    expect(response!.status()).toBe(401)
  })

  it('admin cannot access applicant pages', async () => {
    const {page} = ctx
    ctx.browserErrorWatcher.ignoreErrorsFromUrl(
      /.*applicants\/1234567\/programs/,
    )
    await loginAsAdmin(page)
    const response = await gotoEndpoint(page, '/applicants/1234567/programs')
    expect(response!.status()).toBe(401)
  })
})
