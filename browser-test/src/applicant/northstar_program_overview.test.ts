import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  loginAsTrustedIntermediary,
  ClientInformation,
} from '../support'

test.describe('Applicant program overview', {tag: ['@northstar']}, () => {
  const programName = 'test'

  test.beforeEach(async ({page, adminPrograms}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await test.step('create a new program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(programName)
      await logout(page)
    })
  })

  test('can view program overview', async ({page, programOverview}) => {
    await page.goto(`/programs/${programName}`)

    await programOverview.expectProgramOverviewPage()
    expect(await page.title()).toBe('test - Program Overview')
  })

  test('redirects to disabled program info page when program is disabled', async ({
    page,
    adminPrograms,
  }) => {
    await enableFeatureFlag(page, 'disabled_visibility_condition_enabled')
    const disabledProgramName = 'dis'

    await test.step('create a new disabled program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addDisabledProgram(disabledProgramName)
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await page.goto(`/programs/${disabledProgramName}`)

    await expect(
      page.getByRole('heading', {
        name: 'This program is no longer available',
      }),
    ).toBeVisible()
  })

  test('trusted intermediary can view program overview with applicant id in the URL', async ({
    page,
    tiDashboard,
    programOverview,
  }) => {
    await loginAsTrustedIntermediary(page)
    const client: ClientInformation = {
      emailAddress: 'test@sample.com',
      firstName: 'first',
      middleName: 'middle',
      lastName: 'last',
      dobDate: '2021-06-10',
    }
    await tiDashboard.createClient(client)
    await tiDashboard.expectDashboardContainClient(client)
    await tiDashboard.clickOnViewApplications()

    const url = page.url()
    expect(url).toContain('/applicants/')

    await page.goto(`${url}/${programName}`)

    await programOverview.expectProgramOverviewPage()
  })
})
