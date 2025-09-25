import {expect, test} from '../support/civiform_fixtures'
import {
  isLocalDevEnvironment,
  loginAsAdmin,
  waitForPageJsLoad,
} from '../support'

test.describe('Create and edit map question', () => {
  if (isLocalDevEnvironment()) {
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

        const locationNameSelect = page.getByLabel('Name key')
        const locationAddressSelect = page.getByLabel('Address key')
        const locationDetailsUrlSelect = page.getByLabel(
          'View more details URL key',
        )

        await expect(locationNameSelect).toBeVisible()
        await expect(locationAddressSelect).toBeVisible()
        await expect(locationDetailsUrlSelect).toBeVisible()

        for (const dropdown of [
          locationNameSelect,
          locationAddressSelect,
          locationDetailsUrlSelect,
        ]) {
          await expect(dropdown.locator('option[value="name"]')).toBeAttached()
          await expect(
            dropdown.locator('option[value="address"]'),
          ).toBeAttached()
        }

        await expect(
          page
            .getByTestId('question-settings')
            .getByText('Filters', {exact: true}),
        ).toBeVisible()
        await expect(
          page.getByText(
            'Add up to six filters to make available to applicants.',
          ),
        ).toBeVisible()

        await page.getByRole('button', {name: 'Add filter'}).click()

        const filterKeySelect = page.getByTestId('key-select')
        const filterDisplayInput = page.getByTestId('display-name-input')

        await expect(filterKeySelect).toBeVisible()
        await expect(filterDisplayInput).toBeVisible()
        await expect(
          filterKeySelect.locator('option[value="name"]'),
        ).toBeAttached()
        await expect(
          filterKeySelect.locator('option[value="address"]'),
        ).toBeAttached()
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

        await expect(
          page.getByLabel('Maximum location selections'),
        ).toHaveValue('2')
        await expect(page.getByLabel('Name key')).toHaveValue('name')
        await expect(page.getByLabel('Address key')).toHaveValue('address')

        await page
          .getByLabel('Question text')
          .fill('Choose your service location')
        await adminQuestions.clickSubmitButtonAndNavigate('Update')
      })

      await test.step('Verify updated question exists', async () => {
        await adminQuestions.expectDraftQuestionExist('editable-map-question')
      })
    })
  }
})
