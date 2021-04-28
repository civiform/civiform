import { AdminPrograms, AdminQuestions, AdminTranslations, ApplicantQuestions, endSession, loginAsAdmin, loginAsGuest, logout, selectApplicantLanguage, startSession } from './support'

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
    const publicName = 'Spanish name';
    await adminTranslations.editTranslations(publicName, 'Spanish description');
    await adminPrograms.publishProgram(programName);

    // View the applicant program page in Spanish and check that the translations are present
    await logout(page);
    await loginAsGuest(page);
    await selectApplicantLanguage(page, 'Español');
    const cardText = await page.innerText('.cf-application-card:has-text("' + publicName + '")');
    expect(cardText).toContain('Spanish name');
    expect(cardText).toContain('Spanish description');

    await endSession(browser);
  });

  it('Applicant sees toast message warning translation is not complete', async () => {
    const { browser, page } = await startSession();

    // Add a new program with one non-translated question
    await loginAsAdmin(page);
    const adminPrograms = new AdminPrograms(page);
    const adminQuestions = new AdminQuestions(page);

    const programName = 'toast';
    await adminPrograms.addProgram(programName);

    await adminQuestions.addNameQuestion('name-english');
    await adminPrograms.editProgramBlock(programName, 'not translated', ['name-english']);

    await adminPrograms.publishProgram(programName);
    await logout(page);

    // Set applicant preferred language to Spanish
    // DO NOT LOG IN AS TEST USER. We want a fresh guest so we can guarantee
    // the language has not yet been set.
    await loginAsGuest(page);
    await selectApplicantLanguage(page, 'Español');
    const applicantQuestions = new ApplicantQuestions(page);
    await applicantQuestions.applyProgram(programName);

    // Check that a toast appears warning the program is not fully translated
    const toastMessages = await page.innerText('#toast-container')
    expect(toastMessages).toContain('Lo siento, este programa no está completamente traducido al español.');

    await endSession(browser);
  });
})
