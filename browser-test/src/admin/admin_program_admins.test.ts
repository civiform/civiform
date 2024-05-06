import {test} from '../support/civiform_fixtures'
import {loginAsAdmin, validateScreenshot} from '../support'

test.describe('manage program admins', {tag: ['@uses-fixtures']}, () => {
  test('does not add a program admin that does not exist', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Add program admins'
    await adminPrograms.addProgram(programName)

    // Try to add a program admin for an email address that does not have an account.
    await adminPrograms.gotoManageProgramAdminsPage(programName)
    await page.fill('#admin-email-input', 'test@test.com')
    await page.click('#add-admin-button')

    await adminPrograms.expectManageProgramAdminsPage()
    await adminPrograms.expectAddProgramAdminErrorToast()

    await validateScreenshot(page, 'add-program-admin-error')
  })
})
