import { startSession, loginAsAdmin, AdminQuestions, AdminPrograms, endSession } from './support'

describe('Create program with repeater and repeated questions', () => {
  it('create program with repeater and repeated questions', async () => {
    const { browser, page } = await startSession();
    page.setDefaultTimeout(4000);

    await loginAsAdmin(page);
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    await adminQuestions.addAddressQuestion('apc-address');
    await adminQuestions.addNameQuestion('apc-name');
    await adminQuestions.addTextQuestion('apc-text');
    await adminQuestions.addRepeaterQuestion('apc-repeater');
    await adminQuestions.addRepeatedQuestion('apc-repeated', 'apc-repeater');


    const programName = 'apc program';
    await adminPrograms.addProgram(programName);
    await adminPrograms.editProgramBlock(programName, 'apc program description');

    // All non-repeated questions should be available in the question bank
    expect(await page.innerText('id=question-bank-questions')).toContain('apc-address');
    expect(await page.innerText('id=question-bank-questions')).toContain('apc-name');
    expect(await page.innerText('id=question-bank-questions')).toContain('apc-text');
    expect(await page.innerText('id=question-bank-questions')).toContain('apc-repeater');
    expect(await page.innerText('id=question-bank-questions')).not.toContain('apc-repeated');

    // Add a non-repeater question and the repeater option should go away
    await page.click('button:text("apc-name")');
    expect(await page.innerText('id=question-bank-questions')).not.toContain('apc-repeater');
    expect(await page.innerText('id=question-bank-questions')).not.toContain('apc-repeated');

    // Remove the non-repeater question and add a repeater question. All options should go away..
    await page.click('button:text("apc-name")');
    await page.click('button:text("apc-repeater")');
    expect(await page.innerText('id=question-bank-questions')).toBe('Question bank');

    // Create a repeated block. The repeated question should be the only option.
    await page.click('#create-repeated-block-button');
    expect(await page.innerText('id=question-bank-questions')).toContain('apc-repeated');

    await endSession(browser);
  })
})
