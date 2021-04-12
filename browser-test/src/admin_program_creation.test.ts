import { startSession, loginAsAdmin, AdminQuestions, endSession } from './support'

describe('create dropdown question with options', () => {
  it('add remove buttons work correctly', async () => {
    const { browser, page } = await startSession();

    await loginAsAdmin(page);

    const adminQuestions = new AdminQuestions(page);
    await page.click('text=Questions');
    await page.click('#create-question-button');
    await page.click('#create-dropdown-question');

    // Fill in basic info
    const questionName = 'favorite ice cream';
    await page.fill('text="Name"', questionName);
    await page.fill('text=Description', 'description');
    await page.fill('text=Question Text', 'questionText');
    await page.fill('text=Question help text', 'helpText');

    // Add three options
    await page.click('#add-new-option');
    await page.fill('input:above(#add-new-option)', 'chocolate');
    await page.click('#add-new-option');
    await page.fill('input:above(#add-new-option)', 'vanilla');
    await page.click('#add-new-option');
    await page.fill('input:above(#add-new-option)', 'strawberry');

    // Assert there are three options present
    var questionSettingsDiv = await page.innerHTML('#question-settings');
    expect(questionSettingsDiv.match(/<input/g)).toHaveLength(3);

    // Remove first option - use :visible to not select the hidden template
    await page.click('button:text("Remove"):visible')

    // Assert there are only two options now
    questionSettingsDiv = await page.innerHTML('#question-settings');
    expect(questionSettingsDiv.match(/<input/g)).toHaveLength(2);

    // Submit the form, then edit that question again
    await page.click('text=Create');
    await adminQuestions.expectDraftQuestionExist(questionName);

    // Edit the question
    await adminQuestions.gotoQuestionEditPage(questionName);
    var questionSettingsDiv = await page.innerHTML('#question-settings');
    expect(questionSettingsDiv.match(/<input/g)).toHaveLength(2);

    await endSession(browser);
  })
})

