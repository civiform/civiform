import {AdminPrograms, AdminQuestions, ApplicantQuestions, endSession, loginAsAdmin, loginAsTestUser, logout, selectApplicantLanguage, startSession} from './support'

describe('currency applicant flow', () => {
  describe('single currency question', () => {
    let pageObject, applicantQuestions;
    let programName = 'test program for csv export';
    beforeAll( async () => {
      const {browser, page} = await startSession()
      pageObject = page;
      page.setDefaultTimeout(5000);

      await loginAsAdmin(page);
      const adminQuestions = new AdminQuestions(page);
      const adminPrograms = new AdminPrograms(page);
      applicantQuestions = new ApplicantQuestions(page);

      await adminQuestions.addCurrencyQuestion({questionName: 'csv-currency'});
      await adminPrograms.addAndPublishProgramWithQuestions(['csv-currency'], programName);

      await logout(page);
    });

    it('with valid currency does submit', async () => {
      await loginAsTestUser(pageObject);
      await selectApplicantLanguage(pageObject, 'English');

      await applicantQuestions.applyProgram(programName);
      await applicantQuestions.answerCurrencyQuestion('1000');
      await applicantQuestions.clickNext();

      await applicantQuestions.submitFromReviewPage(programName);
      await logout(pageObject);
    });

    it('program with invalid currency does not submit', async () => {
      await loginAsTestUser(pageObject);
      await selectApplicantLanguage(pageObject, 'English');

      await applicantQuestions.applyProgram(programName);
      const error = await pageObject.$('.cf-currency-value-error');
      expect(await error.isHidden()).toEqual(true);

      // Input has not enough decimal points.
      await applicantQuestions.answerCurrencyQuestion('1.0');
      await applicantQuestions.clickNext();

      // The block should be displayed still with the error shown.
      expect(await error.isHidden()).toEqual(false);

      await logout(pageObject);
    })
  })
})
