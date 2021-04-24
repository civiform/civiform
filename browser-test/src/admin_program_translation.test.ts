import { AdminPrograms, AdminTranslations, endSession, loginAsAdmin, loginAsGuest, logout, selectApplicantLanguage, startSession } from './support'

describe('Create program and manage translations', () => {
  it('create a program and add translation', async () => {
    const { browser, page } = await startSession();

    await loginAsAdmin(page);
    const adminPrograms = new AdminPrograms(page);

    const programName = 'program to be translated';
    await adminPrograms.addProgram(programName);

    // Go to manage translations page.
    await adminPrograms.gotoDraftProgramManageTranslationsPage(programName);
    const adminTranslations = new AdminTranslations(page);

    // Add translations for Spanish and publish
    await adminTranslations.selectLanguage('Spanish');
    await adminTranslations.editTranslations('Spanish name', 'Spanish description');
    await adminPrograms.publishProgram(programName);

    // View the applicant program page in Spanish and check that the translations are present
    await logout(page);
    await loginAsGuest(page);
    await selectApplicantLanguage(page, 'Español');
    const cardText = await page.innerText('.cf-application-card:has-text("' + programName + '")');
    expect(cardText).toContain('Spanish name');
    expect(cardText).toContain('Spanish description');
  })
})