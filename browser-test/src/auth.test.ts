import {test, expect} from './fixtures/custom_fixture'
import {
  loginAsTestUser,
  validateScreenshot,
  testUserDisplayName,
  AuthStrategy,
  logout,
  loginAsAdmin,
  validateAccessibility,
} from './support'
import {TEST_USER_AUTH_STRATEGY} from './support/config'

test.describe('applicant auth', {tag: ['@migrated']}, () => {
  test('applicant can login', async ({page}) => {
    await loginAsTestUser(page)
    await validateScreenshot(page, 'logged-in')

    expect(await page.textContent('html')).toContain(
      `Logged in as ${testUserDisplayName()}`,
    )
    expect(await page.textContent('html')).toContain('Logout')
  })

  test('applicant can login as guest', async ({page}) => {
    await validateScreenshot(page, 'logged-in-guest')
    expect(await page.textContent('html')).toContain("You're a guest user.")
    expect(await page.textContent('html')).toContain('End session')
  })

  // so far only fake-oidc provider requires user to click "Yes" to confirm
  // logout. AWS staging uses Auth0 which doesn't. And Seattle staging uses
  // IDCS which at the moment doesn't have central logout enabled.
  if (TEST_USER_AUTH_STRATEGY === AuthStrategy.FAKE_OIDC) {
    test('applicant can confirm central provider logout', async ({page}) => {
      await loginAsTestUser(page)
      expect(await page.textContent('html')).toContain(
        `Logged in as ${testUserDisplayName()}`,
      )

      await page.click('text=Logout')

      await validateScreenshot(page, 'central-provider-logout')
      expect(await page.textContent('html')).toContain(
        'Do you want to sign-out from',
      )
    })
  }

  test('applicant can logout', async ({page}) => {
    await loginAsTestUser(page)
    expect(await page.textContent('html')).toContain(
      `Logged in as ${testUserDisplayName()}`,
    )

    await logout(page)

    expect(await page.textContent('html')).toContain('Find programs')

    // Try login again, ensuring that full login process is followed. If login
    // page doesn't ask for username/password - the method will fail.
    await loginAsTestUser(page)
    expect(await page.textContent('html')).toContain(
      `Logged in as ${testUserDisplayName()}`,
    )
  })

  test('applicant can logout (end session) from guest', async ({page}) => {
    expect(await page.textContent('html')).toContain("You're a guest user.")

    await page.click('text=End session')
    expect(await page.textContent('html')).toContain('Find programs')
  })

  test('toast is shown when either guest or logged-in user end their session', async ({
    page,
  }) => {
    await logout(page, /* closeToast=*/ false)
    await validateScreenshot(page, 'guest-just-ended-session')
    await validateAccessibility(page)

    await loginAsTestUser(page)
    await logout(page, /* closeToast=*/ false)
    await validateScreenshot(page, 'user-just-ended-session')
    await validateAccessibility(page)
  })

  test('guest login followed by auth login stores submitted applications', async ({
    page,
    adminPrograms,
    applicantQuestions,
  }) => {
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
