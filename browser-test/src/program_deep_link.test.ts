import {test, expect} from './support/civiform_fixtures'
import {
  loginAsAdmin,
  loginAsTestUser,
  logout,
  selectApplicantLanguage,
  validateScreenshot,
} from './support'

test.describe('navigating to a deep link', () => {
  const questionText = 'What is your address?'

  test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
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

  test('shows a login prompt for guest users', async ({page}) => {
    await page.goto('/programs/test-deep-link')
    await expect(page.locator('#modal-container')).toContainText(
      'Create an account or sign in',
    )
    await validateScreenshot(
      page,
      'login-prompt-for-guest-users-using-program-slug',
    )
  })

  test('does not show login prompt for logged in users', async ({page}) => {
    await page.goto('/programs/test-deep-link')
    await loginAsTestUser(page, 'button:has-text("Log in")')
    await expect(page.locator('#modal-container')).not.toContainText(
      'Create an account or sign in',
    )
  })

  test('takes guests and logged in users through the flow correctly', async ({
    page,
    applicantQuestions,
  }) => {
    // Exercise guest path
    // Act
    await page.goto('/programs/test-deep-link')
    await page.getByRole('button', {name: 'Continue to application'}).click()

    // Assert
    await page.getByRole('link', {name: 'Continue'}).click()
    await applicantQuestions.validateQuestionIsOnPage(questionText)

    await logout(page)

    // Exercise test user path
    // Act
    await page.goto('/programs/test-deep-link')
    await loginAsTestUser(page, 'button:has-text("Log in")')

    // Assert
    await page.getByRole('link', {name: 'Continue'}).click()
    await applicantQuestions.validateQuestionIsOnPage(questionText)
  })

  test('Non-logged in user should get redirected to the program page and not an error', async ({
    page,
    applicantQuestions,
  }) => {
    await page.goto('/programs/test-deep-link')
    await page.getByRole('button', {name: 'Continue to application'}).click()

    await selectApplicantLanguage(page, 'English')

    // Assert
    await page.getByRole('link', {name: 'Continue'}).click()
    await applicantQuestions.validateQuestionIsOnPage(questionText)
  })

  test('Logging in to an existing account after opening a deep link in a new browser session', async ({
    page,
    applicantQuestions,
    context,
  }) => {
    await test.step('Log in and log out to establish the test user in the database', async () => {
      await loginAsTestUser(page)
      await logout(page)
      await context.clearCookies()
    })

    // Go to deep link as a guest
    await page.goto('/programs/test-deep-link')
    // Log in as the same test user
    await loginAsTestUser(page, 'button:has-text("Log in")')

    await page.getByRole('link', {name: 'Continue'}).click()
    await applicantQuestions.validateQuestionIsOnPage(questionText)

    await logout(page)
  })

  test('Going to a deep link does not retain redirect in session', async ({
    page,
  }) => {
    // Go to a deep link
    await page.goto('/programs/test-deep-link')
    await page.getByRole('button', {name: 'Continue to application'}).click()

    // Logging out should not take us back to /programs/test-deep-link, but rather
    // to the program index page.
    await logout(page)

    await expect(
      page.getByRole('heading', {
        name: 'Save time applying for programs and services',
      }),
    ).toBeAttached()
  })

  test('redirects the applicant to an error info page if the program is disabled', async ({
    page,
    adminPrograms,
  }) => {
    await test.step(`log in as admin and publish a disabled program`, async () => {
      await loginAsAdmin(page)

      const programName = 'dis1'
      await adminPrograms.addDisabledProgram(programName)

      await adminPrograms.gotoAdminProgramsPage(/* isProgramDisabled*/ true)
      await adminPrograms.expectDraftProgram(programName)
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await test.step(`opens the deep link of the disabled program and gets redirected to an error info page`, async () => {
      await page.goto('/programs/dis1')
      expect(page.url()).toContain('/disabled')
      await validateScreenshot(page, 'disabled-program-error-info-page')
    })

    await test.step(`clicks on visit homepage button and it takes me to home page`, async () => {
      await page.click('#visit-home-page-button')
      expect(page.url()).toContain('/programs')
      await validateScreenshot(page, 'home-page')
    })
  })
})
