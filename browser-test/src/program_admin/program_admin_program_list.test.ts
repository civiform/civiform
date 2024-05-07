import {expect, test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsProgramAdmin,
  logout,
  validateScreenshot,
} from '../support'
import {ProgramVisibility} from '../support/admin_programs'

test.describe('Program admin program list', {tag: ['@uses-fixtures']}, () => {
  test('shows all the programs that are active', async ({
    page,
    adminPrograms,
  }) => {
    await test.step('log in as a CiviForm admin and publish multiple programs', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram('Program Name One')
      await adminPrograms.addProgram('Program Name Two')
      await adminPrograms.addProgram('Program Name Three')
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await test.step('log in as a program admin and view the program list, verify that all active programs are shown', async () => {
      await loginAsProgramAdmin(page)
      await expect(page.getByText('Program Name One')).toHaveCount(1)
      await expect(page.getByText('Program Name Two')).toHaveCount(1)
      await expect(page.getByText('Program Name Three')).toHaveCount(1)
      await validateScreenshot(page, 'program-admin-program-list')
    })
  })

  test('shows all the programs that are active, including the program with disabled visibility', async ({
    page,
    adminPrograms,
  }) => {
    await enableFeatureFlag(page, 'disabled_visibility_condition_enabled')

    await test.step('log in as a CiviForm admin and publish multiple programs', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(
        'Disabled Program Name',
        'Program Description',
        'https://usa.gov',
        ProgramVisibility.DISABLED,
      )
      await adminPrograms.addProgram('Program Name Two')
      await adminPrograms.addProgram('Program Name Three')
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await test.step('log in as a program admin and view the program list, verify that the disabled program is shown', async () => {
      await loginAsProgramAdmin(page)
      await expect(page.getByText('Disabled Program Name')).toHaveCount(1)
      await expect(page.getByText('Program Name Two')).toHaveCount(1)
      await expect(page.getByText('Program Name Three')).toHaveCount(1)

      await validateScreenshot(
        page,
        'program-admin-program-list-visible-disabled-program',
      )
    })
  })
})
