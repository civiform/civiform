import { startSession, loginAsAdmin, AdminPrograms, endSession } from './support'

describe('manage program admins', () => {
  it('add program admins', async () => {
    const { browser, page } = await startSession();

    await loginAsAdmin(page);
    const adminPrograms = new AdminPrograms(page);

    const programName = 'add program admins';
    await adminPrograms.addProgram(programName);

    // Add two program admins and save
    await adminPrograms.gotoManageProgramAdminsPage(programName);
    await page.click('#add-program-admin-button');
    await page.fill('input:above(#add-program-admin-button)', 'test@test.com');
    await page.click('#add-program-admin-button');
    await page.fill('input:above(#add-program-admin-button)', 'other@test.com');
    await page.click('text=Save');

    // Go to manage program admins again
    await adminPrograms.expectAdminProgramsPage();
    await adminPrograms.gotoManageProgramAdminsPage(programName);

    // Assert that the two emails we added are present.
    // Use input:visible to get the first visible input, since the template is hidden.
    expect(await page.getAttribute('input:visible', 'value')).toContain('test@test.com');
    expect(await page.getAttribute('input:above(#add-program-admin-button)', 'value')).toContain('other@test.com');

    await endSession(browser);
  })
})