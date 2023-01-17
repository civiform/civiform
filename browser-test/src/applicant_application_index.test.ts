import {
  createTestContext,
  loginAsAdmin,
  loginAsGuest,
  loginAsTestUser,
  logout,
  selectApplicantLanguage,
  validateScreenshot,
} from './support'

describe('applicant program index page', () => {
  const ctx = createTestContext()

  const primaryProgramName = 'Application index primary program'
  const otherProgramName = 'Application index other program'

  beforeEach(async () => {
    const {page, adminPrograms, adminQuestions} = ctx
    await loginAsAdmin(page)

    // Create a program with two questions on separate blocks so that an applicant can partially
    // complete an application.
    await adminPrograms.addProgram(primaryProgramName)
    await adminQuestions.addTextQuestion({questionName: 'first-q'})
    await adminQuestions.addTextQuestion({questionName: 'second-q'})
    await adminPrograms.addProgramBlock(primaryProgramName, 'first block', [
      'first-q',
    ])
    await adminPrograms.addProgramBlock(primaryProgramName, 'second block', [
      'second-q',
    ])

    await adminPrograms.addProgram(otherProgramName)
    await adminPrograms.addProgramBlock(otherProgramName, 'first block', [
      'first-q',
    ])

    await adminPrograms.publishAllPrograms()
    await logout(page)
  })

  it('categorizes programs for draft and applied applications', async () => {
    const {page, applicantQuestions} = ctx
    await loginAsTestUser(page)
    await selectApplicantLanguage(
      page,
      'English',
      /* assertProgramIndexPage= */ true,
    )

    // Navigate to the applicant's program index and validate that both programs appear in the
    // "Not started" section.
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [primaryProgramName, otherProgramName],
      wantInProgressPrograms: [],
      wantSubmittedPrograms: [],
    })

    // Fill out first application block and confirm that the program appears in the "In progress"
    // section.
    await applicantQuestions.applyProgram(primaryProgramName)
    await applicantQuestions.answerTextQuestion('first answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.gotoApplicantHomePage()
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [otherProgramName],
      wantInProgressPrograms: [primaryProgramName],
      wantSubmittedPrograms: [],
    })

    // Finish the application and confirm that the program appears in the "Submitted" section.
    await applicantQuestions.applyProgram(primaryProgramName)
    await applicantQuestions.answerTextQuestion('second answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [otherProgramName],
      wantInProgressPrograms: [],
      wantSubmittedPrograms: [primaryProgramName],
    })

    // Logout, then login as guest and confirm that everything appears unsubmitted (https://github.com/civiform/civiform/pull/3487).
    await logout(page)
    await loginAsGuest(page)
    await selectApplicantLanguage(
      page,
      'English',
      /* assertProgramIndexPage= */ true,
    )
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [otherProgramName, primaryProgramName],
      wantInProgressPrograms: [],
      wantSubmittedPrograms: [],
    })
  })

  it('shows previously answered on text for program that was previously applied to', async () => {
    const {page, applicantQuestions} = ctx

    await loginAsTestUser(page)
    await selectApplicantLanguage(
      page,
      'English',
      /* assertProgramIndexPage= */ true,
    )

    // Fill out application with one question and confirm it was successfully submitted.
    await applicantQuestions.applyProgram(otherProgramName)
    await applicantQuestions.answerTextQuestion('first answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [primaryProgramName],
      wantInProgressPrograms: [],
      wantSubmittedPrograms: [otherProgramName],
    })

    // Check that the question repeated in the program with two questions shows previously answered.
    await applicantQuestions.clickApplyProgramButton(primaryProgramName)
    await validateScreenshot(page, 'question-shows-previously-answered')

    // Fill out second question.
    await applicantQuestions.clickContinue()
    await applicantQuestions.answerTextQuestion('second answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()

    // Check that the original program doesn't show previously answered.
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.clickApplyProgramButton(otherProgramName)
    await validateScreenshot(page, 'question-does-not-show-previously-answered')
    await applicantQuestions.clickSubmit()

    // Change first response on second program.
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.clickApplyProgramButton(primaryProgramName)
    await applicantQuestions.clickEdit()
    await applicantQuestions.answerTextQuestion('first answer 2')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()

    // Check that the first program now shows the previously answered text.
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.clickApplyProgramButton(otherProgramName)
    await validateScreenshot(page, 'first-program-shows-previously-answered')
  })
})
