import {
  createTestContext,
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  testUserDisplayName,
  validateAccessibility,
  validateScreenshot,
} from './support'
import {Page} from 'playwright'
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

    await adminPrograms.publishAllDrafts()
    await logout(page)
  })

  it('shows log in button for guest users', async () => {
    const {page, applicantQuestions} = ctx
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
    await validateAccessibility(page)

    // Click Apply on the primary program. This should show the login prompt modal.
    await page.click(
      `.cf-application-card:has-text("${primaryProgramName}") .cf-apply-button`,
    )
    expect(await page.textContent('html')).toContain(
      'Create an account or sign in',
    )
    await validateScreenshot(
      page,
      'apply-program-login-prompt',
      /* fullPage= */ false,
    )

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
    await applicantQuestions.expectPrograms({
      wantNotStartedPrograms: [otherProgramName, primaryProgramName],
      wantInProgressPrograms: [],
      wantSubmittedPrograms: [],
    })
  })

  it('common intake form enabled but not present', async () => {
    const {page} = ctx
    await enableFeatureFlag(page, 'intake_form_enabled')

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
    await adminPrograms.publishAllDrafts()
    await logout(page)

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

describe('applicant program index page with images', () => {
  const ctx = createTestContext()

  it('shows program with wide image', async () => {
    const {page, adminPrograms, adminProgramImage} = ctx
    const programName = 'Wide Image Program'
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()
    await logout(page)

    await validateScreenshot(page, 'program-image-wide')
    await validateAccessibility(page)
  })

  it('shows program with tall image', async () => {
    const {page, adminPrograms, adminProgramImage} = ctx
    const programName = 'Tall Image Program'
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-tall.png',
    )
    await adminPrograms.publishAllDrafts()
    await logout(page)

    await validateScreenshot(page, 'program-image-tall')
  })

  it('no program image if flag off', async () => {
    const {page, adminPrograms, adminProgramImage} = ctx
    const programName = 'Image Flag Off Program'
    await loginAsAdmin(page)
    // Enable the flag and set an image as the admin
    await enableFeatureFlag(page, 'program_card_images')
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-tall.png',
    )
    await adminPrograms.publishAllDrafts()
    // Then disable the flag before logging out
    await disableFeatureFlag(page, 'program_card_images')
    await logout(page)

    // Verify the user doesn't see the image
    await validateScreenshot(page, 'program-image-flag-off')
  })

  it('shows program with image and status', async () => {
    const {page, adminPrograms, adminProgramStatuses, adminProgramImage} = ctx
    const programName = 'Image And Status Program'
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )

    const approvedStatusName = 'Approved'
    await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
    await adminProgramStatuses.createStatus(approvedStatusName)
    await adminPrograms.publishProgram(programName)
    await adminPrograms.expectActiveProgram(programName)
    await logout(page)

    await submitApplicationAndApplyStatus(page, programName, approvedStatusName)

    // Verify program card shows both the Accepted status and image
    await loginAsTestUser(page)
    await validateScreenshot(page, 'program-image-with-status')
  })

  // This test puts programs with different specs in the different sections of the homepage
  // to verify that different card formats appear correctly next to each other and across sections.
  it('shows programs with and without images in all sections', async () => {
    const {
      page,
      adminPrograms,
      adminProgramStatuses,
      adminProgramImage,
      adminQuestions,
      applicantQuestions,
    } = ctx
    await enableFeatureFlag(page, 'intake_form_enabled')
    await enableFeatureFlag(page, 'program_card_images')

    // Common Intake: Basic (no image or status)
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

    // In Progress: Image
    const programNameInProgressImage = 'In Progress Program [Image]'
    await adminPrograms.addProgram(programNameInProgressImage)
    await adminQuestions.addTextQuestion({
      questionName: 'first-q',
      questionText: 'first question text',
    })
    await adminPrograms.addProgramBlock(
      programNameInProgressImage,
      'first block',
      ['first-q'],
    )

    await adminPrograms.goToProgramImagePage(programNameInProgressImage)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()
    await logout(page)

    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programNameInProgressImage)
    await applicantQuestions.answerTextQuestion('first answer')
    await applicantQuestions.clickNext()
    await applicantQuestions.gotoApplicantHomePage()
    await logout(page)

    // Submitted #1: Image and status
    const programNameSubmittedWithImageAndStatus =
      'Submitted Program [Image and Status]'
    const approvedStatusName = 'Approved'
    await loginAsAdmin(page)
    await adminPrograms.addProgram(programNameSubmittedWithImageAndStatus)
    await adminPrograms.goToProgramImagePage(
      programNameSubmittedWithImageAndStatus,
    )
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.gotoDraftProgramManageStatusesPage(
      programNameSubmittedWithImageAndStatus,
    )
    await adminProgramStatuses.createStatus(approvedStatusName)
    await adminPrograms.publishProgram(programNameSubmittedWithImageAndStatus)
    await adminPrograms.expectActiveProgram(
      programNameSubmittedWithImageAndStatus,
    )
    await logout(page)

    await submitApplicationAndApplyStatus(
      page,
      programNameSubmittedWithImageAndStatus,
      approvedStatusName,
    )

    // Submitted #2: Basic
    const programNameSubmittedBasic = 'Submitted Program [Basic]'
    await loginAsAdmin(page)
    await adminPrograms.addProgram(programNameSubmittedBasic)
    await adminPrograms.publishProgram(programNameSubmittedBasic)
    await adminPrograms.expectActiveProgram(programNameSubmittedBasic)
    await logout(page)

    await loginAsTestUser(page)
    await applicantQuestions.clickApplyProgramButton(programNameSubmittedBasic)
    await applicantQuestions.submitFromReviewPage()
    await logout(page)

    // Submitted #3: Status
    const programNameSubmittedWithStatus = 'Submitted Program [Status]'
    await loginAsAdmin(page)

    await adminPrograms.addProgram(programNameSubmittedWithStatus)
    await adminPrograms.gotoDraftProgramManageStatusesPage(
      programNameSubmittedWithStatus,
    )
    await adminProgramStatuses.createStatus(approvedStatusName)
    await adminPrograms.publishProgram(programNameSubmittedWithStatus)
    await adminPrograms.expectActiveProgram(programNameSubmittedWithStatus)
    await logout(page)

    await submitApplicationAndApplyStatus(
      page,
      programNameSubmittedWithStatus,
      approvedStatusName,
    )

    // Submitted #4 (on new row): Image
    const programNameSubmittedImage = 'Submitted Program [Image]'
    await loginAsAdmin(page)

    await adminPrograms.addProgram(programNameSubmittedImage)
    await adminPrograms.goToProgramImagePage(programNameSubmittedImage)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()
    await logout(page)

    await loginAsTestUser(page)
    await applicantQuestions.clickApplyProgramButton(programNameSubmittedImage)
    await applicantQuestions.submitFromReviewPage()
    await logout(page)

    // Not Started #1: Basic
    await loginAsAdmin(page)
    await adminPrograms.addProgram('Not Started Program [Basic]')

    // Not Started #2: Image
    const programNameNotStartedImage = 'Not Started Program [Image]'
    await adminPrograms.addProgram(programNameNotStartedImage)
    await adminPrograms.goToProgramImagePage(programNameNotStartedImage)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()
    await logout(page)

    // Verify homepage
    await loginAsTestUser(page)
    await validateScreenshot(page, 'program-image-all-types')
    await validateAccessibility(page)
  })

  // TODO(#5676): Test with a very small image.

  async function submitApplicationAndApplyStatus(
    page: Page,
    programName: string,
    statusName: string,
  ) {
    const {adminPrograms, applicantQuestions} = ctx
    // Submit an application as a test user.
    await loginAsTestUser(page)
    await applicantQuestions.clickApplyProgramButton(programName)
    await applicantQuestions.submitFromReviewPage()
    await logout(page)

    // Set a status as a program admin
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)
    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    const modal = await adminPrograms.setStatusOptionAndAwaitModal(statusName)
    await adminPrograms.confirmStatusUpdateModal(modal)
    await logout(page)
  }
})
