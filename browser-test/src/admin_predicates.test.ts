import { AdminPredicates, AdminPrograms, AdminQuestions, endSession, loginAsAdmin, logout, startSession } from './support'

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
  })
})
