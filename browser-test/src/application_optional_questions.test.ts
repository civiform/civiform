import { startSession, loginAsProgramAdmin, loginAsAdmin, AdminQuestions, AdminPrograms, endSession, logout, loginAsTestUser, selectApplicantLanguage, ApplicantQuestions, userDisplayName } from './support'

describe('normal application flow', () => {
  it('all major steps', async () => {
    const { browser, page } = await startSession()
    page.setDefaultTimeout(5000);

    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    const questions = await adminQuestions.addAllNonSingleBlockQuestionTypes('optional-');
    await adminQuestions.addFileUploadQuestion('optional-file-upload');
    await adminQuestions.addStaticQuestion('optional-static');
    questions.push('optional-file-upload');
    questions.push('optional-static');

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
