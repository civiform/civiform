import {
  createTestContext,
  enableFeatureFlag,
  gotoEndpoint,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  resetContext,
  selectApplicantLanguage,
  validateScreenshot,
  TestContext,
} from './support'

describe('navigating to a deep link', () => {
  const ctx: TestContext = createTestContext()

  const questionText = 'What is your address?'

  beforeEach(async () => {
    const {page, adminQuestions, adminPrograms} = ctx

    await enableFeatureFlag(page, 'bypass_login_language_screens')

    // Arrange
    await loginAsAdmin(page)

    await adminQuestions.addAddressQuestion({
      questionName: 'Test address question',
      questionText,
    })

    const programName = 'Test deep link'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'first description', [
      'Test address question',
    ])

    await adminPrograms.gotoAdminProgramsPage()
    await adminPrograms.expectDraftProgram(programName)
    await adminPrograms.publishAllPrograms()
    await adminPrograms.expectActiveProgram(programName)

    await logout(page)
  })

  it('shows a login prompt for guest users', async () => {
    await resetContext(ctx)
    const {page} = ctx

    await gotoEndpoint(page, '/programs/test-deep-link')
    expect(await page.innerText('html')).toContain(
      'Create an account or sign in',
    )
    await validateScreenshot(
      page,
      'login-prompt-for-guest-users-using-program-slug',
    )
  })

  it('does not show login prompt for logged in users', async () => {
    await resetContext(ctx)
    const {page} = ctx

    await gotoEndpoint(page, '/programs/test-deep-link')
    await loginAsTestUser(page, 'button:has-text("Log in")')
    expect(await page.innerText('html')).not.toContain(
      'Create an account or sign in',
    )
  })

  it('takes guests and logged in users through the flow correctly', async () => {
    await resetContext(ctx)
    const {page} = ctx

    // Exercise guest path
    // Act
    await gotoEndpoint(page, '/programs/test-deep-link')
    await page.click('text="Continue to application"')
    await selectApplicantLanguage(page, 'English')

    // Assert
    await page.click('#continue-application-button')
    expect(await page.innerText('.cf-applicant-question-text')).toContain(
      questionText,
    )

    await logout(page)

    // Exercise test user path
    // Act
    await gotoEndpoint(page, '/programs/test-deep-link')
    await loginAsTestUser(page, 'button:has-text("Log in")')
    await selectApplicantLanguage(page, 'English')

    // Assert
    await page.click('#continue-application-button')
    expect(await page.innerText('.cf-applicant-question-text')).toContain(
      questionText,
    )
  })

  it('Non-logged in user should get redirected to the program page and not an error', async () => {
    await resetContext(ctx)
    const {page, browserContext} = ctx

    await selectApplicantLanguage(page, 'English')

    await logout(page)
    await browserContext.clearCookies()
    await gotoEndpoint(page, '/programs/test-deep-link')
    await page.click('text="Continue to application"')
    await selectApplicantLanguage(page, 'English')

    // Assert
    await page.click('#continue-application-button')
    expect(await page.innerText('.cf-applicant-question-text')).toContain(
      questionText,
    )

    await logout(page)
  })

  it('Logging in to an existing account after opening a deep link in a new browser session', async () => {
    await resetContext(ctx)
    const {page, browserContext} = ctx

    await selectApplicantLanguage(page, 'English')

    // Log in and log out to establish the test user in the database.
    await loginAsTestUser(page)
    await logout(page)
    await browserContext.clearCookies()

    // Go to deep link as a guest
    await gotoEndpoint(page, '/programs/test-deep-link')
    // Log in as the same test user
    await loginAsTestUser(page, 'button:has-text("Log in")')

    await page.click('#continue-application-button')
    expect(await page.innerText('.cf-applicant-question-text')).toContain(
      questionText,
    )

    await logout(page)
  })
})
