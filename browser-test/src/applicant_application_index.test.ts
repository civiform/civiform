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
  const ctx = createTestContext(/* clearDb= */ false)

  const primaryProgramName = 'Application index primary program'
  const otherProgramName = 'Application index other program'

  const firstQuestionText = 'This is the first question'
  const secondQuestionText = 'This is the second question'

  beforeAll(async () => {
    const {page, adminPrograms, adminQuestions} = ctx
    await loginAsAdmin(page)

    // Create a program with two questions on separate blocks so that an applicant can partially
    // complete an application.
    await adminPrograms.addProgram(primaryProgramName)
    await adminQuestions.addTextQuestion({
      questionName: 'first-q',
      questionText: firstQuestionText,
    })
    await adminQuestions.addTextQuestion({
      questionName: 'second-q',
      questionText: secondQuestionText,
    })
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

  it('shows previously answered on text for questions that had been answered', async () => {
    const {page, applicantQuestions} = ctx

    await loginAsGuest(page)
    await selectApplicantLanguage(
      page,
      'English',
      /* assertProgramIndexPage= */ true,
    )

    // Fill out application with one question and confirm it shows previously answered at the end.
    await applicantQuestions.applyProgram(otherProgramName)
    await applicantQuestions.answerTextQuestion('first answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.validatePreviouslyAnsweredText(firstQuestionText)
    await applicantQuestions.submitFromReviewPage()
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [primaryProgramName],
      wantInProgressPrograms: [],
      wantSubmittedPrograms: [otherProgramName],
    })

    // Check that the question repeated in the program with two questions shows previously answered.
    await applicantQuestions.clickApplyProgramButton(primaryProgramName)
    await applicantQuestions.validatePreviouslyAnsweredText(firstQuestionText)
    await applicantQuestions.validateNoPreviouslyAnsweredText(
      secondQuestionText,
    )
    await validateScreenshot(page, 'question-shows-previously-answered')

    // Fill out second question.
    await applicantQuestions.clickContinue()
    await applicantQuestions.answerTextQuestion('second answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()

    // Check that the original program shows previously answered.
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.clickApplyProgramButton(otherProgramName)
    await applicantQuestions.validatePreviouslyAnsweredText(firstQuestionText)
    await applicantQuestions.clickSubmit()

    // Change first response on second program.
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.clickApplyProgramButton(primaryProgramName)
    await applicantQuestions.clickEdit()
    await applicantQuestions.answerTextQuestion('first answer 2')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()

    // Check that the other program shows the previously answered text too.
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.clickApplyProgramButton(otherProgramName)
    await applicantQuestions.validatePreviouslyAnsweredText(firstQuestionText)
    await validateScreenshot(page, 'other-program-shows-previously-answered')
  })
})
