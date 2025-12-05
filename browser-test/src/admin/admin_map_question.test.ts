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
            'Select up to six filters to make available to applicants.',
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

      await test.step('Verify filter button disabled after 6 filters', async () => {
        for (let i = 0; i < 5; i++) {
          const responsePromise = page.waitForResponse(
            (response) =>
              response.url().includes('addMapQuestionFilter') &&
              response.status() === 200,
          )
          await page.getByRole('button', {name: 'Add filter'}).click()
          await responsePromise
        }

        const addFilterButton = page.getByRole('button', {name: 'Add filter'})
        await expect(addFilterButton).toBeDisabled()
      })

      await test.step('Add a tag', async () => {
        const tagButton = page.getByRole('button', {name: 'Add tag'})
        await tagButton.click()

        const tagKeySelect = page.getByTestId('tag-key-select')
        const tagDisplayInput = page.getByTestId('tag-display-name-input')
        const tagValueInput = page.getByTestId('tag-value-input')
        const tagTextInput = page.getByTestId('tag-text-input')

        await expect(tagKeySelect).toBeVisible()
        await expect(tagDisplayInput).toBeVisible()
        await expect(tagValueInput).toBeVisible()
        await expect(tagTextInput).toBeVisible()

        await expect(
          tagKeySelect.locator('option[value="name"]'),
        ).toBeAttached()
        await expect(
          tagKeySelect.locator('option[value="address"]'),
        ).toBeAttached()

        await tagKeySelect.selectOption({value: 'requiresDirectEnrollment'})
        await tagDisplayInput.fill('Requires direct enrollment')
        await tagValueInput.fill('true')
        await tagTextInput.fill(
          'You selected a location that requires direct enrollment!',
        )
      })

      await test.step('Delete the tag', async () => {
        const deleteTagButton = page.getByRole('button', {
          name: 'Delete tag',
        })
        await expect(deleteTagButton).toBeVisible()
        await deleteTagButton.click()
        await expect(deleteTagButton).toBeHidden()

        const tagKeySelect = page.getByTestId('tag-key-select')
        const tagDisplayInput = page.getByTestId('tag-display-name-input')
        const tagValueInput = page.getByTestId('tag-value-input')
        const tagTextInput = page.getByTestId('tag-text-input')
        await expect(tagKeySelect).toBeHidden()
        await expect(tagDisplayInput).toBeHidden()
        await expect(tagValueInput).toBeHidden()
        await expect(tagTextInput).toBeHidden()
      })
    })

    test('Map question validation with empty settings', async ({
      page,
      adminQuestions,
    }) => {
      await test.step('Create map question with empty settings', async () => {
        await loginAsAdmin(page)
        await adminQuestions.gotoAdminQuestionsPage()
        await page.click('#create-question-button')
        await page.click('#create-map-question')
        await waitForPageJsLoad(page)

        await adminQuestions.fillInQuestionBasics({
          questionName: 'map-question-with-empty-settings',
          description: 'test map question',
          questionText: 'test map question',
          helpText: 'map question',
        })
      })

      await test.step('Verify validation prevents submission with empty settings', async () => {
        await adminQuestions.clickSubmitButtonAndNavigate('Create')

        await expect(page.getByText('Create')).toBeVisible()
        await expect(page.getByLabel('GeoJSON endpoint')).toBeVisible()

        const toastContainer = await page.innerHTML('#toast-container')
        expect(toastContainer).toContain('bg-cf-toast-error')
        expect(toastContainer).toContain('cannot be empty')
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
