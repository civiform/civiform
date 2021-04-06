import { startSession, loginAsAdmin, AdminQuestions, AdminPrograms, endSession } from './support'

describe('normal application flow', () => {
  // If this times out, a likely cause is the .click() calls in
  // support/admin_programs, which are called out as being asserts.
  jest.setTimeout(25000);
  it('all major steps', async () => {
    const { browser, page } = await startSession()
    page.setDefaultTimeout(1000);

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page);
    const adminPrograms = new AdminPrograms(page);

    await adminQuestions.addDropdownQuestion('ice cream', ['chocolate', 'banana', 'black raspberry'])
    await adminQuestions.addAddressQuestion('address-q');
    await adminQuestions.addNameQuestion('name-q');
    await adminQuestions.addNumberQuestion('number-q');
    await adminQuestions.addTextQuestion('text-q');

    // const adminProgram = new AdminPrograms(page)

    // const programName = 'A shiny new program'
    // await adminProgram.addProgram(programName)
    // await adminProgram.addProgramBlock(programName)

    var tableInnerText = await page.innerText('table');
    expect(tableInnerText).toContain('Edit Draft');

    await adminPrograms.addProgramWithQuestions(['address-q', 'name-q'], 'new program');

    await page.click('text=Questions')
    tableInnerText = await page.innerText('table');
    expect(tableInnerText).toContain('New Version');

    await endSession(browser);
  })
})
