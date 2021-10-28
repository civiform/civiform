import {AdminPrograms, AdminQuestions, ApplicantQuestions, loginAsAdmin, loginAsTestUser, logout, selectApplicantLanguage, startSession} from './support'

describe('currency applicant flow', () => {
  const validCurrency = "1000";
  // Not enough decimals.
  const invalidCurrency = "1.0";
  describe('single currency question', () => {
    let pageObject, applicantQuestions;
    let programName = 'test program for single currency';

    beforeAll(async () => {
      const {browser, page} = await startSession()
      pageObject = page;
      page.setDefaultTimeout(5000);

      await loginAsAdmin(page);
      const adminQuestions = new AdminQuestions(page);
      const adminPrograms = new AdminPrograms(page);
      applicantQuestions = new ApplicantQuestions(page);

      await adminQuestions.addCurrencyQuestion({questionName: 'currency-q'});
      await adminPrograms.addAndPublishProgramWithQuestions(['currency-q'], programName);

      await logout(page);
    });

    it('with valid currency does submit', async () => {
      await loginAsTestUser(pageObject);
      await selectApplicantLanguage(pageObject, 'English');

      await applicantQuestions.applyProgram(programName);
      await applicantQuestions.answerCurrencyQuestion(validCurrency);
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
      await applicantQuestions.answerCurrencyQuestion(invalidCurrency);
      await applicantQuestions.clickNext();

      // The block should be displayed still with the error shown.
      expect(await error.isHidden()).toEqual(false);

      await logout(pageObject);
    })
  });

  describe('multiple currency questions', () => {
    let pageObject, applicantQuestions;
    let programName = 'test program for multiple currencies';
    beforeAll(async () => {
      const {page} = await startSession()
      pageObject = page;
      page.setDefaultTimeout(5000);

      await loginAsAdmin(page);
      const adminQuestions = new AdminQuestions(page);
      const adminPrograms = new AdminPrograms(page);
      applicantQuestions = new ApplicantQuestions(page);

      await adminQuestions.addCurrencyQuestion({questionName: 'currency-a-q'});
      await adminQuestions.addCurrencyQuestion({questionName: 'currency-b-q'});
      await adminPrograms.addAndPublishProgramWithQuestions(['currency-a-q', 'currency-b-q'], programName);

      await logout(page);
    });

    it('with valid currencies does submit', async () => {
      await loginAsTestUser(pageObject);
      await selectApplicantLanguage(pageObject, 'English');

      await applicantQuestions.applyProgram(programName);
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 0);
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 1);
      await applicantQuestions.clickNext();

      await applicantQuestions.submitFromReviewPage(programName);
      await logout(pageObject);
    });

    it('with first invalid does not submit', async () => {
      await loginAsTestUser(pageObject);
      await selectApplicantLanguage(pageObject, 'English');

      await applicantQuestions.applyProgram(programName);
      const error = await pageObject.$('.cf-currency-value-error >> nth=0');
      expect(await error.isHidden()).toEqual(true);

      await applicantQuestions.answerCurrencyQuestion(invalidCurrency, 0);
      await applicantQuestions.answerCurrencyQuestion(validCurrency, 1);
      await applicantQuestions.clickNext();

      expect(await error.isHidden()).toEqual(false);
      await logout(pageObject);
    });

    it('with second invalid does not submit', async () => {
      await loginAsTestUser(pageObject);
      await selectApplicantLanguage(pageObject, 'English');

      await applicantQuestions.applyProgram(programName);
      const error = await pageObject.$('.cf-currency-value-error >> nth=1');
      expect(await error.isHidden()).toEqual(true);

      await applicantQuestions.answerCurrencyQuestion(validCurrency, 0);
      await applicantQuestions.answerCurrencyQuestion(invalidCurrency, 1);
      await applicantQuestions.clickNext();

      expect(await error.isHidden()).toEqual(false);
      await logout(pageObject);
    });
  });
})
