import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminPrograms,
  AdminQuestions,
  isLocalDevEnvironment,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'
import {Page} from 'playwright'
import * as path from 'path'

// number of locations expected to be visible per page
const EXPECTED_LOCATION_COUNT = 6
// number of locations in the mock data
const TOTAL_LOCATION_COUNT = 7

// map question tests rely on mock web services so they will only work in local dev environments
if (isLocalDevEnvironment()) {
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

        await test.step('Set up route intercept', async () => {
          await page.route(
            'https://tile.openstreetmap.org/**',
            async (route) => {
              await route.fulfill({
                status: 200,
                contentType: 'image/png',
                path: path.join(__dirname, '../../support/mock-tile.png'),
              })
            },
          )
        })
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

        await test.step('Wait for map to load and verify canvas', async () => {
          const mapContainer = page.getByTestId('map-container')
          await expect(mapContainer).toBeVisible()

          const mapCanvas = mapContainer.getByRole('region', {name: 'Map'})
          await expect(mapCanvas).toBeVisible()
        })

        const locationsList = page.getByRole('group', {
          name: 'Location selection',
        })
        await test.step('Verify locations list container exists', async () => {
          await expect(locationsList).toBeVisible()
        })

        await test.step('Verify initial messaging is shown', async () => {
          const noSelectionsMessage = page.getByText(
            'No locations have been selected.',
          )
          if (await noSelectionsMessage.isVisible()) {
            await expect(noSelectionsMessage).toBeVisible()
          }
        })

        await test.step('Verify location checkboxes count and initial state', async () => {
          const checkboxes = locationsList.getByRole('checkbox')
          await expect(checkboxes).toHaveCount(EXPECTED_LOCATION_COUNT)

          for (const checkbox of await checkboxes.all()) {
            await expect(checkbox).not.toBeChecked()
          }
        })

        await test.step('Verify location count is displayed', async () => {
          const locationCount = page.getByText(
            `Displaying ${TOTAL_LOCATION_COUNT} of ${TOTAL_LOCATION_COUNT} locations`,
          )
          await expect(locationCount).toBeVisible()
        })
      })

      test('select locations from checkboxes', async ({
        page,
        applicantQuestions,
      }) => {
        await test.step('Navigate to map question', async () => {
          await applicantQuestions.applyProgram(programName, true)
        })

        const locationsList = page.getByRole('group', {
          name: 'Location selection',
        })
        const locationCheckboxes =
          locationsList.getByTestId('location-checkbox')

        await test.step('Select first location checkbox', async () => {
          const firstCheckbox = locationCheckboxes.first()
          await firstCheckbox.getByTestId('location-checkbox-label').click()
          await expect(
            firstCheckbox.getByTestId('location-checkbox-input'),
          ).toBeChecked()

          const selectedLocationsList = page.getByTestId(
            'selected-locations-list',
          )
          await expect(
            selectedLocationsList.getByTestId('location-checkbox'),
          ).toHaveCount(1)
        })

        await test.step('Verify location appears in selected list', async () => {
          const selectedLocationsList = page.getByTestId(
            'selected-locations-list',
          )
          await expect(
            selectedLocationsList.getByTestId('location-checkbox'),
          ).toHaveCount(1)
        })

        await test.step('Confirm the rest of the checkboxes are disabled', async () => {
          const restOfCheckboxes = (await locationCheckboxes.all()).slice(1)
          for (const checkbox of restOfCheckboxes) {
            await expect(
              checkbox.getByTestId('location-checkbox-input'),
            ).toBeDisabled()
          }
        })

        await test.step('Verify no selections message is hidden', async () => {
          const noSelectionsMessage = page.getByText(
            'No locations have been selected.',
          )
          if (await noSelectionsMessage.isVisible()) {
            await expect(noSelectionsMessage).toBeHidden()
          }
        })

        await test.step('Deselect the location', async () => {
          const firstCheckbox = locationCheckboxes.first()
          await firstCheckbox.getByTestId('location-checkbox-label').click()
          await expect(
            firstCheckbox.getByTestId('location-checkbox-input'),
          ).not.toBeChecked()
        })

        await test.step('Confirm all the checkboxes are enabled', async () => {
          for (const checkbox of await locationCheckboxes.all()) {
            await expect(
              checkbox.getByTestId('location-checkbox-input'),
            ).toBeEnabled()
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

        await test.step('Dismiss attribution control to avoid overlap', async () => {
          // Close the MapLibre attribution control so it doesn't cover popups
          const attributionButton = page.getByLabel('Toggle attribution')
          await attributionButton.click()
        })

        await test.step('Click on map to trigger popups', async () => {
          const mapContainer = page.getByTestId('map-container')
          const mapCanvas = mapContainer.getByRole('region', {name: 'Map'})
          await mapCanvas.click()
        })

        await test.step('Click popup select buttons', async () => {
          const selectButtons = page.getByRole('button', {
            name: /select.*location/i,
          })
          const selectButtonsCount = await selectButtons.count()
          expect(selectButtonsCount).toBe(1)
          await selectButtons.first().click()

          const selectButtonAfter = page.getByRole('button', {
            name: /selected/i,
          })
          await expect(selectButtonAfter).toBeVisible()
        })

        await test.step('Confirm location is selected all the other checkboxes are disabled', async () => {
          const selectedLocationsList = page.getByTestId(
            'selected-locations-list',
          )
          const selectedCheckboxes = selectedLocationsList.getByRole('checkbox')
          const selectedCheckboxCount = await selectedCheckboxes.count()
          expect(selectedCheckboxCount).toBeGreaterThan(0)
          await expect(selectedCheckboxes.first()).toBeChecked()

          const locationsList = page.getByTestId('locations-list')
          const allLocationCheckboxes = await locationsList
            .getByRole('checkbox')
            .all()
          for (const checkbox of allLocationCheckboxes) {
            await expect(checkbox).toBeDisabled()
          }
        })
      })

      test('filter locations', async ({page, applicantQuestions}) => {
        await test.step('Navigate to map question', async () => {
          await applicantQuestions.applyProgram(programName, true)
        })

        const filterSelects = page.getByRole('combobox')
        const applyButton = page.getByRole('button', {name: /apply.*filters/i})
        const resetButton = page.getByRole('button', {name: /clear.*filters/i})

        await test.step('Check for filter dropdowns and buttons', async () => {
          await expect(filterSelects.first()).toBeVisible()

          await expect(applyButton).toBeVisible()
          await expect(resetButton).toBeVisible()
        })

        await test.step('Select a filter option', async () => {
          const firstFilter = filterSelects.first()
          await firstFilter.selectOption({index: 2})
        })

        await test.step('Apply filters', async () => {
          await applyButton.click()

          // Verify location has changed
          const locationCount = page.getByText(
            /Displaying \d+ of \d+ locations/i,
          )
          await locationCount.isVisible()
          await expect(locationCount).toHaveText('Displaying 1 of 7 locations')
        })

        await test.step('Apply another filter', async () => {
          await filterSelects.nth(1).selectOption({index: 2})
          await applyButton.click()

          const noResultsMessage = page.getByText(
            'No results found. Please try adjusting your filters.',
          )
          await noResultsMessage.isVisible()
          const locationsList = page.getByRole('group', {
            name: 'Location selection',
          })
          await validateScreenshot(locationsList, 'map-filters-no-results')
        })

        await test.step('Reset filters', async () => {
          const resetButton = page.getByRole('button', {
            name: /clear.*filters/i,
          })
          await resetButton.click()

          // Verify first filter is reset to default option
          const firstFilter = filterSelects.first()
          const selectedValue = firstFilter
          await expect(selectedValue).toHaveValue('')
        })
      })

      test('paginate map question locations', async ({
        page,
        applicantQuestions,
      }) => {
        await test.step('Navigate to map question', async () => {
          await applicantQuestions.applyProgram(programName, true)
        })

        const locationsList = page.getByRole('group', {
          name: 'Location selection',
        })
        const checkboxes = locationsList.getByRole('checkbox')
        await test.step('Verify initial locations list state', async () => {
          await expect(checkboxes).toHaveCount(EXPECTED_LOCATION_COUNT)
        })

        await test.step('Go to next page', async () => {
          const nextButton = page.getByText('Next')
          await nextButton.click()
          const checkboxes = locationsList.getByRole('checkbox')
          await expect(checkboxes).toHaveCount(1)
        })

        const previousButton = page.getByText('Previous', {exact: true})
        await test.step('Go back to previous page', async () => {
          await expect(previousButton).toBeVisible()
          await previousButton.click()
          await expect(checkboxes).toHaveCount(EXPECTED_LOCATION_COUNT)
        })
      })

      test('pin icon and popup button state changes on selection', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName, true)

        const mapContainer = page.getByTestId('map-container')
        const mapCanvas = mapContainer.getByRole('region', {name: 'Map'})
        const locationsList = page.getByRole('group', {
          name: 'Location selection',
        })
        const selectedLocationsList = page.getByTestId(
          'selected-locations-list',
        )

        // Dismiss attribution control to avoid overlap with popups
        await page.getByLabel('Toggle attribution').click()

        await test.step('Select location and verify pin changes color', async () => {
          const firstCheckbox = locationsList
            .getByTestId('location-checkbox')
            .first()
          await firstCheckbox.getByTestId('location-checkbox-label').click()
          await validateScreenshot(mapContainer, 'map-with-selected-pin')
          await firstCheckbox.getByTestId('location-checkbox-label').click()
        })

        await test.step('Verify popup button states', async () => {
          await mapCanvas.click()
          await validateScreenshot(mapContainer, 'map-popup-button-unselected')
          const selectButton = page.getByRole('button', {
            name: /select.*location/i,
          })

          await selectButton.click()
          await validateScreenshot(mapContainer, 'map-popup-button-selected')

          await expect(
            selectedLocationsList.getByTestId('location-checkbox'),
          ).toHaveCount(1)
        })

        await test.step('Unselect and verify select button returns to default', async () => {
          const selectedCheckbox = selectedLocationsList
            .getByTestId('location-checkbox')
            .first()
          await selectedCheckbox.getByTestId('location-checkbox-label').click()

          await expect(
            page.getByText('No locations have been selected.'),
          ).toBeVisible()

          await validateScreenshot(
            mapContainer,
            'map-popup-button-unselected-after-unselect',
          )
        })
      })

      test('toggle between map and list view', async ({
        page,
        applicantQuestions,
      }) => {
        await test.step('Navigate to map question', async () => {
          await applicantQuestions.applyProgram(programName, true)
        })

        await test.step('Set viewport to mobile size', async () => {
          // The size of a Google Pixel 7
          await page.setViewportSize({width: 412, height: 915})
        })

        await test.step('Take screenshot of list view', async () => {
          await validateScreenshot(page, 'map-question-mobile-view-list')
        })

        await test.step('Switch to map view', async () => {
          const switchToMapViewButton = page.getByText('Switch to map view')
          await switchToMapViewButton.click()
        })

        await test.step('Take screenshot of map view', async () => {
          await validateScreenshot(page, 'map-question-mobile-view-map')
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

      test('display multiple maps', async ({page, applicantQuestions}) => {
        await test.step('Navigate to multiple map questions', async () => {
          await applicantQuestions.applyProgram(programName, true)
        })

        await test.step('Verify both maps are present', async () => {
          const mapContainers = page.getByTestId('map-container')
          await expect(mapContainers).toHaveCount(2)

          const mapCanvases = page.getByRole('region', {name: 'Map'})
          await expect(mapCanvases).toHaveCount(2)
        })

        const locationsLists = page.getByRole('group', {
          name: 'Location selection',
        })
        await test.step('Verify both maps have location lists', async () => {
          await expect(locationsLists).toHaveCount(2)
        })

        await test.step('Select from first map', async () => {
          const firstMapCheckboxes = locationsLists
            .first()
            .getByTestId('location-checkbox')

          const firstCheckbox = firstMapCheckboxes.first()
          await firstCheckbox.getByTestId('location-checkbox-label').click()
          await expect(
            firstCheckbox.getByTestId('location-checkbox-input'),
          ).toBeChecked()
        })

        await test.step('Select from second map', async () => {
          const secondMapCheckboxes = locationsLists
            .nth(1)
            .getByTestId('location-checkbox')

          const secondCheckbox = secondMapCheckboxes.nth(1)
          await secondCheckbox.getByTestId('location-checkbox-label').click()
          await expect(
            secondCheckbox.getByTestId('location-checkbox-input'),
          ).toBeChecked()
        })

        await test.step('Verify selections are independent', async () => {
          const firstMapCheckedBoxes = locationsLists
            .first()
            .getByRole('checkbox', {
              checked: true,
            })

          const secondMapCheckedBoxes = locationsLists
            .nth(1)
            .getByRole('checkbox', {
              checked: true,
            })

          // Each map should have exactly one checked box
          await expect(firstMapCheckedBoxes).toHaveCount(1)
          await expect(secondMapCheckedBoxes).toHaveCount(1)

          // first map should have the first location checked
          const firstMapContainer = locationsLists.first()
          const firstMapCheckboxes = firstMapContainer.getByRole('checkbox')
          await expect(firstMapCheckboxes.first()).toBeChecked()
          await expect(firstMapCheckboxes.nth(1)).not.toBeChecked()

          // Second map should have the second location checked
          const secondMapContainer = locationsLists.nth(1)
          const secondMapCheckboxes = secondMapContainer.getByRole('checkbox')
          await expect(secondMapCheckboxes.first()).not.toBeChecked()
          await expect(secondMapCheckboxes.nth(1)).toBeChecked()
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
        filters: [
          {key: 'type', displayName: 'Type'},
          {
            key: 'requiresDirectEnrollment',
            displayName: 'Requires Direct Enrollment',
          },
        ],
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
}
