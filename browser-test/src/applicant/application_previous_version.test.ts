import {test} from '../fixtures/custom_fixture'
import {
  ApplicantQuestions,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  testUserDisplayName,
} from '../support'

test.describe('view an application in an older version', {tag: ['@migrated']}, () => {
  test('create an application, and create a new version of the program, and view the application in the old version of the program', async ({page, adminQuestions, adminPrograms} ) => {
    await loginAsAdmin(page)

    // Create a program with one question
    const questionName = 'text-to-be-obsolete-q'
    await adminQuestions.addTextQuestion({questionName})
    const programName = 'Program with previous applications'
    await adminPrograms.addAndPublishProgramWithQuestions(
      [questionName],
      programName,
    )

    await logout(page)
    await loginAsTestUser(page)
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
    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
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
    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    await adminPrograms.expectApplicationAnswers(
      'Screen 1',
      questionName,
      'some text',
    )
  })
})
