import {test, expect} from '../support/civiform_fixtures'
import {
  disableFeatureFlag,
  enableFeatureFlag,
  isLocalDevEnvironment,
  validateAccessibility,
  validateScreenshot,
} from '../support'
import {waitForPageJsLoad, waitForHtmxReady} from '../support/wait'

test.describe('Address checker', () => {
  if (isLocalDevEnvironment()) {
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
        await expect(
          page.getByRole('heading', {name: 'Address Tools'}),
        ).toBeVisible()
        await page.getByRole('link', {name: 'Go to address tools'}).click()
        await waitForPageJsLoad(page)
        await expect(
          page.getByRole('heading', {name: 'Address Checker', level: 1}),
        ).toBeVisible()
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
        await page.getByRole('spinbutton', {name: 'Well Known ID'}).fill('4326')

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
  }

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
      await expect(
        page.getByRole('heading', {name: 'Address Tools'}),
      ).toBeVisible()
      await page.getByRole('link', {name: 'Go to address tools'}).click()
      await waitForPageJsLoad(page)
      await expect(
        page.getByRole('heading', {name: 'Address Checker', level: 1}),
      ).toBeVisible()
    })

    await test.step('Settings show both features disabled', async () => {
      const settings = page.locator('section', {
        has: page.getByRole('heading', {name: 'Current Address Settings'}),
      })

      await expect(settings).toContainText('Address Correction: Disabled')
      await expect(settings).toContainText('Address Validation: Disabled')
    })

    await test.step('Forms are hidden', async () => {
      await expect(
        page.getByRole('form', {name: 'Find Correction Results'}),
      ).toHaveCount(0)
      await expect(
        page.getByRole('form', {name: 'Search Service Area'}),
      ).toHaveCount(0)
      await expect(
        page.getByRole('button', {name: 'Correct Address'}),
      ).toHaveCount(0)
      await expect(
        page.getByRole('button', {name: 'Check Service Area'}),
      ).toHaveCount(0)
    })

    await test.step('Alerts explain why each tool is hidden', async () => {
      const correctionAlert = page
        .getByRole('alert')
        .filter({hasText: 'Cannot show address correction tool'})

      await expect(correctionAlert).toBeVisible()
      await expect(correctionAlert).toContainText(
        'ESRI_FIND_ADDRESS_CANDIDATES_URLS has no endpoints',
      )
      await expect(correctionAlert).toContainText(
        'ESRI_ADDRESS_CORRECTION_ENABLED is not enabled',
      )

      const serviceAreaAlert = page
        .getByRole('alert')
        .filter({hasText: 'Cannot show service area validation tool'})

      await expect(serviceAreaAlert).toBeVisible()
      await expect(serviceAreaAlert).toContainText(
        'ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS has no endpoints',
      )
      await expect(serviceAreaAlert).toContainText(
        'ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ENABLED is not enabled',
      )
    })

    await validateAccessibility(page)
  })
})
