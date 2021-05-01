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
    await adminQuestions.addEnumeratorQuestion('enumerator-ete-enumerator');
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
    await applicantQuestions.answerNameQuestion("first", "last");
    await applicantQuestions.saveAndContinue();

    // Put in two things in the enumerator question
    await applicantQuestions.addEnumeratorAnswer("first");
    await applicantQuestions.addEnumeratorAnswer("second");
    await applicantQuestions.saveAndContinue();

    // FIRST REPEATED ENTITY
    // Answer name
    await applicantQuestions.answerNameQuestion("first repeated", "last repeated");
    await applicantQuestions.saveAndContinue();

    // Put nothing in the first nested enumerator
    await applicantQuestions.saveAndContinue();

    // SECOND REPEATED ENTITY
    // Answer name
    await applicantQuestions.answerNameQuestion("second repeated", "second repeated");
    await applicantQuestions.saveAndContinue();

    // Put two things in the second nested enumerator
    await applicantQuestions.addEnumeratorAnswer("one");
    await applicantQuestions.addEnumeratorAnswer("two");
    await applicantQuestions.saveAndContinue();

    // Answer two nested repeated text questions
    await applicantQuestions.answerTextQuestion("hello");
    await applicantQuestions.saveAndContinue();
    await applicantQuestions.answerTextQuestion("world");
    await applicantQuestions.saveAndContinue();

    // Go back and delete some stuff
    await applicantQuestions.applyProgram(programName);
    expect(await page.innerHTML("#enumerator-fields")).toContain("first");
    expect(await page.innerHTML("#enumerator-fields")).toContain("second");
    await applicantQuestions.selectEnumeratorAnswerForDelete("first");
    await applicantQuestions.selectEnumeratorAnswerForDelete("second");
    await applicantQuestions.saveAndContinue();

    // Go back and see that it is empty
    await applicantQuestions.applyProgram(programName);
    expect(await page.innerHTML("#enumerator-fields")).not.toContain("first");
    expect(await page.innerHTML("#enumerator-fields")).not.toContain("second");

    await endSession(browser);
  });
})
