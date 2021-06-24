import { AdminPredicates, AdminPrograms, AdminQuestions, ApplicantQuestions, endSession, loginAsAdmin, loginAsTestUser, logout, selectApplicantLanguage, startSession, userDisplayName } from './support'

describe('create and edit predicates', () => {
  it('add a hide predicate', async () => {
    const { browser, page } = await startSession();

    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    // Add a program with two blocks
    await adminQuestions.addTextQuestion('predicate-q');
    await adminQuestions.addTextQuestion('other-q', 'desc', 'conditional question');

    const programName = 'create predicate';
    await adminPrograms.addProgram(programName);
    await adminPrograms.editProgramBlock(programName, 'first block', ['predicate-q']);
    await adminPrograms.addProgramBlock(programName, 'block with predicate', ['other-q']);

    // Edit predicate for second block
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Block 2');
    const adminPredicates = new AdminPredicates(page);
    await adminPredicates.addPredicate('predicate-q', 'hidden if', 'text', 'is equal to', 'hide me');
    await adminPredicates.expectVisibilityConditionEquals('Block 2 is hidden if predicate-q\'s text is equal to "hide me"');

    // Publish the program
    await adminPrograms.publishProgram(programName);

    // Switch to the applicant view and apply to the program
    await logout(page);
    await loginAsTestUser(page);
    await selectApplicantLanguage(page, 'English');
    const applicant = new ApplicantQuestions(page);
    await applicant.applyProgram(programName);

    // Initially fill out the first block so that the next block will be shown
    await applicant.answerTextQuestion('show me');
    await applicant.clickNext();

    // Fill out the second block
    await applicant.answerTextQuestion('will be hidden and not submitted');
    await applicant.clickNext();

    // We should be on the review page, with an answer to Block 2's question
    expect(await page.innerText('#application-summary')).toContain('conditional question');

    // Return to the first block and answer it so that the second block is hidden
    page.click('text=Edit'); // first block edit
    await applicant.answerTextQuestion('hide me');
    await applicant.clickNext();

    // We should be on the review page
    expect(await page.innerText('#application-summary')).not.toContain('conditional question');
    await applicant.submitFromReviewPage(programName);

    // Visit the program admin page and assert the hidden question does not show
    await logout(page);
    await loginAsAdmin(page);
    await adminPrograms.viewApplications(programName);
    await adminPrograms.viewApplicationForApplicant(userDisplayName());
    expect(await page.innerText('#application-view')).not.toContain('Block 2');

    await endSession(browser);
  })
})
