import {
  AdminApiKeys,
  AdminPrograms,
  createBrowserContext,
  loginAsAdmin,
} from './support'

describe('Managing API keys', () => {
  const ctx = createBrowserContext()

  it('Creates, views and retires new API key', async () => {
    await loginAsAdmin(ctx.page)
    const adminApiKeys = new AdminApiKeys(ctx.page)
    const adminPrograms = new AdminPrograms(ctx.page)

    const programName = 'API using program'
    const programDescription = 'This program uses the API.'
    await adminPrograms.addProgram(programName, programDescription, '', false)
    await adminPrograms.publishAllPrograms()

    const credentials = await adminApiKeys.createApiKey({
      name: 'Test API key',
      expiration: '2100-01-01',
      subnet: '0.0.0.0/0',
      programSlugs: ['api-using-program'],
    })

    expect(typeof credentials).toEqual('string')

    await adminApiKeys.expectApiKeyIsActive('Test API key')

    let apiResponse = await adminApiKeys.callCheckAuth(credentials)
    expect(apiResponse.status).toEqual(200)
    await adminApiKeys.expectKeyCallCount('test-api-key', 1)
    await adminApiKeys.expectLastCallIpAddressToBeSet('test-api-key')

    apiResponse = await adminApiKeys.callCheckAuth(credentials)
    expect(apiResponse.status).toEqual(200)
    await adminApiKeys.expectKeyCallCount('test-api-key', 2)

    await adminApiKeys.retireApiKey('test-api-key')
    await adminApiKeys.expectApiKeyIsRetired('Test API key')
  })
})
