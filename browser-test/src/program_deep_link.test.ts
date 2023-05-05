import {
  createTestContext,
  gotoEndpoint,
  loginAsAdmin,
  loginAsGuest,
  loginAsTestUser,
  logout,
  selectApplicantLanguage,
} from './support'

describe('navigating to a deep link', () => {
  const ctx = createTestContext()

  it('as a guest user or registered user', async () => {
    const {page, adminQuestions, adminPrograms} = ctx

    // Arrange
    await loginAsAdmin(page)

    const questionText = 'What is your address?'

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

    // Exercise guest path
    // Act
    await gotoEndpoint(page, '/programs/test-deep-link')
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')

    // Assert
    await page.click('#continue-application-button')
    expect(await page.innerText('.cf-applicant-question-text')).toContain(
      questionText,
    )

    await logout(page)

    // Exercise test user path
    // Act

    // TODO(#4705): make goToEndpoint execute before loginAsTestUser as it should,
    // such that user can go to the slug, log in, and be taken back to the slug. Currently
    // with BYPASS_LOGIN_LANGUAGE_SCREENS on, logging in always takes you back to the programs
    // index page,and this behavior will take some work to override in a feasible way.
    await loginAsTestUser(page)
    await gotoEndpoint(page, '/programs/test-deep-link')
    await selectApplicantLanguage(page, 'English')

    // Assert
    await page.click('#continue-application-button')
    expect(await page.innerText('.cf-applicant-question-text')).toContain(
      questionText,
    )
  })
})
