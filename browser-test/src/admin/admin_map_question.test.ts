import {expect, test} from '../support/civiform_fixtures'
import {loginAsAdmin, waitForPageJsLoad} from '../support'

test.describe('Create and edit map question', () => {
  test('Map question creation', async ({page, adminQuestions}) => {
    await test.step('Navigate to map question creation form', async () => {
      await loginAsAdmin(page)
      await adminQuestions.gotoAdminQuestionsPage()
      await page.click('#create-question-button')
      await page.click('#create-map-question')
      await waitForPageJsLoad(page)
    })

    await test.step('Verify form validation', async () => {
      // Verify GeoJSON endpoint is required
      const geoJsonInput = page.locator('input[name="geoJsonEndpoint"]')
      await expect(geoJsonInput).toHaveAttribute('aria-required', 'true')
    })

    await test.step('Fill in basic question information', async () => {
      await page.fill(
        '#question-text-textarea',
        'Select your preferred location',
      )
      await page.fill('#question-name-input', 'preferred-location')
      await page.fill(
        '#question-help-text-textarea',
        'Choose a location from the map',
      )
    })

    await test.step('Add GeoJSON endpoint and trigger change event', async () => {
      const geoJsonInput = page.locator('input[name="geoJsonEndpoint"]')
      const htmxResponsePromise = page.waitForResponse(
        '**/admin/geoJson/hx/getData',
      )

      await page.fill(
        'input[name="geoJsonEndpoint"]',
        'http://localhost:8000/geojson/data',
      )
      await geoJsonInput.dispatchEvent('change')
      await htmxResponsePromise
    })

    await test.step('Verify and configure max location selections', async () => {
      await expect(
        page.locator('label[for="maxLocationSelections"]'),
      ).toContainText('Maximum location selections')

      const maxLocationInput = page.locator('#maxLocationSelections')
      await expect(maxLocationInput).toHaveAttribute('type', 'number')
      await expect(maxLocationInput).toBeVisible()

      await page.fill('#maxLocationSelections', '5')
      await expect(maxLocationInput).toHaveValue('5')
    })

    await test.step('Verify default location settings', async () => {
      // Verify labels and dropdowns for location name, address, and details URL
      await expect(page.locator('label[for="locationName.key"]')).toContainText(
        'Location name',
      )
      await expect(
        page.locator('label[for="locationAddress.key"]'),
      ).toContainText('Location address')
      await expect(
        page.locator('label[for="locationDetailsUrl.key"]'),
      ).toContainText('Location details URL')

      await expect(page.locator('#locationName\\.key')).toBeVisible()
      await expect(page.locator('#locationAddress\\.key')).toBeVisible()
      await expect(page.locator('#locationDetailsUrl\\.key')).toBeVisible()

      // Verify all location dropdowns have both name and address options
      const locationDropdowns = [
        '#locationName\\.key',
        '#locationAddress\\.key',
        '#locationDetailsUrl\\.key',
      ]
      for (const dropdown of locationDropdowns) {
        await expect(
          page.locator(`${dropdown} option[value="name"]`),
        ).toBeAttached()
        await expect(
          page.locator(`${dropdown} option[value="address"]`),
        ).toBeAttached()
      }
    })

    await test.step('Verify filters', async () => {
      await expect(
        page.locator('#geoJsonOutput').getByText('Filters', {exact: true}),
      ).toBeVisible()
      await expect(
        page
          .locator('#geoJsonOutput')
          .getByText(
            'Select up to three filters to make available to applicants.',
          ),
      ).toBeVisible()

      for (let i = 0; i < 3; i++) {
        await expect(
          page.locator(`select[name="filters[${i}].key"]`),
        ).toBeVisible()
        await expect(
          page.locator(`input[name="filters[${i}].displayName"]`),
        ).toBeVisible()

        await expect(
          page.locator(`select[name="filters[${i}].key"] option[value="name"]`),
        ).toBeAttached()
        await expect(
          page.locator(
            `select[name="filters[${i}].key"] option[value="address"]`,
          ),
        ).toBeAttached()
      }
    })
  })
})
