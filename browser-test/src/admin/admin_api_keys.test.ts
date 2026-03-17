import {test, expect} from '../support/civiform_fixtures'
import {loginAsAdmin, validateScreenshot} from '../support'
import {ProgramVisibility} from '../support/admin_programs'

test.describe('Managing API keys', () => {
  test('Creates, views and retires new API key', async ({
    page,
    adminApiKeys,
    adminPrograms,
  }) => {
    const internalProgramName = 'Api using program'
    const internalProgramDescription = 'This program uses the API.'
    const externalProgramName = 'External Program'
    const externalProgramDescription =
      'This is an external program that should not appear in API key creation.'

    await loginAsAdmin(page)

    await test.step('Validate new api key page without any programs created', async () => {
      await adminApiKeys.gotoNewApiKeyPage()
      await validateScreenshot(page, 'new-api-key-no-programs')
    })

    await test.step('Add external program only', async () => {
      await adminPrograms.addExternalProgram(
        externalProgramName,
        externalProgramDescription,
        'https://external.gov',
        ProgramVisibility.PUBLIC,
      )
    })

    await test.step('Validate new api key page still shows no programs', async () => {
      await adminApiKeys.gotoNewApiKeyPage()
      await expect(page.getByText(externalProgramName)).toBeHidden()
      await expect(
        page.getByText(
          'You must create and publish a program before creating an API Key',
        ),
      ).toBeVisible()
    })

    await test.step('Add and publish default program', async () => {
      await adminPrograms.addProgram(internalProgramName, {
        description: internalProgramDescription,
      })
      await adminPrograms.publishAllDrafts()
    })

    await test.step('Validate external programs do not appear in API key form', async () => {
      await adminApiKeys.gotoNewApiKeyPage()

      await expect(
        page.getByRole('checkbox', {name: 'api-using-program'}),
      ).toBeVisible()
      await expect(
        page.getByRole('checkbox', {name: 'external-program'}),
      ).toBeHidden()
      await validateScreenshot(page, 'new-api-key-page-with-programs')
    })

    await test.step('Submit key creation request with invalid fields', async () => {
      await adminApiKeys.submitInvalidApiKeyRequest()

      await expect(
        page.getByRole('checkbox', {name: 'external-program'}),
      ).toBeHidden()
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
