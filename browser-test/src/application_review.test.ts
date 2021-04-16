import { startSession, loginAsAdmin, AdminQuestions, AdminPrograms, endSession, logout, loginAsGuest, ApplicantQuestions } from './support'

describe('normal application flow', () => {
  it('all major steps', async () => {
    const { browser, page } = await startSession()
    page.setDefaultTimeout(2000);

    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    await adminQuestions.addDropdownQuestion('ice-cream-q', ['chocolate', 'banana', 'black raspberry']);
    await adminQuestions.addCheckboxQuestion('favorite-trees-q', ['oak', 'maple', 'pine', 'cherry']);
    await adminQuestions.addAddressQuestion('address-q');
    await adminQuestions.addNameQuestion('name-q');
    await adminQuestions.addNumberQuestion('number-q');
    await adminQuestions.addTextQuestion('text-q');
    await adminQuestions.addRadioButtonQuestion('radio-q', ['one', 'two', 'three']);

    const programName = 'a shiny new program';
    await adminPrograms.addProgram(programName);
    await adminPrograms.editProgramBlock(programName, 'block description', ['address-q', 'name-q', 'radio-q']);
    await adminPrograms.addProgramBlock(programName, 'another description', ['ice-cream-q', 'favorite-trees-q', 'number-q', 'text-q']);

    await adminPrograms.gotoAdminProgramsPage();
    await adminPrograms.expectDraftProgram(programName);

    await adminPrograms.publishProgram(programName);
    await adminPrograms.expectActiveProgram(programName);

    await adminQuestions.expectActiveQuestionExist('ice-cream-q');
    await adminQuestions.expectActiveQuestionExist('favorite-trees-q');
    await adminQuestions.expectActiveQuestionExist('address-q');
    await adminQuestions.expectActiveQuestionExist('name-q');
    await adminQuestions.expectActiveQuestionExist('number-q');
    await adminQuestions.expectActiveQuestionExist('text-q');
    await adminQuestions.expectActiveQuestionExist('radio-q');

    await logout(page);
    await loginAsGuest(page);

    const applicantQuestions = new ApplicantQuestions(page);

    await applicantQuestions.applyProgram(programName);
    await applicantQuestions.answerAddressQuestion('1234 St', 'Sim', 'Ames', '54321');
    await applicantQuestions.answerNameQuestion('Queen', 'Hearts', 'of');
    await applicantQuestions.answerRadioButtonQuestion('two');
    await applicantQuestions.saveAndContinue();

    await applicantQuestions.answerDropdownQuestion('banana');
    await applicantQuestions.answerCheckboxQuestion(['cherry', 'pine']);
    await applicantQuestions.answerNumberQuestion('42');
    await applicantQuestions.answerTextQuestion('some text');
    await applicantQuestions.saveAndContinue();

    await logout(page);
    await loginAsAdmin(page);

    await adminPrograms.viewApplications(programName);
    await adminPrograms.viewApplicationForApplicant('<Anonymous Applicant>');
    await adminPrograms.expectApplicationAnswers('Block 1', 'address-q', '1234 St');
    await adminPrograms.expectApplicationAnswers('Block 1', 'name-q', 'Queen');

    // TODO: display the string values of selects instead of integer IDs
    await adminPrograms.expectApplicationAnswers('Block 1', 'radio-q', '2');
    await adminPrograms.expectApplicationAnswers('Block 2', 'ice-cream-q', '2');
    await adminPrograms.expectApplicationAnswers('Block 2', 'favorite-trees-q', '[3, 4]');

    await adminPrograms.expectApplicationAnswers('Block 2', 'number-q', '42');
    await adminPrograms.expectApplicationAnswers('Block 2', 'text-q', 'some text');
    await endSession(browser);
  })
})
