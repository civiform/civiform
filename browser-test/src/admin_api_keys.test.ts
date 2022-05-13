import {
  startSession,
  loginAsAdmin,
  AdminApiKeys,
  AdminPrograms,
} from './support'

describe('Managing API keys', () => {
  it('Creates, views and retires new API key', async () => {
    const { browser, page } = await startSession()

    await loginAsAdmin(page)
    const adminApiKeys = new AdminApiKeys(page)
    const adminPrograms = new AdminPrograms(page)

    const programName = 'API using program'
    const programDescription = 'This program uses the API.'
    await adminPrograms.addProgram(programName, programDescription, '', false)
    await adminPrograms.publishAllPrograms()

    const credentials = await adminApiKeys.createApiKey({
      name: 'Test API key',
      expiration: '2022-01-31',
      subnet: '8.8.8.8/32',
      programSlugs: ['api-using-program'],
    })

    expect(typeof credentials).toEqual('string')

    await adminApiKeys.expectApiKeyIsActive('Test API key')
    await adminApiKeys.retireApiKey('test-api-key')
    await adminApiKeys.expectApiKeyIsRetired('Test API key')
  })
})
