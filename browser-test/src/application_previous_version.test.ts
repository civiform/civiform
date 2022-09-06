import {
  ApplicantQuestions,
  createTestContext,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  selectApplicantLanguage,
  userDisplayName,
} from './support'

describe('view an application in an older version', () => {
  const ctx = createTestContext()

  it('create an application, and create a new version of the program, and view the application in the old version of the program', async () => {
    const {page, adminQuestions, adminPrograms} = ctx
    page.setDefaultTimeout(5000)

    await loginAsAdmin(page)

    // Create a program with one question
    const questionName = 'text-to-be-obsolete-q'
    await adminQuestions.addTextQuestion({questionName})
    const programName = 'program with previous applications'
    await adminPrograms.addAndPublishProgramWithQuestions(
      [questionName],
      programName,
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
    await applicantQuestions.submitFromReviewPage()

    await logout(page)
    await loginAsProgramAdmin(page)

    // See the application in admin page
    await adminPrograms.viewApplications(programName)
    await adminPrograms.viewApplicationForApplicant(userDisplayName())
    await adminPrograms.expectApplicationAnswers(
      'Screen 1',
      questionName,
      'some text',
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
      'some text',
    )
  })
})
