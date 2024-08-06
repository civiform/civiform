import {test, expect} from './support/civiform_fixtures'
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

test.describe('Applicant auth', () => {
  test('Applicant can login', async ({page}) => {
    await loginAsTestUser(page)
    await validateScreenshot(page, 'logged-in')

    await expect(page.getByRole('banner')).toContainText(
      `Logged in as ${testUserDisplayName()}`,
    )
    await expect(
      page.getByRole('banner').getByRole('link', {name: 'Logout'}),
    ).toBeAttached()
  })

  test('Applicant can login as guest', async ({page}) => {
    await validateScreenshot(page, 'logged-in-guest')

    await expect(page.getByRole('banner')).toContainText("You're a guest user.")
    await expect(
      page.getByRole('banner').getByRole('link', {name: 'End session'}),
    ).toBeAttached()
  })

  test('Applicant can confirm central provider logout', async ({page}) => {
    test.skip(
      TEST_USER_AUTH_STRATEGY !== AuthStrategy.FAKE_OIDC,
      'Only runs in test environment',
    )
    // so far only fake-oidc provider requires user to click "Yes" to confirm
    // logout. AWS staging uses Auth0 which doesn't. And Seattle staging uses
    // IDCS which at the moment doesn't have central logout enabled.

    await loginAsTestUser(page)
    await expect(page.getByRole('banner')).toContainText(
      `Logged in as ${testUserDisplayName()}`,
    )

    await page.getByRole('link', {name: 'Logout'}).click()

    await validateScreenshot(page, 'central-provider-logout')
    await expect(
      page.getByRole('heading', {name: 'Do you want to sign-out from'}),
    ).toBeAttached()
  })

  test('Applicant can logout', async ({page}) => {
    await loginAsTestUser(page)
    await expect(page.getByRole('banner')).toContainText(
      `Logged in as ${testUserDisplayName()}`,
    )

    await logout(page)

    await expect( page.title()).toContain('Find programs')

    // Try login again, ensuring that full login process is followed. If login
    // page doesn't ask for username/password - the method will fail.
    await loginAsTestUser(page)
    await expect(page.getByRole('banner')).toContainText(
      `Logged in as ${testUserDisplayName()}`,
    )
  })

  test('Applicant can logout (end session) from guest', async ({page}) => {
    await expect(page.getByRole('banner')).toContainText("You're a guest user.")
    await page
      .getByRole('banner')
      .getByRole('link', {name: 'End session'})
      .click()
    await expect( page.title()).toContain('Find programs')
  })

  test('Toast is shown when either guest or logged-in user end their session', async ({
    page,
  }) => {
    await logout(page, /* closeToast=*/ false)
    await validateScreenshot(page, 'guest-just-ended-session')

    await loginAsTestUser(page)
    await logout(page, /* closeToast=*/ false)
    await validateScreenshot(page, 'user-just-ended-session')

    await validateAccessibility(page)
  })

  test('Guest login followed by auth login stores submitted applications', async ({
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
    const applicationCardLocator = page.getByRole('heading', {
      name: programName,
    })
    await expect(applicationCardLocator).toBeAttached()

    // locator("..") gets the direct parent element
    await expect(
      applicationCardLocator.locator('..').getByText('Submitted'),
    ).toContainText(/\d?\d\/\d?\d\/\d\d/)

    // Logout and login to make sure data is tied to account.
    await logout(page)
    await loginAsTestUser(page)

    // locator("..") gets the direct parent element
    await expect(
      applicationCardLocator.locator('..').getByText('Submitted'),
    ).toContainText(/\d?\d\/\d?\d\/\d\d/)
  })
})
