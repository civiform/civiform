import {createTestContext, loginAsAdmin, validateScreenshot} from './support'

describe('manage program admins', () => {
  const ctx = createTestContext()

  it('does not add a program admin that does not exist', async () => {
    const {page, adminPrograms} = ctx

    await loginAsAdmin(page)

    const programName = 'add-program-admins'
    await adminPrograms.addProgram(programName)

    // Add two program admins and save
    await adminPrograms.gotoManageProgramAdminsPage(programName)
    await page.click('#add-program-admin-button')
    const lastProgramAdminEmailInput =
      '#program-admin-emails div.flex-row:last-of-type input'
    await page.fill(lastProgramAdminEmailInput, 'test@test.com')
    await page.click('#add-program-admin-button')
    await page.fill(lastProgramAdminEmailInput, 'other@test.com')
    await page.click('text=Save')

    await adminPrograms.expectManageProgramAdminsPage()
    await adminPrograms.expectAddProgramAdminErrorToast()

    await validateScreenshot(page, 'add-program-admin-error')
  })
})
