import {test, expect} from '@playwright/test'
import {createTestContext, validateAccessibility} from './support'
import {BASE_URL} from './support/config'

test.describe('user HTTP sessions', () => {
  const ctx = createTestContext()

  interface Profile {
    id: string
    roles: string[]
    clientName: string
  }

  test('ensures that an initial request gets a user profile', async () => {
    // Load a page that corresponds to a user-facing route in order to get a profile.
    const {page} = ctx
    await validateAccessibility(page)
    expect(await page.textContent('#login-button')).toContain('Log in')

    // Now validate that a user profile is present.
    await ctx.page.goto(BASE_URL + '/dev/profile')

    const content = await ctx.page.content()
    // Pull out the JSON from the page using multiline matching.
    const matches = content.match(/{.*}/s)

    expect(matches).not.toBeNull()

    if (matches) {
      const profile = JSON.parse(matches[0]) as Profile
      expect(profile.roles.length).toEqual(1)
      expect(profile.roles[0]).toEqual('ROLE_APPLICANT')
      expect(profile.clientName).toEqual('GuestClient')
    }
  })
})
