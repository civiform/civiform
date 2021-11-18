import {
  startSession, loginAsAdmin, loginAsTestUser, logout, selectApplicantLanguage, AdminQuestions, AdminPrograms, ApplicantQuestions, waitForPageJsLoad, endSession
} from './support'

describe('input type', () => {
  it('sets the inputmode attribute to decimal', async () => {
    const { browser, page } = await startSession();
    page.setDefaultTimeout(4000);
    // Go to number question preview
    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    adminQuestions.gotoAdminQuestionsPage();
    await page.click('#create-question-button');
    await page.click('#create-number-question');
    await waitForPageJsLoad(page);

    // Confirm that inputmode is set to decimal
    expect(await page.getAttribute('div.cf-question-number input', 'inputmode')).toEqual('decimal');

  });
});

describe('input validation for number questions', () => {
  it('displays error message for non-numeric characters in input', async () => {
    const { browser, page } = await startSession();
    page.setDefaultTimeout(4000);

    // Set up test program and question
    const questionName = 'single-number';
    const programName = 'number test program';
    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);


    await adminQuestions.addNumberQuestion({ questionName });
    await adminPrograms.addAndPublishProgramWithQuestions([questionName], programName);


    // Switch to applicant view and open an application for the new program
    await logout(page);
    await loginAsTestUser(page);
    await selectApplicantLanguage(page, 'English');
    const applicant = new ApplicantQuestions(page);
    await applicant.applyProgram(programName);
    await applicant.validateHeader('en-US');

    const testValues = [
      '12e3', '12E3', '+123', '-123'
    ]
    const numberInput = 'div.cf-question-number'
    const numberInputError = 'div.cf-question-number-error'

    for (const testValue of testValues) {
      await page.type(numberInput, testValue);
      await applicant.clickNext();
      expect(await page.isHidden(numberInputError)).toBeFalsy();
      await page.fill(numberInput, '');
    }

    await endSession(browser);
  });
});
