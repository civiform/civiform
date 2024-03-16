import {test, expect} from '../fixtures/custom_fixture'
import {loginAsAdmin, validateScreenshot} from '../support'

test.describe('Managing API keys', {tag: ['@migrated']}, () => {
  test('Creates, views and retires new API key', async ({
    page,
    adminApiKeys,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Api using program'
    const programDescription = 'This program uses the API.'
    await adminPrograms.addProgram(
      programName,
      programDescription,
      'https://usa.gov',
    )
    await adminPrograms.publishAllDrafts()
    await adminApiKeys.gotoNewApiKeyPage()
    await validateScreenshot(page, 'new-api-key-page')

    const credentials = await adminApiKeys.createApiKey({
      name: 'Test API key',
      expiration: '2100-01-01',
      subnet: '0.0.0.0/0,1.1.1.1/0',
      programSlugs: ['api-using-program'],
    })

    expect(typeof credentials).toEqual('string')

    await adminApiKeys.expectApiKeyIsActive('Test API key')
    await validateScreenshot(page, 'api-key-index-page')

    let apiResponse = await adminApiKeys.callCheckAuth(credentials)
    expect(apiResponse.status()).toEqual(200)
    await adminApiKeys.expectKeyCallCount('test-api-key', 1)
    await adminApiKeys.expectLastCallIpAddressToBeSet('test-api-key')

    apiResponse = await adminApiKeys.callCheckAuth(credentials)
    expect(apiResponse.status()).toEqual(200)
    await adminApiKeys.expectKeyCallCount('test-api-key', 2)

    await adminApiKeys.retireApiKey('test-api-key')
    await validateScreenshot(page, 'api-key-index-page-no-active-keys')
    await adminApiKeys.expectApiKeyIsRetired('Test API key')
  })
})
