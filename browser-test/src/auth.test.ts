import {
  createTestContext,
  loginAsTestUser,
  loginAsGuest,
  selectApplicantLanguage,
  validateScreenshot,
  testUserDisplayName,
  AuthStrategy,
} from './support'
import {TEST_USER_AUTH_STRATEGY} from './support/config'

describe('applicant auth', () => {
  const ctx = createTestContext()

  it('applicant can login', async () => {
    const {page} = ctx
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')

    await validateScreenshot(page, 'logged-in')

    expect(await ctx.page.textContent('html')).toContain(
      `Logged in as ${testUserDisplayName()}`,
    )
    expect(await ctx.page.textContent('html')).toContain('Logout')
  })

  it('applicant can login as guest', async () => {
    const {page} = ctx
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')

    await validateScreenshot(page, 'logged-in-guest')
    expect(await ctx.page.textContent('html')).toContain('Logged in as Guest')
    expect(await ctx.page.textContent('html')).toContain('Logout')
  })

  // so far only fake-oidc provider requires user to click "Yes" to confirm
  // logout. AWS staging uses Auth0 which doesn't. And Seattle staging uses
  // IDCS which at the moment doesn't have central logout enabled.
  if (TEST_USER_AUTH_STRATEGY === AuthStrategy.FAKE_OIDC) {
    it('applicant can confirm central provider logout', async () => {
      const {page} = ctx
      await loginAsTestUser(page)
      await selectApplicantLanguage(page, 'English')
      expect(await ctx.page.textContent('html')).toContain(
        `Logged in as ${testUserDisplayName()}`,
      )

      await page.click('text=Logout')

      await validateScreenshot(page, 'central-provider-logout')
      expect(await ctx.page.textContent('html')).toContain(
        'Do you want to sign-out from',
      )
    })
  }

  it('applicant can logout', async () => {
    const {page} = ctx
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')
    expect(await ctx.page.textContent('html')).toContain(
      `Logged in as ${testUserDisplayName()}`,
    )

    await page.click('text=Logout')
    // At the moment only fake OIDC has confirmation page during logout.
    if (TEST_USER_AUTH_STRATEGY === AuthStrategy.FAKE_OIDC) {
      await page.click('button:has-text("Yes")')
    }

    expect(await ctx.page.textContent('html')).toContain('Continue as guest')
  })

  it('applicant can logout from guest', async () => {
    const {page} = ctx
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')
    expect(await ctx.page.textContent('html')).toContain('Logged in as Guest')

    await page.click('text=Logout')
    expect(await ctx.page.textContent('html')).toContain('Continue as guest')
  })
})
