import { startSession, logout, loginAsGuest, loginAsAdmin, ApplicantQuestions, AdminQuestions, AdminPrograms, endSession } from './support'

describe('normal application flow', () => {
  // Our browser tests could be long-running (as of 2021-04-06, the longest
  // takes 27 seconds). However, if your seletor fails to locate the HTML
  // element, the test hangs as well. If you find the tests take unnaturally
  // long, you want to verify that your selectors are working as expected.
  // Because all tests are run concurrently, it could be that your selector
  // selects a different entity from another test.
  jest.setTimeout(200000);
  it('all major steps', async () => {
    const { browser, page } = await startSession();
    // timeout for clicks and element fills.
    page.setDefaultTimeout(1000);

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);
    const applicantQuestions = new ApplicantQuestions(page);

    const programName = 'test program for csv export';
    await adminQuestions.addNameQuestion('name');
    await adminPrograms.addAndPublishProgramWithQuestions(['name'], programName);

    await logout(page);
    await loginAsGuest(page);

    await applicantQuestions.applyProgram(programName);

    await applicantQuestions.answerQuestion('applicant.name.first', 'sarah');
    await applicantQuestions.answerQuestion('applicant.name.last', 'smith');
    await applicantQuestions.saveAndContinue();

    await logout(page);
    await loginAsAdmin(page);

    await adminPrograms.viewApplications(programName);
    const csvContent = await adminPrograms.getCsv();
    expect(csvContent).toContain('sarah,COLUMN_EMPTY,smith');
    await endSession(browser);
  })
})
