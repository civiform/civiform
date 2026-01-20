import {test, expect} from './support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, waitForPageJsLoad} from './support'
import {ProgramVisibility} from './support/admin_programs'

test.describe('Viewing API docs', () => {
  const program1 = 'comprehensive-sample-program'
  const program2 = 'minimal-sample-program'

  test.beforeEach(async ({seeding}) => {
    await seeding.seedProgramsAndCategories()
  })

  test.use({
    bypassCSP: true,
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

    await test.step('Add external program does not show in program options', async () => {
      await enableFeatureFlag(page, 'external_program_cards_enabled')
      await adminPrograms.addExternalProgram(
        'External Program Name',
        'short program description',
        'https://usa.gov',
        ProgramVisibility.PUBLIC,
      )
      await page.goto('/docs/api/schemas')
      await waitForPageJsLoad(page)

      await expect(
        page.getByRole('combobox', {name: 'Program'}),
      ).not.toContainText('external-program-name')
    })
  })
})
