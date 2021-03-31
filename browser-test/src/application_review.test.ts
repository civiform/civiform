import { startSession, loginAsAdmin, AdminQuestions } from './support'

describe('normal application flow', () => {
  it('all major steps', async () => {
    const { page } = await startSession()

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)

    await adminQuestions.addNameQuestion('What is your name?', 'applicant.name')
  })
})
