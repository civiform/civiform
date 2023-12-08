import {createTestContext, isHermeticTestEnvironment} from './support'
import {BASE_URL, FROZEN_PLAY_SESSION_COOKIE_VALUE} from './support/config'

describe('user HTTP sessions', () => {
  const ctx = createTestContext()

  interface Profile {
    id: string
    roles: string[]
    clientName: string
  }

  // TODO(#6069): Get this to work as a prober test.
  if (isHermeticTestEnvironment()) {
    // This test ensures that an existing user profile, stored in a browser cookie,
    // is recognized and properly deserialized by the server.
    //
    // This guards against changes that unexpectedly affect serialization.
    it('recognizes the profile from a frozen cookie', async () => {
      const {hostname} = new URL(BASE_URL)
      const frozenCookie = {
        name: 'PLAY_SESSION',
        value: FROZEN_PLAY_SESSION_COOKIE_VALUE,
        domain: hostname,
        path: '/',
      }

      await ctx.page.context().addCookies([frozenCookie])
      await ctx.page.goto(BASE_URL + '/dev/profile')

      const content = await ctx.page.content()
      // Pull out the JSON from the page using multiline matching.
      const matches = content.match(/{.*}/s)

      expect(matches).not.toBeNull()

      if (matches) {
        const profile = JSON.parse(matches[0]) as Profile
        expect(profile.id).toEqual('1')
        expect(profile.roles.length).toEqual(1)
        expect(profile.roles[0]).toEqual('ROLE_APPLICANT')
        expect(profile.clientName).toEqual('GuestClient')
      }
    })
  }
})
