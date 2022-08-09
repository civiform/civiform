import { AdminPrograms, AdminQuestions, ApplicantQuestions, endSession, loginAsAdmin, loginAsProgramAdmin, loginAsTestUser, logout, selectApplicantLanguage, startSession, userDisplayName, validateScreenshot, } from './support'

describe('view an application in an older version', () => {
  it('create an application, and create a new version of the program, and view the application in the old version of the program', async () => {
    const { browser, page } = await startSession()
    page.setDefaultTimeout(5000)

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)

    // Create a program with one question
    const questionName = 'text-to-be-obsolete-q'
    await adminQuestions.addTextQuestion({ questionName })
    const programName = 'program with previous applications'
    await adminPrograms.addAndPublishProgramWithQuestions(
      [questionName],
      programName
    )

    await logout(page)
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')
    const applicantQuestions = new ApplicantQuestions(page)
    await applicantQuestions.validateHeader('en-US')

    // Submit an application to the program
    await applicantQuestions.applyProgram(programName)
    await applicantQuestions.answerTextQuestion('some text')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage(programName)

    await logout(page)
    await loginAsProgramAdmin(page)

    // See the application in admin page
    await adminPrograms.viewApplications(programName)
    await adminPrograms.viewApplicationForApplicant(userDisplayName())
    await adminPrograms.expectApplicationAnswers(
      'Screen 1',
      questionName,
      'some text'
    )

    await logout(page)
    await loginAsAdmin(page)

    // Create a new version of the question and program
    await adminQuestions.createNewVersion(questionName)
    await adminPrograms.publishProgram(programName)

    await logout(page)
    await loginAsProgramAdmin(page)

    // See the application in admin page in the old version
    await adminPrograms.viewApplications(programName)
    await adminPrograms.viewApplicationForApplicant(userDisplayName())
    await adminPrograms.expectApplicationAnswers(
      'Screen 1',
      questionName,
      'some text'
    )
    await validateScreenshot(page)

    await endSession(browser)
  })
})
