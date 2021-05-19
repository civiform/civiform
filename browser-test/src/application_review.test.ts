import { startSession, loginAsAdmin, AdminQuestions, AdminPrograms, endSession, logout, loginAsTestUser, selectApplicantLanguage, ApplicantQuestions, userDisplayName } from './support'

describe('normal application flow', () => {
  it('all major steps', async () => {
    const { browser, page } = await startSession()
    page.setDefaultTimeout(2000);

    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    await adminQuestions.addDateQuestion('date-q');
    await adminQuestions.addDropdownQuestion('ice-cream-q', ['chocolate', 'banana', 'black raspberry']);
    await adminQuestions.addCheckboxQuestion('favorite-trees-q', ['oak', 'maple', 'pine', 'cherry']);
    await adminQuestions.addAddressQuestion('address-q');
    await adminQuestions.addFileUploadQuestion('fileupload-q');
    await adminQuestions.addNameQuestion('name-q');
    await adminQuestions.addNumberQuestion('number-q');
    await adminQuestions.addTextQuestion('text-q');
    await adminQuestions.addRadioButtonQuestion('radio-q', ['one', 'two', 'three']);

    const programName = 'a shiny new program';
    await adminPrograms.addProgram(programName);
    await adminPrograms.editProgramBlock(programName, 'block description', ['date-q', 'address-q', 'name-q', 'radio-q']);
    await adminPrograms.addProgramBlock(programName, 'another description', ['ice-cream-q', 'favorite-trees-q', 'number-q', 'text-q']);
    await adminPrograms.addProgramBlock(programName, 'third description', ['fileupload-q']);

    await adminPrograms.gotoAdminProgramsPage();
    await adminPrograms.expectDraftProgram(programName);

    await adminPrograms.publishProgram(programName);
    await adminPrograms.expectActiveProgram(programName);

    await adminQuestions.expectActiveQuestionExist('ice-cream-q');
    await adminQuestions.expectActiveQuestionExist('favorite-trees-q');
    await adminQuestions.expectActiveQuestionExist('address-q');
    await adminQuestions.expectActiveQuestionExist('name-q');
    await adminQuestions.expectActiveQuestionExist('date-q');
    await adminQuestions.expectActiveQuestionExist('number-q');
    await adminQuestions.expectActiveQuestionExist('text-q');
    await adminQuestions.expectActiveQuestionExist('radio-q');

    await logout(page);
    await loginAsTestUser(page);
    await selectApplicantLanguage(page, 'English');

    const applicantQuestions = new ApplicantQuestions(page);
    await applicantQuestions.validateHeader('en-US');

    // Applicant fills out first application block.
    await applicantQuestions.applyProgram(programName);
    await applicantQuestions.answerAddressQuestion('1234 St', 'Unit B', 'Sim', 'Ames', '54321');
    await applicantQuestions.answerNameQuestion('Queen', 'Hearts', 'of');
    await applicantQuestions.answerRadioButtonQuestion('two');
    await applicantQuestions.answerDateQuestion('2021-05-10');
    await applicantQuestions.clickNext();

    // Applicant fills out second application block.
    await applicantQuestions.answerDropdownQuestion('banana');
    await applicantQuestions.answerCheckboxQuestion(['cherry', 'pine']);
    await applicantQuestions.answerNumberQuestion('42');
    await applicantQuestions.answerTextQuestion('some text');
    await applicantQuestions.clickNext();

    // Applicant fills out third application block.
    await applicantQuestions.answerFileUploadQuestion('file key');
    await applicantQuestions.clickNext();

    // Applicant submits answers from review page.
    await applicantQuestions.submitFromReviewPage(programName);

    await logout(page);
    await loginAsAdmin(page);

    await adminPrograms.viewApplications(programName);
    await adminPrograms.viewApplicationForApplicant(userDisplayName());
    await adminPrograms.expectApplicationAnswers('Block 1', 'address-q', '1234 St');
    await adminPrograms.expectApplicationAnswers('Block 1', 'name-q', 'Queen');

    // TODO: display the string values of selects instead of integer IDs
    // https://github.com/seattle-uat/civiform/issues/778
    await adminPrograms.expectApplicationAnswers('Block 1', 'radio-q', '2');
    await adminPrograms.expectApplicationAnswers('Block 1', 'date-q', '05/10/2021');

    await adminPrograms.expectApplicationAnswers('Block 2', 'ice-cream-q', '2');
    await adminPrograms.expectApplicationAnswers('Block 2', 'favorite-trees-q', 'pine cherry');

    await adminPrograms.expectApplicationAnswers('Block 2', 'number-q', '42');
    await adminPrograms.expectApplicationAnswers('Block 2', 'text-q', 'some text');
    await adminPrograms.expectApplicationAnswers('Block 3', 'fileupload-q', '-- FILE UPLOADED --');
    await endSession(browser);
  })
})
