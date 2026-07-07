import {test, expect} from './support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin} from './support'
import {ProgramVisibility} from './support/admin_programs'
import {ApiSchemaViewerPage} from './page/admin/docs/api_schema_viewer_page'

test.describe('Viewing API schema', () => {
  const program1 = 'comprehensive-sample-program'
  const program2 = 'minimal-sample-program'

  test.beforeEach(async ({page, seeding}) => {
    await enableFeatureFlag(page, 'ADMIN_UI_MIGRATION_SC_ENABLED')
    await seeding.seedProgramsAndCategories()
  })

  test.use({
    bypassCSP: true,
  })

  test('Views OpenApi Schema', async ({page, adminPrograms}) => {
    const schemaViewerPage = new ApiSchemaViewerPage(page)

    await test.step('Login as admin and publish drafts', async () => {
      await page.goto('/')
      await loginAsAdmin(page)
      await adminPrograms.publishAllDrafts()
    })

    await test.step(`Going to slugless route defaults to ${program1}`, async () => {
      await schemaViewerPage.goto()
      await expect(
        schemaViewerPage.getSwaggerProgramHeading(program1),
      ).toBeAttached()
    })

    await test.step(`Change program to ${program2}`, async () => {
      await schemaViewerPage.selectProgram(program2)
      await expect(
        schemaViewerPage.getSwaggerProgramHeading(program2),
      ).toBeAttached()
    })

    await test.step(`Change status to draft for ${program2} shows error message`, async () => {
      await schemaViewerPage.selectStatus('draft')
      await expect(schemaViewerPage.getErrorMessage()).toContainText(
        'A program with this status could not be found',
      )
    })

    await test.step(`Change status back to active for ${program2}`, async () => {
      await schemaViewerPage.selectStatus('active')
      await expect(
        schemaViewerPage.getSwaggerProgramHeading(program2),
      ).toBeAttached()
    })

    await test.step('Change openapi version to swagger-v2', async () => {
      await schemaViewerPage.selectOpenApiVersion('swagger-v2')
      await expect(
        schemaViewerPage.getSwaggerProgramHeading(program2),
      ).toBeAttached()
    })

    await test.step('Add external program does not show in program options', async () => {
      await adminPrograms.addExternalProgram(
        'External Program Name',
        'short program description',
        'https://usa.gov',
        ProgramVisibility.PUBLIC,
      )
      await schemaViewerPage.goto()

      await expect(schemaViewerPage.getProgramSelect()).not.toContainText(
        'external-program-name',
      )
    })
  })
})
