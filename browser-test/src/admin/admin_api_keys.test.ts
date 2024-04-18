import {test, expect} from '../support/civiform_fixtures'
import {loginAsAdmin, validateScreenshot} from '../support'

test.describe('Managing API keys', {tag: ['@uses-fixtures']}, () => {
  test('Creates, views and retires new API key', async ({
    page,
    adminApiKeys,
    adminPrograms,
  }) => {
    const programName = 'Api using program'
    const programDescription = 'This program uses the API.'

    await loginAsAdmin(page)

    await test.step('Validate new api key page without any programs created', async () => {
      await adminApiKeys.gotoNewApiKeyPage()
      await validateScreenshot(page, 'new-api-key-no-programs')
    })

    await test.step('Add and publish program', async () => {
      await adminPrograms.addProgram(
        programName,
        programDescription,
        'https://usa.gov',
      )

      await adminPrograms.publishAllDrafts()
    })

    await test.step('Validate new api key page', async () => {
      await adminApiKeys.gotoNewApiKeyPage()
      await validateScreenshot(page, 'new-api-key-page')
    })

    await test.step('Submit key creation request with invalid fields', async () => {
      await adminApiKeys.submitInvalidApiKeyRequest()
      await validateScreenshot(page, 'api-key-index-page-invalid')
    })

    const credentials = await test.step('Create new api key', async () => {
      const credentials = await adminApiKeys.createApiKey({
        name: 'Test API key',
        expiration: '2100-01-01',
        subnet: '0.0.0.0/0,1.1.1.1/0',
        programSlugs: ['api-using-program'],
      })

      expect(typeof credentials).toEqual('string')
      await adminApiKeys.expectApiKeyIsActive('Test API key')
      await validateScreenshot(page, 'api-key-index-page')

      return credentials
    })

    await test.step('Check new api key', async () => {
      let apiResponse = await adminApiKeys.callCheckAuth(credentials)
      await expect(apiResponse).toBeOK()
      await adminApiKeys.expectKeyCallCount('test-api-key', 1)
      await adminApiKeys.expectLastCallIpAddressToBeSet('test-api-key')

      apiResponse = await adminApiKeys.callCheckAuth(credentials)
      await expect(apiResponse).toBeOK()
      await adminApiKeys.expectKeyCallCount('test-api-key', 2)
    })

    await test.step('Retire api key', async () => {
      await adminApiKeys.retireApiKey('test-api-key')
      await validateScreenshot(page, 'api-key-index-page-no-active-keys')
      await adminApiKeys.expectApiKeyIsRetired('Test API key')
    })
  })
})
