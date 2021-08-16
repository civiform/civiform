import { startSession, loginAsProgramAdmin, loginAsAdmin, AdminQuestions, AdminPrograms, endSession, logout, loginAsTestUser, selectApplicantLanguage, ApplicantQuestions, userDisplayName } from './support'

describe('normal application flow', () => {
  it('all major steps', async () => {
    const { browser, page } = await startSession()
    page.setDefaultTimeout(50000);

    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    await adminQuestions.addDateQuestion('date-optional-q');
    await adminQuestions.addEmailQuestion('email-optional-q');
    await adminQuestions.addDropdownQuestion('ice-cream-optional-q', ['chocolate', 'banana', 'black raspberry']);
    await adminQuestions.addCheckboxQuestion('favorite-trees-optional-q', ['oak', 'maple', 'pine', 'cherry']);
    await adminQuestions.addAddressQuestion('address-optional-q');
    await adminQuestions.addFileUploadQuestion('fileupload-optional-q');
    await adminQuestions.addNameQuestion('name-optional-q');
    await adminQuestions.addNumberQuestion('number-optional-q');
    await adminQuestions.addTextQuestion('text-optional-q');
    await adminQuestions.addRadioButtonQuestion('radio-optional-q', ['one', 'two', 'three']);
    await adminQuestions.addStaticQuestion('static-optional-q');

    const programName = 'Optional Questions Program';
    await adminPrograms.addProgram(programName);
    await adminPrograms.editProgramBlockWithOptional(programName, 'first description', [], 'date-optional-q');
    await adminPrograms.addProgramBlockWithOptional(programName, 'second description', [], 'address-optional-q');
    await adminPrograms.addProgramBlockWithOptional(programName, 'third description', [], 'name-optional-q');
    await adminPrograms.addProgramBlockWithOptional(programName, 'fourth description', [], 'radio-optional-q');
    await adminPrograms.addProgramBlockWithOptional(programName, 'fifth description', [], 'email-optional-q');
    await adminPrograms.addProgramBlockWithOptional(programName, 'sixth description', ['static-optional-q'],
    'ice-cream-optional-q');
    await adminPrograms.addProgramBlockWithOptional(programName, 'seventh description', [],
    'favorite-trees-optional-q');
    await adminPrograms.addProgramBlockWithOptional(programName, 'eighth description', [], 'number-optional-q');
    await adminPrograms.addProgramBlockWithOptional(programName, 'ninth description', [], 'text-optional-q');
    await adminPrograms.addProgramBlockWithOptional(programName, 'tenth description', [], 'fileupload-optional-q');

    const programName2 = 'Second Optional Questions Program';
    await adminPrograms.addProgram(programName2);
    await adminPrograms.editProgramBlockWithOptional(programName2, 'first description', [], 'date-optional-q');

    await adminPrograms.gotoAdminProgramsPage();
    await adminPrograms.expectDraftProgram(programName);
    await adminPrograms.expectDraftProgram(programName2);

    await adminPrograms.publishAllPrograms();
    await adminPrograms.expectActiveProgram(programName);
    await adminPrograms.expectActiveProgram(programName2);

    await logout(page);
    await loginAsTestUser(page);
    await selectApplicantLanguage(page, 'English');

    const applicantQuestions = new ApplicantQuestions(page);
    await applicantQuestions.applyProgram(programName);

    // Skip blocks 1-5 without answering any questions
    await applicantQuestions.clickNext();
    await applicantQuestions.clickNext();
    await applicantQuestions.clickNext();
    await applicantQuestions.clickNext();
    await applicantQuestions.clickNext();

    //Skip blocks 6-10 without answering any questions
    await applicantQuestions.seeStaticQuestion('static question text');
    await applicantQuestions.clickNext();
    await applicantQuestions.clickNext();
    await applicantQuestions.clickNext();
    await applicantQuestions.clickNext();
    // Skip file upload
    await applicantQuestions.clickSkip();

    // Submit the first program
    await applicantQuestions.submitFromReviewPage(programName);

    // Complete the second program
    await applicantQuestions.applyProgram(programName2);

    // Skip Screen 1 when it pops up to be answered again
    await applicantQuestions.clickNext();
    // Submit from review page
    await applicantQuestions.submitFromReviewPage(programName2);
    await endSession(browser);
  })
})
