import {test, expect} from './support/civiform_fixtures'
import {
  isLocalDevEnvironment,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  testUserDisplayName,
  validateAccessibility,
  validateScreenshot,
  validateToastMessage,
} from './support'
import {CardSectionName} from './support/applicant_program_list'

test.describe('Applicant auth', () => {
  const endYourSessionText = 'end your session'

  test('Applicant can login', async ({page}) => {
    await loginAsTestUser(page)

    await expect(page.getByRole('banner')).toContainText(
      `Logged in as ${testUserDisplayName()}`,
    )
    await expect(
      page.getByRole('banner').getByRole('button', {name: 'Logout'}),
    ).toBeAttached()
  })

  test('End your session banner is not shown when first viewing index page', async ({
    page,
  }) => {
    await expect(
      page.getByRole('banner').getByRole('button', {name: endYourSessionText}),
    ).not.toBeAttached()
  })

  test('Guest user can end session after starting an application and toast is shown', async ({
    page,
    adminPrograms,
    applicantQuestions,
    seeding,
  }) => {
    await seeding.seedProgramsAndCategories()
    await page.goto('/')
    await loginAsAdmin(page)
    await adminPrograms.publishAllDrafts()
    await logout(page)

    await applicantQuestions.applyProgram('Minimal Sample Program')
    await expect(page.getByTestId('login-button')).toBeAttached()
    await expect(
      page.getByRole('button', {name: endYourSessionText}),
    ).toBeAttached()

    await page.getByRole('button', {name: endYourSessionText}).click()
    expect(await page.title()).toContain('Find programs')

    await validateToastMessage(page, 'Your session has ended.')
  })

  test('Applicant can confirm central provider logout', async ({page}) => {
    test.skip(!isLocalDevEnvironment(), 'Only runs in test environment')
    // so far only fake-oidc provider requires user to click "Yes" to confirm
    // logout. AWS staging uses Auth0 which doesn't. And Seattle staging uses
    // IDCS which at the moment doesn't have central logout enabled.

    await loginAsTestUser(page)
    await expect(page.getByRole('banner')).toContainText(
      `Logged in as ${testUserDisplayName()}`,
    )

    await page.getByRole('button', {name: 'Logout'}).click()

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

    expect(await page.title()).toContain('Find programs')

    // Try login again, ensuring that full login process is followed. If login
    // page doesn't ask for username/password - the method will fail.
    await loginAsTestUser(page)
    await expect(page.getByRole('banner')).toContainText(
      `Logged in as ${testUserDisplayName()}`,
    )
  })

  test('Toast is shown when logged-in user ends their session', async ({
    page,
  }) => {
    await loginAsTestUser(page)
    await logout(page, /* closeToast=*/ false)
    await validateToastMessage(page, 'Your session has ended.')
    await expect(page.locator('.cf-toast')).toHaveClass(
      /(^|\s)flex-align-center(\s|$)/,
    )

    await validateAccessibility(page)
  })

  test('Guest login followed by auth login stores submitted applications', async ({
    page,
    adminPrograms,
    applicantProgramList,
    applicantQuestions,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()

    await logout(page)
    await applicantQuestions.applyProgram(programName)
    await applicantQuestions.submitFromReviewPage()
    await loginAsTestUser(page)

    // Check that program is marked as submitted.
    const applicationCardLocator = page.getByRole('heading', {
      name: programName,
    })
    await expect(applicationCardLocator).toBeAttached()

    await expect(
      applicantProgramList.getSubmittedTagLocator(
        CardSectionName.MyApplications,
        programName,
      ),
    ).toContainText(/\d?\d\/\d?\d\/\d\d/)

    // Logout and login to make sure data is tied to account.
    await logout(page)
    await loginAsTestUser(page)

    await expect(
      applicantProgramList.getSubmittedTagLocator(
        CardSectionName.MyApplications,
        programName,
      ),
    ).toContainText(/\d?\d\/\d?\d\/\d\d/)
  })
})
