import {test, expect} from '../support/civiform_fixtures'
import {
  disableFeatureFlag,
  enableFeatureFlag,
  validateAccessibility,
  validateScreenshot,
} from '../support'
import {waitForPageJsLoad, waitForHtmxReady} from '../support/wait'

test.describe('Address checker', () => {
  test('All features run when correction and validation are enabled', async ({
    page,
  }) => {
    await enableFeatureFlag(page, 'ESRI_ADDRESS_CORRECTION_ENABLED')
    await enableFeatureFlag(
      page,
      'ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ENABLED',
    )

    await test.step('Navigate to address tools', async () => {
      await page.click('#debug-content-modal-button')
      await page.click('#additional-tools')
      await waitForPageJsLoad(page)
      await page.getByRole('heading', {name: 'Address Checker'}).isVisible()
      await page.getByRole('link', {name: 'Go to address tools'}).click()
      await waitForPageJsLoad(page)
    })

    await test.step('Address correction runs and loads results', async () => {
      await page
        .getByRole('textbox', {name: 'Address 1'})
        .fill('Address In Area')

      await page.getByRole('button', {name: 'Correct Address'}).click()

      await waitForHtmxReady(page)
      await expect(
        page.getByRole('heading', {name: 'Address Correction Results'}),
      ).toBeVisible()
    })

    await test.step('Address correction result runs and loads service area validation', async () => {
      await page.getByRole('textbox', {name: 'Latitude/Y'}).fill('100')
      await page.getByRole('textbox', {name: 'Longitude/X'}).fill('-100')
      await page.getByRole('textbox', {name: 'WellKnownId'}).fill('4326')

      await page
        .getByTestId('address-correction-results')
        .getByRole('button', {name: 'Check Service Area'})
        .last()
        .click()

      await waitForHtmxReady(page)
      await expect(
        page.getByRole('heading', {name: 'Validation Result: Failed'}),
      ).toBeVisible()
    })

    await test.step('Service area validation runs and loads results', async () => {
      await page
        .getByRole('form', {name: 'Search Service Area'})
        .getByRole('button', {name: 'Check Service Area'})
        .click()

      await waitForHtmxReady(page)
      await expect(
        page.getByRole('heading', {name: 'Validation Result: InArea'}),
      ).toBeVisible()
    })

    await validateAccessibility(page)
    await validateScreenshot(page, 'address-checker-enabled')
  })

  test('All features run when correction and validation are disabled', async ({
    page,
  }) => {
    await disableFeatureFlag(page, 'ESRI_ADDRESS_CORRECTION_ENABLED')
    await disableFeatureFlag(
      page,
      'ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ENABLED',
    )

    await test.step('Navigate to address tools', async () => {
      await page.click('#debug-content-modal-button')
      await page.click('#additional-tools')
      await waitForPageJsLoad(page)
      await page.getByRole('heading', {name: 'Address Checker'}).isVisible()
      await page.getByRole('link', {name: 'Go to address tools'}).click()
      await waitForPageJsLoad(page)
    })

    await test.step('Forms are disabled as expected', async () => {
      await expect(
        page.getByRole('heading', {name: 'Cannot show address'}),
      ).toBeVisible()
      await expect(
        page.getByRole('heading', {name: 'Cannot show service area'}),
      ).toBeVisible()
    })

    await validateAccessibility(page)
    await validateScreenshot(page, 'address-checker-disabled')
  })
})
