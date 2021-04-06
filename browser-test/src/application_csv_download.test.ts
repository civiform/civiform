import { startSession, logout, loginAsGuest, loginAsAdmin, ApplicantQuestions, AdminQuestions, AdminPrograms, endSession } from './support'

describe('normal application flow', () => {
  it('all major steps', async () => {
    const { browser, page } = await startSession();
    // Timeout for clicks and element fills. If your seletor fails to locate
    // the HTML element, the test hangs. If you find the tests time out, you
    // want to verify that your selectors are working as expected first.
    // Because all tests are run concurrently, it could be that your selector
    // selects a different entity from another test.
    page.setDefaultTimeout(1000);

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);
    const applicantQuestions = new ApplicantQuestions(page);

    const programName = 'test program for csv export';
    await adminQuestions.addNameQuestion('name-csv');
    await adminPrograms.addAndPublishProgramWithQuestions(['name-csv'], programName);

    await logout(page);
    await loginAsGuest(page);

    await applicantQuestions.applyProgram(programName);

    await applicantQuestions.answerNameQuestion('sarah', 'smith');
    await applicantQuestions.saveAndContinue();

    await logout(page);
    await loginAsAdmin(page);

    await adminPrograms.viewApplications(programName);
    const csvContent = await adminPrograms.getCsv();
    expect(csvContent).toContain('sarah,COLUMN_EMPTY,smith');
    await endSession(browser);
  })
})
