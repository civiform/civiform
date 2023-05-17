import {
  createTestContext,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsGuest,
  loginAsTestUser,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from './support'
import {ProgramVisibility} from './support/admin_programs'

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

  it('shows log in button for guest users', async () => {
    const {page, applicantQuestions} = ctx
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')

    await validateAccessibility(page)

    // We cannot check that the login/create account buttons redirect the user to a particular
    // URL because it varies between environments, so just check for their existence.
    expect(await page.textContent('#login-button')).toContain('Log in')
    expect(await page.textContent('#create-account')).toContain(
      'Create account',
    )
    await applicantQuestions.gotoApplicantHomePage()
    await logout(page)
  })

  it('shows login prompt for guest users when they click apply', async () => {
    const {page} = ctx
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')

    await validateAccessibility(page)

    // Click Apply on the primary program. This should show the login prompt modal.
    await page.click(
      `.cf-application-card:has-text("${primaryProgramName}") .cf-apply-button`,
    )
    expect(await page.textContent('html')).toContain(
      'Create an account or sign in',
    )
    await validateScreenshot(page, 'apply-program-login-prompt', {
      fullPage: false,
    })

    // Close the modal and click Apply again. This time, we should not see the login prompt modal.
    await page.click(`.cf-modal .cf-modal-close`)
    await page.click(
      `.cf-application-card:has-text("${primaryProgramName}") .cf-apply-button`,
    )
    expect(await page.textContent('html')).not.toContain(
      'Create an account or sign in',
    )

    // End guest session and start a new one. Login prompt should show this time upon clicking Apply.
    await logout(page)
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')
    await page.click(
      `.cf-application-card:has-text("${primaryProgramName}") .cf-apply-button`,
    )
    expect(await page.textContent('html')).toContain(
      'Create an account or sign in',
    )
  })

  it('categorizes programs for draft and applied applications', async () => {
    const {page, applicantQuestions} = ctx
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')

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
    await selectApplicantLanguage(page, 'English')
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [otherProgramName, primaryProgramName],
      wantInProgressPrograms: [],
      wantSubmittedPrograms: [],
    })
  })

  it('common intake form enabled but not present', async () => {
    const {page} = ctx
    await enableFeatureFlag(page, 'intake_form_enabled')

    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')

    await validateScreenshot(page, 'common-intake-form-not-set')
    await validateAccessibility(page)
  })

  it('shows common intake form when enabled and present', async () => {
    const {page, adminPrograms, applicantQuestions} = ctx
    await enableFeatureFlag(page, 'intake_form_enabled')

    await loginAsAdmin(page)
    const commonIntakeFormProgramName = 'Benefits finder'
    await adminPrograms.addProgram(
      commonIntakeFormProgramName,
      'program description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
      'admin description',
      /* isCommonIntake= */ true,
    )
    await adminPrograms.publishAllPrograms()
    await logout(page)

    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')

    await applicantQuestions.applyProgram(primaryProgramName)
    await applicantQuestions.answerTextQuestion('first answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.gotoApplicantHomePage()

    await validateScreenshot(page, 'common-intake-form-sections')
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [otherProgramName],
      wantInProgressPrograms: [primaryProgramName],
      wantSubmittedPrograms: [],
    })
    await applicantQuestions.expectCommonIntakeForm(commonIntakeFormProgramName)
    await validateAccessibility(page)
  })

  it('shows a different title for the common intake form', async () => {
    const {page, applicantQuestions} = ctx
    await enableFeatureFlag(page, 'intake_form_enabled')

    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')

    await applicantQuestions.clickApplyProgramButton(primaryProgramName)
    expect(await page.innerText('h2')).toContain('Program application summary')

    await applicantQuestions.gotoApplicantHomePage()
    await applicantQuestions.clickApplyProgramButton('Benefits finder')
    expect(await page.innerText('h2')).toContain(
      'Benefits pre-screener summary',
    )
  })

  it('shows previously answered on text for questions that had been answered', async () => {
    const {page, applicantQuestions} = ctx

    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English')

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
