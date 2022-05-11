import {
  startSession,
  loginAsAdmin,
  AdminApiKeys,
  AdminPrograms,
} from './support'

describe('Managing API keys', () => {
  it('Creating a new API key, viewing, and retiring it', async () => {
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
      "8.8.8.8/32",
      ['api-using-program']
    )

    expect(typeof credentials).toEqual("string")

    await adminApiKeys.expectApiKeyIsActive('Test API key')
    await adminApiKeys.retireApiKey('test-api-key')
    await adminApiKeys.expectApiKeyIsRetired('Test API key')
  })
})
