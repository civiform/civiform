import {
  createTestContext,
  loginAsAdmin,
  loginAsGuest,
  loginAsTestUser,
  logout,
  selectApplicantLanguage,
  validateScreenshot,
} from './support'

describe('applicant application previously answered', () => {
  const ctx = createTestContext()

  const primaryProgramName = 'Application primary program'
  const otherProgramName = 'Application other program'

  beforeAll(async () => {
    const {page, adminPrograms, adminQuestions} = ctx
    await loginAsAdmin(page)

    // Create a program with one question.
    await adminPrograms.addProgram(primaryProgramName)
    await adminQuestions.addTextQuestion({questionName: 'first-q'})
    await adminPrograms.addProgramBlock(primaryProgramName, 'first block', [
      'first-q',
    ])

    // Create a second program that repeats one question and contains another.
    await adminPrograms.addProgram(otherProgramName)
    await adminQuestions.addTextQuestion({questionName: 'second-q'})
    await adminPrograms.addProgramBlock(otherProgramName, 'first block', [
      'first-q',
    ])
    await adminPrograms.addProgramBlock(otherProgramName, 'second block', [
      'second-q',
    ])

    await adminPrograms.publishAllPrograms()
    await logout(page)
  })

  it('shows previously answered on text for program that was previously applied to', async () => {
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

    // Fill out first application and confirm it was successfully submitted.
    await applicantQuestions.applyProgram(primaryProgramName)
    await applicantQuestions.answerTextQuestion('first answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [otherProgramName],
      wantInProgressPrograms: [],
      wantSubmittedPrograms: [primaryProgramName],
    })

    // Check that the question repeated in the second program shows previously answered.
    await applicantQuestions.clickApplyProgramButton(otherProgramName)
    await validateScreenshot(page, 'question-shows-previously-answered')

    // Complete second program.
    await applicantQuestions.clickContinue()
    await applicantQuestions.answerTextQuestion('second answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()

    // Check that the first program doesn't show previously answered.
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.clickApplyProgramButton(primaryProgramName)
    await validateScreenshot(page, 'question-does-not-show-previously-answered')
    await applicantQuestions.clickSubmit()

    // Change first response on second program
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.clickApplyProgramButton(otherProgramName)
    await applicantQuestions.clickEdit()
    await applicantQuestions.answerTextQuestion('first answer 2')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()

    // Check that the first program now shows the previously answered text.
    await applicantQuestions.returnToProgramsFromSubmissionPage()
    await applicantQuestions.clickApplyProgramButton(primaryProgramName)
    await validateScreenshot(page, 'first-program-shows-previously-answered')

  })
})
