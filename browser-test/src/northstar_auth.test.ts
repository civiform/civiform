import {test, expect} from './support/civiform_fixtures'
import {
  AuthStrategy,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  seedProgramsAndCategories,
  testUserDisplayName,
  validateAccessibility,
  validateScreenshot,
  validateToastMessage,
} from './support'
import {TEST_USER_AUTH_STRATEGY} from './support/config'
import {CardSectionName} from './support/applicant_program_list'

test.describe('Applicant auth', {tag: ['@northstar']}, () => {
  const endYourSessionText = 'end your session'
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test('Applicant can login', async ({page}) => {
    await loginAsTestUser(page)

    await expect(page.getByRole('banner')).toContainText(
      `Logged in as ${testUserDisplayName()}`,
    )
    await expect(
      page.getByRole('banner').getByRole('link', {name: 'Logout'}),
    ).toBeAttached()
  })

  test('End your session banner is not shown when first viewing index page', async ({
    page,
  }) => {
    await expect(
      page.getByRole('banner').getByRole('link', {name: endYourSessionText}),
    ).not.toBeAttached()
  })

  test('Guest user can end session after starting an application and toast is shown', async ({
    page,
    adminPrograms,
    applicantQuestions,
  }) => {
    await seedProgramsAndCategories(page)
    await page.goto('/')
    await loginAsAdmin(page)
    await adminPrograms.publishAllDrafts()
    await logout(page)

    await applicantQuestions.applyProgram(
      'Minimal Sample Program',
      /* northStarEnabled= */ true,
    )
    await expect(page.getByTestId('login-button')).toBeAttached()
    await expect(
      page.getByRole('link', {name: endYourSessionText}),
    ).toBeAttached()

    await page.getByRole('link', {name: endYourSessionText}).click()
    expect(await page.title()).toContain('Find programs')

    await validateToastMessage(page, 'Your session has ended.')
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
    await applicantQuestions.applyProgram(
      programName,
      /* northStarEnabled= */ true,
    )
    await applicantQuestions.submitFromReviewPage(/* northStarEnabled= */ true)
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
