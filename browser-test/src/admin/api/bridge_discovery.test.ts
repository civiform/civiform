import {expect, test} from '../../support/civiform_fixtures'
import {
  enableFeatureFlag,
  isLocalDevEnvironment,
  loginAsAdmin,
  validateAccessibility,
  validateScreenshot,
} from '../../support'
import {MOCK_WEB_SERVICES_URL} from '../../support/config'

test.describe('api bridge discovery', () => {
  test.skip(!isLocalDevEnvironment(), 'Requires mock-web-services')

  test.beforeEach(async ({page, seeding}) => {
    await enableFeatureFlag(page, 'api_bridge_enabled')
    await seeding.seedProgramsAndCategories()
    await page.goto('/')
    await loginAsAdmin(page)
  })

  test('export a program', async ({page, bridgeDiscoveryPage}) => {
    const hostUrl = `${MOCK_WEB_SERVICES_URL}/api-bridge`
    const urlPath = '/bridge/success'

    await test.step('navigate to bridge page', async () => {
      await bridgeDiscoveryPage.clickPrimaryNavSubMenuLink(
        'API',
        'Bridge Discovery',
      )
    })

    await test.step('verify page', async () => {
      await expect(bridgeDiscoveryPage.getPageHeading()).toBeVisible()

      await expect(bridgeDiscoveryPage.getSelectedTab()).toBeVisible()
      await expect(bridgeDiscoveryPage.getSelectedTab()).toHaveAttribute(
        'aria-current',
        'page',
      )
    })

    await test.step('attempt to submit with an empty url discovery process', async () => {
      await bridgeDiscoveryPage.fillUrl('')
      await bridgeDiscoveryPage.clickSearchButton()

      await expect(bridgeDiscoveryPage.getErrorAlert()).toBeVisible()
      await expect(bridgeDiscoveryPage.getErrorAlert()).toContainText(
        'URL is required',
      )

      await expect(
        bridgeDiscoveryPage.getEndpointHeading(urlPath),
      ).not.toBeAttached()
    })

    await test.step('attempt to submit with an invalid url discovery process', async () => {
      await bridgeDiscoveryPage.fillUrl('blah-de-blah')
      await bridgeDiscoveryPage.clickSearchButton()

      await expect(bridgeDiscoveryPage.getErrorAlert()).toBeVisible()
      await expect(bridgeDiscoveryPage.getErrorAlert()).toContainText(
        'Please enter a valid URL',
      )

      await expect(
        bridgeDiscoveryPage.getEndpointHeading(urlPath),
      ).not.toBeAttached()
    })

    await test.step('run successful discovery process', async () => {
      await bridgeDiscoveryPage.fillUrl(hostUrl)
      await bridgeDiscoveryPage.clickSearchButton()

      await expect(bridgeDiscoveryPage.getErrorAlert()).not.toBeAttached()
      await expect(
        bridgeDiscoveryPage.getEndpointHeading(urlPath),
      ).toBeAttached()

      await expect(
        bridgeDiscoveryPage.getTable(urlPath, 'Input Fields'),
      ).toBeVisible()
      await expect(
        bridgeDiscoveryPage.getTable(urlPath, 'Input Fields').getByRole('row'),
      ).toHaveCount(3)

      await expect(
        bridgeDiscoveryPage.getTable(urlPath, 'Output Fields'),
      ).toBeVisible()
      await expect(
        bridgeDiscoveryPage.getTable(urlPath, 'Output Fields').getByRole('row'),
      ).toHaveCount(3)

      await validateScreenshot(page.locator('main'), 'api-bridge-discovery')
      await validateAccessibility(page)
    })

    await test.step('add bridge', async () => {
      await bridgeDiscoveryPage.clickAddButton(urlPath)
      await expect(bridgeDiscoveryPage.getSaveSuccessfulAlert()).toContainText(
        'Saved successfully',
      )
    })
  })
})
