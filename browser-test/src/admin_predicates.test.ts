import { AdminPredicates, AdminPrograms, AdminQuestions, ApplicantQuestions, endSession, loginAsAdmin, loginAsTestUser, logout, selectApplicantLanguage, startSession, userDisplayName } from './support'

describe('create and edit predicates', () => {
  it('add a predicate', async () => {
    const { browser, page } = await startSession();

    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    // Add a program with two blocks
    await adminQuestions.addTextQuestion('predicate-q');
    await adminQuestions.addTextQuestion('other-q');

    const programName = 'create predicate';
    await adminPrograms.addProgram(programName);
    await adminPrograms.editProgramBlock(programName, 'first block', ['predicate-q']);
    await adminPrograms.addProgramBlock(programName, 'block with predicate', ['other-q']);

    // Edit predicate for second block
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Block 2');
    const adminPredicates = new AdminPredicates(page);
    await adminPredicates.addPredicate('predicate-q', 'hidden if', 'text', 'is equal to', 'hide me');
    await adminPredicates.expectVisibilityConditionEquals('Block 2 is hidden if predicate-q\'s text is equal to "hide me"');
  });

  it('every right hand type evaluates correctly', async () => {
    const { browser, page } = await startSession();

    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    // DATE, STRING, LONG, LIST_OF_STRINGS, LIST_OF_LONGS
    await adminQuestions.addNameQuestion('single-string');
    await adminQuestions.addTextQuestion('list of strings');
    await adminQuestions.addNumberQuestion('single-long');
    await adminQuestions.addDateQuestion('date');
    await adminQuestions.addCheckboxQuestion('both sides are lists', ['a', 'b', 'c']);
    await adminQuestions.addTextQuestion('depends on previous');

    const programName = 'test all predicate types';
    await adminPrograms.addProgram(programName);
    await adminPrograms.editProgramBlock(programName, 'string', ['single-string']);
    await adminPrograms.addProgramBlock(programName, 'list of strings', ['list of strings']);
    await adminPrograms.addProgramBlock(programName, 'long', ['single-long']);
    await adminPrograms.addProgramBlock(programName, 'date', ['date']);
    await adminPrograms.addProgramBlock(programName, 'two lists', ['both sides are lists']);
    await adminPrograms.addProgramBlock(programName, 'last', ['depends on previous']);

    // Simple string predicate
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Block 2');
    const adminPredicates = new AdminPredicates(page);
    await adminPredicates.addPredicate('single-string', 'shown if', 'first name', 'is not equal to', 'hidden');

    // Single string one of a list of strings
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Block 3');
    await adminPredicates.addPredicate('list of strings', 'shown if', 'text', 'is one of', 'blue, green');

    // Simple long predicate
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Block 4');
    await adminPredicates.addPredicate('single-long', 'shown if', 'number', 'is equal to', '42');

    // Date predicate
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Block 5');
    await adminPredicates.addPredicate('date', 'shown if', 'date', 'is earlier than', '2021-01-01');

    // Lists of strings on both sides
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Block 6');
    await adminPredicates.addPredicate('both sides are lists', 'shown if', 'selections', 'contains any of', 'a, c');

    await adminPrograms.publishProgram(programName);

    // Switch to applicant view - if they answer each question according to the predicate,
    // the next block will be shown.
    await logout(page);
    await loginAsTestUser(page);
    await selectApplicantLanguage(page, 'English');
    const applicant = new ApplicantQuestions(page);
    await applicant.applyProgram(programName);

    await applicant.answerNameQuestion('show', 'next', 'block');
    await applicant.clickNext();
    await applicant.answerTextQuestion('blue');
    await applicant.clickNext();
    await applicant.answerNumberQuestion('42');
    await applicant.clickNext();
    await applicant.answerDateQuestion('1998-09-04');
    await applicant.clickNext();
    await applicant.answerCheckboxQuestion(['c']);
    await applicant.clickNext();

    // We should now be on the summary page
    await applicant.submitFromReviewPage(programName);
    await endSession(browser);
  });
})
