import {test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
} from '../support'

test.describe('Applicant breadcrumb navigation', {tag: ['@northstar']}, () => {
  const programName = 'Test program for breadcrumb navigation'
  const programDescription = 'Test description'
  const staticQuestionText = 'static question text'

  test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await loginAsAdmin(page)

    await adminQuestions.addStaticQuestion({
      questionName: 'nav-static-q',
      questionText: staticQuestionText,
    })

    await adminPrograms.addProgram(programName, programDescription)
    await adminPrograms.editProgramBlock(programName, 'first description', [
      'nav-static-q',
    ])

    await adminPrograms.gotoAdminProgramsPage()
    await adminPrograms.publishProgram(programName)
    await logout(page)
  })

  test('validate breadcrumb Home navigation', async ({
    page,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    // Setup
    await applicantQuestions.clickApplyProgramButton(programName)
    await applicantProgramOverview.startApplicationFromProgramOverviewPage(
      programName,
    )

    // Test
    await applicantQuestions.clickBreadcrumbHomeLink()

    // Verify
    await applicantQuestions.expectProgramsPage()
    await validateAccessibility(page)
  })

  test('validate breadcrumb program overview navigation', async ({
    page,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    // Setup
    await applicantQuestions.clickApplyProgramButton(programName)
    await applicantProgramOverview.startApplicationFromProgramOverviewPage(
      programName,
    )

    // Test
    await applicantQuestions.clickBreadcrumbProgramLink(programName)

    // Verify
    await applicantProgramOverview.expectProgramOverviewPage(programName)
    await validateAccessibility(page)
  })
})
