import { startSession, loginAsAdmin, AdminQuestions, AdminPrograms, endSession } from './support'

describe('normal application flow', () => {
  // If this times out, a likely cause is the .click() calls in
  // support/admin_programs, which are called out as being asserts.
  jest.setTimeout(25000);
  it('all major steps', async () => {
    const { browser, page } = await startSession()

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    await adminQuestions.addDropdownQuestion('ice cream', ['chocolate', 'banana', 'black raspberry'])
    await adminQuestions.addAddressQuestion('What is your address?')
    await adminQuestions.addNameQuestion('What is your name?')
    await adminQuestions.addNumberQuestion('Give me a number')
    await adminQuestions.addRadioButtonQuestion('Favorite tree?', ['oak', 'pine', 'sycamore'])
    await adminQuestions.addTextQuestion('What is your favorite color?')

    var tableInnerText = await page.innerText('table');
    expect(tableInnerText).toContain('Edit Draft');

    await adminPrograms.addProgram(['address', 'name'], 'program');

    await page.click('text=Questions')
    tableInnerText = await page.innerText('table');
    expect(tableInnerText).toContain('New Version');

    await endSession(browser);
  })
})
