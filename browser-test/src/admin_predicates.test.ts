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
  });

  it('every right hand type evaluates correctly', async () => {
  	const { browser, page } = await startSession();

  	await loginAsAdmin(page);
  	const adminQuestions = new AdminQuestions();
  	const adminPrograms = new AdminPrograms();

  	// DATE, STRING, LONG, LIST_OF_STRINGS, LIST_OF_LONGS
    await adminQuestions.addNameQuestion('string');
    await adminQuestions.addTextQuestion('list of strings');
    await adminQuestions.addNumberQuestion('long');
    await adminQuestions.addNumberQuestion('list of longs');
    await adminQuestions.addDateQuestion('date');
    await adminQuestions.addCheckboxQuestion('both sides are lists', ['a', 'b', 'c']);
    await adminQuestions.addTextQuestion('depends on previous');

    const programName = 'test all predicate types';
    await adminPrograms.addProgram(programName);
    await adminPrograms.editProgramBlock(programName, 'string', ['string']);
    await adminPrograms.addProgramBlock(programName, 'list of strings', ['list of strings']);
    await adminPrograms.addProgramBlock(programName, 'long', ['long']);
    await adminPrograms.addProgramBlock(programName, 'list of longs', ['list of longs']);
    await adminPrograms.addProgramBlock(programName, 'date', ['date']);
    await adminPrograms.addProgramBlock(programName, 'two lists', ['both sides are lists']);
    await adminPrograms.addProgramBlock(programName, 'last', ['depends on previous']);

    // Simple string predicate
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Block 2');
    const adminPredicates = new AdminPredicates(page);
    await adminPredicates.addPredicate('string', 'shown if', 'first name', 'is not equal to', 'hidden');

    // Single string one of a list of strings
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Block 3');
    const adminPredicates = new AdminPredicates(page);
    await adminPredicates.addPredicate('list of strings', 'shown if', 'text', 'is one of', 'blue,green');

    // Simple long predicate
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Block 4');
    const adminPredicates = new AdminPredicates(page);
    await adminPredicates.addPredicate('long', 'shown if', 'number', 'is equal to', '42');

    // Long is one of a list of longs
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Block 5');
    const adminPredicates = new AdminPredicates(page);
    await adminPredicates.addPredicate('list of longs', 'shown if', 'number', 'is not one of', '100,200');

    // Date predicate
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Block 6');
    const adminPredicates = new AdminPredicates(page);
    await adminPredicates.addPredicate('date', 'shown if', 'date', 'is earlier than', '2021-04-31');

    // Lists of strings on both sides
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Block 7');
    const adminPredicates = new AdminPredicates(page);
    await adminPredicates.addPredicate('both sides are lists', 'shown if', 'selections', 'contains any of', 'a,c');

  });
})
