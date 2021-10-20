import {
  startSession, loginAsAdmin, loginAsTestUser, logout, selectApplicantLanguage, AdminQuestions, AdminPrograms, ApplicantQuestions, endSession
} from './support'

describe('input validation for number questions', () => {
  it('blocks non-numeric characters from input', async () => {
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
      '123', 'abc123', '123abc', '12!@#$%^&*()3', '12[]3', '12d3', '12e3', '12E3', '12+3', '12-3'
    ]
    const expectedValue = '123';
    const numberInput = 'div.cf-question-number input'

    for (const testValue of testValues) {
      await page.type(numberInput, testValue);
      expect([testValue, await page.inputValue(numberInput)])
        .toStrictEqual([testValue, expectedValue]);
      await page.fill(numberInput, '');
    }

    await endSession(browser);
  });
});
