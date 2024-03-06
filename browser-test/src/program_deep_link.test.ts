import {test, expect} from '@playwright/test'
import {
  createTestContext,
  gotoEndpoint,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  resetContext,
  selectApplicantLanguage,
  validateScreenshot,
  TestContext,
} from './support'

test.describe('navigating to a deep link', () => {
  const ctx: TestContext = createTestContext()

  const questionText = 'What is your address?'

  test.beforeEach(async () => {
    const {page, adminQuestions, adminPrograms} = ctx

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
    await adminPrograms.publishAllDrafts()
    await adminPrograms.expectActiveProgram(programName)

    await logout(page)
  })

  test('shows a login prompt for guest users', async () => {
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

  test('does not show login prompt for logged in users', async () => {
    await resetContext(ctx)
    const {page} = ctx

    await gotoEndpoint(page, '/programs/test-deep-link')
    await loginAsTestUser(page, 'button:has-text("Log in")')
    expect(await page.innerText('html')).not.toContain(
      'Create an account or sign in',
    )
  })

  test('takes guests and logged in users through the flow correctly', async () => {
    await resetContext(ctx)
    const {page, applicantQuestions} = ctx

    // Exercise guest path
    // Act
    await gotoEndpoint(page, '/programs/test-deep-link')
    await page.click('text="Continue to application"')
    // Assert
    await page.click('#continue-application-button')
    await applicantQuestions.validateQuestionIsOnPage(questionText)

    await logout(page)

    // Exercise test user path
    // Act
    await gotoEndpoint(page, '/programs/test-deep-link')
    await loginAsTestUser(page, 'button:has-text("Log in")')
    // Assert
    await page.click('#continue-application-button')
    await applicantQuestions.validateQuestionIsOnPage(questionText)
  })

  test('Non-logged in user should get redirected to the program page and not an error', async () => {
    await resetContext(ctx)
    const {page, applicantQuestions, browserContext} = ctx

    await logout(page)
    await browserContext.clearCookies()
    await gotoEndpoint(page, '/programs/test-deep-link')
    await page.click('text="Continue to application"')
    await selectApplicantLanguage(page, 'English')

    // Assert
    await page.click('#continue-application-button')
    await applicantQuestions.validateQuestionIsOnPage(questionText)

    await logout(page)
  })

  test('Logging in to an existing account after opening a deep link in a new browser session', async () => {
    await resetContext(ctx)
    const {page, applicantQuestions, browserContext} = ctx

    // Log in and log out to establish the test user in the database.
    await loginAsTestUser(page)
    await logout(page)
    await browserContext.clearCookies()

    // Go to deep link as a guest
    await gotoEndpoint(page, '/programs/test-deep-link')
    // Log in as the same test user
    await loginAsTestUser(page, 'button:has-text("Log in")')

    await page.click('#continue-application-button')
    await applicantQuestions.validateQuestionIsOnPage(questionText)

    await logout(page)
  })

  test('Going to a deep link does not retain redirect in session', async () => {
    await resetContext(ctx)
    const {page, browserContext} = ctx
    await browserContext.clearCookies()

    // Go to a deep link
    await gotoEndpoint(page, '/programs/test-deep-link')
    await page.click('text="Continue to application"')

    // Logging out should not take us back to /programs/test-deep-link, but rather
    // to the program index page.
    await logout(page)
    expect(await page.innerText('h1')).toContain(
      'Save time applying for programs and services',
    )
  })
})
