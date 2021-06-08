import { startSession, loginAsAdmin, AdminPrograms, endSession } from './support'

describe('manage program admins', () => {
  it('add and remove program admins', async () => {
    const { browser, page } = await startSession();

    await loginAsAdmin(page);
    const adminPrograms = new AdminPrograms(page);

    const programName = 'add program admins';
    await adminPrograms.addProgram(programName);

    // Add two program admins and save
    await adminPrograms.gotoManageProgramAdminsPage(programName);
    await page.click('#add-program-admin-button');
    var lastProgramAdminEmailInput = '#program-admin-emails div.flex-row:last-of-type input';
    await page.fill(lastProgramAdminEmailInput, 'test@test.com');
    await page.click('#add-program-admin-button');
    await page.fill(lastProgramAdminEmailInput, 'other@test.com');
    await page.click('text=Save');

    // Go to manage program admins again
    await adminPrograms.expectAdminProgramsPage();
    await adminPrograms.gotoManageProgramAdminsPage(programName);

    // Assert that the two emails we added are present.
    var firstProgramAdminEmailInput = '#program-admin-emails div.flex-row:first-of-type input';
    expect(await page.getAttribute(firstProgramAdminEmailInput, 'value')).toContain('test@test.com');
    expect(await page.getAttribute(lastProgramAdminEmailInput, 'value')).toContain('other@test.com');

    // Add another program admin
    await page.click('#add-program-admin-button');
    await page.fill(lastProgramAdminEmailInput, 'newperson@test.com');

    // Remove the one we just added
    var removeLastProgramAdminEmailButton = '#program-admin-emails div.flex-row:last-of-type button';
    await page.click(removeLastProgramAdminEmailButton);
    const programAdminEmailInputs = await page.$$('#program-admin-emails input');
    expect(programAdminEmailInputs.length).toBe(2);

    // Remove an old one and add a new one, then submit.
    await page.click(removeLastProgramAdminEmailButton);
    await page.click('#add-program-admin-button');
    await page.fill(lastProgramAdminEmailInput, 'new@test.com');
    await page.click('text=Save');

    // Go back and check that there are exactly two - test and new.
    await adminPrograms.gotoManageProgramAdminsPage(programName);
    expect(await page.getAttribute(firstProgramAdminEmailInput, 'value')).toContain('test@test.com');
    expect(await page.getAttribute(lastProgramAdminEmailInput, 'value')).toContain('new@test.com');

    await endSession(browser);
  });
})
