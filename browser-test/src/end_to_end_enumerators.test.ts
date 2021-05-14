import { startSession, loginAsAdmin, loginAsGuest, AdminQuestions, AdminPrograms, ApplicantQuestions, selectApplicantLanguage, endSession } from './support'

describe('End to end enumerator test', () => {
  const programName = 'ete enumerator program';

  it('Create nested enumerator and repeated questions', async () => {
    const { browser, page } = await startSession();
    page.setDefaultTimeout(4000);

    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    await adminQuestions.addNameQuestion('enumerator-ete-name');
    await adminQuestions.addEnumeratorQuestion('enumerator-ete-enumerator', 'desc', 'enumerator-ete-question');
    await adminQuestions.addNameQuestion('enumerator-ete-enumerator-name', 'desc', 'text', 'helptext', 'enumerator-ete-enumerator');
    await adminQuestions.addEnumeratorQuestion('enumerator-ete-enumerator-enumerator', 'desc', 'text', 'helptext', 'enumerator-ete-enumerator');
    await adminQuestions.addTextQuestion('enumerator-ete-enumerator-enumerator-text', 'desc', 'text', 'helptext', 'enumerator-ete-enumerator-enumerator');

    await adminPrograms.addProgram(programName);
    await adminPrograms.editProgramBlock(programName, 'ete enumerator program description');

    // All non-repeated questions should be available in the question bank.
    expect(await page.innerText('id=question-bank-questions')).toContain('enumerator-ete-name');
    expect(await page.innerText('id=question-bank-questions')).toContain('enumerator-ete-enumerator');

    // Add an enumerator question. All options should go away.
    await page.click('button:text("enumerator-ete-enumerator")');
    expect(await page.innerText('id=question-bank-questions')).toBe('Question bank');

    // Remove the enumerator question and add a non-enumerator question, and the enumerator option should not be in the bank.
    await page.click('button:text("enumerator-ete-enumerator")');
    await page.click('button:text("enumerator-ete-name")');
    expect(await page.innerText('id=question-bank-questions')).not.toContain('enumerator-ete-enumerator');

    // Create a new block with the first enumerator question, and then create a repeated block. The repeated questions should be the only option.
    await page.click('#add-block-button');
    await page.click('button:text("enumerator-ete-enumerator")');
    await page.click('#create-repeated-block-button');
    expect(await page.innerText('id=question-bank-questions')).toContain('enumerator-ete-enumerator-name');
    expect(await page.innerText('id=question-bank-questions')).toContain('enumerator-ete-enumerator-enumerator');

    // Create the rest of the program.
    // Add repeated name question
    await page.click('button:text("enumerator-ete-enumerator-name")');

    // Create another repeated block and add the nested enumerator question
    await page.click('p:text("Block 2")');
    await page.click('#create-repeated-block-button');
    await page.click('button:text("enumerator-ete-enumerator-enumerator")');

    // Create a nested repeated block and add the nested text question
    await page.click('#create-repeated-block-button');
    await page.click('button:text("enumerator-ete-enumerator-enumerator-text")');

    // Publish!
    await adminPrograms.publishProgram(programName);

    await endSession(browser);
  });


  it('Applicant can fill in lots of blocks, and then go back and delete some repeated entities', async () => {
    const { browser, page } = await startSession();
    await loginAsGuest(page);
    await selectApplicantLanguage(page, 'English');
    const applicantQuestions = new ApplicantQuestions(page);
    await applicantQuestions.applyProgram(programName);

    // Fill in name question
    await applicantQuestions.answerNameQuestion("first name", "last name");
    await applicantQuestions.saveAndContinue();

    // Put in two things in the enumerator question
    await applicantQuestions.addEnumeratorAnswer("enum one");
    await applicantQuestions.addEnumeratorAnswer("enum two");
    await applicantQuestions.saveAndContinue();

    // FIRST REPEATED ENTITY
    // Answer name
    await applicantQuestions.answerNameQuestion("enum one first", "enum one last");
    await applicantQuestions.saveAndContinue();

    // Put one thing in the nested enumerator for enum one
    await applicantQuestions.addEnumeratorAnswer("enum one's first thing");
    await applicantQuestions.saveAndContinue();

    // Answer the nested repeated question
    await applicantQuestions.answerTextQuestion("hello world");
    await applicantQuestions.saveAndContinue()

    // SECOND REPEATED ENTITY
    // Answer name
    await applicantQuestions.answerNameQuestion("enum two first", "enum two last");
    await applicantQuestions.saveAndContinue();

    // Put two things in the nested enumerator for enum two
    await applicantQuestions.addEnumeratorAnswer("enum two's first thing");
    await applicantQuestions.addEnumeratorAnswer("enum two's second thing");
    await applicantQuestions.saveAndContinue();

    // Answer two nested repeated text questions
    await applicantQuestions.answerTextQuestion("hello");
    await applicantQuestions.saveAndContinue();
    await applicantQuestions.answerTextQuestion("world");
    await applicantQuestions.saveAndContinue();

    // Make sure the enumerator answers are in the review page
    expect(await page.innerText("#application-summary")).toContain("first name");
    expect(await page.innerText("#application-summary")).toContain("last name");
    expect(await page.innerText("#application-summary")).toContain("enum one first");
    expect(await page.innerText("#application-summary")).toContain("enum one last");
    expect(await page.innerText("#application-summary")).toContain("enum two first");
    expect(await page.innerText("#application-summary")).toContain("enum two last");
    expect(await page.innerText("#application-summary")).toContain("enum one's first thing");
    expect(await page.innerText("#application-summary")).toContain("hello world");
    expect(await page.innerText("#application-summary")).toContain("enum two's first thing");
    expect(await page.innerText("#application-summary")).toContain("enum two's second thing");
    expect(await page.innerText("#application-summary")).toContain("hello");
    expect(await page.innerText("#application-summary")).toContain("world");

    // Go back to delete enumerator answers
    await page.click('.cf-applicant-summary-row:has(div:has-text("enumerator-ete-question")) a:has-text("Edit")');
    await applicantQuestions.deleteEnumeratorEntity("enum one");
    await applicantQuestions.deleteEnumeratorEntity("enum two");
    await applicantQuestions.saveAndContinue();


    // Make sure there are no enumerators or repeated things in the review page
    expect(await page.innerText("#application-summary")).toContain("first name");
    expect(await page.innerText("#application-summary")).toContain("last name");
    expect(await page.innerText("#application-summary")).not.toContain("enum one first");
    expect(await page.innerText("#application-summary")).not.toContain("enum one last");
    expect(await page.innerText("#application-summary")).not.toContain("enum two first");
    expect(await page.innerText("#application-summary")).not.toContain("enum two last");
    expect(await page.innerText("#application-summary")).not.toContain("thing one");
    expect(await page.innerText("#application-summary")).not.toContain("thing two");
    expect(await page.innerText("#application-summary")).not.toContain("hello");
    expect(await page.innerText("#application-summary")).not.toContain("world");


    // Go back and add an enumerator answer.
    await page.click('.cf-applicant-summary-row:has(div:has-text("enumerator-ete-question")) a:has-text("Edit")');
    await applicantQuestions.addEnumeratorAnswer("enum three");
    await applicantQuestions.saveAndContinue();
    await applicantQuestions.answerNameQuestion("enum three first", "enum three last");
    await applicantQuestions.saveAndContinue();
    await applicantQuestions.saveAndContinue();

    // Make sure there are no enumerators or repeated things in the review page
    expect(await page.innerText("#application-summary")).toContain("first name");
    expect(await page.innerText("#application-summary")).toContain("last name");
    expect(await page.innerText("#application-summary")).toContain("enum three first");
    expect(await page.innerText("#application-summary")).toContain("enum three last");
    expect(await page.innerText("#application-summary")).not.toContain("enum one first");
    expect(await page.innerText("#application-summary")).not.toContain("enum one last");
    expect(await page.innerText("#application-summary")).not.toContain("enum two first");
    expect(await page.innerText("#application-summary")).not.toContain("enum two last");
    expect(await page.innerText("#application-summary")).not.toContain("thing one");
    expect(await page.innerText("#application-summary")).not.toContain("thing two");
    expect(await page.innerText("#application-summary")).not.toContain("hello");
    expect(await page.innerText("#application-summary")).not.toContain("world");

    await endSession(browser);
  });
})
