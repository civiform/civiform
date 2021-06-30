import { startSession, logout, loginAsTestUser, loginAsProgramAdmin, loginAsAdmin, selectApplicantLanguage, ApplicantQuestions, AdminQuestions, AdminPrograms, endSession, isLocalDevEnvironment } from './support'

describe('normal application flow', () => {
  it('all major steps', async () => {
    const { browser, page } = await startSession();
    // Timeout for clicks and element fills. If your seletor fails to locate
    // the HTML element, the test hangs. If you find the tests time out, you
    // want to verify that your selectors are working as expected first.
    // Because all tests are run concurrently, it could be that your selector
    // selects a different entity from another test.
    page.setDefaultTimeout(4000);

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);
    const applicantQuestions = new ApplicantQuestions(page);

    const programName = 'test program for csv export';
    await adminQuestions.addNameQuestion('name-csv-download');
    await adminQuestions.addDropdownQuestion('dropdown-csv-download', ['op1', 'op2', 'op3']);
    await adminQuestions.exportQuestion('name-csv-download');
    await adminQuestions.exportQuestion('dropdown-csv-download');
    await adminPrograms.addAndPublishProgramWithQuestions(['name-csv-download', 'dropdown-csv-download'], programName);

    await logout(page);
    await loginAsTestUser(page);
    await selectApplicantLanguage(page, 'English');

    await applicantQuestions.applyProgram(programName);

    // Applicant fills out first application block.
    await applicantQuestions.answerNameQuestion('sarah', 'smith');
    await applicantQuestions.answerDropdownQuestion('op2');
    await applicantQuestions.clickNext();

    // Application submits answers from review page.
    await applicantQuestions.submitFromReviewPage(programName);

    await logout(page);
    await loginAsProgramAdmin(page);

    await adminPrograms.viewApplications(programName);
    const csvContent = await adminPrograms.getCsv();
    expect(csvContent).toContain('sarah,,smith,op2');

    await logout(page);
    await loginAsAdmin(page)

    await adminQuestions.createNewVersion('dropdown-csv-download');
    await adminQuestions.gotoQuestionEditPage('dropdown-csv-download');
    await page.click('#question-settings button:text("Remove"):visible')
    await page.click('text=Update');
    await adminPrograms.publishProgram(programName);

    await logout(page);
    await loginAsProgramAdmin(page);

    await adminPrograms.viewApplications(programName);
    await adminPrograms.viewApplicationsInOldVersion();
    const postEditCsvContent = await adminPrograms.getCsv();
    expect(postEditCsvContent).toContain('sarah,,smith,op2');

    await logout(page);
    await loginAsAdmin(page)

    await adminPrograms.gotoAdminProgramsPage();
    const demographicsCsvContent = await adminPrograms.getDemographicsCsv();
    expect(demographicsCsvContent).toContain('Opaque ID,Program,Submitter Email (Opaque),TI Organization,Create time,Submit time,dropdowncsvdownload (selection),namecsvdownload (first_name),namecsvdownload (middle_name),namecsvdownload (last_name)');
    expect(demographicsCsvContent).toContain('op2,sarah,,smith');

    await adminQuestions.createNewVersion('name-csv-download');
    await adminQuestions.exportQuestionOpaque('name-csv-download');
    await adminPrograms.publishProgram(programName);

    await adminPrograms.gotoAdminProgramsPage();
    const newDemographicsCsvContent = await adminPrograms.getDemographicsCsv();
    expect(newDemographicsCsvContent).toContain('Opaque ID,Program,Submitter Email (Opaque),TI Organization,Create time,Submit time,dropdowncsvdownload (selection),namecsvdownload (first_name),namecsvdownload (middle_name),namecsvdownload (last_name)');
    expect(newDemographicsCsvContent).not.toContain(',sarah,,smith');
    expect(newDemographicsCsvContent).toContain(',op2,');
    if (isLocalDevEnvironment()) {
      // The hashed values "sarah", empty value, "smith", with the dev secret key.
      expect(newDemographicsCsvContent).toContain('5009769596aa83552389143189cec81abfc8f56abc1bb966715c47ce4078c403,057ba03d6c44104863dc7361fe4578965d1887360f90a0895882e58a6248fc86,6eecddf47b5f7a90d41ccc978c4c785265242ce75fe50be10c824b73a25167ba');
    }

    await endSession(browser);
  })
})
