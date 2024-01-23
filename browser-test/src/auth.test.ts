import {
  createTestContext,
  loginAsTestUser,
  validateScreenshot,
  testUserDisplayName,
  AuthStrategy,
  logout,
  loginAsAdmin,
  validateAccessibility,
  enableFeatureFlag,
} from './support'
import {TEST_USER_AUTH_STRATEGY} from './support/config'

describe('applicant auth', () => {
  const ctx = createTestContext()

  it('applicant can login', async () => {
    const {page} = ctx
    await loginAsTestUser(page)
    await validateScreenshot(page, 'logged-in')

    expect(await ctx.page.textContent('html')).toContain(
      `Logged in as ${testUserDisplayName()}`,
    )
    expect(await ctx.page.textContent('html')).toContain('Logout')
  })

  it('applicant can login as guest', async () => {
    const {page} = ctx
    await validateScreenshot(page, 'logged-in-guest')
    expect(await ctx.page.textContent('html')).toContain("You're a guest user.")
    expect(await ctx.page.textContent('html')).toContain('End session')
  })

  // so far only fake-oidc provider requires user to click "Yes" to confirm
  // logout. AWS staging uses Auth0 which doesn't. And Seattle staging uses
  // IDCS which at the moment doesn't have central logout enabled.
  if (TEST_USER_AUTH_STRATEGY === AuthStrategy.FAKE_OIDC) {
    it('applicant can confirm central provider logout', async () => {
      const {page} = ctx
      await loginAsTestUser(page)
      expect(await ctx.page.textContent('html')).toContain(
        `Logged in as ${testUserDisplayName()}`,
      )

      await page.click('text=Logout')

      await validateScreenshot(page, 'central-provider-logout')
      expect(await ctx.page.textContent('html')).toContain(
        'Do you want to sign-out from',
      )
    })

    it('applicant can confirm central provider logout, enhanced version', async () => {
      const {page} = ctx
      await enableFeatureFlag(page, 'applicant_oidc_enhanced_logout_enabled')
      await loginAsTestUser(page)
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
    expect(await ctx.page.textContent('html')).toContain(
      `Logged in as ${testUserDisplayName()}`,
    )

    await logout(page)

    expect(await ctx.page.textContent('html')).toContain('Find programs')

    // Try login again, ensuring that full login process is followed. If login
    // page doesn't ask for username/password - the method will fail.
    await loginAsTestUser(page)
    expect(await ctx.page.textContent('html')).toContain(
      `Logged in as ${testUserDisplayName()}`,
    )
  })

  it('applicant can logout, enhanced version', async () => {
    const {page} = ctx
    await enableFeatureFlag(page, 'applicant_oidc_enhanced_logout_enabled')
    await loginAsTestUser(page)
    expect(await ctx.page.textContent('html')).toContain(
      `Logged in as ${testUserDisplayName()}`,
    )

    await logout(page)

    expect(await ctx.page.textContent('html')).toContain('Find programs')

    // Try login again, ensuring that full login process is followed. If login
    // page doesn't ask for username/password - the method will fail.
    await loginAsTestUser(page)
    expect(await ctx.page.textContent('html')).toContain(
      `Logged in as ${testUserDisplayName()}`,
    )
  })

  it('applicant can logout (end session) from guest', async () => {
    const {page} = ctx
    expect(await ctx.page.textContent('html')).toContain("You're a guest user.")

    await page.click('text=End session')
    expect(await ctx.page.textContent('html')).toContain('Find programs')
  })

  it('toast is shown when either guest or logged-in user end their session', async () => {
    const {page} = ctx
    await logout(page, /* closeToast=*/ false)
    await validateScreenshot(page, 'guest-just-ended-session')
    await validateAccessibility(page)

    await loginAsTestUser(page)
    await logout(page, /* closeToast=*/ false)
    await validateScreenshot(page, 'user-just-ended-session')
    await validateAccessibility(page)
  })

  it('guest login followed by auth login stores submitted applications', async () => {
    const {page, adminPrograms, applicantQuestions} = ctx
    await loginAsAdmin(page)
    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()

    await logout(page)
    await applicantQuestions.clickApplyProgramButton(programName)
    await applicantQuestions.submitFromReviewPage()
    await loginAsTestUser(page)

    // Check that program is marked as submitted.
    expect(
      await page.innerText(`.cf-application-card:has-text("${programName}")`),
    ).toMatch(/Submitted \d?\d\/\d?\d\/\d\d/)

    // Logout and login to make sure data is tied to account.
    await logout(page)
    await loginAsTestUser(page)
    expect(
      await page.innerText(`.cf-application-card:has-text("${programName}")`),
    ).toMatch(/Submitted \d?\d\/\d?\d\/\d\d/)
  })
})
