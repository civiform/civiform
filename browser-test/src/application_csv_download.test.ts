import { startSession, logout, loginAsGuest, loginAsAdmin, ApplicantQuestions, AdminQuestions, AdminPrograms, endSession } from './support'

describe('normal application flow', () => {
  // If this times out, a likely cause is the .click() calls in
  // support/admin_programs, which are called out as being asserts.
  jest.setTimeout(25000);
  it('all major steps', async () => {
    const { browser, page } = await startSession()
    // timeout for clicks and element fills.
    page.setDefaultTimeout(1000);

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);
    const applicantQuestions = new ApplicantQuestions(page);

    await adminQuestions.addNameQuestion('name');
    await adminPrograms.addProgram(['name'], 'program');

    await logout(page);
    await loginAsGuest(page);

    await applicantQuestions.applyButton();

    await applicantQuestions.answerQuestion('applicant.name.first', 'sarah');
    await applicantQuestions.answerQuestion('applicant.name.last', 'smith');
    await applicantQuestions.saveAndContinue();

    await logout(page);
    await loginAsAdmin(page)

    await adminPrograms.viewApplications();
    const csvContent = await adminPrograms.getCsv();
    expect(csvContent).toContain('sarah,COLUMN_EMPTY,smith');
    await endSession(browser);
  })
})
