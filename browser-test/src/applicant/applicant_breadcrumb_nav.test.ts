import {test} from '../support/civiform_fixtures'
import {loginAsAdmin, logout, validateAccessibility} from '../support'
import {SAMPLE_QUESTIONS} from '../support/seeding'

test.describe('Applicant breadcrumb navigation', () => {
  const programName = 'Test program for breadcrumb navigation'
  const programDescription = 'Test description'

  test.beforeEach(async ({page, adminPrograms, seeding}) => {
    await seeding.seedQuestions()
    await loginAsAdmin(page)

    await adminPrograms.addProgram(programName, {
      description: programDescription,
    })
    await adminPrograms.editProgramBlock(programName, 'first description', [
      SAMPLE_QUESTIONS.staticContent,
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
