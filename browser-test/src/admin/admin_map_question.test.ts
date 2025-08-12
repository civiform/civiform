import {expect, test} from '../support/civiform_fixtures'
import {loginAsAdmin, validateScreenshot, waitForPageJsLoad} from '../support'

test.describe('Create and edit map question', () => {
  test('Map question form', async ({page, adminQuestions}) => {
    await test.step('Navigate to map question creation form', async () => {
      await loginAsAdmin(page)
      await adminQuestions.gotoAdminQuestionsPage()
      await page.click('#create-question-button')
      await page.click('#create-map-question')
      await waitForPageJsLoad(page)
    })

    await test.step('Verify form validation and HTMX behavior', async () => {
      const geoJsonInput = page.getByLabel('GeoJSON endpoint')
      await expect(geoJsonInput).toHaveAttribute('aria-required', 'true')

      const htmxResponsePromise = page.waitForResponse(
        '**/admin/geoJson/hx/getData',
      )
      await geoJsonInput.fill('http://mock-web-services:8000/geojson/data')
      await geoJsonInput.dispatchEvent('change')
      await htmxResponsePromise
    })

    await test.step('Verify question settings', async () => {
      const maxLocationInput = page.getByLabel('Maximum location selections')
      await expect(maxLocationInput).toHaveAttribute('type', 'number')
      await expect(maxLocationInput).toBeVisible()
      await maxLocationInput.fill('3')
      await expect(maxLocationInput).toHaveValue('3')

      const locationNameSelect = page.getByLabel('Location name')
      const locationAddressSelect = page.getByLabel('Location address')
      const locationDetailsUrlSelect = page.getByLabel('Location details URL')

      await expect(locationNameSelect).toBeVisible()
      await expect(locationAddressSelect).toBeVisible()
      await expect(locationDetailsUrlSelect).toBeVisible()

      for (const dropdown of [
        locationNameSelect,
        locationAddressSelect,
        locationDetailsUrlSelect,
      ]) {
        await expect(dropdown.locator('option[value="name"]')).toBeAttached()
        await expect(dropdown.locator('option[value="address"]')).toBeAttached()
      }

      await expect(page.getByText('Filters', {exact: true})).toBeVisible()
      await expect(
        page.getByText(
          'Select up to three filters to make available to applicants.',
        ),
      ).toBeVisible()

      for (let i = 0; i < 3; i++) {
        const filterKeySelect = page.locator('select[name^="filters["]').nth(i)
        const filterDisplayInput = page
          .locator('input[name*="displayName"]')
          .nth(i)

        await expect(filterKeySelect).toBeVisible()
        await expect(filterDisplayInput).toBeVisible()
        await expect(
          filterKeySelect.locator('option[value="name"]'),
        ).toBeAttached()
        await expect(
          filterKeySelect.locator('option[value="address"]'),
        ).toBeAttached()
      }
    })

    await test.step('Screenshot of map question', async () => {
      await validateScreenshot(
        page.locator('#question-settings'),
        'map-question-settings',
      )
    })
  })

  test('Edit map question', async ({page, adminQuestions}) => {
    await test.step('Create map question', async () => {
      await loginAsAdmin(page)
      await adminQuestions.addMapQuestion({
        questionName: 'editable-map-question',
        questionText: 'Pick a location',
        maxLocationSelections: '2',
        locationNameKey: 'name',
        locationAddressKey: 'address',
      })
    })

    await test.step('Edit question and verify settings', async () => {
      await adminQuestions.gotoQuestionEditPage('editable-map-question')
      await waitForPageJsLoad(page)

      await expect(page.getByLabel('Maximum location selections')).toHaveValue(
        '2',
      )
      await expect(page.getByLabel('Location name')).toHaveValue('name')
      await expect(page.getByLabel('Location address')).toHaveValue('address')

      await page
        .getByLabel('Question text')
        .fill('Choose your service location')
      await adminQuestions.clickSubmitButtonAndNavigate('Update')
    })

    await test.step('Verify updated question exists', async () => {
      await adminQuestions.expectDraftQuestionExist('editable-map-question')
    })
  })
})
