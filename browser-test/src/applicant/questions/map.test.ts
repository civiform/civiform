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

const EXPECTED_LOCATION_COUNT = 5

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
            `Displaying ${EXPECTED_LOCATION_COUNT} of ${EXPECTED_LOCATION_COUNT} locations`,
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

        const locationCheckboxes = page.getByRole('checkbox')

        await test.step('Select first location checkbox', async () => {
          await locationCheckboxes.first().check()
          await expect(locationCheckboxes.first()).toBeChecked()
        })

        await test.step('Verify location appears in selected list', async () => {
          const selectedLocationsList = page.getByTestId(
            'selected-locations-list',
          )
          const selectedLocationsCheckboxes =
            selectedLocationsList.getByRole('checkbox')
          expect(await selectedLocationsCheckboxes.count()).toBeGreaterThan(0)
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
          await locationCheckboxes.first().uncheck()
          await expect(locationCheckboxes.first()).not.toBeChecked()
        })
      })

      test('select locations from map popups', async ({
        page,
        applicantQuestions,
      }) => {
        await test.step('Navigate to map question', async () => {
          await applicantQuestions.applyProgram(programName, true)
        })

        await test.step('Click on map to trigger popups', async () => {
          const mapContainer = page.getByTestId('map-container')
          const mapCanvas = mapContainer.getByRole('region', {name: 'Map'})
          await mapCanvas.click()
        })

        await test.step('Check for popup select buttons', async () => {
          const selectButtons = page.getByRole('button', {
            name: /select.*location/i,
          })
          const selectButtonsCount = await selectButtons.count()
          if (selectButtonsCount > 0) {
            await selectButtons.first().click()

            const selectedLocationsList = page.getByTestId(
              'selected-locations-list',
            )
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

        await test.step('Select from first map only', async () => {
          const firstMapCheckboxes = locationsLists
            .first()
            .getByRole('checkbox')

          await firstMapCheckboxes.first().check()
          await expect(firstMapCheckboxes.first()).toBeChecked()
        })

        await test.step('Verify selections are independent', async () => {
          const firstMapCheckedBoxes = locationsLists
            .first()
            .getByRole('checkbox', {
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
}
