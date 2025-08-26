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
          await expect(mapCanvas).toBeVisible({timeout: 10000})
        })

        await test.step('Verify location checkboxes count and initial state', async () => {
          const checkboxes = page.getByRole('checkbox')
          await expect(checkboxes).toHaveCount(EXPECTED_LOCATION_COUNT)

          for (const checkbox of await checkboxes.all()) {
            await expect(checkbox).not.toBeChecked()
          }
        })

        await test.step('Verify locations list container exists', async () => {
          const locationsList = page.getByRole('group', {
            name: 'Location selection',
          })
          await expect(locationsList).toBeVisible()
        })

        await test.step('Verify location count is displayed', async () => {
          const locationCount = page.getByText(
            `Displaying ${EXPECTED_LOCATION_COUNT} of ${EXPECTED_LOCATION_COUNT} locations`,
          )
          await expect(locationCount).toBeVisible()
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
