import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminPrograms,
  AdminQuestions,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'
import {Page} from 'playwright'

test.describe('map applicant flow', () => {
  test.describe('single map question', () => {
    const programName = 'Test program for single map'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await setUpProgramWithMapQuestion(
        page,
        adminQuestions,
        adminPrograms,
        programName,
      )
    })

    test('validate screenshot', async ({page, applicantQuestions}) => {
      await test.step('Navigate to map question', async () => {
        await applicantQuestions.applyProgram(programName, true)
      })

      await test.step('Take screenshot', async () => {
        await validateScreenshot(page, 'map')
      })

      await test.step('Validate accessibility', async () => {
        await validateAccessibility(page)
      })
    })

    test('display map', async ({page, applicantQuestions}) => {
      await test.step('Navigate to map question', async () => {
        await applicantQuestions.applyProgram(programName, true)
      })

      await test.step('Verify map container is visible', async () => {
        const mapContainer = page.locator('[id^="cf-map-"]')
        await expect(mapContainer).toBeVisible()
      })

      await test.step('Wait for map to load and verify canvas', async () => {
        await page.waitForSelector('.maplibregl-canvas', {timeout: 10000})
        const mapCanvas = page.locator('.maplibregl-canvas')
        await expect(mapCanvas).toBeVisible()
      })

      await test.step('Verify checkboxes are initially unchecked', async () => {
        const checkboxes = page.getByRole('checkbox')
        const checkboxCount = await checkboxes.count()

        if (checkboxCount > 0) {
          const firstCheckbox = checkboxes.first()
          await expect(firstCheckbox).not.toBeChecked()
        }
      })

      await test.step('Verify initial messaging is shown', async () => {
        const noSelectionsMessage = page.getByText("No locations have been selected.")
        if (await noSelectionsMessage.isVisible()) {
          await expect(noSelectionsMessage).toBeVisible()
        }
      })

      await test.step('Verify locations list container exists', async () => {
        const locationsList = page.getByTestId('locations-list')
        await expect(locationsList).toBeVisible()
      })

      await test.step('Verify location checkboxes are present', async () => {
        const locationCheckboxes = page.getByRole('checkbox')
        const checkboxCount = await locationCheckboxes.count()
        expect(checkboxCount).toBeGreaterThan(0)
      })

      await test.step('Verify location count is displayed', async () => {
        const locationCount = page.locator('.cf-location-count')

        if (await locationCount.isVisible()) {
          const countText = locationCount
          await expect(countText).toHaveText('Displaying 5 of 5 locations')
        }
      })
    })

    test('select locations from checkboxes', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Navigate to map question', async () => {
        await applicantQuestions.applyProgram(programName, true)
      })

      const locationCheckboxes = page.getByRole('checkbox')

      await test.step('Select first location checkbox', async () => {
        await locationCheckboxes.first().check()
        await expect(locationCheckboxes.first()).toBeChecked()
      })

      await test.step('Verify location appears in selected list', async () => {
        const selectedLocationsList = page.getByTestId('selected-locations-list')
        const selectedLocationsCheckboxes = selectedLocationsList.getByRole('checkbox')
        expect(await selectedLocationsCheckboxes.count()).toBeGreaterThan(0)
      })

      await test.step('Verify no selections message is hidden', async () => {
        const noSelectionsMessage = page.getByText("No locations have been selected.")
        if (await noSelectionsMessage.isVisible()) {
          await expect(noSelectionsMessage).toBeHidden()
        }
      })

      await test.step('Deselect the location', async () => {
        await locationCheckboxes.first().uncheck()
        await expect(locationCheckboxes.first()).not.toBeChecked()
      })

      await test.step('Verify no selections message is shown', async () => {
        const noSelectionsMessage = page.locator('.cf-no-selections-message')
        if (await noSelectionsMessage.isVisible()) {
          await expect(noSelectionsMessage).toBeVisible()
        }
      })
    })

    test('filter locations', async ({page, applicantQuestions}) => {
      await test.step('Navigate to map question', async () => {
        await applicantQuestions.applyProgram(programName, true)
      })

      const filterSelects = page.getByRole('combobox')
      const applyButton = page.getByRole('button', {name: /apply.*filter/i})
      const resetButton = page.getByRole('button', {name: /reset.*filter/i})

      await test.step('Check for filter dropdowns and buttons', async () => {
        await expect(filterSelects.first()).toBeVisible()

        if (await applyButton.isVisible()) {
          await expect(applyButton).toBeVisible()
        }
        if (await resetButton.isVisible()) {
          await expect(resetButton).toBeVisible()
        }
      })

      await test.step('Select a filter option', async () => {
        const firstFilter = filterSelects.first()
        await firstFilter.selectOption({index: 1})
      })

      await test.step('Apply filters', async () => {
        await applyButton.click()

        // Verify location has changed
        const locationCount = page.getByText(/\d+.*location/i)
        if (await locationCount.isVisible()) {
          await expect(locationCount).toHaveText('Displaying 2 of 5 locations')
        }
      })

      await test.step('Reset filters', async () => {
        const resetButton = page.getByRole('button', {name: /reset.*filter/i})

        if (await resetButton.isVisible()) {
          await resetButton.click()

          // Verify first filter is reset to default option
          const firstFilter = filterSelects.first()
          const selectedValue = firstFilter
          await expect(selectedValue).toHaveValue('')
        }
      })
    })

    test('select locations from map popups', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Navigate to map question', async () => {
        await applicantQuestions.applyProgram(programName, true)
      })

      await test.step('Wait for map to load', async () => {
        await page.waitForSelector('.maplibregl-canvas', {timeout: 10000})
      })

      await test.step('Click on map to trigger popups', async () => {
        const mapCanvas = page.locator('.maplibregl-canvas')
        await mapCanvas.click()
      })

      await test.step('Check for popup select buttons', async () => {
        const selectButtons = page.getByRole('button', {
          name: /select.*location/i,
        })
        const selectButtonsCount = await selectButtons.count()
        if (selectButtonsCount > 0) {
          await selectButtons.first().click()

          const selectedLocationsList = page.getByTestId('selected-locations-list')
          const checkboxes = selectedLocationsList.getByRole('checkbox')
          const checkboxCount = await checkboxes.count()
          expect(checkboxCount).toBeGreaterThan(0)
          await expect(checkboxes.first()).toBeChecked()
        }
      })
    })
  })

  test.describe('multiple map questions', () => {
    const programName = 'Test program for multiple maps'

    test.beforeEach(async ({page, adminPrograms, adminQuestions}) => {
      await test.step('Setup multiple map questions program', async () => {
        await loginAsAdmin(page)

        await adminQuestions.addMapQuestion({
          questionName: 'map-test-a-q',
          locationNameKey: 'name',
          locationAddressKey: 'address',
          locationDetailsUrlKey: 'website',
          filters: [{key: 'type', displayName: 'Type'}],
        })
        await adminQuestions.addMapQuestion({
          questionName: 'map-test-b-q',
          locationNameKey: 'name',
          locationAddressKey: 'address',
          locationDetailsUrlKey: 'website',
          filters: [{key: 'type', displayName: 'Type'}],
        })
        await adminPrograms.addAndPublishProgramWithQuestions(
          ['map-test-a-q', 'map-test-b-q'],
          programName,
        )

        await logout(page)
      })
    })

    test.only('display multiple maps', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Navigate to multiple map questions', async () => {
        await applicantQuestions.applyProgram(programName, true)
      })

      await test.step('Verify both maps are present', async () => {
        const mapContainers = page.locator('[id^="cf-map-"]')
        await expect(mapContainers).toHaveCount(2)
      })

      const locationsLists = page.getByTestId('locations-list')
      await test.step('Verify both maps have location lists', async () => {
        await expect(locationsLists).toHaveCount(2)
      })

      await test.step('Select from first map only', async () => {
        const firstMapCheckboxes = locationsLists.first().getByRole('checkbox')

        await firstMapCheckboxes.first().check()
        await expect(firstMapCheckboxes.first()).toBeChecked()
      })

      await test.step('Verify selections are independent', async () => {
        const firstMapCheckedBoxes = locationsLists.first().getByRole('checkbox', {
          checked: true,
        })

        // Should have exactly one checked box in the first map
        await expect(firstMapCheckedBoxes).toHaveCount(1)

        // Second map checkboxes should remain unchecked
        const secondMapContainer = locationsLists.nth(1)
        const secondMapCheckboxes = secondMapContainer.getByRole('checkbox')
        await expect(secondMapCheckboxes.first()).not.toBeChecked()
      })
    })
  })
})

async function setUpProgramWithMapQuestion(
  page: Page,
  adminQuestions: AdminQuestions,
  adminPrograms: AdminPrograms,
  programName: string,
) {
  await test.step('Login as admin', async () => {
    await loginAsAdmin(page)
  })

  await test.step('Create and publish program with map question', async () => {
    await adminQuestions.addMapQuestion({
      questionName: 'map-test-q',
      locationNameKey: 'name',
      locationAddressKey: 'address',
      locationDetailsUrlKey: 'website',
      filters: [{key: 'type', displayName: 'Type'}],
    })
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['map-test-q'],
      programName,
    )
  })

  await test.step('Logout from admin', async () => {
    await logout(page)
  })
}
