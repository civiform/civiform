import { startSession, loginAsAdmin, AdminQuestions, AdminPrograms, endSession } from './support'

describe('normal application flow', () => {
  jest.setTimeout(200000);
  it('all major steps', async () => {
    const { browser, page } = await startSession()
    page.setDefaultTimeout(1000);

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    await adminQuestions.addDropdownQuestion('ice cream flavor', ['chocolate', 'banana', 'black raspberry']);
    await adminQuestions.addAddressQuestion('address-q');
    await adminQuestions.addNameQuestion('name-q');
    await adminQuestions.addNumberQuestion('number-q');
    await adminQuestions.addTextQuestion('text-q');

    await adminPrograms.addAndPublishProgramWithQuestions(['address-q', 'name-q'], 'new program');

    await adminQuestions.expectActiveQuestionExist('address-q');
    await adminQuestions.expectActiveQuestionExist('name-q');

    await endSession(browser);
  })
})
