import {
  startSession,
  endSession,
  gotoEndpoint,
  loginAsAdmin,
  loginAsGuest,
} from './support'

describe('applicant security', () => {
  it('applicant cannot access another applicant data', async () => {
    const {browser, page} = await startSession()

    await loginAsGuest(page)

    const response = await gotoEndpoint(page, '/applicants/1234/programs')
    expect(response!.status()).toBe(401)

    await endSession(browser)
  })

  it('admin cannot access applicant pages', async () => {
    const {browser, page} = await startSession()

    await loginAsAdmin(page)
    const response = await gotoEndpoint(page, '/applicants/1234567/programs')
    expect(response!.status()).toBe(401)

    await endSession(browser)
  })
})
