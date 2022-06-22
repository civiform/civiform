import {startSession, loginAsAdmin, AdminPrograms, endSession} from './support'

describe('manage program admins', () => {
  it('does not add a program admin that does not exist', async () => {
    const {browser, page} = await startSession()

    await loginAsAdmin(page)
    const adminPrograms = new AdminPrograms(page)

    const programName = 'add program admins'
    await adminPrograms.addProgram(programName)

    // Add two program admins and save
    await adminPrograms.gotoManageProgramAdminsPage(programName)
    await page.click('#add-program-admin-button')
    var lastProgramAdminEmailInput =
      '#program-admin-emails div.flex-row:last-of-type input'
    await page.fill(lastProgramAdminEmailInput, 'test@test.com')
    await page.click('#add-program-admin-button')
    await page.fill(lastProgramAdminEmailInput, 'other@test.com')
    await page.click('text=Save')

    await adminPrograms.expectManageProgramAdminsPage()
    await adminPrograms.expectAddProgramAdminErrorToast()

    await endSession(browser)
  })
})
