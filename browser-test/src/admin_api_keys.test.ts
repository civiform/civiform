import {
  startSession,
  loginAsProgramAdmin,
  loginAsAdmin,
  AdminApiKeys,
  AdminQuestions,
  AdminPrograms,
  endSession,
  logout,
  loginAsGuest,
  selectApplicantLanguage,
  ApplicantQuestions,
  userDisplayName,
} from './support'

describe('Managing API keys', () => {
  it('Creating a new API key displays its credentials', async () => {
    const { browser, page } = await startSession()

    await loginAsAdmin(page)
    const adminApiKeys = new AdminApiKeys(page)
    const adminPrograms = new AdminPrograms(page)

    const programName = 'API using program'
    const programDescription = 'This program uses the API.'
    await adminPrograms.addProgram(programName, programDescription, '', false)
    await adminPrograms.publishAllPrograms()

    const credentials = await adminApiKeys.createApiKey(
      'Test API key',
      '2022-01-31',
      "0.0.0.0/0",
      ['API-using-program']
    )

    expect(credentials).toBeInstanceOf("string")
  })
})
