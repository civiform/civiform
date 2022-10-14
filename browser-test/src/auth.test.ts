import {
  createTestContext,
  gotoEndpoint,
  loginAsAdmin,
  loginAsTestUser,
  loginAsGuest,
  selectApplicantLanguage,
  validateScreenshot,
  logout,
  testUserEmail,
} from './support'
describe('applicant auth', () => {
  const ctx = createTestContext()

  it('applicant can login', async () => {
    const {page} = ctx
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')

    await validateScreenshot(page, 'logged_in')

    expect(await ctx.page.textContent('html')).toContain('Logged in as ' + testUserEmail())
    expect(await ctx.page.textContent('html')).toContain('Logout')
  })

  it('applicant can login as guest', async () => {
    const {page} = ctx
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')

    await validateScreenshot(page, 'logged_in_guest')
    expect(await ctx.page.textContent('html')).toContain('Logged in as Guest')
    expect(await ctx.page.textContent('html')).toContain('Logout')
  })

  it('applicant can confirm central provider logout', async () => {
    const {page} = ctx
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')
    expect(await ctx.page.textContent('html')).toContain('Logged in as ' + testUserEmail())

    await page.click('text=Logout')

    await validateScreenshot(page, 'central-provider-logout')
    expect(await ctx.page.textContent('html')).toContain('Do you want to sign-out from')
  })

  it('applicant can logout', async () => {
    const {page} = ctx
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')
    expect(await ctx.page.textContent('html')).toContain('Logged in as ' + testUserEmail())

    await page.click('text=Logout')
    await page.click('button:has-text("Yes")')

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
