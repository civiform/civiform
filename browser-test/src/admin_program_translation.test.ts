import { AdminPrograms, AdminTranslations, endSession, loginAsAdmin, startSession } from './support'

describe('Create program and manage translations', () => {
  it('create a program and add translation', async () => {
    const { browser, page } = await startSession();

    await loginAsAdmin(page);
    const adminPrograms = new AdminPrograms(page);
    const adminTranslations = new AdminTranslations(page);

    const programName = 'program to be translated';
    await adminPrograms.addProgram(programName);

    // Go to manage translations page.
    adminPrograms.gotoDraftProgramManageTranslationsPage(programName);

    // Add translations for Spanish
    adminTranslations.selectLanguage('Spanish');
    adminTranslations.editTranslations('Spanish name', 'Spanish description');

    // Go back to manage translations page and check the Spanish translations are there
    adminPrograms.gotoDraftProgramManageTranslationsPage(programName);
    adminTranslations.selectLanguage('Spanish');
    expect(await this.page.innerText('#localize-display-name')).toContain('Spanish name');
    expect(await this.page.innerText('#localize-display-description')).toContain('Spanish description');
  })
})