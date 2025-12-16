import {test} from '../support/civiform_fixtures'
import {loginAsAdmin, logout, validateAccessibility} from '../support'

test.describe('Applicant breadcrumb navigation', () => {
  const programName = 'Test program for breadcrumb navigation'
  const programDescription = 'Test description'
  const staticQuestionText = 'static question text'

  test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
    await loginAsAdmin(page)

    await adminQuestions.addStaticQuestion({
      questionName: 'nav-static-q',
      questionText: staticQuestionText,
    })

    await adminPrograms.addProgram(programName, {
      description: programDescription,
    })
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
    await test.step('Start applying to the program', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantProgramOverview.startApplicationFromProgramOverviewPage(
        programName,
      )
    })
    await test.step('verify accessibility', async () => {
      await validateAccessibility(page)
    })

    await test.step('Select Home breadcrumb and verify navigation', async () => {
      await applicantQuestions.clickBreadcrumbHomeLink()
      await applicantQuestions.expectProgramsPage()
    })
  })

  test('validate breadcrumb program overview navigation', async ({
    page,
    applicantQuestions,
    applicantProgramOverview,
  }) => {
    await test.step('Start applying to the program', async () => {
      await applicantQuestions.clickApplyProgramButton(programName)
      await applicantProgramOverview.startApplicationFromProgramOverviewPage(
        programName,
      )
    })
    await test.step('verify accessibility', async () => {
      await validateAccessibility(page)
    })

    await test.step('Select program breadcrumb and verify navigation', async () => {
      await applicantQuestions.clickBreadcrumbProgramLink(programName)

      await applicantProgramOverview.expectProgramOverviewPage(programName)
    })
  })
})
