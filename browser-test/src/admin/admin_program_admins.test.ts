import {test} from '../support/civiform_fixtures'
import {loginAsAdmin, validateScreenshot} from '../support'
import {SAMPLE_PROGRAMS} from '../support/seeding'

test.describe('manage program admins', () => {
  test('does not add a program admin that does not exist', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    await seeding.seedProgramsAndCategories()
    await loginAsAdmin(page)

    // Try to add a program admin for an email address that does not have an account.
    await adminPrograms.gotoManageProgramAdminsPage(SAMPLE_PROGRAMS.minimal)
    await page.fill('#admin-email-input', 'test@test.com')
    await page.click('#add-admin-button')

    await adminPrograms.expectManageProgramAdminsPage()
    await adminPrograms.expectAddProgramAdminErrorToast()

    await validateScreenshot(page, 'add-program-admin-error')
  })
})
