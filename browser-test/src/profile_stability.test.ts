import {test, expect} from './support/civiform_fixtures'
import {isHermeticTestEnvironment} from './support'
import {BASE_URL, FROZEN_PLAY_SESSION_COOKIE_VALUE} from './support/config'

test.describe(
  'User HTTP sessions',
  {tag: ['@parallel-candidate']},
  () => {
    interface Profile {
      id: string
      roles: string[]
      clientName: string
    }

    // This test ensures that an existing user profile, stored in a browser cookie,
    // is recognized and properly deserialized by the server.
    //
    // This guards against changes that unexpectedly affect serialization.
    test('Recognizes the profile from a frozen cookie', async ({
      page,
      context,
    }) => {
      test.skip(!isHermeticTestEnvironment(), 'Only runs in test environment')

      // Play encrypts cookies with the server secret:
      // https://www.playframework.com/documentation/2.8.x/ApplicationSecret
      //
      // For this test to run as a prober, we would need to provision one frozen
      // cookie value per environment and pass that in. That is more trouble than
      // it is worth, since the hermetic test (which relies on using the default
      // server secret) will detect any breaking changes to profile serialization.
      //
      // If this test fails after an upgrade to the Play Framework the value
      // in FROZEN_PLAY_SESSION_COOKIE_VALUE may need to be regenerated.
      const {hostname} = new URL(BASE_URL)
      const frozenCookie = {
        name: 'PLAY_SESSION',
        value: FROZEN_PLAY_SESSION_COOKIE_VALUE,
        domain: hostname,
        path: '/',
      }

      await context.addCookies([frozenCookie])
      await page.goto('/dev/profile')

      await test.step('Verify profile data', async () => {
        const content = await page.content()
        const matches = content.match(/{.*}/s)

        expect(matches).not.toBeNull()

        const profile = JSON.parse(matches![0]) as Profile
        expect(profile.id).toEqual('1')
        expect(profile.roles.length).toEqual(1)
        expect(profile.roles[0]).toEqual('ROLE_APPLICANT')
        expect(profile.clientName).toEqual('GuestClient')
      })
    })

    test('Ensures that an initial request gets a user profile', async ({
      page,
    }) => {
      // Load a page that corresponds to a user-facing route in order to get a profile.
      await expect(page.getByRole('button', {name: 'Log in'})).toHaveText(
        'Log in',
      )

      // Now validate that a user profile is present.
      await page.goto('/dev/profile')

      await test.step('Verify profile data', async () => {
        const content = await page.content()
        const matches = content.match(/{.*}/s)

        expect(matches).not.toBeNull()

        const profile = JSON.parse(matches![0]) as Profile
        expect(profile.roles.length).toEqual(1)
        expect(profile.roles[0]).toEqual('ROLE_APPLICANT')
        expect(profile.clientName).toEqual('GuestClient')
      })
    })
  },
)
