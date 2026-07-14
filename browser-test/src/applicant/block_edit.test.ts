import {test} from '../support/civiform_fixtures'
import {
  loginAsAdmin,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from '../support'
import {SAMPLE_QUESTIONS} from '../support/seeding'

test.describe('Applicant block edit', () => {
  const programName = 'Test program for block edit page'
  const programDescription = 'Test description'

  test.beforeEach(async ({page, adminPrograms, seeding}) => {
    await seeding.seedQuestions()
    await loginAsAdmin(page)

    await adminPrograms.addProgram(programName, {
      description: programDescription,
    })
    await adminPrograms.editProgramBlock(programName, 'first description', [
      SAMPLE_QUESTIONS.date,
      SAMPLE_QUESTIONS.email,
    ])
    await adminPrograms.addProgramBlockUsingSpec(programName, {
      description: 'second description',
      questions: [{name: SAMPLE_QUESTIONS.staticContent, isOptional: false}],
    })
    await adminPrograms.addProgramBlockUsingSpec(programName, {
      description: 'third description',
      questions: [{name: SAMPLE_QUESTIONS.address, isOptional: false}],
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
