import {
  createTestContext,
  loginAsTestUser,
  loginAsGuest,
  selectApplicantLanguage,
  validateScreenshot,
  testUserDisplayName,
  AuthStrategy,
  gotoEndpoint,
  logout,
  loginAsAdmin,
} from './support'
import {TEST_USER_AUTH_STRATEGY} from './support/config'
import {Page} from 'playwright'

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

    await ctx.page.waitForURL(/.*\/loginForm/)
    expect(await ctx.page.textContent('html')).toContain('Continue as guest')

    // Try login again, ensuring that full login process is followed. If login
    // page doesn't ask for username/password - the method will fail.
    await loginAsTestUser(page)
    expect(await ctx.page.textContent('html')).toContain(
      `Logged in as ${testUserDisplayName()}`,
    )
  })

  it('applicant can logout from guest', async () => {
    const {page} = ctx
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')
    expect(await ctx.page.textContent('html')).toContain('Logged in as Guest')

    await page.click('text=Logout')
    expect(await ctx.page.textContent('html')).toContain('Continue as guest')
  })

  it('guest login followed by auth login stores submitted applications', async () => {
    const {page, adminPrograms, applicantQuestions} = ctx
    await loginAsAdmin(page)
    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllPrograms()

    await logout(page)
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')

    await applicantQuestions.clickApplyProgramButton(programName)
    await applicantQuestions.submitFromPreviewPage()
    await loginAsTestUser(page, 'a:has-text("Create account or sign in")')

    // Check that program is marked as submitted.
    expect(
      await page.innerText(`.cf-application-card:has-text("${programName}")`),
    ).toMatch(/Submitted \d\d\/\d\d\/\d\d/)

    // Logout and login to make sure data is tied to account.
    await logout(page)
    await loginAsTestUser(page)
    expect(
      await page.innerText(`.cf-application-card:has-text("${programName}")`),
    ).toMatch(/Submitted \d\d\/\d\d\/\d\d/)
  })
})
