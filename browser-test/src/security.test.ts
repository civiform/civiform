import {
  createBrowserContext,
  gotoEndpoint,
  loginAsAdmin,
  loginAsGuest,
} from './support'

describe('applicant security', () => {
  const ctx = createBrowserContext()
  it('applicant cannot access another applicant data', async () => {
    const {page} = ctx

    await loginAsGuest(page)

    const response = await gotoEndpoint(page, '/applicants/1234/programs')
    expect(response!.status()).toBe(401)
  })

  it('admin cannot access applicant pages', async () => {
    const {page} = ctx

    await loginAsAdmin(page)
    const response = await gotoEndpoint(page, '/applicants/1234567/programs')
    expect(response!.status()).toBe(401)
  })
})
