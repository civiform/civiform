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

    // Add another program admin
    await page.click('#add-program-admin-button');
    await page.fill('input:above(#add-program-admin-button)', 'newperson@test.com');

    // Remove the one we just added
    await page.click('button:above(#add-program-admin-button)');
    const visibleInputs = await page.$$('input:visible');
    expect(visibleInputs.length).toBe(2);

    // Remove an old one and add a new one, then submit.
    await page.click('button:above(#add-program-admin-button)'); // Remove
    await page.click('#add-program-admin-button');
    await page.fill('input:above(#add-program-admin-button)', 'new@test.com');
    await page.click('text=Save');

    // Go back and check that there are exactly two - test and new.
    await adminPrograms.gotoManageProgramAdminsPage(programName);
    expect(await page.getAttribute('input:visible', 'value')).toContain('test@test.com');
    expect(await page.getAttribute('input:above(#add-program-admin-button)', 'value')).toContain('new@test.com');

    await endSession(browser);
  });
})
