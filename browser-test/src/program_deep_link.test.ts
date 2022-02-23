import {
  gotoEndpoint,
  startSession,
  loginAsAdmin,
  AdminQuestions,
  AdminPrograms,
  endSession,
  logout,
  selectApplicantLanguage,
  loginAsGuest,
  loginAsTestUser,
  waitForPageJsLoad,
} from './support'

describe('navigating to a deep link', () => {
  it('as a guest user or registered user', async () => {
    const { browser, page } = await startSession()

    // Arrange
    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)

    const questionText = 'What is your address?'

    await adminQuestions.addAddressQuestion({
      questionName: 'Test address question',
      questionText,
    })

    const programName = 'Test Deep Link'
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
    expect(await page.innerText('.cf-applicant-question-text')).toEqual(
      questionText
    )

    await logout(page)

    // Exercise test user path
    // Act
    await gotoEndpoint(page, '/programs/test-deep-link')
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')

    // Assert
    await page.click('#continue-application-button')
    expect(await page.innerText('.cf-applicant-question-text')).toEqual(
      questionText
    )

    await endSession(browser)
  })
})
