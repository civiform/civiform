import {
  createTestContext,
  enableFeatureFlag,
  gotoEndpoint,
  loginAsAdmin,
  loginAsGuest,
  loginAsTestUser,
  logout, resetContext,
  selectApplicantLanguage,
  validateScreenshot,
} from './support'

describe('navigating to a deep link', () => {
  const ctx = createTestContext()

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
    await loginAsGuest(page)
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
    ctx.browser.close()
    const {page} = ctx

    // Exercise guest path
    // Act
    await gotoEndpoint(page, '/programs/test-deep-link')
    await loginAsGuest(page)
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
})
