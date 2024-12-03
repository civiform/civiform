import {expect, test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, logout} from '../support'

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

  test('can view program overview', async ({page}) => {
    await page.goto(`/programs/${programName}`)
    await expect(
      page.getByText('Welcome to the program overview page!'),
    ).toBeVisible()
    expect(page.title()).toBe('test - Program Overview')
  })
})
