import {
  createTestContext,
  enableFeatureFlag,
  gotoEndpoint,
  loginAsAdmin,
  loginAsGuest,
  loginAsTestUser,
  logout,
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
    const {page} = ctx

    await gotoEndpoint(page, '/programs/test-deep-link')
    await loginAsGuest(page)
    expect(await page.innerText('html')).toContain('Continue to application')
    await validateScreenshot(
      page,
      'login-prompt-for-guest-users-using-program-slug',
    )
  })

  it('does not show login prompt for logged in users', async () => {
    const {page} = ctx

    await gotoEndpoint(page, '/programs/test-deep-link')
    await loginAsTestUser(page, 'button:has-text("Log in")')
    expect(await page.innerText('html')).not.toContain(
      'Continue to application',
    )
  })

  it('as a guest user or registered user', async () => {
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
