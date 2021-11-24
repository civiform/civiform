import { startSession, logout, loginAsTestUser, loginAsGuest, loginAsProgramAdmin, loginAsAdmin, selectApplicantLanguage, ApplicantQuestions, AdminQuestions, AdminPrograms, endSession, isLocalDevEnvironment } from './support'

describe('normal application flow', () => {
  it('all major steps', async () => {
    const { browser, page } = await startSession();
    // Timeout for clicks and element fills. If your selector fails to locate
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
    await adminQuestions.addNameQuestion({questionName: 'name-csv-download'});
    await adminQuestions.addDropdownQuestion({questionName: 'dropdown-csv-download', options: ['op1', 'op2', 'op3']});
    await adminQuestions.addDateQuestion({questionName: 'csv-date'});
    await adminQuestions.addCurrencyQuestion({questionName: 'csv-currency'});
    await adminQuestions.exportQuestion('name-csv-download');
    await adminQuestions.exportQuestion('dropdown-csv-download');
    await adminQuestions.exportQuestion('csv-date');
    await adminQuestions.exportQuestion('csv-currency');
    await adminPrograms.addAndPublishProgramWithQuestions(['name-csv-download', 'dropdown-csv-download', 'csv-date', 'csv-currency'], programName);

    await logout(page);
    await loginAsTestUser(page);
    await selectApplicantLanguage(page, 'English');

    await applicantQuestions.applyProgram(programName);

    // Applicant fills out first application block.
    await applicantQuestions.answerNameQuestion('sarah', 'smith');
    await applicantQuestions.answerDropdownQuestion('op2');
    await applicantQuestions.answerDateQuestion('2021-05-10');
    await applicantQuestions.answerCurrencyQuestion('1000');
    await applicantQuestions.clickNext();

    // Applicant submits answers from review page.
    await applicantQuestions.submitFromReviewPage(programName);

    await logout(page);
    await loginAsProgramAdmin(page);

    await adminPrograms.viewApplications(programName);
    const csvContent = await adminPrograms.getCsv();
    expect(csvContent).toContain('sarah,,smith,op2,05/10/2021,1000.00');

    await logout(page);
    await loginAsAdmin(page)

    // Change export visibility of a question
    await adminQuestions.createNewVersion('dropdown-csv-download');
    await adminQuestions.gotoQuestionEditPage('dropdown-csv-download');
    await page.click('#question-settings button:text("Remove"):visible')
    await page.click('text=Update');

    // Add a new question so that the program has multiple versions
    await adminQuestions.addNumberQuestion({ questionName: 'number-csv-download' });
    await adminQuestions.exportQuestion('number-csv-download');
    await adminPrograms.editProgramBlock(programName, 'block description', ['number-csv-download']);
    await adminPrograms.publishProgram(programName);

    await logout(page);

    // Apply to the program again, this time a different user
    await loginAsGuest(page);
    await selectApplicantLanguage(page, 'English');
    await applicantQuestions.applyProgram(programName);
    await applicantQuestions.answerNameQuestion('Gus', 'Guest');
    await applicantQuestions.answerDropdownQuestion('op2');
    await applicantQuestions.answerDateQuestion('1990-01-01');
    await applicantQuestions.answerCurrencyQuestion('2000');
    await applicantQuestions.answerNumberQuestion('1600');
    await applicantQuestions.clickNext();
    await applicantQuestions.submitFromReviewPage(programName);

    // Apply to the program again as the same user
    await applicantQuestions.clickApplyProgramButton(programName);
    await applicantQuestions.clickStartHere();
    await applicantQuestions.answerNameQuestion('Gus2', 'Guest');
    await applicantQuestions.clickNext();
    await applicantQuestions.clickNext();
    await applicantQuestions.clickNext();
    await applicantQuestions.clickNext();
    await applicantQuestions.clickNext();
    await applicantQuestions.submitFromReviewPage(programName);
    await logout(page);

    await loginAsProgramAdmin(page);
    await adminPrograms.viewApplications(programName);
    const postEditCsvContent = await adminPrograms.getCsv();

    expect(postEditCsvContent).toContain('sarah,,smith,op2,05/10/2021,1000.00');
    expect(postEditCsvContent).toContain('Gus,,Guest,op2,01/01/1990,2000.00');
    expect(postEditCsvContent).toContain('Gus2,,Guest,op2,01/01/1990,2000.00');

    await logout(page);

    await loginAsAdmin(page)
    await adminPrograms.gotoAdminProgramsPage();
    const demographicsCsvContent = await adminPrograms.getDemographicsCsv();

    expect(demographicsCsvContent).toContain('Opaque ID,Program,Submitter Email (Opaque),TI Organization,Create time,Submit time,csvcurrency (currency),csvdate (date),dropdowncsvdownload (selection),namecsvdownload (first_name),namecsvdownload (middle_name),namecsvdownload (last_name)');
    expect(demographicsCsvContent).toContain('1000.00,05/10/2021,op2,sarah,,smith');

    await adminQuestions.createNewVersion('name-csv-download');
    await adminQuestions.exportQuestionOpaque('name-csv-download');
    await adminPrograms.publishProgram(programName);

    await adminPrograms.gotoAdminProgramsPage();
    const newDemographicsCsvContent = await adminPrograms.getDemographicsCsv();

    expect(newDemographicsCsvContent).toContain('Opaque ID,Program,Submitter Email (Opaque),TI Organization,Create time,Submit time,csvcurrency (currency),csvdate (date),dropdowncsvdownload (selection),numbercsvdownload (number),namecsvdownload (first_name),namecsvdownload (middle_name),namecsvdownload (last_name)');
    expect(newDemographicsCsvContent).not.toContain(',sarah,,smith');
    expect(newDemographicsCsvContent).toContain(',op2,');
    expect(newDemographicsCsvContent).toContain(',1600,');

    if (isLocalDevEnvironment()) {
      // The hashed values "sarah", empty value, "smith", with the dev secret key.
      expect(newDemographicsCsvContent).toContain('5009769596aa83552389143189cec81abfc8f56abc1bb966715c47ce4078c403,057ba03d6c44104863dc7361fe4578965d1887360f90a0895882e58a6248fc86,6eecddf47b5f7a90d41ccc978c4c785265242ce75fe50be10c824b73a25167ba');
    }

    await endSession(browser);
  })
})
