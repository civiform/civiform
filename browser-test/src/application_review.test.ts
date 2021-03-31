import { startSession, loginAsAdmin, AdminPrograms, AdminQuestions, endSession } from './support'

describe('normal application flow', () => {
  it('all major steps', async () => {
    const { browser, page } = await startSession()

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)

    await adminQuestions.addDropdownQuestion('ice cream', ['chocolate', 'banana', 'black raspberry'])
    await adminQuestions.addAddressQuestion('What is your address?')
    await adminQuestions.addNameQuestion('What is your name?')
    await adminQuestions.addNumberQuestion('Give me a number')
    await adminQuestions.addTextQuestion('What is your favorite color?')

    const adminProgram = new AdminPrograms(page)

    await adminProgram.addProgram('A shiny new program')

    await endSession(browser)
  })
})
