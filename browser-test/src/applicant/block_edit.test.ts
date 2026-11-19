import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from '../support'

test.describe('Applicant block edit', () => {
  const programName = 'Test program for block edit page'
  const programDescription = 'Test description'
  const dateQuestionText = 'date question text'
  const emailQuestionText = 'email question text'
  const staticQuestionText = 'static question text'
  const addressQuestionText = 'address question text'
  const radioQuestionText = 'radio question text'

  test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
    await loginAsAdmin(page)

    await adminQuestions.addDateQuestion({
      questionName: 'nav-date-q',
      questionText: dateQuestionText,
    })
    await adminQuestions.addEmailQuestion({
      questionName: 'nav-email-q',
      questionText: emailQuestionText,
    })
    await adminQuestions.addAddressQuestion({
      questionName: 'nav-address-q',
      questionText: addressQuestionText,
    })
    await adminQuestions.addRadioButtonQuestion({
      questionName: 'nav-radio-q',
      questionText: radioQuestionText,
      options: [
        {adminName: 'one_admin', text: 'one'},
        {adminName: 'two_admin', text: 'two'},
        {adminName: 'three_admin', text: 'three'},
      ],
    })
    await adminQuestions.addStaticQuestion({
      questionName: 'nav-static-q',
      questionText: staticQuestionText,
    })

    await adminPrograms.addProgram(programName, {
      description: programDescription,
    })
    await adminPrograms.editProgramBlock(programName, 'first description', [
      'nav-date-q',
      'nav-email-q',
    ])
    await adminPrograms.addProgramBlockUsingSpec(programName, {
      description: 'second description',
      questions: [{name: 'nav-static-q', isOptional: false}],
    })
    await adminPrograms.addProgramBlockUsingSpec(programName, {
      description: 'third description',
      questions: [{name: 'nav-address-q', isOptional: false}],
    })

    await adminPrograms.gotoAdminProgramsPage()
    await adminPrograms.publishProgram(programName)
    await logout(page)
  })

  test('validate block edit page title', async ({
    page,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    await applicantQuestions.clickApplyProgramButton(programName)
    await applicantProgramOverview.startApplicationFromProgramOverviewPage(
      programName,
    )

    await applicantQuestions.expectTitle(
      page,
      'Test program for block edit page — 1 of 4',
    )

    await validateAccessibility(page)
  })

  test('applies color theming on block edit page', async ({
    page,
    adminSettings,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    await loginAsAdmin(page)
    await adminSettings.gotoAdminSettings()

    await adminSettings.setStringSetting('THEME_COLOR_PRIMARY', '#6d4bfa')
    await adminSettings.setStringSetting('THEME_COLOR_PRIMARY_DARK', '#a72f10')

    await adminSettings.saveChanges()
    await logout(page)

    await applicantQuestions.clickApplyProgramButton(programName)
    await applicantProgramOverview.startApplicationFromProgramOverviewPage(
      programName,
    )

    await validateScreenshot(page, 'block-edit-page-theme')
  })

  test('switches language after a validation error with program slug URLs', async ({
    page,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    await enableFeatureFlag(page, 'program_slug_urls_enabled')
    await applicantQuestions.clickApplyProgramButton(programName)
    await applicantProgramOverview.startApplicationFromProgramOverviewPage(
      programName,
    )

    await test.step('Submit the block with required questions unanswered', async () => {
      await applicantQuestions.clickContinue()
      await expect(
        page.getByText('This question is required').first(),
      ).toBeVisible()
    })

    await test.step('Switch language from the error page', async () => {
      await selectApplicantLanguage(page, 'es-US')
      await expect(page).toHaveURL(
        /\/programs\/test-program-for-block-edit-page\/blocks\/1\/edit/,
      )
      await expect(page.getByText(dateQuestionText)).toBeVisible()
    })
  })

  test('renders right to left on block edit page', async ({
    page,
    applicantQuestions,
  }) => {
    await applicantQuestions.clickApplyProgramButton(programName)
    await selectApplicantLanguage(page, 'ar')

    await page.getByRole('link', {name: 'بدء الطلب'}).first().click()
    // Dismiss toast saying the program's not fully translated.
    await page.locator('#toast-container').getByText('x').click()

    await validateScreenshot(page, 'block-edit-page-right-to-left', {
      fullPage: false,
      mobileScreenshot: true,
    })
  })
})
