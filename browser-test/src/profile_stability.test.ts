import {createTestContext} from './support'
import {BASE_URL} from './support/config'
import {FrozenPlaySessionCookie} from './support/cookies'

describe('user HTTP sessions', () => {
  const ctx = createTestContext()

  interface Profile {
    id: string
    roles: string[]
    clientName: string
  }

  // This test ensures that an existing user profile, stored in a browser cookie,
  // is recognized and properly deserialized by the server.
  //
  // This guards against changes that unexpectedly affect serialization.
  it('recognizes the profile from a frozen cookie', async () => {
    await ctx.page.context().addCookies([FrozenPlaySessionCookie.getCookie()])

    await ctx.page.goto(BASE_URL + '/dev/profile')

    const content = await ctx.page.content()
    // Pull out the JSON from the page using multiline matching.
    const matches = content.match(/{.*}/s)

    expect(matches).not.toBeNull()

    if (matches) {
      const profile = JSON.parse(matches[0]) as Profile
      // The profile id is a string containing a number. The id varies
      // in different environments, so we just ensure it is present and
      // holds a positive integer.
      expect(profile.id.length).toBeGreaterThanOrEqual(1)
      expect(Number(profile.id)).toBeGreaterThanOrEqual(1)
      expect(profile.roles.length).toEqual(1)
      expect(profile.roles[0]).toEqual('ROLE_APPLICANT')
      expect(profile.clientName).toEqual('GuestClient')
    }
  })
})
