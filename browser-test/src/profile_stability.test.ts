import {createTestContext} from './support'
import {BASE_URL, FROZEN_PLAY_SESSION_COOKIE_VALUE} from './support/config'

describe('developer tools', () => {
  const ctx = createTestContext()

  // This test ensures that an existing user profile, stored in a browser cookie,
  // is recognized and properly deserialized by the server.
  //
  // This guards against changes that unexpectedly affect serialization.
  it('recognizes the profile from a frozen cookie', async () => {
    const hostname = new URL(BASE_URL).hostname
    const frozenCookie = {
      name: 'PLAY_SESSION',
      value: FROZEN_PLAY_SESSION_COOKIE_VALUE,
      domain: hostname,
      expires: 2147483647,
      path: '/',
    }

    await ctx.page.context().addCookies([frozenCookie])
    await ctx.page.goto(BASE_URL + '/dev/profile')
    const content = await ctx.page.content()

    // The field order is non-deterministic, so just check some key fields.
    expect(content).toContain('"id" : "1"')
    expect(content).toContain('"roles" : [ "ROLE_APPLICANT" ]')
    expect(content).toContain('"clientName" : "GuestClient"')
  })
})
