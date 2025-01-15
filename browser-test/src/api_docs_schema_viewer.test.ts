import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  seedProgramsAndCategories,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'

test.describe('Viewing API docs', () => {
  const program1 = 'comprehensive-sample-program'
  const program2 = 'minimal-sample-program'

  test.beforeEach(async ({page}) => {
    await seedProgramsAndCategories(page)
    await enableFeatureFlag(page, 'api_generated_docs_enabled')
  })

  test('Views OpenApi Schema', async ({page, adminPrograms}) => {
    await test.step('Login as admin and publish drafts', async () => {
      await page.goto('/')
      await loginAsAdmin(page)
      await adminPrograms.publishAllDrafts()
    })

    await test.step(`Going to slugless route defaults ${program1}`, async () => {
      await page.goto('/docs/api/schemas')
      await waitForPageJsLoad(page)
      await expect(page.getByRole('heading', {name: program1})).toBeAttached()
    })

    await test.step(`Change program to ${program2}`, async () => {
      await page.getByRole('combobox', {name: 'Program'}).selectOption(program2)
      await waitForPageJsLoad(page)
      await expect(page.getByRole('heading', {name: program2})).toBeAttached()
    })

    await test.step(`Change status to draft for ${program2} shows error message`, async () => {
      await page.getByRole('combobox', {name: 'Status'}).selectOption('draft')
      await waitForPageJsLoad(page)
      await expect(page.getByTestId('ui-error')).toContainText(
        'A program with this status could not be found',
      )
    })

    await test.step(`Change status to active for ${program2}`, async () => {
      await page.getByRole('combobox', {name: 'Status'}).selectOption('active')
      await waitForPageJsLoad(page)
      await expect(page.getByRole('heading', {name: program2})).toBeAttached()
    })

    await test.step(`Change openapi version to swagger-v2`, async () => {
      await page
        .getByRole('combobox', {name: 'OpenApi Version'})
        .selectOption('swagger-v2')
      await waitForPageJsLoad(page)
      await expect(page.getByRole('heading', {name: program2})).toBeAttached()
    })

    // We'll take a screenshot to verify that the swagger-ui appears to load correctly. Adding
    // the mask over the program ID as that value changes.
    await validateScreenshot(page, 'api-docs-schema-viewer', true, false, [
      page.locator('.version:not(.version-stamp .version)'),
    ])
  })
})
